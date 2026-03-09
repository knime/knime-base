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

package org.knime.base.node.preproc.correlation.filter;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import org.knime.base.node.preproc.correlation.pmcc.PMCCPortObjectAndSpec;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.NominalValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.webui.node.dialog.defaultdialog.util.updates.StateComputationFailureException;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.migration.LoadDefaultsForAbsentFields;
import org.knime.node.parameters.persistence.NodeParametersPersistor;
import org.knime.node.parameters.persistence.Persist;
import org.knime.node.parameters.persistence.Persistor;
import org.knime.node.parameters.updates.Effect;
import org.knime.node.parameters.updates.Effect.EffectType;
import org.knime.node.parameters.updates.EffectPredicate;
import org.knime.node.parameters.updates.EffectPredicateProvider;
import org.knime.node.parameters.updates.ParameterReference;
import org.knime.node.parameters.updates.StateProvider;
import org.knime.node.parameters.updates.ValueProvider;
import org.knime.node.parameters.updates.ValueReference;
import org.knime.node.parameters.widget.choices.ChoicesProvider;
import org.knime.node.parameters.widget.choices.StringChoicesProvider;
import org.knime.node.parameters.widget.choices.filter.TwinlistWidget;
import org.knime.node.parameters.widget.message.TextMessage;
import org.knime.node.parameters.widget.number.NumberInputWidget;
import org.knime.node.parameters.widget.number.NumberInputWidgetValidation.MaxValidation;
import org.knime.node.parameters.widget.number.NumberInputWidgetValidation.MinValidation.IsPositiveDoubleValidation;

/**
 * Node parameters for Correlation Filter.
 *
 * @author Magnus Gohm, KNIME GmbH, Konstanz, Germany
 * @author AI Migration Pipeline v1.2
 */
@SuppressWarnings("restriction")
@LoadDefaultsForAbsentFields
final class CorrelationFilterNodeParameters implements NodeParameters {

    @TextMessage(CorrelationModelInfoProvider.class)
    Void m_correlationModelInfo;

    @Persist(configKey = CorrelationFilterNodeModel.CFG_THRESHOLD)
    @Widget(title = "Correlation threshold", description = """
            Choose the correlation threshold. The higher the value, the fewer columns get filtered out.
            """)
    @NumberInputWidget(minValidation = IsPositiveDoubleValidation.class, maxValidation = IsAtMostOneValidation.class,
        stepSize = 0.01)
    @ValueReference(ThresholdRef.class)
    double m_threshold = 1.0;

    interface ThresholdRef extends ParameterReference<Double> {
    }

    static final class IsAtMostOneValidation extends MaxValidation {

        @Override
        protected double getMax() {
            return 1.0;
        }

    }

    @Persistor(DoNotPersistStringArray.class)
    @Widget(title = "Included columns", description = """
            Shows the columns which will be included or excluded in the output table. The list is updated based on the
            correlation model and the chosen correlation threshold. This list cannot be edited.
            """)
    @TwinlistWidget
    @ChoicesProvider(IncludedColumnsChoicesProvider.class)
    @ValueProvider(IncludedColumnsProvider.class)
    @ValueReference(IncludedColumnsRef.class)
    @Effect(predicate = AlwaysTrue.class, type = EffectType.DISABLE)
    String[] m_includedColumns = new String[0];

    interface IncludedColumnsRef extends ParameterReference<String[]> {
    }

