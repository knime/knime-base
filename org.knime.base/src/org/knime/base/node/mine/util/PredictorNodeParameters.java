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
 *   Feb 24, 2026 (magnus): created
 */
package org.knime.base.node.mine.util;

import java.util.function.Supplier;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.node.port.pmml.PMMLPortObjectSpec;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.webui.node.dialog.defaultdialog.internal.dirty.DirtyTracker;
import org.knime.core.webui.node.dialog.defaultdialog.util.updates.StateComputationFailureException;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Modification;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.updates.ParameterReference;
import org.knime.node.parameters.updates.StateProvider;
import org.knime.node.parameters.widget.OptionalWidget.DefaultValueProvider;

/**
 * {@NodeParameters} for various predictor nodes.
 *
 * @author Magnus Gohm, KNIME GmbH, Konstanz, Germany
 * @since 5.11
 */
@SuppressWarnings("restriction")
public abstract class PredictorNodeParameters implements NodeParameters {

    @Modification.WidgetReference(DialogDirtyMakerModRef.class)
    Void m_makeDialogDirty;

    private interface DialogDirtyMakerModRef extends Modification.Reference {
    }

    /**
     * Adds the widget metadata for the {@code changePredictionColumnName} field.
     *
     * @param groupModifier the group modifier to add the widget metadata to
     * @param makeDialogDirtyProviderClass the state provider class to determine if the dialog is dirty
     */
    public static void addDirtyTracker(final Modification.WidgetGroupModifier groupModifier,
        final Class<? extends StateProvider<Boolean>> makeDialogDirtyProviderClass) {
        groupModifier.find(DialogDirtyMakerModRef.class) //
            .addAnnotation(DirtyTracker.class) //
            .withValue(makeDialogDirtyProviderClass) //
            .modify();
    }

    /**
     * Default provider for the prediction column name widget used in various predictor nodes.
     *
     * @author Magnus Gohm, KNIME GmbH, Konstanz, Germany
     */
    public abstract static class PredictionColumnNameDefaultProvider implements DefaultValueProvider<String> {

        private Class<? extends ParameterReference<String>> m_predictionColumnClass;

        private String m_predictionColumnDefault;

        /**
         * Constructor.
         *
         * @param predictionColumnClass The parameter reference class of the prediction column name.
         * @param predictionColumnDefault The default value for the prediction column name.
         */
        protected PredictionColumnNameDefaultProvider(
            final Class<? extends ParameterReference<String>> predictionColumnClass,
            final String predictionColumnDefault) {
            m_predictionColumnClass = predictionColumnClass;
            m_predictionColumnDefault = predictionColumnDefault;
        }

        private Supplier<String> m_currentValue;

        @Override
        public void init(final StateProviderInitializer i) {
            i.computeBeforeOpenDialog();
            m_currentValue = i.getValueSupplier(m_predictionColumnClass);
        }

        @Override
        public String computeState(final NodeParametersInput context) {
            final String current = m_currentValue.get();
            if (current != null && !current.isEmpty() && !m_predictionColumnDefault.equals(current)) {
                return current;
            }
            return getDefaultPredictionColumnNameFromContext(context);
        }

        private String getDefaultPredictionColumnNameFromContext(final NodeParametersInput context) {
            final var pmmlSpec = context.getInPortSpec(0);
            if (pmmlSpec.isPresent()) {
                final var targetCols = ((PMMLPortObjectSpec)pmmlSpec.get()).getTargetCols();
                if (!targetCols.isEmpty()) {
                    final DataColumnSpec classColumn = targetCols.get(0);
                    final PredictorHelper predictorHelper = PredictorHelper.getInstance();
                    return predictorHelper.computePredictionDefault(classColumn.getName());
                }
            }
            return m_predictionColumnDefault;
        }

    }

    /**
     * State provider to determine the enabled state of the legacy internal settings in the predictor nodes.
     *
     * @author Magnus Gohm, KNIME GmbH, Konstanz, Germany
     */
    public abstract static class EnabledStatusProvider implements StateProvider<Boolean> {

        private final Class<? extends ParameterReference<Boolean>> m_changePropertyRef;
        private final Class<? extends ParameterReference<Boolean>> m_enabledStatusRef;

