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

import java.util.List;
import java.util.stream.IntStream;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.webui.node.dialog.configmapping.ConfigMigration;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings;
import org.knime.core.webui.node.dialog.defaultdialog.layout.After;
import org.knime.core.webui.node.dialog.defaultdialog.layout.CheckboxesWithVennDiagram;
import org.knime.core.webui.node.dialog.defaultdialog.layout.HorizontalLayout;
import org.knime.core.webui.node.dialog.defaultdialog.layout.Layout;
import org.knime.core.webui.node.dialog.defaultdialog.layout.Section;
import org.knime.core.webui.node.dialog.defaultdialog.layout.WidgetGroup;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.api.Migrate;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.api.Migration;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.api.NodeSettingsMigration;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.api.PersistableSettings;
import org.knime.core.webui.node.dialog.defaultdialog.setting.columnfilter.ColumnFilter;
import org.knime.core.webui.node.dialog.defaultdialog.setting.columnfilter.LegacyColumnFilterMigration;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ArrayWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ChoicesWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Label;
import org.knime.core.webui.node.dialog.defaultdialog.widget.NumberInputWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.RadioButtonsWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.StringChoicesStateProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ValueSwitchWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Widget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.choices.SpecialColumns;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.Effect;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.Effect.EffectType;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.Predicate;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.PredicateProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.Reference;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.ValueReference;
import org.knime.core.webui.node.dialog.defaultdialog.widget.validation.NumberInputWidgetValidation.MinValidation;

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

        @CheckboxesWithVennDiagram
        interface WithVennDiagram {

            interface Matched {
            }

            @After(Matched.class)
            interface LeftUnmatched {
            }

            @After(LeftUnmatched.class)
            interface RightUnmatched {
            }
        }
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
            spec -> m_leftColumnSelectionConfigV2 = new ColumnFilter(spec.getColumnNames()).withIncludeUnknownColumns());
        context.getDataTableSpec(1).ifPresent(
            spec -> m_rightColumnSelectionConfigV2 = new ColumnFilter(spec.getColumnNames()).withIncludeUnknownColumns());
    }

    enum CompositionMode {
            @Label(value = "All of the following",
                description = "If selected, joins two rows only when all matching criteria are satisfied")
            MATCH_ALL, //
            @Label(value = "Any of the following",
                description = "If selected, joins two rows when at least one of the matching criteria is satisfied")
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
    @Widget(title = "Match", description = "Defines the logic for the matching criteria:")
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
        @Widget(title = "Top input ('left' table)",
            description = "Select the column from the top input table that should be "
                + "used to compare with the column selected for the bottom input.")
        @Layout(Horizontal.class)
        String m_leftTableColumn;

        @ChoicesWidget(choicesProvider = RightTableChoices.class, showRowKeysColumn = true)
        @Widget(title = "Bottom input ('right' table)",
            description = "Select the column from the bottom input table that should be "
                + "used to compare with the column selected for the top input.")
        @Layout(Horizontal.class)
        String m_rightTableColumn;

    }

    /**
     * Previously, the matching criteria were stored in separate arrays of equal length.
     *
     * @author Paul Bärnreuther
     */
    static class MatchingCriteriaMigration implements NodeSettingsMigration<MatchingCriterion[]> {

        static final String LEGACY_LEFT_TABLE_JOIN_PREDICATE_KEY = "leftTableJoinPredicate";

        static final String LEGACY_RIGHT_TABLE_JOIN_PREDICATE_KEY = "rightTableJoinPredicate";

        static final String LEGACY_ROW_ID_COLUMN_ID = "$RowID$";

        @Override
        public List<ConfigMigration<MatchingCriterion[]>> getConfigMigrations() {
            return List.of(ConfigMigration.builder(MatchingCriteriaMigration::loadFromLegacyLeftTableJoinPredicate) //
                .withDeprecatedConfigPath(LEGACY_LEFT_TABLE_JOIN_PREDICATE_KEY) //
                .withDeprecatedConfigPath(LEGACY_RIGHT_TABLE_JOIN_PREDICATE_KEY) //
                .build());

        }

        private static MatchingCriterion[] loadFromLegacyLeftTableJoinPredicate(final NodeSettingsRO settings)
            throws InvalidSettingsException {
            final var leftColumns = settings.getStringArray(LEGACY_LEFT_TABLE_JOIN_PREDICATE_KEY);
            final var rightColumns = settings.getStringArray(LEGACY_RIGHT_TABLE_JOIN_PREDICATE_KEY);
            final var targetLength = Math.max(leftColumns.length, rightColumns.length);
            return IntStream.range(0, targetLength).mapToObj(i -> getNthMatchingCriterion(i, leftColumns, rightColumns))
                .toArray(MatchingCriterion[]::new);
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

    }

    @Widget(title = "Join columns",
        description = """
                Defines the columns from the top input ('left' table) and the bottom input ('right' table) that should be used for joining.
                For two rows to be joined, the row from the left input table must have the same value in column A as the row from the right
                 input table in column B. RowIDs can be compared to RowIDs or regular columns, in which case the RowID will be interpreted
                 as a string value.
                    """)
    @Layout(MatchingCriteriaSection.class)
    @ArrayWidget(addButtonText = "Add matching criterion")
    @Migration(MatchingCriteriaMigration.class)
    MatchingCriterion[] m_matchingCriteria = new MatchingCriterion[]{new MatchingCriterion()};

    enum DataCellComparisonMode {
            @Label(value = "Value and type",
                description = "Two rows match only if their values in the join columns selected have the same value and type, "
                    + "e.g. Number (integer) values will never match Number (long) values because they have two different types.")
            STRICT, //
            @Label(value = "String representation",
                description = "Use this option if you want the values to be converted to string before comparing them. "
                    + "In this way you compare only the value in the selected join columns.")
            STRING,

            @Label(value = "Make integer types compatible",
                description = "Use this option to ignore type differences between Number (integer) and Number (long) types.")
            NUMERIC
    }

    @Widget(title = "Compare values in join columns by",
        description = "Defines how to compare the values in the join columns:")
    @Layout(MatchingCriteriaSection.class)
    @Migrate(/* Introduced with KNIME 4.4*/ loadDefaultIfAbsent = true)
    DataCellComparisonMode m_dataCellComparisonMode = DataCellComparisonMode.STRICT;

    @Widget(title = "Matching rows", description = "Include rows that match on the selected column pairs.")
    @Layout(IncludeInOutputSection.WithVennDiagram.Matched.class)
    boolean m_includeMatchesInOutput = true;

    @Widget(title = "Left unmatched rows",
        description = "Include rows from the left input table for which no matching row in the right input table is found.")
    @Layout(IncludeInOutputSection.WithVennDiagram.LeftUnmatched.class)
    boolean m_includeLeftUnmatchedInOutput;

    @Widget(title = "Right unmatched rows",
        description = "Include rows from the right input table for which no matching row in the left input table is found.")
    @Layout(IncludeInOutputSection.WithVennDiagram.RightUnmatched.class)
    boolean m_includeRightUnmatchedInOutput;

    enum RowKeyFactory {
            @Label(value = "Concatenate with separator",
                description = "The RowID of the output table will be made of the RowID of the top input ('left' table) "
                    + "and the RowID of the bottom ('right' table) separated by the defined separator.")
            CONCATENATE, //
            @Label(value = "Create new",
                description = "The RowIDs of the output table will be assigned sequential RowIDs, e.g. Row0, Row1, etc.")
            SEQUENTIAL, //
            @Label(value = "Retain",
                description = "If the matching rows have the same RowIDs in both input tables as a matching criteria the"
                    + " output table will keep the input tables RowIDs.")
            KEEP_ROWID;

    }

    static final class LeftColumnSelectionMigration extends LegacyColumnFilterMigration {

        LeftColumnSelectionMigration() {
            super("leftColumnSelectionConfig");
        }
    }

    @Widget(title = "Top input ('left' table)",
        description = "Select columns from top input ('left' table) that should be included or excluded in the output table.")
    @Layout(OutputColumnsSection.class)
    @Migration(LeftColumnSelectionMigration.class)
    @ChoicesWidget(choicesProvider = LeftTableChoices.class)
    ColumnFilter m_leftColumnSelectionConfigV2 = new ColumnFilter();

    static final class RightColumnSelectionMigration extends LegacyColumnFilterMigration {

        RightColumnSelectionMigration() {
            super("rightColumnSelectionConfig");
        }
    }

    @Widget(title = "Bottom input ('right' table)",
        description = "Select columns from bottom input ('right' table) that should be included or excluded in the output table.")
    @Layout(OutputColumnsSection.class)
    @Migration(RightColumnSelectionMigration.class)
    @ChoicesWidget(choicesProvider = RightTableChoices.class)
    ColumnFilter m_rightColumnSelectionConfigV2 = new ColumnFilter();

    @Widget(title = "Merge join columns",
        description = """
                If selected, the join columns of the right input table are merged into their join partners of the left input table.
                The merged column is named like the left join column if one of its join partners in the right table has the same name.
                If the join partners have different names, the merged column is named in the form "left column=right column".
                """)
    @Layout(OutputColumnsSection.class)
    boolean m_mergeJoinColumns;

    enum DuplicateHandling {//
            @Label(value = "Append custom suffix",
                description = "Adds the defined custom suffix to the column name of the right table.")
            APPEND_SUFFIX, //
            @Label(value = "Do not execute",
                description = "Prevents the node to be executed if the columns have the same name.")
            DO_NOT_EXECUTE;
    }

    interface DuplicateHandlingRef extends Reference<DuplicateHandling> {
    }

    static final class IsAppendSuffix implements PredicateProvider {
        @Override
        public Predicate init(final PredicateInitializer i) {
            return i.getEnum(DuplicateHandlingRef.class).isOneOf(DuplicateHandling.APPEND_SUFFIX);
        }
    }

    @Widget(title = "If there are duplicate column names",
        description = "Defines what should happen if there are column names included in the output that have the same name:")
    @ValueReference(DuplicateHandlingRef.class)
    @Layout(OutputColumnsSection.class)
    @RadioButtonsWidget
    DuplicateHandling m_duplicateHandling = DuplicateHandling.APPEND_SUFFIX;

    @Widget(title = "Custom suffix", description = "The suffix to be added to the column name of the right table")
    @Effect(predicate = IsAppendSuffix.class, type = EffectType.SHOW)
    @Layout(OutputColumnsSection.class)
    String m_suffix = " (Right)";

    @Widget(title = "Split join result into multiple tables",
        description = "Output unmatched rows (if selected under \"Include in"
            + " output\") at the second and third output port, i.e." //
            + "<ul>" //
            + "<li> top: Matching rows </li>" //
            + "<li> middle: Left unmatched rows </li>" //
            + "<li> bottom: Right unmatched rows </li>" //
            + "</ul>")

    @Layout(OutputRowsSection.class)
    boolean m_outputUnmatchedRowsToSeparatePorts;

    interface RowKeyFactoryRef extends Reference<RowKeyFactory> {
    }

    static final class IsConcatenate implements PredicateProvider {
        @Override
        public Predicate init(final PredicateInitializer i) {
            return i.getEnum(RowKeyFactoryRef.class).isOneOf(RowKeyFactory.CONCATENATE);
        }
    }

    @Widget(title = "RowIDs", description = "Defines how the RowIDs of the output table are generated:")
    @ValueReference(RowKeyFactoryRef.class)
    @RadioButtonsWidget
    @Layout(OutputRowsSection.class)
    RowKeyFactory m_rowKeyFactory = RowKeyFactory.CONCATENATE;

    @Widget(title = "Separator",
        description = "The separator to be added in between RowIDs of the input tables to generate the RowIDs of the output table.")
    @Effect(predicate = IsConcatenate.class, type = EffectType.SHOW)
    @Layout(OutputRowsSection.class)
    String m_rowKeySeparator = "_";

    enum OutputRowOrder {
            /** Output rows may be provided in any order. */
            @Label(value = "Arbitrary",
                description = "The order of the ouput table rows is defined based on the currently available memory. "
                    + "Select this to improve the performance of the node since the output does not have to be sorted. "
                    + "Be aware that it can produce different output orders on consecutive executions.")
            ARBITRARY, //
            @Label(value = "Input order", description = "Rows are output in three blocks:" //
                + "<ol>                                    " //
                + "<li>matched rows</li>                   " //
                + "<li>unmatched rows from left table</li> " //
                + "<li>unmatched rows from right table</li>" //
                + "</ol>                                   "
                + "Each block is sorted so that rows are sorted based on their position in the left table. "
                + "In case of rows with the same position in the left table, "
                + "the sorting is determined by the row position in the right table.")
            LEFT_RIGHT;
    }

    @Widget(title = "Row order", description = "Defines the row order for the output table rows.")
    @Layout(PerformanceSection.class)
    @ValueSwitchWidget
    OutputRowOrder m_outputRowOrder = OutputRowOrder.ARBITRARY;

    static final class OpenFilesMinValidation extends MinValidation {
        @Override
        protected double getMin() {
            return 3;
        }
    }

    @Widget(title = "Maximum number of temporary files",
        description = """
                 Defines the number of temporary files that can be created during the join operation and possibly subsequent sorting operations.
                 Increase the number of temporary files to improve the performance of the node.
                 Be aware that the operating system might impose a limit on the maximum number of open files.
                """)
    @Layout(PerformanceSection.class)
    @NumberInputWidget(validation = OpenFilesMinValidation.class)
    int m_maxOpenFiles = 200;

    @Widget(title = "Hiliting enabled",
        description = "If selected, hiliting rows in the output will hilite the rows in the left and right input tables that contributed to that row. "
            + "Equally, when hiliting a row in one of the input tables, all rows that the input row contributed to are hilited. "
            + "Disabling this option reduces the memory footprint of the joiner, the disk footprint of the workflow, "
            + "and may speed up the execution in cases where main memory is scarce.")

    @Layout(PerformanceSection.class)
    boolean m_enableHiliting;
}
