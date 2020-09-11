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
 *   Jan 29, 2020 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.node.table.reader;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.List;

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
import org.knime.core.node.streamable.OutputPortRole;
import org.knime.core.node.streamable.PartitionInfo;
import org.knime.core.node.streamable.PortInput;
import org.knime.core.node.streamable.PortOutput;
import org.knime.core.node.streamable.RowOutput;
import org.knime.core.node.streamable.StreamableOperator;
import org.knime.core.node.streamable.StreamableOperatorInternals;
import org.knime.filehandling.core.defaultnodesettings.filechooser.reader.ReadPathAccessor;
import org.knime.filehandling.core.defaultnodesettings.status.NodeModelStatusConsumer;
import org.knime.filehandling.core.defaultnodesettings.status.StatusMessage.MessageType;
import org.knime.filehandling.core.node.table.reader.config.ReaderSpecificConfig;
import org.knime.filehandling.core.node.table.reader.config.StorableMultiTableReadConfig;
import org.knime.filehandling.core.node.table.reader.paths.PathSettings;
import org.knime.filehandling.core.util.SettingsUtils;

/**
 * Generic implementation of a Reader node that reads tables.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
final class TableReaderNodeModel<C extends ReaderSpecificConfig<C>> extends NodeModel {

    static final int FS_INPUT_PORT = 0;

    private final StorableMultiTableReadConfig<C> m_config;

    private final PathSettings m_pathSettings;

    private final NodeModelStatusConsumer m_statusConsumer =
        new NodeModelStatusConsumer(EnumSet.of(MessageType.ERROR, MessageType.WARNING));

    /**
     * A supplier is used to avoid any issues should this node model ever be used in parallel. However, this also means
     * that the specs are recalculated for each generated reader.
     */
    private final MultiTableReader<C> m_tableReader;


    /**
     * Constructs a node model with no inputs and one output.
     *
     * @param config storing the user settings
     * @param pathSettingsModel storing the paths selected by the user
     * @param tableReader reader for reading tables
     */
    protected TableReaderNodeModel(final StorableMultiTableReadConfig<C> config, final PathSettings pathSettingsModel,
        final MultiTableReader<C> tableReader) {
        super(0, 1);
        m_config = config;
        m_pathSettings = pathSettingsModel;
        m_tableReader = tableReader;
    }

    /**
     * Constructs a node model with no inputs and one output.
     *
     * @param config storing the user settings
     * @param pathSettingsModel storing the paths selected by the user
     * @param tableReader reader for reading tables
     * @param portsConfig determines the in and outports.
     */
    protected TableReaderNodeModel(final StorableMultiTableReadConfig<C> config, final PathSettings pathSettingsModel,
        final MultiTableReader<C> tableReader,
        final PortsConfiguration portsConfig) {
        super(portsConfig.getInputPorts(), portsConfig.getOutputPorts());
        m_config = config;
        m_pathSettings = pathSettingsModel;
        m_tableReader = tableReader;
    }

    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        m_pathSettings.configureInModel(inSpecs, m_statusConsumer);
        m_statusConsumer.setWarningsIfRequired(this::setWarningMessage);
        if (m_config.hasTableSpecConfig()) {
            if (m_config.getTableSpecConfig().isConfiguredWith(m_pathSettings.getPath())) {
                return new PortObjectSpec[]{m_config.getTableSpecConfig().getDataTableSpec()};
            }
            setWarningMessage("The stored spec has not been created with the given file/path.");
        }
        return null;// NOSONAR, we aren't allowed to do I/O in configure therefore we can't calculate the output spec
    }

    @Override
    protected PortObject[] execute(final PortObject[] inObjects, final ExecutionContext exec) throws Exception {
        try (final ReadPathAccessor accessor = m_pathSettings.createReadPathAccessor()) {
            final List<Path> paths = getPaths(accessor);
            return new PortObject[]{m_tableReader.readTable(m_pathSettings.getPath(), paths, m_config, exec)};
        }
    }

    @Override
    public PortObjectSpec[] computeFinalOutputSpecs(final StreamableOperatorInternals internals,
        final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        try (final ReadPathAccessor accessor = m_pathSettings.createReadPathAccessor()) {
            final List<Path> paths = getPaths(accessor);
            return new PortObjectSpec[]{m_tableReader.createTableSpec(m_pathSettings.getPath(), paths, m_config)};
        } catch (IOException ex) {
            throw new InvalidSettingsException(ex);
        }
    }

    @Override
    public StreamableOperator createStreamableOperator(final PartitionInfo partitionInfo,
        final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        return new StreamableOperator() {
            @Override
            public void runFinal(final PortInput[] inputs, final PortOutput[] outputs, final ExecutionContext exec)
                throws Exception {
                try (final ReadPathAccessor accessor = m_pathSettings.createReadPathAccessor()) {
                    final List<Path> paths = getPaths(accessor);
                    final RowOutput output = (RowOutput)outputs[0];
                    m_tableReader.fillRowOutput(m_pathSettings.getPath(), paths, m_config, output, exec);
                }
            }
        };
    }

    private List<Path> getPaths(final ReadPathAccessor accessor) throws IOException, InvalidSettingsException {
        final List<Path> paths = accessor.getPaths(m_statusConsumer);
        m_statusConsumer.setWarningsIfRequired(this::setWarningMessage);
        return paths;
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
        // FIXME: the path settings should become part of the config (AP-14460)
        m_pathSettings.saveSettingsTo(SettingsUtils.getOrAdd(settings, SettingsUtils.CFG_SETTINGS_TAB));
    }

    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_config.validate(settings);
        // FIXME: the path settings should become part of the config (AP-14460)
        m_pathSettings.validateSettings(settings.getNodeSettings(SettingsUtils.CFG_SETTINGS_TAB));
    }

    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_config.loadInModel(settings);
        // FIXME: the path settings should become part of the config (AP-14460)
        m_pathSettings.loadSettingsFrom(settings.getNodeSettings(SettingsUtils.CFG_SETTINGS_TAB));
    }

    @Override
    protected void reset() {
        m_tableReader.reset();
    }

}
