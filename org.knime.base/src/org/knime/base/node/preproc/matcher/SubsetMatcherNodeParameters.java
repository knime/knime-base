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

package org.knime.base.node.preproc.matcher;

import java.util.List;
import java.util.Optional;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.collection.CollectionDataValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.webui.node.dialog.defaultdialog.setting.singleselection.RowIDChoice;
import org.knime.core.webui.node.dialog.defaultdialog.setting.singleselection.StringOrEnum;
import org.knime.core.webui.node.dialog.defaultdialog.util.updates.StateComputationFailureException;
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
import org.knime.node.parameters.updates.legacy.AutoGuessValueProvider;
import org.knime.node.parameters.updates.legacy.ColumnNameAutoGuessValueProvider;
import org.knime.node.parameters.widget.choices.ChoicesProvider;
import org.knime.node.parameters.widget.choices.util.AllColumnsProvider;
import org.knime.node.parameters.widget.choices.util.ColumnSelectionUtil;
import org.knime.node.parameters.widget.choices.util.CompatibleColumnsProvider;
import org.knime.node.parameters.widget.number.NumberInputWidget;
import org.knime.node.parameters.widget.number.NumberInputWidgetValidation.MinValidation.IsNonNegativeValidation;

/**
 * Node parameters for Subset Matcher.
 *
 * @author Magnus Gohm, KNIME GmbH, Konstanz, Germany
 * @author AI Migration Pipeline v1.2
 */
@LoadDefaultsForAbsentFields
@SuppressWarnings("restriction")
final class SubsetMatcherNodeParameters implements NodeParameters {

    @Widget(title = "Subset column", description = """
            The column that contains the subsets to search for.
            """)
    @ChoicesProvider(CollectionColumnChoicesProviderPort0.class)
    @Persist(configKey = SubsetMatcherNodeModel.CFG_SUBSET_COLUMN)
    @ValueProvider(SubSetColumnProvider.class)
    @ValueReference(SubSetColumnRef.class)
    String m_subsetColumn;

    static final class SubSetColumnRef implements ParameterReference<String> {
    }

    @Widget(title = "ID column", description = """
            The id of the set to search in.
            """)
    @ChoicesProvider(AllColumnsProviderPort1.class)
    @Persistor(SetIdColumnPersistor.class)
    @ValueProvider(SetIdColumnProvider.class)
    @ValueReference(SetIdColumnRef.class)
    StringOrEnum<RowIDChoice> m_setIDColumn = new StringOrEnum<>(RowIDChoice.ROW_ID);

    static final class SetIdColumnRef implements ParameterReference<StringOrEnum<RowIDChoice>> {
    }

    @Widget(title = "Set column", description = """
            The column that contains sets to search in
            """)
    @ChoicesProvider(CollectionColumnChoicesProviderPort1.class)
    @Persist(configKey = SubsetMatcherNodeModel.CFG_SET_COLUMN)
    @ValueProvider(SetColColumnProvider.class)
    @ValueReference(SetColColumnRef.class)
    String m_setColumn;

    static final class SetColColumnRef implements ParameterReference<String> {
    }

    @Widget(title = "Append set column", description = """
            The matching set is appended if this option is ticked. A new row is created for each matching set.
            """)
    @Persist(configKey = SubsetMatcherNodeModel.CFG_APPEND_SET_LIST_COLUMN)
    boolean m_appendSetColumn = true;

    @Widget(title = "Maximum mismatches", description = """
            The maximum number of allowed mismatches, e.g. 1 allows for one item of the subset to be missing in the
            set. Default value is 0 where only sets match that contain all items of the subset.
            """)
    @NumberInputWidget(minValidation = IsNonNegativeValidation.class)
    @Persist(configKey = SubsetMatcherNodeModel.CFG_MAX_MISMATCHES)
    int m_maxMismatches;

    static final class CollectionColumnChoicesProviderPort0 extends CompatibleColumnsProvider {

        protected CollectionColumnChoicesProviderPort0() {
            super(List.of(CollectionDataValue.class));
        }

    }

    static final class CollectionColumnChoicesProviderPort1 extends CompatibleColumnsProvider {

        protected CollectionColumnChoicesProviderPort1() {
            super(List.of(CollectionDataValue.class));
        }

