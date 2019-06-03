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
 *   May 10, 2019 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.base.node.meta.explain.feature;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.knime.base.node.meta.explain.util.iter.IntIterator;
import org.knime.core.data.DataCell;
import org.knime.core.node.util.CheckUtils;

/**
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
final class DefaultRowHandler implements RowHandler {

    // TODO rename FeatureHandler to cell handler because they really handle cells
    private final Iterable<FeatureHandler> m_cellHandlers;

    private final int[] m_numFeaturesPerHandler;

    DefaultRowHandler(final Iterable<FeatureHandler> cellHandlers, final int[] numFeaturesPerHandler) {
        m_cellHandlers = cellHandlers;
        m_numFeaturesPerHandler = numFeaturesPerHandler;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setOriginal(final Iterable<DataCell> original) {
        final Iterator<FeatureHandler> handlers = m_cellHandlers.iterator();
        for (final DataCell cell : original) {
            CheckUtils.checkArgument(handlers.hasNext(), "The provided row has more cells than expected.");
            handlers.next().setOriginal(cell);
        }
        CheckUtils.checkArgument(!handlers.hasNext(), "The provided row has fewer cells than expected.");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setReplacement(final Iterable<DataCell> replacement) {
        final Iterator<FeatureHandler> handlers = m_cellHandlers.iterator();
        for (final DataCell cell : replacement) {
            CheckUtils.checkArgument(handlers.hasNext(), "The provided row has more cells than expected.");
            handlers.next().setSampled(cell);
        }
        CheckUtils.checkArgument(!handlers.hasNext(), "The provided row has fewer cells than expected.");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setReplacementIndices(final IntIterator replacementIndices) {
        int offset = 0;
        final Iterator<FeatureHandler> handlers = m_cellHandlers.iterator();
        FeatureHandler currentHandler = handlers.next();
        int currentHandlerIdx = 0;
        while (replacementIndices.hasNext()) {
            final int globalIdx = replacementIndices.next();
            while (globalIdx >= offset + m_numFeaturesPerHandler[currentHandlerIdx]) {
                offset += m_numFeaturesPerHandler[currentHandlerIdx];
                currentHandlerIdx++;
                currentHandler = handlers.next();
            }
            final int localIdx = globalIdx - offset;
            currentHandler.markForReplacement(localIdx);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void resetReplacementIndices() {
        m_cellHandlers.forEach(FeatureHandler::resetReplaceState);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getExpectedNumberOfCells() {
        return m_numFeaturesPerHandler.length;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<DataCell> createReplaced() {
        final List<DataCell> cells = new ArrayList<>(m_numFeaturesPerHandler.length);
        m_cellHandlers.forEach(h -> cells.add(h.createReplaced()));
        return cells;
    }

}
