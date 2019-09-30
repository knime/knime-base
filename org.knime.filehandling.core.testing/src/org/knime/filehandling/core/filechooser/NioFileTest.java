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
 *   17.09.2019 (Mareike Hoeger, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.filechooser;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.junit.Assume;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * These Junit test test whether the wrapped NIOFile methods behave in the same
 * manner as a native {@link java.io.File}
 *
 * @author Mareike Hoeger, KNIME GmbH, Konstanz, Germany
 *
 */
public class NioFileTest {

    /**
     * Temporary test folder
     */
    @Rule
    public final TemporaryFolder m_testFolder = new TemporaryFolder();

    private NioFile m_nioFile;

    private File m_file;

    private NioFile m_nioDir;

    private File m_dir;

    /**
     * Inits the tests by creating a temporary folder and a nio Path for the tests
     *
     * @throws IOException
     */
    @Before
    public void init() throws IOException {

        m_file = m_testFolder.newFile("testFile");
        m_dir = m_testFolder.getRoot();

        m_nioFile = new NioFile(m_file.toPath());
        m_nioDir = new NioFile(m_dir.toPath());

    }

    /**
     * Tests whether the name is equal.
     */
    @Test
    public void get_name_equal() {
        assertEquals("Names do not match", m_file.getName(), m_nioFile.getName());
    }

    /**
     * Tests whether absolute is equal for an absolute path.
     */
    @Test
    public void absolute_equals() {
        assertEquals(m_file.isAbsolute(), m_nioFile.isAbsolute());
    }

    /**
     * Tests whether exists equals.
     */
    @Test
    public void exists_equals() {
        assertEquals(m_file.exists(), m_nioFile.exists());
    }

    /**
     * Tests whether isFile equals.
     */
    @Test
    public void isFile_equals() {
        assertEquals(m_file.isFile(), m_nioFile.isFile());
        assertEquals(m_dir.isFile(), m_nioDir.isFile());
    }

    /**
     * Tests whether isDirectory equals.
     */
    @Test
    public void isDirectory_equals() {
        assertEquals(m_file.isDirectory(), m_nioFile.isDirectory());
        assertEquals(m_dir.isDirectory(), m_nioDir.isDirectory());
    }

    /**
     * Tests whether isHidden equals.
     */
    @Test
    public void isHidden_equals() {
        assertEquals(m_file.isHidden(), m_nioFile.isHidden());
    }

    /**
     * Tests whether getCanonicalPath equals.
     *
     * @throws IOException
     */
    @Test
    public void getCanonicalPath_equals() throws IOException {
        assertEquals(m_file.getCanonicalPath(), m_nioFile.getCanonicalPath());
    }

    /**
     * Tests whether getCanonicalFile equals.
     *
     * @throws IOException
     */
    @Test
    public void getCanonicalFile_equals() throws IOException {
        assertEquals(m_file.getCanonicalFile(), m_nioFile.getCanonicalFile());
    }

    /**
     * Tests whether toURL equals.
     *
     * @throws IOException
     */
    @SuppressWarnings("deprecation")
    @Test
    public void toUrl_equals() throws IOException {
        assertEquals(m_file.toURL(), m_nioFile.toURL());
    }

    /**
     * Tests whether toURI equals.
     *
     * @throws IOException
     */
    @Test
    public void toURI_equals() throws IOException {
        assertEquals(m_file.toURI(), m_nioFile.toURI());
    }

    /**
     * Tests whether getParent equals.
     */
    @Test
    public void get_parent_equals() {
        assertEquals(m_file.getParent(), m_nioFile.getParent());
    }

    /**
     * Tests whether getParentFile equals.
     */
    @Test
    public void get_parent_file_equals() {
        assertEquals(m_file.getParentFile(), m_nioFile.getParentFile());
    }

    /**
     * Tests whether set read only equals. POSIX FileSystems only
     *
     */
    @Test
    public void read_only_permissions_equals() {
        Assume.assumeFalse(System.getProperty("os.name").toLowerCase().startsWith("win"));
        m_nioFile.setReadOnly();

        assertEquals(m_file.canRead(), m_nioFile.canRead());
        assertTrue(m_nioFile.canRead());
        assertEquals(m_file.canWrite(), m_nioFile.canWrite());
        assertFalse(m_nioFile.canWrite());
        assertEquals(m_file.canExecute(), m_nioFile.canExecute());
        assertFalse(m_nioFile.canExecute());
    }

    /**
     * Tests whether set execute permissions equals. POSIX FileSystems only
     */
    @Test
    public void execute_permissions_equals() {
        Assume.assumeFalse(System.getProperty("os.name").toLowerCase().startsWith("win"));
        m_nioFile.setExecutable(true, false);
        assertTrue(m_nioFile.canExecute());

        assertEquals(m_file.canExecute(), m_nioFile.canExecute());

    }

    /**
     * Test whether set write permissions equals. POSIX FileSystems only
     */
    @Test
    public void write_permissions_equals() {
        Assume.assumeFalse(System.getProperty("os.name").toLowerCase().startsWith("win"));
        m_nioFile.setWritable(true, false);

        assertTrue(m_nioFile.canWrite());
        assertEquals(m_file.canWrite(), m_nioFile.canWrite());

    }

    /**
     * Tests whether file length equals.
     *
     * @throws IOException
     */
    @Test
    public void length_equals() throws IOException {

        assertEquals(m_file.length(), m_nioFile.length());

        try (FileWriter fileWriter = new FileWriter(m_file)) {
            fileWriter.write("Some Test Data");
        }

        assertEquals(m_file.length(), m_nioFile.length());
    }

    /**
     * Tests whether lastModified equals.
     */
    @Test
    public void lastModified_equals() {
        // JNI lastModified losses the milliseconds, so we have to round here
        // see https://bugs.openjdk.java.net/browse/JDK-8177809

        assertEquals((m_file.lastModified() / 1000) * 1000, (m_nioFile.lastModified() / 1000) * 1000);

    }

    /**
     * Tests whether file renaming works.
     */
    @Test
    public void renaming_test() {

        final String name = "newName";
        final File renamedFile = new File(m_testFolder.getRoot(), name);
        m_nioFile.renameTo(renamedFile);

        assertTrue(renamedFile.exists());
        assertFalse(m_nioFile.exists());
    }

    /**
     * Tests whether file deleting works.
     */
    @Test
    public void delete_test() {
        m_nioFile.delete();
        assertFalse(m_nioFile.exists());
        assertEquals(m_file.exists(), m_nioFile.exists());
    }

    /**
     * Tests whether getUsableSpace equals.
     */
    @Test
    public void usable_space_equals() {
        assertEquals(m_file.getUsableSpace(), m_nioFile.getUsableSpace());
    }

    /**
     * Tests whether getTotalSpace equals.
     */
    @Test
    public void total_space_equals() {
        assertEquals(m_file.getTotalSpace(), m_nioFile.getTotalSpace());
    }

    /**
     * Tests whether getFreeSpace equals.
     */
    @Test
    public void free_space_equals() {
        assertEquals(m_file.getFreeSpace(), m_nioFile.getFreeSpace());
    }

    /**
     * Tests whether listing of files is equal.
     */
    @Test
    public void list_equals() {
        assertArrayEquals(m_dir.list(), m_nioDir.list());
    }

    /**
     * Tests whether listing of files is equal.
     */
    @Test
    public void listFile_equals() {
        assertArrayEquals(m_dir.listFiles(), m_nioDir.listFiles());
    }
}
