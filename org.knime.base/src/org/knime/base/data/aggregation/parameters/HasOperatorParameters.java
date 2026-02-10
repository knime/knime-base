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
 *   20 Oct 2025 (Manuel Hotz, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.base.data.aggregation.parameters;

import java.util.Optional;
import java.util.function.Supplier;

import org.knime.base.data.aggregation.parameters.AggregationFunctionParametersProvider.AggregationMethodRef;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.webui.node.dialog.defaultdialog.util.updates.StateComputationFailureException;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.updates.StateProvider;

/**
 * Indicator that the selected aggregation method has optional settings.
 *
 * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
 *
 * @since 5.11
 */
@SuppressWarnings("restriction")
public abstract class HasOperatorParameters implements StateProvider<Boolean> {

    private Supplier<String> m_agg;

    /**
     * Gets the class of the {@link AggregationMethodRef} to use.
     *
     * @return the class of the {@link AggregationMethodRef} to use
     */
    protected abstract Class<? extends AggregationMethodRef> getAggregationMethodRefClass();

    /**
     * Looks up an aggregation function by its ID to determine if it has optional parameters.
     *
     * @param spec the {@code null}-able input spec to derive available functions from
     * @param id the non-{@code null} ID of the aggregation function
     *
     * @return the aggregation function, or {@link Optional#empty()} if no such function exists
     */
    protected abstract Optional<AggregationSpec> lookupFunctionById(PortObjectSpec spec, String id);

    @Override
    public final void init(final StateProviderInitializer init) {
        init.computeBeforeOpenDialog();
        m_agg = init.computeFromValueSupplier(getAggregationMethodRefClass());
    }

    @Override
    public final Boolean computeState(final NodeParametersInput in) throws StateComputationFailureException {
        final var id = m_agg.get();
        if (id == null) {
            throw new StateComputationFailureException();
        }
        // unknown aggregation function has no optional settings
        // we pass the spec even if it is null to give the implementation a chance to
        // provide a default even if no spec is connected
        final var spec = in.getInPortSpec(0).orElse(null);
        return lookupFunctionById(spec, id) //
            .map(AggregationSpec::hasOptionalSettings) //
            .orElse(false);
    }

}
