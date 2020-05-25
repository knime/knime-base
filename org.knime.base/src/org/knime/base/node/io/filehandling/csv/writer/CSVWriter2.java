/*
 * ------------------------------------------------------------------------
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
 * -------------------------------------------------------------------
 *
 * History
 *   May 26, 2020 (Temesgen H. Dadi, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.base.node.io.filehandling.csv.writer;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Writer;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.StringJoiner;

import org.apache.commons.lang.StringUtils;
import org.knime.base.node.io.filehandling.csv.writer.config.AdvancedConfig.QuoteMode;
import org.knime.base.node.io.filehandling.csv.writer.config.CSVWriter2Config;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.streamable.DataTableRowInput;
import org.knime.core.node.streamable.RowInput;

/**
 * Class to write a {@link DataTable} to a CSV file
 *
 * @author Temesgen H. Dadi, KNIME GmbH, Berlin, Germany
 */
class CSVWriter2 extends BufferedWriter {

    private final CSVWriter2Config m_config;

    private final DecimalFormat m_decimalFormatter;

    private String m_lastWarning;

    /**
     * Creates a new writer with default settings.
     *
     * @param writer the writer to write the table to.
     */
    public CSVWriter2(final Writer writer) {
        this(writer, new CSVWriter2Config());
        m_lastWarning = null;
    }

    /**
     * Creates new instance which writes tables to the given writer class. An immediate write operation, will write the
     * table headers (both column and row headers) and will write missing values as "" (empty string).
     *
     * @param writer to write to
     * @param config the object holding all settings, influencing how data tables are written to file.
     */
    public CSVWriter2(final Writer writer, final CSVWriter2Config config) {
        this(writer, new CSVWriter2Config(), false);
    }

    public CSVWriter2(final Writer writer, final CSVWriter2Config config, final boolean disableHeaderWriting) {
        super(writer);
        if (config == null) {
            throw new NullPointerException("The CSVWriter doesn't accept null settings.");
        }
        m_lastWarning = null;
        m_config = config;
        if (disableHeaderWriting) {
            m_config.setWriteColumnHeader(false);
        }
        final String decFormat = m_config.getAdvancedConfig().keepTrailingZero() ? "#.0" : "#.#";
        m_decimalFormatter = new DecimalFormat(decFormat, DecimalFormatSymbols.getInstance(Locale.ENGLISH));
        m_decimalFormatter.setMaximumFractionDigits(340); // DecimalFormat.DOUBLE_FRACTION_DIGITS = 340
    }

