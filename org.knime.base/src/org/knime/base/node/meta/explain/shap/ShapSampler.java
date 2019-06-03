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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import org.apache.commons.math3.random.RandomDataGenerator;
import org.apache.commons.math3.util.CombinatoricsUtils;
import org.knime.base.node.meta.explain.util.RandomDataGeneratorFactory;
import org.knime.core.data.DataRow;

/**
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
final class ShapSampler {

    private final SubsetReplacer m_subsetReplacer;

    private final int m_numSubsetSamples;

    private final int m_numFeatures;

    private final MaskFactory m_maskFactory;

    private final RandomDataGeneratorFactory m_rdgFactory;

    ShapSampler(final SubsetReplacer subsetReplacer, final MaskFactory maskFactory, final int numSubSetSamples,
        final int numFeatures, final RandomDataGeneratorFactory rdgFactory) {
        m_numSubsetSamples = numSubSetSamples;
        m_numFeatures = numFeatures;
        m_subsetReplacer = subsetReplacer;
        m_maskFactory = maskFactory;
        m_rdgFactory = rdgFactory;
    }

    Iterator<ShapSample> createSamples(final DataRow roi) {
        final List<ShapSample> samples = new ArrayList<>();
        final WeightVector weightVector = new WeightVector(m_numFeatures);
        final SubsetEnumerator subsetEnumerator = new SubsetEnumerator(roi, weightVector);
        subsetEnumerator.enumerateSubsets(samples::add);
        if (weightVector.getNumSubsetSizes() > subsetEnumerator.getNumFullSubsetSizes()) {
            final ShapSubsetSampler subsetSampler = new ShapSubsetSampler(roi, weightVector,
                subsetEnumerator.getNumFullSubsetSizes(), m_rdgFactory.create());
            subsetSampler.sampleSubsets(samples::add);
        }

        return samples.iterator();
    }

    private final class ShapSubsetSampler {
        private final DataRow m_roi;

        private final SubsetSampler m_subsetSampler;

        private final double m_weightLeft;

        private final WeightVector m_weightVector;

        private int m_samplesLeft;

        ShapSubsetSampler(final DataRow roi, final WeightVector weightVector, final int numFixedSamples,
            final RandomDataGenerator rdg) {
            m_samplesLeft = m_numSubsetSamples - numFixedSamples;
            m_roi = roi;
            final int[] leftSubsetSizes = increasingArray(numFixedSamples + 1, weightVector.getNumSubsetSizes());
            m_weightLeft = weightVector.getWeightLeft(numFixedSamples);
            m_subsetSampler = new SubsetSampler(rdg, leftSubsetSizes, weightVector.getTailDistribution(numFixedSamples),
                m_numFeatures);
            m_weightVector = weightVector;
        }

        void sampleSubsets(final Consumer<ShapSample> sink) {
            final Map<Mask, ShapSample> usedMasks = new HashMap<>();
            double weightSum = 0.0;
            while (m_samplesLeft > 0) {
                final int[] subset = m_subsetSampler.sampleSubset();
                final Mask mask = m_maskFactory.createMask(subset);
                ShapSample sample = usedMasks.get(mask);
                weightSum++;
                boolean newSample = false;
                if (sample == null) {
                    newSample = true;
                    sample = new ShapSample(mask, 1.0, m_subsetReplacer.replace(m_roi, mask));
                    usedMasks.put(mask, sample);
                    m_samplesLeft--;
                    sink.accept(sample);
                } else {
                    sample.setWeight(sample.getWeight() + 1.0);
                }
                if (m_samplesLeft > 0 && m_weightVector.isPairedSubsetSize(subset.length)) {
                    weightSum++;
                    final ShapSample finalSample = sample;
                    Optional<ShapSample> optionalComplement = sample.getComplement();
                    final ShapSample complement = optionalComplement.orElseGet(() -> createComplement(finalSample));
                    complement.setWeight(complement.getWeight() + 1.0);
                    if (newSample) {
                        m_samplesLeft--;
                        sink.accept(complement);
                    }
                }
            }
            if (weightSum > 0) {
                normalizeWeights(usedMasks.values(), weightSum);
            }
        }

        private ShapSample createComplement(final ShapSample sample) {
            final Mask mask = sample.getMask();
            final Mask complementMask = mask.getComplement();
            final ShapSample complement =
                new ShapSample(complementMask, 0.0, m_subsetReplacer.replace(m_roi, complementMask));
            sample.setComplement(complement);
            return complement;
        }

        private void normalizeWeights(final Iterable<ShapSample> samples, final double weightSum) {
            final double scaler = m_weightLeft / weightSum;
            for (ShapSample sample : samples) {
                sample.setWeight(scaler * sample.getWeight());
                final Optional<ShapSample> optComplement = sample.getComplement();
                if (optComplement.isPresent()) {
                    final ShapSample complement = optComplement.get();
                    complement.setWeight(scaler * complement.getWeight());
                }
            }
        }
    }

    private static int[] increasingArray(final int from, final int to) {
        final int[] array = new int[to - from + 1];
        for (int i = 0; i < array.length; i++) {
            array[i] = i + from;
        }
        return array;
    }

    private final class SubsetEnumerator {
        private final DataRow m_roi;

        private final WeightVector m_weightVector;

        private int m_numFullSubsetSizes = 0;

        private int m_numSamplesLeft = m_numSubsetSamples;

        SubsetEnumerator(final DataRow roi, final WeightVector weightVector) {
            m_roi = roi;
            m_weightVector = weightVector;
        }

        void enumerateSubsets(final Consumer<ShapSample> sink) {
            for (int subsetSize = 1; subsetSize <= m_weightVector.getNumSubsetSizes(); subsetSize++) {
                final long binom = CombinatoricsUtils.binomialCoefficient(m_numFeatures, subsetSize);
                final long numSubsets = m_weightVector.isPairedSubsetSize(subsetSize) ? binom * 2 : binom;
                if (m_numSamplesLeft * m_weightVector.getScaled(subsetSize) / numSubsets >= 1.0 - 1e-8) {
                    m_numFullSubsetSizes++;
                    m_numSamplesLeft -= numSubsets;
                    if (m_weightVector.getScaled(subsetSize) < 1.0) {
                        m_weightVector.rescale(1.0 / (1.0 - m_weightVector.getScaled(subsetSize)));
                    }
                    final double weight = m_weightVector.get(subsetSize) / binom;
                    addAllSubsets(sink, subsetSize, weight);
                } else {
                    break;
                }
            }
        }

        private void addAllSubsets(final Consumer<ShapSample> sink, final int subsetSize, final double weight) {
            final Iterator<Mask> masks = m_maskFactory.allMasks(subsetSize);
            while (masks.hasNext()) {
                final Mask mask = masks.next();
                final ShapSample sample = new ShapSample(mask, weight, m_subsetReplacer.replace(m_roi, mask));
                sink.accept(sample);
                if (m_weightVector.isPairedSubsetSize(subsetSize)) {
                    final Mask complementMask = mask.getComplement();
                    final ShapSample complement =
                        new ShapSample(complementMask, weight, m_subsetReplacer.replace(m_roi, complementMask));
                    sink.accept(complement);
                }
            }
        }

        int getNumFullSubsetSizes() {
            return m_numFullSubsetSizes;
        }
    }

}
