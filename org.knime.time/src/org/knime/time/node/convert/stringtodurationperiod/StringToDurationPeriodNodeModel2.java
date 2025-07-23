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
 *   Feb 10, 2025 (david): created
 */
package org.knime.time.node.convert.stringtodurationperiod;

import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.lang3.StringUtils;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.MissingCell;
import org.knime.core.data.StringValue;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.container.SingleCellFactory;
import org.knime.core.data.time.duration.DurationCellFactory;
import org.knime.core.data.time.period.PeriodCellFactory;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.KNIMEException;
import org.knime.core.node.message.MessageBuilder;
import org.knime.core.webui.node.impl.WebUINodeConfiguration;
import org.knime.core.webui.node.impl.WebUINodeModel;
import org.knime.node.parameters.widget.choices.util.ColumnSelectionUtil;
import org.knime.time.node.convert.stringtodurationperiod.StringToDurationPeriodNodeSettings.DurationPeriodType;
import org.knime.time.util.ActionIfExtractionFails;
import org.knime.time.util.DurationPeriodFormatUtils;
import org.knime.time.util.ReplaceOrAppend.InputColumn;

/**
 * Node model for the "String to Duration/Period" node.
 *
 * @author David Hickey, TNG Technology Consulting GmbH
 */
@SuppressWarnings("restriction")
final class StringToDurationPeriodNodeModel2 extends WebUINodeModel<StringToDurationPeriodNodeSettings> {

