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
 *   Dec 12, 2024 (created): created
 */
package org.knime.base.node.mine.bayes.naivebayes.predictor4;

import org.knime.base.node.mine.util.PredictorHelper;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.node.port.pmml.PMMLPortObjectSpec;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.layout.Layout;
import org.knime.node.parameters.layout.Section;
import org.knime.node.parameters.persistence.Persist;
import org.knime.node.parameters.updates.Effect;
import org.knime.node.parameters.updates.Effect.EffectType;
import org.knime.node.parameters.updates.EffectPredicate;
import org.knime.node.parameters.updates.EffectPredicateProvider;
import org.knime.node.parameters.updates.ParameterReference;
import org.knime.node.parameters.updates.StateProvider;
import org.knime.node.parameters.updates.ValueProvider;
import org.knime.node.parameters.updates.ValueReference;
import org.knime.node.parameters.widget.text.TextInputWidget;

/**
 * Node settings for the Naive Bayes Predictor node.
 *
 * @author Tobias Koetter, KNIME AG, Zurich, Switzerland
 * @author Carsten Haubold, KNIME GmbH, Konstanz, Germany
 * @since 5.7
 */
public final class NaiveBayesPredictorNodeSettings implements NodeParameters {

    @Section(title = "Prediction Settings")
    interface PredictionSection {
    }

    @Section(title = "Probability Column Settings")
    interface ProbabilitySection {
    }

    static class OverridePredictedRef implements ParameterReference<Boolean> {
    }

    static class ChangePredictionNamePredicate implements EffectPredicateProvider {

        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getBoolean(OverridePredictedRef.class).isTrue();
        }
    }

    static class IncludeProbabilityRef implements ParameterReference<Boolean> {
    }

    static class AppendProbabilityColumnsPredicate implements EffectPredicateProvider {

        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getBoolean(IncludeProbabilityRef.class).isTrue();
        }
    }

    /** Reference for the prediction column name value (needed for ValueProvider). */
    static class PredictionColumnNameRef implements ParameterReference<String> { }

    /**
     * Provides a dynamic default for the prediction column name once the PMML spec (with target column) is available.
     * It only overwrites the current value if the user has not modified it yet (i.e. it is null, empty or equals the
     * static default {@link PredictorHelper#DEFAULT_PREDICTION_COLUMN}).
     */
    static class PredictionColumnNameProvider implements StateProvider<String> {

        private java.util.function.Supplier<String> m_currentValue;

        @Override
        public void init(final StateProviderInitializer i) {
            // Depend on (but do not trigger from) the current value, so we can decide whether to override it.
            m_currentValue = i.getValueSupplier(PredictionColumnNameRef.class);
            // Compute once synchronously before the dialog is shown.
            i.computeBeforeOpenDialog();
        }

        @Override
        public String computeState(final NodeParametersInput context) {
            final String current = m_currentValue.get();
            // Respect a user-entered custom value.
            if (current != null && !current.isEmpty()
                    && !PredictorHelper.DEFAULT_PREDICTION_COLUMN.equals(current)) {
                return current;
            }
            return getDefaultPredictionColumnNameFromContext(context);
        }
    }


    /**
     * Get the default prediction column name based on the PMML model input spec.
     *
     * @param context the node parameters input context
     * @return the default prediction column name
     */
    private static String getDefaultPredictionColumnNameFromContext(final NodeParametersInput context) {
        try {
            // Try to get the PMML model spec from the input context

            final var pmmlSpec = context.getInPortSpec(0);
            if (pmmlSpec.isPresent()) {
                final var targetCols = ((PMMLPortObjectSpec)pmmlSpec.get()).getTargetCols();
                if (!targetCols.isEmpty()) {
                    final DataColumnSpec classColumn = targetCols.get(0);
                    final PredictorHelper predictorHelper = PredictorHelper.getInstance();
                    return predictorHelper.computePredictionDefault(classColumn.getName());
                }
            }
        } catch (Exception e) {
            // If anything goes wrong, fall back to the default
        }
        return PredictorHelper.DEFAULT_PREDICTION_COLUMN;
    }


    @Layout(PredictionSection.class)
    @Widget(title = "Change prediction column name",
            description = "When set, you can change the name of the prediction column. "
                + "The default prediction column name is: Prediction (trainingColumn).")
    @ValueReference(OverridePredictedRef.class)
    @Persist(configKey = PredictorHelper.CFGKEY_CHANGE_PREDICTION)
    boolean m_overridePredicted = PredictorHelper.DEFAULT_CHANGE_PREDICTION;

    @Layout(PredictionSection.class)
    @Widget(title = "Prediction column name",
            description = "Specify the custom name for the prediction column. This field is only enabled when "
                + "'Change prediction column name' is checked.")
    @Effect(predicate = ChangePredictionNamePredicate.class, type = EffectType.SHOW)
    @TextInputWidget
    @Persist(configKey = PredictorHelper.CFGKEY_PREDICTION_COLUMN)
    @ValueReference(PredictionColumnNameRef.class)
    @ValueProvider(PredictionColumnNameProvider.class)
    String m_predictionColumnName = PredictorHelper.DEFAULT_PREDICTION_COLUMN; // initial placeholder until provider runs

    @Layout(ProbabilitySection.class)
    @Widget(title = "Append columns with normalized class distribution",
            description = "If selected, a column is appended for each class instance with the normalized probability "
                + "of this row being a member of this class. The probability columns will have names like: "
                + "P (trainingColumn=value) with an optional suffix.")
    @ValueReference(IncludeProbabilityRef.class)
    @Persist(configKey = "inclProbVals")
    boolean m_inclProbVals = false;

    @Layout(ProbabilitySection.class)
    @Widget(title = "Suffix for probability columns",
            description = "Specify a suffix to append to the probability column names. The probability columns are named "
                + "like: P (trainingColumn=value) followed by this suffix. This field is only enabled when "
                + "'Append columns with normalized class distribution' is checked.")
    @Effect(predicate = AppendProbabilityColumnsPredicate.class, type = EffectType.SHOW)
    @TextInputWidget
    @Persist(configKey = PredictorHelper.CFGKEY_SUFFIX)
    String m_probabilitySuffix = PredictorHelper.DEFAULT_SUFFIX;
}
