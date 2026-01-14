package org.knime.filehandling.core.example.node.writer.port;

import org.knime.core.node.context.NodeCreationConfiguration;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortType;
import org.knime.filehandling.core.node.portobject.writer.PortObjectWriterNodeFactory;

/**
 * Example port object writer node factory.
 *
 * @author Mark Ortmann, KNIME GmbH
 */
public class PortObjectWriterSkeletonNodeFactory
    extends PortObjectWriterNodeFactory<PortObjectWriterSkeletonNodeModel, PortObjectWriterSkeletonNodeDialog> {

    @Override
    protected PortType getInputPortType() {
        return PortObject.TYPE;
    }

    @Override
    protected PortObjectWriterSkeletonNodeDialog createDialog(final NodeCreationConfiguration creationConfig) {
        return new PortObjectWriterSkeletonNodeDialog(new PortObjectWriterSkeletonNodeConfig(creationConfig),
            "example_history_id");
    }

    @Override
    protected PortObjectWriterSkeletonNodeModel createNodeModel(final NodeCreationConfiguration creationConfig) {
        return new PortObjectWriterSkeletonNodeModel(creationConfig,
            new PortObjectWriterSkeletonNodeConfig(creationConfig));
    }
}
