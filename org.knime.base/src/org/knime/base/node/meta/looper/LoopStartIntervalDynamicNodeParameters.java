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
 * ------------------------------------------------------------------------
 */

package org.knime.base.node.meta.looper;

import java.util.function.Supplier;

import org.knime.base.node.meta.looper.LoopStartIntervalDynamicNodeModel.OutputType;
import org.knime.core.webui.node.dialog.defaultdialog.util.updates.StateComputationFailureException;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.migration.LoadDefaultsForAbsentFields;
import org.knime.node.parameters.persistence.Persist;
import org.knime.node.parameters.updates.ParameterReference;
import org.knime.node.parameters.updates.StateProvider;
import org.knime.node.parameters.updates.ValueReference;
import org.knime.node.parameters.widget.choices.ValueSwitchWidget;
import org.knime.node.parameters.widget.number.NumberInputWidget;
import org.knime.node.parameters.widget.number.NumberInputWidgetValidation.MaxValidation;
import org.knime.node.parameters.widget.number.NumberInputWidgetValidation.MinValidation;

/**
 * Node parameters for Interval Loop Start.
 *
 * @author Magnus Gohm, KNIME GmbH, Konstanz, Germany
 * @author AI Migration Pipeline v1.2
 */
@LoadDefaultsForAbsentFields
@SuppressWarnings("restriction")
final class LoopStartIntervalDynamicNodeParameters implements NodeParameters {

    @Persist(configKey = LoopStartIntervalDynamicSettings.CFG_FROM)
    @Widget(title = "From", description = "The interval start value (inclusive).")
    @ValueReference(FromRef.class)
    double m_from;

    static final class FromRef implements ParameterReference<Double> {
    }

    @Persist(configKey = LoopStartIntervalDynamicSettings.CFG_TO)
    @Widget(title = "To", description = "The interval end value (inclusive).")
    @NumberInputWidget(minValidationProvider = ToMinimumValidationProvider.class,
        maxValidationProvider = ToMaximumValidationProvider.class)
    double m_to = 1.0;

    static final class ToMinimumValidationProvider implements StateProvider<MinValidation> {

        private Supplier<Double> m_fromSupplier;

        private Supplier<Double> m_stepSizeSupplier;

        @Override
        public void init(final StateProviderInitializer initializer) {
            initializer.computeBeforeOpenDialog();
            m_fromSupplier = initializer.computeFromValueSupplier(FromRef.class);
            m_stepSizeSupplier = initializer.computeFromValueSupplier(StepSizeRef.class);
        }

        @Override
        public MinValidation computeState(final NodeParametersInput parametersInput)
            throws StateComputationFailureException {
            if (m_fromSupplier.get() == null || m_stepSizeSupplier.get() == null) {
                throw new StateComputationFailureException();
            }
            if (m_stepSizeSupplier.get() > 0) {
                return new MinValidation() {

                    @Override
                    protected double getMin() {
                        return m_fromSupplier.get();
                    }

                    @Override
                    public String getErrorMessage() {
                        return "To must be bigger than from if step size is positive.";
                    }

                };
            }
            return null;
        }

    }

    static final class ToMaximumValidationProvider implements StateProvider<MaxValidation> {

        private Supplier<Double> m_fromSupplier;

        private Supplier<Double> m_stepSizeSupplier;

        @Override
        public void init(final StateProviderInitializer initializer) {
            initializer.computeBeforeOpenDialog();
            m_fromSupplier = initializer.computeFromValueSupplier(FromRef.class);
            m_stepSizeSupplier = initializer.computeFromValueSupplier(StepSizeRef.class);
        }

        @Override
        public MaxValidation computeState(final NodeParametersInput parametersInput)
            throws StateComputationFailureException {
            if (m_fromSupplier.get() == null || m_stepSizeSupplier.get() == null) {
                throw new StateComputationFailureException();
            }
            if (m_stepSizeSupplier.get() < 0) {
                return new MaxValidation() {

                    @Override
                    protected double getMax() {
                        return m_fromSupplier.get();
                    }

                    @Override
                    public String getErrorMessage() {
                        return "To must be smaller than from if step size is negative.";
                    }

                };
            }
            return null;
        }

    }

    @Persist(configKey = LoopStartIntervalDynamicSettings.CFG_STEP)
    @Widget(title = "Step", description = """
            The step size by which the value is increased after each iteration. Negative step sizes are
            possible, if <i>from</i> is greater than <i>to</i>.
            """)
    @ValueReference(StepSizeRef.class)
    double m_step = 0.01;

    static final class StepSizeRef implements ParameterReference<Double> {
    }

    @Persist(configKey = LoopStartIntervalDynamicSettings.CFG_VARIABLE_TYPE)
    @Widget(title = "Loop variable type", description = """
            Select the type of the variable that is exposed by the node (integers, longs or doubles). The
            node creates variables for <i>from</i>, <i>to</i>, <i>step</i> and the current <i>value</i>. Keep in mind
            that this only affects the type of the variables. Internally doubles are used to keep track of the
            loop state. This means that the values may be rounded to fit the specified type.
            """)
    @ValueSwitchWidget
    OutputType m_variableType = OutputType.DOUBLE;

    @Widget(title = "Variable prefix", description = "The prefix of the variable names.")
    @Persist(configKey = LoopStartIntervalDynamicSettings.CFG_PREFIX)
    String m_prefix = "loop_";

}
