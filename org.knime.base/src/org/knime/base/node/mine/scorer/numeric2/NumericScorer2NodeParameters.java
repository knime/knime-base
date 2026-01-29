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

package org.knime.base.node.mine.scorer.numeric2;

import java.util.Optional;
import java.util.function.Supplier;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.webui.node.dialog.defaultdialog.internal.widget.PersistWithin;
import org.knime.core.webui.node.dialog.defaultdialog.util.updates.StateComputationFailureException;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.migration.LoadDefaultsForAbsentFields;
import org.knime.node.parameters.persistence.NodeParametersPersistor;
import org.knime.node.parameters.persistence.Persist;
import org.knime.node.parameters.persistence.Persistor;
import org.knime.node.parameters.persistence.legacy.OptionalStringPersistor;
import org.knime.node.parameters.updates.Effect;
import org.knime.node.parameters.updates.Effect.EffectType;
import org.knime.node.parameters.updates.ParameterReference;
import org.knime.node.parameters.updates.StateProvider;
import org.knime.node.parameters.updates.ValueProvider;
import org.knime.node.parameters.updates.ValueReference;
import org.knime.node.parameters.updates.legacy.ColumnNameAutoGuessValueProvider;
import org.knime.node.parameters.updates.util.BooleanReference;
import org.knime.node.parameters.widget.OptionalWidget;
import org.knime.node.parameters.widget.choices.ChoicesProvider;
import org.knime.node.parameters.widget.choices.util.ColumnSelectionUtil;
import org.knime.node.parameters.widget.choices.util.CompatibleColumnsProvider.DoubleColumnsProvider;
import org.knime.node.parameters.widget.number.NumberInputWidget;
import org.knime.node.parameters.widget.number.NumberInputWidgetValidation.MinValidation.IsNonNegativeValidation;

/**
 * Node parameters for Numeric Scorer.
 *
 * @author Magnus Gohm, KNIME GmbH, Konstanz, Germany
 * @author AI Migration Pipeline v1.2
 */
@LoadDefaultsForAbsentFields
@SuppressWarnings("restriction")
final class NumericScorer2NodeParameters implements NodeParameters {

    @Widget(title = "Reference column", description = """
            Column with the correct, observed, training data values.
            """)
    @PersistWithin(NumericScorer2Settings.CFGKEY_REFERENCE)
    @Persistor(ColumnNamePersistor.class)
    @ChoicesProvider(DoubleColumnsProvider.class)
    @ValueReference(ReferenceColumnRef.class)
    @ValueProvider(ReferenceColumnProvider.class)
    String m_referenceColumn;

    static final class ReferenceColumnRef implements ParameterReference<String> {
    }

    @Widget(title = "Predicted column", description = """
            Column with the modeled, predicted data values.
            """)
    @PersistWithin(NumericScorer2Settings.CFGKEY_PREDICTED)
    @Persistor(ColumnNamePersistor.class)
    @ChoicesProvider(DoubleColumnsProvider.class)
    @ValueReference(PredictedColumnRef.class)
    @ValueProvider(PredictedColumnProvider.class)
    String m_predictedColumn;

    static final class PredictedColumnRef implements ParameterReference<String> {
    }

    @Widget(title = "Override output column name", description = """
            Overrides the name of the column in the output. By default, the name of the predicted column is used.
            """)
    @Persist(configKey = NumericScorer2Settings.CFGKEY_OVERRIDE_OUTPUT)
    @ValueReference(IsOverrideOutputEnabled.class)
    boolean m_overrideOutput;

    static final class IsOverrideOutputEnabled implements BooleanReference {
    }

    @Widget(title = "Output column name", description = """
            The name of the column in the output.
            """)
    @Persist(configKey = NumericScorer2Settings.CFGKEY_OUTPUT)
    @ValueReference(OutputColumnNameRef.class)
    @ValueProvider(OutputColumnNameProvider.class)
    @Effect(predicate = IsOverrideOutputEnabled.class, type = EffectType.ENABLE)
    String m_outputColumnName;

    static final class OutputColumnNameRef implements ParameterReference<String> {
    }

    static final class OutputColumnNameProvider implements StateProvider<String> {

        private Supplier<String> m_predictionColumnSupplier;

        private Supplier<String> m_outputColumnNameSupplier;

        private Supplier<Boolean> m_isOverrideOutputEnabledSupplier;

