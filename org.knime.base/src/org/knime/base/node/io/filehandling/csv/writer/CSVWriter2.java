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

import java.io.Closeable;
import java.io.IOException;
import java.io.Writer;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.Locale;
import java.util.StringJoiner;
import java.util.regex.Pattern;

import org.knime.base.node.io.filehandling.csv.writer.config.AdvancedConfig.QuoteMode;
import org.knime.base.node.io.filehandling.csv.writer.config.CSVWriter2Config;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.streamable.DataTableRowInput;
import org.knime.core.node.streamable.RowInput;

/**
 * Class to write a {@link DataTable} to a CSV file
 *
 * @author Temesgen H. Dadi, KNIME GmbH, Berlin, Germany
 */
class CSVWriter2 implements Closeable {

    private final Writer m_writer;

    private final CSVWriter2Config m_config;

    private final DecimalFormat m_decimalFormatter;

    private final DecimalFormat m_integerFormatter;

    private final String m_quoteReplacement;

    private String m_lastWarning;

    private final Pattern m_columnOrRowDelimiter;

    /**
     * Creates new writer which writes {@link DataTable} to a CSV files based on the provided
     * {@link CSVWriter2Config}
     *
     * @param writer the {@link Writer}
     * @param config the {@link CSVWriter2Config} object determining how the {@link DataTable} is written to file.
     */
    public CSVWriter2(final Writer writer, final CSVWriter2Config config) {
        if (config == null) {
            throw new NullPointerException("The CSVWriter doesn't accept null settings.");
        }
        m_writer = writer;
        m_config = config;
        m_lastWarning = null;

        final DecimalFormatSymbols symbolFormat = DecimalFormatSymbols.getInstance(Locale.ENGLISH);

        final String decFormat = m_config.getAdvancedConfig().keepTrailingZero() ? "#.0" : "#.#";
        m_decimalFormatter = new DecimalFormat(decFormat, symbolFormat);
        m_decimalFormatter.setMaximumFractionDigits(340); // DecimalFormat.DOUBLE_FRACTION_DIGITS = 340

        m_integerFormatter = new DecimalFormat("#", symbolFormat);

        m_quoteReplacement = String.valueOf(m_config.getQuoteEscapeChar()) + String.valueOf(m_config.getQuoteChar());
        m_columnOrRowDelimiter = Pattern.compile(m_config.getColumnDelimiter() + "|\r|\n");

    }

    /**
     * Writes a list of {@code String} to file. Each item in the list will be written as a line.
     *
     * @param lines list of lines to write
     * @throws IOException if something went wrong during writing
     */
    public void writeLines(final List<String> lines) throws IOException {
        for (final String line : lines) {
            writeLine(line);
        }
    }

    /**
     * Writes the column headers of a DataTable to file.
     *
     * @param inSpec the Spec of the DataTable
     * @throws IOException if something went wrong during writing
     */
    public void writeColumnHeader(final DataTableSpec inSpec) throws IOException {
        final StringJoiner rowJoiner = new StringJoiner(m_config.getColumnDelimiter());
        if (m_config.writeRowHeader()) {
            rowJoiner.add(replaceAndQuote("row ID", false)); // RowHeader header
        }
        inSpec.stream()//
            .forEachOrdered(s -> rowJoiner.add(replaceAndQuote(s.getName(), false)));
        writeLine(rowJoiner.toString());
    }

