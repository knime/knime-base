/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ---------------------------------------------------------------------
 */
package org.knime.base.node.misc.jira;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.WidgetGroup;
import org.knime.node.parameters.layout.After;
import org.knime.node.parameters.layout.Layout;
import org.knime.node.parameters.layout.Section;
import org.knime.node.parameters.persistence.Persist;
import org.knime.node.parameters.persistence.Persistable;
import org.knime.node.parameters.updates.StateProvider;
import org.knime.node.parameters.updates.Effect;
import org.knime.node.parameters.updates.Effect.EffectType;
import org.knime.node.parameters.updates.ValueReference;
import org.knime.node.parameters.widget.choices.ChoicesProvider;
import org.knime.node.parameters.widget.choices.StringChoice;
import org.knime.node.parameters.widget.choices.StringChoicesProvider;
import org.knime.node.parameters.widget.credentials.Credentials;
import org.knime.node.parameters.widget.credentials.CredentialsWidget;
import org.knime.node.parameters.widget.message.TextMessage;
import org.knime.node.parameters.widget.message.TextMessage.MessageType;
import org.knime.node.parameters.widget.text.TextAreaWidget;
import org.knime.node.parameters.widget.text.TextInputWidget;
import org.knime.base.node.misc.jira.conn.JiraConnectionPortObjectSpec;

@SuppressWarnings("restriction")
public final class JiraTicketCreatorNodeSettings implements NodeParameters {

    @Section(title = "Jira Server")
    interface ServerSection {}

    @Section(title = "Issue Details")
    @After(ServerSection.class)
    interface IssueSection {}

    @Section(title = "Advanced")
    @After(IssueSection.class)
    interface AdvancedSection {}

    static final class AuthGroup implements WidgetGroup, Persistable {
        @Widget(title = "E‑mail / Username", description = "Jira account e‑mail (Cloud) or username (Server)")
        @TextInputWidget(placeholder = "me@example.com")
        @Persist(configKey = "auth/email")
        @ValueReference(EmailRef.class)
        public String m_email = "";

        @Widget(title = "API Token / Password", description = "API token (Cloud) or password (Server)")
        @CredentialsWidget
        @Persist(configKey = "auth/secret")
        @ValueReference(TokenRef.class)
        public Credentials m_token = new Credentials();
    }

    @Widget(title = "Base URL", description = "Jira site URL, e.g. https://your-domain.atlassian.net")
    @Effect(predicate = org.knime.base.node.misc.jira.shared.JiraUI.JiraConnectionPresent.class, type = EffectType.HIDE)
    @TextInputWidget(placeholder = "https://example.atlassian.net")
    @Layout(ServerSection.class)
    @Persist(configKey = "server/url")
    @ValueReference(BaseUrlRef.class)
    public String m_baseUrl = "";

    @Widget(title = "Credentials", description = "Credentials used for Jira REST authentication")
    @Effect(predicate = org.knime.base.node.misc.jira.shared.JiraUI.JiraConnectionPresent.class, type = EffectType.HIDE)
    @Layout(ServerSection.class)
    @Persist(configKey = "server/credentials")
    public AuthGroup m_auth = new AuthGroup();

    // Info banner: when a Jira Connection port is connected, show that credentials are taken from the port
    static final class JiraConnectionInfo implements TextMessage.SimpleTextMessageProvider {
        @Override
        public boolean showMessage(final NodeParametersInput context) {
            if (context == null || context.getInPortSpecs() == null || context.getInPortSpecs().length == 0) {
                return false;
            }
            // Show if a Jira connection spec is present on the first input port
            return context.getInPortSpecs()[0] instanceof JiraConnectionPortObjectSpec;
        }
        @Override
        public String title() { return "Jira connection managed by input port"; }
        @Override
        public String description() {
            return "A Jira Connection is connected. Base URL and Credentials are taken from the connection and "
                + "are used to populate the dropdowns (Projects, Issue types, Users, Priorities, Components).";
        }
        @Override
        public MessageType type() { return MessageType.INFO; }
    }

    @TextMessage(value = org.knime.base.node.misc.jira.shared.JiraUI.JiraConnectionInfo.class)
    @Layout(ServerSection.class)
    Void m_connectionInfo;

    @Widget(title = "Project", description = "Choose the target project")
    @Layout(IssueSection.class)
    @Persist(configKey = "issue/project")
    @ValueReference(ProjectRef.class)
    @ChoicesProvider(ProjectsProvider.class)
    public String m_project = "";

    @Widget(title = "Issue Type", description = "Choose the issue type available for the selected project")
    @Layout(IssueSection.class)
    @Persist(configKey = "issue/type")
    @ChoicesProvider(IssueTypesProvider.class)
    public String m_issueType = "Task";

