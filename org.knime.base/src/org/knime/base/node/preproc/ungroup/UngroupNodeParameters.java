/*
 * ------------------------------------------------------------------------
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
 * -------------------------------------------------------------------
 *
 * History
 *   17.10.2008 (Tobias Koetter): created
 */

package org.knime.base.node.preproc.ungroup;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.collection.CollectionDataValue;
import org.knime.core.webui.node.dialog.defaultdialog.setting.filter.column.LegacyColumnFilterPersistor;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.layout.Before;
import org.knime.node.parameters.layout.Layout;
import org.knime.node.parameters.layout.Section;
import org.knime.node.parameters.persistence.Persist;
import org.knime.node.parameters.persistence.Persistor;
import org.knime.node.parameters.widget.choices.ChoicesProvider;
import org.knime.node.parameters.widget.choices.filter.ColumnFilter;
import org.knime.node.parameters.widget.choices.filter.TwinlistWidget;
import org.knime.node.parameters.widget.choices.util.FilteredInputTableColumnsProvider;

/**
 * Node parameters for the Ungroup node (WebUI) using DefaultNodeSettings.
 *
 * Preserves legacy config keys to ensure compatibility with existing workflows:
 * - "columnNames" for collection columns selection
 * - "removeCollectionCol" for remove collection column option
 * - "skipMissingValues" for skip missing values option
 * - "skipEmptyCollections" for skip empty collections option
 * - "enableHilite" for enable hiliting option
 *
 * @author Tobias Koetter, University of Konstanz
 */
@SuppressWarnings("restriction") // New Node UI is not yet API
final class UngroupNodeParameters implements NodeParameters {

    @Section(title = "Collection columns",
        description = "Select the collection columns to ungroup. Only collection columns are offered for selection.")
    @Before(AdditionalSettings.class)
    interface CollectionColumns {
    }

    @Section(title = "Additional settings",
        description = "Configure additional options for the ungrouping operation.")
    interface AdditionalSettings {
    }

    /**
     * Constructor for the ungroup node parameters.
     */
    UngroupNodeParameters() {
        // Default constructor
    }

    /**
     * Constructor with context for initializing default values based on input table.
     *
     * @param context the node parameters input context
     */
    UngroupNodeParameters(final NodeParametersInput context) {
        var spec = context.getInTableSpec(0);
        if (spec.isPresent()) {
            final var collectionColumns = getCollectionColumns(spec.get());
            m_collectionColumns = new ColumnFilter(collectionColumns);
        }
    }

    @Widget(title = "Collection columns to ungroup",
        description = "Select the collection columns that should be ungrouped. " +
            "For each list of collection values, a list of rows will be created with the values of the collection " +
            "in one column and all other columns given from the original row. " +
            "Move columns between the 'Columns to ungroup' and 'Available columns' lists. " +
            "Only collection columns are shown.")
    @Layout(CollectionColumns.class)
    @ChoicesProvider(CollectionColumnsProvider.class)
    @TwinlistWidget(includedLabel = "Columns to ungroup", excludedLabel = "Available columns")
    @Persistor(CollectionColumnsPersistor.class)
    ColumnFilter m_collectionColumns = new ColumnFilter();

    @Widget(title = "Remove selected collection column",
        description = "If enabled, the selected collection column will be removed from the result table. " +
            "If disabled, the original collection column will be kept in the output alongside the ungrouped values.")
    @Layout(AdditionalSettings.class)
    @Persist(configKey = "removeCollectionCol")
    boolean m_removeCollectionColumn = true;

    @Widget(title = "Skip missing values",
        description = "If enabled, rows with a missing value in all selected collection columns are skipped, " +
            "as well as missing values in a collection cell if they occur in all selected collection columns. " +
            "If disabled, missing values are processed normally.")
    @Layout(AdditionalSettings.class)
    @Persist(configKey = "skipMissingValues")
    boolean m_skipMissingValues = false;

    @Widget(title = "Skip empty collections",
        description = "If enabled, rows in which all selected collection columns are empty will be skipped. " +
            "If disabled, empty collections are processed normally.")
    @Layout(AdditionalSettings.class)
    @Persist(configKey = "skipEmptyCollections")
    boolean m_skipEmptyCollections = false;

    @Widget(title = "Enable hiliting",
        description = "If enabled, the hiliting of an input row results in hiliting all rows " +
            "(given in the collection cell) of the ungrouped output table. Conversely, if all output rows " +
            "(represented in one or more collection cells) are highlighted, the input row(s) are highlighted as well. " +
            "Depending on the number of rows, enabling this feature might consume a lot of memory.",
        advanced = true)
    @Layout(AdditionalSettings.class)
    @Persist(configKey = "enableHilite")
    boolean m_enableHiliting = false;

    /**
     * Choices provider that lists only collection columns from the first input table.
     */
    static final class CollectionColumnsProvider implements FilteredInputTableColumnsProvider {
        @Override
        public boolean isIncluded(final DataColumnSpec col) {
            return col.getType().isCompatible(CollectionDataValue.class);
        }

        @Override
        public int getInputTableIndex() {
            return 0; // first and only input port
        }
    }

    /**
     * Helper method to get collection columns from a data table spec.
     *
     * @param spec the data table spec
     * @return array of collection column names
     */
    private static String[] getCollectionColumns(final org.knime.core.data.DataTableSpec spec) {
        return spec.stream()
            .filter(col -> col.getType().isCompatible(CollectionDataValue.class))
            .map(DataColumnSpec::getName)
            .toArray(String[]::new);
    }

    /**
     * Persistor that stores/loads the column filter under the legacy key "columnNames"
     * to maintain compatibility with the existing UngroupNodeModel.
     */
    static final class CollectionColumnsPersistor extends LegacyColumnFilterPersistor {
        CollectionColumnsPersistor() {
            super("columnNames");
        }
    }
}
