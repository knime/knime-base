/*
 * ------------------------------------------------------------------------
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
 *   Apr 28, 2008 (wiswedel): created
 */
package org.knime.base.node.flowvariable.tablerowtovariable3;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import org.knime.base.node.flowvariable.converter.celltovariable.CellToVariableConverterFactory;
import org.knime.core.data.BooleanValue;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.IntValue;
import org.knime.core.data.LongValue;
import org.knime.core.data.MissingValue;
import org.knime.core.data.MissingValueException;
import org.knime.core.data.StringValue;
import org.knime.core.data.collection.CollectionCellFactory;
import org.knime.core.data.container.CloseableRowIterator;
import org.knime.core.data.def.BooleanCell;
import org.knime.core.data.def.BooleanCell.BooleanCellFactory;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.LongCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelColumnFilter2;
import org.knime.core.node.defaultnodesettings.SettingsModelDouble;
import org.knime.core.node.defaultnodesettings.SettingsModelInteger;
import org.knime.core.node.defaultnodesettings.SettingsModelLong;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.flowvariable.FlowVariablePortObject;
import org.knime.core.node.port.flowvariable.FlowVariablePortObjectSpec;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.node.util.filter.NameFilterConfiguration;
import org.knime.core.node.util.filter.column.DataColumnSpecFilterConfiguration;
import org.knime.core.node.workflow.FlowVariable;
import org.knime.core.node.workflow.VariableType;

/**
 * The node model for the table row to variable node.
 *
 * @author Mark Ortmann, KNIME GmbH, Berlin, Germany
 */
public class TableToVariable3NodeModel extends NodeModel {

    /** Suffix for missing value exception. */
    private static final String MISSING_VALUE_EXCEPTION_SUFFIX = "%s -- column \"%s\" (index %d)\"";

    private final SettingsModelString m_onMV = getOnMissing();

    private final SettingsModelInteger m_int = getReplaceInteger();

    private final SettingsModelLong m_long = getReplaceLong();

    private final SettingsModelDouble m_double = getReplaceDouble();

    private final SettingsModelString m_string = getReplaceString();

    private final SettingsModelString m_boolean = getReplaceBoolean();

    private final SettingsModelColumnFilter2 m_columnSelection = getColumnFilter();

    static final SettingsModelString getOnMissing() {
        return new SettingsModelString("missing_value_policy", MissingValuePolicy.DEFAULT.name());
    }

    static final SettingsModelDouble getReplaceDouble() {
        return new SettingsModelDouble("default_value_double", 0);
    }

    static final SettingsModelString getReplaceString() {
        return new SettingsModelString("default_value_string", "missing");
    }

    static final SettingsModelString getReplaceBoolean() {
        return new SettingsModelString("default_value_boolean", "false");
    }

    static final SettingsModelInteger getReplaceInteger() {
        return new SettingsModelInteger("default_value_integer", 0);
    }

    static final SettingsModelLong getReplaceLong() {
        return new SettingsModelLong("default_value_long", 0L);
    }

    static final SettingsModelColumnFilter2 getColumnFilter() {
        return new SettingsModelColumnFilter2("column_selection", null,
            NameFilterConfiguration.FILTER_BY_NAMEPATTERN | DataColumnSpecFilterConfiguration.FILTER_BY_DATATYPE);
    }

