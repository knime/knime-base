package org.knime.base.node.misc.jira.issue.transition;

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
final class JiraTransitionIssueNodeModel extends WebUINodeModel<JiraTransitionIssueNodeSettings> {
    JiraTransitionIssueNodeModel(final WebUINodeConfiguration c, final Class<JiraTransitionIssueNodeSettings> s){ super(c, s);}    

    JiraTransitionIssueNodeModel(final org.knime.core.node.port.PortType[] in, final org.knime.core.node.port.PortType[] out,
            final Class<JiraTransitionIssueNodeSettings> s) { super(in, out, s); }

    @Override
    protected BufferedDataTable[] execute(final PortObject[] in, final ExecutionContext exec, final JiraTransitionIssueNodeSettings s) throws Exception {
        final var spec = new DataTableSpec(
            new DataColumnSpecCreator("Key", StringCell.TYPE).createSpec(),
            new DataColumnSpecCreator("URL", StringCell.TYPE).createSpec());
        final var con = exec.createDataContainer(spec);
        final String key = s.m_key == null ? "" : s.m_key;
        final String name = s.m_transition == null ? "" : s.m_transition;

        // fetch transitions to resolve id by name
    boolean hasConn = JiraNodePorts.hasConnection(in);
    if (hasConn) JiraNodePorts.validateConnectionSpec(JiraNodePorts.getConnectionSpec(in));
        final var resT = hasConn
            ? JiraClient.get((org.knime.base.node.misc.jira.conn.JiraConnectionPortObjectSpec)((JiraConnectionPortObject)in[0]).getSpec(), "/rest/api/3/issue/" + JiraClient.enc(key) + "/transitions")
            : JiraClient.get(s.m_baseUrl, s.m_auth.m_email, s.m_auth.m_token.getPassword(), "/rest/api/3/issue/" + JiraClient.enc(key) + "/transitions");
        if (resT.statusCode()/100 != 2) throw new RuntimeException("Fetching transitions failed: HTTP " + resT.statusCode() + " body=" + resT.body());
        final String body = resT.body();
        String id = "";
        int p = 0;
        while ((p = body.indexOf("\"name\"", p)) >= 0) {
            int s1 = body.indexOf('"', body.indexOf(':', p) + 1) + 1; int s2 = body.indexOf('"', s1);
            String n = (s1>0 && s2>s1) ? body.substring(s1, s2) : "";
            int iid = body.indexOf("\"id\"", s2);
            int i1 = iid>0 ? body.indexOf('"', body.indexOf(':', iid) + 1) + 1 : -1; int i2 = i1>0 ? body.indexOf('"', i1) : -1;
            String cand = (i1>0 && i2>i1) ? body.substring(i1, i2) : "";
            if (n.equals(name)) { id = cand; break; }
            p = s2 + 1;
        }
        if (id.isEmpty()) throw new RuntimeException("Transition '" + name + "' not found");

        final String payload = "{\"transition\":{\"id\":\"" + JiraClient.escapeJson(id) + "\"}}";
        final var res = hasConn
            ? JiraClient.post((org.knime.base.node.misc.jira.conn.JiraConnectionPortObjectSpec)((JiraConnectionPortObject)in[0]).getSpec(), "/rest/api/3/issue/" + JiraClient.enc(key) + "/transitions", payload)
            : JiraClient.post(s.m_baseUrl, s.m_auth.m_email, s.m_auth.m_token.getPassword(), "/rest/api/3/issue/" + JiraClient.enc(key) + "/transitions", payload);
        if (res.statusCode() / 100 != 2) throw new RuntimeException("Jira transition failed: HTTP " + res.statusCode() + " body=" + res.body());
        final String base = hasConn
            ? ((org.knime.base.node.misc.jira.conn.JiraConnectionPortObjectSpec)((JiraConnectionPortObject)in[0]).getSpec()).m_baseUrl
            : s.m_baseUrl;
        final String url = JiraClient.trimBase(base) + "/browse/" + key;
        con.addRowToTable(new DefaultRow(RowKey.createRowKey(0), new DataCell[]{ new StringCell(key), new StringCell(url)}));
        con.close();
        return new BufferedDataTable[]{ con.getTable() };
    }

    @Override
    protected DataTableSpec[] configure(final PortObjectSpec[] inSpecs, final JiraTransitionIssueNodeSettings s) throws InvalidSettingsException {
        final var out = new DataTableSpec(
            new DataColumnSpecCreator("Key", StringCell.TYPE).createSpec(),
            new DataColumnSpecCreator("URL", StringCell.TYPE).createSpec());
        return new DataTableSpec[]{ out };
    }
}
