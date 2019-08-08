package org.knime.filehandling.core.nodes.localfsconnection;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * Local FS connection factory.
 *
 * @author Tobias Urhaug, KNIME GmbH, Berlin, Germany
 */
public class LocalFSConnectionNodeFactory
        extends NodeFactory<LocalFSConnectionNodeModel> {

    /**
     * {@inheritDoc}
     */
    @Override
    public LocalFSConnectionNodeModel createNodeModel() {
        return new LocalFSConnectionNodeModel();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getNrNodeViews() {
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeView<LocalFSConnectionNodeModel> createNodeView(final int viewIndex,
            final LocalFSConnectionNodeModel nodeModel) {
		return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasDialog() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeDialogPane createNodeDialogPane() {
        return new LocalFSConnectionNodeDialog();
    }

}

