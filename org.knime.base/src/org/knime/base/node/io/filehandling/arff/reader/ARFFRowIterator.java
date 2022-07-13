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
 *   02.07.2022. (Dragan Keselj): created
 */
package org.knime.base.node.io.filehandling.arff.reader;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.NoSuchElementException;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.RowIterator;
import org.knime.core.data.UnmaterializedCell;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.DoubleCell.DoubleCellFactory;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.IntCell.IntCellFactory;
import org.knime.core.data.def.LongCell;
import org.knime.core.data.def.LongCell.LongCellFactory;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.def.StringCell.StringCellFactory;
import org.knime.core.node.NodeLogger;
import org.knime.core.util.tokenizer.Tokenizer;
import org.knime.core.util.tokenizer.TokenizerSettings;
import org.knime.filehandling.core.defaultnodesettings.ExceptionUtil;
import org.knime.filehandling.core.node.table.reader.spec.TableSpecGuesser;
import org.knime.filehandling.core.util.BomEncodingUtils;
import org.knime.filehandling.core.util.CompressionAwareCountingInputStream;

/**
 * Iterator for ARFF table. It returns rows one by one.
 *
 * @author Dragan Keselj, KNIME GmbH
 */
public class ARFFRowIterator extends RowIterator implements Closeable {
    /** The node logger fot this class. */
    private static final NodeLogger LOGGER = NodeLogger.getLogger(ARFFRowIterator.class);

    private static final String MISSING_CELL_VALUE = "?";

    private final DataTableSpec m_tSpec;

    private Path m_filePath;

    private final Tokenizer m_tokenizer;

    private int m_rowNo; // we count the rows read so far

    private int m_numMsgExtraCol;

    private int m_numMsgMissCol;

    private int m_numMsgWrongFormat;

    private int m_numMsgMissVal;

    private static final int MAX_ERR_MSG = 10;

    /**
     * Create a new row iterator for reading rows from the ARFF file at the specified location.
     *
     * @param filePath valid path of the file to be read
     * @param charset
     * @param tSpec the structure of the table to create
     *
     * @throws IOException if the ARFF file location couldn't be opened
     */
    @SuppressWarnings("resource")
    public ARFFRowIterator(final Path filePath, final Charset charset, final DataTableSpec tSpec) throws IOException {
        m_filePath = filePath;
        m_tSpec = tSpec;

        m_rowNo = 1;

        m_numMsgExtraCol = 0;
        m_numMsgMissCol = 0;
        m_numMsgWrongFormat = 0;
        m_numMsgMissVal = 0;

        InputStream inputStream = null;
        BufferedReader reader = null;
        Tokenizer tokenizer = null;

        try {
            inputStream = new CompressionAwareCountingInputStream(filePath);
            reader = BomEncodingUtils.createBufferedReader(inputStream, charset);

            // eat the ARFF header
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().equalsIgnoreCase("@DATA")) {
                    // we ate the "data" declaration token. Data starts from here.
                    break;
                }
            }

            // setup the tokenizer to read the file
            tokenizer = new Tokenizer(reader);
            // create settings for the tokenizer
            final var settings = new TokenizerSettings();
            // add the ARFF single line comment
            settings.addSingleLineCommentPattern("%", false, false);
            // LF is a row seperator - add it as delimiter
            settings.addDelimiterPattern("\n", /* combine multiple= */true, /* return as token= */true,
                /* include in token= */false);
            // ARFF knows single and double quotes
            settings.addQuotePattern("'", "'");
            settings.addQuotePattern("\"", "\"");
            // the data in the data section is separated by comma.
            settings.addDelimiterPattern(",", false, false, false);

            settings.addWhiteSpaceCharacter(' ');
            settings.addWhiteSpaceCharacter('\t');

