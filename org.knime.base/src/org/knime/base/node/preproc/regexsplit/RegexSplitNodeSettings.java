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
 *   15 Oct 2023 (jasper): created
 */
package org.knime.base.node.preproc.regexsplit;

import org.knime.base.node.preproc.regexsplit.OutputSettings.OutputGroupLabelMode;
import org.knime.core.data.StringValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings;
import org.knime.core.webui.node.dialog.defaultdialog.layout.After;
import org.knime.core.webui.node.dialog.defaultdialog.layout.Layout;
import org.knime.core.webui.node.dialog.defaultdialog.layout.Section;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.field.DefaultProvider;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.field.NodeSettingsPersistorWithConfigKey;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.field.Persist;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.field.settingsmodel.SettingsModelBooleanPersistor;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.field.settingsmodel.SettingsModelStringPersistor;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ChoicesWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Label;
import org.knime.core.webui.node.dialog.defaultdialog.widget.TextInputWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ValueSwitchWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Widget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.choices.ColumnChoicesProviderUtil;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.Effect;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.Effect.EffectType;

/**
 * Settings class for the String Splitter (Regex) (formerly known as Regex Split)
 *
 * @author Jasper Krauter, KNIME GmbH, Konstanz, Germany
 */
@SuppressWarnings({"restriction", "squid:S3052"}) // Pending API, initialise with defaults
final class RegexSplitNodeSettings implements DefaultNodeSettings {

    RegexSplitNodeSettings() {
        //
    }

    RegexSplitNodeSettings(final DefaultNodeSettingsContext context) {
        var tableSpec = context.getDataTableSpec(0).orElse(null);
        if (tableSpec != null) {
            for (var spec : tableSpec) {
                if (spec.getType().isCompatible(StringValue.class)) {
                    m_column = spec.getName();
                }
            }
        }
    }

    // Layout

    interface DialogSections {
        @Section(title = "Splitting")
        interface Splitting {
        }

        @Section(title = "Splitting (Advanced)", advanced = true)
        @After(DialogSections.Splitting.class)
        interface SplittingAdvanced {
        }

        @Section(title = "Output")
        @After(DialogSections.SplittingAdvanced.class)
        interface Output {
        }
    }

    // Splitting

    @Layout(DialogSections.Splitting.class)
    @Widget(title = "String column", description = "Choose the column containing the strings to split")
    @ChoicesWidget(choices = ColumnChoicesProviderUtil.StringColumnChoicesProvider.class)
    @Persist(configKey = "column", customPersistor = SettingsModelStringPersistor.class)
    String m_column;

    @Layout(DialogSections.Splitting.class)
    @Widget(title = "Pattern", description = """
            Define a pattern according to which the input string will be split. The capture groups that are defined in
            this pattern will correspond to the output values. A group can be defined in one of two ways:
            <ul>
                <li>For a named group, define <tt>(?&lt;groupName&gt;pattern)</tt>,
                where <tt>groupName</tt> is the name of the group and <tt>pattern</tt> can be replaced by any regular
                expression that should be matched. Note that group names need to start with a letter and may
                contain only letters and digits, no spaces.</li>
                <li>For an unnamed capture group, simply use parenthesis around your pattern:
                <tt>(pattern)</tt>, where again <tt>pattern</tt> can be replaced by any
                pattern. Unnamed capture groups are simply identified by their position in the pattern string, and they
                are enumerated starting at 1.</li>
            </ul>
            If you want to use non-capturing groups, construct them with
            <tt>(?:pattern)</tt>
            """)
    @TextInputWidget(minLength = 1)
    @Persist(configKey = "pattern", customPersistor = SettingsModelStringPersistor.class)
    String m_pattern = "(.*)";

    enum CaseMatching {
            /** Respect case when matching strings. */
            @Label("Case sensitive")
            CASESENSITIVE, //
            /** Disregard case when matching strings. */
            @Label("Case insensitive")
            CASEINSENSITIVE;

        /** persists the case sensitivity as a boolean indicating whether the matching is case-INsensitive */
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

    @Layout(DialogSections.Splitting.class)
    @Widget(title = "Case sensitive", description = """
            Specifies whether matching will distinguish between upper and lower case letters. <br />
            By default, case-insensitive matching assumes that only characters in the US-ASCII charset are being
            matched. Unicode-aware case-insensitive matching can be enabled by enabling
            <i>Unicode-aware case folding</i>. <br />
            Matching case-insensitive may impose a slight performance penalty.
            """)
    @ValueSwitchWidget
    @Persist(configKey = "isCaseInsensitive", customPersistor = CaseMatching.CaseInsensitivityPersistor.class)
    CaseMatching m_caseMatching = CaseMatching.CASESENSITIVE;

    @Layout(DialogSections.Splitting.class)
    @Widget(title = "Require whole string to match", description = """
            If enabled, the provided pattern must match the whole string in order to return any results. Otherwise, the
            first match in the input string is used.
            """)
    @Persist(optional = true)
    boolean m_requireWholeMatch = true;

    enum NoMatchBehaviour {
            @Label("Insert missing value")
            INSERT_MISSING, //
            @Label("Insert empty string")
            INSERT_EMPTY, //
            @Label("Fail")
            FAIL;
    }

    @Layout(DialogSections.Splitting.class)
    @Widget(title = "If pattern does not match", description = """
            Define what to do if a pattern can't be matched to the input string:
            <ul>
                <li><i>Insert missing value</i> puts missing cell(s) in place of the output column(s).
                    The node will emit a warning when an input string doesn't match.</li>
                <li><i>Insert empty string</i> puts empty string(s) in place of the output column(s).
                    The node will emit a warning when an input string doesn't match.</li>
                <li><i>Fail</i> causes the node to fail if one of the inputs can not be matched against the pattern.
                </li>
            </ul>
            """)
    @Persist(optional = true)
    @ValueSwitchWidget
    NoMatchBehaviour m_noMatchBehaviour = NoMatchBehaviour.INSERT_MISSING;

