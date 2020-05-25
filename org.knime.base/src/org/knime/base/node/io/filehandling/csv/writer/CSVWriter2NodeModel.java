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
import java.nio.charset.Charset;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.zip.GZIPOutputStream;

import org.knime.base.node.io.csvwriter.CSVWriter;
import org.knime.base.node.io.csvwriter.FileWriterSettings;
import org.knime.base.node.io.filehandling.csv.writer.config.CSVWriter2Config;
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
import org.knime.filehandling.core.defaultnodesettings.FileChooserHelper;
import org.knime.filehandling.core.port.FileSystemPortObject;
import org.knime.filehandling.core.port.FileSystemPortObjectSpec;

/**
 * NodeModel to write a DataTable to a CSV file.
 *
 * @author Bernd Wiswedel, University of Konstanz
 * @author Temesgen H. Dadi, KNIME GmbH, Berlin, Germany
 */
final class CSVWriter2NodeModel extends NodeModel {

    /** The name of the optional connection input port group. */
    static final String CONNECTION_INPUT_PORT_GRP_NAME = "File System Connection";

    /** The name of the data table input port group. */
    static final String DATA_TABLE_INPUT_PORT_GRP_NAME = "Data Table";

    /** The node logger for this class. */
    private static final NodeLogger LOGGER = NodeLogger.getLogger(CSVWriter2NodeModel.class);

    private CSVWriter2Config m_writerConfig;

    private PortsConfiguration m_portsConfig;

    CSVWriter2NodeModel(final PortsConfiguration portsConfig) {
        super(portsConfig.getInputPorts(), portsConfig.getOutputPorts());
        m_writerConfig = new CSVWriter2Config();
        m_portsConfig = portsConfig;
    }

    @Override
    protected DataTableSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        String warnMsg = "";
        final String pathOrURL = m_writerConfig.getFileChooserModel().getPathOrURL();
        if (pathOrURL == null || pathOrURL.trim().isEmpty()) {
            throw new InvalidSettingsException("Please enter a valid location.");
        }
        if (m_writerConfig.isHardToReadBack()) {
            // we will write the table out - but it will be hard to read it in again.
            warnMsg +=
                "No separator and no quotes and no missing value pattern set. Written data will be hard to read!";
        }
        DataTableSpec inSpec = (DataTableSpec)inSpecs[getDataTablePortIndex()];
        for (int i = 0; i < inSpec.getNumColumns(); i++) {
            DataType c = inSpec.getColumnSpec(i).getType();
            if (!c.isCompatible(DoubleValue.class) && !c.isCompatible(IntValue.class)
                && !c.isCompatible(StringValue.class)) {
                throw new InvalidSettingsException("Input table must only contain String, Int, or Doubles");
            }
        }
        if (inSpec.containsCompatibleType(DoubleValue.class) && m_writerConfig.colSeparatorContainsDecSeparator()) {
            warnMsg +=
                "The data separator contains (or is equal to) the  decimal separator. Written data will be hard to read!";
        }

