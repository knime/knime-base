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
 *   15 Mar 2021 (Laurin Siefermann, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.base.node.io.filehandling.imagewriter.table;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.AccessDeniedException;
import java.nio.file.FileAlreadyExistsException;
import java.util.EnumSet;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.MissingCell;
import org.knime.core.data.MissingValue;
import org.knime.core.data.MissingValueException;
import org.knime.core.data.StringValue;
import org.knime.core.data.container.AbstractCellFactory;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.def.StringCell.StringCellFactory;
import org.knime.core.data.image.ImageValue;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.context.ports.PortsConfiguration;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelColumnName;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.streamable.InputPortRole;
import org.knime.core.node.streamable.PartitionInfo;
import org.knime.core.node.streamable.StreamableFunction;
import org.knime.core.node.streamable.StreamableOperator;
import org.knime.filehandling.core.connections.FSFiles;
import org.knime.filehandling.core.connections.FSLocationSpec;
import org.knime.filehandling.core.connections.FSPath;
import org.knime.filehandling.core.data.location.FSLocationValueMetaData;
import org.knime.filehandling.core.data.location.cell.MultiSimpleFSLocationCellFactory;
import org.knime.filehandling.core.data.location.cell.SimpleFSLocationCell;
import org.knime.filehandling.core.data.location.cell.SimpleFSLocationCellFactory;
import org.knime.filehandling.core.defaultnodesettings.filechooser.writer.FileOverwritePolicy;
import org.knime.filehandling.core.defaultnodesettings.filechooser.writer.SettingsModelWriterFileChooser;
import org.knime.filehandling.core.defaultnodesettings.filechooser.writer.WritePathAccessor;
import org.knime.filehandling.core.defaultnodesettings.status.NodeModelStatusConsumer;
import org.knime.filehandling.core.defaultnodesettings.status.StatusMessage.MessageType;

/**
 * Node model of the image writer table node.
 *
 * @author Laurin Siefermann, KNIME GmbH, Konstanz, Germany
 */
final class ImageWriterTableNodeModel extends NodeModel {

    private static final String DATA_TABLE_OUTPUT_COLUMN_NAME = "Output Location";

    private final int m_inputTableIdx;

    private final ImageWriterTableNodeConfig m_nodeConfig;

    private final SettingsModelWriterFileChooser m_folderChooserModel;

    private final SettingsModelString m_imgColSelectModel;

    private final SettingsModelBoolean m_removeImgColModel;

    private final SettingsModelString m_userDefinedOutputFileName;

    private final SettingsModelColumnName m_filenameColSelectionModel;

    private final NodeModelStatusConsumer m_statusConsumer;

    /**
     * Constructor.
     *
     * @param portConfig
     * @param nodeConfig
     * @param inputTableIdx index of data table input port group name
     */
    ImageWriterTableNodeModel(final PortsConfiguration portConfig, final ImageWriterTableNodeConfig nodeConfig,
        final int inputTableIdx) {
        super(portConfig.getInputPorts(), portConfig.getOutputPorts());

        m_inputTableIdx = inputTableIdx;
        m_nodeConfig = nodeConfig;
        m_folderChooserModel = m_nodeConfig.getFolderChooserModel();
        m_imgColSelectModel = m_nodeConfig.getImgColSelectionModel();
        m_removeImgColModel = m_nodeConfig.getRemoveImgColModel();
        m_userDefinedOutputFileName = m_nodeConfig.getUserDefinedOutputFilenameModel();
        m_filenameColSelectionModel = m_nodeConfig.getFilenameColSelectionModel();
        m_statusConsumer = new NodeModelStatusConsumer(EnumSet.of(MessageType.ERROR, MessageType.WARNING));
    }

    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        final DataTableSpec dataTableSpec = (DataTableSpec)inSpecs[m_inputTableIdx];
        String selectedImgCol = m_imgColSelectModel.getStringValue();
        final String selectedFilenameCol = m_filenameColSelectionModel.getStringValue();

        if (selectedImgCol == null) {
            selectedImgCol = autoGuess(dataTableSpec);
        }

        final int imgColIdx = dataTableSpec.findColumnIndex(selectedImgCol);
        if (imgColIdx < 0) {
            throw new InvalidSettingsException(
                String.format("The selected image column '%s' is not part of the input", selectedImgCol));
        }

        validateNameGenerationSettings(dataTableSpec, selectedFilenameCol);

        m_nodeConfig.getFolderChooserModel().configureInModel(inSpecs, m_statusConsumer);
        m_statusConsumer.setWarningsIfRequired(this::setWarningMessage);

        final ImageColumnsToFilesCellFactory imageColumnsToFilesCellFactory = new ImageColumnsToFilesCellFactory(
            getNewColumnsSpec(dataTableSpec), imgColIdx, m_folderChooserModel.getFileOverwritePolicy(), null, null);

        final ColumnRearranger c = createColumnRearranger(dataTableSpec, imageColumnsToFilesCellFactory);
        final DataTableSpec inputTableSpec = c.createSpec();

        return new PortObjectSpec[]{inputTableSpec};
    }

    @Override
    protected BufferedDataTable[] execute(final PortObject[] inObjects, final ExecutionContext exec) throws Exception {
        final BufferedDataTable inputDataTable = (BufferedDataTable)inObjects[m_inputTableIdx];
        final DataTableSpec dataTableSpec = inputDataTable.getDataTableSpec();

        try (final WritePathAccessor writePathAccessor = m_folderChooserModel.createWritePathAccessor()) {
            final FSPath outputPath = writePathAccessor.getOutputPath(m_statusConsumer);
            m_statusConsumer.setWarningsIfRequired(this::setWarningMessage);

            createOutputDirIfRequired(outputPath);

            final int imgColIdx = dataTableSpec.findColumnIndex(m_imgColSelectModel.getStringValue());

            final ImageColumnsToFilesCellFactory imageColumnsToFilesCellFactory =
                createImageColumnsToFilesCellFactory(dataTableSpec, imgColIdx, outputPath);

            final ColumnRearranger c = createColumnRearranger(dataTableSpec, imageColumnsToFilesCellFactory);
            final BufferedDataTable out = exec.createColumnRearrangeTable(inputDataTable, c, exec);

            if (imageColumnsToFilesCellFactory.getMissingCellCount() > 0) {
                setWarningMessage("Skipped " + imageColumnsToFilesCellFactory.getMissingCellCount()
                    + " row(s) due to missing values.");
            }

            return new BufferedDataTable[]{out};
        }

    }

    private void validateNameGenerationSettings(final DataTableSpec dataTableSpec, final String selectedFilenameCol)
        throws InvalidSettingsException {
        final boolean isFileNameColumnSelected = !m_nodeConfig.isGenerateFilenameRadioSelected();

        if (isFileNameColumnSelected) {
            final boolean isRowIdSelected = m_filenameColSelectionModel.useRowID();
            if (!isRowIdSelected) {
                final int filenameColIdx = dataTableSpec.findColumnIndex(selectedFilenameCol);
                if (filenameColIdx < 0) {
                    throw new InvalidSettingsException(String
                        .format("The selected image naming column '%s' is not part of the input", selectedFilenameCol));
                }
            }
        }
    }

    private ImageColumnsToFilesCellFactory createImageColumnsToFilesCellFactory(final DataTableSpec dataTableSpec,
        final int imgColIdx, final FSPath outputPath) {
        final DataColumnSpec[] newColumnsSpec = getNewColumnsSpec(dataTableSpec);
        final FileOverwritePolicy fileOverwritePolicy = m_folderChooserModel.getFileOverwritePolicy();

        if (m_nodeConfig.isGenerateFilenameRadioSelected()) {
            final String generatedFilenamePattern = m_userDefinedOutputFileName.getStringValue();
            return new ImageColumnsToFilesCellFactory(newColumnsSpec, imgColIdx, fileOverwritePolicy, outputPath,
                new UserPatternFileNameGenerator(generatedFilenamePattern));
        } else {
            final boolean isRowIdSelected = m_nodeConfig.getFilenameColSelectionModel().useRowID();
            final int filenameColIdx;
            if (isRowIdSelected) {
                return new ImageColumnsToFilesCellFactory(newColumnsSpec, imgColIdx, fileOverwritePolicy, outputPath,
                    new RowIdFileNameGenerator());
            } else {
                filenameColIdx = dataTableSpec.findColumnIndex(m_filenameColSelectionModel.getStringValue());
                return new ImageColumnsToFilesCellFactory(newColumnsSpec, imgColIdx, fileOverwritePolicy, outputPath,
                    new NameingColumnFileNameGenerator(filenameColIdx));
            }
        }
    }

    private ColumnRearranger createColumnRearranger(final DataTableSpec in,
        final ImageColumnsToFilesCellFactory factory) {

        final ColumnRearranger c = new ColumnRearranger(in);
        c.append(factory);

        if (m_removeImgColModel.getBooleanValue()) {
            c.remove(m_imgColSelectModel.getStringValue());
        }

        return c;
    }

    private void createOutputDirIfRequired(final FSPath outputPath) throws IOException {
        if (!FSFiles.exists(outputPath)) {
            if (m_folderChooserModel.isCreateMissingFolders()) {
                FSFiles.createDirectories(outputPath);
            } else {
                throw new IOException(String.format(
                    "The directory '%s' does not exist and must not be created due to user settings.", outputPath));
            }
        }
    }

    private DataColumnSpec[] getNewColumnsSpec(final DataTableSpec spec) {
        final FSLocationSpec location = m_folderChooserModel.getLocation();

        final FSLocationValueMetaData metaData = new FSLocationValueMetaData(location.getFileSystemCategory(),
            location.getFileSystemSpecifier().orElse(null));

        final String newColName = spec.containsName(DATA_TABLE_OUTPUT_COLUMN_NAME)
            ? DataTableSpec.getUniqueColumnName(spec, DATA_TABLE_OUTPUT_COLUMN_NAME) : DATA_TABLE_OUTPUT_COLUMN_NAME;
        final DataColumnSpecCreator fsLocationSpec =
            new DataColumnSpecCreator(newColName, SimpleFSLocationCellFactory.TYPE);

        final String statusCol =
            spec.containsName("Status") ? DataTableSpec.getUniqueColumnName(spec, "Status") : "Status";
        final DataColumnSpecCreator statusColumnSpec = new DataColumnSpecCreator(statusCol, StringCell.TYPE);

        fsLocationSpec.addMetaData(metaData, true);

        return new DataColumnSpec[]{fsLocationSpec.createSpec(), statusColumnSpec.createSpec()};
    }

    private String autoGuess(final DataTableSpec spec) throws InvalidSettingsException {
        final String guessedColumn = spec.stream()//
            .filter(s -> s.getType().isCompatible(ImageValue.class))//
            .map(DataColumnSpec::getName)//
            .findFirst()//
            .orElseThrow(() -> new InvalidSettingsException("No applicable image column available"));

        m_imgColSelectModel.setStringValue(guessedColumn);

        setWarningMessage(String.format("Auto-guessed image column '%s'", guessedColumn));

        return guessedColumn;
    }

    @Override
    public InputPortRole[] getInputPortRoles() {
        final InputPortRole[] inputPortRoles = super.getInputPortRoles();
        inputPortRoles[m_inputTableIdx] = InputPortRole.NONDISTRIBUTED_STREAMABLE;
        return inputPortRoles;
    }

    @SuppressWarnings("resource")
    @Override
    public StreamableOperator createStreamableOperator(final PartitionInfo partitionInfo,
        final PortObjectSpec[] inSpecs) throws InvalidSettingsException {

        final DataTableSpec dataTableSpec = (DataTableSpec)inSpecs[m_inputTableIdx];
        final int imgColIdx = dataTableSpec.findColumnIndex(m_imgColSelectModel.getStringValue());
        final WritePathAccessor writePathAccessor = m_folderChooserModel.createWritePathAccessor(); //NOSONAR

        final FSPath outputPath = writePathAccessor.getOutputPath(m_statusConsumer);

        final ImageColumnsToFilesCellFactory imageColumnsToFilesCellFactory =
            createImageColumnsToFilesCellFactory(dataTableSpec, imgColIdx, outputPath);

        final StreamableFunction wrappedFunction =
            createColumnRearranger(dataTableSpec, imageColumnsToFilesCellFactory).createStreamableFunction();

        return new StreamableFunction(m_inputTableIdx, 0) {

            @Override
            public void init(final ExecutionContext ctx) throws Exception {
                createOutputDirIfRequired(outputPath);
                wrappedFunction.init(ctx);
            }

            @Override
            public void finish() {
                wrappedFunction.finish();
                try {
                    writePathAccessor.close();
                } catch (IOException ex) {
                    throw new IllegalStateException("An IOException occured while closing the WritePathAccessor.", ex);
                }
            }

            @Override
            public DataRow compute(final DataRow input) throws Exception {
                return wrappedFunction.compute(input);
            }
        };

    }

    @Override
    protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec) {
        // Nothing to do
    }

    @Override
    protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec) {
        // Nothing to do
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_nodeConfig.saveSettingsForModel(settings);
    }

    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_nodeConfig.validateSettingsForModel(settings);
    }

    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_nodeConfig.loadSettingsForModel(settings);
    }

    @Override
    protected void reset() {
        // Nothing to do
    }

    /**
     * Factory that extends {@link AbstractCellFactory}, which creates one new column {@link SimpleFSLocationCell}
     * holding the output path of written images. It also handles the writing of images to the file system.
     *
     * @author Laurin Siefermann, KNIME GmbH, Konstanz, Germany
     */
    private static final class ImageColumnsToFilesCellFactory extends AbstractCellFactory {

        private final MultiSimpleFSLocationCellFactory m_multiFSLocationCellFactory;

        private final FSPath m_outputPath;

        private final FileOverwritePolicy m_overwritePolicy;

        private final int m_imgColIdx;

        private int m_rowIdx = 0;

        private int m_missingCellCount = 0;

        private static final MissingCell MISSING_IMAGE_VALUE_CELL = new MissingCell("Missing image value");

        private final FileNameGenerator m_fileNameGenerator;

        ImageColumnsToFilesCellFactory(final DataColumnSpec[] columnSpec, final int imgColIdx,
            final FileOverwritePolicy overwritePolicy, final FSPath outputPath,
            final FileNameGenerator fileNameGenerator) {
            super(columnSpec);
            m_imgColIdx = imgColIdx;
            m_outputPath = outputPath;
            m_overwritePolicy = overwritePolicy;
            m_multiFSLocationCellFactory = new MultiSimpleFSLocationCellFactory();
            m_fileNameGenerator = fileNameGenerator;
        }

        @Override
        public DataCell[] getCells(final DataRow row) {
            final DataCell imageCell = row.getCell(m_imgColIdx);

            if (imageCell.isMissing()) {
                m_missingCellCount++;
                m_rowIdx++;
                return new DataCell[]{MISSING_IMAGE_VALUE_CELL, MISSING_IMAGE_VALUE_CELL};
            }

            final ImageValue imgValue = (ImageValue)imageCell;
            final FSPath imgPath = (FSPath)m_outputPath
                .resolve(m_fileNameGenerator.getOutputFilename(row, m_rowIdx) + "." + imgValue.getImageExtension());
            m_rowIdx++;

            final String fileStatus = writeImage(imgValue, imgPath);
            return new DataCell[]{m_multiFSLocationCellFactory.createCell(imgPath.toFSLocation()),
                StringCellFactory.create(fileStatus)};
        }

        private String writeImage(final ImageValue imgValue, final FSPath imgPath) {
            String status;

            try {
                status = FSFiles.exists(imgPath) ? "overwritten" : "created";
            } catch (AccessDeniedException accessDeniedException) {
                throw new IllegalStateException(
                    String.format("An AccessDeniedException occured while writing '%s'", imgPath.toString()),
                    accessDeniedException);
            }

            try (final OutputStream outputStream =
                FSFiles.newOutputStream(imgPath, m_overwritePolicy.getOpenOptions())) {
                imgValue.getImageContent().save(outputStream);
            } catch (FileAlreadyExistsException fileAlreadyExistsException) {
                if (m_overwritePolicy == FileOverwritePolicy.FAIL) {
                    throw new IllegalStateException(
                        String.format("The file '%s' already exists and must not be overwritten", imgPath.toString()),
                        fileAlreadyExistsException);
                } else if (m_overwritePolicy == FileOverwritePolicy.IGNORE) {
                    status = "unmodified";
                }
            } catch (IOException writeImageException) {
                throw new IllegalStateException(
                    String.format("An IOException occured while writing '%s'", imgPath.toString()),
                    writeImageException);
            }
            return status;
        }

        private int getMissingCellCount() {
            return m_missingCellCount;
        }

    }

    /**
     * FileNameGenerator class extending {@link FileNameGenerator} creating file names according to a user pattern.
     *
     * @author Laurin Siefermann, KNIME GmbH, Konstanz, Germany
     */
    private static final class UserPatternFileNameGenerator implements FileNameGenerator {

        private final String m_generatedFilenamePattern;

        /**
         * Implementation of {@link FileNameGenerator} creating file names according to a user pattern.
         *
         * @param String generatedFilenamePattern
         */
        UserPatternFileNameGenerator(final String generatedFilenamePattern) {
            m_generatedFilenamePattern = generatedFilenamePattern;
        }

        @Override
        public String getOutputFilename(final DataRow row, final int rowIdx) {
            return m_generatedFilenamePattern.replace("?", Integer.toString(rowIdx));
        }

    }

    /**
     * FileNameGenerator class extending {@link FileNameGenerator} creating file names according to the row id.
     *
     * @author Laurin Siefermann, KNIME GmbH, Konstanz, Germany
     */
    private static final class RowIdFileNameGenerator implements FileNameGenerator {

        @Override
        public String getOutputFilename(final DataRow row, final int rowIdx) {
            return row.getKey().getString();
        }

    }

    /**
     * FileNameGenerator class extending {@link FileNameGenerator} creating file names according to a nameing column.
     *
     * @author Laurin Siefermann, KNIME GmbH, Konstanz, Germany
     */
    private static final class NameingColumnFileNameGenerator implements FileNameGenerator {

        private final int m_filenameColIdx;

        /**
         * Implementation of {@link FileNameGenerator} creating file names according to a nameing column.
         *
         * @param int filenameColIdx
         */
        NameingColumnFileNameGenerator(final int filenameColIdx) {
            m_filenameColIdx = filenameColIdx;
        }

        @Override
        public String getOutputFilename(final DataRow row, final int rowIdx) {
            final DataCell outputFilenameDataCell = row.getCell(m_filenameColIdx);
            if (outputFilenameDataCell.isMissing()) {
                throw new MissingValueException((MissingValue)outputFilenameDataCell,
                    "Missing values are not supported for image names");
            }

            final StringValue outputFilename = (StringValue)outputFilenameDataCell;
            return outputFilename.getStringValue();
        }
    }
}
