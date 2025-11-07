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

package org.knime.base.node.mine.transformation.pca.apply;

import java.text.DecimalFormat;
import java.util.Optional;
import java.util.function.Supplier;

import org.knime.base.node.mine.transformation.pca.settings.PCAApplySettings;
import org.knime.base.node.mine.transformation.port.TransformationPortObjectSpec;
import org.knime.base.node.mine.transformation.settings.TransformationApplySettings;
import org.knime.base.node.mine.transformation.settings.TransformationReverseSettings;
import org.knime.base.node.mine.transformation.util.TransformationUtils;
import org.knime.core.webui.node.dialog.defaultdialog.util.updates.StateComputationFailureException;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.migration.LoadDefaultsForAbsentFields;
import org.knime.node.parameters.persistence.Persist;
import org.knime.node.parameters.persistence.Persistor;
import org.knime.node.parameters.persistence.legacy.EnumBooleanPersistor;
import org.knime.node.parameters.updates.Effect;
import org.knime.node.parameters.updates.Effect.EffectType;
import org.knime.node.parameters.updates.EffectPredicate;
import org.knime.node.parameters.updates.EffectPredicateProvider;
import org.knime.node.parameters.updates.ParameterReference;
import org.knime.node.parameters.updates.StateProvider;
import org.knime.node.parameters.updates.ValueReference;
import org.knime.node.parameters.widget.choices.Label;
import org.knime.node.parameters.widget.choices.RadioButtonsWidget;
import org.knime.node.parameters.widget.message.TextMessage;
import org.knime.node.parameters.widget.message.TextMessage.Message;
import org.knime.node.parameters.widget.number.NumberInputWidget;
import org.knime.node.parameters.widget.number.NumberInputWidgetValidation.MaxValidation;
import org.knime.node.parameters.widget.number.NumberInputWidgetValidation.MinValidation.IsPositiveDoubleValidation;
import org.knime.node.parameters.widget.number.NumberInputWidgetValidation.MinValidation.IsPositiveIntegerValidation;

/**
 * Node parameters for PCA Apply.
 *
 * @author Magnus Gohm, KNIME GmbH, Konstanz, Germany
 * @author AI Migration Pipeline v1.2
 */
@LoadDefaultsForAbsentFields
@SuppressWarnings("restriction")
class PCA2ApplyNodeParameters implements NodeParameters {

    @Persistor(value = DimensionSelectionPersitor.class)
    @Widget(title = "Target dimensions",
        description = "Determines the number of dimensions the input data is projected to. "
            + "The number of target dimensions can either be selected directly or by specifying "
            + "the minimal amount of information to be preserved.")
    @RadioButtonsWidget
    @ValueReference(value = DimensionSelectionMethodRef.class)
    DimensionSelectionMethod m_dimensionSelectionMethod = DimensionSelectionMethod.FIXED_DIMENSIONS;

    @Persist(configKey = TransformationApplySettings.K_CFG)
    @Widget(title = "Number of dimensions", description = """
            The number of dimensions to reduce the data to. Must be lower than or equal to the number of dimensions in
            the transformation model. If the PCA Compute node is connected and executed, the possible choices for
            number of dimensions are directly mapped to the percentage of information preserved.
            """)
    @NumberInputWidget(minValidation = IsPositiveIntegerValidation.class,
        maxValidationProvider = MaxDimensionsFromModelValidation.class)
    @Effect(predicate = NumberOfDimensionsSelected.class, type = EffectType.SHOW)
    @ValueReference(value = TargetDimensionRef.class)
    int m_numberOfDimensions = 1;

    @Persist(configKey = PCAApplySettings.INFORMATION_PRESERVATION_CFG)
    @Widget(title = "Information preservation (%)", description = """
            The minimum percentage of information to preserve in the reduced dimensions. If the PCA Compute node is
            connected and executed, the possible choices for information preservation are directly mapped to the number
            of output dimensions.
            """)
    @NumberInputWidget(minValidation = IsPositiveDoubleValidation.class,
        maxValidation = IsLessThanOrEqual100Percent.class)
    @Effect(predicate = MinimumInformationPreservationSelected.class, type = EffectType.SHOW)
    @ValueReference(value = InformationPreservationRef.class)
    double m_informationPreservation = 100.0;

    @TextMessage(DimensionSelectionMessage.class)
    Void m_dimensionSelectionMessage;

    @Widget(title = "Remove original data columns",
        description = "If checked, the columns containing the input data are removed.")
    @Persist(configKey = TransformationReverseSettings.REMOVE_USED_COLS_CFG)
    boolean m_removeOriginalColumns;

    @Widget(title = "Fail if missing values are encountered", description = """
            If checked, execution fails when the selected columns contain missing values. By default, rows with missing
            values are ignored and excluded from the computation.
            """)
    @Persist(configKey = TransformationApplySettings.FAIL_ON_MISSING_CFG)
    boolean m_failOnMissings;

    static final class DimensionSelectionMethodRef implements ParameterReference<DimensionSelectionMethod> {
    }

    static final class TargetDimensionRef implements ParameterReference<Integer> {
    }

    static final class InformationPreservationRef implements ParameterReference<Double> {
    }