    StringToDurationPeriodNodeModel2(final WebUINodeConfiguration configuration) {
        super(configuration, StringToDurationPeriodNodeSettings.class);
    }

    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs,
        final StringToDurationPeriodNodeSettings modelSettings) throws InvalidSettingsException {

        var warningMessageBuilder = createMessageBuilder();

        return new DataTableSpec[]{switch (modelSettings.m_durationType) {
            case AUTO_DETECT -> null; // we don't know until runtime what the output columns will be
            case DURATION, PERIOD -> createColumnRearranger(inSpecs[0], modelSettings,
                modelSettings.m_durationType.m_associatedDataTypeIfApplicable, warningMessageBuilder).createSpec();
        }};
    }

    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec,
        final StringToDurationPeriodNodeSettings modelSettings) throws Exception {

        var warningMessageBuilder = createMessageBuilder();

        var inTable = inData[0];
        var includedColumns = getSupportedTargetColumns(inTable.getDataTableSpec(), modelSettings);

        if (modelSettings.m_durationType == DurationPeriodType.AUTO_DETECT) {
            var types = autoDetectTypes(inTable, includedColumns);

            // if any columns could not be auto-guessed, we should warn or fail as appropriate. First find out
            // which columns failed to be auto-guessed.
            var indicesOfColsThatFailedAutoGuess = IntStream.range(0, types.length) //
                .filter(i -> types[i].isMissingValueType()) //
                .toArray();

            if (indicesOfColsThatFailedAutoGuess.length > 0
                && modelSettings.m_actionIfExtractionFails == ActionIfExtractionFails.FAIL) {
                var firstMissingColumnName = includedColumns.get(indicesOfColsThatFailedAutoGuess[0]);
                throw new KNIMEException(
                    "The type of column '%s' could not be auto-detected.".formatted(firstMissingColumnName));
            } else {
                Arrays.stream(indicesOfColsThatFailedAutoGuess).forEach(i -> warningMessageBuilder.addTextIssue(
                    "The type of column '%s' could not be auto-detected. All outputs from this column will be missing."
                        .formatted(includedColumns.get(i))));
            }

            var rearranger = createColumnRearranger(inTable.getDataTableSpec(), modelSettings,
                IntStream.range(0, types.length) //
                    .boxed() //
                    .collect(Collectors.toMap(includedColumns::get, i -> types[i], (l, r) -> l, TreeMap::new)),
                warningMessageBuilder);

            return new BufferedDataTable[]{ //
                exec.createColumnRearrangeTable(inTable, rearranger, exec) //
            };
        } else {
            var rearranger = createColumnRearranger(inTable.getDataTableSpec(), modelSettings,
                modelSettings.m_durationType.m_associatedDataTypeIfApplicable, warningMessageBuilder);

            return new BufferedDataTable[]{ //
                exec.createColumnRearrangeTable(inTable, rearranger, exec) //
            };
        }
    }

    /**
     * Try to autodetect the types of the columns in the input table. The first cell in a column that successfully
     * parses to either a duration or a period will determine the inferred type of that column. If no cell in a column
     * can be parsed to a duration or period, the column type will be inferred as missing.
     */
    private static DataType[] autoDetectTypes(final BufferedDataTable inTable, final List<String> includedColumns) {

        DataType[] detectedTypes = new DataType[includedColumns.size()];

        try (var it = inTable.iterator()) {
            while (it.hasNext() && Arrays.stream(detectedTypes).anyMatch(Objects::isNull)) {
                var row = it.next();
                detectTypesForOneRow(inTable.getDataTableSpec(), row, includedColumns, detectedTypes);
            }
        }

        // The old node assumed that anything it couldn't autog-uess was a period.
        // But in our case let's assume it's missing.
        for (int i = 0; i < detectedTypes.length; ++i) {
            if (detectedTypes[i] == null) {
                detectedTypes[i] = DataType.getMissingCell().getType();
            }
        }

        return detectedTypes;
    }

    /**
     * Modify the detectedTypesSoFar parameter by adding any additional types that can be inferred from the given row.
     * Non-null entries won't be touched.
     */
    private static void detectTypesForOneRow(final DataTableSpec spec, final DataRow row,
        final List<String> includedColumns, final DataType[] detectedTypesSoFar) {

        for (int i = 0; i < detectedTypesSoFar.length; ++i) {
            var cell = row.getCell(spec.findColumnIndex(includedColumns.get(i)));

            if (detectedTypesSoFar[i] != null || cell.isMissing()) {
                continue;
            }

            var stringValue = ((StringValue)cell).getStringValue();

            try {
                DurationPeriodFormatUtils.parseDuration(stringValue);
                detectedTypesSoFar[i] = DurationCellFactory.TYPE;
            } catch (DateTimeParseException e1) {
                try {
                    DurationPeriodFormatUtils.parsePeriod(stringValue);
                    detectedTypesSoFar[i] = PeriodCellFactory.TYPE;
                } catch (DateTimeParseException e2) {
                    // ignore, we only care about the first successful parse
                }
            }
        }
    }

    /**
     * Create a column rearranger that will convert the specified columns to the specified output types.
     */
    private ColumnRearranger createColumnRearranger(final DataTableSpec spec,
        final StringToDurationPeriodNodeSettings modelSettings,
        final SortedMap<String, DataType> inputColumnNameAndOutputType, final MessageBuilder messageBuilder) {

        var targetColumnNames = inputColumnNameAndOutputType.keySet();

        return modelSettings.m_replaceOrAppend.createRearranger(targetColumnNames, spec, (inputColumn, outputName) -> {
            var outputColType = inputColumnNameAndOutputType.get(inputColumn.spec().getName());
            var outputColSpec = new DataColumnSpecCreator(outputName, outputColType).createSpec();

            return new StringToDurationPeriodCellFactory(outputColSpec, inputColumn, modelSettings, messageBuilder);
        }, modelSettings.m_appendedSuffix, () -> {
            if (messageBuilder.getIssueCount() > 0) {
                messageBuilder.withSummary("%s warning%s encountered.".formatted(messageBuilder.getIssueCount(),
                    messageBuilder.getIssueCount() == 1 ? "" : "s")).build().ifPresent(this::setWarning);
            }
        });
    }

    /**
     * Create a column rearranger that will convert the all specified columns to the single specified output type.
     */
    private ColumnRearranger createColumnRearranger(final DataTableSpec spec,
        final StringToDurationPeriodNodeSettings modelSettings, final DataType outputType,
        final MessageBuilder warningMessageBuilder) {

        var targetColumns = getSupportedTargetColumns(spec, modelSettings);

        return createColumnRearranger(spec, modelSettings, targetColumns.stream() //
            .collect(Collectors.toMap(Function.identity(), x -> outputType, (l, r) -> l, TreeMap::new)),
            warningMessageBuilder);
    }

    /**
     * Get the columns that are both supported and selected for conversion to duration/period.
     */
    private static List<String> getSupportedTargetColumns(final DataTableSpec spec,
        final StringToDurationPeriodNodeSettings settings) {
        var supportedColumn = ColumnSelectionUtil.getCompatibleColumns(spec, StringValue.class);
        return Arrays.asList(settings.m_columnFilter.filter(supportedColumn));
    }

    private static final class StringToDurationPeriodCellFactory extends SingleCellFactory {

        private final int m_targetColIndex;

        private final String m_targetColNameAbbr;

        private final boolean m_failOnParseError;

        private final MessageBuilder m_warningListener;

        StringToDurationPeriodCellFactory(final DataColumnSpec newColSpec, final InputColumn targetCol,
            final StringToDurationPeriodNodeSettings settings, final MessageBuilder warningListener) {
            super(newColSpec);

            m_targetColIndex = targetCol.index();
            m_targetColNameAbbr = StringUtils.abbreviate(targetCol.spec().getName(), 32);
            m_failOnParseError = settings.m_actionIfExtractionFails == ActionIfExtractionFails.FAIL;
            m_warningListener = warningListener;
        }

        @Override
        public DataCell getCell(final DataRow row, final long rowIndex) {
            final DataCell cell = row.getCell(m_targetColIndex);
            if (cell.isMissing()) {
                return cell;
            }
            final DataColumnSpec newColumnSpec = getColumnSpecs()[0];

            var stringValue = ((StringValue)cell).getStringValue();

            try {
                if (newColumnSpec.getType().equals(DurationCellFactory.TYPE)) {
                    return DurationCellFactory.create(DurationPeriodFormatUtils.parseDuration(stringValue));
                } else if (newColumnSpec.getType().equals(PeriodCellFactory.TYPE)) {
                    return PeriodCellFactory.create(DurationPeriodFormatUtils.parsePeriod(stringValue));
                } else if (newColumnSpec.getType().equals(DataType.getMissingCell().getType())) {
                    return new MissingCell(
                        "Could not auto-detect the type of this column: value of this row was \"%s\"."
                            .formatted(stringValue));
                } else {
                    throw new IllegalStateException(
                        "Implementation error: unexpected column type " + newColumnSpec.getType().getName());
                }
            } catch (DateTimeParseException e) {
                final var stringValueAbbr = StringUtils.abbreviate(stringValue, 32);
                final var rowKeyAbbr = StringUtils.abbreviate(row.getKey().getString(), 16);
                final var warningMessage = String.format(
                    "Could not parse string \"%s\" in cell [%s, column \"%s\", row number %d] to type %s: %s",
                    stringValueAbbr, rowKeyAbbr, m_targetColNameAbbr, rowIndex, newColumnSpec.getType().getName(),
                    e.getMessage());

                m_warningListener.addRowIssue(0, m_targetColIndex, rowIndex, warningMessage);

                if (m_failOnParseError) {
                    m_warningListener.withSummary("Error encountered.");
                    m_warningListener.addResolutions(
                        "Deselect the \"Fail on error\" option to output missing values for non-matching strings.");
                    throw KNIMEException.of(m_warningListener.build().orElseThrow(), e).toUnchecked();
                }

                final var missingCellMessage = String.format("Could not parse string \"%s\" to type %s: %s",
                    stringValueAbbr, newColumnSpec.getType().getName(), e.getMessage());

                return new MissingCell(missingCellMessage);
            }
        }

    }
}
