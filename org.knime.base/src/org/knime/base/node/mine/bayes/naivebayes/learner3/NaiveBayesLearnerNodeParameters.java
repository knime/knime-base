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
package org.knime.base.node.mine.bayes.naivebayes.learner3;

import org.knime.base.node.mine.bayes.naivebayes.datamodel3.NaiveBayesModel;
import org.knime.core.data.NominalValue;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.migration.LoadDefaultsForAbsentFields;
import org.knime.node.parameters.persistence.Persist;
import org.knime.node.parameters.updates.Effect;
import org.knime.node.parameters.updates.Effect.EffectType;
import org.knime.node.parameters.updates.EffectPredicate;
import org.knime.node.parameters.updates.EffectPredicateProvider;
import org.knime.node.parameters.updates.ParameterReference;
import org.knime.node.parameters.updates.StateProvider;
import org.knime.node.parameters.updates.ValueProvider;
import org.knime.node.parameters.updates.ValueReference;
import org.knime.node.parameters.widget.choices.ChoicesProvider;
import org.knime.node.parameters.widget.choices.util.CompatibleColumnsProvider;
import org.knime.node.parameters.widget.number.NumberInputWidget;
import org.knime.node.parameters.widget.number.NumberInputWidgetValidation.MinValidation;
import org.knime.node.parameters.widget.number.NumberInputWidgetValidation.MinValidation.IsNonNegativeValidation;

/**
 * Node settings for the Naive Bayes Learner node.
 *
 * @author Tobias Koetter
 * @author Carsten Haubold, KNIME GmbH, Konstanz, Germany
 */
@LoadDefaultsForAbsentFields
final class NaiveBayesLearnerNodeParameters implements NodeParameters {
    @Widget(title = "Classification column",
        description = "The column containing the class values that the model should learn to predict.")
    @ChoicesProvider(NominalColumnChoicesProvider.class)
    @Persist(configKey = NaiveBayesLearnerNodeModel3.CFG_CLASSIFYCOLUMN_KEY)
    String m_classifyColumn;

    @Widget(title = "Default probability", //
        description = """
                A probability of zero for a given attribute/class value pair requires special attention.
                Without adjustment, a probability of zero would exercise an absolute veto over a likelihood in which
                that probability appears as a factor. Therefore, the Bayes model incorporates a default probability
                parameter that specifies a default (usually very small) probability to use in lieu of zero probability
                for a given attribute/class value pair. The default probability is used if the attribute is:
                <ul>
                    <li>nominal and was not seen by the learner
                    </li>
                    <li>continuous and its probability is smaller than the default probability
                    </li>
                </ul>
                """)
    @NumberInputWidget(minValidation = MinProbabilityThresholdValidation.class)
    @Persist(configKey = NaiveBayesLearnerNodeModel3.CFG_THRESHOLD_KEY)
    double m_threshold = NaiveBayesModel.DEFAULT_MIN_PROB_THRESHOLD;

    @Widget(title = "Minimum standard deviation",
        description = "Specify the minimum standard deviation to use for observations without enough (diverse) data. "
            + "The value must be at least 1e-10.")
    @NumberInputWidget(minValidation = MinProbabilityThresholdValidation.class)
    @Persist(configKey = NaiveBayesLearnerNodeModel3.CFG_MIN_SD_VALUE_KEY)
    double m_minSdValue = NaiveBayesLearnerNodeModel3.MIN_SD_VALUE_DEF;

    @Widget(title = "Threshold standard deviation",
        description = "Specify the threshold for standard deviation. The value must be positive. "
            + "If this threshold is not met, the minimum standard deviation value is used.")
    @NumberInputWidget(minValidation = IsNonNegativeValidation.class)
    @Persist(configKey = NaiveBayesLearnerNodeModel3.CFG_MIN_SD_THRESHOLD_KEY)
    double m_minSdThreshold = NaiveBayesLearnerNodeModel3.MIN_SD_THRESHOLD_DEF;

