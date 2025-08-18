package org.knime.base.node.misc.jira.agile.boards;

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
final class JiraListBoardsNodeModel extends WebUINodeModel<JiraListBoardsNodeSettings> {
    JiraListBoardsNodeModel(final WebUINodeConfiguration c, final Class<JiraListBoardsNodeSettings> s){ super(c, s);}    

    JiraListBoardsNodeModel(final org.knime.core.node.port.PortType[] in, final org.knime.core.node.port.PortType[] out,
            final Class<JiraListBoardsNodeSettings> s) { super(in, out, s); }

    @Override
    protected BufferedDataTable[] execute(final PortObject[] in, final ExecutionContext exec, final JiraListBoardsNodeSettings s) throws Exception {
        final var spec = new DataTableSpec(
            new DataColumnSpecCreator("Id", LongCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Name", StringCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Type", StringCell.TYPE).createSpec(),
            new DataColumnSpecCreator("ProjectKey", StringCell.TYPE).createSpec());
        final var con = exec.createDataContainer(spec);
    final boolean hasConn = JiraNodePorts.hasConnection(in);
    if (hasConn) JiraNodePorts.validateConnectionSpec(JiraNodePorts.getConnectionSpec(in));
        String q = "/rest/agile/1.0/board?maxResults=200";
        if (s.m_type != null && !s.m_type.isBlank()) q += "&type=" + JiraClient.enc(s.m_type.trim());
        if (s.m_project != null && !s.m_project.isBlank()) q += "&projectKeyOrId=" + JiraClient.enc(s.m_project.trim());
        final var res = hasConn
            ? JiraClient.get((JiraConnectionPortObjectSpec)((JiraConnectionPortObject)in[0]).getSpec(), q)
            : JiraClient.get(s.m_baseUrl, s.m_auth.m_email, s.m_auth.m_token.getPassword(), q);
        if (res.statusCode() / 100 != 2) throw new RuntimeException("List boards failed: HTTP " + res.statusCode() + " body=" + res.body());
        final String b = res.body();
        int p = 0; long r = 0;
        while ((p = b.indexOf("\"id\"", p)) >= 0) {
            int i1 = b.indexOf(':', p) + 1; int i2 = b.indexOf(',', i1); if (i2 < 0) break; String idStr = b.substring(i1, i2).trim();
            int npos = b.indexOf("\"name\"", i2); int n1 = b.indexOf('"', b.indexOf(':', npos)+1)+1; int n2 = b.indexOf('"', n1); String name = (n1>0&&n2>n1)? b.substring(n1, n2):"";
            int tpos = b.indexOf("\"type\"", n2); int t1 = b.indexOf('"', b.indexOf(':', tpos)+1)+1; int t2 = b.indexOf('"', t1); String type = (t1>0&&t2>t1)? b.substring(t1, t2):"";
            int lpos = b.indexOf("\"location\"", t2); int pkp = lpos>0? b.indexOf("\"projectKey\"", lpos):-1; int pk1 = pkp>0? b.indexOf('"', b.indexOf(':', pkp)+1)+1:-1; int pk2 = pk1>0? b.indexOf('"', pk1):-1; String pkey = (pk1>0 && pk2>pk1)? b.substring(pk1, pk2):"";
            long id = 0; try { id = Long.parseLong(idStr.replaceAll("[^0-9]","")); } catch (Exception ignore) {}
            con.addRowToTable(new DefaultRow(RowKey.createRowKey(r++), new DataCell[]{ new LongCell(id), new StringCell(name), new StringCell(type), new StringCell(pkey)}));
            p = t2 + 1;
        }
        con.close(); return new BufferedDataTable[]{ con.getTable() };
    }

    @Override
    protected DataTableSpec[] configure(final PortObjectSpec[] inSpecs, final JiraListBoardsNodeSettings s) throws InvalidSettingsException {
        final var out = new DataTableSpec(
            new DataColumnSpecCreator("Id", LongCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Name", StringCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Type", StringCell.TYPE).createSpec(),
            new DataColumnSpecCreator("ProjectKey", StringCell.TYPE).createSpec());
        return new DataTableSpec[]{ out };
    }
}
