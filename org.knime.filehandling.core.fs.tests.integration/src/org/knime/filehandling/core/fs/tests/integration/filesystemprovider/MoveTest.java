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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

import org.junit.Assume;
import org.junit.Test;
import org.knime.filehandling.core.fs.tests.integration.AbstractParameterizedFSTest;
import org.knime.filehandling.core.testing.FSTestInitializer;
import org.knime.filehandling.core.util.IOESupplier;

/**
 * Test class for move operations on file systems.
 *
 * @author Tobias Urhaug, KNIME GmbH, Berlin, Germany
 *
 */
public class MoveTest extends AbstractParameterizedFSTest {

    private static final String TEST_CONTENT = "Some simple test content";

    public MoveTest(final String fsType, final IOESupplier<FSTestInitializer> testInitializer) throws IOException {
        super(fsType, testInitializer);
    }

    private void moveFileAndCheck(final Path srcFile, final Path targetFile) throws IOException {
        assertTrue(Files.isRegularFile(srcFile));
        assertTrue(Files.exists(srcFile));
        assertTrue(Files.notExists(targetFile));

        Files.move(srcFile, targetFile);

        assertFalse(Files.exists(srcFile));
        assertFalse(Files.isRegularFile(srcFile));
        assertTrue(Files.exists(targetFile));
        assertTrue(Files.isRegularFile(targetFile));
        assertArrayEquals(TEST_CONTENT.getBytes(StandardCharsets.UTF_8), Files.readAllBytes(targetFile));
    }


    @Test
    public void test_move_file() throws Exception {
        Assume.assumeTrue(m_connection.getFSDescriptor().getCapabilities().canWriteFiles());
        Assume.assumeTrue(m_connection.getFSDescriptor().getCapabilities().canDeleteFiles());

        moveFileAndCheck(m_testInitializer.createFileWithContent(TEST_CONTENT, "dir", "file"), //
            m_testInitializer.makePath("dir", "movedFile"));
    }

    @Test
    public void test_move_file_with_spaces() throws Exception {
        Assume.assumeTrue(m_connection.getFSDescriptor().getCapabilities().canWriteFiles());
        Assume.assumeTrue(m_connection.getFSDescriptor().getCapabilities().canDeleteFiles());

        moveFileAndCheck(m_testInitializer.createFileWithContent(TEST_CONTENT, "dir with spaces", "file with spaces"), //
            m_testInitializer.makePath("dir with spaces", "moved file with spaces"));
    }

    @Test
    public void test_move_file_with_pluses() throws Exception {
        Assume.assumeTrue(m_connection.getFSDescriptor().getCapabilities().canWriteFiles());
        Assume.assumeTrue(m_connection.getFSDescriptor().getCapabilities().canDeleteFiles());

        moveFileAndCheck(m_testInitializer.createFileWithContent(TEST_CONTENT, "dir+with+pluses", "file+with+pluses"), //
            m_testInitializer.makePath("dir+with+pluses", "moved+file+with+pluses"));
    }

    @Test
    public void test_move_file_with_percent_encoding() throws Exception {
        Assume.assumeTrue(m_connection.getFSDescriptor().getCapabilities().canWriteFiles());
        Assume.assumeTrue(m_connection.getFSDescriptor().getCapabilities().canDeleteFiles());

        moveFileAndCheck(
            m_testInitializer.createFileWithContent(TEST_CONTENT, "dir%20with%20percent%2520encodings", "file%20with%20percent%2520encodings"), //
            m_testInitializer.makePath("dir%20with%20percent%2520encodings",
                "movedfile%20with%20percent%2520encodings"));
    }

    @Test(expected = NoSuchFileException.class)
    public void test_move_non_existing_file() throws Exception {
        Assume.assumeTrue(m_connection.getFSDescriptor().getCapabilities().canWriteFiles());
        Assume.assumeTrue(m_connection.getFSDescriptor().getCapabilities().canDeleteFiles());

        final Path source = m_testInitializer.getTestCaseScratchDir().resolve("non-existing-file");
        final Path target = source.getParent().resolve("movedFile");

        Files.move(source, target);
    }

