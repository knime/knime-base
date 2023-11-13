/*
 * ------------------------------------------------------------------------
 *
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
 * ---------------------------------------------------------------------
 *
 * History
 *   21 Oct 2022 (jasper): created
 */
package org.knime.base.node.preproc.valuelookup;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import org.knime.base.node.preproc.valuelookup.ValueLookupNodeSettings.DictionaryTableChoices;
import org.knime.base.node.preproc.valuelookup.ValueLookupNodeSettings.LookupColumnNoMatchReplacement;
import org.knime.base.node.preproc.valuelookup.ValueLookupNodeSettings.LookupColumnOutput;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.container.AbstractCellFactory;
import org.knime.core.data.container.CellFactory;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.def.BooleanCell;
import org.knime.core.data.def.BooleanCell.BooleanCellFactory;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.streamable.InputPortRole;
import org.knime.core.node.streamable.OutputPortRole;
import org.knime.core.node.streamable.PartitionInfo;
import org.knime.core.node.streamable.PortInput;
import org.knime.core.node.streamable.PortObjectInput;
import org.knime.core.node.streamable.PortOutput;
import org.knime.core.node.streamable.StreamableOperator;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.webui.node.impl.WebUINodeConfiguration;
import org.knime.core.webui.node.impl.WebUINodeModel;

/**
 * Implementation of the Value Lookup node. This node looks up values in a dictionary table (2nd input) and adds them to
 * the data table (1st input), or inserts missing cells.
 *
 * @author Jasper Krauter, KNIME GmbH, Konstanz, Germany
 */
@SuppressWarnings("restriction") // New Node UI is not yet API
public class ValueLookupNodeModel extends WebUINodeModel<ValueLookupNodeSettings> {
    /**
     * The name of the column that can be added to indicate whether a match has been found
     */
    static final String COLUMN_NAME_MATCHFOUND = "Match Found";

    /**
     * TODO: Get rid of this once UIEXT-722 is merged This is currently only needed to support streaming execution.
     */
    private ValueLookupNodeSettings m_settings;

    static final NodeLogger LOGGER = NodeLogger.getLogger(ValueLookupNodeModel.class);

