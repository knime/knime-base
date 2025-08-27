/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME AG, Zurich, Switzerland
 * ---------------------------------------------------------------------
 */
package org.knime.base.node.misc.jira.update;

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

@SuppressWarnings({"restriction", "deprecation"})
final class JiraUpdateIssueNodeModel extends WebUINodeModel<JiraUpdateIssueNodeSettings> {

    JiraUpdateIssueNodeModel(final WebUINodeConfiguration c, final Class<JiraUpdateIssueNodeSettings> s) { super(c, s); }

    JiraUpdateIssueNodeModel(final org.knime.core.node.port.PortType[] in, final org.knime.core.node.port.PortType[] out,
            final Class<JiraUpdateIssueNodeSettings> s) { super(in, out, s); }

    @Override
    protected BufferedDataTable[] execute(final PortObject[] in, final ExecutionContext exec, final JiraUpdateIssueNodeSettings s) throws Exception {
        final var outSpec = new DataTableSpec(
            new DataColumnSpecCreator("Key", StringCell.TYPE).createSpec(),
            new DataColumnSpecCreator("URL", StringCell.TYPE).createSpec());
        final var con = exec.createDataContainer(outSpec);

        final boolean hasConn = JiraNodePorts.hasConnection(in);
        if (hasConn) JiraNodePorts.validateConnectionSpec(JiraNodePorts.getConnectionSpec(in));

        // If an Issue Table (port 1) and a key column are provided, batch update per row; else single-key mode
        final BufferedDataTable issueTable = (in != null && in.length > 1 && in[1] instanceof BufferedDataTable)
                ? (BufferedDataTable) in[1] : null;
        final boolean batchMode = issueTable != null && s.m_keyColumn != null && !s.m_keyColumn.isBlank();

        if (batchMode) {
            final var tableSpec = issueTable.getDataTableSpec();
            final int keyColIdx = tableSpec.findColumnIndex(s.m_keyColumn);
            if (keyColIdx < 0) {
                throw new InvalidSettingsException("Selected Issue Key column not found: " + s.m_keyColumn);
            }
            long rowIdx = 0;
            for (final var row : issueTable) {
                exec.checkCanceled();
                exec.setProgress(rowIdx / (double)Math.max(1, issueTable.size()), () -> "Updating issue " + rowIdx);
                final DataCell kc = row.getCell(keyColIdx);
                if (kc.isMissing()) { rowIdx++; continue; }
                final String key = ((StringValue)kc).getStringValue();
                updateOneIssue(in, s, hasConn, key);
                final String base = hasConn
                    ? ((org.knime.base.node.misc.jira.conn.JiraConnectionPortObjectSpec)((JiraConnectionPortObject)in[0]).getSpec()).m_baseUrl
                    : s.m_baseUrl;
                final String browse = JiraClient.trimBase(base) + "/browse/" + key;
                con.addRowToTable(new DefaultRow(RowKey.createRowKey(rowIdx), new DataCell[]{ new StringCell(key), new StringCell(browse)}));
                rowIdx++;
            }
        } else {
            final String key = s.m_key == null ? "" : s.m_key;
            if (key.isBlank()) {
                throw new InvalidSettingsException("No Issue Key provided. Either enter a key or connect a table and select a key column.");
            }
            updateOneIssue(in, s, hasConn, key);
            final String base = hasConn
                ? ((org.knime.base.node.misc.jira.conn.JiraConnectionPortObjectSpec)((JiraConnectionPortObject)in[0]).getSpec()).m_baseUrl
                : s.m_baseUrl;
            final String browse = JiraClient.trimBase(base) + "/browse/" + key;
            con.addRowToTable(new DefaultRow(RowKey.createRowKey(0), new DataCell[]{ new StringCell(key), new StringCell(browse)}));
        }

        con.close();
        return new BufferedDataTable[]{ con.getTable() };
    }

    private static void updateOneIssue(final PortObject[] in, final JiraUpdateIssueNodeSettings s, final boolean hasConn, final String key) throws Exception {
        final StringBuilder sb = new StringBuilder();
        sb.append("{\"fields\":{");
        boolean first = true;
        if (s.m_summary != null && !s.m_summary.isBlank()) { if(!first) sb.append(','); first=false; sb.append("\"summary\":\"").append(JiraClient.escapeJson(s.m_summary)).append("\""); }
        if (s.m_description != null && !s.m_description.isBlank()) {
            if(!first) sb.append(','); first=false;
            final String adf = "{\"type\":\"doc\",\"version\":1,\"content\":[{\"type\":\"paragraph\",\"content\":[{\"type\":\"text\",\"text\":\"" + JiraClient.escapeJson(s.m_description) + "\"}]}]}";
            sb.append("\"description\":").append(adf);
        }
        if (s.m_priority != null && !s.m_priority.isBlank()) { if(!first) sb.append(','); first=false; sb.append("\"priority\":{\"name\":\"").append(JiraClient.escapeJson(s.m_priority)).append("\"}"); }
        if (s.m_assignee != null && !s.m_assignee.isBlank()) { if(!first) sb.append(','); first=false; sb.append("\"assignee\":{\"id\":\"").append(JiraClient.escapeJson(s.m_assignee)).append("\"}"); }
        if (s.m_labels != null && !s.m_labels.isBlank()) { if(!first) sb.append(','); first=false; sb.append("\"labels\":["); boolean f=true; for(String l: s.m_labels.split(",")){ l=l.trim(); if(l.isEmpty()) continue; if(!f) sb.append(','); f=false; sb.append("\"").append(JiraClient.escapeJson(l)).append("\""); } sb.append("]"); }
        if (s.m_components != null && !s.m_components.isBlank()) { if(!first) sb.append(','); first=false; sb.append("\"components\":["); boolean f=true; for(String c: s.m_components.split(",")){ c=c.trim(); if(c.isEmpty()) continue; if(!f) sb.append(','); f=false; sb.append("{\"name\":\"").append(JiraClient.escapeJson(c)).append("\"}"); } sb.append("]"); }
        sb.append("}}");

        final var res = hasConn
            ? JiraClient.put((org.knime.base.node.misc.jira.conn.JiraConnectionPortObjectSpec)((JiraConnectionPortObject)in[0]).getSpec(), "/rest/api/3/issue/" + JiraClient.enc(key), sb.toString())
            : JiraClient.put(s.m_baseUrl, s.m_auth.m_email, s.m_auth.m_token.getPassword(), "/rest/api/3/issue/" + JiraClient.enc(key), sb.toString());
        if (res.statusCode() / 100 != 2) {
            throw new RuntimeException("Jira update failed for " + key + ": HTTP " + res.statusCode() + " body=" + res.body());
        }
    }

    @Override
    protected DataTableSpec[] configure(final PortObjectSpec[] inSpecs, final JiraUpdateIssueNodeSettings s) throws InvalidSettingsException {
        final var out = new DataTableSpec(
            new DataColumnSpecCreator("Key", StringCell.TYPE).createSpec(),
            new DataColumnSpecCreator("URL", StringCell.TYPE).createSpec());
        return new DataTableSpec[]{ out };
    }
}
