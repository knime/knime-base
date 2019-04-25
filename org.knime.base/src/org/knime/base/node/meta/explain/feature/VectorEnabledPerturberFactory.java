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
 *   Apr 8, 2019 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.base.node.meta.explain.feature;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.knime.base.node.meta.explain.util.RowSampler;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.MissingValueException;

/**
 * Supports the perturbation of collections and vectors via {@link PerturberFactory} objects. Use only if collections
 * are contained, otherwise the SimpleReplacingPerturberFactory is more efficient.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
public final class VectorEnabledPerturberFactory implements PerturberFactory<DataRow, Set<Integer>, DataCell[]> {

    private final RowSampler m_rowSampler;

    private final List<FeatureHandlerFactory> m_factories;

    private final int m_numFeatures;

    private final int[] m_featuresPerHandler;

    /**
     * @param rowSampler
     * @param factories
     * @param featuresPerHandler
     */
    public VectorEnabledPerturberFactory(final RowSampler rowSampler, final List<FeatureHandlerFactory> factories,
        final int[] featuresPerHandler) {
        m_rowSampler = rowSampler;
        m_factories = factories;
        m_numFeatures = Arrays.stream(featuresPerHandler).sum();
        m_featuresPerHandler = featuresPerHandler;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Perturber<DataRow, Set<Integer>, DataCell[]> createPerturber() {
        final DataRow replacementRow = m_rowSampler.sampleRow();
        final List<FeatureHandler> handlers = createHandlers();
        return new VectorEnabledPerturber(replacementRow, handlers, m_featuresPerHandler, m_numFeatures);
    }

    private List<FeatureHandler> createHandlers() {
        return m_factories.stream().map(FeatureHandlerFactory::createFeatureHandler).collect(Collectors.toList());
    }

    private static class VectorEnabledPerturber implements Perturber<DataRow, Set<Integer>, DataCell[]> {

        private final DataRow m_replacementSource;

        private final List<FeatureHandler> m_handlers;

        private final int[] m_featureIdx2Handler;

        private final int[] m_featureOffsets;

        private final int m_numFeatures;

        public VectorEnabledPerturber(final DataRow replacementSource, final List<FeatureHandler> handlers,
            final int[] featuresPerHandler, final int totalNumFeatures) {
            m_replacementSource = replacementSource;
            m_handlers = handlers;
            m_numFeatures = totalNumFeatures;
            setSampled();
            m_featureOffsets = calculateFeatureOffsets(featuresPerHandler);
            m_featureIdx2Handler = calculateFeatureIdxToHandler(m_featureOffsets);
        }

        private void setSampled() {
            try {
                for (int i = 0; i < m_replacementSource.getNumCells(); i++) {
                    m_handlers.get(i).setSampled(m_replacementSource.getCell(i));
                }
            } catch (MissingValueException mve) {
                throw new IllegalArgumentException(
                    "Missing value in row " + m_replacementSource.getKey() + " detected.");
            }
        }

        private void setOriginal(final DataRow original) {
            assert original.getNumCells() == m_handlers.size();
            try {
                for (int i = 0; i < original.getNumCells(); i++) {
                    m_handlers.get(i).setOriginal(original.getCell(i));
                }
            } catch (MissingValueException mve) {
                throw new IllegalArgumentException("Missing value in row " + original.getKey() + " detected.");
            }
        }

        /**
         * @param featuresPerHandler
         */
        private static int[] calculateFeatureOffsets(final int[] featuresPerHandler) {
            final int[] featureOffsets = new int[featuresPerHandler.length];
            int currentFeatureIdx = 0;
            for (int i = 0; i < featureOffsets.length; i++) {
                featureOffsets[i] = currentFeatureIdx;
                currentFeatureIdx += featuresPerHandler[i];
            }
            return featureOffsets;
        }

        private int[] calculateFeatureIdxToHandler(final int[] featureOffsets) {
            final int[] featureIdxToHandler = new int[m_numFeatures];
            int currentFeature = 0;
            for (int i = 0; i < featureOffsets.length - 1; i++) {
                final int offset = featureOffsets[i];
                final int nextOffset = featureOffsets[i + 1];
                for (int j = offset; j < nextOffset; j++) {
                    featureIdxToHandler[currentFeature] = i;
                    currentFeature++;
                }
            }
            final int lastHandler = featureOffsets.length - 1;
            for (int i = currentFeature; i < m_numFeatures; i++) {
                featureIdxToHandler[i] = lastHandler;
            }
            return featureIdxToHandler;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public DataCell[] perturb(final DataRow perturbee, final Set<Integer> config) {
            setOriginal(perturbee);
            markForReplacement(config);
            return createCells();
        }

        private int getNumCells() {
            return m_handlers.size();
        }

        private DataCell[] createCells() {
            final DataCell[] cells = new DataCell[getNumCells()];
            for (int i = 0; i < cells.length; i++) {
                final FeatureHandler featureHandler = m_handlers.get(i);
                cells[i] = featureHandler.createReplaced();
                featureHandler.resetReplaceState();
            }
            return cells;
        }

        /**
         * @param config
         */
        private void markForReplacement(final Set<Integer> config) {
            for (Integer featureToPerturb : config) {
                final int globalFeatureIdx = featureToPerturb.intValue();
                final int handlerIdx = m_featureIdx2Handler[globalFeatureIdx];
                final FeatureHandler handler = m_handlers.get(handlerIdx);
                final int offset = m_featureOffsets[handlerIdx];
                final int localFeatureIdx = globalFeatureIdx - offset;
                handler.markForReplacement(localFeatureIdx);
            }
        }

    }

}
