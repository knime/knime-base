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

package org.knime.base.collection.list.create2;

import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.migration.DefaultProvider;
import org.knime.node.parameters.migration.LoadDefaultsForAbsentFields;
import org.knime.node.parameters.migration.Migration;
import org.knime.node.parameters.persistence.Persist;
import org.knime.node.parameters.persistence.Persistor;
import org.knime.node.parameters.persistence.legacy.EnumBooleanPersistor;
import org.knime.node.parameters.persistence.legacy.LegacyColumnFilterPersistor;
import org.knime.node.parameters.widget.choices.Label;
import org.knime.node.parameters.widget.choices.ValueSwitchWidget;
import org.knime.node.parameters.widget.choices.filter.ColumnFilter;
import org.knime.node.parameters.widget.choices.filter.ColumnFilterWidget;
import org.knime.node.parameters.widget.choices.util.AllColumnsProvider;
import org.knime.node.parameters.widget.text.TextInputWidget;

/**
 * Node parameters for the Columns to Collection node aka Create Collection Column node
 *
 * @author Magnus Gohm, KNIME GmbH, Konstanz, Germany
 * @author AI Migration Pipeline v1.2
 */
@LoadDefaultsForAbsentFields
@SuppressWarnings("restriction")
final class CollectionCreate2NodeParameters implements NodeParameters {

    @Widget(title = "Collected columns",
        description = "Select the columns to be collected into a new collection column.")
    @ColumnFilterWidget(choicesProvider = AllColumnsProvider.class)
    @Persistor(ColumnFilterPersistor.class)
    ColumnFilter m_columnFilter = new ColumnFilter();

    @Widget(title = "Collection type", description = "Sets the collection column type.")
    @Persistor(CollectionTypePersistor.class)
    @ValueSwitchWidget
    CollectionType m_collectionType = CollectionType.LIST;

    @Widget(title = "Skip missing values",
        description = "If checked, missing values are skipped, i.e. not stored in collection cells.")
    @Persist(configKey = CollectionCreate2NodeModel.CFG_IGNORE_MISSING_VALUE)
    boolean m_ignoreMissing = CollectionCreate2NodeModel.DEFAULT_IGNORE_MISSING_VALUE;

    @Widget(title = "Output column name",
        description = "Specifies the name of the output column containing the collection cells.")
    @TextInputWidget
    @Persist(configKey = CollectionCreate2NodeModel.CFG_NEW_COL_NAME)
    @Migration(NewColumnNameMigration.class)
    String m_newColumnName = CollectionCreate2NodeModel.DEFAULT_NEW_COL_NAME;

    @Widget(title = "Remove collected columns",
        description = "If checked, collected columns are removed from output table.")
    @Persist(configKey = CollectionCreate2NodeModel.CFG_REMOVE_COLS)
    boolean m_removeColumns = CollectionCreate2NodeModel.DEFAULT_REMOVE_COLS;

    private static final class ColumnFilterPersistor extends LegacyColumnFilterPersistor {

        ColumnFilterPersistor() {
            super(CollectionCreate2NodeModel.CFG_INCLUDES);
        }

    }

    private static final class CollectionTypePersistor extends EnumBooleanPersistor<CollectionType> {

        CollectionTypePersistor() {
            super(CollectionCreate2NodeModel.CFG_CREATE_SET, CollectionType.class, CollectionType.SET);
        }

    }

    private static final class NewColumnNameMigration implements DefaultProvider<String> {

        @Override
        public String getDefault() {
            return CollectionCreate2NodeModel.DEFAULT_LEGACY_NEW_COLUMN_NAME;
        }

    }

    enum CollectionType {
            @Label(value = "List", description = "Creates a collection column of type List.")
            LIST,

            @Label(value = "Set (remove duplicates)",
                description = "Creates a collection column of type Set, duplicate values are removed.")
            SET;
    }

}
