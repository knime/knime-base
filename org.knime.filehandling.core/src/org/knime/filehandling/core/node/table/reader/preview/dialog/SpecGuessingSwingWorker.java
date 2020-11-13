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
 *   Aug 6, 2020 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.node.table.reader.preview.dialog;

import java.awt.event.ActionListener;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

import javax.swing.ButtonModel;

import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeProgressMonitor;
import org.knime.core.node.util.SharedIcons;
import org.knime.core.node.workflow.NodeProgressEvent;
import org.knime.core.util.SwingWorkerWithContext;
import org.knime.filehandling.core.defaultnodesettings.ExceptionUtil;
import org.knime.filehandling.core.node.table.reader.GenericMultiTableReadFactory;
import org.knime.filehandling.core.node.table.reader.config.GenericImmutableMultiTableReadConfig;
import org.knime.filehandling.core.node.table.reader.config.GenericMultiTableReadConfig;
import org.knime.filehandling.core.node.table.reader.config.ReaderSpecificConfig;
import org.knime.filehandling.core.node.table.reader.config.TableReadConfig;
import org.knime.filehandling.core.node.table.reader.preview.PreviewExecutionMonitor;
import org.knime.filehandling.core.node.table.reader.util.GenericStagedMultiTableRead;
import org.knime.filehandling.core.node.table.reader.util.StagedMultiTableRead;

