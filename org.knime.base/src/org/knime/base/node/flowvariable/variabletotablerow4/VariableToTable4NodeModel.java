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
 *   Jan 29, 2025 (Martin Sillye, TNG Technology Consulting GmbH): created
 */
package org.knime.base.node.flowvariable.variabletotablerow4;

import java.util.Collection;

import org.knime.base.node.flowvariable.converter.variabletocell.VariableToCellConverterFactory;
import org.knime.base.node.flowvariable.converter.variabletocell.VariableToDataColumnConverter;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.node.workflow.FlowVariable;
import org.knime.core.webui.node.impl.WebUINodeConfiguration;
import org.knime.core.webui.node.impl.WebUINodeModel;

/**
 * The model for the "Variable to Table Row" node.
 *
 * @author Martin Sillye, TNG Technology Consulting GmbH
 */
@SuppressWarnings("restriction")
public class VariableToTable4NodeModel extends WebUINodeModel<VariableToTable4NodeSettings> {

    VariableToTable4NodeModel(final WebUINodeConfiguration configuration) {
        super(configuration, VariableToTable4NodeSettings.class);
    }

    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs,
        final VariableToTable4NodeSettings modelSettings) throws InvalidSettingsException {
        CheckUtils.checkSetting(modelSettings.m_rowName != null && !modelSettings.m_rowName.trim().isEmpty(),
            "Please specify a row name.");
        final var variables =
            getAvailableInputFlowVariables(VariableToCellConverterFactory.getSupportedTypes()).values();
        try (final var converter = new VariableToDataColumnConverter()) {
            final var selectedVariables = modelSettings.m_filter.filter(variables);
            if (selectedVariables.isEmpty()) {
                setWarningMessage("No variables selected");
            }
            return new DataTableSpec[]{createTableSpec(selectedVariables, converter)};
        }
    }

    @Override
    protected PortObject[] execute(final PortObject[] inObjects, final ExecutionContext exec,
        final VariableToTable4NodeSettings modelSettings) throws Exception {
        final var variables =
            getAvailableInputFlowVariables(VariableToCellConverterFactory.getSupportedTypes()).values();
        try (final var converter = new VariableToDataColumnConverter()) {
            final var selectedVariables = modelSettings.m_filter.filter(variables);
            final BufferedDataContainer cont = exec.createDataContainer(createTableSpec(selectedVariables, converter));
            cont.addRowToTable(createTableRow(converter, modelSettings.m_rowName, selectedVariables));
            cont.close();
            return new BufferedDataTable[]{cont.getTable()};
        }
    }

    private static final DataRow createTableRow(final VariableToDataColumnConverter conv, final String rowId,
        final Collection<FlowVariable> variables) {
        final DataCell[] cells = variables.stream()//
            .map(variable -> conv.getDataCell(variable.getName(), variable))//
            .toArray(DataCell[]::new);
        return new DefaultRow(rowId, cells);
    }

    private static final DataTableSpec createTableSpec(final Collection<FlowVariable> variables,
        final VariableToDataColumnConverter converter) {
        final var colSpecs = variables.stream().map(variable -> converter.createSpec(variable.getName(), variable))
            .toArray(DataColumnSpec[]::new);
        return new DataTableSpec(colSpecs);
    }
}
