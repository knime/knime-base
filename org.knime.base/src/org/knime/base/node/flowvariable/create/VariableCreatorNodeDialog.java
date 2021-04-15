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
 *   06.04.2021 (jl): created
 */
package org.knime.base.node.flowvariable.create;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.knime.base.node.flowvariable.create.VariableTable.Type;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.util.SharedIcons;

/**
 * Represents the dialog that is opened to edit the create variable node.
 *
 * @author Jannik Löscher
 */
final class VariableCreatorNodeDialog extends NodeDialogPane {

    /** The width of the type column. */
    static final int COL_TYPE_WIDTH = 100;

    /** The width of the name column. */
    static final int COL_NAME_WIDTH = 150;

    /** The width of the value column. */
    static final int COL_VAL_WIDTH = 200;

    /** The padding that is inserted between the heading and the variable rows. */
    private static final int COL_HEADER_PADDING_BOTTOM = 5;

    /**
     * The left (as long as it is not to the far left) and right (as long as it is not to the far right) padding of each
     * row.
     */
    private static final int COL_PADDING_LEFT_RIGHT = 2;

    /** The internal representation of the variables. */
    private final VariableTable m_vars;

    /** The panel the whole dialogue is created on. */
    private final JPanel m_panel = new JPanel();

    /** The layout of this component for later access. */
    private final GridBagLayout m_layout = new GridBagLayout();

    /** Contains the names that are currently in use by one of the name fields. */
    private final Map<String, Set<DialogVariableRow>> m_usedNames = new HashMap<>();

    /** Contains GUI elements with errors. */
    private final Set<JLabel> m_errorHints = new HashSet<>();

    /** Contains the rows in this dialog. */
    private final List<DialogVariableRow> m_rows = new ArrayList<>();

    /** The “add” button. */
    private final JButton m_addButton = new JButton();

    /**
     * @param gridY the y position in the grid, the type column shall have
     * @param insetsBottom the bottom padding for the element this constrains are applied to
     * @return the {@link GridBagConstraints} that position the types in the first column.
     */
    static GridBagConstraints getTypeColumnConstraints(final int gridY, final int insetsBottom) {
        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.gridx = 0;
        gbc.gridy = gridY;
        gbc.insets = new Insets(0, 0, insetsBottom, COL_PADDING_LEFT_RIGHT);

        return gbc;
    }

    /**
     * @param gridY the y position in the grid, the name column shall have
     * @param insetsBottom the bottom padding for the element this constrains are applied to
     * @return the {@link GridBagConstraints} that position the names in the second column.
     */
    static GridBagConstraints getNameColumnConstraints(final int gridY, final int insetsBottom) {
        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.gridx = 1;
        gbc.gridy = gridY;
        gbc.insets = new Insets(0, COL_PADDING_LEFT_RIGHT, insetsBottom, COL_PADDING_LEFT_RIGHT);

        return gbc;
    }

    /**
     * @param gridY the y position in the grid, the value column shall have
     * @param insetsBottom the bottom padding for the element this constrains are applied to
     * @return the {@link GridBagConstraints} that position the values in the third column.
     */
    static GridBagConstraints getValueColumnConstraints(final int gridY, final int insetsBottom) {
        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.gridx = 2;
        gbc.gridy = gridY;
        gbc.insets = new Insets(0, COL_PADDING_LEFT_RIGHT, insetsBottom, COL_PADDING_LEFT_RIGHT);

        return gbc;
    }

    /**
     * @param gridY the y position in the grid, the button column shall have
     * @return the {@link GridBagConstraints} that position the buttons in the forth column.
     */
    static GridBagConstraints getButtonsColumnConstraints(final int gridY) {
        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.gridx = 3;
        gbc.gridy = gridY;
        gbc.insets = new Insets(0, COL_PADDING_LEFT_RIGHT, 0, 0);

        return gbc;
    }

    /**
     * @param gridY the y position in the grid, the “add” button shall have.
     * @return the {@link GridBagConstraints} that position the “add” button in the last column.
     */
    static GridBagConstraints getAddButtonConstraints(final int gridY) {
        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 3;
        gbc.gridy = gridY;
        gbc.insets = new Insets(0, 2, 0, 0);

        return gbc;
    }

    /**
     * Utility method to update the preferred width of a component while keeping the height the same.
     *
     * @param component the component to be edited
     * @param width the width to be set
     */
    static void setPreferredWidth(final JComponent component, final int width) {
        final Dimension old = component.getPreferredSize();
        component.setPreferredSize(new Dimension(width, old.height));
    }

    /**
     * Utility method to update the maximum width of a component while keeping the height the same.
     *
     * @param component the component to be edited
     * @param width the width to be set
     */
    static void setMaxWidth(final JComponent component, final int width) {
        final Dimension old = component.getMaximumSize();
        component.setMaximumSize(new Dimension(width, old.height));
    }

    /**
     * Utility method to update the minimum width of a component while keeping the height the same.
     *
     * @param component the component to be edited
     * @param width the width to be set
     */
    static void setMinWidth(final JComponent component, final int width) {
        final Dimension old = component.getMinimumSize();
        component.setMinimumSize(new Dimension(width, old.height));
    }

