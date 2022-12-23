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
 * History
 *   23.01.2019 (Temesgen H. Dadi): created
 */
package org.knime.base.node.preproc.columnappend2;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.container.CloseableRowIterator;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.context.ports.PortsConfiguration;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.streamable.InputPortRole;
import org.knime.core.node.streamable.OutputPortRole;
import org.knime.core.node.streamable.PartitionInfo;
import org.knime.core.node.streamable.PortInput;
import org.knime.core.node.streamable.PortOutput;
import org.knime.core.node.streamable.RowInput;
import org.knime.core.node.streamable.RowOutput;
import org.knime.core.node.streamable.StreamableOperator;
import org.knime.core.node.util.ButtonGroupEnumInterface;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.util.UniqueNameGenerator;
import org.knime.core.webui.node.dialog.impl.Schema;

/**
 * This is the model implementation of ColumnAppender.
 *
 * @author Temesgen H. Dadi, KNIME GmbH, Berlin, Germany
 */

@SuppressWarnings("restriction")
final class ColumnAppender2NodeModel extends NodeModel {

    /** Key for storing the selected way of getting rowIDs. */
    static final String KEY_SELECTED_ROWID_MODE = "selected_rowid_mode";

    /** Key for storing the table (port index) used for rowIDs. */
    static final String KEY_SELECTED_ROWID_TABLE = "selected_rowid_table";

    static final String KEY_SELECTED_ROWID_TABLE_NUMBER = "selected_rowid_table_number";

    static final int NOT_SET = -1;

    /** Different options of getting rowIDs. */
    private final SettingsModelString m_rowIDModeSettings = createRowIDModeSelectModel();


    /**
     * Old zero-based setting for the row id table index
     */
    private int m_rowIdTable;

    /**
     * New setting one-based setting for the row id table number
     */
    private int m_rowIdTableNumber;

    private final int m_numInPorts;

    static SettingsModelString createRowIDModeSelectModel() {
        return new SettingsModelString(KEY_SELECTED_ROWID_MODE, RowKeyMode.IDENTICAL.getActionCommand());
    }

