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
package org.knime.base.node.io.database;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

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

import org.knime.base.node.preproc.sorter.SortItem;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.node.InvalidSettingsException;

/**
 * This Panel holds subpanels consisting of SortItems.
 *
 * @see SortItem
 * @author Nicolas Cebron, University of Konstanz
 * @author Patrick Winter, KNIME AG, Zurich, Switzerland
 * @since 2.10
 */
@Deprecated
public class DBSorterNodeDialogPanel extends JPanel {
    private static final long serialVersionUID = -1757898824881266019L;

    /**
     * The entry in the JComboBox for not sorting a column.
     */
    public static final DataColumnSpec NOSORT = new DataColumnSpecCreator("- DO NOT SORT -",
        DataType.getType(DataCell.class)).createSpec();

    /*
     * Keeps track of the components on this JPanel
     */
    private Vector<DBSortItem> m_components;

    /*
     * The DataTableSpec
     */
    private DataTableSpec m_spec;

    /**
     * Constructs a new empty JPanel used for displaying the three first selected columns in the according order and the
     * sorting order for each.
     *
     */
    DBSorterNodeDialogPanel() {
        BoxLayout bl = new BoxLayout(this, BoxLayout.Y_AXIS);
        super.setLayout(bl);
        m_components = new Vector<DBSortItem>();
    }

    /**
     * Updates this panel based on the DataTableSpec, the list of columns to include and the corresponding sorting
     * order.
     *
     * @param spec the DataTableSpec
     * @param incl the list to include
     * @param sortOrder the sorting order
     * @param nrsortitems the inital number of sortitems to be shown
     */
    void update(final DataTableSpec spec, final List<String> incl, final boolean[] sortOrder, final int nrsortitems) {
        m_spec = spec;
        super.removeAll();
        m_components.removeAllElements();
        int interncounter = 0;

        if (spec != null) {
            Vector<DataColumnSpec> values = new Vector<DataColumnSpec>();
            values.add(NOSORT);
            for (int j = 0; j < spec.getNumColumns(); j++) {
                values.add(spec.getColumnSpec(j));
            }
            if ((incl == null) || (sortOrder == null)) {

                for (int i = 0; i < nrsortitems && i < spec.getNumColumns(); i++) {
                    DataColumnSpec selected = (i == 0) ? values.get(i + 1) : values.get(0);
                    DBSortItem temp = new DBSortItem(i, values, selected, true);
                    super.add(temp);
                    m_components.add(temp);
                }
            } else {
                for (int i = 0; i < incl.size(); i++) {
                    String includeString = incl.get(i);
                    int columnIndex = spec.findColumnIndex(includeString);
                    if (columnIndex != -1) {
                        DataColumnSpec colspec = spec.getColumnSpec(columnIndex);
                        DBSortItem temp = new DBSortItem(interncounter, values, colspec, sortOrder[interncounter]);
                        super.add(temp);
                        m_components.add(temp);
                        interncounter++;
                    } else if (includeString.equals(NOSORT.getName())) {
                        DBSortItem temp = new DBSortItem(interncounter, values, NOSORT, sortOrder[interncounter]);
                        super.add(temp);
                        m_components.add(temp);
                        interncounter++;
                    } else if (columnIndex == -1) {
                        DBSortItem temp =
                            new DBSortItem(interncounter, values, includeString, sortOrder[interncounter]);
                        super.add(temp);
                        m_components.add(temp);
                        interncounter++;
                    }

                }
            }
            Box buttonbox = Box.createHorizontalBox();
            Border addColumnBorder = BorderFactory.createTitledBorder("Add columns");
            buttonbox.setBorder(addColumnBorder);
            int maxCols = m_spec.getNumColumns() - m_components.size();

            JButton addSortItemButton = new JButton("new columns");
            final JSpinner spinner = new JSpinner();
            SpinnerNumberModel snm;
            if (maxCols == 0) {
                snm = new SpinnerNumberModel(0, 0, maxCols, 1);
                spinner.setEnabled(false);
                addSortItemButton.setEnabled(false);
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

            addSortItemButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(final ActionEvent ae) {
                    ArrayList<String> newlist = new ArrayList<String>();
                    for (int i = 0; i < m_components.size(); i++) {
                        DBSortItem temp = m_components.get(i);
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
                        DBSortItem temp2 = m_components.get(i);
                        newbool[i] = temp2.getSortOrder();
                    }
                    // create new values
                    for (int i = oldbool.length; i < newbool.length; i++) {
                        newbool[i] = true;
                    }
                    update(m_spec, newlist, newbool, (oldsize + newsize));
                }
            });
            buttonbox.add(spinner);
            buttonbox.add(Box.createHorizontalStrut(10));
            buttonbox.add(addSortItemButton);
            super.add(buttonbox);
            revalidate();
        }
    }

    /**
     * Tests if user selections are valid and throws an Exception if not.
     *
     * @throws InvalidSettingsException if user selection is not valid
     */
    void checkValid() throws InvalidSettingsException {
        for (int i = 0; i < m_components.size(); i++) {
            DBSortItem temp = m_components.get(i);
            if (!temp.isColumnSelected()) {
                throw new InvalidSettingsException("There are invalid "
                    + "column selections (highlighted with a red border).");
            }
        }
    }

    /**
     * Returns all columns from the include list.
     *
     * @return a list of all columns from the include list
     */
    public List<String> getIncludedColumnList() {
        ArrayList<String> list = new ArrayList<String>();
        for (int i = 0; i < m_components.size(); i++) {
            DBSortItem temp = m_components.get(i);
            if (temp.isColumnSelected() && !(temp.getSelectedColumn().equals(NOSORT))) {
                list.add(temp.getSelectedColumn().getName());
            }
        }
        return list;
    }

    /**
     * Returns the sortOrder array.
     *
     * @return sortOrder
     */
    public boolean[] getSortOrder() {
        Vector<Boolean> boolvector = new Vector<Boolean>();
        for (int i = 0; i < m_components.size(); i++) {
            DBSortItem temp = m_components.get(i);
            if (temp.isColumnSelected() && !(temp.getSelectedColumn().equals(NOSORT))) {
                boolvector.add(temp.getSortOrder());
            }
        }
        boolean[] boolarray = new boolean[boolvector.size()];
        for (int i = 0; i < boolarray.length; i++) {
            boolarray[i] = boolvector.get(i);
        }
        return boolarray;
    }
}
