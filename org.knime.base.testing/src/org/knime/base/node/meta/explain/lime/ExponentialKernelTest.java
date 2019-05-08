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

import static org.junit.Assert.*;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.knime.base.node.meta.explain.util.iter.DoubleIterable;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
@RunWith(MockitoJUnitRunner.class)
public class ExponentialKernelTest {

    MockDistance m_dist = new MockDistance();

    @Mock
    DoubleIterable m_iter1;

    @Mock
    DoubleIterable m_iter2;

    @Test
    public void testCalculate() throws Exception {
        ExponentialKernel kernel1 = new ExponentialKernel(m_dist, 1.0);
        ExponentialKernel kernel2 = new ExponentialKernel(m_dist, 2.0);
        // calculated with Python: np.sqrt(np.exp(-(d**2)/w**2))
        m_dist.m_d = 0.0;
        assertEquals(1.0, kernel1.calculate(m_iter1, m_iter2), 1e-5);
        m_dist.m_d = 1.0;
        assertEquals(0.6065307, kernel1.calculate(m_iter1, m_iter2), 1e-5);
        m_dist.m_d = 2.0;
        assertEquals(0.6065307, kernel2.calculate(m_iter1, m_iter2), 1e-5);
        m_dist.m_d = 3.0;
        assertEquals(0.3246525, kernel2.calculate(m_iter1, m_iter2), 1e-5);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testNonPositiveWidth() throws Exception {
        new ExponentialKernel(m_dist, 0);
    }

    private static class MockDistance implements Distance {
        private double m_d = 0.0;

        /**
         * {@inheritDoc}
         */
        @Override
        public double calculate(final DoubleIterable a, final DoubleIterable b) {
            return m_d;
        }


    }
}
