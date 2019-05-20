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
 *   May 6, 2019 (Mark Ortmann, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.base.node.mine.transformation.pca.apply;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.Box;
import javax.swing.JPanel;

import org.knime.base.node.mine.transformation.pca.perform.DimensionSelectionPanel;
import org.knime.base.node.mine.transformation.pca.settings.PCAApplySettings;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.port.PortObjectSpec;

/**
 * The PCA apply node dialog.
 *
 * @author Mark Ortmann, KNIME GmbH, Berlin, Germany
 */
final class PCA2ApplyNodeDialog extends NodeDialogPane {

    private final DimensionSelectionPanel m_dimSelection;

    private final DialogComponentBoolean m_remUsedCols;

    private final DialogComponentBoolean m_failOnMissingsComp;

    PCA2ApplyNodeDialog() {
        final PCAApplySettings applySettings = new PCAApplySettings();
        m_dimSelection = new DimensionApplyPanel(applySettings, PCA2ApplyNodeModel.MODEL_IN_PORT);
        m_remUsedCols =
            new DialogComponentBoolean(applySettings.getRemoveUsedColsModel(), "Remove original data columns");
        m_failOnMissingsComp = new DialogComponentBoolean(applySettings.getFailOnMissingsModel(),
            "Fail if missing values are encountered");
        addTab("Settings", createPanel());
    }

    private Component createPanel() {
        final JPanel p = new JPanel();
        p.setLayout(new GridBagLayout());
        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        gbc.weighty = 0;
        gbc.gridy = 0;
        p.add(m_dimSelection.getMainPanel(), gbc);
        ++gbc.gridy;
        p.add(m_remUsedCols.getComponentPanel(), gbc);
        ++gbc.gridy;
        p.add(m_failOnMissingsComp.getComponentPanel(), gbc);
        ++gbc.gridy;
        gbc.insets = new Insets(0, 5, 0, 0);
        p.add(m_dimSelection.getErrorPanel(), gbc);
        gbc.weightx = 1;
        gbc.weighty = 1;
        gbc.fill = GridBagConstraints.BOTH;
        p.add(Box.createVerticalBox(), gbc);
        return p;
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        m_dimSelection.saveSettingsTo(settings);
        m_remUsedCols.saveSettingsTo(settings);
        m_failOnMissingsComp.saveSettingsTo(settings);
    }

    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final PortObjectSpec[] specs)
        throws NotConfigurableException {
        if (specs[PCA2ApplyNodeModel.MODEL_IN_PORT] == null) {
            throw new NotConfigurableException("Model input missing");
        }
        m_remUsedCols.loadSettingsFrom(settings, specs);
        m_failOnMissingsComp.loadSettingsFrom(settings, specs);
        m_dimSelection.loadSettingsFrom(settings, specs);
    }

}
