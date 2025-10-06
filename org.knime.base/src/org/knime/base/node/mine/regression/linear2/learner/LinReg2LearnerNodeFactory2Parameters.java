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
package org.knime.base.node.mine.regression.linear2.learner;

import static org.knime.base.node.mine.regression.linear2.learner.LinReg2LearnerSettings.CFG_COLUMN_FILTER;
import static org.knime.base.node.mine.regression.linear2.learner.LinReg2LearnerSettings.CFG_INCLUDE_CONSTANT;
import static org.knime.base.node.mine.regression.linear2.learner.LinReg2LearnerSettings.CFG_MISSING_VALUE_HANDLING;
import static org.knime.base.node.mine.regression.linear2.learner.LinReg2LearnerSettings.CFG_OFFSET_VALUE;
import static org.knime.base.node.mine.regression.linear2.learner.LinReg2LearnerSettings.CFG_SCATTER_PLOT_FIRST_ROW;
import static org.knime.base.node.mine.regression.linear2.learner.LinReg2LearnerSettings.CFG_SCATTER_PLOT_ROW_COUNT;
import static org.knime.base.node.mine.regression.linear2.learner.LinReg2LearnerSettings.CFG_TARGET;

import org.knime.base.node.mine.regression.MissingValueHandling;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.layout.After;
import org.knime.node.parameters.layout.Layout;
import org.knime.node.parameters.layout.Section;
import org.knime.node.parameters.migration.LoadDefaultsForAbsentFields;
import org.knime.node.parameters.persistence.NodeParametersPersistor;
import org.knime.node.parameters.persistence.Persist;
import org.knime.node.parameters.persistence.Persistor;
import org.knime.node.parameters.persistence.legacy.LegacyColumnFilterPersistor;
import org.knime.node.parameters.updates.Effect;
import org.knime.node.parameters.updates.Effect.EffectType;
import org.knime.node.parameters.updates.EffectPredicate;
import org.knime.node.parameters.updates.EffectPredicateProvider;
import org.knime.node.parameters.updates.ParameterReference;
import org.knime.node.parameters.updates.ValueReference;
import org.knime.node.parameters.widget.choices.ChoicesProvider;
import org.knime.node.parameters.widget.choices.Label;
import org.knime.node.parameters.widget.choices.RadioButtonsWidget;
import org.knime.node.parameters.widget.choices.filter.ColumnFilter;
import org.knime.node.parameters.widget.choices.util.AllColumnsProvider;
import org.knime.node.parameters.widget.choices.util.ColumnSelectionUtil;
import org.knime.node.parameters.widget.choices.util.CompatibleColumnsProvider.DoubleColumnsProvider;
import org.knime.node.parameters.widget.number.NumberInputWidget;
import org.knime.node.parameters.widget.number.NumberInputWidgetValidation.MinValidation.IsPositiveIntegerValidation;

/**
 * Node parameters for Linear Regression Learner.
 *
 * @author Benjamin Moser, KNIME GmbH, Konstanz, Germany
 * @author AI Migration Pipeline v1.1
 */
@LoadDefaultsForAbsentFields
final class LinReg2LearnerNodeFactory2Parameters implements NodeParameters {

    LinReg2LearnerNodeFactory2Parameters() {
    }

    LinReg2LearnerNodeFactory2Parameters(final NodeParametersInput context) {
        m_columnFilter =
            new ColumnFilter(ColumnSelectionUtil.getAllColumnsOfFirstPort(context)).withIncludeUnknownColumns();
        var possibleTargets = context.getInTableSpec(0).stream().flatMap(DataTableSpec::stream)
            .filter(spec -> spec.getType().isCompatible(DoubleValue.class)).map(DataColumnSpec::getName).toList();
        if (!possibleTargets.isEmpty()) {
            m_targetColumn = possibleTargets.get(0);
        }
    }

    @Section(title = "Input Configuration")
    interface InputConfigurationSection {
    }

    @Section(title = "Regression Properties")
    @After(InputConfigurationSection.class)
    interface RegressionPropertiesSection {
    }

    @Section(title = "Missing Values in Input Data")
    @After(RegressionPropertiesSection.class)
    interface MissingValuesSection {
    }

    @Section(title = "Scatter Plot View")
    @After(MissingValuesSection.class)
    interface ScatterPlotSection {
    }

    @SuppressWarnings("restriction")
    static final class ColumnFilterPersistor extends LegacyColumnFilterPersistor {
        ColumnFilterPersistor() {
            super(CFG_COLUMN_FILTER);
        }
    }

    interface UsePredefinedOffsetRef extends ParameterReference<Boolean> {
    }

    static final class UsePredefinedOffsetPredicate implements EffectPredicateProvider {
        @Override
        public EffectPredicate init(final PredicateInitializer initializer) {
            return initializer.getBoolean(UsePredefinedOffsetRef.class).isTrue();
        }
    }

