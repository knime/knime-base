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
 * ------------------------------------------------------------------------
 */

package org.knime.base.node.util.sendmail;

import java.net.URI;
import java.net.URISyntaxException;

import org.knime.base.node.util.sendmail.SendMailConfiguration.ConnectionSecurity;
import org.knime.base.node.util.sendmail.SendMailConfiguration.EMailFormat;
import org.knime.base.node.util.sendmail.SendMailConfiguration.EMailPriority;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.webui.node.dialog.defaultdialog.internal.file.FileReaderWidget;
import org.knime.core.webui.node.dialog.defaultdialog.internal.file.FileSelection;
import org.knime.filehandling.core.connections.FSCategory;
import org.knime.filehandling.core.connections.FSLocation;
import org.knime.filehandling.core.connections.FSLocationUtil;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.WidgetGroup;
import org.knime.node.parameters.array.ArrayWidget;
import org.knime.node.parameters.layout.After;
import org.knime.node.parameters.layout.Layout;
import org.knime.node.parameters.layout.Section;
import org.knime.node.parameters.migration.ConfigMigration;
import org.knime.node.parameters.migration.LoadDefaultsForAbsentFields;
import org.knime.node.parameters.migration.Migration;
import org.knime.node.parameters.migration.NodeParametersMigration;
import org.knime.node.parameters.persistence.NodeParametersPersistor;
import org.knime.node.parameters.persistence.Persist;
import org.knime.node.parameters.persistence.Persistor;
import org.knime.node.parameters.updates.Effect;
import org.knime.node.parameters.updates.Effect.EffectType;
import org.knime.node.parameters.updates.EffectPredicate;
import org.knime.node.parameters.updates.EffectPredicateProvider;
import org.knime.node.parameters.updates.ParameterReference;
import org.knime.node.parameters.updates.ValueReference;
import org.knime.node.parameters.widget.choices.RadioButtonsWidget;
import org.knime.node.parameters.widget.credentials.Credentials;
import org.knime.node.parameters.widget.credentials.CredentialsWidget;
import org.knime.node.parameters.widget.number.NumberInputWidget;
import org.knime.node.parameters.widget.number.NumberInputWidgetValidation.MinValidation.IsNonNegativeValidation;
import org.knime.node.parameters.widget.text.TextAreaWidget;
import org.knime.node.parameters.widget.text.TextInputWidget;

/**
 * Node parameters for Send Email.
 *
 * @author Daniel Bogenrieder, KNIME GmbH, Konstanz, Germany
 * @author AI Migration Pipeline v1.0
 */
@SuppressWarnings("restriction") // Modern UI classes may be marked as internal during development
@LoadDefaultsForAbsentFields
class SendMailNodeParameters implements NodeParameters {

    @Section(title = "Mail")
    interface MailSection {
    }

    @Section(title = "Attachments")
    @After(MailSection.class)
    interface AttachmentsSection {
    }

    @Section(title = "Mail Host (SMTP)")
    @After(AttachmentsSection.class)
    interface SMTPSection {
    }

    // Mail Section
    @Persist(configKey = "to")
    @Widget(title = "To", description = "Email address of the intended recipient")
    @TextInputWidget
    @Layout(MailSection.class)
    String m_to = "";

    @Persist(configKey = "cc")
    @Widget(title = "CC", description = "Carbon Copy: Additional recipients who may also be interested in the message.")
    @TextInputWidget
    @Layout(MailSection.class)
    String m_cc = "";

    @Persist(configKey = "bcc")
    @Widget(title = "BCC",
        description = "Blind Carbon Copy: Additional recipients to receive discrete copies of the message (TO and CC recipients will not see who is BCC'd).")
    @TextInputWidget
    @Layout(MailSection.class)
    String m_bcc = "";

    @Persist(configKey = "replyTo")
    @Widget(title = "Reply to",
        description = "Email address that the reply message is sent (different email address than the FROM)")
    @TextInputWidget
    @Layout(MailSection.class)
    String m_replyTo = "";

