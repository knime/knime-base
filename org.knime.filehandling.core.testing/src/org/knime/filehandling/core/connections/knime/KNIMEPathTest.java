package org.knime.filehandling.core.connections.knime;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.lang3.SystemUtils;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.knime.filehandling.core.connections.base.UnixStylePathUtil;
import org.knime.filehandling.core.defaultnodesettings.KNIMEConnection;

public class KNIMEPathTest {
	
	@Rule
	public TemporaryFolder m_tempFolder = new TemporaryFolder();
	
	private File m_workspaceFolder;
	private File m_workflowFolder;
	private File m_nodeFolder;
	
	private static final String WORKSPACE_NAME = "workspace"; 
	private static final String WORKFLOW_NAME = "workflow"; 
	private static final String NODE_NAME = "node"; 
	
	private KNIMEFileSystemProvider m_fsProvider;
	
	@Before
	public void setup() throws IOException {
		m_fsProvider = KNIMEFileSystemProvider.getInstance();
		m_nodeFolder = m_tempFolder.newFolder(WORKSPACE_NAME, WORKFLOW_NAME, NODE_NAME);
		m_workflowFolder = m_nodeFolder.getParentFile();
		m_workspaceFolder = m_workflowFolder.getParentFile();
	}
	
	/**
	 * 
	 */
	@Test
	public void a_paths_file_system_can_be_retrieved() {
		KNIMEFileSystem workflowRelativeFS = workflowRelativeFS();
		KNIMEPath knimePath = new KNIMEPath(workflowRelativeFS, "../../my-data/data.txt");
		assertTrue(knimePath.getFileSystem() == workflowRelativeFS);
	}
	
	/**
	 * 
	 */
	@Test
	public void a_relative_KNIME_path_is_not_absolute() {
		KNIMEPath absoluteKnimePath = new KNIMEPath(workflowRelativeFS(), "../my-data/data.txt");
		assertFalse(absoluteKnimePath.isAbsolute());
	}
	
	/**
	 * 
	 */
	@Test
	public void an_absolute_KNIME_path_is_absolute() {
		Assume.assumeTrue(SystemUtils.IS_OS_WINDOWS);
		KNIMEPath absoluteKnimePath = new KNIMEPath(workflowRelativeFS(), "C:/my-data/data.txt");
		assertTrue(absoluteKnimePath.isAbsolute());
	}
	
	@Test
	public void a_file_name_can_be_retrieved() throws Exception {
		KNIMEPath knimePath = new KNIMEPath(workflowRelativeFS(), "../my-data/data.txt");
		
		KNIMEPath fileName = new KNIMEPath(workflowRelativeFS(), "data.txt");
		assertTrue(knimePath.getFileName().equals(fileName));
		assertTrue(knimePath.getFileName() instanceof KNIMEPath);
	}
	
	@Test
	public void path_component_count() {
		KNIMEPath path = new KNIMEPath(localMountPointRelativeFS(), "my-folder/hello/world/data.txt");
		assertEquals(4, path.getNameCount());
	}
	
	@Test
	public void get_parent_returns_the_absolute_path() {
		KNIMEPath knimePath = new KNIMEPath(localMountPointRelativeFS(), "my-folder/hello/world/data.txt");
		KNIMEPath parent = 
			new KNIMEPath(
				localMountPointRelativeFS(), 
				m_workspaceFolder.getAbsolutePath(), 
				"my-folder/hello/world/"
			);
		assertEquals(parent, knimePath.getParent());
		assertTrue(knimePath instanceof KNIMEPath);
	}
	
	@Test
	public void a_subpath_can_be_created() {
		KNIMEPath knimePath = new KNIMEPath(localMountPointRelativeFS(), "my-folder/hello/world/data.txt");
		KNIMEPath subpath = new KNIMEPath(localMountPointRelativeFS(), "hello/world/");
		assertEquals(subpath, knimePath.subpath(1, 3));
		assertTrue(knimePath instanceof KNIMEPath);
	}
	
	@Test
	public void a_subpath_equal_to_the_whole_path() {
		KNIMEPath knimePath = new KNIMEPath(localMountPointRelativeFS(), "my-folder/hello/world/data.txt");
		KNIMEPath subpath = new KNIMEPath(localMountPointRelativeFS(), "my-folder/hello/world/data.txt");
		assertEquals(subpath, knimePath.subpath(0, 4));
		assertTrue(knimePath instanceof KNIMEPath);
	}
	
