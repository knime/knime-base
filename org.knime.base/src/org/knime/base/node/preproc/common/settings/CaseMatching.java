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
 *   17 Jan 2024 (jasper): created
 */
package org.knime.base.node.preproc.common.settings;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.NodeSettingsPersistorWithConfigKey;
import org.knime.core.webui.node.dialog.defaultdialog.rule.OneOfEnumCondition;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Label;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ValueSwitchWidget;

/**
 * Can be used for UI elements to specify whether letter case matters or not. Intended to be used as a
 * {@link ValueSwitchWidget}.
 *
 * @author Jasper Krauter, KNIME GmbH, Konstanz, Germany
 */
@SuppressWarnings("restriction")
public enum CaseMatching {
        /** Respect case when matching strings. */
        @Label("Case sensitive")
        CASESENSITIVE, //
        /** Disregard case when matching strings. */
        @Label("Case insensitive")
        CASEINSENSITIVE;

    public static final CaseMatching DEFAULT = CASESENSITIVE;

    /** Displayed in dialogs as title for controls. */
    public static final String OPTION_NAME = "Case sensitive";

    /** Displayed in dialogs as help text on controls. */
    public static final String OPTION_DESCRIPTION =
        "Specifies whether matching will distinguish between upper and lower case letters.";


    /** Condition to test whether the matching is case-sensitive */
    public static final class IsCaseSensitiveCondition extends OneOfEnumCondition<CaseMatching> {
        @Override
        public CaseMatching[] oneOf() {
            return new CaseMatching[]{CASESENSITIVE};
        }
    }

    /** Condition to test whether the matching is case-INsensitive */
    public static final class IsNotCaseSensitiveCondition extends OneOfEnumCondition<CaseMatching> {
        @Override
        public CaseMatching[] oneOf() {
            return new CaseMatching[]{CASEINSENSITIVE};
        }
    }

    /**
     * Persists the case sensitivity as a boolean indicating whether the matching is case-sensitive. Intended to be used
     * if the setting was previously persisted as a boolean
     */
    public static final class CaseSensitivityPersistor extends NodeSettingsPersistorWithConfigKey<CaseMatching> {
        @Override
        public CaseMatching load(final NodeSettingsRO settings) throws InvalidSettingsException {
            return settings.getBoolean(getConfigKey()) ? CASESENSITIVE : CASEINSENSITIVE;
        }

        @Override
        public void save(final CaseMatching cm, final NodeSettingsWO settings) {
            settings.addBoolean(getConfigKey(), cm == CASESENSITIVE);
        }

        @Override
        public String[] getConfigKeys() {
            return new String[]{getConfigKey()};
        }
    }

    /**
     * Persists the case sensitivity as a boolean indicating whether the matching is case-INsensitive. Intended to be
     * used if the setting was previously persisted as a boolean
     */
    public static final class CaseInsensitivityPersistor extends NodeSettingsPersistorWithConfigKey<CaseMatching> {
        @Override
        public CaseMatching load(final NodeSettingsRO settings) throws InvalidSettingsException {
            return settings.getBoolean(getConfigKey()) ? CASEINSENSITIVE : CASESENSITIVE;
        }

        @Override
        public void save(final CaseMatching cm, final NodeSettingsWO settings) {
            settings.addBoolean(getConfigKey(), cm == CASEINSENSITIVE);
        }

        @Override
        public String[] getConfigKeys() {
            return new String[]{getConfigKey()};
        }
    }
}