    @Widget(title = "Summary", description = "Issue summary / title")
    @TextInputWidget
    @Layout(IssueSection.class)
    @Persist(configKey = "issue/summary")
    public String m_summary = "";

    @Widget(title = "Description", description = "Issue description (Markdown supported)")
    @TextAreaWidget
    @Layout(IssueSection.class)
    @Persist(configKey = "issue/description")
    public String m_description = "";

    @Widget(title = "Labels", description = "Comma-separated labels, e.g. bug, ui, backend")
    @Layout(AdvancedSection.class)
    @TextInputWidget(placeholder = "label1, label2")
    @Persist(configKey = "issue/labels")
    public String m_labels = "";

    @Widget(title = "Assignee", description = "Assignable user for the selected project (account id)")
    @Layout(AdvancedSection.class)
    @Persist(configKey = "issue/assignee")
    @ChoicesProvider(AssignableUsersProvider.class)
    public String m_assignee = "";

    @Widget(title = "Priority", description = "Priority name")
    @Layout(AdvancedSection.class)
    @Persist(configKey = "issue/priority")
    @ChoicesProvider(PrioritiesProvider.class)
    public String m_priority = "";

    @Widget(title = "Components", description = "Comma-separated components, e.g. API, Frontend")
    @Layout(AdvancedSection.class)
    @TextInputWidget(placeholder = "component1, component2")
    @Persist(configKey = "issue/components")
    public String m_components = "";

    /* ====================== Value References for dependencies ====================== */

    static final class BaseUrlRef implements org.knime.node.parameters.updates.ParameterReference<String> { }
    static final class EmailRef implements org.knime.node.parameters.updates.ParameterReference<String> { }
    static final class TokenRef implements org.knime.node.parameters.updates.ParameterReference<Credentials> { }
    static final class ProjectRef implements org.knime.node.parameters.updates.ParameterReference<String> { }

    /* ====================== Choices Providers (HTTP-backed) ====================== */

    static abstract class JiraChoicesProvider implements StringChoicesProvider {
        protected java.util.function.Supplier<String> m_urlSup;
        protected java.util.function.Supplier<String> m_emailSup;
        protected java.util.function.Supplier<Credentials> m_tokenSup;

        // Cache Basic auth headers per (baseUrl|email) to survive masked password on dialog reopen
        private static final ConcurrentHashMap<String, String> AUTH_CACHE = new ConcurrentHashMap<>();
        private String m_lastBase = "";
        private String m_lastEmail = "";

        @Override
        public void init(final StateProvider.StateProviderInitializer i) {
            // Keep default eager compute and add dependencies
            StringChoicesProvider.super.init(i);
            m_urlSup = i.getValueSupplier(BaseUrlRef.class);
            m_emailSup = i.getValueSupplier(EmailRef.class);
            m_tokenSup = i.getValueSupplier(TokenRef.class);
            i.computeOnValueChange(BaseUrlRef.class);
            i.computeOnValueChange(EmailRef.class);
            i.computeOnValueChange(TokenRef.class);
        }

        private static boolean isMasked(final String token) {
            if (token == null || token.isBlank()) {
                return true;
            }
            // common placeholders: only asterisks or bullet-like characters
            return token.matches("^[\\*•●]+$");
        }

        private static String key(final String base, final String email) {
            return base + "|" + email;
        }

        protected HttpRequest.Builder baseRequest(final NodeParametersInput in, final String path) {
            String base = String.valueOf(m_urlSup.get()).replaceAll("/+\\$", "");
            String email = String.valueOf(m_emailSup.get());
            final var tokenObj = m_tokenSup.get();
            String token = tokenObj != null ? tokenObj.getPassword() : "";

            // If a Jira connection port is connected, prefer its values
            try {
                if (in != null && in.getInPortSpecs() != null && in.getInPortSpecs().length > 0
                        && in.getInPortSpecs()[0] instanceof JiraConnectionPortObjectSpec c) {
                    base = c.m_baseUrl;
                    email = c.m_email;
                    token = c.m_token;
                }
            } catch (Exception ignore) { /* fall back to settings */ }
            m_lastBase = base;
            m_lastEmail = email;
            String header = null;
            if (!isMasked(token)) {
                final var auth = Base64.getEncoder().encodeToString((email + ":" + token).getBytes(StandardCharsets.UTF_8));
                header = "Basic " + auth;
                AUTH_CACHE.put(key(base, email), header);
            } else {
                header = AUTH_CACHE.get(key(base, email));
            }
            final var uri = URI.create(base + path);
            final var b = HttpRequest.newBuilder(uri)
                .header("Accept", "application/json");
            if (header != null && !header.isBlank()) {
                b.header("Authorization", header);
            }
            return b;
        }