            tokenizer.setSettings(settings);
        } catch (Exception e) { // NOSONAR
            closeSilently(inputStream);
            closeSilently(reader);
            throw ExceptionUtil.wrapAsIOException(e);
        }
        m_tokenizer = tokenizer;
    }

    /**
     * @return how many rows read so far.
     */
    public int getRowNo() {
        return m_rowNo;
    }

    /**
     * @return Returns the number of bytes returned so far.
     */
    public long getReadBytes() {
        return m_tokenizer.getReadBytes();
    }

    @Override
    public boolean hasNext() {
        String token = null;
        try {
            token = m_tokenizer.nextToken();
            // skip empty lines.
            while ((token != null) && (token.equals("\n") || (!m_tokenizer.lastTokenWasQuoted() && token.isEmpty()))) {
                token = m_tokenizer.nextToken();
            }
            m_tokenizer.pushBack();
        } catch (Exception e) { //NOSONAR
            token = null;
            close();
        }
        return (token != null);
    }

    @Override
    public DefaultRow next() {
        // before anything else: check if there is more in the stream
        // skips empty lines!
        if (!hasNext()) {
            close();
            throw new NoSuchElementException(
                "The row iterator proceeded beyond the last line of '" + m_filePath.toString() + "'.");
        }

        // Now, read the columns until we have enough or see a row delimiter
        String token;
        token = m_tokenizer.nextToken();
        m_tokenizer.pushBack(); // peek at the token

        //Check if format of row is sparse
        DataCell[] rowCells;
        if (!token.isEmpty() && token.charAt(0) == '{' && !m_tokenizer.lastTokenWasQuoted()) {
            rowCells = readSparseRow();
        } else {
            rowCells = readDataRow();
        }

        m_rowNo++;
        return new DefaultRow(String.valueOf(m_rowNo - 1), rowCells);
    }

    /**
     * Reads a single data row as {@link DataCell} array.
     *
     * @return cell values in the row as {@code DataCell[]}
     */
    private DataCell[] readDataRow() {
        int noOfCols = m_tSpec.getNumColumns();
        final var rowCells = new DataCell[noOfCols];
        String token;
        int createdCols = 0; //NOSONAR
        while (createdCols < noOfCols) { //NOSONAR
            token = m_tokenizer.nextToken();
            if (token == null) {
                // file ended early.
                break;
            }
            // EOL is returned as token
            if (token.equals("\n")) {
                if (createdCols == 0) {
                    /*
                     * this is a bit of a hack. The tokenizer doesn't combine
                     * delimiters if there is comment between them. That's why a
                     * comment line would create a row filled with missing
                     * values. This avoids that.
                     */
                    continue;
                }
                // line ended early.
                m_tokenizer.pushBack();
                // we need the row delim in the file, for after the loop
                break;
            }

            // figure out if its a missing value
            var isMissingCell = false;
            if (token.equals("") && (!m_tokenizer.lastTokenWasQuoted())) {
                if (m_numMsgMissVal < MAX_ERR_MSG) {
                    LOGGER.warn("ARFF reader WARNING: No value for" + " column " + (createdCols + 1) + "("
                        + m_tSpec.getColumnSpec(createdCols) + "), file '" + m_filePath.toString() + "' line "
                        + m_tokenizer.getLineNumber() + ". Creating missing value for it.");
                    m_numMsgMissVal++;
                }
                if (m_numMsgMissVal == MAX_ERR_MSG) {
                    LOGGER.warn("   (last message of this kind)");
                    m_numMsgMissVal++;
                }
                isMissingCell = true;
            } else if ((token.equals("?")) && (!m_tokenizer.lastTokenWasQuoted())) {
                // the ARFF pattern for missing values
                isMissingCell = true;
            }

            // now get that new cell (it throws something at us if it couldn't)
            rowCells[createdCols] = getDataCell(m_tSpec.getColumnSpec(createdCols).getType(), token, isMissingCell);
            createdCols++;
        }

        // In case we've seen a row delimiter before the row was complete:
        // fill the row with missing cells
        if (createdCols < noOfCols) {
            if (m_numMsgMissCol < MAX_ERR_MSG) {
                LOGGER.warn("ARFF reader WARNING: Too few columns in " + "file '" + m_filePath.toString() + "' line "
                    + m_tokenizer.getLineNumber() + ". Creating missing values for the missing columns.");
                m_numMsgMissCol++;
            }
            if (m_numMsgMissCol == MAX_ERR_MSG) {
                LOGGER.warn("   (last message of this kind)");
                m_numMsgMissCol++;
            }
            while (createdCols < noOfCols) {
                rowCells[createdCols] = getDataCell(m_tSpec.getColumnSpec(createdCols).getType(), null, true);
                createdCols++;
            }
        }
        // now read the row delimiter from the file - and ignore whatever is
        // before it.
        readUntilEOL();
        return rowCells;
    }

    /**
     * Reads a single data row in the
     * <a href="https://waikato.github.io/weka-wiki/formats_and_processing/arff_developer/#sparse-arff-files">SPARSE</a>
     * ARFF file as {@link DataCell} array.
     *
     * @return cell values in the row as {@code DataCell[]}
     */
    private DataCell[] readSparseRow() {
        DataCell[] rowCells = new DataCell[m_tSpec.getNumColumns()];
        String token;
        token = m_tokenizer.nextToken().substring(1);
        var foundending = false;
        //while not at the end parse entries of the type: col val,
        while ((token != null) && !token.isEmpty() && (!token.equals("\n"))) {
            if (token.charAt(token.length() - 1) == '}') {
                foundending = true;
                token = token.substring(0, token.length() - 1);
            }
            String[] fields = token.split(" ");
            if (fields.length != 2) {
                throw new IllegalStateException("Malformatted sparse data entry: '" + token + "'");

            }
            int col;
            try {
                col = Integer.parseInt(fields[0].trim());
            } catch (NumberFormatException e) {
                throw new IllegalStateException(
                    "Malformatted column index in sparse data entry: '" + fields[0].trim() + "'");
            }
            String data = fields[1];
            if (!m_tokenizer.lastTokenWasQuoted()) {
                data = data.trim();
            }
            boolean missCell = data.equals("?") && !m_tokenizer.lastTokenWasQuoted();
            rowCells[col] = getDataCell(m_tSpec.getColumnSpec(col).getType(), data, missCell);

            token = m_tokenizer.nextToken();
        }
        if (!foundending) {
            int line = m_tokenizer.getLineNumber();
            if (token != null && token.equals("\n")) {
                line--;
            }
            LOGGER.error("Malformatted sparse row in line " + line + " (closing bracket not found).");
        }

        //now go through the row and fill the nulls with 0s
        for (var c = 0; c < rowCells.length; c++) {
            if (rowCells[c] == null) {
                rowCells[c] = getDataCell(m_tSpec.getColumnSpec(c).getType(), "0", false);
            }
        }
        return rowCells;
    }

    /*
     * reads from the tokenizer until it reads a token containing '\n'.
     */
    private void readUntilEOL() {
        String token = m_tokenizer.nextToken();

        while ((token != null) && !token.equals("\n")) { // EOF is also EOL
            if (m_numMsgExtraCol < MAX_ERR_MSG) {
                LOGGER.warn("ARFF reader WARNING: Ignoring extra " + "columns in the data section of file '"
                    + m_filePath.toString() + "' line " + m_tokenizer.getLineNumber() + ".");
                m_numMsgExtraCol++;
            }
            if (m_numMsgExtraCol == MAX_ERR_MSG) {
                LOGGER.warn("   (last message of this kind.)");
                m_numMsgExtraCol++;
            }
            token = m_tokenizer.nextToken();
        }

    }

    /**
     * The function creates a default {@link DataCell} of a type depending on the <code>type</code> passed in, and
     * initializes the value of this data cell from the <code>data</code> string (converting the string to the
     * corresponding type). It will create a missing cell and print a warning if it couldn't convert the string into the
     * appropriate format. It throws a <code>IllegalStateException</code> if the <code>type</code> passed in is not
     * supported.
     *
     * @param type Specifies the type of DataCell that is to be created. <code>UnmaterializedCell.TYPE</code> is used
     *            for columns we need to 'guess' the correct type based on column values.
     * @param data the string representation of the value that will be set in the DataCell created. It gets trimmed
     *            before it's converted into a number.
     * @param createMissingCell If set true the default '<code>missing</code>' value of that cell type will be set
     *            indicating that the data in that cell was not specified. The <code>data</code> parameter is ignored
     *            then.
     *
     * @return <code>DataCell</code> of the type specified in <code>type</code>.
     *
     * @see {@link TableSpecGuesser}
     *
     */
    private DataCell getDataCell(final DataType type, final String data, final boolean createMissingCell) {
        if (type.equals(DataType.getType(UnmaterializedCell.class))) { // NUMERIC, DATE arrf types
            return getStringCell(data, createMissingCell);
        } else if (type.equals(StringCell.TYPE)) {
            return getStringCell(data, createMissingCell);
        } else if (type.equals(IntCell.TYPE)) {
            return getIntCell(data, createMissingCell);
        } else if (type.equals(LongCell.TYPE)) {
            return getLongCell(data, createMissingCell);
        } else if (type.equals(DoubleCell.TYPE)) {
            return getDoubleCell(data, createMissingCell);
        } else {
            throw new IllegalStateException("Cannot create data cell of type" + type.toString());
        }
    }

    @SuppressWarnings("static-method")
    private DataCell getStringCell(final String data, final boolean createMissingCell) {
        return createMissingCell ? StringCellFactory.create(MISSING_CELL_VALUE) : StringCellFactory.create(data);
    }

    private DataCell getIntCell(final String data, final boolean createMissingCell) {
        if (createMissingCell) {
            return DataType.getMissingCell();
        } else {
            try {
                Integer.parseInt(data.trim());
                return IntCellFactory.create(data);
            } catch (NumberFormatException nfe) {
                logError(data, "integer");
                return DataType.getMissingCell();
            }
        }
    }

    private DataCell getLongCell(final String data, final boolean createMissingCell) {
        if (createMissingCell) {
            return DataType.getMissingCell();
        } else {
            try {
                Long.parseLong(data.trim());
                return LongCellFactory.create(data);
            } catch (NumberFormatException nfe) {
                logError(data, "long");
                return DataType.getMissingCell();
            }
        }
    }

    private DataCell getDoubleCell(final String data, final boolean createMissingCell) {
        if (createMissingCell) {
            return DataType.getMissingCell();
        } else {
            try {
                Double.parseDouble(data.trim());
                return DoubleCellFactory.create(data);
            } catch (NumberFormatException nfe) {
                logError(data, "double");
                return DataType.getMissingCell();
            }
        }
    }

    private void logError(final String data, final String type) {
        if (m_numMsgWrongFormat < MAX_ERR_MSG) {
            LOGGER.warn("ARFF reader WARNING: Wrong data format. In line " + m_tokenizer.getLineNumber() + " read '"
                + data + "' as " + type + " type");
            LOGGER.warn("    Creating missing cell for it.");
            m_numMsgWrongFormat++;
        }
        if (m_numMsgWrongFormat == MAX_ERR_MSG) {
            LOGGER.warn("    (last message of " + "this kind.)");
            m_numMsgWrongFormat++;
        }
    }

    @Override
    public void close() {
        m_tokenizer.closeSourceStream();
    }

    private static void closeSilently(final Closeable closeable) {
        if (closeable == null) {
            return;
        }
        try {
            closeable.close();
        } catch (IOException e) { // NOSONAR
            // ignore
        }
    }

}
