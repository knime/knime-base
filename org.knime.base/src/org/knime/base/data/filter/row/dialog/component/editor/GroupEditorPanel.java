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
 * ------------------------------------------------------------------------
 */

package org.knime.base.data.filter.row.dialog.component.editor;

import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Enumeration;
import java.util.Objects;

import javax.swing.AbstractButton;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import org.knime.base.data.filter.row.dialog.component.config.EditorPanelConfig;
import org.knime.base.data.filter.row.dialog.component.handler.TreeGroupHandler;
import org.knime.base.data.filter.row.dialog.model.GroupType;

/**
 * Panel for edit connection group.
 *
 * @author Mor Kalla
 */
public class GroupEditorPanel extends JPanel {

    private static final long serialVersionUID = -4020466189584375462L;

    private final JButton m_ungroupButton;

    private final JButton m_deleteButton;

    private final ButtonGroup m_buttonGroup;

    private final ErrorLabel m_errorLabel;

    private TreeGroupHandler m_group;

    /**
     * Constructs a {@link GroupEditorPanel}.
     *
     * @param config the {@link EditorPanelConfig}
     * @param errorLabel the {@link JLabel} component to show group errors.
     */
    public GroupEditorPanel(final EditorPanelConfig config, final ErrorLabel errorLabel) {
        Objects.requireNonNull(config, "config");
        m_errorLabel = Objects.requireNonNull(errorLabel, "errorLabel");

        setLayout(new GridBagLayout());

        final JLabel label = new JLabel("Edit condition group");

        final GridBagConstraints gbc = new GridBagConstraints();

        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.insets = new Insets(10, 2, 2, 2);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        gbc.gridheight = 1;
        gbc.weightx = 1;
        gbc.weighty = 1;
        add(label, gbc);

        // Button "Ungroup"
        m_ungroupButton = new JButton("Ungroup");
        m_ungroupButton.addActionListener(e -> {
            if (m_group != null) {
                m_group.ungroup();
            }
        });
        gbc.gridx++;
        add(m_ungroupButton, gbc);

        //Button "Delete"
        m_deleteButton = new JButton("Delete");
        m_deleteButton.addActionListener(e -> {
            if (m_group != null) {
                m_group.delete();
            }
        });
        gbc.gridx++;
        add(m_deleteButton, gbc);

        final JPanel radioButonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        radioButonPanel.add(new JLabel("Connection"));

        m_buttonGroup = new ButtonGroup();
        for (GroupType groupType : config.getGroupTypes()) {
            final JRadioButton button = new JRadioButton(groupType.getName());
            button.addActionListener(e -> {
                m_group.get().setType(groupType);
                updateTreeGroup();
            });
            m_buttonGroup.add(button);
            radioButonPanel.add(button);
        }

        gbc.gridx = 0;
        gbc.gridwidth = 2;
        gbc.weighty = 50;
        gbc.gridy++;
        add(radioButonPanel, gbc);
    }

    /**
     * Displays a group object.
     *
     * @param group the {@linkplain TreeGroupHandler group}
     */
    public void display(final TreeGroupHandler group) {
        m_group = Objects.requireNonNull(group, "group");

        m_ungroupButton.setEnabled(m_group.supportsUngroup());

        final Enumeration<AbstractButton> buttons = m_buttonGroup.getElements();
        while (buttons.hasMoreElements()) {
            final AbstractButton button = buttons.nextElement();
            final GroupType groupType = group.get().getType();
            if (groupType.getName().equals(button.getText())) {
                button.setSelected(true);
                break;
            }
        }

        updateTreeGroup();
    }

    private void updateTreeGroup() {
        m_group.updateView();

        m_errorLabel.setErrors(m_group.getValidationResult());
    }

}
