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
    private String m_fsName;

    protected FSTestEnvironmentNodeModel() {
        super(null, new PortType[]{FileSystemPortObject.TYPE});
    }

    @Override
    protected PortObject[] execute(PortObject[] inObjects, ExecutionContext exec) throws Exception {
        Properties testProperties = FSTestPropertiesResolver.forWorkflowTests();
        Map<String, String> configuration = FSTestConfigurationReader.read(m_fsName, testProperties);
        FSTestInitializer initializer = FSTestInitializerManager.instance().createInitializer(m_fsName, configuration);

        m_fsConnection = initializer.getFSConnection();
        FSConnectionRegistry.getInstance().register(m_fsId, m_fsConnection);

        return new PortObject[]{new FileSystemPortObject(createSpec())};
    }

    @Override
    protected PortObjectSpec[] configure(PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        m_fsId = FSConnectionRegistry.getInstance().getKey();

        m_fsName = FSTestPropertiesResolver.forWorkflowTests().getProperty("test-fs");
        if (m_fsName == null) {
            throw new InvalidSettingsException(
                    "Missing property 'test-fs'. Please add it to fs-test.properties and specify the file system to test.");
        }
        return new PortObjectSpec[]{createSpec()};
    }

    private FileSystemPortObjectSpec createSpec() {
        return new FileSystemPortObjectSpec(m_fsName, m_fsId);
    }

    @Override
    protected void loadInternals(File nodeInternDir, ExecutionMonitor exec)
            throws IOException, CanceledExecutionException {
    }

    @Override
    protected void saveInternals(File nodeInternDir, ExecutionMonitor exec)
            throws IOException, CanceledExecutionException {
    }

    @Override
    protected void saveSettingsTo(NodeSettingsWO settings) {
    }

    @Override
    protected void validateSettings(NodeSettingsRO settings) throws InvalidSettingsException {
    }

    @Override
    protected void loadValidatedSettingsFrom(NodeSettingsRO settings) throws InvalidSettingsException {
    }

    @Override
    protected void onDispose() {
        //close the file system also when the workflow is closed
        reset();
    }

    @Override
    protected void reset() {
        m_fsConnection.ensureClosed();
    }

}