        if (!warnMsg.isEmpty()) {
            setWarningMessage(warnMsg.trim());
        }
        return new DataTableSpec[0];
    }

    @Override
    protected BufferedDataTable[] execute(final PortObject[] data, final ExecutionContext exec) throws Exception {

        final Path outputPath = getFilePath(data);
        createParentDirIfRequired(outputPath);
        // create BufferedDataTable implementing RowInput
        BufferedDataTable tbl = (BufferedDataTable)data[getDataTablePortIndex()];
        final DataTableRowInput rowInput = new DataTableRowInput(tbl);
        return writeToFile(rowInput, exec, outputPath);
    }

    /**
     * Performs the actual writing to file.
     *
     * @param input a row input
     * @param exec the execution context
     * @param outputPath the path of the output file
     * @return an empty array of BufferedDataTable
     * @throws Exception If the writing to file fails.
     */
    private BufferedDataTable[] writeToFile(final RowInput input, final ExecutionContext exec, final Path outputPath)
        throws Exception {
        final FileWriterSettings writerSettings = createModifiedFileWriterSettings(outputPath);
        final OutputStream outStream = createOutputStream(outputPath);
        final String charSet = m_writerConfig.getCharacterSet() != null ? m_writerConfig.getCharacterSet()
            : Charset.defaultCharset().name();

        CSVWriter tableWriter = new CSVWriter(new OutputStreamWriter(outStream, charSet), writerSettings);

        // write the comment header, if we are supposed to
        final String tableName = input.getDataTableSpec().getName();
        m_writerConfig.writeCommentHeader(tableWriter, tableName, m_writerConfig.isFileAppended());

        try {
            tableWriter.write(input, exec);
            tableWriter.close();
            if (tableWriter.hasWarningMessage()) {
                setWarningMessage(tableWriter.getLastWarningMessage());
            }
            // execution successful return an empty BufferedDataTable array
            return new BufferedDataTable[0];
        } catch (CanceledExecutionException e) {
            try {
                tableWriter.close();
            } catch (IOException ex) {
                // may happen if the stream is already closed by the interrupted thread
            }
            try {
                Files.delete(outputPath);
                LOGGER.debug("File '" + outputPath + "' deleted after node has been canceled.");
            } catch (IOException ex) {
                LOGGER.warn("Unable to delete file '" + outputPath + "' after cancellation: " + ex.getMessage(), ex);
            }
            throw e;
        } finally {
            outStream.close();
        }
    }

    /**
     * Creates a {@link FileWriterSettings} with modified option to write column header. Writing column headers will be
     * disabled if a) skipColumnHeaderOnAppend option is enabled, b) the file exists and we are appending to it.
     *
     * @return a FileWriterSettings with a modified option to write column header
     */
    private FileWriterSettings createModifiedFileWriterSettings(final Path outputPath) {
        FileWriterSettings writerSettings = m_writerConfig.createFileWriterSettings();

        final boolean doNotWriteColumnHeader = m_writerConfig.skipColumnHeaderOnAppend() // a) skipColumnHeaderOnAppend enabled
            && outputPath.toFile().exists() && m_writerConfig.isFileAppended(); // b) file exists and appending
        writerSettings.setWriteColumnHeader(m_writerConfig.writeColumnHeader() && !doNotWriteColumnHeader);

        return writerSettings;
    }

    private OutputStream createOutputStream(final Path outputPath) throws IOException {
        warnAboutExistingFile(outputPath);
        OutputStream outStream;
        try {
            outStream = Files.newOutputStream(outputPath, getFileOpenOption());
        } catch (FileAlreadyExistsException e) {
            throw new IOException(
                "Output file '" + e.getFile() + "' exists and must not be overwritten due to user settings.", e);
        }
        if (m_writerConfig.getAdvancedConfig().compressWithGzip()) {
            outStream = new GZIPOutputStream(outStream);
        }
        outStream = new BufferedOutputStream(outStream);
        return outStream;
    }

    /**
     * @return an array of {@link OpenOption} used for opening an {@link OutputStream}
     */
    private OpenOption[] getFileOpenOption() {
        if (m_writerConfig.isFileAppended()) {
            return new OpenOption[]{StandardOpenOption.CREATE, StandardOpenOption.APPEND};
        } else if (m_writerConfig.isFileOverwritten()) {
            return new OpenOption[]{StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING};
        } else {
            return new OpenOption[]{StandardOpenOption.CREATE_NEW};
        }
    }

    private void warnAboutExistingFile(final Path outputPath) {
        if (outputPath.toFile().exists()) {
            if (m_writerConfig.isFileAppended()) {
                setWarningMessage("Output file " + outputPath.toString() + " exists and will be appended!");
            } else if (m_writerConfig.isFileOverwritten()) {
                setWarningMessage("Output file " + outputPath.toString() + " exists and will be overwritten!");
            }
        }
    }

    private void createParentDirIfRequired(final Path outputPath) throws IOException {
        // create parent directories according to the state of m_createDirectoryConfig.
        final Path parentPath = outputPath.getParent();
        if (parentPath != null && !parentPath.toFile().exists()) {
            if (m_writerConfig.createParentDirIfRequired()) {
                Files.createDirectories(parentPath);
            } else {
                throw new IOException(String.format(
                    "The directory '%s' does not exist and must not be created due to user settings.", parentPath));
            }
        }
    }

    private Path getFilePath(final PortObjectSpec[] inSpecs) throws IOException, InvalidSettingsException {
        final FileChooserHelper fch =
            new FileChooserHelper(FileSystemPortObjectSpec.getFileSystemConnection(inSpecs, getFSConnectionPortIndex()),
                m_writerConfig.getFileChooserModel());

        return fch.getPathFromSettings();
    }

    private Path getFilePath(final PortObject[] inObjects) throws IOException, InvalidSettingsException {
        final FileChooserHelper fch =
            new FileChooserHelper(FileSystemPortObject.getFileSystemConnection(inObjects, getFSConnectionPortIndex()),
                m_writerConfig.getFileChooserModel());

        return fch.getPathFromSettings();
    }

    private int getDataTablePortIndex() {
        return m_portsConfig.getInputPortLocation().get(DATA_TABLE_INPUT_PORT_GRP_NAME)[0];
    }

    private int getFSConnectionPortIndex() {
        return getNrInPorts() < 2 ? 0 : m_portsConfig.getInputPortLocation().get(CONNECTION_INPUT_PORT_GRP_NAME)[0];
    }

    @Override
    public InputPortRole[] getInputPortRoles() {
        InputPortRole[] inputPortRoles = new InputPortRole[getNrInPorts()];
        // Set all ports except the data table input port to NONSTREAMABLE.
        Arrays.fill(inputPortRoles, InputPortRole.NONDISTRIBUTED_NONSTREAMABLE);
        inputPortRoles[getDataTablePortIndex()] = InputPortRole.NONDISTRIBUTED_STREAMABLE;
        return inputPortRoles;
    }

    @Override
    public StreamableOperator createStreamableOperator(final PartitionInfo partitionInfo,
        final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        final Path outputPath;
        try {
            outputPath = getFilePath(inSpecs);
            createParentDirIfRequired(outputPath);
        } catch (IOException ex) {
            throw new InvalidSettingsException(ex);
        }
        return new StreamableOperator() {
            @Override
            public void runFinal(final PortInput[] inputs, final PortOutput[] outputs, final ExecutionContext exec)
                throws Exception {
                RowInput input = (RowInput)inputs[getDataTablePortIndex()];
                writeToFile(input, exec, outputPath);
            }
        };
    }

    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_writerConfig.validateSettings(settings);
    }

    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_writerConfig.loadInModel(settings);
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_writerConfig.saveSettingsTo(settings);
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
