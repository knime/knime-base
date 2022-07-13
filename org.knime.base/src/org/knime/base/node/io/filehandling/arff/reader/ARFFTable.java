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
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnDomainCreator;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.UnmaterializedCell;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.util.tokenizer.Tokenizer;
import org.knime.core.util.tokenizer.TokenizerSettings;
import org.knime.filehandling.core.connections.FSPath;
import org.knime.filehandling.core.defaultnodesettings.ExceptionUtil;
import org.knime.filehandling.core.node.table.reader.config.TableReadConfig;
import org.knime.filehandling.core.util.BomEncodingUtils;
import org.knime.filehandling.core.util.CompressionAwareCountingInputStream;

/**
 * Creates ARFF table with its specs and iterator from the ARFF file.
 *
 * @author Dragan Keselj, KNIME GmbH
 */
class ARFFTable implements DataTable {

    /** The node logger fot this class. */
    private static final NodeLogger LOGGER = NodeLogger.getLogger(ARFFTable.class);

    private final DataTableSpec m_tableSpec;

    private final ARFFRowIterator m_rowIterator;

    /**
     * Constructor.
     *
     * @param path the location of the ARFF file to read.
     * @param charset
     * @param config
     * @throws IOException
     */
    ARFFTable(final FSPath path, final Charset charset, final TableReadConfig<ARFFReaderConfig> config) throws IOException {
        try (var inputStream = new CompressionAwareCountingInputStream(path);
                var reader = BomEncodingUtils.createBufferedReader(inputStream, charset)) {
            m_tableSpec = ARFFTable.createDataTableSpecFromARFFfile(path, reader);
        } catch (InvalidSettingsException ex) {
            throw ExceptionUtil.wrapAsIOException(ex);
        }
        m_rowIterator = new ARFFRowIterator(path, charset, getDataTableSpec());
    }

    @Override
    public DataTableSpec getDataTableSpec() {
        return m_tableSpec;
    }

    @Override
    public ARFFRowIterator iterator() {
        return m_rowIterator;
    }

