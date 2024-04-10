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
 *   Apr 4, 2024 (Paul Bärnreuther): created
 */
package org.knime.base.node.preproc.joiner3;

import java.util.stream.IntStream;

import org.knime.base.node.preproc.joiner3.Joiner3NodeSettings.DuplicateHandling.IsAppendSuffix;
import org.knime.base.node.preproc.joiner3.Joiner3NodeSettings.RowKeyFactory.IsConcatenate;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelColumnFilter2;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings;
import org.knime.core.webui.node.dialog.defaultdialog.layout.After;
import org.knime.core.webui.node.dialog.defaultdialog.layout.HorizontalLayout;
import org.knime.core.webui.node.dialog.defaultdialog.layout.Layout;
import org.knime.core.webui.node.dialog.defaultdialog.layout.Section;
import org.knime.core.webui.node.dialog.defaultdialog.layout.WidgetGroup;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.NodeSettingsPersistor;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.NodeSettingsPersistorWithConfigKey;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.PersistableSettings;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.field.DefaultFieldNodeSettingsPersistorFactory;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.field.DeprecatedConfigs;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.field.Persist;
import org.knime.core.webui.node.dialog.defaultdialog.rule.Effect;
import org.knime.core.webui.node.dialog.defaultdialog.rule.Effect.EffectType;
import org.knime.core.webui.node.dialog.defaultdialog.rule.OneOfEnumCondition;
import org.knime.core.webui.node.dialog.defaultdialog.rule.Signal;
import org.knime.core.webui.node.dialog.defaultdialog.setting.columnfilter.ColumnFilter;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ArrayWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ChoicesWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Label;
import org.knime.core.webui.node.dialog.defaultdialog.widget.NumberInputWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.RadioButtonsWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.StringChoicesStateProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ValueSwitchWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Widget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.choices.SpecialColumns;

/**
 *
 * @author Paul Bärnreuther
 */
@SuppressWarnings({"restriction", "java:S103"}) // we accept too long lines
final class Joiner3NodeSettings implements DefaultNodeSettings {

    @Section(title = "Matching Criteria")
    interface MatchingCriteriaSection {
    }

    @Section(title = "Include in Output")
    @After(MatchingCriteriaSection.class)
    interface IncludeInOutputSection {
    }

    @Section(title = "Output Columns")
    @After(IncludeInOutputSection.class)
    interface OutputColumnsSection {
    }

    @Section(title = "Output Rows")
    @After(OutputColumnsSection.class)
    interface OutputRowsSection {
    }

    @Section(title = "Performance")
    @After(OutputRowsSection.class)
    interface PerformanceSection {
    }

    Joiner3NodeSettings() {
    }

    Joiner3NodeSettings(final DefaultNodeSettingsContext context) {
        context.getDataTableSpec(0).ifPresent(
            spec -> m_leftColumnSelectionConfig = new ColumnFilter(spec.getColumnNames()).withIncludeUnknownColumns());
        context.getDataTableSpec(1).ifPresent(
            spec -> m_rightColumnSelectionConfig = new ColumnFilter(spec.getColumnNames()).withIncludeUnknownColumns());
    }

    enum CompositionMode {
            @Label(value = "All of the following",
                description = "Join rows when all join attributes match (logical and).")
            MATCH_ALL, //
            @Label(value = "Any of the following",
                description = "Join rows when at least one join attribute matches (logical or).")
            MATCH_ANY,
    }

    abstract static class TableChoices implements StringChoicesStateProvider {

        abstract int getInputPortIndex();

        @Override
        public String[] choices(final DefaultNodeSettingsContext context) {
            return context.getDataTableSpec(getInputPortIndex()).map(DataTableSpec::getColumnNames)
                .orElse(new String[0]);
        }

    }

    static class LeftTableChoices extends TableChoices {

        @Override
        int getInputPortIndex() {
            return 0;
        }

    }

    static class RightTableChoices extends TableChoices {

        @Override
        int getInputPortIndex() {
            return 1;
        }

    }

