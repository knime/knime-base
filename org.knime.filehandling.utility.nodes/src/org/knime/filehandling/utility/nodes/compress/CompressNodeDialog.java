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
package org.knime.filehandling.utility.nodes.compress;

import java.awt.GridBagLayout;
import java.util.Arrays;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JPanel;

import org.knime.core.node.FlowVariableModel;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.context.ports.PortsConfiguration;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.filehandling.core.connections.FSLocation;
import org.knime.filehandling.core.data.location.variable.FSLocationVariableType;
import org.knime.filehandling.core.defaultnodesettings.filechooser.reader.DialogComponentReaderFileChooser;
import org.knime.filehandling.core.defaultnodesettings.filechooser.reader.SettingsModelReaderFileChooser;
import org.knime.filehandling.core.defaultnodesettings.filechooser.writer.DialogComponentWriterFileChooser;
import org.knime.filehandling.core.defaultnodesettings.filechooser.writer.SettingsModelWriterFileChooser;
import org.knime.filehandling.core.defaultnodesettings.filtermode.SettingsModelFilterMode.FilterMode;
import org.knime.filehandling.core.defaultnodesettings.status.StatusSwingWorker;
import org.knime.filehandling.core.defaultnodesettings.status.StatusView;
import org.knime.filehandling.core.util.GBCBuilder;
import org.knime.filehandling.utility.nodes.dialog.swingworker.IncludeSourceFolderSwingWorker;
import org.knime.filehandling.utility.nodes.dialog.swingworker.SwingWorkerManager;

/**
 * Node Dialog for the "Compress Files/Folder" node
 *
 * @author Timmo Waller-Ehrat, KNIME GmbH, Konstanz, Germany
 */
final class CompressNodeDialog extends NodeDialogPane {

    private static final Pattern PATTERN = Pattern.compile(//
        Arrays.stream(CompressNodeConfig.COMPRESSIONS)//
            .map(s -> "\\." + s)//
            .collect(Collectors.joining("|", "(", ")\\s*$")),
        Pattern.CASE_INSENSITIVE);

    private static final String FILE_HISTORY_ID = "compress_files_history";

    private final DialogComponentReaderFileChooser m_inputFileChooserPanel;

    private final DialogComponentWriterFileChooser m_destinationFileChooserPanel;

    private final DialogComponentStringSelection m_compressionSelection;

    private final JCheckBox m_includeSourceFolder;

    private final JCheckBox m_flattenHierarchyPanel;

    private final CompressNodeConfig m_config;

    private final SwingWorkerManager m_includeSourceFolderSwingWorkerManager;

    private final StatusView m_flattenHierarchyStatusView;

    private final StatusView m_includeSourceFolderStatusView;

    private final SwingWorkerManager m_flattenHierarchySwingWorkerManager;

    private boolean m_isLoading;

    CompressNodeDialog(final PortsConfiguration portsConfig) {
        m_config = new CompressNodeConfig(portsConfig);

        final SettingsModelReaderFileChooser sourceLocationChooserModel = m_config.getInputLocationChooserModel();
        final SettingsModelWriterFileChooser destinationFileChooserModel = m_config.getTargetFileChooserModel();

        final FlowVariableModel readFvm =
            createFlowVariableModel(sourceLocationChooserModel.getKeysForFSLocation(), FSLocationVariableType.INSTANCE);
        m_inputFileChooserPanel = new DialogComponentReaderFileChooser(sourceLocationChooserModel, FILE_HISTORY_ID,
            readFvm, FilterMode.FILES_IN_FOLDERS, FilterMode.FOLDER, FilterMode.FILE);

        m_includeSourceFolder = new JCheckBox("Include selected source folder");
        m_includeSourceFolderStatusView = new StatusView();

        m_flattenHierarchyPanel = new JCheckBox("Flatten hierarchy");
        m_flattenHierarchyStatusView = new StatusView();

        m_config.getInputLocationChooserModel().addChangeListener(c -> m_flattenHierarchyPanel
            .setEnabled((m_config.getInputLocationChooserModel().getFilterMode()) != FilterMode.FILE));

        m_config.getInputLocationChooserModel().addChangeListener(c -> m_includeSourceFolder
            .setEnabled((m_config.getInputLocationChooserModel().getFilterMode()) != FilterMode.FILE));

        final FlowVariableModel writeFvm = createFlowVariableModel(destinationFileChooserModel.getKeysForFSLocation(),
            FSLocationVariableType.INSTANCE);
        m_destinationFileChooserPanel = new DialogComponentWriterFileChooser(destinationFileChooserModel,
            FILE_HISTORY_ID, writeFvm, FilterMode.FILE);

        SettingsModelString compressionModel = m_config.getCompressionModel();
        m_compressionSelection = new DialogComponentStringSelection(compressionModel, "Compression",
            Arrays.asList(CompressNodeConfig.COMPRESSIONS));
        compressionModel.addChangeListener(l -> updateLocation());

        m_includeSourceFolderSwingWorkerManager = new SwingWorkerManager(this::createIncludeParentFolderSwingWorker);

        m_flattenHierarchySwingWorkerManager = new SwingWorkerManager(this::createFlattenSwingWorker);

        sourceLocationChooserModel.addChangeListener(l -> includeSourceFolder());
        sourceLocationChooserModel.addChangeListener(l -> flattenHierarchy());

        m_flattenHierarchyPanel.addActionListener(l -> flattenHierarchy());
        m_includeSourceFolder.addActionListener(l -> includeSourceFolder());

        addTab("Settings", initLayout());
    }

