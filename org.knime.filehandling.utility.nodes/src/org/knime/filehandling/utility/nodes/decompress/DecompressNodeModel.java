/*
 * ------------------------------------------------------------------------
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
 * ------------------------------------------------------------------------
 *
 * History
 *   24 Aug 2020 (Timmo Waller-Ehrat, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.utility.nodes.decompress;

import java.io.BufferedInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowKey;
import org.knime.core.data.def.BooleanCell.BooleanCellFactory;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.StringCell.StringCellFactory;
import org.knime.core.data.filestore.FileStoreFactory;
import org.knime.core.data.util.CancellableReportingInputStream;
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
import org.knime.filehandling.core.connections.FSLocation;
import org.knime.filehandling.core.connections.FSPath;
import org.knime.filehandling.core.data.location.FSLocationValueMetaData;
import org.knime.filehandling.core.data.location.cell.FSLocationCellFactory;
import org.knime.filehandling.core.defaultnodesettings.filechooser.reader.ReadPathAccessor;
import org.knime.filehandling.core.defaultnodesettings.filechooser.writer.FileOverwritePolicy;
import org.knime.filehandling.core.defaultnodesettings.filechooser.writer.WritePathAccessor;
import org.knime.filehandling.core.defaultnodesettings.status.NodeModelStatusConsumer;
import org.knime.filehandling.core.defaultnodesettings.status.StatusMessage.MessageType;
import org.knime.filehandling.utility.nodes.utils.FileStatus;

/**
 * Node Model for the "Decompress Files" node
 *
 * @author Timmo Waller-Ehrat, KNIME GmbH, Konstanz, Germany
 */
final class DecompressNodeModel extends NodeModel {

    private static final int LOCATION_CELL_IDX = 0;

    private static final int DIRECTORY_CELL_IDX = 1;

    private static final int STATUS_CELL_IDX = 2;

    private static final NodeLogger LOGGER = NodeLogger.getLogger(DecompressNodeModel.class);

    private final DecompressNodeConfig m_config;

    private final NodeModelStatusConsumer m_statusConsumer;

