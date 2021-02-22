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

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveOutputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.archivers.ar.ArArchiveOutputStream;
import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
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
import org.knime.filehandling.core.connections.FSFiles;
import org.knime.filehandling.core.connections.FSLocation;
import org.knime.filehandling.core.connections.FSPath;
import org.knime.filehandling.core.defaultnodesettings.filechooser.writer.FileOverwritePolicy;
import org.knime.filehandling.core.defaultnodesettings.filechooser.writer.WritePathAccessor;
import org.knime.filehandling.core.defaultnodesettings.status.NodeModelStatusConsumer;
import org.knime.filehandling.core.defaultnodesettings.status.StatusMessage.MessageType;
import org.knime.filehandling.utility.nodes.compress.archiver.ArchiveEntryCreator;
import org.knime.filehandling.utility.nodes.compress.archiver.ArchiveEntryFactory;
import org.knime.filehandling.utility.nodes.compress.iterator.CompressEntry;
import org.knime.filehandling.utility.nodes.compress.iterator.CompressIterator;
import org.knime.filehandling.utility.nodes.compress.truncator.PathTruncator;
import org.knime.filehandling.utility.nodes.compress.truncator.TruncationException;

/**
 * Node Model for the "Compress Files/Folder" node
 *
 * @author Timmo Waller-Ehrat, KNIME GmbH, Konstanz, Germany
 * @author Mark Ortmann, KNIME GmbH, Berlin, Germany
 * @param <T> an instance of {@link AbstractCompressNodeConfig}
 */
public abstract class AbstractCompressNodeModel<T extends AbstractCompressNodeConfig> extends NodeModel {

    /**
     * The template string for the name collision error. It requires two strings, i.e., the paths to the files causing
     * the collision.
     */
    public static final String NAME_COLLISION_ERROR_TEMPLATE =
        "Name collision: '%s' and '%s' map to the same archive entry. Adapting the settings might resolve the problem.";

    private final T m_config;

    private final NodeModelStatusConsumer m_statusConsumer;

    /**
     * Constructor
     *
     * @param portsConfig {@link PortsConfiguration} of the node
     * @param config the configuration
     */
    protected AbstractCompressNodeModel(final PortsConfiguration portsConfig, final T config) {
        super(portsConfig.getInputPorts(), portsConfig.getOutputPorts());
        m_config = config;
        m_statusConsumer = new NodeModelStatusConsumer(EnumSet.of(MessageType.ERROR, MessageType.WARNING));
    }

    /**
     * Allows for additional configuration required by the concrete node model instance.
     *
     * @param inSpecs the input specs
     * @throws InvalidSettingsException - If something goes wrong during configuration
     */
    protected abstract void doConfigure(PortObjectSpec[] inSpecs) throws InvalidSettingsException;

    /**
     * Returns the configuration.
     *
     * @return the configuration
     */
    protected final T getConfig() {
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

    @Override
    protected final PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        doConfigure(inSpecs);
        getConfig().getTargetFileChooserModel().configureInModel(inSpecs, getStatusConsumer());
        getStatusConsumer().setWarningsIfRequired(this::setWarningMessage);
        return new PortObjectSpec[]{};
    }

    @Override
    protected final PortObject[] execute(final PortObject[] inData, final ExecutionContext exec) throws Exception {
        compress(inData, exec);
        return new PortObject[]{};
    }

    private void compress(final PortObject[] inData, final ExecutionContext exec)
        throws IOException, InvalidSettingsException, CanceledExecutionException {
        try (final CompressIterator filesToCompress = getFilesToCompress(inData, m_config.includeEmptyFolders())) {
            try (final WritePathAccessor writeAccessor =
                m_config.getTargetFileChooserModel().createWritePathAccessor()) {
                final FSPath outputPath = writeAccessor.getOutputPath(m_statusConsumer);
                m_statusConsumer.setWarningsIfRequired(this::setWarningMessage);

                final FileOverwritePolicy overwritePolicy =
                    m_config.getTargetFileChooserModel().getFileOverwritePolicy();
                createParentDirIfRequired(outputPath);
                compress(exec, filesToCompress, outputPath, overwritePolicy);
            }
        }
    }

