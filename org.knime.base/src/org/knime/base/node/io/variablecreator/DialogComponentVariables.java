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
package org.knime.base.node.io.variablecreator;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.apache.commons.lang3.ArrayUtils;
import org.knime.base.node.io.variablecreator.SettingsModelVariables.Type;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponent;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.util.SharedIcons;
import org.knime.core.node.workflow.FlowVariable;

/**
 * Represents the dialog that is opened to edit the create variable node.
 *
 * @author Jannik Löscher, KNIME GmbH, Konstanz, Germany
 * @noreference This class is not intended to be referenced by clients.
 * @noinstantiate This class is not intended to be instantiated by clients.
 */
public final class DialogComponentVariables extends DialogComponent {

    /** The width of the type column. */
    static final int COL_TYPE_WIDTH = 110;

    /** The width of the name column. */
    static final int COL_NAME_WIDTH = 150;

    /** The width of the value column. */
    static final int COL_VAL_WIDTH = 200;

    /** The padding that is inserted between the heading and the variable rows. */
    private static final int COL_HEADER_PADDING_BOTTOM = 5;

    /**
     * The inner padding of a column cell.
     */
    static final int COL_PADDING_INNER = 2;

    /** The padding of the table's border. */
    private static final int TAB_PADDING = 10;

    /** Listeners listening for changing of a row. */
    private final CopyOnWriteArrayList<ChangeListener> m_listenersRowChange;

    /** The panel the whole dialogue is created on. */
    private final JPanel m_panel = new JPanel();

    /** The layout of this component for later access. */
    private final GridBagLayout m_layout = new GridBagLayout();

    /** Contains the names that are currently in use by one of the name fields. */
    private final Map<String, Set<DialogComponentVariableRow>> m_usedNames = new HashMap<>();

    /** Contains GUI elements with errors. */
    private final Set<JLabel> m_errorHints = new HashSet<>();

    /** Contains the rows in this dialog. */
    private final List<DialogComponentVariableRow> m_rows = new ArrayList<>();

    /** The “add” button. */
    private final JButton m_addButton = new JButton();

    /** The label informing the user that they should add a variable */
    private final JLabel m_addHint = new JLabel("No variables defined.");

    /** An array containing the labels defining the header. */
    private final JLabel[] m_headerLabels;

    /** Whether this component is currently enabled. */
    private boolean m_enabled;

    /**
     * @param gridY the y position in the grid, the type column shall have
     * @param insetsBottom the bottom padding for the element this constrains are applied to
     * @return the {@link GridBagConstraints} that position the types in the first column.
     */
    static GridBagConstraints getTypeColumnConstraints(final int gridY, final int insetsBottom) {
        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.gridx = SettingsModelVariables.COL_TYPE_INDEX;
        gbc.gridy = gridY;
        gbc.insets = new Insets(COL_PADDING_INNER, 0, insetsBottom, COL_PADDING_INNER);

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
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = SettingsModelVariables.COL_NAME_INDEX;
        gbc.weightx = 1.0;
        gbc.gridy = gridY;
        gbc.insets = new Insets(COL_PADDING_INNER, COL_PADDING_INNER, insetsBottom, COL_PADDING_INNER);

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
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = SettingsModelVariables.COL_VAL_INDEX;
        gbc.weightx = 1.0;
        gbc.gridy = gridY;
        gbc.insets = new Insets(COL_PADDING_INNER, COL_PADDING_INNER, insetsBottom, COL_PADDING_INNER);

        return gbc;
    }

    /**
     * @param gridY the y position in the grid, the button column shall have
     * @return the {@link GridBagConstraints} that position the buttons in the forth column.
     */
    static GridBagConstraints getButtonsColumnConstraints(final int gridY) {
        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.gridx = SettingsModelVariables.COL_NUMBER;
        gbc.gridy = gridY;
        gbc.insets = new Insets(COL_PADDING_INNER, COL_PADDING_INNER, COL_PADDING_INNER, 0);

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
        gbc.gridx = SettingsModelVariables.COL_NUMBER;
        gbc.gridy = gridY;
        gbc.insets = new Insets(COL_PADDING_INNER, COL_PADDING_INNER, COL_PADDING_INNER, 0);

        return gbc;
    }

