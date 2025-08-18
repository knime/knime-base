package org.knime.base.node.misc.jira.issue.comment;

import org.knime.base.node.misc.jira.JiraClient;
import org.knime.core.data.*;
import org.knime.core.data.def.*;
import org.knime.core.node.*;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.base.node.misc.jira.conn.JiraConnectionPortObject;
import org.knime.base.node.misc.jira.shared.JiraNodePorts;
import org.knime.core.webui.node.impl.WebUINodeConfiguration;
import org.knime.core.webui.node.impl.WebUINodeModel;

@SuppressWarnings({"restriction","deprecation"})
final class JiraAddCommentNodeModel extends WebUINodeModel<JiraAddCommentNodeSettings> {
    JiraAddCommentNodeModel(final WebUINodeConfiguration c, final Class<JiraAddCommentNodeSettings> s){ super(c, s);}    

    JiraAddCommentNodeModel(final org.knime.core.node.port.PortType[] in, final org.knime.core.node.port.PortType[] out,
            final Class<JiraAddCommentNodeSettings> s) { super(in, out, s); }

    @Override
    protected BufferedDataTable[] execute(final PortObject[] in, final ExecutionContext exec, final JiraAddCommentNodeSettings s) throws Exception {
        final var spec = new DataTableSpec(
            new DataColumnSpecCreator("Key", StringCell.TYPE).createSpec(),
            new DataColumnSpecCreator("URL", StringCell.TYPE).createSpec());
        final var con = exec.createDataContainer(spec);
        final String key = s.m_key == null ? "" : s.m_key;
        final String adf = "{\"type\":\"doc\",\"version\":1,\"content\":[{\"type\":\"paragraph\",\"content\":[{\"type\":\"text\",\"text\":\"" + JiraClient.escapeJson(s.m_comment) + "\"}]}]}";
        final String payload = "{\"body\":" + adf + "}";
    boolean hasConn = JiraNodePorts.hasConnection(in);
    if (hasConn) JiraNodePorts.validateConnectionSpec(JiraNodePorts.getConnectionSpec(in));
        final var res = hasConn
            ? JiraClient.post((org.knime.base.node.misc.jira.conn.JiraConnectionPortObjectSpec)((JiraConnectionPortObject)in[0]).getSpec(), "/rest/api/3/issue/" + JiraClient.enc(key) + "/comment", payload)
            : JiraClient.post(s.m_baseUrl, s.m_auth.m_email, s.m_auth.m_token.getPassword(), "/rest/api/3/issue/" + JiraClient.enc(key) + "/comment", payload);
        if (res.statusCode() / 100 != 2) throw new RuntimeException("Jira add comment failed: HTTP " + res.statusCode() + " body=" + res.body());
        final String base = hasConn
            ? ((org.knime.base.node.misc.jira.conn.JiraConnectionPortObjectSpec)((JiraConnectionPortObject)in[0]).getSpec()).m_baseUrl
            : s.m_baseUrl;
        final String url = JiraClient.trimBase(base) + "/browse/" + key;
        con.addRowToTable(new DefaultRow(RowKey.createRowKey(0), new DataCell[]{ new StringCell(key), new StringCell(url)}));
        con.close();
        return new BufferedDataTable[]{ con.getTable() };
    }

    @Override
    protected DataTableSpec[] configure(final PortObjectSpec[] inSpecs, final JiraAddCommentNodeSettings s) throws InvalidSettingsException {
        final var out = new DataTableSpec(
            new DataColumnSpecCreator("Key", StringCell.TYPE).createSpec(),
            new DataColumnSpecCreator("URL", StringCell.TYPE).createSpec());
        return new DataTableSpec[]{ out };
    }
}
