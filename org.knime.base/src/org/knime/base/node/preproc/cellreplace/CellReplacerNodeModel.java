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
 * ------------------------------------------------------------------------
 *
 * History
 *   Aug 7, 2010 (wiswedel): created
 */
package org.knime.base.node.preproc.cellreplace;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.knime.base.node.preproc.cellreplace.CellReplacerNodeSettings.NoMatchPolicy;
import org.knime.base.node.preproc.cellreplace.CellReplacerNodeSettings.StringMatchBehaviour;
import org.knime.base.util.WildcardMatcher;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.StringValue;
import org.knime.core.data.collection.CollectionDataValue;
import org.knime.core.data.container.AbstractCellFactory;
import org.knime.core.data.container.CellFactory;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.streamable.InputPortRole;
import org.knime.core.node.streamable.OutputPortRole;
import org.knime.core.node.streamable.PartitionInfo;
import org.knime.core.node.streamable.PortInput;
import org.knime.core.node.streamable.PortObjectInput;
import org.knime.core.node.streamable.PortOutput;
import org.knime.core.node.streamable.StreamableOperator;
import org.knime.core.node.util.CheckUtils;

/**
 * This is the model implementation of CellReplacer. Replaces cells in a column according to dictionary table (2nd
 * input)
 *
 * @author Bernd Wiswedel
 * @deprecated Replaced by "Value Lookup" node
 */
@Deprecated
public class CellReplacerNodeModel extends NodeModel {

    /**
     * Holds the user-specified settings for this node
     */
    private final CellReplacerNodeSettings m_settings;

    /**
     * Constructor for the node model.
     */
    protected CellReplacerNodeModel() {
        super(2, 1);
        m_settings = new CellReplacerNodeSettings();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {
        var rearranger = createColumnRearranger(inSpecs[0], inSpecs[1], null, null);
        return new DataTableSpec[]{rearranger.createSpec()};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec)
        throws Exception {
        BufferedDataTable table = inData[0];
        BufferedDataTable dictionary = inData[1];
        double tableSize = table.size();
        double dictSize = dictionary.size();
        double dictCreateAmount = dictSize / (dictSize + tableSize);
        double outputCreateAmount = tableSize / (dictSize + tableSize);
        var rearranger = createColumnRearranger(inData[0].getDataTableSpec(), inData[1].getDataTableSpec(), inData[1],
            exec.createSubProgress(dictCreateAmount));

        BufferedDataTable output =
            exec.createColumnRearrangeTable(table, rearranger, exec.createSubProgress(outputCreateAmount));
        return new BufferedDataTable[]{output};
    }

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
                BufferedDataTable dictTable = (BufferedDataTable)((PortObjectInput)inputs[1]).getPortObject();
                var rearranger =
                    createColumnRearranger((DataTableSpec)inSpecs[0], dictTable.getDataTableSpec(), dictTable, exec);
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
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        // no internals
    }

