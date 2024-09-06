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
package org.knime.base.node.preproc.stringreplacer.dict2;

import java.util.ArrayList;
import java.util.Arrays;

import org.knime.base.node.preproc.stringreplacer.PatternType;
import org.knime.base.node.preproc.stringreplacer.dict2.DictReplacer.IllegalReplacementException;
import org.knime.base.node.preproc.stringreplacer.dict2.DictReplacer.IllegalSearchPatternException;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.StringValue;
import org.knime.core.data.container.AbstractCellFactory;
import org.knime.core.data.container.CellFactory;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.KNIMEException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.message.Message;
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
 * Implementation of the (new) String Replacer (Dictionary) node. This node performs row-by-row string manipulations
 * based on a dictionary of patterns and replacement texts
 *
 * @author Jasper Krauter, KNIME GmbH, Konstanz, Germany
 */
@SuppressWarnings("restriction") // New Node UI is not yet API
public class StringReplacerDictNodeModel extends WebUINodeModel<StringReplacerDictNodeSettings> {

    static final NodeLogger LOGGER = NodeLogger.getLogger(StringReplacerDictNodeModel.class);

    /**
     * TODO: Get rid of this once UIEXT-722 is merged This is currently only needed to support streaming execution.
     */
    private StringReplacerDictNodeSettings m_settings;

