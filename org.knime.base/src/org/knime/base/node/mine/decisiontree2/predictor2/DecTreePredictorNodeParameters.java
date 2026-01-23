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

package org.knime.base.node.mine.decisiontree2.predictor2;

import java.util.Optional;

import org.knime.base.node.mine.util.PredictorHelper;
import org.knime.base.node.mine.util.PredictorHelper.ProbabilitySuffixDefaultProvider;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.migration.LoadDefaultsForAbsentFields;
import org.knime.node.parameters.persistence.Persist;
import org.knime.node.parameters.persistence.Persistor;
import org.knime.node.parameters.persistence.legacy.OptionalStringPersistor;
import org.knime.node.parameters.updates.ParameterReference;
import org.knime.node.parameters.updates.ValueReference;
import org.knime.node.parameters.widget.OptionalWidget;
import org.knime.node.parameters.widget.number.NumberInputWidget;
import org.knime.node.parameters.widget.number.NumberInputWidgetValidation.MinValidation.IsNonNegativeValidation;
import org.knime.node.parameters.widget.text.TextInputWidget;
import org.knime.node.parameters.widget.text.util.ColumnNameValidationUtils.ColumnNameValidation;

/**
 * Node parameters for Decision Tree Predictor.
 *
 * @author Magnus Gohm, KNIME GmbH, Konstanz, Germany
 * @author AI Migration Pipeline v1.2
 */
@LoadDefaultsForAbsentFields
@SuppressWarnings("restriction")
final class DecTreePredictorNodeParameters implements NodeParameters {

    @Persist(configKey = DecTreePredictorNodeModel.MAXCOVERED)
    @Widget(title = "Number of patterns for hiliting", description = """
            Determines the maximum number of patterns the tree will store to support hiliting.
            """)
    @NumberInputWidget(minValidation = IsNonNegativeValidation.class, stepSize = 100)
    int m_maxNumCoveredPattern = 10000;

    @Persistor(PredictionColumnNamePersistor.class)
    @Widget(title = "Change prediction column name", description = """
            When set, you can change the name of the prediction column.
            The default prediction column name is: <tt>Prediction (trainingColumn)</tt>.
            """)
    @OptionalWidget(defaultProvider = DecisionTreePredictionColumnDefaultProvider.class)
    @TextInputWidget(patternValidation = ColumnNameValidation.class)
    @ValueReference(PredictionColumnNameRef.class)
    Optional<String> m_predictionColumnName = Optional.empty();

    static class PredictionColumnNameRef implements ParameterReference<Optional<String>> {
    }

    @Persistor(ProbabilitySuffixPersistor.class)
    @Widget(title = "Append columns with normalized class distribution", description = """
            If selected, a column is appended for each class instance with the normalized probability
            of this row being a member of this class. The probability columns will have names like:
            <tt>P (trainingColumn=value)</tt> with an optional suffix that can be specified.
            """)
    @OptionalWidget(defaultProvider = ProbabilitySuffixDefaultProvider.class)
    Optional<String> m_probabilitySuffix = Optional.empty();

    static class DecisionTreePredictionColumnDefaultProvider
        extends PredictorHelper.PredictionColumnNameDefaultProvider {

        protected DecisionTreePredictionColumnDefaultProvider() {
            super(PredictionColumnNameRef.class, PredictorHelper.DEFAULT_PREDICTION_COLUMN);
        }

    }

    static final class PredictionColumnNamePersistor extends OptionalStringPersistor {

        PredictionColumnNamePersistor() {
            super(PredictorHelper.CFGKEY_CHANGE_PREDICTION, PredictorHelper.CFGKEY_PREDICTION_COLUMN);
        }

    }

    static final class ProbabilitySuffixPersistor extends OptionalStringPersistor {

        ProbabilitySuffixPersistor() {
            super(DecTreePredictorNodeModel.SHOW_DISTRIBUTION, PredictorHelper.CFGKEY_SUFFIX);
        }

    }

}
