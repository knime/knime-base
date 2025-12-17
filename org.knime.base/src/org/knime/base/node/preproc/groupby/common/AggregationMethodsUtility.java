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
package org.knime.base.node.preproc.groupby.common;

import java.util.Optional;
import java.util.stream.Stream;

import org.knime.base.data.aggregation.AggregationMethod;
import org.knime.base.data.aggregation.AggregationMethods;
import org.knime.base.data.aggregation.AggregationOperatorParameters;
import org.knime.base.data.aggregation.parameters.AggregationFunctionsUtility;
import org.knime.base.data.aggregation.parameters.AggregationSpec;
import org.knime.core.data.DataType;

/**
 * Utility for native (as in "not-DB") aggregation methods.
 *
 * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
 *
 * @since 5.10
 */
public final class AggregationMethodsUtility extends AggregationFunctionsUtility<AggregationMethod> {

    private static final AggregationMethodsUtility INSTANCE = new AggregationMethodsUtility();

    /**
     * Gets the singleton instance of this utility.
     *
     * @return the singleton instance
     */
    public static AggregationMethodsUtility getInstance() {
        return INSTANCE;
    }

    private AggregationMethodsUtility() {
        // singleton
    }

    @Override
    protected AggregationMethod getFunction(final String id) {
        return AggregationMethods.getMethod4Id(id);
    }

    @Override
    public Stream<AggregationMethod> getCompatibleAggregationFunctions(final DataType type, final boolean sorted) {
        return AggregationMethods.getCompatibleMethods(type, sorted).stream();
    }

    @Override
    protected Stream<AggregationMethod> getFunctions(final boolean sorted) {
        return AggregationMethods.getInstance().getFunctions(sorted).stream();
    }

    @Override
    public Optional<AggregationMethod> getDefaultFunction(final DataType type) {
        return Optional.ofNullable(AggregationMethods.getInstance().getDefaultFunction(type));
    }

    @Override
    public Optional<Class<? extends AggregationOperatorParameters>>
        lookupParametersForFunction(final AggregationSpec fun) {
        return AggregationMethods.getInstance().getParametersClassFor(fun.id());
    }

}
