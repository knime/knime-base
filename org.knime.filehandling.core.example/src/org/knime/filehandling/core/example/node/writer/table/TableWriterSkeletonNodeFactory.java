package org.knime.filehandling.core.example.node.writer.table;

import java.util.Optional;

import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ConfigurableNodeFactory;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeView;
import org.knime.core.node.context.NodeCreationConfiguration;
import org.knime.core.node.context.ports.PortsConfiguration;
import org.knime.filehandling.core.port.FileSystemPortObject;

/**
 * Example table writer node factory.
 *
 * @author Mark Ortmann, KNIME GmbH
 */
public class TableWriterSkeletonNodeFactory extends ConfigurableNodeFactory<TableWriterSkeletonNodeModel> {

    /** The name of the optional connection input port group. */
    private static final String CONNECTION_INPUT_PORT_GRP_NAME = "File System Connection";

    /** The name of the table input port group. */
    private static final String TABLE_INPUT_PORT_GRP_NAME = "Input Table";

    @Override
    protected Optional<PortsConfigurationBuilder> createPortsConfigBuilder() {
        final PortsConfigurationBuilder b = new PortsConfigurationBuilder();
        // add the optional FS connection port
        b.addOptionalInputPortGroup(CONNECTION_INPUT_PORT_GRP_NAME, FileSystemPortObject.TYPE);
        // add the fixed table input port
        b.addFixedInputPortGroup(TABLE_INPUT_PORT_GRP_NAME, BufferedDataTable.TYPE);
        return Optional.of(b);
    }

    @Override
    protected TableWriterSkeletonNodeModel createNodeModel(final NodeCreationConfiguration creationConfig) {
        final PortsConfiguration portsCfg = getPortsCfg(creationConfig);
        return new TableWriterSkeletonNodeModel(portsCfg, createSettings(portsCfg), getTableIdx(portsCfg));
    }

    @Override
    protected NodeDialogPane createNodeDialogPane(final NodeCreationConfiguration creationConfig) {
        final PortsConfiguration portsCfg = getPortsCfg(creationConfig);
        return new TableWriterSkeletonNodeDialog(createSettings(portsCfg));
    }

    @Override
    protected int getNrNodeViews() {
        return 0;
    }

    @Override
    public NodeView<TableWriterSkeletonNodeModel> createNodeView(final int viewIndex,
        final TableWriterSkeletonNodeModel nodeModel) {
        return null;
    }

    @Override
    protected boolean hasDialog() {
        return true;
    }

    private static PortsConfiguration getPortsCfg(final NodeCreationConfiguration creationCfg) {
        return creationCfg.getPortConfig().orElseThrow(IllegalStateException::new);
    }

    private static TableWriterSkeletonSettings createSettings(final PortsConfiguration portsCfg) {
        return new TableWriterSkeletonSettings(portsCfg, CONNECTION_INPUT_PORT_GRP_NAME);
    }

    private static int getTableIdx(final PortsConfiguration portsCfg) {
        return portsCfg.getInputPortLocation().get(TABLE_INPUT_PORT_GRP_NAME)[0];
    }
}
