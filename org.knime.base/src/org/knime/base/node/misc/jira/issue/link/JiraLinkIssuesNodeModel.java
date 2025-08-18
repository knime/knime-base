package org.knime.base.node.misc.jira.issue.link;

import org.knime.base.node.misc.jira.JiraClient;
import org.knime.core.data.*;
import org.knime.core.data.def.*;
import org.knime.core.node.*;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.webui.node.impl.WebUINodeConfiguration;
import org.knime.core.webui.node.impl.WebUINodeModel;
import org.knime.base.node.misc.jira.conn.JiraConnectionPortObject;
import org.knime.base.node.misc.jira.shared.JiraNodePorts;
import org.knime.base.node.misc.jira.conn.JiraConnectionPortObjectSpec;

@SuppressWarnings({"restriction","deprecation"})
final class JiraLinkIssuesNodeModel extends WebUINodeModel<JiraLinkIssuesNodeSettings> {
    JiraLinkIssuesNodeModel(final WebUINodeConfiguration c, final Class<JiraLinkIssuesNodeSettings> s){ super(c, s);}    

    JiraLinkIssuesNodeModel(final org.knime.core.node.port.PortType[] in, final org.knime.core.node.port.PortType[] out,
            final Class<JiraLinkIssuesNodeSettings> s) { super(in, out, s); }

    @Override
    protected BufferedDataTable[] execute(final PortObject[] in, final ExecutionContext exec, final JiraLinkIssuesNodeSettings s) throws Exception {
        final var spec = new DataTableSpec(
            new DataColumnSpecCreator("Inward", StringCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Outward", StringCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Type", StringCell.TYPE).createSpec());
        final var con = exec.createDataContainer(spec);
        final String inward=s.m_inward==null?"":s.m_inward, outward=s.m_outward==null?"":s.m_outward, type=s.m_type==null?"relates to":s.m_type;
        final String payload = "{\"type\":{\"name\":\""+JiraClient.escapeJson(type)+"\"},\"inwardIssue\":{\"key\":\""+JiraClient.escapeJson(inward)+"\"},\"outwardIssue\":{\"key\":\""+JiraClient.escapeJson(outward)+"\"}}";
    boolean hasConn = JiraNodePorts.hasConnection(in);
    if (hasConn) JiraNodePorts.validateConnectionSpec(JiraNodePorts.getConnectionSpec(in));
        final var res = hasConn
            ? JiraClient.post((JiraConnectionPortObjectSpec)((JiraConnectionPortObject)in[0]).getSpec(), "/rest/api/3/issueLink", payload)
            : JiraClient.post(s.m_baseUrl, s.m_auth.m_email, s.m_auth.m_token.getPassword(), "/rest/api/3/issueLink", payload);
        if (res.statusCode() / 100 != 2) throw new RuntimeException("Jira link issues failed: HTTP " + res.statusCode() + " body=" + res.body());
        con.addRowToTable(new DefaultRow(RowKey.createRowKey(0), new DataCell[]{ new StringCell(inward), new StringCell(outward), new StringCell(type)}));
        con.close(); return new BufferedDataTable[]{ con.getTable() };
    }

    @Override
    protected DataTableSpec[] configure(final PortObjectSpec[] inSpecs, final JiraLinkIssuesNodeSettings s) throws InvalidSettingsException {
        final var out = new DataTableSpec(
            new DataColumnSpecCreator("Inward", StringCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Outward", StringCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Type", StringCell.TYPE).createSpec());
        return new DataTableSpec[]{ out };
    }
}