    @Persist(configKey = "subject")
    @Widget(title = "Subject", description = "The Subject of your message.")
    @TextInputWidget
    @Layout(MailSection.class)
    String m_subject = "";

    @Persist(configKey = "text")
    @Widget(title = "Body",
        description = "The body of your message. Note that you can reference flow variables in your message.")
    @TextAreaWidget
    @Layout(MailSection.class)
    String m_text = "";

    @Persist(configKey = "emailPriority")
    @Widget(title = "Priority",
        description = "The X-Priority flag in the mail header. Standard email clients display the email in different styles depending on the flag.")
    @RadioButtonsWidget
    @Layout(MailSection.class)
    EMailPriority m_priority = EMailPriority.Normal;

    @Persist(configKey = "emailFormat")
    @Widget(title = "Text/HTML",
        description = "Choose whether your message should be interpreted as plain text or HTML. Note, if HTML is chosen you will have to enter HTML tags manually.")
    @RadioButtonsWidget
    @Layout(MailSection.class)
    EMailFormat m_format = EMailFormat.Text;

    // Attachments Section
    static class AttachmentFile implements WidgetGroup {
        @Widget(title = "File Path", description = "Select the file to attach to the email")
        @FileReaderWidget
        FileSelection m_filePath = new FileSelection(new FSLocation(FSCategory.LOCAL, ""));
    }

    static class AttachmentsPersistor implements NodeParametersPersistor<AttachmentFile[]> {

        @Override
        public AttachmentFile[] load(final NodeSettingsRO settings) throws InvalidSettingsException {
            String[] urlAsArray = settings.getStringArray("attachedURLs", new String[0]);
            AttachmentFile[] attachments = new AttachmentFile[urlAsArray.length];
            for (int i = 0; i < urlAsArray.length; i++) {
                attachments[i] = new AttachmentFile();
                attachments[i].m_filePath = new FileSelection(stringToFSLocation(urlAsArray[i]));
            }
            return attachments;
        }

        private static FSLocation stringToFSLocation(final String pathOrUrl) {
            try {
                var u = new URI(pathOrUrl);
                if (u.getScheme() != null) {
                    // For KNIME URLs, always preserve as custom URL to maintain the full URL
                    if ("knime".equals(u.getScheme())) {
                        return new FSLocation(FSCategory.CUSTOM_URL, "10000", pathOrUrl);
                    }
                    // For other URLs, try to create from URL first
                    try {
                        return FSLocationUtil.createFromURL(pathOrUrl);
                    } catch (Exception e) {
                        // If that fails, treat as custom URL (preserving the full URL)
                        return new FSLocation(FSCategory.CUSTOM_URL, "10000", pathOrUrl);
                    }
                } else {
                    // Regular file path - use local category
                    return new FSLocation(FSCategory.LOCAL, pathOrUrl);
                }
            } catch (URISyntaxException e) {
                // Not a valid URI, treat as local file path
                return new FSLocation(FSCategory.LOCAL, pathOrUrl);
            }
        }

        @Override
        public void save(AttachmentFile[] attachments, final NodeSettingsWO settings) {
            if (attachments == null) {
                attachments = new AttachmentFile[0];
            }
            String[] urlAsArray = new String[attachments.length];
            for (int i = 0; i < attachments.length; i++) {
                urlAsArray[i] = attachments[i] != null && attachments[i].m_filePath != null
                    ? fsLocationToString(attachments[i].m_filePath.getFSLocation()) : "";
            }
            settings.addStringArray("attachedURLs", urlAsArray);
        }

        private static String fsLocationToString(final FSLocation location) {
            // For custom URLs (including knime:// URLs), return the full URL
            if (location.getFSCategory() == FSCategory.CUSTOM_URL) {
                return location.getPath(); // The full URL is stored in the path
            }
            // For local files, return just the path
            return location.getPath();
        }

        @Override
        public String[][] getConfigPaths() {
            return new String[][]{{"attachedURLs"}};
        }
    }

