/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME AG, Zurich, Switzerland
 * ---------------------------------------------------------------------
 */
package org.knime.base.node.misc.jira;

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

/**
 * WebUI Node Factory for the Jira Ticket Creator node.
 */
@SuppressWarnings({"restriction", "deprecation"})
public final class JiraTicketCreatorNodeFactory extends ConfigurableNodeFactory<JiraTicketCreatorNodeModel> implements NodeDialogFactory {

    private static final WebUINodeConfiguration CONFIG = WebUINodeConfiguration.builder()
        .name("Jira Ticket Creator")
        .icon("./jira-icon.svg")
        .shortDescription("Creates a Jira issue via REST and outputs its key and URL.")
        .fullDescription("""
                Creates a Jira issue in the specified project using the Jira Cloud/Server REST API.\n\n"
                + "Authenticate with e‑mail and API token (Cloud) or username/password (Server)."
                + " The node returns a single-row table with the issue key and a URL to the issue."
                + """)
        .modelSettingsClass(JiraTicketCreatorNodeSettings.class)
    // Use a configurable (add/remove) optional connection port
    .addInputPort("Jira Connection", PortTypeRegistry.getInstance().getPortType(JiraConnectionPortObject.class, true),
                "Connection from the Jira Connector", true)
        .addOutputPort("Created Issue", BufferedDataTable.TYPE, "Single-row table with the created issue key and URL.")
        .build();

    @Override
    protected NodeDescription createNodeDescription() throws SAXException, IOException, XmlException {
        return WebUINodeFactory.createNodeDescription(CONFIG);
    }

    @Override
    public JiraTicketCreatorNodeModel createNodeModel() {
        return new JiraTicketCreatorNodeModel(CONFIG, JiraTicketCreatorNodeSettings.class);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getNrNodeViews() { return 0; }

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeView<JiraTicketCreatorNodeModel> createNodeView(final int viewIndex, final JiraTicketCreatorNodeModel nodeModel) { return null; }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean hasDialog() { return true; }

    @Override
    public NodeDialogPane createNodeDialogPane() {
        return NodeDialogManager.createLegacyFlowVariableNodeDialog(createNodeDialog());
    }

    @Override
    public NodeDialog createNodeDialog() {
        return new DefaultNodeDialog(SettingsType.MODEL, JiraTicketCreatorNodeSettings.class);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Optional<PortsConfigurationBuilder> createPortsConfigBuilder() {
        final var b = new PortsConfigurationBuilder();
        // Optional Jira connection input that can be added/removed by the user
        b.addOptionalInputPortGroup("Jira Connection",
            PortTypeRegistry.getInstance().getPortType(JiraConnectionPortObject.class));
        // Fixed single output table with issue key and URL
        b.addFixedOutputPortGroup("Created Issue", BufferedDataTable.TYPE);
        return Optional.of(b);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected JiraTicketCreatorNodeModel createNodeModel(final NodeCreationConfiguration creationConfig) {
        final var portsConfig = creationConfig.getPortConfig().orElseThrow(IllegalStateException::new);
        return new JiraTicketCreatorNodeModel(portsConfig.getInputPorts(), portsConfig.getOutputPorts(),
            JiraTicketCreatorNodeSettings.class);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected NodeDialogPane createNodeDialogPane(final NodeCreationConfiguration creationConfig) {
    return NodeDialogManager.createLegacyFlowVariableNodeDialog(createNodeDialog());
    }
}
