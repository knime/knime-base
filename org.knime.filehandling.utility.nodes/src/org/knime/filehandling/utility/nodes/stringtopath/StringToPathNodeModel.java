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
 *   Jul 28, 2020 (Timmo Waller-Ehrat, KNIME GmbH, Konstanz, Germany): created
 */

package org.knime.filehandling.utility.nodes.stringtopath;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.util.EnumSet;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.MissingValue;
import org.knime.core.data.MissingValueException;
import org.knime.core.data.StringValue;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.container.SingleCellFactory;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.context.ports.PortsConfiguration;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.streamable.InputPortRole;
import org.knime.core.node.streamable.OutputPortRole;
import org.knime.core.node.streamable.PartitionInfo;
import org.knime.core.node.streamable.PortInput;
import org.knime.core.node.streamable.PortOutput;
import org.knime.core.node.streamable.RowInput;
import org.knime.core.node.streamable.RowOutput;
import org.knime.core.node.streamable.StreamableFunction;
import org.knime.core.node.streamable.StreamableOperator;
import org.knime.core.node.util.ButtonGroupEnumInterface;
import org.knime.core.node.util.CheckUtils;
import org.knime.filehandling.core.connections.FSCategory;
import org.knime.filehandling.core.connections.FSFiles;
import org.knime.filehandling.core.connections.FSLocation;
import org.knime.filehandling.core.connections.FSLocationFactory;
import org.knime.filehandling.core.connections.FSLocationSpec;
import org.knime.filehandling.core.connections.FSPath;
import org.knime.filehandling.core.connections.location.FSPathProvider;
import org.knime.filehandling.core.connections.location.FSPathProviderFactory;
import org.knime.filehandling.core.data.location.FSLocationValueMetaData;
import org.knime.filehandling.core.data.location.cell.SimpleFSLocationCellFactory;
import org.knime.filehandling.core.defaultnodesettings.filesystemchooser.SettingsModelFileSystem;
import org.knime.filehandling.core.defaultnodesettings.status.NodeModelStatusConsumer;
import org.knime.filehandling.core.defaultnodesettings.status.StatusMessage.MessageType;

/**
 * The NodeModel for the "String to Path" Node.
 *
 * @author Timmo Waller-Ehrat, KNIME GmbH, Konstanz, Germany
 */
final class StringToPathNodeModel extends NodeModel {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(StringToPathNodeModel.class);

    static final String CONNECTION_INPUT_PORT_GRP_NAME = "File System Connection";

    static final String DATA_TABLE_INPUT_PORT_GRP_NAME = "Data Table";

    private static final int DATA_TABLE_OUTPUT_IDX = 0;

    private static final String CFG_FILE_SYSTEM = "file_system";

    private static final String CFG_SELECTED_COLUMN_NAME = "selected_column_name";

    private static final String CFG_GENERATED_COLUMN_MODE = "generated_column_mode";

    private static final String CFG_APPENDED_COLUMN_NAME = "appended_column_name";

    private static final String CFG_ABORT_ON_MISSING_FILE = "fail_on_missing_file_folder";

    private static final String CFG_FAIL_ON_MISSING_VALS = "fail_on_missing_values";

    private final SettingsModelFileSystem m_fileSystemModel;

    private final SettingsModelString m_selectedColumnNameModel = createSettingsModelColumnName();

    private final SettingsModelString m_generatedColumnModeModel = createSettingsModelColumnMode();

    private final SettingsModelString m_appendedColumnNameModel = createSettingsModelAppendedColumnName();

    private final SettingsModelBoolean m_abortOnMissingFileModel = createSettingsModelAbortOnMissingFile();

    private final SettingsModelBoolean m_failOnMissingValues = createSettingsModelFailOnMissingValues();

    private final NodeModelStatusConsumer m_statusConsumer;

    private final int m_dataTablePortIndex;

    static SettingsModelFileSystem createSettingsModelFileSystem(final PortsConfiguration portsConfig) {
        return new SettingsModelFileSystem(CFG_FILE_SYSTEM, portsConfig,
            StringToPathNodeModel.CONNECTION_INPUT_PORT_GRP_NAME, EnumSet.allOf(FSCategory.class));
    }