        /**
         * Constructor.
         *
         * @param changePropertyRef the parameter reference for the change property (e.g. change prediction column name)
         * @param enabledStatusRef the parameter reference for the enabled status of the legacy internal setting
         * (e.g. prediction column name enabled)
         */
        protected EnabledStatusProvider(final Class<? extends ParameterReference<Boolean>> changePropertyRef,
            final Class<? extends ParameterReference<Boolean>> enabledStatusRef) {
            m_changePropertyRef = changePropertyRef;
            m_enabledStatusRef = enabledStatusRef;
        }

        private Supplier<Boolean> m_changePropertySupplier;
        private Supplier<Boolean> m_enabledStatusSupplier;

        @Override
        public void init(final StateProviderInitializer initializer) {
            initializer.computeBeforeOpenDialog();
            m_changePropertySupplier = initializer.getValueSupplier(m_changePropertyRef);
            m_enabledStatusSupplier = initializer.getValueSupplier(m_enabledStatusRef);
        }

        @Override
        public Boolean computeState(final NodeParametersInput parametersInput) throws StateComputationFailureException {
            if(m_enabledStatusSupplier.get() != m_changePropertySupplier.get()) {
                return m_changePropertySupplier.get();
            }
            throw new StateComputationFailureException();
        }

    }

    /**
     * State provider to determine whether the dialog should be marked dirty when opening it based on the enabled
     * state of the legacy internal settings in the predictor nodes.
     *
     * @author Magnus Gohm, KNIME GmbH, Konstanz, Germany
     */
    public abstract static class MakeDialogDirtyProvider implements StateProvider<Boolean> {

        private final Class<? extends ParameterReference<Boolean>> m_predictionColumnEnabledStatusRef;
        private final Class<? extends ParameterReference<Boolean>> m_probabilitySuffixEnabledStatusRef;

        /**
         * Constructor.
         *
         * @param predictionColumnEnabledStatusRef the parameter reference for the enabled status of the prediction
         *        column name legacy internal setting
         * @param probabilitySuffixEnabledStatusRef the parameter reference for the enabled status of the probability
         *        suffix legacy internal setting
         */
        protected MakeDialogDirtyProvider(
            final Class<? extends ParameterReference<Boolean>> predictionColumnEnabledStatusRef,
            final Class<? extends ParameterReference<Boolean>> probabilitySuffixEnabledStatusRef) {
            CheckUtils.checkArgument(
                predictionColumnEnabledStatusRef != null || probabilitySuffixEnabledStatusRef != null,
                "At least one of the provided parameter references must be non-null");
            m_predictionColumnEnabledStatusRef = predictionColumnEnabledStatusRef;
            m_probabilitySuffixEnabledStatusRef = probabilitySuffixEnabledStatusRef;
        }

        private Supplier<Boolean> m_predictionColumnNameEnabledStatus;
        private Supplier<Boolean> m_probabilitySuffixEnabledStatus;

        @Override
        public void init(final StateProviderInitializer initializer) {
            initializer.computeBeforeOpenDialog();
            if (m_predictionColumnEnabledStatusRef != null) {
                m_predictionColumnNameEnabledStatus = initializer.getValueSupplier(m_predictionColumnEnabledStatusRef);
            }
            if (m_probabilitySuffixEnabledStatusRef != null) {
                m_probabilitySuffixEnabledStatus = initializer.getValueSupplier(m_probabilitySuffixEnabledStatusRef);
            }
        }

        @Override
        public Boolean computeState(final NodeParametersInput parametersInput) throws StateComputationFailureException {
            if(m_predictionColumnEnabledStatusRef == null && m_probabilitySuffixEnabledStatusRef != null) {
                return m_probabilitySuffixEnabledStatus.get();
            }
            if(m_predictionColumnEnabledStatusRef != null && m_probabilitySuffixEnabledStatusRef == null) {
                return m_predictionColumnNameEnabledStatus.get();
            }
            return m_predictionColumnNameEnabledStatus.get() || m_probabilitySuffixEnabledStatus.get();
        }

    }

}
