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
package org.knime.base.node.preproc.valuelookup;

import java.util.stream.Stream;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings;
import org.knime.core.webui.node.dialog.defaultdialog.layout.After;
import org.knime.core.webui.node.dialog.defaultdialog.layout.Layout;
import org.knime.core.webui.node.dialog.defaultdialog.layout.Section;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.api.Migrate;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.api.NodeSettingsPersistor;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.api.Persistor;
import org.knime.core.webui.node.dialog.defaultdialog.setting.columnfilter.ColumnFilter;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ChoicesWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ColumnChoicesProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Label;
import org.knime.core.webui.node.dialog.defaultdialog.widget.RadioButtonsWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ValueSwitchWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Widget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.Effect;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.Effect.EffectType;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.Predicate;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.PredicateProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.Reference;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.ValueReference;

/**
 * Node Settings for the Value Lookup Node
 *
 * @author Jasper Krauter, KNIME GmbH, Konstanz, Germany
 */
@SuppressWarnings("restriction") // New Node UI is not yet API
public final class ValueLookupNodeSettings implements DefaultNodeSettings {

    /** How Strings in the target column / dictionary lookup shall be handled */
    enum StringMatching {
            /** Only match exact correspondence */
            @Label("Full string")
            FULLSTRING,
            /** Match if dictionary lookup is substring of target column */
            @Label("Substring")
            SUBSTRING,
            /** Allow Wildcards in dictionary lookup column */
            @Label("Wildcard")
            WILDCARD,
            /** Allow Regex in dictionary lookup column */
            @Label("Regex")
            REGEX;
    }

    /** Whether only exact matches are acceptable or the next-lower or next-higher match is also of interest */
    enum MatchBehaviour {
            /** Only match if the number is one of the dict values */
            @Label("Insert missing values")
            EQUAL,
            /** Match to the queried number or, if not available, the next lower number */
            @Label("Match next smaller")
            EQUALORSMALLER,
            /** Match to the queried number or, if not available, the next higher number */
            @Label("Match next larger")
            EQUALORLARGER;
    }

    /** In what direction to search (determines which match is selected, can speed up things) */
    enum SearchDirection {
            /** Search forwards through input table, select first match */
            @Label("Use first")
            FORWARD,
            /** Search backwards through input table, select last match */
            @Label("Use last")
            BACKWARD;
    }

    /**
     * Supersedes the <code>delete lookup column</code> setting.
     *
     * @since 5.2
     */
    enum LookupColumnOutput {
            /** Leave the lookup column unchanged. */
            @Label("Retain")
            RETAIN,
            /**
             * Replace the value in the cell in the lookup column with a value from a cell in a selected column from the
             * dictionary table if there is a match. If there is no match, use {@link LookupColumnNoMatchReplacement} to
             * determine the value.
             */
            @Label("Replace")
            REPLACE,
            /** Remove the lookup column. */
            @Label("Remove")
            REMOVE;

        private static final class LookupColumnOutputPersistor implements NodeSettingsPersistor<LookupColumnOutput> {

            private static final String CFG_LOOKUP_COLUMN_OUTPUT = "lookupColumnOutput";

            private static final String CFG_51_DELETE = "deleteLookupCol";

            @Override
            public LookupColumnOutput load(final NodeSettingsRO settings) throws InvalidSettingsException {
                // pre 5.2, there was only boolean m_deleteLookupCol
                if (settings.containsKey(CFG_51_DELETE)) {
                    final var deleteLookupCol = settings.getBoolean(CFG_51_DELETE);
                    return deleteLookupCol ? REMOVE : RETAIN;
                } else {
                    final var name = settings.getString(CFG_LOOKUP_COLUMN_OUTPUT);
                    return LookupColumnOutput.valueOf(name);
                }
            }

            @Override
            public void save(final LookupColumnOutput policy, final NodeSettingsWO settings) {
                settings.addString(CFG_LOOKUP_COLUMN_OUTPUT, policy == null ? null : policy.name());
            }

            @Override
            public String[][] getConfigPaths() {
                return new String[][]{{CFG_LOOKUP_COLUMN_OUTPUT}};
            }
        }
    }