    static SettingsModelString createSettingsModelColumnName() {
        return new SettingsModelString(CFG_SELECTED_COLUMN_NAME, null);
    }

    static SettingsModelString createSettingsModelColumnMode() {
        return new SettingsModelString(CFG_GENERATED_COLUMN_MODE, GenerateColumnMode.APPEND_NEW.getActionCommand());
    }

    static SettingsModelString createSettingsModelAppendedColumnName() {
        return new SettingsModelString(CFG_APPENDED_COLUMN_NAME, "Path");
    }

    static SettingsModelBoolean createSettingsModelAbortOnMissingFile() {
        return new SettingsModelBoolean(CFG_ABORT_ON_MISSING_FILE, false);
    }

    static SettingsModelBoolean createSettingsModelFailOnMissingValues() {
        return new SettingsModelBoolean(CFG_FAIL_ON_MISSING_VALS, true);
    }

    StringToPathNodeModel(final PortsConfiguration portsConfig) {
        super(portsConfig.getInputPorts(), portsConfig.getOutputPorts());
        m_dataTablePortIndex = portsConfig.getInputPortLocation().get(DATA_TABLE_INPUT_PORT_GRP_NAME)[0];
        m_statusConsumer = new NodeModelStatusConsumer(EnumSet.of(MessageType.ERROR, MessageType.INFO));
        m_fileSystemModel = createSettingsModelFileSystem(portsConfig);
    }

    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        final DataTableSpec inSpec = (DataTableSpec)inSpecs[m_dataTablePortIndex];
        // auto-guessing
        if (m_selectedColumnNameModel.getStringValue() == null) {
            autoGuess(inSpec);
            setWarningMessage(
                String.format("Auto-guessed column to convert '%s'", m_selectedColumnNameModel.getStringValue()));
        }
        // validate the settings
        validateSettings(inSpecs);

