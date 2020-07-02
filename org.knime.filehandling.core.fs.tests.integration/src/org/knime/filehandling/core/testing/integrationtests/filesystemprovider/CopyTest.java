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
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.knime.filehandling.core.testing.FSTestInitializer;
import org.knime.filehandling.core.testing.integrationtests.AbstractParameterizedFSTest;
import org.knime.filehandling.core.util.IOESupplier;

/**
 * Test class for copy operations on file systems.
 *
 * @author Tobias Urhaug, KNIME GmbH, Berlin, Germany
 *
 */
public class CopyTest extends AbstractParameterizedFSTest {

    public CopyTest(final String fsType, final IOESupplier<FSTestInitializer> testInitializer) throws IOException {
        super(fsType, testInitializer);
    }

    @Test
    public void test_copy_file() throws Exception {
        final String testContent = "Some simple test content";
        final Path fileA1 = m_testInitializer.createFileWithContent(testContent, "file-A1");
        final Path fileB2 = m_testInitializer.createFileWithContent("test", "dir-B", "file-B2");
        final Path dirB = fileB2.getParent();

        // list dir-B
        final List<Path> before = new ArrayList<>();
        try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(dirB, path -> true)) {
            directoryStream.forEach(before::add);
        }
        assertTrue(before.contains(fileB2));
        assertEquals(1, before.size());

        // ensure file does not exist
        final Path fileB3 = m_testInitializer.makePath("dir-B", "file-B3");
        assertFalse(Files.isRegularFile(fileB3));
        assertFalse(Files.exists(fileB3));

        // copy file-A1 to dir-B/file-B3
        Files.copy(fileA1, fileB3);

        // ensure file exists now and contains the original data
        assertTrue(Files.isRegularFile(fileB3));
        assertTrue(Files.exists(fileB3));
        final List<String> copiedContent = Files.readAllLines(fileB3);
        assertEquals(1, copiedContent.size());
        assertEquals(testContent, copiedContent.get(0));

        // list dir-B again ensure that it now contains the new file-B3
        final List<Path> after = new ArrayList<>();
        try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(dirB, path -> true)) {
            directoryStream.forEach(after::add);
        }
        assertTrue(after.contains(fileB2));
        assertTrue(after.contains(fileB3));
        assertEquals(2, after.size());
    }

    @Test(expected = NoSuchFileException.class)
    public void test_copy_non_existing_file() throws Exception {
        final Path source = m_testInitializer.getTestCaseScratchDir().resolve("non-existing-file");
        final Path target = source.getParent().resolve("copiedFile");

        Files.copy(source, target);
    }

    @Test(expected = FileAlreadyExistsException.class)
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

    @Test(expected = NoSuchFileException.class)
    public void test_copy_file_to_non_existing_directory() throws Exception {
        final String testContent = "Some simple test content";
        final Path source = m_testInitializer.createFileWithContent(testContent, "dir", "file");
        final Path target = source.getParent().resolve("newDir").resolve("copiedFile");

        Files.copy(source, target);
    }

    @Test(expected = FileAlreadyExistsException.class)
    public void test_copy_directory_to_other_directory() throws Exception {
        final String testContent = "Some simple test content";
        final Path dirA = m_testInitializer.createFileWithContent(testContent, "dirA", "fileA").getParent();
        final Path dirB = m_testInitializer.createFileWithContent(testContent, "dirB", "fileB").getParent();

        Files.copy(dirA, dirB);
    }

    @Test(expected = DirectoryNotEmptyException.class)
    public void test_copy_directory_with_replace_to_non_empty_existing_directory() throws Exception {
        final String testContent = "Some simple test content";
        final Path dirA = m_testInitializer.createFileWithContent(testContent, "dirA", "fileA").getParent();
        final Path dirB = m_testInitializer.createFileWithContent(testContent, "dirB", "fileB").getParent();

        Files.copy(dirA, dirB, StandardCopyOption.REPLACE_EXISTING);
    }
}
