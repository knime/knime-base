package org.knime.base.node.misc.jira.issue.transition;

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
public final class JiraTransitionIssueNodeFactory extends ConfigurableNodeFactory<JiraTransitionIssueNodeModel> implements NodeDialogFactory {
    private static final WebUINodeConfiguration CONFIG = WebUINodeConfiguration.builder()
        .name("Jira Transition Issue")
        .icon("./jira-icon.svg")
        .shortDescription("Execute a workflow transition on an issue by transition name.")
    .fullDescription("Executes a workflow transition on an issue by transition name.")
        .modelSettingsClass(JiraTransitionIssueNodeSettings.class)
    .addInputPort("Jira Connection", PortTypeRegistry.getInstance().getPortType(JiraConnectionPortObject.class, true), "Connection from the Jira Connector", true)
        .addOutputPort("Result", BufferedDataTable.TYPE, "One row with key and URL to the issue")
        .build();
    @Override protected NodeDescription createNodeDescription() throws SAXException, IOException, XmlException { return WebUINodeFactory.createNodeDescription(CONFIG); }
    @Override public JiraTransitionIssueNodeModel createNodeModel() { return new JiraTransitionIssueNodeModel(CONFIG, JiraTransitionIssueNodeSettings.class);}    
    @Override public int getNrNodeViews(){ return 0; }
    @Override public NodeView<JiraTransitionIssueNodeModel> createNodeView(final int i, final JiraTransitionIssueNodeModel m){ return null; }
    @Override protected boolean hasDialog(){ return true; }
    @Override public NodeDialogPane createNodeDialogPane(){ return NodeDialogManager.createLegacyFlowVariableNodeDialog(createNodeDialog()); }
    @Override public NodeDialog createNodeDialog(){ return new DefaultNodeDialog(SettingsType.MODEL, JiraTransitionIssueNodeSettings.class);}    

    @Override
    protected Optional<PortsConfigurationBuilder> createPortsConfigBuilder() {
        final var b = new PortsConfigurationBuilder();
        b.addOptionalInputPortGroup("Jira Connection",
            PortTypeRegistry.getInstance().getPortType(JiraConnectionPortObject.class));
        b.addFixedOutputPortGroup("Result", BufferedDataTable.TYPE);
        return Optional.of(b);
    }

    @Override
    protected JiraTransitionIssueNodeModel createNodeModel(final NodeCreationConfiguration creationConfig) {
        final var portsConfig = creationConfig.getPortConfig().orElseThrow(IllegalStateException::new);
        return new JiraTransitionIssueNodeModel(portsConfig.getInputPorts(), portsConfig.getOutputPorts(),
            JiraTransitionIssueNodeSettings.class);
    }

    @Override
    protected NodeDialogPane createNodeDialogPane(final NodeCreationConfiguration creationConfig) {
        return NodeDialogManager.createLegacyFlowVariableNodeDialog(createNodeDialog());
    }
}