    static final class AlwaysTrue implements EffectPredicateProvider {

        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.always();
        }

    }

    static final class CorrelationModelInfoProvider implements StateProvider<Optional<TextMessage.Message>> {

        Supplier<PMCCPortObjectAndSpec> m_modelSupplier;

        @Override
        public void init(final StateProviderInitializer initializer) {
            m_modelSupplier = initializer.computeFromProvidedState(PMCCPortObjectAndSpecProvider.class);
        }

        @Override
        public Optional<TextMessage.Message> computeState(final NodeParametersInput parametersInput)
            throws StateComputationFailureException {
            if (m_modelSupplier.get() == null) {
                return Optional.of(new TextMessage.Message("Missing input connection",
                    "Connect a correlation model to the first input port.",
                    TextMessage.MessageType.INFO));
            }
            final var pmccModel = m_modelSupplier.get();

            final var inSpecOpt = parametersInput.getInTableSpec(1);
            if (inSpecOpt.isEmpty()) {
                return Optional.of(new TextMessage.Message("Missing input connection",
                    "Connect a input data table to the second input port.",
                    TextMessage.MessageType.INFO));
            }

            HashSet<String> allColsInModel = new HashSet<>(Arrays.asList(pmccModel.getColNames()));
            for (DataColumnSpec s : inSpecOpt.get()) {
                if (s.getType().isCompatible(NominalValue.class)
                        || s.getType().isCompatible(DoubleValue.class)) {
                    allColsInModel.remove(s.getName());
                }
            }
            if (!allColsInModel.isEmpty()) {
                return Optional.of(new TextMessage.Message("Missing input columns",
                    "Some columns in the model are not contained in an input data table or incompatible: "
                            + allColsInModel.iterator().next(),
                    TextMessage.MessageType.WARNING));
            }

            if (!pmccModel.hasData()) {
                return Optional.of(new TextMessage.Message("No correlation data",
                    "Execute the upstream correlation node first.",
                    TextMessage.MessageType.INFO));
            }
            return Optional.empty();
        }

    }

    static final class IncludedColumnsProvider implements StateProvider<String[]> {

        Supplier<Double> m_thresholdSupplier;

        Supplier<PMCCPortObjectAndSpec> m_modelSupplier;

        @Override
        public void init(final StateProviderInitializer initializer) {
            m_thresholdSupplier = initializer.computeFromValueSupplier(ThresholdRef.class);
            m_modelSupplier = initializer.computeFromProvidedState(PMCCPortObjectAndSpecProvider.class);
        }

        @Override
        public String[] computeState(final NodeParametersInput parametersInput)
            throws StateComputationFailureException {
            final var threshold = m_thresholdSupplier.get();
            if (threshold == null) {
                throw new StateComputationFailureException();
            }
            if (m_modelSupplier.get() == null) {
                return new String[0];
            }
            final var inSpecOpt = parametersInput.getInTableSpec(1);
            if (inSpecOpt.isEmpty()) {
                return new String[0];
            }
            final var inSpec = inSpecOpt.get();
            final var pmccModel = m_modelSupplier.get();
            return pmccModel.hasData() ?
                Arrays.stream(pmccModel.getReducedSet(threshold)).filter(inSpec::containsName).toArray(String[]::new)
                : new String[0];
        }

    }

    static final class IncludedColumnsChoicesProvider implements StringChoicesProvider {

        Supplier<PMCCPortObjectAndSpec> m_modelSupplier;

        @Override
        public void init(final StateProviderInitializer initializer) {
            m_modelSupplier = initializer.computeFromProvidedState(PMCCPortObjectAndSpecProvider.class);
        }

        @Override
        public List<String> choices(final NodeParametersInput context) {
            if (m_modelSupplier.get() == null) {
                return List.of();
            }
            final var pmccModel = m_modelSupplier.get();

            final var inSpecOpt = context.getInTableSpec(1);
            if (inSpecOpt.isEmpty()) {
                return List.of();
            }
            final var inSpec = inSpecOpt.get();
            return pmccModel.hasData() ?
                Arrays.stream(pmccModel.getColNames()).filter(inSpec::containsName).toList() : List.of();
        }

    }

    static final class PMCCPortObjectAndSpecProvider implements StateProvider<PMCCPortObjectAndSpec> {

        @Override
        public void init(final StateProviderInitializer initializer) {
            initializer.computeBeforeOpenDialog();
        }

        @Override
        public PMCCPortObjectAndSpec computeState(final NodeParametersInput parametersInput)
            throws StateComputationFailureException {
            final var modelSpecOpt = parametersInput.getInPortSpec(0);
            if (modelSpecOpt.isEmpty()) {
                return null;
            }
            return (PMCCPortObjectAndSpec)modelSpecOpt.get();
        }

    }

    static final class DoNotPersistStringArray implements NodeParametersPersistor<String[]> {

        @Override
        public String[] load(final NodeSettingsRO settings) throws InvalidSettingsException {
            return new String[0];
        }

        @Override
        public void save(final String[] param, final NodeSettingsWO settings) {
            // do not persist
        }

        @Override
        public String[][] getConfigPaths() {
            return new String[][]{};
        }

    }

}