    @Widget(title = "Maximum number of unique nominal values per attribute", description = """
            All nominal columns with more unique values than the defined number will be skipped during learning.
            """)
    @NumberInputWidget(minValidation = IsNonNegativeValidation.class)
    @Persist(configKey = NaiveBayesLearnerNodeModel3.CFG_MAX_NO_OF_NOMINAL_VALS_KEY)
    int m_maxNoOfNominalVals = NaiveBayesLearnerNodeModel3.MAX_NO_OF_NOMINAL_VALS_DEF;

    @Widget(title = "Ignore missing values",
        description = "By default the node uses the missing value information to improve the prediction result. "
            + "Since the PMML standard does not support this option and ignores missing values this option is disabled "
            + "if the PMML compatibility option is selected and missing values are ignored.")
    @Effect(predicate = PmmlCompatibleIsTrue.class, type = EffectType.DISABLE)
    @ValueReference(IgnoreMissingValsRef.class)
    @ValueProvider(IgnoreMissingValsValueProvider.class)
    @Persist(configKey = NaiveBayesLearnerNodeModel3.CFG_SKIP_MISSING_VALUES)
    boolean m_ignoreMissingVals;

    @Widget(title = "Create PMML 4.2 compatible model", //
        description = """
                Select this option to create a model which is compliant with the
                <a href="http://www.dmg.org/v4-2/NaiveBayes.html">PMML 4.2 standard</a>.
                The PMML 4.2 standard ignores missing values and does not support bit vectors. Therefore bit vector
                columns and missing values are ignored during learning and prediction if this option is selected.
                <p>
                Even if this option is not selected the node creates a valid PMML model. However the model contains
                KNIME specific information to store missing value and bit vector information. This information is used
                in the KNIME Naive Bayes Predictor to improve the prediction result but ignored by any other PMML
                compatible predictor which might result in different prediction results.
                </p>
                """)
    @Persist(configKey = NaiveBayesLearnerNodeModel3.CFG_PMML_COMPATIBLE)
    @ValueReference(PmmlCompatibleRef.class)
    boolean m_pmmlCompatible;

    /**
     * Column choices provider for nominal columns only.
     */
    static final class NominalColumnChoicesProvider extends CompatibleColumnsProvider {
        NominalColumnChoicesProvider() {
            super(NominalValue.class);
        }
    }

    interface PmmlCompatibleRef extends ParameterReference<Boolean> {
    }

    static final class PmmlCompatibleIsTrue implements EffectPredicateProvider {
        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getBoolean(PmmlCompatibleRef.class).isTrue();
        }
    }

    interface IgnoreMissingValsRef extends ParameterReference<Boolean> {
    }

    /**
     * Ensures that ignore-missing is set to true whenever PMML compatibility is enabled, otherwise leaves it as is.
     */
    static final class IgnoreMissingValsValueProvider implements StateProvider<Boolean> {
        private java.util.function.Supplier<Boolean> m_pmmlSupplier;

        private java.util.function.Supplier<Boolean> m_currentValueSupplier;

        @Override
        public void init(final StateProviderInitializer initializer) {
            // Recompute on PMML compatibility changes and depend on its value
            m_pmmlSupplier = initializer.computeFromValueSupplier(PmmlCompatibleRef.class);
            // Read current value without triggering recomputation (to preserve user choice when PMML is off)
            m_currentValueSupplier = initializer.getValueSupplier(IgnoreMissingValsRef.class);
            // Also compute once when opening the dialog to enforce true initially when needed
            initializer.computeBeforeOpenDialog();
        }

        @Override
        public Boolean computeState(final org.knime.node.parameters.NodeParametersInput parametersInput) {
            return Boolean.TRUE.equals(m_pmmlSupplier.get()) ? Boolean.TRUE : m_currentValueSupplier.get();
        }
    }

    static final class MinProbabilityThresholdValidation extends MinValidation {
        @Override
        public double getMin() {
            return NaiveBayesLearnerNodeModel3.MIN_PROB_THRESHOLD_DEF;
        }
    }
}
