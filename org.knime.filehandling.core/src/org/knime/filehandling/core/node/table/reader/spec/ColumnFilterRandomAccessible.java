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
 *   Apr 9, 2020 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.node.table.reader.spec;

import org.knime.core.node.util.CheckUtils;
import org.knime.filehandling.core.node.table.reader.randomaccess.AbstractRandomAccessible;
import org.knime.filehandling.core.node.table.reader.randomaccess.RandomAccessible;

/**
 * Filters out a single column from an underlying {@link RandomAccessible}.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
final class ColumnFilterRandomAccessible<V> extends AbstractRandomAccessible<V> {

    private RandomAccessible<V> m_decoratee;

    private final int m_columnToFilter;

    /**
     * Constructor.
     *
     * @param columnToFilter the index of the column to filter out (indices start at 0)
     */
    ColumnFilterRandomAccessible(final int columnToFilter) {
        m_columnToFilter = columnToFilter;
    }

    void setDecoratee(final RandomAccessible<V> decoratee) {
        m_decoratee = decoratee;
    }

    private void checkDecorateeIsPresent() {
        CheckUtils.checkState(m_decoratee != null, "Either next hasn't been called or it has returned null.");
    }

    @Override
    public int size() {
        checkDecorateeIsPresent();
        final int underlyingSize = m_decoratee.size();
        return underlyingSize > m_columnToFilter ? underlyingSize - 1 : underlyingSize;
    }

    @Override
    public V get(final int idx) {
        checkDecorateeIsPresent();
        final int filteredIdx = idx < m_columnToFilter ? idx : idx + 1;
        return m_decoratee.get(filteredIdx);
    }

}
