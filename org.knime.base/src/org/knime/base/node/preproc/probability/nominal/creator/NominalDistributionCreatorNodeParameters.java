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

package org.knime.base.node.preproc.probability.nominal.creator;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import org.knime.base.node.preproc.probability.nominal.ExceptionHandling;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.NominalValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.webui.node.dialog.defaultdialog.internal.widget.TypedStringFilterWidgetInternal;
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
import org.knime.node.parameters.updates.ValueProvider;
import org.knime.node.parameters.updates.ValueReference;
import org.knime.node.parameters.updates.legacy.ColumnNameAutoGuessValueProvider;
import org.knime.node.parameters.updates.util.BooleanReference;
import org.knime.node.parameters.widget.choices.ChoicesProvider;
import org.knime.node.parameters.widget.choices.EnumChoicesProvider;
import org.knime.node.parameters.widget.choices.ValueSwitchWidget;
import org.knime.node.parameters.widget.choices.filter.ColumnFilter;
import org.knime.node.parameters.widget.choices.util.ColumnSelectionUtil;
import org.knime.node.parameters.widget.choices.util.CompatibleColumnsProvider;
import org.knime.node.parameters.widget.choices.util.CompatibleColumnsProvider.DoubleColumnsProvider;
import org.knime.node.parameters.widget.number.NumberInputWidget;
import org.knime.node.parameters.widget.number.NumberInputWidgetValidation.MinValidation.IsPositiveIntegerValidation;
import org.knime.node.parameters.widget.text.TextInputWidget;
import org.knime.node.parameters.widget.text.TextInputWidgetValidation.PatternValidation.IsNotBlankValidation;

/**
 * Node parameters for Nominal Probability Distribution Creator.
 *
 * @author Magnus Gohm, KNIME GmbH, Konstanz, Germany
 * @author AI Migration Pipeline v1.2
 */
@LoadDefaultsForAbsentFields
@SuppressWarnings("restriction")
final class NominalDistributionCreatorNodeParameters implements NodeParameters {

    @Section(title = "Numeric Column Selection")
    @Effect(predicate = IsStringColumnType.class, type = EffectType.HIDE)
    interface NumericColumnsSection {
    }

    @Section(title = "Output")
    @After(NumericColumnsSection.class)
    interface OutputSection {

    }

    @Widget(title = "Column type", description = """
            Choose whether to create probability distributions from numeric columns or a single string column.
            """)
    @Persistor(ColumnTypePersistor.class)
    @ValueReference(ColumnTypeRef.class)
    @ValueSwitchWidget
    ColumnType m_columnType = ColumnType.NUMERIC_COLUMN;

    @Widget(title = "String column", description = """
            Select a single string column with a valid domain to create a one-hot encoding probability distribution. \
            I.e., the number of distinct values in the string column will be the number of classes in the created \
            distribution and the string value of a cell will have probability 1 whereby all other possible string \
            values of a cell will have a probablity of 0. \
            """)
    @ChoicesProvider(NominalChoicesProvider.class)
    @Persist(configKey = NominalDistributionCreatorNodeModel.CFG_SINGLE_STRING_COLUMN)
    @Effect(predicate = IsStringColumnType.class, type = Effect.EffectType.SHOW)
    @ValueReference(StringColumnRef.class)
    @ValueProvider(StringColumnProvider.class)
    String m_stringColumn;

    @Layout(NumericColumnsSection.class)
    @Widget(title = "Numeric columns", description = """
            Move the columns that contain the probability values to the "Include" list.
            """)
    @ChoicesProvider(DoubleColumnsProvider.class)
    @TypedStringFilterWidgetInternal(hideTypeFilter = true)
    @Persistor(NumericColumnFilterPersistor.class)
    ColumnFilter m_columnFilter = new ColumnFilter();

