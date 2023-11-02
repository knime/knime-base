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
 *   11 Jan 2023 (Manuel Hotz, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.base.node.preproc.rowagg.aggregation;

import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.knime.core.data.DataType;
import org.knime.core.data.DataValue;

/**
 * Stateless combiner for two data values which produces an optional data value as output.
 *
 * @param <T> first operand
 * @param <U> second operand
 * @param <R> result
 *
 * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
 * @noreference This interface is not intended to be referenced by clients.
 */
public interface Combiner<T extends DataValue, U extends DataValue, R extends DataValue>
    extends BiFunction<T, U, Optional<R>> {

    /**
     * Gets the result type of the combiner.
     *
     * @return type of result
     */
    DataType getResultDataType();

    /**
     * {@inheritDoc}
     *
     * @throws ArithmeticException if combining numeric values, can indicate if overflow occurred
     */
    @Override
    default Optional<R> apply(final T t, final U u) throws ArithmeticException {
        return Optional.empty();
    }

    /**
     * {@inheritDoc}
     *
     * @throws ArithmeticException if combining numeric values, can indicate if overflow occurred
     */
    @Override
    default <V> BiFunction<T, U, V> andThen(final Function<? super Optional<R>, ? extends V> after)
            throws ArithmeticException {
        return BiFunction.super.andThen(after);
    }
}
