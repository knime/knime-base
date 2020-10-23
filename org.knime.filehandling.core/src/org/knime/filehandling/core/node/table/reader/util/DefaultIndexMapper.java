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
 *   Feb 11, 2020 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.node.table.reader.util;

import java.util.Arrays;
import java.util.OptionalInt;

import org.knime.core.node.util.CheckUtils;

/**
 * Default implementation of an IndexMapper.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
public final class DefaultIndexMapper implements IndexMapper {

    private final int[] m_mapping;

    private final int m_rowIDIdx;

    private DefaultIndexMapper(final int[] mapping, final int rowIDIdx) {
        m_mapping = mapping;
        m_rowIDIdx = rowIDIdx;
    }

    /**
     * Creates a {@link DefaultIndexMapperBuilder} that allows to add mappings for indices in the range [0, size).
     *
     * @param size the maximum number of mappings
     * @return a {@link DefaultIndexMapperBuilder} for building {@link DefaultIndexMapper} objects
     */
    public static DefaultIndexMapperBuilder builder(final int size) {
        return new DefaultIndexMapperBuilder(size);
    }

    /**
     * Creates a {@link DefaultIndexMapperBuilder} that allows to add mapping for indices in the range [0, size). The
     * second argument identifies the rowID column which is skipped when adding mappings to the builder.
     *
     * @param size the maximum number of mappings
     * @param rowIDIdx the index of the rowID column (skipped when adding mappings)
     * @return a {@link DefaultIndexMapperBuilder} for building {@link DefaultIndexMapper} objects
     */
    public static DefaultIndexMapperBuilder builder(final int size, final int rowIDIdx) {
        return new DefaultIndexMapperBuilder(size, rowIDIdx);
    }

    @Override
    public int map(final int idx) {
        final int mapped = m_mapping[idx];
//        CheckUtils.checkArgument(mapped >= 0, "There is no mapping for index %s.", idx);
        return mapped;
    }

    @Override
    public boolean hasMapping(final int idx) {
        return m_mapping[idx] != -1;
    }

    @Override
    public OptionalInt getIndexRangeEnd() {
        return OptionalInt.of(m_mapping.length - 1);
    }

    @Override
    public OptionalInt getRowIDIdx() {
        return m_rowIDIdx >= 0 ? OptionalInt.of(m_rowIDIdx) : OptionalInt.empty();
    }

    /**
     * Builder for {@link DefaultIndexMapper}.
     *
     * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
     */
    public static final class DefaultIndexMapperBuilder {

        private final int[] m_mapping;

        private final int m_rowIDIdx;

        /**
         * Constructor to use if there is no row id column.
         *
         * @param size number of columns of the merged table
         */
        private DefaultIndexMapperBuilder(final int size) {
            this(size, -1);
        }

        /**
         * Constructor to use if there is a row id column.
         *
         * @param size number of columns of the merged table (excluding the row id column)
         * @param rowIDIdx the index of the row id column
         */
        private DefaultIndexMapperBuilder(final int size, final int rowIDIdx) {
            m_mapping = new int[size];
            Arrays.fill(m_mapping, -1);
            m_rowIDIdx = rowIDIdx;
        }

        private boolean hasRowIdx() {
            return m_rowIDIdx >= 0;
        }

        /**
         * Adds a mapping from <b>from</b> to <b>to</b>.
         *
         * @param from the index to map from
         * @param to the index to map to
         * @return this builder
         */
        public DefaultIndexMapperBuilder addMapping(final int from, final int to) {
            CheckUtils.checkArgument(from >= 0, "The 'from' argument must be non-negative but was %s.", from);
            CheckUtils.checkArgument(to >= 0, "The 'to' argument must be non-negative but was %s.", to);
            int actualTo = to;
            if (hasRowIdx() && to >= m_rowIDIdx) {
                actualTo++;
            }
            m_mapping[from] = actualTo;
            return this;
        }

        /**
         * Builds the final index mapper.
         *
         * @return the final index mapper
         */
        public DefaultIndexMapper build() {
            return new DefaultIndexMapper(m_mapping.clone(), m_rowIDIdx);
        }
    }

}
