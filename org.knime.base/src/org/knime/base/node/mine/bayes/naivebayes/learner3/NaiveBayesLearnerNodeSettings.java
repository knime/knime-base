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

import java.util.Arrays;
import java.util.List;

import org.knime.base.node.mine.bayes.naivebayes.datamodel3.NaiveBayesModel;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.NominalValue;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.persistence.Persist;
import org.knime.node.parameters.widget.choices.ChoicesProvider;
import org.knime.node.parameters.widget.choices.ColumnChoicesProvider;
import org.knime.node.parameters.widget.number.NumberInputWidget;
import org.knime.node.parameters.widget.number.NumberInputWidgetValidation.MinValidation;
import org.knime.node.parameters.widget.number.NumberInputWidgetValidation.MinValidation.IsNonNegativeValidation;
import org.knime.node.parameters.widget.number.NumberInputWidgetValidation.MinValidation.IsPositiveIntegerValidation;

/**
 * Node settings for the Naive Bayes Learner node.
 *
 * @author Tobias Koetter
 * @author Generated
 */
public final class NaiveBayesLearnerNodeSettings implements NodeParameters {

    /**
     * Constructor called by the framework to get default settings.
     *
     * @param context of the settings creation
     */
    NaiveBayesLearnerNodeSettings(final NodeParametersInput context) {
        // Default constructor
    }

    /**
     * Constructor called by the framework for persistence and JSON conversion.
     */
    NaiveBayesLearnerNodeSettings() {
        // Default constructor
    }

    @Widget(title = "Classification Column",
            description = "The column containing the class values that the model should learn to predict.")
    @ChoicesProvider(NominalColumnChoicesProvider.class)
    @Persist(configKey = NaiveBayesLearnerNodeModel3.CFG_CLASSIFYCOLUMN_KEY)
    String m_classifyColumn;

    @Widget(title = "Default probability",
            description = "A probability of zero for a given attribute/class value pair requires special attention. "
                + "Without adjustment, a probability of zero would exercise an absolute veto over a likelihood in which that "
                + "probability appears as a factor. Therefore, the Bayes model incorporates a default probability parameter "
                + "that specifies a default (usually very small) probability to use in lieu of zero probability for a "
                + "given attribute/class value pair. The default probability is used if the attribute is: "
                + "(1) nominal and was not seen by the learner, or "
                + "(2) continuous and its probability is smaller than the default probability.")
    @NumberInputWidget(minValidation = MinProbabilityThresholdValidation.class)
    @Persist(configKey = "threshold")
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
    double m_minSdThreshold = 0.0;

    @Widget(title = "Maximum number of unique nominal values per attribute",
            description = "All nominal columns with more unique values than the defined number will be skipped during learning.")
    @NumberInputWidget(minValidation = IsPositiveIntegerValidation.class)
    @Persist(configKey = NaiveBayesLearnerNodeModel3.CFG_MAX_NO_OF_NOMINAL_VALS_KEY)
    int m_maxNoOfNominalVals = 20;

    @Widget(title = "Ignore missing values",
            description = "By default the node uses the missing value information to improve the prediction result. "
                + "Since the PMML standard does not support this option and ignores missing values this option is disabled "
                + "if the PMML compatibility option is selected and missing values are ignored.")
    @Persist(configKey = NaiveBayesLearnerNodeModel3.CFG_SKIP_MISSING_VALUES)
    boolean m_ignoreMissingVals = false;

    @Widget(title = "Create PMML 4.2 compatible model",
            description = "Select this option to create a model which is compliant with the PMML 4.2 standard. "
                + "The PMML 4.2 standard ignores missing values and does not support bit vectors. Therefore bit vector columns "
                + "and missing values are ignored during learning and prediction if this option is selected. "
                + "Even if this option is not selected the node creates a valid PMML model. However the model contains "
                + "KNIME specific information to store missing value and bit vector information. This information is used in "
                + "the KNIME Naive Bayes Predictor to improve the prediction result but ignored by any other PMML compatible "
                + "predictor which might result in different prediction results.")
    @Persist(configKey = NaiveBayesLearnerNodeModel3.CFG_PMML_COMPATIBLE)
    boolean m_pmmlCompatible = false;

    /**
     * Column choices provider for nominal columns only.
     */
    static final class NominalColumnChoicesProvider implements ColumnChoicesProvider {
        @Override
        public List<DataColumnSpec> columnChoices(final NodeParametersInput context) {
            return context.getInTableSpec(0) //
                .map(spec -> spec.stream() //
                   .filter(colSpec -> colSpec.getType().isCompatible(NominalValue.class))) //
                   .orElse(Arrays.stream(new DataColumnSpec[0]))
                   .toList();
        }
    }

    /**
     *
     */
    static final class MinProbabilityThresholdValidation extends MinValidation{
        @Override
        public double getMin() {
            return NaiveBayesLearnerNodeModel3.MIN_PROB_THRESHOLD_DEF;
        }
    }
}
