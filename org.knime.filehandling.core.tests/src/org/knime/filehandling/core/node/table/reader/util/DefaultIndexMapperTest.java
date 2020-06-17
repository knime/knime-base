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
 *   Mar 31, 2020 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.node.table.reader.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.OptionalInt;

import org.junit.Test;
import org.knime.filehandling.core.node.table.reader.util.DefaultIndexMapper.DefaultIndexMapperBuilder;

/**
 * Contains unit tests for {@link DefaultIndexMapper}.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
public class DefaultIndexMapperTest {

    /**
     * Tests the behavior if no RowID column is contained.
     */
    @Test
    public void testNoRowIDIdx() {
        DefaultIndexMapper idxMapper = DefaultIndexMapper.builder(5)//
            .addMapping(0, 0)//
            .addMapping(1, 2)//
            .addMapping(3, 1)//
            .addMapping(4, 3)//
            .build();
        assertEquals(OptionalInt.of(4), idxMapper.getIndexRangeEnd());
        assertEquals(OptionalInt.empty(), idxMapper.getRowIDIdx());
        assertTrue(idxMapper.hasMapping(0));
        assertTrue(idxMapper.hasMapping(1));
        assertFalse(idxMapper.hasMapping(2));
        assertTrue(idxMapper.hasMapping(3));
        assertTrue(idxMapper.hasMapping(4));

        assertEquals(0, idxMapper.map(0));
        assertEquals(2, idxMapper.map(1));
        assertEquals(1, idxMapper.map(3));
        assertEquals(3, idxMapper.map(4));
    }

    /**
     * Tests the behavior if a RowID column is contained.
     */
    @Test
    public void testRowIDIdx() {
        DefaultIndexMapper idxMapper = DefaultIndexMapper.builder(5, 2)//
            .addMapping(0, 0)//
            .addMapping(1, 2)//
            .addMapping(3, 1)//
            .addMapping(4, 3)//
            .build();
        assertEquals(OptionalInt.of(4), idxMapper.getIndexRangeEnd());
        assertEquals(OptionalInt.of(2), idxMapper.getRowIDIdx());
        assertTrue(idxMapper.hasMapping(0));
        assertTrue(idxMapper.hasMapping(1));
        assertFalse(idxMapper.hasMapping(2));
        assertTrue(idxMapper.hasMapping(3));
        assertTrue(idxMapper.hasMapping(4));

        assertEquals(0, idxMapper.map(0));
        assertEquals(3, idxMapper.map(1));
        assertEquals(1, idxMapper.map(3));
        assertEquals(4, idxMapper.map(4));
    }

    /**
     * Tests if {@link DefaultIndexMapper#map(int)} fails if the argument is negative.
     */
    @Test(expected = IndexOutOfBoundsException.class)
    public void testMapFailsOnNegativeIdx() {
        DefaultIndexMapper.builder(5).addMapping(0, 0).build().map(-1);
    }

    /**
     * Tests if {@link DefaultIndexMapper#map(int)} fails if the argument is negative.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testMapFailsIfNoMappingIsAvailable() {
        DefaultIndexMapper.builder(5).addMapping(0, 0).build().map(1);
    }

    /**
     * Tests if {@link DefaultIndexMapperBuilder#addMapping(int, int)} fails if the from argument is negative.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testAddMappingFailsOnNegativeFrom() {
        DefaultIndexMapper.builder(5).addMapping(-1, 0);
    }

    /**
     * Tests if {@link DefaultIndexMapperBuilder#addMapping(int, int)} fails if the to argument is negative.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testAddMappingFailsOnNegativeTo() {
        DefaultIndexMapper.builder(5).addMapping(0, -1);
    }

}
