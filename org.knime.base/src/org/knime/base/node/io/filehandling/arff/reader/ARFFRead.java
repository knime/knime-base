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
 *   02.07.2022. (Dragan Keselj): created
 */
package org.knime.base.node.io.filehandling.arff.reader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.OptionalLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.text.StringTokenizer;
import org.apache.commons.text.matcher.StringMatcherFactory;
import org.knime.base.node.io.filehandling.csv.reader.OSIndependentNewLineReader;
import org.knime.core.node.NodeLogger;
import org.knime.filehandling.core.connections.FSPath;
import org.knime.filehandling.core.node.table.reader.config.TableReadConfig;
import org.knime.filehandling.core.node.table.reader.randomaccess.RandomAccessible;
import org.knime.filehandling.core.node.table.reader.randomaccess.RandomAccessibleUtils;
import org.knime.filehandling.core.node.table.reader.read.Read;
import org.knime.filehandling.core.util.BomEncodingUtils;
import org.knime.filehandling.core.util.CompressionAwareCountingInputStream;

import com.univocity.parsers.common.TextParsingException;
import com.univocity.parsers.csv.CsvParser;

/**
 * Class for the ARFF reader which implements {@link Read}. We read as {@link String} tokens.
 *
 * @author Dragan Keselj, Redfield SE
 *
 */
final class ARFFRead implements Read<String> {

    private static final char[] TIME_PATTERN_LETTERS = {'h', 'H', 'm', 's', 'S', 'k', 'K', 'a', 'A', 'n', 'N'};

    private static final char[] ZONE_PATTERN_LETTERS = {'z', 'Z', 'v', 'V', 'O', 'x', 'X'};

    private static final Pattern INDEX_EXTRACTION_PATTERN =
        Pattern.compile("Index (\\d+) out of bounds for length \\d+");

    /**
     * Default format used to create <code>DataCell</code> in <code>ZonedDateTimeCellFactory</code>
     **/
    private static final DateTimeFormatter DEFAULT_ZONED_DATE_TIME_FORMAT = new DateTimeFormatterBuilder()
        .appendPattern("yyyy-MM-dd'T'HH:mm:ssXXX'['zzzz']'").toFormatter(Locale.getDefault());

    /**
     * Default format used to create <code>DataCell</code> in <code>LocalDateTimeCellFactory</code>
     **/
    private static final DateTimeFormatter DEFAULT_DATE_TIME_FORMAT =
        new DateTimeFormatterBuilder().appendPattern("yyyy-MM-dd'T'HH:mm:ss").toFormatter(Locale.getDefault());

    /**
     * Default format used to create <code>DataCell</code> in <code>LocalDateCellFactory</code>
     **/
    private static final DateTimeFormatter DEFAULT_DATE_FORMAT =
        new DateTimeFormatterBuilder().appendPattern("yyyy-MM-dd").toFormatter(Locale.getDefault());

    private static final NodeLogger LOGGER = NodeLogger.getLogger(ARFFRead.class);

    private final ARFFSpec m_spec;

    /** the size of the file being read */
    private final long m_size;

    private final ARFFReaderConfig m_readerConfig;

    /** The {@link CompressionAwareCountingInputStream} which creates the necessary streams */
    private final CompressionAwareCountingInputStream m_compressionAwareStream;

    /** the reader reading from m_countingStream */
    private final BufferedReader m_reader;

    /** a parser used to parse the file */
    private final CsvParser m_parser;

    /** a tokenizer for reading Sparse ARFF file data */
    private static final StringTokenizer m_sparseTokenizer = new StringTokenizer();
    static {
        m_sparseTokenizer.setDelimiterMatcher(StringMatcherFactory.INSTANCE.spaceMatcher()) //
            .setQuoteMatcher(StringMatcherFactory.INSTANCE.quoteMatcher());
    }

