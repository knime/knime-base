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
 *
 * History
 *   Jun 22, 2020 (bjoern): created
 */
package org.knime.filehandling.core.fs.tests.integration.files;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.Assume;
import org.junit.Test;
import org.knime.filehandling.core.connections.FSCategory;
import org.knime.filehandling.core.connections.FSFiles;
import org.knime.filehandling.core.connections.FSLocation;
import org.knime.filehandling.core.connections.FSPath;
import org.knime.filehandling.core.connections.location.FSPathProvider;
import org.knime.filehandling.core.connections.location.FSPathProviderFactory;
import org.knime.filehandling.core.fs.tests.integration.AbstractParameterizedFSTest;
import org.knime.filehandling.core.testing.FSTestInitializer;
import org.knime.filehandling.core.util.IOESupplier;

/**
 * @author Bjoern Lohrmann, KNIME GmbH
 */
public class FSFilesTest extends AbstractParameterizedFSTest {

    public FSFilesTest(final String fsType, final IOESupplier<FSTestInitializer> testInitializer)
        throws IOException {
        super(fsType, testInitializer);
    }

    @Test
    public void test_delete_recursively() throws IOException {
        Assume.assumeTrue(m_connection.getFSDescriptor().getCapabilities().canCreateDirectories());
        Assume.assumeTrue(m_connection.getFSDescriptor().getCapabilities().canDeleteDirectories());
        Assume.assumeTrue(m_connection.getFSDescriptor().getCapabilities().canDeleteFiles());

        final Path dir = m_testInitializer.makePath("dir");
        Files.createDirectory(m_testInitializer.makePath("dir"));
        Files.createDirectory(m_testInitializer.makePath("dir", "childdir"));
        m_testInitializer.createFile("dir", "file");
        m_testInitializer.createFile("dir", "aFile");
        m_testInitializer.createFile("dir", "childdir", "a");
        m_testInitializer.createFile("dir", "childdir", "b");
        m_testInitializer.createFile("dir", "childdir", "c");

        FSFiles.deleteRecursively(dir);

        assertFalse(Files.exists(dir));
        assertFalse(Files.exists(m_testInitializer.makePath("dir", "childdir")));
        assertFalse(Files.exists(m_testInitializer.makePath("dir", "file")));
        assertFalse(Files.exists(m_testInitializer.makePath("dir", "aFile")));
        assertFalse(Files.exists(m_testInitializer.makePath("dir", "childdir", "a")));
        assertFalse(Files.exists(m_testInitializer.makePath("dir", "childdir", "b")));
        assertFalse(Files.exists(m_testInitializer.makePath("dir", "childdir", "c")));
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_copy_recursively_file() throws IOException {
        Assume.assumeTrue(m_connection.getFSDescriptor().getCapabilities().canWriteFiles());

        final FSPath source = m_testInitializer.createFile("file");
        final FSPath target = m_testInitializer.makePath("file2");
        FSFiles.copyRecursively(source, target);
    }

    @Test
    public void test_copy_recursively_empty_dir() throws IOException {
        Assume.assumeTrue(m_connection.getFSDescriptor().getCapabilities().canCreateDirectories());
        Assume.assumeTrue(m_connection.getFSDescriptor().getCapabilities().canWriteFiles());

        final FSPath source = m_testInitializer.makePath("sourcedir");
        final FSPath target = m_testInitializer.makePath("targetdir");

        Files.createDirectories(source);
        FSFiles.copyRecursively(source, target);

        assertTrue(Files.isDirectory(source));
        assertTrue(Files.isDirectory(target));

        try(Stream<Path> tgtFiles = Files.list(target)) {
            assertEquals(0, tgtFiles.toArray(Path[]::new).length);
        }
    }

    @Test
    public void test_copy_recursively_file_tree() throws IOException {
        Assume.assumeTrue(m_connection.getFSDescriptor().getCapabilities().canCreateDirectories());
        Assume.assumeTrue(m_connection.getFSDescriptor().getCapabilities().canWriteFiles());

        final FSPath source = m_testInitializer.makePath("dir1");
        final FSPath a = m_testInitializer.createFileWithContent("a", "dir1", "a");
        final FSPath b = m_testInitializer.createFileWithContent("b", "dir1", "b");
        final FSPath c = m_testInitializer.createFileWithContent("c", "dir1", "dir11", "a");
        final FSPath d = m_testInitializer.createFileWithContent("d", "dir1", "dir12", "d");
        final FSPath emptyDir = m_testInitializer.makePath("dir1", "empty_dir");
        Files.createDirectories(emptyDir);

        final FSPath target = m_testInitializer.makePath("target");

        FSFiles.copyRecursively(source, target);

        assertTrue(Files.isRegularFile(a));
        assertTrue(Files.isRegularFile(b));
        assertTrue(Files.isRegularFile(c));
        assertTrue(Files.isRegularFile(d));
        assertTrue(Files.isDirectory(emptyDir));


        final FSPath targetA = m_testInitializer.makePath("target", "a");
        final FSPath targetB = m_testInitializer.makePath("target", "b");
        final FSPath targetC = m_testInitializer.makePath("target", "dir11", "a");
        final FSPath targetD = m_testInitializer.makePath("target", "dir12", "d");
        final FSPath targetEmptyDir = m_testInitializer.makePath("target", "empty_dir");

        assertTrue(Files.isRegularFile(targetA));
        assertTrue(Files.isRegularFile(targetB));
        assertTrue(Files.isRegularFile(targetC));
        assertTrue(Files.isRegularFile(targetD));
        assertTrue(Files.isDirectory(targetEmptyDir));

        assertEquals(Files.readAllLines(a), Files.readAllLines(targetA));
        assertEquals(Files.readAllLines(b), Files.readAllLines(targetB));
        assertEquals(Files.readAllLines(c), Files.readAllLines(targetC));
        assertEquals(Files.readAllLines(d), Files.readAllLines(targetD));
        try(Stream<Path> tgtFiles = Files.list(targetEmptyDir)) {
            assertEquals(0, tgtFiles.toArray(Path[]::new).length);
        }
    }

    @Test
    public void test_copy_recursively_merge() throws IOException {
        Assume.assumeTrue(m_connection.getFSDescriptor().getCapabilities().canCreateDirectories());
        Assume.assumeTrue(m_connection.getFSDescriptor().getCapabilities().canWriteFiles());

        final FSPath source = m_testInitializer.makePath("dir1");
        final FSPath a = m_testInitializer.createFileWithContent("a", "dir1", "a");
        final FSPath b = m_testInitializer.createFileWithContent("c", "dir1", "dir11", "b");
        final FSPath c = m_testInitializer.createFileWithContent("d", "dir1", "dir12", "c");
        final FSPath emptyDir = m_testInitializer.makePath("dir1", "empty_dir");
        Files.createDirectories(emptyDir);

        final FSPath target = m_testInitializer.makePath("target");
        final FSPath targetA = m_testInitializer.createFileWithContent("targetA", "target", "a");
        final FSPath targetX = m_testInitializer.createFileWithContent("targetX", "target", "x");
        final FSPath targetB = m_testInitializer.createFileWithContent("targetB", "target", "dir11", "b");
        final FSPath targetC = m_testInitializer.makePath("target", "dir12", "c");
        final FSPath targetEmptyDir = m_testInitializer.makePath("target", "empty_dir");

        assertTrue(Files.isRegularFile(a));
        assertTrue(Files.isRegularFile(b));
        assertTrue(Files.isRegularFile(c));
        assertTrue(Files.isDirectory(emptyDir));

        FSFiles.copyRecursively(source, target, StandardCopyOption.REPLACE_EXISTING);

        assertEquals(Files.readAllLines(a), Files.readAllLines(targetA));
        assertEquals(Collections.singletonList("targetX"), Files.readAllLines(targetX));
        assertEquals(Files.readAllLines(b), Files.readAllLines(targetB));
        assertEquals(Files.readAllLines(c), Files.readAllLines(targetC));
        try(Stream<Path> tgtFiles = Files.list(targetEmptyDir)) {
            assertEquals(0, tgtFiles.toArray(Path[]::new).length);
        }
    }

    @Test
    public void test_copy_recursively_target_already_exists() throws IOException {
        Assume.assumeTrue(m_connection.getFSDescriptor().getCapabilities().canWriteFiles());

        final FSPath source = m_testInitializer.makePath("source");
        final FSPath target = m_testInitializer.makePath("target");

        m_testInitializer.createFileWithContent("s", "source", "a");
        final FSPath targetA = m_testInitializer.createFileWithContent("t", "target", "a");

        try {
            FSFiles.copyRecursively(source, target);
            fail("FileAlreadyExistsException was not thrown");
        } catch (FileAlreadyExistsException e) { // NOSONAR
        }

        assertEquals(Collections.singletonList("t"), Files.readAllLines(targetA));
    }

    @Test
    public void test_copy_recursively_cross_provider() throws IOException {
        Assume.assumeTrue(m_connection.getFSDescriptor().getCapabilities().canWriteFiles());

        final Path localTmpDir = Files.createTempDirectory("knime-filehanding-tests");
        final Path localTmpFile = localTmpDir.resolve("file");
        try (Writer writer = Files.newBufferedWriter(localTmpFile)) {
            writer.append("content");
        }
        FSLocation sourceLoc = new FSLocation(FSCategory.LOCAL, localTmpDir.toString());

        try (FSPathProviderFactory factory = FSPathProviderFactory.newFactory(Optional.empty(), sourceLoc)) {
            try (FSPathProvider pathProvider = factory.create(sourceLoc)) {
                final FSPath source = pathProvider.getPath();
                final FSPath target = m_testInitializer.makePath("target");

                FSFiles.copyRecursively(source, target);

                assertTrue(Files.isDirectory(target));
                assertEquals(Collections.singletonList("content"), Files.readAllLines(target.resolve("file")));
            }
        }
    }
}
