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
 *   Sep 23, 2020 (lars.schweikardt): created
 */
package org.knime.base.node.io.filehandling.util.decompress;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;

import org.apache.commons.io.FilenameUtils;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.filehandling.core.connections.FSFiles;
import org.knime.filehandling.core.connections.FSPath;
import org.knime.filehandling.core.defaultnodesettings.ExceptionUtil;
import org.knime.filehandling.core.defaultnodesettings.filechooser.StatusMessageReporter;
import org.knime.filehandling.core.defaultnodesettings.filechooser.reader.ReadPathAccessor;
import org.knime.filehandling.core.defaultnodesettings.filechooser.reader.SettingsModelReaderFileChooser;
import org.knime.filehandling.core.defaultnodesettings.filechooser.writer.DialogComponentWriterFileChooser;
import org.knime.filehandling.core.defaultnodesettings.filechooser.writer.FileOverwritePolicy;
import org.knime.filehandling.core.defaultnodesettings.filechooser.writer.SettingsModelWriterFileChooser;
import org.knime.filehandling.core.defaultnodesettings.filechooser.writer.WritePathAccessor;
import org.knime.filehandling.core.defaultnodesettings.status.DefaultStatusMessage;
import org.knime.filehandling.core.defaultnodesettings.status.PriorityStatusConsumer;
import org.knime.filehandling.core.defaultnodesettings.status.StatusMessage;
import org.knime.filehandling.core.defaultnodesettings.status.StatusMessageNewPathUtils;

/**
 * Computes the status message for the destination {@link DialogComponentWriterFileChooser} of the decompress node
 * dialog.
 *
 * @author Lars Schweikardt, KNIME GmbH, Konstanz, Germany
 */
final class DecompressStatusMessageReporter implements StatusMessageReporter {

    private static final String[] FILE_EXTENSIONS =
        new String[]{"zip", ".jar", ".tar", ".tar.gz", ".tar.bz2", ".cpio", ".ar"};

    private static final StatusMessage INVALID_SOURCE = DefaultStatusMessage.mkError("Invalid source path.");

    private static final NodeLogger LOGGER = NodeLogger.getLogger(DecompressStatusMessageReporter.class);

    private final SettingsModelWriterFileChooser m_settingsWriter;

    private final SettingsModelReaderFileChooser m_settingsReader;

    private final PriorityStatusConsumer m_consumerWriter = new PriorityStatusConsumer();

    private final PriorityStatusConsumer m_consumerReader = new PriorityStatusConsumer();

    private final ExistsHandler m_existsHandler;

    /**
     * Constructor.
     *
     * @param settings the writer file chooser settings
     */
    DecompressStatusMessageReporter(final SettingsModelWriterFileChooser settingsWriter,
        final SettingsModelReaderFileChooser settingsReader) {
        m_settingsWriter = settingsWriter;
        m_settingsReader = settingsReader;
        m_existsHandler = createExistsHandler();
    }

    @Override
    public StatusMessage report() throws IOException, InvalidSettingsException {
        try (final WritePathAccessor writePathAccessor = m_settingsWriter.createWritePathAccessor();
                final ReadPathAccessor readPathAccessor = m_settingsReader.createReadPathAccessor()) {

            final FSPath destinationPath = writePathAccessor.getOutputPath(m_consumerWriter);

            try {
                final Path rootPathSource = readPathAccessor.getRootPath(m_consumerReader);

                if (!Files.readAttributes(rootPathSource, BasicFileAttributes.class).isRegularFile()) {
                    return INVALID_SOURCE;
                }
                final String rootPathSourceLower = rootPathSource.toString().toLowerCase();
                if ((Arrays.stream(FILE_EXTENSIONS).noneMatch(rootPathSourceLower::endsWith))) {
                    return DefaultStatusMessage.mkError("Invalid source file extension .%s",
                        FilenameUtils.getExtension(rootPathSource.getFileName().toString()));
                }
            } catch (InvalidSettingsException e) {
                LOGGER.error(
                    "An error occured during the process of retrieving the source path. See log for more details.", e);
                return INVALID_SOURCE;
            }

            if (FSFiles.exists(destinationPath)) {
                return handlePathExists(destinationPath);
            } else {
                return StatusMessageNewPathUtils.handleNewPath(m_settingsWriter.isCreateMissingFolders());
            }
        }
    }

    /**
     * Handler in case the {@link DialogComponentWriterFileChooser} gets a path which exist.
     *
     * @param path the destination {@link Path}
     * @param numberOfExistingPahts the number of already existing paths
     * @return the corresponding {@link StatusMessage}
     * @throws AccessDeniedException
     */
    private StatusMessage handlePathExists(final FSPath path) throws AccessDeniedException {
        if (!Files.isWritable(path)) {
            throw ExceptionUtil.createAccessDeniedException(path);
        }
        try {
            return m_existsHandler.create(path, Files.readAttributes(path, BasicFileAttributes.class));
        } catch (IOException ex) {
            LOGGER.error("Can't access attributes of existing path", ex);
            return DefaultStatusMessage.mkError("Can't access attributes of %s.", path);
        }
    }

    /**
     * Creates the {@link ExistsHandler}.
     *
     * @return a {@link ExistsHandler}
     */
    private static ExistsHandler createExistsHandler() {
        return DecompressStatusMessageReporter::createStatusMessage;
    }

    private interface ExistsHandler {
        StatusMessage create(final Path path, final BasicFileAttributes attrs);
    }

    /**
     * Creates the status messages based on a {@link Path}, {@link BasicFileAttributes} and the
     * {@link FileOverwritePolicy}.
     *
     * @param path the destination {@link Path}
     * @param attrs the {@link BasicFileAttributes} of the destination {@link Path}
     * @param numberOfExistingPaths the number of already existing paths in the destination folder
     * @return the {@link StatusMessage}
     */
    private static StatusMessage createStatusMessage(final Path path, final BasicFileAttributes attrs) {
        if (attrs.isDirectory()) {
            return DefaultStatusMessage.SUCCESS_MSG;
        } else {
            return DefaultStatusMessage.mkError("%s is not a folder.", path);
        }
    }
}
