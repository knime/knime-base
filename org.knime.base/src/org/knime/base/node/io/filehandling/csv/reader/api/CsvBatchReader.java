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
 *   Mar 26, 2021 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.base.node.io.filehandling.csv.reader.api;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.stream.IntStream;

import org.knime.core.columnar.batch.ReadBatch;
import org.knime.core.columnar.batch.SequentialBatchReader;
import org.knime.core.columnar.data.NullableReadData;
import org.knime.core.columnar.data.StringData.StringReadData;
import org.knime.core.columnar.filter.ColumnSelection;

import com.univocity.parsers.csv.CsvParser;
import com.univocity.parsers.csv.CsvParserSettings;

/**
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
final class CsvBatchReader implements SequentialBatchReader {

    private final BufferedReader m_reader;

    private final CsvParser m_parser;

    private final ColumnSelection m_columnSelection;

    private final String[][] m_batch;

    private final CsvReadData[] m_columns;

    CsvBatchReader(final BufferedReader reader, final CsvParserSettings parserSettings,
        final ColumnSelection columnSelection, final int batchSize) {
        m_parser = new CsvParser(parserSettings);
        m_reader = reader;
        m_parser.beginParsing(reader);
        m_columnSelection = columnSelection;
        m_batch = new String[batchSize][];
        m_columns = IntStream.range(0, columnSelection.numColumns())//
                .mapToObj(i -> new CsvReadData(i, batchSize))//
                .toArray(CsvReadData[]::new);
    }

    @Override
    public void close() throws IOException {
        m_parser.stopParsing();
        m_reader.close();
    }

    @Override
    public ReadBatch readRetained() throws IOException {
        readNextBatch();
        return m_columnSelection.createBatch(this::getReadData);
    }

    private void readNextBatch() {
        for (int i = 0; i < m_batch.length; i++) {
            String[] row = m_parser.parseNext();
            if (row == null) {
                final int lastBatchSize = i;
                Arrays.stream(m_columns).forEach(c -> c.m_length = lastBatchSize);
                break;
            }
            m_batch[i] = row;
        }
    }

    private NullableReadData getReadData(final int index) {
        return m_columns[index];
    }

    private class CsvReadData implements StringReadData {

        private final int m_columnIndex;

        private int m_length;

        CsvReadData(final int columnIndex, final int batchSize) {
            m_columnIndex = columnIndex;
            m_length = batchSize;
        }

        @Override
        public boolean isMissing(final int index) {
            return getStringInternal(index) == null;
        }

        @Override
        public int length() {
            return m_length;
        }

        @Override
        public String getString(final int index) {
            return getStringInternal(index);
        }

        private String getStringInternal(final int index) {
            assert index < m_length : "The index exceeds length";
            return m_batch[index][m_columnIndex];
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

    }

}
