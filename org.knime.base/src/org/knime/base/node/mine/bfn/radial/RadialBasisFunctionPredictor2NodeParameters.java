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

package org.knime.base.node.mine.bfn.radial;

import java.util.Optional;

import org.knime.base.node.mine.bfn.BasisFunctionPredictor2NodeDialog;
import org.knime.base.node.mine.bfn.radial.RadialBasisFunctionPredictor2NodeParameters.DontKnowClassNodeParameters.DontKnowClassPersistor;
import org.knime.base.node.mine.util.PredictorHelper;
import org.knime.base.node.mine.util.PredictorNodeParameters;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.webui.node.dialog.defaultdialog.internal.widget.PersistWithin;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Modification;
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
import org.knime.node.parameters.updates.ValueProvider;
import org.knime.node.parameters.updates.ValueReference;
import org.knime.node.parameters.updates.util.BooleanReference;
import org.knime.node.parameters.widget.choices.Label;
import org.knime.node.parameters.widget.choices.ValueSwitchWidget;
import org.knime.node.parameters.widget.number.NumberInputWidget;
import org.knime.node.parameters.widget.number.NumberInputWidgetValidation.MaxValidation;
import org.knime.node.parameters.widget.number.NumberInputWidgetValidation.MinValidation.IsNonNegativeValidation;
import org.knime.node.parameters.widget.text.TextInputWidget;
import org.knime.node.parameters.widget.text.util.ColumnNameValidationUtils.ColumnNameValidation;

/**
 * Node parameters for PNN Predictor.
 *
 * @author Magnus Gohm, KNIME GmbH, Konstanz, Germany
 * @author AI Migration Pipeline v1.2
 */
@SuppressWarnings("restriction")
@LoadDefaultsForAbsentFields
@Modification(RadialBasisFunctionPredictor2NodeParameters.PNNPredictorNodeParametersModification.class)
final class RadialBasisFunctionPredictor2NodeParameters extends PredictorNodeParameters {

    @Persistor(DontKnowClassPersistor.class)
    DontKnowClassNodeParameters m_dontKnowClass = new DontKnowClassNodeParameters();

    static class DontKnowClassNodeParameters implements NodeParameters {

        @Widget(title = "Don't know class", description = """
                Select how to handle cases where the activation degree for all classes is below a certain threshold.
                """)
        @ValueSwitchWidget
        @ValueReference(DontKnowClassModeRef.class)
        DontKnowClassMode m_mode = DontKnowClassMode.IGNORE;

        static final class DontKnowClassModeRef implements ParameterReference<DontKnowClassMode> {
        }

        @Widget(title = "Threshold", description = """
                The threshold value between 0 and 1. Instances where the activation lies below this threshold
                are classified as unknown.
                """)
        @NumberInputWidget(minValidation = IsNonNegativeValidation.class, maxValidation = DontKnowMaxValidation.class)
        @Effect(predicate = IsUseDontKnowClassMode.class, type = EffectType.SHOW)
        double m_threshold = 0.0;

        static final class DontKnowMaxValidation extends MaxValidation {

            @Override
            protected double getMax() {
                return 1.0;
            }

        }

        static final class IsUseDontKnowClassMode implements EffectPredicateProvider {

            @Override
            public EffectPredicate init(final PredicateInitializer i) {
                return i.getEnum(DontKnowClassModeRef.class).isOneOf(DontKnowClassMode.USE);
            }

        }

        static final class DontKnowClassPersistor implements NodeParametersPersistor<DontKnowClassNodeParameters> {

            @Override
            public DontKnowClassNodeParameters load(final NodeSettingsRO settings) throws InvalidSettingsException {
                final var dontKnowSettings = new DontKnowClassNodeParameters();
                final var ignoreDontKnow =
                        settings.getBoolean(BasisFunctionPredictor2NodeDialog.CFG_DONT_KNOW_IGNORE, false);
                final var dontKnowValue = settings.getDouble(BasisFunctionPredictor2NodeDialog.DONT_KNOW_PROP, -1.0);
                if (ignoreDontKnow) {
                    dontKnowSettings.m_mode = DontKnowClassMode.IGNORE;
                    dontKnowSettings.m_threshold = 0.0;
                } else {
                    if (dontKnowValue < 0.0) {
                        dontKnowSettings.m_mode = DontKnowClassMode.DEFAULT;
                        dontKnowSettings.m_threshold = 0.0;
                    } else {
                        dontKnowSettings.m_mode = DontKnowClassMode.USE;
                        dontKnowSettings.m_threshold = dontKnowValue;
                    }
                }
                return dontKnowSettings;
            }

            @Override
            public void save(final DontKnowClassNodeParameters param, final NodeSettingsWO settings) {
                if (param.m_mode == DontKnowClassMode.IGNORE) {
                    settings.addBoolean(BasisFunctionPredictor2NodeDialog.CFG_DONT_KNOW_IGNORE, true);
                    settings.addDouble(BasisFunctionPredictor2NodeDialog.DONT_KNOW_PROP, 0.0);
                } else {
                    if (param.m_mode == DontKnowClassMode.DEFAULT) {
                        settings.addDouble(BasisFunctionPredictor2NodeDialog.DONT_KNOW_PROP, -1.0);
                    } else {
                        settings.addDouble(BasisFunctionPredictor2NodeDialog.DONT_KNOW_PROP, param.m_threshold);
                    }
                }
            }

