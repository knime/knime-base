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
 *   Oct 19, 2020 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.node.table.reader.util;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.util.Collections;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.knime.filehandling.core.node.table.reader.config.TableReadConfig;
import org.knime.filehandling.core.node.table.reader.spec.TypedReaderTableSpec;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Unit tests for the the {@link IndexMapperFactory}.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
@RunWith(MockitoJUnitRunner.class)
public class IndexMapperFactoryTest {

    @Mock
    private TableReadConfig<?> m_tableReadConfig;

    /**
     * Creates a TypedReaderTableSpec that uses the names as types as well.
     *
     * @param names names of the columns
     * @return a {@link TypedReaderTableSpec}
     */
    private static TypedReaderTableSpec<String> createIndividualSpec(final String... names) {
        return TypedReaderTableSpec.create(asList(names), asList(names),
            Collections.nCopies(names.length, Boolean.TRUE));
    }

    /**
     */
    @Test
    public void testWithRowID() {
        when(m_tableReadConfig.useRowIDIdx()).thenReturn(true);
        when(m_tableReadConfig.getRowIDIdx()).thenReturn(1);
        final TypedReaderTableSpec<?> individualSpec = createIndividualSpec("bar", "notInGlobal", "foo");
        final IndexMapperFactory factory = new IndexMapperFactory(asList("foo", "bar", "foobar"), m_tableReadConfig);
        final IndexMapper idxMapper = factory.createIndexMapper(individualSpec);

        assertTrue(idxMapper.hasMapping(0));
        assertTrue(idxMapper.hasMapping(1));
        assertFalse(idxMapper.hasMapping(2));
        // the index 1 is occupied by the row idx in the underlying read while index 1
        // in individualSpec is notInGlobal
        // therefore we need to increase the indices >= 1 when mapping from
        // individualSpec indices to read indices
        assertEquals(3, idxMapper.map(0));
        assertEquals(0, idxMapper.map(1));
    }

    /**
     */
    @Test
    public void testWithoutRowID() {
        final IndexMapperFactory factory = new IndexMapperFactory(asList("foo", "bar", "foobar"), m_tableReadConfig);
        final TypedReaderTableSpec<String> individualSpec = createIndividualSpec("bar", "foo");
        IndexMapper idxMapper = factory.createIndexMapper(individualSpec);
        assertTrue(idxMapper.hasMapping(0));
        assertTrue(idxMapper.hasMapping(1));
        assertFalse(idxMapper.hasMapping(2));
        assertEquals(1, idxMapper.map(0));
        assertEquals(0, idxMapper.map(1));
    }

}
