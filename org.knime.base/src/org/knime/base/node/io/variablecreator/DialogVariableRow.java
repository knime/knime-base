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
 *   12.04.2021 (jl): created
 */
package org.knime.base.node.io.variablecreator;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.ListCellRenderer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.apache.commons.lang.WordUtils;
import org.knime.base.node.io.variablecreator.VariableTable.Type;
import org.knime.core.node.util.SharedIcons;
import org.knime.core.util.Pair;

/**
 * Represents a row in the dialog (panel).
 *
 * @author Jannik Löscher, KNIME GmbH, Konstanz, Germany
 */
final class DialogVariableRow {

    /** The index in the row list this row currently has. */
    private int m_index;

    /** The panel on which this row is contained. */
    private final VariableCreatorNodeDialog m_parent;

    /** The combo box for selecting a type. */
    private final JComboBox<Type> m_typeSelection;

    /** The error/warning hint for the type combo box. */
    private final JLabel m_typeSelectionHint;

    /** The panel containing both {@link #m_typeSelection} and {@link #m_typeSelectionHint}. */
    private final JPanel m_typeBox;

    /** The input field for choosing a name. */
    private final JTextField m_nameInput;

    /** The error/warning hint for the name input field. */
    private final JLabel m_nameInputHint;

    /** The panel containing both {@link #m_nameInput} and {@link #m_nameInputHint}. */
    private final JPanel m_nameBox;

    /** The input field for choosing a value. */
    private final JTextField m_valueInput;

    /** The error/warning hint for the value input field. */
    private final JLabel m_valueInputHint;

    /** The panel containing both {@link #m_valueInput} and {@link #m_valueInputHint}. */
    private final JPanel m_valueBox;

    /** The button that moves this row up. */
    private final JButton m_up;

    /** The button that moves this row down. */
    private final JButton m_down;

    /** The button that removes this row. */
    private final JButton m_delete;

    /** The panel containing both {@link #m_up},{@link #m_down} and {@link #m_delete}. */
    private final JPanel m_buttonBox;

    /** Whether this row is currently deleted. (Used to prevent checks triggered by removing components.) */
    private boolean m_isDeleting;

    /**
     * Creates a new row in the panel of the dialog
     *
     * @param parent the dialog panel this row belongs to (is assumed to be lied out by a {@link GridBagLayout})
     * @param loading whether this row is created while loading the values from a {@link VariableTable}
     */
    DialogVariableRow(final VariableCreatorNodeDialog parent, final boolean loading) {
        m_parent = parent;
        m_index = m_parent.getRows().size();
        m_isDeleting = false;

        // initialize objects but not values
        m_typeSelection = new JComboBox<>(Type.values()) {
            private static final long serialVersionUID = -5078958064144519774L;

            @Override
            public void setPopupVisible(final boolean visible) {
                super.setPopupVisible(visible);
                if (!visible && !m_isDeleting) { // when closing
                    checkTypeBox(false);
                }
            }
        };
        m_typeSelectionHint = new JLabel();
        m_typeBox = new JPanel();
        m_nameInput = new JTextField();
        m_nameInputHint = new JLabel();
        m_nameBox = new JPanel();
        m_valueInput = new JTextField();
        m_valueInputHint = new JLabel();
        m_valueBox = new JPanel();
        m_up = new JButton();
        m_down = new JButton();
        m_delete = new JButton();
        m_buttonBox = new JPanel();

        initRow(loading);
    }

    /**
     * Initiates new row in the dialog
     *
     * @param loading whether this row is created while loading the values from a {@link VariableTable}
     */
    private void initRow(final boolean loading) {
        if (!loading) {
            m_parent.getVars().addRow();
        }
        final JPanel parentPanel = m_parent.getGUIPanel();

        // we create this in reverse order to ensure correct checks
        final JPanel value = makeValueInputBox();
        final JPanel name = makeNameInputBox();
        parentPanel.add(makeTypeSelectionBox(), VariableCreatorNodeDialog.getTypeColumnConstraints(m_index + 1, 0));
        parentPanel.add(name, VariableCreatorNodeDialog.getNameColumnConstraints(m_index + 1, 0));
        parentPanel.add(value, VariableCreatorNodeDialog.getValueColumnConstraints(m_index + 1, 0));
        parentPanel.add(makeButtons(), VariableCreatorNodeDialog.getButtonsColumnConstraints(m_index + 1));
    }

