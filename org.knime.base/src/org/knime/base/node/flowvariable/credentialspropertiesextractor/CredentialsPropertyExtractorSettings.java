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
 * ------------------------------------------------------------------------
 *
 * History
 *   Aug 7, 2010 (wiswedel): created
 */
package org.knime.base.node.flowvariable.credentialspropertiesextractor;

import java.util.List;

import org.knime.core.node.workflow.VariableType.CredentialsType;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings;
import org.knime.core.webui.node.dialog.defaultdialog.setting.filter.StringFilter;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Widget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.choices.ChoicesProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.choices.StringChoicesProvider;

/**
 * @author Carl Witt, KNIME AG, Zurich, Switzerland
 */
@SuppressWarnings("restriction")
public final class CredentialsPropertyExtractorSettings implements DefaultNodeSettings {

    CredentialsPropertyExtractorSettings() {
        // empty constructor for persistence
    }

    CredentialsPropertyExtractorSettings(final DefaultNodeSettingsContext context) {
        m_selectedCredentials = new StringFilter(CredentialsFlowVariables.getAllCredentialsVariables(context));
    }

    @Widget(title = "Credentials Selection", description = """
            Select the credentials flow variables for which to extract properties.
            """)
    @ChoicesProvider(CredentialsFlowVariables.class)
    StringFilter m_selectedCredentials = new StringFilter();

    @Widget(title = "Fail if selected credentials have no user name set", description = """
            Select this to prevent execution of subsequent nodes in case the selected credentials have
            no user name set. This is useful to avoid failures or unauthorized requests.
            """)
    boolean m_failOnEmptyUser;

    @Widget(title = "Fail if selected credentials have no password set", description = """
            Select this to prevent execution of subsequent nodes in case the selected credentials have
            no password set. This is useful to avoid failures or unauthorized requests.
            """)
    boolean m_failOnEmptyPassword;

    @Widget(title = "Fail if selected credentials have no second authentication factor set", description = """
            Select this to prevent execution of subsequent nodes in case the selected credentials have
            no second authentication factor set. This is useful to avoid failures or unauthorized requests.
            """)
    boolean m_failOnEmptySecondAuthenticationFactor;

    /** Select all credentials variables by default. */
    private static final class CredentialsFlowVariables implements StringChoicesProvider {
        @Override
        public List<String> choices(final DefaultNodeSettingsContext context) {
            return getAllCredentialsVariables(context);
        }

        static List<String> getAllCredentialsVariables(final DefaultNodeSettingsContext context) {
            final var credentialVariables = context.getAvailableInputFlowVariables(CredentialsType.INSTANCE);
            return credentialVariables.keySet().stream().toList();

        }
    }
}