    enum LookupColumnNoMatchReplacement {
            /** Use the lookup value - can create a mixed type column. */
            @Label("Retain")
            RETAIN,
            /** Insert a missing value. */
            @Label("Insert missing")
            INSERT_MISSING
    }

    /** Provides the column choices of the table at input port 0 */
    static final class DataTableChoices implements ColumnChoicesProvider {
        @Override
        public DataColumnSpec[] columnChoices(final DefaultNodeSettingsContext context) {
            return context.getDataTableSpec(0)//
                .map(DataTableSpec::stream)//
                .orElseGet(Stream::empty)//
                .toArray(DataColumnSpec[]::new);
        }
    }

    /** Provides the column choices of the table at input port 1 */
    static final class DictionaryTableChoices implements ColumnChoicesProvider {
        @Override
        public DataColumnSpec[] columnChoices(final DefaultNodeSettingsContext context) {
            return context.getDataTableSpec(1)//
                .map(DataTableSpec::stream)//
                .orElseGet(Stream::empty)//
                .toArray(DataColumnSpec[]::new);
        }

        static String[] choices(final DataTableSpec spec) {
            return spec.stream().map(DataColumnSpec::getName).toArray(String[]::new);
        }
    }

    @Section(title = "Matching")
    interface MatchingSection {
    }

    // ---- Match options

    /** The name of the lookup column in the data table */
    @Widget(title = "Lookup column (data table)", //
        description = "The column in the data table that will be used to look up cells in the dictionary.") //
    @ChoicesWidget(choices = DataTableChoices.class)
    @Layout(MatchingSection.class)
    String m_lookupCol;

    /** The name of the key column in the dictionary table */
    @Widget(title = "Key column (dictionary table)", //
        description = "The column in the dictionary table that contains the search key / criterion.") //
    @ChoicesWidget(choices = DictionaryTableChoices.class)
    @Layout(MatchingSection.class)
    String m_dictKeyCol;

    /** The search direction (forwards / backwards / binSearch) */
    @Widget(title = "If multiple rows match", //
        description = "Defines the behavior in case there are multiple matching keys in the dictionary table.")
    @ValueSwitchWidget
    @Layout(MatchingSection.class)
    SearchDirection m_searchDirection = SearchDirection.FORWARD;

    interface MatchBehaviourRef extends Reference<MatchBehaviour> {
    }

    static final class MatchBehaviourIsNotEqual implements PredicateProvider {
        @Override
        public Predicate init(final PredicateInitializer i) {
            return i.getEnum(MatchBehaviourRef.class).isOneOf(MatchBehaviour.EQUALORSMALLER,
                MatchBehaviour.EQUALORLARGER);
        }
    }

    /** The matching behaviour (only exact, exact or next lower, exact or next higher) */
    @Widget(title = "If no row matches", //
        description = "Defines what happens when a lookup key is not present in the dictionary: "
            + "If \"Insert missing values\" is selected, missing values are inserted. "
            + "If \"Match next smaller\" (\"- larger\") is selected, the next smaller (larger) value from the "
            + "dictionary is matched, based on the value of the lookup key. "
            + "If no such element can be found, a missing value is inserted.")
    @RadioButtonsWidget(horizontal = true)
    @ValueReference(MatchBehaviourRef.class)
    @Layout(MatchingSection.class)
    MatchBehaviour m_matchBehaviour = MatchBehaviour.EQUAL;

    /** The selected string match behaviour */
    @Widget(title = "String matching", //
        description = "The matching behavior when matching strings: "
            + "Full string matching matches a lookup string only if it exactly matches a search string. "
            + "Substring matching matches a lookup string if the key in the dictionary is a substring of it. "
            + "Wildcard and Regex matching match a lookup string if a pattern in the dictionary matches it.",
        advanced = true)
    @ValueSwitchWidget
    @Effect(predicate = MatchBehaviourIsNotEqual.class, type = EffectType.DISABLE)
    @Layout(MatchingSection.class)
    StringMatching m_stringMatchBehaviour = StringMatching.FULLSTRING;

    /** Whether the string match shall be case sensitive */
    @Widget(title = "Match strings case-sensitive", //
        description = "When enabled, the string matching will be case-sensitive, otherwise case-insensitive.",
        advanced = true)
    @Layout(MatchingSection.class)
    @Effect(predicate = MatchBehaviourIsNotEqual.class, type = EffectType.DISABLE)
    boolean m_caseSensitive = true;

