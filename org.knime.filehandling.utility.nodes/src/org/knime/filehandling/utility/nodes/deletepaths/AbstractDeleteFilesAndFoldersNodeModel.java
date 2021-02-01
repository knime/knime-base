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
 *   14.01.2021 (lars.schweikardt): created
 */
package org.knime.filehandling.utility.nodes.deletepaths;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowKey;
import org.knime.core.data.def.BooleanCell.BooleanCellFactory;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.context.ports.PortsConfiguration;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.streamable.BufferedDataTableRowOutput;
import org.knime.core.node.streamable.RowOutput;
import org.knime.filehandling.core.connections.FSFiles;
import org.knime.filehandling.core.connections.FSPath;
import org.knime.filehandling.core.data.location.FSLocationValueMetaData;
import org.knime.filehandling.core.data.location.cell.MultiSimpleFSLocationCellFactory;
import org.knime.filehandling.core.data.location.cell.SimpleFSLocationCell;
import org.knime.filehandling.core.data.location.cell.SimpleFSLocationCellFactory;
import org.knime.filehandling.core.defaultnodesettings.status.NodeModelStatusConsumer;
import org.knime.filehandling.core.defaultnodesettings.status.StatusMessage.MessageType;

/**
 * Abstract super class for the {@link NodeModel} of the "Delete Files/Folders" nodes.
 *
 * @author Lars Schweikardt, KNIME GmbH, Konstanz, Germany
 * @param <C> an {@link AbstractDeleteFilesAndFoldersNodeConfig} instance
 */

