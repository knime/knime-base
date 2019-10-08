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
 *   Jul 22, 2019 (Perla Gjoka, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.base.data.filter.row.dialog.panel;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.knime.base.data.filter.row.dialog.OperatorPanelParameters;
import org.knime.base.data.filter.row.dialog.ValidationResult;
import org.knime.core.data.DataCell;

/**
 * A text panel with a drop down for the possible values and also checkboxes to specify whether the user provided string
 * should be interpreted case sensitive, as regular expression or as wildcard expression. This is similar to the panel
 * available in the old KNIME Row Filter implementation.
 *
 * @author Perla Gjoka, KNIME GmbH, Konstanz, Germany
 * @since 4.1
 */
public final class PatternMatchPanel extends AbstractFieldPanel {

    private static final long serialVersionUID = 7505889814476637077L;

    private final JCheckBox m_caseSensitive;

    private final JCheckBox m_wildcard;

    private final JCheckBox m_regularExpression;

    private final JComboBox<DataCell> m_possiblePatterns;

    private JTextField m_textField;

    /**
     * Creates the specific panel for the Pattern Match Operator of Strings
     */
    public PatternMatchPanel() {
        //creates the comboBox which hold the possible values of the column selected
        m_possiblePatterns = new JComboBox<>();
        m_possiblePatterns.setEditable(true);
        m_possiblePatterns.setSelectedItem("");
        m_textField = (JTextField)m_possiblePatterns.getEditor().getEditorComponent();
        //add Listener for detecting entering of values
        m_textField.addKeyListener(new KeyListener() {
            @Override
            public void keyPressed(final KeyEvent e) {
                notifyListeners();
            }

            @Override
            public void keyReleased(final KeyEvent e) {
                notifyListeners();
            }

            @Override
            public void keyTyped(final KeyEvent e) {
                notifyListeners();
            }

        });
        m_possiblePatterns.addItemListener(e -> notifyListeners());
        m_possiblePatterns.setMinimumSize(new Dimension(50, m_possiblePatterns.getPreferredSize().height));
        m_possiblePatterns.setMaximumSize(new Dimension(300, 50));
        m_caseSensitive = new JCheckBox("case sensitive match");
        m_wildcard = new JCheckBox("contains wild cards");
        m_wildcard.addActionListener(this::listenToAction);
        m_regularExpression = new JCheckBox("regular expression");
        m_regularExpression.addActionListener(this::listenToAction);
        performLayout();
    }

    private void performLayout() {
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        m_possiblePatterns.setAlignmentX(LEFT_ALIGNMENT);
        add(m_possiblePatterns, gbc);
        gbc.weightx = 1;
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(10, 0, 0, 0);
        //create string manipulation panel
        add(createMatchingOptionsPanel(), gbc);
    }

    private JPanel createMatchingOptionsPanel() {
        final JPanel matchingOptionsPanel = new JPanel();
        matchingOptionsPanel.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridwidth = 2;
        c.ipadx = 70;
        matchingOptionsPanel.setBorder(
            BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "String Matching Criteria"));
        matchingOptionsPanel.add(m_caseSensitive, c);
        c.ipadx = 10;
        c.gridx = 2;
        c.gridy = 0;
        matchingOptionsPanel.add(m_wildcard, c);
        c.gridx = 2;
        c.gridy = 2;
        matchingOptionsPanel.add(m_regularExpression, c);
        return matchingOptionsPanel;
    }

    /**
     * Initialize the values of the user interface components. Checking as well for unexpected number of parameters.
     */
    @Override
    public void init(final OperatorPanelParameters parameters) {
        final String[] values = parameters.getValues();
        parameters.getColumnSpec().getPossibleValues().forEach(m_possiblePatterns::addItem);
        m_possiblePatterns.setSelectedItem(values == null || values.length < 1 ? "" : values[0]);
        m_caseSensitive.setSelected(Boolean.parseBoolean(values == null || values.length < 2 ? "" : values[1]));
        m_wildcard.setSelected(Boolean.parseBoolean(values == null || values.length < 3 ? "" : values[2]));
        m_regularExpression.setSelected(Boolean.parseBoolean(values == null || values.length < 4 ? "" : values[3]));

    }

    /**
     * {@inheritDoc} Get the values of the parameters.
     */
    @Override
    public String[] getValues() {
        return new String[]{m_textField.getText(), Boolean.toString(m_caseSensitive.isSelected()),
            Boolean.toString(m_wildcard.isSelected()), Boolean.toString(m_regularExpression.isSelected())};
    }

    /**
     * {@inheritDoc} Validation of the textfield entry.
     */
    @Override
    public void setValidationResult(final ValidationResult result) {
        setValidationResult(result, m_textField, 0);
    }

    /**
     * Action Listener attached to wildcard and regular expression. Makes their selection exclusive (one or the other,
     * but not both).
     */
    private void listenToAction(final ActionEvent actionEvent) {
        AbstractButton abstractButton = (AbstractButton)actionEvent.getSource();
        boolean selected = abstractButton.getModel().isSelected();
        if (selected) {
            if (abstractButton == m_wildcard) {
                m_regularExpression.setSelected(false);
            } else if (abstractButton == m_regularExpression) {
                m_wildcard.setSelected(false);
            }
        }
    }

}
