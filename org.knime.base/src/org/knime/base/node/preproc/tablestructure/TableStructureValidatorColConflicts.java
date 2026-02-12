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
 *   12.02.2026 (mgohm): created
 */
package org.knime.base.node.preproc.tablestructure;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.knime.base.node.preproc.tablestructure.TableStructureValidatorColConflicts.TableStructureValidatorColConflict;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataTableSpecCreator;
import org.knime.core.data.DataType;
import org.knime.core.data.RowKey;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.StringCell;

/**
 * Buffer for all column conflicts during one execution.
 *
 * @author Magnus Gohm, KNIME GmbH, Konstanz, Germany
 */
final class TableStructureValidatorColConflicts implements Iterable<TableStructureValidatorColConflict> {

    /**
     * The table spec for the optional data output.
     */
    public static final DataTableSpec CONFLICTS_SPEC = new DataTableSpecCreator().addColumns(
        new DataColumnSpecCreator("Column", StringCell.TYPE).createSpec(), //
        new DataColumnSpecCreator("Error ID", StringCell.TYPE).createSpec(), //
        new DataColumnSpecCreator("Description", StringCell.TYPE).createSpec()).createSpec();

    private List<TableStructureValidatorColConflict> m_conflicts = new ArrayList<>();

    /**
     * Adds a conflict to this container.
     *
     * @param conflict the conflict
     * @return the added conflict.
     */
    TableStructureValidatorColConflict addConflict(final TableStructureValidatorColConflict conflict) {
        m_conflicts.add(conflict);
        return conflict;
    }

    /**
     * Checks if the list of {@link TableStructureValidatorColConflict} is empty.
     *
     * @return <code>true</code> if no conflicts have been published
     */
    boolean isEmpty() {
        return m_conflicts.isEmpty();
    }

    /**
     * Creates a missing column {@link TableStructureValidatorColConflict}.
     *
     * @param columnName the column name
     * @return {@link TableStructureValidatorColConflict}
     */
    static TableStructureValidatorColConflict missingColumn(final String columnName) {
        return new TableStructureValidatorColConflict(columnName, Conflict.COLUMN_NOT_CONTAINED,
            Conflict.COLUMN_NOT_CONTAINED.getDescription(columnName));
    }

    /**
     * Creates a invalid type {@link TableStructureValidatorColConflict}.
     *
     * @param columnName the column name
     * @param expected the expected {@link DataType}
     * @param got the current {@link DataType}
     * @return {@link TableStructureValidatorColConflict}
     */
    static TableStructureValidatorColConflict invalidType(
        final String columnName, final DataType expected, final DataType got) {
        return new TableStructureValidatorColConflict(columnName, Conflict.INVALID_DATATYPE,
            Conflict.INVALID_DATATYPE.getDescription(columnName, expected, got));
    }

    /**
     * Creates a conversion failed {@link TableStructureValidatorColConflict}.
     *
     * @param columnName the column name
     * @param rowKey the {@link RowKey} of the current row
     * @param targetType the target {@link DataType}
     * @return {@link TableStructureValidatorColConflict}
     */
    static TableStructureValidatorColConflict conversionFailed(final String columnName, final RowKey rowKey,
        final DataType targetType) {
        return new TableStructureValidatorColConflict(columnName, Conflict.CONVERSION_FAILED,
            Conflict.CONVERSION_FAILED.getDescription(columnName, rowKey, targetType));
    }

    /**
     * Creates a unknown column {@link TableStructureValidatorColConflict}.
     *
     * @param columnName the column name
     * @return {@link TableStructureValidatorColConflicts}
     */
    static TableStructureValidatorColConflict unknownColumn(final String columnName) {
        return new TableStructureValidatorColConflict(columnName, Conflict.UNKNOWN_COLUMN,
            Conflict.UNKNOWN_COLUMN.getDescription(columnName));
    }

    private enum Conflict {

        COLUMN_NOT_CONTAINED("Column '%s' is missing."), //
        CONVERSION_FAILED("Conversion of cell in column '%s' in row '%s' to type '%s' failed."), //
        INVALID_DATATYPE("DataType of column '%s' should be of type '%s' but was '%s'."), //
        UNKNOWN_COLUMN("Columns '%s' was not defined in input spec.");

        private final String m_descriptionTemplate;

        Conflict(final String descriptionTemplate) {
            m_descriptionTemplate = descriptionTemplate;
        }

        private String getDescription(final Object... templateObjects) {
            return String.format(m_descriptionTemplate, templateObjects);
        }

    }

    @Override
    public String toString() {
        var builder = new StringBuilder();
        for (int i = 0; i < m_conflicts.size(); i++) {
            builder.append(m_conflicts.get(i));
            if (i < m_conflicts.size() - 1) {
                builder.append("\n");
            }
        }
        return builder.toString();
    }

    @Override
    public Iterator<TableStructureValidatorColConflict> iterator() {
        return Collections.unmodifiableList(new ArrayList<>(m_conflicts)).iterator();
    }

    static final class TableStructureValidatorColConflict {

        private String m_columnName;
        private Conflict m_conflict;
        private String m_message;

        private TableStructureValidatorColConflict(
            final String columnName, final Conflict conflict, final String message) {
            m_columnName = columnName;
            m_conflict = conflict;
            m_message = message;
        }

        /**
         * Retrieves the {@link DataRow} containing the conflict in the format
         * specified by {@link TableStructureValidatorColConflicts#CONFLICTS_SPEC}.
         *
         * @param rowKey the current {@link RowKey}
         * @return {@link DataRow}
         */
        public DataRow toDataRow(final RowKey rowKey) {
            return new DefaultRow(rowKey, m_columnName, m_conflict.name(), m_message);
        }

        @Override
        public String toString() {
            return String.format("Column: '%s' causes validation issue: '%s'. Detailed information:%n\t%s",
                m_columnName, m_conflict.name(), m_message);
        }

    }

}
