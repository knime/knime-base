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

import javax.swing.BoxLayout;
import javax.swing.JPanel;

import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentButtonGroup;

/**
 *
 * @author Timmo Waller-Ehrat, KNIME GmbH, Konstanz, Germany
 */
final class AdvancedSettings {

    private JPanel m_panel;

    private final TopKSelectorSettings m_settings;

    /**
     * Creates a new Panel with a checkBox for "Move missing cells to end of the list" and a GroupBox for choosing the
     * output order
     *
     * @param settings TopKSelectorSettings object to load and save settings
     */
    public AdvancedSettings(final TopKSelectorSettings settings) {
        m_settings = settings;

        m_panel = new JPanel();
        m_panel.setLayout(new BoxLayout(m_panel, BoxLayout.Y_AXIS));

        final DialogComponentBoolean missingsToEnd =
            new DialogComponentBoolean(m_settings.getMissingToEndModel(), "Move missing cells to end of sorted list");
        m_panel.add(missingsToEnd.getComponentPanel());

        final DialogComponentButtonGroup outputOrder = new DialogComponentButtonGroup(m_settings.getOutputOrderModel(),
            "Output order", true, OutputOrder.values());
        m_panel.add(outputOrder.getComponentPanel());
    }

    public JPanel getPanel() {
        return m_panel;
    }

//    public void load(final NodeSettingsRO settings) throws NotConfigurableException {
//        try {
//            m_settings.loadValidatedSettingsFrom(settings);
//        } catch (InvalidSettingsException e) {
//            throw new NotConfigurableException("Couldn't load settings", e);
//        }
//    }
//
//    public void save(final NodeSettingsWO settings) {
//        m_settings.saveSettingsTo(settings);
//    }
}
