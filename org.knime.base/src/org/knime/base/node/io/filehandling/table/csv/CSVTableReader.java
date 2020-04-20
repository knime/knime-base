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
 *   Feb 6, 2020 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.base.node.io.filehandling.table.csv;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.OptionalLong;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import org.knime.filehandling.core.node.table.reader.TableReader;
import org.knime.filehandling.core.node.table.reader.config.TableReadConfig;
import org.knime.filehandling.core.node.table.reader.randomaccess.RandomAccessible;
import org.knime.filehandling.core.node.table.reader.randomaccess.RandomAccessibleUtils;
import org.knime.filehandling.core.node.table.reader.read.Read;
import org.knime.filehandling.core.node.table.reader.read.ReadUtils;
import org.knime.filehandling.core.node.table.reader.spec.ReaderTableSpec;
import org.knime.filehandling.core.node.table.reader.spec.TableSpecGuesser;
import org.knime.filehandling.core.node.table.reader.type.hierarchy.TreeTypeHierarchy;
import org.knime.filehandling.core.node.table.reader.type.hierarchy.TypeFocusableTypeHierarchy;
import org.knime.filehandling.core.node.table.reader.type.hierarchy.TypeTester;
import org.knime.filehandling.core.util.BomEncodingUtils;

import com.google.common.io.CountingInputStream;
import com.univocity.parsers.csv.CsvParser;
import com.univocity.parsers.csv.CsvParserSettings;

/**
 * {@link TableReader} that reads CSV files.
 *
 * @author Temesgen H. Dadi, KNIME GmbH, Berlin, Germany
 */
public final class CSVTableReader implements TableReader<CSVTableReaderConfig, Class<?>, String> {

    /**
     * {@link TreeTypeHierarchy} that defines the hierarchy of data types while reading from csv files
     */
    static final TypeFocusableTypeHierarchy<Class<?>, String> TYPE_HIERARCHY =
        TreeTypeHierarchy.builder(createTypeTester(String.class, t -> {
        })).addType(String.class, createTypeTester(Double.class, Double::parseDouble))
            .addType(Double.class, createTypeTester(Long.class, Long::parseLong))
            .addType(Long.class, createTypeTester(Integer.class, Integer::parseInt)).build();

    /**
     * {@link TableSpecGuesser} a spec guesser based on the hierarchy of data types defined by {@link TreeTypeHierarchy}
     */
    private static final TableSpecGuesser<Class<?>, String> SPEC_GUESSER =
        new TableSpecGuesser<>(TYPE_HIERARCHY, Function.identity());

    private static TypeTester<Class<?>, String> createTypeTester(final Class<?> type, final Consumer<String> tester) {
        return TypeTester.createTypeTester(type, consumerToPredicate(tester));
    }

    private static Predicate<String> consumerToPredicate(final Consumer<String> tester) {
        return s -> {
            try {
                tester.accept(s);
                return true;
            } catch (NumberFormatException ex) {
                return false;
            }
        };
    }

    @Override
    public Read<String> read(final Path path, final TableReadConfig<CSVTableReaderConfig> config) throws IOException {
        return createDecoratedRead(path, config, false);
    }

    /**
     * Parses the provided {@link InputStream} containing csv into a {@link Read} using the given
     * {@link TableReadConfig}.
     *
     * @param inputStream to read from
     * @param config specifying how to read
     * @return a {@link Read} containing the parsed inputs
     * @throws IOException if an I/O problem occurs
     */
    @SuppressWarnings("resource") // closing the read is the responsibility of the caller
    public static Read<String> read(final InputStream inputStream, final TableReadConfig<CSVTableReaderConfig> config)
        throws IOException {
        final CsvRead read = new CsvRead(inputStream, config.getReaderSpecificConfig());
        return decorateForReading(config, read);
    }

    @Override
    public ReaderTableSpec<Class<?>> readSpec(final Path path, final TableReadConfig<CSVTableReaderConfig> config)
        throws IOException {
        try (final Read<String> read = createDecoratedRead(path, config, true)) {
            return SPEC_GUESSER.guessSpec(read, config);
        }

    }

    /**
     * Creates a decorated {@link Read} from {@link CSVRead}, taking into account how many rows should be skipped or
     * what is the maximum number of rows to read. It also distinguishes between the purpose of the read, i.e., if it is
     * needed filling the actual table or for guessing column specifications. In the former case data rows will never be
     * skipped.
     *
     * @param path the path of the file to read
     * @param config the {@link TableReadConfig} used
     * @param isForSpec <code>true</code> if
     * @return a decorated read of type {@link Read}
     * @throws IOException if a stream can not be created from the provided file.
     */
    // the read is used in a try catch in the caller
    @SuppressWarnings("resource")
    private static Read<String> createDecoratedRead(final Path path, final TableReadConfig<CSVTableReaderConfig> config,
        final boolean isForSpec) throws IOException {
        final Read<String> read = new CsvRead(path, config.getReaderSpecificConfig());
        if (isForSpec) {
            return decorateForSpec(config, read);
        } else {
            return decorateForReading(config, read);
        }
    }