    /**
     * Returns an {@link CompressIterator} encapsulating all files that have to be compressed.
     *
     * @param inData the input ports objects
     * @param includeEmptyFolders flag indicating whether or not to skip empty columns
     * @return an instance of {@link CompressIterator}
     * @throws IOException
     * @throws InvalidSettingsException
     */
    protected abstract CompressIterator getFilesToCompress(final PortObject[] inData, boolean includeEmptyFolders)
        throws IOException, InvalidSettingsException;

    private void createParentDirIfRequired(final FSPath outputPath) throws IOException {
        final Path parentPath = outputPath.getParent();
        if (parentPath != null && !Files.exists(parentPath)) {
            if (m_config.getTargetFileChooserModel().isCreateMissingFolders()) {
                FSFiles.createDirectories(parentPath);
            } else {
                throw new IOException(String.format(
                    "The directory '%s' does not exist and must not be created due to user settings.", parentPath));
            }
        }
    }

    private void compress(final ExecutionContext exec, final CompressIterator filesToCompress, final FSPath outputPath,
        final FileOverwritePolicy overwritePolicy)
        throws IOException, CanceledExecutionException, InvalidSettingsException {

        final String compression = m_config.getCompressionModel().getStringValue().toLowerCase();
        if (overwritePolicy == FileOverwritePolicy.FAIL && FSFiles.exists(outputPath)) {
            throw new FileAlreadyExistsException(
                String.format("The file '%s' already exists and must not be overwritten", outputPath));
        }
        try (final OutputStream outputStream = FSFiles.newOutputStream(outputPath, overwritePolicy.getOpenOptions())) {
            try (final OutputStream compressorStream = openCompressorStream(outputStream, compression)) {
                compress(exec, outputPath.toFSLocation(), filesToCompress, compressorStream, compression);
            } catch (CompressorException e) {
                throw new InvalidSettingsException("Unsupported compression type", e);
            }
        }
    }

    private void compress(final ExecutionContext exec, final FSLocation outputLocation,
        final CompressIterator filesToCompress, final OutputStream compressorStream, final String compression)
        throws IOException, CanceledExecutionException, InvalidSettingsException {
        final String archiver = getArchiver(compression);
        try (ArchiveOutputStream archiveStream =
            new ArchiveStreamFactory().createArchiveOutputStream(archiver, compressorStream)) {
            // without that only names with 16 chars would be possible, known limitation from the docs
            if (archiveStream instanceof ArArchiveOutputStream) {
                ((ArArchiveOutputStream)archiveStream).setLongFileMode(ArArchiveOutputStream.LONGFILE_BSD);
            }

            final ArchiveEntryCreator entryCreator = ArchiveEntryFactory.getArchiveEntryCreator(archiver);
            final long numOfFiles = filesToCompress.size();

            final Map<String, String> createdEntries = new HashMap<>();
            final PathTruncator pathTruncator = m_config.getTruncatePathOption()
                .createPathTruncator(m_config.flattenHierarchy(), m_config.getTruncateRegexModel().getStringValue());
            while (filesToCompress.hasNext()) {
                final ExecutionContext subExec = exec.createSubExecutionContext(1d / numOfFiles);
                compress(subExec, pathTruncator, outputLocation, filesToCompress.next(), archiveStream, entryCreator,
                    createdEntries);
            }
        } catch (ArchiveException e) {
            throw new IllegalArgumentException("Unsupported archive type", e);
        }

    }

    private static String getArchiver(final String compression) {
        final int archiverDelimiterIdx = compression.indexOf('.');
        final String archiver;
        if (archiverDelimiterIdx < 0) {
            archiver = compression;
        } else {
            archiver = compression.substring(0, archiverDelimiterIdx);
        }
        return archiver;
    }

    private void compress(final ExecutionContext exec, final PathTruncator pathTruncator,
        final FSLocation outputLocation, final CompressEntry compressEntry, final ArchiveOutputStream archiveStream,
        final ArchiveEntryCreator entryCreator, final Map<String, String> createdEntries)
        throws CanceledExecutionException, IOException, InvalidSettingsException {
        List<FSPath> pathsToCompress = compressEntry.getPaths();
        if (!pathsToCompress.isEmpty()) {
            final Path baseFolder = compressEntry.getBaseFolder().orElseGet(() -> null);
            int fileCounter = 1;
            final double numOfFiles = pathsToCompress.size();
            for (final FSPath pathToCompress : pathsToCompress) {
                exec.setProgress((fileCounter / numOfFiles), () -> ("Compressing file: " + pathToCompress.toString()));
                exec.checkCanceled();
                if (!pathToCompress.toFSLocation().equals(outputLocation)) {
                    compress(pathTruncator, archiveStream, entryCreator, createdEntries, pathsToCompress, baseFolder,
                        pathToCompress);
                } else {
                    setWarningMessage(String.format("Skipping the compression of '%s' as this is the archive itself",
                        pathToCompress.toString()));
                }
                fileCounter++;
            }
        } else {
            // can only be empty if the entry was created from a folder and therefore the base folder is always present
            compressEntry.getBaseFolder().ifPresent(
                p -> setWarningMessage(String.format("'%s' does not contain any files/folders to compress", p)));
        }
    }

