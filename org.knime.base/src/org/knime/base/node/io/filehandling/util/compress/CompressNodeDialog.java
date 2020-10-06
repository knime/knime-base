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
 *   27 Aug 2020 (Timmo Waller-Ehrat, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.base.node.io.filehandling.util.compress;

import java.awt.GridBagLayout;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JPanel;

import org.knime.base.node.io.filehandling.util.IncludeParentFolderAvailableSwingWorker;
import org.knime.base.node.io.filehandling.util.SwingWorkerManager;
import org.knime.core.node.FlowVariableModel;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.context.ports.PortsConfiguration;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.filehandling.core.data.location.variable.FSLocationVariableType;
import org.knime.filehandling.core.defaultnodesettings.filechooser.reader.DialogComponentReaderFileChooser;
import org.knime.filehandling.core.defaultnodesettings.filechooser.reader.SettingsModelReaderFileChooser;
import org.knime.filehandling.core.defaultnodesettings.filechooser.writer.DialogComponentWriterFileChooser;
import org.knime.filehandling.core.defaultnodesettings.filechooser.writer.SettingsModelWriterFileChooser;
import org.knime.filehandling.core.defaultnodesettings.filtermode.SettingsModelFilterMode.FilterMode;
import org.knime.filehandling.core.util.GBCBuilder;

/**
 * Node Dialog for the "Compress Files/Folder" node
 *
 * @author Timmo Waller-Ehrat, KNIME GmbH, Konstanz, Germany
 */
final class CompressNodeDialog extends NodeDialogPane {

    private static final String FILE_HISTORY_ID = "compress_files_history";

    private final DialogComponentReaderFileChooser m_inputFileChooserPanel;

    private final DialogComponentWriterFileChooser m_destinationFileChooserPanel;

    private final JCheckBox m_includeSelected;

    private final CompressNodeConfig m_config;

    private final SwingWorkerManager m_includeParentSwingWorkerManager;

    CompressNodeDialog(final PortsConfiguration portsConfig) {
        m_config = new CompressNodeConfig(portsConfig);

        final SettingsModelReaderFileChooser sourceLocationChooserModel = m_config.getInputLocationChooserModel();
        final SettingsModelWriterFileChooser destinationFileChooserModel = m_config.getTargetFileChooserModel();

        final FlowVariableModel readFvm =
            createFlowVariableModel(sourceLocationChooserModel.getKeysForFSLocation(), FSLocationVariableType.INSTANCE);
        m_inputFileChooserPanel = new DialogComponentReaderFileChooser(sourceLocationChooserModel, FILE_HISTORY_ID,
            readFvm, FilterMode.FILES_IN_FOLDERS, FilterMode.FOLDER, FilterMode.FILE);

        m_includeSelected = new JCheckBox("Include selected source folder");

        final FlowVariableModel writeFvm = createFlowVariableModel(destinationFileChooserModel.getKeysForFSLocation(),
            FSLocationVariableType.INSTANCE);
        m_destinationFileChooserPanel = new DialogComponentWriterFileChooser(destinationFileChooserModel,
            FILE_HISTORY_ID, writeFvm, CompressDestinationStatusMessageReporter::new, FilterMode.FILE);

        m_includeParentSwingWorkerManager = new SwingWorkerManager(
            () -> new IncludeParentFolderAvailableSwingWorker(sourceLocationChooserModel::createReadPathAccessor,
                m_config.getInputLocationChooserModel().getFilterModeModel().getFilterMode(),
                m_includeSelected::setEnabled));

        sourceLocationChooserModel.addChangeListener(l -> m_includeParentSwingWorkerManager.startSwingWorker());

        addTab("Settings", initLayout());
    }

    /**
     *
     * Cancels the {@link IncludeParentFolderAvailableSwingWorker} when the dialog will be closed.
     */
    @Override
    public void onClose() {
        m_includeParentSwingWorkerManager.cancelSwingWorker();
    }

    private JPanel initLayout() {
        final JPanel panel = new JPanel(new GridBagLayout());
        GBCBuilder gbc = new GBCBuilder().resetX().resetY();

        panel.add(createInputPanel(), gbc.fillHorizontal().setWeightX(1).build());
        gbc.incY();
        panel.add(createOutputPanel(), gbc.resetX().fillHorizontal().setWeightX(1).build());
        gbc.incY();
        panel.add(createOptionsPanel(), gbc.build());
        gbc.incY().setWeightY(1);
        panel.add(new JPanel(), gbc.build());
        return panel;
    }

    private JPanel createInputPanel() {
        final JPanel inputPanel = new JPanel(new GridBagLayout());
        inputPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Source"));
        final GBCBuilder gbc = new GBCBuilder().resetX().resetY();
        inputPanel.add(m_inputFileChooserPanel.getComponentPanel(),
            gbc.resetX().incY().fillHorizontal().setWeightX(1).build());
        return inputPanel;
    }

    private JPanel createOutputPanel() {
        final JPanel outputPanel = new JPanel(new GridBagLayout());
        outputPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Destination"));

        final GBCBuilder gbc = new GBCBuilder().resetX().resetY();

        outputPanel.add(m_destinationFileChooserPanel.getComponentPanel(),
            gbc.resetX().incY().fillHorizontal().setWeightX(1).build());

        return outputPanel;
    }

    private JPanel createOptionsPanel() {
        final JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Options"));
        final GBCBuilder gbc = new GBCBuilder().resetX().resetY().anchorPageStart().fillHorizontal();
        panel.add(m_includeSelected, gbc.build());
        panel.add(new JPanel(), gbc.fillHorizontal().setWeightX(1).incX().build());
        return panel;
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        m_inputFileChooserPanel.saveSettingsTo(settings);
        m_destinationFileChooserPanel.saveSettingsTo(settings);

        m_config.includeSelectedFolder(m_includeSelected.isSelected());
        m_config.saveSettingsForDialog(settings);
    }

    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final PortObjectSpec[] specs)
        throws NotConfigurableException {
        m_inputFileChooserPanel.loadSettingsFrom(settings, specs);
        m_destinationFileChooserPanel.loadSettingsFrom(settings, specs);

        m_config.loadSettingsForDialog(settings);
        m_includeSelected.setSelected(m_config.includeParentFolder());
    }
}
