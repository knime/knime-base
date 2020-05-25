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
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.config.Config;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.port.PortObjectSpec;

/**
 * Advanced setting configurations for CSV writer node
 *
 * @author Temesgen H. Dadi, KNIME GmbH, Berlin, Germany
 */
public final class AdvancedConfig extends SettingsModel {

    /**
     * Different modes of putting values inside quotes
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

    private static final String CFGKEY_ROOT = "advanced_settings";

    private static final String CFGKEY_COMPRESS_WITH_GZIP = "compress_with_gzip";

    private static final String CFGKEY_QUOTE_MODE = "quote_mode";

    private static final String CFGKEY_SEPARATOR_REPL = "separator_replacement";

    private static final String CFGKEY_DEC_SEPARATOR = "decimal_separator";

    private static final String CFGKEY_SCIENTIFIC_FORMAT = "use_scientific_format";

    private static final String CFGKEY_KEEP_TRAILING_ZERO = "keep_trailing_zero_in_decimals";

    private String m_missingValuePattern;

    private boolean m_compressWithGzip;

    private String m_quoteModeName;

    private String m_separatorReplacement; // only used with mode=REPLACE

    private char m_decimalSeparator;

    private boolean m_useScientificFormat;

    private boolean m_keepTrailingZero;

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

    /**
     * Copy constructor
     *
     * @param source the source object to copy
     */
    public AdvancedConfig(final AdvancedConfig source) {
        m_missingValuePattern = source.getMissingValuePattern();
        m_compressWithGzip = false;
        m_quoteModeName = source.getQuoteModeName();
        m_separatorReplacement = source.getSeparatorReplacement();
        m_decimalSeparator = source.getDecimalSeparator();
        m_useScientificFormat = source.useScientificFormat();
        m_keepTrailingZero = source.useScientificFormat();
    }

    @SuppressWarnings("unchecked")
    @Override
    protected AdvancedConfig createClone() {
        return new AdvancedConfig(this);
    }

    @Override
    protected String getModelTypeID() {
        return "MODEL_TYPE_ID_" + CFGKEY_ROOT;
    }

    @Override
    protected String getConfigName() {
        return CFGKEY_ROOT;
    }

    @Override
    protected void loadSettingsForDialog(final NodeSettingsRO settings, final PortObjectSpec[] specs)
        throws NotConfigurableException {
        loadSettingsForDialog(settings);
    }

    /**
     * Read the value(s) of this settings model from configuration object for the purpose of loading them into node
     * dialog. Default values are used if the key to a specific setting is not found.
     *
     * @param settings the configuration object
     * @throws NotConfigurableException if the sub-setting can not be extracted.
     */
    public void loadSettingsForDialog(final NodeSettingsRO settings) throws NotConfigurableException {
        Config config;
        try {
            config = settings.getConfig(CFGKEY_ROOT);
        } catch (final InvalidSettingsException ex) {
            throw new NotConfigurableException(ex.getMessage());
        }
        m_missingValuePattern = config.getString(CFGKEY_MISSING_VALUE, "");
        m_compressWithGzip = config.getBoolean(CFGKEY_COMPRESS_WITH_GZIP, false);

        m_quoteModeName = config.getString(CFGKEY_QUOTE_MODE, QuoteMode.IF_NEEDED.name());
        m_separatorReplacement = config.getString(CFGKEY_SEPARATOR_REPL, "");

        m_decimalSeparator = config.getChar(CFGKEY_DEC_SEPARATOR, '.');
        m_useScientificFormat = config.getBoolean(CFGKEY_SCIENTIFIC_FORMAT, false);
        m_keepTrailingZero = config.getBoolean(CFGKEY_KEEP_TRAILING_ZERO, false);
    }

    @Override
    protected void saveSettingsForDialog(final NodeSettingsWO settings) throws InvalidSettingsException {
        saveSettingsForModel(settings);
    }

    @Override
    protected void validateSettingsForModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        final Config config = settings.getConfig(CFGKEY_ROOT);
        config.getString(CFGKEY_MISSING_VALUE);
        config.getBoolean(CFGKEY_COMPRESS_WITH_GZIP);

        config.getString(CFGKEY_QUOTE_MODE);
        config.getString(CFGKEY_SEPARATOR_REPL);

