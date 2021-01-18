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
 *   Oct 23, 2020 (Mark Ortmann, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.base.node.flowvariable.variabletotablerow4;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.knime.base.node.flowvariable.converter.variabletocell.VariableToCellConverterFactory;
import org.knime.base.node.flowvariable.converter.variabletocell.VariableToDataColumnConverter;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.util.filter.variable.FlowVariableFilterConfiguration;
import org.knime.core.node.workflow.FlowVariable;
import org.knime.core.node.workflow.VariableType;

/**
 * {@link NodeModel} that offers the basic functionality to convert a set of selected variables to a table row.
 *
 * @author Mark Ortmann, KNIME GmbH, Berlin, Germany
 */
public abstract class AbstractVariableToTableNodeModel extends NodeModel {

    /** Key for the filter configuration. */
    protected static final String CFG_KEY_FILTER = "variable_filter";

    private FlowVariableFilterConfiguration m_filter;

    /**
     * Constructor.
     *
     * @param inputPortType the input port type
     */
    protected AbstractVariableToTableNodeModel(final PortType inputPortType) {
        super(new PortType[]{inputPortType}, new PortType[]{BufferedDataTable.TYPE});
        m_filter = new FlowVariableFilterConfiguration(CFG_KEY_FILTER);
        m_filter.loadDefaults(getAvailableFlowVariables(VariableToCellConverterFactory.getSupportedTypes()), false);
    }

    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        return new DataTableSpec[]{createOutSpec(true)};
    }

    /**
     * Creates the data row containing the selected variables.
     *
     * @param exec the {@link ExecutionContext}
     * @param conv the {@link VariableToDataColumnConverter}
     * @param rowId a String specifying the row ID
     * @return the {@link DataRow} obtained by converting the selected {@link FlowVariable}s
     */
    protected DataRow createTableRow(final ExecutionContext exec, final VariableToDataColumnConverter conv,
        final String rowId) {
        final DataCell[] cells = getFilteredVariables().entrySet().stream()//
            .map(e -> conv.getDataCell(exec, e.getKey(), e.getValue()))//
            .toArray(DataCell[]::new);
        return new DefaultRow(rowId, cells);
    }

    private Map<String, FlowVariable> getFilteredVariables() {
        final VariableType<?>[] types = VariableToCellConverterFactory.getSupportedTypes();

        final Map<String, FlowVariable> availableVars = getAvailableFlowVariables(types);
        final Set<String> includeNames = new HashSet<>(Arrays.asList(m_filter.applyTo(availableVars).getIncludes()));

        return availableVars.entrySet().stream() //
            .filter(e -> includeNames.contains(e.getKey())) //
            .filter(e -> VariableToCellConverterFactory.isSupported(e.getValue().getVariableType())) //
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (x, y) -> y, LinkedHashMap::new));
    }

    /**
     * Returns the {@link DataTableSpec} with respect to the selected {@link FlowVariable}s.
     *
     * @param warn if {@code true} a warning is issued if no variables have been selected
     * @return the data table spec created when converting the selected {@link FlowVariable}s
     * @throws InvalidSettingsException - If any of the settings is incorrect
     */
    protected DataTableSpec createOutSpec(final boolean warn) throws InvalidSettingsException {
        try (final VariableToDataColumnConverter conv = new VariableToDataColumnConverter()) {
            final DataColumnSpec[] specs = getFilteredVariables().entrySet().stream() //
                .map(e -> conv.createSpec(e.getKey(), e.getValue())) //
                .toArray(DataColumnSpec[]::new);
            if (warn && specs.length == 0) {
                setWarningMessage("No variables selected");
            }
            return new DataTableSpec(specs);
        }
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
    protected final void loadInternals(final File nodeInternDir, final ExecutionMonitor exec) {
        // nothing to do
    }

    @Override
    protected final void saveInternals(final File nodeInternDir, final ExecutionMonitor exec) {
        // nothing to do
    }

}
