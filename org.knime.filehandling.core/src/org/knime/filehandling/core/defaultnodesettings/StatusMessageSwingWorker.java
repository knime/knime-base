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
 *   Sep 6, 2019 (bjoern): created
 */
package org.knime.filehandling.core.defaultnodesettings;

import static java.lang.String.format;

import java.awt.Color;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

import javax.swing.JLabel;
import javax.swing.SwingWorker;

import org.knime.core.node.FSConnectionFlowVariableProvider;
import org.knime.core.node.NodeLogger;
import org.knime.core.util.Pair;

/**
 * Swing worker used to update the status message of the file chooes dialog component.
 *
 * @author Julian Bunzel, KNIME GmbH, Berlin, Germany
 */
class StatusMessageSwingWorker extends SwingWorker<Pair<Color, String>, Pair<Color, String>> {

    private final static NodeLogger LOGGER = NodeLogger.getLogger(DialogComponentFileChooser2.class);

    /** String used as placeholder for the label when typing in the file selection component */
    private static final String SCANNING_MESSAGE = "Scanning...";

    /** String used for the label in case of an IOException while scanning */
    private static final String FOLDER_SCAN_EXCEPTION_FAILURE_MESSAGE = "Failed to scan folder: %s";

    /** String used for the label in case the selected file does not exist */
    private static final String FILE_ACCESS_FAILURE_MESSAGE = "Selected file does not exist or cannot be accessed: %s";

    /** String used for the label in case no files matched the selected filters */
    private static final String NO_FILES_MATCHED_FILTER_MESSAGE = "No files matched the filters";

    /** Formatted string used for the label in case the scanning was successful */
    private static final String SCANNED_FILES_MESSAGE = "Selected %d of %d files for reading (%d filtered)";

    private static final Color SUCCESS_GREEN = new Color(136, 170, 0);

    private final FSConnectionFlowVariableProvider m_connectionFlowVariableProvider;

    /** Settings model */
    private final SettingsModelFileChooser2 m_settingsModel;

    private final JLabel m_statusMessageLabel;

    /**
     * Creates a new instance of {@code StatusLineSwingWorker}.
     *
     * @param the label to update
     */
    StatusMessageSwingWorker(final FSConnectionFlowVariableProvider connectionFlowVariableProvider,
        final SettingsModelFileChooser2 settingsModel, final JLabel statusMessageLabel) {
        m_connectionFlowVariableProvider = connectionFlowVariableProvider;
        m_settingsModel = settingsModel;
        m_statusMessageLabel = statusMessageLabel;
    }

    @Override
    protected Pair<Color, String> doInBackground() throws Exception {
        if (m_settingsModel.getFileSystemChoice().equals(FileSystemChoice.getCustomFsUrlChoice())) {
            return mkSuccess("");
        }

        publish(new Pair<>(Color.BLACK, SCANNING_MESSAGE));

        // sleep for 200ms to allow for quick cancellation without getting stuck in IO
        Thread.sleep(200);

        // get file systems
        final FileChooserHelper helper;
        try {
            helper = new FileChooserHelper(m_connectionFlowVariableProvider, m_settingsModel);
        } catch (IOException e) {
            final String msg = "Could not get file system: " + ExceptionUtil.getDeepestErrorMessage(e, true);
            LOGGER.debug(msg, e);
            return mkError(msg);
        }

        // instantiate a path
        final Path fileOrFolder;
        try {
            fileOrFolder = helper.getFileSystem().getPath(m_settingsModel.getPathOrURL());
        } catch (InvalidPathException e) {
            return mkError(ExceptionUtil.getDeepestErrorMessage(e, false));
        }

        // fetch file attributes for path, so we can determine wheter it exists, is accessible
        // and whether it is file or folder
        final BasicFileAttributes basicAttributes;
        try {
            basicAttributes = Files.getFileAttributeView(fileOrFolder, BasicFileAttributeView.class).readAttributes();
        } catch (IOException e) {
            return mkError(format(FILE_ACCESS_FAILURE_MESSAGE, ExceptionUtil.getDeepestErrorMessage(e, false)));
        }

        // if folder, then scan. If file, then do nothing
        Pair<Color, String> toReturn = mkSuccess("");
        if (basicAttributes.isDirectory()) {
            toReturn = scanFolder(helper);
        }

        return toReturn;
    }

    private static Pair<Color, String> scanFolder(final FileChooserHelper helper) {
        Pair<Color, String> toReturn;

        try {
            helper.scanDirectoryTree();
            final Pair<Integer, Integer> matchPair = helper.getCounts();

            if (matchPair.getFirst() > 0) {
                final String msg = format(SCANNED_FILES_MESSAGE, matchPair.getFirst(),
                    matchPair.getFirst() + matchPair.getSecond(), matchPair.getSecond());
                toReturn = mkSuccess(msg);
            } else {
                toReturn = mkError(NO_FILES_MATCHED_FILTER_MESSAGE);
            }
        } catch (UncheckedIOException | IOException e) {
            final String msg =
                format(FOLDER_SCAN_EXCEPTION_FAILURE_MESSAGE, ExceptionUtil.getDeepestErrorMessage(e, true));
            LOGGER.debug(msg, e);
            toReturn = mkError(msg);
        }

        return toReturn;
    }

    private static Pair<Color, String> mkSuccess(final String msg) {
        return new Pair<>(SUCCESS_GREEN, msg);
    }

    private static Pair<Color, String> mkError(final String msg) {
        return new Pair<>(Color.RED, msg);
    }

    @Override
    protected void done() {
        try {
            updateStatusMessageLabel(get());
        } catch (ExecutionException e) {
            if (!(e.getCause() instanceof InterruptedException)) {
                LOGGER.debug(e.getMessage(), e);
            }
        } catch (InterruptedException | CancellationException ex) {
            // ignore
        }
    }

    @Override
    protected void process(final List<Pair<Color, String>> chunks) {
        final Pair<Color, String> colorAndMessage = chunks.get(chunks.size() - 1);
        updateStatusMessageLabel(colorAndMessage);
    }

    private void updateStatusMessageLabel(final Pair<Color, String> colorAndMessage) {
        m_statusMessageLabel.setForeground(colorAndMessage.getFirst());
        m_statusMessageLabel.setText(colorAndMessage.getSecond());
    }
}