    /**
     * Selects all the text in the name input field and grabs the focus
     */
    void selectName() {
        m_nameInput.selectAll();
        m_nameInput.requestFocusInWindow();
    }

    /**
     * @return the name defined in this row
     */
    String getName() {
        return m_nameInput.getText().trim();
    }

    /**
     * Removes this row (from its parent).
     */
    void removeRow() {
        m_isDeleting = true;
        m_parent.getVars().removeRow(m_index);
        m_parent.getRows().remove(m_index);

        final JPanel parentPanel = m_parent.getGUIPanel();
        parentPanel.remove(m_typeBox);
        parentPanel.remove(m_nameBox);
        parentPanel.remove(m_valueBox);
        parentPanel.remove(m_buttonBox);

        for (int i = m_index; i < m_parent.getRows().size(); i++) {
            final DialogVariableRow after = m_parent.getRows().get(i);
            after.m_index = i;
            after.setRowInLayout(i);
        }
        m_parent.getLayout().setConstraints(m_parent.getAddButton(),
            VariableCreatorNodeDialog.getAddButtonConstraints(m_parent.getRows().size() + 1));

        m_parent.getErrorHints().remove(m_typeSelectionHint);
        m_parent.getErrorHints().remove(m_nameInputHint);
        m_parent.getErrorHints().remove(m_valueInputHint);

        checkDuplicateNames(m_nameInput.getText(), null);

        updateMoveButtions(m_index);

        updateMoveButtions(m_index);
        updateMoveButtions(m_index + 1);

        parentPanel.revalidate();
        parentPanel.repaint();
    }

    /**
     * Swaps the to rows with the given indices.
     *
     * @param first the first row
     * @param second the second row
     */
    void swapRows(final int first, final int second) {
        m_parent.getVars().swapRows(first, second);
        Collections.swap(m_parent.getRows(), first, second);

        // this is after the swap so the positions do not match their indices anymore
        final DialogVariableRow firstRow = m_parent.getRows().get(first); // old second row
        final DialogVariableRow secondRow = m_parent.getRows().get(second);// old first  row
        firstRow.m_index = first;
        secondRow.m_index = second;
        firstRow.setRowInLayout(first);
        secondRow.setRowInLayout(second);

        updateMoveButtions(first);
        updateMoveButtions(second);

        final JPanel parentPanel = m_parent.getGUIPanel();
        parentPanel.revalidate();
        parentPanel.repaint();
    }

    /**
     * Moves this {@link DialogVariableRow} to a specific row in the {@link GridBagLayout} of the parent.
     *
     * @param row the row number to move to
     */
    private void setRowInLayout(final int row) {
        final GridBagLayout parentLayout = m_parent.getLayout();
        final int destination = row + 1; // we need to skip the header
        parentLayout.setConstraints(m_typeBox, VariableCreatorNodeDialog.getTypeColumnConstraints(destination, 0));
        parentLayout.setConstraints(m_nameBox, VariableCreatorNodeDialog.getNameColumnConstraints(destination, 0));
        parentLayout.setConstraints(m_valueBox, VariableCreatorNodeDialog.getValueColumnConstraints(destination, 0));
        parentLayout.setConstraints(m_buttonBox, VariableCreatorNodeDialog.getButtonsColumnConstraints(destination));
    }

    /**
     * Sets a Info for a hint label and updates the error hints accordingly.
     *
     * @param hint the hint to edit
     * @param text the text to set
     */
    private void setInfo(final JLabel hint, final String text) {
        m_parent.getErrorHints().remove(hint);
        hint.setText(text);
        hint.setIcon(SharedIcons.INFO_BALLOON.get());
        hint.setToolTipText("Info: " + text);
    }

