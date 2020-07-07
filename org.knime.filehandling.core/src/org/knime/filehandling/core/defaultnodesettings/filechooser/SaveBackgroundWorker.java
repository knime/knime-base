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
 *   May 29, 2020 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.defaultnodesettings.filechooser;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.concurrent.Callable;
import java.util.function.Predicate;

import org.knime.core.node.NodeLogger;
import org.knime.core.node.util.FileSystemBrowser.FileSelectionMode;
import org.knime.filehandling.core.connections.FSFiles;
import org.knime.filehandling.core.connections.FSPath;
import org.knime.filehandling.core.defaultnodesettings.ExceptionUtil;
import org.knime.filehandling.core.defaultnodesettings.filechooser.writer.SettingsModelWriterFileChooser;
import org.knime.filehandling.core.defaultnodesettings.filechooser.writer.WritePathAccessor;
import org.knime.filehandling.core.defaultnodesettings.status.DefaultStatusMessage;
import org.knime.filehandling.core.defaultnodesettings.status.PriorityStatusConsumer;
import org.knime.filehandling.core.defaultnodesettings.status.StatusMessage;
import org.knime.filehandling.core.defaultnodesettings.status.StatusMessage.MessageType;

/**
 * Computes the status message for dialogs of type "save".
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
final class SaveBackgroundWorker implements Callable<StatusMessage> {

    private static final DefaultStatusMessage MISSING_FOLDERS_MSG =
        new DefaultStatusMessage(MessageType.ERROR, "Some folders in the specified path are missing.");

    private static final String FOLDER = "folder";

    private static final DefaultStatusMessage SUCCESS_MSG = new DefaultStatusMessage(MessageType.INFO, "");

    private static final NodeLogger LOGGER = NodeLogger.getLogger(SaveBackgroundWorker.class);

    private final SettingsModelWriterFileChooser m_settings;

    private final ExistsHandler m_existsHandler;

    SaveBackgroundWorker(final SettingsModelWriterFileChooser settings) {
        m_settings = settings;
        m_existsHandler = createExistsHandler();
    }

    @Override
    public StatusMessage call() throws Exception {
        try (final WritePathAccessor accessor = m_settings.createPathAccessor()) {
            final PriorityStatusConsumer consumer = new PriorityStatusConsumer();
            final FSPath path = accessor.getOutputPath(consumer);
            if (FSFiles.exists(path)) {
                return handlePathExists(path);
            } else {
                return handleNewPath(path);
            }
        }
    }

    private StatusMessage handleNewPath(final FSPath path) throws AccessDeniedException {
        if (m_settings.isCreateMissingFolders()) {
            return SUCCESS_MSG;
        } else {
            return checkIfParentFoldersAreMissing(path);
        }
    }

    private StatusMessage handlePathExists(final FSPath path) throws AccessDeniedException {
        if (!Files.isWritable(path)) {
            throw ExceptionUtil.createAccessDeniedException(path);
        }
        try {
            return m_existsHandler.create(path, Files.readAttributes(path, BasicFileAttributes.class));
        } catch (IOException ex) {
            LOGGER.error("Can't access attributes of existing path", ex);
            return new DefaultStatusMessage(MessageType.ERROR, "Can't access attributes of %s.", path);
        }
    }

    private static StatusMessage checkIfParentFoldersAreMissing(final FSPath path) throws AccessDeniedException {
        final Path parent = path.getParent();
        if (parent == null) {
            return SUCCESS_MSG;
        }
        if (FSFiles.exists(parent)) {
            if (!Files.isWritable(parent)) {
                throw ExceptionUtil.createAccessDeniedException(parent);
            }
            return SUCCESS_MSG;
        } else {
            // TODO recursively go through ancestors until an existing one is found and check if we can write into it
            return MISSING_FOLDERS_MSG;
        }
    }

    private ExistsHandler createExistsHandler() {
        switch (m_settings.getFileOverwritePolicy()) {
            case APPEND:
                return createAppendHandler();
            case FAIL:
                return SaveBackgroundWorker::createFailStatusMsg;
            case IGNORE:
                return (p, a) -> SUCCESS_MSG;
            case OVERWRITE:
                return this::createOverwriteStatusMsg;
            default:
                throw new IllegalStateException("Unknown overwrite policy: " + m_settings.getFileOverwritePolicy());
        }
    }

    private AppendHandler createAppendHandler() {
        final FileSelectionMode fileSelectionMode = getFileSelectionMode();
        switch (fileSelectionMode) {
            case DIRECTORIES_ONLY:
                return new AppendHandler(FOLDER, BasicFileAttributes::isDirectory);
            case FILES_AND_DIRECTORIES:
                // yes, it's expected that we accept everything
                return new AppendHandler("file or folder", a -> true);
            case FILES_ONLY:
                return new AppendHandler("file", BasicFileAttributes::isRegularFile);
            default:
                throw new IllegalStateException("Unknown file selection mode: " + fileSelectionMode);
        }
    }

    private FileSelectionMode getFileSelectionMode() {
        return m_settings.getFilterModeModel().getFilterMode().getFileSelectionMode();
    }

    private interface ExistsHandler {
        StatusMessage create(final Path path, final BasicFileAttributes attrs);
    }

    private static class AppendHandler implements ExistsHandler {

        private final String m_expectedEntity;

        private final Predicate<BasicFileAttributes> m_successPredicate;

        AppendHandler(final String expectedEntity, final Predicate<BasicFileAttributes> predicate) {
            m_expectedEntity = expectedEntity;
            m_successPredicate = predicate;
        }

        @Override
        public StatusMessage create(final Path path, final BasicFileAttributes attrs) {
            if (m_successPredicate.test(attrs)) {
                return SUCCESS_MSG;
            } else {
                return mkError("The specified path '%s' does not point to a %s", path, m_expectedEntity);
            }
        }

    }

    private static StatusMessage createFailStatusMsg(final Path path, final BasicFileAttributes attrs) {
        return mkError("There already exists a %s with the specified path '%s'.", attrs.isDirectory() ? FOLDER : "file",
            path);
    }

    private StatusMessage createOverwriteStatusMsg(final Path path, final BasicFileAttributes attrs) {
        switch (getFileSelectionMode()) {
            case DIRECTORIES_ONLY:
                return attrs.isDirectory() ? mkOverwriteWarning(path, attrs) : createFailStatusMsg(path, attrs);
            case FILES_AND_DIRECTORIES:
                return mkOverwriteWarning(path, attrs);
            case FILES_ONLY:
                return attrs.isRegularFile() ? mkOverwriteWarning(path, attrs) : createFailStatusMsg(path, attrs);
            default:
                throw new IllegalStateException("Unknown file selection mode: " + getFileSelectionMode());
        }
    }

    private static StatusMessage mkOverwriteWarning(final Path path, final BasicFileAttributes attrs) {
        return mkWarning("There exists a %s with the specified path '%s' that will be overwritten.",
            attrs.isDirectory() ? FOLDER : "file", path);
    }

    private static StatusMessage mkError(final String format, final Object... args) {
        return new DefaultStatusMessage(MessageType.ERROR, format, args);
    }

    private static StatusMessage mkWarning(final String format, final Object... args) {
        return new DefaultStatusMessage(MessageType.WARNING, format, args);
    }

}
