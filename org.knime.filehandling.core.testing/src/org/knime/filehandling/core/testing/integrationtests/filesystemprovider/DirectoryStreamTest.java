package org.knime.filehandling.core.testing.integrationtests.filesystemprovider;

import static org.junit.Assert.assertTrue;

import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.knime.filehandling.core.testing.FSTestInitializer;
import org.knime.filehandling.core.testing.integrationtests.AbstractParameterizedFSTest;

/**
 * Test class for file system directory streams.
 *  
 * @author Tobias Urhaug, KNIME GmbH, Berlin, Germany
 *
 */
public class DirectoryStreamTest extends AbstractParameterizedFSTest {

	public DirectoryStreamTest(String fsType, FSTestInitializer testInitializer) {
		super(fsType, testInitializer);
	}
	
	@Test
	public void test_list_files_in_directory() throws Exception {
		Path fileA = m_testInitializer.createFileWithContent("contentA", "dir", "fileA");
		Path fileB = m_testInitializer.createFileWithContent("contentB", "dir", "fileB");
		Path fileC = m_testInitializer.createFileWithContent("contentC", "dir", "fileC");
		Path directory = fileA.getParent();
		
		List<Path> paths = new ArrayList<>();
		try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(directory, (path) -> true)) {
			directoryStream.forEach(paths::add);
		}
		
		assertTrue(paths.contains(fileA));
		assertTrue(paths.contains(fileB));
		assertTrue(paths.contains(fileC));
	}
	
	@Test
	public void test_list_emtpy_directory() throws Exception {
		Path directory = m_testInitializer.getRoot();
		// root directory might contains files (e.g. workflows), use a fresh empty directory
		Path emptyDirectory = Files.createDirectories(directory.resolve("empty-directory"));
		
		List<Path> paths = new ArrayList<>();
		try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(emptyDirectory, (path) -> true)) {
			directoryStream.forEach(paths::add);
		}
		
		assertTrue(paths.isEmpty());
	}

	@Test (expected = NoSuchFileException.class)
	public void test_non_existent_directory() throws Exception {
		Path directory = m_testInitializer.getRoot().resolve("doesnotexist");
		
		List<Path> paths = new ArrayList<>();
		try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(directory, (path) -> true)) {
			directoryStream.forEach(paths::add);
		}
		
		assertTrue(paths.isEmpty());
	}
}