    /**
     * Creates a new dialogue for the node.
     */
    VariableCreatorNodeDialog() {
        m_vars = new VariableTable(getAvailableFlowVariables(Type.getAllTypes()));

        addTab("Create and Define Variables", initVariableCreationPane());
    }

    /**
     * @return the variable representation of this dialog
     */
    VariableTable getVars() {
        return m_vars;
    }

    /**
     * @return a map that associates a row with the name it is currently using
     */
    Map<String, Set<DialogVariableRow>> getUsedNames() {
        return m_usedNames;
    }

    /**
     * @return a list of hints with errors
     */
    Set<JLabel> getErrorHints() {
        return m_errorHints;
    }

    /**
     * @return the panel the GUI is contained on
     */
    JPanel getGUIPanel() {
        return m_panel;
    }

    /**
     * @return the layout
     */
    GridBagLayout getLayout() {
        return m_layout;
    }

    /**
     * @return the addButton
     */
    JButton getAddButton() {
        return m_addButton;
    }

    /**
     * @return the rows in this dialog
     */
    List<DialogVariableRow> getRows() {
        return m_rows;
    }

    /**
     * Initializes the variable creation pane by loading the rows from the {@link #m_vars} field.
     *
     * @return the newly created or updated pane.
     */
    private JPanel initVariableCreationPane() {
        m_errorHints.clear();
        m_rows.clear();
        m_usedNames.clear();
        for (final ActionListener l : m_addButton.getActionListeners()) {
            m_addButton.removeActionListener(l);
        }
        m_panel.removeAll();

        m_panel.setLayout(m_layout);

        // add the heading
        final JLabel typeHeading = new JLabel(m_vars.getColumnName(VariableTable.COL_TYPE_INDEX));
        setPreferredWidth(typeHeading, COL_TYPE_WIDTH);
        final JLabel nameHeading = new JLabel(m_vars.getColumnName(VariableTable.COL_NAME_INDEX));
        setPreferredWidth(nameHeading, COL_NAME_WIDTH);
        final JLabel valHeading = new JLabel(m_vars.getColumnName(VariableTable.COL_VAL_INDEX));
        setPreferredWidth(valHeading, COL_VAL_WIDTH);
        m_panel.add(typeHeading, getTypeColumnConstraints(0, COL_HEADER_PADDING_BOTTOM));
        m_panel.add(nameHeading, getNameColumnConstraints(0, COL_HEADER_PADDING_BOTTOM));
        m_panel.add(valHeading, getValueColumnConstraints(0, COL_HEADER_PADDING_BOTTOM));

        for (int i = 0; i < m_vars.getRowCount(); ++i) {
            addRow(true);
        }
        if (!m_rows.isEmpty()) {
            m_rows.get(0).updateMoveButtions();
            m_rows.get(m_rows.size() - 1).updateMoveButtions();
        }

        JButton addButton = m_addButton;
        addButton.setText("Add");
        addButton.setToolTipText("Add a new variable");
        addButton.setIcon(SharedIcons.ADD_PLUS.get());
        addButton.addActionListener(a -> addRow(false));
        m_panel.add(addButton, getAddButtonConstraints(m_rows.size() + 1));

        // ensure that the user is not confused by an empty layout.
        // there may be an better way of doing that (maybe just a label with “No variables defined”
        if (m_rows.isEmpty()) {
            addRow(false);
        }

        m_panel.revalidate();
        m_panel.repaint();

        return m_panel;
    }

    /**
     * Add a row to the dialog.
     *
     * @param loading whether this row is loaded from a {@link VariableTable} or newly created
     */
    void addRow(final boolean loading) {
        m_rows.add(new DialogVariableRow(this, loading));
        if (!loading) {
            m_layout.setConstraints(m_addButton, getAddButtonConstraints(m_rows.size() + 1));
            m_rows.get(m_rows.size() - 1).updateMoveButtions();
            if (m_rows.size() > 1) {
                m_rows.get(m_rows.size() - 2).updateMoveButtions();
            }
            m_panel.revalidate();
            m_panel.repaint();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        if (m_errorHints.size() == 1) {
            throw new InvalidSettingsException(
                "Please fix the following error: “" + m_errorHints.iterator().next().getText() + "”");
        } else if (m_errorHints.size() >= 2) {
            throw new InvalidSettingsException(
                "Please fix the following error: “" + m_errorHints.iterator().next().getText() + "” and "
                    + (m_errorHints.size() - 1) + " other remaining error" + (m_errorHints.size() == 2 ? "" : "s"));
        }
        for (int idx = 0; idx < m_rows.size(); idx++) {
            if (!m_vars.setName(idx, m_rows.get(idx).getName()).getFirst().booleanValue()) {
                throw new IllegalStateException("Could not save name!");
            }
        }
        m_vars.saveVariablesToSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final PortObjectSpec[] specs)
        throws NotConfigurableException {
        try {
            m_vars.validatevariablesFromSettings(settings);
            m_vars.loadVariablesFromSettings(settings);
        } catch (InvalidSettingsException e) { // NOSONAR: the implementation guide states the object shall be reset to default if the values are invalid
            m_vars.reset();
        }
        m_vars.setExternalVariables(getAvailableFlowVariables(Type.getAllTypes()));
        initVariableCreationPane();
    }

}
