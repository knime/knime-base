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

import org.knime.base.node.preproc.common.settings.CaseMatching;
import org.knime.base.node.preproc.regexsplit.OutputSettings.OutputGroupLabelMode;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings;
import org.knime.core.webui.node.dialog.defaultdialog.layout.After;
import org.knime.core.webui.node.dialog.defaultdialog.layout.Layout;
import org.knime.core.webui.node.dialog.defaultdialog.layout.Section;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.field.DefaultProvider;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.field.Persist;
import org.knime.core.webui.node.dialog.defaultdialog.rule.Effect;
import org.knime.core.webui.node.dialog.defaultdialog.rule.Effect.EffectType;
import org.knime.core.webui.node.dialog.defaultdialog.rule.Signal;
import org.knime.core.webui.node.dialog.defaultdialog.rule.TrueCondition;
import org.knime.core.webui.node.dialog.defaultdialog.rule.Xor;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ChoicesWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.TextInputWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ValueSwitchWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Widget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.choices.ColumnChoicesProviderUtil;

/**
 * Settings class for the String Splitter (Regex) (formerly known as Regex Split)
 *
 * @author Jasper Krauter, KNIME GmbH, Konstanz, Germany
 */
@SuppressWarnings({"restriction", "squid:S3052"}) // Pending API, initialise with defaults
final class RegexSplitNodeSettings implements DefaultNodeSettings {

    // Layout

    interface DialogSections {
        @Section(title = "Splitting")
        interface Splitting {
        }

        @Section(title = "Splitting (Legacy)", advanced = true)
        @After(DialogSections.Splitting.class)
        interface SplittingLegacy {
        }

        @Section(title = "Output")
        @After(DialogSections.SplittingLegacy.class)
        interface Output {
        }
    }

    @Layout(DialogSections.Splitting.class)
    @Widget(title = "Target Column", description = "Choose the column containing the strings to split")
    @ChoicesWidget(choices = ColumnChoicesProviderUtil.StringColumnChoicesProvider.class)
    @Persist(configKey = "column", settingsModel = SettingsModelString.class)
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
    @Persist(configKey = "pattern", settingsModel = SettingsModelString.class)
    String m_pattern = "(.*)";

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

    /**
     * Legacy match settings
     */

    @Layout(DialogSections.SplittingLegacy.class)
    @Widget(title = "Enable Unix lines mode", advanced = true,
        description = "In this mode, only the '\\n' line terminator is recognized in the behavior of ., ^, and $.")
    @Persist(configKey = "isUnixLines", settingsModel = SettingsModelBoolean.class)
    boolean m_isUnixLines = false;

    @Layout(DialogSections.SplittingLegacy.class)
    @Widget(title = "Enable multiline mode (^ and $ match at the beginning / end of a line)", advanced = true,
        description = """
                In multiline mode the expressions ^ and $ match just after or just before, respectively, a line
                terminator or the end of the input sequence. By default these expressions only match at the beginning
                and the end of the entire input sequence.""")
    @Persist(configKey = "isMultiLine", settingsModel = SettingsModelBoolean.class)
    boolean m_isMultiLine = false;

    @Layout(DialogSections.SplittingLegacy.class)
    @Widget(title = "Enable dotall mode (Dot . also matches newline characters)", advanced = true, description = """
            In dotall mode, the expression . matches any character, including a line terminator. By default this
            expression does not match line terminators.""")
    @Persist(configKey = "isDotAll", settingsModel = SettingsModelBoolean.class)
    boolean m_isDotAll = false;

    @Layout(DialogSections.SplittingLegacy.class)
    @Widget(title = "Enable Unicode-aware case folding", advanced = true, description = """
            When this is enabled then case-insensitive matching, when enabled, is done in a manner consistent with the
            Unicode Standard. By default, case-insensitive matching assumes that only characters in the US-ASCII charset
            are being matched. <br />
            Enabling this may impose a performance penalty.""")
    @Persist(configKey = "isUniCodeCase", settingsModel = SettingsModelBoolean.class)
    boolean m_isUnicodeCase = false;

