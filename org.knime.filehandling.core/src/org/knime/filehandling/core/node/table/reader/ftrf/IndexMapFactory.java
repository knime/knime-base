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
 *   Mar 30, 2021 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.node.table.reader.ftrf;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.knime.filehandling.core.node.table.reader.spec.TypedReaderTableSpec;
import org.knime.filehandling.core.node.table.reader.util.MultiTableUtils;

/**
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
final class IndexMapFactory {

    private final String[] m_union;

    IndexMapFactory(final TypedReaderTableSpec<?> union) {
        m_union = union.stream().map(MultiTableUtils::getNameAfterInit).toArray(String[]::new);
    }

    IndexMap createIndexMap(final TypedReaderTableSpec<?> individualSpec) {
        final Map<String, Integer> nameToIndex = createNameToIndexMap(individualSpec);
        return createIndexMap(nameToIndex);
    }

    private static Map<String, Integer> createNameToIndexMap(final TypedReaderTableSpec<?> spec) {
        final Map<String, Integer> nameToIndex = new HashMap<>(spec.size());
        for (int i = 0; i < spec.size(); i++) {
            nameToIndex.put(MultiTableUtils.getNameAfterInit(spec.getColumnSpec(i)), i);
        }
        return nameToIndex;
    }

    private IndexMap createIndexMap(final Map<String, Integer> nameToIndex) {
        final DefaultIndexMap.Builder indexMapBuilder = DefaultIndexMap.builder(m_union.length);
        for (int i = 0; i < m_union.length; i++) {
            final Integer idx = nameToIndex.get(m_union[i]);
            if (idx != null) {
                indexMapBuilder.addMapping(i, idx);
            }
        }
        return indexMapBuilder.build();
    }

    interface IndexMap {
        boolean hasMapping(int index);

        int map(int index);

        int size();
    }

    private static class DefaultIndexMap implements IndexMap {

        private final int[] m_indexMap;

        private DefaultIndexMap(final Builder builder) {
            m_indexMap = builder.m_indexMap.clone();
        }

        @Override
        public boolean hasMapping(final int index) {
            return m_indexMap[index] > -1;
        }

        @Override
        public int map(final int index) {
            return m_indexMap[index];
        }

        @Override
        public int size() {
            return m_indexMap.length;
        }

        static Builder builder(final int numOutputColumns) {
            return new Builder(numOutputColumns);
        }

        private static class Builder {

            private final int[] m_indexMap;

            private Builder(final int numOutputColumns) {
                m_indexMap = new int[numOutputColumns];
                Arrays.fill(m_indexMap, -1);
            }

            Builder addMapping(final int from, final int to) {
                m_indexMap[from] = to;
                return this;
            }

            DefaultIndexMap build() {
                return new DefaultIndexMap(this);
            }

        }

    }
}
