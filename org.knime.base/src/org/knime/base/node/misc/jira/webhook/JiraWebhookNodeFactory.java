package org.knime.base.node.misc.jira.webhook;

import java.io.IOException;
import org.apache.xmlbeans.XmlException;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ConfigurableNodeFactory;
import org.knime.core.node.NodeDescription;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeView;
import org.knime.core.node.context.NodeCreationConfiguration;
import org.knime.core.node.port.PortTypeRegistry;
import org.knime.base.node.misc.jira.conn.JiraConnectionPortObject;
import org.knime.core.webui.node.dialog.NodeDialog;
import org.knime.core.webui.node.dialog.NodeDialogFactory;
import org.knime.core.webui.node.dialog.NodeDialogManager;
import org.knime.core.webui.node.dialog.SettingsType;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeDialog;
import org.knime.core.webui.node.impl.WebUINodeConfiguration;
import org.knime.core.webui.node.impl.WebUINodeFactory;
import org.xml.sax.SAXException;

@SuppressWarnings({"restriction","deprecation"})
public final class JiraWebhookNodeFactory extends ConfigurableNodeFactory<JiraWebhookNodeModel> implements NodeDialogFactory {
    private static final WebUINodeConfiguration CONFIG = WebUINodeConfiguration.builder()
        .name("Jira Webhook Wait")
        .icon("./jira-icon.svg")
        .shortDescription("Wait for a Jira webhook and output the request data.")
        .fullDescription("Starts a lightweight HTTP listener that waits for a single webhook request from Jira and then outputs the received request.")
        .modelSettingsClass(JiraWebhookNodeSettings.class)
    .addInputPort("Jira Connection", PortTypeRegistry.getInstance().getPortType(JiraConnectionPortObject.class, true),
        "Connection from Jira Connector", true)
        .addOutputPort("Webhook", BufferedDataTable.TYPE, "Request information and payload")
        .build();

    @Override protected NodeDescription createNodeDescription() throws SAXException, IOException, XmlException { return WebUINodeFactory.createNodeDescription(CONFIG); }
    @Override public JiraWebhookNodeModel createNodeModel() { return new JiraWebhookNodeModel(CONFIG, JiraWebhookNodeSettings.class);}    
    @Override public int getNrNodeViews(){ return 0; }
    @Override public NodeView<JiraWebhookNodeModel> createNodeView(final int i, final JiraWebhookNodeModel m){ return null; }
    @Override protected boolean hasDialog(){ return true; }
    @Override public NodeDialogPane createNodeDialogPane(){ return NodeDialogManager.createLegacyFlowVariableNodeDialog(createNodeDialog()); }
    @Override public NodeDialog createNodeDialog(){ return new DefaultNodeDialog(SettingsType.MODEL, JiraWebhookNodeSettings.class);}    

    @Override
    protected JiraWebhookNodeModel createNodeModel(final NodeCreationConfiguration creationConfig) {
        final var portsConfig = creationConfig.getPortConfig().orElseThrow(IllegalStateException::new);
        return new JiraWebhookNodeModel(portsConfig.getInputPorts(), portsConfig.getOutputPorts(), JiraWebhookNodeSettings.class);
    }

    @Override
    protected java.util.Optional<PortsConfigurationBuilder> createPortsConfigBuilder() {
        final var b = new PortsConfigurationBuilder();
        b.addOptionalInputPortGroup("Jira Connection",
            PortTypeRegistry.getInstance().getPortType(JiraConnectionPortObject.class));
        b.addFixedOutputPortGroup("Webhook", BufferedDataTable.TYPE);
        return java.util.Optional.of(b);
    }

    @Override
    protected NodeDialogPane createNodeDialogPane(final NodeCreationConfiguration creationConfig) {
        return NodeDialogManager.createLegacyFlowVariableNodeDialog(createNodeDialog());
    }
}
