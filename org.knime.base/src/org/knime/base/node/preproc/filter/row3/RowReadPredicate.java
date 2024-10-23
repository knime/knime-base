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
 *   22 Mar 2024 (jasper): created
 */
package org.knime.base.node.preproc.filter.row3;

import static org.knime.base.node.preproc.filter.row3.predicates.PredicateFactory.ALWAYS_FALSE;
import static org.knime.base.node.preproc.filter.row3.predicates.PredicateFactory.ALWAYS_TRUE;

import org.knime.base.node.preproc.filter.row3.AbstractRowFilterNodeSettings.FilterCriterion;
import org.knime.base.node.preproc.filter.row3.predicates.IndexedRowReadPredicate;
import org.knime.base.node.preproc.filter.row3.predicates.PredicateFactories;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.v2.RowRead;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.webui.node.dialog.defaultdialog.widget.dynamic.DynamicValuesInput;

/**
 * Utility to create predicates for filtering rows by RowID, data values, or missingness.
 *
 * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
 */
@SuppressWarnings("restriction") // webui
final class RowReadPredicate {

    private RowReadPredicate() {
        // hidden
    }

    /**
     * Builds a single predicate from filter criteria, combined by AND or OR.
     *
     * @param isAnd if {@code true}, combine predicates with AND, otherwise with OR
     * @param filterCriteria filter criteria to build predicates from
     * @param spec data table spec to filter on
     * @return predicate that combines all filter criteria
     * @throws InvalidSettingsException if a filter criterion is invalid
     */
    static IndexedRowReadPredicate buildPredicate(final boolean isAnd, final Iterable<FilterCriterion> filterCriteria,
        final DataTableSpec spec) throws InvalidSettingsException {
        final var iter = filterCriteria.iterator();
        if (!iter.hasNext()) {
            return null;
        }
        // collect predicates from filter criteria, short-circuiting whenever we can prove that the predicate will
        // always be true or false
        var filterPredicate = createFrom(iter.next(), spec);
        while (iter.hasNext()) {
            final var predicate = createFrom(iter.next(), spec);
            if (isAnd) {
                // x AND false -> false
                // x AND true -> x
                if (predicate == ALWAYS_FALSE) {
                    filterPredicate = ALWAYS_FALSE;
                } else if (predicate == ALWAYS_TRUE) {
                    // nothing to change
                } else {
                    filterPredicate = filterPredicate.and(predicate);
                }
            } else {
                // x OR false -> x
                // x OR true -> true
                if (predicate == ALWAYS_FALSE) {
                    // nothing to change
                } else if (predicate == ALWAYS_TRUE) {
                    filterPredicate = ALWAYS_TRUE;
                } else {
                    filterPredicate = filterPredicate.or(predicate);
                }
            }
        }
        return filterPredicate;
    }

    private static IndexedRowReadPredicate createFrom(final FilterCriterion criterion, final DataTableSpec spec)
        throws InvalidSettingsException {

        // Special case for RowID, which is not a DataValue
        if (criterion.isFilterOnRowKeys()) {
            return rowKeyPredicate(criterion);
        }

        // case where row numbers are treated as data, e.g. WILDCARD/REGEX matching
        if (criterion.isFilterOnRowNumbers()) {
            return rowNumberPredicate(criterion);
        }

        final var column = criterion.m_column.getSelected();
        final var columnIndex = spec.findColumnIndex(column);
        if (columnIndex < 0) {
            throw new InvalidSettingsException("Column \"%s\" could not be found in input table".formatted(column));
        }

        return translateToPredicate(criterion.m_operator, criterion.m_predicateValues, columnIndex,
            spec.getColumnSpec(columnIndex).getType());
    }


    private static IndexedRowReadPredicate rowKeyPredicate(final FilterCriterion criterion)
            throws InvalidSettingsException {
//        final int index = 0; // take from first widget input value
//        final var predicate = new StringPredicate(criterion.m_operator, isCaseSensitiveMatch(criterion, index),
//            criterion.m_predicateValues.getCellAt(index).map(c -> (StringCell)c).map(StringCell::getStringValue)
//                .orElseThrow(() -> new InvalidSettingsException("Missing string value for RowID comparison")));
//        return (i, row) -> predicate.test(row.getRowKey().getString());


    }

