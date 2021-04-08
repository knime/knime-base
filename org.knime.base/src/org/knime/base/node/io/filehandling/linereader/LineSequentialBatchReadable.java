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
 *   Apr 8, 2021 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.base.node.io.filehandling.linereader;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.knime.base.node.io.filehandling.streams.CompressionAwareCountingInputStream;
import org.knime.core.columnar.ColumnarSchema;
import org.knime.core.columnar.batch.ReadBatch;
import org.knime.core.columnar.batch.SequentialBatchReadable;
import org.knime.core.columnar.batch.SequentialBatchReader;
import org.knime.core.columnar.data.DataSpec;
import org.knime.core.columnar.data.NullableReadData;
import org.knime.core.columnar.data.StringData.StringReadData;
import org.knime.core.columnar.filter.ColumnSelection;
import org.knime.filehandling.core.node.table.reader.config.TableReadConfig;
import org.knime.filehandling.core.node.table.reader.randomaccess.RandomAccessible;
import org.knime.filehandling.core.util.BomEncodingUtils;

/**
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
final class LineSequentialBatchReadable implements SequentialBatchReadable {

    private final Path m_path;

    private final TableReadConfig<LineReaderConfig2> m_config;

    private final int m_batchSize;

    private final boolean m_replaceEmpty;

    private final String m_emptyLineReplacement;

    private final boolean m_useRegex;

    private final Pattern m_regexPattern;

    LineSequentialBatchReadable(final Path path, final TableReadConfig<LineReaderConfig2> config, final int batchSize) {
        m_path = path;
        m_config = config;
        m_batchSize = batchSize;
        final LineReaderConfig2 lineReaderConfig = config.getReaderSpecificConfig();
        m_replaceEmpty = lineReaderConfig.getReplaceEmptyMode() == EmptyLineMode.REPLACE_EMPTY;
        m_emptyLineReplacement = lineReaderConfig.getEmptyLineReplacement();
        m_useRegex = lineReaderConfig.useRegex();
        m_regexPattern = Pattern.compile(lineReaderConfig.getRegex());
    }

    @Override
    public ColumnarSchema getSchema() {
        return LineColumnarSchema.INSTANCE;
    }

    @Override
    public void close() throws IOException {
        // nothing to close
    }

    @Override
    public SequentialBatchReader createReader(final ColumnSelection selection) {
        assert selection.numColumns() == 1
            && selection.isSelected(0) : "The line reader reads only a single column and doesn't support filtering.";
        try {
            return new LineBatchReader();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private final class LineBatchReader implements SequentialBatchReader {

        private final BufferedReader m_reader;

        private int m_linesRead;

        private boolean m_endOfFile = false;

        @SuppressWarnings("resource") // the input stream is closed by m_reader
        LineBatchReader() throws IOException {
            final String charSetName = m_config.getReaderSpecificConfig().getCharSetName();
            final Charset charset = charSetName == null ? Charset.defaultCharset() : Charset.forName(charSetName);
            m_reader = BomEncodingUtils.createBufferedReader(new CompressionAwareCountingInputStream(m_path), charset);
            if (m_config.useColumnHeaderIdx()) {
                // the column header is in the first row and not part of the content
                m_reader.readLine();
            }
        }

        @Override
        public void close() throws IOException {
            m_reader.close();
        }

        @Override
        public ReadBatch readRetained() throws IOException {
            final List<String> batch = new ArrayList<>(m_batchSize);
            for (int i = 0; !m_endOfFile && i < m_batchSize; i++) {
                final String line = getNextLine();
                if (m_endOfFile) {
                    break;
                }
                m_linesRead++;
                batch.add(line);
            }
            return new LineReadBatch(batch);
        }

        /**
         * Returns the next line.
         *
         * @return the next line
         * @throws IOException
         */
        private String getNextLine() throws IOException {
            if (!m_config.limitRows() || m_linesRead < m_config.getMaxRows()) {
                return getLine();
            } else {
                m_endOfFile = true;
                //When limit rows true and it exceeds the set limit
                return null;
            }

        }

        /**
         * Returns the next line.
         *
         * @return the next {@link RandomAccessible}
         * @throws IOException
         */
        private String getLine() throws IOException {
            boolean matches = false;
            String line;
            do {
                line = m_reader.readLine();
                if (line == null) {
                    m_endOfFile = true;
                    //no more lines to read
                    return null;
                }

                if (line.trim().isEmpty()) {
                    if (m_config.skipEmptyRows()) {
                        continue;
                    } else if (m_replaceEmpty) {
                        line = m_emptyLineReplacement;
                    } else {
                        return null;
                    }
                }

                if (m_useRegex) {
                    matches = m_regexPattern.matcher(line).matches();
                } else {
                    matches = true;
                }
            } while (!m_endOfFile && !matches);

            return line;
        }

    }

    private static final class LineReadBatch implements ReadBatch, StringReadData {

        private final List<String> m_values;

        LineReadBatch(final List<String> values) {
            m_values = values;
        }

        @Override
        public NullableReadData[] getUnsafe() {
            return new NullableReadData[]{this};
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
            return m_values.get(index) == null;
        }

        @Override
        public String getString(final int index) {
            return m_values.get(index);
        }

        @Override
        public NullableReadData get(final int index) {
            return this;
        }

        @Override
        public int size() {
            return m_values.size();
        }

        @Override
        public int length() {
            return size();
        }

    }

    private enum LineColumnarSchema implements ColumnarSchema {
            INSTANCE;

        @Override
        public int numColumns() {
            return 1;
        }

        @Override
        public DataSpec getSpec(final int index) {
            return DataSpec.stringSpec();
        }

    }
}
