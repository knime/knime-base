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
 *   Feb 7, 2020 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.node.table.reader.rowkey;

import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

import org.knime.core.data.RowKey;
import org.knime.filehandling.core.node.table.reader.randomaccess.RandomAccessible;

/**
 * Extracts the {@link RowKey RowKeys} from a single column using a user provided extraction function.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
final class ExtractingRowKeyGeneratorContext<I, V> implements GenericRowKeyGeneratorContext<I, V> {

    private static final String MISSING_ROW_KEY_PREFIX = "?";

    private final Function<V, String> m_rowKeyExtractor;

    private final int m_colIdx;

    private final AtomicLong m_rowIndex = new AtomicLong(-1);

    private final boolean m_prependSourcePrefix;

    private final String m_sourcePrefix;

    private int m_generatorCounter = -1;

    /**
     * Constructor.
     *
     * @param prefix common prefix of all generated keys
     * @param rowKeyExtractor converts a V into a String
     * @param colIdx index of the column containing the row keys
     */
    ExtractingRowKeyGeneratorContext(final Function<V, String> rowKeyExtractor, final int colIdx,
        final String sourcePrefix, final boolean prependSourcePrefix) {
        m_rowKeyExtractor = rowKeyExtractor;
        m_colIdx = colIdx;
        m_prependSourcePrefix = prependSourcePrefix;
        m_sourcePrefix = sourcePrefix;
    }

    @Override
    public RowKeyGenerator<V> createKeyGenerator(final I path) {
        m_generatorCounter++;
        return new SourceRowKeyGenerator(m_generatorCounter);
    }

    private class SourceRowKeyGenerator implements RowKeyGenerator<V> {

        private final String m_sourceIdxSuffix;

        SourceRowKeyGenerator(final int sourceIdx) {
            m_sourceIdxSuffix = m_sourcePrefix + "_" + sourceIdx + "_";
        }

        @Override
        public RowKey createKey(final RandomAccessible<V> values) {
            final long idx = m_rowIndex.incrementAndGet();
            final V key = values.size() < m_colIdx ? null : values.get(m_colIdx);
            String rowKey = key != null ? m_rowKeyExtractor.apply(key) : (MISSING_ROW_KEY_PREFIX + idx);
            if (m_prependSourcePrefix) {
                rowKey = m_sourceIdxSuffix + rowKey;
            }
            return new RowKey(rowKey);
        }

    }
}
