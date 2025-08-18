/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME AG, Zurich, Switzerland
 * ---------------------------------------------------------------------
 */
package org.knime.base.node.misc.jira.search;

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
public final class JiraSearchIssuesNodeFactory extends ConfigurableNodeFactory<JiraSearchIssuesNodeModel> implements NodeDialogFactory {

    private static final WebUINodeConfiguration CONFIG = WebUINodeConfiguration.builder()
        .name("Jira Search Issues")
        .icon("./jira-icon.svg")
        .shortDescription("Searches Jira issues by JQL and returns key, summary, status, assignee.")
    .fullDescription("Searches Jira issues using a JQL query and returns a table with common fields.")
        .modelSettingsClass(JiraSearchIssuesNodeSettings.class)
    .addInputPort("Jira Connection", PortTypeRegistry.getInstance().getPortType(JiraConnectionPortObject.class, true),
            "Connection from Jira Connector", true)
        .addOutputPort("Issues", BufferedDataTable.TYPE, "Issues found by the JQL query")
        .build();

    @Override protected NodeDescription createNodeDescription() throws SAXException, IOException, XmlException {
        return WebUINodeFactory.createNodeDescription(CONFIG);
    }

    @Override public JiraSearchIssuesNodeModel createNodeModel() { return new JiraSearchIssuesNodeModel(CONFIG, JiraSearchIssuesNodeSettings.class); }

    @Override public int getNrNodeViews() { return 0; }
    @Override public NodeView<JiraSearchIssuesNodeModel> createNodeView(final int i, final JiraSearchIssuesNodeModel m) { return null; }
    @Override protected boolean hasDialog() { return true; }
    @Override public NodeDialogPane createNodeDialogPane() { return NodeDialogManager.createLegacyFlowVariableNodeDialog(createNodeDialog()); }
    @Override public NodeDialog createNodeDialog() { return new DefaultNodeDialog(SettingsType.MODEL, JiraSearchIssuesNodeSettings.class); }

    @Override
    protected Optional<PortsConfigurationBuilder> createPortsConfigBuilder() {
        final var b = new PortsConfigurationBuilder();
        b.addOptionalInputPortGroup("Jira Connection",
            PortTypeRegistry.getInstance().getPortType(JiraConnectionPortObject.class));
        b.addFixedOutputPortGroup("Issues", BufferedDataTable.TYPE);
        return Optional.of(b);
    }

    @Override
    protected JiraSearchIssuesNodeModel createNodeModel(final NodeCreationConfiguration creationConfig) {
        final var portsConfig = creationConfig.getPortConfig().orElseThrow(IllegalStateException::new);
        return new JiraSearchIssuesNodeModel(portsConfig.getInputPorts(), portsConfig.getOutputPorts(),
            JiraSearchIssuesNodeSettings.class);
    }

    @Override
    protected NodeDialogPane createNodeDialogPane(final NodeCreationConfiguration creationConfig) {
        return NodeDialogManager.createLegacyFlowVariableNodeDialog(createNodeDialog());
    }
}
