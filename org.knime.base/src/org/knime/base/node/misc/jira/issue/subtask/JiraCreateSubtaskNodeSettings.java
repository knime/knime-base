package org.knime.base.node.misc.jira.issue.subtask;

import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.WidgetGroup;
import org.knime.node.parameters.layout.After;
import org.knime.node.parameters.layout.Layout;
import org.knime.node.parameters.layout.Section;
import org.knime.node.parameters.persistence.Persist;
import org.knime.node.parameters.persistence.Persistable;
import org.knime.node.parameters.widget.credentials.Credentials;
import org.knime.node.parameters.widget.credentials.CredentialsWidget;
import org.knime.node.parameters.updates.Effect;
import org.knime.node.parameters.updates.Effect.EffectType;
import org.knime.node.parameters.widget.text.TextAreaWidget;
import org.knime.node.parameters.widget.message.TextMessage;
import org.knime.node.parameters.widget.message.TextMessage.MessageType;
import org.knime.base.node.misc.jira.conn.JiraConnectionPortObjectSpec;
import org.knime.node.parameters.widget.text.TextInputWidget;

@SuppressWarnings("restriction")
public final class JiraCreateSubtaskNodeSettings implements NodeParameters {
    @Section(title = "Jira Server") interface ServerSection {}
    @Section(title = "Sub-task") @After(ServerSection.class) interface SubtaskSection {}

    static final class AuthGroup implements WidgetGroup, Persistable {
        @Widget(title = "E‑mail / Username", description = "Jira account e‑mail (Cloud) or username (Server)") @TextInputWidget @Persist(configKey = "auth/email") public String m_email = "";
        @Widget(title = "API Token / Password", description = "API token (Cloud) or password (Server)") @CredentialsWidget @Persist(configKey = "auth/secret") public Credentials m_token = new Credentials();
    }

    // Info banner: show when a Jira Connection is connected
    static final class JiraConnectionInfo implements TextMessage.SimpleTextMessageProvider {
        @Override public boolean showMessage(final NodeParametersInput context) {
            return context != null && context.getInPortSpecs() != null && context.getInPortSpecs().length > 0
                && context.getInPortSpecs()[0] instanceof JiraConnectionPortObjectSpec;
        }
        @Override public String title() { return "Jira connection managed by input port"; }
        @Override public String description() { return "A Jira Connection is connected. Base URL and Credentials are taken from the connection."; }
        @Override public MessageType type() { return MessageType.INFO; }
    }

    @TextMessage(value = org.knime.base.node.misc.jira.shared.JiraUI.JiraConnectionInfo.class) @Layout(ServerSection.class) Void m_connectionInfo;

    @Widget(title = "Base URL", description = "Jira site URL, e.g. https://your-domain.atlassian.net") @Effect(predicate = org.knime.base.node.misc.jira.shared.JiraUI.JiraConnectionPresent.class, type = EffectType.HIDE) @TextInputWidget @Layout(ServerSection.class) @Persist(configKey = "server/url") public String m_baseUrl = "";
    @Widget(title = "Credentials", description = "Credentials used for Jira REST authentication") @Effect(predicate = org.knime.base.node.misc.jira.shared.JiraUI.JiraConnectionPresent.class, type = EffectType.HIDE) @Layout(ServerSection.class) @Persist(configKey = "server/credentials") public AuthGroup m_auth = new AuthGroup();

    @Widget(title = "Parent Issue Key", description = "Parent issue key, e.g. PROJ-123") @TextInputWidget @Layout(SubtaskSection.class) @Persist(configKey = "issue/parent") public String m_parentKey = "";
    @Widget(title = "Summary", description = "Sub-task summary") @TextInputWidget @Layout(SubtaskSection.class) @Persist(configKey = "issue/summary") public String m_summary = "";
    @Widget(title = "Description", description = "Sub-task description") @TextAreaWidget @Layout(SubtaskSection.class) @Persist(configKey = "issue/description") public String m_description = "";
    @Widget(title = "Sub-task Issue Type Name", description = "Defaults to Sub-task") @TextInputWidget(placeholder = "Sub-task") @Layout(SubtaskSection.class) @Persist(configKey = "issue/type") public String m_issueType = "Sub-task";
}
