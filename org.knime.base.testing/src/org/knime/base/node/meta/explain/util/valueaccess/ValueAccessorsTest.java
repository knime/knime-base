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
package org.knime.base.node.meta.explain.util.valueaccess;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.vector.bitvector.DenseBitVectorCell;
import org.knime.core.data.vector.bitvector.DenseBitVectorCellFactory;
import org.knime.core.data.vector.bytevector.DenseByteVectorCell;
import org.knime.core.data.vector.bytevector.DenseByteVectorCellFactory;

/**
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
public class ValueAccessorsTest {

    @Test
    public void testGetByteVectorValueAccessor() throws Exception {
        DenseByteVectorCellFactory factory = new DenseByteVectorCellFactory(3);
        factory.setValue(0, 1);
        factory.setValue(1, 2);
        final DenseByteVectorCell cell = factory.createDataCell();
        final NumericValueAccessor access1 = ValueAccessors.getByteVectorValueAccessor(0);
        final NumericValueAccessor access2 = ValueAccessors.getByteVectorValueAccessor(1);
        final NumericValueAccessor access3 = ValueAccessors.getByteVectorValueAccessor(2);
        assertEquals(1.0, access1.getValue(cell), 1e-10);
        assertEquals(2.0, access2.getValue(cell), 1e-10);
        assertEquals(0.0, access3.getValue(cell), 1e-10);
    }

    @Test
    public void testGetBitVectorValueAccessor() throws Exception {
        DenseBitVectorCellFactory factory = new DenseBitVectorCellFactory(3);
        factory.set(1);
        final DenseBitVectorCell cell = factory.createDataCell();
        final NumericValueAccessor access1 = ValueAccessors.getBitVectorValueAccessor(0);
        final NumericValueAccessor access2 = ValueAccessors.getBitVectorValueAccessor(1);
        assertEquals(0.0, access1.getValue(cell), 1e-10);
        assertEquals(1.0, access2.getValue(cell), 1e-10);
    }

    @Test
    public void testGetDoubleValueAccessor() throws Exception {
        final DoubleCell cell = new DoubleCell(3.0);
        final NumericValueAccessor access = ValueAccessors.getDoubleValueAccessor();
        assertEquals(3.0, access.getValue(cell), 1e-10);
    }

    @Test
    public void testGetNominalValueAccessor() throws Exception {
        final StringCell cell = new StringCell("test");
        final NominalValueAccessor access = ValueAccessors.getNominalValueAccessor();
        assertEquals(cell, access.getValue(cell));
    }

}
