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
 *   21 Dec 2022 (jasper): created
 */
package org.knime.base.node.preproc.stringcleaner;

import static org.knime.node.parameters.widget.choices.util.ColumnSelectionUtil.getStringColumnsOfFirstPort;

import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.layout.After;
import org.knime.node.parameters.layout.HorizontalLayout;
import org.knime.node.parameters.layout.Layout;
import org.knime.node.parameters.layout.Section;
import org.knime.node.parameters.migration.Migrate;
import org.knime.node.parameters.updates.Effect;
import org.knime.node.parameters.updates.Effect.EffectType;
import org.knime.node.parameters.updates.EffectPredicate;
import org.knime.node.parameters.updates.EffectPredicateProvider;
import org.knime.node.parameters.updates.ParameterReference;
import org.knime.node.parameters.updates.ValueReference;
import org.knime.node.parameters.updates.util.BooleanReference;
import org.knime.node.parameters.widget.choices.ChoicesProvider;
import org.knime.node.parameters.widget.choices.Label;
import org.knime.node.parameters.widget.choices.ValueSwitchWidget;
import org.knime.node.parameters.widget.choices.filter.ColumnFilter;
import org.knime.node.parameters.widget.choices.util.CompatibleColumnsProvider.StringColumnsProvider;
import org.knime.node.parameters.widget.number.NumberInputWidget;
import org.knime.node.parameters.widget.number.NumberInputWidgetValidation.MinValidation.IsPositiveIntegerValidation;
import org.knime.node.parameters.widget.text.TextInputWidget;
import org.knime.node.parameters.widget.text.TextInputWidgetValidation.PatternValidation.IsNotEmptyValidation;
import org.knime.node.parameters.widget.text.TextInputWidgetValidation.PatternValidation.IsSingleCharacterValidation;

/**
 * Node Settings for the Value Lookup Node
 *
 * @author Jasper Krauter, KNIME GmbH, Konstanz, Germany
 * @since 5.2
 */
public final class StringCleanerNodeSettings implements NodeParameters {

    // Layout
    interface DialogLayout {
        @Section(title = "Column Selection")
        interface ColumnSelection {
        }

        @Section(title = "Special sequences")
        @After(ColumnSelection.class)
        interface SpecialSequences {
        }

        @Section(title = "Characters")
        @After(SpecialSequences.class)
        interface Characters {
        }

        @Section(title = "Whitespace")
        @After(Characters.class)
        interface Whitespace {
        }

        @Section(title = "String Manipulation")
        @After(Whitespace.class)
        interface Manipulation {
            interface CapitalizeAndPad {
            }

            @HorizontalLayout
            @After(CapitalizeAndPad.class)
            interface PadOptions {
            }
        }

        @Section(title = "Output")
        @After(Manipulation.class)
        interface Output {
        }
    }

    // Settings
    @Widget(title = "Columns to clean", description = """
            Select which columns should be cleaned. \
            The strings in these columns will be modified according to the configuration of this node.
            """)
    @ChoicesProvider(StringColumnsProvider.class)
    @Layout(DialogLayout.ColumnSelection.class)
    ColumnFilter m_columnsToClean;

    @Widget(title = "Remove accents and diacritics", description = """
            When enabled, all accents and diacritics are removed from letters, leaving only the underlying letter. \
            For example, Å becomes A, ë becomes e and だ becomes た.
            """)
    @Layout(DialogLayout.SpecialSequences.class)
    boolean m_removeAccents = false;

    @Widget(title = "Remove non-ASCII characters", description = """
            When enabled, all non-<a href="https://en.wikipedia.org/wiki/ASCII">ASCII</a> characters are removed from \
            strings.
            """)
    @Layout(DialogLayout.SpecialSequences.class)
    boolean m_removeNonASCII = false;

    @Widget(title = "Remove non-printable characters", description = """
            When enabled, all non-printable characters like a tabulator or non-break space are removed from strings.
            """)
    @Migrate(loadDefaultIfAbsent = true)
    @Layout(DialogLayout.SpecialSequences.class)
    boolean m_removeNonPrintableChars = false;

    enum RemoveLettersCategory {
            @Label("None")
            NONE, //
            @Label("All")
            ALL, //
            @Label("Uppercase")
            UPPERCASE, //
            @Label("Lowercase")
            LOWERCASE;
    }

