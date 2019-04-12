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
 *   Apr 10, 2019 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.base.node.meta.explain.feature;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.knime.base.node.meta.explain.feature.BitVectorFeatureHandlerFactory;
import org.knime.base.node.meta.explain.feature.FeatureHandler;
import org.knime.core.data.DataCell;
import org.knime.core.data.vector.bitvector.DenseBitVectorCell;
import org.knime.core.data.vector.bitvector.DenseBitVectorCellFactory;
import org.knime.core.data.vector.bitvector.SparseBitVectorCell;
import org.knime.core.data.vector.bitvector.SparseBitVectorCellFactory;

/**
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
public class BitVectorFeatureHandlerFactoryTest {

    @Test
    public void testDenseBitVector() throws Exception {
        final DenseBitVectorCellFactory original = new DenseBitVectorCellFactory(10);
        final DenseBitVectorCellFactory sampled = new DenseBitVectorCellFactory(10);

        original.set(0);
        original.set(2);
        original.set(4);

        sampled.set(1);
        sampled.set(3);
        sampled.set(5);

        final FeatureHandler fh = new BitVectorFeatureHandlerFactory().createFeatureHandler();

        fh.setOriginal(original.createDataCell());
        fh.setSampled(sampled.createDataCell());

        fh.markForReplacement(0);
        fh.markForReplacement(3);
        fh.markForReplacement(5);

        final boolean[] expected =
            new boolean[]{false, false, true, true, true, true, false, false, false, false};

        final DataCell cell = fh.createReplaced();
        assertTrue(cell instanceof DenseBitVectorCell);
        final DenseBitVectorCell bv = (DenseBitVectorCell)cell;

        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i], bv.get(i));
        }

    }

    @Test
    public void testSparseBitVector() throws Exception {
        final SparseBitVectorCellFactory original = new SparseBitVectorCellFactory(10);
        final SparseBitVectorCellFactory sampled = new SparseBitVectorCellFactory(10);

        original.set(0);
        original.set(2);
        original.set(4);

        sampled.set(1);
        sampled.set(3);
        sampled.set(5);

        final FeatureHandler fh = new BitVectorFeatureHandlerFactory().createFeatureHandler();

        fh.setOriginal(original.createDataCell());
        fh.setSampled(sampled.createDataCell());

        fh.markForReplacement(0);
        fh.markForReplacement(3);
        fh.markForReplacement(5);

        final boolean[] expected =
            new boolean[]{false, false, true, true, true, true, false, false, false, false};

        final DataCell cell = fh.createReplaced();
        assertTrue(cell instanceof SparseBitVectorCell);
        final SparseBitVectorCell bv = (SparseBitVectorCell)cell;

        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i], bv.get(i));
        }

    }

}
