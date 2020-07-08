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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.junit.Test;
import org.knime.filehandling.core.fs.tests.integration.AbstractParameterizedFSTest;
import org.knime.filehandling.core.testing.FSTestInitializer;
import org.knime.filehandling.core.util.IOESupplier;

/**
 * Test class for file system directory streams.
 *
 * @author Tobias Urhaug, KNIME GmbH, Berlin, Germany
 *
 */
public class DirectoryStreamTest extends AbstractParameterizedFSTest {

    public DirectoryStreamTest(final String fsType, final IOESupplier<FSTestInitializer> testInitializer)
        throws IOException {
        super(fsType, testInitializer);
    }

    @Test
    public void test_list_files_in_directory() throws Exception {
        final Path fileA = m_testInitializer.createFileWithContent("contentA", "dir", "fileA");
        final Path fileB = m_testInitializer.createFileWithContent("contentB", "dir", "fileB");
        final Path fileC = m_testInitializer.createFileWithContent("contentC", "dir", "fileC");
        final Path directory = fileA.getParent();

        final List<Path> paths = new ArrayList<>();
        try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(directory, (path) -> true)) {
            directoryStream.forEach(paths::add);
        }

        assertTrue(paths.contains(fileA));
        assertTrue(paths.contains(fileB));
        assertTrue(paths.contains(fileC));
    }

    @Test
    public void test_list_emtpy_directory() throws Exception {
        final Path directory = m_testInitializer.getTestCaseScratchDir();
        // root directory might contains files (e.g. workflows), use a fresh empty
        // directory
        final Path emptyDirectory = Files.createDirectories(directory.resolve("empty-directory"));

        final List<Path> paths = new ArrayList<>();
        try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(emptyDirectory, (path) -> true)) {
            directoryStream.forEach(paths::add);
        }

        assertTrue(paths.isEmpty());
    }

    @Test
    public void test_list_files_with_filter() throws IOException {
        final Path fileA = m_testInitializer.createFileWithContent("contentA", "dir", "fileA");
        final Path fileB = m_testInitializer.createFileWithContent("contentB", "dir", "fileB");
        final Path fileC = m_testInitializer.createFileWithContent("contentC", "dir", "fileC");
        final Path directory = fileA.getParent();

        final Filter<Path> filter = path -> path.getFileName().toString().equals("fileB");
        final List<Path> paths = new ArrayList<>();
        try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(directory, filter)) {
            directoryStream.forEach(paths::add);
        }

        assertEquals(1, paths.size());
        assertTrue(paths.contains(fileB));
    }

    @Test(expected = NoSuchFileException.class)
    public void test_non_existent_directory() throws Exception {
        final Path directory = m_testInitializer.getTestCaseScratchDir().resolve("doesnotexist");

        try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(directory, (path) -> true)) {
            fail("should fail with NoSuchFileException and never reach this code");
        }
    }

    @Test(expected = NotDirectoryException.class)
    public void test_list_file_instead_of_directory() throws IOException {
        final Path file = m_testInitializer.createFileWithContent("test", "some-dir", "some-file");

        try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(file, path -> true)) {
            fail("should fail with before");
        }
    }

    @Test
    public void test_list_relative_directory() throws Exception {
        final Path relDir = getFileSystem().getWorkingDirectory().relativize(m_testInitializer.makePath("dir"));
        Files.createDirectory(relDir);

        final Path relFile =
            getFileSystem().getWorkingDirectory().relativize(m_testInitializer.createFile("dir", "file"));

        final Path[] dirList;
        try (Stream<Path> dirStream = Files.list(relDir)) {
            dirList = dirStream.toArray(Path[]::new);
        }
        assertEquals(1, dirList.length);
        assertEquals(relFile, dirList[0]);
    }

    @Test
    public void test_list_dot_directory() throws Exception {
        ignoreWithReason("The working directory in knime-local-relative-workflow is actually a file",
            KNIME_LOCAL_RELATIVE_WORKFLOW);
        ignoreWithReason("The working directory in knime-local-relative-mountpoint contains the workflow, hence the assertEquals will fail",
            KNIME_LOCAL_RELATIVE_MOUNTPOINT);

        // force creation of scratch dir
        m_testInitializer.createFile("file");

        final Path[] dirList;
        try (final Stream<Path> stream = Files.list(getFileSystem().getPath("."))) {
            dirList = stream.toArray(Path[]::new);
        }
        assertEquals(1, dirList.length);

        final Path expectedPath =
            getFileSystem().getPath(".").resolve(m_testInitializer.getTestCaseScratchDir().getFileName());
        assertEquals(expectedPath, dirList[0]);
    }

    @Test
    public void test_list_dot_directory_relative_mountpoint() throws Exception {
        ignoreAllExcept(KNIME_LOCAL_RELATIVE_MOUNTPOINT);

        // force creation of scratch dir inside working dir
        m_testInitializer.createFile("file");

        final Path[] dirList;
        try (final Stream<Path> stream = Files.list(getFileSystem().getPath("."))) {
            dirList = stream.toArray(Path[]::new);
        }
        assertEquals(2, dirList.length);

        final Path expectedPath1 =
            getFileSystem().getPath(".").resolve(m_testInitializer.getTestCaseScratchDir().getFileName());
        assertEquals(expectedPath1, dirList[0]);

        final Path expectedPath2 =
                getFileSystem().getPath(".").resolve("current-workflow");
            assertEquals(expectedPath2, dirList[1]);
    }

}
