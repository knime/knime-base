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
 *   Aug 16, 2019 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.base.node.preproc.topk;

import java.awt.Dimension;

import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.knime.base.node.preproc.sorter.dialog.DynamicSorterPanel;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;

/**
 * Dialog for the Element Selector node.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
final class TopKSelectorNodeDialog extends NodeDialogPane {

    /**
     * The tab's name.
     */
    private static final String TAB = "Settings";

    private static final String TAB_ADVANCED_SETTINGS = "Advanced Settings";

    private final TopKSelectorSettings m_settings;

    private final DynamicSorterPanel m_panel;

    private final AdvancedSettings m_advancedSettings;

    /**
     *
     */
    public TopKSelectorNodeDialog() {

        super();

        m_settings = new TopKSelectorSettings();
        m_advancedSettings = new AdvancedSettings(m_settings);

        m_panel = new DynamicSorterPanel(TopKSelectorNodeModel.INCLUDELIST_KEY, TopKSelectorNodeModel.SORTORDER_KEY);

        final DialogComponentNumber kComp = new DialogComponentNumber(m_settings.getKModel(), "Number of rows", 1);

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));

        JPanel tempPanel = kComp.getComponentPanel();
        tempPanel.setPreferredSize(new Dimension(0, 25));
        tempPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        mainPanel.add(tempPanel);
        mainPanel.add(m_panel.getPanel());
        JScrollPane scrollPane = new JScrollPane(mainPanel);
        scrollPane.setPreferredSize(new Dimension(100, 250));
        super.addTab(TAB, scrollPane);

        scrollPane = new JScrollPane(m_advancedSettings.getPanel());
        super.addTab(TAB_ADVANCED_SETTINGS, scrollPane);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        m_settings.saveSettingsTo(settings);
        m_panel.save(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final DataTableSpec[] specs)
        throws NotConfigurableException {
        final DataTableSpec spec = specs[TopKSelectorNodeModel.IN_DATA];
        if (spec == null || spec.getNumColumns() < 1) {
            throw new NotConfigurableException("No columns to select by.");
        }

        m_panel.load(settings, specs);

        try {
            m_settings.loadValidatedSettingsFrom(settings);

        } catch (InvalidSettingsException e) {
            throw new NotConfigurableException("Couldn't load settings", e);
        }
    }
}
