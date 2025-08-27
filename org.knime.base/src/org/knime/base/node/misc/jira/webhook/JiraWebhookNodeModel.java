package org.knime.base.node.misc.jira.webhook;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowKey;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.webui.node.impl.WebUINodeConfiguration;
import org.knime.core.webui.node.impl.WebUINodeModel;
import org.knime.base.node.misc.jira.JiraClient;
import org.knime.base.node.misc.jira.conn.JiraConnectionPortObject;
import org.knime.base.node.misc.jira.conn.JiraConnectionPortObjectSpec;

@SuppressWarnings({"restriction", "deprecation"})
final class JiraWebhookNodeModel extends WebUINodeModel<JiraWebhookNodeSettings> {

    JiraWebhookNodeModel(final WebUINodeConfiguration c, final Class<JiraWebhookNodeSettings> s) { super(c, s); }

    JiraWebhookNodeModel(final org.knime.core.node.port.PortType[] in, final org.knime.core.node.port.PortType[] out,
            final Class<JiraWebhookNodeSettings> s) { super(in, out, s); }

    @Override
    protected BufferedDataTable[] execute(final PortObject[] in, final ExecutionContext exec, final JiraWebhookNodeSettings s) throws Exception {
        final var outSpec = new DataTableSpec(
            new DataColumnSpecCreator("Webhook URL", StringCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Method", StringCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Path", StringCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Query", StringCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Headers", StringCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Body", StringCell.TYPE).createSpec()
        );
        final var container = exec.createDataContainer(outSpec);

        final var queue = new ArrayBlockingQueue<WebhookData>(1);

    final HttpServer server = HttpServer.create(new InetSocketAddress(s.m_host, s.m_port), 0);
        final String path = normalizePath(s.m_path);
    final ExecutorService executor = Executors.newSingleThreadExecutor();
        server.createContext(path, new Handler(s.m_secret, s.m_responseBody, queue));
        server.setExecutor(executor);
    server.start();
        final int actualPort = server.getAddress().getPort();

        // Build internal URL (listener bind) and a public URL (for Jira registration)
    final String internalUrl = "http://" + (s.m_host.equals("0.0.0.0") ? "localhost" : s.m_host) + ":" + actualPort + path;
    final String autoHost = (s.m_autoHost) ? detectHost() : null;
    final String autoUrl = (autoHost == null || autoHost.isBlank()) ? null : ("http://" + autoHost + ":" + actualPort + path);
    String urlForJira = (s.m_publicUrl != null && !s.m_publicUrl.isBlank()) ? s.m_publicUrl : (autoUrl != null ? autoUrl : internalUrl);
    urlForJira = ensureScheme(urlForJira, "http");
        final String url = internalUrl;
    setWarningMessage("Listening for Jira webhook at " + url + (autoUrl != null && !autoUrl.equals(url) ? " (public: " + autoUrl + ")" : ""));

        // Optional: register webhook in Jira via REST
    String registeredWebhookId = null;
        EndpointVersion registeredEndpointVersion = null;
        JiraConnectionPortObjectSpec connSpec = null;
        final boolean hasConn = (in != null && in.length > 0 && in[0] instanceof JiraConnectionPortObject);
        if (hasConn) {
            connSpec = (JiraConnectionPortObjectSpec)((JiraConnectionPortObject)in[0]).getSpec();
        }
        if (s.m_register) {
            try {
                final String base = normalizeBaseUrl(connSpec != null ? connSpec.m_baseUrl : s.m_baseUrl);
                if (base == null || base.isBlank()) {
                    throw new InvalidSettingsException("Base URL is required for webhook registration. Provide a Jira Connection input or set the Base URL (including scheme, e.g., https://your-domain.atlassian.net)");
                }
                final String email = connSpec != null ? connSpec.m_email : s.m_auth.m_email;
                final String token = connSpec != null ? connSpec.m_token : s.m_auth.m_token.getPassword();
                // Try Jira 10+ endpoint first
                var res = JiraClient.post(base, email, token, "/rest/jira-webhook/1.0/webhooks", buildRegistrationPayloadV10(urlForJira, s));
                if (res.statusCode() / 100 == 2) {
                    registeredWebhookId = tryExtractId(res.body());
                    registeredEndpointVersion = EndpointVersion.V10;
                } else {
                    // Fallback to Jira 9.x legacy endpoint
                    res = JiraClient.post(base, email, token, "/rest/webhooks/1.0/webhook", buildRegistrationPayloadV9(urlForJira, s));
                    if (res.statusCode() / 100 != 2) {
                        throw new RuntimeException("Webhook registration failed: HTTP " + res.statusCode() + " body=" + res.body());
                    }
                    registeredWebhookId = tryExtractId(res.body());
                    registeredEndpointVersion = EndpointVersion.V9;
                }
            } catch (Exception e) {
                // Ensure cleanup on registration failure
                try { server.stop(0); } catch (Exception ignore) {}
                try { executor.shutdownNow(); } catch (Exception ignore) {}
                // Try to unregister if we partially registered
                if (s.m_unregister && registeredWebhookId != null) {
                    try { unregister(connSpec, s, registeredWebhookId, registeredEndpointVersion); } catch (Exception ignore) {}
                }
                throw e;
            }
        }

        // Wait for a single request or timeout
        WebhookData data = null;
        try {
            data = queue.poll(s.m_timeoutSeconds, TimeUnit.SECONDS);
            if (data == null) {
                throw new RuntimeException("Timed out waiting for webhook (" + s.m_timeoutSeconds + "s)");
            }
        } finally {
            try { server.stop(0); } catch (Exception ignore) {}
            try { executor.shutdownNow(); } catch (Exception ignore) {}
            try { executor.awaitTermination(2, TimeUnit.SECONDS); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
            if (s.m_unregister && registeredWebhookId != null) {
                try { unregister(connSpec, s, registeredWebhookId, registeredEndpointVersion); } catch (Exception ignore) {}
            }
        }

        container.addRowToTable(new DefaultRow(RowKey.createRowKey(0), new DataCell[]{
            new StringCell(url), new StringCell(data.method), new StringCell(data.path), new StringCell(data.query == null ? "" : data.query),
            new StringCell(data.headers), new StringCell(data.body)
        }));
        container.close();

        return new BufferedDataTable[]{ container.getTable() };
    }

    @Override
    protected DataTableSpec[] configure(final PortObjectSpec[] inSpecs, final JiraWebhookNodeSettings s) throws InvalidSettingsException {
        final var spec = new DataTableSpec(
            new DataColumnSpecCreator("Webhook URL", StringCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Method", StringCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Path", StringCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Query", StringCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Headers", StringCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Body", StringCell.TYPE).createSpec()
        );
        return new DataTableSpec[]{ spec };
    }

    private static final class Handler implements HttpHandler {
        private final String m_secret;
        private final String m_response;
        private final ArrayBlockingQueue<WebhookData> m_queue;
        Handler(final String secret, final String response, final ArrayBlockingQueue<WebhookData> q) {
            m_secret = secret == null ? "" : secret; m_response = response == null ? "OK" : response; m_queue = q;
        }
        @Override public void handle(final HttpExchange exchange) throws IOException {
            try {
                if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
                    respond(exchange, 405, "Method Not Allowed");
                    return;
                }
                if (!m_secret.isEmpty()) {
                    final var hdr = exchange.getRequestHeaders().getFirst("X-Webhook-Token");
                    final var query = exchange.getRequestURI().getRawQuery();
                    final boolean ok = m_secret.equals(hdr) || (query != null && query.contains("token=" + m_secret));
                    if (!ok) { respond(exchange, 401, "Unauthorized"); return; }
                }
                // Capture request details before responding
                final String method = exchange.getRequestMethod();
                final URI uri = exchange.getRequestURI();
                final String reqPath = uri.getPath();
                final String query = uri.getRawQuery();
                final String headers = toFlatHeaderString(exchange.getRequestHeaders());
                final String body;
                try (var in = exchange.getRequestBody()) {
                    body = new String(in.readAllBytes(), StandardCharsets.UTF_8);
                }

                respond(exchange, 200, m_response);
                m_queue.offer(new WebhookData(method, reqPath, query, headers, body));
            } catch (Exception e) {
                respond(exchange, 500, "Internal Server Error");
            }
        }
        private static void respond(final HttpExchange ex, final int code, final String body) throws IOException {
            final byte[] data = body.getBytes(StandardCharsets.UTF_8);
            ex.sendResponseHeaders(code, data.length);
            try (var os = ex.getResponseBody()) { os.write(data); }
        }
    }

    private static String normalizePath(final String p) {
        if (p == null || p.isBlank()) return "/jira/webhook";
        return p.startsWith("/") ? p : "/" + p;
    }

    private static String toFlatHeaderString(final com.sun.net.httpserver.Headers headers) {
        final StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, java.util.List<String>> e : headers.entrySet()) {
            if (!first) sb.append(", ");
            first = false;
            sb.append(e.getKey()).append("=").append(e.getValue());
        }
        return sb.toString();
    }

    private static String detectHost() {
        try {
            // 1) Try the primary hostname/IP
            final InetAddress local = InetAddress.getLocalHost();
            if (local != null && !local.isLoopbackAddress() && !local.isLinkLocalAddress()) {
                final String addr = local.getHostAddress();
                if (addr != null && !addr.isBlank()) { return addr; }
                final String name = local.getHostName();
                if (name != null && !name.isBlank()) { return name; }
            }
        } catch (Exception ignore) {}
        try {
            // 2) Walk network interfaces, prefer first non-loopback IPv4
            final java.util.Enumeration<NetworkInterface> ifaces = NetworkInterface.getNetworkInterfaces();
            while (ifaces != null && ifaces.hasMoreElements()) {
                final NetworkInterface ni = ifaces.nextElement();
                try {
                    if (!ni.isUp() || ni.isLoopback() || ni.isVirtual()) { continue; }
                } catch (Exception e) { continue; }
                final java.util.Enumeration<java.net.InetAddress> addrs = ni.getInetAddresses();
                while (addrs.hasMoreElements()) {
                    final java.net.InetAddress ia = addrs.nextElement();
                    if (ia.isLoopbackAddress() || ia.isLinkLocalAddress()) { continue; }
                    if (ia instanceof java.net.Inet4Address) { return ia.getHostAddress(); }
                }
            }
        } catch (Exception ignore) {}
        // 3) Fallback
        return "localhost";
    }

    private static String normalizeBaseUrl(final String baseRaw) {
        if (baseRaw == null) return null;
        String b = baseRaw.trim();
        if (b.isEmpty()) return b;
        if (!hasScheme(b)) { b = "https://" + b; }
        // remove trailing slash
        if (b.endsWith("/")) { b = b.substring(0, b.length() - 1); }
        return b;
    }

    private static boolean hasScheme(final String url) {
        return url != null && url.matches("^[a-zA-Z][a-zA-Z0-9+.-]*://.*");
    }

    private static String ensureScheme(final String url, final String defaultScheme) {
        if (url == null || url.isBlank()) return url;
        if (hasScheme(url)) return url;
        return (defaultScheme == null || defaultScheme.isBlank() ? "http" : defaultScheme) + "://" + url;
    }

    private static final class WebhookData {
        final String method; final String path; final String query; final String headers; final String body;
        WebhookData(final String m, final String p, final String q, final String h, final String b) { method = m; path = p; query = q; headers = h; body = b; }
    }

    private static String buildRegistrationPayloadV9(final String callbackUrl, final JiraWebhookNodeSettings s) {
        // Jira 9.x style payload
        final String[] ev = (s.m_events == null || s.m_events.isBlank()) ? new String[]{"jira:issue_updated"} : java.util.Arrays.stream(s.m_events.split(","))
            .map(String::trim).filter(x -> !x.isEmpty()).toArray(String[]::new);
        final StringBuilder sb = new StringBuilder();
        sb.append('{');
        sb.append("\"name\":\"").append(JiraClient.escapeJson(s.m_name)).append('\"');
        sb.append(",\"url\":\"").append(JiraClient.escapeJson(callbackUrl)).append('\"');
        sb.append(",\"events\":[");
        for (int i=0;i<ev.length;i++) { if (i>0) sb.append(','); sb.append('\"').append(JiraClient.escapeJson(ev[i])).append('\"'); }
        sb.append(']');
        // Prefer Jira 9 style fields
        if (s.m_jql != null && !s.m_jql.isBlank()) {
            sb.append(",\"filters\":{\"issue-related-events-section\":\"").append(JiraClient.escapeJson(s.m_jql)).append("\"}");
        }
        sb.append(",\"excludeBody\": ").append(s.m_excludeBody ? "true" : "false");
        if (s.m_description != null && !s.m_description.isBlank()) { sb.append(",\"description\":\"").append(JiraClient.escapeJson(s.m_description)).append('\"'); }
        sb.append('}');
        return sb.toString();
    }

    private static String buildRegistrationPayloadV10(final String callbackUrl, final JiraWebhookNodeSettings s) {
        // Jira 10.x style payload with uppercase configuration keys
        final String[] ev = (s.m_events == null || s.m_events.isBlank()) ? new String[]{"jira:issue_updated"} : java.util.Arrays.stream(s.m_events.split(","))
            .map(String::trim).filter(x -> !x.isEmpty()).toArray(String[]::new);
        final StringBuilder sb = new StringBuilder();
        sb.append('{');
        sb.append("\"name\":\"").append(JiraClient.escapeJson(s.m_name)).append('\"');
        sb.append(",\"url\":\"").append(JiraClient.escapeJson(callbackUrl)).append('\"');
        sb.append(",\"events\":[");
        for (int i=0;i<ev.length;i++) { if (i>0) sb.append(','); sb.append('\"').append(JiraClient.escapeJson(ev[i])).append('\"'); }
        sb.append(']');
        sb.append(",\"configuration\":{");
        boolean first = true;
        if (s.m_jql != null && !s.m_jql.isBlank()) { sb.append("\"FILTERS\":\"").append(JiraClient.escapeJson(s.m_jql)).append('\"'); first = false; }
        sb.append(first?"":" ,"); sb.append("\"EXCLUDE_BODY\":\"").append(s.m_excludeBody ? "true" : "false").append('\"'); first = false;
        if (s.m_description != null && !s.m_description.isBlank()) { sb.append(",\"DESCRIPTION\":\"").append(JiraClient.escapeJson(s.m_description)).append('\"'); }
        sb.append(",\"ACTIVE\":\"").append(s.m_active ? "true" : "false").append('\"');
        sb.append('}');
        sb.append('}');
        return sb.toString();
    }

    private static String tryExtractId(final String body) {
        String id = JiraClient.extract(body, "\\\"id\\\"\\s*:\\s*\\\"?", "\\\"|,|");
        if (id == null || id.isBlank()) { id = JiraClient.extract(body, "\\\"webhookId\\\"\\s*:\\s*\\\"?", "\\\"|,|"); }
        return id == null ? "" : id.replaceAll("[^0-9]", "");
    }

    private static void unregister(final JiraConnectionPortObjectSpec connSpec, final JiraWebhookNodeSettings s, final String id, final EndpointVersion version) throws Exception {
        final String base = connSpec != null ? connSpec.m_baseUrl : s.m_baseUrl;
        final String email = connSpec != null ? connSpec.m_email : s.m_auth.m_email;
        final String token = connSpec != null ? connSpec.m_token : s.m_auth.m_token.getPassword();
        final String path = (version == EndpointVersion.V10) ? "/rest/jira-webhook/1.0/webhooks/" + id : "/rest/webhooks/1.0/webhook/" + id;
        JiraClient.delete(base, email, token, path);
    }

    private static enum EndpointVersion { V9, V10 }
}
