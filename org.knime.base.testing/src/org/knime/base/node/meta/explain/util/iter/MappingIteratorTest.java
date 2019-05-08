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
 *   May 3, 2019 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.base.node.meta.explain.util.iter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Function;

import org.junit.Before;
import org.junit.Test;

/**
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
public class MappingIteratorTest {

    private static int NUM_VALUES = 5;

    private List<Integer> m_source;

    private List<Function<Integer, Integer>> m_mappings;

    private List<Integer> m_expected;

    private MappingIterator<Integer, Integer> m_testInstance;

    @Before
    public void init() {
        m_source = new ArrayList<>(NUM_VALUES);
        m_mappings = new ArrayList<>(NUM_VALUES);
        m_expected = new ArrayList<>(NUM_VALUES);
        for (int i = 0; i < NUM_VALUES; i++) {
            m_source.add(i);
            final int increment = i;
            m_mappings.add(v -> v + increment);
            m_expected.add(i + increment);
        }
        m_testInstance = new MappingIterator<>(m_source.iterator(), m_mappings.iterator());
    }

    @Test
    public void testHasNext() throws Exception {
        for (int i = 0; i < m_source.size(); i++) {
            assertTrue(m_testInstance.hasNext());
            m_testInstance.next();
        }
        assertFalse(m_testInstance.hasNext());
    }

    @Test
    public void testNext() throws Exception {
        final Iterator<Integer> expectedIterator = m_expected.iterator();
        while (expectedIterator.hasNext()) {
            assertEquals(expectedIterator.next(), m_testInstance.next());
        }
    }

    @Test (expected = NoSuchElementException.class)
    public void testNoSuchElement() throws Exception {
        for (int i = 0; i < NUM_VALUES + 1; i++) {
            m_testInstance.next();
        }
    }

}