    @Persistor(AttachmentsPersistor.class)
    @Widget(title = "Attachments",
        description = "Select files to attach to the email. Each attachment will be sent with the email.")
    @ArrayWidget(addButtonText = "Add Attachment", elementTitle = "Attachment")
    @Layout(AttachmentsSection.class)
    AttachmentFile[] m_attachments = new AttachmentFile[0];

    // SMTP Section
    @Persist(configKey = "smtpHost")
    @Widget(title = "SMTP host", description = "The domain name of your SMTP server.")
    @TextInputWidget
    @Layout(SMTPSection.class)
    String m_smtpHost = "";

    @Persist(configKey = "smtpPort")
    @Widget(title = "SMTP port",
        description = "The port on which to connect to your SMTP server (25, 465, 587 are popular choices, but check with your mail provider).")
    @NumberInputWidget(minValidation = IsNonNegativeValidation.class)
    @Layout(SMTPSection.class)
    int m_smtpPort = 25;

    @Persist(configKey = "from")
    @Widget(title = "From",
        description = "An alternate return address for this message (Only if supported by your SMTP host).")
    @TextInputWidget
    @Layout(SMTPSection.class)
    String m_from = "";

    @Persist(configKey = "smtpConnectionTimeout")
    @Widget(title = "Connection timeout (ms)",
        description = "The timeout in milliseconds for establishing a connection to the SMTP host.")
    @NumberInputWidget(minValidation = IsNonNegativeValidation.class)
    @Layout(SMTPSection.class)
    int m_smtpConnectionTimeout = 2000;

    @Persist(configKey = "smtpReadTimeout")
    @Widget(title = "Read timeout (ms)", description = "The timeout in milliseconds for reading from the SMTP host.")
    @NumberInputWidget(minValidation = IsNonNegativeValidation.class)
    @Layout(SMTPSection.class)
    int m_smtpReadTimeout = 30000;

    @Persist(configKey = "useAuthentication")
    @Widget(title = "SMTP host authentication",
        description = "Check this box if your SMTP host requires authentication.")
    @ValueReference(AuthenticationRef.class)
    @Layout(SMTPSection.class)
    boolean m_smtpAuthentication = false;

    @Migration(UseWorkflowCredentialsMigration.class)
    @Persist(configKey = "useCredentials")
    @Widget(title = "Use workflow credentials",
        description = "Check this box to use workflow credentials instead of entering username and password directly.")
    @Effect(predicate = AuthenticationEnabledPredicate.class, type = EffectType.SHOW)
    @ValueReference(UseWorkflowCredentialsRef.class)
    @Layout(SMTPSection.class)
    boolean m_useWorkflowCredentials = false;

    @Migration(CredentialsIdMigration.class)
    @Persist(configKey = "credentialsId")
    @Widget(title = "Workflow credentials",
        description = "Select the workflow credentials to use for SMTP authentication.")
    @Effect(predicate = WorkflowCredentialsEnabledPredicate.class, type = EffectType.SHOW)
    @Layout(SMTPSection.class)
    String m_workflowCredentialsId = "";

    // SMTP credentials (saved as individual username/password)
    static class SmtpCredentialsPersistor implements NodeParametersPersistor<Credentials> {
        @Override
        public Credentials load(final NodeSettingsRO settings) throws InvalidSettingsException {
            // Load username - always available
            String username = settings.getString("smtpUser", "");
            String password = "";

            // Only load password when individual credentials are being used
            // (not when workflow credentials are being used)
            boolean useAuthentication = settings.getBoolean("useAuthentication", false);
            boolean useCredentials = settings.getBoolean("useCredentials", false);

            if (useAuthentication && !useCredentials) {
                // Load password using the same logic as SendMailConfiguration
                // Try old format first (until v2.11 excl)
                try {
                    password = settings.getString("smtpPassword");
                } catch (InvalidSettingsException ise) { // NOSONAR
                    // Use newer encrypted format
                    password = settings.getPassword("smtpPasswordWeaklyEncrypted", "0=d#Fs64h", "");
                }
            }

            return new Credentials(username, password);
        }

