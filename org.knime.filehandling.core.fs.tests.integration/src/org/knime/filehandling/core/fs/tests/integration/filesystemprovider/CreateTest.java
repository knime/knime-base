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
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.stream.Stream;

import org.junit.Assume;
import org.junit.Test;
import org.knime.filehandling.core.fs.tests.integration.AbstractParameterizedFSTest;
import org.knime.filehandling.core.testing.FSTestInitializer;
import org.knime.filehandling.core.util.IOESupplier;

/**
 * Test class for create operations on file systems.
 *
 * @author Tobias Urhaug, KNIME GmbH, Berlin, Germany
 */
public class CreateTest extends AbstractParameterizedFSTest {

    public CreateTest(final String fsType, final IOESupplier<FSTestInitializer> testInitializer) throws IOException {
        super(fsType, testInitializer);
    }

    @Test
    public void test_create_file() throws IOException {
        Assume.assumeTrue(m_connection.getFSDescriptor().getCapabilities().canWriteFiles());

        Path path = m_testInitializer.getTestCaseScratchDir().resolve("file");

        assertFalse(Files.exists(path));
        Files.createFile(path);
        assertTrue(Files.exists(path));
    }

    @Test
    public void test_create_file_on_path() throws IOException {
        Assume.assumeTrue(m_connection.getFSDescriptor().getCapabilities().canWriteFiles());

        Path path = m_testInitializer.getTestCaseScratchDir().resolve("path").resolve("to").resolve("file");

        Files.createDirectories(path.getParent());
        Files.createFile(path);
        assertTrue(Files.exists(path));
    }

    @Test(expected = FileAlreadyExistsException.class)
    public void test_create_file_which_already_exists() throws Exception {
        Assume.assumeTrue(m_connection.getFSDescriptor().getCapabilities().canWriteFiles());

        Path file = m_testInitializer.createFileWithContent("content", "existing", "file");

        Files.createFile(file);
    }

    @Test
    public void test_create_directory() throws Exception {
        Assume.assumeTrue(m_connection.getFSDescriptor().getCapabilities().canWriteFiles());

        Path pathToDriectory = m_testInitializer.getTestCaseScratchDir().resolve("directory");

        Path directory = Files.createDirectory(pathToDriectory);

        assertTrue(Files.exists(directory));
        assertTrue(Files.isDirectory(directory));
    }

    @Test
    public void test_create_directories() throws Exception {
        Assume.assumeTrue(m_connection.getFSDescriptor().getCapabilities().canWriteFiles());

        Path pathToDirectory = m_testInitializer.getTestCaseScratchDir().resolve("path").resolve("to").resolve("directory");

        Path directory = Files.createDirectories(pathToDirectory);

        assertTrue(Files.exists(directory));
        assertTrue(Files.isDirectory(directory));
        assertTrue(Files.exists(directory.getParent()));
        assertTrue(Files.isDirectory(directory.getParent()));
    }

    @Test
    public void test_create_directory_with_space() throws Exception {
        Assume.assumeTrue(m_connection.getFSDescriptor().getCapabilities().canWriteFiles());

        Path pathToDirectory = m_testInitializer.getTestCaseScratchDir().resolve("dir with spaces");

        Files.createDirectories(pathToDirectory);
        assertTrue(Files.readAttributes(pathToDirectory, BasicFileAttributes.class).isDirectory());

        try (Stream<Path> dirStream = Files.list(m_testInitializer.getTestCaseScratchDir())) {
            assertEquals(1, dirStream.filter(p -> p.equals(pathToDirectory)).count());
        }
    }


    @Test
    public void test_create_directory_with_percent_encodings() throws Exception {
        Assume.assumeTrue(m_connection.getFSDescriptor().getCapabilities().canWriteFiles());

        Path pathToDirectory = m_testInitializer.getTestCaseScratchDir().resolve("dir%20with%20percent%2520encodings");

        Files.createDirectories(pathToDirectory);

        assertTrue(Files.readAttributes(pathToDirectory, BasicFileAttributes.class).isDirectory());

        try (Stream<Path> dirStream = Files.list(m_testInitializer.getTestCaseScratchDir())) {
            assertEquals(1, dirStream.filter(p -> p.equals(pathToDirectory)).count());
        }
    }


    @Test(expected = FileAlreadyExistsException.class)
    public void test_create_directory_which_already_exists() throws Exception {
        Assume.assumeTrue(m_connection.getFSDescriptor().getCapabilities().canWriteFiles());

        Path file = m_testInitializer.createFile("directory", "file");
        Path directory = file.getParent();

        Files.createDirectory(directory);
    }

}
