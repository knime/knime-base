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
 *   Apr 2, 2019 (Adrian Nembach): created
 */
package org.knime.base.node.meta.explain.feature;

import java.util.Iterator;

import org.knime.core.data.DataCell;
import org.knime.core.data.vector.bytevector.ByteVectorValue;
import org.knime.core.data.vector.bytevector.DenseByteVectorCell;
import org.knime.core.data.vector.bytevector.DenseByteVectorCellFactory;
import org.knime.core.node.util.CheckUtils;

final class ByteVectorFeatureHandlerFactory extends AbstractCollectionFeatureHandlerFactory<ByteVectorValue> {

    /**
     * {@inheritDoc}
     */
    @Override
    public ByteVectorFeatureHandler createFeatureHandler() {
        return new ByteVectorFeatureHandler(getCaster());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Class<ByteVectorValue> getAcceptValueClass() {
        return ByteVectorValue.class;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected int getNumFeatures(final ByteVectorValue value) {
        final long length = value.length();
        CheckUtils.checkArgument(length <= Integer.MAX_VALUE,
            "Only byte vectors with a length up to Integer.Max_VALUE are supported.");
        return (int)length;
    }


    /**
     *
     * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
     */
    static final class ByteVectorFeatureHandler extends AbstractCollectionFeatureHandler<ByteVectorValue> {

        /**
         * @param caster
         */
        public ByteVectorFeatureHandler(final Caster<ByteVectorValue> caster) {
            super(caster);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public DataCell createReplaced() {
            final BVFactory factory = getFactory();
            final Iterator<Integer> iter = getReplacedIterator();
            while (iter.hasNext()) {
                final int idx = iter.next().intValue();
                factory.setValue(idx, m_sampled.get(idx));
            }
            return factory.createDataCell();
        }

        private BVFactory getFactory() {
            if (m_original instanceof DenseByteVectorCell) {
                return new DenseBVFactory((DenseByteVectorCell)m_original);
            } else {
                return new DefaultBVFactory(m_original);
            }
        }

    }

    /**
     * Introduced as a common interface to different types of byte vector factories since
     * they don't have a common interface in the KNIME core.
     *
     * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
     */
    private interface BVFactory {
        DataCell createDataCell();

        void setValue(final int idx, final int value);
    }

    private static final class DenseBVFactory implements BVFactory {

        private final DenseByteVectorCellFactory m_factory;

        public DenseBVFactory(final DenseByteVectorCell bvCell) {
            m_factory = new DenseByteVectorCellFactory(bvCell, 0, (int)bvCell.length());
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
        public void setValue(final int idx, final int value) {
            m_factory.setValue(idx, value);
        }

    }

    private static final class DefaultBVFactory implements BVFactory {

        private final DenseByteVectorCellFactory m_factory;

        public DefaultBVFactory(final ByteVectorValue bv) {
            m_factory = new DenseByteVectorCellFactory((int)bv.length());
            for (long i = bv.nextCountIndex(0); i >= 0; i = bv.nextCountIndex(i + 1)) {
                m_factory.setValue((int)i, bv.get(i));
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
        public void setValue(final int idx, final int value) {
            m_factory.setValue(idx, value);
        }

    }

}
