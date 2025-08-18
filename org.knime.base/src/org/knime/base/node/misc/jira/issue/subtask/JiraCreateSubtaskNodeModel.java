package org.knime.base.node.misc.jira.issue.subtask;

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
final class JiraCreateSubtaskNodeModel extends WebUINodeModel<JiraCreateSubtaskNodeSettings> {
    JiraCreateSubtaskNodeModel(final WebUINodeConfiguration c, final Class<JiraCreateSubtaskNodeSettings> s){ super(c, s);}    

    JiraCreateSubtaskNodeModel(final org.knime.core.node.port.PortType[] in, final org.knime.core.node.port.PortType[] out,
            final Class<JiraCreateSubtaskNodeSettings> s) { super(in, out, s); }

    @Override
    protected BufferedDataTable[] execute(final PortObject[] in, final ExecutionContext exec, final JiraCreateSubtaskNodeSettings s) throws Exception {
        final var spec = new DataTableSpec(
            new DataColumnSpecCreator("Key", StringCell.TYPE).createSpec(),
            new DataColumnSpecCreator("URL", StringCell.TYPE).createSpec());
        final var con = exec.createDataContainer(spec);
        final String parent = s.m_parentKey==null?"":s.m_parentKey;
        final String summary = s.m_summary==null?"":s.m_summary;
        final String desc = s.m_description==null?"":s.m_description;
        final String type = s.m_issueType==null?"Sub-task":s.m_issueType;
        final String adf = "{\"type\":\"doc\",\"version\":1,\"content\":[{\"type\":\"paragraph\",\"content\":[{\"type\":\"text\",\"text\":\"" + JiraClient.escapeJson(desc) + "\"}]}]}";
        final String payload = "{\"fields\":{\"parent\":{\"key\":\""+JiraClient.escapeJson(parent)+"\"},\"summary\":\""+JiraClient.escapeJson(summary)+"\",\"description\":"+adf+",\"issuetype\":{\"name\":\""+JiraClient.escapeJson(type)+"\"}}}";
    boolean hasConn = JiraNodePorts.hasConnection(in);
    if (hasConn) JiraNodePorts.validateConnectionSpec(JiraNodePorts.getConnectionSpec(in));
        final var res = hasConn
            ? JiraClient.post((org.knime.base.node.misc.jira.conn.JiraConnectionPortObjectSpec)((JiraConnectionPortObject)in[0]).getSpec(), "/rest/api/3/issue", payload)
            : JiraClient.post(s.m_baseUrl, s.m_auth.m_email, s.m_auth.m_token.getPassword(), "/rest/api/3/issue", payload);
        if (res.statusCode()/100 != 2) throw new RuntimeException("Create sub-task failed: HTTP "+res.statusCode()+" body="+res.body());
        final String key = JiraClient.extract(res.body(), "\"key\"\s*:\s*\"", "\"");
        final String base = hasConn
            ? ((org.knime.base.node.misc.jira.conn.JiraConnectionPortObjectSpec)((JiraConnectionPortObject)in[0]).getSpec()).m_baseUrl
            : s.m_baseUrl;
        final String url = JiraClient.trimBase(base) + "/browse/" + key;
        con.addRowToTable(new DefaultRow(RowKey.createRowKey(0), new DataCell[]{ new StringCell(key), new StringCell(url)}));
        con.close(); return new BufferedDataTable[]{ con.getTable() };
    }

    @Override
    protected DataTableSpec[] configure(final PortObjectSpec[] inSpecs, final JiraCreateSubtaskNodeSettings s) throws InvalidSettingsException {
        final var out = new DataTableSpec(
            new DataColumnSpecCreator("Key", StringCell.TYPE).createSpec(),
            new DataColumnSpecCreator("URL", StringCell.TYPE).createSpec());
        return new DataTableSpec[]{ out };
    }
}