    @Layout(DialogSections.SplittingLegacy.class)
    @Widget(title = "Enable canonical equivalence", advanced = true, description = """
            When enabled, two characters will be considered to match if, and only if, their full canonical
            decompositions match. The expression "a\\u030A", for example, will match the string "\\u00E5" when this is
            enabled. By default, matching does not take canonical equivalence into account.""")
    @Persist(configKey = "isCanonEQ", settingsModel = SettingsModelBoolean.class)
    boolean m_isCanonEQ = false;

    @Layout(DialogSections.SplittingLegacy.class)
    @Widget(title = "Enable Unicode character classes", advanced = true, description = """
            When enabled, the (US-ASCII only) <i>Predefined character classes</i> and <i>POSIX character classes</i> are
            in conformance with the Unicode Standard. <br />
            Enabling this may impose a performance penalty.""")
    @Persist(configKey = "isUnicodeCharacterClass", settingsModel = SettingsModelBoolean.class, optional = true)
    boolean m_isUnicodeCharacterClass = false;

    /**
     * This setting serves the purpose to not break backwards-compatibility while still using sensible defaults going
     * forward. For regex-groups it is convention, that index 0 is reserved for the whole string, and indices 1 to n are
     * the actual capture groups. However, in earlier iterations of this node, the first capture group started at 0.
     */
    @Layout(DialogSections.SplittingLegacy.class)
    @Widget(title = "Start group index counting from zero", advanced = true, description = """
            If enabled, the indices of non-named capturing groups start at zero instead of one.
            This setting is not meant to be manually enabled, but exists solely for the purpose of
            backwards-compatibility. Earlier versions of this node have this enabled to reflect how the node used to
            behave.""")
    @Persist(defaultProvider = TrueProvider.class) // If it's an old node instance, default to true
    @Effect(signals = OutputGroupLabelMode.IsCaptureGroupNames.class, type = EffectType.SHOW)
    boolean m_decrementGroupIndexByOne = false;

    /**
     * Output settings
     */

    @Widget
    @Persist(defaultProvider = OutputSettings.LegacyProvider.class)
    OutputSettings m_output = new OutputSettings();

    static class TrueProvider implements DefaultProvider<Boolean> {
        @Override
        public Boolean getDefault() {
            return true;
        }
    }

    // ===== Please don't look below this line

    interface SignalThatsUsedToHideFlagSettings {
    }

    /**
     * This setting doesn't make sense, since in Literal mode, no capture groups are possible. Don't include this in the
     * dialog (therefore the \@Effect hack). Setting is included only to avoid warnings when loading old nodes, since
     * they have this setting saved with them. Also, the \@Widget annotation is missing to not include this setting in
     * the node description.
     *
     * @deprecated
     */
    @Persist(configKey = "isLiteral", settingsModel = SettingsModelBoolean.class, optional = true)
    @Deprecated // NOSONAR: Deprecated since the beginning of this classes existence
    @Signal(id = SignalThatsUsedToHideFlagSettings.class, condition = TrueCondition.class) // bind signal to avoid NPE
    @Effect(signals = {SignalThatsUsedToHideFlagSettings.class, SignalThatsUsedToHideFlagSettings.class},
        operation = Xor.class, type = EffectType.SHOW) // xor on the same signal ensures that this is never shown
    boolean m_isLiteral = false; // NOSONAR: kept for backwards-compatibility

    /**
     * The story is a similar one for this setting, except that here the implementation of the
     * {@link CaptureGroupExtractor} is just not up to the task. Having this setting enabled is also a very unlikely edge
     * case, and in prior iterations of this node it also lead to errors when a comment had parenthesis in it.
     *
     * @deprecated
     */
    @Persist(configKey = "isComments", settingsModel = SettingsModelBoolean.class, optional = true)
    @Deprecated // NOSONAR: Deprecated since the beginning of this classes existence
    @Effect(signals = {SignalThatsUsedToHideFlagSettings.class, SignalThatsUsedToHideFlagSettings.class},
        operation = Xor.class, type = EffectType.SHOW) // xor on the same signal ensures that this is never shown
    boolean m_isComments = false; // NOSONAR: kept for backwards-compatibility

}
