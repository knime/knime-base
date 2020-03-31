package org.knime.filehandling.core.testing.integrationtests.filesystemprovider;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.Test;
import org.knime.filehandling.core.testing.FSTestInitializer;
import org.knime.filehandling.core.testing.integrationtests.AbstractParameterizedFSTest;

/**
 * Test class for create operations on file systems.
 * 
 * @author Tobias Urhaug, KNIME GmbH, Berlin, Germany
 */
public class CreateTest extends AbstractParameterizedFSTest {

	public CreateTest(String fsType, FSTestInitializer testInitializer) {
		super(fsType, testInitializer);
	}
	
	@Test
	public void test_create_file() throws IOException {
		Path path = m_testInitializer.getRoot().resolve("file");
		
		assertFalse(Files.exists(path));
		Files.createFile(path);
		assertTrue(Files.exists(path));
	}
	
	@Test
	public void test_create_file_on_path() throws IOException {
		Path path = m_testInitializer.getRoot().resolve("path").resolve("to").resolve("file");
		
		Files.createDirectories(path.getParent());
		Files.createFile(path);
		assertTrue(Files.exists(path));
	}
	
	@Test (expected = FileAlreadyExistsException.class)
	public void test_create_file_which_already_exists() throws Exception {
		Path file = m_testInitializer.createFileWithContent("content", "existing", "file");
		
		Files.createFile(file);
	}
	
	@Test
	public void test_create_directory() throws Exception {
		ignoreWithReason("S3 has no directory concept", S3);
		Path pathToDriectory = m_testInitializer.getRoot().resolve("directory");
		
		Path directory = Files.createDirectory(pathToDriectory);
		
		assertTrue(Files.exists(directory));
		assertTrue(Files.isDirectory(directory));
	}
	
	@Test
	public void test_create_directories() throws Exception {
		ignoreWithReason("S3 has no directory concept", S3);
		Path pathToDirectory = m_testInitializer.getRoot().resolve("path").resolve("to").resolve("directory");
		
		Path directory = Files.createDirectories(pathToDirectory);
		
		assertTrue(Files.exists(directory));
		assertTrue(Files.isDirectory(directory));
		assertTrue(Files.exists(directory.getParent()));
		assertTrue(Files.isDirectory(directory.getParent()));
	}
	
	@Test (expected = FileAlreadyExistsException.class)
	public void test_create_directory_which_already_exists() throws Exception {
		Path file = m_testInitializer.createFile("directory", "file");
		Path directory = file.getParent();
		
		Files.createDirectory(directory);
	}

}
