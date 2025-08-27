/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME AG, Zurich, Switzerland
 * ---------------------------------------------------------------------
 */
package org.knime.base.node.misc.jira.search;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import org.knime.base.node.misc.jira.shared.JiraUI;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.WidgetGroup;
import org.knime.node.parameters.layout.After;
import org.knime.node.parameters.layout.Layout;
import org.knime.node.parameters.layout.Section;
import org.knime.node.parameters.persistence.Persist;
import org.knime.node.parameters.persistence.Persistable;
import org.knime.node.parameters.updates.Effect;
import org.knime.node.parameters.updates.Effect.EffectType;
import org.knime.node.parameters.updates.StateProvider;
import org.knime.node.parameters.updates.ValueProvider;
import org.knime.node.parameters.updates.ValueReference;
import org.knime.node.parameters.widget.choices.ChoicesProvider;
import org.knime.node.parameters.widget.choices.StringChoice;
import org.knime.node.parameters.widget.choices.StringChoicesProvider;
import org.knime.node.parameters.widget.choices.filter.TwinlistWidget;
import org.knime.node.parameters.widget.credentials.Credentials;
import org.knime.node.parameters.widget.credentials.CredentialsWidget;
import org.knime.node.parameters.widget.message.TextMessage;
import org.knime.node.parameters.widget.number.NumberInputWidget;
import org.knime.node.parameters.widget.text.TextAreaWidget;
import org.knime.node.parameters.widget.text.TextInputWidget;

@SuppressWarnings("restriction")
public final class JiraSearchIssuesNodeSettings implements NodeParameters {

    @Section(title = "Jira Server") interface ServerSection {}
    @Section(title = "Query") @After(ServerSection.class) interface QuerySection {}
    @Section(title = "AI Assistant (optional)") @After(QuerySection.class) interface AiSection {}

    static final class AuthGroup implements WidgetGroup, Persistable {
        @Widget(title = "E‑mail / Username", description = "Jira account e‑mail (Cloud) or username (Server)")
        @TextInputWidget(placeholder = "me@example.com")
        @Persist(configKey = "auth/email")
        public String m_email = "";

        @Widget(title = "API Token / Password", description = "API token (Cloud) or password (Server)")
        @CredentialsWidget
        @Persist(configKey = "auth/secret")
        public Credentials m_token = new Credentials();
    }

    @Widget(title = "Base URL", description = "https://your-domain.atlassian.net")
    @Effect(predicate = JiraUI.JiraConnectionPresent.class, type = EffectType.HIDE)
    @TextInputWidget(placeholder = "https://example.atlassian.net")
    @Layout(ServerSection.class)
    @Persist(configKey = "server/url")
    public String m_baseUrl = "";

    @Widget(title = "Credentials", description = "Credentials used for Jira REST authentication")
    @Effect(predicate = JiraUI.JiraConnectionPresent.class, type = EffectType.HIDE)
    @Layout(ServerSection.class)
    @Persist(configKey = "server/credentials")
    public AuthGroup m_auth = new AuthGroup();

    @TextMessage(value = JiraUI.JiraConnectionInfo.class)
    @Layout(ServerSection.class)
    Void m_connectionInfo;

    @Widget(title = "JQL", description = "Jira Query Language")
    @TextAreaWidget
    @Layout(QuerySection.class)
    @Persist(configKey = "query/jql")
    @ValueReference(JqlRef.class)
    @ValueProvider(AiJqlAutoApplyProvider.class)
    public String m_jql = "";

    // --- AI assistant controls placed directly below the JQL field ---

    @Widget(title = "Describe your search", description = "Tell the assistant what to find; it will propose JQL.")
    @TextAreaWidget
    @Layout(QuerySection.class)
    @Persist(configKey = "ai/prompt")
    @ValueReference(PromptRef.class)
    public String m_aiPrompt = "";

    // Warn if there's no valid Hub authentication available for K-AI
    @TextMessage(value = AiAuthWarning.class)
    @Layout(QuerySection.class)
    Void m_aiAuthWarning;

    // Button to explicitly trigger AI suggestions (avoids requests on every keystroke)
    @Widget(title = "Generate suggestion", description = "Call K-AI to propose a JQL based on the prompt above.")
    @Effect(predicate = AiBusyPredicate.class, type = EffectType.DISABLE)
    @org.knime.core.webui.node.dialog.defaultdialog.internal.button.SimpleButtonWidget(
        ref = GenerateButtonRef.class)
    @Layout(QuerySection.class)
    Void m_generate;

