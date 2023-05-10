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
 *   5 Jan 2023 (chaubold): created
 */
package org.knime.base.node.preproc.stringreplacer;

import java.util.Optional;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.StringValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings;
import org.knime.core.webui.node.dialog.defaultdialog.layout.Layout;
import org.knime.core.webui.node.dialog.defaultdialog.layout.Section;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.field.FieldNodeSettingsPersistor;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.field.Persist;
import org.knime.core.webui.node.dialog.defaultdialog.rule.Effect;
import org.knime.core.webui.node.dialog.defaultdialog.rule.Effect.EffectType;
import org.knime.core.webui.node.dialog.defaultdialog.rule.OneOfEnumCondition;
import org.knime.core.webui.node.dialog.defaultdialog.rule.Signal;
import org.knime.core.webui.node.dialog.defaultdialog.rule.TrueCondition;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ChoicesProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ChoicesWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Label;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ValueSwitchWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Widget;

/**
 * The StringReplacerNodeSettings define the WebUI dialog of the StringReplacer Node. The serialization must go via the
 * {@link StringReplacerSettings}.
 *
 * @author Carsten Haubold, KNIME GmbH, Konstanz, Germany
 */
@SuppressWarnings("restriction")
final class StringReplacerNodeSettings implements DefaultNodeSettings {

    // Enums

    enum PatternType {
            @Label("Literal")
            LITERAL,

            @Label("Wildcard")
            WILDCARD,

            @Label("Regular expression")
            REGEX;

        static final String OPTION_DESCRIPTION = """
                Select the type of pattern which you want to use.
                <ul>
                    <li><i>Literal</i> matches the pattern as is.</li>
                    <li>
                        <i>Wildcard</i> matches <tt>*</tt> to zero or more arbitrary characters and matches
                        <tt>?</tt> to  any single character.
                    </li>
                    <li>
                        <i>Regular expression</i>
                        matches using the full functionality of Java regular expressions, including backreferences
                        in the replacement text. See the
                        <a href="http://docs.oracle.com/javase/6/docs/api/java/util/regex/Pattern.html">Java API
                        </a> for details.
                    </li>
                </ul>
                """;

        static final PatternType defaultType = LITERAL;

        static Optional<PatternType> get(final String name) {
            if (LITERAL.name().equals(name)) {
                return Optional.of(LITERAL);
            } else if (WILDCARD.name().equals(name)) {
                return Optional.of(WILDCARD);
            } else if (REGEX.name().equals(name)) {
                return Optional.of(REGEX);
            } else {
                return Optional.empty();
            }
        }
    }

    /** Whether to distinguish between upper-case and lower-case letters **/
    enum CaseMatching {
            @Label("Case insensitive")
            CASEINSENSITIVE, //
            @Label("Case sensitive")
            CASESENSITIVE;

        static final String OPTION_DESCRIPTION =
            "Specifies whether strings should be matched case-insensitive or case-sensitive.";
    }

    enum ReplacementStrategy {
            @Label("Whole string")
            WHOLE_STRING,

            @Label("All occurrences")
            ALL_OCCURRENCES;

        static final String OPTION_DESCRIPTION = """
                Select what to replace in case a string matches a pattern.
                <ul>
                    <li>
                        <i>Whole string</i> replaces the entire string with the replacement string, requiring an
                        exact  match of the whole string.
                    </li>
                    <li>
                        <i>All occurrences</i> replaces all occurrences of the pattern with the replacement string.
                        Note that when e.g. matching on the RegEx-pattern <tt>.*</tt>, an empty string at the end
                        of the input is also matched and replaced. To avoid that, use e.g. the pattern <tt>^.*</tt>
                        to indicate that the match has to start at the beginning of the string.
                    </li>
                </ul>
                """;
    }


    // Rules

    /** Indicates that the "Wildcard" pattern type is selected */
    interface IsWildcard {
        class Condition extends OneOfEnumCondition<PatternType> {
            @Override
            public PatternType[] oneOf() {
                return new PatternType[]{PatternType.WILDCARD};
            }
        }
    }

    /** Indicates that the option "Append column" is enabled **/
    interface IsAppendColumn {}


    // Layout

    interface DialogSections {
        @Section(title = "Column Selection")
        interface ColumnSelection {}

        @Section(title = "Find & Replace")
        interface FindAndReplace {}

        @Section(title = "Output")
        interface Output {}
    }


    // Settings

    @Layout(DialogSections.ColumnSelection.class)
    @Persist(configKey = StringReplacerSettings.CFG_COL_NAME)
    @Widget(title = "Target column", description = "Select the column in which the strings should be replaced.")
    @ChoicesWidget(choices = StringColumnChoices.class)
    String m_colName;

    @Layout(DialogSections.FindAndReplace.class)
    @Persist(customPersistor = PatternTypePersistor.class)
    @Widget(title = "Pattern type", description = PatternType.OPTION_DESCRIPTION)
    @ValueSwitchWidget
    @Signal(id = IsWildcard.class, condition = IsWildcard.Condition.class)
    PatternType m_patternType = PatternType.LITERAL;

    @Layout(DialogSections.FindAndReplace.class)
    @Persist(configKey = StringReplacerSettings.CFG_ENABLE_ESCAPING)
    @Widget(title = "Use backslash as escape character", description = """
            If checked, the backslash character can be used to escape special characters. For instance, <tt>\\?</tt>
            will match the literal character <tt>?</tt> instead of an arbitrary character. In order to match a
            backslash you need to escape the backslash, too (<tt>\\</tt>).
            """)
    @Effect(signals = IsWildcard.class, type = EffectType.SHOW)
    boolean m_enableEscaping;