        protected void updateAuthCacheOnResponse(final int statusCode) {
            final var tokenObj = m_tokenSup.get();
            final var token = tokenObj != null ? tokenObj.getPassword() : "";
            final var k = key(m_lastBase, m_lastEmail);
            if (statusCode == 401) {
                AUTH_CACHE.remove(k);
            } else if (statusCode / 100 == 2 && !isMasked(token)) {
                final var auth = Base64.getEncoder().encodeToString((m_lastEmail + ":" + token).getBytes(StandardCharsets.UTF_8));
                AUTH_CACHE.put(k, "Basic " + auth);
            }
        }

    protected static HttpResponse<String> send(final HttpRequest req) throws Exception {
            return HttpClient.newHttpClient().send(req, HttpResponse.BodyHandlers.ofString());
        }
    }

    /** Lists projects (id = key, text = key — name). */
    static final class ProjectsProvider extends JiraChoicesProvider {
        @Override
    public List<StringChoice> computeState(final NodeParametersInput in) {
            try {
        final var req = baseRequest(in, "/rest/api/3/project/search").GET().build();
                final var res = send(req);
                updateAuthCacheOnResponse(res.statusCode());
                if (res.statusCode() / 100 != 2) {
                    return List.of();
                }
                final String body = res.body();
                // extract project entries: key and name; parsing naive
                final var out = new ArrayList<StringChoice>();
                int idx = 0;
                while ((idx = body.indexOf("\"key\"", idx)) >= 0) {
                    int ks = body.indexOf('"', body.indexOf(':', idx) + 1) + 1;
                    int ke = body.indexOf('"', ks);
                    String key = ks > 0 && ke > ks ? body.substring(ks, ke) : "";
                    int nn = body.indexOf("\"name\"", ke);
                    int ns = nn > 0 ? body.indexOf('"', body.indexOf(':', nn) + 1) + 1 : -1;
                    int ne = ns > 0 ? body.indexOf('"', ns) : -1;
                    String name = ns > 0 && ne > ns ? body.substring(ns, ne) : key;
                    out.add(new StringChoice(key, key + " — " + name));
                    idx = ke + 1;
                }
                return out;
            } catch (Exception e) {
                return List.of();
            }
        }
    }

    /** Lists issue types for the selected project (id=text=name). */
    static final class IssueTypesProvider extends JiraChoicesProvider {
        private java.util.function.Supplier<String> m_projectSup;
        @Override public void init(final StateProvider.StateProviderInitializer i) {
            super.init(i);
            m_projectSup = i.getValueSupplier(ProjectRef.class);
            i.computeOnValueChange(ProjectRef.class);
        }
        @Override
        public List<StringChoice> computeState(final NodeParametersInput in) {
            final var proj = String.valueOf(m_projectSup.get());
            if (proj == null || proj.isBlank()) {
                return List.of();
            }
            try {
                final var path = "/rest/api/3/issue/createmeta?projectKeys=" + java.net.URLEncoder.encode(proj, java.nio.charset.StandardCharsets.UTF_8) + "&expand=projects.issuetypes";
                final var req = baseRequest(in, path).GET().build();
                final var res = send(req);
                updateAuthCacheOnResponse(res.statusCode());
                if (res.statusCode() / 100 != 2) {
                    return List.of();
                }
                final String body = res.body();
                // naive: scan for \"issuetypes\" then collect \"name\"
                final var out = new ArrayList<StringChoice>();
                int it = body.indexOf("\"issuetypes\"");
                int idx = it >= 0 ? it : 0;
                while ((idx = body.indexOf("\"name\"", idx)) >= 0) {
                    int ns = body.indexOf('"', body.indexOf(':', idx) + 1) + 1;
                    int ne = body.indexOf('"', ns);
                    if (ns <= 0 || ne <= ns) {
                        break;
                    }
                    String name = body.substring(ns, ne);
                    out.add(StringChoice.fromId(name));
                    idx = ne + 1;
                }
                return out;
            } catch (Exception e) { return List.of(); }
        }
    }

