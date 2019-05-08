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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.knime.base.node.meta.explain.util.iter.DoubleIterator;
import org.knime.core.data.DataCell;
import org.knime.core.data.def.DoubleCell;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.google.common.collect.Lists;

/**
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
@RunWith(MockitoJUnitRunner.class)
public class NumericVectorFeatureGroupTest {
    private static final Normalizer NORMALIZER = d -> d;

    private static final Function<double[], DataCell> TO_INVERSE = ds -> new DoubleCell(ds[0]);

    private static final Function<double[], Iterable<DataCell>> TO_DATA =
        ds -> Collections.singleton(new DoubleCell(ds[0]));

    private List<MockNumericSampler> m_samplers =
        Lists.newArrayList(new MockNumericSampler(), new MockNumericSampler());

    private MockVectorHandler m_vectorHandler = new MockVectorHandler();

    private List<Normalizer> m_normalizers = Lists.newArrayList(NORMALIZER, NORMALIZER);

    @Mock
    private DataCell m_cell;

    private NumericVectorFeatureGroup m_testInstance =
        new NumericVectorFeatureGroup(m_vectorHandler, m_samplers, m_normalizers, TO_INVERSE, TO_DATA);

    @Test
    public void testCreateForOriginal() throws Exception {
        final double[] expectedValues = new double[]{1.0, 2.0};
        m_vectorHandler.m_values = expectedValues;
        LimeCellSample sample = m_testInstance.createForOriginal(new DoubleCell(1.0));
        checkSample(expectedValues, sample);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDifferentNumberOfSamplersAndNormalizers() throws Exception {
        new NumericVectorFeatureGroup(m_vectorHandler, m_samplers, Lists.newArrayList(NORMALIZER),
            TO_INVERSE, TO_DATA);
    }

    @Test
    public void testSample() throws Exception {
        final double[] expectedValues = new double[]{1.0, 2.0};
        m_vectorHandler.m_values = expectedValues;
        m_samplers.get(0).m_sample = expectedValues[0];
        m_samplers.get(1).m_sample = expectedValues[1];
        LimeCellSample sample = m_testInstance.sample(m_cell);
        checkSample(expectedValues, sample);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSampleWrongLength() throws Exception {
        m_vectorHandler.m_values = new double[4];
        m_testInstance.sample(m_cell);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateForOriginalWrongLength() throws Exception {
        m_vectorHandler.m_values = new double[4];
        m_testInstance.sample(m_cell);
    }

    /**
     * @param expectedValues
     * @param sample
     */
    private static void checkSample(final double[] expectedValues, final LimeCellSample sample) {
        assertTrue(sample instanceof VectorLimeCellSample);
        DoubleIterator di = sample.iterator();
        checkDoubleIterator(expectedValues, di);
        DoubleCell expectedCell = new DoubleCell(expectedValues[0]);
        checkInverse(expectedCell, sample);
        checkData(sample, expectedCell);
    }

    /**
     * @param sample
     * @param expectedCell
     */
    private static void checkData(final LimeCellSample sample, final DoubleCell expectedCell) {
        Iterable<DataCell> dataIterable = sample.getData();
        Iterator<DataCell> dataIter = dataIterable.iterator();
        assertTrue(dataIter.hasNext());
        assertEquals(expectedCell, dataIter.next());
        assertFalse(dataIter.hasNext());
    }

    /**
     * @param expectedValues
     * @param sample
     */
    private static void checkInverse(final DoubleCell expectedCell, final LimeCellSample sample) {
        assertEquals(expectedCell, sample.getInverse());
    }

    /**
     * @param expectedValues
     * @param di
     */
    private static void checkDoubleIterator(final double[] expectedValues, final DoubleIterator di) {
        for (int i = 0; i < expectedValues.length; i++) {
            assertTrue(di.hasNext());
            assertEquals(expectedValues[i], di.next(), 1e-5);
        }
        assertFalse(di.hasNext());
    }

    private static class MockNumericSampler implements NumericSampler {

        private double m_sample = 0.0;

        /**
         * {@inheritDoc}
         */
        @Override
        public double sample(final double originalValue) {
            return m_sample;
        }
    }

    private static class MockVectorHandler implements VectorHandler {
        private double[] m_values = new double[0];

        /**
         * {@inheritDoc}
         */
        @Override
        public int getLength(final DataCell vector) {
            return m_values.length;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public double getValue(final DataCell vector, final int idx) {
            return m_values[idx];
        }

    }

}
