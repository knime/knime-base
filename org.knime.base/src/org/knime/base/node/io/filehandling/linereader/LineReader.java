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
 *   Sep 30, 2019 (julian): created
 */
package org.knime.base.node.io.filehandling.linereader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.knime.base.node.io.filehandling.FilesToDataTableReader;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowKey;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.streamable.RowOutput;
import org.knime.core.node.util.CheckUtils;

/**
 * Specific implementation of {@link FilesToDataTableReader} to process file(s) line by line.
 *
 * @author Mareike Hoeger, KNIME GmbH, Konstanz, Germany
 * @author Julian Bunzel, KNIME GmbH, Berlin, Germany
 */
final class LineReader implements FilesToDataTableReader {

    private boolean m_readColHeader;

    private final String m_rowPrefix;

    private final int m_limit;

    private final boolean m_limitLines;

    private final boolean m_useRegex;

    private RowBuilder m_rowBuilder = null;

    private final String m_regex;

    private final boolean m_skipEmptyLines;

    private final String m_columnHeader;

    /**
     * Creates a new instance of {@link LineReader} with given {@link LineReaderConfig}.
     *
     * @param config The {@code LineReaderConfig}
     */
    LineReader(final LineReaderConfig config) {
        m_readColHeader = config.getReadColHeader();
        m_rowPrefix = config.getRowPrefix();
        m_limit = config.getLimitRowCount();
        m_limitLines = config.getLimitLines();
        m_useRegex = config.getUseRegex();
        m_regex = config.getRegex();
        m_skipEmptyLines = config.getSkipEmptyLines();
        m_columnHeader = config.getColumnHeader();
    }

    @Override
    public DataTableSpec createDataTableSpec(final List<Path> paths, final ExecutionMonitor exec)
            throws InvalidSettingsException {
        final String tableName = getTableName(paths);
        final Path path = paths.get(0);
        try (Stream<String> currentLines = Files.lines(path)) {
            String colName;

            if (m_readColHeader) {

                final Optional<String> optColName =
                    currentLines.filter(s -> (!m_skipEmptyLines || !s.trim().isEmpty())).findFirst();

                if (!optColName.isPresent() || optColName.get().trim().isEmpty()) {
                    // if top line or all lines are blank in file use a default non-empty string
                    colName = "<empty>";
                } else {
                    colName = optColName.get();
                }
            } else {
                colName = CheckUtils.checkNotNull(m_columnHeader, "column header in config must not be null");
            }

            final DataColumnSpecCreator creator = new DataColumnSpecCreator(colName, StringCell.TYPE);

            return new DataTableSpec(tableName, creator.createSpec());
        } catch (final IOException e) {
            throw new InvalidSettingsException(e);
        }
    }

    @Override
    public void pushRowsToOutput(final Path path, final RowOutput output, final ExecutionContext exec)
        throws Exception {
        if (m_rowBuilder == null) {
            m_rowBuilder = new RowBuilder(m_rowPrefix, output);
        }
        if (m_limitLines) {
            readLimited(m_limit - m_rowBuilder.getRowCount(), path, m_rowBuilder, m_readColHeader);

        } else {
            readAllLines(path, m_rowBuilder, m_readColHeader);
        }

        m_readColHeader = false;
    }

    private static class RowBuilder implements Consumer<String> {

        private final String m_rowPrefix;

        private final RowOutput m_output;

        private int m_rowCount = 0;

        /**
         * Creates a new instance of {@link RowBuilder}.
         *
         * @param rowPrefix The row's prefix
         * @param output The {@code RowOutput}
         */
        public RowBuilder(final String rowPrefix, final RowOutput output) {
            m_rowPrefix = rowPrefix;
            m_output = output;
        }

        @Override
        public void accept(final String line) {
            final RowKey key = new RowKey(m_rowPrefix + (m_rowCount));
            m_rowCount++;
            final DefaultRow row = new DefaultRow(key, new StringCell(line));
            try {
                m_output.push(row);
            } catch (InterruptedException e) {
                final Thread currentThread = Thread.currentThread();
                if (currentThread.isInterrupted()) {
                    currentThread.interrupt();
                }
            }
        }

        public int getRowCount() {
            return m_rowCount;
        }
    }

    private void readLimited(final long l, final Path path, final RowBuilder rowbuilder, final boolean skipFirst)
        throws IOException {
        try (Stream<String> lineStream = Files.lines(path)) {
            lineStream.filter(this::empytFilter).skip(skipFirst ? 1 : 0).limit(l).filter(this::regExMatch)
                .forEachOrdered(rowbuilder);
        }
    }

    private void readAllLines(final Path path, final RowBuilder rowbuilder, final boolean skipFirst)
        throws IOException {
        try (Stream<String> lineStream = Files.lines(path)) {
            lineStream.filter(this::empytFilter).skip(skipFirst ? 1 : 0).filter(this::regExMatch)
                .forEachOrdered(rowbuilder);
        }
    }

    private boolean regExMatch(final String s) {
        return !m_useRegex || s.matches(m_regex);
    }

    private boolean empytFilter(final String s) {
        return !m_skipEmptyLines || !s.trim().isEmpty();
    }

    private static String getTableName(final List<Path> pathes) {
        Path namePath = pathes.get(0);

        if (pathes.size() > 1) {
            namePath = namePath.getParent();
        }
        String tableName;
        if (namePath == null || namePath.getFileName() == null) {
            tableName = "LineReader output";
        } else {
            tableName = namePath.getFileName().toString();
        }

        return tableName;
    }
}
