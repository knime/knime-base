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
package org.knime.filehandling.core.fs.tests.integration.filesystem;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.util.stream.IntStream;

import org.junit.Test;
import org.knime.filehandling.core.fs.tests.integration.AbstractParameterizedFSTest;
import org.knime.filehandling.core.testing.FSTestInitializer;
import org.knime.filehandling.core.util.IOESupplier;

/**
 * Test class for copy operations on file systems.
 *
 * @author Bjoern Lohrmann, KNIME GmbH, Berlin, Germany
 *
 */
public class GetPathTest extends AbstractParameterizedFSTest {

    public GetPathTest(final String fsType, final IOESupplier<FSTestInitializer> testInitializer) throws IOException {
        super(fsType, testInitializer);
    }

    @Test
    public void test_get_path_testing_root() throws Exception {
        final FileSystem fileSystem = m_connection.getFileSystem();
        assertEquals(m_testInitializer.getTestCaseScratchDir(), fileSystem.getPath(m_testInitializer.getTestCaseScratchDir().toString()));
    }

    @Test
    public void test_get_path_testing_root2() throws Exception {
        final FileSystem fileSystem = m_connection.getFileSystem();
        final Path testingRoot = m_testInitializer.getTestCaseScratchDir();
        final String[] pathComponents = IntStream.range(0, testingRoot.getNameCount())//
            .mapToObj(i -> testingRoot.getName(i).toString())//
            .toArray(String[]::new);

        final Path resultingPath = fileSystem.getPath(testingRoot.getRoot().toString(), pathComponents);
        assertEquals(testingRoot, resultingPath);
    }

    @Test
    public void test_get_path_appending_file() {
        final FileSystem fileSystem = m_connection.getFileSystem();
        final Path testingRoot = m_testInitializer.getTestCaseScratchDir();
        assertEquals(testingRoot.resolve("file"), fileSystem.getPath(testingRoot.toString(), "file"));
    }

    @Test
    public void test_get_path_appending_folders() {
        final FileSystem fileSystem = m_connection.getFileSystem();
        final Path testingRoot = m_testInitializer.getTestCaseScratchDir();

        assertEquals(testingRoot.resolve("folder1").resolve("folder2" + fileSystem.getSeparator()),
            fileSystem.getPath(testingRoot.toString(), "folder1", "folder2" + fileSystem.getSeparator()));
    }

}
