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
 *   08.04.2021 (Lars Schweikardt, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.utility.nodes.decompress;

import java.io.BufferedInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.AccessDeniedException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.compressors.CompressorInputStream;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.apache.commons.io.FilenameUtils;
import org.knime.core.data.DataCell;
import org.knime.core.data.RowKey;
import org.knime.core.data.def.BooleanCell.BooleanCellFactory;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.StringCell.StringCellFactory;
import org.knime.core.data.util.CancellableReportingInputStream;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.streamable.RowOutput;
import org.knime.filehandling.core.connections.FSFiles;
import org.knime.filehandling.core.connections.FSPath;
import org.knime.filehandling.core.data.location.cell.SimpleFSLocationCellFactory;
import org.knime.filehandling.core.defaultnodesettings.filechooser.writer.FileOverwritePolicy;
import org.knime.filehandling.utility.nodes.utils.FileStatus;

/**
 * Decompresses and unarchives a source file to a specified destination.
 *
 * @author Lars Schweikardt, KNIME GmbH, Konstanz, Germany
 */
final class Decompressor {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(Decompressor.class);

    private static final int LOCATION_CELL_IDX = 0;

    private static final int DIRECTORY_CELL_IDX = 1;

    private static final int STATUS_CELL_IDX = 2;

    private boolean m_isCompressed = false;

    private boolean m_isArchived = false;

    private final RowOutput m_rowOutput;

    private final ExecutionContext m_exec;

    private final DecompressNodeConfig m_config;

    private final WriteFileFunction m_writeFileFunction;

    private final SimpleFSLocationCellFactory m_locationCellFactory;

    Decompressor(final DecompressNodeConfig config, final RowOutput rowOutput, final ExecutionContext exec) {
        m_config = config;
        m_rowOutput = rowOutput;
        m_exec = exec;
        m_writeFileFunction = getWriteFileFunction(m_config.getOutputDirChooserModel().getFileOverwritePolicy());
        m_locationCellFactory = new SimpleFSLocationCellFactory(m_config.getOutputDirChooserModel().getLocation());
    }

    /**
     * Decompresses the file of the source path to the destination path.
     *
     * @param sourcePath the source {@link FSPath} of the file
     * @param destinationPath the {@link FSPath} to the destination
     * @throws IOException
     * @throws InterruptedException
     * @throws InvalidSettingsException
     */
    void decompress(final FSPath sourcePath, final FSPath destinationPath)
        throws IOException, InterruptedException, InvalidSettingsException {
        final long fileSize = Files.readAttributes(sourcePath, BasicFileAttributes.class).size();
        try (final InputStream sourceStream =
            new CancellableReportingInputStream(Files.newInputStream(sourcePath), m_exec, fileSize)) {
            createParentDirIfRequired(destinationPath);
            try (final InputStream archiveInputStream = openStreams(sourceStream)) {
                if (m_isArchived) {
                    decompressArchive((ArchiveInputStream)archiveInputStream, destinationPath);
                } else {
                    decompressFile(archiveInputStream, sourcePath, destinationPath);
                }
            }
        } catch (EOFException e) {
            throw new InvalidSettingsException(
                "The end of the archive has been reached unexpectedly. The archive might be corrupted.", e);
        }
    }

    /**
     * Creates destination directory if needed.
     *
     * @param destinationPath the destination {@link FSPath}
     * @throws IOException
     * @throws InvalidSettingsException
     */
    private void createParentDirIfRequired(final FSPath destinationPath) throws IOException, InvalidSettingsException {
        if (!FSFiles.exists(destinationPath)) {
            if (m_config.getOutputDirChooserModel().isCreateMissingFolders()) {
                Files.createDirectories(destinationPath);
            } else {
                throw new InvalidSettingsException(
                    String.format("The specified destination folder %s does not exist.", destinationPath));
            }
        } else {
            checkDestinationIsDirectory(destinationPath);
        }
    }

    /**
     * Checks if the destination path is a directory.
     *
     * @param destinationPath the destination {@link FSPath}
     * @throws AccessDeniedException if access is denied
     * @throws InvalidSettingsException if destinationPath is not a directory
     */
    private static void checkDestinationIsDirectory(final FSPath destinationPath)
        throws AccessDeniedException, InvalidSettingsException {
        if(!FSFiles.isDirectory(destinationPath)) {
            throw new InvalidSettingsException(String.format("The specified path '%s' does not point to a folder", destinationPath));
        }
    }

    /**
     * Opens the neccessary streams do decompress and / or unarchive the source file.
     *
     * @param source the source {@link InputStream}
     * @return a instance of an{@link InputStream} based on the source file extension
     * @throws InvalidSettingsException is thrown when the source file is not an archive or not comporessed
     */
    @SuppressWarnings("resource") //closing responsibility of the caller
    private InputStream openStreams(final InputStream source) throws InvalidSettingsException {
        final InputStream inputStream = openArchiveStream(openUncompressStream(source));
        if (!m_isArchived && !m_isCompressed) {
            throw new InvalidSettingsException(
                "File is not an archive and not compressed. See node description for supported file formats.");
        }
        return inputStream;
    }

    /**
     * Creates a {@link ArchiveInputStream}.
     *
     * @param source The potentially archived source stream
     * @return Unarchived version of the source, or source if it was not archived
     */
    private InputStream openArchiveStream(final InputStream source) {
        // Buffered stream needed for autodetection of type
        final BufferedInputStream bufferedStream = new BufferedInputStream(source);
        try {
            ArchiveStreamFactory.detect(bufferedStream);
            m_isArchived = true;
            return new ArchiveStreamFactory().createArchiveInputStream(bufferedStream);
        } catch (ArchiveException e) {
            if (m_isArchived) {
                throw new IllegalArgumentException(
                    "Something went wrong during the creation of the ArchiveStream. See log for more details.", e);
            } else {
                LOGGER.debug("Source is not an archive.", e);
                return bufferedStream;
            }
        }
    }

    /**
     * Creates a {@link CompressorInputStream}.
     *
     * @param source The potentially compressed source stream
     * @return Uncompressed version of the source, or source if it was not compressed
     */
    private InputStream openUncompressStream(final InputStream source) {
        // Buffered stream needed for autodetection of type
        final BufferedInputStream bufferedStream = new BufferedInputStream(source);
        try {
            CompressorStreamFactory.detect(bufferedStream);
            m_isCompressed = true;
            return new CompressorStreamFactory().createCompressorInputStream(bufferedStream);
        } catch (CompressorException e) {
            if (m_isCompressed) {
                throw new IllegalArgumentException(
                    "Something went wrong during the creation of the CompressorInputStream. See log for more details.",
                    e);
            } else {
                LOGGER.debug("Source is not compressed.", e);
                return bufferedStream;
            }
        }
    }

    /**
     * Decompresses an archive.
     *
     * @param archiveInputStream the {@link ArchiveInputStream}
     * @throws IOException
     * @throws InterruptedException
     */
    private void decompressArchive(final ArchiveInputStream archiveInputStream, final FSPath destinationPath)
        throws IOException, InterruptedException {
        final Set<String> processedDirs = new HashSet<>();
        long rowId = 0;
        ArchiveEntry entry;

        // Process each archive entry
        while ((entry = archiveInputStream.getNextEntry()) != null) {
            final Path outputFilePath = destinationPath.resolve(entry.getName());
            final boolean isDirectory = entry.isDirectory();
            m_exec.setMessage("Decompressing " + outputFilePath);
            rowId = createDirectories(destinationPath, rowId, processedDirs, outputFilePath, isDirectory);
            if (!isDirectory) {
                final FileStatus status = m_writeFileFunction.apply(archiveInputStream, outputFilePath);
                pushRow(rowId, outputFilePath, status, false);
                rowId++;
            }
        }
    }

    /**
     * Decompresses a compressed file.
     *
     * @param uncompressInputStream the
     * @throws IOException
     * @throws InterruptedException
     */
    private void decompressFile(final InputStream uncompressInputStream, final FSPath sourcePath,
        final FSPath destinationPath) throws IOException, InterruptedException {
        final Path outputFilePath = destinationPath.resolve(FilenameUtils.getBaseName(sourcePath.toString()));
        m_exec.setMessage("Decompressing " + outputFilePath);
        final FileStatus status = m_writeFileFunction.apply(uncompressInputStream, outputFilePath);
        pushRow(0, outputFilePath, status, false);
    }

    private long createDirectories(final FSPath destinationPath, long rowId, final Set<String> processedDirs,
        final Path outputFilePath, final boolean isDirectory) throws IOException, InterruptedException {
        final Path relDestPath = destinationPath.relativize(outputFilePath);
        if (isDirectory) {
            rowId = createDirectories(processedDirs, rowId, relDestPath, outputFilePath);
        } else {
            rowId = createDirectories(processedDirs, rowId, relDestPath.getParent(), outputFilePath.getParent());
        }
        return rowId;
    }

    private long createDirectories(final Set<String> processedDirs, final long rowId, final Path relDestPath,
        final Path destPath) throws IOException, InterruptedException {
        if (relDestPath == null) {
            return rowId;
        } else {
            if (!processedDirs.add(relDestPath.toString())) {
                return rowId;
            }
            final long idx;
            //The recursion is necessary since we want to create a row for every folder
            idx = createDirectories(processedDirs, rowId, relDestPath.getParent(), destPath.getParent());
            final FileStatus status;
            if (FSFiles.exists(destPath)) {
                status = FileStatus.ALREADY_EXISTED;
            } else {
                FSFiles.createDirectories(destPath);
                status = FileStatus.CREATED;
            }
            pushRow(idx, destPath, status, true);
            return idx + 1;
        }
    }

    private void pushRow(final long rowId, final Path destinationPath, final FileStatus status,
        final boolean isDirectory) throws InterruptedException {
        final DataCell[] row = new DataCell[3];
        row[LOCATION_CELL_IDX] = m_locationCellFactory.createCell(destinationPath.toString());
        row[DIRECTORY_CELL_IDX] = BooleanCellFactory.create(isDirectory);
        row[STATUS_CELL_IDX] = StringCellFactory.create(status.getText());
        m_rowOutput.push(new DefaultRow(RowKey.createRowKey(rowId), row));
    }

    /**
     * Returns a specific {@link WriteFileFunction} based on the selected {@link FileOverwritePolicy}.
     *
     * @param fileOverWritePolicy the {@link FileOverwritePolicy}
     * @return a {@link WriteFileFunction}
     */
    private static WriteFileFunction getWriteFileFunction(final FileOverwritePolicy fileOverWritePolicy) {
        switch (fileOverWritePolicy) {
            case OVERWRITE:
                return Decompressor::createOverwriteWriteFileFunction;
            case FAIL:
                return Decompressor::createFailWriteFileFunction;
            case IGNORE:
                return Decompressor::createIgnoreWriteFileFunction;
            default:
                throw new IllegalArgumentException(
                    String.format("Unsupported FileOverwritePolicy '%s' encountered.", fileOverWritePolicy));
        }
    }

    private static FileStatus createOverwriteWriteFileFunction(final InputStream inputStream, final Path path)
        throws IOException {
        final FileStatus status = FSFiles.exists(path) ? FileStatus.OVERWRITTEN : FileStatus.CREATED;
        Files.copy(inputStream, path, StandardCopyOption.REPLACE_EXISTING);
        return status;
    }

    private static FileStatus createFailWriteFileFunction(final InputStream inputStream, final Path path)
        throws IOException {
        if (FSFiles.exists(path)) {
            throw new FileAlreadyExistsException(
                String.format("The file '%s' already exists and must not be overwritten", path.toString()));
        }
        Files.copy(inputStream, path);
        return FileStatus.CREATED;
    }

    private static FileStatus createIgnoreWriteFileFunction(final InputStream inputStream, final Path path)
        throws IOException {
        final FileStatus status;
        if (FSFiles.exists(path)) {
            status = FileStatus.UNMODIFIED;
        } else {
            status = FileStatus.CREATED;
            Files.copy(inputStream, path);
        }
        return status;
    }
}
