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

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
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
import org.apache.commons.compress.archivers.ar.ArArchiveEntry;
import org.apache.commons.compress.archivers.ar.ArArchiveOutputStream;
import org.apache.commons.compress.archivers.cpio.CpioArchiveEntry;
import org.apache.commons.compress.archivers.jar.JarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.apache.commons.io.FilenameUtils;
import org.knime.base.node.io.filehandling.util.PathRelativizer;
import org.knime.base.node.io.filehandling.util.PathRelativizerNonTableInput;
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
import org.knime.filehandling.core.connections.FSPath;
import org.knime.filehandling.core.defaultnodesettings.filechooser.reader.ReadPathAccessor;
import org.knime.filehandling.core.defaultnodesettings.filechooser.writer.FileOverwritePolicy;
import org.knime.filehandling.core.defaultnodesettings.filechooser.writer.WritePathAccessor;
import org.knime.filehandling.core.defaultnodesettings.filtermode.SettingsModelFilterMode.FilterMode;
import org.knime.filehandling.core.defaultnodesettings.status.NodeModelStatusConsumer;
import org.knime.filehandling.core.defaultnodesettings.status.StatusMessage.MessageType;

/**
 * Node Model for the "Compress Files/Folder" node
 *
 * @author Timmo Waller-Ehrat, KNIME GmbH, Konstanz, Germany
 */
final class CompressNodeModel extends NodeModel {

