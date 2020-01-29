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
import java.util.List;
import java.util.stream.Stream;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.RowIterator;
import org.knime.core.data.RowKey;
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
import org.knime.core.util.UniqueNameGenerator;

/**
 * This is the model implementation of ColumnAppender.
 *
 * @author Temesgen H. Dadi, KNIME GmbH, Berlin, Germany
 */
final class ColumnAppender2NodeModel extends NodeModel {

    /* different options of getting a rowID */
    static final String[] ROWID_MODE_OPTIONS =
        {"Identical row keys and table lengths", "Generate new row keys", "Use row keys from one of the input tables"};

    /* key for storing the selected way of getting rowIDs */
    private static final String KEY_SELECTED_ROWID_MODE = "selected_rowid_mode";

    /* key for storing the table (port index) used for rowIDs */
    private static final String KEY_SELECTED_ROWID_TABLE = "selected_rowid_table";

    static SettingsModelString createRowIDModeSelectModel() {
        return new SettingsModelString(KEY_SELECTED_ROWID_MODE, ROWID_MODE_OPTIONS[0]);
    }

    static SettingsModelIntegerBounded createRowIDTableSelectModel(final int maxValue) {
        return new SettingsModelIntegerBounded(KEY_SELECTED_ROWID_TABLE, 0, 0, maxValue);
    }

    private final SettingsModelString m_rowIDModesSettings = createRowIDModeSelectModel();

    private final SettingsModelIntegerBounded m_rowIDTableSettings = createRowIDTableSelectModel(2);

    private final int m_numInPorts;

    /**
     * Constructor for dynamic ports
     *
     * @param portsConfiguration the ports configuration
     */
    ColumnAppender2NodeModel(final PortsConfiguration portsConfiguration) {
        super(portsConfiguration.getInputPorts(), portsConfiguration.getOutputPorts());
        m_numInPorts = portsConfiguration.getInputPorts().length;
        m_rowIDTableSettings
            .setEnabled(m_rowIDModesSettings.getStringValue().equals(ColumnAppender2NodeModel.ROWID_MODE_OPTIONS[2]));
        m_rowIDModesSettings.addChangeListener(l -> m_rowIDTableSettings
            .setEnabled(m_rowIDModesSettings.getStringValue().equals(ColumnAppender2NodeModel.ROWID_MODE_OPTIONS[2])));
    }

    private static interface RowConsumer {
        void consume(DataRow row) throws InterruptedException;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec)
        throws Exception {

        DataTableSpec[] inSpecs = new DataTableSpec[m_numInPorts];
        for (int i = 0; i < m_numInPorts; i++) {
            inSpecs[i] = inData[i].getDataTableSpec();
        }

        BufferedDataTable out = null;
        if (identicaRowKeys()) {
            DataTableSpec[] outTableSpec = uniqifyOutSpecs(inSpecs);
            BufferedDataTable[] uniqColTables = new BufferedDataTable[m_numInPorts];
            for (int i = 1; i < uniqColTables.length; i++) {
                uniqColTables[i] = exec.createSpecReplacerTable(inData[i], outTableSpec[i]);
                if (i == 1) {
                    out = exec.createJoinedTable(inData[0], uniqColTables[i], exec);
                } else {
                    out = exec.createJoinedTable(out, uniqColTables[i], exec);
                }
            }
        } else {
            //create a new table and fill the rows accordingly
            // in case of generating new row keys, focus table will be the one with the most rows
            int focusTableIndex = generateNewKeys() ? longestTableIndex(inData) : rowIDTable();
            BufferedDataContainer container = exec.createDataContainer(combineOutSpecs(inSpecs));
            CustomRowIterator[] rowItList = new CustomRowIterator[m_numInPorts];
            long[] numRowsList = new long[m_numInPorts];
            for (int i = 0; i < m_numInPorts; i++) {
                rowItList[i] = new CustomRowIteratorImpl(inData[i].iterator(), inData[i].getSpec().getNumColumns());
                numRowsList[i] = inData[i].size();
            }
            compute(rowItList, focusTableIndex, container::addRowToTable, exec, numRowsList);

            container.close();
            out = container.getTable();
        }
        return new BufferedDataTable[]{out};
    }

