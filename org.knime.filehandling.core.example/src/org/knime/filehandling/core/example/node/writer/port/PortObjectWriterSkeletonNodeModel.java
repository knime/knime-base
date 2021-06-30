package org.knime.filehandling.core.example.node.writer.port;

import java.io.OutputStream;

import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.context.NodeCreationConfiguration;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.filehandling.core.node.portobject.writer.PortObjectToFileWriterNodeModel;

final class PortObjectWriterSkeletonNodeModel
		extends PortObjectToFileWriterNodeModel<PortObjectWriterSkeletonNodeConfig> {

	protected PortObjectWriterSkeletonNodeModel(NodeCreationConfiguration creationConfig,
			PortObjectWriterSkeletonNodeConfig config) {
		super(creationConfig, config);
	}

	@Override
	protected void write(PortObject object, OutputStream outputStream, ExecutionContext exec) throws Exception {
		// write your port object to the given output stream
	}

	@Override
	protected void configureInternal(PortObjectSpec[] inSpecs) throws InvalidSettingsException {
		// do additional configuration check here
		if (getConfig().getExampleModel().getBooleanValue()) {
			throw new InvalidSettingsException("Configuration failed");
		}
		super.configureInternal(inSpecs);
	}
}
