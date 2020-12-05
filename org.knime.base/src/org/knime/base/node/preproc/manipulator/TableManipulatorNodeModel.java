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
 *   Nov 4, 2020 (Tobias): created
 */
package org.knime.base.node.preproc.manipulator;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.knime.base.node.preproc.manipulator.framework.RowInputTableReader;
import org.knime.base.node.preproc.manipulator.mapping.DataTypeTypeHierarchy;
import org.knime.base.node.preproc.manipulator.mapping.DataValueReadAdapterFactory;
import org.knime.base.node.preproc.manipulator.table.DataTableBackedBoundedTable;
import org.knime.base.node.preproc.manipulator.table.EmptyTable;
import org.knime.base.node.preproc.manipulator.table.RowInputBackedTable;
import org.knime.base.node.preproc.manipulator.table.Table;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValue;
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
import org.knime.core.node.streamable.InputPortRole;
import org.knime.core.node.streamable.OutputPortRole;
import org.knime.core.node.streamable.PartitionInfo;
import org.knime.core.node.streamable.PortInput;
import org.knime.core.node.streamable.PortOutput;
import org.knime.core.node.streamable.RowInput;
import org.knime.core.node.streamable.RowOutput;
import org.knime.core.node.streamable.StreamableOperator;
import org.knime.filehandling.core.node.table.reader.DefaultMultiTableReadFactory;
import org.knime.filehandling.core.node.table.reader.DefaultProductionPathProvider;
import org.knime.filehandling.core.node.table.reader.MultiTableReader;
import org.knime.filehandling.core.node.table.reader.ProductionPathProvider;
import org.knime.filehandling.core.node.table.reader.ReadAdapterFactory;
import org.knime.filehandling.core.node.table.reader.config.DefaultMultiTableReadConfig;
import org.knime.filehandling.core.node.table.reader.config.DefaultTableReadConfig;
import org.knime.filehandling.core.node.table.reader.config.StorableMultiTableReadConfig;
import org.knime.filehandling.core.node.table.reader.config.TableSpecConfig;
import org.knime.filehandling.core.node.table.reader.rowkey.DefaultRowKeyGeneratorContextFactory;

/**
 * Node model implementation of the table manipulation node.
 *
 * @author Tobias Koetter, KNIME GmbH, Konstanz, Germany
 */
final class TableManipulatorNodeModel extends NodeModel {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(TableManipulatorNodeModel.class);

    private final StorableMultiTableReadConfig<TableManipulatorConfig> m_config;

    /**
     * A supplier is used to avoid any issues should this node model ever be used in parallel. However, this also means
     * that the specs are recalculated for each generated reader.
     */
    private final MultiTableReader<Table, TableManipulatorConfig> m_tableReader;

    private final InputPortRole[] m_inputPortRoles;

    TableManipulatorNodeModel(final PortsConfiguration portConfig) {
        super(portConfig.getInputPorts(), portConfig.getOutputPorts());
        final int noOfInputPorts = portConfig.getInputPorts().length;
        m_inputPortRoles = new InputPortRole[noOfInputPorts];
        Arrays.fill(m_inputPortRoles, InputPortRole.DISTRIBUTED_STREAMABLE);
        m_config = createConfig();
        final DefaultMultiTableReadFactory<Table, TableManipulatorConfig, DataType, DataValue> multiTableReadFactory =
            createReadFactory();
        m_tableReader = new MultiTableReader<>(multiTableReadFactory);
    }

    static
        DefaultMultiTableReadConfig<TableManipulatorConfig, DefaultTableReadConfig<TableManipulatorConfig>>
        createConfig() {
        DefaultTableReadConfig<TableManipulatorConfig> tc = new DefaultTableReadConfig<>(new TableManipulatorConfig());
        final DefaultMultiTableReadConfig<TableManipulatorConfig, DefaultTableReadConfig<TableManipulatorConfig>> config =
            new DefaultMultiTableReadConfig<>(tc, TableManipulatorConfigSerializer.INSTANCE);
        config.setFailOnDifferingSpecs(false);
        config.getTableReadConfig().setRowIDIdx(0);
        return config;
    }

