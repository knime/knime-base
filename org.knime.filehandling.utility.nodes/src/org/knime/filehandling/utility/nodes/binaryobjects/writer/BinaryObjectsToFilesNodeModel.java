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
 *   Oct 9, 2020 (ayazqureshi): created
 */
package org.knime.filehandling.utility.nodes.binaryobjects.writer;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.EnumSet;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValue;
import org.knime.core.data.StringValue;
import org.knime.core.data.blob.BinaryObjectDataCell;
import org.knime.core.data.blob.BinaryObjectDataValue;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.container.SingleCellFactory;
import org.knime.core.data.def.StringCell;
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
import org.knime.core.node.util.CheckUtils;
import org.knime.filehandling.core.connections.FSFiles;
import org.knime.filehandling.core.connections.FSLocationSpec;
import org.knime.filehandling.core.connections.FSPath;
import org.knime.filehandling.core.data.location.FSLocationValueMetaData;
import org.knime.filehandling.core.data.location.cell.FSLocationCellFactory;
import org.knime.filehandling.core.data.location.cell.MultiFSLocationCellFactory;
import org.knime.filehandling.core.defaultnodesettings.filechooser.writer.FileOverwritePolicy;
import org.knime.filehandling.core.defaultnodesettings.filechooser.writer.SettingsModelWriterFileChooser;
import org.knime.filehandling.core.defaultnodesettings.filechooser.writer.WritePathAccessor;
import org.knime.filehandling.core.defaultnodesettings.status.NodeModelStatusConsumer;
import org.knime.filehandling.core.defaultnodesettings.status.StatusMessage.MessageType;

/**
 * The node model allowing to convert binary objects to files.
 *
 * @author Ayaz Ali Qureshi, KNIME GmbH, Berlin, Germany
 */
final class BinaryObjectsToFilesNodeModel extends NodeModel {

    final BinaryObjectsToFilesNodeConfig m_binaryObjectsToFileNodeConfig;

    private final SettingsModelWriterFileChooser m_fileWriterSelectionModel;

    private int m_inputTableIdx;

    private final NodeModelStatusConsumer m_statusConsumer;

    private static final String OUTPUT_LOCATION_COL_NAME = "output location";

    private static final NodeLogger LOGGER = NodeLogger.getLogger(BinaryObjectsToFilesNodeModel.class);

    BinaryObjectsToFilesNodeModel(final PortsConfiguration config, final BinaryObjectsToFilesNodeConfig nodeSettings) {
        super(config.getInputPorts(), config.getOutputPorts());
        m_binaryObjectsToFileNodeConfig = nodeSettings;
        m_fileWriterSelectionModel = m_binaryObjectsToFileNodeConfig.getFileSettingsModelWriterFileChooser();

        m_inputTableIdx =
            config.getInputPortLocation().get(BinaryObjectsToFilesNodeFactory.DATA_TABLE_INPUT_PORT_GRP_NAME)[0];
        m_statusConsumer = new NodeModelStatusConsumer(EnumSet.of(MessageType.ERROR, MessageType.WARNING));
    }