    @ValueSwitchWidget
    @Layout(MatchingCriteriaSection.class)
    @Widget(title = "Match", description = "")
    CompositionMode m_compositionMode = CompositionMode.MATCH_ALL;

    static class MatchingCriterion implements PersistableSettings, WidgetGroup {

        MatchingCriterion() {
            this(SpecialColumns.ROWID.getId(), SpecialColumns.ROWID.getId());
        }

        MatchingCriterion(final String leftColumnName, final String rightColumnName) {
            m_leftTableColumn = leftColumnName;
            m_rightTableColumn = rightColumnName;
        }

        @HorizontalLayout
        interface Horizontal {

        }

        @ChoicesWidget(choicesProvider = LeftTableChoices.class, showRowKeysColumn = true)
        @Widget(title = "Top input ('left' table)", description = "TODO")
        @Layout(Horizontal.class)
        String m_leftTableColumn;

        @ChoicesWidget(choicesProvider = RightTableChoices.class, showRowKeysColumn = true)
        @Widget(title = "Bottom input ('right' table)", description = "TODO")
        @Layout(Horizontal.class)
        String m_rightTableColumn;

    }

    static class MatchingCriteriaPersistor extends NodeSettingsPersistorWithConfigKey<MatchingCriterion[]> {

        static final String LEGACY_LEFT_TABLE_JOIN_PREDICATE_KEY = "leftTableJoinPredicate";

        static final String LEGACY_RIGHT_TABLE_JOIN_PREDICATE_KEY = "rightTableJoinPredicate";

        static final String LEGACY_ROW_ID_COLUMN_ID = "$RowID$";

        private NodeSettingsPersistor<MatchingCriterion[]> m_defaultPersistor;

        @Override
        public void setConfigKey(final String configKey) {
            super.setConfigKey(configKey);
            m_defaultPersistor =
                DefaultFieldNodeSettingsPersistorFactory.createDefaultPersistor(MatchingCriterion[].class, configKey);
        }

        @Override
        public DeprecatedConfigs[] getDeprecatedConfigs() {
            return new DeprecatedConfigs[]{//
                new DeprecatedConfigs.DeprecatedConfigsBuilder() //
                    .forNewConfigPath(getConfigKeys()) //
                    .forDeprecatedConfigPath(LEGACY_LEFT_TABLE_JOIN_PREDICATE_KEY) //
                    .forDeprecatedConfigPath(LEGACY_RIGHT_TABLE_JOIN_PREDICATE_KEY) //
                    .build() //
            };
        }

        @Override
        public MatchingCriterion[] load(final NodeSettingsRO settings) throws InvalidSettingsException {
            if (settings.containsKey(LEGACY_LEFT_TABLE_JOIN_PREDICATE_KEY)) {
                final var leftColumns = settings.getStringArray(LEGACY_LEFT_TABLE_JOIN_PREDICATE_KEY);
                final var rightColumns = settings.getStringArray(LEGACY_RIGHT_TABLE_JOIN_PREDICATE_KEY);
                final var targetLength = Math.max(leftColumns.length, rightColumns.length);
                return IntStream.range(0, targetLength)
                    .mapToObj(i -> getNthMatchingCriterion(i, leftColumns, rightColumns))
                    .toArray(MatchingCriterion[]::new);
            }
            return m_defaultPersistor.load(settings);
        }

        private static MatchingCriterion getNthMatchingCriterion(final int i, final String[] leftColumns,
            final String[] rightColumns) {
            return new MatchingCriterion(getNthColumnName(i, leftColumns), getNthColumnName(i, rightColumns));
        }

        private static String getNthColumnName(final int i, final String[] columns) {
            if (i >= columns.length) {
                return null;
            }
            final var column = columns[i];
            if (LEGACY_ROW_ID_COLUMN_ID.equals(column)) {
                return SpecialColumns.ROWID.getId();
            }
            return column;
        }

        @Override
        public void save(final MatchingCriterion[] obj, final NodeSettingsWO settings) {
            m_defaultPersistor.save(obj, settings);

        }

    }

