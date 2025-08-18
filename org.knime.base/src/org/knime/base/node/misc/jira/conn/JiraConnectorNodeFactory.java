package org.knime.base.node.misc.jira.conn;

import java.io.IOException;
import org.apache.xmlbeans.XmlException;
import org.knime.core.node.NodeDescription;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;
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
public final class JiraConnectorNodeFactory extends NodeFactory<JiraConnectorNodeModel> implements NodeDialogFactory {

    private static final WebUINodeConfiguration CONFIG = WebUINodeConfiguration.builder()
        .name("Jira Connector")
        .icon("./jira-conn.svg")
        .shortDescription("Creates a Jira connection to be reused by Jira nodes.")
        .fullDescription("Outputs a Jira connection containing base URL and credentials. Downstream Jira nodes can"
            + " use this connection for authentication instead of per-node credentials.")
        .modelSettingsClass(JiraConnectorNodeSettings.class)
    .addOutputPort("Jira Connection", PortTypeRegistry.getInstance().getPortType(JiraConnectionPortObject.class), "Jira connection port")
        .build();

    @Override protected NodeDescription createNodeDescription() throws SAXException, IOException, XmlException { return WebUINodeFactory.createNodeDescription(CONFIG); }
    @Override public JiraConnectorNodeModel createNodeModel() { return new JiraConnectorNodeModel(CONFIG, JiraConnectorNodeSettings.class);}    
    @Override public int getNrNodeViews(){ return 0; }
    @Override public NodeView<JiraConnectorNodeModel> createNodeView(final int i, final JiraConnectorNodeModel m){ return null; }
    @Override protected boolean hasDialog(){ return true; }
    @Override public NodeDialogPane createNodeDialogPane(){ return NodeDialogManager.createLegacyFlowVariableNodeDialog(createNodeDialog()); }
    @Override public NodeDialog createNodeDialog(){ return new DefaultNodeDialog(SettingsType.MODEL, JiraConnectorNodeSettings.class);}    
}
