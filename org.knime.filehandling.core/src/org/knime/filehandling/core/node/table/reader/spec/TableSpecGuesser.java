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
 *   Jan 22, 2020 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.node.table.reader.spec;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.Function;
import java.util.function.IntUnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.knime.core.node.util.CheckUtils;
import org.knime.filehandling.core.node.table.reader.config.TableReadConfig;
import org.knime.filehandling.core.node.table.reader.randomaccess.RandomAccessible;
import org.knime.filehandling.core.node.table.reader.read.AbstractReadDecorator;
import org.knime.filehandling.core.node.table.reader.read.Read;
import org.knime.filehandling.core.node.table.reader.typehierarchy.TypeHierarchy;
import org.knime.filehandling.core.node.table.reader.typehierarchy.TypeHierarchy.TypeResolver;
import org.knime.filehandling.core.node.table.reader.util.IndexMapper;

/**
 * Guesses the spec of a table by finding the most specific type of every column using a TypeHierarchy provided by the
 * client.
 *
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 * @param <T> the type used to identify data types
 * @param <V> the type used as values
 */
public final class TableSpecGuesser<T, V> {

    private final TypeHierarchy<T, V> m_typeHierarchy;

    private final Function<V, String> m_valueToString;

    /**
     * Constructor.
     *
     * @param typeHierarchy used to resolve type conflicts
     * @param columnNameExtractor used to extract column names from values
     */
    public TableSpecGuesser(final TypeHierarchy<T, V> typeHierarchy, final Function<V, String> columnNameExtractor) {
        m_typeHierarchy = typeHierarchy;
        m_valueToString = columnNameExtractor;
    }

    /**
     * Guesses the spec from the rows provided by the {@link Read read}.</br>
     *
     * It is assumed that all rows produced by {@link Read read} have the same size.
     *
     * @param read providing the rows to guess the spec from
     * @param config providing the user settings
     * @return the guessed spec
     * @throws IOException if I/O problems occur
     */
    public ReaderTableSpec<T> guessSpec(final Read<V> read, final TableReadConfig<?> config) throws IOException {
        try (final ExtractColumnHeaderRead<V> source = new ExtractColumnHeaderRead<>(read, m_valueToString,
            config.useColumnHeaderIdx() ? config.getColumnHeaderIdx() : -1)) {
            final IndexMapper indexMapper = createIndexMapper(config);
            final List<T> types = guessTypes(source, indexMapper);
            final Optional<String[]> headerArray = source.getColumnHeaders();
            CheckUtils.checkState(headerArray.isPresent() || !config.useColumnHeaderIdx(),
                "The row containing the table headers was not read or contained only missing values.");
            return createTableSpec(types, headerArray, indexMapper);
        }
    }

    private ReaderTableSpec<T> createTableSpec(final List<T> types, final Optional<String[]> columnNames,
        final IndexMapper idxTranslator) {
        if (columnNames.isPresent()) {
            String[] headerArray = columnNames.get();
            return new ReaderTableSpec<>(IntStream.range(0, types.size())
                .mapToObj(i -> ReaderColumnSpec.createWithName(headerArray[idxTranslator.map(i)], types.get(i)))
                .collect(Collectors.toList()));
        } else {
            return new ReaderTableSpec<>(types.stream().map(ReaderColumnSpec::create).collect(Collectors.toList()));
        }
    }

    private List<T> guessTypes(final Read<V> source, final IndexMapper idxTranslator)
        throws IOException {
        RandomAccessible<V> row = source.next();
        CheckUtils.checkState(row != null, "Can't determine the types because the table is empty.");
        final TypeGuesser<T, V> typeGuesser = new TypeGuesser<>(m_typeHierarchy, row, idxTranslator);
        while (!typeGuesser.canStop() && (row = source.next()) != null) {
            typeGuesser.consume(row);
        }
        return typeGuesser.getMostSpecificTypes();

    }

    private static IndexMapper createIndexMapper(final TableReadConfig<?> config) {
        return new ContinuousIndexMapper(config.useRowIDIdx() ? config.getRowIDIdx() : -1);
    }

    private static final class ContinuousIndexMapper implements IndexMapper {