    @Test(expected = FileAlreadyExistsException.class)
    public void test_move_file_to_already_existing_file_without_replace_throws_exception() throws Exception {
        Assume.assumeTrue(m_connection.getFSDescriptor().getCapabilities().canWriteFiles());
        Assume.assumeTrue(m_connection.getFSDescriptor().getCapabilities().canDeleteFiles());

        String sourceContent = "The source content";
        Path source = m_testInitializer.createFileWithContent(sourceContent, "dir", "file");
        String targetContent = "The target content";
        Path existingTarget = m_testInitializer.createFileWithContent(targetContent, "dir", "target");

        Files.move(source, existingTarget);
    }

    @Test
    public void test_move_file_to_already_existing_file_with_replace() throws Exception {
        Assume.assumeTrue(m_connection.getFSDescriptor().getCapabilities().canWriteFiles());
        Assume.assumeTrue(m_connection.getFSDescriptor().getCapabilities().canDeleteFiles());

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

    @Test
    public void test_move_file_to_already_existing_file_may_update_attribute_times() throws Exception {
        Assume.assumeTrue(m_connection.getFSDescriptor().getCapabilities().canWriteFiles());
        Assume.assumeTrue(m_connection.getFSDescriptor().getCapabilities().canDeleteFiles());

        final Path source = m_testInitializer.createFileWithContent("a", "src");
        final Path existingTarget = m_testInitializer.createFileWithContent("b", "target");

        final BasicFileAttributes sourceAttrs = Files.readAttributes(source, BasicFileAttributes.class);
        final BasicFileAttributes beforeAttrs = Files.readAttributes(existingTarget, BasicFileAttributes.class);
        Thread.sleep(1000);
        Files.move(source, existingTarget, StandardCopyOption.REPLACE_EXISTING);
        final BasicFileAttributes afterAttrs = Files.readAttributes(existingTarget, BasicFileAttributes.class);

        // some file systems (e.g. local) also copy ctime/mtime, hence we need the OR clause
        assertTrue(sourceAttrs.creationTime().equals(afterAttrs.creationTime())
            || beforeAttrs.creationTime().toMillis() <= afterAttrs.creationTime().toMillis());

        // some file systems (e.g. local) also copy ctime/mtime, hence we need the OR clause
        assertTrue(sourceAttrs.lastModifiedTime().equals(afterAttrs.lastModifiedTime())
            || beforeAttrs.lastModifiedTime().toMillis() <= afterAttrs.lastModifiedTime().toMillis());
    }


    @Test(expected = NoSuchFileException.class)
    public void test_move_file_to_non_existing_directory_throws_exception() throws Exception {
        Assume.assumeTrue(m_connection.getFSDescriptor().getCapabilities().canWriteFiles());
        Assume.assumeTrue(m_connection.getFSDescriptor().getCapabilities().canDeleteFiles());

        String sourceContent = "The source content";
        Path source = m_testInitializer.createFileWithContent(sourceContent, "dir", "fileA");
        Path target = m_testInitializer.getTestCaseScratchDir().resolve("dirB").resolve("fileB");

        Files.move(source, target);
    }

    @Test
    public void test_move_file_to_itself() throws Exception {
        Assume.assumeTrue(m_connection.getFSDescriptor().getCapabilities().canWriteFiles());

        final String testContent = "Some simple test content";
        final Path source = m_testInitializer.createFileWithContent(testContent, "dirA", "fileA");

        Files.move(source, source, StandardCopyOption.REPLACE_EXISTING);

        assertTrue(Files.exists(source));
        final List<String> copiedContent = Files.readAllLines(source);
        assertEquals(1, copiedContent.size());
        assertEquals(testContent, copiedContent.get(0));
    }

    @Test(expected = FileAlreadyExistsException.class)
    public void test_move_directory_to_other_directory() throws Exception {
        Assume.assumeTrue(m_connection.getFSDescriptor().getCapabilities().canWriteFiles());
        Assume.assumeTrue(m_connection.getFSDescriptor().getCapabilities().canDeleteFiles());
        Assume.assumeTrue(m_connection.getFSDescriptor().getCapabilities().canCreateDirectories());
        Assume.assumeTrue(m_connection.getFSDescriptor().getCapabilities().canDeleteDirectories());

        final String testContent = "Some simple test content";
        final Path dirA = m_testInitializer.createFileWithContent(testContent, "dirA", "fileA").getParent();
        final Path dirB = m_testInitializer.createFileWithContent(testContent, "dirB", "fileB").getParent();

        Files.copy(dirA, dirB);

        assertTrue(Files.exists(dirB.resolve("dirA").resolve("fileA")));
    }

    @Test(expected = DirectoryNotEmptyException.class)
    public void test_move_directory_with_replace_to_non_empty_existing_directory() throws Exception {
        Assume.assumeTrue(m_connection.getFSDescriptor().getCapabilities().canWriteFiles());
        Assume.assumeTrue(m_connection.getFSDescriptor().getCapabilities().canDeleteFiles());
        Assume.assumeTrue(m_connection.getFSDescriptor().getCapabilities().canCreateDirectories());
        Assume.assumeTrue(m_connection.getFSDescriptor().getCapabilities().canDeleteDirectories());

        final String testContent = "Some simple test content";
        final Path dirA = m_testInitializer.createFileWithContent(testContent, "dirA", "fileA").getParent();
        final Path dirB = m_testInitializer.createFileWithContent(testContent, "dirB", "fileB").getParent();

        Files.move(dirA, dirB, StandardCopyOption.REPLACE_EXISTING);
    }

    @Test
    public void test_move_directory() throws IOException {
        Assume.assumeTrue(m_connection.getFSDescriptor().getCapabilities().canWriteFiles());
        Assume.assumeTrue(m_connection.getFSDescriptor().getCapabilities().canDeleteFiles());
        Assume.assumeTrue(m_connection.getFSDescriptor().getCapabilities().canCreateDirectories());
        Assume.assumeTrue(m_connection.getFSDescriptor().getCapabilities().canDeleteDirectories());

        final Path fileA3 = m_testInitializer.createFile("dir-A1", "dir-A2", "file-A3");
        m_testInitializer.createFile("dir-A1", "dir-A2", "afile-A4");
        m_testInitializer.createFile("dir-A1", "dir-A2", "dir-A5", "file-A6");
        m_testInitializer.createFile("dir-A1", "dir-A2", "zfile-A7");
        final Path dirA2 = fileA3.getParent();
        final Path dirA1 = dirA2.getParent();
        final Path fileB2 = m_testInitializer.createFile("dir-B1", "file-B2");
        final Path dirB1 = fileB2.getParent();

        // load file attributes and ensure file exists
        assertTrue(Files.isRegularFile(fileA3));
        assertTrue(Files.exists(fileA3));

        // list dir-A1
        final List<Path> beforeA1 = listDir(dirA1);
        assertTrue(beforeA1.contains(dirA2));
        assertEquals(1, beforeA1.size());

        // list dir-B1
        final List<Path> beforeB1 = listDir(dirB1);
        assertTrue(beforeB1.contains(fileB2));
        assertEquals(1, beforeB1.size());

        // move dir-A1/dir-A2 to dir-B1/dir-B3
        final Path dirB3 = m_testInitializer.makePath("dir-B1", "dir-B3");
        Files.move(dirA2, dirB3);

        // check file attributes of old file
        assertFalse(Files.isRegularFile(fileA3));
        assertFalse(Files.exists(fileA3));

        // list dir-A1 and ensure that it is now empty
        final List<Path> afterA1 = listDir(dirA1);
        assertEquals(0, afterA1.size());

        // list dir-B1 and ensure that it now contains dir-B3
        final List<Path> afterB1 = listDir(dirB1);
        assertTrue(afterB1.contains(fileB2));
        assertTrue(afterB1.contains(dirB3));
        assertEquals(2, afterB1.size());

        // list dir-B3 and ensure that it still contains all files/directories
        final List<Path> afterB3 = listDir(dirB3);
        assertTrue(afterB3.contains(dirB3.resolve("file-A3")));
        assertTrue(afterB3.contains(dirB3.resolve("afile-A4")));
        assertTrue(afterB3.contains(dirB3.resolve("dir-A5")));
        assertTrue(afterB3.contains(dirB3.resolve("zfile-A7")));
        assertEquals(4, afterB3.size());

        // list dir-B3 and ensure that it still contains all files/directories
        final Path dirA5 = dirB3.resolve("dir-A5");
        final List<Path> afterA5 = listDir(dirA5);
        assertTrue(afterA5.contains(dirA5.resolve("file-A6")));
        assertEquals(1, afterA5.size());
    }

    private static List<Path> listDir(final Path path) throws IOException {
        final List<Path> list = new ArrayList<>();
        try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(path, p -> true)) {
            directoryStream.forEach(list::add);
        }
        return list;
    }

