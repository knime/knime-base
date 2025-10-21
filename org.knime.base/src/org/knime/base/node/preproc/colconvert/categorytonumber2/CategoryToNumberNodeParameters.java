
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

package org.knime.base.node.preproc.colconvert.categorytonumber2;

import java.util.List;
import java.util.Optional;

import org.knime.core.data.DataType;
import org.knime.core.data.IntValue;
import org.knime.core.data.NominalValue;
import org.knime.core.data.def.IntCell;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.webui.node.dialog.defaultdialog.internal.widget.TypedStringFilterWidgetInternal;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.migration.ConfigMigration;
import org.knime.node.parameters.migration.LoadDefaultsForAbsentFields;
import org.knime.node.parameters.migration.Migration;
import org.knime.node.parameters.migration.NodeParametersMigration;
import org.knime.node.parameters.persistence.NodeParametersPersistor;
import org.knime.node.parameters.persistence.Persist;
import org.knime.node.parameters.persistence.Persistor;
import org.knime.node.parameters.persistence.legacy.LegacyColumnFilterPersistor;
import org.knime.node.parameters.persistence.legacy.OptionalStringPersistor;
import org.knime.node.parameters.widget.OptionalWidget;
import org.knime.node.parameters.widget.OptionalWidget.DefaultValueProvider;
import org.knime.node.parameters.widget.choices.filter.ColumnFilter;
import org.knime.node.parameters.widget.choices.filter.ColumnFilterWidget;
import org.knime.node.parameters.widget.choices.util.ColumnSelectionUtil;
import org.knime.node.parameters.widget.choices.util.CompatibleColumnsProvider;
import org.knime.node.parameters.widget.number.NumberInputWidget;
import org.knime.node.parameters.widget.number.NumberInputWidgetValidation;
import org.knime.node.parameters.widget.text.TextInputWidget;

/**
 * Node parameters for Category to Number.
 *
 * @author Benjamin Moser, KNIME GmbH, Konstanz, Germany
 * @author AI Migration Pipeline v1.1
 */
@SuppressWarnings({"OptionalUsedAsFieldOrParameterType", "restriction"})
@LoadDefaultsForAbsentFields
final class CategoryToNumberNodeParameters implements NodeParameters {

    CategoryToNumberNodeParameters() {
        // Default constructor
    }

    CategoryToNumberNodeParameters(final NodeParametersInput input) {
        m_columns = new ColumnFilter(ColumnSelectionUtil.getCompatibleColumnsOfFirstPort(input, NominalValue.class))
            .withIncludeUnknownColumns();
    }

    private static final class ColumnFilterLegacyPersistor extends LegacyColumnFilterPersistor {
        ColumnFilterLegacyPersistor() {
            super("column-filter");
        }
    }

    private static final class NominalColumnsProvider extends CompatibleColumnsProvider {
        private NominalColumnsProvider() {
            super(NominalValue.class);
        }
    }

    @Persistor(CategoryToNumberNodeParameters.ColumnFilterLegacyPersistor.class)
    @ColumnFilterWidget(choicesProvider = NominalColumnsProvider.class)
    @Widget(title = "Columns to transform", description = """
            Select the columns to be converted from nominal (category) values to numbers. Only columns with nominal \
            data are available. Use the include/exclude lists to specify which columns should be processed. \
            """)
    @TypedStringFilterWidgetInternal(hideTypeFilter = true)
    ColumnFilter m_columns = new ColumnFilter();

    private final static class ColumnSuffixPersistor extends OptionalStringPersistor {

        ColumnSuffixPersistor() {
            super("append_columns", "column_suffix");
        }

    }

    static final class DefaultColumnSuffixProvider implements DefaultValueProvider<String> {
        @Override
        public String computeState(final NodeParametersInput parametersInput) {
            return " (to number)";
        }
    }

    @Widget(title = "Append suffix to column name", description = """
            If checked, append the given suffix to the names of the computed columns. \
            Otherwise the computed columns replace the columns in the include list.\
            """)
    @OptionalWidget(defaultProvider = DefaultColumnSuffixProvider.class)
    @TextInputWidget
    @Persistor(ColumnSuffixPersistor.class)
    Optional<String> m_columnSuffix = Optional.of(" (to number)");

    @Widget(title = "Start value", description = """
            The category in the first row will be mapped to this value. \
            """)
    @Persist(configKey = "start_index")
    int m_startIndex;

