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
 *   Mar 29, 2021 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.node.table.reader.ftrf;

import java.util.List;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.MissingCell;
import org.knime.core.data.RowKey;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.filestore.FileStoreFactory;
import org.knime.core.data.v2.ReadValue;
import org.knime.core.data.v2.RowContainer;
import org.knime.core.data.v2.RowCursor;
import org.knime.core.data.v2.RowRead;
import org.knime.core.data.v2.RowWrite;
import org.knime.core.data.v2.RowWriteCursor;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.streamable.RowOutput;
import org.knime.filehandling.core.node.table.reader.PreviewRowIterator;
import org.knime.filehandling.core.node.table.reader.config.tablespec.TableSpecConfig;
import org.knime.filehandling.core.node.table.reader.ftrf.table.SourcedReaderTable;
import org.knime.filehandling.core.node.table.reader.util.MultiTableRead;

/**
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
class FtrfMultiTableRead<T> implements MultiTableRead<T> {

    private final TableSpecConfig<T> m_tableSpecConfig;

    private final List<SourcedReaderTable<T>> m_tables;

    private final FtrfRowCursorFactory<T> m_rowCursorFactory;

    FtrfMultiTableRead(final TableSpecConfig<T> tableSpecConfig, final List<SourcedReaderTable<T>> sourceTuples) {
        m_tableSpecConfig = tableSpecConfig;
        m_tables = sourceTuples;
        m_rowCursorFactory = new FtrfRowCursorFactory<>(tableSpecConfig.getTableTransformation());
    }

    @Override
    public DataTableSpec getOutputSpec() {
        return m_tableSpecConfig.getDataTableSpec();
    }

    @Override
    public TableSpecConfig<T> getTableSpecConfig() {
        return m_tableSpecConfig;
    }

    @SuppressWarnings("resource") // the PreviewRowIterator takes care of closing the cursor
    @Override
    public PreviewRowIterator createPreviewIterator() {
        return new RowCursorPreviewRowIterator(createConcatenatedRowCursor());
    }

    @Override
    public void fillRowOutput(final RowOutput output, final ExecutionMonitor exec, final FileStoreFactory fsFactory)
        throws Exception {
        try (final RowCursor concatenated = createConcatenatedRowCursor()) {
            while (concatenated.canForward()) {
                output.push(toRow(concatenated.forward()));
            }
        }
    }

    public BufferedDataTable readTable(final ExecutionContext exec) {
        try (final RowContainer rowContainer = exec.createRowContainer(getOutputSpec());
                RowWriteCursor cursor = rowContainer.createCursor()) {
            fillRowWriteCursor(cursor);
            return rowContainer.finish();
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }

    }

    private void fillRowWriteCursor(final RowWriteCursor cursor) {
        try (final RowCursor rowCursor = createConcatenatedRowCursor()) {
            fillFrom(cursor, rowCursor);
        }
    }

    private static void fillFrom(final RowWriteCursor writeCursor, final RowCursor readCursor) {
        while (readCursor.canForward()) {
            assert writeCursor.canForward();
            final RowWrite rowWrite = writeCursor.forward();
            final RowRead rowRead = readCursor.forward();
            rowWrite.setFrom(rowRead);
        }
    }

    private RowCursor createConcatenatedRowCursor() {
        final RowCursor[] cursors = m_tables.stream()//
            .map(m_rowCursorFactory::create)//
            .toArray(RowCursor[]::new);
        return concatenate(cursors);
    }

    private static RowCursor concatenate(final RowCursor[] cursors) {
        // TODO utility function on row cursor level
        return null;
    }

    // TODO this feels like it should be in a utility class that provides compatibility between the old and the new data API
    private static DataRow toRow(final RowRead rowRead) {
        final RowKey rowKey = new RowKey(rowRead.getRowKey().getString());
        final DataCell[] cells = toCells(rowRead);
        return new DefaultRow(rowKey, cells);
    }

    private static DataCell[] toCells(final RowRead rowRead) {
        final DataCell[] cells = new DataCell[rowRead.getNumColumns()];
        for (int i = 0; i < cells.length; i++) {
            if (rowRead.isMissing(i)) {
                cells[i] = new MissingCell("missing");
            } else {
                final ReadValue value = rowRead.getValue(i);
                cells[i] = value.getDataCell();
            }
        }
        return cells;
    }

    private static final class RowCursorPreviewRowIterator extends PreviewRowIterator {

        private final RowCursor m_cursor;

        RowCursorPreviewRowIterator(final RowCursor cursor) {
            m_cursor = cursor;
        }

        @Override
        public void close() {
            m_cursor.close();
        }

        @Override
        public boolean hasNext() {
            return m_cursor.canForward();
        }

        @Override
        public DataRow next() {
            final RowRead rowRead = m_cursor.forward();
            return toRow(rowRead);
        }

    }

}