    // ---- Output options

    @Section(title = "Output")
    @After(MatchingSection.class)
    interface OutputSection {
    }

    interface LookupColumnOutputRef extends Reference<LookupColumnOutput> {
    }

    static final class ShowLookupColumnReplacement implements PredicateProvider {
        @Override
        public Predicate init(final PredicateInitializer i) {
            return i.getEnum(LookupColumnOutputRef.class).isOneOf(LookupColumnOutput.REPLACE);
        }
    }

    /** Whether to keep, replace, or delete the lookup column in the output table */
    @Widget(title = "Lookup column output", //
        description = """
                Defines the content of the column that is selected as lookup column (data table).
                If "Retain" the content of the lookup column is left unchanged.
                If "Replace" is selected, the cell contents are replaced with values from the dictionary table.
                If a match is found, the selected column's value is inserted, otherwise the original value can be kept
                or a missing value can be inserted. The name of the column does not change.
                If "Delete" is selected, the lookup column is removed entirely from the output table.
                """)
    @ValueSwitchWidget
    @ValueReference(LookupColumnOutputRef.class)
    @Persistor(LookupColumnOutput.LookupColumnOutputPersistor.class)
    @Layout(OutputSection.class)
    LookupColumnOutput m_lookupColumnOutput = LookupColumnOutput.RETAIN;

    /**
     * The name of the dictionary column to replace the lookup column in the data table
     *
     * @since 5.2
     */
    @Widget(title = "Replacement column", //
        description = """
                The column from the dictionary table that provides
                the new values for the lookup column in the data table.
                """)
    @ChoicesWidget(choices = DictionaryTableChoices.class)
    @Effect(type = EffectType.SHOW, predicate = ShowLookupColumnReplacement.class)
    @Layout(OutputSection.class)
    @Migrate(loadDefaultIfAbsent = true)
    String m_lookupReplacementCol;

    /**
     * What to do with non-matching rows in case the data table's lookup column is to be replaced with a dictionary
     * column.
     *
     * @since 5.2
     */
    @Widget(title = "If no match found", description = """
            Defines the content of the lookup column if no match is found in the dictionary table.
                If "Retain" is selected, the cell is left as is.
                If "Insert missing" is selected, a missing value is used as content for the cell in the lookup column.
            """)
    @ValueSwitchWidget
    @Effect(type = EffectType.SHOW, predicate = ShowLookupColumnReplacement.class)
    @Layout(OutputSection.class)
    @Migrate(loadDefaultIfAbsent = true)
    LookupColumnNoMatchReplacement m_columnNoMatchReplacement = LookupColumnNoMatchReplacement.RETAIN;

    /** The names of the columns from the dictionary table that shall be added to the output table */
    @Widget(title = "Append columns (from dictionary table)", //
        description = "The columns in the dictionary table that contain the values added to the data table.") //
    @ChoicesWidget(choices = DictionaryTableChoices.class)
    @Layout(OutputSection.class)
    ColumnFilter m_dictValueCols;

    /** Whether to create a column that indicates whether a match has been found */
    @Widget(title = "Append a column indicating whether a match was found", //
        description = "When checked, a new column \"" + ValueLookupNodeModel.COLUMN_NAME_MATCHFOUND
            + "\" is appended to the output that contains a boolean indicating whether a match was found.")
    @Layout(OutputSection.class)
    boolean m_createFoundCol = false; //NOSONAR: more verbosity

    @Widget(title = "Enable hiliting", advanced = true,
        description = "Enable hiliting between the dictionary table and the output table.")
    @Layout(OutputSection.class)
    @Migrate(loadDefaultIfAbsent = true)
    boolean m_enableHiliting = false; //NOSONAR: more verbosity

    /**
     * Constructor for de/serialization.
     */
    ValueLookupNodeSettings() {
        // required by interface
    }

    ValueLookupNodeSettings(final DefaultNodeSettingsContext ctx) {
        m_dictValueCols = ColumnFilter.createDefault(DictionaryTableChoices.class, ctx);
    }
}