    @Layout(NumericColumnsSection.class)
    @Widget(title = "Allow probabilities that sum up to 1 imprecisely", description = """
            If enabled, the probabilities must not sum up to 1 precisely. This might be helpful if there are, e.g., some
            rounding errors in the probability values.
            """)
    @Persist(configKey = NominalDistributionCreatorNodeModel.CFG_ENABLE_PRECISION)
    @ValueReference(AllowImpreciseProbabilitiesRef.class)
    boolean m_allowImpreciseProbabilities = true;

    @Layout(NumericColumnsSection.class)
    @Widget(title = "Precision (number of decimal digits)", description = """
            Defines the precision that the sum of the probabilities must have by restricting the number of decimal
            digits that must be precise. The sum is accepted if <i>abs(sum - 1) &lt;= 10^(-precision)</i> , e.g., if the
             sum is 0.999, it is only accepted with a precision of &lt;=2. The lower the specified number, the higher
             is the tolerance.
            """)
    @NumberInputWidget(minValidation = IsPositiveIntegerValidation.class)
    @Effect(predicate = AllowImpreciseProbabilities.class, type = Effect.EffectType.ENABLE)
    @Persist(configKey = NominalDistributionCreatorNodeModel.CFG_PRECISION)
    int m_precisionDigits = 4;

    @Layout(NumericColumnsSection.class)
    @Widget(title = "Invalid probability distribution handling", description = """
            Specify how to treat invalid probabilities. Invalid means, e.g., negative probabilities or probabilities
            that do not sum up to 1 (with respect to the specified precision). If <i>Fail</i> is selected, the node will
             fail. Otherwise, the node just gives a warning and puts missing values in the output for the corresponding
            rows.
            """)
    @ValueSwitchWidget
    @Persistor(InvalidDistributionHandlingPersistor.class)
    ExceptionHandling m_invalidDistributionHandling = ExceptionHandling.FAIL;

    @Layout(OutputSection.class)
    @Widget(title = "Output column name",
        description = "Specify the name of the created column containing the probability distribution.")
    @TextInputWidget(patternValidation = IsNotBlankValidation.class)
    @Persist(configKey = NominalDistributionCreatorNodeModel.CFG_COLUMN_NAME)
    String m_outputColumnName = "Probability Distribution";

    @Layout(OutputSection.class)
    @Widget(title = "Remove included columns", description = """
            If selected, the included numeric columns or the picked string column will be removed from the output.
            """)
    @Persist(configKey = NominalDistributionCreatorNodeModel.CFG_REMOVE_INCLUDED_COLUMNS)
    boolean m_removeIncludedColumns;

    @Layout(OutputSection.class)
    @Widget(title = "Missing value handling", description = """
            Specify how to treat a missing value in one of the input columns. If 'Fail' is selected, the node will fail.
             If 'Ignore' is selected, the node just gives a warning and puts missing values in the output for the
             corresponding rows. If 'Treat as zero' is selected, the missing value will be treated as 0.
             """)
    @ChoicesProvider(MissingValueHandlingChoicesProvider.class)
    @Persistor(MissingValueHandlingPersistor.class)
    MissingValueHandling m_missingValueHandling = MissingValueHandling.FAIL;

    static final class StringColumnRef implements ParameterReference<String> {
    }

    static final class ColumnTypeRef implements ParameterReference<ColumnType> {
    }

    static final class AllowImpreciseProbabilitiesRef implements BooleanReference {
    }

