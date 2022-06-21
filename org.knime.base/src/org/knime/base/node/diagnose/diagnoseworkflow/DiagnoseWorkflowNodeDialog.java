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
 *   21 Jun 2022 (jasper): created
 */
package org.knime.base.node.diagnose.diagnoseworkflow;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.Collections;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.util.filter.StringFilterPanel;

/**
 * Dialog for the new diagnose workflow node.
 * Allows for setting a max depth parameter and selecting included properties.
 *
 * @author Jasper Krauter, KNIME AG, Schloss
 * @author Leon Wenzler, KNIME AG, Schloss
 */
public class DiagnoseWorkflowNodeDialog extends NodeDialogPane {

    // dialog labels
    static final String CFGLABEL_MAXDEPTH = "Max depth:";
    static final String CFGLABEL_INCLUDECOLUMNS = "Include columns:";

    // dialog settings
    private final SpinnerNumberModel m_maxDepthSelector;
    private final StringFilterPanel m_filterPanel;

    /**
     * Create the node dialog
     */
    public DiagnoseWorkflowNodeDialog() {
        var m = DiagnoseWorkflowNodeModel.createMaxDepthSettingsModel();
        m_maxDepthSelector = new SpinnerNumberModel(m.getIntValue(), m.getLowerBound(), m.getUpperBound(), 1);

        m_filterPanel = new StringFilterPanel();
        String[] allIncludes = DiagnoseWorkflowNodeModel.getPropertiesAsStringArray();
        m_filterPanel.update(List.of(allIncludes), Collections.emptyList(), allIncludes);

        JPanel panel = new JPanel(new BorderLayout());
        JPanel north = new JPanel(new FlowLayout(FlowLayout.CENTER));
        north.add(new JLabel(CFGLABEL_MAXDEPTH));
        north.add(new JSpinner(m_maxDepthSelector));
        panel.add(north, BorderLayout.NORTH);
        panel.add(m_filterPanel, BorderLayout.CENTER);
        addTab("Properties", panel);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        settings.addInt(DiagnoseWorkflowNodeModel.CFGKEY_MAXDEPTH, m_maxDepthSelector.getNumber().intValue());
        String[] includes = m_filterPanel.getIncludedNamesAsSet().toArray(new String[0]);
        settings.addStringArray(DiagnoseWorkflowNodeModel.CFGKEY_INCLUDES, includes);
        String[] excludes = m_filterPanel.getExcludedNamesAsSet().toArray(new String[0]);
        settings.addStringArray(DiagnoseWorkflowNodeModel.CFGKEY_EXCLUDES, excludes);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final DataTableSpec[] specs) {
        final var allProps = DiagnoseWorkflowNodeModel.getPropertiesAsStringArray();

        m_maxDepthSelector.setValue(settings.getInt(DiagnoseWorkflowNodeModel.CFGKEY_MAXDEPTH, 2));
        final var selectedIncludes = settings.getStringArray(DiagnoseWorkflowNodeModel.CFGKEY_INCLUDES, allProps);
        final var selectedExcludes = settings.getStringArray(DiagnoseWorkflowNodeModel.CFGKEY_EXCLUDES, allProps);

        var nothingWasSelected = selectedIncludes == null || selectedIncludes.length == 0;
        if (!nothingWasSelected) {
            m_filterPanel.update(List.of(selectedIncludes), List.of(selectedExcludes), allProps);
        }
    }
}
