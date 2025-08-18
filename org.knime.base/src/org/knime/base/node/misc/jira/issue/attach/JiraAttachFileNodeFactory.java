package org.knime.base.node.misc.jira.issue.attach;

import java.io.IOException;
import java.util.Optional;
import org.apache.xmlbeans.XmlException;
import org.knime.base.node.misc.jira.conn.JiraConnectionPortObject;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ConfigurableNodeFactory;
import org.knime.core.node.NodeDescription;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeView;
import org.knime.core.node.context.NodeCreationConfiguration;
import org.knime.core.node.port.PortTypeRegistry;
import org.knime.core.webui.node.dialog.NodeDialog;
import org.knime.core.webui.node.dialog.NodeDialogFactory;
import org.knime.core.webui.node.dialog.NodeDialogManager;
import org.knime.core.webui.node.dialog.SettingsType;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeDialog;
import org.knime.core.webui.node.impl.WebUINodeConfiguration;
import org.knime.core.webui.node.impl.WebUINodeFactory;
import org.xml.sax.SAXException;

@SuppressWarnings({"restriction","deprecation"})
public final class JiraAttachFileNodeFactory extends ConfigurableNodeFactory<JiraAttachFileNodeModel> implements NodeDialogFactory {
    private static final WebUINodeConfiguration CONFIG = WebUINodeConfiguration.builder()
        .name("Jira Attach File")
        .icon("./jira-icon.svg")
        .shortDescription("Attach a file to a Jira issue.")
    .fullDescription("Attaches a file to a Jira issue using the Jira REST API.")
        .modelSettingsClass(JiraAttachFileNodeSettings.class)
    .addInputPort("Jira Connection", PortTypeRegistry.getInstance().getPortType(JiraConnectionPortObject.class, true),
            "Connection from Jira Connector", true)
        .addOutputPort("Result", BufferedDataTable.TYPE, "One row with key, filename and size")
        .build();
    @Override protected NodeDescription createNodeDescription() throws SAXException, IOException, XmlException { return WebUINodeFactory.createNodeDescription(CONFIG); }
    @Override public JiraAttachFileNodeModel createNodeModel() { return new JiraAttachFileNodeModel(CONFIG, JiraAttachFileNodeSettings.class);}    
    @Override public int getNrNodeViews(){ return 0; }
    @Override public NodeView<JiraAttachFileNodeModel> createNodeView(final int i, final JiraAttachFileNodeModel m){ return null; }
    @Override protected boolean hasDialog(){ return true; }
    @Override public NodeDialogPane createNodeDialogPane(){ return NodeDialogManager.createLegacyFlowVariableNodeDialog(createNodeDialog()); }
    @Override public NodeDialog createNodeDialog(){ return new DefaultNodeDialog(SettingsType.MODEL, JiraAttachFileNodeSettings.class);}    

    @Override
    protected Optional<PortsConfigurationBuilder> createPortsConfigBuilder() {
        final var b = new PortsConfigurationBuilder();
        b.addOptionalInputPortGroup("Jira Connection",
            PortTypeRegistry.getInstance().getPortType(JiraConnectionPortObject.class));
        b.addFixedOutputPortGroup("Result", BufferedDataTable.TYPE);
        return Optional.of(b);
    }

    @Override
    protected JiraAttachFileNodeModel createNodeModel(final NodeCreationConfiguration creationConfig) {
        final var portsConfig = creationConfig.getPortConfig().orElseThrow(IllegalStateException::new);
        return new JiraAttachFileNodeModel(portsConfig.getInputPorts(), portsConfig.getOutputPorts(),
            JiraAttachFileNodeSettings.class);
    }

    @Override
    protected NodeDialogPane createNodeDialogPane(final NodeCreationConfiguration creationConfig) {
        return NodeDialogManager.createLegacyFlowVariableNodeDialog(createNodeDialog());
    }
}
