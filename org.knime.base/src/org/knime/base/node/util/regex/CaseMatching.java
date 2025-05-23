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
 *
 * History
 *   23 Jun 2023 (carlwitt): created
 */
package org.knime.base.node.util.regex;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.api.NodeSettingsPersistor;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Label;

/**
 * Whether to distinguish between upper-case and lower-case letters during string replacement.
 *
 * @author Carl Witt, KNIME AG, Zurich, Switzerland
 * @since 5.5
 */
@SuppressWarnings("restriction")
public enum CaseMatching {
        /** Respect case when matching strings. */
        @Label("Case sensitive")
        CASESENSITIVE, //
        /** Disregard case when matching strings. */
        @Label("Case insensitive")
        CASEINSENSITIVE;

    /** Recommended default setting. */
    public static final CaseMatching DEFAULT = CASESENSITIVE;

    /** Displayed in dialogs as title for controls. */
    public static final String OPTION_NAME = "Case sensitive";

    /** Displayed in dialogs as help text on controls. */
    public static final String OPTION_DESCRIPTION =
        "Specifies whether matching will distinguish between upper and lower case letters.";

    /**
     * Store the selected option as a boolean value under the key {@code caseSensitive}.
     *
     * This is for compatibility with legacy node settings. However, newer nodes, e.g., String Replacer (Dictionary) use
     * this too for consistency.
     */
    public static final class Persistor implements NodeSettingsPersistor<CaseMatching> {

        static final String CFG_CASE_SENSITIVE = "caseSensitive";

        @Override
        public CaseMatching load(final NodeSettingsRO settings) throws InvalidSettingsException {
            return settings.getBoolean(CFG_CASE_SENSITIVE) ? CaseMatching.CASESENSITIVE : CaseMatching.CASEINSENSITIVE;
        }

        @Override
        public void save(final CaseMatching matchingStrategy, final NodeSettingsWO settings) {
            settings.addBoolean(CFG_CASE_SENSITIVE, matchingStrategy == CaseMatching.CASESENSITIVE);
        }

        /**
         * @since 5.5
         */
        @Override
        public String[][] getConfigPaths() {
            return new String[][]{{CFG_CASE_SENSITIVE}};
        }

    }
}
