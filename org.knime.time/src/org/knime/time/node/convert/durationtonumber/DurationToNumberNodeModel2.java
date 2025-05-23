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
 *   Nov 18, 2024 (david): created
 */
package org.knime.time.node.convert.durationtonumber;

import java.util.Locale;

import org.apache.commons.lang3.StringUtils;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.MissingCell;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.container.SingleCellFactory;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.LongCell;
import org.knime.core.data.time.duration.DurationValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.message.MessageBuilder;
import org.knime.core.webui.node.dialog.defaultdialog.util.column.ColumnSelectionUtil;
import org.knime.core.webui.node.impl.WebUINodeConfiguration;
import org.knime.core.webui.node.impl.WebUISimpleStreamableFunctionNodeModel;
import org.knime.time.node.convert.durationtonumber.DurationToNumberNodeSettings.RoundingBehaviour;
import org.knime.time.util.ReplaceOrAppend.InputColumn;
import org.knime.time.util.TimeBasedGranularityUnit;

/**
 * New node model for the node that converts durations to numbers.
 *
 * @author David Hickey, TNG Technology Consulting GmbH
 */
@SuppressWarnings("restriction")
final class DurationToNumberNodeModel2 extends WebUISimpleStreamableFunctionNodeModel<DurationToNumberNodeSettings> {

    /**
     * @param configuration
     */
    public DurationToNumberNodeModel2(final WebUINodeConfiguration configuration) {
        super(configuration, DurationToNumberNodeSettings.class);
    }

    /**
     * Get the column names that are to be processed during node execution.
     *
     * @param inputSpec
     * @param settings
     * @return
     */
    private static String[] getInputColumnNames(final DataTableSpec inputSpec,
        final DurationToNumberNodeSettings settings) {
        var compatibleColumns = ColumnSelectionUtil.getCompatibleColumns(inputSpec, DurationValue.class);
        return settings.m_filter.filter(compatibleColumns);
    }

    @Override
    protected ColumnRearranger createColumnRearranger(final DataTableSpec inputSpec,
        final DurationToNumberNodeSettings modelSettings) throws InvalidSettingsException {

        final var messageBuilder = createMessageBuilder();

        var inputColumnNames = getInputColumnNames(inputSpec, modelSettings);
        return modelSettings.m_appendOrReplaceColumn.createRearranger(inputColumnNames, inputSpec,
            (inputColumn, newColumnName) -> new DurationToNumberCellFactory( //
                inputColumn, //
                newColumnName, //
                modelSettings, //
                messageBuilder //
            ), modelSettings.m_suffix, () -> {
                if (messageBuilder.getIssueCount() > 0) {
                    messageBuilder //
                        .withSummary("%s warning%s encountered.".formatted(messageBuilder.getIssueCount(),
                            messageBuilder.getIssueCount() == 1 ? "" : "s")) //
                        .build() //
                        .ifPresent(this::setWarning);
                }
            });
    }

    static final class DurationToNumberCellFactory extends SingleCellFactory {

        private final RoundingBehaviour m_roundingBehaviour;

        private final TimeBasedGranularityUnit m_unit;

        private final String m_unitNameAbbr;

        private final int m_inputColumnIndex;

        private final String m_inputColumnNameAbbr;

        private final MessageBuilder m_messageBuilder;

        DurationToNumberCellFactory( //
            final InputColumn inputColumn, //
            final String newColumnName, //
            final DurationToNumberNodeSettings settings, //
            final MessageBuilder messageBuilder //
        ) {
            super(createNewColumnSpec(newColumnName, settings.m_roundingBehaviour));
            m_inputColumnIndex = inputColumn.index();
            m_inputColumnNameAbbr = StringUtils.abbreviate(inputColumn.spec().getName(), 32);
            m_roundingBehaviour = settings.m_roundingBehaviour;
            m_unit = settings.m_unit;
            m_messageBuilder = messageBuilder;
            m_unitNameAbbr = settings.m_unit.name().toLowerCase(Locale.getDefault());
        }

        @Override
        public DataCell getCell(final DataRow row, final long rowIndex) {
            var cell = row.getCell(m_inputColumnIndex);
            if (cell.isMissing()) {
                return cell;
            }

            var durationCell = (DurationValue)cell;
            final var duration = durationCell.getDuration();

            try {
                return switch (m_roundingBehaviour) {
                    case DOUBLE -> new DoubleCell(m_unit.getConversionExact(duration));
                    case INTEGER -> new LongCell(m_unit.getConversionFloored(duration));
                };
            } catch (ArithmeticException e) { // NOSONAR
                final var durationAbbr = StringUtils.abbreviate(duration.toString(), 32);
                final var rowKeyAbbr = StringUtils.abbreviate(row.getKey().getString(), 16);

                final var warningMessage = String.format(
                    "Could not convert duration \"%s\" in cell [%s, column \"%s\", row number %d] to %s: %s",
                    durationAbbr, rowKeyAbbr, m_inputColumnNameAbbr, rowIndex, m_unitNameAbbr, e.getMessage());

                m_messageBuilder.addRowIssue(0, m_inputColumnIndex, rowIndex, warningMessage);

                final var missingCellMessage = String.format("Could not convert duration \"%s\" to %s: %s",
                    durationAbbr, m_unitNameAbbr, e.getMessage());
                return new MissingCell(missingCellMessage);
            }
        }

        private static DataColumnSpec createNewColumnSpec(final String newColumnName,
            final RoundingBehaviour roundingBehaviour) {

            return new DataColumnSpecCreator( //
                newColumnName, roundingBehaviour == RoundingBehaviour.DOUBLE ? DoubleCell.TYPE : LongCell.TYPE //
            ).createSpec();
        }

    }
}
