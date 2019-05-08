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
 *   Apr 30, 2019 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.base.node.meta.explain.lime;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Function;

import org.knime.base.node.meta.explain.util.iter.DoubleIterable;
import org.knime.base.node.meta.explain.util.iter.IterableUtils;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;

import com.google.common.collect.Iterables;

/**
 * Performs LIME-style sampling.
 * See https://github.com/marcotcr/lime/blob/master/lime/lime_tabular.py for the details
 * of the algorithm.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
final class LimeSampler {

    private final Iterable<FeatureGroup> m_featureGroups;

    private final Kernel m_kernel;

    private final int m_numSamples;

    /**
     * @param featureGroups the featureGroups corresponding to feature columns
     * @param kernel the kernel function that is used to calculate the weight of LimeSamples
     * @param numSamples the number of samples to draw for each row of interest
     *
     */
    public LimeSampler(final Iterable<FeatureGroup> featureGroups, final Kernel kernel, final int numSamples) {
        m_featureGroups = featureGroups;
        m_kernel = kernel;
        // the first sample will be the original row
        m_numSamples = numSamples - 1;
    }

    /**
     * Creates an iterator of LimeSamples for the {@link DataRow roi}.
     * @param roi the {@link DataRow} of interest
     * @return an iterator of LimeSample
     */
    public Iterator<LimeSample> createSampleSet(final DataRow roi) {
        return new LimeSampleIterator(roi);
    }

    private class LimeSampleIterator implements Iterator<LimeSample> {

        private final Iterable<LimeCellSample> m_originalCells;
        private final DoubleIterable m_originalNormalized;
        private final DataRow m_roi;

        LimeSampleIterator(final DataRow roi) {
            m_originalCells = roiAsSample(roi);
            m_originalNormalized = concatenate(m_originalCells);
            m_roi = roi;
        }

        private int m_idx = -1;


        /**
         * {@inheritDoc}
         */
        @Override
        public boolean hasNext() {
            return m_idx < m_numSamples;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public LimeSample next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            m_idx++;
            if (m_idx == 0) {
                return new LimeSample(m_originalCells, m_kernel.calculate(m_originalNormalized, m_originalNormalized));
            }
            final Iterable<LimeCellSample> cellSamples = drawSample(m_roi);
            final DoubleIterable normalizedSample = concatenate(cellSamples);
            final double weight = m_kernel.calculate(m_originalNormalized, normalizedSample);
            return new LimeSample(cellSamples, weight);
        }

        private Iterable<LimeCellSample> drawSample(final DataRow reference) {
            final List<LimeCellSample> cellSamples = new ArrayList<>();
            final Iterator<FeatureGroup> featureGroupIterator = m_featureGroups.iterator();
            final Iterator<DataCell> referenceIterator = reference.iterator();
            while (referenceIterator.hasNext()) {
                assert featureGroupIterator.hasNext();
                final DataCell referenceCell = referenceIterator.next();
                final FeatureGroup featureGroup = featureGroupIterator.next();
                final LimeCellSample sample = featureGroup.sample(referenceCell);
                cellSamples.add(sample);
            }
            assert !featureGroupIterator.hasNext();
            return cellSamples;
        }

        private Iterable<LimeCellSample> roiAsSample(final DataRow roi) {
            final Iterable<Function<DataCell, LimeCellSample>> mappingIterable =
                Iterables.transform(m_featureGroups, LimeSampler::getMappingFromFeatureGroup);
            return IterableUtils.mappingIterable(roi, mappingIterable);
        }

    }

    /*
     * Needed to satisfy the compiler.
     */
    private static DoubleIterable concatenate(final Iterable<LimeCellSample> cellSamples) {
        final Iterable<? extends DoubleIterable> normalizedCells = cellSamples;
        return IterableUtils.concatenatedDoubleIterable(normalizedCells);
    }

    /*
     * Needed to satisfy the compiler.
     */
    private static Function<DataCell, LimeCellSample> getMappingFromFeatureGroup(final FeatureGroup fg) {
        return fg::createForOriginal;
    }



}
