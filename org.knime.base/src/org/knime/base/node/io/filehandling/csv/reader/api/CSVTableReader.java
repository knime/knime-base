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
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalLong;
import java.util.function.Function;
import java.util.regex.Pattern;

import org.knime.base.node.io.filehandling.csv.reader.ChunkReader;
import org.knime.base.node.io.filehandling.csv.reader.OSIndependentNewLineReader;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.NodeLogger;
import org.knime.filehandling.core.connections.FSPath;
import org.knime.filehandling.core.connections.meta.FSType;
import org.knime.filehandling.core.node.table.reader.SourceGroup;
import org.knime.filehandling.core.node.table.reader.TableReader;
import org.knime.filehandling.core.node.table.reader.config.TableReadConfig;
import org.knime.filehandling.core.node.table.reader.randomaccess.RandomAccessible;
import org.knime.filehandling.core.node.table.reader.randomaccess.RandomAccessibleUtils;
import org.knime.filehandling.core.node.table.reader.read.Read;
import org.knime.filehandling.core.node.table.reader.read.ReadUtils;
import org.knime.filehandling.core.node.table.reader.spec.TableSpecGuesser;
import org.knime.filehandling.core.node.table.reader.spec.TypedReaderTableSpec;
import org.knime.filehandling.core.util.BomEncodingUtils;
import org.knime.filehandling.core.util.CompressionAwareCountingInputStream;
import org.knime.filehandling.core.util.FileCompressionUtils;

import com.univocity.parsers.common.TextParsingException;
import com.univocity.parsers.csv.CsvParser;
import com.univocity.parsers.csv.CsvParserSettings;

/**
 * {@link TableReader} that reads CSV files.
 *
 * @author Temesgen H. Dadi, KNIME GmbH, Berlin, Germany
 * @noreference This class is not intended to be referenced by clients.
 * @noinstantiate This class is not intended to be instantiated by clients.
 */
public final class CSVTableReader implements TableReader<CSVTableReaderConfig, Class<?>, String> {

    @SuppressWarnings("resource") // closing the read is the responsibility of the caller
    @Override
    public Read<String> read(final FSPath path, final TableReadConfig<CSVTableReaderConfig> config)
        throws IOException {
        return decorateForSequentialReading(new CsvRead(path, config), config);
    }

    @SuppressWarnings("resource") // the returned reads are closed by the client
    @Override
    public List<Read<String>> multiRead(final FSPath item, final TableReadConfig<CSVTableReaderConfig> config)
        throws IOException {
        if (cannotParallelize(item, config)) {
            return List.of(read(item, config));
        }

        var csvConfig = config.getReaderSpecificConfig();
        var fileSize = Files.size(item);
        var maxNumChunks = csvConfig.getMaxNumChunksPerFile();
        var minChunkSize = csvConfig.getMinChunkSizeInBytes();
        var numChunks = findNumberOfChunks(fileSize, maxNumChunks, minChunkSize);
        if (numChunks == 1) {
            return List.of(read(item, config));
        }
        var chunkSize = fileSize / numChunks;
        if (fileSize % numChunks > 0) {
            // if fileSize is not divisible by numChunks, then each chunk would actually need
            // to read a fraction of a byte more, which in the worst case can lead to rows not being read
            // hence we increase the individual chunkSize by 1 byte to ensure that everything is read
            chunkSize++;
        }
        var reads = new ArrayList<Read<String>>(numChunks);
        for (int i = 0; i < numChunks; i++) {//NOSONAR
            Read<String> read = new ParallelCsvRead(item, i * chunkSize, chunkSize, config);
            read = ReadUtils.decorateAllowShortRows(read, config);
            read = ReadUtils.decorateSkipEmpty(read, config);
            reads.add(read);
        }
        return reads;
    }

    private static int findNumberOfChunks(final long fileSize, final int maxNumChunks, final long minChunkSize) {
        var numChunksWithMinSize = fileSize / minChunkSize;
        return (int)Math.max(1, Math.min(maxNumChunks, numChunksWithMinSize));

    }

