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
 *   31.03.2019 (Adrian): created
 */
package org.knime.base.node.mine.regression.glmnet.cycle;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
final class ActiveSetFeatureCycleStrategy implements FeatureCycle {

    private final Set<Integer> m_activeSet;

    private final int m_numFeatures;

    private boolean m_activeSetConverged = false;

    private boolean m_activeSetChanged = false;

    private boolean m_isFullCycle = true;

    private int m_featureIdx = -1;

    private Iterator<Integer> m_activeSetIterator;

    public ActiveSetFeatureCycleStrategy(final int numFeatures) {
        m_numFeatures = numFeatures;
        m_activeSet = new HashSet<>(numFeatures);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasNext() {
        if (m_isFullCycle) {
            return fullCycleHasNext();
        } else {
            return m_activeSetIterator.hasNext();
        }
    }

    private boolean fullCycleHasNext() {
        if (m_featureIdx < m_numFeatures - 1) {
            return true;
        }
        return m_featureIdx < m_numFeatures || m_activeSetChanged;
    }

    private boolean activeSetCycleHasNext() {
        return !m_activeSetConverged || m_activeSetIterator.hasNext();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int next() {
        // TODO the active set always converged prior to a full cycle (unless it is the first one)
        // hence we only need to check those features that are not yet part of the active set for changes
        if (m_isFullCycle) {
            return fullCycleNext();
        } else {
            return activeSetCycleNext();
        }
    }

    private int fullCycleNext() {
        if (m_featureIdx < m_numFeatures - 1) {
            m_featureIdx++;
            return m_featureIdx;
        } else {
            startActiveSetCycle();
            return activeSetCycleNext();
        }
    }

    private void startActiveSetCycle() {
        m_activeSetChanged = false;
        m_activeSetConverged = true;
        m_isFullCycle = false;
        m_activeSetIterator = m_activeSet.iterator();
    }

    private int activeSetCycleNext() {
        if (m_activeSetIterator.hasNext()) {
            return m_activeSetIterator.next();
        } else if (m_activeSetConverged) {
            startFullCycle();
            return fullCycleNext();
        } else {
            startActiveSetCycle();
            return activeSetCycleNext();
        }
    }

    private void startFullCycle() {
        m_featureIdx = 0;
        m_activeSetChanged = false;
        m_isFullCycle = true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void betaChanged() {
        if (m_isFullCycle) {
            m_activeSetChanged = m_activeSet.add(m_featureIdx);
        } else {
            m_activeSetConverged = false;
        }
    }

}