    private void compress(final PathTruncator pathTruncator, final ArchiveOutputStream archiveStream,
        final ArchiveEntryCreator entryCreator, final Map<String, String> createdEntries,
        final List<FSPath> pathsToCompress, final Path baseFolder, final FSPath pathToCompress)
        throws IOException, InvalidSettingsException {
        try {
            final String entryName = pathTruncator.truncate(baseFolder, pathToCompress);
            addEntry(archiveStream, entryCreator, createdEntries, pathToCompress, entryName);
        } catch (final TruncationException e) {
            switch (e.getTruncatePathOption()) {
                case KEEP_SRC_FOLDER:
                    throw new InvalidSettingsException(String.format(
                        "The file '%s' is not located within any folder. Please change the truncation option.",
                        pathToCompress.toString()), e);
                case TRUNCATE_REGEX:
                    throw new InvalidSettingsException(String.format(
                        "%s Please adapt the regular expression or uncheck the include empty folders option.",
                        e.getMessage()), e);
                case TRUNCATE_SRC_FOLDER:
                    if (pathsToCompress.size() == 1 && baseFolder != null) {
                        setWarningMessage(String.format("'%s' does not contain any files/folders to compress",
                            baseFolder.toString()));
                    }
                    break;
                default:
                    throw e;
            }
        }
    }

    private static void addEntry(final ArchiveOutputStream archiveStream, final ArchiveEntryCreator entryCreator,
        final Map<String, String> createdEntries, final FSPath pathToCompress, final String entryName)
        throws IOException {
        if (!createdEntries.containsKey(entryName)) {
            final ArchiveEntry archiveEntry = entryCreator.apply(pathToCompress, entryName);
            createArchiveEntry(archiveStream, pathToCompress, archiveEntry);
        } else {
            throw new IllegalArgumentException(
                String.format(NAME_COLLISION_ERROR_TEMPLATE, createdEntries.get(entryName), pathToCompress.toString()));
        }
        createdEntries.put(entryName, pathToCompress.toString());
    }

    private static void createArchiveEntry(final ArchiveOutputStream archiveStream, final Path fileToCompress,
        final ArchiveEntry archiveEntry) throws IOException {
        archiveStream.putArchiveEntry(archiveEntry);
        try {
            if (!archiveEntry.isDirectory()) {
                Files.copy(fileToCompress, archiveStream);
            }
        } finally {
            archiveStream.closeArchiveEntry();
        }
    }

    @SuppressWarnings("resource") // closing the stream is the responsibility of the caller
    private static OutputStream openCompressorStream(final OutputStream outputStream, final String compression)
        throws CompressorException {
        final OutputStream compressorStream;

        if (compression.endsWith(AbstractCompressNodeConfig.BZ2_EXTENSION)) {
            compressorStream =
                new CompressorStreamFactory().createCompressorOutputStream(CompressorStreamFactory.BZIP2, outputStream);
        } else if (compression.endsWith(AbstractCompressNodeConfig.GZ_EXTENSION)) {
            compressorStream =
                new CompressorStreamFactory().createCompressorOutputStream(CompressorStreamFactory.GZIP, outputStream);
        } else {
            compressorStream = outputStream;
        }

        return compressorStream;
    }

    @Override
    protected final void reset() {
        // Not used
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
    protected final void saveSettingsTo(final NodeSettingsWO settings) {
        m_config.saveSettingsForModel(settings);
    }

    @Override
    protected final void loadInternals(final File internDir, final ExecutionMonitor exec) {
        // Not used
    }

    @Override
    protected final void saveInternals(final File internDir, final ExecutionMonitor exec) {
        // Not used
    }

}
