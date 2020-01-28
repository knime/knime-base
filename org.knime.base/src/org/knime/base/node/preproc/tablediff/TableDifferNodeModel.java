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
 * 11.12.2019 (Lars Schweikardt): created
 */
package org.knime.base.node.preproc.tablediff;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.IntStream;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnDomain;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.MissingCell;
import org.knime.core.data.RowIterator;
import org.knime.core.data.RowKey;
import org.knime.core.data.container.CloseableRowIterator;
import org.knime.core.data.def.BooleanCell;
import org.knime.core.data.def.BooleanCell.BooleanCellFactory;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.def.StringCell.StringCellFactory;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelColumnFilter2;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.util.ButtonGroupEnumInterface;
import org.knime.core.node.util.CheckUtils;

/**
 * Node model of the "Table Difference Finder" node. It offers the functionality to compare two table by means of their
 * values and table specs. Depending on the configuration the two input tables will be either be entirely be compared or
 * solely respective a subset of the columns existent in the reference table, i.e., the first input.
 *
 * @author Lars Schweikardt, KNIME GmbH, Konstanz, Germany
 */
final class TableDifferNodeModel extends NodeModel {

    /** Port index of the reference table. */
    private static final int PORT_REFERENCE_TABLE = 0;

    /** Port index of the the comparison table. */
    private static final int PORT_COMPARED_TABLE = 1;

    /** Error message in case the node has to fail on different values. */
    private static final String ERROR_DIFFERENT_VALUES = "There are differences in the values.";

    /** Error message in case the node has to fail on different specs. */
    private static final String ERROR_DIFFERENT_SPECS = "There are differences in the specs.";

    /** Name for the value differences output port spec. */
    private static final String TABLE_DIFFERENCES_SPEC_NAME = "Table Differences";

    /** Name for the spec difference output port spec. */
    private static final String SPEC_DIFFERENCES_SPEC_NAME = "Spec Differences";

    /** MissingCell in case a row is not available in one of both tables. */
    private static final MissingCell MISSING_ROW_CELL = new MissingCell("No such row");

    /** MissingCell in case a column is not available in both tables. */
    private static final MissingCell MISSING_COLUMN_CELL = new MissingCell("No such column");

    /** Column names of the bottom output spec. */
    private static final String[] SPEC_DIFFERENCES_COL_NAMES = {"Column Name Reference Table",
        "Column Name Compared Table", "Type Equals", "Domain Equals", "Position Equals"};

    /** Column types for the bottom spec. */
    private static final DataType[] SPEC_DIFFERENCES_COL_TYPES =
        new DataType[]{StringCell.TYPE, StringCell.TYPE, BooleanCell.TYPE, BooleanCell.TYPE, BooleanCell.TYPE};

    /** Column names for the top output spec. */
    private static final String[] TABLE_DIFFERENCES_COL_NAMES = {"RowId Reference Table", "RowId Compared Table",
        "Column Name", "Value Reference Table", "Value Compared Table"};

    /** Column types for the top spec. */
    private static final DataType[] TABLE_DIFFERENCES_COL_TYPES = new DataType[]{StringCell.TYPE, StringCell.TYPE,
        StringCell.TYPE, DataType.getType(DataCell.class), DataType.getType(DataCell.class)};

    private static final DataTableSpec VALUE_TABLE_SPEC =
        createOutSpec(TABLE_DIFFERENCES_SPEC_NAME, TABLE_DIFFERENCES_COL_NAMES, TABLE_DIFFERENCES_COL_TYPES);

    private static final DataTableSpec SPEC_TABLE_SPEC =
        createOutSpec(SPEC_DIFFERENCES_SPEC_NAME, SPEC_DIFFERENCES_COL_NAMES, SPEC_DIFFERENCES_COL_TYPES);

