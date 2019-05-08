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
 *   May 6, 2019 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.base.node.meta.explain.util.iter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.NoSuchElementException;

import org.junit.Before;
import org.junit.Test;
import org.knime.base.node.meta.explain.TestingUtil;

import com.google.common.collect.Lists;

/**
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
public class ConcatenatedDoubleIteratorTest {

    private static final double[] ARRAY1 = new double[] {1.0, 2.0};

    private static final double[] ARRAY2 = new double[] {3.0};

    private static final double[] EMPTY_ARRAY = new double[0];

    private static final DoubleIterable ITERABLE1 = TestingUtil.createDoubleIterable(ARRAY1);

    private static final DoubleIterable ITERABLE2 = TestingUtil.createDoubleIterable(ARRAY2);

    private static final DoubleIterable EMPTY_ITERABLE = TestingUtil.createDoubleIterable(EMPTY_ARRAY);

    DoubleIterator m_testInstance;

    @Before
    public void init() {
        m_testInstance = new ConcatenatedDoubleIterator(Lists.newArrayList(ITERABLE1, ITERABLE2).iterator());
    }

    @Test
    public void testHasNext() throws Exception {
        assertTrue(m_testInstance.hasNext());
        m_testInstance.next();
        assertTrue(m_testInstance.hasNext());
        m_testInstance.next();
        assertTrue(m_testInstance.hasNext());
        m_testInstance.next();
        assertFalse(m_testInstance.hasNext());
    }

    @Test
    public void testNext() throws Exception {
        for (int i = 0; i < ARRAY1.length; i++) {
            assertEquals(ARRAY1[i], m_testInstance.next(), 1e-10);
        }
        for (int j = 0; j < ARRAY2.length; j++) {
            assertEquals(ARRAY2[j], m_testInstance.next(), 1e-10);
        }
    }

    @Test (expected = NoSuchElementException.class)
    public void testNoSuchElement() throws Exception {
        for (int i = 0; i < ARRAY1.length + ARRAY2.length; i++) {
            m_testInstance.next();
        }
        m_testInstance.next();
    }

    @Test
    public void testEmptyIterableAtTheEnd() throws Exception {
        m_testInstance = new ConcatenatedDoubleIterator(Lists.newArrayList(ITERABLE1, EMPTY_ITERABLE).iterator());
        for (int i = 0; i < ARRAY1.length; i++) {
            assertTrue(m_testInstance.hasNext());
            m_testInstance.next();
        }
        assertFalse(m_testInstance.hasNext());
    }

}
