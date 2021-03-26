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
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
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
import org.knime.core.node.util.CheckUtils;
import org.knime.filehandling.core.connections.FSFiles;
import org.knime.filehandling.core.connections.FSPath;
import org.knime.filehandling.core.defaultnodesettings.filechooser.reader.ReadPathAccessor;
import org.knime.filehandling.core.defaultnodesettings.filechooser.writer.FileOverwritePolicy;
import org.knime.filehandling.core.defaultnodesettings.filechooser.writer.WritePathAccessor;
import org.knime.filehandling.core.defaultnodesettings.filtermode.SettingsModelFilterMode.FilterMode;
import org.knime.filehandling.core.defaultnodesettings.status.NodeModelStatusConsumer;
import org.knime.filehandling.core.defaultnodesettings.status.StatusMessage.MessageType;
import org.knime.filehandling.utility.nodes.compress.archiver.ArchiveEntryCreator;
import org.knime.filehandling.utility.nodes.compress.archiver.ArchiveEntryFactory;
import org.knime.filehandling.utility.nodes.utils.PathHandlingUtils;
import org.knime.filehandling.utility.nodes.utils.PathRelativizer;
import org.knime.filehandling.utility.nodes.utils.PathRelativizerNonTableInput;

/**
 * Node Model for the "Compress Files/Folder" node
 *
 * @author Timmo Waller-Ehrat, KNIME GmbH, Konstanz, Germany
 * @author Mark Ortmann, KNIME GmbH, Berlin, Germany
 * @deprecated since 4.3.3
 */
@Deprecated
final class CompressNodeModel extends NodeModel {

    /**
     * The template string for the name collision error. It requires two strings, i.e., the paths to the files causing
     * the collision.
     */
    static final String NAME_COLLISION_ERROR_TEMPLATE = "Name collision while hierarchy flattening ('%s' and '%s').";

    private final CompressNodeConfig m_config;

    private final NodeModelStatusConsumer m_statusConsumer;

    /**
     * Constructor
     *
     * @param portsConfig {@link PortsConfiguration} of the node
     */
    CompressNodeModel(final PortsConfiguration portsConfig) {
        super(portsConfig.getInputPorts(), portsConfig.getOutputPorts());
        m_config = new CompressNodeConfig(portsConfig);
        m_statusConsumer = new NodeModelStatusConsumer(EnumSet.of(MessageType.ERROR, MessageType.WARNING));
    }

    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        m_config.getInputLocationChooserModel().configureInModel(inSpecs, m_statusConsumer);
        m_config.getTargetFileChooserModel().configureInModel(inSpecs, m_statusConsumer);
        m_statusConsumer.setWarningsIfRequired(this::setWarningMessage);

