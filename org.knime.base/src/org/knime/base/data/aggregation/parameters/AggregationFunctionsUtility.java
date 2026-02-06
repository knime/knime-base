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
package org.knime.base.data.aggregation.parameters;

import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

import org.knime.base.data.aggregation.AggregationOperatorParameters;
import org.knime.core.data.DataType;
import org.knime.core.node.port.database.aggregation.AggregationFunction;

/**
 * Utility to bundle aggregation function provider (e.g. from native methods or DB methods) and its functions' optional
 * parameters classes as well as serve an {@link Optional}-based lookup API.
 *
 * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
 *
 * @param <F> the type of aggregation function provided by this utility
 *
 * @since 5.11
 */
public abstract class AggregationFunctionsUtility<F extends AggregationFunction> {

    /**
     * Gets the aggregation function by its ID.
     *
     * @param id the ID of the aggregation function
     * @return the aggregation function
     *
     * @throws IllegalArgumentException if no such function exists
     */
    protected abstract F getFunction(final String id);

    /**
     * Looks up an aggregation method by its ID.
     *
     * If you need an {@link AggregationSpec}, do:
     *
     * <pre><code>
     * util -> util.lookupFunctionById(id).map(util::mapToSpec)
     * </code></pre>
     *
     * @param id the ID of the aggregation method
     * @return the aggregation method, or {@link Optional#empty()} if no such method exists
     */
    public final Optional<F> lookupFunctionById(final String id) {
        return lookupFunctionById(this::getFunction, id);
    }

    /**
     * Looks up an aggregation function by its ID using the given lookup function.
     *
     * @param <R> the type of aggregation function
     * @param fnLookup the function to look up the aggregation function by its ID, which is allowed to throw an
     *            {@link IllegalArgumentException} in case the ID has no function associated with it
     * @param id the ID of the aggregation function
     *
     * @return the aggregation function, or {@link Optional#empty()} if no such function exists
     */
    protected static <R extends AggregationFunction> Optional<R> lookupFunctionById(final Function<String, R> fnLookup,
        final String id) {
        try {
            return Optional.ofNullable(fnLookup.apply(id));
        } catch (final IllegalArgumentException e) { // NOSONAR we map this exception to empty optional
            // some provider implementations throw IAE if the id is not found, but we want to work with an optional
            return Optional.empty();
        }
    }

    /**
     * Maps the aggregation function to an {@link AggregationSpec}.
     *
     * @param method the optional aggregation function
     * @return the optional aggregation spec
     */
    public final AggregationSpec mapToSpec(final F method) {
        return new AggregationSpec(method.getId(), method.getLabel(), method.hasOptionalSettings());
    }

    /**
     * Returns all aggregation functions that are compatible with the given data type.
     *
     * @param type the data type
     * @param sorted whether to sort the functions by user-facing label
     * @return a stream of compatible aggregation functions
     */
    public abstract Stream<F> getCompatibleAggregationFunctions(DataType type, boolean sorted);

    /**
     * Returns all aggregation functions that are compatible with the given data type.
     *
     * @param type the data type
     * @param sorted whether to sort the functions by user-facing label
     * @return a stream of compatible aggregation functions
     */
    public final Stream<AggregationSpec> getCompatibleFunctions(final DataType type, final boolean sorted) {
        return getCompatibleAggregationFunctions(type, sorted) //
            .map(am -> new AggregationSpec(am.getId(), am.getLabel(), am.hasOptionalSettings()));
    }

    /**
     * Returns all aggregation functions.
     *
     * @param sorted whether to sort the functions by user-facing label
     * @return a stream of aggregation functions
     */
    protected abstract Stream<F> getFunctions(boolean sorted);

    /**
     * Gets the default aggregation function for the given data type.
     *
     * @param type the data type
     * @return the default aggregation function
     */
    public abstract Optional<F> getDefaultFunction(DataType type);

    /**
     * Looks up the parameters class supporting the given aggregation function.
     *
     * @param fun the aggregation function definition
     * @return if available, the parameters class for the given function
     */
    public abstract Optional<Class<? extends AggregationOperatorParameters>>
        lookupParametersForFunction(final AggregationSpec fun);
}
