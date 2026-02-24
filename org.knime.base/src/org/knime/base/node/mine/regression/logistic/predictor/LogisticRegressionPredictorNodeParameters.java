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

package org.knime.base.node.mine.regression.logistic.predictor;

import org.knime.base.node.mine.regression.predict3.RegressionPredictorSettings;
import org.knime.base.node.mine.util.PredictorHelper;
import org.knime.base.node.mine.util.PredictorNodeParameters;
import org.knime.core.webui.node.dialog.defaultdialog.internal.widget.PersistWithin;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Modification;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.migration.LoadDefaultsForAbsentFields;
import org.knime.node.parameters.persistence.Persist;
import org.knime.node.parameters.updates.Effect;
import org.knime.node.parameters.updates.Effect.EffectType;
import org.knime.node.parameters.updates.ParameterReference;
import org.knime.node.parameters.updates.ValueProvider;
import org.knime.node.parameters.updates.ValueReference;
import org.knime.node.parameters.updates.util.BooleanReference;
import org.knime.node.parameters.widget.text.TextInputWidget;
import org.knime.node.parameters.widget.text.util.ColumnNameValidationUtils.ColumnNameValidation;

/**
 * Node parameters for Logistic Regression Predictor.
 *
 * @author Magnus Gohm, KNIME GmbH, Konstanz, Germany
 * @author AI Migration Pipeline v1.2
 */
@SuppressWarnings("restriction")
@LoadDefaultsForAbsentFields
@Modification(LogisticRegressionPredictorNodeParameters.LogRegPredictorNodeParametersModification.class)
final class LogisticRegressionPredictorNodeParameters extends PredictorNodeParameters {

    static final class LogRegPredictorNodeParametersModification implements Modification.Modifier {

        @Override
        public void modify(final Modification.WidgetGroupModifier group) {
            addDirtyTracker(group, MakeLogRegPredictorDialogDirtyProvider.class);
        }

    }

    @Persist(configKey = RegressionPredictorSettings.CFG_HAS_CUSTOM_PREDICTION_NAME)
    @Widget(title = "Change prediction column name", description = """
            When set, you can change the name of the prediction column.
            """)
    @ValueReference(ChangePredictionColumn.class)
    boolean m_changePredictionColumn;

    private static class ChangePredictionColumn implements BooleanReference {
    }

    @Persist(configKey = RegressionPredictorSettings.CFG_CUSTOM_PREDICTION_NAME)
    @Widget(title = "Prediction column", description = """
            The possibly overridden column name for the predicted column.
            (The default is <tt>Prediction(trainingColumn)</tt>)
            """)
    @TextInputWidget(patternValidation = ColumnNameValidation.class)
    @ValueProvider(LogRegPredictionColumnDefaultProvider.class)
    @ValueReference(PredictionColumnNameRef.class)
    @Effect(predicate = ChangePredictionColumn.class, type = EffectType.SHOW)
    String m_predictionColumnName;

    private static class PredictionColumnNameRef implements ParameterReference<String> {
    }

    @PersistWithin({RegressionPredictorSettings.CFG_CUSTOM_PREDICTION_NAME + "_Internals"})
    @Persist(configKey = "EnabledStatus")
    @ValueProvider(PredictionColumnEnabledProvider.class)
    @ValueReference(IsPredictionColumnNameEnabled.class)
    boolean m_predictionColumnNameEnabled;

    private static final class IsPredictionColumnNameEnabled implements BooleanReference {
    }

    @PersistWithin({RegressionPredictorSettings.CFG_CUSTOM_PREDICTION_NAME + "_Internals"})
    @Persist(configKey = "SettingsModelID")
    String m_predictionColumnModelID;

    @Persist(configKey = RegressionPredictorSettings.CFG_INCLUDE_PROBABILITIES)
    @Widget(title = "Append class columns", description = """
            Append class probability columns.
            """)
    @ValueReference(AddProbabilitySuffix.class)
    boolean m_addProbabilitySuffix;

    private static class AddProbabilitySuffix implements BooleanReference {
    }

    @Persist(configKey = RegressionPredictorSettings.CFG_PROP_COLUMN_SUFFIX)
    @Widget(title = "Suffix for probability columns", description = """
            Suffix for the normalized distribution columns. Their names are like: <tt>P(trainingColumn=value)</tt>.
            """)
    @ValueReference(ProbabilitySuffixRef.class)
    @Effect(predicate = AddProbabilitySuffix.class, type = EffectType.SHOW)
    String m_probabilitySuffix;

    private static class ProbabilitySuffixRef implements ParameterReference<String> {
    }

    @PersistWithin({RegressionPredictorSettings.CFG_PROP_COLUMN_SUFFIX + "_Internals"})
    @Persist(configKey = "EnabledStatus")
    @ValueProvider(ProbabilitySuffixEnabledProvider.class)
    @ValueReference(IsProbabilitySuffixEnabled.class)
    boolean m_probabilitySuffixEnabled;

    private static final class IsProbabilitySuffixEnabled implements BooleanReference {
    }

    @PersistWithin({RegressionPredictorSettings.CFG_PROP_COLUMN_SUFFIX + "_Internals"})
    @Persist(configKey = "SettingsModelID")
    String m_probabilitySuffixModelID;

    static class LogRegPredictionColumnDefaultProvider extends PredictionColumnNameDefaultProvider {

        protected LogRegPredictionColumnDefaultProvider() {
            super(PredictionColumnNameRef.class, PredictorHelper.DEFAULT_PREDICTION_COLUMN);
        }

    }

    private static final class PredictionColumnEnabledProvider extends EnabledStatusProvider {

        PredictionColumnEnabledProvider() {
            super(ChangePredictionColumn.class, IsPredictionColumnNameEnabled.class);
        }

    }

    private static final class ProbabilitySuffixEnabledProvider extends EnabledStatusProvider {

        ProbabilitySuffixEnabledProvider() {
            super(AddProbabilitySuffix.class, IsProbabilitySuffixEnabled.class);
        }

    }

    static final class MakeLogRegPredictorDialogDirtyProvider extends MakeDialogDirtyProvider {

        MakeLogRegPredictorDialogDirtyProvider() {
            super(IsPredictionColumnNameEnabled.class, IsProbabilitySuffixEnabled.class);
        }

    }

}
