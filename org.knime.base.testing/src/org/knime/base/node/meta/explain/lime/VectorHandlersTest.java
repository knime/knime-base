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
 *   May 7, 2019 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.base.node.meta.explain.lime;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.knime.core.data.vector.bitvector.DenseBitVectorCell;
import org.knime.core.data.vector.bitvector.DenseBitVectorCellFactory;
import org.knime.core.data.vector.bytevector.DenseByteVectorCell;
import org.knime.core.data.vector.bytevector.DenseByteVectorCellFactory;
import org.knime.core.data.vector.doublevector.DenseDoubleVectorCell;
import org.knime.core.data.vector.doublevector.DoubleVectorCellFactory;

/**
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
public class VectorHandlersTest {

    @Test
    public void testBitVectorHandler() throws Exception {
        final DenseBitVectorCellFactory factory = new DenseBitVectorCellFactory(3);
        factory.set(0);
        factory.set(2);
        final DenseBitVectorCell cell = factory.createDataCell();
        VectorHandler handler = VectorHandlers.getVectorHandler(cell.getType());
        assertEquals(3, handler.getLength(cell));
        assertEquals(1.0, handler.getValue(cell, 0), 1e-5);
        assertEquals(0.0, handler.getValue(cell, 1), 1e-5);
        assertEquals(1.0, handler.getValue(cell, 2), 1e-5);
    }

    @Test
    public void testByteVectorHandler() throws Exception {
        final DenseByteVectorCellFactory factory = new DenseByteVectorCellFactory(3);
        factory.setValue(0, 3);
        factory.setValue(1, 8);
        final DenseByteVectorCell cell = factory.createDataCell();
        VectorHandler handler = VectorHandlers.getVectorHandler(cell.getType());
        assertEquals(3, handler.getLength(cell));
        assertEquals(3.0, handler.getValue(cell, 0), 1e-5);
        assertEquals(8.0, handler.getValue(cell, 1), 1e-5);
        assertEquals(0.0, handler.getValue(cell, 2), 1e-5);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testUnsupportedType() throws Exception {
        DenseDoubleVectorCell cell = DoubleVectorCellFactory.createCell(new double[] {1.0});
        VectorHandlers.getVectorHandler(cell.getType());
    }
}