    /**
     * Create the column rearranger that will either replace the target column with the replacement column or append the
     * replacement column. Additionally, a "found/not found" column might be appended.
     *
     * @param spec The {@link DataTableSpec} for the input table (in which a column is to be replaced)
     * @param dictSpec The {@link DataTableSpec} for the dictionary / lookup table. Passed separately to
     *            {@code dictTable} for use in node configuration
     * @param dictTable The actual dictionary / lookup table (if available)
     * @param dictInitExec An {@link ExecutionMonitor} that keeps track of the process of reading the dictionary into
     *            memory (if applicable)
     * @return The configured {@link ColumnRearranger}
     * @throws InvalidSettingsException If the method encounters invalid column names
     */
    private ColumnRearranger createColumnRearranger(final DataTableSpec spec, final DataTableSpec dictSpec,
        final BufferedDataTable dictTable, final ExecutionMonitor dictInitExec) throws InvalidSettingsException {
        // Gather information on the target column
        final var targetColIndex = getColIndex(spec, m_settings.getTargetColNameModel());
        CheckUtils.checkSetting(targetColIndex >= 0, //
            "No such column \"%s\"", m_settings.getTargetColName());
        final var targetColSpec = spec.getColumnSpec(targetColIndex);

        // Get the column index of the input / lookup column in the dictionary table
        final int dictInputColIndex;
        final boolean isDictInputString;
        if (m_settings.getDictInputColModel().useRowID()) {
            dictInputColIndex = -1;
            isDictInputString = true;
        } else {
            dictInputColIndex = getColIndex(dictSpec, m_settings.getDictInputColModel());
            CheckUtils.checkSetting(dictInputColIndex >= 0 || m_settings.getDictInputColModel().useRowID(),
                "No such column \"%s\"", m_settings.getDictInputColModel().getStringValue());
            var dictInputColSpec = dictSpec.getColumnSpec(dictInputColIndex);
            isDictInputString = dictInputColSpec.getType().isCompatible(StringValue.class);
        }

        // Gather information on the output / replacement column in the dictionary table
        final int dictOutputColIndex;
        final DataColumnSpec dictColSpec;
        if (m_settings.isDictOutputRowID()) {
            dictOutputColIndex = -1;
            // name not relevant, overwritten further down -- important is string type
            dictColSpec = new DataColumnSpecCreator("rowID", StringCell.TYPE).createSpec();
        } else {
            dictOutputColIndex = getColIndex(dictSpec, m_settings.getDictOutputColModel());
            CheckUtils.checkSetting(dictOutputColIndex >= 0, //
                "No such column \"%s\"", m_settings.getTargetColName());
            dictColSpec = dictSpec.getColumnSpec(dictOutputColIndex);
        }

        // Define the spec for the replacement column (i.e. the "output column" of this node
        final DataColumnSpecCreator replaceColumnSpecCreator;
        if (m_settings.getNoMatchPolicy() == NoMatchPolicy.INPUT) {
            // Both the original type and replacement type might occur in the column
            var outputType = DataType.getCommonSuperType(dictColSpec.getType(), targetColSpec.getType());
            // resets all properties, element names, color/size/shape handlers etc -- it's a NEW column
            replaceColumnSpecCreator = new DataColumnSpecCreator(targetColSpec.getName(), outputType);
        } else if (m_settings.isRetainColumnProperties()) {
            // Only the type of the output column in dictionary can occur, so use that type
            // retain all properties, element names, and such
            replaceColumnSpecCreator = new DataColumnSpecCreator(dictColSpec);
        } else {
            // Same as above, but create a new spec to get rid of metadata / column properties
            replaceColumnSpecCreator = new DataColumnSpecCreator(dictColSpec.getName(), dictColSpec.getType());
        }

        // Set the name of the replacement column
        if (m_settings.isAppendColumn()) {
            // We use the user-provided name for the new column, but "uniquify" it first to avoid any name clashes
            final var newName = m_settings.getAppendColumnName();
            CheckUtils.checkSetting(StringUtils.isNotEmpty(newName), "No new column name given");
            replaceColumnSpecCreator.setName(DataTableSpec.getUniqueColumnName(spec, newName));
        } else {
            // We re-use the name of the target column, since we replace it
            replaceColumnSpecCreator.setName(targetColSpec.getName());
        }

        // Create column specs of the output. This might be one or two columns: The replacement column and possibly a
        // "found/not found" column
        final DataColumnSpec[] producedColumns;
        if (m_settings.isAppendFoundColumn()) {
            // Same as above, generate new column name first
            var foundColumnName = DataTableSpec.getUniqueColumnName(spec, "found");
            producedColumns = new DataColumnSpec[]{replaceColumnSpecCreator.createSpec(),
                new DataColumnSpecCreator(foundColumnName, StringCell.TYPE).createSpec()};
        } else {
            // Just the replacement column will be produced
            producedColumns = new DataColumnSpec[]{replaceColumnSpecCreator.createSpec()};
        }

        // The cell factory will, given a data row, produce the required column. It does the actual lookups.
        final var c = createCellFactory(producedColumns, targetColIndex, dictTable, dictInputColIndex,
            dictOutputColIndex, isDictInputString, dictInitExec);

        // Now, create the column rearranger
        var result = new ColumnRearranger(spec);
        if (m_settings.isAppendColumn()) {
            // append (both) column at the end
            result.append(c);
        } else if (m_settings.isAppendFoundColumn()) { // add "found/not found" column, but don't append the result
            // replace target column, keep "found/not found" column at the end
            result.append(c);
            result.remove(targetColIndex);
            result.move(result.getColumnCount() - 2, targetColIndex);
        } else { // don't append either column --> replace target column
            // there's only one column to handle
            result.replace(c, targetColIndex);
        }

        return result;
    }