    @Override
    protected PortObject[] execute(final PortObject[] inObjects, final ExecutionContext exec) throws Exception {

        final PortObjectSpec[] inSpecs = Arrays.stream(inObjects)//
            .map(PortObject::getSpec)//
            .toArray(PortObjectSpec[]::new);

        final DataTableSpec inputTableSpec = (DataTableSpec)inSpecs[m_inputTableIdx];
        final int binaryObjColIdx = inputTableSpec
            .findColumnIndex(m_binaryObjectsToFileNodeConfig.getStringValSelectedBinaryObjectColumnModel());

        try (final WritePathAccessor writePathAccessor = m_fileWriterSelectionModel.createWritePathAccessor()) {

            final FSPath outputPath = writePathAccessor.getOutputPath(m_statusConsumer);
            m_statusConsumer.setWarningsIfRequired(this::setWarningMessage);

            //check user settings and create missing folders if required
            createOutputFoldersIfMissing(outputPath);

            try (final BinaryObjectsToFilesCellFactory binaryObjectsToFilesCellFactory =
                m_binaryObjectsToFileNodeConfig.generateFileNames()
                    ? new BinaryObjectsToFilesCellFactory(getNewColumnSpec(inputTableSpec), binaryObjColIdx,
                        m_binaryObjectsToFileNodeConfig.getStringValUserDefinedOutputFilenameModel(),
                        m_fileWriterSelectionModel.getFileOverwritePolicy(), outputPath, exec)
                    : new BinaryObjectsToFilesCellFactory(getNewColumnSpec(inputTableSpec), binaryObjColIdx,
                        inputTableSpec
                            .findColumnIndex(m_binaryObjectsToFileNodeConfig.getStringValOutputFilenameColumnModel()),
                        m_fileWriterSelectionModel.getFileOverwritePolicy(), outputPath, exec)) {
                final ColumnRearranger columnRearrangerObj =
                    createColumnRearranger(inputTableSpec, binaryObjectsToFilesCellFactory);
                final BufferedDataTable outputBufferTable = exec.createColumnRearrangeTable(
                    (BufferedDataTable)inObjects[m_inputTableIdx], columnRearrangerObj, exec);

                return new PortObject[]{outputBufferTable};
            }
        }
    }

    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        final DataTableSpec inputTableSpec = (DataTableSpec)inSpecs[m_inputTableIdx];
        if (m_binaryObjectsToFileNodeConfig.getStringValSelectedBinaryObjectColumnModel() == null) {
            autoGuess(inSpecs);
        }

        validate(inSpecs);

        //configure the SettingsModelWriterFileChooser
        m_fileWriterSelectionModel.configureInModel(inSpecs, m_statusConsumer);
        m_statusConsumer.setWarningsIfRequired(this::setWarningMessage);

        final int binaryColIndx = inputTableSpec
            .findColumnIndex(m_binaryObjectsToFileNodeConfig.getStringValSelectedBinaryObjectColumnModel());

