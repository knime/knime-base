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
 *   6 Apr 2020 (Temesgen H. Dadi, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.base.node.io.filehandling.csv.reader.api;

import java.nio.charset.Charset;

import org.knime.core.node.util.CheckUtils;
import org.knime.filehandling.core.node.table.reader.config.ReaderSpecificConfig;

import com.univocity.parsers.csv.CsvFormat;
import com.univocity.parsers.csv.CsvParser;
import com.univocity.parsers.csv.CsvParserSettings;

/**
 * An implementation of {@link ReaderSpecificConfig} class for CSV table reader.
 *
 * @author Temesgen H. Dadi, KNIME GmbH, Berlin, Germany
 * @since 4.2
 */
public final class CSVTableReaderConfig implements ReaderSpecificConfig<CSVTableReaderConfig> {

    /** The default quote option. */
    private static final QuoteOption DEFAULT_QUOTE_OPTION = QuoteOption.REMOVE_QUOTES_AND_TRIM;

    /**
     * According to the javadoc a value of -1 allows for auto-expansion of the array which indicates that this value
     * defines the size of the buffer array hence setting a very large value might cause memory problems. Current
     * default equals 1MB (char has at most 2 bytes).
     *
     * @see CsvParserSettings#setMaxCharsPerColumn(int)
     */
    private static final int DEFAULT_MAX_CHARS_PER_COLUMN = 1024 * (1024 / 2);

    /**
     * The default number of characters used for csv format autodetection
     */
    public static final int DEFAULT_AUTODETECTION_BUFFER_SIZE = 1024 * 1024;

    /**
     * The default maximum number of columns to parse.
     *
     * @see CsvParserSettings#setMaxColumns(int)
     */
    public static final int DEFAULT_MAX_COLUMNS = 8192;

    /** Setting used to parse csv files */
    private final CsvParserSettings m_settings;

    /** Setting to toggle between default OS independent line breaks and custom line breaks. */
    private boolean m_useLineBreakRowDel;

    /** Setting used to decide whether or not lines are skipped at the beginning */
    private boolean m_skipLines = false;

    /** Setting used to decide how many lines are skipped at the beginning */
    private long m_numLinesToSkip = 1L;

    /** Setting used to store the character set name (encoding) */
    private String m_charSet = Charset.defaultCharset().name();

    /** Setting used to store the buffer size (autodetection) */
    private int m_bufferSize;

    private QuoteOption m_quoteOption;

    /** If true enables parallel reading of CSV files. Added in 5.1.0. Default is false **/
    private boolean m_noLineSeparatorsInQuotes;

    private char m_thousandsSeparator = '\0';

    private char m_decimalSeparator = '.';

    private int m_maxNumChunksPerFile = Runtime.getRuntime().availableProcessors() / 2;

    // defaults to 64MB
    private long m_minChunkSizeInBytes = 1L << 26;

    /**
     * Constructor.
     */
    public CSVTableReaderConfig() {
        m_settings = new CsvParserSettings();
        m_settings.setEmptyValue("");
        m_settings.setMaxCharsPerColumn(DEFAULT_MAX_CHARS_PER_COLUMN);
        m_settings.setMaxColumns(DEFAULT_MAX_COLUMNS);
        useLineBreakRowDelimiter(true);
        setQuoteOption(DEFAULT_QUOTE_OPTION);
        setReplaceEmptyWithMissing(true);
        limitCharsPerColumn(true);
        setMaxColumns(DEFAULT_MAX_COLUMNS);
        setAutoDetectionBufferSize(DEFAULT_AUTODETECTION_BUFFER_SIZE);
    }

    private CSVTableReaderConfig(final CSVTableReaderConfig toCopy) {
        m_settings = toCopy.m_settings.clone();
        useLineBreakRowDelimiter(toCopy.useLineBreakRowDelimiter());
        setSkipLines(toCopy.skipLines());
        setNumLinesToSkip(toCopy.getNumLinesToSkip());
        setCharSetName(toCopy.getCharSetName());
        setQuoteOption(toCopy.getQuoteOption());
        setAutoDetectionBufferSize(toCopy.getAutoDetectionBufferSize());
        m_decimalSeparator = toCopy.getDecimalSeparatorChar();
        m_thousandsSeparator = toCopy.getThousandsSeparatorChar();
    }

    /**
     * Returns the stored parser settings used by univocity's {@link CsvParser}.
     *
     * @return the parser settings used
     */
    CsvParserSettings getCsvSettings() {
        return getSettings().clone();
    }

    private CsvParserSettings getSettings() {
        return m_settings;
    }

    /**
     *
     * @return the CSV reader format
     */
    private CsvFormat getFormat() {
        return getSettings().getFormat();
    }

    /**
     * Returns whether or not line breaks are being used as row delimiter.
     *
     * @return {@code true} if line breaks define the row delimiter and {@code false} otherwise
     */
    public boolean useLineBreakRowDelimiter() {
        return m_useLineBreakRowDel;
    }

    /**
     * Specifies whether line breaks are the row delimiters or not.
     *
     * @param useLinebreakRowDel {@code true} if line breaks define the row delimiter and {@code false} otherwise
     */
    public void useLineBreakRowDelimiter(final boolean useLinebreakRowDel) {
        m_useLineBreakRowDel = useLinebreakRowDel;
    }

    /**
     * Defines the column delimiter string.
     *
     * @param delimiter the column delimiter string from the node dialog.
     */
    public void setDelimiter(final String delimiter) {
        getFormat().setDelimiter(delimiter);
    }

    /**
     * Gets the delimiter string.
     *
     * @return the delimiter string
     */
    public String getDelimiter() {
        return getFormat().getDelimiterString();
    }

    /**
     * Sets the line separator used to define rows or records.
     *
     * @param lineSeparator the line separator used
     */
    public void setLineSeparator(final String lineSeparator) {
        // AP-15964: CsvFormat#setLineSeparator with String#length == 1 changes the normalized new line char causing
        // different behavior when calling this method several times with varying lineSeperators
        if (lineSeparator.length() == 2) {
            getFormat().setNormalizedNewline('\n');
        }
        getFormat().setLineSeparator(lineSeparator);
    }

    /**
     * Gets the line separator used to define rows or records.
     *
     * @return the line separator used to define rows
     */
    public String getLineSeparator() {
        return getFormat().getLineSeparatorString();
    }

    /**
     * Gets the character used as quotes enclosed in a String.
     *
     * @return the character used as quotes
     */
    public String getQuote() {
        return Character.toString(getFormat().getQuote());
    }

    /**
     * Sets the character used as quotes by the parser
     *
     * @param quoteChar a string containing a character used as quotes .
     */
    public void setQuote(final String quoteChar) {
        getFormat().setQuote(getFirstChar(quoteChar, "Quote character"));
    }

    /**
     * Gets the character used for escaping quotes inside an already quoted value enclosed in a String.
     *
     * @return a string containing the character used for escaping quotes
     */
    public String getQuoteEscape() {
        return Character.toString(getFormat().getQuoteEscape());
    }

    /**
     * Sets the character used for escaping quotes inside an already quoted value.
     *
     * @param quoteEscapeChar a string containing a character used for escaping quotes
     */
    public void setQuoteEscape(final String quoteEscapeChar) {
        getFormat().setQuoteEscape(getFirstChar(quoteEscapeChar, "Quote escape character"));
    }

    /**
     * Gets the character used for commenting a line enclosed in a String.
     *
     * @return a string containing the character used for commenting a line
     */
    public String getComment() {
        return Character.toString(getFormat().getComment());
    }

    /**
     * Sets the character used for commenting a line enclosed in a String.
     *
     * @param commentChar string containing the character used for commenting a line
     */
    public void setComment(final String commentChar) {
        getFormat().setComment(getFirstChar(commentChar, "Comment character"));
    }

    /**
     * Gets the number of lines that are skipped at the beginning.
     *
     * @return the number of lines skipped at the beginning of the files,
     */
    public long getNumLinesToSkip() {
        return m_numLinesToSkip;
    }

    /**
     * Checks whether or not skipping a certain number of lines is enforced.
     *
     * @return <code>true</code> if lines are to be skipped
     */
    public boolean skipLines() {
        return m_skipLines;
    }

    /**
     * Sets the flag on whether or not a certain number of lines are skipped at the beginning.
     *
     * @param selected flag indicating whether or not line skipping is enforced
     */
    public void setSkipLines(final boolean selected) {
        m_skipLines = selected;
    }

    /**
     * Configures the number of lines that should be skipped. Used only when m_skipLines is set to <code>truw</code>.
     *
     * @param numLinesToSkip the number of lines that will be skipped
     */
    public void setNumLinesToSkip(final long numLinesToSkip) {
        m_numLinesToSkip = numLinesToSkip;
    }

    /**
     * Sets whether empty strings within quotes should be replaced by a missing value or left as they are.
     *
     * @param replaceByMissingVal flag that decides if empty strings should be replaced by a missing value
     */
    public void setReplaceEmptyWithMissing(final boolean replaceByMissingVal) {
        getSettings().setEmptyValue(replaceByMissingVal ? null : "");
    }

    /**
     * Gets whether empty strings within quotes are being replaced by a missing value or left as they are.
     *
     * @return {@code true} if empty strings within quotes are being replaced.
     */
    public boolean replaceEmptyWithMissing() {
        return getSettings().getEmptyValue() == null;
    }

    /**
     * Sets the quote selected to the {@link CsvParserSettings}.
     *
     * @param quoteOption the {@link QuoteOption} to apply to the {@link CsvParserSettings}
     */
    public void setQuoteOption(final QuoteOption quoteOption) {
        m_quoteOption = quoteOption;
        getSettings().setKeepQuotes(m_quoteOption.keepQuotes());
        getSettings().trimQuotedValues(m_quoteOption.trimQuotedValues());
    }

    /**
     * Returns the {@link QuoteOption}.
     *
     * @return the {@link QuoteOption}
     */
    public QuoteOption getQuoteOption() {
        return m_quoteOption;
    }

    /**
     * @return true if there are no line separators inside of quotes.
     * @since 5.1
     */
    public boolean noRowDelimitersInQuotes() {
        return m_noLineSeparatorsInQuotes;
    }

    /**
     * @param noRowDelimitersInQuotes whether quotes may contain line separators
     * @since 5.1
     */
    public void noRowDelimitersInQuotes(final boolean noRowDelimitersInQuotes) {
        m_noLineSeparatorsInQuotes = noRowDelimitersInQuotes;
    }

    /**
     * The default is the number of available processors.
     *
     * @return the maximum number of chunks to split a single file into
     * @since 5.1
     */
    public int getMaxNumChunksPerFile() {
        return m_maxNumChunksPerFile ;
    }

    /**
     * @param maxNumChunksPerFile the maximum number of chunks to split a single file into
     * @since 5.1
     */
    public void setMaxNumChunksPerFile(final int maxNumChunksPerFile) {
        m_maxNumChunksPerFile = maxNumChunksPerFile;
    }

    /**
     * The default is 64MB.
     *
     * @return the minimum size of a chunk individual files are split into
     * @since 5.1
     */
    public long getMinChunkSizeInBytes() {
        return m_minChunkSizeInBytes;
    }

    /**
     * @param minChunkSizeInBytes the minimum size of a chunk individual files are split into
     * @since 5.1
     */
    public void setMinChunkSizeInBytes(final long minChunkSizeInBytes) {
        m_minChunkSizeInBytes = minChunkSizeInBytes;
    }

    /**
     * Gets the character set name (encoding) used to read files.
     *
     * @return the character set name (encoding), or <code>null</code> if the default character set should be used.
     */
    public String getCharSetName() {
        return m_charSet;
    }

    /**
     * Sets the character set name (encoding) used to read files.
     *
     * @param charSet the new character set name (encoding), or <code>null</code> if the default should be used.
     */
    public void setCharSetName(final String charSet) {
        m_charSet = charSet;
    }

    /**
     * Returns the hard limit of how many columns a row can have (defaults to 512).
     *
     * @return The maximum number of columns a row can have
     */
    public int getMaxColumns() {
        return getSettings().getMaxColumns();
    }

    /**
     * Defines a hard limit of how many columns a record can have (defaults to 512). You need this to avoid OutOfMemory
     * errors in case of inputs that might be inconsistent with the format you are dealing with.
     *
     * @param maxColumns The maximum number of columns a record can have.
     */
    public void setMaxColumns(final int maxColumns) {
        getSettings().setMaxColumns(maxColumns);
    }

    /**
     * Returns the flag indicating whether the number of columns is limited, or not.
     *
     * @return {@code true} if the number of characters per column is limited, {@code false} otherwise.
     */
    public boolean isCharsPerColumnLimited() {
        return getSettings().getMaxCharsPerColumn() != -1;
    }

    /**
     * Defines whether the number of chars per column is limited. This option allows to avoid memory exhaustion.
     *
     * @param limited The maximum number of characters allowed to be read.
     */
    public void limitCharsPerColumn(final boolean limited) {
        if (limited) {
            getSettings().setMaxCharsPerColumn(DEFAULT_MAX_CHARS_PER_COLUMN);
        } else {
            //enable auto expansion of the interal array by setting -1
            getSettings().setMaxCharsPerColumn(-1);
        }
    }

    /**
     * Gets the input buffer size used for the format auto detection
     *
     * @return the buffer size
     */
    public int getAutoDetectionBufferSize() {
        return m_bufferSize;
    }

    /**
     * Sets the input buffer size used for the format auto detection
     *
     * @param bufferSize the new buffer size
     */
    public void setAutoDetectionBufferSize(final int bufferSize) {
        m_bufferSize = bufferSize;
    }

    @Override
    public CSVTableReaderConfig copy() {
        return new CSVTableReaderConfig(this);
    }

    /**
     * After removing non-visible white space characters line '\0', it returns the first character from a string. The
     * provided If the provided string is empty it returns '\0'. If the provided string has more than 2 chars, an error
     * will be displayed.
     *
     * @param str the string input
     * @param fieldName the name of the field the string is coming from. Used to customize error message
     * @return the first character in input string if it is not empty, '\0' otherwise
     */
    private static char getFirstChar(final String str, final String fieldName) {
        if (str == null || str.isEmpty() || str.equals("\0")) {
            return '\0';
        } else {
            final String cleanStr = str.replace("\0", "");
            CheckUtils.checkArgument(cleanStr.length() < 2,
                "Only a single character is allowed for %s. Escape sequences, such as \\n can be used.", fieldName);
            return cleanStr.charAt(0);
        }
    }

    /**
     * @return the thousandsSeparator
     */
    public char getThousandsSeparatorChar() {
        return m_thousandsSeparator;
    }

    /**
     * @return the thousandsSeparator
     */
    public String getThousandsSeparator() {
        return Character.toString(m_thousandsSeparator);
    }

    /**
     * @param thousandsSeparator the thousandsSeparator to set
     */
    public void setThousandsSeparator(final String thousandsSeparator) {
        m_thousandsSeparator = getFirstChar(thousandsSeparator, "thousands separator");
    }

    /**
     * @return the decimalSeparator
     */
    public char getDecimalSeparatorChar() {
        return m_decimalSeparator;
    }

    /**
     * @return the decimalSeparator
     */
    public String getDecimalSeparator() {
        return Character.toString(m_decimalSeparator);
    }

    /**
     * @param decimalSeparator the decimalSeparator to set
     */
    public void setDecimalSeparator(final String decimalSeparator) {
        m_decimalSeparator = getFirstChar(decimalSeparator, "decimal separator");
    }

}
