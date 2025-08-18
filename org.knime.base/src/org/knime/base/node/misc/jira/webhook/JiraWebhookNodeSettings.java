package org.knime.base.node.misc.jira.webhook;

import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.layout.Layout;
import org.knime.node.parameters.layout.Section;
import org.knime.node.parameters.persistence.Persist;
import org.knime.node.parameters.persistence.Persistable;
import org.knime.node.parameters.widget.number.NumberInputWidget;
import org.knime.node.parameters.widget.number.NumberInputWidgetValidation;
import org.knime.node.parameters.widget.text.TextAreaWidget;
import org.knime.node.parameters.widget.text.TextInputWidget;
import org.knime.node.parameters.widget.message.TextMessage;
import org.knime.node.parameters.widget.message.TextMessage.MessageType;
import org.knime.node.parameters.updates.Effect;
import org.knime.node.parameters.updates.Effect.EffectType;
import org.knime.node.parameters.WidgetGroup;
import org.knime.node.parameters.widget.credentials.Credentials;
import org.knime.node.parameters.widget.credentials.CredentialsWidget;
import org.knime.base.node.misc.jira.shared.JiraUI;

@SuppressWarnings("restriction")
public final class JiraWebhookNodeSettings implements NodeParameters {

    @Section(title = "Webhook Listener") interface MainSection {}
    @Section(title = "Response") interface ResponseSection {}
    @Section(title = "Jira Registration") interface RegSection {}

    static final class Intro implements TextMessage.SimpleTextMessageProvider {
        @Override public boolean showMessage(final NodeParametersInput context) { return true; }
        @Override public String title() { return "Wait for Jira Webhook"; }
        @Override public String description() {
            return "This node starts a lightweight HTTP listener and waits for a single POST from Jira.\n\n"
                + "Tip: Use a random port (0) to avoid conflicts. The final URL is emitted when the node is executing.";
        }
        @Override public MessageType type() { return MessageType.INFO; }
    }

    @TextMessage(value = Intro.class) @Layout(MainSection.class) Void m_intro;

    @Widget(title = "Host", description = "Interface to bind (e.g., 0.0.0.0 or 127.0.0.1)")
    @TextInputWidget(placeholder = "0.0.0.0")
    @Layout(MainSection.class)
    @Persist(configKey = "listener/host")
    public String m_host = "0.0.0.0";

    @Widget(title = "Detect host automatically", description = "Resolve the machine hostname or first non-loopback IPv4 for the callback URL at execution time")
    @Layout(MainSection.class)
    @Persist(configKey = "listener/autoHost")
    public boolean m_autoHost = true;

    @Widget(title = "Port", description = "Port to listen on. Use 0 to choose a random free port.")
    @NumberInputWidget(
        minValidation = NumberInputWidgetValidation.MinValidation.IsNonNegativeValidation.class,
        maxValidation = JiraWebhookNodeSettings.PortMax.class)
    @Layout(MainSection.class)
    @Persist(configKey = "listener/port")
    public int m_port = 0;

    @Widget(title = "Path", description = "Path for the webhook endpoint (e.g., /jira/webhook)")
    @TextInputWidget(placeholder = "/jira/webhook")
    @Layout(MainSection.class)
    @Persist(configKey = "listener/path")
    public String m_path = "/jira/webhook";

    @Widget(title = "Shared Secret (optional)", description = "If set, the sender must supply this token via ?token=... or header X-Webhook-Token")
    @TextInputWidget(placeholder = "optional-shared-secret")
    @Layout(MainSection.class)
    @Persist(configKey = "listener/secret")
    public String m_secret = "";

    @Widget(title = "Timeout (seconds)", description = "Maximum time to wait for a webhook before failing")
    @NumberInputWidget(
        minValidation = NumberInputWidgetValidation.MinValidation.IsPositiveIntegerValidation.class,
        maxValidation = JiraWebhookNodeSettings.TimeoutMax.class)
    @Layout(MainSection.class)
    @Persist(configKey = "listener/timeoutSeconds")
    public int m_timeoutSeconds = 1800; // 30 minutes

