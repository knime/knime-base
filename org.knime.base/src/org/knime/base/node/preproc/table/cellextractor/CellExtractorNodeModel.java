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
 *   Nov 8, 2022 (ivan.prigarin): created
 */
package org.knime.base.node.preproc.table.cellextractor;

import java.util.NoSuchElementException;

import org.knime.base.node.flowvariable.converter.celltovariable.CellToVariableConverterFactory;
import org.knime.base.node.flowvariable.converter.celltovariable.MissingValueHandler;
import org.knime.base.node.preproc.table.cellextractor.CellExtractorSettings.ColumnSpecificationMode;
import org.knime.base.node.preproc.table.utils.Position;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.MissingValue;
import org.knime.core.data.container.CloseableRowIterator;
import org.knime.core.data.container.filter.TableFilter;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.flowvariable.FlowVariablePortObject;
import org.knime.core.node.port.flowvariable.FlowVariablePortObjectSpec;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.node.workflow.FlowVariable;
import org.knime.core.node.workflow.VariableType;
import org.knime.core.webui.node.impl.WebUINodeConfiguration;
import org.knime.core.webui.node.impl.WebUINodeModel;

/**
 * NodeModel for the Cell Extractor node.
 *
 * @author Ivan Prigarin, KNIME GmbH, Konstanz, Germany
 */
@SuppressWarnings("restriction")
final class CellExtractorNodeModel extends WebUINodeModel<CellExtractorSettings> {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(CellExtractorNodeModel.class);