        config.getChar(CFGKEY_DEC_SEPARATOR);
        config.getBoolean(CFGKEY_SCIENTIFIC_FORMAT);
        config.getBoolean(CFGKEY_KEEP_TRAILING_ZERO);
    }

    @Override
    protected void loadSettingsForModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        final Config config = settings.getConfig(CFGKEY_ROOT);
        m_missingValuePattern = config.getString(CFGKEY_MISSING_VALUE);
        m_compressWithGzip = config.getBoolean(CFGKEY_COMPRESS_WITH_GZIP);

        m_quoteModeName = config.getString(CFGKEY_QUOTE_MODE);
        m_separatorReplacement = config.getString(CFGKEY_SEPARATOR_REPL);

        m_decimalSeparator = config.getChar(CFGKEY_DEC_SEPARATOR);
        m_useScientificFormat = config.getBoolean(CFGKEY_SCIENTIFIC_FORMAT);
        m_keepTrailingZero = config.getBoolean(CFGKEY_KEEP_TRAILING_ZERO);
    }

    @Override
    protected void saveSettingsForModel(final NodeSettingsWO settings) {
        final Config config = settings.addConfig(CFGKEY_ROOT);
        config.addString(CFGKEY_MISSING_VALUE, m_missingValuePattern);
        config.addBoolean(CFGKEY_COMPRESS_WITH_GZIP, m_compressWithGzip);

        config.addString(CFGKEY_QUOTE_MODE, m_quoteModeName);
        config.addString(CFGKEY_SEPARATOR_REPL, m_separatorReplacement);

        config.addChar(CFGKEY_DEC_SEPARATOR, m_decimalSeparator);
        config.addBoolean(CFGKEY_SCIENTIFIC_FORMAT, m_useScientificFormat);
        config.addBoolean(CFGKEY_KEEP_TRAILING_ZERO, m_keepTrailingZero);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " ('" + CFGKEY_ROOT + "')";
    }

    /**
     * @return the missingValuePattern
     */
    public String getMissingValuePattern() {
        return m_missingValuePattern;
    }

    /**
     * @param missingValuePattern the missingValuePattern to set
     */
    public void setMissingValuePattern(final String missingValuePattern) {
        m_missingValuePattern = missingValuePattern;
    }

    /**
     * @return the compressWithGzip
     */
    public boolean compressWithGzip() {
        return m_compressWithGzip;
    }

    /**
     * @param compressWithGzip the compressWithGzip to set
     */
    public void setCompressWithGzip(final boolean compressWithGzip) {
        m_compressWithGzip = compressWithGzip;
    }

    /**
     * @return the quoteMode
     */
    public QuoteMode getQuoteMode() {
        return QuoteMode.valueOf(m_quoteModeName);
    }

    /**
     * @return the quoteModeName
     */
    public String getQuoteModeName() {
        return m_quoteModeName;
    }

    /**
     * @param quoteModeName the QuoteMode to set
     */
    public void setQuoteModeName(final String quoteModeName) {
        m_quoteModeName = quoteModeName;
    }

    /**
     * @return the separatorReplacement
     */
    public String getSeparatorReplacement() {
        return m_separatorReplacement;
    }

    /**
     * @param separatorReplacement the separatorReplacement to set
     */
    public void setSeparatorReplacement(final String separatorReplacement) {
        m_separatorReplacement = separatorReplacement;
    }

    /**
     * @return the decimalSeparator
     */
    public char getDecimalSeparator() {
        return m_decimalSeparator;
    }

    /**
     * @param decimalSeparator the decimalSeparator to set
     */
    public void setDecimalSeparator(final char decimalSeparator) {
        m_decimalSeparator = decimalSeparator;
    }

    /**
     * @param str a String containing the decimalSeparator to set
     */
    public void setDecimalSeparator(final String str) {
        setDecimalSeparator(CSVWriter2Config.getFirstChar(str, "Decimal Separator"));
    }

    /**
     * @return the useScientificFormat
     */
    public boolean useScientificFormat() {
        return m_useScientificFormat;
    }

    /**
     * @param useScientificFormat the useScientificFormat to set
     */
    public void setUseScientificFormat(final boolean useScientificFormat) {
        m_useScientificFormat = useScientificFormat;
    }

    /**
     * @return the keepTrailingZero
     */
    public boolean keepTrailingZero() {
        return m_keepTrailingZero;
    }

    /**
     * @param keepTrailingZero the keepTrailingZero to set
     */
    public void setKeepTrailingZero(final boolean keepTrailingZero) {
        m_keepTrailingZero = keepTrailingZero;
    }

}
