package org.knime.filehandling.core.testing.integrationtests.filesystemprovider;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

import org.junit.Test;
import org.knime.filehandling.core.testing.FSTestInitializer;
import org.knime.filehandling.core.testing.integrationtests.AbstractParameterizedFSTest;

/**
 * Test class for move operations on file systems.
 * 
 * @author Tobias Urhaug, KNIME GmbH, Berlin, Germany
 *
 */
public class MoveTest extends AbstractParameterizedFSTest {

	public MoveTest(String fsType, FSTestInitializer testInitializer) {
		super(fsType, testInitializer);
	}

	@Test
	public void test_move_file() throws Exception {
		String sourceContent = "Some simple test content";
		Path source = m_testInitializer.createFileWithContent(sourceContent, "dir", "file");
		Path target = source.getParent().resolve("movedFile");
		
		Files.move(source, target);
		
		assertFalse(Files.exists(source));
		assertTrue(Files.exists(target));
		List<String> movedContent = Files.readAllLines(target);
		assertEquals(sourceContent, movedContent.get(0));
	}

	@Test (expected = NoSuchFileException.class)
	public void test_move_non_existing_file() throws Exception {
		final Path source = m_testInitializer.getRoot().resolve("non-existing-file");
		final Path target = source.getParent().resolve("movedFile");

		Files.move(source, target);
	}

	@Test (expected = FileAlreadyExistsException.class)
	public void test_move_file_to_already_existing_file_without_replace_throws_exception() throws Exception {
		String sourceContent = "The source content";
		Path source = m_testInitializer.createFileWithContent(sourceContent, "dir", "file");
		String targetContent = "The target content";
		Path existingTarget = m_testInitializer.createFileWithContent(targetContent, "dir", "target");
		
		Files.move(source, existingTarget);
	}
	
	@Test
	public void test_move_file_to_already_existing_file_with_replace() throws Exception {
		String sourceContent = "The source content";
		Path source = m_testInitializer.createFileWithContent(sourceContent, "dir", "file");
		String targetContent = "The target content";
		Path existingTarget = m_testInitializer.createFileWithContent(targetContent, "dir", "target");
		
		Files.move(source, existingTarget, StandardCopyOption.REPLACE_EXISTING);
		
		assertFalse(Files.exists(source));
		assertTrue(Files.exists(existingTarget));
		List<String> movedContent = Files.readAllLines(existingTarget);
		assertEquals(sourceContent, movedContent.get(0));
	}
	
	@Test (expected = NoSuchFileException.class)
	public void test_move_file_to_non_existing_directory_throws_exception() throws Exception {
		String sourceContent = "The source content";
		Path source = m_testInitializer.createFileWithContent(sourceContent, "dir", "fileA");
		Path target = m_testInitializer.getRoot().resolve("dirB").resolve("fileB");
		
		Files.move(source, target);
	}
	
	@Test
	public void test_renaming_a_file() throws Exception {
		String sourceContent = "The source content";
		Path source = m_testInitializer.createFileWithContent(sourceContent, "dir", "fileA");
		Path target = source.getParent().resolve("fileB");

		Files.move(source, target);
		
		assertFalse(Files.exists(source));
		assertTrue(Files.exists(target));
		List<String> renamedContent = Files.readAllLines(target);
		assertEquals(sourceContent, renamedContent.get(0));
	}
	
}