    /**
     * Constructor
     *
     * @param path the path of the file to read
     * @param config ARFF table reader configuration.
     * @throws IOException if a stream can not be created from the provided file.
     */
    @SuppressWarnings("resource") // The input stream is closed by the close method
    ARFFRead(final FSPath path, final TableReadConfig<ARFFReaderConfig> config) throws IOException {
        this(new CompressionAwareCountingInputStream(path), Files.size(path), config);//NOSONAR
    }

    /**
     * Constructor
     *
     * @param inputStream the {@link InputStream} to read from
     * @param config ARFF table reader configuration.
     * @throws IOException if a stream can not be created from the provided file.
     */
    @SuppressWarnings("resource") //streams will be closed in the close method
    ARFFRead(final InputStream inputStream, final TableReadConfig<ARFFReaderConfig> config) throws IOException {
        this(new CompressionAwareCountingInputStream(inputStream), -1, config);
    }

    private ARFFRead(final CompressionAwareCountingInputStream inputStream, final long size,
        final TableReadConfig<ARFFReaderConfig> config) throws IOException {
        m_size = size;
        m_compressionAwareStream = inputStream;

        m_readerConfig = config.getReaderSpecificConfig();

        m_reader = createReader();
        if (m_readerConfig.skipLines()) {
            skipLines(m_readerConfig.getNumLinesToSkip());
        }

        m_spec = new ARFFSpec(m_reader);

        m_parser = new CsvParser(m_readerConfig.getArffParserSettings());
        m_parser.beginParsing(m_reader);
    }

    @SuppressWarnings("resource")
    private BufferedReader createReader() {
        final String charSetName = m_readerConfig.getCharSetName();
        final Charset charset = charSetName == null ? Charset.defaultCharset() : Charset.forName(charSetName);
        if (m_readerConfig.useLineBreakRowDelimiter()) {
            m_readerConfig.getArffParserSettings().getFormat().setLineSeparator(OSIndependentNewLineReader.LINE_BREAK);
            return ignoreCommentsReader(
                new OSIndependentNewLineReader(BomEncodingUtils.createReader(m_compressionAwareStream, charset)));
        } else {
            return ignoreCommentsReader(BomEncodingUtils.createBufferedReader(m_compressionAwareStream, charset));
        }
    }

    private static BufferedReader ignoreCommentsReader(final Reader reader) {
        return new BufferedReader(reader) {
            @Override
            public String readLine() throws IOException {
                final String line = super.readLine();
                if (line == null) {
                    return null;
                }
                if (StringUtils.isBlank(line) || line.trim().charAt(0) == ARFFReaderConfig.COMMENT_CHARACTER) {
                    return "";
                }
                //ignore text after comment char
                String[] linePieces = StringUtils.split(line, ARFFReaderConfig.COMMENT_CHARACTER);
                if (linePieces == null || linePieces.length == 0
                    || Arrays.stream(linePieces).allMatch(StringUtils::isBlank)) {
                    return "";
                } else {
                    return linePieces[0].trim();
                }
            }
        };
    }

    @Override
    public RandomAccessible<String> next() throws IOException {
        String[] row = null;
        try {
            row = m_parser.parseNext();
            if (isSparse(row)) {
                row = parseSparseRow(row, m_spec.getAttributes().size());
            }
            setDatesToDefaultFormat(row);
        } catch (final TextParsingException e) {
            //Log original exception message
            LOGGER.debug(e.getMessage(), e);
            final Throwable cause = e.getCause();
            if (cause instanceof ArrayIndexOutOfBoundsException) {
                final String message = cause.getMessage();
                //Exception handling in case maxCharsPerCol or maxCols are exceeded like in the AbstractParser
                final int index = extractErrorIndex(message);
                // for some reason when running in non-debug mode the memory limit per column exception often
                // contains a null message
                if (index == m_readerConfig.getArffParserSettings().getMaxCharsPerColumn() || message == null) {
                    throw new IOException("Memory limit per column exceeded. Please adapt the according setting.", e);
                } else if (index == m_readerConfig.getArffParserSettings().getMaxColumns()) {
                    throw new IOException("Number of parsed columns exceeds the defined limit ("
                        + m_readerConfig.getArffParserSettings().getMaxColumns()
                        + "). Please adapt the according setting.", e);
                } else {
                    // fall through to default exception
                }
            }
            throw new IOException(
                "Something went wrong during the parsing process. For further details please have a look into "
                    + "the log.",
                e);

        }
        return row == null ? null : RandomAccessibleUtils.createFromArrayUnsafe(row);
    }

