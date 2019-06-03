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
 *   May 21, 2019 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.base.node.meta.explain.shap;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

/**
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
public class WeightVectorTest {

    private WeightVector m_testInstance;

    private static final double[] EXPECTED = new double[]{8.0 / 11.0, 3.0 / 11.0};

    private static final double[] EXPECTED_TAIL_DISTRIBUTION = new double[] {4.0 / 7.0, 3.0 / 7.0};

    @Before
    public void init() {
        m_testInstance = new WeightVector(4);
    }

    @Test
    public void testScaling() throws Exception {
        assertEquals(EXPECTED[0], m_testInstance.getScaled(1), 1e-5);
        assertEquals(EXPECTED[1], m_testInstance.getScaled(2), 1e-5);
        m_testInstance.rescale(2.0);
        assertEquals(EXPECTED[0] * 2, m_testInstance.getScaled(1), 1e-5);
        assertEquals(EXPECTED[1] * 2, m_testInstance.getScaled(2), 1e-5);
        m_testInstance.resetScale();
        assertEquals(EXPECTED[0], m_testInstance.getScaled(1), 1e-5);
        assertEquals(EXPECTED[1], m_testInstance.getScaled(2), 1e-5);
    }

    @Test
    public void testGet() throws Exception {
        assertEquals(EXPECTED[0] / 2, m_testInstance.get(1), 1e-5);
        assertEquals(EXPECTED[1], m_testInstance.get(2), 1e-5);
    }

    @Test
    public void testGetWeightLeft() throws Exception {
        assertEquals(1.0, m_testInstance.getWeightLeft(0), 1e-5);
        assertEquals(EXPECTED[1], m_testInstance.getWeightLeft(1), 1e-5);
    }

    @Test
    public void testIsPairedSubsetSize() throws Exception {
        assertTrue(m_testInstance.isPairedSubsetSize(1));
        assertFalse(m_testInstance.isPairedSubsetSize(2));
    }

    @Test
    public void testGetNumSubsetSizes() throws Exception {
        assertEquals(2, m_testInstance.getNumSubsetSizes());
    }

    @Test
    public void testGetTailDistribution() throws Exception {
        assertArrayEquals(EXPECTED_TAIL_DISTRIBUTION, m_testInstance.getTailDistribution(0), 1e-5);
    }

}
