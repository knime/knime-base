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
 *   Oct 21, 2020 (Mark Ortmann, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.filehandling.utility.nodes.pathtostring;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Optional;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.container.SingleCellFactory;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.def.StringCell.StringCellFactory;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
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
import org.knime.filehandling.core.connections.FSLocation;
import org.knime.filehandling.core.connections.location.FSPathProviderFactory;
import org.knime.filehandling.core.connections.uriexport.URIExporter;
import org.knime.filehandling.core.connections.uriexport.URIExporterIDs;
import org.knime.filehandling.core.data.location.FSLocationValue;
import org.knime.filehandling.core.data.location.FSLocationValueMetaData;
import org.knime.filehandling.core.data.location.cell.FSLocationCell;

/**
 * This node allows you to convert a {@link FSLocationCell} to a {@link StringCell}. Depending on the settings, the
 * {@link StringCell} will contain the value returned by {@link FSLocation#getPath()} stored in the
 * {@link FSLocationCell} or the string value of the {@link URI} returned by the {@link URIExporter} with id
 * {@link URIExporterIDs#LEGACY_KNIME_URL}.
 *
 * @author Mark Ortmann, KNIME GmbH, Berlin, Germany
 */
final class PathToStringNodeModel extends NodeModel {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(PathToStringNodeModel.class);

    private static final String CFG_SELECTED_COLUMN_NAME = "selected_column_name";

    private static final String CFG_GENERATED_COLUMN_MODE = "generated_column_mode";

    private static final String CFG_APPENDED_COLUMN_NAME = "appended_column_name";

    private static final String CFG_CREATE_KNIME_URL = "create_knime_url";

    private final SettingsModelString m_selectedColumn = createSettingsModelColumnName();

    private final SettingsModelString m_columnMode = createSettingsModelColumnMode();

    private final SettingsModelString m_appendColumnName = createSettingsModelAppendedColumnName();

    private final SettingsModelBoolean m_createKNIMEUrl = createSettingsModelCreateKNIMEUrl();

    static SettingsModelString createSettingsModelColumnName() {
        return new SettingsModelString(CFG_SELECTED_COLUMN_NAME, null);
    }

    static SettingsModelString createSettingsModelColumnMode() {
        return new SettingsModelString(CFG_GENERATED_COLUMN_MODE, GenerateColumnMode.APPEND_NEW.getActionCommand());
    }

    static SettingsModelString createSettingsModelAppendedColumnName() {
        return new SettingsModelString(CFG_APPENDED_COLUMN_NAME, "Location");
    }

    static SettingsModelBoolean createSettingsModelCreateKNIMEUrl() {
        return new SettingsModelBoolean(CFG_CREATE_KNIME_URL, true);
    }

    PathToStringNodeModel() {
        super(new PortType[]{BufferedDataTable.TYPE}, new PortType[]{BufferedDataTable.TYPE});
    }

    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {
        final DataTableSpec inSpec = inSpecs[0];
        // auto-guessing
        if (m_selectedColumn.getStringValue() == null) {
            autoGuess(inSpecs);
            setWarningMessage(String.format("Auto-guessed column to convert '%s'", m_selectedColumn.getStringValue()));
        }
        // validate
        validateSettings(inSpec);
        // create output spec
        try (final PathToStringCellFactory factory = createPathToStringCellFactory(inSpec)) {
            final ColumnRearranger rearranger = createColumnRearranger(inSpec, factory);
            return new DataTableSpec[]{rearranger.createSpec()};
        }
    }

    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] data, final ExecutionContext exec)
        throws Exception {
        final BufferedDataTable inTable = data[0];
        final DataTableSpec inSpec = inTable.getDataTableSpec();
        try (final PathToStringCellFactory factory = createPathToStringCellFactory(inSpec)) {
            final BufferedDataTable out =
                exec.createColumnRearrangeTable(inTable, createColumnRearranger(inSpec, factory), exec);
            return new BufferedDataTable[]{out};
        }
    }

    private void autoGuess(final DataTableSpec[] inSpecs) throws InvalidSettingsException {
        final DataTableSpec inputTableSpec = inSpecs[0];
        m_selectedColumn.setStringValue(inputTableSpec.stream()//
            .filter(dcs -> dcs.getType().isCompatible(FSLocationValue.class))//
            .map(DataColumnSpec::getName)//
            .findFirst()//
            .orElseThrow(() -> new InvalidSettingsException("No applicable column available"))//
        );
    }

    private void validateSettings(final DataTableSpec inSpec) throws InvalidSettingsException {
        final String pathColName = m_selectedColumn.getStringValue();
        final int colIndex = inSpec.findColumnIndex(pathColName);

        // check column existence
        CheckUtils.checkSetting(colIndex >= 0, "The selected column '%s' is not part of the input", pathColName);

        // check column type
        final DataColumnSpec pathColSpec = inSpec.getColumnSpec(colIndex);
        if (!pathColSpec.getType().isCompatible(FSLocationValue.class)) {
            throw new InvalidSettingsException(
                String.format("The selected column '%s' has the wrong type", pathColName));
        }

        if (isAppendMode()) {
            // Is column name empty?
            if (m_appendColumnName.getStringValue() == null || m_appendColumnName.getStringValue().trim().isEmpty()) {
                throw new InvalidSettingsException("The name of the column to create cannot be empty");
            }
            if (inSpec.containsName(m_appendColumnName.getStringValue())) {
                setWarningMessage(
                    String.format("The name of the column to create is already taken, using '%s' instead.",
                        getUniqueColumnName(inSpec)));
            }
        }
    }

    private String getUniqueColumnName(final DataTableSpec inSpec) {
        return DataTableSpec.getUniqueColumnName(inSpec, m_appendColumnName.getStringValue());
    }

    private ColumnRearranger createColumnRearranger(final DataTableSpec spec, final PathToStringCellFactory factory) {
        final ColumnRearranger rearranger = new ColumnRearranger(spec);
        final int colIdx = spec.findColumnIndex(m_selectedColumn.getStringValue());
        if (isAppendMode()) {
            rearranger.append(factory);
        } else {
            rearranger.replace(factory, colIdx);
        }
        return rearranger;
    }

    private PathToStringCellFactory createPathToStringCellFactory(final DataTableSpec inSpec) {
        final int colIdx = inSpec.findColumnIndex(m_selectedColumn.getStringValue());
        final DataColumnSpec colSpec = createNewSpec(inSpec);
        return new PathToStringCellFactory(colSpec, colIdx, inSpec.getColumnSpec(colIdx));
    }

    private DataColumnSpec createNewSpec(final DataTableSpec inSpec) {
        final String columnName = isAppendMode() ? getUniqueColumnName(inSpec) : m_selectedColumn.getStringValue();
        final DataColumnSpecCreator fsLocationSpec = new DataColumnSpecCreator(columnName, StringCellFactory.TYPE);
        return fsLocationSpec.createSpec();
    }

    boolean isAppendMode() {
        return isAppendMode(m_columnMode);
    }

    @Override
    public InputPortRole[] getInputPortRoles() {
        return new InputPortRole[]{InputPortRole.DISTRIBUTED_STREAMABLE};
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

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_selectedColumn.saveSettingsTo(settings);
        m_columnMode.saveSettingsTo(settings);
        m_appendColumnName.saveSettingsTo(settings);
        m_createKNIMEUrl.saveSettingsTo(settings);
    }

    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_selectedColumn.validateSettings(settings);
        m_columnMode.validateSettings(settings);
        m_appendColumnName.validateSettings(settings);
        m_createKNIMEUrl.validateSettings(settings);
    }

    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_selectedColumn.loadSettingsFrom(settings);
        m_columnMode.loadSettingsFrom(settings);
        m_appendColumnName.loadSettingsFrom(settings);
        m_createKNIMEUrl.loadSettingsFrom(settings);
    }

    @Override
    protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        // nothing to do
    }

    @Override
    protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        // nothing to do
    }

    @Override
    protected void reset() {
        // nothing to do
    }

    private class PathToStringCellFactory extends SingleCellFactory implements AutoCloseable {

        private final int m_colIdx;

        private final FSPathProviderFactory m_fsPathProviderFactory;

        public PathToStringCellFactory(final DataColumnSpec newColSpec, final int colIdx,
            final DataColumnSpec fsLocationColSpec) {
            super(newColSpec);
            m_colIdx = colIdx;
            m_fsPathProviderFactory = getFSPathProviderFactory(fsLocationColSpec);
        }

        private FSPathProviderFactory getFSPathProviderFactory(final DataColumnSpec colSpec) {
            if (m_createKNIMEUrl.getBooleanValue()) {
                final FSLocationValueMetaData fsLocationValueMetaData = colSpec
                    .getMetaDataOfType(FSLocationValueMetaData.class).orElseThrow(() -> new IllegalStateException(
                        String.format("Path column '%s' does not contain meta data.", colSpec.getName())));
                final FSCategory fsCategory = fsLocationValueMetaData.getFSCategory();
                if (fsCategory == FSCategory.RELATIVE || fsCategory == FSCategory.MOUNTPOINT) {
                    return FSPathProviderFactory.newFactory(Optional.empty(), fsLocationValueMetaData);
                }
            }
            return null;
        }

        @Override
        public DataCell getCell(final DataRow row) {
            final DataCell c = row.getCell(m_colIdx);
            if (c.isMissing()) {
                return DataType.getMissingCell();
            }
            final FSLocation fsLocation = ((FSLocationValue)c).getFSLocation();
            return StringCellFactory.create(m_fsPathProviderFactory == null ? fsLocation.getPath()
                : PathToStringUtils.fsLocationToString(fsLocation, m_fsPathProviderFactory));
        }

        @Override
        public void close() {
            if (m_fsPathProviderFactory != null) {
                try {
                    m_fsPathProviderFactory.close();
                } catch (final IOException e) {
                    LOGGER.debug("Unable to close fs path provider factory.", e);
                }
            }
        }

    }

    /**
     * Inner class allowing the node to be executed in streaming mode.
     *
     * @author Simon Schmid, KNIME GmbH, Konstanz, Germany
     */
    private class StringToPathStreamableOperator extends StreamableOperator {

        @Override
        public void runFinal(final PortInput[] inputs, final PortOutput[] outputs, final ExecutionContext exec)
            throws Exception {
            final RowInput in = (RowInput)inputs[0];
            final RowOutput out = (RowOutput)outputs[0];
            final DataTableSpec inSpec = in.getDataTableSpec();
            try (final PathToStringCellFactory factory = createPathToStringCellFactory(inSpec)) {
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
            "Choose whether a new column should be appended with the given name or the selected one be replaced.";

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

}
