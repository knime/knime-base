package org.knime.base.node.misc.jira.conn;

import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.layout.Section;
import org.knime.node.parameters.layout.Layout;
import org.knime.node.parameters.persistence.Persist;
import org.knime.node.parameters.widget.credentials.Credentials;
import org.knime.node.parameters.widget.credentials.CredentialsWidget;
import org.knime.node.parameters.widget.text.TextInputWidget;

@SuppressWarnings("restriction")
public final class JiraConnectorNodeSettings implements NodeParameters {

    @Section(title = "Jira Server") interface Server {}

    @Widget(title = "Base URL", description = "The base URL of your Jira instance, e.g. https://yourcompany.atlassian.net")
    @TextInputWidget @Layout(Server.class) @Persist(configKey = "server/url") public String m_baseUrl = "";

    @Widget(title = "E‑mail / Username", description = "Your Jira account e‑mail or username")
    @TextInputWidget @Layout(Server.class) @Persist(configKey = "auth/email") public String m_email = "";

    @Widget(title = "API Token / Password", description = "API token or password for the account")
    @CredentialsWidget @Layout(Server.class) @Persist(configKey = "auth/token") public Credentials m_token = new Credentials();
}
