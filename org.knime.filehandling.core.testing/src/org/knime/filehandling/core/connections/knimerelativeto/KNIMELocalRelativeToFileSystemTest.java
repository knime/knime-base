package org.knime.filehandling.core.connections.knimerelativeto;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.knime.filehandling.core.connections.knimerelativeto.LocalRelativeToFSTestInitializer.createWorkflowDir;
import static org.knime.filehandling.core.connections.knimerelativeto.LocalRelativeToFSTestInitializer.getWorkflowManager;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.workflow.NodeContext;
import org.knime.core.node.workflow.WorkflowManager;

/**
 * Test local relative to file system specific things.
 *
 * @author Sascha Wolke, KNIME GmbH
 */
public class KNIMELocalRelativeToFileSystemTest {

	@Rule
	public TemporaryFolder m_tempFolder = new TemporaryFolder();

	private WorkflowManager m_workflowManager;

	@Before
	public void beforeTestCase() throws IOException {
		final File mountpointRoot = m_tempFolder.newFolder("mountpoint-root");
		final Path currentWorkflow = createWorkflowDir(mountpointRoot.toPath(), "current-workflow");
		createWorkflowDir(mountpointRoot.toPath(), "other-workflow");
		m_workflowManager = getWorkflowManager(mountpointRoot, currentWorkflow, false);
		NodeContext.pushContext(m_workflowManager);
	}

	@After
	public void afterTestCase() {
		try {
			WorkflowManager.ROOT.removeProject(m_workflowManager.getID());
		} finally {
			NodeContext.removeLastContext();
		}
	}

	@Test(expected = UnsupportedOperationException.class)
	public void unsupportedServerSideExecution() throws IOException {
		// replace the current workflow manager with a server side workflow manager
		NodeContext.removeLastContext();
		final File mountpointRoot = m_tempFolder.newFolder("other-mountpoint-root");
		final Path currentWorkflow = createWorkflowDir(mountpointRoot.toPath(), "current-workflow");
		m_workflowManager = getWorkflowManager(mountpointRoot, currentWorkflow, true);
		NodeContext.pushContext(m_workflowManager);

		// initialization should fail
		getMountpointRelativeFS();
	}

	@Test
	public void getRootDirectories() throws IOException {
		final LocalRelativeToFileSystem fs = getWorkflowRelativeFS();
		assertEquals(Collections.singletonList(fs.getPath("/")), fs.getRootDirectories());
	}

	@Test
	public void workingDirectoryWorkflowRelative() throws IOException {
		final LocalRelativeToFileSystem fs = getWorkflowRelativeFS();
		assertEquals(fs.getPath("/current-workflow"), fs.getWorkingDirectory());
	}

	@Test
	public void workingDirectoryMountpointRelative() throws IOException {
		final LocalRelativeToFileSystem fs = getMountpointRelativeFS();
		assertEquals(fs.getPath("/"), fs.getWorkingDirectory());
	}

	public void outsideMountpointWorkflowRelative() throws IOException {
		final LocalRelativeToFileSystem fs = getWorkflowRelativeFS();
		final LocalRelativeToPath path = (LocalRelativeToPath) fs.getPath("../../../somewhere-outside");
		assertFalse(fs.isLocalPathAccessible(path.toAbsoluteLocalPath()));
	}

	public void outsideMountpointMountpointRelative() throws IOException {
		final LocalRelativeToFileSystem fs = getMountpointRelativeFS();
		final LocalRelativeToPath path = (LocalRelativeToPath) fs.getPath("/../../../somewhere-outside");
		assertFalse(fs.isLocalPathAccessible(path.toAbsoluteLocalPath()));
	}

	@Test
	public void insideCurrentWorkflowWithWorkflowRelative() throws IOException {
		final LocalRelativeToFileSystem fs = getWorkflowRelativeFS();
		final LocalRelativeToPath path = (LocalRelativeToPath) fs
				.getPath("../current-workflow/some-file.txt");
		assertTrue(fs.isLocalPathAccessible(path.toAbsoluteLocalPath()));
	}

	public void insideCurrentWorkflowWithMountpointRelative() throws IOException {
		final LocalRelativeToFileSystem fs = getMountpointRelativeFS();
		final LocalRelativeToPath path = (LocalRelativeToPath) fs.getPath("/current-workflow/some-file.txt");
		assertFalse(fs.isLocalPathAccessible(path.toAbsoluteLocalPath()));
	}

	public void insideOtherWorkflowWithWorkflowRelative() throws IOException {
		final LocalRelativeToFileSystem fs = getWorkflowRelativeFS();
		final LocalRelativeToPath path = (LocalRelativeToPath) fs.getPath("../other-workflow/some-file.txt");
		assertFalse(fs.isLocalPathAccessible(path.toAbsoluteLocalPath()));
	}

