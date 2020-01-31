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
 *   02.02.2005 (cebron): created
 */
package org.knime.base.node.preproc.sorter;

import java.awt.Dimension;

import javax.swing.JScrollPane;

import org.knime.base.node.preproc.sorter.dialog.DynamicSorterPanel;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;

/**
 * Dialog for choosing the columns that will be sorted. It is also possible to set the order of columns
 *
 * @author Nicolas Cebron, University of Konstanz
 */
public class SorterNodeDialog extends NodeDialogPane {
    /**
     * The tab's name.
     */
    private static final String TAB = "Sorting Filter";

    private static final String TAB_ADVANCED_SETTINGS = "Advanced Settings";

    /*
     * Hold the outer panels
     */
    private final DynamicSorterPanel m_panel;

    private final AdvancedSettings m_advancedSettings;

    /**
     * Creates a new {@link NodeDialogPane} for the Sorter Node in order to choose the desired columns and the sorting
     * order (ascending/ descending).
     */
    SorterNodeDialog() {
        super();
        m_panel = new DynamicSorterPanel(SorterNodeModel.INCLUDELIST_KEY, SorterNodeModel.SORTORDER_KEY);
        m_advancedSettings = new AdvancedSettings();
        JScrollPane scrollPane = new JScrollPane(m_panel.getPanel());
        scrollPane.setPreferredSize(new Dimension(100, 200));
        super.addTab(TAB, scrollPane);

        scrollPane = new JScrollPane(m_advancedSettings.getPanel());
        super.addTab(TAB_ADVANCED_SETTINGS, scrollPane);
    }

    /**
     * Creates a list of sorting criteria from the settings using the input data
     * table spec from this {@link SorterNodeModel} and fills the panel with the different sort criteria, if available.
     *
     * @param settings the node settings to read from
     * @param specs the input specs
     *
     * @see NodeDialogPane#loadSettingsFrom(NodeSettingsRO, DataTableSpec[])
     * @throws NotConfigurableException if the dialog cannot be opened.
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final DataTableSpec[] specs)
        throws NotConfigurableException {

        if (specs.length == 0 || specs[0] == null || specs[0].getNumColumns() == 0) {
            throw new NotConfigurableException("No columns to sort.");
        }

        m_panel.load(settings, specs);
        m_advancedSettings.load(settings);
    }

    /**
     * Sets the list of columns to include and the sorting order list inside the
     * underlying {@link SorterNodeModel} retrieving them from the
     * {@link SorterNodeDialogPanel}.
     *
     * @param settings the node settings to write into
     * @throws InvalidSettingsException if settings are not valid
     * @see NodeDialogPane#saveSettingsTo(NodeSettingsWO)
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings)
            throws InvalidSettingsException {
         m_panel.save(settings);
         m_advancedSettings.save(settings);
    }
}