    /**
     * Constructor
     *
     * @param portsConfig {@link PortsConfiguration} of the node
     */
    DecompressNodeModel(final PortsConfiguration portsConfig) {
        super(portsConfig.getInputPorts(), portsConfig.getOutputPorts());
        m_config = new DecompressNodeConfig(portsConfig);
        m_statusConsumer = new NodeModelStatusConsumer(EnumSet.of(MessageType.ERROR, MessageType.WARNING));
    }

    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        m_config.getInputFileChooserModel().configureInModel(inSpecs, m_statusConsumer);
        m_config.getOutputDirChooserModel().configureInModel(inSpecs, m_statusConsumer);
        m_statusConsumer.setWarningsIfRequired(this::setWarningMessage);
        return new PortObjectSpec[]{createOutputSpec()};
    }

    private DataTableSpec createOutputSpec() {
        final DataColumnSpecCreator colCreator = new DataColumnSpecCreator("Path", FSLocationCellFactory.TYPE);
        final FSLocation location = m_config.getOutputDirChooserModel().getLocation();
        final FSLocationValueMetaData metaData = new FSLocationValueMetaData(location.getFileSystemCategory(),
            location.getFileSystemSpecifier().orElse(null));
        colCreator.addMetaData(metaData, true);

        return new DataTableSpec(colCreator.createSpec(),
            new DataColumnSpecCreator("Directory", BooleanCellFactory.TYPE).createSpec(),
            new DataColumnSpecCreator("Status", StringCellFactory.TYPE).createSpec());
    }

    @Override
    protected PortObject[] execute(final PortObject[] inData, final ExecutionContext exec) throws Exception {
        final BufferedDataTableRowOutput rowOutput =
            new BufferedDataTableRowOutput(exec.createDataContainer(createOutputSpec()));
        decompress(rowOutput, exec);
        return new PortObject[]{rowOutput.getDataTable()};
    }

    private void decompress(final RowOutput rowOutput, final ExecutionContext exec)
        throws IOException, InvalidSettingsException, InterruptedException {

        try (final ReadPathAccessor readAccessor = m_config.getInputFileChooserModel().createReadPathAccessor()) {
            final List<FSPath> fsPaths = readAccessor.getFSPaths(m_statusConsumer);
            final FSPath sourcePath = fsPaths.get(0);
            m_statusConsumer.setWarningsIfRequired(this::setWarningMessage);

            decompress(sourcePath, rowOutput, exec);
        } finally {
            rowOutput.close();
        }
    }

    private void decompress(final FSPath sourcePath, final RowOutput rowOutput, final ExecutionContext exec)
        throws InterruptedException, IOException, InvalidSettingsException {

        try (final InputStream sourceStream = Files.newInputStream(sourcePath)) {
            final long fileSize = Files.readAttributes(sourcePath, BasicFileAttributes.class).size();

            try (final InputStream reportingInputStream =
                new CancellableReportingInputStream(sourceStream, exec, fileSize)) {

                try (final InputStream uncompressedStream = openUncompressStream(reportingInputStream)) {
                    decompress(uncompressedStream, rowOutput, exec);
                }
            }
        }
    }

    private void decompress(final InputStream uncompressedStream, final RowOutput rowOutput,
        final ExecutionContext exec) throws IOException, InterruptedException, InvalidSettingsException {

        final FileStoreFactory fsFac = FileStoreFactory.createFileStoreFactory(exec);

        try (final WritePathAccessor writeAccessor = m_config.getOutputDirChooserModel().createWritePathAccessor()) {
            final FSPath outputPath = writeAccessor.getOutputPath(m_statusConsumer);
            m_statusConsumer.setWarningsIfRequired(this::setWarningMessage);
            createParentDirIfRequired(outputPath);
            final FSLocationCellFactory locationCellFactory =
                new FSLocationCellFactory(fsFac, m_config.getOutputDirChooserModel().getLocation());

            final FileOverwritePolicy overwritePolicy = m_config.getOutputDirChooserModel().getFileOverwritePolicy();

            // archiver autodetection needs stream which supports marks
            try (final BufferedInputStream bufferedStream = new BufferedInputStream(uncompressedStream)) {
                try (ArchiveInputStream archiveInputStream =
                    new ArchiveStreamFactory().createArchiveInputStream(bufferedStream)) {

                    decompress(archiveInputStream, outputPath, overwritePolicy, locationCellFactory, rowOutput, exec);
                } catch (EOFException e) {
                    throw new InvalidSettingsException(
                        "The end of the archive has been reached unexpectedly. The archive might be corrupted.", e);
                } catch (ArchiveException e) {
                    throw new IllegalArgumentException("Unsupported archive type", e);
                }
            }

        } finally {
            fsFac.close();
        }
    }

    private void createParentDirIfRequired(final FSPath outputPath) throws IOException, InvalidSettingsException {
        if (!FSFiles.exists(outputPath)) {
            if (m_config.getOutputDirChooserModel().isCreateMissingFolders()) {
                Files.createDirectories(outputPath);
            } else {
                throw new InvalidSettingsException(
                    String.format("The specified destination folder %s does not exist.", outputPath));
            }
        }
    }

    private static void decompress(final ArchiveInputStream archiveInputStream, final FSPath outputPath,
        final FileOverwritePolicy overwritePolicy, final FSLocationCellFactory locationCellFactory,
        final RowOutput rowOutput, final ExecutionContext exec) throws IOException, InterruptedException {

        long rowId = 0;
        ArchiveEntry entry;

        final Set<String> processedDirs = new HashSet<>();
        // Process each archive entry
        while ((entry = archiveInputStream.getNextEntry()) != null) {
            final Path destinationPath = outputPath.resolve(entry.getName());
            exec.setMessage("Decompressing " + destinationPath);
            rowId = createDirectories(outputPath, locationCellFactory, rowOutput, rowId, entry, processedDirs,
                destinationPath);
            if (!entry.isDirectory()) {
                final FileStatus status = writeToDestination(archiveInputStream, destinationPath, overwritePolicy);
                pushRow(locationCellFactory, rowOutput, rowId, destinationPath, status, false);
                rowId++;
            }
        }
    }

    private static long createDirectories(final FSPath outputPath, final FSLocationCellFactory locationCellFactory,
        final RowOutput rowOutput, long rowId, final ArchiveEntry entry, final Set<String> processedDirs,
        final Path destinationPath) throws IOException, InterruptedException {
        final Path relDestPath = outputPath.relativize(destinationPath);
        if (entry.isDirectory()) {
            rowId =
                createDirectories(processedDirs, rowId, rowOutput, relDestPath, destinationPath, locationCellFactory);
        } else {
            rowId = createDirectories(processedDirs, rowId, rowOutput, relDestPath.getParent(),
                destinationPath.getParent(), locationCellFactory);
        }
        return rowId;
    }

    private static long createDirectories(final Set<String> processedDirs, final long rowId, final RowOutput rowOutput,
        final Path relDestPath, final Path destPath, final FSLocationCellFactory locationCellFactory)
        throws IOException, InterruptedException {
        if (relDestPath == null) {
            return rowId;
        } else {
            if (!processedDirs.add(relDestPath.toString())) {
                return rowId;
            }
            final long idx;
            idx = createDirectories(processedDirs, rowId, rowOutput, relDestPath.getParent(), destPath.getParent(),
                locationCellFactory);
            final FileStatus status;
            if (FSFiles.exists(destPath)) {
                status = FileStatus.ALREADY_EXISTED;
            } else {
                FSFiles.createDirectories(destPath);
                status = FileStatus.CREATED;
            }
            pushRow(locationCellFactory, rowOutput, idx, destPath, status, true);
            return idx + 1;
        }
    }

    private static void pushRow(final FSLocationCellFactory locationCellFactory, final RowOutput rowOutput,
        final long rowId, final Path destinationPath, final FileStatus status, final boolean isDirectory)
        throws InterruptedException {
        final DataCell[] row = new DataCell[3];
        row[LOCATION_CELL_IDX] = locationCellFactory.createCell(destinationPath.toString());
        row[DIRECTORY_CELL_IDX] = BooleanCellFactory.create(isDirectory);
        row[STATUS_CELL_IDX] = StringCellFactory.create(status.getText());
        rowOutput.push(new DefaultRow(RowKey.createRowKey(rowId), row));
    }

    private static FileStatus writeToDestination(final ArchiveInputStream archiveInputStream,
        final Path destinationPath, final FileOverwritePolicy overwritePolicy) throws IOException {
        final boolean exists = FSFiles.exists(destinationPath);
        final FileStatus status;
        if (overwritePolicy == FileOverwritePolicy.FAIL) {
            if (exists) {
                throw new FileAlreadyExistsException(String
                    .format("The file '%s' already exists and must not be overwritten", destinationPath.toString()));
            }
            status = FileStatus.CREATED;
            Files.copy(archiveInputStream, destinationPath);
        } else if (overwritePolicy == FileOverwritePolicy.IGNORE) {
            if (exists) {
                status = FileStatus.UNMODIFIED;
            } else {
                status = FileStatus.CREATED;
                Files.copy(archiveInputStream, destinationPath);
            }
        } else if (overwritePolicy == FileOverwritePolicy.OVERWRITE) {
            if (exists) {
                status = FileStatus.OVERWRITTEN;
            } else {
                status = FileStatus.CREATED;
            }
            Files.copy(archiveInputStream, destinationPath, StandardCopyOption.REPLACE_EXISTING);
        } else {
            throw new IllegalArgumentException(
                String.format("Unsupported FileOverwritePolicy '%s' encountered.", overwritePolicy));
        }
        return status;
    }

    /**
     * Decompresses the given file.
     *
     * @param source The potentially compressed source stream
     * @return Uncompressed version of the source, or source if it was not compressed
     */
    @SuppressWarnings("resource") // closing the stream is the responsibility of the caller
    private static InputStream openUncompressStream(final InputStream source) {
        InputStream uncompressStream;
        // Buffered stream needed for autodetection of type
        InputStream in = new BufferedInputStream(source);
        try {
            // Try to create a compressor for the source, throws exception if
            // source is not compressed
            uncompressStream = new CompressorStreamFactory().createCompressorInputStream(in);
        } catch (CompressorException e) {
            // Source is not compressed
            uncompressStream = in;
            LOGGER.debug("Source is not compressed", e);
        }
        return uncompressStream;
    }

    @Override
    protected void reset() {
        // Not used
    }

    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_config.validateSettingsForModel(settings);
    }

    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_config.loadSettingsForModel(settings);
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_config.saveSettingsForModel(settings);
    }

    @Override
    protected void loadInternals(final File internDir, final ExecutionMonitor exec) {
        // Not used
    }

    @Override
    protected void saveInternals(final File internDir, final ExecutionMonitor exec) {
        // Not used
    }
}
