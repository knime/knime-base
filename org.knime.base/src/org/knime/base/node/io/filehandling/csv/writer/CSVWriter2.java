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
import java.util.List;
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
     * Creates a new {@link Writer} with default {@link CSVWriter2Config}.
     *
     * @param writer the {@link Writer} object
     */
    public CSVWriter2(final Writer writer) {
        this(writer, new CSVWriter2Config());
    }

    /**
     * Creates new writer which writes {@link DataTable} to a CSV files based on the provided {@link CSVWriter2Config}
     *
     * @param writer the {@link Writer} object
     * @param config the {@link CSVWriter2Config} object determining how the {@link DataTable} is written to file.
     */
    public CSVWriter2(final Writer writer, final CSVWriter2Config config) {
        this(writer, config, false);
    }

    /**
     * Creates new writer which writes {@link DataTable} to a CSV files based on the provided {@link CSVWriter2Config}.
     * An additional flag indicating if the file is created new or is opened for appending. This is useful when
     * appending to a file,
     *
     * a) to decide if column header skipping should be disabled. b) to decided on comments header format (file created
     * vs data appended)
     *
     * @param writer the {@link Writer} object
     * @param config the {@link CSVWriter2Config} object determining how the {@link DataTable} is written to file.
     * @param isNewFile a flag indicating if the file is created new or is opened for appending
     */
    public CSVWriter2(final Writer writer, final CSVWriter2Config config, final boolean isNewFile) {
        super(writer);
        if (config == null) {
            throw new NullPointerException("The CSVWriter doesn't accept null settings.");
        }
        m_lastWarning = null;
        m_config = new CSVWriter2Config(config);

        if (isNewFile && m_config.isFileAppended()) {
            m_config.setFileOverwritePolicy(FileOverwritePolicy.OVERWRITE);
        }
        modifyColumnHeaderWriting(isNewFile);

        final String decFormat = m_config.getAdvancedConfig().keepTrailingZero() ? "#.0" : "#.#";
        m_decimalFormatter = new DecimalFormat(decFormat, DecimalFormatSymbols.getInstance(Locale.ENGLISH));
        m_decimalFormatter.setMaximumFractionDigits(340); // DecimalFormat.DOUBLE_FRACTION_DIGITS = 340
    }


    private void modifyColumnHeaderWriting(final boolean isNewFile) {
        if(isNewFile) { // leave unchanged
            return;
        } else if (m_config.skipColumnHeaderOnAppend() && m_config.isFileAppended()) {
            m_config.setWriteColumnHeader(false);
        }
    }


    /**
     * Writes a comment header to the file, if specified so in the settings.
     *
     * @param tableName the name of input table being written
     * @throws IOException if something went wrong during writing
     */
    public void writeCommentHeader(final String tableName) throws IOException {
        List<String> commentLines = m_config.getCommentConfig().getCommentHeader(tableName, m_config.isFileAppended());
        for (String cLine : commentLines) {
            writeLine(cLine);
        }
    }

    public void writeRows(final RowInput input, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException, InterruptedException {

        DataTableSpec inSpec = input.getDataTableSpec();
        m_lastWarning = null; // reset any previous warning

        // write column names
        if (m_config.writeColumnHeader()) {
            StringJoiner rowJoiner = new StringJoiner(m_config.getColumnDelimeter());
            if (m_config.writeRowHeader()) {
                rowJoiner.add(replaceAndQuote("row ID", false)); // RowHeader header
            }
            for (int i = 0; i < inSpec.getNumColumns(); i++) { // for each column name
                rowJoiner.add(replaceAndQuote(inSpec.getColumnSpec(i).getName(), false)); // RowHeader header
            }
            writeLine(rowJoiner.toString());
        }

        // write each row of the data
        long rowIdx = 0;
        long rowCnt = -1;
        if (input instanceof DataTableRowInput) {
            rowCnt = ((DataTableRowInput)input).getRowCount();
        }

        DataRow row;
        while ((row = input.poll()) != null) {
            checkAndSetExecProgress(rowIdx, rowCnt, row.getKey().toString(), exec);
            writeLine(dataRowToLine(row, rowIdx, inSpec));
            rowIdx++;
        }
    }

    private String dataRowToLine(final DataRow row, final long rowIdx, final DataTableSpec inSpec) {
        StringJoiner rowJoiner = new StringJoiner(m_config.getColumnDelimeter());
        if (m_config.writeRowHeader()) {
            rowJoiner.add(replaceAndQuote(row.getKey().toString(), false));
        }
        // Iterate over all data cells in a row
        for (int colIdx = 0; colIdx < inSpec.getNumColumns(); colIdx++) {
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
        return rowJoiner.toString();
    }

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
     * Formats a numerical value from a {@link DataCell} according to the settings. Custom decimal separator used only
     * if scientific notation is not on and the number contains exactly one dot.
     *
     * @param dCell the {@link DataCell} with numeric value
     * @param rowIdx the row index of the {@link DataCell}. Required for warnings.
     * @param colIdx the column index of the {@link DataCell}. Required for warnings.
     * @return a formated string representing the number in a numerical {@link DataCell}
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

    /**
     * Writes a string to file and appends a newline, which can be different from the system default.
     *
     * @param value the {@link String} value to write
     * @throws IOException if something went wrong during writing
     */
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

    private boolean replaceDelimiter(final String value) {
        return m_config.getAdvancedConfig().getQuoteMode() == QuoteMode.NEVER
            && value.contains(m_config.getColumnDelimeter());
    }

    /**
     * Returns a quoted string after escaping occurrences of the quote character with the provided quote escape.
     * Numerical values are treated differently.
     *
     * @param value the string to examine and change
     * @param isNumerical whether the value is numeric or not
     * @return the input string with quotes around it when appropriate.
     */
    private String replaceAndQuote(final String value, final boolean isNumerical) {
        // if never quote is selected and there is a replacement for delimiter

        if (!isNumerical && replaceDelimiter(value)) {
            return value.replace(String.valueOf(m_config.getColumnDelimeter()),
                m_config.getAdvancedConfig().getSeparatorReplacement());
        }

        if (needsQuote(value, isNumerical)) {
            String result = String.valueOf(m_config.getQuoteChar());
            final String quteReplacement =
                String.valueOf(m_config.getQuoteEscapeChar()) + String.valueOf(m_config.getQuoteChar());
            result += value.replaceAll(String.valueOf(m_config.getQuoteChar()), quteReplacement);
            result += m_config.getQuoteChar();
            return result;
        }
        return value;
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
