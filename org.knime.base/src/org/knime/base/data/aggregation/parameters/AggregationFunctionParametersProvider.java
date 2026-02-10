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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

import org.knime.base.data.aggregation.AggregationOperatorParameters;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.port.database.aggregation.AggregationFunction;
import org.knime.core.webui.node.dialog.FallbackDialogNodeParameters;
import org.knime.core.webui.node.dialog.defaultdialog.NodeParametersUtil;
import org.knime.core.webui.node.dialog.defaultdialog.internal.dynamic.ClassIdStrategy;
import org.knime.core.webui.node.dialog.defaultdialog.internal.dynamic.DefaultClassIdStrategy;
import org.knime.core.webui.node.dialog.defaultdialog.internal.dynamic.DynamicParameters;
import org.knime.core.webui.node.dialog.defaultdialog.util.updates.StateComputationFailureException;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.updates.ParameterReference;

/**
 * Provider for aggregation operator parameters (aka optional parameters), which depend on the
 * selected aggregation method and currently present operator parameters.
 *
 * In case no default dialog is registered via the extension point, a fallback dialog is shown.
 *
 * @param <F> type of aggregation function returned by the parameters implementation
 *
 * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
 *
 * @since 5.11
 */
@SuppressWarnings({"restriction"})
public abstract class AggregationFunctionParametersProvider<F extends AggregationFunction>
    implements DynamicParameters.DynamicParametersWithFallbackProvider<AggregationOperatorParameters> {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(AggregationFunctionParametersProvider.class);

    private Supplier<? extends AggregationOperatorParameters> m_optionalParametersSupplier;

    private Supplier<String> m_aggregationMethodSupplier;

    /** (Legacy) key for the optional function settings. */
    /* NB: All optional settings except manual native (i.e. non-DB) operator settings are stored under this key.
       The manual native settings use "aggregationOperatorSettings",
       which is handled by `LegacyColumnAggregatorsPersistor`.
    */
    protected static final String CFG_FUNCTION_SETTINGS = "functionSettings";

    /**
     * Gets the function utility to use for looking up aggregation functions and parameter classes.
     *
     * @param parametersInput node parameters input
     * @return utility for aggregation functions and parameter classes
     */
    protected abstract AggregationFunctionsUtility<F> getFunctionUtility(final NodeParametersInput parametersInput);

    /**
     * Gets the reference to use for optional aggregation parameters.
     *
     * @return the reference for optional aggregation parameters
     */
    protected abstract Class<? extends ParameterReference<? extends AggregationOperatorParameters>> // NOSONAR needed since we don't know the concrete type
        getParameterRefClass();

    /**
     * Gets the reference to use for the selected aggregation method.
     *
     * @return the reference for the selected aggregation method
     */
    protected abstract Class<? extends AggregationMethodRef> getMethodParameterRefClass();

    /**
     * Gets all aggregation function parameter classes (without the "fallback" class).
     *
     * @return all aggregation function parameter classes
     */
    protected abstract Collection<Class<? extends AggregationOperatorParameters>> getAllParameterClasses();

    /**
     * Marker type for aggregation method references.
     */
    public interface AggregationMethodRef extends ParameterReference<String> {
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
        allClasses.add(FallbackAggregationOperatorParameters.class);
        allClasses.addAll(getAllParameterClasses());
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
        final var functions = getFunctionUtility(parametersInput);
        final var methodOpt = functions.lookupFunctionById(currentMethod).map(functions::mapToSpec);
        if (methodOpt.isEmpty()) {
            LOGGER.warn("Unknown aggregation method: " + currentMethod);
            throw new StateComputationFailureException();
        }
        final var method = methodOpt.get();
        if (!method.hasOptionalSettings()) {
            throw new StateComputationFailureException();
        }

        final var currentValue = m_optionalParametersSupplier.get();

        final var paramClass = functions.lookupParametersForFunction(method) //
                .orElse(null);
        if (paramClass != null && currentValue != null && paramClass.isInstance(currentValue)) {
            return currentValue;
        } else if (paramClass != null) {
            // e.g. DB functions are loaded as fallback first, since the DB session is not available at settings
            // load time, but now we know a potential parameter class
            if (currentValue instanceof FallbackAggregationOperatorParameters fallback) {
                try {
                    return NodeParametersUtil.loadSettings(fallback.getNodeSettings(), paramClass);
                } catch (final InvalidSettingsException e) { // NOSONAR best-effort
                    // fall-through: cannot re-use loaded fallback settings as fancy params
                }
            }
            try {
                return NodeParametersUtil.createSettings(paramClass, parametersInput);
            } catch (final Exception e) { // NOSONAR we want to be safe and rather fall back to the fallback dialog
                LOGGER.warn(() -> String.format(
                    "Failed to instantiate parameter class \"%s\", falling back to legacy parameters",
                    paramClass.getName()), e);
            }
        }

        return createFallbackParameters(functions, currentMethod, currentValue);
    }

    /**
     * Creates legacy parameters in case no parameter class is registered for the selected aggregation method.
     *
     * @param functions the aggregation function utility
     * @param functionId the ID of the selected aggregation function
     * @param currentValue the currently present, {@code null}able aggregation function parameters, if any
     * @return the created fallback parameters
     * @throws StateComputationFailureException if the function provider does not know a function for the given ID
     */
    private final AggregationOperatorParameters createFallbackParameters(
        final AggregationFunctionsUtility<F> functions,
        final String functionId, final AggregationOperatorParameters currentValue)
        throws StateComputationFailureException {
        final F method = functions.lookupFunctionById(functionId).orElseThrow(StateComputationFailureException::new);

        // try to re-use the settings from existing fallback parameters
        if (currentValue instanceof FallbackAggregationOperatorParameters fallbackParams) {
            final var paramSettings = fallbackParams.getNodeSettings();
            try {
                method.validateSettings(paramSettings);
                method.loadValidatedSettings(paramSettings);
                return fallbackParams;
            } catch (final InvalidSettingsException e) { // NOSONAR best-effort
                // fall-through: cannot re-use settings
            }
        }

        // cannot re-use existing settings, we need to create new ones based on the defaults from the method
        return FallbackAggregationOperatorParameters.withInitial(CFG_FUNCTION_SETTINGS, method::saveSettingsTo);
    }

    @Override
    public final NodeSettings computeFallbackSettings(final NodeParametersInput parametersInput)
            throws StateComputationFailureException {
        final var params = computeParameters(parametersInput);
        if (params instanceof FallbackAggregationOperatorParameters legacy) {
            return legacy.getNodeSettings();
        }
        // no fallback "dialog" needed (no operator parameters or new parameters based)
        return null;
    }

    @Override
    public final FallbackDialogNodeParameters getParametersFromFallback(final NodeSettingsRO fallbackSettings) {
        return new FallbackAggregationOperatorParameters(CFG_FUNCTION_SETTINGS, fallbackSettings);
    }
}
