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
 *   20 Jul 2021 (modithahewasinghage): created
 */
package org.knime.filehandling.core.node.table.writer;

import java.io.File;
import java.io.IOException;
import java.util.EnumSet;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataValue;
import org.knime.core.data.MissingValue;
import org.knime.core.data.MissingValueException;
import org.knime.core.data.StringValue;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.def.StringCell;
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
import org.knime.filehandling.core.data.location.cell.SimpleFSLocationCellFactory;
import org.knime.filehandling.core.defaultnodesettings.filechooser.writer.FileOverwritePolicy;
import org.knime.filehandling.core.defaultnodesettings.filechooser.writer.SettingsModelWriterFileChooser;
import org.knime.filehandling.core.defaultnodesettings.filechooser.writer.WritePathAccessor;
import org.knime.filehandling.core.defaultnodesettings.status.NodeModelStatusConsumer;
import org.knime.filehandling.core.defaultnodesettings.status.StatusMessage.MessageType;

/**
 * Generic implementation of a Writer node that writes tables.
 *
 * @author Laurin Siefermann, KNIME GmbH, Konstanz, Germany
 */
public abstract class AbstractMultiTableWriterNodeModel<C extends AbstractMultiTableWriterNodeConfig, F extends AbstractColumnsToFilesCellFactory<DataValue>>
    extends NodeModel {

    private static final String DATA_TABLE_OUTPUT_COLUMN_NAME = "Output Location";

    private final int m_inputTableIdx;

    private final C m_nodeConfig;

    // TODO: Make final later on
    private SettingsModelWriterFileChooser m_outputLocationModel;

    private SettingsModelString m_colSelectModel;

    private SettingsModelBoolean m_removeColModel;

    private SettingsModelString m_userDefinedOutputFileName;

    private SettingsModelColumnName m_filenameColSelectionModel;

    private NodeModelStatusConsumer m_statusConsumer;

    /**
     * Constructor.
     *
     * @param portConfig
     * @param nodeConfig
     * @param inputTableIdx index of data table input port group name
     */
    AbstractMultiTableWriterNodeModel(final PortsConfiguration portConfig, final C nodeConfig,
        final int inputTableIdx) {
        super(portConfig.getInputPorts(), portConfig.getOutputPorts());

        m_inputTableIdx = inputTableIdx;
        m_nodeConfig = nodeConfig;

        //        m_outputLocationModel = m_nodeConfig.getOutputLocationModel();
        //        m_imgColSelectModel = m_nodeConfig.getImgColSelectionModel();
        //        m_removeColModel = m_nodeConfig.getRemoveImgColModel();
        //        m_userDefinedOutputFileName = m_nodeConfig.getUserDefinedOutputFilenameModel();
        //        m_filenameColSelectionModel = m_nodeConfig.getFilenameColSelectionModel();

        m_statusConsumer = new NodeModelStatusConsumer(EnumSet.of(MessageType.ERROR, MessageType.WARNING));
    }

    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        final DataTableSpec dataTableSpec = (DataTableSpec)inSpecs[m_inputTableIdx];
        String selectedCol = m_colSelectModel.getStringValue();
        final String selectedFilenameCol = m_filenameColSelectionModel.getStringValue();

        if (selectedCol == null) {
            selectedCol = autoGuess(dataTableSpec);
        }

        final int colIdx = dataTableSpec.findColumnIndex(selectedCol);
        if (colIdx < 0) {
            throw new InvalidSettingsException(
                String.format("The selected column '%s' is not part of the input", selectedCol));
        }

        validateNameGenerationSettings(dataTableSpec, selectedFilenameCol);

        // TODO: add in config
        //m_nodeConfig.getFolderChooserModel().configureInModel(inSpecs, m_statusConsumer);
        m_statusConsumer.setWarningsIfRequired(this::setWarningMessage);

        final F columnsToFilesCellFactory = getFactory(getNewColumnsSpec(dataTableSpec), colIdx,
            m_outputLocationModel.getFileOverwritePolicy(), null, null);

        final ColumnRearranger c = createColumnRearranger(dataTableSpec, columnsToFilesCellFactory);
        final DataTableSpec inputTableSpec = c.createSpec();

        return new PortObjectSpec[]{inputTableSpec};
    }

    private String autoGuess(final DataTableSpec spec) throws InvalidSettingsException {
        final String guessedColumn = spec.stream()//
            .filter(s -> s.getType().isCompatible(ImageValue.class))//
            .map(DataColumnSpec::getName)//
            .findFirst()//
            .orElseThrow(() -> new InvalidSettingsException("No applicable column available"));

        m_colSelectModel.setStringValue(guessedColumn);

        setWarningMessage(String.format("Auto-guessed column '%s'", guessedColumn));

        return guessedColumn;
    }

    private void validateNameGenerationSettings(final DataTableSpec dataTableSpec, final String selectedFilenameCol)
        throws InvalidSettingsException {

        // TODO: add in config.
        // final boolean isFileNameColumnSelected = !m_nodeConfig.isGenerateFilenameRadioSelected();
        final boolean isFileNameColumnSelected = true;

        if (isFileNameColumnSelected) {
            final boolean isRowIdSelected = m_filenameColSelectionModel.useRowID();
            if (!isRowIdSelected) {
                final int filenameColIdx = dataTableSpec.findColumnIndex(selectedFilenameCol);
                if (filenameColIdx < 0) {
                    throw new InvalidSettingsException(
                        String.format("The selected naming column '%s' is not part of the input", selectedFilenameCol));
                }
            }
        }
    }

    private ColumnRearranger createColumnRearranger(final DataTableSpec in, final F factory) {

        final ColumnRearranger c = new ColumnRearranger(in);
        c.append(factory);

        if (m_removeColModel.getBooleanValue()) {
            c.remove(m_colSelectModel.getStringValue());
        }

        return c;
    }

    /**
     * TODO: Write Javadoc
     *
     * @return
     */
    protected abstract F getFactory(final DataColumnSpec[] columnSpec, final int imgColIdx,
        final FileOverwritePolicy overwritePolicy, final FSPath outputPath, final FileNameGenerator fileNameGenerator);

    private DataColumnSpec[] getNewColumnsSpec(final DataTableSpec spec) {
        final FSLocationSpec location = m_outputLocationModel.getLocation();

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

    @Override
    protected BufferedDataTable[] execute(final PortObject[] inObjects, final ExecutionContext exec) throws Exception {
        final BufferedDataTable inputDataTable = (BufferedDataTable)inObjects[m_inputTableIdx];
        final DataTableSpec dataTableSpec = inputDataTable.getDataTableSpec();

        try (final WritePathAccessor writePathAccessor = m_outputLocationModel.createWritePathAccessor()) {
            final FSPath outputPath = writePathAccessor.getOutputPath(m_statusConsumer);
            m_statusConsumer.setWarningsIfRequired(this::setWarningMessage);

            createOutputDirIfRequired(outputPath);

            final int colIdx = dataTableSpec.findColumnIndex(m_colSelectModel.getStringValue());

            final F columnsToFilesCellFactory = createValueColumnsToFilesCellFactory(dataTableSpec, colIdx, outputPath);

            final ColumnRearranger c = createColumnRearranger(dataTableSpec, columnsToFilesCellFactory);
            final BufferedDataTable out = exec.createColumnRearrangeTable(inputDataTable, c, exec);

            if (columnsToFilesCellFactory.getMissingCellCount() > 0) {
                setWarningMessage(
                    "Skipped " + columnsToFilesCellFactory.getMissingCellCount() + " row(s) due to missing values.");
            }

            return new BufferedDataTable[]{out};
        }

    }

    private void createOutputDirIfRequired(final FSPath outputPath) throws IOException {
        if (!FSFiles.exists(outputPath)) {
            if (m_outputLocationModel.isCreateMissingFolders()) {
                FSFiles.createDirectories(outputPath);
            } else {
                throw new IOException(String.format(
                    "The directory '%s' does not exist and must not be created due to user settings.", outputPath));
            }
        }
    }

    private F createValueColumnsToFilesCellFactory(final DataTableSpec dataTableSpec, final int colIdx,
        final FSPath outputPath) {
        final DataColumnSpec[] newColumnsSpec = getNewColumnsSpec(dataTableSpec);
        final FileOverwritePolicy fileOverwritePolicy = m_outputLocationModel.getFileOverwritePolicy();

        // TODO: put that one back in the if statement below.
        // m_nodeConfig.isGenerateFilenameRadioSelected()
        if (true) {
            final String generatedFilenamePattern = m_userDefinedOutputFileName.getStringValue();
            return getFactory(newColumnsSpec, colIdx, fileOverwritePolicy, outputPath,
                new UserPatternFileNameGenerator(generatedFilenamePattern));
        } else {
            // final boolean isRowIdSelected = m_nodeConfig.getFilenameColSelectionModel().useRowID();
            final int filenameColIdx;
            // put in row below: isRowIdSelected
            if (true) {
                return getFactory(newColumnsSpec, colIdx, fileOverwritePolicy, outputPath,
                    new RowIdFileNameGenerator());
            } else {
                filenameColIdx = dataTableSpec.findColumnIndex(m_filenameColSelectionModel.getStringValue());
                return getFactory(newColumnsSpec, colIdx, fileOverwritePolicy, outputPath,
                    new NameingColumnFileNameGenerator(filenameColIdx));
            }
        }
    }

    @SuppressWarnings("resource")
    @Override
    public StreamableOperator createStreamableOperator(final PartitionInfo partitionInfo,
        final PortObjectSpec[] inSpecs) throws InvalidSettingsException {

        final DataTableSpec dataTableSpec = (DataTableSpec)inSpecs[m_inputTableIdx];
        final int imgColIdx = dataTableSpec.findColumnIndex(m_colSelectModel.getStringValue());
        final WritePathAccessor writePathAccessor = m_outputLocationModel.createWritePathAccessor(); //NOSONAR

        final FSPath outputPath = writePathAccessor.getOutputPath(m_statusConsumer);

        final F columnsToFilesCellFactory = createValueColumnsToFilesCellFactory(dataTableSpec, imgColIdx, outputPath);

        final StreamableFunction wrappedFunction =
            createColumnRearranger(dataTableSpec, columnsToFilesCellFactory).createStreamableFunction();

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
    public InputPortRole[] getInputPortRoles() {
        final InputPortRole[] inputPortRoles = super.getInputPortRoles();
        inputPortRoles[m_inputTableIdx] = InputPortRole.NONDISTRIBUTED_STREAMABLE;
        return inputPortRoles;
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
        //TODO
        //m_nodeConfig.saveSettingsForModel(settings);
    }

    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        //TODO
        //m_nodeConfig.validateSettingsForModel(settings);
    }

    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        //TODO
        //m_nodeConfig.loadSettingsForModel(settings);
    }

    @Override
    protected void reset() {
        // Nothing to do
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

    //TODO: Extract from previous location (org.knime.base.node.io.filehandling.imagewriter.table) and extract it
    // into seperate class

    /**
     * Interface for file name strategy pattern.
     *
     * @author Laurin Siefermann, KNIME GmbH, Konstanz, Germany
     */
    interface FileNameGenerator {

        public String getOutputFilename(final DataRow row, final int rowIdx);
    }
}