    @Widget(title = "Remove letters", description = """
            Select whether and what category of letters should be removed from strings.
            <ul>
                <li><b>None</b> removes no letters.</li>
                <li><b>All</b> removes all letters from a string. This includes letters from any language / script.</li>
                <li><b>Uppercase</b> removes all uppercase letters from a string.</li>
                <li><b>Lowercase</b> removes all lowercase letters from a string.</li>
            </ul>
            Note that this step happens before potentially changing the casing of the string.
            """)
    @ValueSwitchWidget
    @Layout(DialogLayout.Characters.class)
    RemoveLettersCategory m_removeLettersCategory = RemoveLettersCategory.NONE;

    @Widget(title = "Remove numbers", description = """
            When enabled, all numbers are removed from strings. This includes e.g. 3, \u2164 or ₅.
            """)
    @Layout(DialogLayout.Characters.class)
    boolean m_removeNumbers = false;

    @Widget(title = "Remove punctuation", description = """
            When enabled, all punctuation is removed from strings. This includes e.g. _, ( or !.
            """)
    @Layout(DialogLayout.Characters.class)
    boolean m_removePunctuation = false;

    @Widget(title = "Remove symbols", description = """
            When enabled, all symbols are removed from strings. This includes e.g. €, = or \u266E.
            """)
    @Layout(DialogLayout.Characters.class)
    boolean m_removeSymbols = false;

    @Widget(title = "Other characters to remove", description = """
            Here, custom characters can be defined that should be removed from strings. \
            The characters are all interpreted literally and case-sensitive. Note that this step happens before \
            potentially changing the casing of the string.
            """)
    @Layout(DialogLayout.Characters.class)
    String m_removeCustomCharactersString = "";

    static final class RemoveAllWhitespace implements BooleanReference {

    }

    @Widget(title = "Remove all whitespace", description = """
            If enabled, all whitespace is removed. \
            This includes normal space ( ), line breaks (\\r\\n), tabulators (\\t) and all other whitespace.
            """)
    @Layout(DialogLayout.Whitespace.class)
    @ValueReference(RemoveAllWhitespace.class)
    boolean m_removeAllWhitespace = false;

    @Widget(title = "Remove leading whitespace", description = """
            If enabled, leading whitespace is removed, that is, all whitespace from the start of the string to the \
            first non-whitespace character.
            """)
    @Layout(DialogLayout.Whitespace.class)
    @Effect(predicate = RemoveAllWhitespace.class, type = EffectType.HIDE)
    boolean m_removeLeadingWhitespace = true;

    @Widget(title = "Remove trailing whitespace", description = """
            If enabled, trailing whitespace is removed, that is, all whitespace from the last non-whitespace character \
            to the end of the string.
            """)
    @Layout(DialogLayout.Whitespace.class)
    @Effect(predicate = RemoveAllWhitespace.class, type = EffectType.HIDE)
    boolean m_removeTrailingWhitespace = true;

    @Widget(title = "Remove duplicate whitespace", description = """
            If selected, all occurrences of two or more whitespace characters in a row are replaced by a single \
            standard space.
            """)
    @Layout(DialogLayout.Whitespace.class)
    @Effect(predicate = RemoveAllWhitespace.class, type = EffectType.HIDE)
    boolean m_removeDuplicateWhitespace = true;

    /**
     * This generalises the following two options -- since we want to have a different order for them wen need to have
     * separate enums.
     */
    enum InternalReplaceWithOption {
            KEEP, REPLACE_WITH_SPACE, REPLACE_WITH_EMPTYSTRING;
    }

    /**
     * Enum used for the UI switch for the "Line Break" option
     */
    enum ReplaceLinebreakWithOption {
            @Label("Keep")
            KEEP(InternalReplaceWithOption.KEEP), //
            @Label("Replace by space")
            REPLACE_WITH_STANDARDSPACE(InternalReplaceWithOption.REPLACE_WITH_SPACE), //
            @Label("Remove")
            REMOVE(InternalReplaceWithOption.REPLACE_WITH_EMPTYSTRING);

        private InternalReplaceWithOption m_internal;

        private ReplaceLinebreakWithOption(final InternalReplaceWithOption i) {
            this.m_internal = i;
        }

        InternalReplaceWithOption getInternal() {
            return m_internal;
        }
    }

