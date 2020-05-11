package org.knime.filehandling.core.connections.knimerelativeto;

import static org.knime.filehandling.core.connections.knimerelativeto.LocalRelativeToFSTestInitializer.createWorkflowDir;
import static org.knime.filehandling.core.connections.knimerelativeto.LocalRelativeToFSTestInitializer.getWorkflowManager;

import java.io.IOException;
import java.nio.file.Path;

import org.knime.core.node.workflow.NodeContext;
import org.knime.core.node.workflow.WorkflowManager;

/**
 * Utility class that provides methods to load a dummy workflow into a KNIME workflow manager, and to subsequently shut
 * down the workflow manager.
 *
 * @author Bjoern Lohrmann, KNIME GmbH
 *
 */
public class LocalRelativeToTestUtil {

    /**
     * Copies a dummy workflow into the given mountpoint root folder and loads it into a workflow manager.
     *
     * @param localMountpointRoot The mountpoint root folder to copy the workflow into.
     * @return the workflow manager for the dummy workflow
     * @throws IOException
     */
    public static WorkflowManager loadDummyWorkflow(final Path localMountpointRoot) throws IOException {
        final Path currentWorkflow = createWorkflowDir(localMountpointRoot, "current-workflow");
        final WorkflowManager workflowManager =
            getWorkflowManager(localMountpointRoot.toFile(), currentWorkflow, false);
        NodeContext.pushContext(workflowManager);
        return workflowManager;
    }

    /**
     * Shuts down the given workflow manager (unloads the corresponding workflow).
     *
     * @param workflowManager The workflow manager of the workflow to unload.
     */
    public static void shutdownWorkflowManager(WorkflowManager workflowManager) {
        try {
            WorkflowManager.ROOT.removeProject(workflowManager.getID());
        } finally {
            NodeContext.removeLastContext();
        }
    }
}
