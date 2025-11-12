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

package org.knime.base.node.preproc.datavalidator;

import org.knime.base.node.preproc.datavalidator.DataValidatorColConfiguration.ColumnExistenceHandling;
import org.knime.base.node.preproc.datavalidator.DataValidatorColConfiguration.DataTypeHandling;
import org.knime.base.node.preproc.datavalidator.DataValidatorColConfiguration.DomainHandling;
import org.knime.base.node.preproc.datavalidator.DataValidatorConfiguration.RejectBehavior;
import org.knime.base.node.preproc.datavalidator.DataValidatorConfiguration.UnknownColumnHandling;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.webui.node.dialog.defaultdialog.internal.widget.PersistWithin;
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
import org.knime.node.parameters.updates.Effect;
import org.knime.node.parameters.updates.Effect.EffectType;
import org.knime.node.parameters.updates.EffectPredicate;
import org.knime.node.parameters.updates.EffectPredicateProvider;
import org.knime.node.parameters.widget.choices.Label;
import org.knime.node.parameters.widget.choices.RadioButtonsWidget;
import org.knime.node.parameters.widget.choices.ValueSwitchWidget;
import org.knime.node.parameters.widget.choices.util.ColumnSelectionUtil;

/**
 * Node parameters for Table Validator (Reference).
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 * @author AI Migration Pipeline v1.2
 */
@LoadDefaultsForAbsentFields
@SuppressWarnings("restriction")
class DataValidatorSpecNodeParameters implements NodeParameters {

    // static keys for some nested settings
    private static final String KEY_1 = "individual_settings";

    private static final String KEY_2 = "0";

    DataValidatorSpecNodeParameters() {
        this((DataTableSpec)null);
    }

    DataValidatorSpecNodeParameters(final NodeParametersInput context) {
        this(context.getInTableSpec(0).orElse(null));
    }

    DataValidatorSpecNodeParameters(final DataTableSpec spec) {
        m_columnNames = spec == null ? new String[0] : spec.getColumnNames();
    }

    // ===== SECTIONS =====

    @Section(title = "Structure Validation")
    interface StructureValidationSection {
    }

    @Section(title = "Data Validation")
    @After(StructureValidationSection.class)
    interface DataValidationSection {
    }

    @Section(title = "Output")
    @After(DataValidationSection.class)
    interface OutputSection {
    }

    // ===== STRUCTURE VALIDATION SECTION =====

    @Widget(title = "Column name matching", description = """
            Controls what counts as a column name match between the input table and the reference table.
            If 'case insensitive' is choosen, it still tries to find an exactly (case sensitively) matching column
            name first, and then falls back to case insensitive matching.
            """)
    @ValueSwitchWidget
    @Layout(StructureValidationSection.class)
    @PersistWithin({KEY_1, KEY_2})
    @Persistor(ColumnNameMatchingLegacyPersistor.class)
    ColumnNameMatchingEnum m_columnNameMatching = ColumnNameMatchingEnum.CASE_SENSITIVE;

    @Widget(title = "If a column is missing in the table", description = """
            Ensures that the reference columns exist in the input table.
            If case insensitive name matching is selected, the first matching column will satisfy this condition.
            """)
    @Layout(StructureValidationSection.class)
    @RadioButtonsWidget
    @PersistWithin({KEY_1, KEY_2})
    @Persist(configKey = DataValidatorColConfiguration.CFG_COLUMN_MISSING_HANDLING)
    ColumnExistenceHandling m_missingColumnHandling = ColumnExistenceHandling.FAIL;

    @Widget(title = "If there is an additional column in the table", description = """
            Specifies how to handle columns which are not included in the reference table but in the table to validate.
            Additional columns can cause the validation to fail, be removed, or moved to the end of the table.
            """)
    @Layout(StructureValidationSection.class)
    @ValueSwitchWidget
    @Persist(configKey = DataValidatorConfiguration.CFG_REMOVE_UNKNOWN_COLUMNS)
    UnknownColumnHandling m_additionalColumnsHandling = UnknownColumnHandling.REJECT;

    @Widget(title = "If data type does not match", description = "Ensures a correct data type.")
    @Layout(StructureValidationSection.class)
    @ValueSwitchWidget
    @PersistWithin({KEY_1, KEY_2})
    @Persist(configKey = DataValidatorColConfiguration.CFG_DATA_TYPE_HANDLING)
    DataTypeHandling m_dataTypeHandling = DataTypeHandling.FAIL;