	@Test
	public void a_subpath_equal_to_a_single_element() {
		KNIMEPath knimePath = new KNIMEPath(localMountPointRelativeFS(), "/my-folder/hello/world/data.txt");
		KNIMEPath subpath = new KNIMEPath(localMountPointRelativeFS(), "data.txt");
		assertEquals(subpath, knimePath.subpath(3, 4));
		assertTrue(knimePath instanceof KNIMEPath);
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void a_subpath_starting_below_the_range_throws_exception() {
		KNIMEPath knimePath = new KNIMEPath(localMountPointRelativeFS(), "my-folder/hello/world/data.txt");

		knimePath.subpath(-1, 4);
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void  a_subpath_starting_above_the_range_throws_exception() {
		KNIMEPath knimePath = new KNIMEPath(localMountPointRelativeFS(), "my-folder/hello/world/data.txt");
		
		knimePath.subpath(0, 5);
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void a_subpaths_end_index_must_be_greater_than_begin_index() {
		KNIMEPath knimePath = new KNIMEPath(localMountPointRelativeFS(), "my-folder/hello/world/data.txt");
		
		knimePath.subpath(2, 2);
	}
	
	@Test
	public void a_path_starts_with_itself() {
		String stringPath = "my-folder/hello/world/data.txt";
		KNIMEPath knimePath = new KNIMEPath(localMountPointRelativeFS(), stringPath);
		
		assertTrue(knimePath.startsWith(knimePath));
		assertTrue(knimePath.startsWith(stringPath));
	}
	
	@Test
	public void a_path_starts_with_its_first_component() {
		String stringPath = "my-folder/hello/world/data.txt";
		String stringRoot = "my-folder";
		KNIMEFileSystem localMountPointRelativeFS = localMountPointRelativeFS();
		KNIMEPath path = new KNIMEPath(localMountPointRelativeFS, stringPath);
		KNIMEPath root = new KNIMEPath(localMountPointRelativeFS, stringRoot);
		
		assertTrue(path.startsWith(root));
		assertTrue(path.startsWith(stringRoot));
	}
	
	@Test
	public void a_path_starts_with_a_subpath_from_index_zero() {
		String stringPath = "my-folder/hello/world/data.txt";
		String stringSubPath = "my-folder/hello";
		KNIMEFileSystem localMountPointRelativeFS = localMountPointRelativeFS();
		KNIMEPath path = new KNIMEPath(localMountPointRelativeFS, stringPath);
		KNIMEPath subPath = new KNIMEPath(localMountPointRelativeFS, stringSubPath);
		
		assertTrue(path.startsWith(subPath));
		assertTrue(path.startsWith(stringSubPath));
	}
	
	@Test
	public void a_path_does_not_start_with_a_path_with_different_compononents() {
		String stringPath = "my-folder/hello/world/data.txt";
		String otherStringPath = "other-folder/hello";
		KNIMEFileSystem localMountPointRelativeFS = localMountPointRelativeFS();
		KNIMEPath path = new KNIMEPath(localMountPointRelativeFS, stringPath);
		KNIMEPath otherPath = new KNIMEPath(localMountPointRelativeFS, otherStringPath);
		
		assertFalse(path.startsWith(otherPath));
		assertFalse(path.startsWith(otherStringPath));
	}
	
	@Test
	public void a_path_does_not_start_with_a_path_from_a_different_file_system() {
		String stringPath = "my-folder/hello/world/data.txt";
		KNIMEPath path = new KNIMEPath(localMountPointRelativeFS(), stringPath);
		KNIMEPath pathOnOtherFileSystem = new KNIMEPath(nodeRelativeFS(), stringPath);
		
		assertFalse(path.startsWith(pathOnOtherFileSystem));
	}

	@Test
	public void a_path_ends_with_itself() {
		String stringPath = "my-folder/hello/world/data.txt";
		KNIMEPath knimePath = new KNIMEPath(localMountPointRelativeFS(), stringPath);
		
		assertTrue(knimePath.endsWith(knimePath));
		assertTrue(knimePath.endsWith(stringPath));
	}
	
	@Test
	public void a_path_ends_with_its_file_name() {
		String stringPath = "my-folder/hello/world/data.txt";
		String stringFileName = "data.txt";
		KNIMEFileSystem localMountPointRelativeFS = localMountPointRelativeFS();
		KNIMEPath knimePath = new KNIMEPath(localMountPointRelativeFS, stringPath);
		KNIMEPath fileName = new KNIMEPath(localMountPointRelativeFS, stringFileName);
		
		assertTrue(knimePath.endsWith(fileName));
		assertTrue(knimePath.endsWith(stringFileName));
	}
	
	@Test
	public void a_path_ends_with_its_last_components() {
		String stringPath = "my-folder/hello/world/data.txt";
		String stringLastComponents = "hello/world/data.txt";
		KNIMEFileSystem localMountPointRelativeFS = localMountPointRelativeFS();
		KNIMEPath knimePath = new KNIMEPath(localMountPointRelativeFS, stringPath);
		KNIMEPath lastComponents = new KNIMEPath(localMountPointRelativeFS, stringLastComponents);
		
		assertTrue(knimePath.endsWith(lastComponents));
		assertTrue(knimePath.endsWith(stringLastComponents));
	}
	
	@Test
	public void a_path_does_not_end_with_a_different_file_name() {
		String stringPath = "my-folder/hello/world/data.txt";
		String stringDifferentFileName = "world/other-data.txt";
		KNIMEFileSystem localMountPointRelativeFS = localMountPointRelativeFS();
		KNIMEPath knimePath = new KNIMEPath(localMountPointRelativeFS, stringPath);
		KNIMEPath differentFileNamePath = new KNIMEPath(localMountPointRelativeFS, stringDifferentFileName);
		
		assertFalse(knimePath.endsWith(differentFileNamePath));
		assertFalse(knimePath.endsWith(stringDifferentFileName));
	}
	
	@Test
	public void a_path_does_not_end_with_a_path_from_a_different_file_system() {
		String stringPath = "my-folder/hello/world/data.txt";
		KNIMEPath path = new KNIMEPath(localMountPointRelativeFS(), stringPath);
		KNIMEPath pathOnOtherFileSystem = new KNIMEPath(nodeRelativeFS(), stringPath);
		
		assertFalse(path.endsWith(pathOnOtherFileSystem));
	}
	
	@Test
	public void normalize() {
		String stringPath = "my-folder/hello/../hello/world/data.txt";
		String stringNormalizedPath = "my-folder/hello/world/data.txt";
		KNIMEFileSystem localMountPointRelativeFS = localMountPointRelativeFS();
		KNIMEPath path = new KNIMEPath(localMountPointRelativeFS, stringPath);
		KNIMEPath normalizedPath = new KNIMEPath(localMountPointRelativeFS, stringNormalizedPath);
		
		assertEquals(normalizedPath, path.normalize());
	}
	
	@Test (expected = IllegalArgumentException.class)
	public void paths_on_different_file_system_cannot_be_resolved_against_each_other() {
		String stringPath = "my-folder/hello/world/data.txt";
		KNIMEPath path = new KNIMEPath(localMountPointRelativeFS(), stringPath);
		KNIMEPath pathOnDifferentFS = new KNIMEPath(nodeRelativeFS(), stringPath);
		
		path.resolve(pathOnDifferentFS);
	}
	
	@Test
	public void resolving_other_path_appends_it() {
		String firstStringPath = "my-folder/hello/";
		KNIMEFileSystem localMountPointRelativeFS = localMountPointRelativeFS();
		KNIMEPath path = new KNIMEPath(localMountPointRelativeFS, firstStringPath);
		String otherStringPath = "world/data.txt";
		KNIMEPath otherPath = new KNIMEPath(localMountPointRelativeFS, otherStringPath);
		
		KNIMEPath expectedPath = new KNIMEPath(localMountPointRelativeFS, "my-folder/hello/world/data.txt");

		assertEquals(expectedPath, path.resolve(otherPath));
		assertEquals(expectedPath, path.resolve(otherStringPath));
	}
	
	@Test
	public void resolving_sibling() {
		String firstStringPath = "my-folder/some-data.csv";
		KNIMEFileSystem localMountPointRelativeFS = localMountPointRelativeFS();
		KNIMEPath path = new KNIMEPath(localMountPointRelativeFS, firstStringPath);
		String otherStringPath = "other-folder/data.txt";
		KNIMEPath otherPath = new KNIMEPath(localMountPointRelativeFS, otherStringPath);
		
		KNIMEPath expectedPath = new KNIMEPath(localMountPointRelativeFS, "my-folder/other-folder/data.txt");
		
		assertEquals(expectedPath, path.resolveSibling(otherPath));
		assertEquals(expectedPath, path.resolveSibling(otherStringPath));
	}
	
	@Test
	public void relativize() {
		KNIMEFileSystem localMountPointRelativeFS = localMountPointRelativeFS();
		KNIMEPath path = new KNIMEPath(localMountPointRelativeFS, "my-folder/");
		KNIMEPath otherPath = new KNIMEPath(localMountPointRelativeFS, "my-folder/other-folder/data.txt");
		
		KNIMEPath expectedPath = new KNIMEPath(localMountPointRelativeFS, "other-folder/data.txt");
		assertEquals(expectedPath, path.relativize(otherPath));
	}
	
	@Test
	public void the_aboslute_path_is_resolved_against_the_file_systems_base_location() {
		KNIMEPath path = new KNIMEPath(localMountPointRelativeFS(), "my-folder/");
		String expected = m_workspaceFolder.getAbsolutePath() + "/my-folder";
		KNIMEPath absolutePath = new KNIMEPath(localMountPointRelativeFS(), expected);
		
		assertEquals(absolutePath, path.toAbsolutePath());
	}
	
	@Test
	public void to_real_path_returnes_absolute_and_normalized_path() throws IOException {
		String stringPath = "my-folder/hello/../hello/world/data.txt";
		KNIMEFileSystem localMountPointRelativeFS = localMountPointRelativeFS();
		KNIMEPath path = new KNIMEPath(localMountPointRelativeFS, stringPath);
		
		KNIMEPath expected = new KNIMEPath(localMountPointRelativeFS, m_workspaceFolder.getAbsolutePath(), "my-folder/hello/world/data.txt");
		
		assertEquals(expected, path.toRealPath());
	}
	
	@Test
	public void to_File() {
		KNIMEPath path = new KNIMEPath(localMountPointRelativeFS(), "my-folder/hello world/data.txt");
		File file = path.toFile();
		
		String unixStylePath = UnixStylePathUtil.asUnixStylePath(file.getPath());

		assertEquals("my-folder/hello world/data.txt", unixStylePath);
	}
	
	@Test(expected = UnsupportedOperationException.class)
	public void watch_keys_are_unsupported() throws IOException {
		KNIMEPath path = new KNIMEPath(localMountPointRelativeFS(), "my-folder/hello/world/data.txt");
		path.register(null);
	}
	
	@Test
	public void more_contains_name_seperated_path() {
		String first = "first";
		String second = "second/folder";
		String lastWindowsStyle = "windows\\data.txt";		
		
		KNIMEPath knimePath = new KNIMEPath(localMountPointRelativeFS(), first, second, lastWindowsStyle);
		
		assertEquals(5, knimePath.getNameCount());
		assertEquals("first", knimePath.getName(0).toString());
		assertEquals("second", knimePath.getName(1).toString());
		assertEquals("folder", knimePath.getName(2).toString());
		assertEquals("windows", knimePath.getName(3).toString());
		assertEquals("data.txt", knimePath.getName(4).toString());
	}
	
	@Test
	public void absolute_local_path() {
		KNIMEPath absoluteLocalPath = 
			new KNIMEPath(
				localMountPointRelativeFS(), 
				m_workspaceFolder.getAbsolutePath(), 
				"my-folder/hello/world/data.txt"
			);
		
		Path localPath = absoluteLocalPath.toLocalPath();
		Path expected = Paths.get(m_workspaceFolder.getAbsolutePath(), "my-folder/hello/world/data.txt");
		
		assertFalse(localPath.equals(absoluteLocalPath));
		assertEquals(expected, localPath);
	}
	
	@Test
	public void relative_local_path() {
		KNIMEPath absoluteLocalPath = new KNIMEPath(localMountPointRelativeFS(), "my-folder/hello/world/data.txt");
		
		Path localPath = absoluteLocalPath.toLocalPath();
		Path expected = Paths.get(m_workspaceFolder.getAbsolutePath(), "my-folder/hello/world/data.txt");
		
		assertFalse(localPath.equals(absoluteLocalPath));
		assertEquals(expected, localPath);
	}
	
	private KNIMEFileSystem localMountPointRelativeFS() {
		String workspaceAbsPath = m_workspaceFolder.getAbsolutePath().replaceAll("\\\\", "/");
		URI workspaceURI = Paths.get(workspaceAbsPath).toUri();
		return new KNIMEFileSystem(m_fsProvider, workspaceURI, KNIMEConnection.Type.MOUNTPOINT_RELATIVE);
	}
	
	private KNIMEFileSystem workflowRelativeFS() {
		String workflowAbsPath = m_workflowFolder.getAbsolutePath().replaceAll("\\\\", "/");
		URI workflowURI = Paths.get(workflowAbsPath).toUri();
		return new KNIMEFileSystem(m_fsProvider, workflowURI, KNIMEConnection.Type.WORKFLOW_RELATIVE);
	}
	
	private KNIMEFileSystem nodeRelativeFS() {
		String nodeAbsPath = m_nodeFolder.getAbsolutePath().replaceAll("\\\\", "/");
		URI nodeURI = Paths.get(nodeAbsPath).toUri();
		return new KNIMEFileSystem(m_fsProvider, nodeURI, KNIMEConnection.Type.NODE_RELATIVE);
	}
	
}
