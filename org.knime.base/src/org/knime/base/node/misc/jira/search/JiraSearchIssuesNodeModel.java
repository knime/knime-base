/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME AG, Zurich, Switzerland
 * ---------------------------------------------------------------------
 */
package org.knime.base.node.misc.jira.search;

import org.knime.base.node.misc.jira.JiraClient;
import org.knime.core.data.*;
import org.knime.core.data.def.*;
import org.knime.core.data.uri.UriCellFactory;
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
        // determine selected fields (order-preserving, unique)
        final String fieldsSel = (s.m_fields == null || s.m_fields.isBlank()) ? "key,summary,status,assignee" : s.m_fields;
        final java.util.LinkedHashSet<String> selected = new java.util.LinkedHashSet<>();
        // Always include key first
        selected.add("key");
        for (String f : fieldsSel.split(",")) {
            if (f != null) {
                f = f.trim();
                if (!f.isEmpty()) selected.add(f);
            }
        }
        // Build output spec: Key + selected (without duplicate 'key') + Link
        final java.util.ArrayList<DataColumnSpec> colSpecs = new java.util.ArrayList<>();
        colSpecs.add(new DataColumnSpecCreator("Key", StringCell.TYPE).createSpec());
        for (String f : selected) {
            if ("key".equalsIgnoreCase(f)) continue;
            colSpecs.add(new DataColumnSpecCreator(titleFor(f), StringCell.TYPE).createSpec());
        }
        colSpecs.add(new DataColumnSpecCreator("Link", UriCellFactory.TYPE).createSpec());
        final var spec = new DataTableSpec(colSpecs.toArray(new DataColumnSpec[0]));
        final var con = exec.createDataContainer(spec);
    boolean hasConn = JiraNodePorts.hasConnection(in);
    if (hasConn) JiraNodePorts.validateConnectionSpec(JiraNodePorts.getConnectionSpec(in));
        final String jql = s.m_jql == null ? "" : s.m_jql;
        final int max = s.m_max <= 0 ? 50 : s.m_max;
        final String fields = fieldsSel;
        final String baseRaw = hasConn
            ? ((JiraConnectionPortObjectSpec)((JiraConnectionPortObject)in[0]).getSpec()).m_baseUrl
            : s.m_baseUrl;
        String base = JiraClient.trimBase(baseRaw);
        if (!base.isBlank() && !base.matches("^[a-zA-Z][a-zA-Z0-9+.-]*://.*$")) {
            base = "https://" + base; // ensure scheme for clickable links
        }
    final var res = hasConn
            ? JiraClient.get((JiraConnectionPortObjectSpec)((JiraConnectionPortObject)in[0]).getSpec(), "/rest/api/3/search?maxResults=" + max + "&fields=" + JiraClient.enc(fields) + "&jql=" + JiraClient.enc(jql))
            : JiraClient.get(s.m_baseUrl, s.m_auth.m_email, s.m_auth.m_token.getPassword(), "/rest/api/3/search?maxResults=" + max + "&fields=" + JiraClient.enc(fields) + "&jql=" + JiraClient.enc(jql));
        if (res.statusCode() / 100 != 2) {
            throw new RuntimeException("Jira search failed: HTTP " + res.statusCode() + " body=" + res.body());
        }
        final String body = res.body();
        long row = 0;
        int[] issuesBounds = findIssuesArray(body);
        if (issuesBounds == null) {
            // Fallback: no issues array, nothing to emit
            con.close();
            return new BufferedDataTable[]{ con.getTable() };
        }
        int cur = issuesBounds[0];
        while (true) {
            int[] obj = nextObject(body, cur, issuesBounds[1]);
            if (obj == null) break;
            int issueStart = obj[0], issueEnd = obj[1];

            // key at top-level of issue object
            String key = extractStringByKey(body, issueStart, issueEnd, "key");

            // Locate fields object within the issue
            int[] fieldsObj = findObjectByKey(body, issueStart, issueEnd, "fields");
            int fStart = fieldsObj != null ? fieldsObj[0] : issueStart;
            int fEnd = fieldsObj != null ? fieldsObj[1] : issueEnd;

            final java.util.ArrayList<DataCell> cells = new java.util.ArrayList<>();
            cells.add(new StringCell(key == null ? "" : key));
            for (String f : selected) {
                if ("key".equalsIgnoreCase(f)) continue;
                String val;
                switch (f.toLowerCase()) {
                    case "summary":
                        val = extractStringByKey(body, fStart, fEnd, "summary");
                        break;
                    case "status":
                        val = extractNestedStringByKey(body, fStart, fEnd, "status", "name");
                        break;
                    case "assignee":
                        val = extractNestedStringByKey(body, fStart, fEnd, "assignee", "displayName");
                        break;
                    case "reporter":
                        val = extractNestedStringByKey(body, fStart, fEnd, "reporter", "displayName");
                        break;
                    case "issuetype":
                        val = extractNestedStringByKey(body, fStart, fEnd, "issuetype", "name");
                        break;
                    case "priority":
                        val = extractNestedStringByKey(body, fStart, fEnd, "priority", "name");
                        break;
                    case "project":
                        val = extractNestedStringByKey(body, fStart, fEnd, "project", "key");
                        if (val.isEmpty()) val = extractNestedStringByKey(body, fStart, fEnd, "project", "name");
                        break;
                    case "labels":
                        val = extractStringArrayCsvByKey(body, fStart, fEnd, "labels");
                        break;
                    case "created":
                    case "updated":
                        val = extractStringByKey(body, fStart, fEnd, f);
                        break;
                    default:
                        val = extractStringByKey(body, fStart, fEnd, f);
                }
                cells.add(new StringCell(val == null ? "" : val));
            }

            DataCell linkCell = DataType.getMissingCell();
            if (!base.isBlank() && key != null && !key.isBlank()) {
                final String url = base + "/browse/" + key;
                try { linkCell = UriCellFactory.create(url); } catch (Exception e) { /* ignore */ }
            }
            cells.add(linkCell);

            con.addRowToTable(new DefaultRow(RowKey.createRowKey(row++), cells.toArray(new DataCell[0])));
            cur = obj[1];
        }
        con.close();
        return new BufferedDataTable[]{ con.getTable() };
    }

    @Override
    protected DataTableSpec[] configure(final PortObjectSpec[] inSpecs, final JiraSearchIssuesNodeSettings s) throws InvalidSettingsException {
        final String fieldsSel = (s.m_fields == null || s.m_fields.isBlank()) ? "key,summary,status,assignee" : s.m_fields;
        final java.util.LinkedHashSet<String> selected = new java.util.LinkedHashSet<>();
        selected.add("key");
        for (String f : fieldsSel.split(",")) {
            if (f != null) { f = f.trim(); if (!f.isEmpty()) selected.add(f); }
        }
        final java.util.ArrayList<DataColumnSpec> colSpecs = new java.util.ArrayList<>();
        colSpecs.add(new DataColumnSpecCreator("Key", StringCell.TYPE).createSpec());
        for (String f : selected) {
            if ("key".equalsIgnoreCase(f)) continue;
            colSpecs.add(new DataColumnSpecCreator(titleFor(f), StringCell.TYPE).createSpec());
        }
        colSpecs.add(new DataColumnSpecCreator("Link", UriCellFactory.TYPE).createSpec());
        final var out = new DataTableSpec(colSpecs.toArray(new DataColumnSpec[0]));
        return new DataTableSpec[]{ out };
    }

    private static String titleFor(final String field) {
        if (field == null || field.isBlank()) return "";
        // simple titleization: capitalize first letter, keep rest
        final String f = field.trim();
        return Character.toUpperCase(f.charAt(0)) + f.substring(1);
    }

    private static int[] findIssuesArray(final String body) {
        int k = body.indexOf("\"issues\"");
        if (k < 0) return null;
        int colon = body.indexOf(':', k);
        if (colon < 0) return null;
        int start = body.indexOf('[', colon);
        if (start < 0) return null;
        int end = findMatching(body, start, '[', ']');
        if (end < 0) return null;
        return new int[]{ start + 1, end }; // inside brackets
    }

    private static int[] nextObject(final String s, int from, final int limit) {
        int i = from;
        while (i < limit && s.charAt(i) != '{' && s.charAt(i) != ']') i++;
        if (i >= limit || s.charAt(i) == ']') return null;
        int end = findMatching(s, i, '{', '}');
        if (end < 0 || end > limit) return null;
        return new int[]{ i, end + 1 };
    }

    private static int[] findObjectByKey(final String s, final int start, final int end, final String key) {
        final String needle = "\"" + key + "\"";
        int p = s.indexOf(needle, start);
        if (p < 0 || p >= end) return null;
        int colon = s.indexOf(':', p);
        if (colon < 0 || colon >= end) return null;
        int brace = s.indexOf('{', colon);
        if (brace < 0 || brace >= end) return null;
        int match = findMatching(s, brace, '{', '}');
        if (match < 0 || match > end) return null;
        return new int[]{ brace, match + 1 };
    }

    private static String extractStringByKey(final String s, final int start, final int end, final String key) {
        final String needle = "\"" + key + "\"";
        int p = s.indexOf(needle, start);
        if (p < 0 || p >= end) return "";
        int colon = s.indexOf(':', p);
        if (colon < 0 || colon >= end) return "";
        int i = skipWs(s, colon + 1, end);
        if (i >= end) return "";
        char c = s.charAt(i);
        if (c == '"') {
            int e = findStringEnd(s, i);
            if (e < 0 || e > end) return "";
            return unescape(s.substring(i + 1, e));
        } else if (i + 3 < end && s.startsWith("null", i)) {
            return "";
        } else {
            // number/boolean fallback; read token
            int j = i;
            while (j < end && ",-}]\n\r\t ".indexOf(s.charAt(j)) < 0) j++;
            return s.substring(i, j);
        }
    }

    private static String extractNestedStringByKey(final String s, final int start, final int end, final String parent, final String child) {
        int[] obj = findObjectByKey(s, start, end, parent);
        if (obj == null) return "";
        return extractStringByKey(s, obj[0], obj[1], child);
    }

    private static String extractStringArrayCsvByKey(final String s, final int start, final int end, final String key) {
        final String needle = "\"" + key + "\"";
        int p = s.indexOf(needle, start);
        if (p < 0 || p >= end) return "";
        int colon = s.indexOf(':', p);
        if (colon < 0 || colon >= end) return "";
        int i = skipWs(s, colon + 1, end);
        if (i >= end || s.charAt(i) != '[') return "";
        int arrEnd = findMatching(s, i, '[', ']');
        if (arrEnd < 0 || arrEnd > end) return "";
        i++;
        final StringBuilder sb = new StringBuilder();
        boolean first = true;
        while (i < arrEnd) {
            i = skipWs(s, i, arrEnd);
            if (i >= arrEnd) break;
            if (s.charAt(i) == '"') {
                int e = findStringEnd(s, i);
                if (e < 0 || e > arrEnd) break;
                String val = unescape(s.substring(i + 1, e));
                if (!val.isEmpty()) {
                    if (!first) sb.append(',');
                    first = false;
                    sb.append(val);
                }
                i = e + 1;
            } else {
                // skip non-string element
                while (i < arrEnd && s.charAt(i) != ',' ) i++;
            }
            // skip comma
            if (i < arrEnd && s.charAt(i) == ',') i++;
        }
        return sb.toString();
    }

    private static int findMatching(final String s, int openPos, final char open, final char close) {
        int depth = 0; boolean inStr = false; boolean esc = false;
        for (int i = openPos; i < s.length(); i++) {
            char c = s.charAt(i);
            if (inStr) {
                if (esc) { esc = false; continue; }
                if (c == '\\') { esc = true; continue; }
                if (c == '"') { inStr = false; }
                continue;
            }
            if (c == '"') { inStr = true; continue; }
            if (c == open) depth++;
            else if (c == close) { depth--; if (depth == 0) return i; }
        }
        return -1;
    }

    private static int skipWs(final String s, int i, final int end) {
        while (i < end) {
            char c = s.charAt(i);
            if (c == ' ' || c == '\n' || c == '\r' || c == '\t') i++; else break;
        }
        return i;
    }

    private static int findStringEnd(final String s, int quotePos) {
        boolean esc = false;
        for (int i = quotePos + 1; i < s.length(); i++) {
            char c = s.charAt(i);
            if (esc) { esc = false; continue; }
            if (c == '\\') { esc = true; continue; }
            if (c == '"') return i;
        }
        return -1;
    }

    private static String unescape(final String s) {
        // Minimal unescape for quotes and backslashes
        return s.replace("\\\"", "\"").replace("\\\\", "\\");
    }
}
