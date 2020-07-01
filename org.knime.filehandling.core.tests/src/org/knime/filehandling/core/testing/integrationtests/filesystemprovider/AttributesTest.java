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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;

import org.junit.Test;
import org.knime.filehandling.core.testing.FSTestInitializer;
import org.knime.filehandling.core.testing.integrationtests.AbstractParameterizedFSTest;
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
    public void test_get_attributes_of_non_existsing_path() throws Exception {
        final Path testFile = m_testInitializer.makePath("non-existing-file");
        assertFalse(Files.isRegularFile(testFile));
        assertFalse(Files.isDirectory(testFile));
        assertFalse(Files.isReadable(testFile));
        assertFalse(Files.isWritable(testFile));
        assertFalse(Files.isHidden(testFile));
    }

    @Test
    public void test_get_file_attribute_view() throws Exception {
        final Path file = m_testInitializer.createFile("file");

        final BasicFileAttributeView view = Files.getFileAttributeView(file, BasicFileAttributeView.class);
        final BasicFileAttributes attribs = view.readAttributes();

        assertTrue(attribs.isRegularFile());
        assertFalse(attribs.isDirectory());
        assertFalse(attribs.isOther());
        assertTrue(attribs.lastModifiedTime().toInstant().isBefore(Instant.now()));
    }
}