    /**
     * Sets an error for a hint label and updates the error hints accordingly.
     *
     * @param hint the hint to edit
     * @param text the text to set
     */
    private void setError(final JLabel hint, final String text) {
        m_parent.getErrorHints().add(hint);
        hint.setText(text);
        hint.setIcon(SharedIcons.ERROR.get());
        hint.setToolTipText("Error: " + text);
    }

    /**
     * Clears an error or warning for a hint label and updates the error hints accordingly.
     *
     * @param hint the hint to edit
     */
    private void clearHint(final JLabel hint) {
        m_parent.getErrorHints().remove(hint);
        hint.setText(" ");
        hint.setIcon(null);
        hint.setToolTipText(null);
    }

    /**
     * Create a panel containing the combo box for the type and its hint label.
     *
     * @return the newly created panel (which is also saved in {@link #m_typeBox}).
     */
    private JPanel makeTypeSelectionBox() {
        JPanel panel = m_typeBox;
        panel.setLayout(new GridBagLayout());

        m_typeSelection.setSelectedItem(m_parent.getVars().getType(m_index));
        m_typeSelection.addActionListener(a -> checkTypeBox(true));
        m_typeSelection.setRenderer(new TypeRenderer());

        clearHint(m_typeSelectionHint);

        final GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.NORTHWEST;
        c.gridx = 0;

        panel.add(m_typeSelection, c);
        c.gridy = 1;
        panel.add(m_typeSelectionHint, c);
        VariableCreatorNodeDialog.setPreferredWidth(panel, VariableCreatorNodeDialog.COL_TYPE_WIDTH);
        VariableCreatorNodeDialog.setPreferredWidth(m_typeSelection, VariableCreatorNodeDialog.COL_TYPE_WIDTH);
        VariableCreatorNodeDialog.setPreferredWidth(m_typeSelectionHint, VariableCreatorNodeDialog.COL_TYPE_WIDTH);
        VariableCreatorNodeDialog.setMinWidth(panel, VariableCreatorNodeDialog.COL_TYPE_WIDTH);
        VariableCreatorNodeDialog.setMinWidth(m_typeSelection, VariableCreatorNodeDialog.COL_TYPE_WIDTH);
        VariableCreatorNodeDialog.setMinWidth(m_typeSelectionHint, VariableCreatorNodeDialog.COL_TYPE_WIDTH);

        return panel;
    }

