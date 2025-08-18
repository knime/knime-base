/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME AG, Zurich, Switzerland
 * ---------------------------------------------------------------------
 */
package org.knime.base.node.misc.jira.search;

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

@SuppressWarnings({"restriction", "deprecation"})
final class JiraSearchIssuesNodeModel extends WebUINodeModel<JiraSearchIssuesNodeSettings> {

    JiraSearchIssuesNodeModel(final WebUINodeConfiguration c, final Class<JiraSearchIssuesNodeSettings> s) { super(c, s); }

    JiraSearchIssuesNodeModel(final org.knime.core.node.port.PortType[] in, final org.knime.core.node.port.PortType[] out,
            final Class<JiraSearchIssuesNodeSettings> s) { super(in, out, s); }

    @Override
    protected BufferedDataTable[] execute(final PortObject[] in, final ExecutionContext exec, final JiraSearchIssuesNodeSettings s) throws Exception {
        final var spec = new DataTableSpec(
            new DataColumnSpecCreator("Key", StringCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Summary", StringCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Status", StringCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Assignee", StringCell.TYPE).createSpec());
        final var con = exec.createDataContainer(spec);
    boolean hasConn = JiraNodePorts.hasConnection(in);
    if (hasConn) JiraNodePorts.validateConnectionSpec(JiraNodePorts.getConnectionSpec(in));
        final String jql = s.m_jql == null ? "" : s.m_jql;
        final int max = s.m_max <= 0 ? 50 : s.m_max;
        final String fields = (s.m_fields == null || s.m_fields.isBlank()) ? "key,summary,status,assignee" : s.m_fields;
    final var res = hasConn
            ? JiraClient.get((JiraConnectionPortObjectSpec)((JiraConnectionPortObject)in[0]).getSpec(), "/rest/api/3/search?maxResults=" + max + "&fields=" + JiraClient.enc(fields) + "&jql=" + JiraClient.enc(jql))
            : JiraClient.get(s.m_baseUrl, s.m_auth.m_email, s.m_auth.m_token.getPassword(), "/rest/api/3/search?maxResults=" + max + "&fields=" + JiraClient.enc(fields) + "&jql=" + JiraClient.enc(jql));
        if (res.statusCode() / 100 != 2) {
            throw new RuntimeException("Jira search failed: HTTP " + res.statusCode() + " body=" + res.body());
        }
        final String body = res.body();
        int idx = 0; long row = 0;
        while ((idx = body.indexOf("\"key\"", idx)) >= 0) {
            int ks = body.indexOf('"', body.indexOf(':', idx) + 1) + 1;
            int ke = body.indexOf('"', ks);
            if (ks <= 0 || ke <= ks) break;
            String key = body.substring(ks, ke);
            // summary
            int ss = body.indexOf("\"summary\"", ke);
            ss = ss > 0 ? body.indexOf('"', body.indexOf(':', ss) + 1) + 1 : -1;
            int se = ss > 0 ? body.indexOf('"', ss) : -1;
            String summary = (ss > 0 && se > ss) ? body.substring(ss, se) : "";
            // status name
            int st = body.indexOf("\"status\"", ke);
            int sn = st > 0 ? body.indexOf("\"name\"", st) : -1;
            int s1 = sn > 0 ? body.indexOf('"', body.indexOf(':', sn) + 1) + 1 : -1;
            int s2 = s1 > 0 ? body.indexOf('"', s1) : -1;
            String status = (s1 > 0 && s2 > s1) ? body.substring(s1, s2) : "";
            // assignee displayName
            int as = body.indexOf("\"assignee\"", ke);
            int dn = as > 0 ? body.indexOf("\"displayName\"", as) : -1;
            int a1 = dn > 0 ? body.indexOf('"', body.indexOf(':', dn) + 1) + 1 : -1;
            int a2 = a1 > 0 ? body.indexOf('"', a1) : -1;
            String assignee = (a1 > 0 && a2 > a1) ? body.substring(a1, a2) : "";

            con.addRowToTable(new DefaultRow(RowKey.createRowKey(row++), new DataCell[]{ new StringCell(key), new StringCell(summary), new StringCell(status), new StringCell(assignee)}));
            idx = ke + 1;
        }
        con.close();
        return new BufferedDataTable[]{ con.getTable() };
    }

    @Override
    protected DataTableSpec[] configure(final PortObjectSpec[] inSpecs, final JiraSearchIssuesNodeSettings s) throws InvalidSettingsException {
        final var out = new DataTableSpec(
            new DataColumnSpecCreator("Key", StringCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Summary", StringCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Status", StringCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Assignee", StringCell.TYPE).createSpec());
        return new DataTableSpec[]{ out };
    }
}
