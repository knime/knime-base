package org.knime.base.node.misc.jira.issue.get;

import org.knime.base.node.misc.jira.JiraClient;
import org.knime.core.data.*;
import org.knime.core.data.def.*;
import org.knime.core.node.*;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.webui.node.impl.WebUINodeConfiguration;
import org.knime.core.webui.node.impl.WebUINodeModel;
import org.knime.base.node.misc.jira.conn.JiraConnectionPortObject;
import org.knime.base.node.misc.jira.conn.JiraConnectionPortObjectSpec;
import org.knime.base.node.misc.jira.shared.JiraNodePorts;

@SuppressWarnings({"restriction","deprecation"})
final class JiraGetIssueNodeModel extends WebUINodeModel<JiraGetIssueNodeSettings> {
    JiraGetIssueNodeModel(final WebUINodeConfiguration c, final Class<JiraGetIssueNodeSettings> s){ super(c, s);}    

    JiraGetIssueNodeModel(final org.knime.core.node.port.PortType[] in, final org.knime.core.node.port.PortType[] out,
            final Class<JiraGetIssueNodeSettings> s) { super(in, out, s); }

    @Override
    protected BufferedDataTable[] execute(final PortObject[] in, final ExecutionContext exec, final JiraGetIssueNodeSettings s) throws Exception {
        final var spec = new DataTableSpec(
            new DataColumnSpecCreator("Key", StringCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Summary", StringCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Status", StringCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Assignee", StringCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Self", StringCell.TYPE).createSpec());
        final var con = exec.createDataContainer(spec);
        final String key = s.m_key == null ? "" : s.m_key;
    boolean hasConn = JiraNodePorts.hasConnection(in);
    if (hasConn) JiraNodePorts.validateConnectionSpec(JiraNodePorts.getConnectionSpec(in));
        final var res = hasConn
            ? JiraClient.get((JiraConnectionPortObjectSpec)((JiraConnectionPortObject)in[0]).getSpec(), "/rest/api/3/issue/" + JiraClient.enc(key))
            : JiraClient.get(s.m_baseUrl, s.m_auth.m_email, s.m_auth.m_token.getPassword(), "/rest/api/3/issue/" + JiraClient.enc(key));
        if (res.statusCode() / 100 != 2) throw new RuntimeException("Jira get issue failed: HTTP " + res.statusCode() + " body=" + res.body());
        final String body = res.body();
        String k = JiraClient.extract(body, "\"key\"\s*:\s*\"", "\"");
        String sum = JiraClient.extract(body, "\"summary\"\s*:\s*\"", "\"");
        String status = JiraClient.extract(body, "\"status\".*?\"name\"\s*:\s*\"", "\"");
        String ass = JiraClient.extract(body, "\"assignee\".*?\"displayName\"\s*:\s*\"", "\"");
        String self = JiraClient.extract(body, "\"self\"\s*:\s*\"", "\"");
        con.addRowToTable(new DefaultRow(RowKey.createRowKey(0), new DataCell[]{ new StringCell(k), new StringCell(sum), new StringCell(status), new StringCell(ass), new StringCell(self)}));
        con.close();
        return new BufferedDataTable[]{ con.getTable() };
    }

    @Override
    protected DataTableSpec[] configure(final PortObjectSpec[] inSpecs, final JiraGetIssueNodeSettings s) throws InvalidSettingsException {
    // Accept that inSpecs[0] may be null when connection port is not connected
        final var out = new DataTableSpec(
            new DataColumnSpecCreator("Key", StringCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Summary", StringCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Status", StringCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Assignee", StringCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Self", StringCell.TYPE).createSpec());
        return new DataTableSpec[]{ out };
    }
}
