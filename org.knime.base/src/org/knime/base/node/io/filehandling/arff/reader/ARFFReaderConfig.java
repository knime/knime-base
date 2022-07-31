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

import java.nio.charset.Charset;

import org.knime.filehandling.core.node.table.reader.config.ReaderSpecificConfig;

import com.univocity.parsers.csv.CsvParserSettings;

/**
 * {@link ReaderSpecificConfig} for the ARFF reader node.
 *
 * Here we keep the configurations specific to this reader node.
 *
 * @author Dragan Keselj, Redfield SE
 */
final class ARFFReaderConfig implements ReaderSpecificConfig<ARFFReaderConfig> {
    public static final char COMMENT_CHARACTER = '%';

    /** Setting used to parse ARFF files */
    private CsvParserSettings m_arffParserSettings;

    /** Setting used to store the character set name (encoding) */
    private String m_charSet = Charset.defaultCharset().name();

    /** Setting to toggle between default OS independent line breaks and custom line breaks. */
    private boolean m_useLineBreakRowDel = true;

    /** Setting used to decide whether or not lines are skipped at the beginning */
    private boolean m_skipLines = false;

    /** Setting used to decide how many lines are skipped at the beginning */
    private long m_numLinesToSkip = 0L;

    /**
     * Constructor.
     */
    ARFFReaderConfig() {
        m_arffParserSettings = new CsvParserSettings();
        m_arffParserSettings.setEmptyValue("");
        setReplaceEmptyWithMissing(true);
    }

    private ARFFReaderConfig(final ARFFReaderConfig toCopy) {
        m_arffParserSettings =  toCopy.m_arffParserSettings.clone();
        setSkipLines(toCopy.skipLines());
        setNumLinesToSkip(toCopy.getNumLinesToSkip());
        setCharSetName(toCopy.getCharSetName());
    }

    public CsvParserSettings getArffParserSettings() {
        return m_arffParserSettings;
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
     * Sets whether empty strings within quotes should be replaced by a missing value or left as they are.
     *
     * @param replaceByMissingVal flag that decides if empty strings should be replaced by a missing value
     */
    public void setReplaceEmptyWithMissing(final boolean replaceByMissingVal) { //NOSONAR
        getArffParserSettings().setEmptyValue(replaceByMissingVal ? null : "");
    }

    @Override
    public ARFFReaderConfig copy() {
        return new ARFFReaderConfig(this);
    }



}