    private JPanel initLayout() {
        final JPanel panel = new JPanel(new GridBagLayout());
        final GBCBuilder gbc = new GBCBuilder().resetX().resetY().fillHorizontal().setWeightX(1).anchorLineStart();
        panel.add(createInputPanel(), gbc.build());

        gbc.incY();
        panel.add(createOutputPanel(), gbc.build());

        gbc.incY().fillHorizontal().setWeightX(1);
        panel.add(createOptionsPanel(), gbc.build());

        gbc.incY().setWeightY(1).setWeightX(1).fillBoth();
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
        final GBCBuilder gbc = new GBCBuilder().resetX().resetY().anchorLineStart().setWeightX(0).fillNone();
        panel.add(m_compressionSelection.getComponentPanel(), gbc.build());

        gbc.incY();
        panel.add(m_includeSourceFolder, gbc.build());

        gbc.incX().setWeightY(1).fillVertical().setHeight(2);
        panel.add(m_includeSourceFolderStatusView.getLabel(), gbc.build());

        gbc.setHeight(1).fillNone().setWeightY(0).resetX().incY();
        panel.add(new JPanel());

        gbc.incY();
        panel.add(m_flattenHierarchyPanel, gbc.build());

        gbc.incX().setWeightY(1).fillVertical().setHeight(2);
        panel.add(m_flattenHierarchyStatusView.getLabel(), gbc.build());

        panel.add(new JPanel(),
            gbc.insetTop(0).resetX().setHeight(1).incY().setWeightX(1).fillHorizontal().setWidth(2).build());
        return panel;
    }

    private StatusSwingWorker createIncludeParentFolderSwingWorker() {
        return new StatusSwingWorker(m_includeSourceFolderStatusView::setStatus,
            new IncludeSourceFolderSwingWorker(m_config.getInputLocationChooserModel().createClone()), false);
    }

    private StatusSwingWorker createFlattenSwingWorker() {
        return new StatusSwingWorker(m_flattenHierarchyStatusView::setStatus,
            new FlattenHierarchyStatusReporter(m_config.getInputLocationChooserModel().createClone()), false);
    }

    private void updateLocation() {
        final String compression = "." + ((SettingsModelString)m_compressionSelection.getModel()).getStringValue();
        SettingsModelWriterFileChooser writerModel = m_destinationFileChooserPanel.getSettingsModel();
        if (!m_isLoading && !writerModel.isOverwrittenByFlowVariable()) {
            FSLocation location = writerModel.getLocation();
            final String locPath = location.getPath();
            final String newPath = PATTERN.matcher(locPath).replaceAll(compression);
            if (!newPath.equals(locPath)) {
                writerModel.setLocation(
                    new FSLocation(location.getFSCategory(), location.getFileSystemSpecifier().orElse(null), newPath));
            }
        }
        writerModel.setFileExtensions(compression);
    }

    private void flattenHierarchy() {
        startCancelSwingWorker(m_flattenHierarchyStatusView, m_flattenHierarchyPanel,
            m_flattenHierarchySwingWorkerManager);
    }

    private void includeSourceFolder() {
        startCancelSwingWorker(m_includeSourceFolderStatusView, m_includeSourceFolder,
            m_includeSourceFolderSwingWorkerManager);
    }

    private void startCancelSwingWorker(final StatusView statusView, final JCheckBox checkBox,
        final SwingWorkerManager swingWorker) {
        statusView.clearStatus();
        if (m_isLoading) {
            return;
        }
        if (checkBox.isSelected() && m_config.getInputLocationChooserModel().getFilterMode() != FilterMode.FILE) {
            swingWorker.startSwingWorker();
        } else {
            swingWorker.cancelSwingWorker();
        }
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        m_inputFileChooserPanel.saveSettingsTo(settings);
        m_destinationFileChooserPanel.saveSettingsTo(settings);
        m_compressionSelection.saveSettingsTo(settings);
        m_config.includeSelectedFolder(m_includeSourceFolder.isSelected());
        m_config.flattenHierarchy(m_flattenHierarchyPanel.isSelected());
        m_config.saveSettingsForDialog(settings);
    }

    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final PortObjectSpec[] specs)
        throws NotConfigurableException {
        // we want to disable the flatten hierarchy invocations until we have loaded all settings
        m_isLoading = true;

        m_inputFileChooserPanel.loadSettingsFrom(settings, specs);
        m_destinationFileChooserPanel.loadSettingsFrom(settings, specs);
        m_compressionSelection.loadSettingsFrom(settings, specs);

        m_config.loadSettingsForDialog(settings);
        m_includeSourceFolder.setSelected(m_config.includeSourceFolder());
        m_flattenHierarchyPanel.setSelected(m_config.flattenHierarchy());

        // update the flatten hierarchy status
        m_isLoading = false;
        flattenHierarchy();
        includeSourceFolder();
    }

    /**
     * Cancels any running swing worker when the dialog is closing.
     */
    @Override
    public void onClose() {
        m_includeSourceFolderSwingWorkerManager.cancelSwingWorker();
        m_flattenHierarchySwingWorkerManager.cancelSwingWorker();
        m_destinationFileChooserPanel.onClose();
        m_inputFileChooserPanel.onClose();
    }
}