    /**
     * @return the {@link GridBagConstraints} that position the “add some variables” hint in the second row spread over
     *         the first three columns.
     */
    static GridBagConstraints getAddHintConstraints() {
        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = SettingsModelVariables.COL_NUMBER;
        gbc.weightx = 1.0;
        gbc.insets = new Insets(0, COL_PADDING_INNER, COL_PADDING_INNER, COL_PADDING_INNER);

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
     *
     * @param model the model to use
     */
    public DialogComponentVariables(final SettingsModelVariables model) {
        super(model);

        final var panel = getComponentPanel();
        m_listenersRowChange = new CopyOnWriteArrayList<>();
        m_headerLabels = new JLabel[SettingsModelVariables.COL_NUMBER];
        panel.setLayout(new GridBagLayout());
        final var constraints = new GridBagConstraints();
        constraints.gridx = constraints.gridy = 0;
        constraints.insets = new Insets(TAB_PADDING, TAB_PADDING, TAB_PADDING, TAB_PADDING);
        constraints.fill = GridBagConstraints.BOTH;
        constraints.weightx = constraints.weighty = 1.0;
        panel.add(initVariableCreationPane(), constraints);

        setEnabledComponents(model.isEnabled());
        model.addChangeListener(e -> setEnabledComponents(model.isEnabled()));
    }

    /**
     * @return the variable representation of this dialog
     */
    public SettingsModelVariables getVariableTable() {
        return (SettingsModelVariables)getModel();
    }

    /**
     * @return a map that associates a row with the name it is currently using
     */
    Map<String, Set<DialogComponentVariableRow>> getUsedNames() {
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
     * @param visible whether the hint to add new variables should be visible
     */
    void setAddHintVisible(final boolean visible) {
        m_addHint.setVisible(visible);
    }

    /**
     * @return the rows in this dialog
     */
    List<DialogComponentVariableRow> getRows() {
        return m_rows;
    }

    /**
     * @return the amount of rows in this dialog. This number may be different from the number of rows in the settings
     *         model if the variables are currently loaded.
     */
    public int getRowCount() {
        return m_rows.size();
    }

    /**
     * @return the enabled
     */
    public boolean isEnabled() {
        return m_enabled;
    }

    /**
     * Initializes the variable creation pane by loading the rows from the {@link SettingsModelVariables}
     *
     * @return the newly created or updated pane.
     */
    private JPanel initVariableCreationPane() {
        m_errorHints.clear();
        m_rows.clear();
        fireRowsChange();
        m_usedNames.clear();
        for (final ActionListener l : m_addButton.getActionListeners()) {
            m_addButton.removeActionListener(l);
        }
        m_panel.removeAll();

        m_panel.setLayout(m_layout);

        // add the heading

        if (m_headerLabels[SettingsModelVariables.COL_TYPE_INDEX] == null) {
            m_headerLabels[SettingsModelVariables.COL_TYPE_INDEX] =
                new JLabel(SettingsModelVariables.getColumnName(SettingsModelVariables.COL_TYPE_INDEX));
            m_headerLabels[SettingsModelVariables.COL_NAME_INDEX] =
                new JLabel(SettingsModelVariables.getColumnName(SettingsModelVariables.COL_NAME_INDEX));
            m_headerLabels[SettingsModelVariables.COL_VAL_INDEX] =
                new JLabel(SettingsModelVariables.getColumnName(SettingsModelVariables.COL_VAL_INDEX));
        }
        final JLabel typeHeading = m_headerLabels[SettingsModelVariables.COL_TYPE_INDEX];
        setPreferredWidth(typeHeading, COL_TYPE_WIDTH);
        final JLabel nameHeading = m_headerLabels[SettingsModelVariables.COL_NAME_INDEX];
        setPreferredWidth(nameHeading, COL_NAME_WIDTH);
        final JLabel valHeading = m_headerLabels[SettingsModelVariables.COL_VAL_INDEX];
        setPreferredWidth(valHeading, COL_VAL_WIDTH);

        m_panel.add(typeHeading, getTypeColumnConstraints(0, COL_HEADER_PADDING_BOTTOM));
        m_panel.add(nameHeading, getNameColumnConstraints(0, COL_HEADER_PADDING_BOTTOM));
        m_panel.add(valHeading, getValueColumnConstraints(0, COL_HEADER_PADDING_BOTTOM));

        for (int i = 0, max = getVariableTable().getRowCount(); i < max; ++i) {
            addRow(true);
        }
        if (!m_rows.isEmpty()) {
            m_rows.get(0).updateMoveButtons();
            m_rows.get(m_rows.size() - 1).updateMoveButtons();
        }

        m_addHint.setHorizontalAlignment(SwingConstants.CENTER);
        m_panel.add(m_addHint, getAddHintConstraints());

        JButton addButton = m_addButton;
        addButton.setText("Add");
        addButton.setToolTipText("Add a new variable");
        addButton.setMnemonic(KeyEvent.VK_A);
        addButton.setIcon(SharedIcons.ADD_PLUS.get());
        addButton.addActionListener(a -> addRow(false));
        m_panel.add(addButton, getAddButtonConstraints(m_rows.size() + 1));

        m_panel.revalidate();
        m_panel.repaint();

        return m_panel;
    }

    /**
     * Add an empty row to the dialog.
     *
     * @param loading whether this row is loaded from a {@link SettingsModelVariables} or newly created
     */
    public void addRow(final boolean loading) {
        addRow(loading, null, null, null);
    }

    /**
     * Add a row to the dialog an initialize it with the following values.
     *
     * @param loading whether this row is loaded from a {@link SettingsModelVariables} or newly created
     * @param type the type to set. May be <code>null</code> to indicate that the type shouldn't be changed.
     * @param name the name to set. May be <code>null</code> to indicate that the name shouldn't be changed.
     * @param value the value to set. May be <code>null</code> to indicate that the value shouldn't be changed.
     */
    void addRow(final boolean loading, final Type type, final String name, final String value) {
        final var row = new DialogComponentVariableRow(this, loading);
        m_rows.add(row);
        row.setFields(type, name, value);

        setAddHintVisible(false);
        if (!loading) {
            m_layout.setConstraints(m_addButton, getAddButtonConstraints(m_rows.size() + 1));
            m_rows.get(m_rows.size() - 1).updateMoveButtons();
            if (m_rows.size() > 1) {
                m_rows.get(m_rows.size() - 2).updateMoveButtons();
            }
            fireRowsChange();
            m_rows.get(m_rows.size() - 1).selectSelectNextInput();
            m_panel.revalidate();
            m_panel.repaint();
        } else {
            fireRowsChange();
        }

    }

    /**
     * Merge the given variables into the current ones by replacing variables with the same name with an external
     * variable with that name and adding other variables as new rows to the end.
     *
     * @param externalVariables the external variables to load.
     */
    public void mergeVariables(final Map<String, FlowVariable> externalVariables) {
        for (final var nameVar : externalVariables.entrySet()) {
            final var type = Type.getTypeFromVariableType(nameVar.getValue().getVariableType());
            if (type == null) {
                throw new IllegalStateException("Encountered unknown type while merging variables: "
                    + nameVar.getValue().getVariableType().getIdentifier());
            }
            final var name = nameVar.getKey();
            final var valueStr = nameVar.getValue().getValueAsString();

            final var rows = m_usedNames.getOrDefault(name.trim(), Collections.emptySet());

            int firstRow = Integer.MAX_VALUE;
            for (final var row : rows) {
                if (row.getIndex() < firstRow) {
                    firstRow = row.getIndex(); // overwrite first column in list
                }
            }

            if (firstRow != Integer.MAX_VALUE) {
                m_rows.get(firstRow).setFields(type, null, valueStr);
            } else {
                addRow(false, type, name, valueStr);
            }
        }
    }

    /**
     * Replace the current variables by the given ones
     *
     * @param externalVariables the external variables to load all have to be of the supported type.
     */
    public void setVariables(final Map<String, FlowVariable> externalVariables) {

        while (!m_rows.isEmpty()) {
            m_rows.get(m_rows.size() - 1).removeRow();
        }

        final var supportedTypes = getVariableTable().getSupportedTypes();
        for (final var nameVar : externalVariables.entrySet()) {
            final var type = Type.getTypeFromVariableType(nameVar.getValue().getVariableType());

            if (!ArrayUtils.contains(supportedTypes, type)) {
                throw new IllegalStateException("Encountered unsupported type while setting variables: "
                    + nameVar.getValue().getVariableType().getIdentifier());
            }
            final var name = nameVar.getKey();
            final var value = nameVar.getValue().getValueAsString();

            addRow(false, type, name, value);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void updateComponent() {
        initVariableCreationPane();
        setEnabledComponents(getModel().isEnabled());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettingsBeforeSave() throws InvalidSettingsException {
        if (m_errorHints.size() == 1) {
            throw new InvalidSettingsException(
                "Please fix the following error: “" + m_errorHints.iterator().next().getText() + "”");
        } else if (m_errorHints.size() >= 2) {
            throw new InvalidSettingsException(
                "Please fix the following error: “" + m_errorHints.iterator().next().getText() + "” and "
                    + (m_errorHints.size() - 1) + " other remaining error" + (m_errorHints.size() == 2 ? "" : "s"));
        }

        final var vars = getVariableTable();
        for (int idx = 0; idx < m_rows.size(); idx++) {
            if (!vars.setName(idx, m_rows.get(idx).getName()).getFirst().booleanValue()) {
                throw new IllegalStateException("Could not save name!");
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void checkConfigurabilityBeforeLoad(final PortObjectSpec[] specs) throws NotConfigurableException {
        // nothing to do: we are always configurable
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void setEnabledComponents(final boolean enabled) {
        if (enabled == m_enabled) {
            return;
        }

        for (final var header : m_headerLabels) {
            header.setEnabled(enabled);
        }
        m_addButton.setEnabled(enabled);
        m_addHint.setEnabled(enabled);

        for (final var row : m_rows) {
            row.setEnabled(enabled);
        }
        m_enabled = enabled;

        m_panel.revalidate();
        m_panel.repaint();
    }

    /**
     * Adds a listener (to the end of the listener list) which is notified, whenever the rows in the dialog change.
     *
     * @param listener the listener to add.
     */
    public void addRowChangeListener(final ChangeListener listener) {
        m_listenersRowChange.addIfAbsent(listener);
    }

    /**
     * Removes a listener that listened to the change of a row.
     *
     * @param listener the listener to remove.
     */
    public void removeRowsChangeListener(final ChangeListener listener) {
        m_listenersRowChange.remove(listener); // NOSONAR: it can be assumed that only a few listeners are registered
    }

    /**
     * Notifies the listeners of the change of a row.
     *
     * @param row the row that was added.
     */
    void fireRowsChange() {
        for (final var l : m_listenersRowChange) {
            l.stateChanged(new ChangeEvent(this));
        }
    }

    /**
     * Set a binding for the name input field
     *
     * @param row the row to set the binding on.
     *
     * @return a consumer that will be applied to the text of the name input field
     */
    public Consumer<String> setNameBinding(final int row) {
        return m_rows.get(row).setNameBinding();
    }

    /**
     * @param row the row to get the binding from.
     * @return the consumer bound to the name input field if present
     */
    public Optional<Consumer<String>> getNameBinding(final int row) {
        return m_rows.get(row).getNameBinding();
    }

    /**
     * Remove the consumer bound to the name input field.
     *
     * @param row the row to remove the binding from.
     * @return whether there was a consumer to remove.
     */
    public boolean removeNameBinding(final int row) {
        return m_rows.get(row).removeNameBinding();
    }

    /**
     * This operation is not supported by this component!
     *
     * @param text the text that would be set
     * @throws UnsupportedOperationException if called
     */
    @Override
    public void setToolTipText(final String text) {
        // I cannot think of any intuitive way of what tool tip on this component would mean
        throw new UnsupportedOperationException("A DialogComponentVaraibles cannot have a tool tip.");
    }

}
