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
 *   Dec 22, 2020 (Ayaz Ali Qureshi, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.filehandling.utility.nodes.pathtouri;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.uri.URIDataCell;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.context.ports.PortsConfiguration;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.streamable.InputPortRole;
import org.knime.core.node.streamable.OutputPortRole;
import org.knime.core.node.streamable.PartitionInfo;
import org.knime.core.node.streamable.StreamableFunction;
import org.knime.core.node.streamable.StreamableOperator;
import org.knime.filehandling.core.connections.FSConnection;
import org.knime.filehandling.core.defaultnodesettings.status.DefaultStatusMessage;
import org.knime.filehandling.core.defaultnodesettings.status.NodeModelStatusConsumer;
import org.knime.filehandling.core.defaultnodesettings.status.StatusMessage.MessageType;
import org.knime.filehandling.core.port.FileSystemPortObjectSpec;
import org.knime.filehandling.core.util.FSLocationColumnUtils;
import org.knime.filehandling.core.util.FSMissingMetadataException;

/**
 * The node model allowing to convert paths to URIs.
 *
 * @author Ayaz Ali Qureshi, KNIME GmbH, Berlin, Germany
 */
final class PathToUriNodeModel extends NodeModel {

    private final int m_dataTablePortIndex;

    private final int m_fileSystemPortIndex;

    private final PathToUriNodeConfig m_config;

    private final URIExporterModelHelper m_modelHelper;

    private final NodeModelStatusConsumer m_statusConsumer =
            new NodeModelStatusConsumer(EnumSet.of(MessageType.WARNING));

    PathToUriNodeModel(final PortsConfiguration portsConfig, final PathToUriNodeConfig nodeSettings) {
        super(portsConfig.getInputPorts(), portsConfig.getOutputPorts());
        m_config = nodeSettings;
        m_dataTablePortIndex = nodeSettings.getDataTablePortIndex();
        m_fileSystemPortIndex = nodeSettings.getFileSystemConnectionPortIndex();
        m_modelHelper = nodeSettings.getExporterModelHelper();
    }

    @Override
    protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        // Nothing to do here
    }

    @Override
    protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        // Nothing to do here
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_config.saveSettingsForModel(settings);
    }

    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_config.validateSettingsForModel(settings);
    }

    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_config.loadValidatedSettingsForModel(settings);
    }

    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        final DataTableSpec inputTableSpec = (DataTableSpec)inSpecs[m_dataTablePortIndex];

        // validate the output column settings
        validateOutputColumnSettings(inSpecs);

        // validate the path column and uri exporter settings
        m_modelHelper.setPortObjectSpecs(inSpecs);
        try {
            m_modelHelper.validate(m_statusConsumer, false);
            m_statusConsumer.setWarningsIfRequired(this::setWarningMessage);

            try (final PathToUriCellFactory cellFactory = createCellFactory(inSpecs)) {
                return new DataTableSpec[]{createColumnRearranger(inputTableSpec, cellFactory).createSpec()};
            }
        } catch (FSMissingMetadataException ex) {
            // AP-17965: ignore missing meta data
            setWarningMessage(ex.getMessage());
            return new PortObjectSpec[]{null};
        }
    }

    private void validateOutputColumnSettings(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        final DataTableSpec inSpec = (DataTableSpec)inSpecs[m_dataTablePortIndex];

        if (m_config.shouldAppendColumn()) {
            // Is column name empty?
            if (StringUtils.isBlank(m_config.getAppendedColumnName())) {
                throw new InvalidSettingsException("The name of the column to create cannot be empty");
            }
            if (inSpec.containsName(m_config.getAppendedColumnName())) {
                m_statusConsumer.accept(
                    DefaultStatusMessage.mkWarning("The name of the column to create is already taken, using '%s' instead.",
                        getUniqueColumnName(inSpec, m_config.getAppendedColumnName())));
            }
        }
    }

    @Override
    protected PortObject[] execute(final PortObject[] inObjects, final ExecutionContext exec) throws Exception {

        final PortObjectSpec[] inSpecs = Arrays.stream(inObjects)//
            .map(PortObject::getSpec)//
            .toArray(PortObjectSpec[]::new);

        // check if the column spec matches the connected files-system
        Optional<String> warningMsg = FSLocationColumnUtils.validateFSLocationColumn(m_modelHelper.getPathColumnSpec(),
            m_fileSystemPortIndex >= 0 ? (FileSystemPortObjectSpec)inSpecs[m_fileSystemPortIndex] : null);
        warningMsg.ifPresent(this::setWarningMessage);

        try (final PathToUriCellFactory cellFactory = createCellFactory(inSpecs)) {
            final BufferedDataTable outputBufferTable =
                exec.createColumnRearrangeTable((BufferedDataTable)inObjects[m_dataTablePortIndex],
                    createColumnRearranger(m_modelHelper.getDataTableSpec(), cellFactory), exec);

            return new PortObject[]{outputBufferTable};
        }
    }

    @SuppressWarnings("resource")
    private PathToUriCellFactory createCellFactory(final PortObjectSpec[] inSpecs) {
        final DataTableSpec inputTableSpec = m_modelHelper.getDataTableSpec();
        final int pathColIndx = inputTableSpec.findColumnIndex(m_config.getPathColumnName());
        final FSConnection fsConnection =
            FileSystemPortObjectSpec.getFileSystemConnection(inSpecs, m_fileSystemPortIndex).orElse(null);

        return new PathToUriCellFactory(pathColIndx, //
            getNewColumnSpec(inputTableSpec), //
            fsConnection, //
            m_config.getExporterModelHelper(), //
            m_config.failIfPathNotExists(), //
            m_config.failOnMissingValues());
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

    @SuppressWarnings("resource")
    @Override
    public StreamableOperator createStreamableOperator(final PartitionInfo partitionInfo,
        final PortObjectSpec[] inSpecs) throws InvalidSettingsException {

        final DataTableSpec tableSpec = (DataTableSpec)inSpecs[m_dataTablePortIndex];
        final PathToUriCellFactory cellFactory = createCellFactory(inSpecs);
        final StreamableFunction wrappedFunction = createColumnRearranger(tableSpec, cellFactory).createStreamableFunction();

        return new StreamableFunction(m_dataTablePortIndex, 0) {

            @Override
            public void init(final ExecutionContext ctx) throws Exception {
                wrappedFunction.init(ctx);
            }

            @Override
            public void finish() {
                wrappedFunction.finish();
                cellFactory.close();
            }

            @Override
            public DataRow compute(final DataRow input) throws Exception {
                return wrappedFunction.compute(input);
            }
        };
    }

    /**
     * Returns an instance of ColumnRearranger which appends the new column from PathToUrlCellFactory
     *
     * @param inSpec An array of PortObjectSpec
     * @param factory A object of {@link PathToUriCellFactory}
     * @return An instance of ColumnRearranger
     */
    private ColumnRearranger createColumnRearranger(final DataTableSpec inSpec, final PathToUriCellFactory factory) {

        final ColumnRearranger rearranger = new ColumnRearranger(inSpec);
        // Either replace or append a column depending on users choice
        if (!m_config.shouldAppendColumn()) {
            rearranger.replace(factory, m_config.getPathColumnName());
        } else {
            rearranger.append(factory);
        }
        return rearranger;
    }

    private DataColumnSpec getNewColumnSpec(final DataTableSpec inSpec) {

        final String columnName = m_config.shouldAppendColumn() //
                ? getUniqueColumnName(inSpec, m_config.getAppendedColumnName())
                : m_config.getPathColumnName();
        return new DataColumnSpecCreator(columnName, URIDataCell.TYPE).createSpec();
    }

    /**
     * Return a new unique string based on the input spec, so a new column spec can be created
     *
     * @param inputSpec A DataTableSpec object
     * @param String The suggested name for the column
     * @return A unique String for creating a new column spec
     */
    private static String getUniqueColumnName(final DataTableSpec inputSpec, final String inputColName) {
        return DataTableSpec.getUniqueColumnName(inputSpec, inputColName);
    }

    @Override
    protected void reset() {
        // Nothing to do here
    }
}
