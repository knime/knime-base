/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME AG, Zurich, Switzerland
 * ---------------------------------------------------------------------
 */
package org.knime.base.node.misc.jira.update;

import org.knime.base.node.misc.jira.*;
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
import org.knime.node.parameters.widget.message.TextMessage;
import org.knime.node.parameters.widget.message.TextMessage.MessageType;
import org.knime.base.node.misc.jira.conn.JiraConnectionPortObjectSpec;
import org.knime.node.parameters.widget.text.TextAreaWidget;
import org.knime.node.parameters.widget.text.TextInputWidget;

@SuppressWarnings("restriction")
public final class JiraUpdateIssueNodeSettings implements NodeParameters {

    @Section(title = "Jira Server") interface ServerSection {}
    @Section(title = "Update") @After(ServerSection.class) interface UpdateSection {}

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

    @Widget(title = "Base URL", description = "Jira site URL, e.g. https://your-domain.atlassian.net")
    @Effect(predicate = org.knime.base.node.misc.jira.shared.JiraUI.JiraConnectionPresent.class, type = EffectType.HIDE)
    @TextInputWidget(placeholder = "https://example.atlassian.net")
    @Layout(ServerSection.class)
    @Persist(configKey = "server/url")
    public String m_baseUrl = "";

    @Widget(title = "Credentials", description = "Credentials used for Jira REST authentication")
    @Effect(predicate = org.knime.base.node.misc.jira.shared.JiraUI.JiraConnectionPresent.class, type = EffectType.HIDE)
    @Layout(ServerSection.class)
    @Persist(configKey = "server/credentials")
    public AuthGroup m_auth = new AuthGroup();

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

    @Widget(title = "Issue Key", description = "Issue key, e.g. PROJ-123")
    @TextInputWidget(placeholder = "PROJ-123")
    @Layout(UpdateSection.class)
    @Persist(configKey = "issue/key")
    public String m_key = "";

    @Widget(title = "Summary", description = "New summary/title") @TextInputWidget @Layout(UpdateSection.class) @Persist(configKey = "issue/summary") public String m_summary = "";
    @Widget(title = "Description", description = "New description text") @TextAreaWidget @Layout(UpdateSection.class) @Persist(configKey = "issue/description") public String m_description = "";
    @Widget(title = "Priority", description = "Priority name") @TextInputWidget @Layout(UpdateSection.class) @Persist(configKey = "issue/priority") public String m_priority = "";
    @Widget(title = "Assignee (account id)", description = "User account id") @TextInputWidget @Layout(UpdateSection.class) @Persist(configKey = "issue/assignee") public String m_assignee = "";
    @Widget(title = "Labels (comma-separated)", description = "Comma-separated labels") @TextInputWidget @Layout(UpdateSection.class) @Persist(configKey = "issue/labels") public String m_labels = "";
    @Widget(title = "Components (comma-separated)", description = "Comma-separated component names") @TextInputWidget @Layout(UpdateSection.class) @Persist(configKey = "issue/components") public String m_components = "";
}
