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
 *   Apr 16, 2020 (Mark Ortmann, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.base.node.flowvariable.tablecoltovariable4;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.stream.Collectors;

import org.knime.base.node.flowvariable.converter.celltovariable.CellToVariableConverter;
import org.knime.base.node.flowvariable.converter.celltovariable.CellToVariableConverterFactory;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.MissingValue;
import org.knime.core.data.MissingValueException;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.flowvariable.FlowVariablePortObject;
import org.knime.core.node.port.flowvariable.FlowVariablePortObjectSpec;
import org.knime.core.node.workflow.FlowVariable;
import org.knime.core.node.workflow.VariableType;

/**
 * This node model allows to convert the values from a table column to flow variables. The flow variable names are
 * derived from the row IDs,
 *
 * @author Mark Ortmann, KNIME GmbH, Berlin, Germany
 */
final class TableColumnToVariable4NodeModel extends NodeModel {

    private static final String CFGKEY_IGNORE_MISSING = "ignore missing";

    private static final boolean DEFAULT_IGNORE_MISSING = true;

    private static final String CFGKEY_COLUMN = "column";

    static final SettingsModelBoolean createIgnoreMissing() {
        return new SettingsModelBoolean(CFGKEY_IGNORE_MISSING, DEFAULT_IGNORE_MISSING);
    }

    static final SettingsModelString createColumnSettings() {
        return new SettingsModelString(CFGKEY_COLUMN, null);
    }

    private final SettingsModelBoolean m_ignoreMissing = createIgnoreMissing();

    private final SettingsModelString m_column = createColumnSettings();

    /**
     * Constructor for the node model.
     */
    TableColumnToVariable4NodeModel() {
        super(new PortType[]{BufferedDataTable.TYPE}, new PortType[]{FlowVariablePortObject.TYPE});
    }

    @Override
    protected PortObject[] execute(final PortObject[] inData, final ExecutionContext exec) throws Exception {
        if (inData[0] instanceof BufferedDataTable) {
            final BufferedDataTable table = (BufferedDataTable)inData[0];
            final DataTableSpec spec = table.getSpec();
            final int colIndex = spec.findColumnIndex(m_column.getStringValue());
            assert colIndex >= 0 : colIndex;
            final CellToVariableConverter<?> cell2VarConverter =
                CellToVariableConverterFactory.createConverter(spec.getColumnSpec(colIndex).getType());
            if (table.size() > 0) {
                convertColToVar(table, colIndex, cell2VarConverter);
            } else {
                setWarningMessage("Node created no variables since the input data table is empty.");
            }
        }
        return new FlowVariablePortObject[]{FlowVariablePortObject.INSTANCE};
    }

    private void convertColToVar(final BufferedDataTable table, final int colIndex,
        final CellToVariableConverter<?> cell2VarConverter) {
        for (final DataRow row : table) {
            final DataCell cell = row.getCell(colIndex);
            final String name = row.getKey().getString();
            if (cell.isMissing()) {
                if (m_ignoreMissing.getBooleanValue()) {
                    continue;
                }
                throw new MissingValueException((MissingValue)cell, String
                    .format("Missing value in column '%s' for row '%s'", m_column.getStringValue(), row.getKey()));
            }

            pushVariable(cell2VarConverter.createFlowVariable(name, cell));
        }
    }

    @SuppressWarnings("unchecked")
    private <T> void pushVariable(final FlowVariable fv) {
        pushFlowVariable(fv.getName(), (VariableType<T>)fv.getVariableType(), (T)fv.getValue(fv.getVariableType()));
    }

    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        final DataTableSpec spec = (DataTableSpec)inSpecs[0];
        if (m_column.getStringValue() == null) {
            autoGuess(spec);
        }
        // validation
        final int colIndex = spec.findColumnIndex(m_column.getStringValue());
        if (colIndex < 0) {
            throw new InvalidSettingsException(
                String.format("The selected column '%s' is not part of the input", m_column.getStringValue()));
        }
        if (applicableColumns(spec).stream()//
            .map(DataColumnSpec::getName)//
            .noneMatch(cName -> cName.equals(m_column.getStringValue()))) {
            throw new InvalidSettingsException(String
                .format("The selected column '%s' cannot be converted to a flow variable", m_column.getStringValue()));
        }
        return new PortObjectSpec[]{FlowVariablePortObjectSpec.INSTANCE};
    }

    private void autoGuess(final DataTableSpec spec) throws InvalidSettingsException {
        final Collection<DataColumnSpec> applicableColumns = applicableColumns(spec);
        if (applicableColumns.isEmpty()) {
            throw new InvalidSettingsException("Input contains no column that can be converted to a flow variable");
        }
        final DataColumnSpec column = applicableColumns.iterator().next();
        m_column.setStringValue(column.getName());
        setWarningMessage(String.format("Auto-guessing: Selected column '%s'", column.getName()));
    }

    private static Collection<DataColumnSpec> applicableColumns(final DataTableSpec spec) {
        return spec.stream()//
            .filter(s -> CellToVariableConverterFactory.isSupported(s.getType()))//
            .collect(Collectors.toList());
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_column.saveSettingsTo(settings);
        m_ignoreMissing.saveSettingsTo(settings);
    }

    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_column.loadSettingsFrom(settings);
        m_ignoreMissing.loadSettingsFrom(settings);
    }

    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_column.validateSettings(settings);
        m_ignoreMissing.validateSettings(settings);
    }

    @Override
    protected void loadInternals(final File internDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        //No internal state
    }

    @Override
    protected void saveInternals(final File internDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        //No internal state
    }

    @Override
    protected void reset() {
        // Do nothing, no internal state
    }
}
