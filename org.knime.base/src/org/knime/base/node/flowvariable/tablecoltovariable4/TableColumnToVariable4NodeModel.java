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
 *   Feb 21, 2025 (Martin Sillye, TNG Technology Consulting GmbH): created
 */
package org.knime.base.node.flowvariable.tablecoltovariable4;

import java.util.Collection;
import java.util.stream.Collectors;

import org.knime.base.node.flowvariable.converter.celltovariable.CellToVariableConverter;
import org.knime.base.node.flowvariable.converter.celltovariable.CellToVariableConverterFactory;
import org.knime.base.node.flowvariable.converter.celltovariable.MissingValueHandler;
import org.knime.base.node.flowvariable.tablecoltovariable4.TableColumnToVariable4NodeSettings.MissingOperation;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.MissingValueException;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.flowvariable.FlowVariablePortObject;
import org.knime.core.node.port.flowvariable.FlowVariablePortObjectSpec;
import org.knime.core.node.workflow.FlowVariable;
import org.knime.core.node.workflow.VariableType;
import org.knime.core.webui.node.impl.WebUINodeConfiguration;
import org.knime.core.webui.node.impl.WebUINodeModel;

/**
 * The model for the "Tabke Column to Variable" node.
 *
 * @author Martin Sillye, TNG Technology Consulting GmbH
 */
@SuppressWarnings("restriction")
final class TableColumnToVariable4NodeModel extends WebUINodeModel<TableColumnToVariable4NodeSettings> {

    TableColumnToVariable4NodeModel(final WebUINodeConfiguration configuration) {
        super(configuration, TableColumnToVariable4NodeSettings.class);
    }

    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs,
        final TableColumnToVariable4NodeSettings modelSettings) throws InvalidSettingsException {
        final DataTableSpec spec = (DataTableSpec)inSpecs[0];
        // validation
        final int colIndex = spec.findColumnIndex(modelSettings.m_column);
        if (colIndex < 0) {
            throw new InvalidSettingsException(
                String.format("The selected column '%s' is not part of the input", modelSettings.m_column));
        }
        if (applicableColumns(spec).stream()//
            .map(DataColumnSpec::getName)//
            .noneMatch(cName -> cName.equals(modelSettings.m_column))) {
            throw new InvalidSettingsException(String
                .format("The selected column '%s' cannot be converted to a flow variable", modelSettings.m_column));
        }
        return new PortObjectSpec[]{FlowVariablePortObjectSpec.INSTANCE};
    }

    @Override
    protected PortObject[] execute(final PortObject[] inObjects, final ExecutionContext exec,
        final TableColumnToVariable4NodeSettings modelSettings) throws Exception {
        if (inObjects[0] instanceof BufferedDataTable) {
            final BufferedDataTable table = (BufferedDataTable)inObjects[0];
            final DataTableSpec spec = table.getSpec();
            final int colIndex = spec.findColumnIndex(modelSettings.m_column);
            assert colIndex >= 0 : colIndex;
            try {
                final CellToVariableConverter<?> cell2VarConverter =
                    CellToVariableConverterFactory.createConverter(spec.getColumnSpec(colIndex).getType());
                if (table.size() > 0) {
                    convertColToVar(table, colIndex, cell2VarConverter, modelSettings);
                } else {
                    setWarningMessage("Node created no variables since the input data table is empty.");
                }
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException(
                    e.getMessage() + " (column \"" + spec.getColumnNames()[colIndex] + "\" (index " + colIndex + "))",
                    e);
            }
        }
        return new FlowVariablePortObject[]{FlowVariablePortObject.INSTANCE};
    }

    private void convertColToVar(final BufferedDataTable table, final int colIndex,
        final CellToVariableConverter<?> cell2VarConverter, final TableColumnToVariable4NodeSettings modelSettings) {
        for (final DataRow row : table) {
            final DataCell cell = row.getCell(colIndex);
            final String name = row.getKey().getString();
            cell2VarConverter.createFlowVariable(name, cell, //
                getHandler(modelSettings.m_column, colIndex, row.getKey().toString(),
                    modelSettings.m_missingOperation == MissingOperation.IGNORE))//
                .ifPresent(this::pushVariable);
        }
    }

    private static MissingValueHandler getHandler(final String columnName, final int columnIndex, final String rowName,
        final boolean ignoreMissing) {
        if (ignoreMissing) {
            return (v, t) -> null;
        } else {
            return (v, t) -> {
                throw new MissingValueException(v,
                    String.format(
                        "Missing values are not allowed as variable values -- column \"%s\" (index %d) for row \"%s\"",
                        columnName, columnIndex, rowName));
            };
        }
    }

    @SuppressWarnings("unchecked")
    private <T> void pushVariable(final FlowVariable fv) {
        pushFlowVariable(fv.getName(), (VariableType<T>)fv.getVariableType(), (T)fv.getValue(fv.getVariableType()));
    }

    private static Collection<DataColumnSpec> applicableColumns(final DataTableSpec spec) {
        return spec.stream()//
            .filter(s -> CellToVariableConverterFactory.isSupported(s.getType()))//
            .collect(Collectors.toList());
    }

}
