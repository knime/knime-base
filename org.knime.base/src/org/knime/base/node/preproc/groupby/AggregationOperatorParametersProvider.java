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
package org.knime.base.node.preproc.groupby;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import org.knime.base.data.aggregation.AggregationMethods;
import org.knime.base.data.aggregation.AggregationOperatorParameters;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.webui.node.dialog.FallbackDialogNodeParameters;
import org.knime.core.webui.node.dialog.defaultdialog.NodeParametersUtil;
import org.knime.core.webui.node.dialog.defaultdialog.internal.dynamic.ClassIdStrategy;
import org.knime.core.webui.node.dialog.defaultdialog.internal.dynamic.DefaultClassIdStrategy;
import org.knime.core.webui.node.dialog.defaultdialog.internal.dynamic.DynamicParameters;
import org.knime.core.webui.node.dialog.defaultdialog.util.updates.StateComputationFailureException;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.updates.ParameterReference;

/**
 * Provider for aggregation operator parameters (aka optional parameters), which depend on the selected aggregation
 * method and currently present operator parameters.
 *
 * In case no default dialog is registered via the extension point, the fallback dialog is shown.
 *
 * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
 */
@SuppressWarnings("restriction")
abstract class AggregationOperatorParametersProvider
    implements DynamicParameters.DynamicParametersWithFallbackProvider<AggregationOperatorParameters> {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(AggregationOperatorParametersProvider.class);

    private Supplier<AggregationOperatorParameters> m_optionalParametersSupplier;

    private Supplier<String> m_aggregationMethodSupplier;

    abstract Class<? extends ParameterReference<AggregationOperatorParameters>> getParameterRefClass();

    abstract Class<? extends AggregationMethodRef> getMethodParameterRefClass();

    interface AggregationMethodRef extends ParameterReference<String> {
    } //

    @Override
    public final void init(final StateProviderInitializer initializer) {
        initializer.computeBeforeOpenDialog();
        m_optionalParametersSupplier = initializer.getValueSupplier(getParameterRefClass());
        m_aggregationMethodSupplier = initializer.computeFromValueSupplier(getMethodParameterRefClass());
    }

    @Override
    public final ClassIdStrategy<AggregationOperatorParameters> getClassIdStrategy() {
        final List<Class<? extends AggregationOperatorParameters>> allClasses = new ArrayList<>();
        allClasses.add(LegacyAggregationOperatorParameters.class);
        allClasses.addAll(AggregationMethods.getAllParameterClasses());
        return new DefaultClassIdStrategy<>(allClasses);
    }

    @Override
    public final AggregationOperatorParameters computeParameters(final NodeParametersInput parametersInput)
        throws StateComputationFailureException {
        final var currentMethod = m_aggregationMethodSupplier.get();
        if (currentMethod == null) {
            // no method selected yet, abort update
            throw new StateComputationFailureException();
        }
        final var method = AggregationMethods.getMethod4Id(currentMethod);
        if (method == null) {
            LOGGER.warn("Unknown aggregation method: " + currentMethod);
            throw new StateComputationFailureException();
        }
        if (!method.hasOptionalSettings()) {
            throw new StateComputationFailureException();
        }

        final var currentValue = m_optionalParametersSupplier.get();

        final var paramClass = AggregationMethods.getInstance().getParametersClassFor(method.getId()).orElse(null);
        if (paramClass != null && currentValue != null && paramClass.isInstance(currentValue)) {
            return currentValue;
        } else if (paramClass != null) {
            try {
                return NodeParametersUtil.createSettings(paramClass, parametersInput);
            } catch (final Exception e) { // NOSONAR we want to be safe and rather fall back to the fallback dialog
                LOGGER.warn(() -> String.format(
                    "Failed to instantiate parameter class \"%s\", falling back to legacy parameters",
                    paramClass.getName()), e);
            }
        }

        if (currentValue instanceof LegacyAggregationOperatorParameters legacy) {
            final var paramSettings = legacy.getNodeSettings();
            try {
                method.validateSettings(paramSettings);
                method.loadValidatedSettings(paramSettings);
                return new LegacyAggregationOperatorParameters(paramSettings);
            } catch (final InvalidSettingsException e) { // NOSONAR best-effort
                // fall-through: cannot re-use settings
            }
        }

        final var settings = new NodeSettings("extracted model settings");
        method.saveSettingsTo(settings);
        return new LegacyAggregationOperatorParameters(settings);
    }

    @Override
    public final NodeSettings computeFallbackSettings(final NodeParametersInput parametersInput)
        throws StateComputationFailureException {
        final var params = computeParameters(parametersInput);
        if (params instanceof LegacyAggregationOperatorParameters legacy) {
            return legacy.getNodeSettings();
        }
        // no fallback "dialog" needed (no operator parameters or new parameters based)
        return null;
    }

    @Override
    public final FallbackDialogNodeParameters getParametersFromFallback(final NodeSettingsRO fallbackSettings) {
        return new LegacyAggregationOperatorParameters(fallbackSettings);
    }
}