    @Widget(title = "Join columns", description = """
            Select the columns from the top input ('left' table) and the bottom input
            ('right' table) that should be used for joining. Each pair of columns defines an equality constraint
            of the form A = B. For two rows to be joined, the row from the left input table
            must have the same value in column A as the row from the right input table in column B.
            Row keys can be compared to row keys or regular columns, in which case the row key will be interpreted
            as a string value.
                """)
    @Layout(MatchingCriteriaSection.class)
    @ArrayWidget(addButtonText = "Add matching criterion")
    @Persist(customPersistor = MatchingCriteriaPersistor.class)
    MatchingCriterion[] m_matchingCriteria = new MatchingCriterion[]{new MatchingCriterion()};

    enum DataCellComparisonMode {
            @Label(value = "Value and Type", description = "Two cells need to have the exact same value and type. "
                + "For instance, a long and an integer cell will never match.")
            STRICT, //
            @Label(value = "String representation",
                description = "Convert values in join columns to string before comparing them.")
            STRING,

            @Label(value = "making integer types compatible",
                description = "Ignore type differences for numerical types. "
                    + "For instance, an integer cell with value 1 will match a long cell with value 1.")
            NUMERIC
    }

    @Widget(title = "Compare values in join columns by", description = "TODO")
    @Layout(MatchingCriteriaSection.class)
    @Persist(/* Introduced with KNIME 4.4*/ optional = true)
    DataCellComparisonMode m_dataCellComparisonMode = DataCellComparisonMode.STRICT;

    @Widget(title = "Matching rows", description = "Include rows that aggree on the selected column pairs.")
    @Layout(IncludeInOutputSection.class)
    boolean m_includeMatchesInOutput = true;

    @Widget(title = "Left unmatched rows",
        description = "Include rows from the left input table for which no matching row in the right input table is found.")
    @Layout(IncludeInOutputSection.class)
    boolean m_includeLeftUnmatchedInOutput;

    @Widget(title = "Right unmatched rows",
        description = "Include rows from the right input table for which no matching row in the left input table is found.")
    @Layout(IncludeInOutputSection.class)
    boolean m_includeRightUnmatchedInOutput;

    enum RowKeyFactory {
            @Label(value = "Concatenate with separator", description = "For instance, when selecting separator \"_\", "
                + "a row joining rows with keys Row0 and Row1 is assigned key Row0_Row1.")
            CONCATENATE, //
            @Label(value = "Create new",
                description = "Output rows are assigned sequential row keys, e.g., Row0, Row1, etc. ")
            SEQUENTIAL, //
            @Label(value = "Retain",
                description = "Only available when join criteria ensure that matching rows have the same row keys.")
            KEEP_ROWID;

        static class IsConcatenate extends OneOfEnumCondition<RowKeyFactory> {

            @Override
            public RowKeyFactory[] oneOf() {
                return new RowKeyFactory[]{CONCATENATE};
            }

        }
    }

    @Widget(title = "Top input ('left' table)", description = "TODO")
    @Layout(OutputColumnsSection.class)
    @Persist(settingsModel = SettingsModelColumnFilter2.class)
    @ChoicesWidget(choicesProvider = LeftTableChoices.class)
    ColumnFilter m_leftColumnSelectionConfig;

    @Widget(title = "Bottom input ('right' table)", description = "TODO")
    @Layout(OutputColumnsSection.class)
    @Persist(settingsModel = SettingsModelColumnFilter2.class)
    @ChoicesWidget(choicesProvider = RightTableChoices.class)
    ColumnFilter m_rightColumnSelectionConfig;