    private static boolean cannotParallelize(final FSPath item, final TableReadConfig<CSVTableReaderConfig> config) {
        var csvConfig = config.getReaderSpecificConfig();
        return !isLocalPath(item)
                || FileCompressionUtils.mightBeCompressed(item)//
                || config.limitRows()//
                || config.skipRows()//
                || !csvConfig.noRowDelimitersInQuotes()//
                || csvConfig.skipLines()//
                || StandardCharsets.UTF_16.equals(getCharset(csvConfig));
    }

    @Override
    public boolean canBeReadInParallel(final SourceGroup<FSPath> sourceGroup) {
        return isLocalPath(sourceGroup.iterator().next());
    }

    /**
     * Checks if a FSPath is on this machine.
     *
     * @param path to check for being a local path
     * @return true if the path is located on the local machine
     */
    private static boolean isLocalPath(final FSPath path) {
        var fsType = path.toFSLocation().getFSType();
        return fsType == FSType.LOCAL_FS//
                || fsType == FSType.RELATIVE_TO_WORKFLOW//
                || fsType == FSType.RELATIVE_TO_WORKFLOW_DATA_AREA;
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
    public static Read<String> read(final InputStream inputStream,
        final TableReadConfig<CSVTableReaderConfig> config) throws IOException {
        final var read = new CsvRead(inputStream, config);
        return decorateForSequentialReading(read, config);
    }

    @Override
    public TypedReaderTableSpec<Class<?>> readSpec(final FSPath path,
        final TableReadConfig<CSVTableReaderConfig> config, final ExecutionMonitor exec) throws IOException {
        final TableSpecGuesser<FSPath, Class<?>, String> guesser = createGuesser(config.getReaderSpecificConfig());
        try (final var read = new CsvRead(path, config)) {
            return guesser.guessSpec(read, config, exec, path);
        }
    }

    private static TableSpecGuesser<FSPath, Class<?>, String> createGuesser(final CSVTableReaderConfig config) {
        return new TableSpecGuesser<>(CSVGuessableType.createHierarchy(config), Function.identity());
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
    private static Read<String> decorateForSequentialReading(final CsvRead read,
        final TableReadConfig<CSVTableReaderConfig> config) {
        Read<String> decorated = read;
        final boolean hasColumnHeader = config.useColumnHeaderIdx();
        final boolean skipRows = config.skipRows();
        if (skipRows) {
            final long numRowsToSkip = config.getNumRowsToSkip();
            decorated = ReadUtils.skip(decorated, hasColumnHeader ? (numRowsToSkip + 1) : numRowsToSkip);
        }
        if (config.limitRows()) {
            final long numRowsToKeep = config.getMaxRows();
            // in case we skip rows, we already skipped the column header
            // otherwise we have to read one more row since the first is the column header
            decorated = ReadUtils.limit(decorated, hasColumnHeader && !skipRows ? (numRowsToKeep + 1) : numRowsToKeep);
        }
        return ReadUtils.decorateForReading(decorated, config);
    }

    private static final class ParallelCsvRead implements Read<String> {

        private final ChunkReader m_reader;

        private final CsvParser m_parser;

        private final ErrorHandler m_errorParser;

        private final long m_limit;

        private boolean m_skipRow;

        private boolean m_parsingStarted;

        private final Reader m_parserReader;

        ParallelCsvRead(final FSPath path, final long offset, final long numBytesToRead,
            final TableReadConfig<CSVTableReaderConfig> config) throws IOException {
            m_limit = numBytesToRead;
            // when we read an offset, we likely start reading in the middle of a row, therefore we skip this partial
            // row
            m_skipRow = offset > 0 || config.useColumnHeaderIdx();
            CSVTableReaderConfig csvReaderConfig = config.getReaderSpecificConfig();
            m_errorParser = new ErrorHandler(csvReaderConfig.getCsvSettings());
            var csvSettings = csvReaderConfig.getCsvSettings();
            m_reader = createReader(path, offset, numBytesToRead, csvReaderConfig);
            m_parserReader = decorateForReading(m_reader, csvReaderConfig, csvSettings);
            // has to happen after the line separator is potentially changed by decorateForReading
            m_parser = new CsvParser(csvSettings);
            // parsing is started on the first next call because it directly starts reading from the channel
        }

        // csvSettings have to passed separately because CSVTableReadConfig#getCSVSettings clones them
        // and the line separator change has to happen in the settings that are used to create the parser
        private static Reader decorateForReading(final Reader reader, final CSVTableReaderConfig config,
            final CsvParserSettings csvSettings) {
            if (config.useLineBreakRowDelimiter()) {
                csvSettings.getFormat().setLineSeparator(OSIndependentNewLineReader.LINE_BREAK);
                return new BufferedReader(new OSIndependentNewLineReader(reader));
            } else {
                return new BufferedReader(reader);
            }
        }

        @SuppressWarnings("resource") // the channel is closed by ChunkReader
        private static ChunkReader createReader(final FSPath path, final long offset, final long bytesToRead,
            final CSVTableReaderConfig config) throws IOException {
            var charset = getCharset(config);
            var channel = Files.newByteChannel(path, StandardOpenOption.READ);
            if (offset == 0) {
                // skip the ByteOrderMark if it is present
                BomEncodingUtils.skipBom(channel, charset);
            } else {
                channel.position(offset);
            }
            return new ChunkReader(channel, charset, bytesToRead, config.getLineSeparator());
        }

        @Override
        public RandomAccessible<String> next() throws IOException {
            if (!m_parsingStarted) {
                m_parsingStarted = true;
                m_parser.beginParsing(m_parserReader);
            }
            try {
                if (m_skipRow) {
                    m_skipRow = false;
                    m_parser.parseNext();
                }
                var row = m_parser.parseNext();
                return row == null ? null : RandomAccessibleUtils.createFromArrayUnsafe(row);
            } catch (TextParsingException ex) {
                throw m_errorParser.parse(ex);
            }
        }

        @Override
        public OptionalLong getMaxProgress() {
            return OptionalLong.of(m_limit);
        }

        @Override
        public long getProgress() {
            return m_reader.getByteCount();
        }

        @Override
        public void close() throws IOException {
            m_parser.stopParsing();
            m_reader.close();
        }

        @Override
        public boolean needsDecoration() {
            return false;
        }

    }

    private static Charset getCharset(final CSVTableReaderConfig config) {
        final String charSetName = config.getCharSetName();
        return charSetName == null ? Charset.defaultCharset() : Charset.forName(charSetName);
    }

    private static final class ErrorHandler {

        private static final NodeLogger LOGGER = NodeLogger.getLogger(ErrorHandler.class);

        private static final Pattern INDEX_EXTRACTION_PATTERN =
                Pattern.compile("Index (\\d+) out of bounds for length \\d+");

        private final CsvParserSettings m_csvParserSettings;

        ErrorHandler(final CsvParserSettings csvParserSettings) {
            m_csvParserSettings = csvParserSettings;
        }

        IOException parse(final TextParsingException e) {
          //Log original exception message
            LOGGER.debug(e.getMessage(), e);
            final Throwable cause = e.getCause();
            if (cause instanceof ArrayIndexOutOfBoundsException) {
                final String message = cause.getMessage();
                //Exception handling in case maxCharsPerCol or maxCols are exceeded like in the AbstractParser
                final int index = extractErrorIndex(message);
                // for some reason when running in non-debug mode the memory limit per column exception often
                // contains a null message
                if (index == m_csvParserSettings.getMaxCharsPerColumn() || message == null) {
                    return new IOException("Memory limit per column exceeded. Please adapt the according setting.",
                        e);
                } else if (index == m_csvParserSettings.getMaxColumns()) {
                    return new IOException("Number of parsed columns exceeds the defined limit ("
                        + m_csvParserSettings.getMaxColumns() + "). Please adapt the according setting.", e);
                } else {
                    // fall through to default exception
                }
            }
            return new IOException(
                "Something went wrong during the parsing process. For further details please have a look into "
                    + "the log.",
                e);
        }

        private static int extractErrorIndex(final String message) {
            if (message != null) {
                final var matcher = INDEX_EXTRACTION_PATTERN.matcher(message);
                if (matcher.find()) {
                    try {
                        return Integer.parseInt(matcher.group(1));
                    } catch (NumberFormatException ex) {
                        LOGGER.debug("Can't parse the matched number.", ex);
                    }
                }
            }
            return -1;
        }
    }

    /**
     * Implements {@link Read} specific to CSV table reader, based on univocity's {@link CsvParser}.
     *
     * @author Temesgen H. Dadi, KNIME GmbH, Berlin, Germany
     */
    private static final class CsvRead implements Read<String> {

        private final ErrorHandler m_errorParser;

        /** a parser used to parse the file */
        private final CsvParser m_parser;

        /** the reader reading from m_countingStream */
        private final BufferedReader m_reader;

        /** the size of the file being read */
        private final long m_size;

        /** The {@link CompressionAwareCountingInputStream} which creates the necessary streams */
        private final CompressionAwareCountingInputStream m_compressionAwareStream;

        /**
         * Constructor
         *
         * @param path the path of the file to read
         * @param config the CSV table reader configuration.
         * @throws IOException if a stream can not be created from the provided file.
         */
        @SuppressWarnings("resource") // The input stream is closed by the close method
        CsvRead(final FSPath path, final TableReadConfig<CSVTableReaderConfig> config) throws IOException {
            this(new CompressionAwareCountingInputStream(path), Files.size(path), config);//NOSONAR
        }

        /**
         * Constructor
         *
         * @param inputStream the {@link InputStream} to read from
         * @param config the CSV table reader configuration.
         * @throws IOException if a stream can not be created from the provided file.
         */
        @SuppressWarnings("resource") //streams will be closed in the close method
        CsvRead(final InputStream inputStream, final TableReadConfig<CSVTableReaderConfig> config) throws IOException {
            this(new CompressionAwareCountingInputStream(inputStream), -1, config);
        }

        private CsvRead(final CompressionAwareCountingInputStream inputStream, final long size,
            final TableReadConfig<CSVTableReaderConfig> config) throws IOException {
            m_size = size;
            m_compressionAwareStream = inputStream;

            final CSVTableReaderConfig csvReaderConfig = config.getReaderSpecificConfig();
            var csvSettings = csvReaderConfig.getCsvSettings();
            m_errorParser = new ErrorHandler(csvSettings);
            m_reader = createReader(csvReaderConfig, csvSettings, m_compressionAwareStream);
            if (csvReaderConfig.skipLines()) {
                skipLines(csvReaderConfig.getNumLinesToSkip());
            }
            m_parser = new CsvParser(csvSettings);
            m_parser.beginParsing(m_reader);
        }

        @SuppressWarnings("resource")
        private static BufferedReader createReader(final CSVTableReaderConfig csvReaderConfig,
            // csvSettings need to be passed separately because CSVTableReaderConfig.getCsvSettings clones them
            final CsvParserSettings csvSettings, final InputStream stream) {
            final var charset = getCharset(csvReaderConfig);
            if (csvReaderConfig.useLineBreakRowDelimiter()) {
                csvSettings.getFormat().setLineSeparator(OSIndependentNewLineReader.LINE_BREAK);
                return new BufferedReader(
                    new OSIndependentNewLineReader(BomEncodingUtils.createReader(stream, charset)));
            } else {
                return BomEncodingUtils.createBufferedReader(stream, charset);
            }
        }

        @Override
        public RandomAccessible<String> next() throws IOException {
            String[] row = null;
            try {
                row = m_parser.parseNext();
            } catch (final TextParsingException e) {
                throw m_errorParser.parse(e);
            }
            return row == null ? null : RandomAccessibleUtils.createFromArrayUnsafe(row);
        }

        @Override
        public void close() throws IOException {
            m_parser.stopParsing();
            // the parser should already close the reader and the streams but we close them anyway just to be sure
            m_reader.close();
            m_compressionAwareStream.close();
        }

        @Override
        public OptionalLong getMaxProgress() {
            return m_size < 0 ? OptionalLong.empty() : OptionalLong.of(m_size);
        }

        @Override
        public long getProgress() {
            return m_compressionAwareStream.getCount();
        }

        /**
         * Skips n lines from m_countingStream. The method supports different newline schemes (\n \r \r\n)
         *
         * @param n the number of lines to skip
         * @throws IOException if reading from the stream fails
         */
        private void skipLines(final long n) throws IOException {
            for (var i = 0; i < n; i++) {
                m_reader.readLine(); //NOSONAR
            }
        }

        @Override
        public boolean needsDecoration() {
            return false;
        }

    }

}
