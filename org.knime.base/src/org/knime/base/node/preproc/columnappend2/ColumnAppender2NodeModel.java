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
import org.knime.core.data.RowIterator;
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
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
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

/**
 * This is the model implementation of ColumnAppender.
 *
 * @author Temesgen H. Dadi, KNIME GmbH, Berlin, Germany
 */
final class ColumnAppender2NodeModel extends NodeModel {

    /* Key for storing the selected way of getting rowIDs. */
    private static final String KEY_SELECTED_ROWID_MODE = "selected_rowid_mode";

    /* Key for storing the table (port index) used for rowIDs. */
    private static final String KEY_SELECTED_ROWID_TABLE = "selected_rowid_table";

    private final int m_numInPorts;

    /** Different options of getting rowIDs. */
    private final SettingsModelString m_rowIDModesSettings;

    /** Table number that decides the rowIDs. */
    private final SettingsModelIntegerBounded m_rowIDTableSettings;

    static SettingsModelString createRowIDModeSelectModel() {
        return new SettingsModelString(KEY_SELECTED_ROWID_MODE, RowKeyMode.IDENTICAL.getActionCommand());
    }

    static SettingsModelIntegerBounded createRowIDTableSelectModel() {
        return new SettingsModelIntegerBounded(KEY_SELECTED_ROWID_TABLE, 1, 1, Integer.MAX_VALUE);
    }

