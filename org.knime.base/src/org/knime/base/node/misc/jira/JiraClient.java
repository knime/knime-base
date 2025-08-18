/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME AG, Zurich, Switzerland
 * ---------------------------------------------------------------------
 */
package org.knime.base.node.misc.jira;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.knime.base.node.misc.jira.conn.JiraConnectionPortObjectSpec;

public final class JiraClient {

    private JiraClient() {}

    public static String trimBase(final String baseUrl) {
        return baseUrl == null ? "" : baseUrl.replaceAll("/+$", "");
    }

    public static String basicAuth(final String email, final String token) {
        final String e = email == null ? "" : email;
        final String t = token == null ? "" : token;
        return "Basic " + Base64.getEncoder().encodeToString((e + ":" + t).getBytes(StandardCharsets.UTF_8));
    }

    public static HttpResponse<String> get(final String baseUrl, final String email, final String token, final String path) throws Exception {
        final var req = HttpRequest.newBuilder(URI.create(trimBase(baseUrl) + path))
            .header("Authorization", basicAuth(email, token))
            .header("Accept", "application/json")
            .GET()
            .build();
        return HttpClient.newHttpClient().send(req, HttpResponse.BodyHandlers.ofString());
    }

    public static HttpResponse<String> put(final String baseUrl, final String email, final String token, final String path, final String json) throws Exception {
        final var req = HttpRequest.newBuilder(URI.create(trimBase(baseUrl) + path))
            .header("Authorization", basicAuth(email, token))
            .header("Accept", "application/json")
            .header("Content-Type", "application/json")
            .PUT(HttpRequest.BodyPublishers.ofString(json))
            .build();
        return HttpClient.newHttpClient().send(req, HttpResponse.BodyHandlers.ofString());
    }

    public static HttpResponse<String> post(final String baseUrl, final String email, final String token, final String path, final String json) throws Exception {
        final var req = HttpRequest.newBuilder(URI.create(trimBase(baseUrl) + path))
            .header("Authorization", basicAuth(email, token))
            .header("Accept", "application/json")
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(json))
            .build();
        return HttpClient.newHttpClient().send(req, HttpResponse.BodyHandlers.ofString());
    }

    public static HttpResponse<String> delete(final String baseUrl, final String email, final String token, final String path) throws Exception {
        final var req = HttpRequest.newBuilder(URI.create(trimBase(baseUrl) + path))
            .header("Authorization", basicAuth(email, token))
            .header("Accept", "application/json")
            .DELETE()
            .build();
        return HttpClient.newHttpClient().send(req, HttpResponse.BodyHandlers.ofString());
    }

    public static HttpResponse<String> postAttachment(final String baseUrl, final String email, final String token, final String path, final String filename, final byte[] content) throws Exception {
        final String boundary = "--------------------------" + System.currentTimeMillis();
        final var sb = new StringBuilder();
        sb.append("--").append(boundary).append("\r\n");
        sb.append("Content-Disposition: form-data; name=\"file\"; filename=\"").append(filename).append("\"\r\n");
        sb.append("Content-Type: application/octet-stream\r\n\r\n");
        final byte[] pre = sb.toString().getBytes(StandardCharsets.UTF_8);
        final byte[] post = ("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8);
        final byte[] body = new byte[pre.length + content.length + post.length];
        System.arraycopy(pre, 0, body, 0, pre.length);
        System.arraycopy(content, 0, body, pre.length, content.length);
        System.arraycopy(post, 0, body, pre.length + content.length, post.length);

        final var req = HttpRequest.newBuilder(URI.create(trimBase(baseUrl) + path))
            .header("Authorization", basicAuth(email, token))
            .header("Accept", "application/json")
            .header("X-Atlassian-Token", "no-check")
            .header("Content-Type", "multipart/form-data; boundary=" + boundary)
            .POST(HttpRequest.BodyPublishers.ofByteArray(body))
            .build();
        return HttpClient.newHttpClient().send(req, HttpResponse.BodyHandlers.ofString());
    }

    public static String enc(final String v) {
        return URLEncoder.encode(v == null ? "" : v, StandardCharsets.UTF_8);
    }

    public static String escapeJson(final String in) {
        return in == null ? "" : in.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }

    public static String extract(final String body, final String prefixRegex, final String end) {
        final var m = java.util.regex.Pattern.compile(prefixRegex).matcher(body);
        if (m.find()) {
            int s = m.end();
            int e = body.indexOf(end, s);
            if (e > s) return body.substring(s, e);
        }
        return "";
    }

    public static HttpResponse<String> get(final JiraConnectionPortObjectSpec c, final String path) throws Exception {
    return get(trimBase(c.m_baseUrl), c.m_email, c.m_token, path);
    }
    public static HttpResponse<String> post(final JiraConnectionPortObjectSpec c, final String path, final String json) throws Exception {
    return post(trimBase(c.m_baseUrl), c.m_email, c.m_token, path, json);
    }
    public static HttpResponse<String> put(final JiraConnectionPortObjectSpec c, final String path, final String json) throws Exception {
    return put(trimBase(c.m_baseUrl), c.m_email, c.m_token, path, json);
    }
    public static HttpResponse<String> delete(final JiraConnectionPortObjectSpec c, final String path) throws Exception {
    return delete(trimBase(c.m_baseUrl), c.m_email, c.m_token, path);
    }
    public static HttpResponse<String> postAttachment(final JiraConnectionPortObjectSpec c, final String path, final String filename, final byte[] content) throws Exception {
    return postAttachment(trimBase(c.m_baseUrl), c.m_email, c.m_token, path, filename, content);
    }
}