    /**
     * Constructor. Creating a node with a {@link BufferedDataTable} input port and a {@link FlowVariablePortObject}
     * output port.
     */
    protected TableToVariable3NodeModel() {
        super(new PortType[]{BufferedDataTable.TYPE}, new PortType[]{FlowVariablePortObject.TYPE});
    }

    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        final DataTableSpec spec = (DataTableSpec)inSpecs[0];
        final String[] selectedColumns = m_columnSelection.applyTo(spec).getIncludes();
        if (selectedColumns.length == 0) {
            setWarningMessage("No column selected only the RowID will be converted");
        }
        if (getMissingValuePolicy() != MissingValuePolicy.OMIT) {
            // Pushes the default variables onto the stack
            pushVariables(spec, null, createDefaultCells(spec), selectedColumns);
        }
        return new PortObjectSpec[]{FlowVariablePortObjectSpec.INSTANCE};
    }

    private MissingValuePolicy getMissingValuePolicy() {
        return MissingValuePolicy.valueOf(m_onMV.getStringValue());
    }

    private DataCell[] createDefaultCells(final DataTableSpec spec) {
        final DataCell[] cells = new DataCell[spec.getNumColumns()];
        for (int i = 0; i < cells.length; i++) {
            final DataType type = spec.getColumnSpec(i).getType();
            final DataCell cell;
            if (type.isCollectionType()) {
                cell = getDefaultCollectionCell(type);
            } else if (type.isCompatible(BooleanValue.class)) {
                cell = BooleanCellFactory.create(Boolean.parseBoolean(m_boolean.getStringValue()));
            } else if (type.isCompatible(IntValue.class)) {
                cell = new IntCell(m_int.getIntValue());
            } else if (type.isCompatible(LongValue.class)) {
                cell = new LongCell(m_long.getLongValue());
            } else if (type.isCompatible(DoubleValue.class)) {
                cell = new DoubleCell(m_double.getDoubleValue());
            } else if (type.isCompatible(StringValue.class)) {
                cell = new StringCell(m_string.getStringValue());
            } else {
                cell = DataType.getMissingCell();
            }
            cells[i] = cell;
        }
        return cells;
    }

    private static DataCell getDefaultCollectionCell(final DataType type) {
        final DataCell cell;
        final DataType elementType = type.getCollectionElementType();
        if (elementType.isCompatible(BooleanValue.class)) {
            cell = CollectionCellFactory.createListCell(new ArrayList<BooleanCell>());
        } else if (elementType.isCompatible(IntValue.class)) {
            cell = CollectionCellFactory.createListCell(new ArrayList<IntCell>());
        } else if (elementType.isCompatible(LongValue.class)) {
            cell = CollectionCellFactory.createListCell(new ArrayList<LongCell>());
        } else if (elementType.isCompatible(DoubleValue.class)) {
            cell = CollectionCellFactory.createListCell(new ArrayList<DoubleCell>());
        } else if (elementType.isCompatible(StringValue.class)) {
            cell = CollectionCellFactory.createListCell(new ArrayList<StringCell>());
        } else {
            cell = CollectionCellFactory.createListCell(new ArrayList<>());
        }
        return cell;
    }

    /**
     * Pushes the variable as given by the row argument onto the stack.
     *
     * @param variablesSpec The spec (for names and types)
     * @param rowKey the name of the row
     * @param row the content of the row
     * @param selectedColumns the names of the column to be converted to flow variables
     */
    private void pushVariables(final DataTableSpec variablesSpec, final String rowKey, final DataCell[] row,
        final String[] selectedColumns) {

        // push the rowID onto the stack
        final String rowIDVarName = "RowID";
        pushFlowVariableString(rowIDVarName, rowKey == null ? "" : rowKey);
        // column names starting with "knime." are uniquified as they represent global constants
        final Set<String> variableNames = new HashSet<>();
        variableNames.add(rowIDVarName);

        // push the selected cells
        for (int i = selectedColumns.length - 1; i >= 0; i--) {
            final String selectedColumnName = selectedColumns[i];
            final int colIdx = variablesSpec.findColumnIndex(selectedColumnName);
            if (!row[colIdx].isMissing()) {
                final DataColumnSpec spec = variablesSpec.getColumnSpec(selectedColumnName);
                final DataType type = spec.getType();
                final String name = getUniqueName(variableNames, colIdx, spec);
                pushVariable(
                    CellToVariableConverterFactory.createConverter(type).createFlowVariable(name, row[colIdx]));
            }
        }
    }

    private static String getUniqueName(final Set<String> variableNames, final int colIdx, final DataColumnSpec spec) {
        final String basename = getBaseName(colIdx, spec);
        int uniquifier = 1;
        String name = basename;
        while (!variableNames.add(basename)) {
            name = basename + "(#" + (uniquifier) + ")";
            uniquifier++;
        }
        return name;
    }

    private static String getBaseName(final int colIdx, final DataColumnSpec spec) {
        final String name = spec.getName();
        if (name.equals("knime.")) {
            return "column_" + colIdx;
        } else if (name.startsWith("knime.")) {
            return name.substring("knime.".length());
        } else {
            return name;
        }
    }

    /**
     * Pushes the {@link FlowVariable} onto the flow variable stack.
     *
     * @param <T> the simple value type of the variable
     * @param fv the {@link FlowVariable} to be pushed onto the stack
     */
    @SuppressWarnings("unchecked")
    protected final <T> void pushVariable(final FlowVariable fv) {
        pushFlowVariable(fv.getName(), (VariableType<T>)fv.getVariableType(), (T)fv.getValue(fv.getVariableType()));
    }

    @Override
    protected PortObject[] execute(final PortObject[] inData, final ExecutionContext exec) throws Exception {
        final BufferedDataTable variables = (BufferedDataTable)inData[0];
        final DataTableSpec spec = variables.getDataTableSpec();
        pushVariables(spec, getFirstRow(variables));
        return new PortObject[]{FlowVariablePortObject.INSTANCE};
    }

    private DataRow getFirstRow(final BufferedDataTable variables) {
        try (final CloseableRowIterator iter = variables.iterator()) {
            if (iter.hasNext()) {
                return iter.next();
            } else {
                CheckUtils.checkArgument(getMissingValuePolicy() != MissingValuePolicy.FAIL,
                    "The input table is empty");
                return null;
            }
        }

    }

    /**
     * Pushes the data row, which can be null, onto the flow variable stack.
     *
     * @param spec the data table spec
     * @param row the row to push onto the flow variable stack
     */
    protected final void pushVariables(final DataTableSpec spec, final DataRow row) {
        pushVariables(spec, row, createDefaultCells(spec));
    }

    private void pushVariables(final DataTableSpec spec, final DataRow row, final DataCell[] defaultCells) {
        final String[] selectedColumns = m_columnSelection.applyTo(spec).getIncludes();

        // now replace missing cells from the input by their defaults!
        final DataCell[] rowToConvert = prepareRow(spec, row, defaultCells, selectedColumns);

        pushVariables(spec, extractRowKey(row), rowToConvert, selectedColumns);
    }

    private DataCell[] prepareRow(final DataTableSpec spec, final DataRow row, final DataCell[] defaultCells,
        final String[] selectedColumns) {
        final DataCell[] updatedRow;
        final MissingValuePolicy policy = getMissingValuePolicy();

        if (row == null) {
            updatedRow = defaultCells;
        } else if (policy == MissingValuePolicy.DEFAULT) {
            updatedRow = new DataCell[row.getNumCells()];
            for (int i = 0; i < updatedRow.length; i++) {
                DataCell cell = row.getCell(i);
                if (cell.isMissing()) {
                    cell = defaultCells[i];
                }
                updatedRow[i] = cell;
            }
        } else {
            updatedRow = row.stream()//
                .toArray(DataCell[]::new);
        }
        checkPolicyCompliance(policy, spec, selectedColumns, updatedRow);
        return updatedRow;
    }

    private static void checkPolicyCompliance(final MissingValuePolicy policy, final DataTableSpec spec,
        final String[] selectedColumns, final DataCell[] updatedRow) {
        for (final String colName : selectedColumns) {
            final int colIdx = spec.findColumnIndex(colName);
            if (updatedRow[colIdx].isMissing()) {
                if (policy == MissingValuePolicy.FAIL) {
                    throw new MissingValueException((MissingValue)updatedRow[colIdx],
                        String.format(MISSING_VALUE_EXCEPTION_SUFFIX,
                            "Missing values are not allowed as variable values", colName, colIdx));
                } else if (policy == MissingValuePolicy.DEFAULT) { // NOSONAR
                    throw new MissingValueException((MissingValue)updatedRow[colIdx],
                        String.format(MISSING_VALUE_EXCEPTION_SUFFIX,
                            "Missing values are not allowed for data types without a default value", colName, colIdx));
                }
            }
        }
    }

    private static String extractRowKey(final DataRow row) {
        final String rowKey;
        if (row == null) {
            rowKey = null;
        } else {
            rowKey = row.getKey().toString();
        }
        return rowKey;
    }

    @Override
    protected void reset() {
        // nothing to do
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_onMV.saveSettingsTo(settings);
        m_string.saveSettingsTo(settings);
        m_boolean.saveSettingsTo(settings);
        m_int.saveSettingsTo(settings);
        m_long.saveSettingsTo(settings);
        m_double.saveSettingsTo(settings);
        m_columnSelection.saveSettingsTo(settings);
    }

    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_onMV.loadSettingsFrom(settings);
        m_string.loadSettingsFrom(settings);
        m_boolean.loadSettingsFrom(settings);
        m_int.loadSettingsFrom(settings);
        m_long.loadSettingsFrom(settings);
        m_double.loadSettingsFrom(settings);
        m_columnSelection.loadSettingsFrom(settings);
    }

    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_onMV.validateSettings(settings);
        m_string.validateSettings(settings);
        m_boolean.validateSettings(settings);
        m_int.validateSettings(settings);
        m_long.validateSettings(settings);
        m_double.validateSettings(settings);
        m_columnSelection.validateSettings(settings);
    }

    @Override
    protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        // nothing to do
    }

    @Override
    protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        // nothing to do
    }

}
