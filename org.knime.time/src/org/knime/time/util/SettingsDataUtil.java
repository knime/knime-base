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
 *   Jan 16, 2025 (sillyem): created
 */
package org.knime.time.util;

import java.util.Arrays;
import java.util.Optional;
import java.util.function.Predicate;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.BufferedDataTable;
import org.knime.node.parameters.NodeParametersInput;

/**
 *
 * @author Martin Sillye, TNG Technology Consulting GmbH
 */
@SuppressWarnings("restriction")
public final class SettingsDataUtil {

    private SettingsDataUtil() {
        // utility class shouldn't be instantiated
    }

    /**
     * Gets the DataTable from the context
     *
     * @param context of the node settings
     * @return the DataTable, or an empty Optional if input ports are null
     */
    public static Optional<BufferedDataTable> getDataTable(final NodeParametersInput context) {
        var inputPorts = context.getInPortObjects();

        if (inputPorts != null && inputPorts.length > 0) {
            return context.getInTable(0);
        }
        return Optional.empty();
    }

    /**
     * Gets the index of the first column which is in the list of selected columns and also
     *
     * @param spec to get the columns from
     * @param columnFilter used to filter columns
     * @param selectedColumns names of allowed columns
     * @return the index of the column
     */
    public static Optional<Integer> getFirstColumnIndexFromSelectedColumnArray(final DataTableSpec spec,
        final Predicate<DataColumnSpec> columnFilter, final String[] selectedColumns) {
        var firstCompatibleColumnNameOptional = spec.stream() //
            .filter(columnFilter) //
            .map(DataColumnSpec::getName) //
            .filter(Arrays.asList(selectedColumns)::contains) //
            .findFirst();

        return firstCompatibleColumnNameOptional //
            .map(spec::findColumnIndex);
    }

    /**
     * Gets the first cell in a column that's not a MissingCell
     *
     * @param dt the datatable
     * @param colNum the index of the column
     * @return the data cell
     */
    public static Optional<DataCell> getFirstNonMissingCellInColumn(final DataTable dt, final int colNum) {
        for (var row : dt) {
            var cell = row.getCell(colNum);
            if (!cell.isMissing()) {
                return Optional.of(cell);
            }
        }
        return Optional.empty();
    }
}