        return new PortObjectSpec[]{};
    }

    @Override
    protected PortObject[] execute(final PortObject[] inData, final ExecutionContext exec) throws Exception {
        compress(exec);
        return new PortObject[]{};
    }

    private void compress(final ExecutionContext exec)
        throws IOException, InvalidSettingsException, CanceledExecutionException {
        try (final ReadPathAccessor readAccessor = m_config.getInputLocationChooserModel().createReadPathAccessor()) {
            final FSPath rootPath = readAccessor.getRootPath(m_statusConsumer);
            m_statusConsumer.setWarningsIfRequired(this::setWarningMessage);
            final List<FSPath> inputPaths = getInputPaths(readAccessor, rootPath);
            // fail if no files/folders need to be compressed as this would create an invalid archive
            CheckUtils.checkSetting(!inputPaths.isEmpty(), "No files and/or folders to compress have been specified");

            PathHandlingUtils.checkSettingsIncludeSourceFolder(m_config.getInputLocationChooserModel().getFilterMode(),
                m_config.includeSourceFolder(), rootPath);

            final PathRelativizer pathRelativizer = getPathRelativizer(rootPath);
            try (final WritePathAccessor writeAccessor =
                m_config.getTargetFileChooserModel().createWritePathAccessor()) {
                final FSPath outputPath = writeAccessor.getOutputPath(m_statusConsumer);
                m_statusConsumer.setWarningsIfRequired(this::setWarningMessage);

                final FileOverwritePolicy overwritePolicy =
                    m_config.getTargetFileChooserModel().getFileOverwritePolicy();
                createParentDirIfRequired(outputPath);
                compress(exec, inputPaths, pathRelativizer, outputPath, overwritePolicy);
            }
        }
    }

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

    private void compress(final ExecutionContext exec, final List<FSPath> inputPaths,
        final PathRelativizer pathRelativizer, final FSPath outputPath, final FileOverwritePolicy overwritePolicy)
        throws IOException, CanceledExecutionException, InvalidSettingsException {

        final String compression = m_config.getCompressionModel().getStringValue().toLowerCase();
        if (overwritePolicy == FileOverwritePolicy.FAIL && FSFiles.exists(outputPath)) {
            throw new FileAlreadyExistsException(
                String.format("The file '%s' already exists and must not be overwritten", outputPath));
        }
        try (final OutputStream outputStream = FSFiles.newOutputStream(outputPath, overwritePolicy.getOpenOptions())) {
            try (final OutputStream compressorStream = openCompressorStream(outputStream, compression)) {
                compress(exec, inputPaths, pathRelativizer, compressorStream, compression);
            } catch (CompressorException e) {
                throw new InvalidSettingsException("Unsupported compression type", e);
            }
        }
    }

    private static void compress(final ExecutionContext exec, final List<FSPath> inputPaths,
        final PathRelativizer pathRelativizer, final OutputStream compressorStream, final String compression)
        throws IOException, CanceledExecutionException {
        final String archiver = getArchiver(compression);
        try (ArchiveOutputStream archiveStream =
            new ArchiveStreamFactory().createArchiveOutputStream(archiver, compressorStream)) {
            // without that only names with 16 chars would be possible, known limitation from the docs
            if (archiveStream instanceof ArArchiveOutputStream) {
                ((ArArchiveOutputStream)archiveStream).setLongFileMode(ArArchiveOutputStream.LONGFILE_BSD);
            }

            final ArchiveEntryCreator entryCreator = ArchiveEntryFactory.getArchiveEntryCreator(archiver);
            final long numOfFiles = inputPaths.size();
            long fileCounter = 0;

            final Map<String, String> createdEntries = new HashMap<>();
            for (Path toCompress : inputPaths) {
                exec.setProgress((fileCounter / (double)numOfFiles),
                    () -> ("Compressing file: " + toCompress.toString()));
                exec.checkCanceled();
                addEntry(archiveStream, pathRelativizer, createdEntries, toCompress, entryCreator);
                fileCounter++;
            }
        } catch (ArchiveException e) {
            throw new IllegalArgumentException("Unsupported archive type", e);
        }
    }

    private List<FSPath> getInputPaths(final ReadPathAccessor readAccessor, final FSPath rootPath)
        throws IOException, InvalidSettingsException {
        if (m_config.getInputLocationChooserModel().getFilterMode() == FilterMode.FOLDER) {
            return getFilesAndEmptyFolders(rootPath);
        } else {
            return readAccessor.getFSPaths(m_statusConsumer);
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

    private PathRelativizer getPathRelativizer(final Path rootPath) {
        final FilterMode filterMode = m_config.getInputLocationChooserModel().getFilterMode();
        final boolean includeParent = m_config.includeSourceFolder();
        return new PathRelativizerNonTableInput(rootPath, includeParent, filterMode, m_config.flattenHierarchy());
    }

    private static void addEntry(final ArchiveOutputStream archiveStream, final PathRelativizer pathRelativizer,
        final Map<String, String> createdEntries, final Path toCompress, final ArchiveEntryCreator entryCreator)
        throws IOException {
        final String entryName = pathRelativizer.apply(toCompress);
        if (!createdEntries.containsKey(entryName)) {
            createdEntries.put(entryName, toCompress.toString());
            final ArchiveEntry archiveEntry = entryCreator.apply(toCompress, entryName);
            createArchiveEntry(archiveStream, toCompress, archiveEntry);
        } else {
            throw new IllegalArgumentException(
                String.format(NAME_COLLISION_ERROR_TEMPLATE, createdEntries.get(entryName), toCompress.toString()));
        }
    }

    private static void createArchiveEntry(final ArchiveOutputStream archiveStream, final Path toCompress,
        final ArchiveEntry archiveEntry) throws IOException {
        archiveStream.putArchiveEntry(archiveEntry);
        try {
            if (!archiveEntry.isDirectory()) {
                Files.copy(toCompress, archiveStream);
            }
        } finally {
            archiveStream.closeArchiveEntry();
        }
    }

    @SuppressWarnings("resource") // closing the stream is the responsibility of the caller
    private static OutputStream openCompressorStream(final OutputStream outputStream, final String compression)
        throws CompressorException {
        final OutputStream compressorStream;

        if (compression.endsWith(CompressNodeConfig.BZ2_EXTENSION)) {
            compressorStream =
                new CompressorStreamFactory().createCompressorOutputStream(CompressorStreamFactory.BZIP2, outputStream);
        } else if (compression.endsWith(CompressNodeConfig.GZ_EXTENSION)) {
            compressorStream =
                new CompressorStreamFactory().createCompressorOutputStream(CompressorStreamFactory.GZIP, outputStream);
        } else {
            compressorStream = outputStream;
        }

        return compressorStream;
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
    protected void loadInternals(final File internDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        // Not used
    }

    @Override
    protected void saveInternals(final File internDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        // Not used
    }

    /**
     * Returns a {@link List} of {@link FSPath}s of a all files in a single folder.
     *
     * @param folder the {@link Path} of the source folder
     * @param linkOptions for resolving symbolic links
     * @return a {@link List} of {@link Path} from files in a folder
     * @throws IOException
     */
    private List<FSPath> getFilesAndEmptyFolders(final FSPath folder, final LinkOption... linkOptions)// NOSONAR
        throws IOException {
        final List<FSPath> paths = new ArrayList<>();
        CheckUtils.checkArgument(FSFiles.isDirectory(folder, linkOptions),
            "%s is not a folder. Please specify a folder.", folder);
        Files.walkFileTree(folder, new FileAndEmptyFolderVisitor(paths));
        // make sure that we include the parent folder if it's empty and the user has selected this option
        if (paths.isEmpty() && m_config.includeSourceFolder() && !m_config.flattenHierarchy()) {
            paths.add(folder);
        }
        FSFiles.sortPathsLexicographically(paths);
        return paths;
    }

    private static class FileAndEmptyFolderVisitor extends SimpleFileVisitor<Path> {

        private final List<FSPath> m_paths;

        int m_visitedFolders = 0;

        boolean m_empty;

        FileAndEmptyFolderVisitor(final List<FSPath> paths) {
            m_paths = paths;
        }

        @Override
        public FileVisitResult preVisitDirectory(final Path dir, final BasicFileAttributes attrs) throws IOException {
            m_empty = true;
            m_visitedFolders++;
            return super.preVisitDirectory(dir, attrs);
        }

        @Override
        public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
            m_paths.add((FSPath)file);
            m_empty = false;
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult postVisitDirectory(final Path dir, final IOException exc) throws IOException {
            m_visitedFolders--;
            if (m_empty && m_visitedFolders > 0) {
                m_paths.add((FSPath)dir);
            }
            m_empty = false;
            return super.postVisitDirectory(dir, exc);
        }
    }
}