    @Override
    protected void reset() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {
        DataTableSpec spec = combineOutSpecs(inSpecs);
        return new DataTableSpec[]{spec};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_rowIDModesSettings.saveSettingsTo(settings);
        m_rowIDTableSettings.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_rowIDModesSettings.loadSettingsFrom(settings);
        m_rowIDTableSettings.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_rowIDModesSettings.validateSettings(settings);
        m_rowIDModesSettings.validateSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File internDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File internDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {

    }

    //////////////// STREAMING FUNCTIONS ////////////////

    /**
     * {@inheritDoc}
     */
    @Override
    public StreamableOperator createStreamableOperator(final PartitionInfo partitionInfo,
        final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        return new StreamableOperator() {
            @Override
            public void runFinal(final PortInput[] inputs, final PortOutput[] outputs, final ExecutionContext exec)
                throws Exception {
                int focusTableIndex = rowIDTable();
                RowInput[] inList = new RowInput[inputs.length];
                CustomRowIterator[] rowItList = new CustomRowIterator[inputs.length];
                for (int i = 0; i < inputs.length; i++) {
                    inList[i] = (RowInput)inputs[i];
                    rowItList[i] =
                        new CustomRowIteratorStreamingImpl(inList[i], inList[i].getDataTableSpec().getNumColumns());
                }

                /* number of rows will be -1 for all inputs in case of streaming */
                long[] numRowsList = new long[inputs.length];
                Arrays.fill(numRowsList, -1);

                RowOutput out = (RowOutput)outputs[0];
                compute(rowItList, focusTableIndex, out::push, exec, numRowsList);

                /* poll all the remaining rows if there are any but don't do anything with them */
                for (int i = 0; i < inputs.length; i++) {
                    while (rowItList[i].hasNext()) {
                        rowItList[i].next();
                    }
                }

                for (int i = 0; i < inputs.length; i++) {
                    inList[i].close();
                }
                out.close();
            }
        };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public InputPortRole[] getInputPortRoles() {
        //in-ports are non-distributed since it can't be guaranteed that the chunks at each port are of identical size
        return Stream.generate(() -> InputPortRole.NONDISTRIBUTED_STREAMABLE).limit(m_numInPorts)
            .toArray(InputPortRole[]::new);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public OutputPortRole[] getOutputPortRoles() {
        return new OutputPortRole[]{OutputPortRole.DISTRIBUTED};
    }

    /* combines the rows in case a new table is created */
    private void compute(final CustomRowIterator[] rowItList, final int focusTableIndex, final RowConsumer output,
        final ExecutionContext exec, final long[] numRowsList) throws InterruptedException, CanceledExecutionException {
        long rowCount = 0;
        long numRowsFocusTable = (focusTableIndex == -1) ? numRowsList[0] : numRowsList[focusTableIndex];
        while (iteratorsHasNext(rowItList, focusTableIndex)) {
            if (numRowsFocusTable != -1) {
                exec.setProgress(rowCount / (double)numRowsFocusTable);
                long rowCountFinal = rowCount;
                exec.setMessage(() -> "Appending columns (row " + rowCountFinal + "/" + numRowsFocusTable + ")");
            }
            exec.checkCanceled();
            ArrayList<DataCell> cells = new ArrayList<>();
            ArrayList<String> rowKeyList = new ArrayList<>();
            for (int i = 0; i < rowItList.length; i++) {
                DataRow currRow = rowItList[i].next();
                for (DataCell cell : currRow) {
                    cells.add(cell);
                }
                // Advance all iterators, capture  the row id while on focus table
                rowKeyList.add(currRow.getKey().getString());
            }

            RowKey rowKey = chooseRowKey(focusTableIndex, rowCount, rowKeyList);

            DefaultRow res = new DefaultRow(rowKey, cells);
            output.consume(res);
            rowCount++;
        }
        differingTableSizeMsg(focusTableIndex, numRowsList);
    }

    /**
     * Given an array of DataTableSpec, it returns a single concatenated DataTableSpec. Redundant column names are
     * resolved by appending "(#1)", "(#2)", and so on as required.
     *
     * @param inSpecs an array of DataTableSpec
     * @return a concatenation of the input Specs
     */
    private static DataTableSpec combineOutSpecs(final DataTableSpec[] inSpecs) {
        // Copy the column specs of the first table
        List<DataColumnSpec> mergedColSpecs = new ArrayList<>();
        for (int i = 0; i < inSpecs[0].getNumColumns(); i++) {
            DataColumnSpec columnSpec = inSpecs[0].getColumnSpec(i);
            mergedColSpecs.add(columnSpec);
        }

        // Initialize the output spec with the spec of first table
        DataTableSpec outTableSpec =
            new DataTableSpec(mergedColSpecs.toArray(new DataColumnSpec[mergedColSpecs.size()]));

        // Incrementally add specs after checking for column name duplicates
        UniqueNameGenerator nameGenerator = new UniqueNameGenerator(outTableSpec);
        for (int i = 1; i < inSpecs.length; i++) {
            for (int j = 0; j < inSpecs[i].getNumColumns(); j++) {
                DataColumnSpec curColSpec = inSpecs[i].getColumnSpec(j);
                mergedColSpecs.add(nameGenerator.newCreator(curColSpec).createSpec());
            }
            // Update the unique name generator with merged column specs
            outTableSpec = new DataTableSpec(mergedColSpecs.toArray(new DataColumnSpec[mergedColSpecs.size()]));
            nameGenerator = new UniqueNameGenerator(outTableSpec);
        }
        return outTableSpec;
    }

    /**
     * Given an array of DataTableSpec, it returns an array of DataTableSpec of the same size in which all column names
     * are unique across all the specs. Redundant column names are resolved by appending "(#1)", "(#2)", and so on as
     * required.
     *
     * @param inSpecs an array of DataTableSpec
     * @return a new array of DataTableSpec where column names are unique across all specs
     */
    private static DataTableSpec[] uniqifyOutSpecs(final DataTableSpec[] inSpecs) {
        DataTableSpec combinedTableSpec = combineOutSpecs(inSpecs);
        DataTableSpec[] uniqTableSpecs = new DataTableSpec[inSpecs.length];
        int globalCounter = 0;
        for (int i = 0; i < inSpecs.length; i++) {
            List<DataColumnSpec> currColSpecs = new ArrayList<>();
            for (int j = 0; j < inSpecs[i].getNumColumns(); j++) {
                currColSpecs.add(combinedTableSpec.getColumnSpec(globalCounter));
                globalCounter++;
            }
            DataColumnSpec[] currColSpecsArray = currColSpecs.toArray(new DataColumnSpec[currColSpecs.size()]);
            uniqTableSpecs[i] = (new DataTableSpec(currColSpecsArray));
        }
        return uniqTableSpecs;
    }

    /**
     * Checks if all/relevant iterators has a next element. It also calls hasNext() on all iterators.
     *
     * @param rowItList
     * @param focusTableIndex The index of the input table deciding the row ids.
     * @return whether or not all/relevant iterators has a next element
     * @throws InterruptedException
     */
    private boolean iteratorsHasNext(final CustomRowIterator[] rowItList, final int focusTableIndex)
        throws InterruptedException {
        int count = 0;
        boolean focusHasNext = false;
        // all iterators must call hasNext()
        for (int i = 0; i < rowItList.length; i++) {
            if (rowItList[i].hasNext()) {
                count++;
                if (i == focusTableIndex) {
                    focusHasNext = true;
                }
            }
        }
        if (generateNewKeys()) {
            return count > 0;
        } else {
            return focusHasNext;
        }
    }

    /* HELPER METHODS */

    /**
     * Checks if the option Identical row keys and table lengths is selected or not
     *
     * @return true if the option Identical row keys and table lengths is selected, false otherwise.
     */
    private boolean identicaRowKeys() {
        return (m_rowIDModesSettings.getStringValue().equals(ROWID_MODE_OPTIONS[0]));
    }

    /**
     * Checks if the option Identical row keys and table lengths is selected or not
     *
     * @return true if the option Identical row keys and table lengths is selected, false otherwise.
     */
    private boolean generateNewKeys() {
        return (m_rowIDModesSettings.getStringValue().equals(ROWID_MODE_OPTIONS[1]));
    }

    /**
     * Gets the index of the table (within the input ports) that decides the rowIDs. In case of newly generated rowIDs
     * it returns -1.
     *
     * @return the index of the table (within the input ports) that decides the rowIDs
     */
    private int rowIDTable() {
        if (m_rowIDModesSettings.getStringValue().equals(ROWID_MODE_OPTIONS[2])) {
            return m_rowIDTableSettings.getIntValue();
        } else if (m_rowIDModesSettings.getStringValue().equals(ROWID_MODE_OPTIONS[1])) {
            return -1; // should not be used / use longest table instead
        }
        return 0;
    }

    /**
     * Given a list of strings, picks the one from the deciding table or generate new using the rowIndex (in the case of
     * newly generated rowIDs and returns a RowKey based on the picked string.
     *
     * @param focusTableIndex The index of the input table deciding the row ids.
     * @param rowIndex
     * @param rowKeyList
     * @return a RowKey based on the deciding table index or newly generated rowID string
     */
    private RowKey chooseRowKey(final int focusTableIndex, final long rowIndex, final ArrayList<String> rowKeyList) {
        String rowKey = "";
        if (generateNewKeys()) {
            rowKey = "Row" + rowIndex;
        } else if (identicaRowKeys()) {
            for (int i = 1; i < rowKeyList.size(); i++) {
                if (!(rowKeyList.get(i - 1).equals(rowKeyList.get(i)))) {
                    throw new IllegalArgumentException("Tables contain non-matching rows or are sorted "
                        + "differently, keys in row " + rowIndex + " do not match: \"" + rowKeyList.toString() + "\"");
                }

            }
            rowKey = rowKeyList.get(0); // any of the similar row keys
        } else {
            rowKey = rowKeyList.get(focusTableIndex);
        }
        return new RowKey(rowKey);
    }

    /**
     * Checks if table sizes (number of rows) differ and displays warning messages according to the setup.
     *
     * @param focusTableIndex The index of the input table deciding the row ids.
     * @param numRowsList The number of rows from each input table.
     */
    private void differingTableSizeMsg(final int focusTableIndex, final long[] numRowsList) {
        if (!identicalElements(numRowsList)) {
            if (generateNewKeys()) {
                /* set warning messages if missing values have been inserted or one table was truncated */
                setWarningMessage("Input tables differ in length! Missing values have been added accordingly.");
            } else {
                detailedTablesDifferMesseges(focusTableIndex, numRowsList);
            }
        }
    }

    /**
     * Displays detailed warning messages if input tables are shorter or longer than the deciding table.
     *
     * @param focusTableIndex The index of the input table deciding the row ids.
     * @param numRowsList The number of rows from each input table.
     */
    private void detailedTablesDifferMesseges(final int focusTableIndex, final long[] numRowsList) {
        for (int i = 0; i < m_numInPorts; i++) {
            if (i == focusTableIndex || numRowsList[focusTableIndex] == numRowsList[i]) {
                continue;
            }
            if (numRowsList[focusTableIndex] > numRowsList[i]) {
                setWarningMessage("Table " + i + " is shorter than the selected table (Table " + focusTableIndex
                    + ")! Missing values have been added accordingly.");
            } else {
                setWarningMessage("Table " + i + " is longer than the selected table (Table " + focusTableIndex
                    + ")! It has been truncated.");
            }
        }
    }

    /**
     * Returns the longest (maximum number of rows) input table index
     *
     * @param inData an array of BufferedDataTable
     * @return the index of longest input table
     */
    private int longestTableIndex(final BufferedDataTable[] inData) {
        long maxNumRows = inData[0].size();
        int focusTableIndex = 0;
        for (int i = 1; i < m_numInPorts; i++) {
            if (inData[i].size() > maxNumRows) {
                maxNumRows = inData[i].size();
                focusTableIndex = i;
            }
        }
        return focusTableIndex;
    }

    /**
     * Checks if an number array contains identical numbers only
     *
     * @param array The array to be checked
     * @return true if all elements are the same, false otherwise.
     */
    private final boolean identicalElements(final long[] array) {
        long currNumRow = array[0];
        for (int i = 1; i < m_numInPorts; i++) {
            if (currNumRow != array[i]) {
                return false;
            }
        }
        return true;
    }

    /**
     * Gets a row of missing value cells with a given number of columns
     *
     * @param numCol the number of columns
     * @return a row of missing value cells
     */
    private static DefaultRow getMissingCellsRow(final int numCol) {
        return new DefaultRow("DefaultRow",
            Stream.generate(DataType::getMissingCell).limit(numCol).toArray(DataCell[]::new));
    }

    static interface CustomRowIterator {
        boolean hasNext() throws InterruptedException;

        DataRow next();
    }

    /**
     * An implementation of custom row iterator that returns a row of missing values when it is at the end.
     *
     * @author Temesgen H. Dadi, KNIME GmbH, Berlin, Germany
     */
    private static final class CustomRowIteratorImpl implements CustomRowIterator {

        private RowIterator m_rowIt;

        private int m_numCol;

        CustomRowIteratorImpl(final RowIterator rowIt, final int numCol) {
            m_rowIt = rowIt;
            m_numCol = numCol;
        }

        @Override
        public boolean hasNext() {
            return m_rowIt.hasNext();
        }

        @Override
        public DataRow next() {
            /* next return a row of missing value cells if iterator is at the end */
            if (!m_rowIt.hasNext()) {
                return getMissingCellsRow(m_numCol);
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

        CustomRowIteratorStreamingImpl(final RowInput rowInput, final int numCol) {
            m_rowInput = rowInput;
            m_numCol = numCol;
        }

        @Override
        public boolean hasNext() throws InterruptedException {
            /* if hasNext() is called multiple times without calling next() in between,
             this if-clause ensures that it still returns true */
            if (m_row == null) {
                m_row = m_rowInput.poll();
            }
            return m_row != null;
        }

        @Override
        public DataRow next() {
            DataRow row = m_row;
            /* next return a row of missing value cells if iterator is at the end */
            if (row == null) {
                row = getMissingCellsRow(m_numCol);
            }
            m_row = null;
            return row;
        }
    }
}