    /**
     * Constructor for dynamic ports.
     *
     * @param portsConfiguration The ports configuration.
     */
    ColumnAppender2NodeModel(final PortsConfiguration portsConfiguration) {
        super(portsConfiguration.getInputPorts(), portsConfiguration.getOutputPorts());
        m_numInPorts = portsConfiguration.getInputPorts().length;
        m_rowIDModesSettings = createRowIDModeSelectModel();
        m_rowIDTableSettings = createRowIDTableSelectModel();
    }

    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {

        // Sanity check settings even though dialog checks, in case any flow variables went bad.
        CheckUtils.checkSetting(m_rowIDTableSettings.getIntValue() <= m_numInPorts,
            "The selected port number for row key must be a number between 1 and %s (the number of input tables)",
            m_numInPorts);
        final DataTableSpec spec = combineToSingleSpec(createUniqueSpecs(inSpecs));
        return new DataTableSpec[]{spec};
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_rowIDModesSettings.saveSettingsTo(settings);
        m_rowIDTableSettings.saveSettingsTo(settings);
    }

    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_rowIDModesSettings.loadSettingsFrom(settings);
        m_rowIDTableSettings.loadSettingsFrom(settings);
    }

    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_rowIDModesSettings.validateSettings(settings);
        m_rowIDModesSettings.validateSettings(settings);
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
    protected void reset() {
        /* Nothing to do here. */
    }

    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec)
        throws Exception {
        final BufferedDataTable out;
        if (m_rowIDModesSettings.getStringValue().equals("IDENTICAL")) {
            out = joinTablesWithIdenticalKeys(inData, exec);
        } else {
            out = createNewTable(inData, exec);
        }
        return new BufferedDataTable[]{out};
    }

    //////////////// STREAMING FUNCTIONS ////////////////

    @Override
    public StreamableOperator createStreamableOperator(final PartitionInfo partitionInfo,
        final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        return new StreamableOperator() {
            @Override
            public void runFinal(final PortInput[] inputs, final PortOutput[] outputs, final ExecutionContext exec)
                throws Exception {
                RowInput[] rowInArray = new RowInput[inputs.length];
                CustomRowIterator[] rowIterators = new CustomRowIterator[inputs.length];
                for (int i = 0; i < inputs.length; i++) {
                    rowInArray[i] = (RowInput)inputs[i];
                    rowIterators[i] = new CustomRowIteratorStreamingImpl(rowInArray[i],
                        rowInArray[i].getDataTableSpec().getNumColumns());
                }
                /* Number of rows will be -1 for all inputs in case of streaming. */
                final Long[] rowCounts = new Long[m_numInPorts];
                Arrays.fill(rowCounts, Long.valueOf(-1));

                RowOutput rowOut = (RowOutput)outputs[0];
                compute(exec, rowOut::push, rowIterators, rowCounts, m_rowIDTableSettings.getIntValue() - 1);

                /* Poll all the remaining rows if there are any but don't do anything with them. */
                for (int i = 0; i < inputs.length; i++) {
                    while (rowIterators[i].hasNext()) {
                        rowIterators[i].next();
                    }
                }

                for (int i = 0; i < inputs.length; i++) {
                    rowInArray[i].close();
                }
                rowOut.close();
            }
        };
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
     * @param inSpecs An array of DataTableSpec.
     * @return A concatenation of the input Specs.
     */
    private static DataTableSpec[] createUniqueSpecs(final DataTableSpec[] inSpecs) {

        final UniqueNameGenerator nameGen = new UniqueNameGenerator((DataTableSpec)null);

        final DataTableSpec[] uniqDataTableSpecs = new DataTableSpec[inSpecs.length];
        for (int i = 0; i < inSpecs.length; i++) {
            final DataTableSpec curSpec = inSpecs[i];
            final DataColumnSpec[] outColSpecs = new DataColumnSpec[curSpec.getNumColumns()];
            for (int j = 0; j < curSpec.getNumColumns(); j++) {
                DataColumnSpec curColSpec = curSpec.getColumnSpec(j);
                outColSpecs[j] = nameGen.newCreator(curColSpec).createSpec();
            }
            uniqDataTableSpecs[i] = new DataTableSpec(outColSpecs);
        }
        return uniqDataTableSpecs;
    }

    /**
     * Creates a single combined DataTableSpec from an array while respecting the order of the input DataTableSpecs.
     *
     * @param inSpecs An array of DataTableSpec.
     * @return A combined DataTableSpec.
     */
    private static DataTableSpec combineToSingleSpec(final DataTableSpec[] inSpecs) {
        return Arrays.stream(inSpecs) //
            .reduce(new DataTableSpec(), DataTableSpec::new);
    }

    /**
     * Provided an array of BufferedDataTable with equal row count and identical RowIDs it returns a joined single
     * BufferedDataTable where the columns of each BufferedDataTable are appended in respective order.
     *
     * @param inData An array of BufferedDataTable.
     * @param exec The execution context.
     * @return A combined BufferedDataTable containing all the columns from all tables.
     * @throws CanceledExecutionException
     */
    private static BufferedDataTable joinTablesWithIdenticalKeys(final BufferedDataTable[] inData,
        final ExecutionContext exec) throws CanceledExecutionException {
        DataTableSpec[] outTableSpec = createUniqueSpecs(getSpecFromInput(inData));
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
     * @param inData An array of BufferedDataTable.
     * @param exec The execution context.
     * @return A combined BufferedDataTable containing all the columns from all tables.
     * @throws InterruptedException
     * @throws CanceledExecutionException
     */
    private BufferedDataTable createNewTable(final BufferedDataTable[] inData, final ExecutionContext exec)
        throws InterruptedException, CanceledExecutionException {

        final Long[] rowCounts = Arrays.stream(inData).map(BufferedDataTable::size).toArray(Long[]::new);

        CustomRowIterator[] rowIterators =
            Arrays.stream(inData).map(d -> new CustomRowIteratorImpl(d.iterator(), d.getSpec().getNumColumns()))
                .toArray(CustomRowIteratorImpl[]::new);

        BufferedDataContainer outDataContainer =
            exec.createDataContainer(combineToSingleSpec(createUniqueSpecs((getSpecFromInput(inData)))));

        if (m_rowIDModesSettings.getStringValue().equals("KEY_TABLE")) {
            compute(exec, outDataContainer::addRowToTable, rowIterators, rowCounts,
                m_rowIDTableSettings.getIntValue() - 1);
        } else {
            final int longestTableIndex = IntStream.range(0, m_numInPorts) //
                .reduce((a, b) -> rowCounts[a] < rowCounts[b] ? b : a) //
                .getAsInt();
            compute(exec, outDataContainer::addRowToTable, rowIterators, rowCounts, longestTableIndex);
        }
        outDataContainer.close();
        return outDataContainer.getTable();
    }

    /**
     * Combines the rows to create new table.
     *
     * @param exec The execution context.
     * @param output
     * @param rowIterators An array of iterators on each table to be appended.
     * @param rowCounts An array of row counts from each table.
     * @param decidingTableIdx The index of the table that decides the RowID and length of the output.
     * @throws InterruptedException
     * @throws CanceledExecutionException
     */
    private void compute(final ExecutionContext exec, final RowConsumer output, final CustomRowIterator[] rowIterators,
        final Long[] rowCounts, final int decidingTableIdx) throws InterruptedException, CanceledExecutionException {
        final long numRowsFocusTable = (decidingTableIdx == -1) ? rowCounts[0] : rowCounts[decidingTableIdx];

        MultipleRowIterators multiIt = new MultipleRowIterators(rowIterators,
            RowKeyMode.valueOf(m_rowIDModesSettings.getStringValue()), decidingTableIdx);
        while (multiIt.hasNext()) {
            if (numRowsFocusTable != -1) {
                final long rowCountFinal = multiIt.getPos();
                exec.setProgress(rowCountFinal / (double)numRowsFocusTable,
                    () -> "Appending columns (row " + rowCountFinal + "/" + numRowsFocusTable + ")");
            }
            exec.checkCanceled();
            output.consume(multiIt.next());
        }
        differingTableSizeMsg(decidingTableIdx, rowCounts);

    }

    /**
     * Checks if table sizes (number of rows) differ and displays warning messages according to the setup.
     *
     * @param decidingTableIdx The index of the table that decides the RowID and length of the output.
     * @param rowCounts An array of row counts from each table.
     */
    private void differingTableSizeMsg(final int decidingTableIdx, final Long[] rowCounts) {
        if (!identicalElements(rowCounts)) {
            /* Set warning messages if missing values have been inserted or one table was truncated.
             * In case of generating new rowIDs a generic warning is displayed. */
            if (m_rowIDModesSettings.getStringValue().equals("GENERATE")) {
                setWarningMessage("Input tables differ in length! Missing values have been added accordingly.");
            } else {
                detailedTablesDifferMesseges(decidingTableIdx, rowCounts);
            }
        }
    }

    /**
     * Displays detailed warning messages if input tables are shorter or longer than the deciding table.
     *
     * @param decidingTableIdx The index of the table that decides the RowID and length of the output.
     * @param rowCounts An array of row counts from each table.
     */
    private void detailedTablesDifferMesseges(final int decidingTableIdx, final Long[] rowCounts) {
        for (int i = 0; i < m_numInPorts; i++) {
            final int compareResult = rowCounts[i].compareTo(rowCounts[decidingTableIdx]);
            // Tables are the same
            if (compareResult == 0) {
                continue;
            }

            if (compareResult < 0) { // Table is shorter than the deciding table
                setWarningMessage("Table " + (i + 1) + " is shorter than the selected table (Table "
                    + (decidingTableIdx + 1) + ")! Missing values have been added accordingly.");
            } else { // Table is longer than the deciding table
                setWarningMessage("Table " + (i + 1) + " is longer than the selected table (Table "
                    + (decidingTableIdx + 1) + ")! It has been truncated.");
            }
        }
    }

    /**
     * Checks if an number array contains identical numbers only.
     *
     * @param array The array to be checked.
     * @return true if all elements are the same, false otherwise.
     */
    private static final boolean identicalElements(final Long[] array) {
        return (Stream.of(array).distinct().count() == 1);
    }

    private static interface RowConsumer {
        void consume(DataRow row) throws InterruptedException;
    }

    static interface CustomRowIterator {
        boolean hasNext() throws InterruptedException;
        DataRow next() throws InterruptedException;
    }

    /**
     * An implementation of custom row iterator that returns a row of missing values when it is at the end.
     *
     * @author Temesgen H. Dadi, KNIME GmbH, Berlin, Germany
     */
    private static final class CustomRowIteratorImpl implements CustomRowIterator {

        private RowIterator m_rowIt;

        private final int m_numCol;

        private final DefaultRow m_emptyRow;

        CustomRowIteratorImpl(final RowIterator rowIt, final int numCol) {
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
            /* next returns a row of missing value cells if iterator is at the end. */
            if (!m_rowIt.hasNext()) {
                return m_emptyRow;
            }
            return m_rowIt.next();
        }
    }

    /**
     * An implementation (streaming variant) of custom row iterator that returns a row of missing values when it is at
     * the end.
     *
     * @author Temesgen H. Dadi, KNIME GmbH, Berlin, Germany
     */
    private static final class CustomRowIteratorStreamingImpl implements CustomRowIterator {

        private RowInput m_rowInput;

        private DataRow m_row = null;

        private int m_numCol;

        private final DefaultRow m_emptyRow;

        CustomRowIteratorStreamingImpl(final RowInput rowInput, final int numCol) {
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
            DataRow row = m_row;
            if (!hasNext()) {
                row = m_emptyRow;
            }
            m_row = null;
            return row;
        }
    }

    /**
     * A pseudo iterator that manages multiple CustomRowIterators.
     *
     * @author Temesgen H. Dadi, KNIME GmbH, Berlin, Germany
     */
    private static final class MultipleRowIterators {

        private final CustomRowIterator[] m_iterators;

        private final RowKeyMode m_keyMode;

        private final int m_itCount;

        private final int m_decidingTableIdx;

        private long m_currentPos;

        /**
         * constructor
         *
         * @param rowIterators An array of row iterators.
         * @param keyMode The way to handle the row keys.
         * @param decidingTableIdx The table deciding the the rowID and output table length
         */

        MultipleRowIterators(final CustomRowIterator[] rowIterators, final RowKeyMode keyMode,
            final int decidingTableIdx) {
            m_iterators = rowIterators;
            m_keyMode = keyMode;
            m_itCount = rowIterators.length;
            m_decidingTableIdx = decidingTableIdx;
            m_currentPos = 0;

        }

        public long getPos() {
            return m_currentPos;
        }

        public boolean hasNext() throws InterruptedException {
            boolean focusHasNext = false;
            int count = 0;
            /* all iterators must call hasNext(). */
            for (int i = 0; i < m_itCount; i++) {
                if (m_iterators[i].hasNext()) {
                    count++;
                    if (i == m_decidingTableIdx) {
                        focusHasNext = true;
                    }
                }
            }
            if (m_keyMode.equals(RowKeyMode.GENERATE)) {
                return count > 0;
            } else {
                return focusHasNext;
            }
        }

        public DataRow next() throws InterruptedException {
            ArrayList<DataCell> cells = new ArrayList<>();
            String rowKey = "";

            for (int i = 0; i < m_itCount; i++) {
                DataRow currRow = m_iterators[i].next();
                for (DataCell cell : currRow) {
                    cells.add(cell);
                }
                if (i == m_decidingTableIdx && !m_keyMode.equals(RowKeyMode.GENERATE)) {
                    rowKey = currRow.getKey().getString();
                }

                if (m_keyMode.equals(RowKeyMode.IDENTICAL)) {
                    if (i == 0) {
                        rowKey = currRow.getKey().getString();
                    } else if (!rowKey.equals(currRow.getKey().getString())) {
                        throw new IllegalArgumentException(
                            "Tables contain non-matching rows or are sorted " + "differently, keys in row " + m_itCount
                                + " do not match: \"" + rowKey + "\" vs \"" + currRow.getKey().getString() + "\"");
                    }

                }
            }

            if (m_keyMode.equals(RowKeyMode.GENERATE)) {
                rowKey = "Row" + m_currentPos;
            }

            DefaultRow res = new DefaultRow(rowKey, cells);
            m_currentPos++;
            return res;
        }
    }

    /**
     * Different options used to decide the RowIDs during column appending.
     *
     * @author Temesgen H. Dadi, KNIME GmbH, Berlin, Germany
     */
    enum RowKeyMode implements ButtonGroupEnumInterface {
            IDENTICAL("Identical row keys and table lengths"), //
            GENERATE("Generate new row keys"), //
            KEY_TABLE("Use the row keys from the input table: ");

        private String m_text;

        private String m_tooltip;

        RowKeyMode(final String text) {
            this.m_text = text;
            this.m_tooltip = "<html>Choose the way the row keys of the output tables are decided.<br>"
                + "If \"Identical row keys and table lengths\" is chosen, all input tables<br> "
                + "should have exactly the same row Ids in the exact same order.<html>";
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
            return m_tooltip;
        }

        @Override
        public boolean isDefault() {
            return this == IDENTICAL;
        }

    }

}
