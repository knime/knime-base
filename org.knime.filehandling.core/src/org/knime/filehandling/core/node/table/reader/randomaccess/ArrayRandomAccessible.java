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
 *   Feb 4, 2020 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.node.table.reader.randomaccess;

/**
 * A {@link RandomAccessible} based on arrays.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 * @param <V> the type of objects stored in the RandomAccessible
 */
final class ArrayRandomAccessible<V> implements RandomAccessible<V> {

    private final V[] m_data;

    private ArrayRandomAccessible(final V[] data) {
        m_data = data;
    }

    /**
     * Creates a new instance by copying the data, i.e. changes to the array <b>data</b> are not reflected in the
     * RandomAccessible. Note however, that the elements contained in <b>data</b> are not copied, so mutating them will
     * also mutate them in the RandomAccessible.
     *
     * @param data to wrap
     * @return a new instance of ArrayRandomAccessible with <b>data</b>
     */
    public static <V> ArrayRandomAccessible<V> createSafe(final V[] data) {
        return new ArrayRandomAccessible<>(data.clone());
    }

    /**
     * Creates a new instance using <b>data</b> directly. Consequently, any changes to <b>data</b> will also change the
     * RandomAccessible. Use with caution.
     *
     * @param data to wrap
     * @return a new instance of ArrayRandomAccessible with <b>data</b>
     */
    public static <V> ArrayRandomAccessible<V> createUnsafe(final V[] data) {
        return new ArrayRandomAccessible<>(data);
    }

    @Override
    public int size() {
        return m_data.length;
    }

    @Override
    public V get(final int idx) {
        return m_data[idx];
    }
}
