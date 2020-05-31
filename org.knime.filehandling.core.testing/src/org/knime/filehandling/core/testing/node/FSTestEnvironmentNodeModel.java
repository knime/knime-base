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
 */
package org.knime.filehandling.core.testing.node;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Properties;

import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.filehandling.core.connections.FSConnection;
import org.knime.filehandling.core.connections.FSConnectionRegistry;
import org.knime.filehandling.core.connections.FSLocationSpec;
import org.knime.filehandling.core.port.FileSystemPortObject;
import org.knime.filehandling.core.port.FileSystemPortObjectSpec;
import org.knime.filehandling.core.testing.FSTestInitializer;
import org.knime.filehandling.core.testing.FSTestInitializerManager;
import org.knime.filehandling.core.testing.integrationtests.FSTestConfigurationReader;
import org.knime.filehandling.core.testing.integrationtests.FSTestPropertiesResolver;

/**
 * Node model for the FS Testing node.
 *
 * @author Tobias Urhaug, KNIME GmbH, Berlin, Germany
 *
 */
public class FSTestEnvironmentNodeModel extends NodeModel {

    private FSConnection m_fsConnection;
    private String m_fsId;
    private String m_fsType;

    protected FSTestEnvironmentNodeModel() {
        super(null, new PortType[]{FileSystemPortObject.TYPE});
    }

    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        m_fsId = FSConnectionRegistry.getInstance().getKey();

        final Properties fsTestProperties = FSTestPropertiesResolver.forWorkflowTests();

        m_fsType = fsTestProperties.getProperty("test-fs");
        if (m_fsType == null) {
            throw new InvalidSettingsException(
                    "Missing property 'test-fs'. Please add it to fs-test.properties and specify the file system to test.");
        }

        final Map<String, String> fsConfiguration = FSTestConfigurationReader.read(m_fsType, fsTestProperties);
        final FSLocationSpec fsLocationSpec = FSTestInitializerManager.instance().createFSLocationSpec(m_fsType, fsConfiguration);


        return new PortObjectSpec[]{createSpec(fsLocationSpec)};
    }

    @SuppressWarnings("resource")
    @Override
    protected PortObject[] execute(final PortObject[] inObjects, final ExecutionContext exec) throws Exception {
        final Properties testProperties = FSTestPropertiesResolver.forWorkflowTests();
        final Map<String, String> configuration = FSTestConfigurationReader.read(m_fsType, testProperties);
        final FSTestInitializer initializer = FSTestInitializerManager.instance().createInitializer(m_fsType, configuration);

        m_fsConnection = initializer.getFSConnection();
        FSConnectionRegistry.getInstance().register(m_fsId, m_fsConnection);

        return new PortObject[]{
            new FileSystemPortObject(createSpec(m_fsConnection.getFileSystem().getFSLocationSpec()))};
    }

    private FileSystemPortObjectSpec createSpec(final FSLocationSpec fsLocationSpec) {
        return new FileSystemPortObjectSpec(m_fsType, m_fsId, fsLocationSpec);
    }

    @Override
    protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec)
            throws IOException, CanceledExecutionException {
    }

    @Override
    protected void saveInternals(final File nodeInternDir,
        final ExecutionMonitor exec) throws IOException, CanceledExecutionException {
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
    }

    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
    }

    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
    }

    @Override
    protected void onDispose() {
        //close the file system also when the workflow is closed
        reset();
    }

    @Override
    protected void reset() {
        m_fsConnection.close();
    }

}