public abstract class AbstractDeleteFilesAndFoldersNodeModel<C extends AbstractDeleteFilesAndFoldersNodeConfig>
    extends NodeModel {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(AbstractDeleteFilesAndFoldersNodeModel.class);

    private final C m_config;

    private final NodeModelStatusConsumer m_statusConsumer;

    /**
     * Constructor.
     *
     * @param portsConfig the {@link PortsConfiguration}
     * @param config the node specific implementation of the {@link AbstractDeleteFilesAndFoldersNodeConfig}
     */
    protected AbstractDeleteFilesAndFoldersNodeModel(final PortsConfiguration portsConfig, final C config) {
        super(portsConfig.getInputPorts(), portsConfig.getOutputPorts());
        m_config = config;
        m_statusConsumer = new NodeModelStatusConsumer(EnumSet.of(MessageType.ERROR, MessageType.WARNING));
        m_statusConsumer.setWarningsIfRequired(this::setWarningMessage);
    }

    /**
     * Allows for additional configuration required by the concrete node model instance.
     *
     * @param inSpecs the input specs
     * @throws InvalidSettingsException - If something goes wrong during configuration
     */
    protected abstract void doConfigure(PortObjectSpec[] inSpecs) throws InvalidSettingsException;

    /**
     * Returns the node specific implementation of the {@link AbstractDeleteFilesAndFoldersNodeConfig}.
     *
     * @return the node config
     */
    protected final C getConfig() {
        return m_config;
    }

    /**
     * Returns the status message consumer.
     *
     * @return the status message consumer
     */
    protected final NodeModelStatusConsumer getStatusConsumer() {
        return m_statusConsumer;
    }

    /**
     * Returns the {@link FSLocationValueMetaData} to create the {@link DataTableSpec}.
     *
     * @param inSpecs the {@link PortObjectSpec}
     * @return the {@link FSLocationValueMetaData}
     */
    protected abstract FSLocationValueMetaData getFSLocationValueMetaData(final PortObjectSpec[] inSpecs);

    /**
     * Returns the {@link DeleteFilesFolderIterator} with the {@link FSPath}s to delete.
     *
     * @param inData the {@link PortObjectSpec}
     * @return the {@link DeleteFilesFolderIterator}
     * @throws IOException
     * @throws InvalidSettingsException
     */
    protected abstract DeleteFilesFolderIterator getDeleteFilesFolderIterator(final PortObject[] inData)
        throws IOException, InvalidSettingsException;

    @Override
    protected final PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        doConfigure(inSpecs);
        getStatusConsumer().setWarningsIfRequired(this::setWarningMessage);
        return new PortObjectSpec[]{createOutputSpec(inSpecs)};
    }

    @Override
    protected final PortObject[] execute(final PortObject[] inData, final ExecutionContext exec) throws Exception {
        final PortObjectSpec[] inSpecs = Arrays.stream(inData)//
            .map(PortObject::getSpec)//
            .toArray(PortObjectSpec[]::new);

        final BufferedDataTableRowOutput rowOutput =
            new BufferedDataTableRowOutput(exec.createDataContainer(createOutputSpec(inSpecs)));

        deletePaths(inData, exec, rowOutput);

        return new PortObject[]{rowOutput.getDataTable()};
    }

    /**
     * Deletes the {@link FSPath} provided by the {@link DeleteFilesFolderIterator}.
     *
     * @param inData the {@link PortObject}
     * @param exec the {@link ExecutionContext}
     * @param rowOutput the {@link RowOutput}
     * @throws IOException
     * @throws InvalidSettingsException
     * @throws CanceledExecutionException
     * @throws InterruptedException
     */
    private final void deletePaths(final PortObject[] inData, final ExecutionContext exec, final RowOutput rowOutput)
        throws IOException, InvalidSettingsException, CanceledExecutionException, InterruptedException {

        try (final DeleteFilesFolderIterator deleteFileIterator = getDeleteFilesFolderIterator(inData)) {
            m_statusConsumer.setWarningsIfRequired(this::setWarningMessage);
            final long numberOfPaths = deleteFileIterator.size();
            final MultiSimpleFSLocationCellFactory locationFactory = new MultiSimpleFSLocationCellFactory();
            long rec = 0;

            while (deleteFileIterator.hasNext()) {
                final FSPath path = deleteFileIterator.next();
                final boolean fileExists = FSFiles.exists(path);
                final boolean isDeleted = deletePath(path);
                final SimpleFSLocationCell locationCell = locationFactory.createCell(path.toFSLocation());

                createRow(rowOutput, locationCell, isDeleted, fileExists, rec);
                ++rec;
                exec.checkCanceled();
                updateProgress(exec, rec, numberOfPaths);
            }
        } finally {
            rowOutput.close();
        }
    }

    /**
     * Deletes a file or folder specified by the passed {@link FSPath}.
     *
     * @param path the {@link FSPath} to a file or folder
     * @return true or false whether or not the deletion process was successful
     * @throws IOException
     */
    private boolean deletePath(final FSPath path) throws IOException {
        boolean successfullyDeleted = true;
        try {
            if (FSFiles.isDirectory(path)) {
                FSFiles.deleteRecursively(path);
            } else {
                Files.delete(path);
            }
        } catch (NoSuchFileException e) {
            if (m_config.isAbortIfFileNotExist()) {
                LOGGER.debug(e);
                throw new NoSuchFileException(e.getFile(), null,
                    "The file/directory couldn't be deleted since it was not found. "
                        + "Execution was aborted due to user settings");
            }
            successfullyDeleted = false;
        } catch (IOException e) {
            if (m_config.isAbortedIfFails().getBooleanValue()) {
                throw new IOException(String
                    .format("The file/directory '%s' couldn't be deleted and the execution was aborted due to user "
                        + "settings.", path),
                    e);
            }
            successfullyDeleted = false;
        }
        return successfullyDeleted;
    }

    /**
     * Creates the {@link DataTableSpec} for the output table.
     *
     * @param specs the {@link PortObjectSpec}s
     * @return the {@link DataTableSpec}
     */
    private final DataTableSpec createOutputSpec(final PortObjectSpec[] specs) {
        final List<DataColumnSpec> columnSpecs = new ArrayList<>(3);
        final FSLocationValueMetaData metaData = getFSLocationValueMetaData(specs);
        final DataColumnSpecCreator colCreator = new DataColumnSpecCreator("Path", SimpleFSLocationCellFactory.TYPE);
        colCreator.addMetaData(metaData, true);

        columnSpecs.add(colCreator.createSpec());

        if (!getConfig().isAbortedIfFails().getBooleanValue()) {
            columnSpecs.add(new DataColumnSpecCreator("Deleted successfully", BooleanCellFactory.TYPE).createSpec());
        }
        if (!getConfig().isAbortIfFileNotExist()) {
            columnSpecs.add(new DataColumnSpecCreator("File exists", BooleanCellFactory.TYPE).createSpec());
        }

        return new DataTableSpec(columnSpecs.stream().toArray(DataColumnSpec[]::new));
    }

    /**
     * Creates a {@link DataRow} for the output table.
     *
     * @param rowOutput the {@link RowOutput}
     * @param locationCell the {@link SimpleFSLocationCell}
     * @param deleted flag whether a file / folder has been deleted or not
     * @param rec number for the {@link RowKey}
     * @throws InterruptedException
     */
    private final void createRow(final RowOutput rowOutput, final SimpleFSLocationCell locationCell,
        final boolean deleted, final boolean fileExists, final long rec) throws InterruptedException {
        final List<DataCell> row = new ArrayList<>(3);

        row.add(locationCell);

        if (!m_config.isAbortedIfFails().getBooleanValue()) {
            row.add(BooleanCellFactory.create(deleted));
        }
        if (!m_config.isAbortIfFileNotExist()) {
            row.add(BooleanCellFactory.create(fileExists));
        }
        rowOutput.push(new DefaultRow(RowKey.createRowKey(rec), row));
    }

    /**
     * Updates the progress of the node.
     *
     * @param exec the {@link ExecutionContext}
     * @param rec the number of the current path
     * @param numEntries the total number of paths to be processed
     * @throws CanceledExecutionException
     */
    private static final void updateProgress(final ExecutionContext exec, final long rec, final long numEntries)
        throws CanceledExecutionException {
        exec.checkCanceled();
        exec.setProgress(rec / (double)numEntries,
            () -> String.format("Processing entry %d out of %d", rec, numEntries));
    }

    @Override
    protected final void saveSettingsTo(final NodeSettingsWO settings) {
        m_config.saveSettingsForModel(settings);
    }

    @Override
    protected final void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_config.validateSettingsForModel(settings);
    }

    @Override
    protected final void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_config.loadSettingsForModel(settings);
    }

    @Override
    protected final void loadInternals(final File nodeInternDir, final ExecutionMonitor exec) {
        // not used
    }

    @Override
    protected final void saveInternals(final File nodeInternDir, final ExecutionMonitor exec) {
        // not used
    }

    @Override
    protected final void reset() {
        // not used
    }
}