    // A loading placeholder shown next to the button while AI is computing
    @TextMessage(value = JiraSearchIssuesNodeSettings.AiLoadingProvider.class)
    @Layout(QuerySection.class)
    Void m_aiSuggestionsLoading;

    @TextMessage(value = AiInfo.class)
    @Layout(QuerySection.class)
    Void m_aiInfo;

    // Hidden persisted string for fields (for model compatibility)
    @Persist(configKey = "query/fields")
    @ValueReference(FieldsStringRef.class)
    @ValueProvider(FieldsStringFromSelectionProvider.class)
    public String m_fields = "";

    // Visible twinlist picker to choose fields from a prefilled list; writes into m_fields via providers
    @Widget(title = "Fields", description = "Select the Jira fields to include in the result")
    @TwinlistWidget
    @Layout(QuerySection.class)
    @ChoicesProvider(FieldsChoicesProvider.class)
    @ValueReference(FieldsSelectionRef.class)
    @ValueProvider(FieldsSelectionProvider.class)
    public String[] m_fieldsPicker = new String[0];

    @Widget(title = "Max Results", description = "Maximum number of results to fetch")
    @NumberInputWidget
    @Layout(QuerySection.class)
    @Persist(configKey = "query/max")
    public int m_max = 50;

    /* ====================== AI Assistant for JQL ====================== */

    /** Shows a loading spinner while the AI computation is in progress by depending on the JQL value provider. */
    static final class AiLoadingProvider
            implements org.knime.node.parameters.updates.StateProvider<java.util.Optional<TextMessage.Message>> {
        private java.util.function.Supplier<String> m_promptSup;

        @Override
        public void init(final StateProvider.StateProviderInitializer initializer) {
            m_promptSup = initializer.getValueSupplier(PromptRef.class);
            // Only react to explicit button clicks; no automatic triggers
            initializer.computeOnButtonClick(GenerateButtonRef.class);
        }

        @Override
        public java.util.Optional<TextMessage.Message> computeState(
                final org.knime.node.parameters.NodeParametersInput context) {
            final String prompt = String.valueOf(m_promptSup.get());
            if (prompt != null && !prompt.isBlank() && isInFlight()) {
                return java.util.Optional.of(new TextMessage.Message(
                    "Generating suggestion…",
                    "Contacting K-AI to generate a JQL from your description.",
                    TextMessage.MessageType.INFO));
            }
            return java.util.Optional.empty();
        }
    }

    // (moved prompt, button, and loading/info into Query section above)

    static final class PromptRef implements org.knime.node.parameters.updates.ParameterReference<String> { }
    static final class JqlRef implements org.knime.node.parameters.updates.ParameterReference<String> { }
    static final class SuggestionRef implements org.knime.node.parameters.updates.ParameterReference<String> { }
    static final class GenerateButtonRef implements org.knime.node.parameters.updates.ButtonReference { }
    static final class FieldsStringRef implements org.knime.node.parameters.updates.ParameterReference<String> { }
    static final class FieldsSelectionRef implements org.knime.node.parameters.updates.ParameterReference<String[]> { }

    /** Holds the last AI suggestion so other providers (e.g., dropdown) can reuse it without re-calling the backend. */
    private static volatile String LAST_AI_SUGGESTION = "";
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    // Suggestions dropdown removed; no loading provider needed

    /** Info banner about using the shared AI endpoint. */
    static final class AiInfo implements TextMessage.SimpleTextMessageProvider {
        @Override public boolean showMessage(final org.knime.node.parameters.NodeParametersInput ctx) { return true; }
        @Override public String title() { return "Ask KAi to help with JQL"; }
        @Override public String description() {
            return "Type a prompt and pick a suggestion. This calls the configured K-AI Python code generation endpoint "
                + "(/code_generation/python) resolved via KaiPreferences (HTTP). No Jira data is sent—only your prompt "
                + "and optional current JQL. Authorization is applied when available.";
        }
        @Override public TextMessage.MessageType type() { return TextMessage.MessageType.INFO; }
    }

