/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ---------------------------------------------------------------------
 */
package org.knime.filehandling.core.fs.tests.integration.filesystemprovider;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.junit.Test;
import org.knime.filehandling.core.fs.tests.integration.AbstractParameterizedFSTest;
import org.knime.filehandling.core.testing.FSTestInitializer;
import org.knime.filehandling.core.util.IOESupplier;

/**
 * Test class for delete operations on file systems.
 *
 * @author Tobias Urhaug, KNIME GmbH, Berlin, Germany
 *
 */
public class DeleteTest extends AbstractParameterizedFSTest {

    public DeleteTest(final String fsType, final IOESupplier<FSTestInitializer> testInitializer) throws IOException {
        super(fsType, testInitializer);
    }

    private void testCreateAndDeleteFile(final Path file) throws IOException {
        final Path fileParent = file.getParent();

        // list prarent dir-A
        final List<Path> before = new ArrayList<>();
        try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(fileParent, path -> true)) {
            directoryStream.forEach(before::add);
        }
        assertTrue(before.contains(file));
        assertEquals(1, before.size());

        // ensure file exist before
        assertTrue(Files.isRegularFile(file));
        assertTrue(Files.exists(file));

        // delete file-B
        Files.delete(file);

        // ensure file does not exist anymore
        assertFalse(Files.isRegularFile(file));
        assertFalse(Files.exists(file));

        if (!m_fsType.equals(AMAZON_S3_COMPATIBLE)) { // empty directories are removed automatically by MinIO
            // list dir-A again and ensure that file-B is not listed anymore
            try (Stream<Path> directoryStream = Files.list(fileParent)) {
                assertEquals(0, directoryStream.count());
            }
        }
    }

    @Test
    public void test_delete_file() throws IOException {
        testCreateAndDeleteFile(m_testInitializer.createFile("dir-A", "file-B"));
    }

    public void test_delete_file_with_spaces() throws IOException {
        testCreateAndDeleteFile(m_testInitializer.createFile("dir A", "file B"));
    }

    public void test_delete_file_with_pluses() throws IOException {
        testCreateAndDeleteFile(m_testInitializer.createFile("dir+A", "file+B"));
    }

    public void test_delete_file_with_percent_encoding() throws IOException {
        testCreateAndDeleteFile(m_testInitializer.createFile("dir%20with%20percent%2520encodings2", "file%20with%20percent%2520encodings"));
    }

    @Test
    public void test_delete_file_by_relative_path() throws IOException {
        final Path fileB = m_testInitializer.createFileWithContent("test", "dir-A", "file-B");
        final Path fileBRelative = getFileSystem().getWorkingDirectory().relativize(fileB);

        // ensure file exist before
        assertTrue(Files.isRegularFile(fileB));
        assertTrue(Files.exists(fileB));

        // delete file-B
        Files.delete(fileBRelative);

        // ensure file does not exist anymore
        assertFalse(Files.isRegularFile(fileB));
        assertFalse(Files.exists(fileB));
    }

    @Test
    public void test_delete_empty_directory() throws IOException {
        ignoreWithReason("empty directories are removed automatically by MinIO", AMAZON_S3_COMPATIBLE);

        Files.createDirectories(m_testInitializer.makePath("folder", "with"));

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
        ignoreWithReason("empty directories are removed automatically by MinIO", AMAZON_S3_COMPATIBLE);

        Files.createDirectories(m_testInitializer.makePath("path", "to"));
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

    @Test
    public void test_deleting_file_from_directory_does_not_delete_directory() throws Exception {
        final Path dir = m_testInitializer.makePath("dir");
        Files.createDirectories(dir);
        assertTrue(Files.isDirectory(dir));

        final Path file = m_testInitializer.createFile("dir", "file");
        assertTrue(Files.isDirectory(dir));
        assertTrue(Files.isRegularFile(file));

        Files.delete(file);
        assertFalse(Files.exists(file));
        assertTrue(Files.isDirectory(dir));
    }
}
