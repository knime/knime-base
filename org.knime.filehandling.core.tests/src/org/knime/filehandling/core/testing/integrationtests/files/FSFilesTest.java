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
package org.knime.filehandling.core.testing.integrationtests.files;

import static org.junit.Assert.assertFalse;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.Test;
import org.knime.filehandling.core.connections.FSFiles;
import org.knime.filehandling.core.testing.FSTestInitializer;
import org.knime.filehandling.core.testing.integrationtests.AbstractParameterizedFSTest;
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
}
