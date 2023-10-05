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

import org.knime.core.data.DataColumnSpec;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings;
import org.knime.core.webui.node.dialog.defaultdialog.layout.After;
import org.knime.core.webui.node.dialog.defaultdialog.layout.HorizontalLayout;
import org.knime.core.webui.node.dialog.defaultdialog.layout.Layout;
import org.knime.core.webui.node.dialog.defaultdialog.layout.Section;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.field.Persist;
import org.knime.core.webui.node.dialog.defaultdialog.rule.And;
import org.knime.core.webui.node.dialog.defaultdialog.rule.Effect;
import org.knime.core.webui.node.dialog.defaultdialog.rule.Effect.EffectType;
import org.knime.core.webui.node.dialog.defaultdialog.rule.OneOfEnumCondition;
import org.knime.core.webui.node.dialog.defaultdialog.rule.Signal;
import org.knime.core.webui.node.dialog.defaultdialog.rule.TrueCondition;
import org.knime.core.webui.node.dialog.defaultdialog.setting.columnfilter.ColumnFilter;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ChoicesWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ColumnChoicesProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Label;
import org.knime.core.webui.node.dialog.defaultdialog.widget.NumberInputWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.TextInputWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ValueSwitchWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Widget;

/**
 * Node Settings for the Value Lookup Node
 *
 * @author Jasper Krauter, KNIME GmbH, Konstanz, Germany
 * @since 5.2
 */
@SuppressWarnings({"restriction", "squid:S3052"}) // New Node UI is not yet API / initialise defaults verbosely
public final class StringCleanerNodeSettings implements DefaultNodeSettings {

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

    // Signals
    interface Signals {
        interface RemoveCustomCharacters {
        }

        interface RemoveAllWhitespace {
        }

        interface ChangeCasingIsCapitalize {
        }

        interface CapitalizeAfterIsCustom {
        }

        interface DoPad {
        }

        interface AppendColumnsWithSuffix {
        }
    }

    // Settings

    static final class StringColumnChoicesProvider implements ColumnChoicesProvider {
        @Override
        public DataColumnSpec[] columnChoices(final DefaultNodeSettingsContext context) {
            return context.getDataTableSpec(0).map(StringCleanerNodeModel::stringColumns)
                .orElse(new DataColumnSpec[]{});
        }
    }

    @Widget(title = "Columns to clean", description = """
            Select which columns should be cleaned. \
            The strings in these columns will be modified according to the configuration of this node.
            """)
    @ChoicesWidget(choices = StringColumnChoicesProvider.class)
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
    @Persist(optional = true)
    @Layout(DialogLayout.SpecialSequences.class)
    boolean m_removeNonPrintableChars = false;

    enum RemoveLettersCategory {
            @Label("All")
            ALL, //
            @Label("Uppercase")
            UPPERCASE, //
            @Label("Lowercase")
            LOWERCASE, //
            @Label("None")
            NONE
    }