    /** Lists priorities (id=text=name). */
    static final class PrioritiesProvider extends JiraChoicesProvider {
        @Override
        public List<StringChoice> computeState(final NodeParametersInput in) {
            try {
                final var req = baseRequest(in, "/rest/api/3/priority").GET().build();
                final var res = send(req);
                updateAuthCacheOnResponse(res.statusCode());
                if (res.statusCode() / 100 != 2) {
                    return List.of();
                }
                // inline parse: collect all "name" fields as choices
                final var out = new ArrayList<StringChoice>();
                final String body = res.body();
                int idx = 0;
                while ((idx = body.indexOf("\"name\"", idx)) >= 0) {
                    int ns = body.indexOf('"', body.indexOf(':', idx) + 1) + 1;
                    int ne = body.indexOf('"', ns);
                    if (ns <= 0 || ne <= ns) break;
                    String name = body.substring(ns, ne);
                    out.add(StringChoice.fromId(name));
                    idx = ne + 1;
                }
                return out;
            } catch (Exception e) { return List.of(); }
        }
    }

    /** Lists assignable users for the selected project (id=accountId, text=displayName). */
    static final class AssignableUsersProvider extends JiraChoicesProvider {
        private java.util.function.Supplier<String> m_projectSup;
        @Override public void init(final StateProvider.StateProviderInitializer i) {
            super.init(i);
            m_projectSup = i.getValueSupplier(ProjectRef.class);
            i.computeOnValueChange(ProjectRef.class);
        }
        @Override
        public List<StringChoice> computeState(final NodeParametersInput in) {
            final var proj = String.valueOf(m_projectSup.get());
            if (proj == null || proj.isBlank()) {
                return List.of();
            }
            try {
                final var path = "/rest/api/3/user/assignable/search?maxResults=1000&project=" + java.net.URLEncoder.encode(proj, java.nio.charset.StandardCharsets.UTF_8);
                final var req = baseRequest(in, path).GET().build();
                final var res = send(req);
                updateAuthCacheOnResponse(res.statusCode());
                if (res.statusCode() / 100 != 2) {
                    return List.of();
                }
                // parse accountId and displayName
                final var out = new ArrayList<StringChoice>();
                final String body = res.body();
                int idx = 0;
                while ((idx = body.indexOf("\"accountId\"", idx)) >= 0) {
                    int is = body.indexOf('"', body.indexOf(':', idx) + 1) + 1;
                    int ie = body.indexOf('"', is);
                    String id = is > 0 && ie > is ? body.substring(is, ie) : "";
                    int dn = body.indexOf("\"displayName\"", ie);
                    int ds = dn > 0 ? body.indexOf('"', body.indexOf(':', dn) + 1) + 1 : -1;
                    int de = ds > 0 ? body.indexOf('"', ds) : -1;
                    String name = ds > 0 && de > ds ? body.substring(ds, de) : id;
                    out.add(new StringChoice(id, name));
                    idx = ie + 1;
                }
                return out;
            } catch (Exception e) { return List.of(); }
        }
    }

    /** Lists components for selected project (id=text=name). */
    static final class ComponentsProvider extends JiraChoicesProvider {
        private java.util.function.Supplier<String> m_projectSup;
        @Override public void init(final StateProvider.StateProviderInitializer i) {
            super.init(i);
            m_projectSup = i.getValueSupplier(ProjectRef.class);
            i.computeOnValueChange(ProjectRef.class);
        }
        @Override
        public List<StringChoice> computeState(final NodeParametersInput in) {
            final var proj = String.valueOf(m_projectSup.get());
            if (proj == null || proj.isBlank()) {
                return List.of();
            }
            try {
                final var path = "/rest/api/3/project/" + java.net.URLEncoder.encode(proj, java.nio.charset.StandardCharsets.UTF_8) + "/components";
                final var req = baseRequest(in, path).GET().build();
                final var res = send(req);
                updateAuthCacheOnResponse(res.statusCode());
                if (res.statusCode() / 100 != 2) {
                    return List.of();
                }
                // inline parse: collect all component "name" fields
                final var out = new ArrayList<StringChoice>();
                final String body = res.body();
                int idx = 0;
                while ((idx = body.indexOf("\"name\"", idx)) >= 0) {
                    int ns = body.indexOf('"', body.indexOf(':', idx) + 1) + 1;
                    int ne = body.indexOf('"', ns);
                    if (ns <= 0 || ne <= ns) break;
                    String name = body.substring(ns, ne);
                    out.add(StringChoice.fromId(name));
                    idx = ne + 1;
                }
                return out;
            } catch (Exception e) { return List.of(); }
        }
    }

    /** Lists available labels in the instance (fallback to empty if disabled). */
    static final class LabelsProvider extends JiraChoicesProvider {
        @Override public java.util.List<org.knime.node.parameters.widget.choices.StringChoice> computeState(final org.knime.node.parameters.NodeParametersInput in) {
            try {
                return java.util.List.of();
            } catch (Exception e) { return java.util.List.of(); }
        }
    }
}
