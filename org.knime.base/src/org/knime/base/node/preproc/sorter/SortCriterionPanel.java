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
 *   Aug 16, 2019 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.base.node.preproc.sorter;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import java.util.stream.Collectors;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFormattedTextField;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JSpinner.NumberEditor;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.Border;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.RowKey;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.InvalidSettingsException;

/**
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 * @since 4.1 extracted from {@link SorterNodeDialogPanel2} for the use in the Element Selector node
 */
public final class SortCriterionPanel {

    /**
     * The entry in the JComboBox for not sorting a column.
     */
    public static final DataColumnSpec NOSORT =
        new DataColumnSpecCreator("- DO NOT SORT -", DataType.getType(DataCell.class)).createSpec();

    /**
     * The entry in the JComboBox for sorting by {@link RowKey}.
     */
    public static final DataColumnSpec ROWKEY =
        new DataColumnSpecCreator("-ROWKEY -", DataType.getType(StringCell.class)).createSpec();

    private final List<SortItem> m_components = new ArrayList<>();

    private final JPanel m_panel = new JPanel();

    private DataTableSpec m_spec;

    private JButton m_addSortItemButton;

    /**
     *
     */
    public SortCriterionPanel() {
        m_panel.setLayout(new BoxLayout(m_panel, BoxLayout.Y_AXIS));
    }

    /**
     * @return the actual panel for display
     */
    public JPanel getPanel() {
        return m_panel;
    }

    /**
     * Updates this panel based on the DataTableSpec, the list of columns to include and the corresponding sorting
     * order.
     *
     * @param spec the DataTableSpec
     * @param incl the list to include
     * @param sortOrder the sorting order
     * @param nrSortItems the inital number of sortitems to be shown
     */
    public void update(final DataTableSpec spec, final List<String> incl, final boolean[] sortOrder,
        final int nrSortItems) {
        m_spec = spec;
        m_panel.removeAll();
        m_components.clear();
        if (spec != null) {
            final Vector<DataColumnSpec> values = createColumnList(spec);
            if ((incl == null) || (sortOrder == null)) {
                createDefault(spec, nrSortItems, values);
            } else {
                createFromInclude(spec, incl, sortOrder, values);
            }
            createButtonBox();
            m_panel.revalidate();
        }
    }

    /**
     * Returns the sortOrder array.
     *
     * @return sortOrder
     */
    public boolean[] getSortOrder() {
        final List<Boolean> boolvector = m_components.stream().filter(SortCriterionPanel::isStorable)
            .map(SortItem::getSortOrder).collect(Collectors.toList());
        boolean[] boolarray = new boolean[boolvector.size()];
        for (int i = 0; i < boolarray.length; i++) {
            boolarray[i] = boolvector.get(i);
        }
        return boolarray;
    }

    private static boolean isStorable(final SortItem item) {
        return item.isColumnSelected() && !(item.getSelectedColumn().equals(NOSORT));
    }

    /**
     * Returns all columns from the include list.
     *
     * @return a list of all columns from the include list
     */
    public List<String> getIncludedColumnList() {
        return m_components.stream().filter(SortCriterionPanel::isStorable).map(SortItem::getSelectedColumn)
            .map(DataColumnSpec::getName).collect(Collectors.toList());
    }

    /**
     * Tests if user selections are valid and throws an Exception if not.
     *
     * @throws InvalidSettingsException if user selection is not valid
     * @since 4.1 made public for the use in ElementSelectorNodeDialog
     */
    public void checkValid() throws InvalidSettingsException {
        for (SortItem temp : m_components) {
            if (!temp.isColumnSelected()) {
                throw new InvalidSettingsException(
                    "There are invalid column selections (highlighted with a red border).");
            }
        }
    }

    /**
     *
     */
    private void createButtonBox() {
        Box buttonbox = Box.createHorizontalBox();
        Border addColumnBorder = BorderFactory.createTitledBorder("Add columns");
        buttonbox.setBorder(addColumnBorder);
        m_addSortItemButton = new JButton("new columns");
        final JSpinner spinner = createSpinner();
        m_addSortItemButton.addActionListener(ae -> updateOnAddingItem(spinner));
        buttonbox.add(spinner);
        buttonbox.add(Box.createHorizontalStrut(10));
        buttonbox.add(m_addSortItemButton);
        m_panel.add(buttonbox);
    }

