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
 *   May 1, 2008 (wiswedel): created
 */
package org.knime.base.node.flowvariable.appendvariabletotable3;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.knime.base.node.flowvariable.VariableAndDataCellPair;
import org.knime.base.node.flowvariable.VariableAndDataCellUtil;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.container.AbstractCellFactory;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.flowvariable.FlowVariablePortObject;
import org.knime.core.node.streamable.simple.SimpleStreamableFunctionNodeModel;
import org.knime.core.node.util.filter.variable.FlowVariableFilterConfiguration;
import org.knime.core.node.workflow.VariableType;

/**
 * NodeModel for the "Variable To TableColumn" node which adds variables as new columns to the input table.
 *
 * @author Bernd Wiswedel, KNIME AG, Zurich, Switzerland
 * @author Patrick Winter, KNIME AG, Zurich, Switzerland
 * @author Marc Bux, KNIME GmbH, Berlin, Germany
 */
class AppendVariableToTable3NodeModel extends SimpleStreamableFunctionNodeModel {

    /** Key for the filter configuration. */
    static final String CFG_KEY_FILTER = "variable-filter";

    private FlowVariableFilterConfiguration m_filter;

    /** One input, one output. */
    AppendVariableToTable3NodeModel() {
        super(new PortType[]{FlowVariablePortObject.TYPE_OPTIONAL, BufferedDataTable.TYPE},
            new PortType[]{BufferedDataTable.TYPE}, 1, 0);
        m_filter = new FlowVariableFilterConfiguration(CFG_KEY_FILTER);
        m_filter.loadDefaults(getAvailableFlowVariables(VariableAndDataCellUtil.getSupportedVariableTypes()), false);
    }

    @Override
    protected PortObject[] execute(final PortObject[] inData, final ExecutionContext exec) throws Exception {
        final BufferedDataTable table = (BufferedDataTable)inData[1];
        final ColumnRearranger columnRearranger = createColumnRearranger(table.getSpec());
        return new BufferedDataTable[]{exec.createColumnRearrangeTable(table, columnRearranger, exec)};
    }

    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        final ColumnRearranger columnRearranger = createColumnRearranger((DataTableSpec)inSpecs[1]);
        return new DataTableSpec[]{columnRearranger.createSpec()};
    }

    @Override
    protected ColumnRearranger createColumnRearranger(final DataTableSpec spec) throws InvalidSettingsException {
        final ColumnRearranger columnRearranger = new ColumnRearranger(spec);
        final Set<String> nameHash = spec.stream().map(c -> c.getName()).collect(Collectors.toCollection(HashSet::new));
        final List<VariableAndDataCellPair> vars = getFilteredVariables();
        if (vars.isEmpty()) {
            throw new InvalidSettingsException("No variables selected");
        }

        final DataColumnSpec[] specs = new DataColumnSpec[vars.size()];
        final DataCell[] cells = new DataCell[vars.size()];
        for (int i = 0; i < vars.size(); i++) {
            final VariableAndDataCellPair var = vars.get(i);
            final DataType type = var.getCellType();
            cells[i] = var.getDataCell();

            String name = var.getName();
            if (nameHash.contains(name) && !name.toLowerCase().endsWith("(variable)")) {
                name = name.concat(" (variable)");
            }
            String newName = name;
            int uniquifier = 1;
            while (!nameHash.add(newName)) {
                newName = name + " (#" + (uniquifier++) + ")";
            }
            specs[i] = new DataColumnSpecCreator(newName, type).createSpec();
        }

        columnRearranger.append(new AbstractCellFactory(specs) {
            @Override
            public DataCell[] getCells(final DataRow row) {
                return cells;
            }
        });
        return columnRearranger;
    }

    private List<VariableAndDataCellPair> getFilteredVariables() {
        final VariableType<?>[] types = VariableAndDataCellUtil.getSupportedVariableTypes();

        final Set<String> include_names =
            new HashSet<>(Arrays.asList(m_filter.applyTo(getAvailableFlowVariables(types)).getIncludes()));

        return VariableAndDataCellUtil.getVariablesAsDataCells(getAvailableFlowVariables(types)).stream()
            .filter(v -> include_names.contains(v.getName())).collect(Collectors.toList());
    }

    @Override
    protected void reset() {
    }

    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        final FlowVariableFilterConfiguration conf = new FlowVariableFilterConfiguration(CFG_KEY_FILTER);
        conf.loadConfigurationInModel(settings);
        m_filter = conf;
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_filter.saveConfiguration(settings);
    }

    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        final FlowVariableFilterConfiguration conf = new FlowVariableFilterConfiguration(CFG_KEY_FILTER);
        conf.loadConfigurationInModel(settings);
    }

    @Override
    protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
    }

    @Override
    protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
    }

}