        @Override
        public void save(final Credentials credentials, final NodeSettingsWO settings) {
            if (credentials != null) {
                // Always save username
                settings.addString("smtpUser", credentials.getUsername());

                // Only save password if not using workflow credentials
                // Check if workflow credentials are being used (useCredentials = true)
                // Note: This check assumes the Modern UI framework calls this persistor
                // only when the credentials field is active (i.e., when useCredentials = false)
                if (credentials.getPassword() != null && !credentials.getPassword().isEmpty()) {
                    // Use the same encrypted format as the original configuration
                    settings.addPassword("smtpPasswordWeaklyEncrypted", "0=d#Fs64h", credentials.getPassword());
                }
            } else {
                // Save empty values when credentials object is null
                settings.addString("smtpUser", "");
                // Don't save empty password to avoid overwriting existing encrypted passwords
            }
        }

        @Override
        public String[][] getConfigPaths() {
            return new String[][]{{"smtpUser"}, {"smtpPassword"}, // Old format for backwards compatibility
                {"smtpPasswordWeaklyEncrypted"} // New encrypted format
            };
        }
    }

    @Widget(title = "SMTP Authentication", description = "Enter your SMTP username and password.")
    @Effect(predicate = UsernamePasswordEnabledPredicate.class, type = EffectType.SHOW)
    @CredentialsWidget(usernameLabel = "Username", passwordLabel = "Password")
    @Persistor(SmtpCredentialsPersistor.class)
    @Layout(SMTPSection.class)
    Credentials m_smtpCredentials = new Credentials();

    @Persist(configKey = "connectionSecurity")
    @Widget(title = "Connection security",
        description = "Choose a method for secure communication with your SMTP Host. SSL or STARTTLS will use these methods respectively, while None will send your data unencrypted over the network.")
    @RadioButtonsWidget
    @Layout(SMTPSection.class)
    ConnectionSecurity m_connectionSecurity = ConnectionSecurity.NONE;

    // Parameter references for predicates
    static final class AuthenticationRef implements ParameterReference<Boolean> {
    }

    static final class UseWorkflowCredentialsRef implements ParameterReference<Boolean> {
    }

    // Predicate classes for authentication UI
    static final class WorkflowCredentialsEnabledPredicate implements EffectPredicateProvider {
        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getBoolean(AuthenticationRef.class).isTrue()
                .and(i.getBoolean(UseWorkflowCredentialsRef.class).isTrue());
        }
    }

    static final class UsernamePasswordEnabledPredicate implements EffectPredicateProvider {
        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getBoolean(AuthenticationRef.class).isTrue()
                .and(i.getBoolean(UseWorkflowCredentialsRef.class).isFalse());
        }
    }

    // Migration classes for workflow credentials fields
    static class UseWorkflowCredentialsMigration implements NodeParametersMigration<Boolean> {
        @Override
        public java.util.List<ConfigMigration<Boolean>> getConfigMigrations() {
            return java.util.List.of(
                ConfigMigration.builder(this::loadUseCredentials).withDeprecatedConfigPath("useCredentials").build(),
                // Default migration
                ConfigMigration.builder(settings -> false).build());
        }

        private Boolean loadUseCredentials(final NodeSettingsRO settings) throws InvalidSettingsException {
            return settings.getBoolean("useCredentials", false);
        }
    }

    static class CredentialsIdMigration implements NodeParametersMigration<String> {
        @Override
        public java.util.List<ConfigMigration<String>> getConfigMigrations() {
            return java.util.List.of(
                ConfigMigration.builder(this::loadCredentialsId).withDeprecatedConfigPath("credentialsId").build(),
                // Default migration
                ConfigMigration.builder(settings -> "").build());
        }

        private String loadCredentialsId(final NodeSettingsRO settings) throws InvalidSettingsException {
            return settings.getString("credentialsId", "");
        }
    } // Predicate classes for showing/hiding authentication fields

    static class AuthenticationEnabledPredicate implements EffectPredicateProvider {
        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getBoolean(AuthenticationRef.class).isTrue();
        }
    }

}
