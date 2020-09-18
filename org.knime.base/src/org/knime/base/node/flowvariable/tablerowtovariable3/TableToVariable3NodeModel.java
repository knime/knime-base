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
import org.knime.core.node.defaultnodesettings.SettingsModelDouble;
import org.knime.core.node.defaultnodesettings.SettingsModelInteger;
import org.knime.core.node.defaultnodesettings.SettingsModelLong;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.flowvariable.FlowVariablePortObject;
import org.knime.core.node.port.flowvariable.FlowVariablePortObjectSpec;
import org.knime.core.node.workflow.FlowVariable;
import org.knime.core.node.workflow.VariableType;

/**
 * The node model for the table row to variable node.
 *
 * @author Bernd Wiswedel, University of Konstanz
 * @author Patrick Winter, KNIME AG, Zurich, Switzerland
 * @author Marc Bux, KNIME GmbH, Berlin, Germany
 * @author Mark Ortmann, KNIME GmbH, Berlin, Germany
 */
class TableToVariable3NodeModel extends NodeModel {

    private final SettingsModelString m_onMV;

    private final SettingsModelInteger m_int;

    private final SettingsModelLong m_long;

    private final SettingsModelDouble m_double;

    private final SettingsModelString m_string;

    private final SettingsModelString m_boolean;

