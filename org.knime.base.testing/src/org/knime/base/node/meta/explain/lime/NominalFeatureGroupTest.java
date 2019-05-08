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
import org.knime.core.data.def.StringCell;

/**
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
public class NominalFeatureGroupTest {

    private static final StringCell CELL = new StringCell("test");

    private MockNominalSampler m_sampler = new MockNominalSampler();

    private NominalFeatureGroup m_testInstance = new NominalFeatureGroup(m_sampler);

    @Test
    public void testCreateForOriginal() throws Exception {
        LimeCellSample sample = m_testInstance.createForOriginal(CELL);
        checkSample(sample, 1.0, CELL);
    }

    /**
     * @param sample
     */
    private static void checkSample(final LimeCellSample sample, final double expectedDi,
        final DataCell expectedInverse) {
        DoubleIterator di = sample.iterator();
        assertTrue(di.hasNext());
        assertEquals(expectedDi, di.next(), 1e-5);
        assertFalse(di.hasNext());

        assertEquals(expectedInverse, sample.getInverse());

        Iterator<DataCell> dataIter = sample.getData().iterator();
        assertTrue(dataIter.hasNext());
        assertEquals(new DoubleCell(expectedDi), dataIter.next());
        assertFalse(dataIter.hasNext());
    }

    @Test
    public void testSample() throws Exception {
        m_sampler.m_val = CELL;
        LimeCellSample sample = m_testInstance.sample(CELL);
        checkSample(sample, 1.0, CELL);
        StringCell differentCell = new StringCell("different");
        m_sampler.m_val = differentCell;
        sample = m_testInstance.sample(CELL);
        checkSample(sample, 0.0, differentCell);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testUnknownValue() throws Exception {
        m_sampler.m_isPossibleValue = false;
        m_testInstance.createForOriginal(CELL);
    }

    private static class MockNominalSampler implements NominalSampler {

        DataCell m_val = null;

        boolean m_isPossibleValue = true;

        /**
         * {@inheritDoc}
         */
        @Override
        public DataCell sample() {
            return m_val;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isPossibleValue(final DataCell cell) {
            return m_isPossibleValue;
        }

    }
}
