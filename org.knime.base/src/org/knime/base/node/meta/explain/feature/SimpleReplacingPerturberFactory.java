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
 *   Apr 2, 2019 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.base.node.meta.explain.feature;

import java.util.Set;

import org.knime.base.node.meta.explain.util.RowSampler;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;

/**
 * Assumes that every column represents exactly one feature.
 * In this case the perturbation can be done by simply swapping out
 * individual cells.
 * Simpler and more efficient than VectorEnabledPerturberFactory but can't handle
 * columns that represent multiple features (e.g. vectors or collections).
 *
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
public class SimpleReplacingPerturberFactory implements PerturberFactory<DataRow, Set<Integer>, DataCell[]> {

    private final RowSampler m_rowSampler;

    /**
     * @param rowSampler allows to draw random rows from a sampling set
     */
    public SimpleReplacingPerturberFactory(final RowSampler rowSampler) {
        m_rowSampler = rowSampler;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Perturber<DataRow, Set<Integer>, DataCell[]> createPerturber() {
        return new SimpleReplacingPerturber(m_rowSampler.sampleRow());
    }

    private static class SimpleReplacingPerturber implements Perturber<DataRow, Set<Integer>, DataCell[]> {

        private final DataRow m_replacementSource;

        /**
         * @param replacementSource source for cell replacements
         *
         */
        public SimpleReplacingPerturber(final DataRow replacementSource) {
            m_replacementSource = replacementSource;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public DataCell[] perturb(final DataRow perturbee, final Set<Integer> config) {
            final DataCell[] cells = new DataCell[perturbee.getNumCells()];
            for (int i = 0; i < cells.length; i++) {
                final DataCell cell = config.contains(i) ? m_replacementSource.getCell(i) : perturbee.getCell(i);
                cells[i] = cell;
            }
            return cells;
        }

    }

}
