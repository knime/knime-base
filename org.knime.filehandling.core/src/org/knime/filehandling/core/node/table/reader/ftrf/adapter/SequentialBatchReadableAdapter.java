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
 *   Apr 6, 2021 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.node.table.reader.ftrf.adapter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.knime.core.columnar.ColumnarSchema;
import org.knime.core.columnar.batch.ReadBatch;
import org.knime.core.columnar.batch.SequentialBatchReadable;
import org.knime.core.columnar.batch.SequentialBatchReader;
import org.knime.core.columnar.data.DataSpec;
import org.knime.core.columnar.data.NullableReadData;
import org.knime.core.columnar.filter.ColumnSelection;
import org.knime.filehandling.core.node.table.reader.GenericTableReader;
import org.knime.filehandling.core.node.table.reader.config.ReaderSpecificConfig;
import org.knime.filehandling.core.node.table.reader.config.TableReadConfig;
import org.knime.filehandling.core.node.table.reader.randomaccess.RandomAccessible;
import org.knime.filehandling.core.node.table.reader.read.Read;
import org.knime.filehandling.core.node.table.reader.spec.TypedReaderColumnSpec;
import org.knime.filehandling.core.node.table.reader.spec.TypedReaderTableSpec;

/**
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
public final class SequentialBatchReadableAdapter<I, C extends ReaderSpecificConfig<C>, T, V>
    implements SequentialBatchReadable {

    private final I m_item;

    private final TableReadConfig<C> m_config;

    private final TypedReaderTableSpec<T> m_spec;

    private final GenericTableReader<I, C, T, V> m_reader;

    private final ColumnarSchema m_schema;

    private final ValueAccessFactory<T> m_valueAccessFactory;

    private final int m_batchSize;

    public SequentialBatchReadableAdapter(final I item, final TableReadConfig<C> config, final TypedReaderTableSpec<T> spec,
        final GenericTableReader<I, C, T, V> reader, final int batchSize,
        final ValueAccessFactory<T> valueAccessFactory) {
        m_item = item;
        m_config = config;
        m_spec = spec;
        m_reader = reader;
        m_batchSize = batchSize;
        m_valueAccessFactory = valueAccessFactory;
        m_schema = new ColumnarSchemaAdapter(m_spec.stream()//
            .map(TypedReaderColumnSpec::getType)//
            .map(valueAccessFactory::getDataSpec)//
            .toArray(DataSpec[]::new));
    }

    @Override
    public ColumnarSchema getSchema() {
        return m_schema;
    }

    @Override
    public void close() throws IOException {
        // nothing to close
    }

    @Override
    public SequentialBatchReader createReader(final ColumnSelection selection) {
        try {
            return new SequentialBatchReaderAdapter(m_reader.read(m_item, m_config), selection);
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private final class SequentialBatchReaderAdapter implements SequentialBatchReader {

        private final Read<I, V> m_read;

        private final ColumnSelection m_selection;

        SequentialBatchReaderAdapter(final Read<I, V> read, final ColumnSelection selection) {
            m_read = read;
            m_selection = selection;
        }

        @Override
        public void close() throws IOException {
            m_read.close();
        }

        @Override
        public ReadBatch readRetained() throws IOException {
            final List<RandomAccessible<V>> batch = new ArrayList<>();
            for (int i = 0; i < m_batchSize; i++) {
                final RandomAccessible<V> next = m_read.next();
                if (next == null) {
                    break;
                }
            }
            final NullableReadData[] adapters = createAdapters(batch);
            return m_selection.createBatch(i -> adapters[i]);
        }

        private NullableReadData[] createAdapters(final List<RandomAccessible<V>> batch) {
            final NullableReadData[] adapters = new NullableReadData[m_spec.size()];
            for (int i = 0; i < adapters.length; i++) {
                final T type = m_spec.getColumnSpec(i).getType();
                final ValueAccess access = m_valueAccessFactory.createValueAccess(type);
                adapters[i] = ReadDataAdapters.createAdapter(batch, i, access);
            }
            return adapters;
        }


    }


    private static final class ColumnarSchemaAdapter implements ColumnarSchema {

        private final DataSpec[] m_specs;

        ColumnarSchemaAdapter(final DataSpec[] specs) {
            m_specs = specs;
        }

        @Override
        public int numColumns() {
            return m_specs.length;
        }

        @Override
        public DataSpec getSpec(final int index) {
            return m_specs[index];
        }

    }

}
