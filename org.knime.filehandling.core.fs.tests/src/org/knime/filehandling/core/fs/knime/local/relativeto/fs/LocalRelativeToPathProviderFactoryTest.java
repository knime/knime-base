package org.knime.filehandling.core.fs.knime.local.relativeto.fs;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import org.junit.Test;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.filehandling.core.connections.FSCategory;
import org.knime.filehandling.core.connections.FSLocation;
import org.knime.filehandling.core.fs.location.FSPathProviderFactoryTestBase;
import org.knime.filehandling.core.testing.WorkflowTestUtil;

/**
 * Unit tests that test FSLocation support for the local relative-to file system.
 *
 * @author Bjoern Lohrmann, KNIME GmbH
 */
public class LocalRelativeToPathProviderFactoryTest extends FSPathProviderFactoryTestBase {

    /**
     * Tests reading from a mountpoint-relative location.
     *
     * @throws IOException
     */
    @Test
    public void test_relativeto_mountpoint_fs_location() throws IOException {
        final WorkflowManager workflowManager = WorkflowTestUtil
                .createAndLoadDummyWorkflow(m_tempFolder.newFolder("mountpoint-root").toPath());

        try {
            final byte[] bytesToWrite = "bla".getBytes();
            final Path tmpFile = workflowManager.getContext().getMountpointRoot().toPath().resolve("tempfile");
            Files.write(tmpFile, bytesToWrite);

            final FSLocation loc = new FSLocation(FSCategory.RELATIVE, "knime.mountpoint",
                    tmpFile.getFileName().toString());

            testReadFSLocation(Optional.empty(), loc, bytesToWrite);
        } finally {
            WorkflowTestUtil.shutdownWorkflowManager(workflowManager);
        }
    }

    /**
     * Tests reading from a workflow-relative location.
     *
     * @throws IOException
     */
    @Test
    public void test_relativeto_workflow_fs_location() throws IOException {
        final WorkflowManager workflowManager = WorkflowTestUtil
                .createAndLoadDummyWorkflow(m_tempFolder.newFolder("mountpoint-root").toPath());

        try {
            final byte[] bytesToWrite = "bla".getBytes();
            final Path localTmpFile = workflowManager.getContext().getMountpointRoot().toPath().resolve("tempfile");
            Files.write(localTmpFile, bytesToWrite);

            final FSLocation loc = new FSLocation(FSCategory.RELATIVE, "knime.workflow", "../tempfile");

            testReadFSLocation(Optional.empty(), loc, bytesToWrite);
        } finally {
            WorkflowTestUtil.shutdownWorkflowManager(workflowManager);
        }
    }
}
