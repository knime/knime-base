/*
 * ------------------------------------------------------------------------
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
 *
 */
package org.knime.base.node.util.sendmail;

import org.knime.base.node.util.sendmail.SendMailConfiguration.ConnectionSecurity;
import org.knime.base.node.util.sendmail.SendMailConfiguration.EMailFormat;
import org.knime.base.node.util.sendmail.SendMailConfiguration.EMailPriority;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.layout.Layout;
import org.knime.node.parameters.layout.Section;
import org.knime.node.parameters.persistence.Persist;
import org.knime.node.parameters.updates.Effect;
import org.knime.node.parameters.updates.Effect.EffectType;
import org.knime.node.parameters.updates.EffectPredicate;
import org.knime.node.parameters.updates.EffectPredicateProvider;
import org.knime.node.parameters.updates.ParameterReference;
import org.knime.node.parameters.updates.ValueReference;
import org.knime.node.parameters.widget.choices.ChoicesProvider;
import org.knime.node.parameters.widget.choices.EnumChoicesProvider;
import org.knime.node.parameters.widget.credentials.Credentials;
import org.knime.node.parameters.widget.credentials.CredentialsWidget;
import org.knime.node.parameters.widget.number.NumberInputWidget;
import org.knime.node.parameters.widget.number.NumberInputWidgetValidation;
import org.knime.node.parameters.widget.number.NumberInputWidgetValidation.MinValidation.IsNonNegativeValidation;
import org.knime.node.parameters.widget.number.NumberInputWidgetValidation.MinValidation.IsPositiveIntegerValidation;
import org.knime.node.parameters.widget.text.TextAreaWidget;
import org.knime.node.parameters.widget.text.TextInputWidget;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.node.parameters.persistence.Persistor;
import org.knime.node.parameters.persistence.NodeParametersPersistor;

/**
 * Settings for the Send Email node.
 *
 * @author Bernd Wiswedel, KNIME AG, Zurich, Switzerland
 */
@SuppressWarnings("restriction")
public final class SendMailNodeSettings implements NodeParameters {

    @Section(title = "SMTP Settings")
    interface SmtpSection {
    }
    
    @Section(title = "Authentication")
    interface AuthSection {
    }
    
    @Section(title = "Email Content")
    interface ContentSection {
    }
    
    @Section(title = "Recipients")
    interface RecipientsSection {
    }
    
    @Section(title = "Advanced")
    interface AdvancedSection {
    }

    // References for predicates
    interface UseAuthenticationRef extends ParameterReference<Boolean> {
    }

    interface UseCredentialsRef extends ParameterReference<Boolean> {
    }