    @Test
    public void test_rename_empty_dir() throws IOException {
        Assume.assumeTrue(m_connection.getFSDescriptor().getCapabilities().canCreateDirectories());
        Assume.assumeTrue(m_connection.getFSDescriptor().getCapabilities().canDeleteDirectories());

        Path root = m_testInitializer.getTestCaseScratchDir();

        Path srcDir = root.resolve("srcDir");
        Path dstDir = root.resolve("dstDir");

        Files.createDirectory(srcDir);

        assertTrue(Files.isDirectory(srcDir));
        assertFalse(Files.exists(dstDir));

        Files.move(srcDir, dstDir);

        assertTrue(Files.isDirectory(dstDir));
        assertFalse(Files.exists(srcDir));
    }

    @Test
    public void test_rename_non_empty_dir() throws IOException {
        Assume.assumeTrue(m_connection.getFSDescriptor().getCapabilities().canWriteFiles());
        Assume.assumeTrue(m_connection.getFSDescriptor().getCapabilities().canDeleteFiles());
        Assume.assumeTrue(m_connection.getFSDescriptor().getCapabilities().canCreateDirectories());
        Assume.assumeTrue(m_connection.getFSDescriptor().getCapabilities().canDeleteDirectories());

        Path root = m_testInitializer.getTestCaseScratchDir();

        Path srcDir = Files.createDirectory(root.resolve("srcDir"));
        Path srcFile = m_testInitializer.createFileWithContent("content", "srcDir", "fileA");

        Path dstDir = root.resolve("dstDir");
        Path dstFile = dstDir.resolve("fileA");

        assertTrue(Files.isRegularFile(srcFile));
        assertTrue(Files.isDirectory(srcDir));
        assertFalse(Files.exists(dstDir));
        assertFalse(Files.exists(dstFile));

        Files.move(srcDir, dstDir);

        assertTrue(Files.isDirectory(dstDir));
        assertTrue(Files.isRegularFile(dstFile));
        assertFalse(Files.exists(srcFile));
        assertFalse(Files.exists(srcDir));
    }