    @SuppressWarnings("resource") // closing the read is the responsibility of the caller
    private static Read<String> decorateForReading(final TableReadConfig<CSVTableReaderConfig> config,
        Read<String> read) {
        final boolean hasColumnHeader = config.useColumnHeaderIdx();
        final boolean skipRows = config.skipRows();
        if (skipRows) {
            final long numRowsToSkip = config.getNumRowsToSkip();
            read = ReadUtils.skip(read, hasColumnHeader ? numRowsToSkip + 1 : numRowsToSkip);
        }
        if (config.limitRows()) {
            final long numRowsToKeep = config.getMaxRows();
            // in case we skip rows, we already skipped the column header
            // otherwise we have to read one more row since the first is the column header
            read = ReadUtils.limit(read, hasColumnHeader && !skipRows ? numRowsToKeep + 1 : numRowsToKeep);
        }
        return read;
    }

    @SuppressWarnings("resource") // closing the read is the responsibility of the caller
    private static Read<String> decorateForSpec(final TableReadConfig<CSVTableReaderConfig> config, Read<String> read) {
        final boolean hasColumnHeader = config.useColumnHeaderIdx();
        // FIXME currently we can't skip if we read the column header, that should change once AP-14021 is implemented
        if (config.skipRows() && !hasColumnHeader) {
            read = ReadUtils.skip(read, config.getNumRowsToSkip());
        }
        if (config.limitRowsForSpec()) {
            final long rowLimit = config.getMaxRowsForSpec();
            read = ReadUtils.limit(read, hasColumnHeader ? rowLimit + 1 : rowLimit);
        }
        return read;
    }

    /**
     * Implements {@link Read} specific to CSV table reader, based on univocity's {@link CsvParser}.
     *
     * @author Temesgen H. Dadi, KNIME GmbH, Berlin, Germany
     */
    private static final class CsvRead implements Read<String> {

        /** a parser used to parse the file */
        private final CsvParser m_parser;

        /** the stream to read from */
        private final CountingInputStream m_countingStream;

        /** the reader reading from m_countingStream */
        private final BufferedReader m_reader;

        /** the size of the file being read */
        private final long m_size;

        /**
         * Constructor
         *
         * @param path the path of the file to read
         * @param csvReaderConfig the CSV reader configuration.
         * @throws IOException if a stream can not be created from the provided file.
         */
        CsvRead(final Path path, final CSVTableReaderConfig csvReaderConfig) throws IOException {
            this(Files.newInputStream(path), Files.size(path), csvReaderConfig);
        }

        CsvRead(final InputStream inputStream, final CSVTableReaderConfig csvReaderConfig) throws IOException {
            this(inputStream, -1, csvReaderConfig);
        }

        private CsvRead(final InputStream inputStream, final long size, final CSVTableReaderConfig csvReaderConfig)
            throws IOException {
            m_size = size;
            m_countingStream = new CountingInputStream(inputStream);
            final String charSetName = csvReaderConfig.getCharSetName();
            final Charset charset = charSetName == null ? Charset.defaultCharset() : Charset.forName(charSetName);
            m_reader = BomEncodingUtils.createBufferedReader(m_countingStream, charset);
            if (csvReaderConfig.skipLines()) {
                skipLines(csvReaderConfig.getNumLinesToSkip());
            }
            final CsvParserSettings settings = csvReaderConfig.getSettings();
            m_parser = new CsvParser(settings);
            m_parser.beginParsing(m_reader);
        }

        @Override
        public RandomAccessible<String> next() throws IOException {
            final String[] row = m_parser.parseNext();
            return row == null ? null : RandomAccessibleUtils.createFromArrayUnsafe(row);
        }

        @Override
        public void close() throws IOException {
            m_parser.stopParsing();
            // the parser should already close the reader and the stream but we close them anyway just to be sure
            m_reader.close();
            m_countingStream.close();
        }

        @Override
        public OptionalLong getEstimatedSizeInBytes() {
            return m_size < 0 ? OptionalLong.empty() : OptionalLong.of(m_size);
        }

        @Override
        public long readBytes() {
            return m_countingStream.getCount();
        }

        /**
         * Skips n lines from m_countingStream. The method supports different newline schemes (\n \r \r\n)
         *
         * @param n the number of lines to skip
         * @throws IOException if reading from the stream fails
         */
        private void skipLines(final long n) throws IOException {
            for (int i = 0; i < n; i++) {
                m_reader.readLine();
            }
        }
    }

}