    /**
     * Creates a new output spec from the given parameters.
     *
     * @param specDesc name of the spec
     * @param colNames column names
     * @param dataTypes data types
     * @return the newly created data table spec
     */
    private static DataTableSpec createOutSpec(final String specDesc, final String[] colNames,
        final DataType[] dataTypes) {
        return new DataTableSpec(specDesc,
            IntStream.range(0, colNames.length)
                .mapToObj(idx -> new DataColumnSpecCreator(colNames[idx], dataTypes[idx]).createSpec())
                .toArray(DataColumnSpec[]::new));
    }

    /** SettingsModel storing the the selected columns from the reference table. */
    private final SettingsModelColumnFilter2 m_comparedColumns = createComparedColumnsModel();

    /**
     * SettingsModel storing a flag that determines whether a one by one or a restricted table comparison will be
     * performed.
     */
    private final SettingsModelBoolean m_compareTablesEntirely = createCompareTablesEntirelyModel();

    /**
     * Variable for the selection of the failure mode.
     */
    private final SettingsModelString m_failureMode = createFailureModeModel();

    /**
     * Creates the SettingsModel used to store the reference table's column selection.
     *
     * @return settings model storing the selected columns
     */
    static SettingsModelColumnFilter2 createComparedColumnsModel() {
        return new SettingsModelColumnFilter2("column_filter");
    }

    /**
     * Creates the SettingsModel used to store the comparison mode flag.
     *
     * @return settings model containing the comparison mode flag.
     */

    static SettingsModelBoolean createCompareTablesEntirelyModel() {
        return new SettingsModelBoolean("compare_entirely", true);
    }

    /**
     * Create the SettingsModel storing the {@link FailureMode}.
     *
     * @return the settings model storing the failure mode
     */
    static SettingsModelString createFailureModeModel() {
        return new SettingsModelString("failure_mode", FailureMode.NEVER.getText());
    }