    TableToVariable3NodeModel() {
        super(new PortType[]{BufferedDataTable.TYPE}, new PortType[]{FlowVariablePortObject.TYPE});
        m_onMV = TableToVariable3NodeDialog.getOnMissing();
        m_int = TableToVariable3NodeDialog.getReplaceInteger(m_onMV);
        m_long = TableToVariable3NodeDialog.getReplaceLong(m_onMV);
        m_double = TableToVariable3NodeDialog.getReplaceDouble(m_onMV);
        m_string = TableToVariable3NodeDialog.getReplaceString(m_onMV);
        m_boolean = TableToVariable3NodeDialog.getReplaceBoolean(m_onMV);
    }

    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        if (!m_onMV.getStringValue().equals(MissingValuePolicy.OMIT.getName())) {
            // Pushes the default variables onto the stack
            final DataTableSpec spec = (DataTableSpec)inSpecs[0];
            try {
                pushVariables(spec, null, createDefaultCells(spec));
            } catch (Exception e) {
                // ignored
            }
        }
        return new PortObjectSpec[]{FlowVariablePortObjectSpec.INSTANCE};
    }

    private DataCell[] createDefaultCells(final DataTableSpec spec) {
        final DataCell[] cells = new DataCell[spec.getNumColumns()];
        for (int i = cells.length; --i >= 0;) { //NOSONAR
            final DataType type = spec.getColumnSpec(i).getType();

            final DataCell cell;
            if (type.isCollectionType()) {
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
                // setting the default to null will omit this column.
                cell = null;
            }
            cells[i] = cell;

        }
        return cells;
    }

    /**
     * Pushes the variable as given by the row argument onto the stack.
     *
     * @param variablesSpec The spec (for names and types)
     * @param rowKey the name of the row
     * @param row the content of the row
     * @throws Exception if the node is supposed to fail on missing values or empty table
     */
    private void pushVariables(final DataTableSpec variablesSpec, final String rowKey, final DataCell[] row)
        throws Exception {
        // push also the rowID onto the stack
        final String rowIDVarName = "RowID";
        final boolean fail = m_onMV.getStringValue().equals(MissingValuePolicy.FAIL.getName());
        final boolean defaults = m_onMV.getStringValue().equals(MissingValuePolicy.DEFAULT.getName());
        pushFlowVariableString(rowIDVarName, rowKey == null ? "" : rowKey);
        final DataCell[] defaultCells = createDefaultCells(variablesSpec);
        // column names starting with "knime." are uniquified as they represent global constants
        final Set<String> variableNames = new HashSet<>();
        variableNames.add(rowIDVarName);
        for (int i = variablesSpec.getNumColumns(); --i >= 0;) {
            final DataColumnSpec spec = variablesSpec.getColumnSpec(i);
            final DataType type = spec.getType();

            String name = spec.getName();
            if (name.equals("knime.")) {
                name = "column_" + i;
            } else if (name.startsWith("knime.")) {
                name = name.substring("knime.".length());
            }
            int uniquifier = 1;
            final String basename = name;
            while (!variableNames.add(name)) {
                name = basename + "(#" + (uniquifier++) + ")";
            }

            final DataCell cell;
            if (row == null) {
                if (fail) {
                    throw new Exception("No rows in input table");
                } else if (defaults) {
                    cell = defaultCells[i];
                } else {
                    // omit
                    cell = null;
                }
            } else if (row[i].isMissing()) {
                if (fail) {
                    throw new MissingValueException((MissingValue)row[i],
                        String.format(
                            "Missing Values not allowed as variable values -- "
                                + "in row with ID \"%s\", column \"%s\" (index %d)",
                            rowKey, variablesSpec.getColumnSpec(i).getName(), i));
                } else if (defaults) {
                    cell = defaultCells[i];
                } else {
                    // omit
                    cell = null;
                }
            } else {
                // take the value from the input table row
                cell = row[i];
            }

            if (cell != null) {
                pushVariable(CellToVariableConverterFactory.createConverter(type).createFlowVariable(name, cell));
            }
        }
    }

    @SuppressWarnings("unchecked")
    private <T> void pushVariable(final FlowVariable fv) {
        pushFlowVariable(fv.getName(), (VariableType<T>)fv.getVariableType(), (T)fv.getValue(fv.getVariableType()));
    }

    @Override
    protected PortObject[] execute(final PortObject[] inData, final ExecutionContext exec) throws Exception {
        final BufferedDataTable variables = (BufferedDataTable)inData[0];
        pushVariables(variables.getDataTableSpec(), getFirstRow(variables));
        return new PortObject[]{FlowVariablePortObject.INSTANCE};
    }

    private static DataRow getFirstRow(final BufferedDataTable variables) {
        try (final CloseableRowIterator iter = variables.iterator()) {
            if (iter.hasNext()) {
                return iter.next();
            } else {
                return null;
            }
        }
    }

    /**
     * Converts the {@link DataRow}'s {@link DataCell}'s to {@link FlowVariable}s and pushes them onto the variable
     * stack.
     *
     * @param spec the input {@link DataTableSpec}
     * @param row the row whose cells have to be converted to {@link FlowVariable}s
     * @throws Exception
     */
    private void pushVariables(final DataTableSpec spec, final DataRow row) throws Exception {
        if (row == null) {
            pushVariables(spec, null, null);
        } else {
            pushVariables(spec, row.getKey().toString(), row.stream().toArray(DataCell[]::new));
        }
    }

    @Override
    protected void reset() {
        // nothing to do
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_onMV.saveSettingsTo(settings);
        m_int.saveSettingsTo(settings);
        m_long.saveSettingsTo(settings);
        m_double.saveSettingsTo(settings);
        m_string.saveSettingsTo(settings);
        m_boolean.saveSettingsTo(settings);
    }

    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        // new since 2.9
        if (settings.containsKey(m_string.getKey())) {
            m_onMV.loadSettingsFrom(settings);
            m_int.loadSettingsFrom(settings);
            m_long.loadSettingsFrom(settings);
            m_double.loadSettingsFrom(settings);
            m_string.loadSettingsFrom(settings);
            m_boolean.loadSettingsFrom(settings);
        }
    }

    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        // new since 2.9
        if (settings.containsKey(m_string.getKey())) {
            m_onMV.validateSettings(settings);
            m_int.validateSettings(settings);
            m_long.validateSettings(settings);
            m_double.validateSettings(settings);
            m_string.validateSettings(settings);
            m_boolean.validateSettings(settings);
        }
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
