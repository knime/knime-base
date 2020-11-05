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
package org.knime.filehandling.utility.nodes.transfer;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.container.SingleCellFactory;
import org.knime.core.data.def.BooleanCell;
import org.knime.core.data.def.BooleanCell.BooleanCellFactory;
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
import org.knime.filehandling.core.defaultnodesettings.filechooser.reader.ReadPathAccessor;
import org.knime.filehandling.core.defaultnodesettings.filechooser.writer.WritePathAccessor;
import org.knime.filehandling.core.defaultnodesettings.filtermode.SettingsModelFilterMode.FilterMode;
import org.knime.filehandling.core.defaultnodesettings.status.NodeModelStatusConsumer;
import org.knime.filehandling.core.defaultnodesettings.status.StatusMessage.MessageType;
import org.knime.filehandling.utility.nodes.utils.PathHandlingUtils;
import org.knime.filehandling.utility.nodes.utils.PathRelativizer;
import org.knime.filehandling.utility.nodes.utils.PathRelativizerNonTableInput;

/**
 * Node model of the Transfer Files node.
 *
 * @author Lars Schweikardt, KNIME GmbH, Konstanz, Germany
 */
final class TransferFilesNodeModel extends NodeModel {

    private final TransferFilesNodeConfig m_config;

    private final NodeModelStatusConsumer m_statusConsumer =
        new NodeModelStatusConsumer(EnumSet.of(MessageType.ERROR, MessageType.WARNING));

    /**
     * Constructor.
     *
     * @param portsConfig the {@link PortsConfiguration}
     * @param config the {@link TransferFilesNodeConfig}
     */
    TransferFilesNodeModel(final PortsConfiguration portsConfig, final TransferFilesNodeConfig config) {
        super(portsConfig.getInputPorts(), portsConfig.getOutputPorts());
        m_config = config;
    }

    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        m_config.getSourceFileChooserModel().configureInModel(inSpecs, m_statusConsumer);
        m_config.getDestinationFileChooserModel().configureInModel(inSpecs, m_statusConsumer);
        m_statusConsumer.setWarningsIfRequired(this::setWarningMessage);

        final ColumnRearranger r = new ColumnRearranger(PathCopier.createOutputSpec(m_config));
        if (m_config.getDeleteSourceFilesModel().getBooleanValue()) {
            r.append(new DeletedColumnCellFactory());
        }

