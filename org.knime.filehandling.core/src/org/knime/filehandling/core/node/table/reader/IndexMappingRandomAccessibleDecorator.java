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
 *   Mar 26, 2020 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.node.table.reader;

import org.knime.core.node.util.CheckUtils;
import org.knime.filehandling.core.node.table.reader.randomaccess.RandomAccessible;
import org.knime.filehandling.core.node.table.reader.util.IndexMapper;

/**
 * Performs mapping between indices and pads {@link RandomAccessible RandomAccessibles} if necessary
 * i.e. if an underlying {@link RandomAccessible} does not contain an index.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
final class IndexMappingRandomAccessibleDecorator<V> implements RandomAccessible<V> {

    private final IndexMapper m_idxMapper;

    private final int m_size;

    private RandomAccessible<V> m_decoratee;

    IndexMappingRandomAccessibleDecorator(final IndexMapper idxMapper) {
        m_idxMapper = idxMapper;
        // + 1 because the indices are zero based
        m_size = 1 + idxMapper.getIndexRangeEnd()
            .orElseThrow(() -> new IllegalArgumentException("The index mapper must have an end index."));
    }

    void set(final RandomAccessible<V> decoratee) {
        m_decoratee = decoratee;
    }

    @Override
    public int size() {
        return m_size;
    }

    @Override
    public V get(final int idx) {
        CheckUtils.checkState(m_decoratee != null, "No RandomAccessible to decorate set.");
        if (m_idxMapper.hasMapping(idx)) {
            final int mappedIdx = m_idxMapper.map(idx);
            return m_decoratee.size() > mappedIdx ? m_decoratee.get(mappedIdx) : null;
        } else {
            return null;
        }
    }

}