    /**
     * Reads the header of the specified ARFF file and returns a corresponding table spec object.
     *
     * @param filePath the location of the ARFF file to read.
     *
     * @return a table spec reflecting the settings in the file header.
     *
     * @throws IOException if the file location couldn't be opened.
     * @throws InvalidSettingsException if the file contains an invalid format.
     */
    static DataTableSpec createDataTableSpecFromARFFfile(final Path filePath, final BufferedReader reader)
        throws InvalidSettingsException {
        final var tokenizer = new Tokenizer(reader);
        // create tokenizer settings that will deliver us the attributes and
        // arguments as tokens.
        tokenizer.setSettings(getTokenizerHeaderSettings());
        // prepare for creating a column spec for each "@attribute" read
        final List<DataColumnSpec> colSpecs = new ArrayList<>();
        String tableName = null;
        String token;
        // now we collect the header information - until we see the EOF or
        // the data section begins.
        while (true) {
            DataCell[] possVals = null;
            DataType type;
            token = tokenizer.nextToken();
            if (token == null) {
                throw new InvalidSettingsException("Incorrect/Incomplete " + "ARFF file. No data section found.");
            }
            if (token.length() == 0) {
                // ignore empty lines
                continue;
            }
            if (token.equalsIgnoreCase("@DATA")) {
                // this starts the data section: we are done.
                break;
            }
            if (token.equalsIgnoreCase("@ATTRIBUTE")) {
                // defines a new data column
                String colName = tokenizer.nextToken();
                String colType = null;
                if (tokenizer.lastTokenWasQuoted() && tokenizer.getLastQuoteBeginPattern().equals("{")) {
                    // Weka allows the nominal value list to be appended without
                    // a space delimiter. We will get it then hanging at the
                    // name. Extract it from there and set it in the 'colType'
                    if (colName.charAt(0) == '{') {
                        // seems we only got a value list.
                        // The col name must be empty/missing then...
                        colType = colName;
                        colName = null;
                    } else {
                        int openBraceIdx = colName.indexOf('{');
                        int closeBraceIdx = colName.lastIndexOf('}');
                        colType = colName.substring(openBraceIdx + 1, closeBraceIdx);
                        colName = colName.substring(0, openBraceIdx);
                        // we ignore everything after the nominal value list
                    }
                } else {
                    colType = tokenizer.nextToken();
                }
                if ((colName == null) || (colType == null)) {
                    throw new InvalidSettingsException("Incomplete '@attribute' statement at line "
                        + tokenizer.getLineNumber() + " in ARFF file '" + filePath + "'.");
                }
                // make sure 'colType' is the last token we read before we
                // start the 'if' thing here.
                switch (colType.trim().toUpperCase()) {
                    case "REAL":
                        type = DoubleCell.TYPE;
                        // ignore whatever still comes in that line, warn though
                        readUntilEOL(tokenizer, filePath.toString());
                        break;
                    case "INTEGER":
                        type = IntCell.TYPE;
                        // ignore whatever still comes in that line, warn though
                        readUntilEOL(tokenizer, filePath.toString());
                        break;
                    case "STRING":
                        type = StringCell.TYPE;
                        // ignore whatever still comes in that line, warn though
                        readUntilEOL(tokenizer, filePath.toString());
                        break;
                    case "NUMERIC":
                    case "DATE":
                        type = DataType.getType(UnmaterializedCell.class); // will be set later by type-guesser
                        readUntilEOL(tokenizer, null);
                        break;
                    default: //NOSONAR
                        if (tokenizer.lastTokenWasQuoted() && tokenizer.getLastQuoteBeginPattern().equals("{")) { //NOSONAR
                            // the braces should be still in the string
                            int openBraceIdx = colType.indexOf('{');
                            int closeBraceIdx = colType.lastIndexOf('}');
                            if ((openBraceIdx >= 0) && (closeBraceIdx > 0) && (openBraceIdx < closeBraceIdx)) {
                                colType = colType.substring(openBraceIdx + 1, closeBraceIdx);
                            }
                            // the type was a list of nominal values
                            possVals = extractNominalVals(colType, filePath.toString(), tokenizer.getLineNumber());
                            // KNIME uses string cells for nominal values.
                            type = DataType.getType(UnmaterializedCell.class); // will be set later by type-guesser
                            readUntilEOL(tokenizer, filePath.toString());
                        } else {
                            throw new InvalidSettingsException("Invalid column type" + " '" + colType
                                + "' in attribute control " + "statement in ARFF file '" + filePath + "' at line "
                                + tokenizer.getLineNumber() + ".");
                        }
                }
                final var dcsc = new DataColumnSpecCreator(colName, type);
                if (possVals != null) {
                    dcsc.setDomain(new DataColumnDomainCreator(possVals).createDomain());
                }
                colSpecs.add(dcsc.createSpec());

            } else if (token.equalsIgnoreCase("@RELATION")) {
                tableName = tokenizer.nextToken();
                if (tableName == null) {
                    throw new InvalidSettingsException("Incomplete '@relation' statement at line "
                        + tokenizer.getLineNumber() + " in ARFF file '" + filePath + "'.");
                }
                // we just ignore the name of the data set.
                readUntilEOL(tokenizer, null);
            } else if (token.charAt(0) == '@') {
                // OOps. What's that?!?
                LOGGER.warn("ARFF reader WARNING: Unsupported control " + "statement '" + token + "' in line "
                    + tokenizer.getLineNumber() + ". Ignoring it! File: " + filePath);
                readUntilEOL(tokenizer, null);
            } else if (!token.equals("\n")) {
                LOGGER.warn("ARFF reader WARNING: Unsupported " + "statement '" + token + "' in header of ARFF file '"
                    + filePath + "', line " + tokenizer.getLineNumber() + ". Ignoring it!");
                readUntilEOL(tokenizer, null);
            } // else ignore empty lines

        }

        // check uniqueness of column names
        HashSet<String> colNames = new HashSet<>();
        for (var c = 0; c < colSpecs.size(); c++) {
            if (!colNames.add(colSpecs.get(c).getName())) {
                throw new InvalidSettingsException(
                    "Two attributes with equal names defined in header of file '" + filePath + "'.");
            }
        }
        return new DataTableSpec(tableName, colSpecs.toArray(new DataColumnSpec[colSpecs.size()]));
    }