    /**
     * The node consumes a single input table, and produces a 1x1 output table containing the extracted cell, as well as
     * a flow variable containing the value of the cell.
     */
    CellExtractorNodeModel(final WebUINodeConfiguration configuration) {
        super(configuration, CellExtractorSettings.class);
    }

    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs, final CellExtractorSettings settings)
        throws InvalidSettingsException {
        final var spec = (DataTableSpec)inSpecs[0];
        validateSettingsAgainstSpecs(settings, spec);
        final var outSpec = createOutputTableSpec(settings, spec);

        return new PortObjectSpec[]{FlowVariablePortObjectSpec.INSTANCE, outSpec};
    }

    /*
     * Retrieve the cell value at the column and row index.
     *
     * The output is a 1x1 table, "Extracted Cell", containing the extracted cell. The column name and
     * the row index of the extracted cell are preserved from the input table.
     *
     * The {@link CellToVariableConverterFactory} takes care of converting the value to a flow variable, which
     * is then also output.
     */
    @Override
    protected PortObject[] execute(final PortObject[] inData, final ExecutionContext exec,
        final CellExtractorSettings settings) throws Exception {
        final var table = (BufferedDataTable)inData[0];

        CheckUtils.checkSetting(settings.m_rowNumber <= table.size(),
            "The row number %s does not exist in the input table with only %s rows.", settings.m_rowNumber,
            table.size());

        final var spec = table.getDataTableSpec();
        final var outputDataSpec = createOutputTableSpec(settings, spec);

        final int colIndex = getColumnIndex(settings, spec);
        final var colType = getColumnType(settings, spec);

        Position rowIdxPosition = new Position(settings.m_rowNumber, settings.m_countFromEnd, "row");
        var rowIndex = rowIdxPosition.resolvePosition(table.size())
            .orElseThrow(() -> new InvalidSettingsException(
                String.format("The selected row number (%s) exceeds the number of rows in the table (%s).",
                    rowIdxPosition.getPosition(), table.size())));
        final DataRow inRow = getRowFromTable(table, rowIndex - 1, colIndex, exec);
        final DataCell value = inRow.getCell(colIndex);
        final var cells = new DataCell[]{value};
        final var outRow = new DefaultRow(inRow.getKey(), cells);
        var cont = exec.createDataContainer(outputDataSpec);
        cont.addRowToTable(outRow);
        cont.close();
        convertValueToFlowVariable(value, "extracted_cell", colType);
        return new PortObject[]{FlowVariablePortObject.INSTANCE, cont.getTable()};
    }

    /**
     * Creates a spec for the output table based on the column specification mode of the settings.
     */
    private static DataTableSpec createOutputTableSpec(final CellExtractorSettings settings,
        final DataTableSpec inSpec) {
        var col = inSpec.getColumnSpec(getColumnIndex(settings, inSpec));
        final var colSpecCreator = new DataColumnSpecCreator(col.getName(), col.getType());
        final DataColumnSpec[] colOutSpec = {colSpecCreator.createSpec()};
        return new DataTableSpec("Extracted Cell", colOutSpec);
    }

    private static int getColumnIndex(final CellExtractorSettings settings, final DataTableSpec spec) {
        if (settings.m_columnSpecificationMode == ColumnSpecificationMode.BY_NUMBER) {
            return settings.m_columnNumber - 1;
        } else {
            return spec.findColumnIndex(settings.m_columnName);
        }
    }

    private static DataType getColumnType(final CellExtractorSettings settings, final DataTableSpec spec) {
        return spec.getColumnSpec(getColumnIndex(settings, spec)).getType();
    }

    /**
     *
     * @param table the input table to get the specified row from
     * @param rowNumber the provided row number
     * @param colIndex the index of the selected column
     * @param exec the execution context that acts as an execution monitor
     * @return the row
     * @throws InvalidSettingsException thrown if the row index is out of bounds
     */
    private static DataRow getRowFromTable(final BufferedDataTable table, final long rowIndex, final int colIndex,
        final ExecutionContext exec) {
        var filter = new TableFilter.Builder()//
            .withFromRowIndex(rowIndex)//
            .withToRowIndex(rowIndex)//
            .withMaterializeColumnIndices(colIndex)//
            .build();

        try (final CloseableRowIterator iter = table.filter(filter, exec).iterator()) {
            return iter.next();
        }
    }

    /**
     * @param value the cell value to be converted into a flow variable
     * @param name name of the flow variable (currently static "extracted_cell")
     * @param type type of the extracted cell
     */
    private void convertValueToFlowVariable(final DataCell value, final String name, final DataType type)
        throws IllegalArgumentException {
        final var handler = new VariableMissingValueHandler();
        CellToVariableConverterFactory//
            .createConverter(type)//
            .createFlowVariable(name, value, handler)
            .ifPresentOrElse(this::pushVariable, () -> setWarningMessage("Couldn't convert cell to variable."));
    }

    @SuppressWarnings("unchecked")
    private <T> void pushVariable(final FlowVariable fv) {
        try {
            peekFlowVariable(fv.getName(), (VariableType<T>)fv.getVariableType());
            LOGGER.warn(
                "Overwriting existing flow variable '" + fv.getName() + "' of type " + fv.getVariableType() + ".");
        } catch (NoSuchElementException e) {//NOSONAR
            // No existing variable on the stack - do nothing
        }
        pushFlowVariable(fv.getName(), (VariableType<T>)fv.getVariableType(), (T)fv.getValue(fv.getVariableType()));
    }

    private static final class VariableMissingValueHandler implements MissingValueHandler {

        @Override
        public Object handle(final MissingValue missingValue, final VariableType<?> requiredType) {
            return null;
        }

    }

    private static void validateSettingsAgainstSpecs(final CellExtractorSettings settings, final DataTableSpec spec)
        throws InvalidSettingsException {
        CheckUtils.checkSetting(spec.getNumColumns() > 0, "The input table should contain at least one column.");

        if (settings.m_columnSpecificationMode == ColumnSpecificationMode.BY_NAME) {
            // This is only reachable if the node was previously autoconfigured with an empty table attached.
            CheckUtils.checkSetting(settings.m_columnName != null,
                "Select the column containing the cell to be extracted.");
            CheckUtils.checkSetting(spec.containsName(settings.m_columnName),
                "The input table does not contain the column '%s'.", settings.m_columnName);
        } else {
            CheckUtils.checkSetting(settings.m_columnNumber > 0, "The column number needs to be a positive number.");
            CheckUtils.checkSetting(settings.m_columnNumber <= spec.getNumColumns(),
                "The column number %s does not exist in the input table with %s columns.", settings.m_columnNumber,
                spec.getNumColumns());
        }

        CheckUtils.checkSetting(settings.m_rowNumber > 0, "The row number needs to be a positive number.");
    }

}
