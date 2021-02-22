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
package org.knime.base.node.flowvariable.converter.variabletocell;

import java.util.HashMap;
import java.util.Map;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.node.workflow.FlowVariable;
import org.knime.core.node.workflow.VariableType;

/**
 * This class allows to convert {@link FlowVariable}s to {@link DataCell}s.
 *
 * @author Mark Ortmann, KNIME GmbH, Berlin, Germany
 */
public final class VariableToDataColumnConverter implements AutoCloseable {

    private final Map<String, ConvenienceVariableToCellConverter> m_converters;

    /**
     * Constructor.
     */
    public VariableToDataColumnConverter() {
        m_converters = new HashMap<>();
    }

    private ConvenienceVariableToCellConverter getConverter(final String columnName) {
        if (!m_converters.containsKey(columnName)) {
            m_converters.put(columnName, new ConvenienceVariableToCellConverter());
        }
        return m_converters.get(columnName);
    }

    /**
     * Creates the {@link DataCell} from the given {@link FlowVariable}.
     * @param columnName the column name
     * @param flowVar the {@link FlowVariable} to be converted to a {@link DataCell}
     *
     * @return the cell representation of the converted variable
     */
    public DataCell getDataCell(final String columnName, final FlowVariable flowVar) {
        return getConverter(columnName).getDataCell(flowVar);
    }

    /**
     * Creates the data column for this converter.
     *
     * @param columnName the name of the data column to be created
     * @param flowVar the {@link FlowVariable} for which the {@link DataColumnSpec} needs to be created
     * @return the data {@link DataColumnSpec} of the column to be created
     */
    public DataColumnSpec createSpec(final String columnName, final FlowVariable flowVar) {
        return getConverter(columnName).createSpec(columnName, flowVar);
    }

    @Override
    public void close() {
        m_converters.values().stream()//
            .close();
    }

    private static class ConvenienceVariableToCellConverter implements VariableToCellConverter {

        private final Map<VariableType<?>, VariableToCellConverter> m_converters;

        ConvenienceVariableToCellConverter() {
            m_converters = new HashMap<>();
        }

        private VariableToCellConverter getConverter(final FlowVariable flowVar) {
            final VariableType<?> varType = flowVar.getVariableType();
            if (!m_converters.containsKey(varType)) {
                m_converters.put(varType, VariableToCellConverterFactory.createConverter(flowVar));
            }
            return m_converters.get(varType);
        }

        @Override
        public DataCell getDataCell(final FlowVariable flowVar) {
            return getConverter(flowVar).getDataCell(flowVar);
        }

        @Override
        public DataColumnSpec createSpec(final String columnName, final FlowVariable flowVar) {
            return getConverter(flowVar).createSpec(columnName, flowVar);
        }

    }

}
