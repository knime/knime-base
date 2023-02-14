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
 *   01.09.2009 (adae): expanded
 */
package org.knime.base.node.preproc.double2int2;

import java.io.File;
import java.io.IOException;
import java.util.Optional;
import java.util.stream.Stream;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.IntValue;
import org.knime.core.data.MissingCell;
import org.knime.core.data.RowKey;
import org.knime.core.data.container.AbstractCellFactory;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.LongCell;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.KNIMEException.KNIMERuntimeException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelColumnFilter2;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.message.Message;
import org.knime.core.node.message.MessageBuilder;
import org.knime.core.node.streamable.simple.SimpleStreamableFunctionWithInternalsNodeModel;
import org.knime.core.node.streamable.simple.SimpleStreamableOperatorInternals;
import org.knime.core.node.util.filter.InputFilter;
import org.knime.core.node.util.filter.NameFilterConfiguration;

/**
 * The NodeModel for the Number to String Node that converts doubles to integers.
 *
 * @author cebron, University of Konstanz
 * @author adae, University of Konstanz
 */
public class DoubleToIntNodeModel
    extends SimpleStreamableFunctionWithInternalsNodeModel<SimpleStreamableOperatorInternals> {

    /* Node Logger of this class. */
    private static final NodeLogger LOGGER = NodeLogger.getLogger(DoubleToIntNodeModel.class);

    /*
     * Config key for the operator internals to propagate error messages.
     */
    private static final String CFG_KEY_ERROR_MESSAGES = "error_message";

    /**
     * Key for the included columns in the NodeSettings.
     */
    public static final String CFG_INCLUDED_COLUMNS = "include";

    /**
     * Key for the ceiling (next bigger) the integer.
     */
    public static final String CFG_CEIL = "ceil";

    /**
     * Key for the flooring (cutting) the integer.
     */
    public static final String CFG_FLOOR = "floor";

    /**
     * Key for rounding the integer.
     */
    public static final String CFG_ROUND = "round";

    /**
     * Key for setting whether to produce long or int.
     *
     * @since 2.11
     */
    public static final String CFG_LONG = "long";

    /**
     * @return the settings model for the column filter
     */
    static SettingsModelColumnFilter2 createColumnFilterModel() {
        return new SettingsModelColumnFilter2(DoubleToIntNodeModel.CFG_INCLUDED_COLUMNS,
            new InputFilter<DataColumnSpec>() {

                @Override
                public boolean include(final DataColumnSpec cspec) {
                    final DataType type = cspec.getType();
                    return (type.isCompatible(DoubleValue.class) && !type.isCompatible(IntValue.class));
                }

            }, NameFilterConfiguration.FILTER_BY_NAMEPATTERN);
    }

    /**
     * Key for the type of rounding.
     */
    public static final String CFG_TYPE_OF_ROUND = "typeofround";

    /*
     * If true, long instead of integer is produced from the double values.
     */
    private SettingsModelBoolean m_prodLong = new SettingsModelBoolean(CFG_LONG, false);


    /*
     * The included columns.
     */
    private SettingsModelColumnFilter2 m_inclCols = createColumnFilterModel();

    private SettingsModelString m_calctype = new SettingsModelString(CFG_TYPE_OF_ROUND, CFG_ROUND);

    /**
     * Default constructor.
     */
    public DoubleToIntNodeModel() {
        super(SimpleStreamableOperatorInternals.class);
    }

    // unfortunately need to overwrite it to propagate error messages
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
        SimpleStreamableOperatorInternals internals = createStreamingOperatorInternals();
        ColumnRearranger rearranger = createColumnRearranger(inData[0].getSpec(), internals);
        BufferedDataTable t = exec.createColumnRearrangeTable(inData[0], rearranger, exec);
        warningMessage(internals);
        return new BufferedDataTable[]{t};
    }

    /**
     * Finds the relevant column indices to use from the input data table.
     * @param spec
     * @return column indices
     * @throws KNIMERuntimeException if a column index cannot be found.
     */
    private int[] findIndices(final DataTableSpec spec)
        throws KNIMERuntimeException {
        var result = m_inclCols.applyTo(spec);
        String[] inclcols = result.getIncludes();
        return Stream.of(inclcols).mapToInt(spec::findColumnIndex).toArray();
    }

    /**
     * Issues a warning message if one was stored in the streamable operator internals.
     * @param internals
     */
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
        final var messageBuilder = createMessageBuilder();
        int[] indices = findIndices(spec);
        ConverterFactory converterFac;
        String calctype = m_calctype.getStringValue();
        if (calctype.equals(CFG_CEIL)) {
            converterFac = new CeilConverterFactory(indices, m_prodLong.getBooleanValue(),
                spec, messageBuilder, emptyInternals);
        } else if (calctype.equals(CFG_FLOOR)) {
            converterFac = new FloorConverterFactory(indices, m_prodLong.getBooleanValue(),
                spec, messageBuilder, emptyInternals);
        } else {
            converterFac = new ConverterFactory(indices, m_prodLong.getBooleanValue(),
                spec, messageBuilder, emptyInternals);
        }

        ColumnRearranger colre = new ColumnRearranger(spec);
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
        // merge warning messages from potentially different partitions -> essentially concatenate the messages
        Message message = null;
        for (SimpleStreamableOperatorInternals oi : operatorInternals) {
            if (message == null) { // only remember first
                try {
                    var errorMsgConfig = oi.getConfig().getConfig(CFG_KEY_ERROR_MESSAGES);
                    message = Message.load(errorMsgConfig).orElse(null);
                } catch (InvalidSettingsException e) { //NOSONAR
                    //if no warning message has been set -> nothing to do
                }
            }
        }
        var res = new SimpleStreamableOperatorInternals();
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
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_inclCols.loadSettingsFrom(settings);
        m_calctype.loadSettingsFrom(settings);

        try {
            m_prodLong.loadSettingsFrom(settings);
        } catch (InvalidSettingsException ex) {
            // option add in 2.11, older workflows don't have this option
            m_prodLong.setBooleanValue(false);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_inclCols.saveSettingsTo(settings);
        m_calctype.saveSettingsTo(settings);
        m_prodLong.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_inclCols.validateSettings(settings);
        m_calctype.validateSettings(settings);

        // added in 2.11, is not present in existing workflows
        // m_prodLong.validateSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
    }

    /**
     * The CellFactory to produce the new converted cells. Standard rounding.
     *
     * @author cebron, University of Konstanz
     * @author adae, University of Konstanz
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

        /*
         * Whether long or int should be created.
         */
        private boolean m_createLong;

        private final MessageBuilder m_messageBuilder;

        /* streamable operator internals to propagate the error messages */
        private SimpleStreamableOperatorInternals m_internals;

        /**
         * @param colindices the column indices to use.
         * @param spec the original DataTableSpec.
         */
        ConverterFactory(final int[] colindices, final boolean createLong, final DataTableSpec spec,
            final MessageBuilder messageBuilder, final SimpleStreamableOperatorInternals internals) {
            m_colindices = colindices;
            m_spec = spec;
            m_createLong = createLong;
            m_messageBuilder = messageBuilder;
            m_internals = internals;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public DataCell[] getCells(final DataRow row, final long rowIndex) {
            DataCell[] newcells = new DataCell[m_colindices.length];
            for (int i = 0; i < newcells.length; i++) {
                DataCell dc = row.getCell(m_colindices[i]);

                // handle integers separately
                if (dc instanceof IntValue) {
                    if (m_createLong) {
                        newcells[i] = new LongCell(((IntValue)dc).getIntValue());
                    } else {
                        newcells[i] = dc;
                    }
                } else if (dc instanceof LongCell && m_createLong) {
                    newcells[i] = dc;
                } else if (dc instanceof DoubleValue) {
                    double d = ((DoubleValue)dc).getDoubleValue();
                    if (m_createLong) {
                        if ((d > Long.MAX_VALUE) || (d < Long.MIN_VALUE)) {
                            if (m_messageBuilder.getIssueCount() == 0L) {
                                m_messageBuilder.withSummary(String.format(
                                    "Value in row %d (\"%s\") is out of range for representation as long: %s",
                                    rowIndex + 1, row.getKey().getString(), Double.toString(d)));
                            }
                            m_messageBuilder.addRowIssue(0, m_colindices[i], rowIndex,
                                "The table contains double values that are outside the value range for longs.");
                            newcells[i] = new MissingCell("Value " + d + " is out of long range.");
                        } else {
                            newcells[i] = new LongCell(getRoundedLongValue(d));
                        }
                    } else {
                        if ((d > Integer.MAX_VALUE) || (d < Integer.MIN_VALUE)) {
                            if (m_messageBuilder.getIssueCount() == 0L) {
                                m_messageBuilder.withSummary(String.format(
                                    "Value in row %d (\"%s\") is out of range for representation as integer: %s",
                                    rowIndex + 1, row.getKey().getString(), Double.toString(d)));
                                m_messageBuilder.addResolutions("Enabling long values in the node configuration.");
                            }
                            m_messageBuilder.addRowIssue(0, m_colindices[i], rowIndex,
                                String.format("Value not in [%d, %d]", Integer.MIN_VALUE, Integer.MAX_VALUE));
                            newcells[i] = new MissingCell("Value " + d + " is out of integer range.");
                        } else {
                            newcells[i] = new IntCell(getRoundedValue(d));
                        }
                    }
                } else {
                    newcells[i] = DataType.getMissingCell();
                }
            }
            return newcells;
        }

        @Override
        public void afterProcessing() {
            final var messageConfig = m_internals.getConfig().addConfig(CFG_KEY_ERROR_MESSAGES);
            extractMessage().ifPresent(m -> m.saveTo(messageConfig));
        }

        private Optional<Message> extractMessage() {
            var parseErrorCount = m_messageBuilder.getIssueCount();
            if (parseErrorCount == 0L) {
                return Optional.empty();
            } else if (parseErrorCount > 1L) { // for value == 1L it has a proper message already
                m_messageBuilder.withSummary(String.format("Problems occurred in %d rows, first error: %s",
                    parseErrorCount, m_messageBuilder.getSummary().orElse("")));
            }
            return m_messageBuilder.build();
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
                // change DataType to IntCell
                colspeccreator =
                    new DataColumnSpecCreator(colspec.getName(), m_createLong ? LongCell.TYPE : IntCell.TYPE);
                newcolspecs[i] = colspeccreator.createSpec();
            }
            return newcolspecs;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void setProgress(final int curRowNr, final int rowCount, final RowKey lastKey,
            final ExecutionMonitor exec) {
            exec.setProgress((double)curRowNr / (double)rowCount, "Converting");
        }

        /**
         * @param val the value to be rounded
         * @return the rounded value
         */
        public int getRoundedValue(final double val) {
            return (int)Math.round(val);
        }

        /**
         * @param val the value to be rounded
         * @return the rounded value
         */
        public long getRoundedLongValue(final double val) {
            return Math.round(val);
        }

    } // end ConverterFactory

    /**
     * This Factory produces integer cells rounded to floor (next smaller int).
     *
     * @author adae, University of Konstanz
     */
    private class FloorConverterFactory extends ConverterFactory {
        /**
         * @param colindices the column indices to use.
         * @param spec the original DataTableSpec.
         */
        FloorConverterFactory(final int[] colindices, final boolean createLong, final DataTableSpec spec,
            final MessageBuilder messageBuilder, final SimpleStreamableOperatorInternals internals) {
            super(colindices, createLong, spec, messageBuilder, internals);
        }

        @Override
        public int getRoundedValue(final double val) {
            return (int)Math.floor(val);
        }

        @Override
        public long getRoundedLongValue(final double val) {
            return (long)Math.floor(val);
        }
    }

    /**
     * This Factory produces integer cells rounded to ceil (next bigger int).
     *
     * @author adae, University of Konstanz
     */
    private class CeilConverterFactory extends ConverterFactory {
        /**
         * @param colindices the column indices to use.
         * @param spec the original DataTableSpec.
         */
        CeilConverterFactory(final int[] colindices, final boolean createLong, final DataTableSpec spec,
            final MessageBuilder messageBuilder, final SimpleStreamableOperatorInternals internals) {
            super(colindices, createLong, spec, messageBuilder, internals);
        }

        @Override
        public int getRoundedValue(final double val) {
            return (int)Math.ceil(val);
        }

        @Override
        public long getRoundedLongValue(final double val) {
            return (long)Math.ceil(val);
        }
    }
}
