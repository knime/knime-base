package org.knime.filehandling.core.nodes.localfsconnection;

import java.io.File;
import java.io.IOException;

import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.workflow.FSConnectionNode;
import org.knime.filehandling.core.connections.FSConnectionRegistry;
import org.knime.filehandling.core.connections.LocalFSConnection;
import org.knime.filehandling.core.port.FileSystemPortObject;
import org.knime.filehandling.core.port.FileSystemPortObjectSpec;

/**
 * Node model for creating a local file system connection.
 *
 * @author Tobias Urhaug, KNIME GmbH, Berlin, Germany
 */
public class LocalFSConnectionNodeModel extends NodeModel implements FSConnectionNode {

    private SettingsModelString m_connectionName = LocalFSConnectionNodeDialog.createNumberFormatSettingsModel();

    private String m_connectionKey;

    /**
     * Constructor for the node model.
     */
    protected LocalFSConnectionNodeModel() {
    	super(new PortType[]{}, new PortType[]{FileSystemPortObject.TYPE});
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObject[] execute(final PortObject[] inObjects, final ExecutionContext exec) throws Exception {
        FSConnectionRegistry.getInstance().register(m_connectionKey, new LocalFSConnection());
        return new PortObject[]{new FileSystemPortObject(new FileSystemPortObjectSpec("Local", m_connectionKey))};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        m_connectionKey = null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs)
            throws InvalidSettingsException {
        m_connectionKey = FSConnectionRegistry.getInstance().getKey();
        return new PortObjectSpec[]{new FileSystemPortObjectSpec("Local", m_connectionKey)};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
         m_connectionName.saveSettingsTo(settings);
         //TODO: save connection key
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_connectionName.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_connectionName.validateSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File internDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        //TODO: load file system information and re-register the connection in the registry with the same key as before!
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File internDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        // No internals
    }

}

