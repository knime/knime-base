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
 * ---------------------------------------------------------------------
 *
 * History
 *   Sep 1, 2008 (wiswedel): created
 */
package org.knime.base.node.preproc.regexsplit;

import static org.knime.node.parameters.widget.text.util.ColumnNameValidationUtils.validateColumnName;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

import org.apache.commons.lang3.StringUtils;
import org.knime.base.node.preproc.regexsplit.CaptureGroupExtractor.CaptureGroup;
import org.knime.base.node.preproc.regexsplit.OutputSettings.OutputGroupLabelMode;
import org.knime.base.node.preproc.regexsplit.OutputSettings.OutputMode;
import org.knime.base.node.preproc.regexsplit.OutputSettings.SingleOutputColumnMode;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataTableSpecCreator;
import org.knime.core.data.DataType;
import org.knime.core.data.RowKey;
import org.knime.core.data.StringValue;
import org.knime.core.data.collection.ListCell;
import org.knime.core.data.collection.SetCell;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.KNIMEException;
import org.knime.core.node.KNIMEException.KNIMERuntimeException;
import org.knime.core.node.message.Message;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.streamable.BufferedDataTableRowOutput;
import org.knime.core.node.streamable.DataTableRowInput;
import org.knime.core.node.streamable.InputPortRole;
import org.knime.core.node.streamable.OutputPortRole;
import org.knime.core.node.streamable.PartitionInfo;
import org.knime.core.node.streamable.PortInput;
import org.knime.core.node.streamable.PortOutput;
import org.knime.core.node.streamable.RowInput;
import org.knime.core.node.streamable.RowOutput;
import org.knime.core.node.streamable.StreamableOperator;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.webui.node.dialog.defaultdialog.widget.validation.ColumnNameValidationMessageBuilder;
import org.knime.core.webui.node.impl.WebUINodeConfiguration;
import org.knime.core.webui.node.impl.WebUINodeModel;
import org.knime.node.parameters.widget.text.util.ColumnNameValidationUtils.InvalidColumnNameState;

/**
 * Implementation of the String Splitter (Regex) (formerly known as Regex Split)
 *
 * @author wiswedel, University of Konstanz
 * @author Jasper Krauter, KNIME GmbH, Konstanz, Germany
 */
@SuppressWarnings("restriction")
final class RegexSplitNodeModel extends WebUINodeModel<RegexSplitNodeSettings> {

