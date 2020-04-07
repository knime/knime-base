package org.knime.filehandling.core.testing.node;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

public class FSTestEnvironmentNodeFactory extends NodeFactory<FSTestEnvironmentNodeModel> {

    @Override
    public FSTestEnvironmentNodeModel createNodeModel() {
        return new FSTestEnvironmentNodeModel();
    }

    @Override
    protected int getNrNodeViews() {
        return 0;
    }

    @Override
    public NodeView<FSTestEnvironmentNodeModel> createNodeView(int viewIndex, FSTestEnvironmentNodeModel nodeModel) {
        return null;
    }

    @Override
    protected boolean hasDialog() {
        return false;
    }

    @Override
    protected NodeDialogPane createNodeDialogPane() {
        return null;
    }

}