    /**
     * Instantiate a new String Replacer (Dictionary) Node
     *
     * @param configuration node description
     * @param modelSettingsClass a reference to {@link StringReplacerDictNodeSettings}
     */
    StringReplacerDictNodeModel(final WebUINodeConfiguration configuration,
        final Class<StringReplacerDictNodeSettings> modelSettingsClass) {
        super(configuration, modelSettingsClass);
    }

    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs,
        final StringReplacerDictNodeSettings modelSettings) throws InvalidSettingsException {
        m_settings = modelSettings; // TODO remove once UIEXT-722 is merged

        CheckUtils.checkSettingNotNull(modelSettings.m_patternColumn,
            "Select a pattern column from the dictionary table");
        CheckUtils.checkSettingNotNull(modelSettings.m_replacementColumn,
            "Select a replacement column from the dictionary table");

        // Create a dummy rearranger to extract the result spec
        var rearranger = createColumnRearranger(modelSettings, inSpecs[0], inSpecs[1], null, null);
        return new DataTableSpec[]{rearranger.createSpec()};
    }

    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec,
        final StringReplacerDictNodeSettings modelSettings) throws Exception {
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
     * Create a {@link ColumnRearranger} that will handle all the matching and replacement logic
     *
     * @param targetSpec The spec of the target/data table
     * @param dictSpec the spec of the dictionary table
     * @param dictTable the data in the dictionary table (can be null, as long as the rearranger is not called)
     * @param dictInitMon a {@link ExecutionMonitor} that reports the progress of reading the dictionary
     * @return the column rearranger that will handle the execution logic
     * @throws InvalidSettingsException if some settings are invalid of type requirements are not met
     */
    private static ColumnRearranger createColumnRearranger(final StringReplacerDictNodeSettings modelSettings,
        final DataTableSpec targetSpec, final DataTableSpec dictSpec, final BufferedDataTable dictTable,
        final ExecutionMonitor dictInitMon) throws InvalidSettingsException {
        CheckUtils.checkArgumentNotNull(targetSpec, "No spec available for data table");
        CheckUtils.checkArgumentNotNull(dictSpec, "No spec available for dictionary table");

        var stringCols = StringReplacerDictNodeSettings.getStringCompatibleColumns(targetSpec);
        var stringColNames = Arrays.stream(stringCols).map(DataColumnSpec::getName).toArray(String[]::new);

        // Also get missing columns to warn user if the node is misconfigured
        var targetCols = modelSettings.m_targetColumns.getSelectedIncludingMissing(stringColNames, targetSpec);
        if (targetCols.length == 0) {
            return new ColumnRearranger(targetSpec);
        }
        var targetColIndices = Arrays.stream(targetCols).mapToInt(targetSpec::findColumnIndex).toArray();

        for (var i = 0; i < targetCols.length; i++) {
            CheckUtils.checkSetting(targetColIndices[i] >= 0,
                "There is no target column \"%s\" in the data table. Please reconfigure the node.", targetCols[i]);
            CheckUtils.checkSetting(
                targetSpec.getColumnSpec(targetColIndices[i]).getType().isCompatible(StringValue.class),
                "The type of target column \"%s\" is not string-compatible.", targetCols[i]);
        }

        var patternColIndex = dictSpec.findColumnIndex(modelSettings.m_patternColumn);
        CheckUtils.checkSetting(patternColIndex >= 0, "No such pattern column \"%s\" in dictionary table",
            modelSettings.m_patternColumn);
        CheckUtils.checkSetting(dictSpec.getColumnSpec(patternColIndex).getType().isCompatible(StringValue.class),
            "The pattern column type is not string-compatible");
        var replacementColIndex = dictSpec.findColumnIndex(modelSettings.m_replacementColumn);
        CheckUtils.checkSetting(replacementColIndex >= 0, "No such replacement column \"%s\" in dictionary table",
            modelSettings.m_patternColumn);
        CheckUtils.checkSetting(dictSpec.getColumnSpec(replacementColIndex).getType().isCompatible(StringValue.class),
            "The replacement column type is not string-compatible");

        var newColumns = new ArrayList<DataColumnSpec>();
        for (var column : targetCols) {
            var newName = modelSettings.m_appendColumns
                ? DataTableSpec.getUniqueColumnName(targetSpec, column + modelSettings.m_columnSuffix) : column;
            var cSpec = new DataColumnSpecCreator(newName, StringCell.TYPE).createSpec();
            newColumns.add(cSpec);
        }
        var factory = createCellFactory(modelSettings, newColumns.toArray(DataColumnSpec[]::new), targetColIndices,
            dictTable, dictInitMon, patternColIndex, replacementColIndex);
        var rearranger = new ColumnRearranger(targetSpec);

        if (modelSettings.m_appendColumns) {
            rearranger.append(factory);
        } else {
            rearranger.replace(factory, targetColIndices);
        }

        return rearranger;
    }

    /**
     * Create the cell factory that will produce cells with the processed strings in them
     *
     * @param modelSettings the node settings
     * @param newColumns a list of specs for the new columns
     * @param targetCols the indices of the original target columns
     * @param dictTable the dictionary table
     * @return the cell factory that handles the execution logic of the node
     */
    private static CellFactory createCellFactory(final StringReplacerDictNodeSettings modelSettings,
        final DataColumnSpec[] newColumns, final int[] targetColIndices, final BufferedDataTable dictTable,
        final ExecutionMonitor dictInitMon, final int patternColIndex, final int replacementColIndex) {
        return new AbstractCellFactory(newColumns) { //NOSONAR: anonymous class is a good option here
            private DictReplacer<?> m_replacer;

            private long m_progressPeriod;

            @Override
            public DataCell[] getCells(final DataRow row) {
                if (m_replacer == null) {
                    try {
                        initDict();
                    } catch (CanceledExecutionException e) {//NOSONAR
                        // The execution has been cancelled -- return the right amount of missing cells as dummies.
                        // The surrounding execution logic will disregard that result and display a warning
                        // Why this catch - clause? Because we want to override AbstractCellFactory#getCells and include
                        // this initialisation logic to support streaming execution
                        //   -> we cannot re-throw the exception
                        var missingCells = new DataCell[newColumns.length];
                        Arrays.fill(missingCells, DataType.getMissingCell());
                        return missingCells;
                    }
                }

                return Arrays.stream(targetColIndices) //
                    .mapToObj(row::getCell) //
                    .map(t -> { //NOSONAR: lambda is short enough, only long because of line breaks
                        try {
                            return t.isMissing() ? t
                                : new StringCell(m_replacer.process(((StringValue)t).getStringValue()));
                        } catch (IllegalReplacementException e) {
                            throw KNIMEException
                                .of(Message.fromSummary(
                                    "Could not replace in row '" + row.getKey() + "': " + e.getMessage()), e)
                                .toUnchecked();
                        }
                    }) //
                    .toArray(DataCell[]::new);
            }

            /**
             * Initialise the dictionary by reading the rows consecutively and inserting the pattern-replacement pairs
             * into the {@link DictReplacer} instance
             *
             * @throws CanceledExecutionException if the execution has been cancelled
             */
            private void initDict() throws CanceledExecutionException {
                m_progressPeriod = dictTable.size() / 25 + 1;
                m_replacer = modelSettings.m_patternType == PatternType.LITERAL
                    ? new StringReplacer(modelSettings) : new PatternReplacer(modelSettings);
                DataRow currentRow = null;
                var rowCounter = 0;
                try {
                    for (var row : dictTable) {
                        currentRow = row; // might need that before insert for proper error reporting
                        ++rowCounter;
                        m_replacer.insertDictRow(row, patternColIndex, replacementColIndex);
                        reportProcessedDictRow(rowCounter);
                    }
                } catch (IllegalSearchPatternException e) {
                    // Most likely a PatternSyntaxException, or some other faulty data that couldn't be processed
                    throw KNIMEException.of(
                        Message.fromRowIssue("Could not insert search pattern in row '" + currentRow.getKey() + "'",
                            1, rowCounter, patternColIndex, e.getMessage()),
                        e).toUnchecked();
                } catch (IllegalReplacementException e) {
                    // Most likely a missing cell in the replacement column
                    throw KNIMEException.of(Message.fromRowIssue(
                        "Could not insert replacement string in row '" + currentRow.getKey() + "'", 1, rowCounter,
                        patternColIndex, e.getMessage()), e).toUnchecked();
                }
            }

            /**
             * Increments {@code m_processedDictRows} by one, updates the progress bar and checks for cancelled
             * execution. See {@link ExecutionMonitor#checkCanceled()}.
             *
             * @throws CanceledExecutionException
             */
            private void reportProcessedDictRow(final int rowCounter) throws CanceledExecutionException {
                if (rowCounter % m_progressPeriod == 0) {
                    dictInitMon.setProgress(rowCounter / (double)dictTable.size(),
                        "Reading dictionary into memory, row " + rowCounter + " of " + dictTable.size());
                    dictInitMon.checkCanceled(); // Might throw a CanceledExecutionException
                }
            }
        };
    }

}