    // Predicate providers
    static final class UseAuthenticationIsTrue implements EffectPredicateProvider {
        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getBoolean(UseAuthenticationRef.class).isTrue();
        }
    }

    static final class AuthAndCredentials implements EffectPredicateProvider {
        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return and(
                i.getBoolean(UseAuthenticationRef.class).isTrue(),
                i.getBoolean(UseCredentialsRef.class).isTrue());
        }
    }

    static final class AuthAndNotCredentials implements EffectPredicateProvider {
        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return and(
                i.getBoolean(UseAuthenticationRef.class).isTrue(),
                i.getBoolean(UseCredentialsRef.class).isFalse());
        }
    }

    // Custom number validations
    static final class PortMaxValidation extends NumberInputWidgetValidation.MaxValidation {
        @Override
        public double getMax() {
            return 65535.0;
        }
    }

    // SMTP Settings
    @Layout(SmtpSection.class)
    @Widget(title = "SMTP Host", description = "The SMTP server hostname")
    @TextInputWidget
    @Persist(configKey = "smtp_host")
    public String smtpHost = "";

    @Layout(SmtpSection.class)
    @Widget(title = "Port", description = "The SMTP server port")
    @NumberInputWidget(minValidation = IsPositiveIntegerValidation.class, maxValidation = PortMaxValidation.class)
    @Persist(configKey = "smtp_port")
    public int smtpPort = 25;

    @Layout(SmtpSection.class)
    @Widget(title = "Connection Security", description = "The security method for SMTP connection")
    @ChoicesProvider(EnumChoicesProvider.class)
    @Persist(configKey = "connection_security")
    public ConnectionSecurity connectionSecurity = ConnectionSecurity.NONE;

    // Authentication
    @Layout(AuthSection.class)
    @Widget(title = "Use authentication", description = "Check if SMTP server requires authentication")
    @Persist(configKey = "use_authentication")
    @ValueReference(UseAuthenticationRef.class)
    public boolean useAuthentication = false;

    @Layout(AuthSection.class)
    @Widget(title = "Use credentials", description = "Use credentials from Credentials input port instead of username/password")
    @Effect(predicate = UseAuthenticationIsTrue.class, type = EffectType.SHOW)
    @Persist(configKey = "use_credentials")
    @ValueReference(UseCredentialsRef.class)
    public boolean useCredentials = false;

    @Layout(AuthSection.class)
    @Widget(title = "Credentials ID", description = "The ID of the credentials to use")
    @TextInputWidget
    @Effect(predicate = AuthAndCredentials.class, type = EffectType.SHOW)
    @Persist(configKey = "credentials_id")
    public String credentialsId = "";

    /** Persistor to map a Credentials widget to legacy separate username/password keys. */
    static final class UsernamePasswordPersistor implements NodeParametersPersistor<Credentials> {
        @Override
        public String[][] getConfigPaths() {
            return new String[][] {{"smtp_user"}, {"smtp_password"}};
        }

        @Override
        public Credentials load(final NodeSettingsRO settings) throws InvalidSettingsException {
            final var user = settings.getString("smtp_user", "");
            final var pass = settings.getString("smtp_password", "");
            return new Credentials(user, pass);
        }

        @Override
        public void save(final Credentials value, final NodeSettingsWO settings) {
            settings.addString("smtp_user", value.getUsername());
            settings.addString("smtp_password", value.getPassword());
        }
    }

    @Layout(AuthSection.class)
    @Widget(title = "Credentials", description = "SMTP authentication credentials")
    @CredentialsWidget(usernameLabel = "Username", passwordLabel = "Password")
    @Effect(predicate = AuthAndNotCredentials.class, type = EffectType.SHOW)
    @Persistor(UsernamePasswordPersistor.class)
    public Credentials smtpCredentials = new Credentials();

    // Recipients
    @Layout(RecipientsSection.class)
    @Widget(title = "From", description = "Sender email address")
    @TextInputWidget
    @Persist(configKey = "from")
    public String from = "";

    @Layout(RecipientsSection.class)
    @Widget(title = "To", description = "Recipient email addresses (separated by semicolon)")
    @TextInputWidget
    @Persist(configKey = "to")
    public String to = "";

    @Layout(RecipientsSection.class)
    @Widget(title = "CC", description = "Carbon copy recipients (separated by semicolon)")
    @TextInputWidget
    @Persist(configKey = "cc")
    public String cc = "";

    @Layout(RecipientsSection.class)
    @Widget(title = "BCC", description = "Blind carbon copy recipients (separated by semicolon)")
    @TextInputWidget
    @Persist(configKey = "bcc")
    public String bcc = "";

    @Layout(RecipientsSection.class)
    @Widget(title = "Reply To", description = "Reply-to email address")
    @TextInputWidget
    @Persist(configKey = "reply_to")
    public String replyTo = "";

    // Email Content
    @Layout(ContentSection.class)
    @Widget(title = "Subject", description = "Email subject line")
    @TextInputWidget
    @Persist(configKey = "subject")
    public String subject = "";

    @Layout(ContentSection.class)
    @Widget(title = "Message Format", description = "Format of the email message")
    @ChoicesProvider(EnumChoicesProvider.class)
    @Persist(configKey = "format")
    public EMailFormat format = EMailFormat.Text;

    @Layout(ContentSection.class)
    @Widget(title = "Priority", description = "Email priority level")
    @ChoicesProvider(EnumChoicesProvider.class)
    @Persist(configKey = "priority")
    public EMailPriority priority = EMailPriority.Normal;

    @Layout(ContentSection.class)
    @Widget(title = "Message Text", description = "Email message content")
    @TextAreaWidget(rows = 10)
    @Persist(configKey = "text")
    public String text = "";

    // Advanced Settings
    @Layout(AdvancedSection.class)
    @Widget(title = "Connection Timeout (ms)", description = "SMTP connection timeout in milliseconds")
    @NumberInputWidget(minValidation = IsNonNegativeValidation.class)
    @Persist(configKey = "smtp_connection_timeout")
    public int smtpConnectionTimeout = 2000;

    @Layout(AdvancedSection.class)
    @Widget(title = "Read Timeout (ms)", description = "SMTP read timeout in milliseconds")
    @NumberInputWidget(minValidation = IsNonNegativeValidation.class)
    @Persist(configKey = "smtp_read_timeout")
    public int smtpReadTimeout = 30000;
}
