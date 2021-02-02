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
 *   Jan 29, 2021 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.node.table.reader.randomaccess;

/**
 * Chains together two {@link RandomAccessibleDecorator RandomAccessibleDecorators}.<br>
 * The first decorator is decorated by the second one. <br>
 * The {@link RandomAccessibleDecorator#set(RandomAccessible)}
 * propagates the call through the {@link RandomAccessibleDecorator#set(RandomAccessible)} of both decorators to ensure
 * that any checks (e.g. empty checking) are done for the new decoratee.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 * @param <V> The type of value held by this {@link RandomAccessibleDecorator}
 * @noinstantiate Non-public API
 * @noreference Non-public API
 */
final class ChainedRandomAccessibleDecorator<V> extends AbstractRandomAccessible<V>
    implements RandomAccessibleDecorator<V> {

    private final RandomAccessibleDecorator<V> m_tail;

    private final RandomAccessibleDecorator<V> m_head;

    /**
     * Constructor.
     *
     * @param head the first decorator to apply
     * @param tail the second decorator to apply
     */
    public ChainedRandomAccessibleDecorator(final RandomAccessibleDecorator<V> head,
        final RandomAccessibleDecorator<V> tail) {
        m_head = head;
        m_tail = tail;
    }

    @Override
    public int size() {
        return m_tail.size();
    }

    @Override
    public V get(final int idx) {
        return m_tail.get(idx);
    }

    @Override
    public void set(final RandomAccessible<V> decoratee) {
        m_head.set(decoratee);
        m_tail.set(m_head);
    }

}