    static final class NegateBooleanPersistor implements NodeParametersPersistor<Boolean> {
        @Override
        public Boolean load(final NodeSettingsRO settings) throws InvalidSettingsException {
            return !settings.getBoolean(CFG_INCLUDE_CONSTANT, true);
        }

        @Override
        public void save(final Boolean usePredefinedOffset, final NodeSettingsWO settings) {
            settings.addBoolean(CFG_INCLUDE_CONSTANT, !Boolean.TRUE.equals(usePredefinedOffset));
        }

        @Override
        public String[][] getConfigPaths() {
            return new String[][]{{CFG_INCLUDE_CONSTANT}};
        }
    }

    enum MissingValueMode {
            @Label(value = "Ignore rows with missing values.", description = """
                    Skips rows containing missing values so the regression model is built only on complete rows.
                    """)
            IGNORE(MissingValueHandling.ignore),

            @Label(value = "Fail on observing missing values.", description = """
                    Stops execution with an error if missing values occur in the input data.
                    """)
            FAIL(MissingValueHandling.fail);

        MissingValueMode(final MissingValueHandling delegate) {
            m_delegate = delegate;
        }

        MissingValueHandling toLegacy() {
            return m_delegate;
        }

        static MissingValueMode fromLegacy(final MissingValueHandling handling) {
            return handling == MissingValueHandling.ignore ? IGNORE : FAIL;
        }

        private final MissingValueHandling m_delegate;
    }

    static final class MissingValueHandlingPersistor implements NodeParametersPersistor<MissingValueMode> {
        @Override
        public MissingValueMode load(final NodeSettingsRO settings) throws InvalidSettingsException {
            var legacyValue = settings.getString(CFG_MISSING_VALUE_HANDLING, MissingValueHandling.ignore.name());
            return MissingValueMode.fromLegacy(MissingValueHandling.valueOf(legacyValue));
        }

        @Override
        public void save(final MissingValueMode value, final NodeSettingsWO settings) {
            settings.addString(CFG_MISSING_VALUE_HANDLING, value.toLegacy().name());
        }

        @Override
        public String[][] getConfigPaths() {
            return new String[][]{{CFG_MISSING_VALUE_HANDLING}};
        }
    }

    @Layout(InputConfigurationSection.class)
    @Widget(title = "Target", description = """
            Select the numeric target column (response variable) the regression should predict.
            """)
    @Persist(configKey = CFG_TARGET)
    @ChoicesProvider(DoubleColumnsProvider.class)
    String m_targetColumn;

    @Layout(InputConfigurationSection.class)
    @Widget(title = "Values", description = """
            Choose the independent variables to include in the regression model.
            Nominal columns are converted into dummy variables automatically,
            as described in
            <a href="http://en.wikipedia.org/wiki/Categorical_variable#Categorical_variables_in_regression">
            Categorical variables in regression</a>.
            """)
    @Persistor(ColumnFilterPersistor.class)
    @ChoicesProvider(AllColumnsProvider.class)
    ColumnFilter m_columnFilter = new ColumnFilter();

    @Layout(RegressionPropertiesSection.class)
    @Widget(title = "Predefined offset value", description = """
            Provide a user-defined intercept instead of estimating the constant term. When enabled, the specified offset
            is used as intercept and the model does not estimate a constant.
            """)
    @Persistor(NegateBooleanPersistor.class)
    @ValueReference(UsePredefinedOffsetRef.class)
    boolean m_usePredefinedOffset;

    @Layout(RegressionPropertiesSection.class)
    @Widget(title = "Offset value", description = """
            Enter the constant term used when a predefined offset is applied to the regression model.
            """)
    @Persist(configKey = CFG_OFFSET_VALUE)
    @Effect(predicate = UsePredefinedOffsetPredicate.class, type = EffectType.SHOW)
    double m_offsetValue;

    @Layout(MissingValuesSection.class)
    @Widget(title = "Missing values in input data", description = """
            Decide whether rows containing missing values should be ignored or cause the node execution to abort.
            """)
    @RadioButtonsWidget
    @Persistor(MissingValueHandlingPersistor.class)
    MissingValueMode m_missingValueHandling = MissingValueMode.FAIL;

    @Layout(ScatterPlotSection.class)
    @Widget(title = "First row", description = """
            Specify the index of the first row that is available for the scatter plot view. Counting starts at 1.
            """)
    @Persist(configKey = CFG_SCATTER_PLOT_FIRST_ROW)
    @NumberInputWidget(minValidation = IsPositiveIntegerValidation.class)
    int m_scatterPlotFirstRow = 1;

    @Layout(ScatterPlotSection.class)
    @Widget(title = "Row count", description = """
            Define how many rows should be provided to the scatter plot view.
            """)
    @Persist(configKey = CFG_SCATTER_PLOT_ROW_COUNT)
    @NumberInputWidget(minValidation = IsPositiveIntegerValidation.class)
    int m_scatterPlotRowCount = 20000;
}
