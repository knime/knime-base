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
 *   2021-08-31 (Alexander Bondaletov): created
 */
package org.knime.filehandling.core.fs.tests.integration.filesystemprovider;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFilePermission;
import java.util.HashSet;
import java.util.Set;

import org.junit.Assume;
import org.junit.Test;
import org.knime.filehandling.core.connections.FSPath;
import org.knime.filehandling.core.connections.meta.FSCapabilities;
import org.knime.filehandling.core.fs.tests.integration.AbstractParameterizedFSTest;
import org.knime.filehandling.core.testing.FSTestInitializer;
import org.knime.filehandling.core.util.IOESupplier;

/**
 * Test gettings and settings POSIX file attributes.
 *
 * @author Alexander Bondaletov
 */
public class PosixAttributesTest extends AbstractParameterizedFSTest {

    public PosixAttributesTest(final String fsType, final IOESupplier<FSTestInitializer> testInitializer)
        throws IOException {
        super(fsType, testInitializer);
    }

    @Override
    public void beforeTestCase() throws IOException {
        super.beforeTestCase();
        FSCapabilities capabilities = m_connection.getFSDescriptor().getCapabilities();
        Assume.assumeTrue("FS doesn't support POSIX attributes",
            capabilities.canGetPosixAttributes() && capabilities.canSetPosixAttributes());
    }

    @Test
    public void test_owner_read_on_file() throws IOException {
        testPermissionForFile(PosixFilePermission.OWNER_READ);
    }

    @Test
    public void test_owner_read_on_dir() throws IOException {
        testPermissionForDir(PosixFilePermission.OWNER_READ);
    }

    @Test
    public void test_owner_write_on_file() throws IOException {
        testPermissionForFile(PosixFilePermission.OWNER_WRITE);
    }

    @Test
    public void test_owner_write_on_dir() throws IOException {
        testPermissionForDir(PosixFilePermission.OWNER_WRITE);
    }

    @Test
    public void test_owner_exec_on_file() throws IOException {
        testPermissionForFile(PosixFilePermission.OWNER_EXECUTE);
    }

    @Test
    public void test_owner_exec_on_dir() throws IOException {
        testPermissionForDir(PosixFilePermission.OWNER_EXECUTE);
    }

    @Test
    public void test_group_read_on_file() throws IOException {
        testPermissionForFile(PosixFilePermission.GROUP_READ);
    }

    @Test
    public void test_group_read_on_dir() throws IOException {
        testPermissionForDir(PosixFilePermission.GROUP_READ);
    }

    @Test
    public void test_group_write_on_file() throws IOException {
        testPermissionForFile(PosixFilePermission.GROUP_WRITE);
    }

    @Test
    public void test_group_write_on_dir() throws IOException {
        testPermissionForDir(PosixFilePermission.GROUP_WRITE);
    }

    @Test
    public void test_group_exec_on_file() throws IOException {
        testPermissionForFile(PosixFilePermission.GROUP_EXECUTE);
    }

    @Test
    public void test_group_exec_on_dir() throws IOException {
        testPermissionForDir(PosixFilePermission.GROUP_EXECUTE);
    }

    @Test
    public void test_others_read_on_file() throws IOException {
        testPermissionForFile(PosixFilePermission.OTHERS_READ);
    }

    @Test
    public void test_others_read_on_dir() throws IOException {
        testPermissionForDir(PosixFilePermission.OTHERS_READ);
    }

    @Test
    public void test_others_write_on_file() throws IOException {
        testPermissionForFile(PosixFilePermission.OTHERS_WRITE);
    }

    @Test
    public void test_others_write_on_dir() throws IOException {
        testPermissionForDir(PosixFilePermission.OTHERS_WRITE);
    }

    @Test
    public void test_others_exec_on_file() throws IOException {
        testPermissionForFile(PosixFilePermission.OTHERS_EXECUTE);
    }

    @Test
    public void test_others_exec_on_dir() throws IOException {
        testPermissionForDir(PosixFilePermission.OTHERS_EXECUTE);
    }

    private void testPermissionForFile(final PosixFilePermission permission) throws IOException {
        FSPath path = m_testInitializer.createFile("posix-test");
        testPermissionForPath(path, permission);
    }

    private void testPermissionForDir(final PosixFilePermission permission) throws IOException {
        FSPath path = m_testInitializer.makePath("test-dir");
        Files.createDirectories(path);
        testPermissionForPath(path, permission);
    }

    private static void testPermissionForPath(final FSPath path, final PosixFilePermission permission)
        throws IOException {
        //1. Initialize permission with true value
        Set<PosixFilePermission> permissions = Files.getPosixFilePermissions(path);
        if (!permissions.contains(permission)) {
            Set<PosixFilePermission> toSet = new HashSet<>(permissions);
            toSet.add(permission);
            Files.setPosixFilePermissions(path, toSet);
        }
        assertTrue(Files.getPosixFilePermissions(path).contains(permission));

        //2. Test transition true -> false
        Set<PosixFilePermission> toSet = new HashSet<>(Files.getPosixFilePermissions(path));
        toSet.remove(permission);
        Files.setPosixFilePermissions(path, toSet);
        assertFalse(Files.getPosixFilePermissions(path).contains(permission));

        //3. Test transition false -> true
        toSet = new HashSet<>(Files.getPosixFilePermissions(path));
        toSet.add(permission);
        Files.setPosixFilePermissions(path, toSet);
        assertTrue(Files.getPosixFilePermissions(path).contains(permission));
    }
}