    /**
     * Constructor for dynamic ports.
     *
     * @param portsConfiguration the ports configuration
     */
    ColumnAppender2NodeModel(final PortsConfiguration portsConfiguration) {
        super(portsConfiguration.getInputPorts(), portsConfiguration.getOutputPorts());
        m_numInPorts = portsConfiguration.getInputPorts().length;
    }

    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {
        // Sanity check settings even though dialog checks, in case any flow variables went bad.
        if (isSelectedMode(RowKeyMode.KEY_TABLE)) {
            CheckUtils.checkSetting(getRowIDTableIndex() < m_numInPorts,
                "The selected port number for row key must be a number between 1 and %s (number of the last port)",
                m_numInPorts);
        }
        warnAboutOldSettingOverwrittenIfNecessary();
        final DataTableSpec spec = combineToSingleSpec(createUniqueSpecs(inSpecs));
        return new DataTableSpec[]{spec};
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_rowIDModeSettings.saveSettingsTo(settings);
        settings.addInt(KEY_SELECTED_ROWID_TABLE, m_rowIdTable);
        settings.addInt(KEY_SELECTED_ROWID_TABLE_NUMBER, m_rowIdTableNumber);
    }

    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_rowIDModeSettings.loadSettingsFrom(settings);
        m_rowIdTable = settings.getInt(KEY_SELECTED_ROWID_TABLE);
        m_rowIdTableNumber = settings.getInt(KEY_SELECTED_ROWID_TABLE_NUMBER, NOT_SET);
    }

    private void warnAboutOldSettingOverwrittenIfNecessary() {
        if (m_rowIdTable != NOT_SET && m_rowIdTableNumber != NOT_SET) {
            // the dialog was opened in 5.0 but rowIdTable is overwritten by a flow variable
            setWarningMessage(String.format(
                "The deprecated setting with key '%s' is overwritten by a flow variable. "
                        + "Please overwrite '%s' instead but keep in mind that a value of 1 now "
                        + "corresponds to the first input table.",
                        KEY_SELECTED_ROWID_TABLE, KEY_SELECTED_ROWID_TABLE_NUMBER));
        }
    }

    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_rowIDModeSettings.validateSettings(settings);
    }

    @Override
    protected void loadInternals(final File internDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        /* Nothing to do here. */
    }

    @Override
    protected void saveInternals(final File internDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        /* Nothing to do here. */
    }

    @Override
    protected void reset() {
        /* Nothing to do here. */
    }

    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec)
        throws Exception {
        final BufferedDataTable out;
        if (isSelectedMode(RowKeyMode.IDENTICAL)) {
            out = joinTablesWithIdenticalKeys(inData, exec);
        } else {
            out = createNewTable(inData, exec);
        }
        return new BufferedDataTable[]{out};
    }

    /**
     * Given an array of BufferedDataTable, it returns their corresponding specs.
     *
     * @param inData an array of BufferedDataTable
     * @return an array of DataTableSpec
     */
    private static DataTableSpec[] getSpecFromInput(final BufferedDataTable[] inData) {
        return Arrays.stream(inData)//
            .map(BufferedDataTable::getDataTableSpec)//
            .toArray(DataTableSpec[]::new);
    }

    /**
     * Given an array of DataTableSpec, it returns same sized array of DataTableSpec in which all column names are
     * unique across all of the array elements. Redundant column names are resolved by appending "(#1)", "(#2)", and so
     * on as required.
     *
     * @param inSpecs an array of DataTableSpec
     * @return a concatenation of the input Specs
     */
    private static DataTableSpec[] createUniqueSpecs(final DataTableSpec[] inSpecs) {

        final UniqueNameGenerator nameGen = new UniqueNameGenerator((DataTableSpec)null);

        final DataTableSpec[] uniqDataTableSpecs = new DataTableSpec[inSpecs.length];
        for (int i = 0; i < inSpecs.length; i++) {
            final DataTableSpec curSpec = inSpecs[i];
            final DataColumnSpec[] outColSpecs = new DataColumnSpec[curSpec.getNumColumns()];
            for (int j = 0; j < curSpec.getNumColumns(); j++) {
                outColSpecs[j] = nameGen.newCreator(curSpec.getColumnSpec(j)).createSpec();
            }
            uniqDataTableSpecs[i] = new DataTableSpec(outColSpecs);
        }
        return uniqDataTableSpecs;
    }

    /**
     * Creates a single combined DataTableSpec from an array while respecting the order of the input DataTableSpecs.
     *
     * @param inSpecs an array of DataTableSpec
     * @return a combined DataTableSpec
     */
    private static DataTableSpec combineToSingleSpec(final DataTableSpec[] inSpecs) {
        return Arrays.stream(inSpecs) //
            .reduce(new DataTableSpec(), DataTableSpec::new);
    }

    /**
     * Provided an array of BufferedDataTable with equal row count and identical RowIDs it returns a joined single
     * BufferedDataTable where the columns of each BufferedDataTable are appended in respective order.
     *
     * @param inData an array of BufferedDataTable
     * @param exec the execution context
     * @return a combined BufferedDataTable containing all the columns from all tables
     * @throws CanceledExecutionException - If the execution is canceled by the user
     */
    private static BufferedDataTable joinTablesWithIdenticalKeys(final BufferedDataTable[] inData,
        final ExecutionContext exec) throws CanceledExecutionException {
        final DataTableSpec[] outTableSpec = createUniqueSpecs(getSpecFromInput(inData));
        BufferedDataTable out = null;
        for (int i = 1; i < inData.length; i++) {
            final BufferedDataTable uniqufiedcolNamesTable = exec.createSpecReplacerTable(inData[i], outTableSpec[i]);
            if (i == 1) {
                out = exec.createJoinedTable(inData[0], uniqufiedcolNamesTable, exec);
            } else {
                out = exec.createJoinedTable(out, uniqufiedcolNamesTable, exec);
            }
        }
        return out;
    }

    /**
     * Provided an array of BufferedDataTable, it returns a joined single BufferedDataTable where the columns of each
     * BufferedDataTable are appended in respective order.
     *
     * @param inData an array of BufferedDataTable
     * @param exec the execution context
     * @return a combined BufferedDataTable containing all the columns from all tables
     * @throws InterruptedException - If the execution is canceled by the user
     * @throws CanceledExecutionException - If the execution is canceled by the user
     */
    private BufferedDataTable createNewTable(final BufferedDataTable[] inData, final ExecutionContext exec)
        throws InterruptedException, CanceledExecutionException {

        final BufferedDataContainer outDataContainer =
            exec.createDataContainer(combineToSingleSpec(createUniqueSpecs((getSpecFromInput(inData)))));

        /* Get the row count of the selected table or the longest table / in case of generated row keys. */
        final int decidingTblIdx = getDecidingTableIndex();

        final MultipleRowIterators iter =
            new MultipleRowIterators(inData, decidingTblIdx, isSelectedMode(RowKeyMode.IDENTICAL));

        compute(exec, outDataContainer::addRowToTable, iter, getOutputRowCount(inData, decidingTblIdx));
        outDataContainer.close();
        return outDataContainer.getTable();
    }

    /**
     * Calculates the number of rows written to the output.
     *
     * @param inData an array of BufferedDataTable
     * @param decidingTblIdx the index of the table that provides the RowIDs.
     * @return the total number of rows written to the output
     */
    private static long getOutputRowCount(final BufferedDataTable[] inData, final int decidingTblIdx) {
        final long[] rowCounts = Arrays.stream(inData).mapToLong(BufferedDataTable::size).toArray();
        final long totalNumRows =
            (decidingTblIdx == -1) ? Arrays.stream(rowCounts).max().getAsLong() : rowCounts[decidingTblIdx];
        return totalNumRows;
    }

    /**
     * Combines the rows to create new table.
     *
     * @param exec The execution context
     * @param output the row consumer
     * @param iters a wrapped array of RowIterators
     * @param totalNumRows the total number of rows at the output table
     * @throws InterruptedException - If the execution is canceled by the user
     * @throws CanceledExecutionException - If the execution is canceled by the user
     */
    private void compute(final ExecutionContext exec, final RowConsumer output, final MultipleRowIterators iters,
        final long totalNumRows) throws InterruptedException, CanceledExecutionException {
        try {
            while (iters.hasNext()) {
                if (totalNumRows != -1) {
                    final long currRowIndex = iters.getCurrentRowIndex();
                    exec.setProgress(currRowIndex / (double)totalNumRows,
                        () -> "Appending columns (row " + currRowIndex + "/" + totalNumRows + ")");
                }
                exec.checkCanceled();
                output.consume(iters.next());
            }
            differingTableSizeMsg(iters);
        } finally {
            iters.close();
        }
    }

    /**
     * A helper method that checks if the configured mode equals the provided mode.
     *
     * @param toCheck the mode to check if it equals the configured mode
     * @return {@code true} if configured equals provided mode
     */
    private final boolean isSelectedMode(final RowKeyMode toCheck) {
        return RowKeyMode.valueOf(m_rowIDModeSettings.getStringValue()) == toCheck;
    }

    /**
     * A helper method that gets the selected table index used for getting row keys.
     *
     * @return the selected table index, if keys are taken from a selected table, -1 otherwise
     */
    private final int getDecidingTableIndex() {
        if (isSelectedMode(RowKeyMode.KEY_TABLE)) {
            warnAboutOldSettingOverwrittenIfNecessary();
            return getRowIDTableIndex();
        } else {
            return -1;
        }
    }

    private int getRowIDTableIndex() {
        if (m_rowIdTable == NOT_SET) {
            // set by dialog -> the old settings was not overwritten by a flow variable
            return m_rowIdTableNumber - 1;
        } else {
            // either the dialog wasn't opened yet or the old setting is overwritten by a flow variable
            return m_rowIdTable;
        }
    }

    /**
     * By checking the final status of individual RowIterators, displays detailed warning messages. Warning is issued if
     * some of the input tables are shorter or longer than the deciding table. In the case of generating rowIDs, the
     * longest table is used for comparison.
     *
     * @param iters a wrapped array of RowIterators
     * @throws InterruptedException - If the execution is canceled by the user
     */
    private void differingTableSizeMsg(final MultipleRowIterators iters) throws InterruptedException {
        final int[] overUsedTables = iters.overUsedTables();
        final int[] underUsedTables = iters.underUsedTables();
        if (overUsedTables.length > 0 || underUsedTables.length > 0) {
            // In the case of generating rowIDs, the longest table is used as a reference for comparison
            final String refTable = isSelectedMode(RowKeyMode.KEY_TABLE)
                ? "the selected table " + iters.m_decidingTableIdx + "" : "the longest table";

            String warningMsg = "";

            // shorter tables can happen if with rowkeymode equals table or generate
            if (overUsedTables.length == 1) {
                warningMsg += "Table " + overUsedTables[0] + " is shorter than " + refTable
                    + ". Corresponding columns were filled using missing values.";
            } else if (overUsedTables.length > 1) {
                warningMsg += "Tables " + Arrays.toString(overUsedTables) + " are shorter than " + refTable
                    + ". Corresponding columns were filled using missing values.";
            }
            // longer tables can only happen if with rowkeymode equals table
            if (underUsedTables.length == 1) {
                warningMsg += warningMsg.isEmpty() ? "" : " ";
                warningMsg +=
                    "Table " + underUsedTables[0] + " is longer than " + refTable + ". Extra rows were ignored.";
            } else if (underUsedTables.length > 1) {
                warningMsg += warningMsg.isEmpty() ? "" : " ";
                warningMsg += "Tables " + Arrays.toString(underUsedTables) + " are longer than " + refTable
                    + ". Extra rows were ignored.";
            }
            setWarningMessage(warningMsg);
        }

    }

    /**
     * The data row iterator interface.
     *
     * @author Temesgen H. Dadi, KNIME GmbH, Berlin, Germany
     */
    private static interface DataRowIterator {

        /**
         * Returns {@code true} if the iteration has more elements.
         *
         * @return {@code true} if the iteration has more elements
         * @throws InterruptedException - If the execution has been canceled
         */
        boolean hasNext() throws InterruptedException;

        /**
         * Returns the next element in the iteration. In case that there a no more new rows a default DataRow is
         * returned.
         *
         * @return the next element in the iteration or an default DataRow
         * @throws InterruptedException - If the execution has been canceled
         */
        DataRow next() throws InterruptedException;

        /**
         * Flag indicating that {@link #next()} has been invoked though {@link #hasNext()} returned @{code false}.
         *
         * @return flag indicating that {@link #next()} has been invoked though no more elements where available
         */
        boolean isOverused();

        /**
         * Closes the iterator.
         */
        void close();
    }

    /**
     * An implementation of custom DataRowIterator that returns a row of missing values when it is at the end.
     *
     * @author Temesgen H. Dadi, KNIME GmbH, Berlin, Germany
     */
    private static final class SimpleDataRowIterator implements DataRowIterator {

        private final CloseableRowIterator m_rowIt;

        private final int m_numCol;

        private final DefaultRow m_emptyRow;

        private boolean m_overused = false;

        SimpleDataRowIterator(final CloseableRowIterator rowIt, final int numCol) {
            m_rowIt = rowIt;
            m_numCol = numCol;
            m_emptyRow = new DefaultRow("NoRowID",
                Stream.generate(DataType::getMissingCell).limit(m_numCol).toArray(DataCell[]::new));
        }

        @Override
        public boolean hasNext() {
            return m_rowIt.hasNext();
        }

        @Override
        public DataRow next() {
            /* Next returns a row of missing value cells if iterator is at the end. */
            if (!m_rowIt.hasNext()) {
                m_overused = true;
                return m_emptyRow;
            }
            return m_rowIt.next();
        }

        @Override
        public boolean isOverused() {
            return m_overused;
        }

        @Override
        public void close() {
            m_rowIt.close();
        }
    }

    /**
     * An implementation (streaming variant) of custom DataRowIterator that returns a row of missing values when it is
     * at the end.
     *
     * @author Temesgen H. Dadi, KNIME GmbH, Berlin, Germany
     */
    private static final class StreamableDataRowIterator implements DataRowIterator {

        private final RowInput m_rowInput;

        private DataRow m_row = null;

        private final int m_numCol;

        private final DefaultRow m_emptyRow;

        private boolean m_overused = false;

        StreamableDataRowIterator(final RowInput rowInput, final int numCol) {
            m_rowInput = rowInput;
            m_numCol = numCol;
            m_emptyRow = new DefaultRow("NoRowID",
                Stream.generate(DataType::getMissingCell).limit(m_numCol).toArray(DataCell[]::new));
        }

        @Override
        public boolean hasNext() throws InterruptedException {
            /* If hasNext() is called multiple times without calling next() in between,
             this if-clause ensures that it still returns true. */
            if (m_row == null) {
                m_row = m_rowInput.poll();
            }
            return m_row != null;
        }

        @Override
        public DataRow next() throws InterruptedException {
            /* next returns a row of missing value cells if iterator is at the end. */
            if (!hasNext()) {
                m_row = m_emptyRow;
                m_overused = true;
            }
            final DataRow row = m_row;
            m_row = null;
            return row;
        }

        @Override
        public boolean isOverused() {
            return m_overused;
        }

        @Override
        public void close() {
            m_rowInput.close();
        }
    }

    /**
     * A wrapper class with an array of DataRowIterator and convenience methods to check their status.
     *
     * @author Temesgen H. Dadi, KNIME GmbH, Berlin, Germany
     */
    private static final class MultipleRowIterators {

        private final DataRowIterator[] m_iterators;

        private final int m_iterCount;

        private final int m_decidingTableIdx;

        private final boolean m_identicalKeys;

        private long m_currentPos;

        /**
         * Constructor.
         *
         * @param rowIterators an array of row iterators
         * @param keyMode the way to handle the row keys
         * @param decidingTableIdx the table deciding the the rowID and output table length
         */
        private MultipleRowIterators(final DataRowIterator[] rowIterators, final int decidingTableIdx,
            final boolean identicalKeys) {
            m_iterators = rowIterators;
            m_iterCount = rowIterators.length;
            m_decidingTableIdx = decidingTableIdx;
            m_identicalKeys = identicalKeys;
            m_currentPos = 0;
        }

        private MultipleRowIterators(final PortInput[] rowInArray, final int decidingTblIdx,
            final boolean identicalKeys) {
            this(Arrays.stream(rowInArray)//
                .map(i -> {
                    final RowInput rowIn = (RowInput)i;//
                    return new StreamableDataRowIterator(rowIn, rowIn.getDataTableSpec().getNumColumns());
                })//
                .toArray(DataRowIterator[]::new)//
                , decidingTblIdx, identicalKeys);
        }

        private MultipleRowIterators(final BufferedDataTable[] inData, final int decidingTblIdx,
            final boolean identicalKeys) {
            this(IntStream.range(0, inData.length)//
                .mapToObj(i -> new SimpleDataRowIterator(inData[i].iterator(), inData[i].getSpec().getNumColumns()))//
                .toArray(DataRowIterator[]::new)//
                , decidingTblIdx, identicalKeys);
        }

        private boolean hasNext() throws InterruptedException {
            boolean decidingTableHasNext = false;
            int count = 0;
            /* all iterators must call hasNext(). */
            for (int i = 0; i < m_iterCount; i++) {
                if (m_iterators[i].hasNext()) {
                    count++;
                    if (i == m_decidingTableIdx) {
                        decidingTableHasNext = true;
                    }
                }
            }
            // If tables suppose to have identical keys all/none should have a next. Streaming case only.
            if (m_identicalKeys) {
                if (count == m_iterCount) { // all have next.
                    return true;
                } else if (count == 0) { // none has next.
                    return false;
                } else {
                    throw new IllegalArgumentException("Tables contain non-matching row counts!");
                }

            } else if (m_decidingTableIdx == -1) { // If row keys are generated 1 is enough.
                return count > 0;
            } else {
                return decidingTableHasNext;
            }
        }

        private DataRow next() throws InterruptedException {
            final ArrayList<DataCell> cells = new ArrayList<>();
            String rowKey = "";

            for (int i = 0; i < m_iterCount; i++) {
                final DataRow currRow = m_iterators[i].next();
                for (final DataCell cell : currRow) {
                    cells.add(cell);
                }
                if (i == m_decidingTableIdx) {
                    rowKey = currRow.getKey().getString();
                }
                /* This is useful in the streaming case.
                   Tables are not simply joined even if identical option is selected. */
                if (m_identicalKeys) {
                    if (i == 0) {
                        rowKey = currRow.getKey().getString();
                    } else if (!rowKey.equals(currRow.getKey().getString())) {
                        throw new IllegalArgumentException("Tables contain non-matching rows or are sorted "
                            + "differently. Keys in row " + m_currentPos + " do not match: \"" + rowKey
                            + "\" (first input) vs. \"" + currRow.getKey().getString() + "\" (input " + (i + 1) + ")");
                    }
                }
            }
            // generate new row key
            if (m_decidingTableIdx == -1 && !m_identicalKeys) {
                rowKey = "Row" + m_currentPos;
            }

            final DefaultRow res = new DefaultRow(rowKey, cells);
            m_currentPos++;
            return res;
        }

        private int[] overUsedTables() {
            return IntStream.range(0, m_iterCount)//
                .filter(i -> m_iterators[i].isOverused())//
                .toArray();
        }

        private int[] underUsedTables() throws InterruptedException {
            final ArrayList<Integer> indices = new ArrayList<Integer>();
            for (int i = 0; i < m_iterCount; i++) {
                if (m_iterators[i].hasNext()) {
                    indices.add(i);
                }
            }
            return indices.stream().mapToInt(i -> i).toArray();
        }

        private long getCurrentRowIndex() {
            return m_currentPos;
        }

        private void close() {
            Arrays.stream(m_iterators).forEach(DataRowIterator::close);
        }

    }

    /**
     * Row consumer interface used to allow pushing rows to {@link BufferedDataContainer} as well as {@link RowOutput}.
     *
     * @author Temesgen H. Dadi, KNIME GmbH, Berlin, Germany
     */
    @FunctionalInterface
    private static interface RowConsumer {
        void consume(DataRow row) throws InterruptedException;
    }

    /**
     * Different options used to decide the RowIDs during column appending.
     *
     * @author Temesgen H. Dadi, KNIME GmbH, Berlin, Germany
     */
    enum RowKeyMode implements ButtonGroupEnumInterface {

            @Schema(title = "Identical RowIDs and table lengths.")
            IDENTICAL("Identical row keys and table lengths"), //

            @Schema(title = "Generate new row RowIDs")
            GENERATE("Generate new row keys"), //

            @Schema(title = "Use the RowIDs from the selected input table")
            KEY_TABLE("Use the row keys from the input table: ");

        private static final String TOOLTIP = "<html>Choose the way row keys of the output tables are decided.<br>"
            + "If \"Identical row keys and table lengths\" is chosen, all input tables<br> "
            + "should have exactly the same row Ids in the exact same order.<html>";

        private final String m_text;

        RowKeyMode(final String text) {
            this.m_text = text;
        }

        @Override
        public String getText() {
            return m_text;
        }

        @Override
        public String getActionCommand() {
            return name();
        }

        @Override
        public String getToolTip() {
            return TOOLTIP;
        }

        @Override
        public boolean isDefault() {
            return this == IDENTICAL;
        }

    }

    //////////////// STREAMING FUNCTIONS ////////////////

    @Override
    public InputPortRole[] getInputPortRoles() {
        /* In-ports are non-distributed since it can't be guaranteed that the chunks at each port are of identical size. */
        return Stream.generate(() -> InputPortRole.NONDISTRIBUTED_STREAMABLE) //
            .limit(m_numInPorts) //
            .toArray(InputPortRole[]::new); //
    }

    @Override
    public OutputPortRole[] getOutputPortRoles() {
        return new OutputPortRole[]{OutputPortRole.DISTRIBUTED};
    }

    @Override
    public StreamableOperator createStreamableOperator(final PartitionInfo partitionInfo,
        final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        return new StreamableOperator() {
            @Override
            public void runFinal(final PortInput[] inputs, final PortOutput[] outputs, final ExecutionContext exec)
                throws Exception {
                final int decidingTblIdx = getDecidingTableIndex();
                /* Number of rows will be -1 for all inputs in case of streaming. */
                final long[] rowCounts = new long[m_numInPorts];
                Arrays.fill(rowCounts, -1);
                final MultipleRowIterators multiIters =
                    new MultipleRowIterators(inputs, decidingTblIdx, isSelectedMode(RowKeyMode.IDENTICAL));
                final RowOutput rowOut = (RowOutput)outputs[0];
                compute(exec, rowOut::push, multiIters, -1);
                rowOut.close();
            }
        };
    }

}
