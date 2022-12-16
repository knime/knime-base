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
 * --------------------------------------------------------------------
 *
 * History
 *   03.07.2007 (cebron): created
 */
package org.knime.base.node.preproc.colconvert.stringtonumber2;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.MissingCell;
import org.knime.core.data.RowKey;
import org.knime.core.data.StringValue;
import org.knime.core.data.container.AbstractCellFactory;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.LongCell;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.KNIMEException;
import org.knime.core.node.KNIMEException.KNIMERuntimeException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.message.Message;
import org.knime.core.node.message.MessageBuilder;
import org.knime.core.node.streamable.simple.SimpleStreamableFunctionWithInternalsNodeModel;
import org.knime.core.node.streamable.simple.SimpleStreamableOperatorInternals;

/**
 * The NodeModel for the String to Number Node that converts strings to numbers.
 *
 * @author Johannes Schweig
 * @param <T> SettingsModel for a ColumnFilter component
 * @since 4.0
 */
public abstract class AbstractStringToNumberNodeModel<T extends SettingsModel>
    extends SimpleStreamableFunctionWithInternalsNodeModel<SimpleStreamableOperatorInternals> {

    /**
     * The possible types that the string can be converted to.
     */
    public static final DataType[] POSSIBLETYPES = new DataType[]{DoubleCell.TYPE, IntCell.TYPE, LongCell.TYPE};

    /* Node Logger of this class. */
    private static final NodeLogger LOGGER = NodeLogger.getLogger(AbstractStringToNumberNodeModel.class);

    /*
     * Config key for the operator internals to propagate error messages.
     */
    private static final String CFG_KEY_ERROR_MESSAGES = "error_message";

    /**
     * Key for the included columns in the NodeSettings.
     */
    public static final String CFG_INCLUDED_COLUMNS = "include";

    /**
     * Key for the decimal separator in the NodeSettings.
     */
    public static final String CFG_DECIMALSEP = "decimal_separator";

    /**
     * Key for the thousands separator in the NodeSettings.
     */
    public static final String CFG_THOUSANDSSEP = "thousands_separator";

    /**
     * Key for the parsing type in the NodeSettings.
     */
    public static final String CFG_PARSETYPE = "parse_type";

    /**
     * Key for parsing with optional trailing {@code d}, {@code f}, {@code l}.
     *
     * @since 2.12
     */
    public static final String CFG_GENERIC_PARSE = "generic_parse";

    /**
     * Key for the option to fail execution on parsing error.
     *
     * @since 4.7
     */
    public static final String CFG_FAIL_ON_ERROR = "fail_on_error";

    /**
     * The default decimal separator.
     */
    public static final String DEFAULT_DECIMAL_SEPARATOR = ".";

    /**
     * The default thousands separator.
     */
    public static final String DEFAULT_THOUSANDS_SEPARATOR = "";

    /**
     * By default do not accept type suffices.
     *
     * @since 2.12
     */
    public static final boolean DEFAULT_GENERIC_PARSE = false;

    /**
     * To guarantee backward compatibility, we don't enable fail-on-error by default.
     *
     * @since 4.7
     */
    public static final boolean DEFAULT_FAIL_ON_ERROR = false;

    /** For compatibility reasons accept type suffices. */
    static final boolean COMPAT_GENERIC_PARSE = true;

    /** The included columns component. */
    private final T m_inclCols;

    /** The decimal separator. */
    private String m_decimalSep = DEFAULT_DECIMAL_SEPARATOR;

    /** The thousands separator. */
    private String m_thousandsSep = DEFAULT_THOUSANDS_SEPARATOR;

    private DataType m_parseType = POSSIBLETYPES[0];

    private boolean m_genericParse = DEFAULT_GENERIC_PARSE;

    private boolean m_failOnError = DEFAULT_FAIL_ON_ERROR;

    /**
     * Constructor with one inport and one outport.
     *
     * @param inclCols SettingsModel for a ColumnFilter component
     */
    public AbstractStringToNumberNodeModel(final T inclCols) {
        super(SimpleStreamableOperatorInternals.class);
        m_inclCols = inclCols;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {
        ColumnRearranger colre = createColumnRearranger(inSpecs[0]);
        DataTableSpec newspec = colre.createSpec();
        return new DataTableSpec[]{newspec};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec)
        throws Exception {
        DataTableSpec inspec = inData[0].getDataTableSpec();
        String[] inclcols = getInclCols(inspec);
        if (inclcols.length == 0) {
            // nothing to convert, let's return the input table.
            setWarningMessage("No columns selected, returning input table.");
            return new BufferedDataTable[]{inData[0]};
        }

        SimpleStreamableOperatorInternals internals = createStreamingOperatorInternals();
        ColumnRearranger colre = createColumnRearranger(inspec, internals);

        BufferedDataTable resultTable = exec.createColumnRearrangeTable(inData[0], colre, exec);

        warningMessage(internals);

        return new BufferedDataTable[]{resultTable};
    }

    /**
     * @param spec the current DataTableSpec
     * @return an integer array with the column indices
     * @throws InvalidSettingsException
     *
     */
    public int[] findColumnIndices(final DataTableSpec spec) throws InvalidSettingsException {
        final String[] inclCols = getInclCols(spec);
        final StringBuilder warnings = new StringBuilder();
        if (inclCols.length == 0) {
            warnings.append("No columns selected");
        }
        final ArrayList<Integer> indices = new ArrayList<>();
        if (isKeepAllSelected()) {
            keepAll(spec, indices);
        } else {
            keepSelected(spec, inclCols, warnings, indices);
        }
        if (warnings.length() > 0) {
            setWarningMessage(warnings.toString());
        }
        return indices.stream()//
            .mapToInt(Integer::intValue)//
            .toArray();
    }

    private static void keepSelected(final DataTableSpec spec, final String[] inclCols, final StringBuilder warnings,
        final List<Integer> indices) throws InvalidSettingsException {
        for (int i = 0; i < inclCols.length; i++) {
            final int colIndex = spec.findColumnIndex(inclCols[i]);
            if (colIndex >= 0) {
                final DataType type = spec.getColumnSpec(colIndex).getType();
                if (type.isCompatible(StringValue.class)) {
                    indices.add(colIndex);
                } else {
                    warnings.append("Ignoring column \"" + spec.getColumnSpec(colIndex).getName() + "\"\n");
                }
            } else {
                throw new InvalidSettingsException("Column \"" + inclCols[i] + "\" not found.");
            }
        }
    }

    private static void keepAll(final DataTableSpec spec, final List<Integer> indices) {
        for (final DataColumnSpec cspec : spec) {
            if (cspec.getType().isCompatible(StringValue.class)) {
                indices.add(spec.findColumnIndex(cspec.getName()));
            }
        }
    }

    /**
     * Returns all present includes from a DataTableSpec.
     *
     * @param inSpec the current DataTableSpec
     * @return a String array with the included columns
     * @since 4.5
     */
    protected abstract String[] getInclCols(final DataTableSpec inSpec);

    /**
     * @return returns true if the keep all selected checkbox is checked, false if it is not checked or not present
     */
    protected abstract boolean isKeepAllSelected();

    private void warningMessage(final SimpleStreamableOperatorInternals internals) {
        try {
            Message.load(internals.getConfig().getConfig(CFG_KEY_ERROR_MESSAGES)).ifPresent(this::setWarning);
        } catch (InvalidSettingsException e) {
            LOGGER.debug("Unable to restore warning message from streaming operators", e);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @since 3.1
     */
    @Override
    protected ColumnRearranger createColumnRearranger(final DataTableSpec spec,
        final SimpleStreamableOperatorInternals emptyInternals) throws InvalidSettingsException {
        int[] indices = findColumnIndices(spec);
        final MessageBuilder messageBuilder = createMessageBuilder();
        ConverterFactory converterFac = new ConverterFactory(indices, spec, m_parseType, messageBuilder, emptyInternals);
        ColumnRearranger colre = new ColumnRearranger(spec);
        String[] inclcols = getInclCols(spec);
        if (inclcols.length == 0) {
            // nothing to convert, let's return the input table.
            messageBuilder.withSummary("No columns selected, returning input table.").build().orElseThrow()
                .saveTo(emptyInternals.getConfig().addConfig(CFG_KEY_ERROR_MESSAGES));
            return colre;
        }
        colre.replace(converterFac, indices);
        return colre;
    }

    /**
     * {@inheritDoc}
     *
     * @since 3.1
     */
    @Override
    protected SimpleStreamableOperatorInternals
        mergeStreamingOperatorInternals(final SimpleStreamableOperatorInternals[] operatorInternals) {
        Message message = null;
        for (int i = 0; i < operatorInternals.length; i++) {
            if (message == null) { // only remember first
                try {
                    var errorMsgConfig = operatorInternals[i].getConfig().getConfig(CFG_KEY_ERROR_MESSAGES);
                    message = Message.load(errorMsgConfig).orElse(null);
                } catch (InvalidSettingsException e) {
                    //if no warning message has been set -> nothing to do
                }
            }
        }
        SimpleStreamableOperatorInternals res = new SimpleStreamableOperatorInternals();
        if (message != null) {
            message.saveTo(res.getConfig().addConfig(CFG_KEY_ERROR_MESSAGES));
        }
        return res;
    }

    /**
     * {@inheritDoc}
     *
     * @since 3.1
     */
    @Override
    protected void finishStreamableExecution(final SimpleStreamableOperatorInternals operatorInternals) {
        warningMessage(operatorInternals);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        // empty
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_inclCols.loadSettingsFrom(settings);
        m_decimalSep = settings.getString(CFG_DECIMALSEP, DEFAULT_DECIMAL_SEPARATOR);
        m_thousandsSep = settings.getString(CFG_THOUSANDSSEP, DEFAULT_THOUSANDS_SEPARATOR);
        m_parseType = settings.getDataType(CFG_PARSETYPE, POSSIBLETYPES[0]);
        // added in 2.12
        m_genericParse = settings.getBoolean(CFG_GENERIC_PARSE, COMPAT_GENERIC_PARSE);
        // added in 4.7
        m_failOnError = settings.getBoolean(CFG_FAIL_ON_ERROR, DEFAULT_FAIL_ON_ERROR);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_inclCols.saveSettingsTo(settings);
        settings.addString(CFG_DECIMALSEP, m_decimalSep);
        settings.addString(CFG_THOUSANDSSEP, m_thousandsSep);
        settings.addDataType(CFG_PARSETYPE, m_parseType);
        // added in 2.12
        settings.addBoolean(CFG_GENERIC_PARSE, m_genericParse);
        // added in 4.7
        settings.addBoolean(CFG_FAIL_ON_ERROR, m_failOnError);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_inclCols.validateSettings(settings);
        String decimalsep = settings.getString(CFG_DECIMALSEP, DEFAULT_DECIMAL_SEPARATOR);
        String thousandssep = settings.getString(CFG_THOUSANDSSEP, DEFAULT_THOUSANDS_SEPARATOR);
        if (decimalsep == null || thousandssep == null) {
            throw new InvalidSettingsException("Separators must not be null");
        }
        if (decimalsep.length() > 1 || thousandssep.length() > 1) {
            throw new InvalidSettingsException("Illegal separator length, expected a single character");
        }

        if (decimalsep.equals(thousandssep)) {
            throw new InvalidSettingsException("Decimal and thousands separator must not be the same.");
        }
        DataType myType = settings.getDataType(CFG_PARSETYPE, POSSIBLETYPES[0]);
        boolean found = false;
        for (DataType type : POSSIBLETYPES) {
            if (type.equals(myType)) {
                found = true;
            }
        }
        if (!found) {
            throw new InvalidSettingsException("Illegal parse type: " + myType);
        }

        //No need to check generic parse.
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        // empty.
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        // empty.
    }

    /**
     * @return the included columns
     */
    protected T getInclCols() {
        return m_inclCols;
    }

    /**
     * The CellFactory to produce the new converted cells.
     *
     * @author cebron, University of Konstanz
     */
    private class ConverterFactory extends AbstractCellFactory {

        /*
         * Column indices to use.
         */
        private int[] m_colindices;

        /*
         * Original DataTableSpec.
         */
        private DataTableSpec m_spec;

        private long m_rowIndex;

        private final MessageBuilder m_messageBuilder;

        private DataType m_type;

        /* streamable operator internals to propagate the error messages */
        private SimpleStreamableOperatorInternals m_internals;

        /**
         *
         * @param colindices the column indices to use.
         * @param spec the original DataTableSpec.
         * @param type the {@link DataType} to convert to.
         * @param messageBuilder a blank message builder to add errors/problems to.
         */
        ConverterFactory(final int[] colindices, final DataTableSpec spec, final DataType type,
            final MessageBuilder messageBuilder, final SimpleStreamableOperatorInternals internals) {
            m_colindices = colindices;
            m_spec = spec;
            m_type = type;
            m_internals = internals;
            m_messageBuilder = messageBuilder;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public DataCell[] getCells(final DataRow row) {
            DataCell[] newcells = new DataCell[m_colindices.length];
            for (int i = 0; i < newcells.length; i++) {
                DataCell dc = row.getCell(m_colindices[i]);
                // should be a DoubleCell, otherwise copy original cell.
                if (!dc.isMissing()) {
                    final String s = ((StringValue)dc).getStringValue();
                    if (s.trim().length() == 0) {
                        newcells[i] = DataType.getMissingCell();
                        continue;
                    }
                    try {
                        String corrected = s;
                        if (m_thousandsSep != null && m_thousandsSep.length() > 0) {
                            // remove thousands separator
                            corrected = s.replaceAll(Pattern.quote(m_thousandsSep), "");
                        }
                        if (!".".equals(m_decimalSep)) {
                            if (corrected.contains(".")) {
                                throw new NumberFormatException("Invalid floating point number");
                            }
                            if (m_decimalSep != null && m_decimalSep.length() > 0 && m_type.equals(DoubleCell.TYPE)) {
                                // replace custom separator with standard if a floating point format was selected
                                corrected = corrected.replaceAll(Pattern.quote(m_decimalSep), ".");
                            }
                        }

                        if (!m_genericParse) {
                            corrected = check(corrected);
                        }
                        if (m_type.equals(DoubleCell.TYPE)) {
                            double parsedDouble = Double.parseDouble(corrected);
                            newcells[i] = new DoubleCell(parsedDouble);
                        } else if (m_type.equals(IntCell.TYPE)) {
                            int parsedInteger = Integer.parseInt(corrected);
                            newcells[i] = new IntCell(parsedInteger);
                        } else if (m_type.equals(LongCell.TYPE)) {
                            long parsedLong = Long.parseLong(corrected);
                            newcells[i] = new LongCell(parsedLong);
                        } else {
                            m_messageBuilder.addRowIssue(0, i, m_rowIndex, "No valid parse type.");
                        }
                    } catch (NumberFormatException e) {
                        handleParseException(row, m_colindices[i], s, e);
                        newcells[i] = new MissingCell(e.getMessage());
                    }
                } else {
                    newcells[i] = DataType.getMissingCell();
                }
            }
            return newcells;
        }

        @Override
        public void setProgress(final long curRowNr, final long rowCount, final RowKey lastKey,
            final ExecutionMonitor exec) {
            super.setProgress(curRowNr, rowCount, lastKey, exec);
            m_rowIndex = curRowNr;
        }

        /**
         * Handles the number parse exception, either by failing the execution or by recording the the error.
         * @param column Column index
         * @param row Row
         * @param value DataCell value at which the parsing failed
         * @param e NumberFormatException
         *
         * @throws KNIMERuntimeException if fail on error is set...
         */
        private void handleParseException(final DataRow row, final int column,
            final String value, final Exception e) throws KNIMERuntimeException {
            String columnName = m_spec.getColumnSpec(column).getName();
            Supplier<String> message = () -> String.format(
                "%s in cell [\"%s\", column \"%s\", row %d] can not be transformed into a number", //
                value == null ? "<null>" : ("\"" + StringUtils.abbreviate(value, 15) + "\""), //
                    StringUtils.abbreviate(row.getKey().getString(), 15), //
                    columnName, //
                    m_rowIndex + 1); // messages to the user are "number based"
            if (m_messageBuilder.getIssueCount() == 0) {
                m_messageBuilder.withSummary(message.get());
            }
            m_messageBuilder.addRowIssue(0, column, m_rowIndex, e.getMessage());
            if (m_failOnError) {
                throw KNIMEException.of(getMessage().orElseThrow()).toUnchecked();
            } else {
                if (m_messageBuilder.getIssueCount() == 1L) { // first error
                    LOGGER.debug(e.getMessage());
                }
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public DataColumnSpec[] getColumnSpecs() {
            DataColumnSpec[] newcolspecs = new DataColumnSpec[m_colindices.length];
            for (int i = 0; i < newcolspecs.length; i++) {
                DataColumnSpec colspec = m_spec.getColumnSpec(m_colindices[i]);
                DataColumnSpecCreator colspeccreator = null;
                if (m_type.equals(DoubleCell.TYPE)) {
                    // change DataType to DoubleCell
                    colspeccreator = new DataColumnSpecCreator(colspec.getName(), DoubleCell.TYPE);
                } else if (m_type.equals(IntCell.TYPE)) {
                    // change DataType to IntCell
                    colspeccreator = new DataColumnSpecCreator(colspec.getName(), IntCell.TYPE);
                } else if (m_type.equals(LongCell.TYPE)) {
                    // change DataType to LongCell
                    colspeccreator = new DataColumnSpecCreator(colspec.getName(), LongCell.TYPE);
                } else {
                    colspeccreator =
                        new DataColumnSpecCreator("Invalid parse mode", DataType.getMissingCell().getType());
                }
                newcolspecs[i] = colspeccreator.createSpec();
            }
            return newcolspecs;
        }

        @Override
        public void afterProcessing() {
            final var messageConfig = m_internals.getConfig().addConfig(CFG_KEY_ERROR_MESSAGES);
            getMessage().ifPresent(m -> m.saveTo(messageConfig));
        }

        private Optional<Message> getMessage() {
            long parseErrorCount = m_messageBuilder.getIssueCount();
            if (parseErrorCount == 0L) {
                return Optional.empty();
            } else if (parseErrorCount == 1L) {
                // message has reasonable summary set (the first and only issue)
            } else {
                m_messageBuilder.withSummary(String.format("Problems in %d rows, first error: %s", parseErrorCount,
                    m_messageBuilder.getSummary().orElseThrow()));
            }
            return m_messageBuilder.build();
        }

    } // end ConverterFactory

    /**
     * @param corrected A potential number as a (non-empty){@link String}.
     * @return The original value.
     * @throws NumberFormatException when the value ends in {@code d} or {@code f}.
     * @since 2.12
     */
    public static String check(final String corrected) {
        char c = Character.toLowerCase(corrected.charAt(corrected.length() - 1));
        switch (c) {
            //case 'l': //int/long do not parse with l suffix, nor double
            case 'd': //intentional fall-through
            case 'f':
                throw new NumberFormatException(corrected + " is invalid because of its suffix.");
            default:
                return corrected;
        }
    }
}
