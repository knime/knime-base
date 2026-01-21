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

package org.knime.base.node.viz.crosstable;

import java.util.Arrays;
import java.util.Optional;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataValue;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.StringValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.webui.node.dialog.defaultdialog.setting.singleselection.NoneChoice;
import org.knime.core.webui.node.dialog.defaultdialog.setting.singleselection.StringOrEnum;
import org.knime.node.parameters.Advanced;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.migration.LoadDefaultsForAbsentFields;
import org.knime.node.parameters.persistence.NodeParametersPersistor;
import org.knime.node.parameters.persistence.Persist;
import org.knime.node.parameters.persistence.Persistor;
import org.knime.node.parameters.updates.ParameterReference;
import org.knime.node.parameters.updates.ValueProvider;
import org.knime.node.parameters.updates.ValueReference;
import org.knime.node.parameters.updates.legacy.ColumnNameAutoGuessValueProvider;
import org.knime.node.parameters.widget.choices.ChoicesProvider;
import org.knime.node.parameters.widget.choices.util.ColumnSelectionUtil;
import org.knime.node.parameters.widget.choices.util.CompatibleColumnsProvider;
import org.knime.node.parameters.widget.choices.util.CompatibleColumnsProvider.DoubleColumnsProvider;

/**
 * Node parameters for Crosstab.
 *
 * @author Magnus Gohm, KNIME GmbH, Konstanz, Germany
 * @author AI Migration Pipeline v1.2
 */
@LoadDefaultsForAbsentFields
@SuppressWarnings("restriction")
final class CrosstabNodeParameters implements NodeParameters {

    static final Class<? extends DataValue>[] SUPPORTED_TYPES = new Class[]{StringValue.class, DoubleValue.class};

    @Widget(title = "Row variable", description = "The input column used as the row variable in the cross-tabulation.")
    @Persist(configKey = CrosstabNodeSettings.ROW_VAR_COLUMN)
    @ChoicesProvider(StringOrDoubleColumnsProvider.class)
    @ValueProvider(RowVarColumnNameProvider.class)
    @ValueReference(RowVarColumnNameRef.class)
    String m_rowVarColumn;

    static final class RowVarColumnNameRef implements ParameterReference<String> {
    }

    @Widget(title = "Column variable",
        description = "The input column used as the column variable in the cross-tabulation.")
    @Persist(configKey = CrosstabNodeSettings.COL_VAR_COLUMN)
    @ChoicesProvider(StringOrDoubleColumnsProvider.class)
    @ValueProvider(ColVarColumnNameProvider.class)
    @ValueReference(ColVarColumnNameRef.class)
    String m_colVarColumn;

    static final class ColVarColumnNameRef implements ParameterReference<String> {
    }

    @Widget(title = "Weight column", description = """
            Applies a numeric weight for each record in the input causing the Crosstab node to treat each record as if
            it were repeated WEIGHT number of times.
            """)
    @Persistor(WeightColumnPersistor.class)
    @ChoicesProvider(DoubleColumnsProvider.class)
    StringOrEnum<NoneChoice> m_weightColumn = new StringOrEnum<>(NoneChoice.NONE);

    static final class WeightColumnNameRef implements ParameterReference<StringOrEnum<NoneChoice>> {
    }

    @Advanced
    @Widget(title = "Enable hiliting", description = """
            If enabled, the hiliting of a cell in the crosstab will hilite all cells with same categories in attached
            views. Depending on the number of rows, enabling this feature might consume a lot of memory.
            """)
    @Persist(configKey = CrosstabNodeSettings.ENALE_HILITING)
    boolean m_enableHiliting;

    // Unused parameter which is only visible in flow variable tab.
    @Persist(configKey = CrosstabNodeSettings.NAMING_VERSION)
    String m_namingVersion;

    static final class StringOrDoubleColumnsProvider extends CompatibleColumnsProvider {

        protected StringOrDoubleColumnsProvider() {
            super(Arrays.asList(SUPPORTED_TYPES));
        }

    }

    static final class RowVarColumnNameProvider extends ColumnNameAutoGuessValueProvider {

        protected RowVarColumnNameProvider() {
            super(RowVarColumnNameRef.class);
        }

        @Override
        protected Optional<DataColumnSpec> autoGuessColumn(final NodeParametersInput parametersInput) {
            return ColumnSelectionUtil.getFirstCompatibleColumnOfFirstPort(parametersInput, SUPPORTED_TYPES);
        }

    }

    static final class ColVarColumnNameProvider extends ColumnNameAutoGuessValueProvider {

        protected ColVarColumnNameProvider() {
            super(ColVarColumnNameRef.class);
        }

        @Override
        protected Optional<DataColumnSpec> autoGuessColumn(final NodeParametersInput parametersInput) {
            final var compatibleColumns =
                ColumnSelectionUtil.getCompatibleColumnsOfFirstPort(parametersInput, SUPPORTED_TYPES);
            if (compatibleColumns.isEmpty()) {
                return Optional.empty();
            }
            return compatibleColumns.size() == 1 ? Optional.of(compatibleColumns.get(0))
                : Optional.of(compatibleColumns.get(1));
        }

    }

    static final class WeightColumnPersistor implements NodeParametersPersistor<StringOrEnum<NoneChoice>> {

        @Override
        public StringOrEnum<NoneChoice> load(final NodeSettingsRO settings) throws InvalidSettingsException {
            String columnName = settings.getString(CrosstabNodeSettings.WEIGHT_COLUMN);
            if (columnName == null || columnName.isEmpty()) {
                return new StringOrEnum<>(NoneChoice.NONE);
            }
            return new StringOrEnum<>(columnName);
        }

        @Override
        public void save(final StringOrEnum<NoneChoice> param, final NodeSettingsWO settings) {
            Optional<NoneChoice> enumChoice = param.getEnumChoice();
            if (enumChoice.isPresent()) {
                settings.addString(CrosstabNodeSettings.WEIGHT_COLUMN, null);
            } else {
                settings.addString(CrosstabNodeSettings.WEIGHT_COLUMN, param.getStringChoice());
            }
        }

        @Override
        public String[][] getConfigPaths() {
            return new String[][]{{CrosstabNodeSettings.WEIGHT_COLUMN}};
        }

    }

}