        return new PortObjectSpec[]{r.createSpec()};
    }

    @Override
    protected PortObject[] execute(final PortObject[] inObjects, final ExecutionContext exec) throws Exception {
        try (final ReadPathAccessor readPathAccessor = m_config.getSourceFileChooserModel().createReadPathAccessor();
                final WritePathAccessor writePathAccessor =
                    m_config.getDestinationFileChooserModel().createWritePathAccessor()) {

            // Create container for output table
            final DataTableSpec outputSpec = PathCopier.createOutputSpec(m_config);
            final BufferedDataContainer container = exec.createDataContainer(outputSpec);

            final BufferedDataTable outputTable = copy(container, readPathAccessor, writePathAccessor, exec);

            return new PortObject[]{outputTable};
        }
    }

    private BufferedDataTable copy(final BufferedDataContainer container, final ReadPathAccessor readPathAccessor,
        final WritePathAccessor writePathAccessor, final ExecutionContext exec)
        throws IOException, InvalidSettingsException, CanceledExecutionException {
        //Get paths
        final FSPath rootPath = readPathAccessor.getRootPath(m_statusConsumer);
        final FilterMode filterMode = m_config.getSourceFileChooserModel().getFilterModeModel().getFilterMode();
        final boolean includeSourceFolder =  m_config.getSettingsModelIncludeSourceFolder().getBooleanValue();

        PathHandlingUtils.checkSettingsIncludeSourceFolder(filterMode, includeSourceFolder, rootPath);

        final FSPath destinationDir = writePathAccessor.getOutputPath(m_statusConsumer);
        final List<FSPath> sourcePaths = getSourcePaths(readPathAccessor, filterMode);
        m_statusConsumer.setWarningsIfRequired(this::setWarningMessage);
        //Creates output directories if necessary
        if (m_config.getDestinationFileChooserModel().isCreateMissingFolders()) {
            PathCopier.createDirectories(destinationDir);
        } else {
            CheckUtils.checkSetting(FSFiles.exists(destinationDir),
                String.format("The specified destination folder %s does not exist.", destinationDir));
        }

        final PathRelativizer pathRelativizer = new PathRelativizerNonTableInput(rootPath,
            m_config.getSettingsModelIncludeSourceFolder().getBooleanValue(), filterMode, false);

        final FileStoreFactory fileStoreFactory = FileStoreFactory.createFileStoreFactory(exec);
        final PathCopier pathCopier = new PathCopier(container::addRowToTable, m_config, fileStoreFactory);

        long rowIdx = 0;
        final long noPaths = sourcePaths.size();
        for (final Path sourcePath : sourcePaths) {
            final Path destinationPath = destinationDir.resolve(pathRelativizer.apply(sourcePath));

            pathCopier.copyPath(sourcePath, destinationPath, rowIdx);

            final long copiedPaths = rowIdx + 1;
            exec.setProgress(copiedPaths / (double)noPaths, () -> ("Copied files/folder :" + copiedPaths));
            exec.checkCanceled();
            rowIdx++;
        }

        BufferedDataTable table;
        if (m_config.getDeleteSourceFilesModel().getBooleanValue()) {
            final ColumnRearranger r = new ColumnRearranger(container.getTableSpec());
            DeletedColumnCellFactory mr = new DeletedColumnCellFactory();
            r.append(mr);
            //delete the files and folders from the source
            mr.deleteFilesFolders(sourcePaths);
            container.close();

            table = exec.createColumnRearrangeTable(container.getTable(), r, exec);
        } else {
            container.close();
            fileStoreFactory.close();
            table = container.getTable();
        }
        return table;

    }

    /**
     * Cell factory to add boolean column for deleted files / folders.
     */
    private class DeletedColumnCellFactory extends SingleCellFactory {

        private int m_i = 0;

        private DataCell[] m_dataCells;

        /**
         * Constructor.
         */
        public DeletedColumnCellFactory() {
            super(false, new DataColumnSpecCreator("Source deleted", BooleanCell.TYPE).createSpec());
        }

        /**
         * Deletes files and folders.
         *
         * @param paths
         * @throws IOException
         */
        private void deleteFilesFolders(final List<FSPath> paths) throws IOException {
            m_dataCells = new DataCell[paths.size()];
            //Delete in reverse order
            Collections.reverse(paths);
            for (final FSPath p : paths) {
                try {
                    m_dataCells[m_i] = BooleanCellFactory.create(Files.deleteIfExists(p));
                    m_i++;
                } catch (IOException e) {
                    if (!m_config.getFailOnDeletionModel().getBooleanValue()) {
                        m_dataCells[m_i] = BooleanCellFactory.create(false);
                        m_i++;
                    } else {
                        throw e;
                    }
                }
            }
        }

        @Override
        public DataCell getCell(final DataRow row) {
            --m_i;
            return m_dataCells[m_i];
        }
    }

    private List<FSPath> getSourcePaths(final ReadPathAccessor readPathAccessor, final FilterMode filterMode)
        throws IOException, InvalidSettingsException {
        List<FSPath> sourcePaths = readPathAccessor.getFSPaths(m_statusConsumer);
        CheckUtils.checkSetting(!sourcePaths.isEmpty(),
            "No files available please select a folder which contains files");
        if (filterMode == FilterMode.FOLDER) {
            final List<FSPath> pathsFromFolder =
                FSFiles.getFilesAndFolders(readPathAccessor.getRootPath(m_statusConsumer),
                    m_config.getSettingsModelIncludeSourceFolder().getBooleanValue());
            sourcePaths = pathsFromFolder;
        }
        return sourcePaths;
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