        try (final BinaryObjectsToFilesCellFactory binaryObjectsToFilesCellFactory =
            new BinaryObjectsToFilesCellFactory(getNewColumnSpec(inputTableSpec), binaryColIndx, null,
                m_fileWriterSelectionModel.getFileOverwritePolicy(), null, null)) {
            return new DataTableSpec[]{
                createColumnRearranger(inputTableSpec, binaryObjectsToFilesCellFactory).createSpec()};
        } catch (Exception e) {
            //Rethrow the exception
            throw new InvalidSettingsException(e);
        }
    }

    /**
     * Automatically select the first column in input table which matches the expected type
     *
     * @param inSpecs An array of PortObjectSpec
     * @throws InvalidSettingsException If no column is found with desired data type
     */
    private void autoGuess(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        final DataTableSpec inputTableSpec = (DataTableSpec)inSpecs[m_inputTableIdx];
        m_binaryObjectsToFileNodeConfig.setStringValSelectedBinaryObjectColumnModel(inputTableSpec.stream()//
            .filter(dcs -> dcs.getType().isCompatible(BinaryObjectDataValue.class))//
            .map(DataColumnSpec::getName)//
            .findFirst()//
            .orElseThrow(() -> new InvalidSettingsException("No applicable column available"))//
        );
        setWarningMessage(String.format("Auto-guessed column to convert '%s'",
            m_binaryObjectsToFileNodeConfig.getStringValSelectedBinaryObjectColumnModel()));

        if (!m_binaryObjectsToFileNodeConfig.generateFileNames()) {
            m_binaryObjectsToFileNodeConfig.setStringValOutputFilenameColumnModel(inputTableSpec.stream()//
                .filter(dcs -> dcs.getType().isCompatible(StringValue.class))//
                .map(DataColumnSpec::getName)//
                .findFirst()//
                .orElseThrow(() -> new InvalidSettingsException("No applicable column available"))//
            );
            setWarningMessage(String.format("Auto-guessed column to convert '%s'",
                m_binaryObjectsToFileNodeConfig.getStringValOutputFilenameColumnModel()));
        }
    }

    /**
     * Validate the selected column in input table
     *
     * @param inSpecs An array of PortObjectSpec
     * @throws InvalidSettingsException If the selected column is not part of the input or doesn't have the correct data
     *             type
     */
    private void validate(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        final DataTableSpec inSpec = (DataTableSpec)inSpecs[m_inputTableIdx];
        final String binaryObjColName = m_binaryObjectsToFileNodeConfig.getStringValSelectedBinaryObjectColumnModel();
        final int colIndex = inSpec.findColumnIndex(binaryObjColName);

        //check if column exists and is of compatible BinaryObjectDataValue type
        validateColumnExistenceAndTypeComptability(inSpec, binaryObjColName, colIndex, BinaryObjectDataValue.class);

        if (inSpec.containsName(OUTPUT_LOCATION_COL_NAME)) {
            setWarningMessage(String.format("The name of the column to create is already taken, using '%s' instead.",
                getUniqueColumnName(inSpec)));
        }

        // Validate m_outputFilenameColumnModel only if this option is selected in m_filenameSettingsModel
        if (!m_binaryObjectsToFileNodeConfig.generateFileNames()) {
            final String outputFilenameColName =
                m_binaryObjectsToFileNodeConfig.getStringValOutputFilenameColumnModel();
            final int outputFilenameColIndex = inSpec.findColumnIndex(outputFilenameColName);

            //check if column exists and is of compatible StringValue type
            validateColumnExistenceAndTypeComptability(inSpec, outputFilenameColName, outputFilenameColIndex,
                StringValue.class);
        }

    }

    /**
     * Checks if the column exists and is compatible with the provided DataValue class
     *
     * @param inSpec A DataTableSpec object
     * @param columnName Name of the column to be checked
     * @param colIndex Index of the column to be checked
     * @throws InvalidSettingsException
     */
    private static void validateColumnExistenceAndTypeComptability(final DataTableSpec inSpec, final String columnName,
        final int colIndex, final Class<? extends DataValue> valueClass) throws InvalidSettingsException {
        // check column existence
        CheckUtils.checkSetting(colIndex >= 0, "The selected column '%s' is not part of the input", columnName);

        // check column type
        final DataColumnSpec pathColSpec = inSpec.getColumnSpec(colIndex);
        CheckUtils.checkSetting(pathColSpec.getType().isCompatible(valueClass),
            "The selected column '%s' has the wrong type", columnName);
    }

    /**
     * Returns a DataColumnSpec with relevant metadata and column name
     *
     * @param inSpec An array of PortObjectSpec
     * @return A new DataColumnSpec
     */

    private DataColumnSpec getNewColumnSpec(final DataTableSpec inSpec) {

        final FSLocationSpec location = m_fileWriterSelectionModel.getLocation();

        final FSLocationValueMetaData metaData = new FSLocationValueMetaData(location.getFileSystemCategory(),
            location.getFileSystemSpecifier().orElse(null));

        final DataColumnSpecCreator fsLocationSpec = new DataColumnSpecCreator(
            inSpec.containsName(OUTPUT_LOCATION_COL_NAME) ? getUniqueColumnName(inSpec) : OUTPUT_LOCATION_COL_NAME,
            FSLocationCellFactory.TYPE);

        fsLocationSpec.addMetaData(metaData, true);
        return fsLocationSpec.createSpec();
    }

    /**
     * Returns an instance of ColumnRearranger which appends the new column from BinaryObjectsToFilesCellFactory
     *
     * @param inSpec An array of PortObjectSpec
     * @param factory A object of {@link BinaryObjectsToFilesCellFactory}
     * @return An instance of ColumnRearranger
     */
    private ColumnRearranger createColumnRearranger(final DataTableSpec inSpec,
        final BinaryObjectsToFilesCellFactory factory) {

        // now create output spec using a ColumnRearranger
        final ColumnRearranger colRearranger = new ColumnRearranger(inSpec);
        colRearranger.append(factory);

        if (m_binaryObjectsToFileNodeConfig.isRemoveBinaryColumnModelEnabled()) {
            // remove the binary column from ouput
            colRearranger.remove(m_binaryObjectsToFileNodeConfig.getStringValSelectedBinaryObjectColumnModel());
        }
        return colRearranger;
    }

    /**
     * Return a new unique string based on the input spec, so a new column spec can be created
     *
     * @param inputSpec A DataTableSpec object
     * @return A unique String for creating a new column spec
     */
    private static String getUniqueColumnName(final DataTableSpec inputSpec) {
        return DataTableSpec.getUniqueColumnName(inputSpec, OUTPUT_LOCATION_COL_NAME);
    }

    /**
     * Create the missing folders in provided output path, depends on the "Create missing folders" option in settings
     *
     * @param outputPath The FSPath for output folder
     * @throws IOException Throw exception if folder is missing and user has not checked "Create missing folders" option
     *             in settings
     */
    private void createOutputFoldersIfMissing(final FSPath outputPath) throws IOException {
        if (!Files.exists(outputPath)) {
            if (m_fileWriterSelectionModel.isCreateMissingFolders()) {
                FSFiles.createDirectories(outputPath);
            } else {
                throw new IOException(String.format(
                    "The directory '%s' does not exist and must not be created due to user settings.", outputPath));
            }
        }
    }

    @Override
    protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        // nothing to do here
    }

    @Override
    protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        // nothing to do here
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_binaryObjectsToFileNodeConfig.saveSettingsTo(settings);
    }

    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_binaryObjectsToFileNodeConfig.validateSettings(settings);
    }

    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_binaryObjectsToFileNodeConfig.loadValidatedSettingsFrom(settings);
    }

    @Override
    protected void reset() {
        // nothing to do here
    }

    // A Factory that extends SingleCellFactory to create a column with FSLocationCell
    // Also implements the actual logic of creating Binary files on the filesystem
    private static final class BinaryObjectsToFilesCellFactory extends SingleCellFactory implements AutoCloseable {

        private final int m_binaryObjColIdx;

        private final MultiFSLocationCellFactory m_multiFSLocationCellFactory;

        private final ExecutionContext m_executionContext;

        private final FSPath m_outputPath;

        private int m_iteratorCount;

        private final FileOverwritePolicy m_overwritePolicy;

        private final int m_outputFilenameColIdx;

        private final String m_userDefinedFilename;

        private BinaryObjectsToFilesCellFactory(final DataColumnSpec columnSpec, final int binaryColId,
            final int outputFilenameColdId, final String userDefinedFilenamePattern,
            final FileOverwritePolicy overwritePolicy, final FSPath outputPath, final ExecutionContext execContext) {
            super(columnSpec);
            m_binaryObjColIdx = binaryColId;
            m_multiFSLocationCellFactory = new MultiFSLocationCellFactory();
            m_outputPath = outputPath;
            m_executionContext = execContext;
            m_iteratorCount = 0;
            m_overwritePolicy = overwritePolicy;
            m_outputFilenameColIdx = outputFilenameColdId;
            m_userDefinedFilename = userDefinedFilenamePattern;
        }

        BinaryObjectsToFilesCellFactory(final DataColumnSpec columnSpec, final int binaryColId,
            final String userDefinedFilenamePattern, final FileOverwritePolicy overwritePolicy, final FSPath outputPath,
            final ExecutionContext execContext) {
            this(columnSpec, binaryColId, -1, userDefinedFilenamePattern, overwritePolicy, outputPath, execContext);
        }

        BinaryObjectsToFilesCellFactory(final DataColumnSpec columnSpec, final int binaryColId,
            final int outputFilenameColdId, final FileOverwritePolicy overwritePolicy, final FSPath outputPath,
            final ExecutionContext execContext) {
            this(columnSpec, binaryColId, outputFilenameColdId, null, overwritePolicy, outputPath, execContext);

        }

        @Override
        public DataCell getCell(final DataRow row) {

            //Check if the column is missing and return a missing DataCell object
            if (row.getCell(m_binaryObjColIdx).isMissing()) {
                return DataType.getMissingCell();
            }

            final BinaryObjectDataCell binaryObjDataCell = (BinaryObjectDataCell)row.getCell(m_binaryObjColIdx);

            final String outputFilename = getOutputFilename(row);

            //will introduce dynamic naming convention in future updates
            final FSPath outputFileFSPath = (FSPath)m_outputPath.resolve(outputFilename);
            m_iteratorCount++;

            //handles file creation and FileOverwritePolicy settings
            try {
                createBinaryFile(binaryObjDataCell, outputFileFSPath);
            } catch (IOException creatFileException) {
                LOGGER.error(creatFileException);
                throw new RuntimeException(creatFileException.getMessage(), creatFileException.getCause()); //NOSONAR
            }

            return m_multiFSLocationCellFactory.createCell(m_executionContext, outputFileFSPath.toFSLocation());
        }

        /**
         * Check factory options whether to get filename from output column or user defined pattern and formulate an
         * output filename
         *
         * @param A DataRow object for the current row
         * @return a string value for output filename
         */
        private String getOutputFilename(final DataRow row) {
            final String outputFilename;
            // -1 indicates this option is not selected
            if (m_outputFilenameColIdx != -1) {
                //Throw exception is filename needs comes from selected column but found Missing value
                if (row.getCell(m_outputFilenameColIdx).isMissing()) {
                    throw new RuntimeException("Output filename in row \"" + row.getKey() + "\" is missing"); //NOSONAR
                }
                final StringCell outputFilenameDataCell = (StringCell)row.getCell(m_outputFilenameColIdx);
                outputFilename = outputFilenameDataCell.getStringValue();
            } else {
                //Replace ? in the user defined input string with m_iteratorCount to generate unique names
                outputFilename = m_userDefinedFilename.replace("?", Integer.toString(m_iteratorCount));
            }
            return outputFilename;
        }

        /**
         * Create the Binary files using NIO library, also responsible for checking FileOverwritePolicy settings
         *
         * @param binaryObjDataCell the BinaryObjectDataCell
         * @param outputFileFSPath the FSPath instance for the output file
         * @throws IOException Throws IOExceptions most likely FileAlreadyExistsException
         */
        private void createBinaryFile(final BinaryObjectDataCell binaryObjDataCell, final FSPath outputFileFSPath)
            throws IOException {
            final boolean fileAlreadyExists = Files.exists(outputFileFSPath);
            try (final InputStream iS = binaryObjDataCell.openInputStream()) {
                if (m_overwritePolicy == FileOverwritePolicy.OVERWRITE) {
                    //Might throw FileAlreadyExistsException exception on AWS-S3 FS, follow ticket AP-15466 for resolution
                    Files.copy(iS, outputFileFSPath, StandardCopyOption.REPLACE_EXISTING);
                } else if (m_overwritePolicy == FileOverwritePolicy.FAIL) {
                    if (fileAlreadyExists) {
                        throw new FileAlreadyExistsException(String.format(
                            "The file '%s' already exists and must not be overwritten", outputFileFSPath.toString()));
                    } else {
                        Files.copy(iS, outputFileFSPath);
                    }
                } else if (m_overwritePolicy == FileOverwritePolicy.IGNORE && !fileAlreadyExists) {
                    Files.copy(iS, outputFileFSPath);
                }
            }
        }

        @Override
        public void close() throws Exception {
            if (m_multiFSLocationCellFactory != null) {
                m_multiFSLocationCellFactory.close();
            }

        }
    }
}