    /**
     * Returns a settings object used to read the ARFF file header.
     */
    private static TokenizerSettings getTokenizerHeaderSettings() {
        final var settings = new TokenizerSettings();
        // add the ARFF single line comment
        settings.addSingleLineCommentPattern("%", false, false);
        // LF is a row seperator - add it as delimiter
        settings.addDelimiterPattern("\n", /* combine multiple= */true, /* return as token= */true,
            /* include in token= */false);
        // ARFF knows single and double quotes
        settings.addQuotePattern("'", "'");
        settings.addQuotePattern("\"", "\"");
        // the nominal values list will be quoted into one token (but the
        // braces must stay in)
        settings.addQuotePattern("{", "}", true);
        // the attribute statement and arguments are separated by space(s)
        settings.addDelimiterPattern(" ", true, false, false);
        // or tabs
        settings.addDelimiterPattern("\t", true, false, false);
        // and a combination of them
        settings.setCombineMultipleDelimiters(true);

        return settings;
    }

    /**
     * Expects the list of nominal values (in curely braces and comma separated) from the "@attribute" line to be next
     * in the tokenizer (including the beginning of the list with the iopening brace). Will return an array of
     * StringsCells with the different values extracted (and removed) from the tokenizer. It will leave the EOL at the
     * end of the list in the tokenizer. Pass in also file name for nice error messages.
     */
    private static DataCell[] extractNominalVals(final String valList, final String fileName, final int lineNo) {

        Collection<DataCell> vals = new LinkedHashSet<>();

        // we must support quotes and stuff - let's use another tokenizer.
        final var strReader = new StringReader(valList);
        final var tokizer = new Tokenizer(strReader);
        final var tokSets = new TokenizerSettings();
        tokSets.addDelimiterPattern(",", false, false, false);
        tokSets.addQuotePattern("'", "'");
        tokSets.addQuotePattern("\"", "\"");
        tokizer.setSettings(tokSets);

        for (String val = tokizer.nextToken(); val != null; val = tokizer.nextToken()) {

            String newval = val;
            // trimm off any whitespaces.
            if (!tokizer.lastTokenWasQuoted()) {
                newval = val.trim();
            }

            // make sure we don't add the same value twice.
            final var newValCell = new StringCell(newval);
            if (!vals.contains(newValCell)) {
                vals.add(newValCell);
            } else {
                LOGGER.warn("ARFF reader WARNING: The list of nominal " + "values in the header of file '" + fileName
                    + "' line " + lineNo + " contains the value '" + newval + "' twice. Ignoring one appearance.");
            }
        }
        return vals.toArray(new DataCell[vals.size()]);
    }

    /**
     * Reads from the tokenizer until it reads a token containing '\n'. The tokenizer must be set up to consider \n as a
     * token delimiter and to return it as separate token. If a filename is passed its considered being a flag to
     * indicate that we are not really expecting anything and this method will print a warning if the first token it
     * reads is NOT the EOL.
     */
    private static void readUntilEOL(final Tokenizer tizer, final String filename) {
        String token = tizer.nextToken();

        while ((token != null) && !token.equals("\n")) { // EOF is also EOL
            token = tizer.nextToken();
            if (filename != null) {
                LOGGER.warn("ARFF reader WARNING: Ignoring extra " + "characters in header of file '" + filename
                    + "' line " + tizer.getLineNumber() + ".");
            }
        }
    }
}
