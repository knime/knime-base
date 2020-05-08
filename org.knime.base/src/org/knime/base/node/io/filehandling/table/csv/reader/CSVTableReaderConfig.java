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
package org.knime.base.node.io.filehandling.table.csv.reader;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
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

    /**
     * According to the javadoc a value of -1 allows for auto-expansion of the array which indicates that this value
     * defines the size of the buffer array hence setting a very large value might cause memory problems
     */
    private static final int DEFAULT_MAX_CHARS_PER_COLUMN = -1;

    /**
     * The default maximum number of columns, used to instantiate an array in univocity.
     */
    private static final int DEFAULT_MAX_COLUMNS = 10000;

    /** string key used to save the value of column delimiter used to read csv files */
    private static final String CFG_DELIMITER = "column_delimiter";

    /** string key used to save the value of line separator used to read csv files */
    private static final String CFG_LINE_SEPARATOR = "line_separator";

    /** string key used to save the value of the character used as qoute */
    private static final String CFG_QUOTE_CHAR = "quote_char";

    /** string key used to save the value of the character used as qoute escape */
    private static final String CFG_QUOTE_ESCAPE_CHAR = "quote_escape_char";

    /** string key used to save the value of the character used as comment start */
    private static final String CFG_COMMENT_CHAR = "comment_char";

    /** string key used to save whether or not lines are skipped at the beginning */
    private static final String CFG_SKIP_LINES = "skip_lines";

    /** string key used to save whether or not empty lines are being skipped */
    private static final String CFG_SKIP_EMPTY_DATA_ROWS = "skip_empty_data_rows";

    /** string key used to save the value of number of lines that should be skipped */
    private static final String CFG_NUM_LINES_TO_SKIP = "num_lines_to_skip";

    /** string key used to save whether or not quoted empty strings are replaced by missing value */
    private static final String CFG_REPLACE_EMPTY_WITH_MISSING = "replace_empty_with_missing";

    /** string key used to save the character set name (encoding) */
    private static final String CFG_CHAR_SET_NAME = "character_set_name";

    /** Setting used to parse csv files */
    private final CsvParserSettings m_settings;

    /** Setting used to decide whether or not lines are skipped at the beginning */
    private boolean m_skipLines = false;

    /** Setting used to decide how many lines are skipped at the beginning */
    private long m_numLinesToSkip = 1L;

    /** Setting used to store the character set name (encoding) */
    private String m_charSet = null;

    /**
     * Constructor.
     */
    public CSVTableReaderConfig() {
        m_settings = new CsvParserSettings();
        m_settings.setEmptyValue("");
        m_settings.setSkipEmptyLines(true);
        m_settings.setMaxCharsPerColumn(DEFAULT_MAX_CHARS_PER_COLUMN);
        m_settings.setMaxColumns(DEFAULT_MAX_COLUMNS);
        setReplaceEmptyWithMissing(true);
    }

    /**
     * Gets the parser settings used by univocity's {@link CsvParser}.
     *
     * @return the parser settings used
     */
    CsvParserSettings getSettings() {
        return m_settings;
    }

    /**
     *
     * @return the CSV reader format
     */
    private CsvFormat getFormat() {
        return m_settings.getFormat();
    }

    /**
     * Defines the column delimiter character. User input should be either empty or a single character, or else an error
     * will be caused. In the case of empty string, the delimiter will be set to '\\0'.
     *
     * @param delimiter the column delimiter string from the node dialog.
     */
    public void setDelimiter(final String delimiter) {
        getFormat().setDelimiter(getFirstChar(delimiter, "Delimiter character"));
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
     * Checks whether or not empty lines are skipped.
     *
     * @return <code>true</code> if empty lines are being skipped
     */
    public boolean skipEmptyLines() {
        return getSettings().getSkipEmptyLines();
    }

    /**
     * Sets the flag on whether or not univocity's {@link CsvParser} should skip empty lines.
     *
     * @param selected flag indicating whether or not empty lines are skipped
     */
    public void setSkipEmptyLines(final boolean selected) {
        getSettings().setSkipEmptyLines(selected);
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

    @Override
    public void loadInDialog(final NodeSettingsRO settings) {
        setDelimiter(settings.getString(CFG_DELIMITER, ","));
        setLineSeparator(settings.getString(CFG_LINE_SEPARATOR, "\n"));
        setQuote(settings.getString(CFG_QUOTE_CHAR, "\""));
        setQuoteEscape(settings.getString(CFG_QUOTE_ESCAPE_CHAR, "\""));
        setComment(settings.getString(CFG_COMMENT_CHAR, "\0"));

        setSkipLines(settings.getBoolean(CFG_SKIP_LINES, false));
        setNumLinesToSkip(settings.getLong(CFG_NUM_LINES_TO_SKIP, 1L));

        setSkipEmptyLines(settings.getBoolean(CFG_SKIP_EMPTY_DATA_ROWS, true));

        setReplaceEmptyWithMissing(settings.getBoolean(CFG_REPLACE_EMPTY_WITH_MISSING, true));

        setCharSetName(settings.getString(CFG_CHAR_SET_NAME, null));
    }

    @Override
    public void loadInModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        setDelimiter(settings.getString(CFG_DELIMITER));
        setLineSeparator(settings.getString(CFG_LINE_SEPARATOR));

        setQuote(settings.getString(CFG_QUOTE_CHAR));
        setQuoteEscape(settings.getString(CFG_QUOTE_ESCAPE_CHAR));
        setComment(settings.getString(CFG_COMMENT_CHAR));

        setSkipLines(settings.getBoolean(CFG_SKIP_LINES));
        setNumLinesToSkip(settings.getLong(CFG_NUM_LINES_TO_SKIP));

        setSkipEmptyLines(settings.getBoolean(CFG_SKIP_EMPTY_DATA_ROWS));

        setReplaceEmptyWithMissing(settings.getBoolean(CFG_REPLACE_EMPTY_WITH_MISSING));

        setCharSetName(settings.getString(CFG_CHAR_SET_NAME));
    }

    @Override
    public void validate(final NodeSettingsRO settings) throws InvalidSettingsException {
        settings.getString(CFG_DELIMITER);
        settings.getString(CFG_LINE_SEPARATOR);

        settings.getString(CFG_QUOTE_CHAR);
        settings.getString(CFG_QUOTE_ESCAPE_CHAR);
        settings.getString(CFG_COMMENT_CHAR);

        settings.getLong(CFG_NUM_LINES_TO_SKIP);
        settings.getBoolean(CFG_SKIP_LINES);
        settings.getBoolean(CFG_SKIP_EMPTY_DATA_ROWS);

        settings.getBoolean(CFG_REPLACE_EMPTY_WITH_MISSING);

        settings.getString(CFG_CHAR_SET_NAME);

    }

    @Override
    public void save(final NodeSettingsWO settings) {
        settings.addString(CFG_DELIMITER, getDelimiter());
        settings.addString(CFG_LINE_SEPARATOR, getLineSeparator());

        settings.addString(CFG_QUOTE_CHAR, getQuote());
        settings.addString(CFG_QUOTE_ESCAPE_CHAR, getQuoteEscape());
        settings.addString(CFG_COMMENT_CHAR, getComment());

        settings.addLong(CFG_NUM_LINES_TO_SKIP, getNumLinesToSkip());
        settings.addBoolean(CFG_SKIP_LINES, skipLines());
        settings.addBoolean(CFG_SKIP_EMPTY_DATA_ROWS, skipEmptyLines());

        settings.addBoolean(CFG_REPLACE_EMPTY_WITH_MISSING, replaceEmptyWithMissing());

        settings.addString(CFG_CHAR_SET_NAME, getCharSetName());
    }

    @Override
    public CSVTableReaderConfig copy() {
        CSVTableReaderConfig configCopy = new CSVTableReaderConfig();
        configCopy.setDelimiter(this.getDelimiter());
        configCopy.setLineSeparator(this.getLineSeparator());

        configCopy.setQuote(this.getQuote());
        configCopy.setQuoteEscape(this.getQuoteEscape());
        configCopy.setComment(this.getComment());

        configCopy.setSkipLines(this.skipLines());
        configCopy.setNumLinesToSkip(this.getNumLinesToSkip());

        configCopy.setSkipEmptyLines(this.skipEmptyLines());

        configCopy.setReplaceEmptyWithMissing(this.replaceEmptyWithMissing());

        configCopy.setCharSetName(this.getCharSetName());

        return configCopy;
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
            CheckUtils.checkArgument(cleanStr.length() <= 2,
                "Only a single character is allowed for %s. Escape sequences, such as \\n can be used.", fieldName);
            return cleanStr.charAt(0);
        }
    }

}