        @Override
        public void init(final StateProviderInitializer initializer) {
            initializer.computeBeforeOpenDialog();
            m_predictionColumnSupplier = initializer.computeFromValueSupplier(PredictedColumnRef.class);
            m_outputColumnNameSupplier = initializer.getValueSupplier(OutputColumnNameRef.class);
            m_isOverrideOutputEnabledSupplier = initializer.computeFromValueSupplier(IsOverrideOutputEnabled.class);
        }

        @Override
        public String computeState(final NodeParametersInput parametersInput) throws StateComputationFailureException {
            final var isOverrideOutputEnabled = m_isOverrideOutputEnabledSupplier.get();
            final var outputColumnName = m_outputColumnNameSupplier.get();
            if (isOverrideOutputEnabled && outputColumnName != null && !outputColumnName.isEmpty()) {
                throw new StateComputationFailureException();
            }

            final var predictionColumn = m_predictionColumnSupplier.get();
            if (predictionColumn == null || predictionColumn.isEmpty()) {
                throw new StateComputationFailureException();
            }
            return predictionColumn;
        }

    }

    @Widget(title = "Export and prefix flow variables", description = """
            If enabled, the scores will be exported as flow variables with hard coded names and prefixed by the given
            value.
            """)
    @Persistor(FlowVariablePrefixPersistor.class)
    @OptionalWidget(defaultProvider = FlowVariablePrefixDefaultProvider.class)
    Optional<String> m_flowVariablePrefix = Optional.of("");

    static final class FlowVariablePrefixDefaultProvider implements OptionalWidget.DefaultValueProvider<String> {

        @Override
        public String computeState(final NodeParametersInput parametersInput) throws StateComputationFailureException {
            return "";
        }

    }

    @Widget(title = "Number of predictors", description = """
            The number of predictors used to compute the adjusted R squared. The adjusted R squared is calculated as:
            1 - ((1 - R²) * (n - 1)) / (n - p - 1), where n is the number of observations and p is the number of
            predictors.
            """)
    @Persist(configKey = NumericScorer2Settings.CFG_KEY_NUM_PREDICTORS)
    @NumberInputWidget(minValidation = IsNonNegativeValidation.class)
    int m_numberOfPredictors;

    static final class ReferenceColumnProvider extends ColumnNameAutoGuessValueProvider {

        protected ReferenceColumnProvider() {
            super(ReferenceColumnRef.class);
        }

        @Override
        protected Optional<DataColumnSpec> autoGuessColumn(final NodeParametersInput parametersInput) {
            final var compatibleColumns =
                ColumnSelectionUtil.getCompatibleColumnsOfFirstPort(parametersInput, DoubleValue.class);
            return compatibleColumns.isEmpty() ? Optional.empty()
                : Optional.of(compatibleColumns.get(compatibleColumns.size() - 1));
        }

    }

    static final class PredictedColumnProvider extends ColumnNameAutoGuessValueProvider {

        protected PredictedColumnProvider() {
            super(PredictedColumnRef.class);
        }

        @Override
        protected Optional<DataColumnSpec> autoGuessColumn(final NodeParametersInput parametersInput) {
            final var compatibleColumns =
                ColumnSelectionUtil.getCompatibleColumnsOfFirstPort(parametersInput, DoubleValue.class);
            return compatibleColumns.isEmpty() ? Optional.empty()
                : Optional.of(compatibleColumns.get(compatibleColumns.size() - 1));
        }

    }

    static final class FlowVariablePrefixPersistor extends OptionalStringPersistor {

        FlowVariablePrefixPersistor() {
            super(NumericScorer2Settings.CFGKEY_USE_FLOWVARIABLE_PREFIX,
                NumericScorer2Settings.CFGKEY_FLOWVARIABLE_PREFIX);
        }

    }

    static final class ColumnNamePersistor implements NodeParametersPersistor<String> {

        @Override
        public String load(final NodeSettingsRO settings) throws InvalidSettingsException {
            return settings.getString("columnName", "");
        }

        @Override
        public void save(final String param, final NodeSettingsWO settings) {
            // It was not possible to execute the node with RowID selected, so we removed that option, but still need
            // it as it is used for the validation of the settings.
            settings.addBoolean("useRowID", false);
            settings.addString("columnName", param);
        }

        @Override
        public String[][] getConfigPaths() {
            return new String[][]{{"columnName"}};
        }

    }

}
