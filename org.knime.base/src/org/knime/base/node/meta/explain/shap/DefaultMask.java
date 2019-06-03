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
 *   May 9, 2019 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.base.node.meta.explain.shap;

import java.util.NoSuchElementException;

import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.math3.util.CombinatoricsUtils;
import org.knime.base.node.meta.explain.util.iter.IntIterator;
import org.knime.base.node.meta.explain.util.iter.IteratorUtils;

/**
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
final class DefaultMask implements Mask {

    private final int[] m_indices;

    private final int m_numFeatures;

    private ComplementMask m_complement = null;

    private int m_hashCode = 0;

    private boolean m_firstCallToHashCode = true;

    /**
     * The constructor is private because we require that <b>indices</b> are ordered,
     * which is ensured by the contract of {@link CombinatoricsUtils#combinationsIterator(int, int)}.
     * @param indices the ordered indices
     * @param numFeatures the total number of features
     */
    DefaultMask(final int[] indices, final int numFeatures) {
        m_numFeatures = numFeatures;
        m_indices = indices;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        if (m_firstCallToHashCode) {
            m_firstCallToHashCode = false;
            HashCodeBuilder hashCodeBuilder = new HashCodeBuilder();
            hashCodeBuilder.append(m_numFeatures);
            hashCodeBuilder.append(m_indices);
            m_hashCode = hashCodeBuilder.toHashCode();
        }
        return m_hashCode;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(final Object obj) {

        if (obj == this) {
            return true;
        }

        if (obj instanceof DefaultMask) {
            final DefaultMask other = (DefaultMask)obj;
            if (other.m_numFeatures != m_numFeatures || other.m_indices.length != this.m_indices.length) {
                return false;
            }
            for (int i = 0; i < m_indices.length; i++) {
                if (m_indices[i] != other.m_indices[i]) {
                    return false;
                }
            }
            return true;
        }

        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return toBitString(iterator(), m_numFeatures);
    }

    private static String toBitString(final IntIterator iterator, final int numFeatures) {
        final StringBuilder sb = new StringBuilder();
        int currentIdx = iterator.hasNext() ? iterator.next() : -1;
        for (int i = 0; i < numFeatures; i++) {
            if (i == currentIdx) {
                sb.append("1");
                currentIdx = iterator.hasNext() ? iterator.next() : -1;
            } else {
                sb.append("0");
            }
        }
        return sb.toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IntIterator iterator() {
        return IteratorUtils.arrayIntIterator(m_indices);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Mask getComplement() {
        if (m_complement == null) {
            m_complement = new ComplementMask();
        }
        return m_complement;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public int getNumberOfFeatures() {
        return m_numFeatures;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getCardinality() {
        return m_indices.length;
    }

    private class ComplementMask implements Mask {

        private int m_complementHashCode = 0;

        private boolean m_complementFirstCallToHashCode = true;

        /**
         * {@inheritDoc}
         */
        @Override
        public IntIterator iterator() {
            return new ComplementIterator();
        }


        /**
         * {@inheritDoc}
         */
        @Override
        public Mask getComplement() {
            return DefaultMask.this;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            return toBitString(this.iterator(), m_numFeatures);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean equals(final Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj instanceof ComplementMask) {
                final ComplementMask other = (ComplementMask)obj;
                return this.getComplement().equals(other.getComplement());
            }
            return false;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int hashCode() {
            if (this.m_complementFirstCallToHashCode) {
                this.m_complementFirstCallToHashCode = false;
                HashCodeBuilder hashCodeBuilder = new HashCodeBuilder();
                hashCodeBuilder.append(DefaultMask.this.hashCode());
                m_complementHashCode = hashCodeBuilder.toHashCode();
            }
            return this.m_complementHashCode;
        }


        /**
         * {@inheritDoc}
         */
        @Override
        public int getNumberOfFeatures() {
            return m_numFeatures;
        }


        /**
         * {@inheritDoc}
         */
        @Override
        public int getCardinality() {
            return m_numFeatures - m_indices.length;
        }

    }

    private class ComplementIterator implements IntIterator {


        private int m_notContainedIdx = -1;

        private int m_idx = -1;

        ComplementIterator() {
            goToNextNotContained();
        }


        /**
         * {@inheritDoc}
         */
        @Override
        public boolean hasNext() {
            return numIndicesLeft() > 0;
        }

        private int numIndicesLeft() {
            final int numNotContainedLeft = numNotContainedLeft();
            return m_numFeatures - numNotContainedLeft - m_idx - 1;
        }

        private int numNotContainedLeft() {
            if (m_idx < nextNotContained()) {
                return m_indices.length - m_notContainedIdx;
            } else {
                return 0;
            }
        }

        private int nextNotContained() {
            return m_indices[m_notContainedIdx];
        }

        private void jumpToNextContained() {
            m_idx++;
            while (m_idx == nextNotContained()) {
                goToNextNotContained();
                m_idx++;
            }
        }

        private void goToNextNotContained() {
            if (m_notContainedIdx < m_indices.length - 1) {
                m_notContainedIdx++;
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            jumpToNextContained();
            return m_idx;
        }

    }

}