        // create output spec
        try (final StringToPathCellFactory factory = createStringPathCellFactory(inSpec, false)) {
            final ColumnRearranger rearranger = createColumnRearranger(inSpec, factory);
            return new PortObjectSpec[]{rearranger.createSpec()};
        }
    }

    private void autoGuess(final DataTableSpec inSpec) throws InvalidSettingsException {
        m_selectedColumnNameModel.setStringValue(inSpec.stream()//
            .filter(dcs -> dcs.getType().isCompatible(StringValue.class))//
            .map(DataColumnSpec::getName)//
            .findFirst()//
            .orElseThrow(() -> new InvalidSettingsException("No applicable column available"))//
        );
    }

    private void validateSettings(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        final DataTableSpec inSpec = (DataTableSpec)inSpecs[m_dataTablePortIndex];
        final String pathColName = m_selectedColumnNameModel.getStringValue();
        final int colIndex = inSpec.findColumnIndex(pathColName);

        // check column existence
        CheckUtils.checkSetting(colIndex >= 0, "The selected column '%s' is not part of the input", pathColName);

        // check column type
        final DataColumnSpec pathColSpec = inSpec.getColumnSpec(colIndex);
        if (!pathColSpec.getType().isCompatible(StringValue.class)) {
            throw new InvalidSettingsException(
                String.format("The selected column '%s' has the wrong type", pathColName));
        }

        if (isAppendMode(m_generatedColumnModeModel)) {
            // Is column name empty?
            if (m_appendedColumnNameModel.getStringValue() == null
                || m_appendedColumnNameModel.getStringValue().trim().isEmpty()) {
                throw new InvalidSettingsException("The name of the column to create cannot be empty");
            }
            if (inSpec.containsName(m_appendedColumnNameModel.getStringValue())) {
                setWarningMessage(
                    String.format("The name of the column to create is already taken, using '%s' instead.",
                        getUniqueColumnName(inSpec)));
            }
        }
        m_fileSystemModel.configureInModel(inSpecs, m_statusConsumer);
        m_statusConsumer.setWarningsIfRequired(this::setWarningMessage);
    }

    private String getUniqueColumnName(final DataTableSpec inSpec) {
        return DataTableSpec.getUniqueColumnName(inSpec, m_appendedColumnNameModel.getStringValue());
    }

    @Override
    protected BufferedDataTable[] execute(final PortObject[] data, final ExecutionContext exec) throws Exception {
        final BufferedDataTable inTable = (BufferedDataTable)data[m_dataTablePortIndex];
        final DataTableSpec inSpec = inTable.getDataTableSpec();
        try (final StringToPathCellFactory factory = createStringPathCellFactory(inSpec, true)) {
            final BufferedDataTable out =
                exec.createColumnRearrangeTable(inTable, createColumnRearranger(inSpec, factory), exec);
            return new BufferedDataTable[]{out};
        }
    }

    @Override
    public InputPortRole[] getInputPortRoles() {
        // assumes that the order is (dynamic) FS connection port before data table input port
        return m_dataTablePortIndex == 0 //
            ? new InputPortRole[]{InputPortRole.DISTRIBUTED_STREAMABLE}//
            : new InputPortRole[]{InputPortRole.NONDISTRIBUTED_NONSTREAMABLE, InputPortRole.DISTRIBUTED_STREAMABLE};
    }

    @Override
    public OutputPortRole[] getOutputPortRoles() {
        return new OutputPortRole[]{OutputPortRole.DISTRIBUTED};
    }

    @Override
    public StreamableOperator createStreamableOperator(final PartitionInfo partitionInfo,
        final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        return new StringToPathStreamableOperator();

    }

    /**
     * Create a {@link ColumnRearranger} that either replaces the selected column with its Path counterpart, or appends
     * a new column.
     *
     * @param inSpec specification of the input table
     * @param factory the {@link StringToPathCellFactory}
     * @return {@link ColumnRearranger} that will append a new column or replace the selected column
     */
    private ColumnRearranger createColumnRearranger(final DataTableSpec inSpec, final StringToPathCellFactory factory) {
        final ColumnRearranger rearranger = new ColumnRearranger(inSpec);
        // Either replace or append a column depending on users choice
        if (isReplaceMode(m_generatedColumnModeModel)) {
            rearranger.replace(factory, m_selectedColumnNameModel.getStringValue());
        } else {
            rearranger.append(factory);
        }
        return rearranger;
    }

    private StringToPathCellFactory createStringPathCellFactory(final DataTableSpec inSpec, final boolean isExecute) {
        final int colIdx = inSpec.findColumnIndex(m_selectedColumnNameModel.getStringValue());
        DataColumnSpec colSpec = getNewColumnSpec(inSpec);
        return new StringToPathCellFactory(colSpec, colIdx, isExecute);
    }

    private DataColumnSpec getNewColumnSpec(final DataTableSpec inSpec) {
        final String columnName = isReplaceMode(m_generatedColumnModeModel) ? m_selectedColumnNameModel.getStringValue()
            : getUniqueColumnName(inSpec);
        final FSLocationSpec location = m_fileSystemModel.getLocationSpec();
        final FSLocationValueMetaData metaData = new FSLocationValueMetaData(location.getFileSystemCategory(),
            location.getFileSystemSpecifier().orElse(null));
        final DataColumnSpecCreator fsLocationSpec = new DataColumnSpecCreator(columnName, SimpleFSLocationCellFactory.TYPE);
        fsLocationSpec.addMetaData(metaData, true);
        return fsLocationSpec.createSpec();
    }

    @Override
    protected void reset() {
        // Nothing to do here
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_fileSystemModel.saveSettingsTo(settings);
        m_selectedColumnNameModel.saveSettingsTo(settings);
        m_abortOnMissingFileModel.saveSettingsTo(settings);
        m_failOnMissingValues.saveSettingsTo(settings);
        m_appendedColumnNameModel.saveSettingsTo(settings);
        m_generatedColumnModeModel.saveSettingsTo(settings);
    }

    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        if (!isOldConnectedSettings(settings)) {
            m_fileSystemModel.loadSettingsFrom(settings);
        }
        m_selectedColumnNameModel.loadSettingsFrom(settings);
        m_abortOnMissingFileModel.loadSettingsFrom(settings);
        m_failOnMissingValues.loadSettingsFrom(settings);
        m_appendedColumnNameModel.loadSettingsFrom(settings);
        m_generatedColumnModeModel.loadSettingsFrom(settings);
    }

    /**
     * In 4.2 the settings were hidden using {@link SettingsModel#CFGKEY_INTERNAL} as suffix. However, that was a bug
     * because this name is used by the framework to store the enabled status and settings model id. In 4.3 we removed
     * the suffix because it also causes problems when adding/removing the file system port. Therefore we don't
     * validate/load the settings in the case we have a fs port AND the settings are old. That's save because in the
     * connected case nothing is loaded anyway.
     *
     * @param settings to validate/load
     * @return {@code true} if the settings are old
     */
    private static boolean isOldConnectedSettings(final NodeSettingsRO settings) {
        return settings.containsKey(CFG_FILE_SYSTEM + SettingsModel.CFGKEY_INTERNAL + SettingsModel.CFGKEY_INTERNAL);
    }

    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        if (!isOldConnectedSettings(settings)) {
            m_fileSystemModel.validateSettings(settings);
        }
        m_selectedColumnNameModel.validateSettings(settings);
        m_abortOnMissingFileModel.validateSettings(settings);
        m_failOnMissingValues.validateSettings(settings);
        m_appendedColumnNameModel.validateSettings(settings);
        m_generatedColumnModeModel.validateSettings(settings);
    }

    @Override
    protected void loadInternals(final File internDir, final ExecutionMonitor exec) {
        // Nothing to do here
    }

    @Override
    protected void saveInternals(final File internDir, final ExecutionMonitor exec) {
        // Nothing to do here
    }

    private class StringToPathCellFactory extends SingleCellFactory implements Closeable {

        private final int m_colIdx;

        private final FSLocationFactory m_fsLocationFactory;

        private final SimpleFSLocationCellFactory m_fsLocationCellFactory;

        private final FSPathProviderFactory m_fsPathProviderFactory;

        /**
         * Cell factory to convert cells from String to Path
         *
         * @param newColSpec the spec for the new column
         * @param colIdx the index of the selected column
         */
        StringToPathCellFactory(final DataColumnSpec newColSpec, final int colIdx, final boolean isExecute) {
            super(newColSpec);
            m_colIdx = colIdx;
            final FSLocationSpec locationSpec = m_fileSystemModel.getLocationSpec();
            m_fsLocationCellFactory = new SimpleFSLocationCellFactory(locationSpec);
            if (isExecute) {
                m_fsLocationFactory = m_fileSystemModel.createFSLocationFactory();
                if (m_abortOnMissingFileModel.getBooleanValue()) {
                    m_fsPathProviderFactory =
                        FSPathProviderFactory.newFactory(m_fileSystemModel.getConnection(), locationSpec);
                } else {
                    m_fsPathProviderFactory = null;
                }
            } else {
                m_fsLocationFactory = null;
                m_fsPathProviderFactory = null;
            }
        }

        @Override
        public DataCell getCell(final DataRow row) {
            return createPathCell(row);
        }

        private DataCell createPathCell(final DataRow row) {
            DataCell pathCell;
            final DataCell tempCell = row.getCell(m_colIdx);

            if (!tempCell.isMissing()) {
                final String cellValue = ((StringValue)tempCell).getStringValue();

                CheckUtils.checkArgument(cellValue != null && !cellValue.trim().isEmpty(),
                    "Selected column contains empty string(s).");

                final FSLocation fsLocation = m_fsLocationFactory.createLocation(cellValue);

                if (m_fsPathProviderFactory != null) {
                    checkIfFileExists(fsLocation);
                }

                pathCell = m_fsLocationCellFactory.createCell(fsLocation);

            } else {
                if (m_failOnMissingValues.getBooleanValue()) {
                    throw new MissingValueException((MissingValue)tempCell,
                        "Selected column contains missing cell(s).");
                }
                pathCell = DataType.getMissingCell();
            }

            return pathCell;
        }

        private void checkIfFileExists(final FSLocation fsLocation) {
            try (final FSPathProvider pathProvider = m_fsPathProviderFactory.create(fsLocation)) {
                final FSPath fsPath = pathProvider.getPath();
                if (!FSFiles.exists(fsPath)) {
                    throw new IllegalArgumentException(
                        String.format("The file/folder '%s' does not exist", fsLocation.getPath()));
                }
            } catch (final AccessDeniedException e) {
                throw new IllegalArgumentException(
                    String.format("The file/folder '%s' cannot be accessed", fsLocation.getPath()), e);
            } catch (final IOException e) {
                throw new IllegalArgumentException(
                    String.format("The file/folder '%s' does not exist or cannot be accessed", fsLocation.getPath()),
                    e);
            }
        }

        @Override
        public void close() {
            // we can catch those exceptions as they cannot occur and even if we don't need to care about them here
            if (m_fsLocationFactory != null) {
                try {
                    m_fsLocationFactory.close();
                } catch (final IOException e) {
                    LOGGER.debug("Unable to close fs location factory", e);
                }
            }
            if (m_fsPathProviderFactory != null) {
                try {
                    m_fsPathProviderFactory.close();
                } catch (final IOException e) {
                    LOGGER.debug("Unable to close fs path factory", e);
                }
            }
        }
    }

    /**
     * Inner class allowing the node to be executed in streaming mode.
     *
     * @author Mark Ortmann, KNIME GmbH, Berlin, Germany
     */
    private class StringToPathStreamableOperator extends StreamableOperator {

        @Override
        public void runFinal(final PortInput[] inputs, final PortOutput[] outputs, final ExecutionContext exec)
            throws Exception {
            final RowInput in = (RowInput)inputs[m_dataTablePortIndex];
            final RowOutput out = (RowOutput)outputs[DATA_TABLE_OUTPUT_IDX];
            final DataTableSpec inSpec = in.getDataTableSpec();
            try (StringToPathCellFactory factory = createStringPathCellFactory(inSpec, true)) {
                final StreamableFunction streamableFunction =
                    createColumnRearranger(inSpec, factory).createStreamableFunction();
                DataRow row;
                while ((row = in.poll()) != null) {
                    out.push(streamableFunction.compute(row));
                }
            } finally {
                in.close();
                out.close();
            }
        }
    }

    /**
     * Options used to decide whether the newly generated column should replace selected column or should be appended as
     * new.
     *
     * @author Temesgen H. Dadi, KNIME GmbH, Berlin, Germany
     */
    enum GenerateColumnMode implements ButtonGroupEnumInterface {

            APPEND_NEW("Append column:"), //
            REPLACE_SELECTED("Replace selected column");

        private static final String TOOLTIP =
            "<html>The newly generated column should replace selected column or should be appended as new.";

        private final String m_text;

        GenerateColumnMode(final String text) {
            this.m_text = text;
        }

        @Override
        public String getText() {
            return m_text;
        }

        @Override
        public String getActionCommand() {
            return name();
        }

        @Override
        public String getToolTip() {
            return TOOLTIP;
        }

        @Override
        public boolean isDefault() {
            return this == APPEND_NEW;
        }
    }

    /**
     * A convenience method to check if a SettingsModelString value is {@code generatedColumnMode.APPEND_NEW}
     *
     * @param settingsModel the settings model to check
     * @return {@code true} if and only if the generatedColumnMode is APPEND_NEW
     */
    static boolean isAppendMode(final SettingsModelString settingsModel) {
        return GenerateColumnMode.valueOf(settingsModel.getStringValue()) == GenerateColumnMode.APPEND_NEW;
    }

    /**
     * A convenience method to check if a SettingsModelString value is {@code generatedColumnMode.REPLACE_SELECTED}
     *
     * @param settingsModel the settings model to check
     * @return {@code true} if and only if the generatedColumnMode is {@code generatedColumnMode.REPLACE_SELECTED}
     */
    static boolean isReplaceMode(final SettingsModelString settingsModel) {
        return GenerateColumnMode.valueOf(settingsModel.getStringValue()) == GenerateColumnMode.REPLACE_SELECTED;
    }
}
