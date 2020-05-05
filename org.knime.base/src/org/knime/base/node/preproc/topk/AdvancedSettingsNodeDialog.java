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
 *   9 Dec 2019 (Timmo Waller-Ehrat, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.base.node.preproc.topk;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.Box;
import javax.swing.JPanel;

import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentButtonGroup;

/**
 * NodeDialog tab for the Top K Selector Node.
 *
 * @author Timmo Waller-Ehrat, KNIME GmbH, Konstanz, Germany
 * @author Lars Schweikardt, KNIME GmbH, Konstanz, Germany
 */
final class AdvancedSettingsNodeDialog {

    private final TopKSelectorSettings m_settings;

    private final DialogComponentBoolean m_missingsToEnd;

    private final DialogComponentButtonGroup m_outputOrder;

    private final DialogComponentButtonGroup m_topKMode;

    /**
     * Creates a new Panel with a checkBox for "Move missing cells to end of the list" and a GroupBox for choosing the
     * output order
     *
     * @param settings TopKSelectorSettings object to load and save settings
     */
    public AdvancedSettingsNodeDialog(final TopKSelectorSettings settings) {
        m_settings = settings;
        m_missingsToEnd =
            new DialogComponentBoolean(m_settings.getMissingToEndModel(), "Move missing cells to end of sorted list");
        m_outputOrder = new DialogComponentButtonGroup(m_settings.getOutputOrderModel(), "Output order", true,
            OutputOrder.values());
        m_topKMode = new DialogComponentButtonGroup(m_settings.getTopKModeModel(), "Selection mode", true,
            TopKMode.values());
    }

    /**
     * Creates the {@link JPanel} for the tab.
     *
     * @return the JPanel
     */
    public JPanel getPanel() {
        final JPanel panel = new JPanel(new GridBagLayout());
        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.FIRST_LINE_START;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        gbc.weighty = 0;
        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(m_topKMode.getComponentPanel(), gbc);
        ++gbc.gridy;
        panel.add(m_outputOrder.getComponentPanel(), gbc);
        ++gbc.gridy;
        panel.add(m_missingsToEnd.getComponentPanel(), gbc);
        gbc.weightx = 1;
        gbc.weighty = 1;
        gbc.fill = GridBagConstraints.BOTH;
        panel.add(Box.createHorizontalBox(), gbc);
        return panel;
    }
}
