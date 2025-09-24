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

package org.knime.base.node.preproc.columntogrid2;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import org.knime.core.data.NominalValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.migration.LoadDefaultsForAbsentFields;
import org.knime.node.parameters.persistence.NodeParametersPersistor;
import org.knime.node.parameters.persistence.Persist;
import org.knime.node.parameters.persistence.Persistor;
import org.knime.node.parameters.persistence.legacy.LegacyColumnFilterPersistor;
import org.knime.node.parameters.widget.OptionalWidget;
import org.knime.node.parameters.widget.OptionalWidget.DefaultValueProvider;
import org.knime.node.parameters.widget.choices.ChoicesProvider;
import org.knime.node.parameters.widget.choices.TypedStringChoice;
import org.knime.node.parameters.widget.choices.filter.ColumnFilter;
import org.knime.node.parameters.widget.choices.filter.ColumnFilterWidget;
import org.knime.node.parameters.widget.choices.util.AllColumnsProvider;
import org.knime.node.parameters.widget.choices.util.CompatibleColumnsProvider;
import org.knime.node.parameters.widget.number.NumberInputWidget;
import org.knime.node.parameters.widget.number.NumberInputWidgetValidation.MinValidation.IsPositiveIntegerValidation;

/**
 * Node parameters for Column to Grid.
 *
 * @author Magnus Gohm, KNIME GmbH, Konstanz, Germany
 * @author AI Migration Pipeline v1.1
 */
@LoadDefaultsForAbsentFields
@SuppressWarnings("restriction")
final class ColumnToGrid2NodeParameters implements NodeParameters {

    /**
     *
     */
    ColumnToGrid2NodeParameters() {
        super();
    }

    ColumnToGrid2NodeParameters(final NodeParametersInput input) {
        final var inSpecs = input.getInTableSpecs();
        final var spec = inSpecs[0];
        if (spec == null) {
            m_columnFilter = new ColumnFilter();
        } else {
            final var guessedColumnName = ColumnToGrid2Configuration.autoGuessIncludeColumns(spec);
            if (guessedColumnName == null) {
                m_columnFilter = new ColumnFilter();
            } else {
                m_columnFilter =new ColumnFilter(List.of(spec.getColumnSpec(guessedColumnName[0])));
            }
        }
    }

    // ====== Grid Column Count

    @Widget(title = "Grid column count", description = """
            The number of grid columns, this should be a relatively small number.
            """)
    @NumberInputWidget(minValidation = IsPositiveIntegerValidation.class)
    @Persist(configKey = ColumnToGrid2NodeModel.GRID_COLUMN_COUNT)
    int m_gridColumnCount = 4;

    // ====== Column Filter Settings

    static final class ColumnFilterPersistor extends LegacyColumnFilterPersistor {
        ColumnFilterPersistor() {
            super(ColumnToGrid2NodeModel.COLUMN_FILTER);
        }
    }

    @Widget(title = "Columns to include", description = """
            Select the column(s) that are to be displayed in a grid. If multiple columns are selected, the
            entire set will constitute a grid column.
            """)
    @ColumnFilterWidget(choicesProvider = AllColumnsProvider.class)
    @Persistor(ColumnFilterPersistor.class)
    ColumnFilter m_columnFilter = new ColumnFilter();

    // ====== Grouping Settings

    @Widget(title = "Group column", description = """
            Select this option and choose a group column in order to separate input rows that do not belong
            to the same group. This is useful when visualizing, e.g. clustering results, whereby records from
            different clusters are represented by different output rows. The group column will be another
            column in the output table.
            """)
    @ChoicesProvider(NominalColumnsProvider.class)
    @OptionalWidget(defaultProvider = GroupColumnNameDefaultProvider.class)
    @Persistor(value = GroupColumnPersitor.class)
    Optional<String> m_groupColumn = Optional.empty();

    static final class NominalColumnsProvider extends CompatibleColumnsProvider {
        protected NominalColumnsProvider() {
            super(NominalValue.class);
        }
    }

    static final class GroupColumnNameDefaultProvider implements DefaultValueProvider<String> {

        private Supplier<List<TypedStringChoice>> m_nominalColumns;

        @Override
        public void init(final StateProviderInitializer i) {
            m_nominalColumns = i.computeFromProvidedState(NominalColumnsProvider.class);
            i.computeBeforeOpenDialog();
        }

        @Override
        public String computeState(final NodeParametersInput context) {
            final var current = m_nominalColumns.get();

            if (current == null || current.isEmpty()) {
                return "";
            }

            // Return the first column of nominal type as default for the group column.
            return current.get(0).id();
        }

    }

    static final class GroupColumnPersitor implements NodeParametersPersistor<Optional<String>> {

        @Override
        public Optional<String> load(final NodeSettingsRO settings) throws InvalidSettingsException {
            final var groupColumn = settings.getString(ColumnToGrid2NodeModel.GROUP_COLUMN, null);
            return Optional.ofNullable(groupColumn);
        }

        @Override
        public void save(final Optional<String> param, final NodeSettingsWO settings) {
            settings.addString(ColumnToGrid2NodeModel.GROUP_COLUMN, param.orElse(null));
        }

        @Override
        public String[][] getConfigPaths() {
            return new String[][]{{ColumnToGrid2NodeModel.GROUP_COLUMN}};
        }

    }

}
