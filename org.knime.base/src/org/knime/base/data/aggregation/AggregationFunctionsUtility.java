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
 *   15 Dec 2025 (Manuel Hotz, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.base.data.aggregation;

import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

import org.knime.core.data.DataType;
import org.knime.core.node.port.database.aggregation.AggregationFunction;
import org.knime.core.node.port.database.aggregation.AggregationFunctionProvider;

/**
 * Utility to bundle aggregation function provider (e.g. from native methods or DB methods) and its functions' optional
 * parameters classes as well as serve an {@link Optional}-based lookup API.
 *
 * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
 *
 * @param <F> the type of aggregation function provided by this utility
 *
 * @since 5.10
 */
public final class AggregationFunctionsUtility<F extends AggregationFunction> {

    private final AggregationFunctionProvider<F> m_functionProvider;

    private Function<AggregationSpec, Optional<Class<? extends AggregationOperatorParameters>>> m_paramClassSupplier;

    /**
     * Constructor with function and parameters class providers
     *
     * @param functionProvider provider for aggregation functions
     * @param paramClassProvider parameters class provider which maps an aggregation function definition to the concrete
     *            parameters class
     */
    public AggregationFunctionsUtility(final AggregationFunctionProvider<F> functionProvider,
        final Function<AggregationSpec, Optional<Class<? extends AggregationOperatorParameters>>> paramClassProvider) {
        m_functionProvider = functionProvider;
        m_paramClassSupplier = paramClassProvider;
    }

    /**
     * Looks up an aggregation method by its ID.
     *
     * @param id the ID of the aggregation method
     * @return the aggregation method, or {@link Optional#empty()} if no such method exists
     */
    public Optional<F> lookupById(final String id) {
        try {
            return Optional.of(m_functionProvider.getFunction(id));
        } catch (final IllegalArgumentException e) { // NOSONAR we map this exception to empty optional
            // some provider implementations throw IAE if the id is not found, but we want to work with an optional
            return Optional.empty();
        }
    }

    /**
     * Looks up an aggregation method by its ID and returns it as an {@link AggregationSpec}.
     *
     * @param id the ID of the aggregation method
     * @return the aggregation function, or {@link Optional#empty()} if no such method exists
     */
    public Optional<AggregationSpec> lookupFunctionById(final String id) {
        try {
            return lookupById(id) //
                    .map(method -> new AggregationSpec(id, method.getLabel(), method.hasOptionalSettings()));
        } catch (final IllegalArgumentException e) { // NOSONAR we map this exception to empty optional
            // IAE by some provider implementations
            return Optional.empty();
        }
    }

    /**
     * Returns all aggregation functions that are compatible with the given data type.
     *
     * @param type the data type
     * @return a stream of compatible aggregation functions
     */
    public Stream<AggregationSpec> getCompatibleFunctions(final DataType type) {
        return m_functionProvider.getCompatibleFunctions(type, true) //
                .stream() //
                .map(am -> new AggregationSpec(am.getId(), am.getLabel(), am.hasOptionalSettings()));
    }

    /**
     * Looks up the parameters class supporting the given aggregation function.
     *
     * @param fun the aggregation function definition
     * @return if available, the parameters class for the given function
     */
    public Optional<Class<? extends AggregationOperatorParameters>>
            lookupParametersForFunction(final AggregationSpec fun) {
        return m_paramClassSupplier.apply(fun);
    }

}
