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
 *   May 4, 2020 (Temesgen H. Dadi, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.base.node.io.filehandling.csv.writer.config;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 * Advanced setting configurations for CSV writer node
 *
 * @author Temesgen H. Dadi, KNIME GmbH, Berlin, Germany
 */
public final class AdvancedConfig implements SimpleConfig {

    /**
     * Different modes of putting values inside quotes
     *
     * @author Temesgen H. Dadi, KNIME GmbH, Berlin, Germany
     */
    public enum QuoteMode {
            /** use quotes only if a value contains a column separator */
            IF_NEEDED,
            /** use quotes always on non-numerical data. */
            STRINGS_ONLY,
            /** always put quotes around the data. */
            ALWAYS,
            /** don't use quotes, replace separator pattern in data. */
            NEVER
    }

    private static final String CFGKEY_MISSING_VALUE = "missing_value_pattern";

    private static final String CFGKEY_COMPRESS_WITH_GZIP = "compress_with_gzip";

    private static final String CFGKEY_QUOTE_MODE = "quote_mode";

    private static final String CFGKEY_SEPARATOR_REPL = "separator_replacement";

    private static final String CFGKEY_DEC_SEPARATOR = "decimal_separator";

    private static final String CFGKEY_SCIENTIFIC_FORMAT = "use_scientific_format";

    private static final String CFGKEY_KEEP_TRAILING_ZERO = "keep_trailing_zero_in_decimals";

    private String m_missingValuePattern;

    private String m_quoteModeName;

    private String m_separatorReplacement; // only used with mode=Never

    private char m_decimalSeparator;

    private boolean m_useScientificFormat;

    private boolean m_keepTrailingZero;

    private boolean m_compressWithGzip;

    /**
     * Default constructor
     */
    public AdvancedConfig() {
        m_missingValuePattern = "";
        m_compressWithGzip = false;
        m_quoteModeName = QuoteMode.IF_NEEDED.name();
        m_separatorReplacement = "";
        m_decimalSeparator = '.';
        m_useScientificFormat = false;
        m_keepTrailingZero = false;
    }

    @Override
    public void loadInDialog(final NodeSettingsRO settings) {
        m_missingValuePattern = settings.getString(CFGKEY_MISSING_VALUE, "");
        m_compressWithGzip = settings.getBoolean(CFGKEY_COMPRESS_WITH_GZIP, false);

        m_quoteModeName = settings.getString(CFGKEY_QUOTE_MODE, QuoteMode.IF_NEEDED.name());
        m_separatorReplacement = settings.getString(CFGKEY_SEPARATOR_REPL, "");

        m_decimalSeparator = settings.getChar(CFGKEY_DEC_SEPARATOR, '.');
        m_useScientificFormat = settings.getBoolean(CFGKEY_SCIENTIFIC_FORMAT, false);
        m_keepTrailingZero = settings.getBoolean(CFGKEY_KEEP_TRAILING_ZERO, false);
    }

    @Override
    public void loadInModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_missingValuePattern = settings.getString(CFGKEY_MISSING_VALUE);
        m_compressWithGzip = settings.getBoolean(CFGKEY_COMPRESS_WITH_GZIP);

        m_quoteModeName = settings.getString(CFGKEY_QUOTE_MODE);
        m_separatorReplacement = settings.getString(CFGKEY_SEPARATOR_REPL);

        m_decimalSeparator = settings.getChar(CFGKEY_DEC_SEPARATOR);
        m_useScientificFormat = settings.getBoolean(CFGKEY_SCIENTIFIC_FORMAT);
        m_keepTrailingZero = settings.getBoolean(CFGKEY_KEEP_TRAILING_ZERO);
    }

    @Override
    public void validate(final NodeSettingsRO settings) throws InvalidSettingsException {
        settings.getString(CFGKEY_MISSING_VALUE);
        settings.getBoolean(CFGKEY_COMPRESS_WITH_GZIP);

        settings.getString(CFGKEY_QUOTE_MODE);
        settings.getString(CFGKEY_SEPARATOR_REPL);

        settings.getChar(CFGKEY_DEC_SEPARATOR);
        settings.getBoolean(CFGKEY_SCIENTIFIC_FORMAT);
        settings.getBoolean(CFGKEY_KEEP_TRAILING_ZERO);
    }

    @Override
    public void save(final NodeSettingsWO settings) {
        settings.addString(CFGKEY_MISSING_VALUE, m_missingValuePattern);
        settings.addBoolean(CFGKEY_COMPRESS_WITH_GZIP, m_compressWithGzip);

        settings.addString(CFGKEY_QUOTE_MODE, m_quoteModeName);
        settings.addString(CFGKEY_SEPARATOR_REPL, m_separatorReplacement);

        settings.addChar(CFGKEY_DEC_SEPARATOR, m_decimalSeparator);
        settings.addBoolean(CFGKEY_SCIENTIFIC_FORMAT, m_useScientificFormat);
        settings.addBoolean(CFGKEY_KEEP_TRAILING_ZERO, m_keepTrailingZero);
    }

    /**
     * @return the String value used in place of missing values while writing.
     */
    public String getMissingValuePattern() {
        return m_missingValuePattern;
    }

    /**
     * @param missingValuePattern the value to be used in place of missing values while writing.
     */
    public void setMissingValuePattern(final String missingValuePattern) {
        m_missingValuePattern = missingValuePattern;
    }

    /**
     * @return {@code true} if file should be written with gzip compression
     */
    public boolean compressWithGzip() {
        return m_compressWithGzip;
    }

    /**
     * @param compressWithGzip a flag deciding if file should be written with gzip compression
     */
    public void setCompressWithGzip(final boolean compressWithGzip) {
        m_compressWithGzip = compressWithGzip;
    }

    /**
     * @return a {@link QuoteMode} that decides when to put values in quotes.
     */
    public QuoteMode getQuoteMode() {
        return QuoteMode.valueOf(m_quoteModeName);
    }

    /**
     * @return the name ({@link String}) of {@link QuoteMode} deciding when to put values in quotes.
     */
    public String getQuoteModeName() {
        return m_quoteModeName;
    }

    /**
     * @param quoteModeName the ({@link String}) name of {@link QuoteMode} deciding when to put values in quotes.
     */
    public void setQuoteModeName(final String quoteModeName) {
        m_quoteModeName = quoteModeName;
    }

    /**
     * @return the replacement text for the chars used as a column separator.
     */
    public String getSeparatorReplacement() {
        return m_separatorReplacement;
    }

    /**
     * @param separatorReplacement the replacement text for the chars used as a column separator.
     */
    public void setSeparatorReplacement(final String separatorReplacement) {
        m_separatorReplacement = separatorReplacement;
    }

    /**
     * @return the character used as decimal separator for decimals
     */
    public char getDecimalSeparator() {
        return m_decimalSeparator;
    }

    /**
     * @param decimalSeparator the character to be used as decimal separator for decimals
     */
    public void setDecimalSeparator(final char decimalSeparator) {
        m_decimalSeparator = decimalSeparator;
    }

    /**
     * @param str a String containing the decimalSeparator to set
     */
    public void setDecimalSeparator(final String str) {
        setDecimalSeparator(SettingsModelCSVWriter.getFirstChar(str, "Decimal Separator"));
    }

    /**
     * @return {@code true} if very large and very small floating point numbers are written in scientific notation
     */
    public boolean useScientificFormat() {
        return m_useScientificFormat;
    }

    /**
     * @param useScientificFormat a flag deciding if very large and very small floating point numbers are to be written
     *            in scientific notation
     */
    public void setUseScientificFormat(final boolean useScientificFormat) {
        m_useScientificFormat = useScientificFormat;
    }

    /**
     * @return {@code true} if all decimal values should be written with .0 suffix
     */
    public boolean keepTrailingZero() {
        return m_keepTrailingZero;
    }

    /**
     * @param keepTrailingZero a flag deciding if all decimal values should be written with .0 suffix
     */
    public void setKeepTrailingZero(final boolean keepTrailingZero) {
        m_keepTrailingZero = keepTrailingZero;
    }
}
