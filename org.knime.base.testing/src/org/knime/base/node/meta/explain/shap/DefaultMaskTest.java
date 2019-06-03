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
 *   May 10, 2019 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.base.node.meta.explain.shap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Test;
import org.knime.base.node.meta.explain.util.iter.IntIterator;

/**
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
public class DefaultMaskTest {

    private static IntIterator createComplementIterator(final int[] expected, final int numFeatures) {
        final Set<Integer> expectedSet = Arrays.stream(expected).boxed().collect(Collectors.toSet());
        final int[] indices = new int[numFeatures - expected.length];
        int i = 0;
        for (int j = 0; j < numFeatures; j++) {
            if (!expectedSet.contains(j)) {
                indices[i] = j;
                i++;
            }
        }
        final Mask mask = new DefaultMask(indices, numFeatures);
        return mask.getComplement().iterator();
    }

    @Test
    public void testComplementIterator() throws Exception {
        int[] expected = new int[] {0, 1};
        IntIterator iter = createComplementIterator(expected, 3);
        testIterator(iter, expected);

        expected = new int[] {0, 2};
        iter = createComplementIterator(expected, 3);
        testIterator(iter, expected);

        expected = new int[] {1, 2};
        iter = createComplementIterator(expected, 3);
        testIterator(iter, expected);

        expected = new int[] {0};
        iter = createComplementIterator(expected, 3);
        testIterator(iter, expected);

        expected = new int[] {1};
        iter = createComplementIterator(expected, 3);
        testIterator(iter, expected);

        expected = new int[] {2};
        iter = createComplementIterator(expected, 3);
        testIterator(iter, expected);
    }

    private void testIterator(final IntIterator iterator, final int[] expected) {
        for (int expectedIdx : expected) {
            assertTrue(iterator.hasNext());
            assertEquals(expectedIdx, iterator.next());
        }
        assertFalse(iterator.hasNext());
    }

    @Test
    public void testToString() throws Exception {
        final int[] included = new int[] {0, 2};
        Mask mask = new DefaultMask(included, 3);
        assertEquals("101", mask.toString());
        assertEquals("010", mask.getComplement().toString());
    }

    @Test
    public void testEquals() throws Exception {
        final int[] included = new int[] {0, 2};
        Mask mask1 = new DefaultMask(new int[] {0, 2}, 3);
        assertEquals(mask1, mask1);
        assertEquals(mask1.getComplement(), mask1.getComplement());
        Mask mask2 = new DefaultMask(included, 3);
        assertTrue(mask1.equals(mask2));
        assertTrue(mask1.getComplement().equals(mask2.getComplement()));
        Mask mask3 = new DefaultMask(included, 4);
        assertFalse(mask1.equals(mask3));
        assertFalse(mask1.getComplement().equals(mask3.getComplement()));
    }

    @Test
    public void testGetNumberOfFeatures() throws Exception {
        final int numFeatures = 3;
        final Mask mask = new DefaultMask(new int[] {1, 2}, numFeatures);
        assertEquals(numFeatures, mask.getNumberOfFeatures());
        assertEquals(numFeatures, mask.getComplement().getNumberOfFeatures());
    }

    @Test
    public void testGetCardinality() throws Exception {
        final Mask mask = new DefaultMask(new int[] {1, 2}, 3);
        assertEquals(2, mask.getCardinality());
        assertEquals(1, mask.getComplement().getCardinality());
    }
}