    @Widget(title = "Line breaks", description = """
            Select whether to remove line breaks or replace them with a standard space. \
            If <b>Replace by space</b> is selected, \\r\\n is replaced by only a single space, not two.
            """)
    @ValueSwitchWidget
    @Layout(DialogLayout.Whitespace.class)
    @Effect(predicate = RemoveAllWhitespace.class, type = EffectType.HIDE)
    ReplaceLinebreakWithOption m_replaceLinebreakStrategy = ReplaceLinebreakWithOption.KEEP;

    /**
     * Enum used for the UI switch for the "White space" option
     */
    enum ReplaceWhitespaceWithOption {
            @Label("Replace by space")
            REPLACE_WITH_STANDARDSPACE(InternalReplaceWithOption.REPLACE_WITH_SPACE), //
            @Label("Keep")
            KEEP(InternalReplaceWithOption.KEEP), //
            @Label("Remove")
            REMOVE(InternalReplaceWithOption.REPLACE_WITH_EMPTYSTRING);

        private InternalReplaceWithOption m_internal;

        private ReplaceWhitespaceWithOption(final InternalReplaceWithOption i) {
            this.m_internal = i;
        }

        InternalReplaceWithOption getInternal() {
            return m_internal;
        }
    }

    @Widget(title = "Special whitespace", description = """
            Select whether to remove special whitespace or replace it with a standard space. \
            Special whitespace is all whitespace that is not a standard space, \\r or \\n.
            """)
    @ValueSwitchWidget
    @Layout(DialogLayout.Whitespace.class)
    @Effect(predicate = RemoveAllWhitespace.class, type = EffectType.HIDE)
    ReplaceWhitespaceWithOption m_replaceSpecialWhitespaceStrategy =
        ReplaceWhitespaceWithOption.REPLACE_WITH_STANDARDSPACE;

    enum ChangeCasingOption { //
            @Label("None")
            NO, //
            @Label("Uppercase")
            UPPERCASE, //
            @Label("Capitalize")
            CAPITALIZE, //
            @Label("Lowercase")
            LOWERCASE;
    }

    class ChangeCasingOptionRef implements ParameterReference<ChangeCasingOption> {

    }

