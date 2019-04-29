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
 *   Apr 29, 2019 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.base.node.meta.explain.feature;

import static org.junit.Assert.assertArrayEquals;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.knime.base.node.meta.explain.util.RowSampler;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.MissingCell;
import org.knime.core.data.MissingValue;
import org.knime.core.data.MissingValueException;
import org.knime.core.data.RowKey;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
@RunWith(MockitoJUnitRunner.class)
public class VectorEnabledPerturberFactoryTest {

    @Mock
    private RowSampler m_rowSampler;

    @Mock
    private DataRow m_samplingRow;

    @Mock
    private DataRow m_perturbee;

    @Mock
    private DataCell m_samplingCell;

    @Mock
    private DataCell m_originalCell;

    private MockFeatureHandlerFactory m_featureHandlerFactory1;

    private MockFeatureHandlerFactory m_featureHandlerFactory2;

    private MockFeatureHandler m_featureHandler1;

    private MockFeatureHandler m_featureHandler2;

    private final int[] m_featuresPerHandler = new int[]{1, 2};

    private PerturberFactory<DataRow, Set<Integer>, DataCell[]> m_perturberFactory;

    @Before
    public void init() {
        Mockito.when(m_rowSampler.sampleRow()).thenReturn(m_samplingRow);
        Mockito.when(m_samplingRow.getCell(ArgumentMatchers.anyInt())).thenReturn(m_samplingCell);
        Mockito.when(m_perturbee.getCell(ArgumentMatchers.anyInt())).thenReturn(m_originalCell);
        Mockito.when(m_samplingRow.getNumCells()).thenReturn(2);
        Mockito.when(m_perturbee.getNumCells()).thenReturn(2);

        m_featureHandler1 = new MockFeatureHandler(false);
        m_featureHandler2 = new MockFeatureHandler(true);

        m_featureHandlerFactory1 = new MockFeatureHandlerFactory(true, 1, m_featureHandler1);
        m_featureHandlerFactory2 = new MockFeatureHandlerFactory(false, 2, m_featureHandler2);

        m_perturberFactory = new VectorEnabledPerturberFactory(m_rowSampler,
            Lists.newArrayList(m_featureHandlerFactory1, m_featureHandlerFactory2), m_featuresPerHandler);
    }

    @Test
    public void testPerturb() throws Exception {
        final Perturber<DataRow, Set<Integer>, DataCell[]> perturber = m_perturberFactory.createPerturber();
        DataCell[] expected = new DataCell[]{m_originalCell, m_samplingCell};
        DataCell[] result = perturber.perturb(m_perturbee, Sets.newHashSet(1));
        assertArrayEquals(expected, result);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testUnsupportedMissingValueInSampledRow() throws Exception {
        Mockito.when(m_samplingRow.getCell(ArgumentMatchers.anyInt())).thenReturn(new MissingCell("missing"));
        Mockito.when(m_samplingRow.getKey()).thenReturn(new RowKey("test"));
        m_perturberFactory.createPerturber();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testUnsupportedMissingValueInOriginalRow() throws Exception {
        Mockito.when(m_perturbee.getCell(ArgumentMatchers.anyInt())).thenReturn(new MissingCell("missing"));
        Mockito.when(m_perturbee.getKey()).thenReturn(new RowKey("test"));
        final Perturber<DataRow, Set<Integer>, DataCell[]> perturber = m_perturberFactory.createPerturber();
        perturber.perturb(m_perturbee, Sets.newHashSet(1));
    }

    private static class MockFeatureHandlerFactory implements FeatureHandlerFactory {

        private boolean m_supportMissingValues;

        private int m_numFeatures;

        private MockFeatureHandler m_handler;

        MockFeatureHandlerFactory(final boolean supportMissingValues, final int numFeatures,
            final MockFeatureHandler handler) {
            m_supportMissingValues = supportMissingValues;
            m_numFeatures = numFeatures;
            m_handler = handler;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public FeatureHandler createFeatureHandler() {
            return m_handler;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int numFeatures(final DataCell cell) {
            return m_numFeatures;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public List<String> getFeatureNames(final DataColumnSpec columnSpec) {
            return IntStream.range(0, m_numFeatures).mapToObj(i -> "feature" + i).collect(Collectors.toList());
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean handlesCollections() {
            return m_numFeatures > 1;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean supportsMissingValues() {
            return m_supportMissingValues;
        }

    }

    private static class MockFeatureHandler implements FeatureHandler {

        private DataCell m_original;

        private DataCell m_sampled;

        private final Set<Integer> m_markedForReplacement = new HashSet<>();

        private final boolean m_replace;

        MockFeatureHandler(final boolean replace) {
            m_replace = replace;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void setOriginal(final DataCell cell) {
            if (cell.isMissing()) {
                throw new MissingValueException((MissingValue)cell);
            }
            m_original = cell;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void setSampled(final DataCell cell) {
            if (cell.isMissing()) {
                throw new MissingValueException((MissingValue)cell);
            }
            m_sampled = cell;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void markForReplacement(final int idx) {
            m_markedForReplacement.add(idx);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void reset() {
            resetReplaceState();
            m_original = null;
            m_sampled = null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void resetReplaceState() {
            m_markedForReplacement.clear();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public DataCell createReplaced() {
            return m_replace ? m_sampled : m_original;
        }

    }
}