    /**
     * Create the {@link CellFactory} that will read the dictionary to memory and then perform lookups on the input
     * data, and produce the output columns
     *
     * @param producedColumns The {@link DataColumnSpec}s for the output column. This is either one element (the
     *            replacement coulumn) or two (the former and a "found/not found" column)
     * @param targetColIndex The index of the target column
     * @param dictTable The dictionary table
     * @param dictInputColIndex The index of the input / lookup column in the dictionary table
     * @param dictOutputColIndex The index of the output / replacement column in the dictionary table
     * @param dictInitExec An {@link ExecutionMonitor} that keeps track of reading the dictionary into memory
     * @return The configured {@link CellFactory}
     */
    @SuppressWarnings({"squid:S3776", "squid:S1188"}) // Hide complexity warnings -- using an anonymous class is easier
                                                      // than creating an extra class
    private CellFactory createCellFactory(final DataColumnSpec[] producedColumns, final int targetColIndex,
        final BufferedDataTable dictTable, final int dictInputColIndex, final int dictOutputColIndex,
        final boolean isDictInputString, final ExecutionMonitor dictInitExec) {
        return new AbstractCellFactory(producedColumns) {
            /**
             * This map holds the input/output pairs of the dictionary table, once initialised. Will only be
             * initialised, once it's needed.
             */
            private Map<DataCell, DataCell> m_dict;

            @Override
            public DataCell[] getCells(final DataRow row) {
                if (m_dict == null) {
                    try {
                        initialiseDictionary();
                    } catch (CanceledExecutionException e) {//NOSONAR
                        // The execution has been cancelled -- return the right amount of missing cells as dummies.
                        // The surrounding execution logic will disregard that result and display a warning
                        // Why this catch - clause? Because we want to override AbstractCellFactory#getCells and include
                        // this initialisation logic to support streaming execution
                        //   -> we cannot re-throw the exception
                        if (!m_settings.isAppendFoundColumn()) {
                            return new DataCell[]{DataType.getMissingCell()};
                        } else {
                            return new DataCell[]{DataType.getMissingCell(), DataType.getMissingCell()};
                        }
                    }
                }

                // Try to match the target cell to a dictionary entry
                var lookup = row.getCell(targetColIndex);
                var replacement = getReplacementCell(lookup);
                var matchFound = replacement != null;

                // Handle the absence of a match
                if (!matchFound) {
                    if (m_settings.getNoMatchPolicy() == NoMatchPolicy.INPUT) {
                        replacement = lookup;
                    } else {
                        replacement = DataType.getMissingCell();
                    }
                }

                // Possibly add the "found/not found" column
                if (m_settings.isAppendFoundColumn()) {
                    var stryes = m_settings.getFoundColumnPositiveString();
                    var strno = m_settings.getFoundColumnNegativeString();
                    var str = matchFound ? stryes : strno;
                    // And return both columns
                    return new DataCell[]{replacement, new StringCell(str)};
                } else {
                    // Just return the replacement column
                    return new DataCell[]{replacement};
                }
            }

            /**
             * Retrieve the matching cell from the dictionary.
             *
             * @param row
             * @return The cell that should fill in for the old cell, or {@code null} if no match has been found
             */
            private DataCell getReplacementCell(DataCell lookup) {
                if (!isDictInputString || lookup.isMissing()) {
                    // fast
                    // "old" exact matching -- just look up the data cell in the map
                    return m_dict.get(lookup);
                } else if (m_settings.getStringMatchBehaviour() == StringMatchBehaviour.EXACT) {
                    // fast
                    // similar to old matching -- but might use lowercase cell
                    if (!m_settings.isCaseSensitive()) {
                        lookup = new StringCell(lookup.toString().toLowerCase(Locale.ROOT));
                    }
                    return m_dict.get(lookup);
                } else {
                    // slow (will iterate through the entire dictionary, trying to find a match)
                    return match(m_dict, lookup);
                }
            }

            /**
             * Initialise the dictionary, i.e. read the dict table and create Key-Value pairs in {@code m_dict}.
             *
             * @throws CanceledExecutionException
             */
            private void initialiseDictionary() throws CanceledExecutionException {
                if (!isDictInputString || m_settings.getStringMatchBehaviour() == StringMatchBehaviour.EXACT) {
                    // For direct lookup, we don't care about the order, since we will just lookup exact values
                    m_dict = new HashMap<>();
                } else {
                    // For all other match behaviour, we want to preserve the input order such that the first matching
                    // dictionary entry is chosen
                    m_dict = new LinkedHashMap<>();
                }
                // For progress monitoring
                var i = 0;
                double rowCount = dictTable.size();
                for (DataRow r : dictTable) {
                    dictInitExec.setProgress(i / rowCount, "Reading dictionary into memory, row " + i);
                    dictInitExec.checkCanceled(); // Throws a CanceledExecutionException if execution has been cancelled
                    // Either a stringCell with the rowID or the output cell
                    DataCell input =
                        dictInputColIndex < 0 ? new StringCell(r.getKey().getString()) : r.getCell(dictInputColIndex);
                    DataCell output =
                        dictOutputColIndex < 0 ? new StringCell(r.getKey().getString()) : r.getCell(dictOutputColIndex);
                    if (input.isMissing()) {
                        addSearchPair(input, output);
                    } else if (dictInputColIndex >= 0
                        && dictTable.getSpec().getColumnSpec(dictInputColIndex).getType().isCollectionType()) {
                        // Add an individual key-value pair for each entry in the collection type
                        var v = (CollectionDataValue)input;
                        for (DataCell element : v) {
                            addSearchPair(element, output);
                        }
                    } else {
                        addSearchPair(input, output);
                    }
                    ++i;
                }
            }

            /**
             * Add a key-value pair to the in-memory dictionary
             *
             * @param k
             * @param v
             */
            private void addSearchPair(DataCell k, final DataCell v) {
                if (!k.isMissing() && isDictInputString && !m_settings.isCaseSensitive()
                    && (m_settings.getStringMatchBehaviour() == StringMatchBehaviour.EXACT
                        || m_settings.getStringMatchBehaviour() == StringMatchBehaviour.SUBSTRING)) {
                    // Convert the search key to lowercase if the search is case-insensitive and we will not use
                    // RegEx-Flags to enable case-insensitivity. Doing this here once for the substring-method
                    // eliminates the need to do that for every lookup later.
                    k = new StringCell(k.toString().toLowerCase(Locale.ROOT));
                }
                var duplicate = m_dict.put(k, v);
                // Just a friendly warning ;)
                if (duplicate != null) {
                    setWarningMessage("Duplicate search key \"" + k + "\"");
                }
            }
        };
    }

