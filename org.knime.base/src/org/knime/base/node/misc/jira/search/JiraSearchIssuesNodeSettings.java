/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME AG, Zurich, Switzerland
 * ---------------------------------------------------------------------
 */
package org.knime.base.node.misc.jira.search;

import org.knime.base.node.misc.jira.*;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.WidgetGroup;
import org.knime.node.parameters.layout.After;
import org.knime.node.parameters.layout.Layout;
import org.knime.node.parameters.layout.Section;
import org.knime.node.parameters.persistence.Persist;
import org.knime.node.parameters.persistence.Persistable;
import org.knime.node.parameters.widget.credentials.Credentials;
import org.knime.node.parameters.updates.Effect;
import org.knime.node.parameters.updates.Effect.EffectType;
import org.knime.node.parameters.widget.credentials.CredentialsWidget;
import org.knime.node.parameters.widget.message.TextMessage;
import org.knime.node.parameters.widget.number.NumberInputWidget;
import org.knime.node.parameters.widget.text.TextAreaWidget;
import org.knime.node.parameters.widget.text.TextInputWidget;
import org.knime.base.node.misc.jira.shared.JiraUI;

@SuppressWarnings("restriction")
public final class JiraSearchIssuesNodeSettings implements NodeParameters {

    @Section(title = "Jira Server") interface ServerSection {}
    @Section(title = "Query") @After(ServerSection.class) interface QuerySection {}

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
    public String m_jql = "";

    @Widget(title = "Fields", description = "Comma-separated field names (default: key,summary,status,assignee)")
    @TextInputWidget(placeholder = "key,summary,status,assignee")
    @Layout(QuerySection.class)
    @Persist(configKey = "query/fields")
    public String m_fields = "";

    @Widget(title = "Max Results", description = "Maximum number of results to fetch")
    @NumberInputWidget
    @Layout(QuerySection.class)
    @Persist(configKey = "query/max")
    public int m_max = 50;
}
