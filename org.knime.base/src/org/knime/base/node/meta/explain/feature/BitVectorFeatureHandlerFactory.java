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
 *   01.04.2019 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.base.node.meta.explain.feature;

import java.util.Iterator;

import org.knime.core.data.DataCell;
import org.knime.core.data.vector.bitvector.BitVectorValue;
import org.knime.core.data.vector.bitvector.DenseBitVectorCell;
import org.knime.core.data.vector.bitvector.DenseBitVectorCellFactory;
import org.knime.core.data.vector.bitvector.SparseBitVectorCell;
import org.knime.core.data.vector.bitvector.SparseBitVectorCellFactory;
import org.knime.core.node.util.CheckUtils;

final class BitVectorFeatureHandlerFactory extends AbstractCollectionFeatureHandlerFactory<BitVectorValue> {

    /**
         * {@inheritDoc}
         */
        @Override
        public FeatureHandler createFeatureHandler() {
            return new BitVectorFeatureHandler(getCaster());
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected int getNumFeatures(final BitVectorValue value) {
            final long length = value.length();
            CheckUtils.checkArgument(length <= Integer.MAX_VALUE,
                "Only bit vectors with a length up to Integer.MAX_VALUE are supported.");
            return (int)value.length();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected Class<BitVectorValue> getAcceptValueClass() {
            return BitVectorValue.class;
        }

    /**
     *
     *
     * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
     */
    static class BitVectorFeatureHandler extends AbstractCollectionFeatureHandler<BitVectorValue> {

        /**
         * @param caster
         */
        public BitVectorFeatureHandler(final Caster<BitVectorValue> caster) {
            super(caster);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public DataCell createReplaced() {
            final BVFactory factory = getFactory(m_original);
            final Iterator<Integer> iter = getReplacedIterator();
            while (iter.hasNext()) {
                final int idx = iter.next().intValue();
                factory.set(idx, m_sampled.get(idx));
            }
            return factory.createDataCell();
        }

        private static BVFactory getFactory(final BitVectorValue val) {
            if (val instanceof DenseBitVectorCell) {
                return new DenseBVFactory((DenseBitVectorCell)val);
            } else if (val instanceof SparseBitVectorCell) {
                return new SparseBVFactory((SparseBitVectorCell)val);
            } else {
                return new DefaultBVFactory(val);
            }
        }

    }

    private interface BVFactory {
        DataCell createDataCell();

        void set(int idx, boolean value);
    }

    private static class DenseBVFactory implements BVFactory {
        private final DenseBitVectorCellFactory m_factory;

        public DenseBVFactory(final DenseBitVectorCell bvCell) {
            m_factory = new DenseBitVectorCellFactory(bvCell, 0, bvCell.length());
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public DataCell createDataCell() {
            return m_factory.createDataCell();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void set(final int idx, final boolean value) {
            m_factory.set(idx, value);
        }
    }

    private static class SparseBVFactory implements BVFactory {
        private final SparseBitVectorCellFactory m_factory;

        public SparseBVFactory(final SparseBitVectorCell bvCell) {
            m_factory = new SparseBitVectorCellFactory(bvCell, 0, bvCell.length());
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public DataCell createDataCell() {
            return m_factory.createDataCell();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void set(final int idx, final boolean value) {
            m_factory.set(idx, value);
        }

    }

    private static class DefaultBVFactory implements BVFactory {
        private final DenseBitVectorCellFactory m_factory;

        public DefaultBVFactory(final BitVectorValue bv) {
            m_factory = new DenseBitVectorCellFactory(bv.length());
            for (long i = bv.nextSetBit(0); i >= 0; i = bv.nextSetBit(i + 1)) {
                m_factory.set(i);
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public DataCell createDataCell() {
            return m_factory.createDataCell();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void set(final int idx, final boolean value) {
            m_factory.set(idx, value);
        }
    }

}