    /**
     * Writes the column headers of a DataTable to file.
     *
     * @param input the {@link RowInput} to be written
     * @param exec the {@link ExecutionMonitor}
     * @throws IOException if something went wrong during writing
     * @throws CanceledExecutionException
     * @throws InterruptedException
     */
    public void writeRows(final RowInput input, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException, InterruptedException {

        final DataTableSpec inSpec = input.getDataTableSpec();
        m_lastWarning = null; // reset any previous warning

        // write each row of the data
        long rowIdx = 0;
        long rowCnt = -1;
        if (input instanceof DataTableRowInput) {
            rowCnt = ((DataTableRowInput)input).getRowCount();
        }

        final boolean[] isDoubleColumn = new boolean[inSpec.getNumColumns()];
        final boolean[] isNumericalColumn = new boolean[inSpec.getNumColumns()];
        for (int i = 0; i < inSpec.getNumColumns(); i++) { // for each column name
            final DataType type = inSpec.getColumnSpec(i).getType();
            isNumericalColumn[i] = type.isCompatible(DoubleValue.class);
            isDoubleColumn[i] = type == DoubleCell.TYPE;
        }

        DataRow row;
        while ((row = input.poll()) != null) {
            checkAndSetExecProgress(rowIdx, rowCnt, row.getKey().toString(), exec);
            writeLine(dataRowToLine(row, rowIdx, isNumericalColumn, isDoubleColumn));
            rowIdx++;
        }
    }

    private String dataRowToLine(final DataRow row, final long rowIdx, final boolean[] isNumericColumn,
        final boolean[] isDoubleColumn) {
        final StringJoiner rowJoiner = new StringJoiner(m_config.getColumnDelimiter());
        if (m_config.writeRowHeader()) {
            rowJoiner.add(replaceAndQuote(row.getKey().toString(), false));
        }
        // Iterate over all data cells in a row
        for (int colIdx = 0; colIdx < isNumericColumn.length; colIdx++) {
            final DataCell dCell = row.getCell(colIdx);
            if (dCell.isMissing()) {
                rowJoiner.add(m_config.getAdvancedConfig().getMissingValuePattern());
            } else {
                if (isNumericColumn[colIdx]) { // numeric type
                    final String formattedNumber =
                        convertNumericCellToString(dCell, rowIdx, colIdx, isDoubleColumn[colIdx]);
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

    private String convertNumericCellToString(final DataCell dCell, final long rowIdx, final int colIdx,
        final boolean isDouble) {
        if (m_config.getAdvancedConfig().useScientificFormat()) {
            return dCell.toString();
        }
        final double dVal = ((DoubleValue)dCell).getDoubleValue();
        if (!isDouble) {
            return m_integerFormatter.format(dVal);
        }
        final String strVal = m_decimalFormatter.format(dVal);
        final char customDecSeparator = m_config.getAdvancedConfig().getDecimalSeparator();
        if ('.' != customDecSeparator) {
            if (strVal.indexOf(customDecSeparator) < 0) {
                // TODO: can be done more efficiently
                return strVal.replace('.', customDecSeparator);
            } else {
                m_lastWarning = "Specified decimal separator ('" + customDecSeparator + "') is"
                    + " contained in the numerical value. Not replacing decimal separator (e.g." + " in row #" + rowIdx
                    + " column #" + colIdx + ").";
            }
        }
        return strVal;
    }

    /**
     * Writes a string to file and appends a newline, which can be different from the system default. (
     *
     * @param value the {@link String} value to write
     * @throws IOException if something went wrong during writing
     */
    private void writeLine(final String value) throws IOException {
        m_writer.write(value);
        newLine();
    }

    private boolean needsQuote(final String value, final boolean isNumerical) {
        final QuoteMode qMode = m_config.getAdvancedConfig().getQuoteMode();
        return qMode == QuoteMode.ALWAYS //
            || (qMode == QuoteMode.STRINGS_ONLY && !isNumerical) //
            || (qMode == QuoteMode.IF_NEEDED // quote if the column delimiter is in the value
                && m_columnOrRowDelimiter.matcher(value).find());
    }

    private boolean replaceDelimiter(final String value) {
        return m_config.getAdvancedConfig().getQuoteMode() == QuoteMode.NEVER
            && value.contains(m_config.getColumnDelimiter());
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
            return value.replace(m_config.getColumnDelimiter(), m_config.getAdvancedConfig().getSeparatorReplacement());
        }

        if (needsQuote(value, isNumerical)) {
            final StringBuilder result = new StringBuilder(String.valueOf(m_config.getQuoteChar()));
            result.append(value.replaceAll(String.valueOf(m_config.getQuoteChar()), m_quoteReplacement));
            result.append(m_config.getQuoteChar());
            return result.toString();
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
     * Writes a line break according to the writer settings.
     */
    private void newLine() throws IOException {
        m_writer.write(m_config.getLineBreak().getLineBreak());
    }

    @Override
    public void close() throws IOException {
        m_writer.close();
    }
}