    @Widget(title = "HTTP Response Body", description = "Body to return to the caller upon success")
    @TextAreaWidget
    @Layout(ResponseSection.class)
    @Persist(configKey = "response/body")
    public String m_responseBody = "OK";

    // Jira REST registration configuration
    static final class AuthGroup implements WidgetGroup, Persistable {
        @Widget(title = "E‑mail / Username", description = "Jira account e‑mail (Cloud) or username (Server)")
        @TextInputWidget @Persist(configKey = "auth/email") public String m_email = "";
        @Widget(title = "API Token / Password", description = "API token (Cloud) or password (Server)")
        @CredentialsWidget @Persist(configKey = "auth/secret") public Credentials m_token = new Credentials();
    }

    @Widget(title = "Base URL", description = "Jira site URL, e.g. https://your-domain.atlassian.net")
    @Effect(predicate = JiraUI.JiraConnectionPresent.class, type = EffectType.HIDE)
    @TextInputWidget @Layout(RegSection.class) @Persist(configKey = "server/url") public String m_baseUrl = "";

    @Widget(title = "Credentials", description = "Credentials used for Jira REST authentication")
    @Effect(predicate = JiraUI.JiraConnectionPresent.class, type = EffectType.HIDE)
    @Layout(RegSection.class) @Persist(configKey = "server/credentials") public AuthGroup m_auth = new AuthGroup();

    @TextMessage(value = JiraUI.JiraConnectionInfo.class) @Layout(RegSection.class) Void m_connectionInfo;

    @Widget(title = "Register automatically", description = "Create the webhook in Jira via REST before waiting")
    @Layout(RegSection.class) @Persist(configKey = "reg/enable") public boolean m_register = true;

    @Widget(title = "Unregister after execution", description = "Delete the webhook in Jira when done")
    @Layout(RegSection.class) @Persist(configKey = "reg/unregister") public boolean m_unregister = true;

    @Widget(title = "Webhook Name", description = "Name of the webhook to create")
    @TextInputWidget(placeholder = "KNIME Webhook")
    @Layout(RegSection.class) @Persist(configKey = "reg/name") public String m_name = "KNIME Webhook";

    @Widget(title = "Events (comma‑separated)", description = "Event IDs, e.g. jira:issue_created,jira:issue_updated")
    @TextInputWidget(placeholder = "jira:issue_created,jira:issue_updated")
    @Layout(RegSection.class) @Persist(configKey = "reg/events") public String m_events = "jira:issue_updated";

    @Widget(title = "JQL Filter", description = "Restrict events to issues matching this JQL (optional)")
    @TextInputWidget(placeholder = "project = TEST")
    @Layout(RegSection.class) @Persist(configKey = "reg/jql") public String m_jql = "";

    @Widget(title = "Exclude Body", description = "Ask Jira to omit the JSON body in callbacks (Server/DC)")
    @Layout(RegSection.class) @Persist(configKey = "reg/excludeBody") public boolean m_excludeBody = false;

    @Widget(title = "Description", description = "Webhook description (optional)")
    @TextInputWidget
    @Layout(RegSection.class) @Persist(configKey = "reg/description") public String m_description = "";

    @Widget(title = "Active", description = "Create webhook as active")
    @Layout(RegSection.class) @Persist(configKey = "reg/active") public boolean m_active = true;

    @Widget(title = "Public URL override", description = "Override the callback URL used for registration (optional)")
    @TextInputWidget(placeholder = "https://public-host/webhook")
    @Layout(RegSection.class) @Persist(configKey = "reg/publicUrl") public String m_publicUrl = "";

    // Custom max validations
    public static final class PortMax extends NumberInputWidgetValidation.MaxValidation {
        @Override protected double getMax() { return 65535; }
    }
    public static final class TimeoutMax extends NumberInputWidgetValidation.MaxValidation {
        @Override protected double getMax() { return 86400; }
    }
}
