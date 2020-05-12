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
package org.knime.filehandling.core.testing.integrationtests.filesystemprovider;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
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

    @Test(expected = NoSuchFileException.class)
    public void test_move_non_existing_file() throws Exception {
        final Path source = m_testInitializer.getRoot().resolve("non-existing-file");
        final Path target = source.getParent().resolve("movedFile");

        Files.move(source, target);
    }

    @Test(expected = FileAlreadyExistsException.class)
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

    @Test(expected = NoSuchFileException.class)
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

    @Test
    public void test_rename_empty_dir() throws IOException {
        Path root = m_testInitializer.getRoot();

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
        Path root = m_testInitializer.getRoot();

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
        String dirName = "dir";
        Path srcParent = Files.createDirectory(m_testInitializer.getRoot().resolve("srcParent"));
        Path dstParent = Files.createDirectory(m_testInitializer.getRoot().resolve("dstParent"));

        Path dirToMove = Files.createDirectory(srcParent.resolve(dirName));

        assertTrue(Files.isDirectory(dirToMove));
        assertFalse(Files.exists(dstParent.resolve(dirName)));

        Files.move(dirToMove, dstParent.resolve(dirName));

        assertTrue(Files.isDirectory(dstParent.resolve(dirName)));
        assertFalse(Files.exists(dirToMove));
    }

    @Test
    public void test_move_non_empty_dir_into_another() throws IOException {
        String dirName = "dir";
        String fileName = "file";

        Path srcParent = Files.createDirectory(m_testInitializer.getRoot().resolve("srcParent"));
        Path dstParent = Files.createDirectory(m_testInitializer.getRoot().resolve("dstParent"));

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
        Path file = m_testInitializer.createFileWithContent("content", "file");
        Path dir = Files.createDirectories(m_testInitializer.getRoot().resolve("dir"));

        Files.move(dir, file);
    }

    @Test(expected = NoSuchFileException.class)
    public void test_move_dir_into_not_existing_throws_exception() throws IOException {
        String dirName = "dir";
        Path srcParent = Files.createDirectory(m_testInitializer.getRoot().resolve("srcParent"));
        Path dstParent = m_testInitializer.getRoot().resolve("dstParent");

        Path dirToMove = Files.createDirectory(srcParent.resolve(dirName));

        assertTrue(Files.isDirectory(dirToMove));
        assertFalse(Files.exists(dstParent));

        Files.move(dirToMove, dstParent.resolve(dirName));
    }
}