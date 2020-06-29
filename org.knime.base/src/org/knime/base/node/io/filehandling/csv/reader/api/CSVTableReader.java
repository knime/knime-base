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
package org.knime.base.node.io.filehandling.csv.reader.api;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.NodeLogger;
import org.knime.filehandling.core.node.table.reader.TableReader;
import org.knime.filehandling.core.node.table.reader.config.TableReadConfig;
import org.knime.filehandling.core.node.table.reader.randomaccess.RandomAccessible;
import org.knime.filehandling.core.node.table.reader.randomaccess.RandomAccessibleUtils;
import org.knime.filehandling.core.node.table.reader.read.Read;
import org.knime.filehandling.core.node.table.reader.read.ReadUtils;
import org.knime.filehandling.core.node.table.reader.spec.TableSpecGuesser;
import org.knime.filehandling.core.node.table.reader.spec.TypedReaderTableSpec;
import org.knime.filehandling.core.node.table.reader.type.hierarchy.TreeTypeHierarchy;
import org.knime.filehandling.core.node.table.reader.type.hierarchy.TypeFocusableTypeHierarchy;
import org.knime.filehandling.core.node.table.reader.type.hierarchy.TypeTester;
import org.knime.filehandling.core.util.BomEncodingUtils;
import org.knime.filehandling.core.util.FileCompressionUtils;

import com.google.common.io.CountingInputStream;
import com.univocity.parsers.common.TextParsingException;
import com.univocity.parsers.csv.CsvParser;
import com.univocity.parsers.csv.CsvParserSettings;

/**
 * {@link TableReader} that reads CSV files.
 *
 * @author Temesgen H. Dadi, KNIME GmbH, Berlin, Germany
 * @since 4.2
 */
public final class CSVTableReader implements TableReader<CSVTableReaderConfig, Class<?>, String> {

    /**
     * {@link TreeTypeHierarchy} that defines the hierarchy of data types while reading from csv files
     */
    public static final TypeFocusableTypeHierarchy<Class<?>, String> TYPE_HIERARCHY =
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

    @SuppressWarnings("resource") // closing the read is the responsibility of the caller
    @Override
    public Read<String> read(final Path path, final TableReadConfig<CSVTableReaderConfig> config) throws IOException {
        return decorateForReading(new CsvRead(path, config), config);
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
        final CsvRead read = new CsvRead(inputStream, config);
        return decorateForReading(read, config);
    }

    @Override
    public TypedReaderTableSpec<Class<?>> readSpec(final Path path, final TableReadConfig<CSVTableReaderConfig> config,
        final ExecutionMonitor exec) throws IOException {
        try (final CsvRead read = new CsvRead(path, config)) {
            return SPEC_GUESSER.guessSpec(read, config, exec);
        }
    }

    /**
     * Creates a decorated {@link Read} from {@link CSVRead}, taking into account how many rows should be skipped or
     * what is the maximum number of rows to read.
     *
     * @param path the path of the file to read
     * @param config the {@link TableReadConfig} used
     * @return a decorated read of type {@link Read}
     * @throws IOException if a stream can not be created from the provided file.
     */
    @SuppressWarnings("resource") // closing the read is the responsibility of the caller
    private static Read<String> decorateForReading(final CsvRead read,
        final TableReadConfig<CSVTableReaderConfig> config) {
        Read<String> filtered = read;
        final boolean hasColumnHeader = config.useColumnHeaderIdx();
        final boolean skipRows = config.skipRows();
        if (skipRows) {
            final long numRowsToSkip = config.getNumRowsToSkip();
            filtered = ReadUtils.skip(filtered, hasColumnHeader ? numRowsToSkip + 1 : numRowsToSkip);
        }
        if (config.limitRows()) {
            final long numRowsToKeep = config.getMaxRows();
            // in case we skip rows, we already skipped the column header
            // otherwise we have to read one more row since the first is the column header
            filtered = ReadUtils.limit(filtered, hasColumnHeader && !skipRows ? numRowsToKeep + 1 : numRowsToKeep);
        }
        return filtered;
    }

