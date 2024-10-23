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
 *   16 Dec 2024 (Manuel Hotz, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.base.data.filter.row.v2;

import org.knime.core.data.v2.RowRead;
import org.knime.core.node.util.CheckUtils;

/**
 * A filter using a row offset (aka. row index) from the start of the table.
 *
 * @param operator filter operator taking an offset value
 * @param offset offset value
 */
public record OffsetFilter(Operator operator, long offset) {

    /**
     * Supported operators for row number offset filter.
     */
    public enum Operator {
            /** "Row number equals". */
            EQ,
            /** "Row number does not equal". */
            NEQ,
            /** "Row number is less than". */
            LT,
            /** "Row number is less than or equal to". */
            LTE,
            /** "Row number is greater than". */
            GT,
            /** "Row number is greater than or equal to". */
            GTE
    }

    /**
     * Creates a new offset filter.
     *
     * @param operator operator to use
     * @param offset non-negative offset from start of table
     */
    public OffsetFilter {
        CheckUtils.checkArgument(offset >= 0, "Offset must not be negative: %d", offset);
    }

    /**
     * Converts the offset filter definition into a predicate that can be evaluated on an {@link RowRead indexed row
     * read}.
     *
     * @return predicate to evaluate on indexed row read
     */
    public IndexedRowReadPredicate asPredicate() {
        return switch (operator) {
            case EQ -> (rowIndex, read) -> rowIndex == offset;
            case NEQ -> (rowIndex, read) -> rowIndex != offset;
            case LT -> (rowIndex, read) -> rowIndex < offset;
            case LTE -> (rowIndex, read) -> rowIndex <= offset;
            case GT -> (rowIndex, read) -> rowIndex > offset;
            case GTE -> (rowIndex, read) -> rowIndex >= offset;
        };
    }
}