    @Test
    public void test_move_empty_dir_into_another() throws IOException {
        Assume.assumeTrue(m_connection.getFSDescriptor().getCapabilities().canCreateDirectories());
        Assume.assumeTrue(m_connection.getFSDescriptor().getCapabilities().canDeleteDirectories());

        String dirName = "dir";
        Path srcParent = Files.createDirectory(m_testInitializer.getTestCaseScratchDir().resolve("srcParent"));
        Path dstParent = Files.createDirectory(m_testInitializer.getTestCaseScratchDir().resolve("dstParent"));

        Path dirToMove = Files.createDirectory(srcParent.resolve(dirName));

        assertTrue(Files.isDirectory(dirToMove));
        assertFalse(Files.exists(dstParent.resolve(dirName)));

        Files.move(dirToMove, dstParent.resolve(dirName));

        assertTrue(Files.isDirectory(dstParent.resolve(dirName)));
        assertFalse(Files.exists(dirToMove));
    }

    @Test
    public void test_move_non_empty_dir_into_another() throws IOException {
        Assume.assumeTrue(m_connection.getFSDescriptor().getCapabilities().canWriteFiles());
        Assume.assumeTrue(m_connection.getFSDescriptor().getCapabilities().canDeleteFiles());
        Assume.assumeTrue(m_connection.getFSDescriptor().getCapabilities().canCreateDirectories());
        Assume.assumeTrue(m_connection.getFSDescriptor().getCapabilities().canDeleteDirectories());

        String dirName = "dir";
        String fileName = "file";

        Path srcParent = Files.createDirectory(m_testInitializer.getTestCaseScratchDir().resolve("srcParent"));
        Path dstParent = Files.createDirectory(m_testInitializer.getTestCaseScratchDir().resolve("dstParent"));

        Path dirToMove = Files.createDirectory(srcParent.resolve(dirName));
        Path fileToMove = m_testInitializer.createFileWithContent("content", "srcParent", dirName, fileName);

        assertTrue(Files.isDirectory(dirToMove));
        assertTrue(Files.isRegularFile(fileToMove));
        assertFalse(Files.exists(dstParent.resolve(dirName)));
        assertFalse(Files.exists(dstParent.resolve(dirName).resolve(fileName)));

        Files.move(dirToMove, dstParent.resolve(dirName));

        assertTrue(Files.isDirectory(dstParent.resolve(dirName)));
        assertTrue(Files.isRegularFile(dstParent.resolve(dirName).resolve(fileName)));
        assertFalse(Files.exists(dirToMove));
        assertFalse(Files.exists(fileToMove));
    }