    static final class IsStringColumnType implements EffectPredicateProvider {

        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getEnum(ColumnTypeRef.class).isOneOf(ColumnType.STRING_COLUMN);
        }

    }

    static final class AllowImpreciseProbabilities implements EffectPredicateProvider {

        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getBoolean(AllowImpreciseProbabilitiesRef.class).isTrue();
        }

    }

    static final class StringColumnProvider extends ColumnNameAutoGuessValueProvider {

        protected StringColumnProvider() {
            super(StringColumnRef.class);
        }

        @Override
        protected Optional<DataColumnSpec> autoGuessColumn(final NodeParametersInput parametersInput) {
            final var compatibleColumns =
                ColumnSelectionUtil.getCompatibleColumnsOfFirstPort(parametersInput, NominalValue.class);
            return compatibleColumns.isEmpty() ? Optional.empty()
                : Optional.of(compatibleColumns.get(compatibleColumns.size() - 1));
        }

    }

    static final class NominalChoicesProvider extends CompatibleColumnsProvider {

        NominalChoicesProvider() {
            super(NominalValue.class);
        }

    }

    static final class MissingValueHandlingChoicesProvider implements EnumChoicesProvider<MissingValueHandling> {

        Supplier<ColumnType> m_columnTypeSupplier;

        @Override
        public void init(final StateProviderInitializer initializer) {
            initializer.computeBeforeOpenDialog();
            m_columnTypeSupplier = initializer.computeFromValueSupplier(ColumnTypeRef.class);
        }

        @Override
        public List<MissingValueHandling> choices(final NodeParametersInput context) {
            final var columnType = m_columnTypeSupplier.get();
            if (columnType == ColumnType.STRING_COLUMN) {
                return List.of(MissingValueHandling.FAIL, MissingValueHandling.IGNORE);
            } else {
                return List.of(MissingValueHandling.FAIL, MissingValueHandling.IGNORE, MissingValueHandling.ZERO);
            }
        }

    }

    static final class ColumnTypePersistor implements NodeParametersPersistor<ColumnType> {

        @Override
        public ColumnType load(final NodeSettingsRO settings) throws InvalidSettingsException {
            return ColumnType.getFromValue(settings.getString(NominalDistributionCreatorNodeModel.CFG_COLUMN_TYPE,
                ColumnType.NUMERIC_COLUMN.name()));
        }

        @Override
        public void save(final ColumnType param, final NodeSettingsWO settings) {
            settings.addString(NominalDistributionCreatorNodeModel.CFG_COLUMN_TYPE, param.name());
        }

        @Override
        public String[][] getConfigPaths() {
            return new String[][]{{NominalDistributionCreatorNodeModel.CFG_COLUMN_TYPE}};
        }

    }

    static final class NumericColumnFilterPersistor extends LegacyColumnFilterPersistor {
        NumericColumnFilterPersistor() {
            super(NominalDistributionCreatorNodeModel.CFG_COLUMN_FILTER);
        }
    }

    static final class InvalidDistributionHandlingPersistor implements NodeParametersPersistor<ExceptionHandling> {

        @Override
        public ExceptionHandling load(final NodeSettingsRO settings) throws InvalidSettingsException {
            return ExceptionHandling.getFromValue(
                settings.getString(NominalDistributionCreatorNodeModel.CFG_INVALID_DISTRIBUTION_HANDLING,
                    ExceptionHandling.FAIL.name()));
        }

        @Override
        public void save(final ExceptionHandling param, final NodeSettingsWO settings) {
            settings.addString(NominalDistributionCreatorNodeModel.CFG_INVALID_DISTRIBUTION_HANDLING, param.name());
        }

        @Override
        public String[][] getConfigPaths() {
            return new String[][]{{NominalDistributionCreatorNodeModel.CFG_INVALID_DISTRIBUTION_HANDLING}};
        }

    }

    static final class MissingValueHandlingPersistor implements NodeParametersPersistor<MissingValueHandling> {

        @Override
        public MissingValueHandling load(final NodeSettingsRO settings) throws InvalidSettingsException {
            return MissingValueHandling.getFromValue(settings.getString(
                NominalDistributionCreatorNodeModel.CFG_MISSING_VALUE_HANDLING, MissingValueHandling.FAIL.name()));
        }

        @Override
        public void save(final MissingValueHandling param, final NodeSettingsWO settings) {
            settings.addString(NominalDistributionCreatorNodeModel.CFG_MISSING_VALUE_HANDLING, param.name());
        }

        @Override
        public String[][] getConfigPaths() {
            return new String[][]{{NominalDistributionCreatorNodeModel.CFG_MISSING_VALUE_HANDLING}};
        }

    }

}
