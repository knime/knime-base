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
import java.util.stream.Stream;

import org.knime.base.node.preproc.valuelookup.ValueLookupNodeSettings.DictionaryTableChoices;
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
import org.knime.core.webui.node.dialog.impl.WebUINodeConfiguration;
import org.knime.core.webui.node.dialog.impl.WebUINodeModel;

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

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs, final ValueLookupNodeSettings modelSettings)
        throws InvalidSettingsException {
        m_settings = modelSettings; // TODO remove once UIEXT-722 is merged

        CheckUtils.checkSettingNotNull(modelSettings.m_lookupCol, "Select a lookup column from the data table");
        CheckUtils.checkSettingNotNull(modelSettings.m_dictKeyCol, "Select a key column from the dictionary table");

        // Create a dummy rearranger to extract the result spec
        var rearranger = createColumnRearranger(modelSettings, inSpecs[0], inSpecs[1], null, null);
        return new DataTableSpec[]{rearranger.createSpec()};
    }

    /**
     * {@inheritDoc}
     */
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

    /**
     * {@inheritDoc}
     */
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

    /**
     * {@inheritDoc}
     */
    @Override
    public InputPortRole[] getInputPortRoles() {
        // The node always need the complete dictionary (port 1), but can lookup and insert values (port 0) row-by-row.
        return new InputPortRole[]{InputPortRole.DISTRIBUTED_STREAMABLE, InputPortRole.NONDISTRIBUTED_NONSTREAMABLE};
    }

    /**
     * {@inheritDoc}
     */
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
        var targetColIndex = targetSpec.findColumnIndex(modelSettings.m_lookupCol);
        CheckUtils.checkSetting(targetColIndex >= 0, "No such column \"%s\"", modelSettings.m_lookupCol);

        CheckUtils.checkArgumentNotNull(dictSpec, "No spec available for dictionary table");
        var dictInputColIndex = dictSpec.findColumnIndex(modelSettings.m_dictKeyCol);
        CheckUtils.checkSetting(dictInputColIndex >= 0, "No such column \"%s\"", modelSettings.m_dictKeyCol);

        final var dictValueCols = modelSettings.m_dictValueCols.getSelected(
            DictionaryTableChoices.choices(dictSpec), dictSpec);
        final var dictOutputColIndices = dictSpec.columnsToIndices(dictValueCols);
        final var insertedColumns = new ArrayList<DataColumnSpec>();
        // Add the columns to the output spec, but check for existence and uniquify name w.r.t. the input table
        for (var col : dictOutputColIndices) {
            CheckUtils.checkSetting(col >= 0, "No such column \"%s\"", col);
            var oldSpec = dictSpec.getColumnSpec(col);
            var newSpec = new DataColumnSpecCreator(oldSpec);
            if (!(modelSettings.m_deleteLookupCol
                && targetSpec.getColumnNames()[targetColIndex].equals(oldSpec.getName()))) {
                // The new column name might clash with the existing ones
                newSpec.setName(DataTableSpec.getUniqueColumnName(targetSpec, oldSpec.getName()));
            }
            insertedColumns.add(newSpec.createSpec());
        }

        if (modelSettings.m_createFoundCol) {
            var name = DataTableSpec.getUniqueColumnName(targetSpec, COLUMN_NAME_MATCHFOUND);
            var foundSpec = new DataColumnSpecCreator(name, BooleanCell.TYPE);
            insertedColumns.add(foundSpec.createSpec());
        }

        final var rearranger = new ColumnRearranger(targetSpec);
        if (modelSettings.m_deleteLookupCol) {
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
        final var cellFactory = createCellFactory(modelSettings, insertedColumns.toArray(DataColumnSpec[]::new),
            targetColIndex, dictTable, dictOutputColIndices, dictInitMon, comparator);
        rearranger.append(cellFactory); // All the new columns are appended to the end of the row
        return rearranger;
    }

    /**
     * Create a {@link CellFactory} that, given an input row, looks it up in the dictionary table and appends produces
     * either the corresponding output columns or missing cells
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

            @Override
            public DataCell[] getCells(final DataRow row) {
                if (m_dict == null) {
                    try {
                        m_dict = new DictFactory(modelSettings, dictTable, dictOutputColIndices, comparator,
                            dictInitMon).initialiseDict();
                        LOGGER.debug("Using dictionary implementation \"" + m_dict.getClass().getSimpleName() + "\"");
                    } catch (CanceledExecutionException e) {//NOSONAR
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
                var lookup = row.getCell(targetColIndex);
                var result = m_dict.getCells(lookup);
                DataCell[] replacements;

                // Handle the presence / absence of a match
                if (result.isPresent()) {
                    replacements = result.get();
                } else {
                    var numReplacementCols =
                        modelSettings.m_createFoundCol ? (insertedColumns.length - 1) : insertedColumns.length;
                    replacements = new DataCell[numReplacementCols];
                    Arrays.fill(replacements, DataType.getMissingCell());
                }

                if (modelSettings.m_createFoundCol) {
                    // Add the "found/not found" column and return both columns
                    return Stream
                        .concat(Arrays.stream(replacements), Stream.of(BooleanCellFactory.create(result.isPresent())))
                        .toArray(DataCell[]::new);
                } else {
                    // Just return the replacement columns
                    return replacements;
                }
            }
        };
    }
}
