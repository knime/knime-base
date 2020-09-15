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
 *   Sep 14, 2020 (Mark Ortmann, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.filehandling.utility.nodes.metainfo;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Unit tests for the KNIMEFileAttributes.
 *
 * @author Mark Ortmann, KNIME GmbH, Berlin, Germany
 */
@RunWith(MockitoJUnitRunner.class)
public class KNIMEFileAttributesTest {

    private BasicFileAttributes m_fileAttributes = null;

    /**
     * Initializes the {@link BasicFileAttributes} mock
     */
    @Before
    public void init() {
        m_fileAttributes = mock(BasicFileAttributes.class);
    }

    @Mock
    private Path m_path = null;

    /**
     * Tests the is directory accessor.
     *
     * @throws IOException - Cannot happen
     */
    @Test
    public void testIsDirectory() throws IOException {
        final boolean isDirectory = true;
        when(m_fileAttributes.isDirectory()).thenReturn(isDirectory);
        assertEquals(isDirectory, initAttributes().isDirectory());
    }

    /**
     * Tests that the size of {@link #m_fileAttributes} is returned in case size calculation is disabled.
     *
     * @throws IOException - Cannot happen
     */
    @SuppressWarnings("javadoc")
    @Test
    public void testGetSizeFromPosixAttributes() throws IOException {
        final long size = 300;
        when(m_fileAttributes.size()).thenReturn(size);
        assertEquals(initAttributes().size(), size);
    }

    /**
     * Tests that the size of {@link #m_fileAttributes} is returned in case size calculation is enabled, but the path
     * points to a file.
     *
     * @throws IOException - Cannot happen
     */
    @SuppressWarnings("javadoc")
    @Test
    public void testGetSizeCalcSizeOnFile() throws IOException {
        final long size = 300;
        when(m_fileAttributes.size()).thenReturn(size);
        when(m_fileAttributes.isDirectory()).thenReturn(false);
        assertEquals(size, new KNIMEFileAttributes(m_path, true, m_fileAttributes).size());
    }

    /**
     * Tests the last modified time access.
     *
     * @throws IOException - Cannot happen
     */

    @Test
    public void testLastModifiedTime() throws IOException {
        final FileTime t = FileTime.fromMillis(4902349);
        when(m_fileAttributes.lastModifiedTime()).thenReturn(t);
        assertEquals(t, initAttributes().lastModifiedTime());
    }

    /**
     * Tests the creation time access.
     *
     * @throws IOException - Cannot happen
     */

    @Test
    public void testCreationTime() throws IOException {
        final FileTime t = FileTime.fromMillis(21423349);
        when(m_fileAttributes.creationTime()).thenReturn(t);
        assertEquals(t, initAttributes().creationTime());
    }

    private KNIMEFileAttributes initAttributes() throws IOException {
        return new KNIMEFileAttributes(m_path, false, m_fileAttributes);
    }
}
