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
import org.knime.core.webui.node.dialog.defaultdialog.persistence.booleanhelpers.AlwaysSaveTrueBoolean;
import org.knime.node.parameters.Advanced;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.layout.After;
import org.knime.node.parameters.layout.Layout;
import org.knime.node.parameters.layout.Section;
import org.knime.node.parameters.migration.DefaultProvider;
import org.knime.node.parameters.migration.Migrate;
import org.knime.node.parameters.migration.Migration;
import org.knime.node.parameters.persistence.NodeParametersPersistor;
import org.knime.node.parameters.persistence.Persist;
import org.knime.node.parameters.persistence.Persistor;
import org.knime.node.parameters.updates.Effect;
import org.knime.node.parameters.updates.Effect.EffectType;
import org.knime.node.parameters.widget.choices.ChoicesProvider;
import org.knime.node.parameters.widget.choices.Label;
import org.knime.node.parameters.widget.choices.ValueSwitchWidget;
import org.knime.node.parameters.widget.choices.util.CompatibleColumnsProvider.StringColumnsProvider;
import org.knime.node.parameters.widget.text.TextInputWidget;
import org.knime.node.parameters.widget.text.TextInputWidgetValidation.PatternValidation.IsNotEmptyValidation;

/**
 * Settings class for the String Splitter (Regex) (formerly known as Regex Split)
 *
 * @author Jasper Krauter, KNIME GmbH, Konstanz, Germany
 */
@SuppressWarnings({"restriction", "squid:S3052"}) // Pending API, initialise with defaults
final class RegexSplitNodeSettings implements NodeParameters {

    RegexSplitNodeSettings() {
        //
    }