    /**
     * Match a lookup string to a pattern in the dictionary. Note that, in the worst case (i.e. no match is found), this
     * method iterates through the entire dictionary for a single lookup string.
     *
     * @param dict The dictionary to use
     * @param lookup The lookup string
     * @return The replacement {@link DataCell}, or {@code null}, if no match has been found
     */
    private DataCell match(final Map<DataCell, DataCell> dict, final DataCell lookup) {
        var string = lookup.toString();
        for (var e : dict.entrySet()) {
            var pattern = e.getKey().toString();
            switch (m_settings.getStringMatchBehaviour()) {
                case SUBSTRING:
                    // Potentially convert the lookup string to lower-case, but not the pattern, since it has been
                    // inserted into the dictionary in lower-case already
                    string = m_settings.isCaseSensitive() ? string : string.toLowerCase(Locale.ROOT);
                    if (string.contains(pattern)) {
                        return e.getValue();
                    }
                    break;
                case WILDCARD: //NOSONAR: No break, we want to continue to next case
                    pattern = WildcardMatcher.wildcardToRegex(pattern);
                    // continue to RegEx case
                case REGEX:
                    // For Wildcard and Regex-matches, the dictionary input is unaltered, so we need to account for
                    // case-insensitivity here. We do that with the appropriate RegEx-Flags
                    var flags = (m_settings.isCaseSensitive()) ? 0 : (Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
                    if (Pattern.compile(pattern, flags).matcher(string).matches()) {
                        return e.getValue();
                    }
                    break;
                default:
                    // We should not reach this state, since exact matching is handled outside of this method.
                    throw new IllegalStateException("Invalid string matching behaviour");
            }
        }
        // No match has been found
        return null;
    }

    /**
     * Extract the column index
     *
     * @param spec
     * @param model
     * @return
     * @throws InvalidSettingsException
     */
    private static int getColIndex(final DataTableSpec spec, final SettingsModelString model)
        throws InvalidSettingsException {
        var col = model.getStringValue();
        if (col == null || col.length() <= 0) {
            throw new InvalidSettingsException("No " + model.getKey() + " column selected");
        }
        return spec.findColumnIndex(col);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_settings.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_settings.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        var s = new CellReplacerNodeSettings();
        s.loadSettingsFrom(settings);
        m_settings.getTargetColNameModel().validateSettings(settings);
        SettingsModelString clone = m_settings.getNoMatchPolicyModel().createCloneWithValidatedValue(settings);
        var nmp = NoMatchPolicy.getPolicy(clone.getStringValue());
        if (!nmp.isPresent()) {
            throw new InvalidSettingsException("Invalid policy: " + clone.getStringValue());
        }
        m_settings.getDictInputColModel().validateSettings(settings);
        m_settings.getDictOutputColModel().validateSettings(settings);
        m_settings.getAppendColumnModel().validateSettings(settings);
        m_settings.getAppendColumnNameModel().validateSettings(settings);
        // m_retainColumnPropertiesModel.validateSettings(settings); -- not needed, added in 4.1.3
        // string match behaviour and "found" / "not found" column aren't validated either -- added with AP-13269
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File internDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        // no internals
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File internDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        // no internals
    }

}