    private static final String BZ2_EXTENSION = "bz2";

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
        try (final WritePathAccessor writeAccessor = m_config.getTargetFileChooserModel().createWritePathAccessor()) {
            final FSPath outputPath = writeAccessor.getOutputPath(m_statusConsumer);
            m_statusConsumer.setWarningsIfRequired(this::setWarningMessage);

            final FileOverwritePolicy overwritePolicy = m_config.getTargetFileChooserModel().getFileOverwritePolicy();
            if (!(FSFiles.exists(outputPath) && overwritePolicy == FileOverwritePolicy.IGNORE)) {

                final Path parentPath = outputPath.getParent();
                if (parentPath != null && !FSFiles.exists(parentPath)
                    && m_config.getTargetFileChooserModel().isCreateMissingFolders()) {
                    Files.createDirectories(parentPath);
                }

                compress(outputPath, exec, overwritePolicy);
            }
        }
    }

    private void compress(final FSPath outputPath, final ExecutionContext exec,
        final FileOverwritePolicy overwritePolicy)
        throws IOException, CanceledExecutionException, InvalidSettingsException {

        final String fileExtension = FilenameUtils.getExtension(outputPath.toString());

        try (final OutputStream outputStream = FSFiles.newOutputStream(outputPath, overwritePolicy.getOpenOptions())) {
            try (final OutputStream compressorStream = openCompressorStream(outputStream, fileExtension)) {
                compress(compressorStream, fileExtension, exec);
            } catch (CompressorException e) {
                throw new InvalidSettingsException("Unsupported compression type", e);
            }
        }
    }

    private List<FSPath> getInputPaths(final ReadPathAccessor readAccessor)
        throws IOException, InvalidSettingsException {
        if (m_config.getInputLocationChooserModel().getFilterMode() == FilterMode.FOLDER) {
            return FSFiles.getFilePathsFromFolder(readAccessor.getRootPath(m_statusConsumer));
        } else {
            return readAccessor.getFSPaths(m_statusConsumer);
        }
    }

    private void compress(final OutputStream compressorStream, final String fileExtension, final ExecutionContext exec)
        throws IOException, CanceledExecutionException, InvalidSettingsException {

        final String archiver = fileExtension.equalsIgnoreCase(BZ2_EXTENSION)
            || fileExtension.equalsIgnoreCase(CompressorStreamFactory.GZIP) ? ArchiveStreamFactory.TAR : fileExtension;

        try (final ReadPathAccessor readAccessor = m_config.getInputLocationChooserModel().createReadPathAccessor()) {
            final List<FSPath> inputPaths = getInputPaths(readAccessor);
            final Path rootPath = readAccessor.getRootPath(m_statusConsumer);
            m_statusConsumer.setWarningsIfRequired(this::setWarningMessage);

            try (ArchiveOutputStream archiveStream =
                new ArchiveStreamFactory().createArchiveOutputStream(archiver, compressorStream)) {

                // without that only names with 16 chars would be possible, known limitation from the docs
                if (archiveStream instanceof ArArchiveOutputStream) {
                    ((ArArchiveOutputStream)archiveStream).setLongFileMode(ArArchiveOutputStream.LONGFILE_BSD);
                }

                final FilterMode filterMode = m_config.getInputLocationChooserModel().getFilterMode();
                final boolean includeParent = m_config.includeParentFolder();

                final PathRelativizer pathRelativizer =
                    new PathRelativizerNonTableInput(rootPath, includeParent, filterMode, m_config.flattenHierarchy());

                final long numOfFiles = inputPaths.size();
                long fileCounter = 0;

                final Map<String, String> createdEntries = new HashMap<>();
                for (Path file : inputPaths) {
                    exec.setProgress((fileCounter / (double)numOfFiles),
                        () -> ("Compressing file: " + file.toString()));
                    exec.checkCanceled();
                    addEntry(archiver, archiveStream, pathRelativizer, createdEntries, file);
                    fileCounter++;
                }
            } catch (ArchiveException e) {
                throw new IllegalArgumentException("Unsupported archive type", e);
            }
        }

    }

    private static void addEntry(final String archiver, final ArchiveOutputStream archiveStream,
        final PathRelativizer pathRelativizer, final Map<String, String> createdEntries, final Path file)
        throws IOException {
        final String entryName = pathRelativizer.apply(file);
        if (!createdEntries.containsKey(entryName)) {
            createdEntries.put(entryName, file.toString());
            createArchiveEntry(archiveStream, file, entryName, archiver);
        } else {
            throw new IllegalArgumentException(
                String.format(NAME_COLLISION_ERROR_TEMPLATE, createdEntries.get(entryName), file.toString()));
        }
    }

    private static void createArchiveEntry(final ArchiveOutputStream archiveStream, final Path file,
        final String entryName, final String archiver) throws IOException {
        archiveStream.putArchiveEntry(getArchiveEntry(archiver, file, entryName));
        try {
            Files.copy(file, archiveStream);
        } finally {
            archiveStream.closeArchiveEntry();
        }
    }

    @SuppressWarnings("resource") // closing the stream is the responsibility of the caller
    private static OutputStream openCompressorStream(final OutputStream outputStream, final String fileExtension)
        throws CompressorException {
        final OutputStream compressorStream;

        if (fileExtension.equalsIgnoreCase(BZ2_EXTENSION)) {
            compressorStream =
                new CompressorStreamFactory().createCompressorOutputStream(CompressorStreamFactory.BZIP2, outputStream);
        } else if (fileExtension.equalsIgnoreCase(CompressorStreamFactory.GZIP)) {
            compressorStream =
                new CompressorStreamFactory().createCompressorOutputStream(CompressorStreamFactory.GZIP, outputStream);
        } else {
            compressorStream = outputStream;
        }

        return compressorStream;
    }

    private static ArchiveEntry getArchiveEntry(final String archiver, final Path path, final String entryName)
        throws IOException {

        final ArchiveEntry archiveEntry;
        final File file = path.toFile();

        if (archiver.equalsIgnoreCase(ArchiveStreamFactory.AR)) {
            archiveEntry = new ArArchiveEntry(file, entryName);
        } else if (archiver.equalsIgnoreCase(ArchiveStreamFactory.CPIO)) {
            final long fileSize = (long)Files.readAttributes(path, "size").get("size");

            archiveEntry = new CpioArchiveEntry(entryName, fileSize);
        } else if (archiver.equalsIgnoreCase(ArchiveStreamFactory.JAR)) {
            archiveEntry = new JarArchiveEntry(entryName);
        } else if (archiver.equalsIgnoreCase(ArchiveStreamFactory.TAR)) {
            archiveEntry = new TarArchiveEntry(file, entryName);
        } else if (archiver.equalsIgnoreCase(ArchiveStreamFactory.ZIP)) {
            archiveEntry = new ZipArchiveEntry(file, entryName);
        } else {
            throw new IllegalArgumentException("Unsupported type: " + archiver);
        }

        return archiveEntry;
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
}
