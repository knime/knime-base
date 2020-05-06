package org.knime.filehandling.core.testing.integrationtests.filesystemprovider;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;

import org.junit.Test;
import org.knime.filehandling.core.testing.FSTestInitializer;
import org.knime.filehandling.core.testing.integrationtests.AbstractParameterizedFSTest;

/**
 * Test class for delete operations on file systems.
 *
 * @author Tobias Urhaug, KNIME GmbH, Berlin, Germany
 *
 */
public class DeleteTest extends AbstractParameterizedFSTest {

    public DeleteTest(final String fsType, final FSTestInitializer testInitializer) {
        super(fsType, testInitializer);
    }

    @Test
    public void test_delete_file() throws IOException {
        final Path file = m_testInitializer.createFile("path", "to", "file");
        Files.delete(file);
        assertFalse(Files.exists(file));
    }

    @Test
    public void test_delete_empty_directory() throws IOException {
        final Path file = m_testInitializer.createFile("folder", "with", "file");
        Files.delete(file);

        // delete the parent folder, obtained using the getParent() method
        Files.delete(file.getParent());
        assertFalse(Files.exists(file.getParent()));

        // delete the parent of the parent, obtained by making a new path object
        final Path parentParent = m_testInitializer.makePath("folder");
        Files.delete(parentParent);
        assertFalse(Files.exists(parentParent));
    }

    @Test
    public void test_parent_of_deleted_file_is_not_deleted() throws IOException {
        final Path file = m_testInitializer.createFile("path", "to", "file");

        Files.delete(file);

        assertTrue(Files.exists(file.getParent()));
    }

    @Test(expected = DirectoryNotEmptyException.class)
    public void test_deleting_a_non_empty_directory_throws_an_exception() throws Exception {
        final Path file = m_testInitializer.createFile("directory", "fileA");
        final Path directory = file.getParent();

        Files.delete(directory);
    }

    @Test
    public void test_deleting_a_non_empty_directory_does_not_delete_it() throws Exception {
        final Path file = m_testInitializer.createFile("directory", "fileA");
        final Path directory = file.getParent();

        try {
            Files.delete(directory);
            Files.deleteIfExists(directory);
        } catch (final DirectoryNotEmptyException e) {
            // do nothing, this exception is expected from some file systems in this test
            // case
        }

        assertTrue(Files.exists(directory));
        assertTrue(Files.exists(file));
    }

    @Test(expected = NoSuchFileException.class)
    public void test_delete_non_existing_file_throws_an_exception() throws Exception {
        final Path pathToNonExistingFile = m_testInitializer.makePath("does", "not", "exist");

        Files.delete(pathToNonExistingFile);
    }

}
