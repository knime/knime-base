package org.knime.filehandling.core.testing.integrationtests.path;

import static org.junit.Assert.assertEquals;

import java.nio.file.FileSystem;
import java.nio.file.Path;

import org.junit.Test;
import org.knime.filehandling.core.testing.FSTestInitializer;
import org.knime.filehandling.core.testing.integrationtests.AbstractParameterizedFSTest;

public class PathTest extends AbstractParameterizedFSTest {
	
	public PathTest(String fsType, FSTestInitializer testInitializer) {
		super(fsType, testInitializer);
	}
	
	@Test
	public void testFileName() {
		FileSystem fileSystem = m_connection.getFileSystem();
		Path path = fileSystem.getPath("path", "to", "file");
		
		assertEquals("file", path.getFileName().toString());
	}
	
	@Test
	public void testNameCount() {
		FileSystem fileSystem = m_connection.getFileSystem();
		Path path = fileSystem.getPath("path", "to", "file");
		
		assertEquals(3, path.getNameCount());
	}
	
}
