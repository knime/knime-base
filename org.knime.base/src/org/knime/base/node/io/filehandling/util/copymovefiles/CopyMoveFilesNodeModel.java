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
 *   Mar 5, 2020 (Simon Schmid, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.base.node.io.filehandling.util.copymovefiles;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.List;

import org.knime.base.node.io.filehandling.util.PathRelativizer;
import org.knime.base.node.io.filehandling.util.PathRelativizerNonTableInput;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.filestore.FileStoreFactory;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.context.ports.PortsConfiguration;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.util.CheckUtils;
import org.knime.filehandling.core.connections.FSFiles;
import org.knime.filehandling.core.connections.FSPath;
import org.knime.filehandling.core.data.location.variable.FSLocationVariableType;
import org.knime.filehandling.core.defaultnodesettings.filechooser.reader.ReadPathAccessor;
import org.knime.filehandling.core.defaultnodesettings.filechooser.writer.WritePathAccessor;
import org.knime.filehandling.core.defaultnodesettings.filtermode.SettingsModelFilterMode.FilterMode;
import org.knime.filehandling.core.defaultnodesettings.status.NodeModelStatusConsumer;
import org.knime.filehandling.core.defaultnodesettings.status.StatusMessage.MessageType;

/**
 * Node model of the Copy/Move Files node.
 *
 * @author Lars Schweikardt, KNIME GmbH, Konstanz, Germany
 */
final class CopyMoveFilesNodeModel extends NodeModel {

    private final CopyMoveFilesNodeConfig m_config;

    private final NodeModelStatusConsumer m_statusConsumer =
        new NodeModelStatusConsumer(EnumSet.of(MessageType.ERROR, MessageType.WARNING));

    /**
     * Constructor.
     *
     * @param portsConfig the {@link PortsConfiguration}
     * @param config the {@link CopyMoveFilesNodeConfig}
     */
    CopyMoveFilesNodeModel(final PortsConfiguration portsConfig, final CopyMoveFilesNodeConfig config) {
        super(portsConfig.getInputPorts(), portsConfig.getOutputPorts());
        m_config = config;
    }

    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        m_config.getSourceFileChooserModel().configureInModel(inSpecs, m_statusConsumer);
        m_config.getDestinationFileChooserModel().configureInModel(inSpecs, m_statusConsumer);
        m_statusConsumer.setWarningsIfRequired(this::setWarningMessage);

        return new PortObjectSpec[]{FileCopier.createOutputSpec(m_config)};
    }

    @Override
    protected PortObject[] execute(final PortObject[] inObjects, final ExecutionContext exec) throws Exception {
        try (final ReadPathAccessor readPathAccessor = m_config.getSourceFileChooserModel().createReadPathAccessor();
                final WritePathAccessor writePathAccessor =
                    m_config.getDestinationFileChooserModel().createWritePathAccessor()) {

            // Create container for output table
            final DataTableSpec outputSpec = FileCopier.createOutputSpec(m_config);
            final BufferedDataContainer container = exec.createDataContainer(outputSpec);

            final BufferedDataTable outputTable = copyFiles(container, readPathAccessor, writePathAccessor, exec);

            return new PortObject[]{outputTable};
        }
    }

    private BufferedDataTable copyFiles(final BufferedDataContainer container, final ReadPathAccessor readPathAccessor,
        final WritePathAccessor writePathAccessor, final ExecutionContext exec)
        throws IOException, CanceledExecutionException, InvalidSettingsException {
        //Get paths
        final FSPath rootPath = readPathAccessor.getRootPath(m_statusConsumer);
        final FSPath destinationDir = writePathAccessor.getOutputPath(m_statusConsumer);
        final FilterMode filterMode = m_config.getSourceFileChooserModel().getFilterModeModel().getFilterMode();
        final List<FSPath> sourcePaths = getSourcePaths(readPathAccessor, filterMode);
        m_statusConsumer.setWarningsIfRequired(this::setWarningMessage);
        //Creates output directories if necessary
        if (m_config.getDestinationFileChooserModel().isCreateMissingFolders()) {
            FileCopier.createOutputDirectories(destinationDir);
        } else {
            CheckUtils.checkSetting(FSFiles.exists(destinationDir),
                String.format("The specified destination folder %s does not exist.", destinationDir));
        }

        final PathRelativizer pathRelativizer = new PathRelativizerNonTableInput(rootPath,
            m_config.getSettingsModelIncludeSourceFolder().getBooleanValue(), filterMode, false);

        createFlowVariables(rootPath, destinationDir);

        final FileStoreFactory fileStoreFactory = FileStoreFactory.createFileStoreFactory(exec);
        final FileCopier fileCopier = new FileCopier(container::addRowToTable, m_config, fileStoreFactory);

        long rowIdx = 0;
        final long noOfFiles = sourcePaths.size();
        for (Path sourceFilePath : sourcePaths) {
            final Path destinationFilePath = destinationDir.resolve(pathRelativizer.apply(sourceFilePath));
            fileCopier.copy(sourceFilePath, destinationFilePath, rowIdx);
            final long copiedFiles = rowIdx + 1;
            exec.setProgress(copiedFiles / (double)noOfFiles, () -> ("Copied files :" + copiedFiles));
            exec.checkCanceled();
            rowIdx++;
        }

        container.close();
        fileStoreFactory.close();

        return container.getTable();
    }

    private List<FSPath> getSourcePaths(final ReadPathAccessor readPathAccessor, final FilterMode filterMode)
        throws IOException, InvalidSettingsException {
        List<FSPath> sourcePaths = readPathAccessor.getFSPaths(m_statusConsumer);
        CheckUtils.checkSetting(!sourcePaths.isEmpty(),
            "No files available please select a folder which contains files");
        if (filterMode == FilterMode.FOLDER) {
            final List<FSPath> pathsFromFolder = FSFiles.getFilePathsFromFolder(sourcePaths.get(0));
            sourcePaths = pathsFromFolder;
        }
        return sourcePaths;
    }

    /**
     * Creates the {@link FSLocationVariableType#INSTANCE} flow variables for the source and target path.
     *
     * @param source the {@link FSPath} of the source file chooser
     * @param target the {@link FSPath} of the target file chooser
     */
    private void createFlowVariables(final FSPath source, final FSPath target) {
        pushFlowVariable("source_path", FSLocationVariableType.INSTANCE, source.toFSLocation());
        pushFlowVariable("destination_path", FSLocationVariableType.INSTANCE, target.toFSLocation());
    }

    @Override
    protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        // nothing to do
    }

    @Override
    protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        // nothing to do
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_config.saveConfigurationForModel(settings);
    }

    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_config.validateConfigurationForModel(settings);
    }

    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_config.loadConfigurationForModel(settings);
    }

    @Override
    protected void reset() {
        // nothing to do
    }

}
