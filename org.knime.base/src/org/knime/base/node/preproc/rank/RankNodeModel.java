/*
 * ------------------------------------------------------------------------
 *
x *  Copyright by KNIME AG, Zurich, Switzerland
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
 *   Feb 5, 2025 (Martin Sillye, TNG Technology Consulting GmbH): created
 */
package org.knime.base.node.preproc.rank;

import java.util.Arrays;
import java.util.Collections;
import java.util.OptionalInt;

import org.knime.base.node.preproc.rank.RankNodeSettings.RankDataType;
import org.knime.base.node.preproc.rank.RankNodeSettings.RankingCriterionSettings;
import org.knime.base.node.preproc.rank.RankNodeSettings.RowOrder;
import org.knime.base.node.util.preproc.SortingUtils.SortingCriterionSettings;
import org.knime.base.node.util.preproc.SortingUtils.SortingOrder;
import org.knime.base.node.util.preproc.SortingUtils.StringComparison;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.StringValue;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.container.SingleCellFactory;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.LongCell;
import org.knime.core.data.sort.BufferedDataTableSorter;
import org.knime.core.data.sort.RowComparator;
import org.knime.core.data.sort.RowComparator.ColumnComparatorBuilder;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.util.UniqueNameGenerator;
import org.knime.core.webui.node.dialog.defaultdialog.setting.singleselection.RowIDChoice;
import org.knime.core.webui.node.dialog.defaultdialog.setting.singleselection.StringOrEnum;
import org.knime.core.webui.node.impl.WebUINodeConfiguration;
import org.knime.core.webui.node.impl.WebUINodeModel;

/**
 *
 * @author Martin Sillye, TNG Technology Consulting GmbH
 */
@SuppressWarnings("restriction")
final class RankNodeModel extends WebUINodeModel<RankNodeSettings> {

    RankNodeModel(final WebUINodeConfiguration configuration) {
        super(configuration, RankNodeSettings.class);
    }

    private static class OrderCellFactory extends SingleCellFactory {

        OrderCellFactory(final DataColumnSpec newColSpec) {
            super(newColSpec);
        }

        @Override
        public DataCell getCell(final DataRow row, final long rowIndex) {
            return new LongCell(rowIndex);
        }

    }

