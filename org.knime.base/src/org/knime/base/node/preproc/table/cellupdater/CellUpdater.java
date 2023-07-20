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
 *   17 Feb 2023 (ivan.prigarin): created
 */
package org.knime.base.node.preproc.table.cellupdater;

import java.util.Map;
import java.util.OptionalInt;
import java.util.stream.IntStream;

import org.knime.base.node.flowvariable.converter.variabletocell.VariableToCellConverterFactory;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.node.workflow.FlowVariable;

/**
 * Utilities for the Cell Updater node.
 * @author Ivan Prigarin, KNIME GmbH, Konstany, Germany
 */
final class CellUpdater {

    /**
     * A Container class for holding the column index and flow variable name for a pair of type-matched column/flow
     * variable.
     */
    static class Match {
        private int m_matchedColumnIndex;

        private String m_matchedVariableName;

        Match(final int colIndex, final String varName) {
            m_matchedColumnIndex = colIndex;
            m_matchedVariableName = varName;
        }

        int getMatchedColIdx() {
            return m_matchedColumnIndex;
        }

        String getMatchedVarName() {
            return m_matchedVariableName;
        }
    }

    /**
     * Matches the available input flow variables with the columns in the given DataTableSpec based on their data types.
     * If a match is found, it returns the column index and the name of the matched flow variable as a Match object.
     * Otherwise returns a Match object with 0 as the column index and the name of the first available flow variable.
     *
     * @param spec the DataTableSpec to match the flow variables against
     * @param availableVars the map of flow variables received from DefaultNodeSettingsContext
     * @return a Match object containing the column index and the name of the matched flow variable, or null if no match
     *         was found.
     */
    static Match matchColumnsAndVariables(final DataTableSpec spec,
        final Map<String, FlowVariable> availableVars) {
        for (FlowVariable fv : availableVars.values()) {
            if (fv == null) {
                continue;
            }
            final String currentVarName = fv.getName();
            DataType currentVarType = convertVariableToCell(fv).getType();
            if (spec.containsCompatibleType(currentVarType.getPreferredValueClass())) {
                OptionalInt matchIndex = IntStream.range(0, spec.getNumColumns())
                    .filter(i -> spec.getColumnSpec(i).getType().isASuperTypeOf(currentVarType)).findFirst();
                if (matchIndex.isPresent()) {
                    return new Match(matchIndex.orElseThrow(), currentVarName);
                }
            }
        }
        return null;
    }

    static DataCell convertVariableToCell(final FlowVariable flowVar) {
        final var varConverter = VariableToCellConverterFactory.createConverter(flowVar);
        return varConverter.getDataCell(flowVar);
    }

    static String getTypeMismatchWarningMessage(final String colName, final DataType currentType,
        final DataType newType) {
        return String.format("Incompatible update value of type \"%s\" for a cell of type \"%s\" in column \"%s\".",
            newType.getName(), currentType.getName(), colName);
    }

    static String getFirstFlowVariableName(final Map<String, FlowVariable> availableVars) {
        return availableVars.values().iterator().next().getName();
    }

}
