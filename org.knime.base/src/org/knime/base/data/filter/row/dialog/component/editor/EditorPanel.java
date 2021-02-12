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

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JPanel;

import org.knime.base.data.filter.row.dialog.component.config.EditorPanelConfig;
import org.knime.base.data.filter.row.dialog.component.handler.TreeConditionHandler;
import org.knime.base.data.filter.row.dialog.component.handler.TreeGroupHandler;

/**
 * Condition panel for database row filter node.
 *
 * @author Mor Kalla
 */
public final class EditorPanel extends JPanel {

    private static final long serialVersionUID = 1253685075052035986L;

    private static final Dimension PANEL_PREFERRED_SIZE = new Dimension(420, 350);

    private final GroupEditorPanel m_groupPanel;

    private final ConditionEditorPanel m_conditionPanel;

    private final ErrorLabel m_errorLabel;

    private final transient EditorPanelConfig m_config;

    /**
     * Constructs an {@link EditorPanel}.
     *
     * @param config the {@link EditorPanelConfig}
     */
    public EditorPanel(final EditorPanelConfig config) {
        m_config = config;

        m_errorLabel = new ErrorLabel();

        m_groupPanel = new GroupEditorPanel(m_config, m_errorLabel);
        m_groupPanel.setVisible(false);

        m_conditionPanel = new ConditionEditorPanel(m_config, m_errorLabel);
        m_conditionPanel.setVisible(false);

        setLayout(new GridBagLayout());
        setPreferredSize(PANEL_PREFERRED_SIZE);

        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.insets = new Insets(2, 2, 2, 2);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        gbc.weightx = 1;
        gbc.weighty = 1;

        add(m_groupPanel, gbc);
        add(m_conditionPanel, gbc);

        gbc.gridy++;
        add(m_errorLabel, gbc);
    }

    /**
     * Cancels every execution that is currently in progress in this panel.
     */
    public void cancelEveryExecution() {
        m_conditionPanel.cancelEveryExecution();
    }

    /**
     * Shows group editor panel.
     *
     * @param group the {@linkplain TreeGroupHandler group}
     */
    public void showGroup(final TreeGroupHandler group) {
        m_errorLabel.setErrors(null);

        m_conditionPanel.setVisible(false);

        m_groupPanel.display(group);

        m_groupPanel.setVisible(true);
    }

    /**
     * Shows group editor panel.
     *
     * @param condition the {@linkplain TreeConditionHandler condition}
     */
    public void showCondition(final TreeConditionHandler condition) {
        m_errorLabel.setErrors(null);

        m_groupPanel.setVisible(false);

        m_conditionPanel.display(condition);

        m_conditionPanel.setVisible(true);
    }

    /**
     * Hides all editor panels.
     */
    public void hidePanel() {
        m_errorLabel.setErrors(null);

        m_groupPanel.setVisible(false);

        m_conditionPanel.setVisible(false);
    }

    /**
     * Save resent changes to a domain model.
     */
    public void saveChanges() {
        if (m_conditionPanel.isVisible()) {
            m_conditionPanel.saveChanges();
        }
    }

}