    @Widget(title = "Increment", description = """
            The i-th category is mapped to the value i * Increment + Start value.\
            """)
    @NumberInputWidget(minValidation = NumberInputWidgetValidation.MinValidation.IsPositiveIntegerValidation.class)
    @Persist(configKey = "increment")
    int m_increment = 1;

    @Widget(title = "Maximum number of categories", description = """
            Processing is interrupted for inputs with more categories.\
            """)
    @NumberInputWidget(minValidation = NumberInputWidgetValidation.MinValidation.IsPositiveIntegerValidation.class)
    @Persist(configKey = "max_categories")
    int m_maxCategories = 100;

    private static final String DEFAULT_VALUE_CFG_KEY = "default_value";

    static final class DefaultValuePersistor extends DataCellOptionalIntPersistor {
        DefaultValuePersistor() {
            super(DEFAULT_VALUE_CFG_KEY);
        }
    }

    static final class DefaultValueMigration extends DataCellOptionalIntMigration {

        DefaultValueMigration() {
            super(DEFAULT_VALUE_CFG_KEY);
        }

    }

    @Widget(title = "Use default value in PMML", description = """
            This value is used when the PMML model is applied. It defines the value used when the input is not \
            found in the mapping. If disabled, a missing cell is assigned in this case. \
            """)
    @Persistor(DefaultValuePersistor.class)
    @Migration(DefaultValueMigration.class)
    Optional<Integer> m_defaultValue = Optional.empty();

    private static final String MAP_MISSING_TO_CONFIG_KEY = "map_missing_to";

    static final class MapMissingToPersistor extends DataCellOptionalIntPersistor {
        MapMissingToPersistor() {
            super(MAP_MISSING_TO_CONFIG_KEY);
        }
    }

    static final class MapMissingToMigration extends DataCellOptionalIntMigration {

        MapMissingToMigration() {
            super(MAP_MISSING_TO_CONFIG_KEY);
        }

    }

    @Widget(title = "Map missing cells to number", description = """
            Missing cells are mapped to this value. If disabled, \
            missing cells will be mapped to missing cells. \
            """)
    @Persistor(MapMissingToPersistor.class)
    @Migration(MapMissingToMigration.class)
    Optional<Integer> m_mapMissingTo = Optional.empty();

    /**
     * Custom migration whose whole purpose is to enable still showing legacy flow variables since we do not allow
     * setting flow variables anymore for DataCells in the modern dialog.
     */
    private abstract static class DataCellOptionalIntMigration implements NodeParametersMigration<Optional<Integer>> {

        private final String m_configKey;

        DataCellOptionalIntMigration(final String configKey) {
            m_configKey = configKey;
        }

        @Override
        public List<ConfigMigration<Optional<Integer>>> getConfigMigrations() {
            return List.of(ConfigMigration.builder(settings -> (Optional<Integer>)null).withMatcher(settings -> false)
                .withDeprecatedConfigPath(m_configKey).build());
        }
    }

    /**
     * Custom persistor to handle DataCell <-> Optional<Integer> conversion for legacy compatibility.
     */
    private abstract static class DataCellOptionalIntPersistor implements NodeParametersPersistor<Optional<Integer>> {

        private final String m_configKey;

        DataCellOptionalIntPersistor(final String configKey) {
            m_configKey = configKey;
        }

        @Override
        public Optional<Integer> load(final NodeSettingsRO settings) throws InvalidSettingsException {
            var cell = settings.getDataCell(m_configKey);
            if (cell == null || cell.isMissing()) { // NOSONAR
                return Optional.empty();
            }
            if (cell.getType().isCompatible(IntValue.class)) {
                return Optional.of(((IntValue)cell).getIntValue());
            }
            throw new InvalidSettingsException(
                "Expected IntValue DataCell for key '" + m_configKey + "', but got: " + cell.getClass());
        }

        @Override
        public void save(final Optional<Integer> value, final NodeSettingsWO settings) {
            if (value != null && value.isPresent()) { // NOSONAR
                settings.addDataCell(m_configKey, new IntCell(value.get()));
            } else {
                settings.addDataCell(m_configKey, DataType.getMissingCell());
            }
        }

        @Override
        public String[][] getConfigPaths() {
            return new String[0][]; // We cannot set the flow variables for DataCells
        }
    }
}