    /**
     * @param row
     * @return
     */
    private static String[] parseSparseRow(final String[] row, final int size) {
        if (row == null) {
            return null; //NOSONAR
        }
        final var sparseRow = new String[size];

        final var firstValue = StringUtils.stripStart(row[0], null);
        final var lastValue = StringUtils.stripEnd(row[row.length - 1], null);

        row[0] = firstValue == null ? null : firstValue.substring(1, firstValue.length());
        row[row.length - 1] = lastValue.substring(0, lastValue.length() - 1);
        for (var i=0; i<row.length; i++) {
            if (StringUtils.isBlank(row[i])) {
                continue;
            }
            m_sparseTokenizer.reset(row[i]);
            final String[] tokens = m_sparseTokenizer.getTokenArray();
            if (tokens.length != 2) {
                throw new TextParsingException(null, tokens + " Format for each entry must be: [index] [space] [value]");
            }
            var index = -1;
            try {
                index = Integer.parseInt(tokens[0]);
            } catch (NumberFormatException e) {
                throw new TextParsingException(null, tokens[0] + " Column index is not integer.");
            }
            final String value = StringUtils.equals(tokens[1], "?") ? null : tokens[1];
            sparseRow[index] = value;
        }
        return sparseRow;
    }

    /**
     * @param row
     * @return
     * @throws ParseException
     */
    private static boolean isSparse(final String[] row) {
        if (row != null && row.length > 0 && row[0].charAt(0) == '{') {
            final var lastValue = StringUtils.trim(row[row.length - 1]);
            if (lastValue == null || (lastValue.charAt(lastValue.length() - 1) != '}')) {
                throw new TextParsingException(null, "'}' is missing at the end of the row.");
            }
            return true;
        }
        return false;
    }

    /**
     * Reads date/time cells with specified format and then converts it into KNIME default date/time format used in
     * date/time <code>DataCell</code> factory classes.
     *
     * @param row
     */
    private void setDatesToDefaultFormat(final String[] row) {
        if (row == null) {
            return;
        }
        for (Entry<Integer, Pair<String, DateTimeFormatter>> col : m_spec.getFormattedDateColumns().entrySet()) {
            final int index = col.getKey();
            if (StringUtils.isBlank(row[index])) {
                row[index] = null;
                continue;
            }
            final Pair<String, DateTimeFormatter> format = col.getValue();
            final var pattern = format.getLeft();
            final var formatter = format.getRight();
            var reformatted = "";
            if (StringUtils.containsAny(pattern, ZONE_PATTERN_LETTERS)) {
                reformatted = ZonedDateTime.parse(row[index], formatter).format(DEFAULT_ZONED_DATE_TIME_FORMAT);
            } else {
                if (StringUtils.containsAny(pattern, TIME_PATTERN_LETTERS)) {
                    reformatted = LocalDateTime.parse(row[index], formatter).format(DEFAULT_DATE_TIME_FORMAT);
                } else {
                    reformatted = LocalDate.parse(row[index], formatter).format(DEFAULT_DATE_FORMAT);
                }
            }
            row[index] = reformatted;
        }
    }

    private static int extractErrorIndex(final String message) {
        if (message != null) {
            final Matcher matcher = INDEX_EXTRACTION_PATTERN.matcher(message); //NOSONAR
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

    public ARFFSpec getSpec() {
        return m_spec;
    }

    /**
     * Skips n lines from m_countingStream. The method supports different newline schemes (\n \r \r\n)
     *
     * @param n the number of lines to skip
     * @throws IOException if reading from the stream fails
     */
    private void skipLines(final long n) throws IOException {
        for (int i = 0; i < n; i++) { //NOSONAR
            m_reader.readLine(); //NOSONAR
        }
    }

}