    @Widget(title = "Merge join columns",
        description = """
                If active, the join columns of the right input table are merged into their join partners of the left
                input table. The merged column is named like the left join column if one of its join partners in the
                right table has the same name. If the join partners have different names, the merged column is named
                in the form <i>left column=right column</i>.<br/>
                For instance, when joining a table with columns A, B, and C as left input table with a table
                that has columns X, A, and Z using the join predicates A=A, A=X, and C=Z, the resulting output table
                would have columns A, B, C=Z. Note how the column A in the output table contains the value of the column
                A in the left table, which is also the value of the column X in the right table, as required by the join conditions  A=X.<br/>
                The value of a merged join column for an unmatched row is taken from whichever row has values.
                For instance, when outputting an unmatched row from the right table in the above example with values x, a, and z,
                the resulting row in format A, B, C=Z has values x, ?, z. <br/> When merge join columns is off, the row is
                instead output as ?, ?, ?, x, a, z.
                """)
    @Layout(OutputColumnsSection.class)
    boolean m_mergeJoinColumns;

    enum DuplicateHandling {//
            @Label(value = "Append custom suffix", description = "Appends the given suffix.")
            APPEND_SUFFIX, //
            @Label(value = "Do not execute",
                description = "Prevents the node from being executed if column names clash.")
            DO_NOT_EXECUTE;

        static class IsAppendSuffix extends OneOfEnumCondition<DuplicateHandling> {

            @Override
            public DuplicateHandling[] oneOf() {
                return new DuplicateHandling[]{APPEND_SUFFIX};
            }

        }
    }

    @Widget(title = "If there are mulitple column names", description = "TODO")
    @Signal(condition = IsAppendSuffix.class)
    @Layout(OutputColumnsSection.class)
    @RadioButtonsWidget
    DuplicateHandling m_duplicateHandling = DuplicateHandling.APPEND_SUFFIX;

    @Widget(title = "Custom suffix", description = "")
    @Effect(signals = IsAppendSuffix.class, type = EffectType.SHOW)
    @Layout(OutputColumnsSection.class)
    String m_suffix = " (Right)";

    @Widget(
        title = "Split join result into multiple tables (top = matching rows, middle = left unmatched rows, bottom = right unmatched rows)",
        description = "Output unmatched rows (if selected under \"Include in"
            + " output\") at the second and third output port.")
    @Layout(OutputRowsSection.class)
    boolean m_outputUnmatchedRowsToSeparatePorts;

    @Widget(title = "RowIDs", description = "Row keys of the output rows")
    @Signal(condition = IsConcatenate.class)
    @RadioButtonsWidget
    @Layout(OutputRowsSection.class)
    RowKeyFactory m_rowKeyFactory = RowKeyFactory.CONCATENATE;

    @Widget(title = "Separator", description = "")
    @Effect(signals = IsConcatenate.class, type = EffectType.SHOW)
    @Layout(OutputRowsSection.class)
    String m_rowKeySeparator = "_";

    enum OutputRowOrder {
            /** Output rows may be provided in any order. */
            @Label(value = "Arbitrary",
                description = "The output can vary depending on the currently "
                    + "available amount of main memory. This means that identical input can produce different output"
                    + " orders on consecutive executions.")
            ARBITRARY, //
            @Label(value = "Input order", description = "Rows are output in three blocks:" //
                + "<ol>                                    " //
                + "<li>matched rows</li>                   " //
                + "<li>unmatched rows from left table</li> " //
                + "<li>unmatched rows from right table</li>" //
                + "</ol>                                   "
                + "Each block is sorted by row offset in the left table, breaking ties using the "
                + "row offset in the right table.")
            LEFT_RIGHT;
    }

    @Widget(title = "Row order", description = "TODO")
    @Layout(PerformanceSection.class)
    @ValueSwitchWidget
    OutputRowOrder m_outputRowOrder = OutputRowOrder.ARBITRARY;

    @Widget(title = "Maximum number of temporary files", description = """
             Controls the number of temporary files that can be created during
                 the join operation and possibly subsequent sorting operations. More temporary files may increase
                 performance, but the operating system might impose a limit on the maximum number of open files.
            """)
    @Layout(PerformanceSection.class)
    @NumberInputWidget(min = 3)
    int m_maxOpenFiles = 200;

    @Widget(title = "Hiliting enabled", description = "Track which output rows have been produced by which input rows.")

    @Layout(PerformanceSection.class)
    boolean m_enableHiliting;

}
