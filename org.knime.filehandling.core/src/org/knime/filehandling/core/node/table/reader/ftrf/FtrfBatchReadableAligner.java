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

import java.io.IOException;
import java.util.stream.IntStream;

import org.knime.core.columnar.ColumnarSchema;
import org.knime.core.columnar.batch.ReadBatch;
import org.knime.core.columnar.batch.SequentialBatchReadable;
import org.knime.core.columnar.batch.SequentialBatchReader;
import org.knime.core.columnar.data.DataSpec;
import org.knime.core.columnar.data.NullableReadData;
import org.knime.core.columnar.data.VoidData.VoidReadData;
import org.knime.core.columnar.filter.ColumnSelection;
import org.knime.core.columnar.filter.FilteredColumnSelection;
import org.knime.filehandling.core.node.table.reader.ftrf.IndexMapFactory.IndexMap;
import org.knime.filehandling.core.node.table.reader.ftrf.table.SourcedReaderTable;
import org.knime.filehandling.core.node.table.reader.spec.TypedReaderTableSpec;

/**
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
final class FtrfBatchReadableAligner<T> {

    private final IndexMapFactory m_indexMapFactory;

    private final TypedReaderTableSpec<T> m_union;

    FtrfBatchReadableAligner(final TypedReaderTableSpec<T> union) {
        m_indexMapFactory = new IndexMapFactory(union);
        m_union = union;
    }

    SourcedReaderTable<T> align(final SourcedReaderTable<T> individualTable) {
        final TypedReaderTableSpec<T> spec = individualTable.getSpec();
        final IndexMap indexMap = m_indexMapFactory.createIndexMap(spec);
        // TODO use union to "fill up" the columns missing in the individualTable spec
        return null;
    }

    private static class AlignedSequentialBatchReadable implements SequentialBatchReadable {

        private final IndexMap m_indexMap;

        private final SequentialBatchReadable m_delegate;

        private final AlignedColumnarSchema m_schema;

        AlignedSequentialBatchReadable(final IndexMap indexMap, final SequentialBatchReadable delegate) {
            m_indexMap = indexMap;
            m_delegate = delegate;
            m_schema = new AlignedColumnarSchema(delegate.getSchema());
        }

        @Override
        public ColumnarSchema getSchema() {
            return m_schema;
        }

        @Override
        public void close() throws IOException {
            m_delegate.close();
        }

        @Override
        public SequentialBatchReader createReader(final ColumnSelection selection) {
            final SequentialBatchReader delegateReader = m_delegate.createReader(map(selection));
            return new AlignedSequentialBatchReader(delegateReader);
        }

        private ColumnSelection map(final ColumnSelection selection) {
            final int[] indices = IntStream.range(0, selection.numColumns())//
                    .filter(selection::isSelected)// only keep selected columns
                    .filter(m_indexMap::hasMapping)// only keep columns that are present in the delegate
                    .map(m_indexMap::map)// map from the output index to the delegate index
                    .toArray();
            return new FilteredColumnSelection(m_schema.numColumnsInDelegate(), indices);
        }

        private class AlignedSequentialBatchReader implements SequentialBatchReader {

            private final SequentialBatchReader m_delegateBatchReader;

            AlignedSequentialBatchReader(final SequentialBatchReader delegate) {
                m_delegateBatchReader = delegate;
            }

            @Override
            public void close() throws IOException {
                m_delegateBatchReader.close();
            }

            @Override
            public ReadBatch readRetained() throws IOException {
                return new AlignedReadBatch(m_delegateBatchReader.readRetained());
            }

        }

        private class AlignedReadBatch implements ReadBatch {

            private final ReadBatch m_delegateBatch;

            AlignedReadBatch(final ReadBatch delegateBatch) {
                m_delegateBatch = delegateBatch;
            }

            @Override
            public NullableReadData[] getUnsafe() {
                final NullableReadData[] underlying = m_delegateBatch.getUnsafe();
                final NullableReadData[] data = new NullableReadData[size()];
                for (int i = 0; i < data.length; i++) {
                    if (m_indexMap.hasMapping(i)) {
                        data[i] = underlying[m_indexMap.map(i)];
                    } else {
                        data[i] = AlignedVoidData.INSTANCE;
                    }
                }
                return data;
            }

            @Override
            public void retain() {
                // TODO Auto-generated method stub
            }

            @Override
            public void release() {
                // TODO Auto-generated method stub
            }

            @Override
            public long sizeOf() {
                // TODO Auto-generated method stub
                return 0;
            }

            @Override
            public boolean isMissing(final int index) {
                if (m_indexMap.hasMapping(index)) {
                    return m_delegateBatch.isMissing(m_indexMap.map(index));
                } else {
                    return true;
                }
            }

            @Override
            public NullableReadData get(final int index) {
                if (m_indexMap.hasMapping(index)) {
                    return m_delegateBatch.get(m_indexMap.map(index));
                } else {
                    return AlignedVoidData.INSTANCE;
                }
            }

            @Override
            public int size() {
                return m_indexMap.size();
            }

            @Override
            public int length() {
                return m_delegateBatch.length();
            }

        }

        private enum AlignedVoidData implements VoidReadData {
            INSTANCE;

            @Override
            public int length() {
                return 0;
            }

            @Override
            public long sizeOf() {
                return 0;
            }

        }

        private class AlignedColumnarSchema implements ColumnarSchema {

            private final ColumnarSchema m_delegateSchema;

            AlignedColumnarSchema(final ColumnarSchema delegate) {
                m_delegateSchema = delegate;
            }

            @Override
            public int numColumns() {
                return m_indexMap.size();
            }

            @Override
            public DataSpec getSpec(final int index) {
                if (m_indexMap.hasMapping(index)) {
                    return m_delegateSchema.getSpec(m_indexMap.map(index));
                } else {
                    return DataSpec.voidSpec();
                }
            }

            int numColumnsInDelegate() {
                return m_delegateSchema.numColumns();
            }

        }
    }
}
