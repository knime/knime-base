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
 *   14.04.2005 (cebron): created
 */
package org.knime.base.node.preproc.sorter;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JPanel;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.RowKey;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.InvalidSettingsException;


/**
 * This Panel holds subpanels consisting of SortItems.
 *
 * @see SortItem
 * @author Nicolas Cebron, University of Konstanz
 *
 * @deprecated as of 4.2.0. Use the DynamicSorterPanel
 */
@Deprecated
public class SorterNodeDialogPanel2 extends JPanel {
    private static final long serialVersionUID = -1757898824881266019L;

    /**
     * The entry in the JComboBox for not sorting a column.
     */
    public static final DataColumnSpec NOSORT = new DataColumnSpecCreator(
            "- DO NOT SORT -", DataType.getType(DataCell.class)).createSpec();

    /**
     * The entry in the JComboBox for sorting by {@link RowKey}.
     */
    public static final DataColumnSpec ROWKEY = new DataColumnSpecCreator(
            "-ROWKEY -", DataType.getType(StringCell.class)).createSpec();

    /*
     * Flag for whether to perform the sorting in memory or not.
     */
    private boolean m_memory;

    private final SortCriterionPanel m_sortCriterionPanel = new SortCriterionPanel();

    /*
     * Corresponding checkbox
     */
    private JCheckBox m_memorycheckb;

    /** Checkbox to sort missing values to the end independent of the
     * chosen sort order. */
    private final JCheckBox m_sortMissingToEndChecker;

    /**
     * Constructs a new empty JPanel used for displaying the three first
     * selected columns in the according order and the sorting order for each.
     * @since 4.1 made public for the use in ElementSelectorNodeDialog
     *
     */
    public SorterNodeDialogPanel2() {
        BoxLayout bl = new BoxLayout(this, BoxLayout.Y_AXIS);
        super.setLayout(bl);
        m_memory = false;
        m_sortMissingToEndChecker =
            new JCheckBox("Move Missing Cells to end of sorted list");
        m_sortMissingToEndChecker.setToolTipText("Missing values will be "
                + "moved to the end independent of the sort order ("
                + "otherwise they are considered to be the smallest elements)");
    }

    /**
     * Updates this panel based on the DataTableSpec, the list of columns to
     * include and the corresponding sorting order.
     *
     * @param spec the DataTableSpec
     * @param incl the list to include
     * @param sortOrder the sorting order
     * @param nrsortitems the inital number of sortitems to be shown
     * @param sortInMemory whether to perform the sorting in memory or not
     * @param missingToEnd Whether to move missings to the end
     * @since 4.1 made public for the use in ElementSelectorNodeDialog
     */
    public void update(final DataTableSpec spec, final List<String> incl,
            final boolean[] sortOrder, final int nrsortitems,
            final boolean sortInMemory, final boolean missingToEnd) {
        m_memory = sortInMemory;
        super.removeAll();
        m_sortCriterionPanel.update(spec, incl, sortOrder, nrsortitems);
        super.add(m_sortCriterionPanel.getPanel());
        if (spec != null) {
            Box memorybox = Box.createHorizontalBox();
            m_memorycheckb = new JCheckBox("Sort in memory", m_memory);
            m_memorycheckb.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(final ActionEvent ae) {
                    if (m_memorycheckb.isSelected()) {
                        m_memory = true;
                    } else {
                        m_memory = false;
                    }
                }
            });
            memorybox.add(m_memorycheckb);
            super.add(memorybox);

            Box missingToEndBox = Box.createHorizontalBox();
            m_sortMissingToEndChecker.setSelected(missingToEnd);
            missingToEndBox.add(m_sortMissingToEndChecker);
            super.add(missingToEndBox);
            revalidate();
        }
    }

    /**
     * Tests if user selections are valid and throws an Exception if not.
     * @throws InvalidSettingsException if user selection is not valid
     * @since 4.1 made public for the use in ElementSelectorNodeDialog
     */
    public void checkValid() throws InvalidSettingsException {
        m_sortCriterionPanel.checkValid();
    }

    /**
     * Returns all columns from the include list.
     *
     * @return a list of all columns from the include list
     */
    public List<String> getIncludedColumnList() {
        return m_sortCriterionPanel.getIncludedColumnList();
    }

    /**
     * Returns the sortOrder array.
     *
     * @return sortOrder
     */
    public boolean[] getSortOrder() {
        return m_sortCriterionPanel.getSortOrder();
    }

    /** @return the sortMissingToEnd checkbox property
     * @since 4.1 made public for the use in ElementSelectorNodeDialog
     */
    public boolean isSortMissingToEnd() {
        return m_sortMissingToEndChecker.isSelected();
    }

    /**
     * @return whether to perform the sorting in memory or not.
     */
    public boolean sortInMemory() {
        return m_memory;
    }
}