    /** Constructor. */
    TableDifferNodeModel() {
        super(2, 2);
    }

    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {
        final FailureMode confFailMode = FailureMode.getValueFromLabel(m_failureMode.getStringValue());
        if (confFailMode == FailureMode.DIFFERENT_SPECS || confFailMode == FailureMode.DIFFERENT_VALUES) {
            final DataTableSpec refSpec = inSpecs[PORT_REFERENCE_TABLE];
            final DataTableSpec compSpec = inSpecs[PORT_COMPARED_TABLE];
            for (int[] pos : getColumnMapping(refSpec, compSpec).values()) {
                CheckUtils.checkSetting(pos[PORT_REFERENCE_TABLE] != -1 // non-missing column in ref table
                    && pos[PORT_COMPARED_TABLE] != -1 // non-missing column in compared table
                    && refSpec.getColumnSpec(pos[PORT_REFERENCE_TABLE]).getType()
                        .equals(compSpec.getColumnSpec(pos[PORT_COMPARED_TABLE]).getType()) // same data types
                    , ERROR_DIFFERENT_SPECS);
            }
        }
        return new DataTableSpec[]{VALUE_TABLE_SPEC, SPEC_TABLE_SPEC};
    }

    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec)
        throws Exception {

        // get the input tables
        final BufferedDataTable refTable = inData[PORT_REFERENCE_TABLE];
        final BufferedDataTable compTable = inData[PORT_COMPARED_TABLE];

        // get the configure fail mode
        final FailureMode confFailMode = FailureMode.getValueFromLabel(m_failureMode.getStringValue());

        // If different lengths and Failure Mode is "Fail on different values" the node will fail right away
        if (confFailMode == FailureMode.DIFFERENT_VALUES && (refTable.size() != compTable.size())) {
            throw new IllegalArgumentException(ERROR_DIFFERENT_VALUES);
        }

        // extract the specs
        final DataTableSpec refSpec = refTable.getDataTableSpec();
        final DataTableSpec compSpec = compTable.getDataTableSpec();
        final Map<String, int[]> colMappingCols = getColumnMapping(refSpec, compSpec);

        // calculate the spec differences
        final BufferedDataTable specTable = getSpecDifferences(exec, refSpec, compSpec, colMappingCols, confFailMode);

        // calculate the value differences
        final BufferedDataTable valueTable =
            getValueDifferences(exec, refTable, compTable, colMappingCols, confFailMode);

        // return the tables
        return new BufferedDataTable[]{valueTable, specTable};
    }

    /**
     * Calculates the columns to compare and their respective position in the input tables. Depending on the comparison
     * selection the map solely contains the mapping for the selected columns w.r.t. the reference table, or for the
     * union of the columns.
     *
     * @param refSpec the reference spec
     * @param compSpec the comparison spec
     * @return mapping of the columns to their indices respective the different tables
     */
    private Map<String, int[]> getColumnMapping(final DataTableSpec refSpec, final DataTableSpec compSpec) {

        final List<String> colNames;
        if (m_compareTablesEntirely.getBooleanValue()) {
            // calculate the union of the column names
            colNames = new ArrayList<>(Arrays.asList(refSpec.getColumnNames()));
            for (final String compColName : compSpec.getColumnNames()) {
                if (!refSpec.containsName(compColName)) {
                    colNames.add(compColName);
                }
            }
        } else {
            // get the selected column names
            colNames = Arrays.asList(m_comparedColumns.applyTo(refSpec).getIncludes());
        }
        // linked hashmap to keep the ordering cols ref table additional cols comp table
        return colNames.stream().collect(//
            LinkedHashMap::new,
            (map, c) -> map.put(c, new int[]{refSpec.findColumnIndex(c), compSpec.findColumnIndex(c)}), //
            Map::putAll);
    }

    /**
     * This method compares the values of the common columns. If there are any differences those will be saved in a new
     * row.
     *
     * @param exec Execution Context
     * @param colMapping Column Mapping of the common columns of the Data
     * @param inData contains the Data of the two input ports
     * @param buf BufferedDataContainer with the value table output Spec
     *
     * @return BufferedDataTable with the differences of the values
     * @throws CanceledExecutionException
     */
    private static BufferedDataTable getValueDifferences(final ExecutionContext exec, final BufferedDataTable refTable,
        final BufferedDataTable compTable, final Map<String, int[]> colMapping, final FailureMode confFailMode)
        throws CanceledExecutionException {

        BufferedDataContainer buf = exec.createDataContainer(VALUE_TABLE_SPEC);

        final long refTableSize = refTable.size();
        final long compTableSize = compTable.size();

        // if one of the two tables is not empty
        final long outputTableSize = Math.max(refTableSize, compTableSize);
        if (outputTableSize != 0) {
            try (CloseableRowIterator refIter = refTable.iterator();
                    CloseableRowIterator compIter = compTable.iterator()) {

                // the number of the currently processed row
                long rowNo;

                // process all rows w.r.t. the shorter table
                for (rowNo = 0L; refIter.hasNext() && compIter.hasNext(); rowNo++) {

                    // get the rows
                    final DataRow refRow = refIter.next();
                    final DataRow compRow = compIter.next();

                    // calculate the differences w.r.t. each column
                    for (Entry<String, int[]> pos : colMapping.entrySet()) {
                        writeDifferingCellEntries(buf, pos, refRow, compRow, confFailMode);
                    }

                    // update progress
                    final long rowNoFinal = rowNo;
                    exec.setProgress(rowNo / (double)outputTableSize, () -> ("Checked row " + rowNoFinal));
                    exec.checkCanceled();
                }

                //Add remaining rows from longer table
                if (refIter.hasNext()) {
                    addCellsMissingInShorterTable(exec, buf, outputTableSize, rowNo, refIter, colMapping,
                        PORT_REFERENCE_TABLE);
                } else if (compIter.hasNext()) {
                    addCellsMissingInShorterTable(exec, buf, outputTableSize, rowNo, compIter, colMapping,
                        PORT_COMPARED_TABLE);
                }
            }
        }

        // close table
        buf.close();
        return buf.getTable();
    }

    /**
     * This method checks if there are differences in the values of two cells. In case the data type of two cells do not
     * match they are considered as unequal.
     *
     * @param buf
     * @param pos Colmapping for two columns
     * @param refRow from the reference table
     * @param compRow from the compared table
     * @param rowKeyNo Current processed row
     */
    private static void writeDifferingCellEntries(final BufferedDataContainer buf, final Entry<String, int[]> pos,
        final DataRow refRow, final DataRow compRow, final FailureMode confFailMode) {
        final int[] positions = pos.getValue();
        final int refColIdx = positions[PORT_REFERENCE_TABLE];
        final int compColIdx = positions[PORT_COMPARED_TABLE];

        if (refColIdx != -1 && compColIdx != -1) {
            final DataCell refCell = refRow.getCell(refColIdx);
            final DataCell compCell = compRow.getCell(compColIdx);
            //Cells are unequal if types not matching or values are different
            if (!refCell.getType().equals(compCell.getType()) || !refCell.equals(compCell)) {
                //Fail in case Fail on different values is selected
                if (confFailMode == FailureMode.DIFFERENT_VALUES) {
                    throw new IllegalArgumentException(ERROR_DIFFERENT_VALUES);
                }
                buf.addRowToTable(createCellDiffRow(buf.size(), pos.getKey(), refCell, compCell,
                    createStringCellFromRowKey(refRow), createStringCellFromRowKey(compRow)));
            }
        } else {
            addRowForMissingCols(buf, pos.getKey(), refRow, compRow, refColIdx, compColIdx);
        }
    }

    /**
     * This method adds the values from a certain column in case a column is missing in one table.
     *
     * @param buf BufferedDataContainer to add new rows
     * @param colName Column name
     * @param refRow Row from the reference table
     * @param compRow Row from the compared table
     * @param posReferenceTable position of the column in the reference table
     * @param posComparedTable position of the column in the compared table
     */
    private static void addRowForMissingCols(final BufferedDataContainer buf, final String colName,
        final DataRow refRow, final DataRow compRow, final int posReferenceTable, final int posComparedTable) {
        final DataCell origRefKey = createStringCellFromRowKey(refRow);
        final DataCell origCompKey = createStringCellFromRowKey(compRow);
        if (posReferenceTable == -1) {
            buf.addRowToTable(createCellDiffRow(buf.size(), colName, MISSING_COLUMN_CELL,
                compRow.getCell(posComparedTable), origRefKey, origCompKey));
        } else {
            buf.addRowToTable(createCellDiffRow(buf.size(), colName, refRow.getCell(posReferenceTable),
                MISSING_COLUMN_CELL, origRefKey, origCompKey));
        }
    }

    private static DataCell createStringCellFromRowKey(final DataRow k) {
        return StringCellFactory.create(k.getKey().getString());
    }

    /**
     * This method adds the remaining values to the table in case both input tables differ in length.
     *
     * @param exec
     * @param buf BufferedDataContainer to add new rows
     * @param tableSize Length of the longest table
     * @param startRowNo Number of the at least processed row
     * @param nonEmptyIter RowIterator from the longer table
     * @param colMapping to identify the positions of the columns in both tables
     * @param isTop Flag to differ if it is the reference table or the compared table
     *
     * @throws CanceledExecutionException
     */
    private static void addCellsMissingInShorterTable(final ExecutionContext exec, final BufferedDataContainer buf,
        final long tableSize, final long startRowNo, final RowIterator nonEmptyIter,
        final Map<String, int[]> colMapping, final int posIdx) throws CanceledExecutionException {

        // for each remaining row from the longer table add a row for each column to the output table
        for (long rowNo = startRowNo; nonEmptyIter.hasNext(); rowNo++) {
            // add missing cell to the output table
            addMissingCells(buf, colMapping, nonEmptyIter.next(), posIdx);

            // update the progress
            final long rowNoFinal = rowNo;
            exec.setProgress(rowNo / (double)tableSize, () -> ("Checked row " + rowNoFinal));
            exec.checkCanceled();
        }
    }

    /**
     * This method acts as an helper function of the addcellsMissingInShorterTable to reduce complexity.
     *
     * @param buf BufferedDataContainer to add new rows
     * @param colMapping to identify the positions of the columns in both tables
     * @param row The row of the longer table
     * @param isTop Flag to differ if it is the reference table or the compared table
     */
    private static void addMissingCells(final BufferedDataContainer buf, final Map<String, int[]> colMapping,
        final DataRow row, final int posIdx) {
        for (Entry<String, int[]> pos : colMapping.entrySet()) {

            // can be null as the column might only exists in the other, shorter table
            final int colIdx = pos.getValue()[posIdx];
            DataCell refCell;
            if (colIdx == -1) {
                refCell = MISSING_COLUMN_CELL;
            } else {
                refCell = row.getCell(colIdx);
            }

            // initialize the remaining cells
            final DataCell compCell;
            final DataCell refRowIdentifier;
            final DataCell compRowIdentifier;
            if (posIdx == PORT_REFERENCE_TABLE) {
                compCell = pos.getValue()[PORT_COMPARED_TABLE] >= 0 ? MISSING_ROW_CELL : MISSING_COLUMN_CELL;
                refRowIdentifier = createStringCellFromRowKey(row);
                compRowIdentifier = MISSING_ROW_CELL;
            } else {
                compCell = refCell;
                refCell = pos.getValue()[PORT_REFERENCE_TABLE] >= 0 ? MISSING_ROW_CELL : MISSING_COLUMN_CELL;
                refRowIdentifier = MISSING_ROW_CELL;
                compRowIdentifier = createStringCellFromRowKey(row);
            }

            // create the row and add it to the table
            buf.addRowToTable(
                createCellDiffRow(buf.size(), pos.getKey(), refCell, compCell, refRowIdentifier, compRowIdentifier));
        }
    }

    /**
     * Creates a new row for the value difference table.
     *
     * @param rowIdx current Row number
     * @param colName Column Name
     * @param refCell Cell from the reference table
     * @param compCell Cell from the compared table
     * @param rowIdentifier Row number from reference table
     * @return
     */
    private static DefaultRow createCellDiffRow(final long rowIdx, final String colName, final DataCell refCell,
        final DataCell compCell, final DataCell refRowIdentifier, final DataCell compRowIdentifier) {
        return new DefaultRow(RowKey.createRowKey(rowIdx), refRowIdentifier, compRowIdentifier,
            StringCellFactory.create(colName), refCell, compCell);
    }

    /**
     * This method compares the DataColumnSpecs ofcolumns in both tables. If there are any differences in the column
     * spec, those differences will be saved in a new row.
     *
     * @param buf BufferedDataContainer with the bottom output Spec
     * @param refSpec spec of the reference table
     * @param compSpec spec of the comparison table
     * @param colMapping Mapping of the column names to their indices in the different tables
     * @return BufferedDataTable
     * @throws CanceledExecutionException - If the execution has been canceled
     */
    private static BufferedDataTable getSpecDifferences(final ExecutionContext exec, final DataTableSpec refSpec,
        final DataTableSpec compSpec, final Map<String, int[]> colMapping, final FailureMode confFailMode)
        throws CanceledExecutionException {

        BufferedDataContainer buf = exec.createDataContainer(SPEC_TABLE_SPEC);

        long rowKeyNo = 0L;

        for (int[] pos : colMapping.values()) {
            checkSpecDifferences(buf, rowKeyNo++, refSpec, pos[PORT_REFERENCE_TABLE], compSpec,
                pos[PORT_COMPARED_TABLE], confFailMode);
            exec.checkCanceled();
        }
        buf.close();
        return buf.getTable();
    }

    /**
     * This method creates a new row for every column spec of common column as well as uncommon columns.
     *
     * @param buf BuffereDataContainer to add addtional rows
     * @param rowKeyNo to create a continuous row id
     * @param refSpec the reference spec
     * @param refPos position of the column from the reference table
     * @param compSpec the comparision spec
     * @param compPos position of the column from the compared table
     *
     */
    private static void checkSpecDifferences(final BufferedDataContainer buf, final long rowKeyNo,
        final DataTableSpec refSpec, final int refPos, final DataTableSpec compSpec, final int compPos,
        final FailureMode confFailMode) {

        DataColumnSpec refDataColSpec = null;
        if (refPos != -1) {
            refDataColSpec = refSpec.getColumnSpec(refPos);
        }

        DataColumnSpec compDataColSpec = null;
        if (compPos != -1) {
            compDataColSpec = compSpec.getColumnSpec(compPos);
        }

        final DataColumnDomain refDomain;
        final DataColumnDomain compDomain;
        final boolean specEquals;
        final boolean domainEquals;
        final boolean typeEquals;
        if (refDataColSpec != null && compDataColSpec != null) {
            specEquals = refDataColSpec.equals(compDataColSpec);
            refDomain = refDataColSpec.getDomain();
            compDomain = compDataColSpec.getDomain();
            domainEquals = refDomain.equals(compDomain);
            typeEquals = refDataColSpec.getType().equals(compDataColSpec.getType());
        } else {
            refDomain = null;
            compDomain = null;
            specEquals = false;
            domainEquals = false;
            typeEquals = false;
        }

        if (!specEquals
            && (confFailMode == FailureMode.DIFFERENT_SPECS || confFailMode == FailureMode.DIFFERENT_VALUES)) {
            throw new IllegalArgumentException(ERROR_DIFFERENT_SPECS);
        }
        // only null if we call this method during configure
        final DataCell refName =
            refDataColSpec != null ? StringCellFactory.create(refDataColSpec.getName()) : MISSING_COLUMN_CELL;
        final DataCell compName =
            compDataColSpec != null ? StringCellFactory.create(compDataColSpec.getName()) : MISSING_COLUMN_CELL;

        buf.addRowToTable(
            new DefaultRow(RowKey.createRowKey(rowKeyNo), refName, compName, BooleanCellFactory.create(typeEquals),
                BooleanCellFactory.create(domainEquals), BooleanCellFactory.create(refPos == compPos)));
    }

    @Override
    protected void reset() {
        //no internals
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_comparedColumns.saveSettingsTo(settings);
        m_compareTablesEntirely.saveSettingsTo(settings);
        m_failureMode.saveSettingsTo(settings);
    }

    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_comparedColumns.loadSettingsFrom(settings);
        m_compareTablesEntirely.loadSettingsFrom(settings);
        m_failureMode.loadSettingsFrom(settings);
    }

    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_comparedColumns.validateSettings(settings);
        m_compareTablesEntirely.validateSettings(settings);
        m_failureMode.validateSettings(settings);
    }

    @Override
    protected void loadInternals(final File internDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        //no internals

    }

    @Override
    protected void saveInternals(final File internDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        //no internals
    }

    enum FailureMode implements ButtonGroupEnumInterface {

            NEVER("Never", true),

            DIFFERENT_SPECS("Different table specs", false),

            DIFFERENT_VALUES("Different values", false);

        private final String m_label;

        private final boolean m_isDefault;

        /**
         * Constructor.
         *
         * @param label the label
         * @param isDefault default value flag
         */
        private FailureMode(final String label, final boolean isDefault) {
            m_label = label;
            m_isDefault = isDefault;
        }

        @Override
        public String getText() {
            return m_label;
        }

        @Override
        public String getActionCommand() {
            return name();
        }

        @Override
        public String getToolTip() {
            return null;
        }

        @Override
        public boolean isDefault() {
            return m_isDefault;
        }

        static FailureMode getValueFromLabel(final String label) {
            if (label == null) {
                throw new NullPointerException("Label is null");
            }
            return Arrays.stream(FailureMode.values()).filter(fm -> fm.m_label.equals(label)).findFirst().orElseThrow(
                () -> new IllegalArgumentException("No enum constant associated with the given label '" + label + "'"));
        }

    }

}