    @Override
    protected void validateSettings(final RankNodeSettings settings) throws InvalidSettingsException {
        final var distinctLength = Arrays.stream(settings.m_sortingCriteria).map(RankingCriterionSettings::getColumn)
            .map(col -> col.getEnumChoice().isPresent() ? col.getEnumChoice().get().name() : col.getStringChoice())
            .distinct().count();
        if (distinctLength != settings.m_sortingCriteria.length) {
            throw new InvalidSettingsException(
                "The same column cannot be selected multiple times for rank ordering criterion.");
        }
    }

    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs, final RankNodeSettings modelSettings)
        throws InvalidSettingsException {
        RankingCriterionSettings[] rankCols = modelSettings.m_sortingCriteria;
        // check if at least one ranking column is selected
        if (rankCols.length == 0) {
            throw new InvalidSettingsException("No ranking column is selected.");
        }
        var tableSpec = inSpecs[0];
        final var sortingColumns = Arrays.stream(rankCols).map(SortingCriterionSettings::getColumn)
            .filter(column -> column.getEnumChoice().isEmpty()).map(StringOrEnum::getStringChoice).toList();
        checkAllContained(sortingColumns.toArray(String[]::new), "ranking", tableSpec);
        final var availableColumns = tableSpec.stream().filter(col -> !sortingColumns.contains(col.getName())).toList();
        checkAllContained(modelSettings.m_categoryColumns.filter(availableColumns), "grouping", tableSpec);

        // check if a name for the column that will contain the ranks in the outputs is provided.
        if (modelSettings.m_rankOutFieldName.isEmpty()) {
            throw new InvalidSettingsException("There is no name for the output rank column provided.");
        }

        return new DataTableSpec[]{createOutSpec(tableSpec, modelSettings)};
    }

    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec,
        final RankNodeSettings modelSettings) throws Exception {
        BufferedDataTable table = inData[0];
        if (table == null) {
            throw new IllegalArgumentException("No input table found.");
        }
        if (table.size() < 1) {
            setWarningMessage("The input table is empty.");
        }

        // calculate number of steps
        double numSteps = 2;
        if (modelSettings.m_rowOrder == RowOrder.INPUT_ORDER) {
            numSteps += 2;
        }

        // insert extra column containing the original order of the input table
        String rowOrderColumn = null;
        if (modelSettings.m_rowOrder == RowOrder.INPUT_ORDER) {
            rowOrderColumn = new UniqueNameGenerator(table.getDataTableSpec()).newName("rowOrder");
            table = appendOrderColumn(exec.createSubExecutionContext(1 / numSteps), table, rowOrderColumn);
        }

        var sortedTable = sortTable(exec.createSubExecutionContext(1 / numSteps), table, modelSettings);

        BufferedDataTable out = appendRank(exec.createSubExecutionContext(1 / numSteps), sortedTable, modelSettings);

        if (modelSettings.m_rowOrder == RowOrder.INPUT_ORDER) {
            out = retainRowOrder(exec.createSubExecutionContext(1 / numSteps), rowOrderColumn, out);
        }

        return new BufferedDataTable[]{out};
    }

    private static void checkAllContained(final String[] columnNames, final String columnPurpose,
        final DataTableSpec tableSpec) throws InvalidSettingsException {
        for (String colName : columnNames) {
            CheckUtils.checkSetting(tableSpec.containsName(colName),
                "The selected %s column '%s' is not contained in the input table.", columnPurpose, colName);
        }
    }

    private static DataTableSpec createOutSpec(final DataTableSpec inSpec, final RankNodeSettings modelSettings) {
        return new DataTableSpec(inSpec,
            new DataTableSpec(createRankColSpec(modelSettings.m_rankDataType, modelSettings.m_rankOutFieldName)));
    }

    private static DataColumnSpec createRankColSpec(final RankDataType rankDataType, final String colName) {
        var outputType = rankDataType == RankDataType.LONG ? LongCell.TYPE : IntCell.TYPE;
        return new DataColumnSpecCreator(colName, outputType).createSpec();
    }

    private static BufferedDataTable appendOrderColumn(final ExecutionContext exec, final BufferedDataTable table,
        final String rowOrderColumn) throws CanceledExecutionException {
        var cr = new ColumnRearranger(table.getDataTableSpec());
        DataColumnSpec rowOrderSpec = new DataColumnSpecCreator(rowOrderColumn, LongCell.TYPE).createSpec();
        var cellFac = new OrderCellFactory(rowOrderSpec);
        cr.append(cellFac);
        return exec.createColumnRearrangeTable(table, cr, exec);
    }

    private static BufferedDataTable sortTable(final ExecutionContext exec, final BufferedDataTable table,
        final RankNodeSettings modelSettings) throws CanceledExecutionException {
        final boolean[] order = new boolean[modelSettings.m_sortingCriteria.length];
        for (int i = 0; i < order.length; i++) {
            order[i] = modelSettings.m_sortingCriteria[i].getSortingOrder() == SortingOrder.ASCENDING;
        }
        return new BufferedDataTableSorter(table, toRowComparator(table.getSpec(), modelSettings)).sort(exec);
    }

    private static RowComparator toRowComparator(final DataTableSpec spec, final RankNodeSettings modelSettings) {
        final var rc = RowComparator.on(spec);
        Arrays.stream(modelSettings.m_sortingCriteria).forEach(criterion -> {
            final var ascending = criterion.getSortingOrder() == SortingOrder.ASCENDING;
            final var alphaNum = criterion.getStringComparison() == StringComparison.NATURAL;
            resolveColumnName(spec, criterion.getColumn()).ifPresentOrElse(
                col -> rc.thenComparingColumn(col,
                    c -> configureColumnComparatorBuilder(spec, modelSettings, ascending, alphaNum, col, c)),
                () -> rc.thenComparingRowKey(
                    k -> k.withDescendingSortOrder(!ascending).withAlphanumericComparison(alphaNum)));
        });
        return rc.build();
    }

    private static ColumnComparatorBuilder configureColumnComparatorBuilder(final DataTableSpec spec,
        final RankNodeSettings modelSettings, final boolean ascending, final boolean alphaNum, final int col,
        final ColumnComparatorBuilder c) {
        var compBuilder = c.withDescendingSortOrder(!ascending);
        if (spec.getColumnSpec(col).getType().isCompatible(StringValue.class)) {
            compBuilder.withAlphanumericComparison(alphaNum);
        }
        return compBuilder.withMissingsLast(modelSettings.m_missingToEnd);
    }

    private static OptionalInt resolveColumnName(final DataTableSpec dts, final StringOrEnum<RowIDChoice> column) {

        if (column.getEnumChoice().isPresent()) {
            return OptionalInt.empty();
        }
        final var colName = column.getStringChoice();
        final var idx = dts.findColumnIndex(colName);
        if (idx == -1) {
            throw new IllegalArgumentException(
                "The column identifier \"" + colName + "\" does not refer to a known column.");
        }
        return OptionalInt.of(idx);
    }

    private static BufferedDataTable appendRank(final ExecutionContext exec, final BufferedDataTable sortedTable,
        final RankNodeSettings modelSettings) throws CanceledExecutionException {
        var spec = sortedTable.getDataTableSpec();
        var columnRearranger = new ColumnRearranger(spec);
        final var sortingColumns =
            Arrays.stream(modelSettings.m_sortingCriteria).map(SortingCriterionSettings::getColumn)
                .filter(column -> column.getEnumChoice().isEmpty()).map(StringOrEnum::getStringChoice).toList();
        final var availableColumns = spec.stream().filter(col -> !sortingColumns.contains(col.getName())).toList();
        int[] groupColIndices = Arrays.stream(modelSettings.m_categoryColumns.filter(availableColumns))
            .mapToInt(spec::findColumnIndex).toArray();
        int[] rankColIndices = Arrays.stream(modelSettings.m_sortingCriteria).map(SortingCriterionSettings::getColumn)
            .mapToInt(
                column -> column.getEnumChoice().isPresent() ? -1 : spec.findColumnIndex(column.getStringChoice()))
            .toArray();
        // append rank column
        columnRearranger.append(new RankCellFactory(
            createRankColSpec(modelSettings.m_rankDataType, modelSettings.m_rankOutFieldName), groupColIndices,
            rankColIndices, modelSettings.m_rankMode, modelSettings.m_rankDataType == RankDataType.LONG));
        return exec.createColumnRearrangeTable(sortedTable, columnRearranger, exec);
    }

    private static BufferedDataTable retainRowOrder(final ExecutionContext exec, final String rowOrderColumn,
        BufferedDataTable out) throws CanceledExecutionException {
        // recover row order
        out = new BufferedDataTableSorter(out, Collections.singleton(rowOrderColumn), new boolean[]{true}).sort(exec);
        // remove order column
        var cr = new ColumnRearranger(out.getDataTableSpec());
        cr.remove(rowOrderColumn);
        out = exec.createColumnRearrangeTable(out, cr, exec.createSilentSubExecutionContext(0));
        return out;
    }
}
