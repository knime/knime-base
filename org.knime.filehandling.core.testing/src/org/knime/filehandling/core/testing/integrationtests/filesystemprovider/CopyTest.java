package org.knime.filehandling.core.testing.integrationtests.filesystemprovider;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

import org.junit.Test;
import org.knime.filehandling.core.testing.FSTestInitializer;
import org.knime.filehandling.core.testing.integrationtests.AbstractParameterizedFSTest;

/**
 * Test class for copy operations on file systems.
 * 
 * @author Tobias Urhaug, KNIME GmbH, Berlin, Germany
 *
 */
public class CopyTest extends AbstractParameterizedFSTest {

	public CopyTest(final String fsType, final FSTestInitializer testInitializer) {
		super(fsType, testInitializer);
	}

	@Test
	public void test_copy_file() throws Exception {
		final String testContent = "Some simple test content";
		final Path source = m_testInitializer.createFileWithContent(testContent, "dir", "file");
		final Path target = source.getParent().resolve("copiedFile");
		
		Files.copy(source, target);
		
		assertTrue(Files.exists(target));
		final List<String> copiedContent = Files.readAllLines(target);
		assertEquals(1, copiedContent.size());
		assertEquals(testContent, copiedContent.get(0));
	}

	@Test (expected = FileAlreadyExistsException.class)
	public void test_copy_file_to_existing_target_without_replace_option() throws Exception {
		final String testContent = "Some simple test content";
		final Path source = m_testInitializer.createFileWithContent(testContent, "dir", "file");
		final Path target = m_testInitializer.createFileWithContent(testContent, "dir", "copyFile");
		
		Files.copy(source, target);
	}

	@Test
	public void test_copy_file_to_existing_target_with_replace_option() throws Exception {
		final String sourceContent = "Source content";
		final Path source = m_testInitializer.createFileWithContent(sourceContent, "dir", "file");
		final String targetContent = "Target content";
		final Path target = m_testInitializer.createFileWithContent(targetContent, "dir", "copyFile");
		
		Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);

		assertTrue(Files.exists(target));
		final List<String> copiedContent = Files.readAllLines(target);
		assertEquals(1, copiedContent.size());
		assertEquals(sourceContent, copiedContent.get(0));
	}
	
	@Test
	public void test_copy_file_to_non_existing_directory() throws Exception {
		final String testContent = "Some simple test content";
		final Path source = m_testInitializer.createFileWithContent(testContent, "dir", "file");
		final Path target = source.getParent().resolve("newDir").resolve("copiedFile");
		
		// On a local system all directories must be created, otherwise a NoSuchFileException will be thrown.
		Files.createDirectories(target.getParent());
		Files.copy(source, target);
		
		assertTrue(Files.exists(target));
		final List<String> copiedContent = Files.readAllLines(target);
		assertEquals(1, copiedContent.size());
		assertEquals(testContent, copiedContent.get(0));
	}

	@Test (expected = DirectoryNotEmptyException.class)
	public void test_copy_file_to_non_empty_existing_directory() throws Exception {
		final String testContent = "Some simple test content";
		final Path source = m_testInitializer.createFileWithContent(testContent, "dirA", "fileA");
		final Path nonEmptyDirectory = m_testInitializer.createFileWithContent(testContent, "dirB", "fileB").getParent();
		
		Files.copy(source.getParent(), nonEmptyDirectory, StandardCopyOption.REPLACE_EXISTING);
	}
	
	@Test
	public void test_copy_file_to_itself() throws Exception {
		final String testContent = "Some simple test content";
		final Path source = m_testInitializer.createFileWithContent(testContent, "dirA", "fileA");
		
		Files.copy(source, source, StandardCopyOption.REPLACE_EXISTING);

		assertTrue(Files.exists(source));
		final List<String> copiedContent = Files.readAllLines(source);
		assertEquals(1, copiedContent.size());
		assertEquals(testContent, copiedContent.get(0));
	}
	
}
