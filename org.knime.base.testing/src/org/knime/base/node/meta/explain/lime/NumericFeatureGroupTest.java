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

import java.util.Iterator;

import org.junit.Test;
import org.knime.base.node.meta.explain.util.iter.DoubleIterator;
import org.knime.core.data.DataCell;
import org.knime.core.data.def.DoubleCell;

/**
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
public class NumericFeatureGroupTest {

    private MockNumericSampler m_sampler = new MockNumericSampler();

    private NumericFeatureGroup m_testInstance =
        new NumericFeatureGroup(m_sampler, d -> d, DoubleCell::new, DoubleCell::new);

    private static final DoubleCell CELL = new DoubleCell(5.0);

    @Test
    public void testCreateForOriginal() throws Exception {
        final LimeCellSample sample = m_testInstance.createForOriginal(CELL);
        checkSample(sample, 5.0);
    }

    /**
     * @param sample
     */
    private void checkSample(final LimeCellSample sample, final double expectedValue) {
        final DoubleCell expectedCell = new DoubleCell(expectedValue);
        assertTrue(sample instanceof SingleLimeCellSample);
        DoubleIterator iter = sample.iterator();
        assertTrue(iter.hasNext());
        assertEquals(expectedValue, iter.next(), 1e-5);
        assertFalse(iter.hasNext());
        Iterable<DataCell> data = sample.getData();
        Iterator<DataCell> dataIterator = data.iterator();
        assertTrue(dataIterator.hasNext());
        assertEquals(expectedCell, dataIterator.next());
        assertFalse(dataIterator.hasNext());
        assertEquals(expectedCell, sample.getInverse());
    }

    @Test
    public void testSample() throws Exception {
        m_sampler.m_sample = 3.0;
        LimeCellSample sample = m_testInstance.sample(CELL);
        checkSample(sample, 3.0);
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

}
