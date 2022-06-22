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

import javax.swing.JPanel;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.json.JSONCell;
import org.knime.core.data.xml.XMLCell;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.util.DefaultStringIconOption;
import org.knime.core.node.util.StringIconOption;
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
    static final String LABEL_COMPONENTS = "Include component contents";
    static final String LABEL_FORMAT = "Output format:";

    // options for the output format selection
    private static final StringIconOption[] OPTIONS =
        {new DefaultStringIconOption(DiagnoseWorkflowNodeModel.FMT_SELECTION_JSON, JSONCell.TYPE.getIcon()),
            new DefaultStringIconOption(DiagnoseWorkflowNodeModel.FMT_SELECTION_XML, XMLCell.TYPE.getIcon()),};

    // dialog settings
    private final DialogComponentBoolean m_scanComponentsSelector;
    private final DialogComponentStringSelection m_outputFormatSelector;
    private final StringFilterPanel m_filterPanel;

    /**
     * Create the node dialog
     */
    public DiagnoseWorkflowNodeDialog() {
        m_scanComponentsSelector =
            new DialogComponentBoolean(DiagnoseWorkflowNodeModel.createComponentScanSettingsModel(), LABEL_COMPONENTS);

        m_outputFormatSelector = new DialogComponentStringSelection(
            DiagnoseWorkflowNodeModel.createOutputFormatSelectionModel(), LABEL_FORMAT, OPTIONS);

        m_filterPanel = new StringFilterPanel();
        String[] allIncludes = DiagnoseWorkflowNodeModel.getPropertiesAsStringArray();
        m_filterPanel.update(List.of(allIncludes), Collections.emptyList(), allIncludes);

        JPanel panel = new JPanel(new BorderLayout());
        JPanel north = new JPanel(new FlowLayout(FlowLayout.CENTER));
        north.add(m_scanComponentsSelector.getComponentPanel());
        north.add(m_outputFormatSelector.getComponentPanel());
        panel.add(north, BorderLayout.NORTH);
        panel.add(m_filterPanel, BorderLayout.CENTER);
        addTab("Properties", panel);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        m_scanComponentsSelector.getModel().saveSettingsTo(settings);
        m_outputFormatSelector.getModel().saveSettingsTo(settings);
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

        try {
            m_scanComponentsSelector.getModel().loadSettingsFrom(settings);
            m_outputFormatSelector.getModel().loadSettingsFrom(settings);
        } catch (InvalidSettingsException e) {
        }

        final var selectedIncludes = settings.getStringArray(DiagnoseWorkflowNodeModel.CFGKEY_INCLUDES, allProps);
        final var selectedExcludes = settings.getStringArray(DiagnoseWorkflowNodeModel.CFGKEY_EXCLUDES, allProps);

        var nothingWasSelected = selectedIncludes == null || selectedIncludes.length == 0;
        if (!nothingWasSelected) {
            m_filterPanel.update(List.of(selectedIncludes), List.of(selectedExcludes), allProps);
        }
    }
}
