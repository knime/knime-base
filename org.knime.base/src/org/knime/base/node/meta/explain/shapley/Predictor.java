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
 *   Apr 5, 2019 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.base.node.meta.explain.shapley;

import java.util.Iterator;
import java.util.NoSuchElementException;

import org.knime.base.node.meta.explain.DefaultExplanation.DefaultExplanationBuilder;
import org.knime.base.node.meta.explain.Explanation;
import org.knime.base.node.meta.explain.shapley.ShapleyValuesKeys.SVKeyParser;
import org.knime.base.node.meta.explain.util.PeekingIterator;
import org.knime.core.data.DataRow;
import org.knime.core.data.RowIterator;
import org.knime.core.data.RowKey;
import org.knime.core.node.util.CheckUtils;

final class Predictor implements Iterator<Explanation> {

    private final ShapleyValues m_algorithm;

    private final PeekingIterator<DataRow> m_rowIterator;

    private final SVKeyParser m_keyParser = ShapleyValuesKeys.createParser();

    private final int m_numFeatures;

    private final int m_numTargets;

    private int m_currentFoi;

    private String m_currentKey;

    public Predictor(final RowIterator rowIterator, final ShapleyValues algorithm, final int numFeatures,
        final int numTargets) {
        m_algorithm = algorithm;
        m_rowIterator = new PeekingIterator<>(rowIterator);
        m_numFeatures = numFeatures;
        m_numTargets = numTargets;
    }

    private boolean nextIsSameFoi() {
        if (!m_rowIterator.hasNext()) {
            return false;
        }
        m_keyParser.accept(peekKey());
        return m_currentFoi == m_keyParser.getFoi();
    }

    private RowKey peekKey() {
        final DataRow peek = m_rowIterator.peek();
        return peek.getKey();
    }

    private Iterator<PredictionVector> createPerFoiIterator() {
        return new Iterator<PredictionVector>() {

            @Override
            public boolean hasNext() {
                return nextIsSameFoi();
            }

            @Override
            public PredictionVector next() {
                return new KnimePredictionVector(m_rowIterator.next());
            }
        };
    }

    private boolean nextIsSameRow() {
        if (!m_rowIterator.hasNext()) {
            return false;
        }
        m_keyParser.accept(peekKey());
        return m_currentKey.equals(m_keyParser.getOriginalKey());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasNext() {
        return m_rowIterator.hasNext();
    }

    @Override
    public Explanation next() {
        if (!hasNext()) {
            throw new NoSuchElementException("There are no more rows to explain.");
        }
        final double[] cellValues = new double[m_numFeatures * m_numTargets];
        int foi = 0;
        m_keyParser.accept(peekKey());
        m_currentKey = m_keyParser.getOriginalKey();
        final DefaultExplanationBuilder explanationBuilder =
            new DefaultExplanationBuilder(m_currentKey, m_numTargets, m_numFeatures);
        while (nextIsSameRow()) {
            m_keyParser.accept(peekKey());
            m_currentFoi = m_keyParser.getFoi();
            CheckUtils.checkState(foi == m_currentFoi, "The order of the prediction table is wrong."
                + "Expected to be at foi %s but instead we are at foi %s.", foi, m_currentFoi);
            final Iterator<PredictionVector> foiIterator = createPerFoiIterator();
            final double[] svPerTarget = m_algorithm.consumePredictionsPerFoi(foiIterator);
            for (int i = 0; i < m_numTargets; i++) {
                explanationBuilder.setExplanationValue(i, foi, svPerTarget[i]);
                cellValues[foi + i * m_numFeatures] = svPerTarget[i];
            }
            foi++;
        }

        return explanationBuilder.build();

    }

}