    RegexSplitNodeSettings(final NodeParametersInput context) {
        var tableSpec = context.getInTableSpec(0).orElse(null);
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

        @Section(title = "Splitting (Advanced)")
        @Advanced
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
    @ChoicesProvider(StringColumnsProvider.class)
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
    @TextInputWidget(minLengthValidation = IsNotEmptyValidation.class)
    String m_pattern = "(.*)";

    enum CaseMatching {
            /** Respect case when matching strings. */
            @Label("Case sensitive")
            CASESENSITIVE, //
            /** Disregard case when matching strings. */
            @Label("Case insensitive")
            CASEINSENSITIVE;

        /** persists the case sensitivity as a boolean indicating whether the matching is case-INsensitive */
        public static final class CaseInsensitivityPersistor implements NodeParametersPersistor<CaseMatching> {

            static final String CFG_KEY_BOOLEAN = "isCaseInsensitive";

            @Override
            public CaseMatching load(final NodeSettingsRO settings) throws InvalidSettingsException {
                return settings.getBoolean(CFG_KEY_BOOLEAN) ? CASEINSENSITIVE : CASESENSITIVE;
            }

            @Override
            public void save(final CaseMatching cm, final NodeSettingsWO settings) {
                settings.addBoolean(CFG_KEY_BOOLEAN, cm == CASEINSENSITIVE);
            }

            @Override
            public String[][] getConfigPaths() {
                return new String[][]{{CFG_KEY_BOOLEAN}};
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
    @Persistor(CaseMatching.CaseInsensitivityPersistor.class)
    CaseMatching m_caseMatching = CaseMatching.CASESENSITIVE;

    @Layout(DialogSections.Splitting.class)
    @Widget(title = "Require whole string to match", description = """
            If enabled, the provided pattern must match the whole string in order to return any results. Otherwise, the
            first match in the input string is used.
            """)
    @Migrate(loadDefaultIfAbsent = true)
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
    @Migrate(loadDefaultIfAbsent = true)
    @ValueSwitchWidget
    NoMatchBehaviour m_noMatchBehaviour = NoMatchBehaviour.INSERT_MISSING;

    // Legacy / Advanced Splitting settings

    @Layout(DialogSections.SplittingAdvanced.class)
    @Widget(title = "Enable Unix lines mode", advanced = true,
        description = "In this mode, only the '\\n' line terminator is recognized in the behavior of ., ^, and $.")
    boolean m_isUnixLines = false;

    @Layout(DialogSections.SplittingAdvanced.class)
    @Widget(title = "Enable multiline mode (^ and $ match at the beginning / end of a line)", advanced = true,
        description = """
                In multiline mode the expressions ^ and $ match just after or just before, respectively, a line
                terminator or the end of the input sequence. By default these expressions only match at the beginning
                and the end of the entire input sequence.""")
    boolean m_isMultiLine = false;

    @Layout(DialogSections.SplittingAdvanced.class)
    @Widget(title = "Enable dotall mode (Dot . also matches newline characters)", advanced = true, description = """
            In dotall mode, the expression . matches any character, including a line terminator. By default this
            expression does not match line terminators.""")
    boolean m_isDotAll = false;

    @Layout(DialogSections.SplittingAdvanced.class)
    @Widget(title = "Enable Unicode-aware case folding", advanced = true, description = """
            When this is enabled then case-insensitive matching, when enabled, is done in a manner consistent with the
            Unicode Standard. By default, case-insensitive matching assumes that only characters in the US-ASCII charset
            are being matched. <br />
            Enabling this may impose a performance penalty.""")
    @Persist(configKey = "isUniCodeCase")
    boolean m_isUnicodeCase = false;

    @Layout(DialogSections.SplittingAdvanced.class)
    @Widget(title = "Enable canonical equivalence", advanced = true, description = """
            When enabled, two characters will be considered to match if, and only if, their full canonical
            decompositions match. The expression "a\\u030A", for example, will match the string "\\u00E5" when this is
            enabled. By default, matching does not take canonical equivalence into account.""")
    boolean m_isCanonEQ = false;

    @Layout(DialogSections.SplittingAdvanced.class)
    @Widget(title = "Enable Unicode character classes", advanced = true, description = """
            When enabled, the (US-ASCII only) <i>Predefined character classes</i> and <i>POSIX character classes</i> are
            in conformance with the Unicode Standard. <br />
            Enabling this may impose a performance penalty.""")
    @Migrate(loadDefaultIfAbsent = true)
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
    @Migration(TrueProvider.class) // If it's an old node instance, default to true
    @Effect(predicate = OutputGroupLabelMode.IsCaptureGroupNames.class, type = EffectType.SHOW)
    boolean m_decrementGroupIndexByOne = false;

    // Output Settings

    @Migration(OutputSettings.LegacyProvider.class)
    OutputSettings m_output = new OutputSettings();

    // Utility

    static final class DoNotAllowEmptyBlankOrPaddedColumnNamePersistor extends AlwaysSaveTrueBoolean {
        protected DoNotAllowEmptyBlankOrPaddedColumnNamePersistor() {
            super("doNotAllowEmptyBlankOrPaddedColumnName");
        }
    }

    @Persistor(DoNotAllowEmptyBlankOrPaddedColumnNamePersistor.class)
    boolean m_doNotAllowEmptyBlankOrPaddedColumnName = true;

    static class TrueProvider implements DefaultProvider<Boolean> {
        @Override
        public Boolean getDefault() {
            return true;
        }
    }

    // ===== Please don't look below this line

    /**
     * This setting doesn't make sense, since in Literal mode, no capture groups are possible. Setting is included only
     * to avoid warnings when loading old nodes, since they have this setting saved with them. The \@Widget annotation
     * is missing to not include this setting in the node description or the dialog.
     *
     * @deprecated
     */
    @Migrate(loadDefaultIfAbsent = true)
    @Deprecated // NOSONAR: Deprecated since the beginning of this classes existence
    boolean m_isLiteral = false; // NOSONAR: kept for backwards-compatibility

    /**
     * The story is a similar one for this setting, except that here the implementation of the
     * {@link CaptureGroupExtractor} is just not up to the task. Having this setting enabled is also a very unlikely
     * edge case, and in prior iterations of this node it also lead to errors when a comment had parenthesis in it.
     *
     * @deprecated
     */
    @Migrate(loadDefaultIfAbsent = true)
    @Deprecated // NOSONAR: Deprecated since the beginning of this classes existence
    boolean m_isComments = false; // NOSONAR: kept for backwards-compatibility

}