    static final class ChangeCasingIsCapitalize implements EffectPredicateProvider {

        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getEnum(ChangeCasingOptionRef.class).isOneOf(ChangeCasingOption.CAPITALIZE);
        }

    }

    @Widget(title = "Change casing", description = """
            Define the casing of letters in the output string.
            <ul>
                <li><b>None</b> makes no changes to the string.</li>
                <li><b>Uppercase</b> converts all letters to uppercase.</li>
                <li><b>Capitalize</b> first converts all letters to lowercase, then capitalizes according to the \
                defined settings.</li>
                <li><b>Lowercase</b> converts all letters to lowercase.</li>
            </ul>
            """)
    @ValueSwitchWidget
    @Layout(DialogLayout.Manipulation.CapitalizeAndPad.class)
    @ValueReference(ChangeCasingOptionRef.class)
    ChangeCasingOption m_changeCasing = ChangeCasingOption.NO;

    enum CapitalizeAfterOption {
            @Label("Whitespace")
            WHITESPACE, //
            @Label("Non-letters")
            NON_LETTERS, //
            @Label("Custom")
            CUSTOM
    }

    class CapitalizeAfterOptionRef implements ParameterReference<CapitalizeAfterOption> {

    }

    @Widget(title = "Capitalize after", description = """
            Define after which characters a character should be capitalized.
            <ul>
                <li><b>Whitespace</b> capitalizes letters that follow immediately after a whitespace character</li>
                <li><b>Non-Letters</b> capitalizes letters that follow any non-letter character.</li>
                <li><b>Custom</b> lets the user decide on a set of characters after which letters should be \
                capitalized</li>
            </ul>
            """)
    @ValueSwitchWidget
    @Layout(DialogLayout.Manipulation.CapitalizeAndPad.class)
    @Effect(predicate = ChangeCasingIsCapitalize.class, type = EffectType.SHOW)
    @ValueReference(CapitalizeAfterOptionRef.class)
    CapitalizeAfterOption m_changeCasingCapitalizeAfter = CapitalizeAfterOption.WHITESPACE;

    static class CapitalizeAfterCustom implements EffectPredicateProvider {

        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getPredicate(ChangeCasingIsCapitalize.class)
                .and(i.getEnum(CapitalizeAfterOptionRef.class).isOneOf(CapitalizeAfterOption.CUSTOM));
        }

    }

    @Widget(title = "Capitalize after characters", description = """
            Here, custom characters can be defined after which characters should be capitalized. \
            The characters are all interpreted literally.
            """)
    @TextInputWidget(minLengthValidation = IsNotEmptyValidation.class)
    @Layout(DialogLayout.Manipulation.CapitalizeAndPad.class)
    @Effect(predicate = CapitalizeAfterCustom.class, type = EffectType.SHOW)
    String m_changeCasingCapitalizeAfterCharacters = "";

    @Widget(title = "Capitalize character at the start of the string", description = """
            If enabled, a letter at the start of the string is always capitalized.
            """)
    @Layout(DialogLayout.Manipulation.CapitalizeAndPad.class)
    @Effect(predicate = ChangeCasingIsCapitalize.class, type = EffectType.SHOW)
    boolean m_changeCasingCapitalizeFirstCharacter = true;

    enum PadOption { //
            @Label("None")
            NO, //
            @Label("Start")
            START, //
            @Label("End")
            END,
    }

    class PadOptionRef implements ParameterReference<PadOption> {

    }

    static final class DoPad implements EffectPredicateProvider {

        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getEnum(PadOptionRef.class).isOneOf(PadOption.START, PadOption.END);
        }

    }

    @Widget(title = "Pad", description = """
            Define whether to pad a string if it doesn't have a certain minimum length.
            <ul>
                <li><b>None</b> makes no change to the string.</li>
                <li><b>Start</b> adds a fill character to the start of the string.</li>
                <li><b>End</b> adds a fill character to the end of the string.</li>
            </ul>
            """)
    @ValueSwitchWidget
    @Layout(DialogLayout.Manipulation.CapitalizeAndPad.class)
    @ValueReference(PadOptionRef.class)
    PadOption m_pad = PadOption.NO;

    @Widget(title = "Minimum string length", description = """
            Define the minimum string length. \
            If a string is shorter than this value, a pad will be added to make the string length equal to it.
            """)
    @NumberInputWidget(minValidation = IsPositiveIntegerValidation.class)
    @Layout(DialogLayout.Manipulation.PadOptions.class)
    @Effect(predicate = DoPad.class, type = EffectType.SHOW)
    int m_padMinimumStringLength = 1;

    @Widget(title = "Fill character", description = """
            Define the fill character that is used to pad the string.
            """)
    @TextInputWidget(patternValidation = IsSingleCharacterValidation.class)
    @Layout(DialogLayout.Manipulation.PadOptions.class)
    @Effect(predicate = DoPad.class, type = EffectType.SHOW)
    String m_padFillCharacter = "_";

    enum OutputOption {
            @Label("Replace")
            REPLACE, //
            @Label("Append with suffix")
            APPEND
    }

    class OutputOptionRef implements ParameterReference<OutputOption> {

    }

    static final class AppendColumnsWithSuffix implements EffectPredicateProvider {

        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getEnum(OutputOptionRef.class).isOneOf(OutputOption.APPEND);
        }

    }

    @Widget(title = "Output columns", description = """
            Define whether to clean the strings in-place (i.e. replace the existing values) or append (a) new column(s).
            """)
    @ValueSwitchWidget
    @Layout(DialogLayout.Output.class)
    @ValueReference(OutputOptionRef.class)
    OutputOption m_output = OutputOption.REPLACE;

    @Widget(title = "Output column suffix", description = """
            Define a suffix that is appended to the column names of the input table.
            """)
    @TextInputWidget(minLengthValidation = IsNotEmptyValidation.class)
    @Layout(DialogLayout.Output.class)
    @Effect(predicate = AppendColumnsWithSuffix.class, type = EffectType.SHOW)
    String m_outputSuffix = " (Cleaned)";

    // Constructor

    StringCleanerNodeSettings() {
        m_columnsToClean = new ColumnFilter().withIncludeUnknownColumns();
    }

    StringCleanerNodeSettings(final NodeParametersInput ctx) {
        m_columnsToClean = new ColumnFilter(getStringColumnsOfFirstPort(ctx)).withIncludeUnknownColumns();
    }
}
