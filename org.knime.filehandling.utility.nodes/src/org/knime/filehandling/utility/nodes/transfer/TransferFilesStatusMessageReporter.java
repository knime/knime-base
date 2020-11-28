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
 *   September 21, 2020 (Lars Schweikardt, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.utility.nodes.transfer;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Iterator;
import java.util.List;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.filehandling.core.connections.FSFiles;
import org.knime.filehandling.core.connections.FSPath;
import org.knime.filehandling.core.defaultnodesettings.ExceptionUtil;
import org.knime.filehandling.core.defaultnodesettings.filechooser.StatusMessageReporter;
import org.knime.filehandling.core.defaultnodesettings.filechooser.reader.DialogComponentReaderFileChooser;
import org.knime.filehandling.core.defaultnodesettings.filechooser.reader.ReadPathAccessor;
import org.knime.filehandling.core.defaultnodesettings.filechooser.reader.SettingsModelReaderFileChooser;
import org.knime.filehandling.core.defaultnodesettings.filechooser.writer.DialogComponentWriterFileChooser;
import org.knime.filehandling.core.defaultnodesettings.filechooser.writer.FileOverwritePolicy;
import org.knime.filehandling.core.defaultnodesettings.filechooser.writer.SettingsModelWriterFileChooser;
import org.knime.filehandling.core.defaultnodesettings.filechooser.writer.WritePathAccessor;
import org.knime.filehandling.core.defaultnodesettings.filtermode.SettingsModelFilterMode.FilterMode;
import org.knime.filehandling.core.defaultnodesettings.status.DefaultStatusMessage;
import org.knime.filehandling.core.defaultnodesettings.status.StatusMessage;
import org.knime.filehandling.core.defaultnodesettings.status.StatusMessageUtils;
import org.knime.filehandling.utility.nodes.utils.PathRelativizer;
import org.knime.filehandling.utility.nodes.utils.PathRelativizerNonTableInput;

/**
 * Computes the status message for the destination {@link DialogComponentWriterFileChooser} of the copy move dialog .
 *
 * @author Lars Schweikardt, KNIME GmbH, Konstanz, Germany
 */
final class TransferFilesStatusMessageReporter implements StatusMessageReporter {

    private static final StatusMessage INVALID_SOURCE = DefaultStatusMessage.mkError("Invalid source path.");

    private static final String SRC_PATH_ERROR_MSG =
        "An error occured during the process of retrieving the source paths. See log for more details.";

    private static final NodeLogger LOGGER = NodeLogger.getLogger(TransferFilesStatusMessageReporter.class);

    private final SettingsModelWriterFileChooser m_settingsWriter;

    private final SettingsModelReaderFileChooser m_settingsReader;

    private final ExistsHandler m_existsHandler;

    private final boolean m_includeParentFolder;

    /**
     * Constructor.
     *
     * @param settings the writer file chooser settings
     */
    TransferFilesStatusMessageReporter(final SettingsModelWriterFileChooser settingsWriter,
        final SettingsModelReaderFileChooser settingsReader, final boolean includeParentFolder) {
        m_settingsWriter = settingsWriter;
        m_settingsReader = settingsReader;
        m_includeParentFolder = includeParentFolder;
        m_existsHandler = createExistsHandler();
    }

    @Override
    public StatusMessage report() throws IOException, InvalidSettingsException {

        try (final WritePathAccessor writePathAccessor = m_settingsWriter.createWritePathAccessor();
                final ReadPathAccessor readPathAccessor = m_settingsReader.createReadPathAccessor()) {

            final FSPath destinationPath = writePathAccessor.getOutputPath(StatusMessageUtils.NO_OP_CONSUMER);

            final List<FSPath> sourcePaths;
            final Path rootSourcePath;

            try {
                rootSourcePath = readPathAccessor.getRootPath(StatusMessageUtils.NO_OP_CONSUMER);

                //Additional check, as an empty path is still a regular path
                if (rootSourcePath.toString().length() == 0) {
                    return INVALID_SOURCE;
                }

                sourcePaths = getSourcePaths(readPathAccessor);
            } catch (InvalidSettingsException e) {
                LOGGER.error(SRC_PATH_ERROR_MSG, e);
                return INVALID_SOURCE;
            } catch (AccessDeniedException e) {
                LOGGER.error(SRC_PATH_ERROR_MSG, e);
                return DefaultStatusMessage.mkError("Source path could not be accessed.");
            }

            if (FSFiles.exists(destinationPath)) {
                final PathRelativizer pathRelativizer = new PathRelativizerNonTableInput(rootSourcePath,
                    m_includeParentFolder, m_settingsReader.getFilterModeModel().getFilterMode(), false);
                final int numberOfExistingFiles =
                    getNumberOfExistingFiles(sourcePaths.iterator(), destinationPath, pathRelativizer);

                return handlePathExists(destinationPath, numberOfExistingFiles);
            } else {
                return StatusMessageUtils.handleNewPath(destinationPath, m_settingsWriter.isCreateMissingFolders());
            }
        }
    }

    /**
     * Returns the number of files from the source which already exists in the destination folder.
     *
     * @param pathIterator the {@link Path} iterator of the source paths
     * @param destinationDir the destination folder
     * @param pathRelativizer the {@link PathRelativizer} to create the output paths
     * @return the number of existing files in the destination folder
     * @throws AccessDeniedException
     */
    private static int getNumberOfExistingFiles(final Iterator<FSPath> pathIterator, final FSPath destinationDir,
        final PathRelativizer pathRelativizer) throws AccessDeniedException {
        int numberOfExistingFiles = 0;

        while (pathIterator.hasNext()) {
            final Path sourceFilePath = pathIterator.next();
            final Path destinationFilePath = destinationDir.resolve(pathRelativizer.apply(sourceFilePath));

            if (FSFiles.exists(destinationFilePath)) {
                numberOfExistingFiles++;
            }
        }
        return numberOfExistingFiles;

    }

    /**
     * Returns the source paths from the {@link DialogComponentReaderFileChooser} based on the the configured
     * {@link FilterMode}.
     *
     * @param readPathAccessor the {@link ReadPathAccessor}
     * @return the source paths of the {@link DialogComponentReaderFileChooser}
     * @throws IOException
     * @throws InvalidSettingsException
     */
    private List<FSPath> getSourcePaths(final ReadPathAccessor readPathAccessor)
        throws IOException, InvalidSettingsException {
        if (m_settingsReader.getFilterModeModel().getFilterMode() == FilterMode.FOLDER) {
            return FSFiles.getFilePathsFromFolder(readPathAccessor.getRootPath(StatusMessageUtils.NO_OP_CONSUMER));
        } else {
            return readPathAccessor.getFSPaths(StatusMessageUtils.NO_OP_CONSUMER);
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
    private StatusMessage handlePathExists(final FSPath path, final int numberOfExistingPaths)
        throws AccessDeniedException {
        if (!Files.isWritable(path)) {
            throw ExceptionUtil.createAccessDeniedException(path);
        }
        try {
            return m_existsHandler.create(path, Files.readAttributes(path, BasicFileAttributes.class),
                numberOfExistingPaths);
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
    private ExistsHandler createExistsHandler() {
        switch (m_settingsWriter.getFileOverwritePolicy()) {
            case FAIL:
                return this::createFailStatusMsg;
            case IGNORE:
                return this::createIgnoreMessage;
            case OVERWRITE:
                return this::createOverwriteStatusMsg;
            default:
                throw new IllegalStateException(
                    "Unknown overwrite policy: " + m_settingsWriter.getFileOverwritePolicy());
        }
    }

    private interface ExistsHandler {
        StatusMessage create(final Path path, final BasicFileAttributes attrs, final int numberOfExistingPaths);
    }

    private StatusMessage createFailStatusMsg(final Path path, final BasicFileAttributes attrs,
        final int numberOfExistingPaths) {
        return createMessages(path, attrs, numberOfExistingPaths, FileOverwritePolicy.FAIL);
    }

    private StatusMessage createOverwriteStatusMsg(final Path path, final BasicFileAttributes attrs,
        final int numberOfExistingPaths) {
        return createMessages(path, attrs, numberOfExistingPaths, FileOverwritePolicy.OVERWRITE);
    }

    private StatusMessage createIgnoreMessage(final Path path, final BasicFileAttributes attrs,
        final int numberOfExistingPaths) {
        return createMessages(path, attrs, numberOfExistingPaths, FileOverwritePolicy.IGNORE);
    }

    /**
     * Creates the status messages based on a {@link Path}, {@link BasicFileAttributes} and the
     * {@link FileOverwritePolicy}.
     *
     * @param path the destination {@link Path}
     * @param attrs the {@link BasicFileAttributes} of the destination {@link Path}
     * @param numberOfExistingPaths the number of already existing paths in the destination folder
     * @param fileOverwritePolicy the {@link FileOverwritePolicy}
     * @return the {@link StatusMessage}
     */
    private StatusMessage createMessages(final Path path, final BasicFileAttributes attrs,
        final int numberOfExistingPaths, final FileOverwritePolicy fileOverwritePolicy) {

        final StatusMessage statusMessage;

        if (attrs.isDirectory()) {
            if (numberOfExistingPaths == 0) {
                statusMessage = StatusMessageUtils.SUCCESS_MSG;
            } else {
                statusMessage = createFileExistingMessages(fileOverwritePolicy, numberOfExistingPaths);
            }
        } else {
            statusMessage = DefaultStatusMessage.mkError("%s is not a folder.", path);
        }
        return statusMessage;
    }

    /**
     * Creates the messages in case there are already existing paths in the destination folder.
     *
     * @param fileOverWritePolicy the {@link FileOverwritePolicy}
     * @param numberOfExistingPaths the number of already existing paths in the destination folder
     * @return the {@link StatusMessage}
     */
    private StatusMessage createFileExistingMessages(final FileOverwritePolicy fileOverWritePolicy,
        final int numberOfExistingPaths) {
        switch (fileOverWritePolicy) {
            case IGNORE:
                return DefaultStatusMessage.mkInfo("%s %s and will not be overwritten.", numberOfExistingPaths,
                    getStartPhrase(numberOfExistingPaths));
            case FAIL:
                return DefaultStatusMessage.mkError("%s %s, the node will fail.", numberOfExistingPaths,
                    getStartPhrase(numberOfExistingPaths));
            case OVERWRITE:
                return DefaultStatusMessage.mkWarning("%s %s and will be overwritten.", numberOfExistingPaths,
                    getStartPhrase(numberOfExistingPaths));
            default:
                throw new IllegalStateException(
                    "Unknown overwrite policy: " + m_settingsWriter.getFileOverwritePolicy());
        }
    }

    private static String getStartPhrase(final int numberOfExistingPaths) {
        return "file" + (numberOfExistingPaths != 1 ? "s" : "") + " already exists";
    }
}
