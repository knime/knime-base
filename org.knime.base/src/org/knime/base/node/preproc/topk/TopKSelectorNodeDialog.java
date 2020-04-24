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

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.knime.base.node.preproc.sorter.dialog.DynamicSorterPanel;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponentButtonGroup;
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

    private final AdvancedSettingsNodeDialog m_advancedSettings;

    private final DialogComponentNumber m_kComp;

    private final DialogComponentButtonGroup m_topKMode;

    /**
     *
     */
    public TopKSelectorNodeDialog() {

        super();

        m_settings = new TopKSelectorSettings();
        m_advancedSettings = new AdvancedSettingsNodeDialog(m_settings);
        m_topKMode =  new DialogComponentButtonGroup(m_settings.getTopKModeModel(), null, true, TopKMode.values());

        m_panel = new DynamicSorterPanel(TopKSelectorNodeModel.INCLUDELIST_KEY, TopKSelectorNodeModel.SORTORDER_KEY);

        m_kComp = new DialogComponentNumber(m_settings.getKModel(), null, 1);

        super.addTab(TAB, createPanel(), false);

        super.addTab(TAB_ADVANCED_SETTINGS, m_advancedSettings.getPanel());
    }

    /**
     * Creates the first tab for the top k Node
     *
     * @return the JScrollPane
     */
    private JScrollPane createPanel() {
        final JPanel p = new JPanel(new GridBagLayout());
        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.FIRST_LINE_START;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 0.2;
        gbc.weighty = 1;

        ++gbc.gridx;
        ++gbc.gridy;
        p.add(createInnerPanel(m_topKMode.getComponentPanel(), "Top k Mode"), gbc);
        gbc.weightx = 0.8;
        ++gbc.gridx;
        p.add(createInnerPanel(m_kComp.getComponentPanel(),"Number of rows"), gbc);

        gbc.gridwidth = 2;
        gbc.gridx = 0;
        gbc.weightx = 1;
        ++gbc.gridy;

        p.add(m_panel.getPanel(), gbc);

        return new JScrollPane(p);
    }

    /**
     * Creates a JPanel {@link JPanel} with the passed {@link Component} and sets a title of the border.
     *
     * @param component Component which will be added to the JPanel
     * @param title title of the Border
     * @return a {@link JPanel}
     */
    private static JPanel createInnerPanel(final Component component, final String title) {
        final JPanel panel = new JPanel(new GridBagLayout());
        final GridBagConstraints gbc = createInnerPanelGridBagConstraint();
        panel.setBorder(BorderFactory.createTitledBorder(title));
        panel.add(component, gbc);
        return panel;
    }

    /**
     * Creates the default {@link GridBagConstraints} for the inner {@link JPanel}
     *
     * @return the default {@link GridBagConstraints}
     */
    private static GridBagConstraints createInnerPanelGridBagConstraint() {
        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.LINE_START;
        gbc.weightx = 1;
        gbc.weighty = 1;
        return gbc;
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