    /** Warning shown when no Hub authentication is available. */
    static final class AiAuthWarning implements TextMessage.SimpleTextMessageProvider {
        @Override
        public boolean showMessage(final org.knime.node.parameters.NodeParametersInput ctx) {
            return resolveKaiAuthToken() == null;
        }
        @Override public String title() { return "K-AI authentication required"; }
        @Override public String description() {
            return "No valid KNIME Hub authentication token was found. Sign in to Hub (or set knime.ai.assistant.token / "
                + "KNIME_AI_ASSISTANT_TOKEN) to enable AI JQL generation.";
        }
        @Override public TextMessage.MessageType type() { return TextMessage.MessageType.WARNING; }
    }

    // Suggestions dropdown removed
    private static final java.util.concurrent.atomic.AtomicBoolean AI_IN_FLIGHT = new java.util.concurrent.atomic.AtomicBoolean(false);

    static boolean isInFlight() { return AI_IN_FLIGHT.get(); }

    /** Choices for Jira fields (static common set). */
    static final class FieldsChoicesProvider implements StringChoicesProvider {
        @Override
        public java.util.List<StringChoice> computeState(final org.knime.node.parameters.NodeParametersInput in) {
            final String[] fields = new String[] {
                "key","summary","status","assignee","reporter","priority","description","issuetype",
                "project","resolution","created","updated","duedate","labels","components","fixVersions",
                "watcher","sprint","parent","timeoriginalestimate","timespent","story points","epic link"
            };
            final java.util.ArrayList<StringChoice> list = new java.util.ArrayList<>(fields.length);
            for (final var f : fields) { list.add(StringChoice.fromId(f)); }
            return list;
        }
    }

    /** Initialize the twinlist from the persisted comma-separated string. */
    static final class FieldsSelectionProvider implements StateProvider<String[]> {
        private java.util.function.Supplier<String> m_fieldsStringSup;

        @Override
        public void init(final StateProvider.StateProviderInitializer initializer) {
            m_fieldsStringSup = initializer.getValueSupplier(FieldsStringRef.class);
            // Recompute when the hidden string changes (e.g., loaded from model)
            initializer.computeOnValueChange(FieldsStringRef.class);
        }

        @Override
        public String[] computeState(final org.knime.node.parameters.NodeParametersInput context) {
            final String s = String.valueOf(m_fieldsStringSup.get());
            if (s == null || s.isBlank()) { return new String[0]; }
            return java.util.Arrays.stream(s.split(","))
                .map(String::trim)
                .filter(t -> !t.isEmpty())
                .toArray(String[]::new);
        }
    }

    /** Join twinlist selection into the persisted comma-separated string. */
    static final class FieldsStringFromSelectionProvider implements StateProvider<String> {
        private java.util.function.Supplier<String[]> m_selectionSup;

        @Override
        public void init(final StateProvider.StateProviderInitializer initializer) {
            m_selectionSup = initializer.getValueSupplier(FieldsSelectionRef.class);
            // Update the string when selection changes
            initializer.computeOnValueChange(FieldsSelectionRef.class);
        }

        @Override
        public String computeState(final org.knime.node.parameters.NodeParametersInput context) {
            final String[] sel = m_selectionSup.get();
            if (sel == null || sel.length == 0) { return ""; }
            return String.join(",", sel);
        }
    }

