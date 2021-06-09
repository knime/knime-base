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
 *   May 26, 2021 (Mark Ortmann): created
 */
package org.knime.base.node.io.filehandling.table.writer;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.util.EnumSet;

import org.knime.core.data.container.DataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.context.ports.PortsConfiguration;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.filehandling.core.connections.FSFiles;
import org.knime.filehandling.core.connections.FSPath;
import org.knime.filehandling.core.defaultnodesettings.filechooser.writer.FileOverwritePolicy;
import org.knime.filehandling.core.defaultnodesettings.filechooser.writer.SettingsModelWriterFileChooser;
import org.knime.filehandling.core.defaultnodesettings.filechooser.writer.WritePathAccessor;
import org.knime.filehandling.core.defaultnodesettings.status.NodeModelStatusConsumer;
import org.knime.filehandling.core.defaultnodesettings.status.StatusMessage.MessageType;

/**
 * The model of the table writer node.
 *
 * @author Mark Ortmann, KNIME GmbH, Berlin, Germany
 */
final class TableWriterNodeModel extends NodeModel {

    /** The node logger for this class. */
    private static final NodeLogger LOGGER = NodeLogger.getLogger(TableWriterNodeModel.class);

    private final TableWriterSettings m_settings;

    private final NodeModelStatusConsumer m_statusConsumer;

    private final int m_dataPortIdx;

    TableWriterNodeModel(final PortsConfiguration portsConfig, final String connectionInputPortGrpName) {
        super(portsConfig.getInputPorts(), portsConfig.getOutputPorts());
        m_settings = new TableWriterSettings(portsConfig, connectionInputPortGrpName);
        m_statusConsumer = new NodeModelStatusConsumer(EnumSet.of(MessageType.ERROR, MessageType.WARNING));
        m_dataPortIdx = portsConfig.getInputPortLocation().get(connectionInputPortGrpName) == null ? 0 : 1;
    }

    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        m_settings.getWriterModel().configureInModel(inSpecs, m_statusConsumer);
        m_statusConsumer.setWarningsIfRequired(this::setWarningMessage);
        return new PortObjectSpec[]{};
    }

    @Override
    protected PortObject[] execute(final PortObject[] inObjects, final ExecutionContext exec) throws Exception {
        final SettingsModelWriterFileChooser writerModel = m_settings.getWriterModel();
        try (final WritePathAccessor accessor = writerModel.createWritePathAccessor()) {
            final FSPath outpath = accessor.getOutputPath(m_statusConsumer);
            m_statusConsumer.setWarningsIfRequired(this::setWarningMessage);
            // create parent directories
            final FSPath parentPath = (FSPath)outpath.getParent();
            if (parentPath != null && !FSFiles.exists(parentPath)) {
                if (m_settings.getWriterModel().isCreateMissingFolders()) {
                    FSFiles.createDirectories(parentPath);
                } else {
                    throw new IOException(String.format(
                        "The directory '%s' does not exist and must not be created due to user settings.", parentPath));
                }
            }
            // since the remainder is rather costly we do this check here
            final FileOverwritePolicy fileOverwritePolicy = writerModel.getFileOverwritePolicy();
            if (fileOverwritePolicy == FileOverwritePolicy.FAIL && FSFiles.exists(outpath)) {
                throw new IOException("Output file '" + outpath.toString()
                    + "' exists and must not be overwritten due to user settings.");
            }
            try (final OutputStream oS = new DelayedOpenOutputStream(outpath, fileOverwritePolicy.getOpenOptions())) {
                DataContainer.writeToStream((BufferedDataTable)inObjects[m_dataPortIdx], oS, exec);
            } catch (final CanceledExecutionException e) {
                if (FSFiles.exists(outpath)) {
                    deleteFile(outpath);
                }
                throw e;
            }
        }
        return new PortObject[]{};
    }

    private static void deleteFile(final FSPath outpath) {
        try {
            Files.delete(outpath);
            LOGGER.debug("File '" + outpath.toString() + "' deleted after node has been canceled.");
        } catch (final IOException ex) {
            LOGGER.warn("Unable to delete file '" + outpath.toString() + "' after cancellation: " + ex.getMessage(),
                ex);
        }
    }

    @Override
    protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec) {
        // nothing to do
    }

    @Override
    protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec) {
        // nothing to do
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_settings.saveSettingsInModel(settings);
    }

    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_settings.validateSettings(settings);
    }

    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_settings.loadSettingsInModel(settings);
    }

    @Override
    protected void reset() {
        // nothing to do
    }

    /**
     * AP-8862: {@link DataContainer#writeToStream} first copies the whole table into a temporary file and then starts
     * writing it to the output stream. This time delay might cause a timeout, therefore opening the output stream must
     * be done once data have to be written.
     */
    private static class DelayedOpenOutputStream extends OutputStream {

        private final FSPath m_path;

        private final OpenOption[] m_options;

        private OutputStream m_delegate;

        DelayedOpenOutputStream(final FSPath path, final OpenOption... options) {
            m_path = path;
            m_options = options;
        }

        private synchronized OutputStream getOrOpenOutputStream() throws IOException {
            if (m_delegate == null) {
                m_delegate = Files.newOutputStream(m_path, m_options);
            }
            return m_delegate;
        }

        @SuppressWarnings("resource")
        @Override
        public void write(final int b) throws IOException {
            getOrOpenOutputStream().write(b);
        }

        @SuppressWarnings("resource")
        @Override
        public void write(final byte[] b) throws IOException {
            getOrOpenOutputStream().write(b);
        }

        @SuppressWarnings("resource")
        @Override
        public void write(final byte b[], final int off, final int len) throws IOException {
            getOrOpenOutputStream().write(b, off, len);
        }

        @SuppressWarnings("resource")
        @Override
        public void flush() throws IOException {
            getOrOpenOutputStream().flush();
        }

        @Override
        public void close() throws IOException {
            if (m_delegate != null) {
                m_delegate.close();
            }
        }
    }
}
