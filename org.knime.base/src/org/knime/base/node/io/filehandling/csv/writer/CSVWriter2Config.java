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
package org.knime.base.node.io.filehandling.csv.writer;

import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.stream.Stream;

import org.apache.commons.lang.StringUtils;
import org.knime.base.node.io.filehandling.csv.writer.config.AdvancedConfig;
import org.knime.base.node.io.filehandling.csv.writer.config.CommentConfig;
import org.knime.base.node.io.filehandling.csv.writer.config.LineBreakTypes;
import org.knime.base.node.io.filereader.FileReaderSettings;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.context.ports.PortsConfiguration;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.util.CheckUtils;
import org.knime.filehandling.core.defaultnodesettings.filechooser.writer.FileOverwritePolicy;
import org.knime.filehandling.core.defaultnodesettings.filechooser.writer.SettingsModelWriterFileChooser;
import org.knime.filehandling.core.defaultnodesettings.filtermode.SettingsModelFilterMode.FilterMode;
import org.knime.filehandling.core.util.SettingsUtils;

/**
 * Configuration for the CSV writer node
 *
 * @author Temesgen H. Dadi, KNIME GmbH, Berlin, Germany (re-factored)
 */
final class CSVWriter2Config {

    /** The allowed/recommended suffixes for writing a CSV file */
    protected final String[] FILE_SUFFIXES = new String[]{".csv", ".tsv", ".txt", ".csv.gz", ".tsv.gz", ".txt.gz"};

    static final String CFG_SETTINGS_TAB = "settings";

    private static final String CFG_ADVANCED = "advanced_settings";

    private static final String CFG_COMMENT = "comment_header_settings";

    private static final String CFG_ENCODING = "encoding";

    /** The settings key for the file chooser dialog */
    public static final String CFG_FILE_CHOOSER = "file_chooser_settings";

    private static final String CFG_COLUMN_DELIMITER = "column_delimiter";

    private static final String CFG_QUOTE_CHAR = "quote_char";

    private static final String CFG_QUOTE_ESCAPE_CHAR = "quote_escape_char";

    private static final String CFG_WRITE_COLUMN_HEADER = "write_column_header";

    private static final String CFG_SKIP_COLUMN_HEADER_ON_APPEND = "skip_column_header_on_append";

    private static final String CFG_WRITE_ROW_HEADER = "write_row_header";

    private static final String CFG_CHAR_ENCODING = "character_set";

    private static final String DEFAULT_COLUMN_DELIMITER = ",";

    private static final char DEFAULT_QUOTE_CHAR = '"';

    private static final char DEFAULT_QUOTE_ESCAPE_CHAR = '"';

    private static final boolean DEFAULT_WRITE_COLUMN_HEADER = true;

    private static final boolean DEFAULT_SKIP_COLUMN_HEADER_ON_APPEND = false;

    private static final boolean DEFAULT_WRITE_ROW_HEADER = false;

    private static final String DEFAULT_CHAR_ENCODING = null;

    private final SettingsModelWriterFileChooser m_fileChooserModel;

    private boolean m_writeColumnHeader;

    private boolean m_skipColumnHeaderOnAppend;

    private boolean m_writeRowHeader;

    private String m_columnDelimiter;

    private LineBreakTypes m_lineBreak;

    private char m_quoteChar;

    private char m_quoteEscapeChar;

    private final AdvancedConfig m_advancedConfig;

    private final CommentConfig m_commentConfig;

    private String m_charsetName;

    /**
     * Constructor.
     *
     * @param portsConfig the ports configuration
     *
     */
    public CSVWriter2Config(final PortsConfiguration portsConfig) {
        m_fileChooserModel = new SettingsModelWriterFileChooser(CFG_FILE_CHOOSER, portsConfig,
            CSVWriter2NodeFactory.CONNECTION_INPUT_PORT_GRP_NAME, FilterMode.FILE,
            org.knime.filehandling.core.defaultnodesettings.filechooser.writer.FileOverwritePolicy.FAIL,
            EnumSet.of(FileOverwritePolicy.FAIL, FileOverwritePolicy.OVERWRITE, FileOverwritePolicy.APPEND),
            FILE_SUFFIXES);

        m_columnDelimiter = DEFAULT_COLUMN_DELIMITER;
        m_lineBreak = LineBreakTypes.SYS_DEFAULT;

        m_quoteChar = DEFAULT_QUOTE_CHAR;
        m_quoteEscapeChar = DEFAULT_QUOTE_ESCAPE_CHAR;

        m_writeColumnHeader = DEFAULT_WRITE_COLUMN_HEADER;
        m_skipColumnHeaderOnAppend = DEFAULT_SKIP_COLUMN_HEADER_ON_APPEND;
        m_writeRowHeader = DEFAULT_WRITE_ROW_HEADER;

        m_advancedConfig = new AdvancedConfig();
        m_commentConfig = new CommentConfig();

        m_charsetName = DEFAULT_CHAR_ENCODING;
    }