    @PersistWithin({KEY_1, KEY_2})
    @Persist(configKey = DataValidatorColConfiguration.CFG_COL_NAMES)
    String[] m_columnNames = new String[0];

    // ===== DATA VALIDATION SECTION =====

    @Widget(title = "If there are missing values",
        description = "Validation fails if any of the columns contains missing values.")
    @Layout(DataValidationSection.class)
    @ValueSwitchWidget
    @PersistWithin({KEY_1, KEY_2})
    @Persistor(MissingValueHandlingLegacyPersistor.class)
    MissingValueHandlingEnum m_missingValueHandling = MissingValueHandlingEnum.IGNORE;

    @Widget(title = "If categoric value is not in the domain", description = """
            Allows one to optionally validate categoric values in columns against a set of allowed values.
            This option is only enabled if the reference column defines possible values.
            """)
    @Layout(DataValidationSection.class)
    @RadioButtonsWidget
    @PersistWithin({KEY_1, KEY_2})
    @Persistor(CategoricDomainHandlingLegacyPersistor.class)
    @Effect(predicate = InputTableHasDomainValues.class, type = EffectType.ENABLE)
    CategoricDomainHandlingEnum m_categoricDomainHandling = CategoricDomainHandlingEnum.IGNORE;

    @Widget(title = "If numeric value is outside the domain", description = """
            Checks if each data object is between min and max defined by the domain of the reference column.
            This option is only enabled if the reference column defines a numeric domain (min/max).
            """)
    @Layout(DataValidationSection.class)
    @RadioButtonsWidget
    @PersistWithin({KEY_1, KEY_2})
    @Persistor(NumericDomainHandlingLegacyPersistor.class)
    @Effect(predicate = InputTableHasDomainValues.class, type = EffectType.ENABLE)
    NumericDomainHandlingEnum m_numericDomainHandling = NumericDomainHandlingEnum.IGNORE;

    // ===== OUTPUT SECTION =====

    @Widget(title = "If validation fails", description = "Controls the effect of a failed validation.")
    @Layout(OutputSection.class)
    @ValueSwitchWidget
    @Persist(configKey = DataValidatorConfiguration.CFG_REJECTING_BEHAVIOR)
    RejectBehavior m_validationFailureBehavior = RejectBehavior.FAIL_NODE;

    // ===== ENUMS =====

    enum ColumnNameMatchingEnum {
            @Label(value = "Case sensitive", description = "Column names must match exactly")
            CASE_SENSITIVE,

            @Label(value = "Case insensitive", description = """
                    Also columns with a similar name will be considered to be validated according to this configuration.
                    """)
            CASE_INSENSITIVE
    }

    enum MissingValueHandlingEnum {
            @Label(value = "Ignore", description = "Missing values in columns are ignored")
            IGNORE,

            @Label(value = "Fail validation", description = "Fails the validation if a column contains missing values")
            FAIL
    }

    enum CategoricDomainHandlingEnum {
            @Label(value = "Ignore", description = "Categoric values are not validated")
            IGNORE,

            @Label(value = "Fail validation",
                description = "Fails validation if values are not in the domain of the reference column")
            FAIL,

            @Label(value = "Replace with missing values",
                description = "Replaces out-of-domain values with missing values")
            MISSING_VALUE
    }

    enum NumericDomainHandlingEnum {
            @Label(value = "Ignore", description = "Numeric values are not validated")
            IGNORE,

            @Label(value = "Fail validation",
                description = "Fails validation if numeric values are outside domain (min/max)")
            FAIL,

            @Label(value = "Replace with missing values",
                description = "Replaces out-of-domain values with missing values")
            MISSING_VALUE
    }

    // ===== CUSTOM PERSISTORS =====

    static final class ColumnNameMatchingLegacyPersistor implements NodeParametersPersistor<ColumnNameMatchingEnum> {

        @Override
        public ColumnNameMatchingEnum load(final NodeSettingsRO settings) throws InvalidSettingsException {
            return settings.getBoolean(DataValidatorColConfiguration.CFG_CASE_INSENSITIVE, false)
                ? ColumnNameMatchingEnum.CASE_INSENSITIVE : ColumnNameMatchingEnum.CASE_SENSITIVE;
        }

        @Override
        public void save(final ColumnNameMatchingEnum value, final NodeSettingsWO settings) {
            settings.addBoolean(DataValidatorColConfiguration.CFG_CASE_INSENSITIVE,
                ColumnNameMatchingEnum.CASE_INSENSITIVE == value);
        }