    @Widget(title = "Remove Letters", description = """
            Select whether and what category of letters should be removed from strings.
            <ul>
                <li><b>All</b> removes all letters from a string. This includes letters from any language / script.</li>
                <li><b>Uppercase</b> removes all uppercase letters from a string.</li>
                <li><b>Lowercase</b> removes all lowercase letters from a string.</li>
                <li><b>None</b> removes no letters.</li>
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

    @Widget(title = "Remove all whitespace", description = """
            If enabled, all whitespace is removed. \
            This includes normal space ( ), line breaks (\r\n), tabulators (\t) and all other whitespace.
            """)
    @Layout(DialogLayout.Whitespace.class)
    @Signal(id = Signals.RemoveAllWhitespace.class, condition = TrueCondition.class)
    boolean m_removeAllWhitespace = false;

    @Widget(title = "Remove leading whitespace", description = """
            If enabled, leading whitespace is removed, that is, all whitespace from the start of the string to the \
            first non-whitespace character.
            """)
    @Layout(DialogLayout.Whitespace.class)
    @Effect(signals = Signals.RemoveAllWhitespace.class, type = EffectType.HIDE)
    boolean m_removeLeadingWhitespace = true;

    enum ReplaceWithOption {
            @Label("Remove")
            REMOVE, //
            @Label("Replace with standard space")
            REPLACE_WITH_STANDARDSPACE, //
            @Label("Keep")
            KEEP;
    }

    @Widget(title = "Replace line breaks", description = """
            Select whether to remove line breaks or replace them with a standard space. \
            If <b>Replace with standard space</b> is selected, \\r\\n is replaced by only a single space, not two.
            """)
    @ValueSwitchWidget
    @Layout(DialogLayout.Whitespace.class)
    @Effect(signals = Signals.RemoveAllWhitespace.class, type = EffectType.HIDE)
    ReplaceWithOption m_replaceLinebreakStrategy = ReplaceWithOption.KEEP;

    @Widget(title = "Replace special whitespace", description = """
            Select whether to remove special whitespace or replace it with a standard space. \
            Special whitespace is all whitespace that is not a standard space, \\r or \\n.
            """)
    @ValueSwitchWidget
    @Layout(DialogLayout.Whitespace.class)
    @Effect(signals = Signals.RemoveAllWhitespace.class, type = EffectType.HIDE)
    ReplaceWithOption m_replaceSpecialWhitespaceStrategy = ReplaceWithOption.REPLACE_WITH_STANDARDSPACE;

    @Widget(title = "Remove duplicate whitespace", description = """
            If selected, all occurrences of two or more whitespace characters in a row are replaced by a single \
            standard space.
            """)
    @Layout(DialogLayout.Whitespace.class)
    @Effect(signals = Signals.RemoveAllWhitespace.class, type = EffectType.HIDE)
    boolean m_removeDuplicateWhitespace = true;

    @Widget(title = "Remove trailing whitespace", description = """
            If enabled, trailing whitespace is removed, that is, all whitespace from the last non-whitespace character \
            to the end of the string.
            """)
    @Layout(DialogLayout.Whitespace.class)
    @Effect(signals = Signals.RemoveAllWhitespace.class, type = EffectType.HIDE)
    boolean m_removeTrailingWhitespace = true;

    enum ChangeCasingOption {
            @Label("UPPERCASE")
            UPPERCASE, //
            @Label("lowercase")
            LOWERCASE, //
            @Label("Capitalize")
            CAPITALIZE, //
            @Label("No")
            NO
    }

    static class IsCapitalizeCondition extends OneOfEnumCondition<ChangeCasingOption> {
        @Override
        public ChangeCasingOption[] oneOf() {
            return new ChangeCasingOption[]{ChangeCasingOption.CAPITALIZE};
        }
    }

    @Widget(title = "Change casing", description = """
            Define the casing of letters in the output string.
            <ul>
                <li><b>UPPERCASE</b> converts all letters to uppercase.</li>
                <li><b>lowercase</b> converts all letters to lowercase.</li>
                <li><b>Capitalize</b> first converts all letters to lowercase, then capitalizes according to the \
                defined settings.</li>
                <li><b>No</b> makes no changes to the string.</li>
            </ul>
            """)
    @ValueSwitchWidget
    @Layout(DialogLayout.Manipulation.CapitalizeAndPad.class)
    @Signal(id = Signals.ChangeCasingIsCapitalize.class, condition = IsCapitalizeCondition.class)
    ChangeCasingOption m_changeCasing = ChangeCasingOption.NO;

    enum CapitalizeAfterOption {
            @Label("Whitespace")
            WHITESPACE, //
            @Label("Non-letters")
            NON_LETTERS, //
            @Label("Custom")
            CUSTOM
    }

    static class IsCustomCondition extends OneOfEnumCondition<CapitalizeAfterOption> {
        @Override
        public CapitalizeAfterOption[] oneOf() {
            return new CapitalizeAfterOption[]{CapitalizeAfterOption.CUSTOM};
        }
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
    @Effect(signals = Signals.ChangeCasingIsCapitalize.class, type = EffectType.SHOW)
    @Signal(id = Signals.CapitalizeAfterIsCustom.class, condition = IsCustomCondition.class)
    CapitalizeAfterOption m_changeCasingCapitalizeAfter = CapitalizeAfterOption.WHITESPACE;

    @Widget(title = "Capitalize after characters", description = """
            Here, custom characters can be defined after which characters should be capitalized. \
            The characters are all interpreted literally.
            """)
    @TextInputWidget(minLength = 1)
    @Layout(DialogLayout.Manipulation.CapitalizeAndPad.class)
    @Effect(signals = {Signals.ChangeCasingIsCapitalize.class, Signals.CapitalizeAfterIsCustom.class},
        operation = And.class, type = EffectType.SHOW)
    String m_changeCasingCapitalizeAfterCharacters = "";

    @Widget(title = "Capitalize character at the start of the string", description = """
            If enabled, a letter at the start of the string is always capitalized.
            """)
    @Layout(DialogLayout.Manipulation.CapitalizeAndPad.class)
    @Effect(signals = Signals.ChangeCasingIsCapitalize.class, type = EffectType.SHOW)
    boolean m_changeCasingCapitalizeFirstCharacter = true;

    enum PadOption {
            @Label("At start")
            START, //
            @Label("At end")
            END, //
            @Label("No")
            NO
    }

    static class IsPadCondition extends OneOfEnumCondition<PadOption> {
        @Override
        public PadOption[] oneOf() {
            return new PadOption[]{PadOption.START, PadOption.END};
        }
    }

    @Widget(title = "Pad", description = """
            Define whether to pad a string if it doesn't have a certain minimum length.
            <ul>
                <li><b>At start</b> adds a fill character to the start of the string.</li>
                <li><b>At end</b> adds a fill character to the end of the string.</li>
                <li><b>No</b> makes no change to the string.</li>
            </ul>
            """)
    @ValueSwitchWidget
    @Layout(DialogLayout.Manipulation.CapitalizeAndPad.class)
    @Signal(id = Signals.DoPad.class, condition = IsPadCondition.class)
    PadOption m_pad = PadOption.NO;

    @Widget(title = "Minimum string length", description = """
            Define the minimum string length. \
            If a string is shorter than this value, a pad will be added to make the string length equal to it.
            """)
    @NumberInputWidget(min = 1)
    @Layout(DialogLayout.Manipulation.PadOptions.class)
    @Effect(signals = Signals.DoPad.class, type = EffectType.SHOW)
    int m_padMinimumStringLength = 1;

    @Widget(title = "Fill character", description = """
            Define the fill character that is used to pad the string.
            """)
    @TextInputWidget(minLength = 1, maxLength = 1)
    @Layout(DialogLayout.Manipulation.PadOptions.class)
    @Effect(signals = Signals.DoPad.class, type = EffectType.SHOW)
    String m_padFillCharacter = "_";

    enum OutputOption {
            @Label("Replace")
            REPLACE, //
            @Label("Append with Suffix")
            APPEND
    }

    static class OutputIsAppendCondition extends OneOfEnumCondition<OutputOption> {
        @Override
        public OutputOption[] oneOf() {
            return new OutputOption[]{OutputOption.APPEND};
        }
    }

    @Widget(title = "Output columns", description = """
            Define whether to clean the strings in-place (i.e. replace the existing values) or append (a) new column(s).
            """)
    @ValueSwitchWidget
    @Layout(DialogLayout.Output.class)
    @Signal(id = Signals.AppendColumnsWithSuffix.class, condition = OutputIsAppendCondition.class)
    OutputOption m_output = OutputOption.REPLACE;

    @Widget(title = "Output column suffix", description = """
            Define a suffix that is appended to the column names of the input table.
            """)
    @TextInputWidget(minLength = 1)
    @Layout(DialogLayout.Output.class)
    @Effect(signals = Signals.AppendColumnsWithSuffix.class, type = EffectType.SHOW)
    String m_outputSuffix = "_clean";

    // Constructor

    /**
     * Constructor for de/serialization.
     */
    StringCleanerNodeSettings() {
        // required by interface
    }

    StringCleanerNodeSettings(final DefaultNodeSettingsContext ctx) {
        m_columnsToClean = ColumnFilter.createDefault(StringColumnChoicesProvider.class, ctx);
    }
}
