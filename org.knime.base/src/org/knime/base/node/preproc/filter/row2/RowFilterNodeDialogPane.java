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
 * -------------------------------------------------------------------
 *
 * History
 *   07.07.2005 (ohl): created
 */
package org.knime.base.node.preproc.filter.row2;

import java.awt.Dimension;
import java.util.EnumSet;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.border.EmptyBorder;

import org.knime.base.data.filter.row.dialog.OperatorPanelParameters;
import org.knime.base.data.filter.row.dialog.component.ColumnRole;
import org.knime.base.data.filter.row.dialog.component.DefaultRowFilterElementFactory;
import org.knime.base.data.filter.row.dialog.component.RowFilterComponent;
import org.knime.base.data.filter.row.dialog.component.RowFilterElementFactory;
import org.knime.base.node.preproc.filter.row2.operator.KnimeRowFilterOperatorRegistry;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;

/**
 * The dialog for the Row Filter node.
 *
 * @author Perla Gjoka, KNIME GmbH, Konstanz, Germany
 */
public class RowFilterNodeDialogPane extends NodeDialogPane {

    private JRadioButton m_includeButton;

    private JRadioButton m_excludeButton;

    private final KnimeRowFilterConfig m_config = AbstractRowFilterNodeModel.getRowFilterConfig();

    private final RowFilterElementFactory m_elementFactory = new DefaultRowFilterElementFactory();

    private final EnumSet<ColumnRole> m_additionalColumns = EnumSet.of(ColumnRole.ROW_ID, ColumnRole.ROW_INDEX);

    private final RowFilterComponent m_rowFilter =
        new RowFilterComponent(m_config, m_elementFactory, m_additionalColumns);

    /**
     * Creates a new panel for the row filter node dialog. Except for the RowFilterDialog, also the radio button for
     * include and exclude are added to the dialog.
     */

    public RowFilterNodeDialogPane() {
        super.addTab("Filter Criteria", createRowFilterPanel());
    }

    /**
     * @return the row filter panel
     */
    protected JPanel createRowFilterPanel() {
        //keeps all sub-panels
        final JPanel bigPanel = new JPanel();
        bigPanel.setLayout(new BoxLayout(bigPanel, BoxLayout.PAGE_AXIS));
        //row filter defines panel, tree view
        final JPanel panel = m_rowFilter.getPanel();
        panel.setBorder(new EmptyBorder(2, 2, 2, 2));
        //create the include-exclude radio buttons
        ButtonGroup includeExclude = new ButtonGroup();
        m_includeButton = new JRadioButton("Include by query");
        m_excludeButton = new JRadioButton("Exclude by query");
        m_includeButton.setActionCommand("include");
        m_includeButton.setSelected(true);
        m_excludeButton.setActionCommand("exclude");
        includeExclude.add(m_includeButton);
        includeExclude.add(m_excludeButton);
        //create the include exclude panel
        final JPanel panelInclude = new JPanel();
        panelInclude.setLayout(new BoxLayout(panelInclude, BoxLayout.LINE_AXIS));
        panelInclude.add(m_includeButton);
        panelInclude.add(Box.createRigidArea(new Dimension(50, 0)));
        panelInclude.add(m_excludeButton);
        //add the subpanels to the bigpanel
        bigPanel.add(panel);
        bigPanel.add(panelInclude);
        return bigPanel;
    }

    /**
     * Loads the settings from the settings file.
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final DataTableSpec[] specs)
        throws NotConfigurableException {

        if (specs == null || specs[0].getNumColumns() < 1 || specs[0] == null) {
            throw new NotConfigurableException("No input data available");
        }
        final OperatorPanelParameters parameters = new OperatorPanelParameters();
        final KnimeRowFilterOperatorRegistry operatorRegistry = KnimeRowFilterOperatorRegistry.getInstance();

        try {
            m_rowFilter.loadSettingsFrom(settings, specs[0], parameters, operatorRegistry);
        } catch (InvalidSettingsException e) {
            throw new NotConfigurableException(e.getMessage());
        }
        final boolean queryDefinesInclude = m_config.isQueryDefinesInclude();
        m_includeButton.setSelected(queryDefinesInclude);
        m_excludeButton.setSelected(!queryDefinesInclude);
    }

    /**
     * Saves the settings to the settings file. The state of the dialog when it is closed, is saved in the settings file
     * (all the user choices).
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        m_config.setQueryDefinesInclude(m_includeButton.isSelected());
        // saves the component state and stores the settings into the provided settings object
        m_rowFilter.saveSettingsTo(settings);
    }

}