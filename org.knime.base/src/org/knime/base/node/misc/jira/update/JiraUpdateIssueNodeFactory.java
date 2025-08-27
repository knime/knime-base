/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME AG, Zurich, Switzerland
 * ---------------------------------------------------------------------
 */
package org.knime.base.node.misc.jira.update;

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
public final class JiraUpdateIssueNodeFactory extends ConfigurableNodeFactory<JiraUpdateIssueNodeModel> implements NodeDialogFactory {

    private static final WebUINodeConfiguration CONFIG = WebUINodeConfiguration.builder()
        .name("Jira Update Issue")
        .icon("./jira-icon.svg")
        .shortDescription("Updates fields of an existing Jira issue.")
    .fullDescription("Updates fields of an existing Jira issue.")
        .modelSettingsClass(JiraUpdateIssueNodeSettings.class)
    .addInputPort("Jira Connection", PortTypeRegistry.getInstance().getPortType(JiraConnectionPortObject.class, true), "Connection from the Jira Connector", true)
        .addInputPort("Issue Table", BufferedDataTable.TYPE, "Optional table with an Issue Key column; one update per row.", true)
        .addOutputPort("Updated", BufferedDataTable.TYPE, "One row with key and URL")
        .build();

    @Override protected NodeDescription createNodeDescription() throws SAXException, IOException, XmlException {
        return WebUINodeFactory.createNodeDescription(CONFIG);
    }

    @Override public JiraUpdateIssueNodeModel createNodeModel() { return new JiraUpdateIssueNodeModel(CONFIG, JiraUpdateIssueNodeSettings.class); }

    @Override public int getNrNodeViews() { return 0; }
    @Override public NodeView<JiraUpdateIssueNodeModel> createNodeView(final int i, final JiraUpdateIssueNodeModel m) { return null; }
    @Override protected boolean hasDialog() { return true; }
    @Override public NodeDialogPane createNodeDialogPane() { return NodeDialogManager.createLegacyFlowVariableNodeDialog(createNodeDialog()); }
    @Override public NodeDialog createNodeDialog() { return new DefaultNodeDialog(SettingsType.MODEL, JiraUpdateIssueNodeSettings.class); }

    @Override
    protected Optional<PortsConfigurationBuilder> createPortsConfigBuilder() {
        final var b = new PortsConfigurationBuilder();
        b.addOptionalInputPortGroup("Jira Connection",
            PortTypeRegistry.getInstance().getPortType(JiraConnectionPortObject.class));
    b.addOptionalInputPortGroup("Issue Table", BufferedDataTable.TYPE);
        b.addFixedOutputPortGroup("Updated", BufferedDataTable.TYPE);
        return Optional.of(b);
    }

    @Override
    protected JiraUpdateIssueNodeModel createNodeModel(final NodeCreationConfiguration creationConfig) {
        final var portsConfig = creationConfig.getPortConfig().orElseThrow(IllegalStateException::new);
        return new JiraUpdateIssueNodeModel(portsConfig.getInputPorts(), portsConfig.getOutputPorts(),
            JiraUpdateIssueNodeSettings.class);
    }

    @Override
    protected NodeDialogPane createNodeDialogPane(final NodeCreationConfiguration creationConfig) {
        return NodeDialogManager.createLegacyFlowVariableNodeDialog(createNodeDialog());
    }
}