/**
 * A {@link SwingWorkerWithContext} that creates a {@link StagedMultiTableRead} in a background thread, reports to an
 * AnalysisComponent and feeds the {@link StagedMultiTableRead} to a {@link Consumer} once it is ready.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
final class SpecGuessingSwingWorker<I, C extends ReaderSpecificConfig<C>, T>
    extends SwingWorkerWithContext<GenericStagedMultiTableRead<I, T>, AnalysisUpdate> {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(SpecGuessingSwingWorker.class);

    private final String m_rootPath;

    private final List<I> m_paths;

    private final GenericMultiTableReadFactory<I, C, T> m_reader;

    private final GenericMultiTableReadConfig<I, C> m_config;

    private final AnalysisComponentModel m_analysisComponent;

    private final PreviewExecutionMonitor m_exec = new PreviewExecutionMonitor();

    private final Consumer<GenericStagedMultiTableRead<I, T>> m_resultConsumer;

    SpecGuessingSwingWorker(final GenericMultiTableReadFactory<I, C, T> reader,
        final String rootPath, final List<I> paths, final GenericImmutableMultiTableReadConfig<I, C> config,
        final AnalysisComponentModel analysisComponent, final Consumer<GenericStagedMultiTableRead<I, T>> resultConsumer) {
        m_rootPath = rootPath;
        m_reader = reader;
        m_config = config;
        m_paths = paths;
        m_analysisComponent = analysisComponent;
        m_resultConsumer = resultConsumer;
        m_exec.setNumItemsToRead(paths.size());
    }

    @Override
    protected GenericStagedMultiTableRead<I, T> doInBackgroundWithContext() throws Exception {
        // Since we do the spec guessing in a background thread, we need to use the publish method
        // to forward any updates to the process method which is called in the UI thread
        final NodeProgressMonitor progressMonitor = m_exec.getProgressMonitor();
        progressMonitor.addProgressListener(e -> publish(createAnalysisUpdate(e)));
        final ButtonModel quickScanModel = m_analysisComponent.getQuickScanModel();
        final ActionListener listener = e -> {
            quickScanModel.setEnabled(false);
            progressMonitor.setExecuteCanceled();
        };
        quickScanModel.addActionListener(listener);
        final GenericStagedMultiTableRead<I, T> read = m_reader.create(m_rootPath, m_paths, m_config, m_exec);
        quickScanModel.removeActionListener(listener);
        return read;
    }

    private AnalysisUpdate createAnalysisUpdate(final NodeProgressEvent progressEvent) {
        final Optional<Path> currentPath = m_exec.getCurrenttem();
        final StringBuilder sb = new StringBuilder("Reading input data ")//
            .append(m_exec.getCurrentlyReadingItemIdx())//
            .append(" of ")//
            .append(m_exec.getNumItemsToRead());
        if (currentPath.isPresent()) {
            sb.append(": ")//
                .append(currentPath.get().toString());
        }
        final String progressPathLabel = sb.toString();
        Double doubleProgress = progressEvent.getNodeProgress().getProgress();
        final int progress = doubleProgress == null ? 0 : (int)Math.round(100 * doubleProgress.doubleValue());
        final String progressLabel = createAnalysisProgressText(progressEvent.getNodeProgress().getMessage());
        return new AnalysisUpdate(progressPathLabel, progressLabel, m_exec.isSizeAssessable(), progress);
    }

    private static String createAnalysisProgressText(final String msg) {
        return "Detecting column types... " + (msg == null ? "" : ("(" + msg + ")"));
    }

    @Override
    protected void processWithContext(final List<AnalysisUpdate> chunks) {
        if (!isDone()) {
            m_analysisComponent.update(chunks.get(chunks.size() - 1));
        }
    }

    @Override
    protected void doneWithContext() {
        m_analysisComponent.setProgressPathLabel("");
        if (isCancelled()) {
            m_analysisComponent.resetErrorLabel();
        } else {
            reportAnalysisStatus();
            feedResultToConsumer();
        }
        m_analysisComponent.setVisible(false);

    }

    private void feedResultToConsumer() {
        try {
            m_resultConsumer.accept(get());
        } catch (InterruptedException ex) {// NOSONAR
            // get() should return immediately if called from doneWithContext() therefore we should never be interrupted
            LOGGER.error("Unexpected InterruptedException, get() is expect to return immediately in doneWithContext().",
                ex);
        } catch (CancellationException ex) {
            LOGGER.error("Unexpected CancellationException, we explicitly checked isCancelled() before calling get().",
                ex);
        } catch (ExecutionException ex) {
            LOGGER.debug("An exception occurred during spec analysis.", ex);
            displayExecutionException(ex);
        }
    }

    private void displayExecutionException(final ExecutionException ex) {
        final Optional<Throwable> firstThrowable = ExceptionUtil.getFirstThrowable(ex,
            t -> !(t instanceof ExecutionException) && t.getMessage() != null && !t.getMessage().isEmpty());
        m_analysisComponent.setError(firstThrowable.map(Throwable::getMessage).orElse("An error occurred."));
    }

    private void reportAnalysisStatus() {
        if (m_exec.isSpecGuessingErrorOccurred()) {
            m_analysisComponent.setError(m_exec.getSpecGuessingErrorRow(), m_exec.getSpecGuessingErrorMsg());
        }
        try {
            m_exec.checkCanceled();
            setAnalysisStatus();
        } catch (CanceledExecutionException ex) {
            LOGGER.info("Spec analysis has been cancelled.", ex);
            m_analysisComponent.setProgressLabel(SharedIcons.WARNING_YELLOW.get(),
                "The suggested column types are based on a partial input data analysis only!");
        }
    }

    private void setAnalysisStatus() {
        if (!m_exec.isSpecGuessingErrorOccurred()) {
            TableReadConfig<C> tableReadConfig = m_config.getTableReadConfig();
            if (tableReadConfig.limitRowsForSpec()) {
                m_analysisComponent.setProgressLabel(SharedIcons.INFO.get(),
                    "The suggested column types are based on the first " + tableReadConfig.getMaxRowsForSpec()
                        + " rows only. See 'Advanced Settings' tab.");
            } else {
                m_analysisComponent.setProgressLabel(SharedIcons.SUCCESS.get(),
                    "Data analysis successfully completed.");
            }
        } else {
            m_analysisComponent.setProgressLabel(null, " ");
        }
    }

}