            @Override
            public String[][] getConfigPaths() {
                return new String[][]{
                    {BasisFunctionPredictor2NodeDialog.CFG_DONT_KNOW_IGNORE},
                    {BasisFunctionPredictor2NodeDialog.DONT_KNOW_PROP}
                };
            }

        }

        enum DontKnowClassMode {
            @Label(value = "Default", description = "Use the minimum activation threshold from the learning algorithm.")
            DEFAULT, //
            @Label(value = "Ignore", description = "If selected, no lower degree of class activation is set.")
            IGNORE, //
            @Label(value = "Use", description = """
                    Instances where the activation lies below this threshold are classified as a missing (unknown)
                    class. This is useful in cases where the feature space is not completely covered by rules.
                    """)
            USE;
        }

    }

    static final class PNNPredictorNodeParametersModification implements Modification.Modifier {

        @Override
        public void modify(final Modification.WidgetGroupModifier group) {
            addDirtyTracker(group, MakePNNPredictorDialogDirtyProvider.class);
        }

    }

    @Persist(configKey = PredictorHelper.CFGKEY_CHANGE_PREDICTION)
    @Widget(title = "Change prediction column name", description = """
            When set, you can change the name of the prediction column.
            """)
    @ValueReference(ChangePredictionColumn.class)
    boolean m_changePredictionColumn;

    private static class ChangePredictionColumn implements BooleanReference {
    }

    @Persist(configKey = PredictorHelper.CFGKEY_PREDICTION_COLUMN)
    @Widget(title = "Prediction column", description = """
            The possibly overridden column name for the predicted column.
            (The default is <tt>Prediction(trainingColumn)</tt>)
            """)
    @TextInputWidget(patternValidation = ColumnNameValidation.class)
    @ValueProvider(SVMPredictionColumnDefaultProvider.class)
    @ValueReference(PredictionColumnNameRef.class)
    @Effect(predicate = ChangePredictionColumn.class, type = EffectType.SHOW)
    String m_predictionColumnName;

    private static class PredictionColumnNameRef implements ParameterReference<String> {
    }

    @PersistWithin({PredictorHelper.CFGKEY_PREDICTION_COLUMN + "_Internals"})
    @Persist(configKey = "EnabledStatus")
    @ValueProvider(PredictionColumnEnabledProvider.class)
    @ValueReference(IsPredictionColumnNameEnabled.class)
    boolean m_predictionColumnNameEnabled;

    private static final class IsPredictionColumnNameEnabled implements BooleanReference {
    }

    @PersistWithin({PredictorHelper.CFGKEY_PREDICTION_COLUMN + "_Internals"})
    @Persist(configKey = "SettingsModelID")
    String m_predictionColumnModelID;

    @Persist(configKey = BasisFunctionPredictor2NodeDialog.CFG_CLASS_PROBS)
    @Widget(title = "Append class columns", description = """
            Append class probability columns.
            """)
    @ValueReference(AddProbabilitySuffix.class)
    boolean m_addProbabilitySuffix;

    private static class AddProbabilitySuffix implements BooleanReference {
    }

    @Persist(configKey = PredictorHelper.CFGKEY_SUFFIX)
    @Widget(title = "Suffix for probability columns", description = """
            Suffix for the normalized distribution columns. Their names are like: <tt>P(trainingColumn=value)</tt>.
            """)
    @ValueReference(ProbabilitySuffixRef.class)
    @Effect(predicate = AddProbabilitySuffix.class, type = EffectType.SHOW)
    String m_probabilitySuffix;

    private static class ProbabilitySuffixRef implements ParameterReference<String> {
    }

    @PersistWithin({PredictorHelper.CFGKEY_SUFFIX + "_Internals"})
    @Persist(configKey = "EnabledStatus")
    @ValueProvider(ProbabilitySuffixEnabledProvider.class)
    @ValueReference(IsProbabilitySuffixEnabled.class)
    boolean m_probabilitySuffixEnabled;

    private static final class IsProbabilitySuffixEnabled implements BooleanReference {
    }

    @PersistWithin({PredictorHelper.CFGKEY_SUFFIX + "_Internals"})
    @Persist(configKey = "SettingsModelID")
    String m_probabilitySuffixModelID;

    static class SVMPredictionColumnDefaultProvider extends PredictionColumnNameDefaultProvider {

        protected SVMPredictionColumnDefaultProvider() {
            super(PredictionColumnNameRef.class, PredictorHelper.DEFAULT_PREDICTION_COLUMN);
        }

      @Override
      protected Optional<String> extractTargetColumn(final NodeParametersInput context) {
          final var specOpt = context.getInTableSpec(0);
          if (specOpt.isPresent()) {
              final var spec = specOpt.get();
              final var predictorHelper = PredictorHelper.getInstance();
              return Optional.of(
                  predictorHelper.computePredictionDefault(spec.getColumnSpec(spec.getNumColumns() - 5).getName()));
          }
          return Optional.empty();
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

    static final class MakePNNPredictorDialogDirtyProvider extends MakeDialogDirtyProvider {

        MakePNNPredictorDialogDirtyProvider() {
            super(IsPredictionColumnNameEnabled.class, IsProbabilitySuffixEnabled.class);
        }

    }

}