        private final int m_rowIDIdx;

        private final IntUnaryOperator m_translator;

        ContinuousIndexMapper(final int rowIDIdx) {
            m_rowIDIdx = rowIDIdx;
            if (rowIDIdx >= 0) {
                m_translator = i -> i < rowIDIdx ? i : i + 1;
            } else {
                m_translator = IntUnaryOperator.identity();
            }
        }

        @Override
        public int map(final int idx) {
            return m_translator.applyAsInt(idx);
        }

        @Override
        public OptionalInt getIndexRangeEnd() {
            return OptionalInt.empty();
        }

        @Override
        public boolean hasMapping(final int idx) {
            return true;
        }

        @Override
        public OptionalInt getRowIDIdx() {
            return m_rowIDIdx >= 0 ? OptionalInt.of(m_rowIDIdx) : OptionalInt.empty();
        }

    }

    private static final class TypeGuesser<T, V> {

        private final IndexMapper m_idxTranslator;

        private final TypeResolver<T, V>[] m_resolvers;

        private boolean m_canStop = false;

        TypeGuesser(final TypeHierarchy<T, V> typeHierarchy, final RandomAccessible<V> firstRow,
            final IndexMapper indexMapper) {
            m_idxTranslator = indexMapper;
            @SuppressWarnings("unchecked")
            final TypeResolver<T, V>[] resolvers = Stream.generate(typeHierarchy::createResolver)
                .limit(firstRow.size() - (indexMapper.getRowIDIdx().isPresent() ? 1L : 0L))
                .toArray(TypeResolver[]::new);
            m_resolvers = resolvers;
            consume(firstRow);
        }

        void consume(final RandomAccessible<V> row) {
            boolean canStop = true;
            for (int i = 0; i < m_resolvers.length; i++) {
                m_resolvers[i].accept(row.get(m_idxTranslator.map(i)));
                canStop &= m_resolvers[i].reachedTop();
            }
            m_canStop = canStop;
        }

        boolean canStop() {
            return m_canStop;
        }

        List<T> getMostSpecificTypes() {
            return Arrays.stream(m_resolvers).map(TypeResolver::getMostSpecificType).collect(Collectors.toList());
        }

    }

    /**
     * A read that extracts the row containing the table headers from the provided {@link Read source}.
     *
     * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
     */
    private static class ExtractColumnHeaderRead<V> extends AbstractReadDecorator<V> {

        private final long m_columnHeaderIdx;

        private final Function<V, String> m_nameExtractor;

        private String[] m_columnHeaders = null;

        private long m_rowIdx = -1;

        ExtractColumnHeaderRead(final Read<V> source, final Function<V, String> nameExtractor,
            final long columnHeaderIdx) {
            super(source);
            m_columnHeaderIdx = columnHeaderIdx;
            m_nameExtractor = nameExtractor;
        }

        /**
         * If necessary keeps reading until the headers are read and returns them.
         *
         * @return the extracted column headers
         * @throws IOException if there I/O problems while reading the headers
         */
        Optional<String[]> getColumnHeaders() throws IOException {
            if (m_columnHeaderIdx >= 0 && m_columnHeaders == null) {
                // make sure that the column headers are read
                while (m_columnHeaders == null) {
                    next();
                }
            }
            return Optional.ofNullable(m_columnHeaders);
        }

        private String[] extractNames(final RandomAccessible<V> values) {
            final String[] names = new String[values.size()];
            for (int i = 0; i < values.size(); i++) {
                names[i] = m_nameExtractor.apply(values.get(i));
            }
            return names;
        }

        /**
         * @return true if the next row contains the column headers
         */
        private boolean isColumnHeaderRow() {
            return m_rowIdx == m_columnHeaderIdx;
        }

        @Override
        public RandomAccessible<V> next() throws IOException {
            m_rowIdx++;
            if (isColumnHeaderRow()) {
                RandomAccessible<V> colHeaderRow = getSource().next();
                CheckUtils.checkState(colHeaderRow != null, "The row containing the row ids is not part of the table.");
                m_columnHeaders = extractNames(colHeaderRow);
            }
            return getSource().next();
        }

    }

}