	public void insideOtherWorkflowWithMountpointRelative() throws IOException {
		final LocalRelativeToFileSystem fs = getMountpointRelativeFS();
		final LocalRelativeToPath path = (LocalRelativeToPath) fs.getPath("/other-workflow/some-file.txt");
		assertFalse(fs.isLocalPathAccessible(path.toAbsoluteLocalPath()));
	}

	@Test
	public void isWorkflow() throws IOException {
		final LocalRelativeToFileSystem fs = getMountpointRelativeFS();
		assertFalse(fs.isWorkflow((LocalRelativeToPath) fs.getPath("/")));
		assertTrue(fs.isWorkflow((LocalRelativeToPath) fs.getPath("/current-workflow")));
		assertTrue(fs.isWorkflow((LocalRelativeToPath) fs.getPath("/other-workflow")));
	}

	@Test
	public void isWorkflowRelative() throws IOException {
		assertTrue(getWorkflowRelativeFS().isWorkflowRelativeFileSystem());
		assertFalse(getMountpointRelativeFS().isWorkflowRelativeFileSystem());
	}

	@Test
	public void isRegularFileWorkflowRelative() throws IOException {
		final LocalRelativeToFileSystem fs = getWorkflowRelativeFS();
		assertFalse(fs.isRegularFile((LocalRelativeToPath) fs.getPath("/")));
		assertFalse(fs.isRegularFile((LocalRelativeToPath) fs.getPath("/current-workflow")));
		assertTrue(fs.isRegularFile((LocalRelativeToPath) fs.getPath("/other-workflow")));

		final LocalRelativeToPath filePath = (LocalRelativeToPath) fs.getPath("/some-file.txt");
		Files.createFile(filePath);
		assertTrue(fs.isRegularFile(filePath));

		final LocalRelativeToPath directoryPath = (LocalRelativeToPath) fs.getPath("/some-directory");
		Files.createDirectory(directoryPath);
		assertFalse(fs.isRegularFile(directoryPath));
	}

	@Test
	public void isRegularFileMountpointRelative() throws IOException {
		final LocalRelativeToFileSystem fs = getMountpointRelativeFS();
		assertFalse(fs.isRegularFile((LocalRelativeToPath) fs.getPath("/")));
		assertTrue(fs.isRegularFile((LocalRelativeToPath) fs.getPath("/current-workflow")));
		assertTrue(fs.isRegularFile((LocalRelativeToPath) fs.getPath("/other-workflow")));

		final LocalRelativeToPath filePath = (LocalRelativeToPath) fs.getPath("/some-file.txt");
		Files.createFile(filePath);
		assertTrue(fs.isRegularFile(filePath));

		final LocalRelativeToPath directoryPath = (LocalRelativeToPath) fs.getPath("/some-directory");
		Files.createDirectory(directoryPath);
		assertFalse(fs.isRegularFile(directoryPath));
	}

	@Test
	public void equalsOnDifferentFS() throws IOException {
		final String filename = "/some-dir/some-file.txt";
		final LocalRelativeToFileSystem mountpointFS = getMountpointRelativeFS();
		final Path mountpointPath = mountpointFS.getPath(filename);
		final LocalRelativeToFileSystem workflowFS = getWorkflowRelativeFS();
		final Path workflowPath = workflowFS.getPath(filename);
		assertFalse(mountpointPath.equals(workflowPath));
		assertTrue(mountpointFS.getPath(filename).equals(mountpointPath));
		assertTrue(workflowFS.getPath(filename).equals(workflowPath));
	}

	@Test
	public void relativizeWorkflowRelative() throws IOException {
		final LocalRelativeToFileSystem fs = getWorkflowRelativeFS();
		assertEquals(fs.getPath("../some-directory/some-workflow"),
				fs.getWorkingDirectory().relativize(fs.getPath("/some-directory/some-workflow")));
	}

	@Test
	public void absolutWorkflowRelative() throws IOException {
		final LocalRelativeToFileSystem fs = getWorkflowRelativeFS();
		assertEquals(fs.getPath("/some-directory/some-workflow"),
				fs.getPath("../some-directory/some-workflow").toAbsolutePath().normalize());
	}

	@Test
	public void absolutMountpointRelative() throws IOException {
		final LocalRelativeToFileSystem fs = getMountpointRelativeFS();
		assertEquals(fs.getPath("/some-directory/some-workflow"),
				fs.getPath("/some-directory/some-workflow").toAbsolutePath().normalize());
	}

	private static LocalRelativeToFileSystem getMountpointRelativeFS() throws IOException {
		return LocalRelativeToFileSystemProvider.getOrCreateFileSystem(URI.create("knime://knime.mountpoint"));
	}

	private static LocalRelativeToFileSystem getWorkflowRelativeFS() throws IOException {
		return LocalRelativeToFileSystemProvider.getOrCreateFileSystem(URI.create("knime://knime.workflow"));
	}
}
