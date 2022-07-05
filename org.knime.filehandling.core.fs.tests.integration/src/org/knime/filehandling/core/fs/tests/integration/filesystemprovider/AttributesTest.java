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
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.junit.Test;
import org.knime.filehandling.core.fs.tests.integration.AbstractParameterizedFSTest;
import org.knime.filehandling.core.testing.FSTestInitializer;
import org.knime.filehandling.core.util.IOESupplier;

/**
 * Test that a file system returns basic file attributes.
 */
public class AttributesTest extends AbstractParameterizedFSTest {

    public AttributesTest(final String fsType, final IOESupplier<FSTestInitializer> testInitializer)
        throws IOException {
        super(fsType, testInitializer);
    }

    @Test
    public void test_get_basic_file_attributes() throws Exception {
        final Path testFile = m_testInitializer.createFile("some-dir", "some-file");
        final Path testDir = testFile.getParent();

        assertFalse(Files.isRegularFile(testDir));
        assertTrue(Files.isDirectory(testDir));
        assertTrue(Files.isReadable(testDir));
        assertTrue(Files.isWritable(testDir));
        assertFalse(Files.isHidden(testDir));

        assertTrue(Files.isRegularFile(testFile));
        assertFalse(Files.isDirectory(testFile));
        assertTrue(Files.isReadable(testFile));
        assertTrue(Files.isWritable(testFile));
        assertFalse(Files.isHidden(testFile));

        assertTrue(Files.isSameFile(testFile, testFile));
        assertTrue(Files.isSameFile(testDir, testDir));
        assertFalse(Files.isSameFile(testDir, testFile));
        assertFalse(Files.isSameFile(testFile, testDir));
    }

    @Test
    public void test_get_root_file_attributes() throws IOException {
        final Path root = getFileSystem().getRootDirectories().iterator().next();

        assertFalse(Files.isRegularFile(root));
        assertTrue(Files.isDirectory(root));
        assertFalse(Files.isHidden(root));

        assertTimeInvariants(root);
    }

    public void test_time_invariants_on_root_children() throws Exception {
        final Path root = getFileSystem().getRootDirectories().iterator().next();

        try (Stream<Path> stream = Files.list(root)) {
            stream.forEach(p -> {
                try {
                    assertTimeInvariants(p);
                } catch (IOException e) { // NOSONAR
                    throw new UncheckedIOException(e);
                }
            });
        }

    }

    @Test
    public void test_file_time_invariants() throws Exception {
        final Path testFile = m_testInitializer.createFile("file");
        assertTimeInvariants(testFile);
    }

    private static void assertTimeInvariants(final Path path) throws IOException {
        final BasicFileAttributes attrs = Files.readAttributes(path, BasicFileAttributes.class);

        assertNotNull(attrs.lastModifiedTime());
        assertNotNull(attrs.creationTime());
        assertNotNull(attrs.lastAccessTime());

        assertTrue(attrs.creationTime().toMillis() <= attrs.lastModifiedTime().toMillis());
        // some file systems will update the mtime when writing a file to a dir, without
        // updating the access time
        if (!attrs.isDirectory()) {
            assertTrue(attrs.lastModifiedTime().toMillis() <= attrs.lastAccessTime().toMillis());
        }
    }

    @Test
    public void test_empty_folder_time_invariants() throws Exception {
        final Path testFolder = m_testInitializer.makePath("myfolder");
        if (m_connection.getFSDescriptor().getCapabilities().canWriteFiles()) {
            Files.createDirectory(testFolder);
        } // read-only file-systems must have the directory in the test file
        assertTimeInvariants(testFolder);
    }

    @Test
    public void test_non_empty_folder_time_invariants() throws Exception {
        final Path testFolder = m_testInitializer.makePath("folder");
        final Path testFile = testFolder.resolve("file");
        if (m_connection.getFSDescriptor().getCapabilities().canWriteFiles()) {
            Files.createDirectory(testFolder);
            Files.write(testFile, new byte[]{0}, StandardOpenOption.CREATE);
        } // read-only file-systems must have the directory in the test file
        assertTimeInvariants(testFolder);
    }

    @Test
    public void test_get_attributes_of_non_existing_path() throws Exception {
        final Path testFile = m_testInitializer.makePath("non-existing-file");
        assertFalse(Files.isRegularFile(testFile));
        assertFalse(Files.isDirectory(testFile));
        assertFalse(Files.isReadable(testFile));
        assertFalse(Files.isWritable(testFile));
        assertFalse(Files.isHidden(testFile));
    }

    @Test
    public void test_get_file_attribute_view_file_type() throws Exception {
        final Path file = m_testInitializer.createFile("file");

        final BasicFileAttributeView view = Files.getFileAttributeView(file, BasicFileAttributeView.class);
        final BasicFileAttributes attribs = view.readAttributes();

        assertTrue(attribs.isRegularFile());
        assertFalse(attribs.isDirectory());
        assertFalse(attribs.isOther());
    }

    @Test
    public void test_get_file_attribute_view_mtime() throws Exception {
        ignoreWithReason("Server REST API does not provide mtime for data files", KNIME_REST_RELATIVE_MOUNTPOINT);
        ignoreWithReason("Server REST API does not provide mtime for data files", KNIME_REST_RELATIVE_WORKFLOW);
        ignoreWithReason("Server REST API does not provide mtime for data files", KNIME_REST);

        final Path file = m_testInitializer.createFile("file");

        final BasicFileAttributeView view = Files.getFileAttributeView(file, BasicFileAttributeView.class);
        final BasicFileAttributes attribs = view.readAttributes();

        final long fileMtime = attribs.lastModifiedTime().to(TimeUnit.SECONDS);
        final long now = Instant.now().getEpochSecond();
        assertNotEquals(0, fileMtime);
        // assert that the mtime is in the "vicinity" of now
        if (m_connection.getFSDescriptor().getCapabilities().canWriteFiles()) {
            assertTrue(Math.abs(fileMtime - now) < 60);
        }
    }

    @Test(expected = NoSuchFileException.class)
    public void test_get_file_attributes_for_funny_file1() throws Exception {
        final Path file = m_testInitializer.makePath("X:\\AA\\B C\\ X~\\#\\doesnotexist?!");
        Files.readAttributes(file, BasicFileAttributes.class);
    }

    @Test(expected = NoSuchFileException.class)
    public void test_get_file_attributes_for_funny_file2() throws Exception {
        final Path file = getFileSystem().getPath("X:\\AA\\B C\\ X~\\#\\doesnotexist?!");
        Files.readAttributes(file, BasicFileAttributes.class);
    }

    @Test
    public void test_get_file_size() throws IOException {
        String content = "some content";
        Path file = m_testInitializer.createFileWithContent(content, "file");

        assertEquals(content.length(), Files.size(file));
    }
}
