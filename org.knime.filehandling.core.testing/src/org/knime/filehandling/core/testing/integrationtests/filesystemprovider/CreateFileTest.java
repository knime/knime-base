package org.knime.filehandling.core.testing.integrationtests.filesystemprovider;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.knime.filehandling.core.testing.FSTestInitializer;
import org.knime.filehandling.core.testing.integrationtests.AbstractParameterizedFSTest;

/**
 * 
 * @author Tobias Urhaug, KNIME GmbH, Berlin, Germany
 */
public class CreateFileTest extends AbstractParameterizedFSTest {

	public CreateFileTest(String fsType, FSTestInitializer testInitializer) {
		super(fsType, testInitializer);
	}
	
	@Test
	public void testCreateFile() throws IOException {
		FileSystem fileSystem = m_connection.getFileSystem();
		Path path = fileSystem.getPath(m_testInitializer.getRoot().toString(), "file");
		
		Files.createDirectories(path.getParent());
		Files.createFile(path);
		assertTrue(Files.exists(path));
	}
	
	@Test
	public void testCreateFileOnPath() throws IOException {
		FileSystem fileSystem = m_connection.getFileSystem();
		Path path = fileSystem.getPath(m_testInitializer.getRoot().toString(), "path", "to", "file");
		
		Files.createDirectories(path.getParent());
		Files.createFile(path);
		assertTrue(Files.exists(path));
	}
	
	@Test
	public void testListFilesInDirectory() throws Exception {
		FileSystem fileSystem = m_connection.getFileSystem();
		String testRoot = m_testInitializer.getRoot().toString();
		
		Path fileA = fileSystem.getPath(testRoot, "path", "to", "fileA");
		Files.createDirectories(fileA.getParent());
		Files.createFile(fileA);
		Path fileB = fileSystem.getPath(testRoot, "path", "to", "fileB");
		Files.createDirectories(fileB.getParent());
		Files.createFile(fileB);
		Path fileC = fileSystem.getPath(testRoot, "path", "to", "fileC");
		Files.createDirectories(fileC.getParent());
		Files.createFile(fileC);
		
		List<Path> paths = new ArrayList<>();
		try (DirectoryStream<Path> directoryStream = 
				fileSystem.provider().newDirectoryStream(fileA.getParent(), (path) -> true)) {
			directoryStream.forEach(path -> paths.add(path));
		}
		
		assertTrue(paths.contains(fileA));
		assertTrue(paths.contains(fileB));
		assertTrue(paths.contains(fileC));
	}

}
