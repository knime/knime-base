package org.knime.base.node.misc.jira.issue.delete;

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
final class JiraDeleteIssueNodeModel extends WebUINodeModel<JiraDeleteIssueNodeSettings> {
    JiraDeleteIssueNodeModel(final WebUINodeConfiguration c, final Class<JiraDeleteIssueNodeSettings> s){ super(c, s);}    

    JiraDeleteIssueNodeModel(final org.knime.core.node.port.PortType[] in, final org.knime.core.node.port.PortType[] out,
            final Class<JiraDeleteIssueNodeSettings> s) { super(in, out, s); }

    @Override
    protected BufferedDataTable[] execute(final PortObject[] in, final ExecutionContext exec, final JiraDeleteIssueNodeSettings s) throws Exception {
        final var spec = new DataTableSpec(
            new DataColumnSpecCreator("Key", StringCell.TYPE).createSpec());
        final var con = exec.createDataContainer(spec);
        final String key = s.m_key==null?"":s.m_key;
    boolean hasConn = JiraNodePorts.hasConnection(in);
    if (hasConn) JiraNodePorts.validateConnectionSpec(JiraNodePorts.getConnectionSpec(in));
        final var res = hasConn
            ? JiraClient.delete((org.knime.base.node.misc.jira.conn.JiraConnectionPortObjectSpec)((JiraConnectionPortObject)in[0]).getSpec(), "/rest/api/3/issue/" + JiraClient.enc(key))
            : JiraClient.delete(s.m_baseUrl, s.m_auth.m_email, s.m_auth.m_token.getPassword(), "/rest/api/3/issue/" + JiraClient.enc(key));
        if (res.statusCode() / 100 != 2 && res.statusCode() != 204) throw new RuntimeException("Jira delete issue failed: HTTP " + res.statusCode() + " body=" + res.body());
        con.addRowToTable(new DefaultRow(RowKey.createRowKey(0), new DataCell[]{ new StringCell(key)}));
        con.close(); return new BufferedDataTable[]{ con.getTable() };
    }

    @Override
    protected DataTableSpec[] configure(final PortObjectSpec[] inSpecs, final JiraDeleteIssueNodeSettings s) throws InvalidSettingsException {
        final var out = new DataTableSpec(
            new DataColumnSpecCreator("Key", StringCell.TYPE).createSpec());
        return new DataTableSpec[]{ out };
    }
}
