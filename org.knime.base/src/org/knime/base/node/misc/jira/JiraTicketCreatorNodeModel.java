/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME AG, Zurich, Switzerland
 * ---------------------------------------------------------------------
 */
package org.knime.base.node.misc.jira;

import java.io.IOException;
import java.net.http.HttpResponse;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowKey;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.webui.node.impl.WebUINodeConfiguration;
import org.knime.core.webui.node.impl.WebUINodeModel;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.base.node.misc.jira.conn.JiraConnectionPortObject;
import org.knime.base.node.misc.jira.conn.JiraConnectionPortObjectSpec;

final class JiraTicketCreatorNodeModel extends WebUINodeModel<JiraTicketCreatorNodeSettings> {

     private static final NodeLogger LOGGER = NodeLogger.getLogger(JiraTicketCreatorNodeModel.class);

     JiraTicketCreatorNodeModel(final WebUINodeConfiguration config,
            final Class<JiraTicketCreatorNodeSettings> modelSettingsClass) {
        super(config, modelSettingsClass);
    }

    public JiraTicketCreatorNodeModel(final org.knime.core.node.port.PortType[] inputPortTypes,
          final org.knime.core.node.port.PortType[] outputPortTypes,
          final Class<JiraTicketCreatorNodeSettings> modelSettingsClass) {
        super(inputPortTypes, outputPortTypes, modelSettingsClass);
    }

     @Override
    protected BufferedDataTable[] execute(final PortObject[] inData, final ExecutionContext exec,
             final JiraTicketCreatorNodeSettings s) throws Exception {
         final var spec = new DataTableSpec(
                 new DataColumnSpecCreator("Key", StringCell.TYPE).createSpec(),
                 new DataColumnSpecCreator("URL", StringCell.TYPE).createSpec());
         final BufferedDataContainer container = exec.createDataContainer(spec);

         if (inData == null || inData.length == 0 || !(inData[0] instanceof JiraConnectionPortObject)) {
             throw new InvalidSettingsException("Jira Connection not provided. Connect the Jira Connector or supply credentials in node settings.");
         }
         final JiraConnectionPortObjectSpec conn = (JiraConnectionPortObjectSpec)((JiraConnectionPortObject)inData[0]).getSpec();
         final String project = s.m_project == null ? "" : s.m_project;
         final String type = s.m_issueType == null || s.m_issueType.isBlank() ? "Task" : s.m_issueType;
         final String summary = s.m_summary == null ? "" : s.m_summary;
         final String description = s.m_description == null ? "" : s.m_description;
         // Use ManualFilter.m_manuallySelected for selected values
         final String labels = s.m_labels == null ? "" : s.m_labels;
         final String assignee = s.m_assignee == null ? "" : s.m_assignee;
         final String priority = s.m_priority == null ? "" : s.m_priority;
         final String components = s.m_components == null ? "" : s.m_components;

         final String issueKey;
         final String issueUrl;
         try {
             final String json = buildPayload(project, type, summary, description, labels, assignee, priority, components);
             final var res = callJira(conn, json);
             if (res.statusCode() / 100 != 2) {
                 throw new IOException("Jira create failed: HTTP " + res.statusCode() + " body=" + res.body());
             }
             String keyTmp = extract(res.body(), "\\\"key\\\"\\s*:\\s*\\\"", "\\\"");
             String selfTmp = extract(res.body(), "\\\"self\\\"\\s*:\\s*\\\"", "\\\"");
             if (keyTmp.isBlank()) {
                 // naive fallback
                 final String b = res.body();
                 int k = b.indexOf("\"key\"");
                 if (k >= 0) {
                     int ks = b.indexOf('"', b.indexOf(':', k) + 1) + 1;
                     int ke = ks > 0 ? b.indexOf('"', ks) : -1;
                     if (ke > ks) keyTmp = b.substring(ks, ke);
                 }
             }
             if (selfTmp.isBlank()) {
                 final String b = res.body();
                 int sIdx = b.indexOf("\"self\"");
                 if (sIdx >= 0) {
                     int ss = b.indexOf('"', b.indexOf(':', sIdx) + 1) + 1;
                     int se = ss > 0 ? b.indexOf('"', ss) : -1;
                     if (se > ss) selfTmp = b.substring(ss, se);
                 }
             }
             issueKey = keyTmp;
             issueUrl = deriveBrowseUrl(conn.m_baseUrl, issueKey, selfTmp);
         } catch (Exception e) {
             LOGGER.error("Failed to create Jira issue", e);
             throw e;
         }

         DataCell[] cells = new DataCell[]{ new StringCell(issueKey), new StringCell(issueUrl) };
         container.addRowToTable(new DefaultRow(RowKey.createRowKey(0), cells));
         container.close();
         return new BufferedDataTable[]{ container.getTable() };
     }

