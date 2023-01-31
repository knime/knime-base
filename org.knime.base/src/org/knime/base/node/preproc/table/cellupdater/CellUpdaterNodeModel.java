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
 *   Jan 31, 2023 (ivan.prigarin): created
 */
package org.knime.base.node.preproc.table.cellupdater;

import java.util.ArrayList;
import java.util.Optional;

import org.knime.base.data.replace.ReplacedColumnsDataRow;
import org.knime.base.node.flowvariable.converter.variabletocell.VariableToCellConverterFactory;
import org.knime.base.node.preproc.table.cellupdater.CellUpdaterSettings.ColumnMode;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.container.filter.TableFilter;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InternalTableAPI;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.node.workflow.FlowVariable;
import org.knime.core.table.row.Selection;
import org.knime.core.webui.node.dialog.impl.WebUINodeConfiguration;
import org.knime.core.webui.node.dialog.impl.WebUINodeModel;

/**
 * NodeModel for the Cell Updater node.
 *
 * @author Ivan Prigarin, KNIME GmbH, Konstanz, Germany
 */
@SuppressWarnings("restriction")
final class CellUpdaterNodeModel extends WebUINodeModel<CellUpdaterSettings> {

    CellUpdaterNodeModel(final WebUINodeConfiguration configuration) {
        super(configuration, CellUpdaterSettings.class);
    }

    @Override
    protected DataTableSpec[] configure(final PortObjectSpec[] inSpecs, final CellUpdaterSettings settings)
        throws InvalidSettingsException {
        final var spec = (DataTableSpec)inSpecs[1];
        validateSettingsAgainstSpec(settings, spec);
        return new DataTableSpec[]{spec};
    }

    @Override
    protected PortObject[] execute(final PortObject[] inData, final ExecutionContext exec,
        final CellUpdaterSettings settings) throws Exception {
        final var table = (BufferedDataTable)inData[1];
        final var spec = table.getDataTableSpec();

        final int colIndex = getColumnIndex(settings, spec);
        final DataType colType = getColumnType(colIndex, spec);

        final DataCell newCell = getNewCellValue(settings);

        CheckUtils.checkArgument(colType == newCell.getType(),
            getTypeMismatchWarningMessage(colType, newCell.getType()));

        final var rowIndex = getRowIndex(settings, table.size());
        BufferedDataTable outputTable = updateInputTable(exec, rowIndex, colIndex, table, newCell);
        return new PortObject[]{outputTable};
    }

    private static long getRowIndex(final CellUpdaterSettings settings, final long rowCount)
        throws InvalidSettingsException {
        final var rowIndex = settings.m_countFromEnd ? (rowCount - settings.m_rowNumber) : (settings.m_rowNumber - 1);
        CheckUtils.checkSetting(rowIndex >= 0 && rowIndex < rowCount,
            "The specified row number (%s) exceeds the number of rows in the table (%s).", settings.m_rowNumber,
            rowCount);
        return rowIndex;
    }

    private DataCell getNewCellValue(final CellUpdaterSettings settings) {
        final var varTypes = VariableToCellConverterFactory.getSupportedTypes();
        final var availableVars = getAvailableInputFlowVariables(varTypes);
        final FlowVariable selectedVar = availableVars.get(settings.m_flowVariableName);

        final var varConverter = VariableToCellConverterFactory.createConverter(selectedVar);
        final var newCell = varConverter.getDataCell(selectedVar);

        return newCell;
    }

    private static BufferedDataTable updateInputTable(final ExecutionContext exec, final long targetRowIdx,
        final int targetColIdx, final BufferedDataTable table, final DataCell newCell) throws Exception {
        var topSelection = Selection.all().retainRows(0, targetRowIdx);
        var bottomSelection = Selection.all().retainRows(targetRowIdx + 1, table.size());
        var slices = new ArrayList<>();

        performExecutionStep(exec, "Extract rows before the row containing the target cell.", 0.25,
            e -> targetRowIdx == 0 ? Optional.empty() : Optional.of(InternalTableAPI.slice(e, table, topSelection)))
                .ifPresent(slices::add);

        slices.add(performExecutionStep(exec, "Update target cell.", 0.25,
            e -> getTargetSlice(e, table, targetRowIdx, targetColIdx, newCell)));

        performExecutionStep(exec, "Extract rows after the row containing the target cell.", 0.25,
            e -> targetRowIdx == table.size() - 1 ? Optional.empty()
                : Optional.of(InternalTableAPI.slice(e, table, bottomSelection))).ifPresent(slices::add);

        return performExecutionStep(exec, "Put together the output table.", 0.25,
            e -> e.createConcatenateTable(e, slices.toArray(BufferedDataTable[]::new)));
    }

    private interface FailableFunction<T, R> {

        R apply(T t) throws Exception; //NOSONAR
    }

    private static <T> T performExecutionStep(final ExecutionContext mainExec, final String stepDescription,
        final double progressFraction, final FailableFunction<ExecutionContext, T> stepExecutor) throws Exception {
        mainExec.setMessage(stepDescription);
        var stepExec = mainExec.createSubExecutionContext(progressFraction);
        var result = stepExecutor.apply(stepExec);
        stepExec.setProgress(1);
        return result;
    }

    private static BufferedDataTable getTargetSlice(final ExecutionContext exec, final BufferedDataTable table,
        final long targetRowIdx, final int targetColIdx, final DataCell newCell) {
        TableFilter filter = new TableFilter.Builder()//
            .withFromRowIndex(targetRowIdx)//
            .withToRowIndex(targetRowIdx)//
            .build();

        BufferedDataContainer targetCont = exec.createDataContainer(table.getDataTableSpec());
        try (var iter = table.filter(filter).iterator()) {
            targetCont.addRowToTable(getMutatedTargetRow(iter.next(), targetColIdx, newCell));
        }
        targetCont.close();

        return targetCont.getTable();
    }

    private static DataRow getMutatedTargetRow(final DataRow row, final int targetColIdx, final DataCell newCell) {
        return new ReplacedColumnsDataRow(row, new DataCell[]{newCell}, new int[]{targetColIdx});
    }

    private static int getColumnIndex(final CellUpdaterSettings settings, final DataTableSpec spec) {
        if (settings.m_columnMode == ColumnMode.BY_NUMBER) {
            return settings.m_columnNumber - 1;
        } else {
            return spec.findColumnIndex(settings.m_columnName);
        }
    }

    private static DataType getColumnType(final int colIndex, final DataTableSpec spec) {
        return spec.getColumnSpec(colIndex).getType();
    }

    private static String getTypeMismatchWarningMessage(final DataType colType, final DataType newType) {
        return String.format("Unable to update cell in a column of type \"%s\" with a value of type \"%s\".",
            colType.getName(), newType.getName());
    }

    private static void validateSettingsAgainstSpec(final CellUpdaterSettings settings, final DataTableSpec spec)
        throws InvalidSettingsException {
        if (settings.m_columnMode == ColumnMode.BY_NAME) {
            CheckUtils.checkSetting(spec.containsName(settings.m_columnName),
                "The input table does not contain the column \"%s\"", settings.m_columnName);
        } else {
            CheckUtils.checkSetting(settings.m_columnNumber > 0, "The column number needs to be a positive number.");
            CheckUtils.checkSetting(settings.m_columnNumber <= spec.getNumColumns(),
                "The column number %s does not exist in the input table with %s columns.", settings.m_columnNumber,
                spec.getNumColumns());
        }

        CheckUtils.checkSetting(settings.m_rowNumber > 0, "The row number needs to be a positive number.");
    }

}
