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
import org.knime.filehandling.core.node.portobject.reader.AbstractPortObjectReaderNodeModel;
import org.knime.filehandling.core.node.portobject.writer.AbstractPortObjectWriterNodeModel;
import org.knime.filehandling.core.port.FileSystemPortObject;

/**
 * Abstract node model for port object reader and writer nodes.
 *
 * @author Simon Schmid, KNIME GmbH, Konstanz, Germany
 * @param <C> the config used by the node
 * @noextend extend either {@link AbstractPortObjectReaderNodeModel} or {@link AbstractPortObjectWriterNodeModel}
 */
public abstract class AbstractPortObjectIONodeModel<C extends AbstractPortObjectIONodeConfig> extends NodeModel {

    /** The config. */
    protected final C m_config;

    /** The name of the optional connection port group. */
    protected static final String CONNECTION_PORT_GRP_NAME = "File System Connection";

    /**
     * Constructor.
     *
     * @param portsConfig the node creation configuration
     * @param config the config
     */
    protected AbstractPortObjectIONodeModel(final PortsConfiguration portsConfig, final C config) {
        super(portsConfig.getInputPorts(), portsConfig.getOutputPorts());
        m_config = config;
    }

    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        final String pathOrURL = m_config.getFileChooserModel().getPathOrURL();
        if (pathOrURL == null || pathOrURL.trim().isEmpty()) {
            throw new InvalidSettingsException("Please enter a valid location.");
        }
        return null;
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
        // can be safely set to 0 since we know that this is the ports object position
        final Optional<FSConnection> fs = FileSystemPortObject.getFileSystemConnection(data, 0);
        return new FileChooserHelper(fs, fileChooserModel, m_config.getTimeoutModel().getIntValue());
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