        @Override
        public String[][] getConfigPaths() {
            return new String[][]{{DataValidatorColConfiguration.CFG_CASE_INSENSITIVE}};
        }
    }

    static final class MissingValueHandlingLegacyPersistor
        implements NodeParametersPersistor<MissingValueHandlingEnum> {
        @Override
        public MissingValueHandlingEnum load(final NodeSettingsRO settings) throws InvalidSettingsException {
            return settings.getBoolean(DataValidatorColConfiguration.CFG_REJECT_ON_MISSING_VALUE, false)
                ? MissingValueHandlingEnum.FAIL : MissingValueHandlingEnum.IGNORE;
        }

        @Override
        public void save(final MissingValueHandlingEnum value, final NodeSettingsWO settings) {
            settings.addBoolean(DataValidatorColConfiguration.CFG_REJECT_ON_MISSING_VALUE,
                MissingValueHandlingEnum.FAIL == value);
        }

        @Override
        public String[][] getConfigPaths() {
            return new String[][]{{DataValidatorColConfiguration.CFG_REJECT_ON_MISSING_VALUE}};
        }
    }

    static final class CategoricDomainHandlingLegacyPersistor
        implements NodeParametersPersistor<CategoricDomainHandlingEnum> {

        @Override
        public CategoricDomainHandlingEnum load(final NodeSettingsRO settings) throws InvalidSettingsException {
            var handling = ConfigSerializationUtils.getEnum(settings,
                DataValidatorColConfiguration.CFG_DOMAIN_HANDLING_POSSIBLE_DOMAIN, DomainHandling.class);
            return switch (handling) {
                case NONE -> CategoricDomainHandlingEnum.IGNORE;
                case FAIL -> CategoricDomainHandlingEnum.FAIL;
                case MISSING_VALUE -> CategoricDomainHandlingEnum.MISSING_VALUE;
            };
        }

        @Override
        public void save(final CategoricDomainHandlingEnum obj, final NodeSettingsWO settings) {
            var handling = switch (obj) {
                case IGNORE -> DomainHandling.NONE;
                case FAIL -> DomainHandling.FAIL;
                case MISSING_VALUE -> DomainHandling.MISSING_VALUE;
            };
            ConfigSerializationUtils.addEnum(settings,
                DataValidatorColConfiguration.CFG_DOMAIN_HANDLING_POSSIBLE_DOMAIN, handling);
        }

        @Override
        public String[][] getConfigPaths() {
            return new String[][]{{DataValidatorColConfiguration.CFG_DOMAIN_HANDLING_POSSIBLE_DOMAIN}};
        }
    }

    static final class NumericDomainHandlingLegacyPersistor
        implements NodeParametersPersistor<NumericDomainHandlingEnum> {

        @Override
        public NumericDomainHandlingEnum load(final NodeSettingsRO settings) throws InvalidSettingsException {
            var handling = ConfigSerializationUtils.getEnum(settings,
                DataValidatorColConfiguration.CFG_DOMAIN_HANDLING_MIN_MAX, DomainHandling.class);
            return switch (handling) {
                case NONE -> NumericDomainHandlingEnum.IGNORE;
                case FAIL -> NumericDomainHandlingEnum.FAIL;
                case MISSING_VALUE -> NumericDomainHandlingEnum.MISSING_VALUE;
            };
        }

        @Override
        public void save(final NumericDomainHandlingEnum value, final NodeSettingsWO settings) {
            var handling = switch (value) {
                case IGNORE -> DomainHandling.NONE;
                case FAIL -> DomainHandling.FAIL;
                case MISSING_VALUE -> DomainHandling.MISSING_VALUE;
            };
            ConfigSerializationUtils.addEnum(settings, DataValidatorColConfiguration.CFG_DOMAIN_HANDLING_MIN_MAX,
                handling);
        }

        @Override
        public String[][] getConfigPaths() {
            return new String[][]{{DataValidatorColConfiguration.CFG_DOMAIN_HANDLING_MIN_MAX}};
        }
    }

    // ===== EFFECTS =====

    static final class InputTableHasDomainValues implements EffectPredicateProvider {

        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getConstant(input -> ColumnSelectionUtil.getAllColumns(input, 1).stream()
                .anyMatch(colSpec -> colSpec.getDomain().hasValues()));
        }
    }

}