    /**
     * Implements {@link Read} specific to CSV table reader, based on univocity's {@link CsvParser}.
     *
     * @author Temesgen H. Dadi, KNIME GmbH, Berlin, Germany
     */
    private static final class CsvRead implements Read<String> {

        /** */
        private static final NodeLogger LOGGER = NodeLogger.getLogger(CsvRead.class);

        /** a parser used to parse the file */
        private final CsvParser m_parser;

        /** the stream to read from */
        private final CountingInputStream m_countingStream;

        /** the reader reading from m_countingStream */
        private final BufferedReader m_reader;

        /** the size of the file being read */
        private final long m_size;

        /** the {@link CsvParserSettings} */
        private final CsvParserSettings m_csvParserSettings;

        /** the path of the underlying source */
        private final Path m_path;

        /**
         * Constructor
         *
         * @param path the path of the file to read
         * @param config the CSV table reader configuration.
         * @throws IOException if a stream can not be created from the provided file.
         */
        CsvRead(final Path path, final TableReadConfig<CSVTableReaderConfig> config) throws IOException {
            this(FileCompressionUtils.createInputStream(path), Files.size(path), path, config);
        }

        /**
         * Constructor
         *
         * @param inputStream the {@link InputStream} to read from
         * @param config the CSV table reader configuration.
         * @throws IOException if a stream can not be created from the provided file.
         */
        CsvRead(final InputStream inputStream, final TableReadConfig<CSVTableReaderConfig> config) throws IOException {
            this(inputStream, -1, null, config);
        }

        private CsvRead(final InputStream inputStream, final long size, final Path path,
            final TableReadConfig<CSVTableReaderConfig> config) throws IOException {
            m_size = size;
            m_path = path;
            m_countingStream = new CountingInputStream(inputStream);

            final CSVTableReaderConfig csvReaderConfig = config.getReaderSpecificConfig();
            final String charSetName = csvReaderConfig.getCharSetName();
            final Charset charset = charSetName == null ? Charset.defaultCharset() : Charset.forName(charSetName);
            m_reader = BomEncodingUtils.createBufferedReader(m_countingStream, charset);
            if (csvReaderConfig.skipLines()) {
                skipLines(csvReaderConfig.getNumLinesToSkip());
            }
            // Get the Univocity Parser settings from the reader specific configuration.
            m_csvParserSettings = csvReaderConfig.getSettings();
            m_parser = new CsvParser(m_csvParserSettings);
            m_parser.beginParsing(m_reader);
        }

        @Override
        public RandomAccessible<String> next() throws IOException {
            String[] row = null;
            try {
                row = m_parser.parseNext();
            } catch (final TextParsingException e) {
                //Log original exception message
                LOGGER.debug(e.getMessage(), e);
                final Throwable cause = e.getCause();
                if (cause instanceof ArrayIndexOutOfBoundsException) {
                    //Exception handling in case maxCharsPerCol or maxCols are exceeded like in the AbstractParser
                    final int index = extractErrorIndex(cause);
                    if (index == m_csvParserSettings.getMaxCharsPerColumn()) {
                        throw new IOException("Memory limit per column exceeded. Please adapt the according setting.");
                    } else if (index == m_csvParserSettings.getMaxColumns()) {
                        throw new IOException("Number of parsed columns exceeds the defined limit ("
                            + m_csvParserSettings.getMaxColumns() + "). Please adapt the according setting.");
                    } else {
                        // fall through to default exception
                    }
                }
                throw new IOException(
                    "Something went wrong during the parsing process. For further details please have a look into "
                        + "the log.", e);

            }
            return row == null ? null : RandomAccessibleUtils.createFromArrayUnsafe(row);
        }

        private static int extractErrorIndex(final Throwable cause) {
            try {
                return Integer.parseInt(cause.getMessage());
            } catch (NumberFormatException ex) {
                return -1;
            }
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

        @Override
        public Optional<Path> getPath() {
            return Optional.ofNullable(m_path);
        }
    }

}
