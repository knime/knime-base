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
 *   Apr 29, 2020 (Temesgen H. Dadi, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.base.node.io.filehandling.csv.writer.config;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.knime.base.node.io.filehandling.csv.writer.LineBreakTypes;
import org.knime.base.node.io.filereader.FileReaderSettings;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.util.CheckUtils;
import org.knime.filehandling.core.defaultnodesettings.SettingsModelFileChooser2;

/**
 * Settings configuration for the CSV writer node
 *
 * @author ohl, University of Konstanz
 * @author Temesgen H. Dadi, KNIME GmbH, Berlin, Germany (re-factored)
 */
public class CSVWriter2Config {

    /**
     * Policy how to proceed when output file exists (overwrite, abort, append).
     *
     */
    public enum FileOverwritePolicy {
            /** Overwrite existing file. */
            OVERWRITE,
            /** Append table content to existing file. */
            APPEND, //
            /** Fail during execution. Neither overwrite nor append. */
            ABORT;
    }

    /** The allowed/recommended suffixes for writing a CSV file */
    public static final String[] FILE_SUFFIXES = new String[]{".csv", ".tsv", ".txt", ".csv.gz", ".tsv.gz", ".txt.gz"};

    /** The settings key for the file chooser dialog */
    public static final String CFG_FILE_CHOOSER = "file_chooser_settings";

    private static final String CFG_OVERWRITE_POLICY = "overwrite_policy";

    private static final String CFG_CREATE_PARENT_DIRECTORY = "create_parent_directory";

    private static final String CFG_COLUMN_DELIMITER = "column_delimiter";

    private static final String CFG_LINE_ENDING_MODE = "line_ending_mode";

    private static final String CFG_QUOTE_CHAR = "quote_char";

    private static final String CFG_QUOTE_ESCAPE_CHAR = "quote_escape_char";

    private static final String CFG_WRITE_COLUMN_HEADER = "write_column_header";

    private static final String CFG_SKIP_COLUMN_HEADER_ON_APPEND = "skip_column_header_on_append";

    private static final String CFG_WRITE_ROW_HEADER = "write_row_header";

    private static final String CFG_CHAR_ENCODING = "character_set";

    private final SettingsModelFileChooser2 m_fileChooserModel;

    private FileOverwritePolicy m_fileOverwritePolicy;

    private boolean m_createParentDirectoryIfRequired;

    private boolean m_writeColumnHeader;

    private boolean m_skipColumnHeaderOnAppend;

    private boolean m_writeRowHeader;

    private String m_columnDelimeter;

    private LineBreakTypes m_lineBreak;

    private char m_quoteChar;

    private char m_quoteEscapeChar;

    private AdvancedConfig m_advancedConfig;

    private CommentConfig m_commentConfig;

    private String m_charsetName;

    /**
     * Default constructor
     */
    public CSVWriter2Config() {
        m_fileChooserModel = new SettingsModelFileChooser2(CFG_FILE_CHOOSER, FILE_SUFFIXES);
        m_fileOverwritePolicy = FileOverwritePolicy.ABORT;
        m_createParentDirectoryIfRequired = false;

        m_columnDelimeter = ",";
        m_lineBreak = LineBreakTypes.SYST_DEFAULT;

        m_quoteChar = '"';
        m_quoteEscapeChar = '"';

        m_writeColumnHeader = false;
        m_skipColumnHeaderOnAppend = false;
        m_writeRowHeader = false;

        m_advancedConfig = new AdvancedConfig();
        m_commentConfig = new CommentConfig();

        m_charsetName = null;
    }

    /**
     * Copy constructor
     */
    public CSVWriter2Config(final CSVWriter2Config source) {
        m_fileChooserModel = source.getFileChooserModel();
        m_fileOverwritePolicy = source.getFileOverwritePolicy();
        m_createParentDirectoryIfRequired = source.createParentDirectoryIfRequired();

        m_columnDelimeter = source.getColumnDelimeter();
        m_lineBreak = source.getLineBreak();

        m_quoteChar = source.getQuoteChar();
        m_quoteEscapeChar = source.getQuoteEscapeChar();

        m_writeColumnHeader = source.writeColumnHeader();
        m_skipColumnHeaderOnAppend = source.skipColumnHeaderOnAppend();
        m_writeRowHeader = source.writeRowHeader();

        m_advancedConfig = source.getAdvancedConfig();
        m_commentConfig = source.getCommentConfig();

        m_charsetName = source.getCharsetName();
    }

    /**
     * Checks if settings are valid and can be read.
     *
     * @param settings
     * @throws InvalidSettingsException if the value(s) in the settings object are invalid.
     */
    public void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_fileChooserModel.validateSettings(settings);
        settings.getString(CFG_OVERWRITE_POLICY);
        settings.getBoolean(CFG_CREATE_PARENT_DIRECTORY);

        settings.getString(CFG_COLUMN_DELIMITER);
        settings.getString(CFG_LINE_ENDING_MODE);

        settings.getChar(CFG_QUOTE_CHAR);
        settings.getChar(CFG_QUOTE_ESCAPE_CHAR);

        settings.getBoolean(CFG_WRITE_COLUMN_HEADER);
        settings.getBoolean(CFG_SKIP_COLUMN_HEADER_ON_APPEND);
        settings.getBoolean(CFG_WRITE_ROW_HEADER);

        m_advancedConfig.validateSettings(settings);
        m_commentConfig.validateSettings(settings);

        settings.getString(CFG_CHAR_ENCODING);
    }

    /**
     * Reads settings and load them into this configuration object.
     *
     * @param settings
     * @throws InvalidSettingsException if the value(s) in the settings object are invalid.
     */
    public void loadInModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_fileChooserModel.loadSettingsFrom(settings);
        m_fileOverwritePolicy = FileOverwritePolicy.valueOf(settings.getString(CFG_OVERWRITE_POLICY));
        m_createParentDirectoryIfRequired = settings.getBoolean(CFG_CREATE_PARENT_DIRECTORY);

        m_columnDelimeter = settings.getString(CFG_COLUMN_DELIMITER);
        m_lineBreak = LineBreakTypes.valueOf(settings.getString(CFG_LINE_ENDING_MODE));

        m_quoteChar = settings.getChar(CFG_QUOTE_CHAR);
        m_quoteEscapeChar = settings.getChar(CFG_QUOTE_ESCAPE_CHAR);

        m_writeColumnHeader = settings.getBoolean(CFG_WRITE_COLUMN_HEADER);
        m_skipColumnHeaderOnAppend = settings.getBoolean(CFG_SKIP_COLUMN_HEADER_ON_APPEND);
        m_writeRowHeader = settings.getBoolean(CFG_WRITE_ROW_HEADER);

        m_advancedConfig.loadSettingsFrom(settings);
        m_commentConfig.loadSettingsFrom(settings);

        m_charsetName = settings.getString(CFG_CHAR_ENCODING);

        checkColSeparator();
        checkCommentSettings();
        checkGzipSettings();
    }

    /**
     * Read the value(s) of this settings model from configuration object for the purpose of loading them into node
     * dialog. Default values are used if the key to a specific setting is not found.
     *
     * @param settings the configuration object
     * @throws NotConfigurableException if the sub-setting can not be extracted.
     */
    public void loadInDialog(final NodeSettingsRO settings) throws NotConfigurableException {
        try {
            m_fileChooserModel.loadSettingsFrom(settings);
        } catch (final InvalidSettingsException ex) {
            throw new NotConfigurableException(ex.getMessage());
        }
        m_fileOverwritePolicy =
            FileOverwritePolicy.valueOf(settings.getString(CFG_OVERWRITE_POLICY, FileOverwritePolicy.ABORT.name()));

        m_createParentDirectoryIfRequired = settings.getBoolean(CFG_CREATE_PARENT_DIRECTORY, false);

        m_columnDelimeter = settings.getString(CFG_COLUMN_DELIMITER, ",");
        m_lineBreak =
            LineBreakTypes.valueOf(settings.getString(CFG_LINE_ENDING_MODE, LineBreakTypes.SYST_DEFAULT.name()));

        m_quoteChar = settings.getChar(CFG_QUOTE_CHAR, '"');
        m_quoteEscapeChar = settings.getChar(CFG_QUOTE_ESCAPE_CHAR, '"');

        m_writeColumnHeader = settings.getBoolean(CFG_WRITE_COLUMN_HEADER, false);
        m_skipColumnHeaderOnAppend = settings.getBoolean(CFG_SKIP_COLUMN_HEADER_ON_APPEND, false);
        m_writeRowHeader = settings.getBoolean(CFG_WRITE_ROW_HEADER, false);

        m_advancedConfig.loadSettingsForDialog(settings);
        m_commentConfig.loadSettingsForDialog(settings);

        m_charsetName = settings.getString(CFG_CHAR_ENCODING, null);
    }

    /**
     * Write value(s) of this settings model to configuration object.
     *
     * @param settings The {@link org.knime.core.node.NodeSettings} to write into.
     */
    public void saveSettingsTo(final NodeSettingsWO settings) {
        m_fileChooserModel.saveSettingsTo(settings);
        settings.addString(CFG_OVERWRITE_POLICY, m_fileOverwritePolicy.name());
        settings.addBoolean(CFG_CREATE_PARENT_DIRECTORY, m_createParentDirectoryIfRequired);

        settings.addString(CFG_COLUMN_DELIMITER, m_columnDelimeter);
        settings.addString(CFG_LINE_ENDING_MODE, m_lineBreak.name());

        settings.addChar(CFG_QUOTE_CHAR, m_quoteChar);
        settings.addChar(CFG_QUOTE_ESCAPE_CHAR, m_quoteEscapeChar);

        settings.addBoolean(CFG_WRITE_COLUMN_HEADER, m_writeColumnHeader);
        settings.addBoolean(CFG_SKIP_COLUMN_HEADER_ON_APPEND, m_skipColumnHeaderOnAppend);
        settings.addBoolean(CFG_WRITE_ROW_HEADER, m_writeRowHeader);

        m_advancedConfig.saveSettingsTo(settings);
        m_commentConfig.saveSettingsTo(settings);

        settings.addString(CFG_CHAR_ENCODING, m_charsetName);

    }

    private void checkColSeparator() throws InvalidSettingsException {
        final String colSep = m_columnDelimeter;
        final String misVal = m_advancedConfig.getMissingValuePattern();
        final String commentBegin = m_commentConfig.getCommentBegin();

        // check consistency of settings
        // the column delimiter must not be contained in the missing value pattern
        // nor in the comment begin pattern.
        if (!StringUtils.isEmpty(colSep)) {
            if (!StringUtils.isEmpty(misVal) && misVal.contains(colSep)) {
                throw new InvalidSettingsException("The missing value replacement ('" + misVal
                    + "') must not contain the column delimiter ('" + colSep + "').");
            }

            if (!StringUtils.isEmpty(commentBegin) && commentBegin.contains(colSep)) {
                throw new InvalidSettingsException("The comment line start ('" + commentBegin
                    + "') must not contain the column delimiter ('" + colSep + "').");
            }
        } else {
            throw new InvalidSettingsException("The column delimiter can not be empty!");
        }
    }

    private void checkCommentSettings() throws InvalidSettingsException {
        // if we are supposed to add some creation data, we need to know the comment pattern
        // if the end pattern is empty, assume a single line comment and
        // write the comment begin pattern in every line.

        final String commBigin = m_commentConfig.getCommentBegin();
        final String commEnd = m_commentConfig.getCommentEnd();
        final String commText = m_commentConfig.getCustomText();
        if ((m_commentConfig.addCreationTime() || m_commentConfig.addCreationUser() || m_commentConfig.addTableName()
            || !StringUtils.isEmpty(commText)) && StringUtils.isEmpty(commBigin)) {
            throw new InvalidSettingsException(
                "The comment pattern must be defined in order to add user, creation date or table name");
        }

        // if a custom comment line is specified, is must not contain the comment end pattern
        if (!StringUtils.isEmpty(commText) && !StringUtils.isEmpty(commEnd) && commText.contains(commEnd)) {
            throw new InvalidSettingsException(
                "The specified comment to add must not contain the comment end pattern.");
        }
    }

    private void checkGzipSettings() throws InvalidSettingsException {
        boolean isAppend = FileOverwritePolicy.APPEND.equals(getFileOverwritePolicy());
        if (m_advancedConfig.compressWithGzip() && isAppend) {
            throw new InvalidSettingsException("Can't append to existing " + "file if output is gzip compressed");
        }
    }

    /**
     * @return a newly created {@link FileReaderSettings} with the a charSet similar to the current object
     */
    public FileReaderSettings getEncodingSettings() {
        FileReaderSettings s = new FileReaderSettings();
        s.setCharsetName(getCharsetName());
        return s;
    }

    /**
     * Writes a comment header to the file, if specified so in the settings.
     *
     * @param file the writer to write the header out to
     * @param tableName the name of input table being written
     * @param append If the output will be appended to an existing file
     * @throws IOException if something went wrong during writing
     */
    public void writeCommentHeader(final BufferedWriter file, final String tableName, final boolean append)
        throws IOException {
        m_commentConfig.writeCommentHeader(file, tableName, append);
    }

    /**
     * @return {@code true} if the column separator is similar to or contains the decimal separator.
     */
    public boolean colSeparatorContainsDecSeparator() {
        return (m_columnDelimeter.indexOf(m_advancedConfig.getDecimalSeparator()) >= 0);
    }


    /*
     * ----------------------------------------------------------------------
     * Setter and getter methods for all settings.
     * ----------------------------------------------------------------------
     */

    /**
     * @return {@code true} if the parent directory(ies) will be created if required.
     */
    public boolean createParentDirectoryIfRequired() {
        return m_createParentDirectoryIfRequired;
    }

    /**
     * @param createDir flag indicating, if the parent directory(ies) will be created if required
     */
    public void setCreateParentDirectory(final boolean createDir) {
        m_createParentDirectoryIfRequired = createDir;
    }

    /**
     * @return the writeColumnHeader
     */
    public boolean writeColumnHeader() {
        return m_writeColumnHeader;
    }

    /**
     * @param writeColumnHeader the writeColumnHeader to set
     */
    public void setWriteColumnHeader(final boolean writeColumnHeader) {
        m_writeColumnHeader = writeColumnHeader;
    }

    /**
     * @return the columnDelimeter
     */
    public String getColumnDelimeter() {
        return m_columnDelimeter;
    }

    /**
     * @param columnDelimeter the columnDelimeter to set
     */
    public void setColumnDelimeter(final String columnDelimeter) {
        CheckUtils.checkArgument(columnDelimeter.length() > 0, "The column delimiter can not be Empty");
        m_columnDelimeter = columnDelimeter;
    }

    /**
     * @return the skipColumnHeaderOnAppend
     */
    public boolean skipColumnHeaderOnAppend() {
        return m_skipColumnHeaderOnAppend;
    }

    /**
     * @param skipColumnHeaderOnAppend the skipColumnHeaderOnAppend to set
     */
    public void setSkipColumnHeaderOnAppend(final boolean skipColumnHeaderOnAppend) {
        m_skipColumnHeaderOnAppend = skipColumnHeaderOnAppend;
    }

    /**
     * @return the writeRowHeader
     */
    public boolean writeRowHeader() {
        return m_writeRowHeader;
    }

    /**
     * @param writeRowHeader the writeRowHeader to set
     */
    public void setWriteRowHeader(final boolean writeRowHeader) {
        m_writeRowHeader = writeRowHeader;
    }

    /**
     * @return the fileOverwritePolicy
     */
    public FileOverwritePolicy getFileOverwritePolicy() {
        return m_fileOverwritePolicy;
    }

    /**
     * @param fileOverwritePolicy the fileOverwritePolicy to set
     */
    public void setFileOverwritePolicy(final FileOverwritePolicy fileOverwritePolicy) {
        m_fileOverwritePolicy = fileOverwritePolicy;
    }

    /**
     * @return the quoteChar
     */
    public char getQuoteChar() {
        return m_quoteChar;
    }

    /**
     * @param quoteChar the quoteChar to set
     */
    public void setQuoteChar(final char quoteChar) {
        m_quoteChar = quoteChar;
    }

    /**
     * @param str a String containing the quoteChar to set
     */
    public void setQuoteChar(final String str) {
        setQuoteChar(getFirstChar(str, "Quote Character"));

    }

    /**
     * @param str a String containing the quoteChar to set
     */
    public void setQuoteEscapeChar(final String str) {
        setQuoteEscapeChar(getFirstChar(str, "Quote Escape Character"));
    }

    /**
     * @return the quoteEscapeChar
     */
    public char getQuoteEscapeChar() {
        return m_quoteEscapeChar;
    }

    /**
     * @param quoteEscapeChar the quoteEscapeChar to set
     */
    public void setQuoteEscapeChar(final char quoteEscapeChar) {
        m_quoteEscapeChar = quoteEscapeChar;
    }

    /**
     * Character set - or null if default encoding should be used.
     *
     * @return character set - or null if default encoding should be used.
     */
    public String getCharsetName() {
        return m_charsetName;
    }

    /**
     * Character set - or null if default encoding should be used.
     *
     * @return character set - or null if default encoding should be used.
     */
    public Charset getCharSet() {
        if (m_charsetName == null) {
            return Charset.defaultCharset();
        } else if (Charset.isSupported(m_charsetName)) {
            return Charset.forName(m_charsetName);
        } else {
            throw new UnsupportedCharsetException(m_charsetName);
        }
    }

    /**
     * The new charS set - can be null indicating default encoding.
     *
     * @param charSet or null.
     */
    public void setCharSetName(final String charSet) {
        if (charSet == null || Charset.isSupported(charSet)) {
            m_charsetName = charSet;
        } else {
            throw new UnsupportedCharsetException(charSet);
        }
    }

    /**
     * @return the file chooser settings model of this configuration
     */
    public SettingsModelFileChooser2 getFileChooserModel() {
        return m_fileChooserModel;
    }

    /**
     * @return the advanced configuration
     */
    public AdvancedConfig getAdvancedConfig() {
        return m_advancedConfig;
    }

    /**
     * @return the commentConfig
     */
    public CommentConfig getCommentConfig() {
        return m_commentConfig;
    }

    /**
     * @return the lineBreak
     */
    public LineBreakTypes getLineBreak() {
        return m_lineBreak;
    }

    /**
     * @return the index of the current lineBreak
     */
    public int getLineBreakIndex() {
        return ArrayUtils.indexOf(LineBreakTypes.lBreakTypes, m_lineBreak);
    }

    /**
     * @param lineBreak the lineBreak to set
     */
    public void setLineBreak(final LineBreakTypes lineBreak) {
        m_lineBreak = lineBreak;
    }

    /**
     * @param index the index of lineBreakType to set
     */
    public void setLineBreak(final int index) {
        m_lineBreak = LineBreakTypes.lBreakTypes[index];
    }

    /**
     * @return {@code true} if existing file should be overwritten
     */
    public final boolean isFileOverwritten() {
        return m_fileOverwritePolicy == FileOverwritePolicy.OVERWRITE;
    }

    /**
     * @return {@code true} if existing file should be appended to
     */
    public final boolean isFileAppended() {
        return m_fileOverwritePolicy == FileOverwritePolicy.APPEND;
    }

    /**
     * @return {@code true} if existing file should neither be overwritten nor appended to
     */
    public final boolean isFileNeverOverwritten() {
        return m_fileOverwritePolicy == FileOverwritePolicy.ABORT;
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
    static char getFirstChar(final String str, final String fieldName) {
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