    static class NumberOfDimensionsSelected implements EffectPredicateProvider {

        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getEnum(DimensionSelectionMethodRef.class).isOneOf(DimensionSelectionMethod.FIXED_DIMENSIONS);
        }

    }

    static class MinimumInformationPreservationSelected implements EffectPredicateProvider {

        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getEnum(DimensionSelectionMethodRef.class)
                .isOneOf(DimensionSelectionMethod.INFORMATION_PRESERVATION);
        }

    }

    static final class IsLessThanOrEqual100Percent extends MaxValidation {

        @Override
        public double getMax() {
            return 100;
        }

    }

    static final class MaxDimensionsFromModelValidation implements StateProvider<MaxValidation> {

        @Override
        public void init(final StateProviderInitializer i) {
            i.computeBeforeOpenDialog();
        }

        @Override
        public MaxValidation computeState(final NodeParametersInput context) {
            final var trafoModelSpecOpt = context.getInPortSpec(0);

            if (trafoModelSpecOpt.isPresent()) {
                final var trafoModelSpec = (TransformationPortObjectSpec)trafoModelSpecOpt.get();
                final var maxDimensionsToReduceTo = trafoModelSpec.getMaxDimToReduceTo();

                return new MaxValidation() {

                    @Override
                    protected double getMax() {
                        return maxDimensionsToReduceTo;
                    }

                    @Override
                    public String getErrorMessage() {
                        if (maxDimensionsToReduceTo == 0) {
                            return "No dimensions available in the transformation model.";
                        }
                        if (maxDimensionsToReduceTo == 1) {
                            return "Only 1 dimension available in the transformation model.";
                        }
                        return String.format("Only %d dimensions available in the transformation model.",
                            maxDimensionsToReduceTo);
                    }

                };

            }

            return new MaxValidation() {

                @Override
                protected double getMax() {
                    return Integer.MAX_VALUE;
                }

            };

        }

    }

    static final class DimensionSelectionMessage implements StateProvider<Optional<TextMessage.Message>> {

        Supplier<DimensionSelectionMethod> m_dimensionSelectionMethodSupplier;

        Supplier<Integer> m_targetDimensionSupplier;

        Supplier<Double> m_informationPreservationSupplier;

        @Override
        public void init(final StateProviderInitializer initializer) {
            initializer.computeBeforeOpenDialog();
            initializer.computeOnValueChange(TargetDimensionRef.class);
            initializer.computeOnValueChange(InformationPreservationRef.class);
            initializer.computeOnValueChange(DimensionSelectionMethodRef.class);
            m_targetDimensionSupplier = initializer.getValueSupplier(TargetDimensionRef.class);
            m_informationPreservationSupplier = initializer.getValueSupplier(InformationPreservationRef.class);
            m_dimensionSelectionMethodSupplier =
                initializer.getValueSupplier(DimensionSelectionMethodRef.class);
        }

        @Override
        public Optional<Message> computeState(final NodeParametersInput parametersInput)
            throws StateComputationFailureException {
            final var dimensionSelectionMethod = m_dimensionSelectionMethodSupplier.get();
            final var targetDimension = m_targetDimensionSupplier.get();
            final var informationPreservation = m_informationPreservationSupplier.get();

            if (targetDimension == null || informationPreservation == null) {
                return Optional.empty();
            }

            final var trafoModelSpecOpt = parametersInput. getInPortSpec(0);
            if (trafoModelSpecOpt.isEmpty()) {
                return Optional.empty();
            }
            final var trafoModelSpec = (TransformationPortObjectSpec)trafoModelSpecOpt.get();

            final var eigenValuesOpt = trafoModelSpec.getEigenValues();
            if (eigenValuesOpt.isEmpty()) {
                return Optional.empty();
            }
            final var eigenValues = eigenValuesOpt.get();

            if (dimensionSelectionMethod == DimensionSelectionMethod.FIXED_DIMENSIONS) {
                double norm = 0;
                double sum = 0;
                for (int i = 0; i < trafoModelSpec.getMaxDimToReduceTo(); i++) {
                    norm += eigenValues.getEntry(i);
                    if (i < targetDimension) {
                        sum = norm;
                    }
                }

                return Optional.of(new TextMessage.Message(new DecimalFormat(".0").format(100d * sum / norm)
                    + "% information preservation", "", TextMessage.MessageType.INFO));
            } else {
                final int tgtDim = TransformationUtils.calcDimForGivenInfPreservation(eigenValues,
                    trafoModelSpec.getMaxDimToReduceTo(),
                    informationPreservation);

                return Optional.of(new TextMessage.Message(tgtDim + (tgtDim == 1 ? " dimension " : " dimensions ")
                    + "in output", "", TextMessage.MessageType.INFO));
            }

        }

    }

    static final class DimensionSelectionPersitor extends EnumBooleanPersistor<DimensionSelectionMethod> {

        protected DimensionSelectionPersitor() {
            super(PCAApplySettings.DIMENSION_SELECTED_CFG,
                DimensionSelectionMethod.class,
                DimensionSelectionMethod.FIXED_DIMENSIONS);
        }

    }

    enum DimensionSelectionMethod {

            @Label(value = "Dimension(s) to reduce to", description = """
                    Select a fixed number of target dimensions. It must be lower than or equal to the number of
                    dimensions in the transformation model.
                    """)
            FIXED_DIMENSIONS, //

            @Label(value = "Minimum information fraction", description = """
                    Specify the minimal amount of information to be preserved.
                    """)
            INFORMATION_PRESERVATION

    }

}
