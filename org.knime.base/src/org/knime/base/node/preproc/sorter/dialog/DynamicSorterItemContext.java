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
 * ---------------------------------------------------------------------
 *
 * History
 *   20 Nov 2019 (Timmo Waller-Ehrat): created
 */
package org.knime.base.node.preproc.sorter.dialog;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.IntPredicate;
import java.util.stream.Collectors;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.port.PortObjectSpec;

/**
 * This class implements the RestorableDynamicItemContext interface for the sorter node.
 *
 * @author Timmo Waller-Ehrat, KNIME GmbH, Konstanz, Germany
 */
final class DynamicSorterItemContext
    implements RestorableDynamicItemContext<DynamicSortItem>, SelectionChangedListener {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(DynamicSorterItemContext.class);

    private final List<DynamicSortItem> m_items = new ArrayList<>();

    private DataTableSpec m_spec;

    private final String m_includeListKey;

    private final String m_sortOrderKey;

    private final String m_alphaNumCompKey;

    /**
     * @param includeListKey the key from/to which the includeList is loaded/saved
     * @param sortOrderKey the key from/to which the sortOrder is loaded/saved
     * @param alphaNumCompKey the key from/to which the alphaNumComp is loaded/saved
     *
     */
    public DynamicSorterItemContext(final String includeListKey, final String sortOrderKey,
        final String alphaNumCompKey) {
        m_includeListKey = includeListKey;
        m_sortOrderKey = sortOrderKey;
        m_alphaNumCompKey = Objects.requireNonNull(alphaNumCompKey);
    }

    @Override
    public DynamicSortItem createItem() {
        final List<DataColumnSpec> values = createColumnList();
        DynamicSortItem item = null;

        if (!values.isEmpty()) {
            final var firstColumn = values.get(0);
            // This also works for RowID, since it's included in the list as StringCell.TYPE
            // (see DynamicSorterPanel.ROWKEY).
            final var alphanumericComparisonDefault = compareAlphanumericByDefault(firstColumn.getType());
            item = new DynamicSortItem(m_items.size(), values, firstColumn, true, alphanumericComparisonDefault);
            item.addListener(this);
            m_items.add(item);
            updateComboBoxes();
        }

        return item;
    }

    @Override
    public boolean canCreateItem() {
        final List<DataColumnSpec> values = createColumnList();
        return !values.isEmpty();
    }

    private void createItem(final List<DataColumnSpec> values, final int selectedIndex, final boolean sortOrder,
        final boolean compareAlphanumerically) {
        if (!values.isEmpty()) {
            final var item = new DynamicSortItem(m_items.size(), values, values.get(selectedIndex), sortOrder,
                compareAlphanumerically);
            item.addListener(this);
            m_items.add(item);
        }
    }

    @Override
    public void remove(final DynamicSortItem removedItem) {
        m_items.remove(removedItem);
        for (int i = 0; i < m_items.size(); i++) {
            m_items.get(i).setID(i);
        }
        updateComboBoxes();
    }

    @Override
    public void swap(final DynamicSortItem first, final DynamicSortItem second) {
        final int firstIndex = m_items.indexOf(first);
        final int secondIndex = m_items.indexOf(second);

        m_items.get(firstIndex).setID(secondIndex);
        m_items.get(secondIndex).setID(firstIndex);
        Collections.swap(m_items, firstIndex, secondIndex);
    }

    @Override
    public List<DynamicSortItem> getItems() {
        return m_items;
    }

    @Override
    public void updateComboBoxes() {
        for (int i = 0; i < m_items.size(); i++) {
            m_items.get(i).updateComboBoxes(createColumnList());
        }
    }

    /**
     * Load the settings, if available
     *
     * @param settings the config settings to read from
     * @param spec the input specs
     */
    @Override
    public void load(final NodeSettingsRO settings, final PortObjectSpec[] spec) {
        m_spec = (DataTableSpec)spec[0];
        List<String> list = null;
        boolean[] sortOrder = null;

        if (settings.containsKey(m_includeListKey)) {
            try {
                String[] alist = settings.getStringArray(m_includeListKey);
                if (alist != null) {
                    list = Arrays.asList(alist);
                }
            } catch (InvalidSettingsException ise) {
                LOGGER.error(ise.getMessage(), ise);
            }
        }

        if (settings.containsKey(m_sortOrderKey)) {
            try {
                sortOrder = settings.getBooleanArray(m_sortOrderKey);
            } catch (InvalidSettingsException ise) {
                LOGGER.error(ise.getMessage(), ise);
            }
        }

        // old node instances don't get the new default
        IntPredicate compareSortKeyItemAtIndexAlphanumerically = sortKeyItemIndex -> false;
        if (settings.containsKey(m_alphaNumCompKey)) {
            try {
                final var fromSettings = settings.getBooleanArray(m_alphaNumCompKey);
                compareSortKeyItemAtIndexAlphanumerically = sortKeyItemIndex ->
                    sortKeyItemIndex < fromSettings.length && fromSettings[sortKeyItemIndex];
            } catch (InvalidSettingsException ise) {
                LOGGER.error("Could not load settings for alphanumeric comparison of strings", ise);
            }
        }

        if ((list != null && sortOrder != null)
                && (!list.isEmpty() || list.size() == sortOrder.length)) {
            m_items.clear();

            final List<DataColumnSpec> values = createColumnList();
            for (var i = 0; i < list.size(); i++) {
                final int selectedIndex = getIndexOfSpec(values, list.get(i));
                if (selectedIndex > -1) {
                    createItem(values, selectedIndex, sortOrder[i], compareSortKeyItemAtIndexAlphanumerically.test(i));
                }
            }
            updateComboBoxes();
        }
    }

    /**
     * Save settings
     *
     * @param settings the node settings to write into
     */
    @Override
    public void save(final NodeSettingsWO settings) {
        if (settings != null) {
            final List<String> inclList = getSelectedColumnNames();
            settings.addStringArray(m_includeListKey, inclList.toArray(new String[inclList.size()]));
            settings.addBooleanArray(m_sortOrderKey, getSortOrder());
            settings.addBooleanArray(m_alphaNumCompKey, getAlphanumComp());
        }
    }

    /**
     * @return list of all unused {@link DataColumnSpec}
     */
    private List<DataColumnSpec> createColumnList() {
        final List<DataColumnSpec> values = new ArrayList<>();
        final Set<DataColumnSpec> usedColumns = getSelectedColumnSpecs();

        if (!usedColumns.contains(DynamicSorterPanel.ROWKEY)) {
            values.add(0, DynamicSorterPanel.ROWKEY);
        }

        for (DataColumnSpec colSpec : m_spec) {
            if (!usedColumns.contains(colSpec)) {
                values.add(colSpec);
            }
        }

        return values;
    }

    private static int getIndexOfSpec(final List<DataColumnSpec> values, final String name) {
        for (int i = 0; i < values.size(); i++) {
            if (values.get(i).getName().equals(name)) {
                return i;
            }
        }
        return -1;
    }

    private Set<DataColumnSpec> getSelectedColumnSpecs() {
        return m_items.stream().map(DynamicSortItem::getSelectedColumn).collect(Collectors.toSet());
    }

    private List<String> getSelectedColumnNames() {
        return m_items.stream().map(item -> item.getSelectedColumn().getName()).collect(Collectors.toList());
    }

    private boolean[] getSortOrder() {
        boolean[] sortOrder = new boolean[m_items.size()];
        for (int i = 0; i < m_items.size(); i++) {
            sortOrder[i] = m_items.get(i).getSortOrder();
        }
        return sortOrder;
    }

    private boolean[] getAlphanumComp() {
        final var order = new boolean[m_items.size()];
        for (int i = 0; i < m_items.size(); i++) {
            order[i] = m_items.get(i).getAlphaNumComp();
        }
        return order;
    }

    private static boolean compareAlphanumericByDefault(final DataType type) {
        return StringCell.TYPE.equals(type);
    }
}
