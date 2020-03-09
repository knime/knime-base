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
 */
package org.knime.filehandling.core.node.portobject;

import java.io.File;
import java.io.IOException;
import java.util.Optional;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.context.ports.PortsConfiguration;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.filehandling.core.connections.FSConnection;
import org.knime.filehandling.core.defaultnodesettings.FileChooserHelper;
import org.knime.filehandling.core.defaultnodesettings.SettingsModelFileChooser2;
import org.knime.filehandling.core.node.portobject.reader.PortObjectFromPathReaderNodeModel;
import org.knime.filehandling.core.node.portobject.writer.PortObjectToPathWriterNodeModel;
import org.knime.filehandling.core.port.FileSystemPortObject;

/**
 * Abstract node model for port object reader and writer nodes.
 *
 * @author Simon Schmid, KNIME GmbH, Konstanz, Germany
 * @param <C> the config used by the node
 * @noextend extend either {@link PortObjectFromPathReaderNodeModel} or {@link PortObjectToPathWriterNodeModel}
 */
public abstract class PortObjectIONodeModel<C extends PortObjectIONodeConfig> extends NodeModel {

    /** The name of the optional connection input port group. */
    static final String CONNECTION_INPUT_PORT_GRP_NAME = "File System Connection";

    /** The ports configuration. */
    private final PortsConfiguration m_portsConfig;

    /** The config. */
    private final C m_config;

    /**
     * Constructor.
     *
     * @param portsConfig the ports configuration
     * @param config the config
     */
    protected PortObjectIONodeModel(final PortsConfiguration portsConfig, final C config) {
        super(portsConfig.getInputPorts(), portsConfig.getOutputPorts());
        m_portsConfig = portsConfig;
        m_config = config;
    }

    @Override
    protected final PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        final String pathOrURL = m_config.getFileChooserModel().getPathOrURL();
        if (pathOrURL == null || pathOrURL.trim().isEmpty()) {
            throw new InvalidSettingsException("Please enter a valid location.");
        }
        configureInternal(inSpecs);
        return null;
    }

    /**
     * Additional configure time check specific to subclasses implementing this method.
     *
     * @param inSpecs The input data table specs. Items of the array could be null if no spec is available from the
     *            corresponding input port (i.e. not connected or upstream node does not produce an output spec). If a
     *            port is of type {@link BufferedDataTable#TYPE} and no spec is available the framework will replace
     *            null by an empty {@link DataTableSpec} (no columns) unless the port is marked as optional as per
     *            constructor.
     * @throws InvalidSettingsException If this node can't be configured.
     */
    protected void configureInternal(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        // nothing to do here
    }

    /**
     * Creates a file chooser helper.
     *
     * @param data the input data
     * @return a file chooser helper
     * @throws IOException if the file system could not be retrieved
     */
    protected FileChooserHelper createFileChooserHelper(final PortObject[] data) throws IOException {
        final SettingsModelFileChooser2 fileChooserModel = m_config.getFileChooserModel();

        // get the fs connection if it exists
        final Optional<FSConnection> fs =
            Optional.ofNullable(getPortsConfig().getInputPortLocation().get(CONNECTION_INPUT_PORT_GRP_NAME)) //
                .map(arr -> FileSystemPortObject.getFileSystemConnection(data, arr[0]).get()); // save due to framework

        return new FileChooserHelper(fs, fileChooserModel, m_config.getTimeoutModel().getIntValue());
    }

    /**
     * Returns the ports configuration used to create this node.
     *
     * @return the ports configuration
     */
    protected final PortsConfiguration getPortsConfig() {
        return m_portsConfig;
    }

    /**
     * Method to obtain the PortObjectIONodeConfig of this node model.
     *
     * @return the config of this node model
     */
    protected final C getConfig() {
        return m_config;
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_config.saveConfigurationForModel(settings);
    }

    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_config.validateConfigurationForModel(settings);
    }

    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_config.loadConfigurationForModel(settings);
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
    protected void reset() {
        // nothing to do here
    }

}
