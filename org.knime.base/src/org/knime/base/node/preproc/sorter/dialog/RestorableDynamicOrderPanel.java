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
 *   5 Dec 2019 (Timmo Waller-Ehrat, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.base.node.preproc.sorter.dialog;

import javax.swing.JPanel;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.port.PortObjectSpec;

/**
 * This class holds a DynamicOrderPanel for displaying a list of items and a
 * RestorableDynamicItemContext to manage the underlying item context and be abled to load and save settings.
 *
 * @author Timmo Waller-Ehrat, KNIME GmbH, Konstanz, Germany
 * @param <T> DynamicPanelItem>
 *
 * @since 4.2
 */
final class RestorableDynamicOrderPanel<T extends DynamicPanelItem> {

    private final RestorableDynamicItemContext<T> m_context;

    private final DynamicOrderPanel<T> m_panel;

    /**
     * @param context the context which manages the list of items
     */
    public RestorableDynamicOrderPanel(final RestorableDynamicItemContext<T> context) {
        m_context = context;
        m_panel = new DynamicOrderPanel<>(context);
    }

    /**
     *
     * @return the panel which holds a list of items
     */
    public JPanel getPanel() {
        return m_panel.getPanel();
    }

    /**
     * Load the settings from a previous configuration into the dialog
     *
     * @param settings the node settings to read from
     * @param specs the input specs
     *
     * @throws NotConfigurableException if the dialog cannot be opened.
     */
    public void load(final NodeSettingsRO settings, final PortObjectSpec[] specs) throws NotConfigurableException {
        m_context.load(settings, specs);
        m_panel.setItemContext(m_context);
    }

    /**
     * Save the settings configured in the dialog
     *
     * @param settings the node settings to write into
     * @throws InvalidSettingsException if settings are not valid
     */
    public void save(final NodeSettingsWO settings) throws InvalidSettingsException {
        m_context.save(settings);
    }

}
