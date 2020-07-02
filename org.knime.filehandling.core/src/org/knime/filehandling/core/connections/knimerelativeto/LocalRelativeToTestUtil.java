package org.knime.filehandling.core.connections.knimerelativeto;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

import org.eclipse.core.runtime.FileLocator;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.workflow.NodeContext;
import org.knime.core.node.workflow.UnsupportedWorkflowVersionException;
import org.knime.core.node.workflow.WorkflowContext;
import org.knime.core.node.workflow.WorkflowLoadHelper;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.node.workflow.WorkflowPersistor.WorkflowLoadResult;
import org.knime.core.util.FileUtil;
import org.knime.core.util.LockFailedException;
import org.knime.filehandling.core.connections.FSFiles;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

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
    public static WorkflowManager createAndLoadDummyWorkflow(final Path localMountpointRoot) throws IOException {
        final Path currentWorkflow = LocalRelativeToTestUtil.createWorkflowDir(localMountpointRoot, "current-workflow");
        final WorkflowManager workflowManager =
            LocalRelativeToTestUtil.getWorkflowManager(localMountpointRoot.toFile(), currentWorkflow, false);
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

    public static Path createWorkflowDir(final Path parentDir, final String workflowName) throws IOException {
        final File dummyWorkflow = LocalRelativeToTestUtil.findInPlugin(LocalRelativeToTestUtil.DUMMY_WORKFLOW);
        final Path workflowDir = parentDir.getFileSystem().getPath(parentDir.toString(), workflowName);
        FileUtil.copyDir(dummyWorkflow, workflowDir.toFile());
        return workflowDir;
    }

    private static File findInPlugin(final String name) throws IOException {
        final Bundle thisBundle = FrameworkUtil.getBundle(LocalRelativeToFSTestInitializer.class);
        final URL url = FileLocator.find(thisBundle, new org.eclipse.core.runtime.Path(name), null);
        if (url == null) {
            throw new FileNotFoundException(thisBundle.getLocation() + name);
        }
        return new File(FileLocator.toFileURL(url).getPath());
    }

    public static WorkflowManager getWorkflowManager(final File mountpointRoot, final Path currentWorkflowDirectory,
        final boolean serverMode) throws IOException {
        try {
            final ExecutionMonitor exec = new ExecutionMonitor();
            final WorkflowContext.Factory fac = new WorkflowContext.Factory(currentWorkflowDirectory.toFile());
            fac.setMountpointRoot(mountpointRoot);
            fac.setTemporaryCopy(serverMode);
            if (serverMode) {
                fac.setRemoteAddress(URI.create("http://test-test-test:-1"), "test-test-test");
                fac.setRemoteAuthToken("test-test-test");
            }
            final WorkflowLoadHelper loadHelper = new WorkflowLoadHelper(fac.createContext());
            final WorkflowLoadResult loadResult =
                WorkflowManager.ROOT.load(currentWorkflowDirectory.toFile(), exec, loadHelper, false);
            return loadResult.getWorkflowManager();
        } catch (final InvalidSettingsException | CanceledExecutionException | UnsupportedWorkflowVersionException
                | LockFailedException e) {
            throw new IOException(e);
        }
    }

    static final String DUMMY_WORKFLOW = "resources/dummy-workflow";

    static void clearDirectoryContents(final Path dir) throws IOException {
        Files.list(dir).forEach((p) -> {
            try {
                FSFiles.deleteRecursively(p);
            } catch (IOException e) {
                // ignore
            }
        });
    }

    /**
     * @return path of a dummy workflow
     * @throws IOException
     */
    public static Path getDummyWorkflowPath() throws IOException {
        return findInPlugin(DUMMY_WORKFLOW).toPath();
    }

    /**
     * @param fs
     * @return the local path that corresponds to the working directory of the given file system.
     * @throws IOException
     */
    public static Path determineLocalWorkingDirectory(final LocalRelativeToFileSystem fs) throws IOException {
        return determineLocalPath(fs, fs.getWorkingDirectory());
    }

    /**
     * @param fs
     * @param path
     * @return the local path that corresponds to the given path in the given file system.
     * @throws IOException
     */
    public static Path determineLocalPath(final LocalRelativeToFileSystem fs, final RelativeToPath path) throws IOException {
        return fs.toRealPathWithAccessibilityCheck(path);
    }

}
