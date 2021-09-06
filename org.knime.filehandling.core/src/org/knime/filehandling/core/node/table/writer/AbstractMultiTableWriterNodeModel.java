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
 *   20 Jul 2021 (Moditha Hewasinghage, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.filehandling.core.node.table.writer;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.EnumSet;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataValue;
import org.knime.core.data.StringValue;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.def.StringCell;
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
 * An abstract implementation of a node model for table writer nodes.
 *
 * @author Moditha Hewasinghage, KNIME GmbH, Berlin, Germany
 * @author Lars Schweikardt, KNIME GmbH, Konstanz, Germany
 * @author Laurin Siefermann, KNIME GmbH, Konstanz, Germany
 *
 * @param <C> the type of {@link AbstractMultiTableWriterNodeConfig}
 * @param <F> the type of {@link AbstractMultiTableWriterCellFactory}
 *
 * @noreference non-public API
 * @noextend non-public API
 */
public abstract class AbstractMultiTableWriterNodeModel<C extends AbstractMultiTableWriterNodeConfig<? extends DataValue, C>, //
        F extends AbstractMultiTableWriterCellFactory<? extends DataValue>>
    extends NodeModel {

    /** The table column name of the file output location. */
    protected static final String DEFAULT_DATA_TABLE_OUTPUT_LOCATION_COLUMN_NAME = "Output Location";

    /** The table column name of the status. */
    protected static final String DEFAULT_DATA_TABLE_STATUS_COLUMN_NAME = "Status";

    private final String m_columnNameOutputLocation;

    private final String m_columnNameStatus;

    private final int m_inputTableIdx;

    private final C m_nodeConfig;

    private final SettingsModelWriterFileChooser m_outputLocationSettings;

    private final SettingsModelString m_sourceColumnSelection;

    private final SettingsModelBoolean m_removeSourceColumn;

    private final SettingsModelString m_filenamePattern;

    private final SettingsModelColumnName m_filenameColumnSelection;

    private final NodeModelStatusConsumer m_statusConsumer;

    private String m_sourceColumn;

    private ColumnRearranger m_columnRearranger;

    private SettingsModelBoolean m_compressFiles;

    private boolean m_compressionSupported;

    private F m_multiFileWriterCellFactory;

    /**
     * Constructor with the default column names.
     *
     * @param portConfig storing the ports configurations
     * @param nodeConfig storing the user settings (concrete subclass instantiation of
     *            {@link AbstractMultiTableWriterNodeConfig})
     * @param inputTableIdx index of data-table-input-port group name
     */
    protected AbstractMultiTableWriterNodeModel(final PortsConfiguration portConfig, final C nodeConfig,
        final int inputTableIdx) {
        this(portConfig, nodeConfig, inputTableIdx, DEFAULT_DATA_TABLE_OUTPUT_LOCATION_COLUMN_NAME,
            DEFAULT_DATA_TABLE_STATUS_COLUMN_NAME);
    }

    /**
     * Constructor.
     *
     * @param portConfig storing the ports configurations
     * @param nodeConfig storing the user settings (concrete subclass instantiation of
     *            {@link AbstractMultiTableWriterNodeConfig})
     * @param inputTableIdx index of data-table-input-port group name
     * @param columnNameOutputLocation the name of the column that stores the output location path
     * @param columnNameStatus the name of the column that stores the status
     */
    protected AbstractMultiTableWriterNodeModel(final PortsConfiguration portConfig, final C nodeConfig,
        final int inputTableIdx, final String columnNameOutputLocation, final String columnNameStatus) {
        super(portConfig.getInputPorts(), portConfig.getOutputPorts());

        m_columnNameOutputLocation = columnNameOutputLocation;
        m_columnNameStatus = columnNameStatus;

        m_inputTableIdx = inputTableIdx;
        m_nodeConfig = nodeConfig;

        m_outputLocationSettings = m_nodeConfig.getOutputLocation();
        m_sourceColumnSelection = m_nodeConfig.getSourceColumn();
        m_removeSourceColumn = m_nodeConfig.getRemoveSourceColumn();
        m_compressionSupported = m_nodeConfig.isCompressionSupported();
        if (m_compressionSupported) {
            m_compressFiles = m_nodeConfig.getCompressFiles();
        }
        m_filenamePattern = m_nodeConfig.getFilenamePattern();
        m_filenameColumnSelection = m_nodeConfig.getFilenameColumn();

        m_statusConsumer = new NodeModelStatusConsumer(EnumSet.of(MessageType.ERROR, MessageType.WARNING));
    }

    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        final var dataTableSpec = (DataTableSpec)inSpecs[m_inputTableIdx];

        autoGuessSourceColumn(dataTableSpec);
        validateFilenameColumn(dataTableSpec, m_filenameColumnSelection.getStringValue());

        final int selectedColumnIndex = getColumnIndexOfSourceColumn(dataTableSpec);

        m_nodeConfig.getOutputLocation().configureInModel(inSpecs, m_statusConsumer);
        m_statusConsumer.setWarningsIfRequired(this::setWarningMessage);

        m_multiFileWriterCellFactory = getFactory(getNewColumnsSpec(dataTableSpec), selectedColumnIndex,
            m_outputLocationSettings.getFileOverwritePolicy());

        m_columnRearranger = createColumnRearranger(dataTableSpec);
        final DataTableSpec outputTableSpec = m_columnRearranger.createSpec();

        return new PortObjectSpec[]{outputTableSpec};
    }

    private ColumnRearranger createColumnRearranger(final DataTableSpec in) {
        final var c = new ColumnRearranger(in);
        c.append(m_multiFileWriterCellFactory);
        if (m_removeSourceColumn.getBooleanValue()) {
            c.remove(m_sourceColumnSelection.getStringValue());
        }
        return c;
    }

    private int getColumnIndexOfSourceColumn(final DataTableSpec dataTableSpec) throws InvalidSettingsException {
        final int columnIndex = dataTableSpec.findColumnIndex(m_sourceColumn);
        if (columnIndex < 0) {
            throw new InvalidSettingsException(String.format("The selected %s column '%s' is not part of the input",
                m_nodeConfig.getWriterTypeName(), m_sourceColumn));
        }
        return columnIndex;
    }

    private void autoGuessSourceColumn(final DataTableSpec spec) throws InvalidSettingsException {
        m_sourceColumn = m_sourceColumnSelection.getStringValue();
        if (m_sourceColumn == null) {
            final String guessedColumn = spec.stream()//
                .filter(s -> s.getType().isCompatible(m_nodeConfig.getValueClass()))//
                .map(DataColumnSpec::getName)//
                .findFirst()//
                .orElseThrow(() -> new InvalidSettingsException(
                    String.format("No applicable %s column available", m_nodeConfig.getWriterTypeName())));

            m_sourceColumnSelection.setStringValue(guessedColumn);

            setWarningMessage(String.format("Auto-guessed column '%s' as %s input column", guessedColumn,
                m_nodeConfig.getWriterTypeName()));

            m_sourceColumn = guessedColumn;
        }
    }

    private void validateFilenameColumn(final DataTableSpec dataTableSpec, final String selectedFilenameColumn)
        throws InvalidSettingsException {

        final boolean isFileNameColumnSelected = !m_nodeConfig.shouldGenerateFilename();

        if (isFileNameColumnSelected) {
            final boolean isRowIdSelected = m_filenameColumnSelection.useRowID();
            if (!isRowIdSelected) {
                final int filenameColumnIndex = dataTableSpec.findColumnIndex(selectedFilenameColumn);
                if (filenameColumnIndex < 0) {
                    throw new InvalidSettingsException(
                        String.format("The selected %s naming column '%s' is not part of the input",
                            m_nodeConfig.getWriterTypeName(), selectedFilenameColumn));
                }
                if (!dataTableSpec.getColumnSpec(filenameColumnIndex).getType().isCompatible(StringValue.class)) {
                    throw new InvalidSettingsException(
                        String.format("The selected %s naming column '%s' is not a string column",
                            m_nodeConfig.getWriterTypeName(), selectedFilenameColumn));
                }
            }
        }
    }

    /**
     * Subclasses must implement this method in order to provide a {@link AbstractMultiTableWriterCellFactory}, which is
     * called from both the <code>execute</code> and the <code>configure</code> method of this node model.
     *
     * <pre>
     * // concrete subclass instantiation of {@link AbstractMultiTableWriterCellFactory}
     * return new ImageColumnsToFilesCellFactory(newColumnsSpec, imgColIdx, fileOverwritePolicy, outputPath,
     *     new UserPatternFileNameGenerator(generatedFilenamePattern));
     * </pre>
     *
     * @param outputColumnSpecs the spec's of the created columns
     * @param srcColIdx index of source column
     * @param overwritePolicy policy how to proceed when output file exists according to {@link FileOverwritePolicy}
     *
     * @return concrete subclass instantiation of {@link AbstractMultiTableWriterCellFactory}
     */
    protected abstract F getFactory(final DataColumnSpec[] outputColumnSpecs, final int srcColIdx,
        final FileOverwritePolicy overwritePolicy);

    private DataColumnSpec[] getNewColumnsSpec(final DataTableSpec spec) {
        final FSLocationSpec location = m_outputLocationSettings.getLocation();

        final var metaData = new FSLocationValueMetaData(location.getFileSystemCategory(),
            location.getFileSystemSpecifier().orElse(null));

        final String newColName = DataTableSpec.getUniqueColumnName(spec, m_columnNameOutputLocation);
        final var fsLocationSpec = new DataColumnSpecCreator(newColName, SimpleFSLocationCellFactory.TYPE);

        final String statusCol = DataTableSpec.getUniqueColumnName(spec, m_columnNameStatus);
        final var statusColumnSpec = new DataColumnSpecCreator(statusCol, StringCell.TYPE);

        fsLocationSpec.addMetaData(metaData, true);

        return new DataColumnSpec[]{fsLocationSpec.createSpec(), statusColumnSpec.createSpec()};
    }

    @Override
    protected BufferedDataTable[] execute(final PortObject[] inObjects, final ExecutionContext exec) throws Exception {
        final BufferedDataTable inputDataTable = (BufferedDataTable)inObjects[m_inputTableIdx];
        final var dataTableSpec = inputDataTable.getDataTableSpec();

        try (final var writePathAccessor = m_outputLocationSettings.createWritePathAccessor()) {
            final FSPath outputPath = writePathAccessor.getOutputPath(m_statusConsumer);
            m_statusConsumer.setWarningsIfRequired(this::setWarningMessage);

            createOutputDirIfRequired(outputPath);
            updateCellFactory(dataTableSpec, outputPath);

            final BufferedDataTable out = exec.createColumnRearrangeTable(inputDataTable, m_columnRearranger, exec);

            if (m_multiFileWriterCellFactory.getMissingCellCount() > 0) {
                setWarningMessage(
                    "Skipped " + m_multiFileWriterCellFactory.getMissingCellCount() + " row(s) due to missing values.");
            }

            return new BufferedDataTable[]{out};
        }

    }

    private void createOutputDirIfRequired(final FSPath outputPath) throws IOException {
        try {
            final BasicFileAttributes outputPathAttrs = Files.readAttributes(outputPath, BasicFileAttributes.class);

            if (!outputPathAttrs.isDirectory()) {
                if (m_outputLocationSettings.isCreateMissingFolders()) {
                    throw new IOException(
                        String.format("There already exits a file with the specified output location name '%s'."
                            + "Please choose a different output location name.", outputPath));
                } else {
                    throw new IOException(
                        String.format("The specified output location '%s' is not refering to a directory. "
                            + "Please specify an output location refering to a directory.", outputPath));
                }
            }
        } catch (NoSuchFileException e) { // NOSONAR can be ignored
            if (m_outputLocationSettings.isCreateMissingFolders()) {
                FSFiles.createDirectories(outputPath);
            } else {
                throw new IOException(String.format(
                    "The directory '%s' does not exist and must not be created due to user settings.", outputPath));
            }
        }
    }

    private void updateCellFactory(final DataTableSpec dataTableSpec, final FSPath outputPath) {
        final var filenameGenerator = createFileNameGenerator(dataTableSpec);

        if (m_compressionSupported) {
            m_multiFileWriterCellFactory.setEnableCompression(m_compressFiles.getBooleanValue());
        }

        m_multiFileWriterCellFactory.setOutputPath(outputPath);
        m_multiFileWriterCellFactory.setFileNameGenerator(filenameGenerator);
        m_multiFileWriterCellFactory.setOverwritePolicy(m_outputLocationSettings.getFileOverwritePolicy());
    }

    private FileNameGenerator createFileNameGenerator(final DataTableSpec dataTableSpec) {
        final FileNameGenerator filenameGenerator;
        if (m_nodeConfig.shouldGenerateFilename()) {
            final var generatedFilenamePattern = m_filenamePattern.getStringValue();
            filenameGenerator = new UserPatternFileNameGenerator(generatedFilenamePattern);
        } else {
            final boolean isRowIdSelected = m_nodeConfig.getFilenameColumn().useRowID();
            if (isRowIdSelected) {
                filenameGenerator = RowIdFileNameGenerator.getInstance();
            } else {
                final int filenameColIdx = dataTableSpec.findColumnIndex(m_filenameColumnSelection.getStringValue());
                filenameGenerator = new NamingColumnFileNameGenerator(filenameColIdx);
            }
        }
        return filenameGenerator;
    }

    @SuppressWarnings("resource")
    @Override
    public StreamableOperator createStreamableOperator(final PartitionInfo partitionInfo,
        final PortObjectSpec[] inSpecs) throws InvalidSettingsException {

        final var dataTableSpec = (DataTableSpec)inSpecs[m_inputTableIdx];
        final WritePathAccessor writePathAccessor = m_outputLocationSettings.createWritePathAccessor(); //NOSONAR

        final FSPath outputPath = writePathAccessor.getOutputPath(m_statusConsumer);

        updateCellFactory(dataTableSpec, outputPath);

        final var wrappedFunction = m_columnRearranger.createStreamableFunction();

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

}