    static String callKaiForJql(final String prompt, final String currentJql) {
        boolean acquired = false;
        try {
            // single-flight guard: if a request is already running, don’t start another
            acquired = AI_IN_FLIGHT.compareAndSet(false, true);
            if (!acquired) {
                return null;
            }
            final String authToken = resolveKaiAuthToken();

            // Determine Python endpoint
            String endpoint = System.getProperty("knime.ai.suggest.url", "").trim();
            if (!endpoint.isEmpty()) {
                // Normalize scheme and ensure we target the python endpoint path
                if (endpoint.startsWith("ws://")) {
                    endpoint = "http://" + endpoint.substring(5);
                } else if (endpoint.startsWith("wss://")) {
                    endpoint = "https://" + endpoint.substring(6);
                }
                if (!endpoint.contains("/code_generation/python")) {
                    endpoint = endpoint.replaceAll("/+$", "");
                    endpoint = endpoint + "/code_generation/python";
                }
            } else {
                final String httpFromPrefs = resolveKaiHttpFromPreferences();
                if (httpFromPrefs != null && !httpFromPrefs.isBlank()) {
                    final String base = httpFromPrefs.endsWith("/") ? httpFromPrefs.substring(0, httpFromPrefs.length() - 1)
                            : httpFromPrefs;
                    endpoint = base + "/code_generation/python";
                } else {
                    endpoint = "https://hub.knime.com/ai/v1/code_generation/python";
                }
            }

        final String userQueryRaw = prompt == null ? "" : prompt;
        final String codeStr = currentJql == null ? "" : currentJql;
        // Prompt engineering: ask explicitly for a JSON {"jql":"..."} or a first-line '# JQL: ...' marker
        final String engineeredQuery = "You are a Jira JQL assistant. Based on the description, produce the final Jira JQL. "
            + "Return either a JSON object with a single field jql containing only the query, or if you must return Python code, "
            + "place the JQL on the very first line as a comment exactly in the form: # JQL: <query>. "
            + "Do not add any extra commentary. Description: " + userQueryRaw + ". Current JQL: " + codeStr + ".";
        final String payload = "{"
                    + "\"user_query\":" + jsonEscape(engineeredQuery)
            + ",\"code\":" + jsonEscape(codeStr)
            + ",\"inputs\":{\"tables\":["
            + "[{\"name\":\"columns\",\"type\":\"columns\",\"columns\":[\"col\"]},{\"name\":\"rows\",\"type\":\"rows\",\"rows\":[[\"\"]]}],"
            + "[{\"name\":\"columns\",\"type\":\"columns\",\"columns\":[\"col2\"]},{\"name\":\"rows\",\"type\":\"rows\",\"rows\":[[\"\"]]}]"
            + "],\"num_objects\":0,\"flow_variables\":[{\"name\":\"current_jql\",\"type\":\"string\",\"value\":" + jsonEscape(codeStr) + "}]},"
            + "\"outputs\":{\"num_tables\":2,\"num_images\":0,\"num_objects\":0,\"has_view\":false}}";

            final HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(endpoint))
                    .timeout(Duration.ofSeconds(20))
                    .header("Content-Type", "application/json");
            if (authToken != null && !authToken.isBlank()) {
                b.header("Authorization", authToken);
            }
            final HttpRequest req = b.POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8)).build();
            final HttpResponse<String> res = HTTP_CLIENT.send(req, HttpResponse.BodyHandlers.ofString());
        if (res.statusCode() / 100 == 2) {
                final String body = res.body();
                final String jql = extractJsonField(body, "jql");
                if (jql != null && !jql.isBlank()) {
            return jql;
                }
                final String codeOut = extractJsonField(body, "code");
                if (codeOut != null && !codeOut.isBlank()) {
                    // Try to extract JQL from returned code or embedded JSON
                    final String innerJsonJql = extractJsonField(codeOut, "jql");
                    if (innerJsonJql != null && !innerJsonJql.isBlank()) {
                        return innerJsonJql;
                    }
                    final String markerJql = extractJqlFromCode(codeOut);
                    if (markerJql != null && !markerJql.isBlank()) {
                        return markerJql;
                    }
                }
                return null;
            }
            return null;
        } catch (final Exception ignore) {
            return null;
        } finally {
            if (acquired) {
                AI_IN_FLIGHT.set(false);
            }
        }
    }




        /** Resolve HTTP server from KaiPreferences if the knime-ai bundle is present. */
        private static String resolveKaiHttpFromPreferences() {
            try {
                if (!isKaiPreferencesAvailable()) {
                    return null;
                }
                final Class<?> prefs = Class.forName("org.knime.ai.assistant.java.prefs.KaiPreferences", false,
                    JiraSearchIssuesNodeSettings.class.getClassLoader());
                final Class<?> proto = Class.forName("org.knime.ai.assistant.java.prefs.KaiPreferences$Protocol");
                @SuppressWarnings("unchecked")
                final Object http = java.lang.Enum.valueOf((Class<java.lang.Enum>)proto, "HTTP");
                final var method = prefs.getMethod("getAiServiceAddress", proto);
                final Object uri = method.invoke(null, http); // returns java.net.URI
                return uri != null ? uri.toString() : null;
            } catch (final Throwable t) {
                return null;
            }
        }

        /** Best-effort check for KaiPreferences presence without throwing CNFE into logs. */
        private static boolean isKaiPreferencesAvailable() {
            final var loader = JiraSearchIssuesNodeSettings.class.getClassLoader();
            if (loader == null) {
                return false;
            }
            final var res = loader.getResource("org/knime/ai/assistant/java/prefs/KaiPreferences.class");
            return res != null;
        }

        /** Resolve auth token from the connected Hub similar to other AI services. */
        private static String resolveKaiAuthToken() {
            // 1) Preferred: get token from ExplorerMountTable (local AP) and selected Hub, but via reflection so it's optional
            try {
                // Try to load KaiPreferences via reflection to get hub id
                String hubId = null;
                try {
                    final Class<?> kaiPrefsClz = Class.forName("org.knime.ai.assistant.java.prefs.KaiPreferences", false,
                            JiraSearchIssuesNodeSettings.class.getClassLoader());
                    final var getHubId = kaiPrefsClz.getMethod("getHubId");
                    final Object id = getHubId.invoke(null);
                    if (id != null) {
                        hubId = id.toString();
                    }
                } catch (final Throwable t) {
                    // ignore, proceed to props/env fallback
                }

                if (hubId != null && !hubId.isBlank()) {
                    // ExplorerMountTable.getMountedContent().get(hubId)
                    final Class<?> mountTableClz = Class.forName("org.knime.workbench.explorer.ExplorerMountTable", false,
                            JiraSearchIssuesNodeSettings.class.getClassLoader());
                    final var getMountedContent = mountTableClz.getMethod("getMountedContent");
                    final Object map = getMountedContent.invoke(null);
                    if (map instanceof java.util.Map<?, ?> m) {
                        final Object contentProvider = m.get(hubId);
                        if (contentProvider != null) {
                            // We can't instanceof directly; use reflection to call getRemoteFileSystem if present
                            try {
                                final var getRfs = contentProvider.getClass().getMethod("getRemoteFileSystem");
                                final Object rfs = getRfs.invoke(contentProvider);
                                if (rfs != null) {
                                    // Check for com.knime.enterprise.client.rest.HubContent methods
                                    try {
                                        final Class<?> hubContentClz = Class.forName("com.knime.enterprise.client.rest.HubContent", false,
                                                JiraSearchIssuesNodeSettings.class.getClassLoader());
                                        if (hubContentClz.isInstance(rfs)) {
                                            final var isAuth = hubContentClz.getMethod("isAuthenticated");
                                            final Object authed = isAuth.invoke(rfs);
                                            if (Boolean.TRUE.equals(authed)) {
                                                final var getAuth = hubContentClz.getMethod("getAuthenticator");
                                                final Object authenticator = getAuth.invoke(rfs);
                                                if (authenticator != null) {
                                                    final var getAuthorization = authenticator.getClass().getMethod("getAuthorization");
                                                    final Object token = getAuthorization.invoke(authenticator);
                                                    if (token != null) {
                                                        final String t = token.toString();
                                                        if (!t.isBlank()) {
                                                            return t;
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    } catch (final Throwable ignoreHubContent) {
                                        // Absent or unexpected, fall through
                                    }
                                }
                            } catch (final Throwable ignoreRfs) {
                                // No getRemoteFileSystem or error calling it; fall through
                            }
                        }
                    }
                }
            } catch (final Throwable ignore) {
                // Continue with fallbacks
            }
            // 2) Fallback: explicit configuration via properties/env (engine/headless, custom deployments)
            final String[] props = {"knime.ai.assistant.token", "knime.ai.token"};
            for (final var p : props) {
                try {
                    final var v = System.getProperty(p);
                    if (v != null && !v.isBlank()) {
                        return v.trim();
                    }
                } catch (final SecurityException se) {
                    // ignore
                }
            }
            final String[] envs = {"KNIME_AI_ASSISTANT_TOKEN", "KNIME_AI_TOKEN"};
            try {
                final var env = System.getenv();
                for (final var e : envs) {
                    final var v = env.get(e);
                    if (v != null && !v.isBlank()) {
                        return v.trim();
                    }
                }
            } catch (final Throwable t) {
                // ignore
            }
            return null;
        }
        private static String jsonEscape(final String s) {
            if (s == null) {
                return "\"\"";
            }
            final var esc = s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
            return "\"" + esc + "\"";
        }

    private static String extractJsonField(final String json, final String key) {
            if (json == null) {
                return null;
            }
            final String needle = "\"" + key + "\"";
            int k = json.indexOf(needle);
            if (k < 0) {
                return null;
            }
            int colon = json.indexOf(':', k + needle.length());
            if (colon < 0) {
                return null;
            }
            // Skip whitespace
            int i = colon + 1;
            while (i < json.length() && Character.isWhitespace(json.charAt(i))) {
                i++;
            }
            if (i >= json.length()) {
                return null;
            }
            // We only handle string values here (surrounded by quotes)
            if (json.charAt(i) != '"') {
                return null;
            }
            i++; // move past opening quote
            final StringBuilder sb = new StringBuilder();
            boolean esc = false;
            while (i < json.length()) {
                char c = json.charAt(i++);
                if (esc) {
                    switch (c) {
                        case '"': sb.append('"'); break;
                        case '\\': sb.append('\\'); break;
                        case '/': sb.append('/'); break;
                        case 'b': sb.append('\b'); break;
                        case 'f': sb.append('\f'); break;
                        case 'n': sb.append('\n'); break;
                        case 'r': sb.append('\r'); break;
                        case 't': sb.append('\t'); break;
                        case 'u':
                            // decode XXXX if present
                            if (i + 4 <= json.length()) {
                                String hex = json.substring(i, Math.min(i + 4, json.length()));
                                try {
                                    int cp = Integer.parseInt(hex, 16);
                                    sb.append((char)cp);
                                    i += 4;
                                } catch (NumberFormatException nfe) {
                                    // fallback: keep as-is
                                    sb.append("\\u").append(hex);
                                    i += 4;
                                }
                            } else {
                                sb.append('u');
                            }
                            break;
                        default:
                            // unknown escape - keep literal
                            sb.append(c);
                    }
                    esc = false;
                    continue;
                }
                if (c == '\\') {
                    esc = true;
                    continue;
                }
                if (c == '"') {
                    // end of string
                    break;
                }
                sb.append(c);
            }
            return sb.toString();
        }

    private static String extractJqlFromCode(final String code) {
            if (code == null) {
                return null;
            }
            // Look for a first-line marker: # JQL: ...
            int nl = code.indexOf('\n');
            final String firstLine = nl >= 0 ? code.substring(0, nl) : code;
            final String prefix = "# JQL:";
            if (firstLine.startsWith(prefix)) {
                return firstLine.substring(prefix.length()).trim();
            }
            // As a fallback, search for `jql = "..."` in the code
            final String assign = "jql = \"";
            int p = code.indexOf(assign);
            if (p >= 0) {
                int start = p + assign.length();
                final StringBuilder sb = new StringBuilder();
                boolean esc = false;
                for (int i = start; i < code.length(); i++) {
                    char c = code.charAt(i);
                    if (esc) {
                        if (c == '"' || c == '\\') {
                            sb.append(c);
                        } else if (c == 'n') {
                            sb.append('\n');
                        } else if (c == 'r') {
                            sb.append('\r');
                        } else if (c == 't') {
                            sb.append('\t');
                        } else {
                            sb.append(c);
                        }
                        esc = false;
                        continue;
                    }
                    if (c == '\\') { esc = true; continue; }
                    if (c == '"') { break; }
                    sb.append(c);
                }
                final String out = sb.toString().trim();
                if (!out.isBlank()) { return out; }
            }
            return null;
        }
    

    /** Auto-applies AI-generated JQL directly into the JQL field when the prompt changes. */
    static final class AiJqlAutoApplyProvider implements StateProvider<String> {
    private java.util.function.Supplier<String> m_promptSup;
    private java.util.function.Supplier<String> m_currentJqlSup;

        @Override
        public void init(final StateProvider.StateProviderInitializer initializer) {
            m_promptSup = initializer.getValueSupplier(PromptRef.class);
            // We read current JQL for context, but don't trigger on it to avoid loops
            m_currentJqlSup = initializer.getValueSupplier(JqlRef.class);
            // Only recompute on explicit button click or suggestion pick
            initializer.computeOnButtonClick(GenerateButtonRef.class);
        }

        @Override
        public String computeState(final org.knime.node.parameters.NodeParametersInput context) {
            final String prompt = String.valueOf(m_promptSup.get());
            final String current = String.valueOf(m_currentJqlSup.get());
            if (prompt == null || prompt.isBlank()) {
                // Don't override when there's no prompt
                return current;
            }
            // Single call site: call backend here, store result, and apply directly
            final String ai = callKaiForJql(prompt, current);
            if (ai != null && !ai.isBlank()) {
                LAST_AI_SUGGESTION = ai;
                return ai;
            }
            return current;
        }
    }

    /** Predicate to disable the Generate button while an AI request is running. */
    static final class AiBusyPredicate implements org.knime.node.parameters.updates.EffectPredicateProvider {
        @Override
        public org.knime.node.parameters.updates.EffectPredicate init(
                final org.knime.node.parameters.updates.EffectPredicateProvider.PredicateInitializer i) {
            return i.getConstant(ctx -> isInFlight());
        }
    }
}