    private JSpinner createSpinner() {
        final JSpinner spinner = new JSpinner();
        int maxCols = m_spec.getNumColumns() - m_components.size() + 1;
        SpinnerNumberModel snm;
        if (maxCols <= 0) {
            snm = new SpinnerNumberModel(0, 0, 0, 1);
            spinner.setEnabled(false);
            m_addSortItemButton.setEnabled(false);
        } else {
            snm = new SpinnerNumberModel(1, 1, maxCols, 1);
        }
        spinner.setModel(snm);
        spinner.setMaximumSize(new Dimension(50, 25));
        spinner.setPreferredSize(new Dimension(50, 25));
        NumberEditor ne = (NumberEditor)spinner.getEditor();
        final JFormattedTextField spinnertextfield = ne.getTextField();
        // workaround to ensure same background color
        Color backColor = spinnertextfield.getBackground();
        // when spinner's text field is editable false
        spinnertextfield.setEditable(false);
        spinnertextfield.setBackground(backColor);
        return spinner;
    }

    void addItemsAddedListener(final ActionListener listener) {
        m_addSortItemButton.addActionListener(listener);
    }

    /**
     * @param spinner
     */
    private void updateOnAddingItem(final JSpinner spinner) {
        ArrayList<String> newlist = new ArrayList<>();
        for (int i = 0; i < m_components.size(); i++) {
            SortItem temp = m_components.get(i);
            newlist.add(temp.getColumnText());
        }
        int oldsize = m_components.size();
        String temp = spinner.getValue().toString();
        int newsize = Integer.parseInt(temp);
        for (int n = oldsize; n < oldsize + newsize; n++) {
            newlist.add(NOSORT.getName());
        }
        boolean[] oldbool = new boolean[oldsize];
        boolean[] newbool = new boolean[oldsize + newsize];
        // copy old values
        for (int i = 0; i < m_components.size(); i++) {
            SortItem temp2 = m_components.get(i);
            newbool[i] = temp2.getSortOrder();
        }
        // create new values
        for (int i = oldbool.length; i < newbool.length; i++) {
            newbool[i] = true;
        }
        update(m_spec, newlist, newbool, (oldsize + newsize));
    }

    private void createFromInclude(final DataTableSpec spec, final List<String> incl, final boolean[] sortOrder,
        final Vector<DataColumnSpec> values) {
        final Iterator<String> iterator = incl.iterator();
        for (int id = 0; iterator.hasNext(); id++) {
            final String includeString = iterator.next();
            final int columnIndex = spec.findColumnIndex(includeString);
            if (columnIndex != -1) {
                DataColumnSpec colspec = spec.getColumnSpec(columnIndex);
                SortItem temp = new SortItem(id, values, colspec, sortOrder[id]);
                m_panel.add(temp);
                m_components.add(temp);
            } else if (includeString.equals(NOSORT.getName())) {
                SortItem temp = new SortItem(id, values, NOSORT, sortOrder[id]);
                m_panel.add(temp);
                m_components.add(temp);
            } else if (includeString.equals(ROWKEY.getName())) {
                SortItem temp = new SortItem(id, values, ROWKEY, sortOrder[id]);
                m_panel.add(temp);
                m_components.add(temp);
            } else if (columnIndex == -1) {
                SortItem temp = new SortItem(id, values, includeString, sortOrder[id]);
                m_panel.add(temp);
                m_components.add(temp);
            }
        }
    }

    private void createDefault(final DataTableSpec spec, final int nrSortItems, final Vector<DataColumnSpec> values) {
        for (int i = 0; i < nrSortItems && i < spec.getNumColumns(); i++) {
            DataColumnSpec selected = (i == 0) ? values.get(i + 1) : values.get(0);
            SortItem temp = new SortItem(i, values, selected, true);
            m_panel.add(temp);
            m_components.add(temp);
        }
    }

    private static Vector<DataColumnSpec> createColumnList(final DataTableSpec spec) {
        Vector<DataColumnSpec> values = new Vector<>();
        values.add(NOSORT);
        values.add(ROWKEY);
        for (int j = 0; j < spec.getNumColumns(); j++) {
            values.add(spec.getColumnSpec(j));
        }
        return values;
    }

}
