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
package org.knime.base.node.preproc.sorter.dialog;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ItemEvent;
import java.util.LinkedList;
import java.util.List;

import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.node.util.ColumnComboBoxRenderer;

/**
 * This class holds a JPanel with a JComboBox two select columns to sort by and two JRadioButtons to select the sort
 * order.
 *
 * @author Timmo Waller-Ehrat, KNIME GmbH, Konstanz, Germany
 */
final class DynamicSortItem implements DynamicPanelItem {

    private JPanel m_panel;

    /*
     * The unique ID
     */
    private int m_id;

    /*
     * Values for the JComboBox
     */
    private List<DataColumnSpec> m_combovalues;

    /*
     * The JComboBox
     */
    private JComboBox<DataColumnSpec> m_combo;

    /*
     * Ascending
     */
    private static final String ASC = "Ascending";

    /*
     * Descending
     */
    private static final String DESC = "Descending";

    /*
     * JRadioButton for ascending order
     */
    private final JRadioButton m_ascRB = new JRadioButton(ASC);

    /*
     * JRadioButton for descending order
     */
    private final JRadioButton m_descRB = new JRadioButton(DESC);

    private final JLabel textLabel;

    private LinkedList<SelectionChangedListener> m_listeners;

    /**
     * Constructs a new JPanel that consists of a JComboBox which lets the user choose the columns to sort and two
     * JRadioButtons to choose the sort order (ascending/descending).
     *
     * @param id the unique ID of the SortItem
     * @param values the columns that the user can choose from
     * @param selected the selected column
     * @param sortOrder the sort
     */
    DynamicSortItem(final int id, final List<DataColumnSpec> values, final DataColumnSpec selected,
        final boolean sortOrder) {
        m_id = id;
        m_combovalues = values;

        m_listeners = new LinkedList<>();

        m_combo = new JComboBox<>(m_combovalues.toArray(new DataColumnSpec[0]));
        ColumnComboBoxRenderer renderer = new ColumnComboBoxRenderer();
        renderer.attachTo(m_combo);
        m_combo.setLightWeightPopupEnabled(false);
        m_combo.setSelectedItem(selected);
        m_combo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));
        m_combo.setAlignmentX(Component.LEFT_ALIGNMENT);
        m_combo.addItemListener(this::notifyListener);

        String bordertext = (id == 0) ? "Sort by:" : "Next by:";
        textLabel = new JLabel(bordertext);
        textLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        textLabel.setFont(new Font("AvantGarde", Font.BOLD, 12));

        
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.anchor = GridBagConstraints.NORTHWEST;
        
        final JPanel comboPanel = new JPanel(new GridBagLayout());

        c.gridy = 0;
        c.insets = new Insets(0, 0, 5, 0);
        comboPanel.add(textLabel, c);

        c.gridy = 1;
        c.weightx = 1;
        c.insets = new Insets(0, 0, 0, 0);
        c.fill = GridBagConstraints.HORIZONTAL;
        comboPanel.add(m_combo, c);

        final JPanel buttonPanel = new JPanel(new GridBagLayout());
        c.weightx = 0;
        c.fill = GridBagConstraints.NONE;

        ButtonGroup group = new ButtonGroup();
        group.add(m_ascRB);
        group.add(m_descRB);

        c.gridy = 0;
        buttonPanel.add(m_ascRB, c);

        c.gridy = 1;
        buttonPanel.add(m_descRB, c);


        if (sortOrder) {
            m_ascRB.setSelected(true);
        } else {
            m_descRB.setSelected(true);
        }

        m_panel = new JPanel(new GridBagLayout());
        c = new GridBagConstraints();
        c.anchor = GridBagConstraints.NORTHWEST;
        c.gridy = 0;

        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        m_panel.add(comboPanel, c);

        c.weightx = 0;
        c.fill = GridBagConstraints.NONE;
        c.insets = new Insets(15, 20, 0, 50);
        m_panel.add(buttonPanel, c);
    }

    private void notifyListener(final ItemEvent e) {
        for (SelectionChangedListener listener : m_listeners) {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                m_combo.setSelectedItem(e.getItem());
                listener.updateComboBoxes();
            }
        }
    }

    @Override
    public JPanel getPanel() {
        return m_panel;
    }

    /**
     * Add a new listener to handle events for changed value in the JComboBox
     *
     * @param listener the listener to add to the list of listeners
     */
    public void addListener(final SelectionChangedListener listener) {
        m_listeners.add(listener);
    }

    /**
     * Each SortItem has a unique ID.
     *
     * @return the ID of the DynamicSortItem
     */
    public int getID() {
        return m_id;
    }

    void setID(final int id) {
        m_id = id;
        String bordertext = (id == 0) ? "Sort by:" : "Next by:";

        textLabel.setText(bordertext);
    }

    /**
     * Updates the values in the JComboBox
     *
     * @param values
     */
    public void updateComboBoxes(final List<DataColumnSpec> values) {
        List<DataColumnSpec> newValues = new LinkedList<>(values);
        newValues.add(0, (DataColumnSpec)m_combo.getSelectedItem());

        m_combovalues = newValues;
        m_combo.setModel(new DefaultComboBoxModel<DataColumnSpec>(m_combovalues.toArray(new DataColumnSpec[0])));
    }

    /**
     * The sort order of this DynmaicSortItem.
     *
     * @return <code>true</code> for ascending, <code>false</code> for descending
     */

    public boolean getSortOrder() {
        return m_ascRB.isSelected();
    }

    /**
     * The column that is selected in the JComboBox.
     *
     * @return the selected column
     */
    public DataColumnSpec getSelectedColumn() {
        return (DataColumnSpec)m_combo.getSelectedItem();
    }

    /**
     * Returns true when a valid selection is done for the column field.
     *
     * @return if a selection is done by the user.
     */
    boolean isColumnSelected() {
        return m_combo.getSelectedIndex() >= 0;
    }

    /**
     * Get the text in the column field that is displayed to the user.
     *
     * @return the text of the column field displayed to the user.
     */
    String getColumnText() {
        if (isColumnSelected()) {
            return getSelectedColumn().getName();
        } else {
            ColumnComboBoxRenderer renderer = (ColumnComboBoxRenderer)m_combo.getRenderer();
            return renderer.getDefaultValue();
        }
    }
}
