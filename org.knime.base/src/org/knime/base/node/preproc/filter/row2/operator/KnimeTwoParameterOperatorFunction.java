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
 *   Jun 11, 2019 (Perla Gjoka): created
 */
package org.knime.base.node.preproc.filter.row2.operator;

import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

import org.knime.base.data.filter.row.dialog.OperatorParameters;
import org.knime.base.data.filter.row.dialog.model.ColumnSpec;
import org.knime.core.data.DataCell;
import org.knime.core.node.util.CheckUtils;

import com.google.common.collect.Range;

/**
 * {@link KnimeOperatorFunction} for operators with two parameters of the same type.
 *
 * @author Perla Gjoka, KNIME GmbH, Konstanz, Germany
 */
final class KnimeTwoParameterOperatorFunction implements KnimeOperatorFunction {

    private final Function<String, DataCell> m_convertToDataCell;

    private final BiFunction<DataCell, DataCell, Predicate<DataCell>> m_cellPredicateFunction;

    private final BiFunction<ColumnSpec, String[], Range<Long>> m_rangeFunction;

    /**
     * @param convertToDataCell takes the input by the user and converts it from string to a DataCell.
     * @param cellPredicateFunction creates a predicate DataCell of the two inputs given by the user.
     * @param rangeFunction maps user input about the row index into a range of needed rows.
     */
    public KnimeTwoParameterOperatorFunction(final Function<String, DataCell> convertToDataCell,
        final BiFunction<DataCell, DataCell, Predicate<DataCell>> cellPredicateFunction,
        final BiFunction<ColumnSpec, String[], Range<Long>> rangeFunction) {
        m_convertToDataCell = convertToDataCell;
        m_cellPredicateFunction = cellPredicateFunction;
        m_rangeFunction = rangeFunction;
    }

    public KnimeTwoParameterOperatorFunction(final Function<String, DataCell> convertToDataCell,
        final BiFunction<DataCell, DataCell, Predicate<DataCell>> cellPredicateFunction) {
        this(convertToDataCell, cellPredicateFunction, RangeExtractorUtil::all);
    }

    /**
     * {@inheritDoc}
     * @param parameters keeps the parameters chosen by the user in the interface.
     */
    @Override
    public RowPredicateFactory apply(final OperatorParameters parameters) {
        CheckUtils.checkNotNull(parameters, "The operator parameters must not be null.");
        final String[] parameterValues = parameters.getValues();
        final DataCell parameter1 = m_convertToDataCell.apply(parameterValues[0]);
        final DataCell parameter2 = m_convertToDataCell.apply(parameterValues[1]);
        return new DefaultRowPredicateFactory(m_cellPredicateFunction.apply(parameter1, parameter2),
            parameters.getColumnSpec(), m_rangeFunction.apply(parameters.getColumnSpec(), parameterValues));
    }

}
