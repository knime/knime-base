package org.knime.base.node.misc.jira.issue.delete;

import java.io.IOException;
import java.util.Optional;
import org.apache.xmlbeans.XmlException;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ConfigurableNodeFactory;
import org.knime.core.node.NodeDescription;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeView;
import org.knime.core.node.context.NodeCreationConfiguration;
import org.knime.core.webui.node.dialog.NodeDialog;
import org.knime.core.webui.node.dialog.NodeDialogFactory;
import org.knime.core.webui.node.dialog.NodeDialogManager;
import org.knime.core.webui.node.dialog.SettingsType;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeDialog;
import org.knime.core.webui.node.impl.WebUINodeConfiguration;
import org.knime.core.webui.node.impl.WebUINodeFactory;
import org.xml.sax.SAXException;
import org.knime.base.node.misc.jira.conn.JiraConnectionPortObject;
import org.knime.core.node.port.PortTypeRegistry;

@SuppressWarnings({"restriction","deprecation"})
public final class JiraDeleteIssueNodeFactory extends ConfigurableNodeFactory<JiraDeleteIssueNodeModel> implements NodeDialogFactory {
    private static final WebUINodeConfiguration CONFIG = WebUINodeConfiguration.builder()
        .name("Jira Delete Issue")
        .icon("./jira-icon.svg")
        .shortDescription("Delete an issue by key.")
    .fullDescription("Deletes a Jira issue by key.")
        .modelSettingsClass(JiraDeleteIssueNodeSettings.class)
    .addInputPort("Jira Connection", PortTypeRegistry.getInstance().getPortType(JiraConnectionPortObject.class, true), "Connection from the Jira Connector", true)
        .addOutputPort("Result", BufferedDataTable.TYPE, "One row with the deleted issue key")
        .build();
    @Override protected NodeDescription createNodeDescription() throws SAXException, IOException, XmlException { return WebUINodeFactory.createNodeDescription(CONFIG); }
    @Override public JiraDeleteIssueNodeModel createNodeModel() { return new JiraDeleteIssueNodeModel(CONFIG, JiraDeleteIssueNodeSettings.class);}    
    @Override public int getNrNodeViews(){ return 0; }
    @Override public NodeView<JiraDeleteIssueNodeModel> createNodeView(final int i, final JiraDeleteIssueNodeModel m){ return null; }
    @Override protected boolean hasDialog(){ return true; }
    @Override public NodeDialogPane createNodeDialogPane(){ return NodeDialogManager.createLegacyFlowVariableNodeDialog(createNodeDialog()); }
    @Override public NodeDialog createNodeDialog(){ return new DefaultNodeDialog(SettingsType.MODEL, JiraDeleteIssueNodeSettings.class);}    

    @Override
    protected Optional<PortsConfigurationBuilder> createPortsConfigBuilder() {
        final var b = new PortsConfigurationBuilder();
        b.addOptionalInputPortGroup("Jira Connection",
            PortTypeRegistry.getInstance().getPortType(JiraConnectionPortObject.class));
        b.addFixedOutputPortGroup("Result", BufferedDataTable.TYPE);
        return Optional.of(b);
    }

    @Override
    protected JiraDeleteIssueNodeModel createNodeModel(final NodeCreationConfiguration creationConfig) {
        final var portsConfig = creationConfig.getPortConfig().orElseThrow(IllegalStateException::new);
        return new JiraDeleteIssueNodeModel(portsConfig.getInputPorts(), portsConfig.getOutputPorts(),
            JiraDeleteIssueNodeSettings.class);
    }

    @Override
    protected NodeDialogPane createNodeDialogPane(final NodeCreationConfiguration creationConfig) {
        return NodeDialogManager.createLegacyFlowVariableNodeDialog(createNodeDialog());
    }
}
