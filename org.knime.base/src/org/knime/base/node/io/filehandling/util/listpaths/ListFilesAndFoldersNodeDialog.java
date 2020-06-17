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
 *   Jun 17, 2020 (Mark Ortmann, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.base.node.io.filehandling.util.listpaths;

import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JCheckBox;
import javax.swing.JPanel;

import org.knime.core.node.FlowVariableModel;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.filehandling.core.data.location.variable.FSLocationVariableType;
import org.knime.filehandling.core.defaultnodesettings.filechooser.reader.DialogComponentReaderFileChooser;
import org.knime.filehandling.core.defaultnodesettings.filtermode.SettingsModelFilterMode.FilterMode;
import org.knime.filehandling.core.util.GBCBuilder;

/**
 * Dialog of the ListFilesAndFoldersNodeModel.
 *
 * @author Mark Ortmann, KNIME GmbH, Berlin, Germany
 */
class ListFilesAndFoldersNodeDialog extends NodeDialogPane {

    private final ListFilesAndFoldersNodeConfiguration m_config;

    private final JCheckBox m_addDirIndicator;

    private final JCheckBox m_includeRootDir;

    private final DialogComponentReaderFileChooser m_dialog;

    ListFilesAndFoldersNodeDialog(final ListFilesAndFoldersNodeConfiguration config) {
        m_config = config;

        final FlowVariableModel readFvm = createFlowVariableModel(
            config.getFileChooserSettings().getKeysForFSLocation(), FSLocationVariableType.INSTANCE);
        m_dialog = new DialogComponentReaderFileChooser(config.getFileChooserSettings(), "list_files_history", readFvm,
            FilterMode.FILES_IN_FOLDERS, FilterMode.FOLDERS, FilterMode.FILES_AND_FOLDERS);
        m_dialog.getComponentPanel().setBorder(BorderFactory.createTitledBorder("Read"));

        m_includeRootDir = new JCheckBox("Include selected directory");
        m_addDirIndicator = new JCheckBox("Add directory identfier column");

        addTab("Settings", layout());
    }

    private JPanel layout() {
        final JPanel panel = new JPanel(new GridBagLayout());
        GBCBuilder gbc = new GBCBuilder(new Insets(5, 5, 5, 5)).resetX().resetY();
        panel.add(m_dialog.getComponentPanel(), gbc.resetX().incY().fillHorizontal().setWeightX(1).build());
        gbc.incY();
        panel.add(m_includeRootDir, gbc.build());
        gbc.incY();
        panel.add(m_addDirIndicator, gbc.build());
        gbc.incY().fillBoth().setWeightY(1);
        panel.add(Box.createVerticalBox(), gbc.build());
        return panel;
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        m_dialog.saveSettingsTo(settings);

        m_config.addDirIndicatorColumn(m_addDirIndicator.isSelected());
        m_config.includeRootDir(m_includeRootDir.isSelected());

        m_config.saveSettingsForDialog(settings);
    }

    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final PortObjectSpec[] specs)
        throws NotConfigurableException {
        m_dialog.loadSettingsFrom(settings, specs);

        m_config.loadSettingsForDialog(settings);
        m_addDirIndicator.setSelected(m_config.addDirIndicatorColumn());
        m_includeRootDir.setSelected(m_config.includeRootDir());
    }

}