    RegexSplitNodeModel(final WebUINodeConfiguration cfg) {
        super(cfg, RegexSplitNodeSettings.class);
    }

    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs, final RegexSplitNodeSettings settings)
        throws InvalidSettingsException {
        if (settings.m_column == null) {
            throw new InvalidSettingsException("No input column selected.");
        }
        getInputColumnIndex(inSpecs[0], settings.m_column); // throws if invalid
        final var outputSpec = switch (settings.m_output.m_mode) {
            case COLUMNS, LIST, SET -> createColumnRearranger(settings, inSpecs[0], this::setWarning).createSpec();
            case ROWS -> createTableSpecForRowOutput(settings, inSpecs[0]);
        };
        return new DataTableSpec[]{outputSpec};
    }

    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec,
        final RegexSplitNodeSettings settings) throws Exception {
        final BufferedDataTable table = switch (settings.m_output.m_mode) {
            case COLUMNS, LIST, SET -> exec.createColumnRearrangeTable(inData[0],
                createColumnRearranger(settings, inData[0].getDataTableSpec(), this::setWarning), exec);
            case ROWS -> createRowOutputTable(inData[0], exec, settings);
        };
        return new BufferedDataTable[]{table};
    }

    @Override
    public StreamableOperator createStreamableOperator(final PartitionInfo partitionInfo,
        final PortObjectSpec[] inSpecs, final RegexSplitNodeSettings settings) throws InvalidSettingsException {
        final var spec = (DataTableSpec)inSpecs[0]; // safe to assume, see javadoc
        return switch (settings.m_output.m_mode) {
            case COLUMNS, LIST, SET -> createColumnRearranger(settings, spec, this::setWarning)
                .createStreamableFunction(0, 0);
            case ROWS -> createStreamableOperatorForRowOutput(settings, spec, this::setWarning);
        };
    }

    @Override
    public InputPortRole[] getInputPortRoles() {
        return new InputPortRole[]{InputPortRole.DISTRIBUTED_STREAMABLE};
    }

    @Override
    public OutputPortRole[] getOutputPortRoles() {
        return new OutputPortRole[]{OutputPortRole.DISTRIBUTED};
    }

    private static final Function<InvalidColumnNameState, String> INVALID_COL_NAME_TO_ERROR_MSG =
        new ColumnNameValidationMessageBuilder("output column name").build();

    @Override
    protected void validateSettings(final RegexSplitNodeSettings settings) throws InvalidSettingsException {
        RegexSplitter.fromSettings(settings); // might throw
        if (settings.m_doNotAllowEmptyBlankOrPaddedColumnName && settings.m_output.m_mode != OutputMode.COLUMNS
            && settings.m_output.m_singleOutputColumnMode == SingleOutputColumnMode.APPEND) {
            validateColumnName(settings.m_output.m_columnName, INVALID_COL_NAME_TO_ERROR_MSG);
        }
    }

    // ##################################
    // ### COLUMN / COLLECTION OUTPUT ###
    // ##################################

    private static ColumnRearranger createColumnRearranger(final RegexSplitNodeSettings settings,
        final DataTableSpec inputTableSpec, final Consumer<Message> warningConsumer) throws InvalidSettingsException {
        final var inputColumnIndex = getInputColumnIndex(inputTableSpec, settings.m_column);
        final var rearranger = new ColumnRearranger(inputTableSpec);
        final var splitter = RegexSplitter.fromSettings(settings);
        if (settings.m_output.m_mode == OutputMode.COLUMNS) {
            if (settings.m_output.m_removeInputColumn) {
                rearranger.remove(inputColumnIndex);
            }
            final var newColumns = createNewColumnSpecsForMultiColumnOutput(settings, rearranger, splitter);
            final var factory =
                new RegexSplitResultCellFactory(newColumns, inputColumnIndex, settings, splitter, warningConsumer);
            rearranger.append(factory);
        } else if (settings.m_output.m_mode == OutputMode.LIST || settings.m_output.m_mode == OutputMode.SET) {
            final DataColumnSpec spec = createNewColumnSpecForSingleColumnOutput(settings, inputTableSpec);
            final var factory = new RegexSplitResultCellFactory(new DataColumnSpec[]{spec}, inputColumnIndex, settings,
                splitter, warningConsumer);
            switch (settings.m_output.m_singleOutputColumnMode) {//NOSONAR: No, an if-statement is not more readable here
                case APPEND -> rearranger.append(factory);
                case REPLACE -> rearranger.replace(factory, inputColumnIndex);
            }
        }
        return rearranger;
    }

    // ##################
    // ### ROW OUTPUT ###
    // ##################

    private static DataTableSpec createTableSpecForRowOutput(final RegexSplitNodeSettings settings,
        final DataTableSpec inSpec) throws InvalidSettingsException {
        final var cSpec = createNewColumnSpecForSingleColumnOutput(settings, inSpec);
        final var creator = new DataTableSpecCreator(inSpec);
        switch (settings.m_output.m_singleOutputColumnMode) { //NOSONAR: switch is more explicit than if
            case APPEND -> creator.addColumns(cSpec);
            case REPLACE -> {
                creator.dropAllColumns();
                creator.addColumns(inSpec.stream().map(c -> c.getName().equals(settings.m_column) ? cSpec : c)
                    .toArray(DataColumnSpec[]::new));
            }
        }
        return creator.createSpec();
    }

    private static StreamableOperator createStreamableOperatorForRowOutput(final RegexSplitNodeSettings settings,
        final DataTableSpec inSpec, final Consumer<Message> warningConsumer) {
        return new StreamableOperator() {
            @Override
            public void runFinal(final PortInput[] inputs, final PortOutput[] outputs, final ExecutionContext exec)
                throws Exception {
                RowInput input = (RowInput)inputs[0];
                RowOutput output = (RowOutput)outputs[0];
                produceRows(settings, input, output, exec, -1, inSpec, warningConsumer);
                input.close();
                output.close();
            }
        };
    }

    private BufferedDataTable createRowOutputTable(final BufferedDataTable inputTable, final ExecutionContext exec,
        final RegexSplitNodeSettings settings)
        throws InvalidSettingsException, CanceledExecutionException, InterruptedException {
        final var dc = exec.createDataContainer(createTableSpecForRowOutput(settings, inputTable.getSpec()));
        if (inputTable.size() == 0) {
            dc.close();
            return dc.getTable();
        }
        final var in = new DataTableRowInput(inputTable);
        final var out = new BufferedDataTableRowOutput(dc);
        try {
            produceRows(settings, in, out, exec, inputTable.size(), inputTable.getDataTableSpec(), this::setWarning);
        } finally {
            in.close();
            out.close();
        }
        return out.getDataTable();
    }

    private static void produceRows(final RegexSplitNodeSettings settings, final RowInput input, final RowOutput output,
        final ExecutionContext exec, final long n, final DataTableSpec inSpec, final Consumer<Message> warningConsumer)
        throws CanceledExecutionException, InterruptedException, InvalidSettingsException {
        final var inputColumnIndex = getInputColumnIndex(inSpec, settings.m_column);
        final var splitter = RegexSplitter.fromSettings(settings);
        final var rowIDSuffixes = getOutputGroupLabels(settings, splitter);
        CheckUtils.checkState(splitter.getCaptureGroups().size() == rowIDSuffixes.length,
            "There were more or less capture groups than row id suffixes.");
        final var newSpecs = new DataColumnSpec[splitter.getCaptureGroups().size()];
        Arrays.fill(newSpecs, createNewColumnSpecForSingleColumnOutput(settings, inSpec));
        final var factory =
            new RegexSplitResultCellFactory(newSpecs, inputColumnIndex, settings, splitter, warningConsumer);
        var counter = 0L;
        DataRow row;
        while ((row = input.poll()) != null) {
            counter++;
            exec.checkCanceled();
            if (n > 0 && counter % (n / 100 + 1) == 0) { // occasionally update the progress
                exec.setProgress(counter / (double)n, "Processing row " + counter + " of " + n);
            }
            final var matches = factory.getCells(row, counter);
            var innerCounter = 0; // used to find row ID suffix
            for (final var match : matches) {
                final DataCell[] cells;
                if (settings.m_output.m_singleOutputColumnMode == SingleOutputColumnMode.APPEND) {
                    cells = Arrays.copyOf(row.stream().toArray(DataCell[]::new), row.getNumCells() + 1);
                    cells[cells.length - 1] = match;
                } else { // replace
                    cells = Arrays.copyOf(row.stream().toArray(DataCell[]::new), row.getNumCells());
                    cells[inputColumnIndex] = match;
                }
                final var newKey = new RowKey(row.getKey() + "_" + rowIDSuffixes[innerCounter]);
                output.push(new DefaultRow(newKey, cells));
                innerCounter++;
            }
        }
    }

    // ####################
    // ### CREATE SPECS ###
    // ####################

    private static DataColumnSpec[] createNewColumnSpecsForMultiColumnOutput(final RegexSplitNodeSettings settings,
        final ColumnRearranger rearranger, final RegexSplitter splitter) throws KNIMERuntimeException {
        final var newColumns = new DataColumnSpec[splitter.getCaptureGroups().size()];
        final var newColPrefix = switch (settings.m_output.m_columnPrefixMode) {
            case NONE -> "";
            case INPUT_COL_NAME -> settings.m_column + " ";
            case CUSTOM -> settings.m_output.m_columnPrefix;
        };
        final var suffixes = getOutputGroupLabels(settings, splitter);
        final var outSpec = rearranger.createSpec();

        for (var g : splitter.getCaptureGroups()) {
            var name = newColPrefix + suffixes[g.index() - 1];
            name = DataTableSpec.getUniqueColumnName(outSpec, name);
            newColumns[g.index() - 1] = new DataColumnSpecCreator(name, StringCell.TYPE).createSpec();
        }

        return newColumns;
    }

    private static DataColumnSpec createNewColumnSpecForSingleColumnOutput(final RegexSplitNodeSettings settings,
        final DataTableSpec inputTableSpec) throws InvalidSettingsException {
        final DataType outputColumnType = switch (settings.m_output.m_mode) {
            case LIST -> ListCell.getCollectionType(StringCell.TYPE);
            case SET -> SetCell.getCollectionType(StringCell.TYPE);
            case ROWS -> StringCell.TYPE;
            default -> throw new IllegalArgumentException(
                "For output mode COLUMNS, please use #createNewColumnSpecsForColumnOutput");
        };
        final DataColumnSpecCreator sc;
        if (settings.m_output.m_singleOutputColumnMode == SingleOutputColumnMode.APPEND) {
            final var name = DataTableSpec.getUniqueColumnName(inputTableSpec, settings.m_output.m_columnName);
            sc = new DataColumnSpecCreator(name, outputColumnType);
        } else {
            final var inputColumnIndex = getInputColumnIndex(inputTableSpec, settings.m_column);
            final var inputColumnSpec = inputTableSpec.getColumnSpec(inputColumnIndex);
            sc = new DataColumnSpecCreator(inputColumnSpec);
            sc.setType(outputColumnType);
            sc.setDomain(null);
        }
        if (settings.m_output.m_mode.isCollection()) {
            sc.setElementNames(getOutputGroupLabels(settings, RegexSplitter.fromSettings(settings)));
        }
        return sc.createSpec();
    }

    // ###############
    // ### HELPERS ###
    // ###############

    private static String getGroupLabel(final RegexSplitNodeSettings settings, final CaptureGroup g) {
        return g.name().orElse(String.valueOf(settings.m_decrementGroupIndexByOne ? (g.index() - 1) : g.index()));
    }

    /**
     * Generate suffixes for column names / row IDs. The output does not contain duplicates nor blank strings nor null.
     *
     * @param settings The settings instance of the node
     * @param splitter The already-initialised {@link RegexSplitter}.
     * @return An array of suffixes that are unique, non-blank and not null.
     */
    private static String[] getOutputGroupLabels(final RegexSplitNodeSettings settings, final RegexSplitter splitter) {
        if (settings.m_output.m_groupLabels == OutputGroupLabelMode.CAPTURE_GROUP_NAMES) {
            return splitter.getCaptureGroups().stream().map(g -> getGroupLabel(settings, g)).toArray(String[]::new);
        } else {
            return ensureUnique( // The output array should not contain duplicate strings
                splitter.apply(settings.m_column)
                    .orElseThrow(() -> KNIMEException
                        .of(Message.fromSummary("The pattern does not match the input column name, "
                            + "but the node is configured to split the input column name to get the suffixes."))
                        .toUnchecked()) //
                    .stream().map(r -> r // r might be empty (because the capture group was optional) or blank
                        .filter(StringUtils::isNotBlank) // Filter out blank matches (e.g. "")
                        .orElseThrow(() -> KNIMEException
                            .of(Message.fromSummary("When matching the pattern to the input column name, "
                                + "one or more (optional) capture groups were not present or blank."))
                            .toUnchecked()))
                    .toArray(String[]::new)) // The output of ensureUnique(T[]) is empty, if we have duplicates
                        .orElseThrow(() -> KNIMEException
                            .of(Message.fromSummary("When matching the pattern to the input column name, "
                                + "two or more (optional) capture groups contained the same string. "
                                + "Please ensure that splitting the input column name produces unique identifiers."))
                            .toUnchecked());
        }
    }

    private static int getInputColumnIndex(final DataTableSpec tableSpec, final String name)
        throws InvalidSettingsException {
        final var inputColumnIndex = tableSpec.findColumnIndex(name);
        CheckUtils.checkSetting(inputColumnIndex >= 0, "No such column in input table: %s", name);
        CheckUtils.checkSetting(tableSpec.getColumnSpec(inputColumnIndex).getType().isCompatible(StringValue.class),
            "Selected column does not contain strings");
        return inputColumnIndex;
    }

    /**
     * Checks whether an array (without null entries) contains only distinct values (as per
     * {@link Object#equals(Object)}.
     *
     * @param <T> The type of the array elements
     * @param arr The array to check
     * @return The input array wrapped in an {@link Optional}, if the array only has unique entries, or
     *         {@link Optional#empty()} if a duplicate has been found.
     */
    private static <T> Optional<T[]> ensureUnique(final T[] arr) {
        for (var i = 0; i < arr.length; ++i) {
            for (var j = i + 1; j < arr.length; ++j) {
                if (Objects.requireNonNull(arr[i]).equals(arr[j])) {
                    return Optional.empty();
                }
            }
        }
        return Optional.of(arr);
    }

}
