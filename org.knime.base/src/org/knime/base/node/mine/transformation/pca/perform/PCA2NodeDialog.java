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
 *   May 2, 2019 (Mark Ortmann, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.base.node.mine.transformation.pca.perform;

import javax.swing.BoxLayout;
import javax.swing.JPanel;

import org.knime.base.node.mine.transformation.pca.AbstractPCA2NodeModel;
import org.knime.base.node.mine.transformation.pca.settings.PCAApplySettings;
import org.knime.base.node.mine.transformation.settings.TransformationComputeSettings;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnFilter2;
import org.knime.core.node.defaultnodesettings.SettingsModelColumnFilter2;

/**
 * The PCA node dialog.
 *
 * @author Mark Ortmann, KNIME GmbH, Berlin, Germany
 */
final class PCA2NodeDialog extends NodeDialogPane {

    private final DimensionSelectionPanel m_dimSelection;

    private final SettingsModelColumnFilter2 m_usedColsModel;

    private final DialogComponentColumnFilter2 m_usedColsComponent;

    private final DialogComponentBoolean m_remUsedColsComp;

    private final DialogComponentBoolean m_failOnMissingsComp;

    private DataTableSpec[] m_lastSpecs;

    private final JPanel m_panel;

    /**
     * Constructor.
     */
    PCA2NodeDialog() {
        final TransformationComputeSettings compSettings = new TransformationComputeSettings();
        final PCAApplySettings applySettings = new PCAApplySettings();

        m_dimSelection = new DimensionSelectionPanel(applySettings);

        m_usedColsModel = compSettings.getUsedColsModel();

        m_usedColsModel.addChangeListener(l -> updateSettings());
        m_usedColsComponent = new DialogComponentColumnFilter2(m_usedColsModel, AbstractPCA2NodeModel.DATA_IN_PORT);

        m_remUsedColsComp =
            new DialogComponentBoolean(applySettings.getRemoveUsedColsModel(), "Remove original data columns");

        m_failOnMissingsComp =
            new DialogComponentBoolean(compSettings.getFailOnMissingsModel(), "Fail if missing values are encountered");

        m_panel = new JPanel();
        final BoxLayout bl = new BoxLayout(m_panel, 1);
        m_panel.setLayout(bl);
        m_panel.add(m_dimSelection.getMainPanel());
        m_panel.add(m_usedColsComponent.getComponentPanel());
        m_panel.add(m_remUsedColsComp.getComponentPanel());
        m_panel.add(m_failOnMissingsComp.getComponentPanel());
        m_panel.add(m_dimSelection.getErrorPanel());
        addTab("Settings", m_panel);
    }

    private void updateSettings() {
        if ((m_lastSpecs != null) && (m_lastSpecs.length > 0)) {
            m_dimSelection.updateMaxDim(
                m_usedColsModel.applyTo(m_lastSpecs[AbstractPCA2NodeModel.DATA_IN_PORT]).getIncludes().length);
        } else {
            m_dimSelection.updateMaxDim(Integer.MAX_VALUE);
        }
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        m_dimSelection.saveSettingsTo(settings);
        m_usedColsComponent.saveSettingsTo(settings);
        m_remUsedColsComp.saveSettingsTo(settings);
        m_failOnMissingsComp.saveSettingsTo(settings);
    }

    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final DataTableSpec[] specs)
        throws NotConfigurableException {
        m_lastSpecs = specs; // Needed to get the number of selected columns.
        m_usedColsComponent.loadSettingsFrom(settings, specs);
        m_remUsedColsComp.loadSettingsFrom(settings, specs);
        m_failOnMissingsComp.loadSettingsFrom(settings, specs);
        m_dimSelection.loadSettingsFrom(settings, specs);
        updateSettings();
    }
}
