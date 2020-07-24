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

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

import javax.swing.SwingWorker;

import org.knime.core.node.NodeLogger;
import org.knime.core.node.util.FileSystemBrowser.DialogType;
import org.knime.core.util.SwingWorkerWithContext;
import org.knime.filehandling.core.defaultnodesettings.filechooser.reader.SettingsModelReaderFileChooser;
import org.knime.filehandling.core.defaultnodesettings.filechooser.writer.SettingsModelWriterFileChooser;
import org.knime.filehandling.core.defaultnodesettings.status.DefaultStatusMessage;
import org.knime.filehandling.core.defaultnodesettings.status.StatusMessage;
import org.knime.filehandling.core.defaultnodesettings.status.StatusMessage.MessageType;

/**
 * Validates the user provided path in the background and also performs filtering given the filter options. Any errors
 * or warnings are reported via the {@link Consumer statusMessageConsumer} provided in the constructor.</br>
 * <b>NOTE:</b> SwingWorkers are NOT reusable i.e. the {@link SwingWorker#execute()} method be called only once in the
 * lifetime of an instance.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
final class StatusSwingWorker extends SwingWorkerWithContext<StatusMessage, StatusMessage> {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(StatusSwingWorker.class);

    private static final DefaultStatusMessage SCANNING_MSG = new DefaultStatusMessage(MessageType.INFO, "Scanning...");

    private final Consumer<StatusMessage> m_statusMessageConsumer;

    private final Callable<StatusMessage> m_backgroundWorker;

    StatusSwingWorker(final AbstractSettingsModelFileChooser settings,
        final Consumer<StatusMessage> statusMessageConsumer, final DialogType dialogType) {
        m_statusMessageConsumer = statusMessageConsumer;
        m_backgroundWorker = dialogType == DialogType.OPEN_DIALOG
            ? new OpenBackgroundWorker(((SettingsModelReaderFileChooser)settings).createClone())
            : new SaveBackgroundWorker(((SettingsModelWriterFileChooser)settings).createClone());
    }

    @Override
    protected StatusMessage doInBackgroundWithContext() throws Exception {

        publish(SCANNING_MSG);

        Thread.sleep(200);
        try {
            return m_backgroundWorker.call();
        } catch (Exception ex) {
            LOGGER.error("Error while updating status message: " + ex.getMessage(), ex);
            return new DefaultStatusMessage(MessageType.ERROR, ex.getMessage());
        }
    }

    @Override
    protected void processWithContext(final List<StatusMessage> chunks) {
        chunks.forEach(m_statusMessageConsumer::accept);
    }

    @Override
    protected void doneWithContext() {
        try {
            m_statusMessageConsumer.accept(get());
        } catch (final ExecutionException e) {
            if (!(e.getCause() instanceof InterruptedException)) {
                LOGGER.debug(e.getMessage(), e);
            }
        } catch (InterruptedException | CancellationException ex) {
            // ignore
        }
    }

}