    public void writeRow(final RowInput input, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException, InterruptedException {

        DataTableSpec inSpec = input.getDataTableSpec();
        final int colCount = inSpec.getNumColumns();
        m_lastWarning = null; // reset any previous warning

        // write column names
        if (m_config.writeColumnHeader()) {
            StringJoiner rowJoiner = new StringJoiner(m_config.getColumnDelimeter());
            if (m_config.writeRowHeader()) {
                rowJoiner.add(replaceAndQuote("row ID", false)); // RowHeader header
            }
            for (int i = 0; i < colCount; i++) { // for each column name
                rowJoiner.add(replaceAndQuote(inSpec.getColumnSpec(i).getName(), false)); // RowHeader header
            }
            writeLine(rowJoiner.toString());
        } // end of if write column names

        // write each row of the data
        long rowIdx = 0;
        long rowCnt = -1;
        if (input instanceof DataTableRowInput) {
            rowCnt = ((DataTableRowInput)input).getRowCount();
        }

        DataRow row;
        while ((row = input.poll()) != null) {
            StringJoiner rowJoiner = new StringJoiner(m_config.getColumnDelimeter());
            String rowKey = row.getKey().toString();
            checkAndSetExecProgress(rowIdx, rowCnt, rowKey, exec);
            if (m_config.writeRowHeader()) {
                rowJoiner.add(replaceAndQuote(row.getKey().getString(), false));
            }
            // Iterate over all data cells in a row
            for (int colIdx = 0; colIdx < colCount; colIdx++) {
                final DataCell dCell = row.getCell(colIdx);
                if (dCell.isMissing()) {
                    rowJoiner.add(m_config.getAdvancedConfig().getMissingValuePattern());
                } else {

                    if (inSpec.getColumnSpec(colIdx).getType().isCompatible(DoubleValue.class)) { // numeric type
                        final String formattedNumber = formatNumericalValue(dCell, rowIdx, colIdx);
                        rowJoiner.add(replaceAndQuote(formattedNumber, true));
                    } else {
                        rowJoiner.add(replaceAndQuote(dCell.toString(), false));
                    }
                }
            }
            writeLine(rowJoiner.toString());
            rowIdx++;
        }
    }

    /**
     * @param finalI
     * @param finalRowCnt
     * @param rowKey
     * @param exec
     * @throws CanceledExecutionException
     */
    private static void checkAndSetExecProgress(final long rowIdx, final long rowCnt, final String rowKey,
        final ExecutionMonitor exec) throws CanceledExecutionException {
        if (rowCnt <= 0) {
            exec.setMessage(() -> "Writing row " + (rowIdx + 1) + " (\"" + rowKey + "\")");
        } else {
            exec.setProgress(rowIdx / (double)rowCnt,
                () -> "Writing row " + (rowIdx + 1) + " (\"" + rowKey + "\") of " + rowCnt);
        }
        // Check if execution was canceled !
        exec.checkCanceled();
    }

    /**
     * If the specified string contains exactly one dot it is replaced by the specified character.
     *
     * @param val the string to replace the standard decimal separator ('.') in
     * @param newSeparator the new separator
     * @return a string with the dot replaced by the new separator. Could be the passed argument itself.
     */

    private String formatNumericalValue(final DataCell dCell, final long rowIdx, final int colIdx) {
        String strVal = dCell.toString();
        final boolean isDouble = StringUtils.countMatches(strVal, ".") == 1;

        if (!m_config.getAdvancedConfig().useScientificFormat() && isDouble) {
            strVal = m_decimalFormatter.format(((DoubleValue)dCell).getDoubleValue());
        }

        final char customDecSeparator = m_config.getAdvancedConfig().getDecimalSeparator();
        if (customDecSeparator != '.') {
            // use the new separator only if it is not already
            // contained in the value.
            if (strVal.indexOf(customDecSeparator) < 0) {
                // If a dot exists and occurs only once. Otherwise it is not a floating point number
                if (StringUtils.countMatches(strVal, ".") == 1) {
                    strVal = strVal.replace('.', customDecSeparator);
                }
            } else {
                if (m_lastWarning == null) {
                    m_lastWarning = "Specified decimal separator ('" + customDecSeparator + "') is"
                        + " contained in the numerical value. Not replacing decimal separator (e.g." + " in row #"
                        + rowIdx + " column #" + colIdx + ").";
                }
            }
        }
        return strVal;
    }

    private void writeLine(final String value) throws IOException {
        write(value);
        newLine();
    }

    private boolean needsQuote(final String value, final boolean isNumerical) {
        final QuoteMode qMode = m_config.getAdvancedConfig().getQuoteMode();
        return qMode == QuoteMode.ALWAYS //
            || (qMode == QuoteMode.STRINGS_ONLY && !isNumerical) //
            || (qMode == QuoteMode.IF_NEEDED // quote if the column delimiter is in the value
                && value.contains(m_config.getColumnDelimeter()));
    }

    private boolean needsDelimiterRelacement(final String value, final boolean isNumerical) {
        return !isNumerical && m_config.getAdvancedConfig().getQuoteMode() == QuoteMode.NEVER
            && value.contains(m_config.getColumnDelimeter());
    }

    /**
     * Replaces the quote end pattern contained in the string and puts quotes around the string.
     *
     * @param value the string to examine and change
     * @param isNumerical
     * @return the input string with quotes around it and either replaced or escaped quote end patterns in the string.
     */
    private String replaceAndQuote(final String value, final boolean isNumerical) {
        if (needsDelimiterRelacement(value, isNumerical)) {
            return value.replace(String.valueOf(m_config.getColumnDelimeter()),
                m_config.getAdvancedConfig().getSeparatorReplacement());
        } else if (!needsQuote(value, isNumerical)) {
            return value;
        } else {
            String result = String.valueOf(m_config.getQuoteChar());
            final String quteReplacement =
                String.valueOf(m_config.getQuoteEscapeChar()) + String.valueOf(m_config.getQuoteChar());
            result += value.replaceAll(String.valueOf(m_config.getQuoteChar()), quteReplacement);
            result += m_config.getQuoteChar();
            return result;
        }
    }

    /**
     * @return true if a warning message is available
     */
    public boolean hasWarningMessage() {
        return m_lastWarning != null;
    }

    /**
     * Returns a warning message from the last write action. Or null, if there is no warning set.
     *
     * @return a warning message from the last write action. Or null, if there is no warning set.
     */
    public String getLastWarningMessage() {
        return m_lastWarning;
    }

    /**
     * {@inheritDoc} Writes a line feed according to the writer settings.
     */
    @Override
    public void newLine() throws IOException {
        write(m_config.getLineBreak().getEndString());
    }
}