    @Test(expected = FileAlreadyExistsException.class)
    public void test_move_dir_to_already_existing_file_throws_exception() throws IOException {
        Assume.assumeTrue(m_connection.getFSDescriptor().getCapabilities().canWriteFiles());
        Assume.assumeTrue(m_connection.getFSDescriptor().getCapabilities().canDeleteFiles());
        Assume.assumeTrue(m_connection.getFSDescriptor().getCapabilities().canCreateDirectories());
        Assume.assumeTrue(m_connection.getFSDescriptor().getCapabilities().canDeleteDirectories());

        Path file = m_testInitializer.createFileWithContent("content", "file");
        Path dir = Files.createDirectories(m_testInitializer.getTestCaseScratchDir().resolve("dir"));

        Files.move(dir, file);
    }

    @Test(expected = NoSuchFileException.class)
    public void test_move_dir_into_not_existing_throws_exception() throws IOException {
        Assume.assumeTrue(m_connection.getFSDescriptor().getCapabilities().canWriteFiles());
        Assume.assumeTrue(m_connection.getFSDescriptor().getCapabilities().canDeleteFiles());
        Assume.assumeTrue(m_connection.getFSDescriptor().getCapabilities().canCreateDirectories());
        Assume.assumeTrue(m_connection.getFSDescriptor().getCapabilities().canDeleteDirectories());

        String dirName = "dir";
        Path srcParent = Files.createDirectory(m_testInitializer.getTestCaseScratchDir().resolve("srcParent"));
        Path dstParent = m_testInitializer.getTestCaseScratchDir().resolve("dstParent");

        Path dirToMove = Files.createDirectory(srcParent.resolve(dirName));

        assertTrue(Files.isDirectory(dirToMove));
        assertFalse(Files.exists(dstParent));

        Files.move(dirToMove, dstParent.resolve(dirName));
    }
}