    @Layout(DialogSections.FindAndReplace.class)
    @Persist(customPersistor = CaseMatchingPersistor.class)
    @Widget(title = "Case sensitive",
        description = "If checked, the matching will distinguish between upper and lower case letters.")
    @ValueSwitchWidget
    CaseMatching m_caseMatching;

    @Layout(DialogSections.FindAndReplace.class)
    @Persist(configKey = StringReplacerSettings.CFG_PATTERN)
    @Widget(title = "Pattern", description = """
            A literal string, wildcard pattern or regular expression, depending on the pattern type selected above.
            """)
    String m_pattern;

    @Layout(DialogSections.FindAndReplace.class)
    @Persist(configKey = StringReplacerSettings.CFG_REPLACEMENT)
    @Widget(title = "Replacement text", description = """
            The text that replaces the previous value in the cell if the pattern matched it. If you are using a
            regular expression, you may also use backreferences (e.g. <tt>$1</tt> to refer to the first capture group).
            """)
    String m_replacement;

    @Layout(DialogSections.FindAndReplace.class)
    @Persist(customPersistor = ReplacementStrategyPersistor.class)
    @Widget(title = "Replacement strategy", description = ReplacementStrategy.OPTION_DESCRIPTION)
    @ValueSwitchWidget
    ReplacementStrategy m_replacementStrategy = ReplacementStrategy.WHOLE_STRING;

    @Layout(DialogSections.Output.class)
    @Persist(configKey = StringReplacerSettings.CFG_CREATE_NEW_COL)
    @Widget(title = "Append new column", description = """
            If enabled, the strings will not be replaced in-place but a new column is appended that contains the
            original string with the replacement applied.
            """)
    @Signal(id = IsAppendColumn.class, condition = TrueCondition.class)
    boolean m_createNewCol;

    @Layout(DialogSections.Output.class)
    @Persist(configKey = StringReplacerSettings.CFG_NEW_COL_NAME)
    @Widget(title = "New column name", description = "The name of the created column with replaced strings")
    @Effect(signals = IsAppendColumn.class, type = EffectType.SHOW)
    String m_newColName = "ReplacedColumn";


    // Persistors

    private static final class PatternTypePersistor implements FieldNodeSettingsPersistor<PatternType> {
        @Override
        public PatternType load(final NodeSettingsRO settings) throws InvalidSettingsException {
            if (settings.getBoolean(StringReplacerSettings.CFG_FIND_PATTERN)) {
                final var isRegex = settings.getBoolean(StringReplacerSettings.CFG_PATTERN_IS_REGEX);
                return isRegex ? PatternType.REGEX : PatternType.WILDCARD;
            } else {
                return PatternType.LITERAL;
            }
        }

        @Override
        public void save(final PatternType patternType, final NodeSettingsWO settings) {
            settings.addBoolean(StringReplacerSettings.CFG_FIND_PATTERN,
                patternType == PatternType.REGEX || patternType == PatternType.WILDCARD);
            settings.addBoolean(StringReplacerSettings.CFG_PATTERN_IS_REGEX, patternType == PatternType.REGEX);
        }

        @Override
        public String[] getConfigKeys() {
            return new String[]{StringReplacerSettings.CFG_FIND_PATTERN, StringReplacerSettings.CFG_PATTERN_IS_REGEX};
        }
    }

    private static final class CaseMatchingPersistor implements FieldNodeSettingsPersistor<CaseMatching> {

        @Override
        public CaseMatching load(final NodeSettingsRO settings) throws InvalidSettingsException {
            return settings.getBoolean(StringReplacerSettings.CFG_CASE_SENSITIVE) ? CaseMatching.CASESENSITIVE
                : CaseMatching.CASEINSENSITIVE;
        }

        @Override
        public void save(final CaseMatching matchingStrategy, final NodeSettingsWO settings) {
            settings.addBoolean(StringReplacerSettings.CFG_CASE_SENSITIVE,
                matchingStrategy == CaseMatching.CASESENSITIVE);
        }

        @Override
        public String[] getConfigKeys() {
            return new String[]{StringReplacerSettings.CFG_CASE_SENSITIVE};
        }

    }

    private static final class ReplacementStrategyPersistor implements FieldNodeSettingsPersistor<ReplacementStrategy> {
        @Override
        public ReplacementStrategy load(final NodeSettingsRO settings) throws InvalidSettingsException {
            if (settings.getBoolean(StringReplacerSettings.CFG_REPLACE_ALL_OCCURENCES)) {
                return ReplacementStrategy.ALL_OCCURRENCES;
            } else {
                return ReplacementStrategy.WHOLE_STRING;
            }
        }

        @Override
        public void save(final ReplacementStrategy obj, final NodeSettingsWO settings) {
            settings.addBoolean(StringReplacerSettings.CFG_REPLACE_ALL_OCCURENCES,
                obj == ReplacementStrategy.ALL_OCCURRENCES);
        }

        @Override
        public String[] getConfigKeys() {
            return new String[]{StringReplacerSettings.CFG_REPLACE_ALL_OCCURENCES};
        }
    }

    private static final class StringColumnChoices implements ChoicesProvider {
        @Override
        public String[] choices(final SettingsCreationContext context) {
            final DataTableSpec specs = context.getDataTableSpecs()[0];
            if (specs == null) {
                return new String[0];
            } else {
                return specs.stream() //
                    .filter(s -> s.getType().isCompatible(StringValue.class)) //
                    .map(DataColumnSpec::getName) //
                    .toArray(String[]::new);
            }
        }
    }
}