    String[] getLocationKeyChain() {
        return Stream.concat(Stream.of(CFG_SETTINGS_TAB), Arrays.stream(m_fileChooserModel.getKeysForFSLocation()))
            .toArray(String[]::new);
    }

    public void validateSettingsForModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_fileChooserModel.validateSettings(settings.getNodeSettings(CFG_SETTINGS_TAB));

        validateSettingsTab(settings.getNodeSettings(CFG_SETTINGS_TAB));

        m_advancedConfig.validate(settings.getNodeSettings(CFG_ADVANCED));
        m_commentConfig.validate(settings.getNodeSettings(CFG_COMMENT));

        settings.getNodeSettings(CFG_ENCODING).getString(CFG_CHAR_ENCODING);
    }

    private static void validateSettingsTab(final NodeSettingsRO settings) throws InvalidSettingsException {
        settings.getString(CFG_COLUMN_DELIMITER);
        LineBreakTypes.loadSettings(settings);

        settings.getChar(CFG_QUOTE_CHAR);
        settings.getChar(CFG_QUOTE_ESCAPE_CHAR);

        settings.getBoolean(CFG_WRITE_COLUMN_HEADER);
        settings.getBoolean(CFG_SKIP_COLUMN_HEADER_ON_APPEND);
        settings.getBoolean(CFG_WRITE_ROW_HEADER);
    }

    void loadSettingsForDialog(final NodeSettingsRO settings, final PortObjectSpec[] specs) {

        try {
            loadSettingsTabForDialog(settings.getNodeSettings(CFG_SETTINGS_TAB));
        } catch (InvalidSettingsException e) {
        }

        m_advancedConfig.loadInDialog(getConfigForDialog(settings, CFG_ADVANCED));
        m_commentConfig.loadInDialog(getConfigForDialog(settings, CFG_COMMENT));
        m_charsetName = getConfigForDialog(settings, CFG_ENCODING).getString(CFG_CHAR_ENCODING, DEFAULT_CHAR_ENCODING);
    }

    private void loadSettingsTabForDialog(final NodeSettingsRO settings) {
        try {
            m_lineBreak = LineBreakTypes.loadSettings(settings);
        } catch (InvalidSettingsException e) {
            // nothing to do
        }
        m_columnDelimiter = settings.getString(CFG_COLUMN_DELIMITER, DEFAULT_COLUMN_DELIMITER);

        m_quoteChar = settings.getChar(CFG_QUOTE_CHAR, DEFAULT_QUOTE_CHAR);
        m_quoteEscapeChar = settings.getChar(CFG_QUOTE_ESCAPE_CHAR, DEFAULT_QUOTE_ESCAPE_CHAR);

        m_writeColumnHeader = settings.getBoolean(CFG_WRITE_COLUMN_HEADER, DEFAULT_WRITE_COLUMN_HEADER);
        m_skipColumnHeaderOnAppend =
            settings.getBoolean(CFG_SKIP_COLUMN_HEADER_ON_APPEND, DEFAULT_SKIP_COLUMN_HEADER_ON_APPEND);
        m_writeRowHeader = settings.getBoolean(CFG_WRITE_ROW_HEADER, DEFAULT_WRITE_ROW_HEADER);
    }

    static NodeSettingsRO getConfigForDialog(final NodeSettingsRO settings, final String configKey) {
        try {
            return settings.getNodeSettings(configKey);
        } catch (InvalidSettingsException ex) {
            return new NodeSettings(configKey);
        }
    }

    void saveSettingsForDialog(final NodeSettingsWO settings) throws InvalidSettingsException {
        save(settings);
    }

    void loadSettingsForModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_fileChooserModel.loadSettingsFrom(settings.getNodeSettings(CFG_SETTINGS_TAB));
        loadSettingsTab(settings.getNodeSettings(CFG_SETTINGS_TAB));

        m_advancedConfig.loadInModel(settings.getNodeSettings(CFG_ADVANCED));
        m_commentConfig.loadInModel(settings.getNodeSettings(CFG_COMMENT));

        m_charsetName = settings.getNodeSettings(CFG_ENCODING).getString(CFG_CHAR_ENCODING);

        checkColSeparator();
        checkGzipSettings();
    }

    private void loadSettingsTab(final NodeSettingsRO settings) throws InvalidSettingsException {

        m_columnDelimiter = settings.getString(CFG_COLUMN_DELIMITER);
        m_lineBreak = LineBreakTypes.loadSettings(settings);

        m_quoteChar = settings.getChar(CFG_QUOTE_CHAR);
        m_quoteEscapeChar = settings.getChar(CFG_QUOTE_ESCAPE_CHAR);

        m_writeColumnHeader = settings.getBoolean(CFG_WRITE_COLUMN_HEADER);
        m_skipColumnHeaderOnAppend = settings.getBoolean(CFG_SKIP_COLUMN_HEADER_ON_APPEND);
        m_writeRowHeader = settings.getBoolean(CFG_WRITE_ROW_HEADER);
    }

    void saveSettingsForModel(final NodeSettingsWO settings) {
        m_fileChooserModel.saveSettingsTo(SettingsUtils.getOrAdd(settings, CFG_SETTINGS_TAB));

        save(settings);
    }

    private void save(final NodeSettingsWO settings) {
        saveSettingsTab(SettingsUtils.getOrAdd(settings, CFG_SETTINGS_TAB));

        m_advancedConfig.save(settings.addNodeSettings(CFG_ADVANCED));
        m_commentConfig.save(settings.addNodeSettings(CFG_COMMENT));

        settings.addNodeSettings(CFG_ENCODING).addString(CFG_CHAR_ENCODING, m_charsetName);
    }

    private void saveSettingsTab(final NodeSettingsWO settings) {
        settings.addString(CFG_COLUMN_DELIMITER, m_columnDelimiter);

        m_lineBreak.saveSettings(settings);

        settings.addChar(CFG_QUOTE_CHAR, m_quoteChar);
        settings.addChar(CFG_QUOTE_ESCAPE_CHAR, m_quoteEscapeChar);

        settings.addBoolean(CFG_WRITE_COLUMN_HEADER, m_writeColumnHeader);
        settings.addBoolean(CFG_SKIP_COLUMN_HEADER_ON_APPEND, m_skipColumnHeaderOnAppend);
        settings.addBoolean(CFG_WRITE_ROW_HEADER, m_writeRowHeader);
    }

    private void checkColSeparator() throws InvalidSettingsException {
        final String colSep = m_columnDelimiter;
        final String misVal = m_advancedConfig.getMissingValuePattern();
        final String commentBegin = m_commentConfig.getCommentLineMarker();

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

    private void checkGzipSettings() throws InvalidSettingsException {
        if (m_advancedConfig.compressWithGzip()
            && FileOverwritePolicy.APPEND == getFileChooserModel().getFileOverwritePolicy()) {
            throw new InvalidSettingsException("Can't append to existing file if output is gzip compressed");
        }
    }

    /**
     * @return a newly created {@link FileReaderSettings} with the a charSet similar to the current object
     */
    FileReaderSettings getEncodingSettings() {
        FileReaderSettings s = new FileReaderSettings();
        s.setCharsetName(getCharsetName());
        return s;
    }

    /**
     * @return {@code true} if the column separator is similar to or contains the decimal separator.
     */
    boolean colSeparatorContainsDecSeparator() {
        return (m_columnDelimiter.indexOf(m_advancedConfig.getDecimalSeparator()) >= 0);
    }

    /**
     * @return {@code true} if the column header should be written
     */
    boolean writeColumnHeader() {
        return m_writeColumnHeader;
    }

    /**
     * @param writeColumnHeader flag indicating if the column header should be written
     */
    void setWriteColumnHeader(final boolean writeColumnHeader) {
        m_writeColumnHeader = writeColumnHeader;
    }

    /**
     * @return the column delimiter value
     */
    String getColumnDelimiter() {
        return m_columnDelimiter;
    }

    /**
     * @param columnDelimiter the column delimiter value to set
     */
    void setColumnDelimiter(final String columnDelimiter) {
        CheckUtils.checkArgument(columnDelimiter.length() > 0, "The column delimiter can not be Empty");
        m_columnDelimiter = columnDelimiter;
    }

    /**
     * @return the skipColumnHeaderOnAppend {@code true} if the column header writing should be skipped if we are
     *         appending to an existing file
     */
    boolean skipColumnHeaderOnAppend() {
        return m_skipColumnHeaderOnAppend;
    }

    /**
     * @param skipColumnHeaderOnAppend flag deciding if the column header writing should be skipped if we are appending
     *            to an existing file
     */
    void setSkipColumnHeaderOnAppend(final boolean skipColumnHeaderOnAppend) {
        m_skipColumnHeaderOnAppend = skipColumnHeaderOnAppend;
    }

    /**
     * @return {@code true} if the row header should be written
     */
    boolean writeRowHeader() {
        return m_writeRowHeader;
    }

    /**
     * @param writeRowHeader flag indicating if the row header should be written
     */
    void setWriteRowHeader(final boolean writeRowHeader) {
        m_writeRowHeader = writeRowHeader;
    }

    /**
     * @return the character used for quoting values
     */
    char getQuoteChar() {
        return m_quoteChar;
    }

    /**
     * @param quoteChar the character to be used for quoting values
     */
    void setQuoteChar(final char quoteChar) {
        m_quoteChar = quoteChar;
    }

    /**
     * @param str a String containing the character to be used for quoting values
     */
    void setQuoteChar(final String str) {
        setQuoteChar(getFirstChar(str, "Quote Character"));

    }

    /**
     * @return the character used for escaping quotes inside an already quoted value
     */
    char getQuoteEscapeChar() {
        return m_quoteEscapeChar;
    }

    /**
     * @param str a String containing the character used for escaping quotes inside an already quoted value
     */
    void setQuoteEscapeChar(final String str) {
        setQuoteEscapeChar(getFirstChar(str, "Quote Escape Character"));
    }

    /**
     * @param quoteEscapeChar the character used for escaping quotes inside an already quoted value
     */
    void setQuoteEscapeChar(final char quoteEscapeChar) {
        m_quoteEscapeChar = quoteEscapeChar;
    }

    /**
     * Character set - or null if default encoding should be used.
     *
     * @return character set - or null if default encoding should be used.
     */
    String getCharsetName() {
        return m_charsetName;
    }

    /**
     * Character set - or null if default encoding should be used.
     *
     * @return character set - or null if default encoding should be used.
     */
    Charset getCharSet() {
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
    void setCharSetName(final String charSet) {
        if (charSet == null || Charset.isSupported(charSet)) {
            m_charsetName = charSet;
        } else {
            throw new UnsupportedCharsetException(charSet);
        }
    }

    /**
     * @return the file chooser settings model of this configuration
     */
    SettingsModelWriterFileChooser getFileChooserModel() {
        return m_fileChooserModel;
    }

    /**
     * @return a {@link AdvancedConfig} containing advanced configurations
     */
    AdvancedConfig getAdvancedConfig() {
        return m_advancedConfig;
    }

    /**
     * @return a {@link CommentConfig} containing the comment header writing related configurations
     */
    CommentConfig getCommentConfig() {
        return m_commentConfig;
    }

    /**
     * @return the line break variant used
     */
    LineBreakTypes getLineBreak() {
        return m_lineBreak;
    }

    /**
     * @param lineBreak the line break variant to set
     */
    void setLineBreak(final LineBreakTypes lineBreak) {
        m_lineBreak = lineBreak;
    }

    /**
     * @return {@code true} if existing file should be overwritten
     */
    final boolean isFileOverwritten() {
        return getFileChooserModel().getFileOverwritePolicy() == FileOverwritePolicy.OVERWRITE;
    }

    /**
     * @return {@code true} if existing file should be appended to
     */
    final boolean isFileAppended() {
        return getFileChooserModel().getFileOverwritePolicy() == FileOverwritePolicy.APPEND;
    }

    /**
     * @return {@code true} if existing file should neither be overwritten nor appended to
     */
    final boolean isFileNeverOverwritten() {
        return getFileChooserModel().getFileOverwritePolicy() == FileOverwritePolicy.FAIL;
    }

    /**
     * After removing non-visible white space characters line '\0', it returns the first character from a string. If the
     * provided string is empty it returns '\0'. If the provided string has more than 2 chars, an error will be
     * displayed.
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
