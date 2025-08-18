package org.knime.base.node.misc.jira.agile.boards;

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
public final class JiraListBoardsNodeFactory extends ConfigurableNodeFactory<JiraListBoardsNodeModel> implements NodeDialogFactory {
    private static final WebUINodeConfiguration CONFIG = WebUINodeConfiguration.builder()
        .name("Jira List Boards")
        .icon("./jira-icon.svg")
        .shortDescription("List Jira boards (Scrum/Kanban).")
    .fullDescription("Lists Jira boards, optionally filtered by type or project.")
        .modelSettingsClass(JiraListBoardsNodeSettings.class)
    .addInputPort("Jira Connection", PortTypeRegistry.getInstance().getPortType(JiraConnectionPortObject.class, true),
            "Connection from Jira Connector", true)
        .addOutputPort("Boards", BufferedDataTable.TYPE, "Boards with id, name, type, project key")
        .build();
    @Override protected NodeDescription createNodeDescription() throws SAXException, IOException, XmlException { return WebUINodeFactory.createNodeDescription(CONFIG); }
    @Override public JiraListBoardsNodeModel createNodeModel() { return new JiraListBoardsNodeModel(CONFIG, JiraListBoardsNodeSettings.class);}    
    @Override public int getNrNodeViews(){ return 0; }
    @Override public NodeView<JiraListBoardsNodeModel> createNodeView(final int i, final JiraListBoardsNodeModel m){ return null; }
    @Override protected boolean hasDialog(){ return true; }
    @Override public NodeDialogPane createNodeDialogPane(){ return NodeDialogManager.createLegacyFlowVariableNodeDialog(createNodeDialog()); }
    @Override public NodeDialog createNodeDialog(){ return new DefaultNodeDialog(SettingsType.MODEL, JiraListBoardsNodeSettings.class);}    

    @Override
    protected Optional<PortsConfigurationBuilder> createPortsConfigBuilder() {
        final var b = new PortsConfigurationBuilder();
        b.addOptionalInputPortGroup("Jira Connection",
            PortTypeRegistry.getInstance().getPortType(JiraConnectionPortObject.class));
        b.addFixedOutputPortGroup("Boards", BufferedDataTable.TYPE);
        return Optional.of(b);
    }

    @Override
    protected JiraListBoardsNodeModel createNodeModel(final NodeCreationConfiguration creationConfig) {
        final var portsConfig = creationConfig.getPortConfig().orElseThrow(IllegalStateException::new);
        return new JiraListBoardsNodeModel(portsConfig.getInputPorts(), portsConfig.getOutputPorts(),
            JiraListBoardsNodeSettings.class);
    }

    @Override
    protected NodeDialogPane createNodeDialogPane(final NodeCreationConfiguration creationConfig) {
        return NodeDialogManager.createLegacyFlowVariableNodeDialog(createNodeDialog());
    }
}