     @Override
    protected DataTableSpec[] configure(final PortObjectSpec[] inSpecs, final JiraTicketCreatorNodeSettings s)
             throws InvalidSettingsException {
         final var outSpec = new DataTableSpec(
                 new DataColumnSpecCreator("Key", StringCell.TYPE).createSpec(),
                 new DataColumnSpecCreator("URL", StringCell.TYPE).createSpec());
         return new DataTableSpec[]{ outSpec };
     }

     private static String buildPayload(final String project, final String type, final String summary,
             final String description, final String labels, final String assignee, final String priority,
             final String components) {
         final String adf = "{\"type\":\"doc\",\"version\":1,\"content\":[{\"type\":\"paragraph\",\"content\":[{\"type\":\"text\",\"text\":\""
                 + escapeJson(description) + "\"}]}]}";
         final StringBuilder sb = new StringBuilder();
         sb.append("{\"fields\":{");
         sb.append("\"summary\":\"").append(escapeJson(summary)).append("\",");
         sb.append("\"description\":").append(adf).append(",");
         sb.append("\"project\":{\"key\":\"").append(escapeJson(project)).append("\"},");
         sb.append("\"issuetype\":{\"name\":\"").append(escapeJson(type)).append("\"}");
         if (!labels.isBlank()) {
             sb.append(",\"labels\":[");
             boolean first=true; for (String l : labels.split(",")) { l = l.trim(); if (l.isEmpty()) continue; if(!first) sb.append(','); first=false; sb.append("\"").append(escapeJson(l)).append("\""); }
             sb.append("]");
         }
         if (!assignee.isBlank()) {
             sb.append(",\"assignee\":{\"id\":\"").append(escapeJson(assignee)).append("\"}");
         }
         if (!priority.isBlank()) {
             sb.append(",\"priority\":{\"name\":\"").append(escapeJson(priority)).append("\"}");
         }
         if (!components.isBlank()) {
             sb.append(",\"components\":[");
             boolean first=true; for (String c : components.split(",")) { c = c.trim(); if (c.isEmpty()) continue; if(!first) sb.append(','); first=false; sb.append("{\"name\":\"").append(escapeJson(c)).append("\"}"); }
             sb.append("]");
         }
         sb.append("}}\n");
         return sb.toString();
     }

     private static HttpResponse<String> callJira(final JiraConnectionPortObjectSpec conn, final String payload)
             throws Exception {
         return JiraClient.post(conn, "/rest/api/3/issue", payload);
     }

     private static String escapeJson(final String in) {
         return in == null ? "" : in.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
     }

     private static String extract(final String body, final String prefixRegex, final String end) {
         final var m = java.util.regex.Pattern.compile(prefixRegex).matcher(body);
         if (m.find()) {
             int s = m.end();
             int e = body.indexOf(end, s);
             if (e > s) {
                 return body.substring(s, e);
             }
         }
         return "";
     }

    private static String deriveBrowseUrl(final String baseUrl, final String key, final String self) {
         if (key != null && !key.isBlank()) {
             return baseUrl.replaceAll("/+$", "") + "/browse/" + key;
         }
         if (self != null && !self.isBlank()) {
             return self;
         }
         return "";
     }
 }
