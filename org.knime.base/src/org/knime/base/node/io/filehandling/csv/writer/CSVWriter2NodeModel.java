/*
 * ------------------------------------------------------------------------
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
 * -------------------------------------------------------------------
 *
 * History
 *   Apr 26, 2020 (Temesgen H. Dadi, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.base.node.io.filehandling.csv.writer;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.zip.GZIPOutputStream;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.IntValue;
import org.knime.core.data.StringValue;
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
import org.knime.core.node.streamable.DataTableRowInput;
import org.knime.core.node.streamable.InputPortRole;
import org.knime.core.node.streamable.PartitionInfo;
import org.knime.core.node.streamable.PortInput;
import org.knime.core.node.streamable.PortOutput;
import org.knime.core.node.streamable.RowInput;
import org.knime.core.node.streamable.StreamableOperator;
import org.knime.filehandling.core.connections.FSFiles;
import org.knime.filehandling.core.connections.FSPath;
import org.knime.filehandling.core.defaultnodesettings.filechooser.writer.WritePathAccessor;
import org.knime.filehandling.core.defaultnodesettings.status.NodeModelStatusConsumer;
import org.knime.filehandling.core.defaultnodesettings.status.StatusMessage.MessageType;

/**
 * NodeModel to write a DataTable to a CSV file.
 *
 * @author Temesgen H. Dadi, KNIME GmbH, Berlin, Germany
 */
final class CSVWriter2NodeModel extends NodeModel {
    /** The node logger for this class. */
    private static final NodeLogger LOGGER = NodeLogger.getLogger(CSVWriter2NodeModel.class);

    private final CSVWriter2Config m_writerConfig;

    private final int m_dataInputPortIdx;

    private final NodeModelStatusConsumer m_statusConsumer;

    CSVWriter2NodeModel(final PortsConfiguration portsConfig) {
        super(portsConfig.getInputPorts(), portsConfig.getOutputPorts());
        m_writerConfig = new CSVWriter2Config(portsConfig);
        // save since this port is fixed, see the Factory class
        m_dataInputPortIdx =
            portsConfig.getInputPortLocation().get(CSVWriter2NodeFactory.DATA_TABLE_INPUT_PORT_GRP_NAME)[0];
        m_statusConsumer = new NodeModelStatusConsumer(EnumSet.of(MessageType.ERROR, MessageType.WARNING));
    }

    @Override
    protected DataTableSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        m_writerConfig.getFileChooserModel().configureInModel(inSpecs, m_statusConsumer);
        m_statusConsumer.setWarningsIfRequired(this::setWarningMessage);

        final DataTableSpec inSpec = (DataTableSpec)inSpecs[m_dataInputPortIdx];