    static DefaultMultiTableReadFactory<Table, TableManipulatorConfig, DataType, DataValue> createReadFactory() {
        final ReadAdapterFactory<DataType, DataValue> readAdapterFactory = DataValueReadAdapterFactory.INSTANCE;
        final ProductionPathProvider<DataType> productionPathProvider = createProductionPathProvider();
        // TODO why create a new hierarchy?
        return new DefaultMultiTableReadFactory<>(new DataTypeTypeHierarchy(),
            new DefaultRowKeyGeneratorContextFactory<>(DataValue::toString, "Table"), new RowInputTableReader(),
            productionPathProvider, readAdapterFactory::createReadAdapter);
    }

    static ProductionPathProvider<DataType> createProductionPathProvider() {
        final ReadAdapterFactory<DataType, DataValue> readAdapterFactory = DataValueReadAdapterFactory.INSTANCE;
        return new DefaultProductionPathProvider<>(readAdapterFactory.getProducerRegistry(),
            readAdapterFactory::getDefaultType);
    }

    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        final List<Table> rowInputs = new ArrayList<>(inSpecs.length);
        for (PortObjectSpec spec : inSpecs) {
            rowInputs.add(new EmptyTable((DataTableSpec)spec));
        }
        final TableSourceGroup tableSourceGroup = new TableSourceGroup(rowInputs);
        try {
            final TableSpecConfig tableSpecConfig =
                m_tableReader.createTableSpecConfig(tableSourceGroup, m_config);
            if (!m_config.hasTableSpecConfig()) {
                m_config.setTableSpecConfig(tableSpecConfig);
            }
            return new PortObjectSpec[]{tableSpecConfig.getDataTableSpec()};
        } catch (IOException | IllegalStateException e) {
            LOGGER.debug(e);
            throw new InvalidSettingsException(e.getMessage());
        }
    }

    @Override
    protected PortObject[] execute(final PortObject[] inObjects, final ExecutionContext exec) throws Exception {
        final TableSourceGroup sourceGroup = createSourceGroup(inObjects);
        return new PortObject[]{m_tableReader.readTable(sourceGroup, m_config, exec)};
    }

    static TableSourceGroup createSourceGroup(final PortObject[] inObjects) {
        final List<Table> rowInputs = new LinkedList<>();
        for (PortObject portObject : inObjects) {
            rowInputs.add(new DataTableBackedBoundedTable((BufferedDataTable)portObject));
        }
        return new TableSourceGroup(rowInputs);
    }

    @Override
    public StreamableOperator createStreamableOperator(final PartitionInfo partitionInfo,
        final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        return new StreamableOperator() {
            @Override
            public void runFinal(final PortInput[] inputs, final PortOutput[] outputs, final ExecutionContext exec)
                throws Exception {
                final TableSourceGroup sourceGroup = createSourceGroup(inputs);
                m_tableReader.fillRowOutput(sourceGroup, m_config, (RowOutput)outputs[0], exec);
            }

        };
    }

    private static TableSourceGroup createSourceGroup(final PortInput[] inputs) {
        final List<Table> rowInputs = new LinkedList<>();
        for (PortInput portObject : inputs) {
            rowInputs.add(new RowInputBackedTable((RowInput)portObject));
        }
        return new TableSourceGroup(rowInputs);
    }

    @Override
    public InputPortRole[] getInputPortRoles() {
        return m_inputPortRoles;
    }

    @Override
    public OutputPortRole[] getOutputPortRoles() {
        return new OutputPortRole[]{OutputPortRole.DISTRIBUTED};
    }

    @Override
    protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        // nothing to load
    }

    @Override
    protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        // no internals to load
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_config.saveInModel(settings);
    }

    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_config.validate(settings);
    }

    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_config.loadInModel(settings);
    }

    @Override
    protected void reset() {
        m_tableReader.reset();
    }

}
