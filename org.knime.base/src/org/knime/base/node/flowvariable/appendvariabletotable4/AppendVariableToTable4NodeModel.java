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
package org.knime.base.node.flowvariable.appendvariabletotable4;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.knime.base.node.flowvariable.converter.variabletocell.VariableToCellConverterFactory;
import org.knime.base.node.flowvariable.converter.variabletocell.VariableToDataColumnConverter;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.AbstractCellFactory;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.flowvariable.FlowVariablePortObject;
import org.knime.core.node.streamable.InputPortRole;
import org.knime.core.node.streamable.OutputPortRole;
import org.knime.core.node.streamable.PartitionInfo;
import org.knime.core.node.streamable.PortInput;
import org.knime.core.node.streamable.PortOutput;
import org.knime.core.node.streamable.RowInput;
import org.knime.core.node.streamable.RowOutput;
import org.knime.core.node.streamable.StreamableFunction;
import org.knime.core.node.streamable.StreamableOperator;
import org.knime.core.node.util.filter.variable.FlowVariableFilterConfiguration;
import org.knime.core.node.workflow.FlowVariable;
import org.knime.core.node.workflow.VariableType;

/**
 * NodeModel for the "Variable To TableColumn" node which adds variables as new columns to the input table.
 *
 * @author Bernd Wiswedel, KNIME AG, Zurich, Switzerland
 * @author Patrick Winter, KNIME AG, Zurich, Switzerland
 * @author Marc Bux, KNIME GmbH, Berlin, Germany
 * @author Mark Ortmann, KNIME GmbH, Berlin, Germany
 */
final class AppendVariableToTable4NodeModel extends NodeModel {

    /** Key for the filter configuration. */
    static final String CFG_KEY_FILTER = "variable_filter";

    private static final int DATA_INPUT_PORT_IDX = 1;

    private static final int DATA_OUTPUT_PORT_IDX = 0;

    private FlowVariableFilterConfiguration m_filter;

    /** One input, one output. */
    AppendVariableToTable4NodeModel() {
        super(new PortType[]{FlowVariablePortObject.TYPE_OPTIONAL, BufferedDataTable.TYPE},
            new PortType[]{BufferedDataTable.TYPE});
        m_filter = new FlowVariableFilterConfiguration(CFG_KEY_FILTER);
        m_filter.loadDefaults(getAvailableFlowVariables(VariableToCellConverterFactory.getSupportedTypes()), false);
    }

    @Override
    protected PortObject[] execute(final PortObject[] inData, final ExecutionContext exec) throws Exception {
        final BufferedDataTable table = (BufferedDataTable)inData[1];
        try (final VariableToDataColumnConverter conv = new VariableToDataColumnConverter()) {
            final ColumnRearranger columnRearranger = createColumnRearranger(table.getSpec(), conv, false);
            return new BufferedDataTable[]{exec.createColumnRearrangeTable(table, columnRearranger, exec)};
        }
    }

    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        try (final VariableToDataColumnConverter conv = new VariableToDataColumnConverter()) {
            final ColumnRearranger columnRearranger = createColumnRearranger((DataTableSpec)inSpecs[1], conv, true);
            return new DataTableSpec[]{columnRearranger.createSpec()};
        }
    }

    private ColumnRearranger createColumnRearranger(final DataTableSpec spec, final VariableToDataColumnConverter conv,
        final boolean warn) {
        final ColumnRearranger columnRearranger = new ColumnRearranger(spec);
        final Set<String> nameHash = spec.stream()//
            .map(DataColumnSpec::getName)//
            .collect(Collectors.toCollection(HashSet::new));
        final Map<String, FlowVariable> vars = getFilteredVariables();
        if (warn && vars.isEmpty()) {
            setWarningMessage("No variables selected");
        }

        final DataColumnSpec[] specs = new DataColumnSpec[vars.size()];
        int pos = 0;
        for (final Entry<String, FlowVariable> entry : vars.entrySet()) {
            final FlowVariable var = entry.getValue();
            String name = entry.getKey();
            if (nameHash.contains(name) && !name.toLowerCase().endsWith("(variable)")) {
                name = name.concat(" (variable)");
            }
            String newName = name;
            int uniquifier = 1;
            while (!nameHash.add(newName)) {
                newName = name + " (#" + (uniquifier) + ")";
                uniquifier++;
            }
            specs[pos] = conv.createSpec(newName, var);
            pos++;
        }

        columnRearranger.append(new AbstractCellFactory(specs) {

            private final Object m_lock = new Object();

            private DataCell[] m_cells;

            @Override
            public DataCell[] getCells(final DataRow row) {
                synchronized (m_lock) {
                    if (m_cells == null) {
                        m_cells = vars.entrySet().stream()//
                            .map(e -> conv.getDataCell(e.getKey(), e.getValue()))//
                            .toArray(DataCell[]::new);
                    }
                }
                return m_cells;
            }
        });
        return columnRearranger;
    }

    private Map<String, FlowVariable> getFilteredVariables() {
        final VariableType<?>[] types = VariableToCellConverterFactory.getSupportedTypes();

        final Map<String, FlowVariable> availableVars = getAvailableFlowVariables(types);
        final Set<String> includeNames = new HashSet<>(Arrays.asList(m_filter.applyTo(availableVars).getIncludes()));

        return availableVars.entrySet().stream() //
            .filter(e -> includeNames.contains(e.getKey())) //
            .filter(e -> VariableToCellConverterFactory.isSupported(e.getValue().getVariableType()))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (x, y) -> y, LinkedHashMap::new));
    }

    @Override
    public InputPortRole[] getInputPortRoles() {
        final InputPortRole[] inRoles = Stream.generate(() -> InputPortRole.NONDISTRIBUTED_NONSTREAMABLE)//
            .limit(getNrInPorts())//
            .toArray(InputPortRole[]::new);
        inRoles[DATA_INPUT_PORT_IDX] = InputPortRole.DISTRIBUTED_STREAMABLE;
        return inRoles;
    }

    @Override
    public OutputPortRole[] getOutputPortRoles() {
        final OutputPortRole[] outRoles = Stream.generate(() -> OutputPortRole.NONDISTRIBUTED)//
            .limit(getNrOutPorts())//
            .toArray(OutputPortRole[]::new);
        outRoles[DATA_OUTPUT_PORT_IDX] = OutputPortRole.DISTRIBUTED;
        return outRoles;
    }

    @Override
    public StreamableOperator createStreamableOperator(final PartitionInfo partitionInfo,
        final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        return new StreamableOperator() {

            @Override
            public void runFinal(final PortInput[] inputs, final PortOutput[] outputs, final ExecutionContext exec)
                throws Exception {
                final RowInput in = (RowInput)inputs[DATA_INPUT_PORT_IDX];
                final RowOutput out = (RowOutput)outputs[DATA_OUTPUT_PORT_IDX];
                try (final VariableToDataColumnConverter conv = new VariableToDataColumnConverter()) {
                    final StreamableFunction streamableFunction =
                        createColumnRearranger(in.getDataTableSpec(), conv, false).createStreamableFunction();
                    DataRow row;
                    for (long r = 0; (row = in.poll()) != null; r++) {
                        out.push(streamableFunction.compute(row, r));
                    }
                    in.close();
                    out.close();
                }
            }
        };

    }

    @Override
    protected void reset() {
        // nothing to do
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
    protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec) {
        // nothing to do
    }

    @Override
    protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec) {
        // nothing to do
    }

}