        for (int i = 0; i < inSpec.getNumColumns(); i++) {
            final DataType c = inSpec.getColumnSpec(i).getType();
            if (!c.isCompatible(StringValue.class) && !c.isCompatible(DoubleValue.class)
                && !c.isCompatible(IntValue.class)) {
                throw new InvalidSettingsException("Input table must only contain String, Int, or Doubles");
            }
        }
        if (inSpec.containsCompatibleType(DoubleValue.class) && m_writerConfig.colSeparatorContainsDecSeparator()) {
            throw new InvalidSettingsException(
                "The column delimiter cannot contain (or be equal to) the  decimal separator!");
        }
        return new DataTableSpec[0];
    }

    @Override
    protected BufferedDataTable[] execute(final PortObject[] data, final ExecutionContext exec) throws Exception {
        try (final WritePathAccessor accessor = m_writerConfig.getFileChooserModel().createWritePathAccessor()) {
            final FSPath outputPath = accessor.getOutputPath(m_statusConsumer);
            m_statusConsumer.setWarningsIfRequired(this::setWarningMessage);
            createParentDirIfRequired(outputPath);
            final BufferedDataTable tbl = (BufferedDataTable)data[m_dataInputPortIdx];
            final DataTableRowInput rowInput = new DataTableRowInput(tbl);
            return writeToFile(rowInput, exec, outputPath);
        }
    }

    /**
     * Performs the actual writing to file.
     *
     * @param input a row input
     * @param exec the execution context
     * @param outputPath the path of the output file
     * @return an empty array of BufferedDataTable
     * @throws IOException
     * @throws InterruptedException
     * @throws CanceledExecutionException
     * @throws Exception If the writing to file fails.
     */
    private BufferedDataTable[] writeToFile(final RowInput input, final ExecutionContext exec, final Path outputPath)
        throws IOException, InterruptedException, CanceledExecutionException {
        final boolean isNewFile = !Files.exists(outputPath);

        try (final OutputStream outStream = createOutputStream(outputPath);
                final OutputStreamWriter writer = new OutputStreamWriter(outStream, m_writerConfig.getCharSet());
                CSVWriter2 tableWriter = new CSVWriter2(writer, m_writerConfig);) {

            final boolean realyAppending = m_writerConfig.isFileAppended() && !isNewFile;
            final boolean realySkipColumnHeader = m_writerConfig.skipColumnHeaderOnAppend() && !isNewFile;

            final List<String> commentLines =
                m_writerConfig.getCommentConfig().getCommentHeader(input.getDataTableSpec().getName(), realyAppending);
            tableWriter.writeLines(commentLines);

            if (m_writerConfig.writeColumnHeader() && !realySkipColumnHeader) {
                tableWriter.writeColumnHeader(input.getDataTableSpec());
            }

            tableWriter.writeRows(input, exec);
            if (tableWriter.hasWarningMessage()) {
                setWarningMessage(tableWriter.getLastWarningMessage());
            }
            return new BufferedDataTable[0];
        } catch (final CanceledExecutionException e) {
            if (isNewFile) {
                deleteIncompleteFile(outputPath);
            } else {
                LOGGER.warn(
                    "Node exection was canceled. The file '" + outputPath + "' could have partial modifications.");
            }
            throw e;
        }
    }

    private static void deleteIncompleteFile(final Path outputPath) {
        try {
            Files.delete(outputPath);
            LOGGER.debug("File created '" + outputPath + "' deleted after node execution was canceled.");

        } catch (final IOException ex) {
            LOGGER.warn("Unable to delete created file '" + outputPath + "' after node execution was canceled. "
                + ex.getMessage(), ex);
        }
    }

    private OutputStream createOutputStream(final Path outputPath) throws IOException {
        OutputStream outStream;
        try {
            outStream = FSFiles.newOutputStream(outputPath,
                m_writerConfig.getFileChooserModel().getFileOverwritePolicy().getOpenOptions());
        } catch (final FileAlreadyExistsException e) {
            throw new IOException(
                "Output file '" + e.getFile() + "' exists and must not be overwritten due to user settings.", e);
        }
        if (m_writerConfig.getAdvancedConfig().compressWithGzip()) {
            outStream = new GZIPOutputStream(outStream);
        }
        outStream = new BufferedOutputStream(outStream);
        return outStream;
    }

    private void createParentDirIfRequired(final Path outputPath) throws IOException {
        // create parent directories according to the state of m_createDirectoryConfig.
        final Path parentPath = outputPath.getParent();
        if (parentPath != null && !Files.exists(parentPath)) {
            if (m_writerConfig.getFileChooserModel().isCreateMissingFolders()) {
                FSFiles.createDirectories(parentPath);
            } else {
                throw new IOException(String.format(
                    "The directory '%s' does not exist and must not be created due to user settings.", parentPath));
            }
        }
    }

    @Override
    public InputPortRole[] getInputPortRoles() {
        final InputPortRole[] inputPortRoles = new InputPortRole[getNrInPorts()];
        // Set all ports except the data table input port to NONSTREAMABLE.
        Arrays.fill(inputPortRoles, InputPortRole.NONDISTRIBUTED_NONSTREAMABLE);
        inputPortRoles[m_dataInputPortIdx] = InputPortRole.NONDISTRIBUTED_STREAMABLE;
        return inputPortRoles;
    }

    @Override
    public StreamableOperator createStreamableOperator(final PartitionInfo partitionInfo,
        final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        return new StreamableOperator() {
            @Override
            public void runFinal(final PortInput[] inputs, final PortOutput[] outputs, final ExecutionContext exec)
                throws Exception {
                try (final WritePathAccessor accessor =
                    m_writerConfig.getFileChooserModel().createWritePathAccessor()) {
                    final FSPath outputPath = accessor.getOutputPath(m_statusConsumer);
                    m_statusConsumer.setWarningsIfRequired(s -> setWarningMessage(s));
                    createParentDirIfRequired(outputPath);
                    final RowInput input = (RowInput)inputs[m_dataInputPortIdx];
                    writeToFile(input, exec, outputPath);
                }
            }
        };
    }

    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_writerConfig.validateSettingsForModel(settings);
    }

    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_writerConfig.loadSettingsForModel(settings);
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_writerConfig.saveSettingsForModel(settings);
    }

    @Override
    protected void reset() {
        // Nothing to do here
    }

    @Override
    protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        // no internals to load
    }

    @Override
    protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        // nothing to save.
    }
}