    /**
     * Create a panel containing the input field for the name and its hint label.
     *
     * @return the newly created panel (which is also saved in {@link #m_nameBox}).
     */
    private JPanel makeNameInputBox() {
        JPanel panel = m_nameBox;
        panel.setLayout(new GridBagLayout());

        if (m_parent.getVars().getName(m_index).isEmpty()) {
            m_nameInput.setText(VariableTable.DEFAULT_NAME_PREFIX + '_' + (m_index + 1));
        } else {
            m_nameInput.setText(m_parent.getVars().getName(m_index));
        }
        m_nameInput.getDocument().addDocumentListener(new DocumentListener() {
            /** The value that was preset before the current change. */
            private String m_oldValue = m_nameInput.getText();

            @Override
            public void removeUpdate(final DocumentEvent e) {
                update();
            }

            @Override
            public void insertUpdate(final DocumentEvent e) {
                update();
            }

            @Override
            public void changedUpdate(final DocumentEvent e) {
                // according to the Java documentation this is only ever called by
                // styled documents (which we are not)
            }

            /**
             * Make the necessary checks for duplicate names and update {@link #m_oldvalue}.
             */
            private void update() {
                checkDuplicateNames(m_oldValue.trim(), m_nameInput.getText().trim());
                m_oldValue = m_nameInput.getText();
            }
        });
        m_nameInput.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(final FocusEvent e) {
                super.focusGained(e);
                m_nameInput.selectAll();
            }
        });

        clearHint(m_nameInputHint);

        // trigger first check
        checkDuplicateNames(null, m_nameInput.getText());

        final GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.NORTHWEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1.0;
        c.gridx = 0;

        panel.add(m_nameInput, c);
        c.gridy = 1;
        panel.add(m_nameInputHint, c);
        VariableCreatorNodeDialog.setPreferredWidth(panel, VariableCreatorNodeDialog.COL_NAME_WIDTH);
        VariableCreatorNodeDialog.setPreferredWidth(m_nameInput, VariableCreatorNodeDialog.COL_NAME_WIDTH);
        VariableCreatorNodeDialog.setPreferredWidth(m_nameInputHint, VariableCreatorNodeDialog.COL_NAME_WIDTH);
        VariableCreatorNodeDialog.setMinWidth(panel, VariableCreatorNodeDialog.COL_NAME_WIDTH);
        VariableCreatorNodeDialog.setMinWidth(m_nameInput, VariableCreatorNodeDialog.COL_NAME_WIDTH);
        VariableCreatorNodeDialog.setMinWidth(m_nameInputHint, VariableCreatorNodeDialog.COL_NAME_WIDTH);

        panel.revalidate();
        panel.repaint();

        return panel;
    }

    /**
     * Create a panel containing the input field for the value and its hint label.
     *
     * @return the newly created panel (which is also saved in {@link #m_valueBox}).
     */
    private JPanel makeValueInputBox() {
        JPanel panel = m_valueBox;
        panel.setLayout(new GridBagLayout());

        m_valueInput.setText(m_parent.getVars().getValue(m_index).toString());
        m_valueInput.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void removeUpdate(final DocumentEvent e) {
                checkValueInputBox();
            }

            @Override
            public void insertUpdate(final DocumentEvent e) {
                checkValueInputBox();
            }

            @Override
            public void changedUpdate(final DocumentEvent e) {
                // according to the Java documentation this is only ever called by
                // styled documents (which we are not)
            }
        });
        m_valueInput.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(final FocusEvent e) {
                super.focusGained(e);
                m_valueInput.selectAll();
            }
        });

        clearHint(m_valueInputHint);

        // trigger first check
        checkValueInputBox();

        final GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.NORTHWEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1.0;
        c.gridx = 0;

        panel.add(m_valueInput, c);
        c.gridy = 1;
        panel.add(m_valueInputHint, c);
        VariableCreatorNodeDialog.setPreferredWidth(panel, VariableCreatorNodeDialog.COL_VAL_WIDTH);
        VariableCreatorNodeDialog.setPreferredWidth(m_valueInput, VariableCreatorNodeDialog.COL_VAL_WIDTH);
        VariableCreatorNodeDialog.setPreferredWidth(m_valueInputHint, VariableCreatorNodeDialog.COL_VAL_WIDTH);
        VariableCreatorNodeDialog.setMinWidth(panel, VariableCreatorNodeDialog.COL_VAL_WIDTH);
        VariableCreatorNodeDialog.setMinWidth(m_valueInput, VariableCreatorNodeDialog.COL_VAL_WIDTH);
        VariableCreatorNodeDialog.setMinWidth(m_valueInputHint, VariableCreatorNodeDialog.COL_VAL_WIDTH);

        return panel;
    }

    /**
     * Create a panel containing the three buttons used to move this row up, down, and delete it.
     *
     * @return the newly created panel (which is also saved in {@link #m_buttonBox}).
     */
    private JPanel makeButtons() {
        m_buttonBox.setLayout(new BoxLayout(m_buttonBox, BoxLayout.X_AXIS));
        final JButton up = m_up;
        up.setIcon(SharedIcons.MOVE_UP.get());
        up.setToolTipText("Move this variable up");

        final JButton down = m_down;
        down.setIcon(SharedIcons.MOVE_DOWN.get());
        down.setToolTipText("Move this variable down");

        updateMoveButtions(m_index);

        final JButton remove = m_delete;
        remove.setIcon(SharedIcons.DELETE_TRASH.get());
        remove.setToolTipText("Remove this variable");

        // make sure that the icons buttons are square
        final Dimension pref = new Dimension(up.getPreferredSize());
        pref.width = Math.max(pref.height, Math.max(down.getPreferredSize().height, remove.getPreferredSize().height));
        up.setPreferredSize(pref);
        down.setPreferredSize(pref);
        remove.setPreferredSize(pref);

        m_buttonBox.add(up);
        m_buttonBox.add(down);
        m_buttonBox.add(remove);

        up.addActionListener(l -> swapRows(m_index, m_index - 1));
        down.addActionListener(l -> swapRows(m_index, m_index + 1));
        remove.addActionListener(l -> removeRow());
        return m_buttonBox;
    }

    /**
     * Updates the variable table and validates the value input box.
     * 
     * @param popupMayBeOpen if the popup of the combo box may still be open when this is fired. This can be the case if
     *            the user is cycling through the options using the keyboard.
     */
    private void checkTypeBox(final boolean popupMayBeOpen) {
        final Type newType = (Type)m_typeSelection.getSelectedItem();
        final Type oldType = m_parent.getVars().getType(m_index);
        if (oldType == newType) {
            return;
        }

        m_parent.getVars().setType(m_index, newType);
        checkNameInputBox(m_nameInput.getText().trim());
        // only update if the default value is an exact match to prevent loosing data from the user
        if (m_valueInput.getText().equals(oldType.getDefaultStringValue())) {
            m_valueInput.setText(newType.getDefaultStringValue());
        }
        checkValueInputBox();
        if (!popupMayBeOpen) {
            m_valueInput.requestFocusInWindow();
        }
    }

    /**
     * Check whether the given new name is already defined by another variable and update the corresponding lookup
     * tables accordingly. Any conflicting rows will be updated as well.
     *
     * @param oldValue the name that was previously used (used to clean up the old entry)
     * @param newValue the name that is now used
     */
    private void checkDuplicateNames(final String oldValue, final String newValue) {
        if (oldValue != null && oldValue.equals(newValue)) {
            return;
        }
        if (oldValue != null && !oldValue.isEmpty()) {
            cleanOldNameValueFromDuplicates(oldValue);
        }

        // we will not remove the value
        if (newValue != null) {
            checkAndUpdateNewvalue(newValue);
        }
    }

    /**
     * Remove an old name value from the duplicate name registry and update remaining values accordingly.
     *
     * @param oldValue the old value to remove
     */
    private void cleanOldNameValueFromDuplicates(final String oldValue) {
        final Set<DialogVariableRow> alreadyUsing = m_parent.getUsedNames().get(oldValue);
        alreadyUsing.remove(this);
        if (alreadyUsing.size() == 1) {
            final DialogVariableRow remaining = alreadyUsing.iterator().next();
            remaining.checkNameInputBox(oldValue);
        } else if (alreadyUsing.isEmpty()) {
            m_parent.getUsedNames().remove(oldValue);
        }
    }

    /**
     * The the a new value for errors or warnings, update it accordingly and add it to the duplicate name registry. If
     * the name is a duplicate, update other fields if necessary.
     *
     * @param newValue the new name to set
     */
    private void checkAndUpdateNewvalue(final String newValue) {
        checkNameInputBox(newValue);
        if (!newValue.isEmpty()) {
            if (!m_parent.getUsedNames().containsKey(newValue)) {
                m_parent.getUsedNames().put(newValue, new HashSet<DialogVariableRow>());
            }
            final Set<DialogVariableRow> alreadyUsing = m_parent.getUsedNames().get(newValue);
            if (alreadyUsing.size() == 1) {
                final JLabel remaining = alreadyUsing.iterator().next().m_nameInputHint;
                alreadyUsing.add(this);
                setError(remaining, "Name conflict");
            } else {
                alreadyUsing.add(this);
            }
        }

    }

    /**
     * Check the value in the value input box and set the hint (as well as an error) hint accordingly.
     *
     * This also checks for duplicate names.
     *
     * @param newValue the name that is now used
     */
    private void checkNameInputBox(final String newValue) {
        final Pair<Boolean, Optional<String>> updateResult =
            m_parent.getVars().checkVariableNameExternal(m_index, newValue);
        Optional<String> messageStr = updateResult.getSecond();
        boolean noError = updateResult.getFirst();
        // this ignores only a warning and overrides it with the error if it occurs
        if (noError && m_parent.getUsedNames().containsKey(newValue)
            && !m_parent.getUsedNames().get(newValue).isEmpty()) {
            final Set<DialogVariableRow> alreadyUsing = m_parent.getUsedNames().get(newValue);
            if (alreadyUsing.size() > 1 || !alreadyUsing.contains(this)) {
                noError = false;
                messageStr = Optional.of("Name conflict");
            }
        }
        if (noError) {
            if (messageStr.isPresent()) {
                setInfo(m_nameInputHint, messageStr.get());
            } else {
                clearHint(m_nameInputHint);
            }
        } else {
            setError(m_nameInputHint, messageStr.orElse("(Unknown Error!)"));
        }
    }

    /**
     * Check the value in the value input box and set the hint (as well as an error) hint accordingly.
     */
    private void checkValueInputBox() {
        final String typed = m_valueInput.getText();
        final Pair<Boolean, Optional<String>> updateResult = m_parent.getVars().setValue(m_index, typed);
        final Optional<String> hintMsg = updateResult.getSecond();
        if (updateResult.getFirst().booleanValue()) {
            if (hintMsg.isPresent()) {
                setInfo(m_valueInputHint, hintMsg.get());
            } else {
                clearHint(m_valueInputHint);
            }
        } else {
            setError(m_valueInputHint, hintMsg.orElse("(Unknown Error!)"));
        }
    }

    /**
     * Update the usable state of the move buttons this row.
     */
    void updateMoveButtions() {
        updateMoveButtions(m_index);
    }

    /**
     * Update the usable state of the move buttons of the row at index {@code index}.
     *
     * @param index the index of the row. The method will do nothing if this value is out of bounds.
     */
    private void updateMoveButtions(final int index) {
        final int maxIdx = m_parent.getRows().size() - 1;
        if (index < 0 || index > maxIdx) {
            return;
        }

        final DialogVariableRow buttons = m_parent.getRows().get(index);
        buttons.m_up.setEnabled(index != 0);
        buttons.m_down.setEnabled(index != maxIdx);
    }

    /**
     * Represents a render for the type combo box. This code is based on the corresponding example from Oracle's
     * <a href="https://docs.oracle.com/javase/tutorial/uiswing/components/combobox.html#renderer"> Combo Box Renderer
     * Tutorial</a>.
     *
     * @author Jannik Löscher, KNIME GmbH, Konstanz, Germany
     */
    private static final class TypeRenderer extends JLabel implements ListCellRenderer<Type> {

        private static final long serialVersionUID = 5183920528373727226L;

        /**
         * Constructs a new renderer similar to that in the tutorial.
         */
        private TypeRenderer() {
            initRenderer();
        }

        /**
         * Initialize the renderer like in the tutorial
         */
        private void initRenderer() {
            setOpaque(true);
            setHorizontalAlignment(LEFT);
            setVerticalAlignment(CENTER);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Component getListCellRendererComponent(final JList<? extends Type> list, final Type value,
            final int index, final boolean isSelected, final boolean cellHasFocus) {
            if (isSelected) {
                setBackground(list.getSelectionBackground());
                setForeground(list.getSelectionForeground());
            } else {
                setBackground(list.getBackground());
                setForeground(list.getForeground());
            }

            setIcon(value.m_type.getIcon());
            setText(WordUtils.capitalizeFully(value.m_type.toString()));
            return this;
        }
    }
}