    // Legacy / Advanced Splitting settings

    @Layout(DialogSections.SplittingAdvanced.class)
    @Widget(title = "Enable Unix lines mode", advanced = true,
        description = "In this mode, only the '\\n' line terminator is recognized in the behavior of ., ^, and $.")
    @Persist(configKey = "isUnixLines", customPersistor = SettingsModelBooleanPersistor.class)
    boolean m_isUnixLines = false;

    @Layout(DialogSections.SplittingAdvanced.class)
    @Widget(title = "Enable multiline mode (^ and $ match at the beginning / end of a line)", advanced = true,
        description = """
                In multiline mode the expressions ^ and $ match just after or just before, respectively, a line
                terminator or the end of the input sequence. By default these expressions only match at the beginning
                and the end of the entire input sequence.""")
    @Persist(configKey = "isMultiLine", customPersistor = SettingsModelBooleanPersistor.class)
    boolean m_isMultiLine = false;

    @Layout(DialogSections.SplittingAdvanced.class)
    @Widget(title = "Enable dotall mode (Dot . also matches newline characters)", advanced = true, description = """
            In dotall mode, the expression . matches any character, including a line terminator. By default this
            expression does not match line terminators.""")
    @Persist(configKey = "isDotAll", customPersistor = SettingsModelBooleanPersistor.class)
    boolean m_isDotAll = false;

    @Layout(DialogSections.SplittingAdvanced.class)
    @Widget(title = "Enable Unicode-aware case folding", advanced = true, description = """
            When this is enabled then case-insensitive matching, when enabled, is done in a manner consistent with the
            Unicode Standard. By default, case-insensitive matching assumes that only characters in the US-ASCII charset
            are being matched. <br />
            Enabling this may impose a performance penalty.""")
    @Persist(configKey = "isUniCodeCase", customPersistor = SettingsModelBooleanPersistor.class)
    boolean m_isUnicodeCase = false;

    @Layout(DialogSections.SplittingAdvanced.class)
    @Widget(title = "Enable canonical equivalence", advanced = true, description = """
            When enabled, two characters will be considered to match if, and only if, their full canonical
            decompositions match. The expression "a\\u030A", for example, will match the string "\\u00E5" when this is
            enabled. By default, matching does not take canonical equivalence into account.""")
    @Persist(configKey = "isCanonEQ", customPersistor  = SettingsModelBooleanPersistor.class)
    boolean m_isCanonEQ = false;

    @Layout(DialogSections.SplittingAdvanced.class)
    @Widget(title = "Enable Unicode character classes", advanced = true, description = """
            When enabled, the (US-ASCII only) <i>Predefined character classes</i> and <i>POSIX character classes</i> are
            in conformance with the Unicode Standard. <br />
            Enabling this may impose a performance penalty.""")
    @Persist(configKey = "isUnicodeCharacterClass", customPersistor = SettingsModelBooleanPersistor.class,
        optional = true)
    boolean m_isUnicodeCharacterClass = false;

    /**
     * This setting serves the purpose to not break backwards-compatibility while still using sensible defaults going
     * forward. For regex-groups it is convention, that index 0 is reserved for the whole string, and indices 1 to n are
     * the actual capture groups. However, in earlier iterations of this node, the first capture group started at 0.
     */
    @Layout(DialogSections.SplittingAdvanced.class)
    @Widget(title = "Start group index counting from zero", advanced = true, description = """
            If enabled, the indices of non-named capturing groups start at zero instead of one.
            This setting is not meant to be manually enabled, but exists solely for the purpose of
            backwards-compatibility. Earlier versions of this node have this enabled to reflect how the node used to
            behave.""")
    @Persist(defaultProvider = TrueProvider.class) // If it's an old node instance, default to true
    @Effect(predicate = OutputGroupLabelMode.IsCaptureGroupNames.class, type = EffectType.SHOW)
    boolean m_decrementGroupIndexByOne = false;

    // Output Settings

    @Persist(defaultProvider = OutputSettings.LegacyProvider.class)
    OutputSettings m_output = new OutputSettings();

    // Utility

    static class TrueProvider implements DefaultProvider<Boolean> {
        @Override
        public Boolean getDefault() {
            return true;
        }
    }

    // ===== Please don't look below this line

    /**
     * This setting doesn't make sense, since in Literal mode, no capture groups are possible. Setting is included only to avoid warnings when loading old nodes, since
     * they have this setting saved with them. The \@Widget annotation is missing to not include this setting in
     * the node description or the dialog.
     *
     * @deprecated
     */
    @Persist(configKey = "isLiteral", customPersistor  = SettingsModelBooleanPersistor.class, optional = true)
    @Deprecated // NOSONAR: Deprecated since the beginning of this classes existence
    boolean m_isLiteral = false; // NOSONAR: kept for backwards-compatibility

    /**
     * The story is a similar one for this setting, except that here the implementation of the
     * {@link CaptureGroupExtractor} is just not up to the task. Having this setting enabled is also a very unlikely
     * edge case, and in prior iterations of this node it also lead to errors when a comment had parenthesis in it.
     *
     * @deprecated
     */
    @Persist(configKey = "isComments", customPersistor = SettingsModelBooleanPersistor.class, optional = true)
    @Deprecated // NOSONAR: Deprecated since the beginning of this classes existence
    boolean m_isComments = false; // NOSONAR: kept for backwards-compatibility

}