    private static IndexedRowReadPredicate rowNumberPredicate(final FilterCriterion criterion)
        throws InvalidSettingsException {
//        final int index = 0; // first widget value
//        final var predicate = new StringPredicate(criterion.m_operator, false,
//            criterion.m_predicateValues.getCellAt(index).map(c -> (StringCell)c).map(StringCell::getStringValue)
//                .orElseThrow(() -> new InvalidSettingsException("Missing reference value for row number comparison")));
//        // translate from index to row number
//        return (idx, row) -> predicate.test(Long.toString(idx + 1));
    }

    /**
     * Translates the operator and predicate values into a predicate on a {@link RowRead}.
     *
     * @param operator filter operator
     * @param predicateValues values for predicate
     * @param columnIndex index of column to filter on, or negative for row key
     * @param dataType data type of column (or {@link StringCell#TYPE} for row key)
     * @return predicate on a row read
     *
     * @throws InvalidSettingsException in case the arguments are inconsistent, e.g. filtering a row key for missingness
     */
    static IndexedRowReadPredicate translateToPredicate(final FilterOperator operator,
        final DynamicValuesInput predicateValues, final int columnIndex, final DataType dataType)
        throws InvalidSettingsException {
        final var isRowKey = columnIndex < 0;
        // handle missingness tests early
        if (operator == FilterOperator.IS_MISSING || operator == FilterOperator.IS_NOT_MISSING) {
            // only missing value filter, no value predicate present
            if (isRowKey) {
                throw new InvalidSettingsException(
                    "Cannot filter RowID for presence, since the RowID is never missing.");
            }
            final var isMissing = operator == FilterOperator.IS_MISSING;
            return isMissing ? PredicateFactories.IS_MISSING_FACTORY.apply(columnIndex)
                : PredicateFactories.IS_NOT_MISSING_FACTORY.apply(columnIndex);
        }

        // get an actual (non-missing) value predicate
        return translateToValuePredicate(operator, predicateValues, columnIndex, dataType);
    }

    private static IndexedRowReadPredicate translateToValuePredicate(final FilterOperator operator,
        final DynamicValuesInput predicateValues, final int columnIndex, final DataType dataType)
        throws InvalidSettingsException {


        final var valuePredicate = PredicateFactories //
            .getValuePredicateFactory(operator, dataType) //
            .orElseThrow(() -> new InvalidSettingsException(
                "Unsupported operator for input column type \"%s\"".formatted(dataType.getName())))
            .createPredicate(columnIndex, predicateValues);

        /*
         * Missing value handling:
         *
         * Missing values never match the RowRead predicate.
         * This means the value predicate can (and should since it operates on values, not cells) only be evaluated if
         * it is not missing. The row key cannot be missing, so we have to always evaluate the predicate against it.
         *
         * This results in the following test to determine if the row should pass the filter or not:
         *
         *   (isRowKey OR !isMissing) AND valuePredicate
         *
         */

        if (valuePredicate == ALWAYS_FALSE) {
            // the AND can never be true, if the value predicate always returns false
            return ALWAYS_FALSE;
        }
        final var isRowKey = columnIndex < 0;
        if (valuePredicate == ALWAYS_TRUE) {
            // since row keys are never missing, we can return always true here
            // for a non-rowkey column for which the predicate is always true, we still need to check its presence
            return isRowKey ? ALWAYS_TRUE : (idx, rowRead) -> !rowRead.isMissing(columnIndex);
        }

        // rowKey cannot be missing, so we can safely evaluate the value predicate
        return isRowKey ? valuePredicate
            : ((idx, rowRead) -> !rowRead.isMissing(columnIndex) && valuePredicate.test(idx, rowRead));
    }
}
