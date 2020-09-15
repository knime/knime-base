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
import java.nio.file.attribute.PosixFileAttributes;
import java.time.LocalDateTime;
import java.time.ZoneId;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.knime.core.data.DataType;
import org.knime.core.data.def.BooleanCell;
import org.knime.core.data.def.BooleanCell.BooleanCellFactory;
import org.knime.core.data.def.LongCell;
import org.knime.core.data.def.LongCell.LongCellFactory;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.time.localdatetime.LocalDateTimeCellFactory;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Unit tests for the KNIMEFileAttributes.
 *
 * @author Mark Ortmann, KNIME GmbH, Berlin, Germany
 */
@RunWith(MockitoJUnitRunner.class)
public class KNIMEFileAttributesConverterTest {

    @Mock
    private Path m_path = null;

    BasicFileAttributes m_basicFileAttributes = null;

    /**
     * Initializes the {@link PosixFileAttributes} mock.
     */
    @Before
    public void init() {
        m_basicFileAttributes = mock(BasicFileAttributes.class);
    }

    /**
     * Tests the directory converter.
     *
     * @throws IOException - Cannot happen
     */
    @Test
    public void testDirectoryConverter() throws IOException {
        final boolean isDirectory = true;
        when(m_basicFileAttributes.isDirectory()).thenReturn(isDirectory);
        final KNIMEFileAttributesConverter converter = KNIMEFileAttributesConverter.DIRECTORY;
        assertEquals(BooleanCell.TYPE, converter.getType());
        assertEquals(BooleanCellFactory.create(isDirectory),
            converter.createCell(new KNIMEFileAttributes(m_path, false, m_basicFileAttributes)));
    }

    /**
     * Tests the size converters.
     *
     * @throws IOException - Cannot happen
     */
    @Test
    public void testSizeConverters() throws IOException {
        final long size = 300;
        when(m_basicFileAttributes.size()).thenReturn(size);
        KNIMEFileAttributesConverter converter = KNIMEFileAttributesConverter.SIZE;
        assertEquals(LongCell.TYPE, converter.getType());
        final KNIMEFileAttributesConverter converter1 = converter;
        assertEquals(LongCellFactory.create(size),
            converter1.createCell(new KNIMEFileAttributes(m_path, false, m_basicFileAttributes)));

        converter = KNIMEFileAttributesConverter.HUMANSIZE;
        assertEquals(StringCell.TYPE, converter.getType());
        final KNIMEFileAttributesConverter converter2 = converter;
        assertEquals(new StringCell("300 bytes"),
            converter2.createCell(new KNIMEFileAttributes(m_path, false, m_basicFileAttributes)));
    }

    /**
     * Test the last modified date converter.
     *
     * @throws IOException - Cannot happen
     */
    @Test
    public void testLastModifiedDateConverter() throws IOException {
        final FileTime t = FileTime.fromMillis(4902349);
        when(m_basicFileAttributes.lastModifiedTime()).thenReturn(t);
        final KNIMEFileAttributesConverter converter = KNIMEFileAttributesConverter.LAST_MODIFIED_DATE;
        assertEquals(LocalDateTimeCellFactory.TYPE, converter.getType());
        assertEquals(LocalDateTimeCellFactory.create(LocalDateTime.ofInstant(t.toInstant(), ZoneId.systemDefault())),
            converter.createCell(new KNIMEFileAttributes(m_path, false, m_basicFileAttributes)));
    }

    /**
     * Tests that the last modified date converter returns a missing value if the last modified instant is 0L.
     *
     * @throws IOException - Cannot happen
     */
    @Test
    public void testLastModifiedDateNotAvailableConverter() throws IOException {
        final FileTime t = FileTime.fromMillis(0);
        when(m_basicFileAttributes.lastModifiedTime()).thenReturn(t);
        final KNIMEFileAttributesConverter converter = KNIMEFileAttributesConverter.LAST_MODIFIED_DATE;
        assertEquals(LocalDateTimeCellFactory.TYPE, converter.getType());
        assertEquals(DataType.getMissingCell(),
            converter.createCell(new KNIMEFileAttributes(m_path, false, m_basicFileAttributes)));
    }

    /**
     * Tests the created date converter.
     *
     * @throws IOException - Cannot happen
     */
    @Test
    public void testCreatedDateConverter() throws IOException {
        final FileTime t = FileTime.fromMillis(21423349);
        when(m_basicFileAttributes.creationTime()).thenReturn(t);
        final KNIMEFileAttributesConverter converter = KNIMEFileAttributesConverter.CREATION_DATE;
        assertEquals(LocalDateTimeCellFactory.TYPE, converter.getType());
        assertEquals(LocalDateTimeCellFactory.create(LocalDateTime.ofInstant(t.toInstant(), ZoneId.systemDefault())),
            converter.createCell(new KNIMEFileAttributes(m_path, false, m_basicFileAttributes)));
    }

    /**
     * Tests that the created date converter returns a missing value if the creation instant is 0L.
     *
     * @throws IOException - Cannot happen
     */
    @Test
    public void testCreatedDateNotAvailableConverter() throws IOException {
        final FileTime t = FileTime.fromMillis(0);
        when(m_basicFileAttributes.creationTime()).thenReturn(t);
        final KNIMEFileAttributesConverter converter = KNIMEFileAttributesConverter.CREATION_DATE;
        assertEquals(LocalDateTimeCellFactory.TYPE, converter.getType());
        assertEquals(DataType.getMissingCell(),
            converter.createCell(new KNIMEFileAttributes(m_path, false, m_basicFileAttributes)));
    }

    /**
     * Tests the file/folder exists converter.
     *
     * @throws IOException - Cannot happen
     */
    @Test
    public void testExistsConverter() throws IOException {
        final KNIMEFileAttributesConverter converter = KNIMEFileAttributesConverter.EXISTS;
        assertEquals(BooleanCell.TYPE, converter.getType());
        assertEquals(BooleanCellFactory.create(true),
            converter.createCell(new KNIMEFileAttributes(m_path, false, m_basicFileAttributes)));
    }

}
