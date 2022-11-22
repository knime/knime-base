package org.knime.filehandling.core.tests.common.workflow;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.stream.Stream;

import org.eclipse.core.runtime.FileLocator;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.workflow.NodeContext;
import org.knime.core.node.workflow.UnsupportedWorkflowVersionException;
import org.knime.core.node.workflow.WorkflowLoadHelper;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.node.workflow.contextv2.LocalLocationInfo;
import org.knime.core.node.workflow.contextv2.ServerLocationInfo;
import org.knime.core.node.workflow.contextv2.WorkflowContextV2;
import org.knime.core.util.FileUtil;
import org.knime.core.util.LockFailedException;
import org.knime.core.util.auth.SimpleTokenAuthenticator;
import org.knime.filehandling.core.connections.FSFiles;
import org.osgi.framework.FrameworkUtil;

/**
 * Utility class that provides methods to load a dummy workflow into a KNIME workflow manager, and to subsequently shut
 * down the workflow manager.
 *
 * @author Bjoern Lohrmann, KNIME GmbH
 * @noreference non-public API
 * @noinstantiate non-public API
 */
public final class WorkflowTestUtil {

    private static final String RESOURCES_FOLDER = "resources";

    private static final String DUMMY_WORKFLOW = "dummy-workflow";

    private WorkflowTestUtil() {
    }

    /**
     * Copies a dummy workflow into the given mountpoint root folder and loads it into a workflow manager.
     *
     * @param localMountpointRoot The mountpoint root folder to copy the workflow into.
     * @return the workflow manager for the dummy workflow
     * @throws IOException
     */
    public static WorkflowManager createAndLoadDummyWorkflow(final Path localMountpointRoot) throws IOException {
        final var workflowName = "current-workflow";
        createWorkflowDir(localMountpointRoot, workflowName);

        final var workflowManager = WorkflowTestUtil.getWorkflowManager(localMountpointRoot, workflowName);
        NodeContext.pushContext(workflowManager);
        return workflowManager;
    }

    /**
     * Shuts down the given workflow manager (unloads the corresponding workflow).
     *
     * @param workflowManager The workflow manager of the workflow to unload.
     */
    public static void shutdownWorkflowManager(final WorkflowManager workflowManager) {
        try {
            WorkflowManager.ROOT.removeProject(workflowManager.getID());
        } finally {
            NodeContext.removeLastContext();
        }
    }

    /**
     * Creates a dummy workflow of the given name in the given parent dir.
     *
     * @param parentDir The directory in which to create the workflow directory.
     * @param workflowName The name of the workflow directory to create.
     * @return the path of the newly created workflow directory.
     * @throws IOException
     */
    public static Path createWorkflowDir(final Path parentDir, final String workflowName) throws IOException {
        final File dummyWorkflow = WorkflowTestUtil.findInPlugin(WorkflowTestUtil.DUMMY_WORKFLOW);
        final Path workflowDir = parentDir.resolve(workflowName);
        FileUtil.copyDir(dummyWorkflow, workflowDir.toFile());
        return workflowDir;
    }

    private static File findInPlugin(final String name) throws IOException {
        final var thisBundle = FrameworkUtil.getBundle(WorkflowTestUtil.class);

        // this works when running tests in maven
        var url = FileLocator.find(thisBundle, new org.eclipse.core.runtime.Path(name), null);
        if (url != null) {
            return new File(FileLocator.toFileURL(url).getPath());
        }

        // this works when running tests in Eclipse
        url = FileLocator.find(thisBundle, new org.eclipse.core.runtime.Path(RESOURCES_FOLDER).append(name), null);
        if (url != null) {
            return new File(FileLocator.toFileURL(url).getPath());
        }

        throw new FileNotFoundException(thisBundle.getLocation() + name);
    }

    /**
     * Creates a {@link WorkflowManager} for the workflow located at <code>knime://LOCAL/workflowName</code>,
     * where the LOCAL mountpoint is root at mountpointRoot in the local file system.
     *
     * @param mountpointRoot Path in local file system where the LOCAL mountpoint should be rooted at.
     * @param workflowName Name of the workflow
     * @return the newly created {@link WorkflowManager}.
     * @throws IOException
     */
    public static WorkflowManager getWorkflowManager(final Path mountpointRoot, final String workflowName) throws IOException {

        final var workflowFile = mountpointRoot.resolve(workflowName).toFile();
        try {
            final var execMon = new ExecutionMonitor();
            final var context = WorkflowContextV2.builder()
                    .withAnalyticsPlatformExecutor(exec -> exec
                        .withCurrentUserAsUserId()
                        .withLocalWorkflowPath(workflowFile.toPath())
                        .withMountpoint("LOCAL", mountpointRoot))
                    .withLocation(LocalLocationInfo.getInstance(null))
                    .build();

            final var loadHelper = new WorkflowLoadHelper(context);
            final var loadResult = WorkflowManager.ROOT.load(workflowFile, execMon, loadHelper, false);
            return loadResult.getWorkflowManager();
        } catch (final InvalidSettingsException | CanceledExecutionException | UnsupportedWorkflowVersionException
                | LockFailedException e) {
            throw new IOException(e);
        }
    }

    /**
     * Creates a {@link WorkflowManager} for the workflow located at <code>knime://SERVER/workflowName</code>, where the
     * (fake) SERVER mountpoint points to a server repository
     *
     * @param executorWorkspaceRoot Path in local file system where the (fake) executor has its workspace.
     * @param workflowName Name of the workflow
     * @return the newly created {@link WorkflowManager}.
     * @throws IOException
     */
    public static WorkflowManager getServerSideWorkflowManager(final Path executorWorkspaceRoot,
        final String workflowName) throws IOException {
        final var workflowFile = executorWorkspaceRoot.resolve(workflowName).toFile();
        try {
            final var execMon = new ExecutionMonitor();
            final var context = WorkflowContextV2.builder().withServerJobExecutor(exec -> exec.withCurrentUserAsUserId() //
                    .withLocalWorkflowPath(workflowFile.toPath()) //
                    .withJobId(UUID.randomUUID()))//
                .withLocation(ServerLocationInfo.builder() //
                    .withRepositoryAddress(URI.create("http://test-test-test/repository")) //
                    .withWorkflowPath("/" + workflowName) //
                    .withAuthenticator(new SimpleTokenAuthenticator("test-test-test"))//
                    .withDefaultMountId("SERVER")//
                    .build())//
                .build();

            final var loadHelper = new WorkflowLoadHelper(context);
            final var loadResult = WorkflowManager.ROOT.load(workflowFile, execMon, loadHelper, false);
            return loadResult.getWorkflowManager();
        } catch (final InvalidSettingsException | CanceledExecutionException | UnsupportedWorkflowVersionException
                | LockFailedException e) {
            throw new IOException(e);
        }
    }

    /**
     * Deletes the given directory recursively, ignoring any exceptions that occur.
     *
     * @param dir
     */
    public static void clearDirectoryContents(final Path dir) {
        try (final Stream<Path> stream = Files.list(dir)) {
            stream.forEach(p -> {
                try {
                    FSFiles.deleteRecursively(p);
                } catch (IOException e) { // NOSONAR supposed to be ignored
                    // ignore
                }
            });
        } catch (IOException ex) { // NOSONAR supposed to be ignored
        }
    }
}