        @Override
        public int getInputTableIndex() {
            return 1;
        }

    }

    static final class AllColumnsProviderPort1 extends AllColumnsProvider {

        @Override
        public int getInputTableIndex() {
            return 1;
        }

    }

    static final class SubSetColumnProvider extends ColumnNameAutoGuessValueProvider {

        protected SubSetColumnProvider() {
            super(SubSetColumnRef.class);
        }

        @Override
        protected Optional<DataColumnSpec> autoGuessColumn(final NodeParametersInput parametersInput) {
            return ColumnSelectionUtil.getFirstCompatibleColumnOfFirstPort(parametersInput, CollectionDataValue.class);
        }

    }

    static final class SetColColumnProvider extends ColumnNameAutoGuessValueProvider {

        protected SetColColumnProvider() {
            super(SetColColumnRef.class);
        }

        @Override
        protected Optional<DataColumnSpec> autoGuessColumn(final NodeParametersInput parametersInput) {
            return ColumnSelectionUtil.getFirstCompatibleColumn(parametersInput, 1, CollectionDataValue.class);
        }

    }

    static class SetIdColumnProvider extends AutoGuessValueProvider<StringOrEnum<RowIDChoice>> {

        protected SetIdColumnProvider() {
            super(SetIdColumnRef.class);
        }

        @Override
        protected boolean isEmpty(final StringOrEnum<RowIDChoice> value) {
            if (value.getEnumChoice().isPresent()) {
                return false;
            }
            final var valueString = value.getStringChoice();
            return valueString == null || valueString.isEmpty();
        }

        Optional<DataColumnSpec> autoGuessColumn(final NodeParametersInput parametersInput) {
            final var compatibleColumns = ColumnSelectionUtil.getAllColumns(parametersInput, 1);
            return compatibleColumns.isEmpty() ? Optional.empty()
                : Optional.of(compatibleColumns.get(compatibleColumns.size() - 1));
        }

        @Override
        protected final StringOrEnum<RowIDChoice> autoGuessValue(final NodeParametersInput parametersInput)
            throws StateComputationFailureException {
            return autoGuessColumn(parametersInput).map(spec -> new StringOrEnum<RowIDChoice>(spec.getName()))
                .orElseThrow(StateComputationFailureException::new);
        }

    }

    static final class SetIdColumnPersistor implements NodeParametersPersistor<StringOrEnum<RowIDChoice>> {

        @Override
        public StringOrEnum<RowIDChoice> load(final NodeSettingsRO settings) throws InvalidSettingsException {
            final var idColumnSettings = settings.getNodeSettings(SubsetMatcherNodeModel.CFG_SET_ID_COLUMN);
            if (idColumnSettings.getBoolean(SubsetMatcherNodeModel.CFG_USE_ROW_ID, false)) {
                return new StringOrEnum<>(RowIDChoice.ROW_ID);
            }
            return new StringOrEnum<>(idColumnSettings.getString(SubsetMatcherNodeModel.CFG_COLUMN_NAME, ""));
        }

        @Override
        public void save(final StringOrEnum<RowIDChoice> value, final NodeSettingsWO settings) {
            final var idColumnSettings = settings.addNodeSettings(SubsetMatcherNodeModel.CFG_SET_ID_COLUMN);
            if (value.getEnumChoice().isPresent()) {
                idColumnSettings.addBoolean(SubsetMatcherNodeModel.CFG_USE_ROW_ID, true);
                idColumnSettings.addString(SubsetMatcherNodeModel.CFG_COLUMN_NAME, "");
            } else {
                idColumnSettings.addBoolean(SubsetMatcherNodeModel.CFG_USE_ROW_ID, false);
                idColumnSettings.addString(SubsetMatcherNodeModel.CFG_COLUMN_NAME, value.getStringChoice());
            }
        }

        @Override
        public String[][] getConfigPaths() {
            return new String[][]{{SubsetMatcherNodeModel.CFG_SET_ID_COLUMN, SubsetMatcherNodeModel.CFG_COLUMN_NAME},
                {SubsetMatcherNodeModel.CFG_SET_ID_COLUMN, SubsetMatcherNodeModel.CFG_USE_ROW_ID}};
        }

    }

}
