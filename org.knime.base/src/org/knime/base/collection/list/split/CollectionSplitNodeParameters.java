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

package org.knime.base.collection.list.split;

import java.util.Optional;

import org.knime.base.collection.list.split.CollectionSplitSettings.CountElementsPolicy;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.collection.CollectionDataValue;
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
import org.knime.node.parameters.updates.ParameterReference;
import org.knime.node.parameters.updates.ValueProvider;
import org.knime.node.parameters.updates.ValueReference;
import org.knime.node.parameters.updates.legacy.ColumnNameAutoGuessValueProvider;
import org.knime.node.parameters.widget.choices.ChoicesProvider;
import org.knime.node.parameters.widget.choices.RadioButtonsWidget;
import org.knime.node.parameters.widget.choices.util.ColumnSelectionUtil;
import org.knime.node.parameters.widget.choices.util.CompatibleColumnsProvider;

/**
 * Node parameters for Split Collection Column.
 *
 * @author Magnus Gohm, KNIME GmbH, Konstanz, Germany
 * @author AI Migration Pipeline v1.2
 */
@LoadDefaultsForAbsentFields
@SuppressWarnings("restriction")
final class CollectionSplitNodeParameters implements NodeParameters {

    @Persist(configKey = CollectionSplitSettings.CFG_COLLECTION_COL_NAME)
    @Widget(title = "Column selection", description = "Select the column containing the collection value.")
    @ChoicesProvider(CollectionColumnProvider.class)
    @ValueProvider(CollectionColumnNameProvider.class)
    @ValueReference(CollectionColumnNameRef.class)
    String m_collectionColName;

    static final class CollectionColumnNameRef implements ParameterReference<String> {
    }

    @Persist(configKey = CollectionSplitSettings.CFG_REPLACE_INPUT_COLUMN)
    @Widget(title = "Replace input column", description = """
            Select this when the column containing the collection is to be removed from the output.
            """)
    boolean m_replaceInputColumn;

    @Persist(configKey = CollectionSplitSettings.CFG_DETERMINE_MOST_SPECIFIC_DATA_TYPE)
    @Widget(title = "Determine most specific type", description = """
            The collection column often contains only general type information to the individual elements. Selecting
            this option will determine the most specific type of the newly appended columns based on the actual content.
             If unsure, keep this option selected.
            """)
    boolean m_determineMostSpecificDataType;

    @Persistor(CountElementsPolicyPersistor.class)
    @Widget(title = "Element count policy", description = """
            Select the policy on how to determine how many elements the input column contains.
            """)
    @RadioButtonsWidget
    CountElementsPolicy m_countElementsPolicy = CountElementsPolicy.BestEffort;

    static final class CollectionColumnProvider extends CompatibleColumnsProvider {

        CollectionColumnProvider() {
            super(CollectionDataValue.class);
        }

    }

    static final class CollectionColumnNameProvider extends ColumnNameAutoGuessValueProvider {

        protected CollectionColumnNameProvider() {
            super(CollectionColumnNameRef.class);
        }

        @Override
        protected Optional<DataColumnSpec> autoGuessColumn(final NodeParametersInput parametersInput) {
            final var compatibleColumns =
                    ColumnSelectionUtil.getCompatibleColumnsOfFirstPort(parametersInput, CollectionDataValue.class);
            return compatibleColumns.isEmpty() ? Optional.empty() :
                Optional.of(compatibleColumns.get(compatibleColumns.size() - 1));
        }

    }

    static final class CountElementsPolicyPersistor implements NodeParametersPersistor<CountElementsPolicy> {

        @Override
        public CountElementsPolicy load(final NodeSettingsRO settings) throws InvalidSettingsException {
            return CountElementsPolicy.getFromName(settings.getString(
                CollectionSplitSettings.CFG_COUNT_ELEMENTS_POLICY, CountElementsPolicy.BestEffort.name()));
        }

        @Override
        public void save(final CountElementsPolicy param, final NodeSettingsWO settings) {
            settings.addString(CollectionSplitSettings.CFG_COUNT_ELEMENTS_POLICY, param.name());
        }

        @Override
        public String[][] getConfigPaths() {
            return new String[][]{{CollectionSplitSettings.CFG_COUNT_ELEMENTS_POLICY}};
        }

    }

}