    /**
     * Instantiate a new Value Lookup Node
     *
     * @param configuration node description
     * @param modelSettingsClass a reference to {@link ValueLookupNodeSettings}
     */
    ValueLookupNodeModel(final WebUINodeConfiguration configuration,
        final Class<ValueLookupNodeSettings> modelSettingsClass) {
        super(configuration, modelSettingsClass);
    }

    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs, final ValueLookupNodeSettings modelSettings)
        throws InvalidSettingsException {
        m_settings = modelSettings; // TODO remove once UIEXT-722 (streaming support) is merged

        CheckUtils.checkSettingNotNull(modelSettings.m_lookupCol, "Select a lookup column from the data table");
        CheckUtils.checkSettingNotNull(modelSettings.m_dictKeyCol, "Select a key column from the dictionary table");

        // Create a dummy rearranger to extract the result spec
        var rearranger = createColumnRearranger(modelSettings, inSpecs[0], inSpecs[1], null, null);
        return new DataTableSpec[]{rearranger.createSpec()};
    }

    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec,
        final ValueLookupNodeSettings modelSettings) throws Exception {
        // Reference to the two tables
        var targetTable = inData[0];
        var dictTable = inData[1];

        // Some numbers to calculate progress bar
        double tableSize = targetTable.size();
        double dictSize = dictTable.size();
        double dictCreateAmount = dictSize / (dictSize + tableSize);
        double outputCreateAmount = tableSize / (dictSize + tableSize);

        // Create the actual rearranger using the input table and dictionary table
        var rearranger = createColumnRearranger(modelSettings, targetTable.getDataTableSpec(),
            dictTable.getDataTableSpec(), dictTable, exec.createSubProgress(dictCreateAmount));
        // Use that rearranger to produce the output
        var output =
            exec.createColumnRearrangeTable(targetTable, rearranger, exec.createSubProgress(outputCreateAmount));
        return new BufferedDataTable[]{output};
    }

    @Override
    public StreamableOperator createStreamableOperator(final PartitionInfo partitionInfo,
        final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        // TODO once UIEXT-722 is merged, change signature and refactor settings to use the settings that are passed as
        // an argument
        return new StreamableOperator() {
            @Override
            public void runFinal(final PortInput[] inputs, final PortOutput[] outputs, final ExecutionContext exec)
                throws Exception {
                // Cast the dict table to a BufferedDataTable (since it cannot be streamed)
                final var dictTable = (BufferedDataTable)((PortObjectInput)inputs[1]).getPortObject();
                // Create the rearranger (once again)
                var rearranger = createColumnRearranger(m_settings, (DataTableSpec)inSpecs[0],
                    dictTable.getDataTableSpec(), dictTable, exec);
                // Use the rearranger logic to create the streamable function and execute the node
                var func = rearranger.createStreamableFunction(0, 0);
                func.runFinal(inputs, outputs, exec);
            }
        };
    }

    @Override
    public InputPortRole[] getInputPortRoles() {
        // The node always need the complete dictionary (port 1), but can lookup and insert values (port 0) row-by-row.
        return new InputPortRole[]{InputPortRole.DISTRIBUTED_STREAMABLE, InputPortRole.NONDISTRIBUTED_NONSTREAMABLE};
    }

    @Override
    public OutputPortRole[] getOutputPortRoles() {
        return new OutputPortRole[]{OutputPortRole.DISTRIBUTED};
    }

    /**
     * Create a {@link ColumnRearranger} that will handle all the lookup and insertion logic
     *
     * @param targetSpec The spec of the target/data table
     * @param dictSpec the spec of the dictionary table
     * @param dictTable the data in the dictionary table (can be null, as long as the rearranger is not executed)
     * @param dictInitMon
     * @return
     * @throws InvalidSettingsException
     */
    private static ColumnRearranger createColumnRearranger(final ValueLookupNodeSettings modelSettings,
        final DataTableSpec targetSpec, final DataTableSpec dictSpec, final BufferedDataTable dictTable,
        final ExecutionMonitor dictInitMon) throws InvalidSettingsException {
        CheckUtils.checkArgumentNotNull(targetSpec, "No spec available for target table");
        CheckUtils.checkArgumentNotNull(dictSpec, "No spec available for dictionary table");

        final var targetColIndex = targetSpec.findColumnIndex(modelSettings.m_lookupCol);
        CheckUtils.checkSetting(targetColIndex >= 0, "No such column \"%s\"", modelSettings.m_lookupCol);

        final var dictInputColIndex = dictSpec.findColumnIndex(modelSettings.m_dictKeyCol);
        CheckUtils.checkSetting(dictInputColIndex >= 0, "No such column \"%s\"", modelSettings.m_dictKeyCol);

        final var lookupReplacementColumnIndex = dictSpec.findColumnIndex(modelSettings.m_lookupReplacementCol);
        CheckUtils.checkSetting(
            modelSettings.m_lookupColumnOutput != LookupColumnOutput.REPLACE || lookupReplacementColumnIndex >= 0,
            "Select a valid column to replace the lookup column. %s",
            Optional.ofNullable(modelSettings.m_lookupReplacementCol).map(name -> "No such column \"" + name + "\"")
                .orElse("None selected."));

        // the columns to be created by the factory
        class ColumnsToAppend {
            // specs of the columns to be returned by the factory
            final List<DataColumnSpec> m_specs = new ArrayList<>();

            // the column indices in the dictionary table to be collected by the dictionary data structure
            // in case of lookup column replacement, the lookup column might be contained twice (not perfect but simple)
            final int[] m_dictOutputColIndices;

            // offset (in the appended columns) of the column that replaces the lookup column
            final int m_extraReplaceColIdx;

            ColumnsToAppend() throws InvalidSettingsException {
                // columns to pull in from the dictionary table
                final var dictValueColNames =
                    modelSettings.m_dictValueCols.getSelected(DictionaryTableChoices.choices(dictSpec), dictSpec);
                final int[] selectedDictCols = dictSpec.columnsToIndices(dictValueColNames);
                // Add the columns to the output spec, but check for existence and uniquify name w.r.t. the input table
                for (var col : selectedDictCols) {
                    CheckUtils.checkSetting(col >= 0, "No such column \"%s\"", col);
                    var oldSpec = dictSpec.getColumnSpec(col);
                    var newSpec = new DataColumnSpecCreator(oldSpec);
                    if (!(modelSettings.m_lookupColumnOutput == LookupColumnOutput.REMOVE
                        && modelSettings.m_lookupCol.equals(oldSpec.getName()))) {
                        // The new column name might clash with the existing ones
                        newSpec.setName(DataTableSpec.getUniqueColumnName(targetSpec, oldSpec.getName()));
                    }
                    m_specs.add(newSpec.createSpec());
                }

                if (modelSettings.m_lookupColumnOutput == LookupColumnOutput.REPLACE) {
                    // make sure the column selected to replace the lookup column is part of the lookup data structure
                    m_dictOutputColIndices = Arrays.copyOf(selectedDictCols, selectedDictCols.length + 1);
                    m_dictOutputColIndices[m_dictOutputColIndices.length - 1] =
                        dictSpec.findColumnIndex(modelSettings.m_lookupReplacementCol);

                    // add column to replace the lookup column - with the common super type and the lookup column's name
                    // not necessary to make column name unique - the original lookup column will be removed
                    final var oldDictSpec = dictSpec.getColumnSpec(modelSettings.m_lookupReplacementCol);
                    final var newDictSpec = new DataColumnSpecCreator(oldDictSpec);
                    newDictSpec.setName(modelSettings.m_lookupCol);

                    // if retain values on missing replacement is selected, we can get mixed type cells
                    // if missing is selected, all values will have the type of the dictionary column
                    if (modelSettings.m_columnNoMatchReplacement == LookupColumnNoMatchReplacement.RETAIN) {
                        var commonSuperType = DataType.getCommonSuperType(newDictSpec.getType(),
                            targetSpec.getColumnSpec(modelSettings.m_lookupCol).getType());
                        newDictSpec.setType(commonSuperType);
                    }

                    m_specs.add(newDictSpec.createSpec());
                } else {
                    m_dictOutputColIndices = selectedDictCols;
                }

                // optional found column
                if (modelSettings.m_createFoundCol) {
                    var name = DataTableSpec.getUniqueColumnName(targetSpec, COLUMN_NAME_MATCHFOUND);
                    var foundSpec = new DataColumnSpecCreator(name, BooleanCell.TYPE);
                    m_specs.add(foundSpec.createSpec());
                }
                m_extraReplaceColIdx = modelSettings.m_createFoundCol ? (m_specs.size() - 2) : (m_specs.size() - 1);
            }

            /**
             * Which of the factory's produced output cells goes where in the input table. For example:
             *
             * <pre>
             * input table column names A L B
             *
             * dictionary table column names  X Y Z
             *
             * model settings
             * target/lookup column = L
             * replacement column = Y
             * include from dict = Y Z
             *
             * factory output Y Z L // L has same contents as Y
             *
             * The output table is formed by first appending the factory output.
             * Ambiguous column names are ok in an intermediate state.
             * A L B Y Z L
             * Then we replace columns 3, 4, 1 with the output of the cell factory (Y, Z, L).
             * Replacing Y and Z is a no op and replacing L updates the lookup column contents.
             * A L B Y Z L
             * Finally, we remove column 5.
             * A L B Y Z
             * </pre>
             *
             * This whole dance around the column rearranger is not the very readable but gives us streaming for free.
             *
             * @return new column indices for the cells output by the cell factory
             */
            int[] replacementMapping() {
                // number of columns in the table to append to
                final int n = targetSpec.getNumColumns()
                    - (modelSettings.m_lookupColumnOutput == LookupColumnOutput.REMOVE ? 1 : 0);
                // by default, all columns are appended to the end
                var map = IntStream.range(n, n + m_specs.size()).toArray();
                // in case of replace the extra column takes the place of the original lookup column
                // (and is deleted afterwards)
                if (modelSettings.m_lookupColumnOutput == LookupColumnOutput.REPLACE) {
                    // replace the target column with the second last column
                    map[m_extraReplaceColIdx] = targetColIndex;
                }
                return map;
            }
        }

        final var newColumns = new ColumnsToAppend();

        final var rearranger = new ColumnRearranger(targetSpec);
        if (modelSettings.m_lookupColumnOutput == LookupColumnOutput.REMOVE) {
            // The index is the same as in the input table since we only appended columns
            rearranger.remove(targetColIndex);
        }

        var dictKeyColType = dictSpec.getColumnSpec(dictInputColIndex).getType();
        if (dictKeyColType.isCollectionType()) {
            dictKeyColType = dictKeyColType.getCollectionElementType();
        }
        var lookupColType = targetSpec.getColumnSpec(targetColIndex).getType();
        var comparator = DataType.getCommonSuperType(dictKeyColType, lookupColType).getComparator();

        // Create the actual cell factory using the values that were checked and extracted above
        final var cellFactory = createCellFactory(modelSettings, newColumns.m_specs.toArray(DataColumnSpec[]::new),
            targetColIndex, dictTable, newColumns.m_dictOutputColIndices, dictInitMon, comparator);

        // all the new columns are appended to the end of the row, including the column to replace the lookup column
        rearranger.append(cellFactory);
        if (modelSettings.m_lookupColumnOutput == LookupColumnOutput.REPLACE) {
            // replace the original lookup column
            rearranger.replace(cellFactory, newColumns.replacementMapping());
            // remove the additional column appended to the end
            rearranger.remove(targetSpec.getNumColumns() + newColumns.m_extraReplaceColIdx);
        }
        return rearranger;
    }

    /**
     * Create a {@link CellFactory} that, given an input row, looks it up in the dictionary table and appends produces
     * either the corresponding output columns or missing cells.
     *
     * The returned cells are
     * <ul>
     * <li>the cells from the dictionary table columns that have been selected for appending</li>
     * <li>if REPLACE is selected for the lookup column: the cell from the dictionary table column that has been
     * selected as replacement column (redundant if in the selected columns, too)</li>
     * <li>if CREATE FOUND COLUMN is selected: a boolean cell indicating whether a match has been found</li>
     * </ul>
     *
     * @param insertedColumns the specs of the new columns
     * @param targetColIndex the column index of the cell that shall be looked up in the dictionary
     * @param dictTable the dictionary (can be null, as long as the factory is not queried with a row, since only on the
     *            first query the dictionary is initialised)
     * @param indices of columns from dictionary that are included in the output
     * @param dictInitMon an {@link ExecutionMonitor} that represents the progress of reading the dictionary table
     * @param comparator Comparator that can compare cells from the key and lookup columns
     * @param dictInputColIndex the index of the key column in the dictionary table
     * @param dictOutputColIndices indices of all the columns (in the dictionary table) that shall be inserted into the
     *            target table
     *
     * @return
     */
    private static CellFactory createCellFactory(final ValueLookupNodeSettings modelSettings,
        final DataColumnSpec[] insertedColumns, final int targetColIndex, final BufferedDataTable dictTable,
        final int[] dictOutputColIndices, final ExecutionMonitor dictInitMon, final Comparator<DataCell> comparator) {
        return new AbstractCellFactory(insertedColumns) { // NOSONAR: this anonymous class is easy to read
            /**
             * This map holds the input/output pairs of the dictionary table, once initialised. Will only be
             * initialised, once it's needed.
             */
            private LookupDict m_dict;

            /**
             * Returns C1, ..., Cn, [X], [F] with
             * <ul>
             * <li>C1, ..., Cn the values of the dictionary table columns that were selected for appending</li>
             * <li>X the column selected to replace the lookup column, if
             * {@link ValueLookupNodeSettings#m_lookupColumnOutput} equals REPLACE</li>
             * <li>F whether the value was found or not, if {@link ValueLookupNodeSettings#m_createFoundCol} is
             * true</li>
             * </ul>
             */
            @Override
            public DataCell[] getCells(final DataRow row) {
                if (m_dict == null) {
                    try {
                        m_dict =
                            new DictFactory(modelSettings, dictTable, dictOutputColIndices, comparator, dictInitMon)
                                .initialiseDict();
                        LOGGER.debug("Using dictionary implementation \"" + m_dict.getClass().getSimpleName() + "\"");
                    } catch (CanceledExecutionException e) {//NOSONAR (see below)
                        // The execution has been cancelled -- return the right amount of missing cells as dummies.
                        // The surrounding execution logic will disregard that result and display a warning
                        // Why this catch - clause? Because we want to override AbstractCellFactory#getCells and include
                        // this initialisation logic to support streaming execution
                        //   -> we cannot re-throw the exception
                        var missingCells = new DataCell[insertedColumns.length];
                        Arrays.fill(missingCells, DataType.getMissingCell());
                        return missingCells;
                    }
                }

                // Try to match the target cell to a dictionary entry
                final var lookup = row.getCell(targetColIndex);
                final var result = m_dict.getCells(lookup);
                final var replacements = new DataCell[insertedColumns.length];

                // Handle the presence / absence of a match
                if (result.isPresent()) {
                    // contains the values of columns C1, ..., Cn that were selected for appending
                    // if replace lookup column with column X is selected, contains C1, ..., Cn, X
                    final var dictContent = result.get();
                    System.arraycopy(result.get(), 0, replacements, 0, dictContent.length);
                } else {
                    Arrays.fill(replacements, DataType.getMissingCell());
                    if (modelSettings.m_lookupColumnOutput == LookupColumnOutput.REPLACE
                        && modelSettings.m_columnNoMatchReplacement == LookupColumnNoMatchReplacement.RETAIN) {
                        final int replacementColIdx = replacements.length - (modelSettings.m_createFoundCol ? 2 : 1);
                        replacements[replacementColIdx] = lookup;
                    }
                }

                if (modelSettings.m_createFoundCol) {
                    replacements[replacements.length - 1] = BooleanCellFactory.create(result.isPresent());
                }
                return replacements;
            }
        };
    }
}
