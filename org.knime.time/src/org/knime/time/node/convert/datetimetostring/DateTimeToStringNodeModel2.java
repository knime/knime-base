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
 *   Dec 18, 2024 (david): created
 */
package org.knime.time.node.convert.datetimetostring;

import java.time.DateTimeException;
import java.time.chrono.Chronology;
import java.time.format.DateTimeFormatter;
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
import org.knime.core.data.def.StringCell.StringCellFactory;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.message.MessageBuilder;
import org.knime.core.webui.node.dialog.defaultdialog.history.DateTimeFormatStringHistoryManager;
import org.knime.core.webui.node.impl.WebUINodeConfiguration;
import org.knime.core.webui.node.impl.WebUISimpleStreamableFunctionNodeModel;
import org.knime.time.util.DateTimeUtils;
import org.knime.time.util.TemporalCellUtils;

/**
 * New node model for the "DateTimeToString" node.
 *
 * @author David Hickey, TNG Technology Consulting GmbH
 */
final class DateTimeToStringNodeModel2 extends WebUISimpleStreamableFunctionNodeModel<DateTimeToStringNodeSettings> {

    /**
     * @param configuration
     * @param modelSettingsClass
     */
    protected DateTimeToStringNodeModel2(final WebUINodeConfiguration configuration) {
        super(configuration, DateTimeToStringNodeSettings.class);
    }

    @Override
    protected void validateSettings(final DateTimeToStringNodeSettings settings) throws InvalidSettingsException {
        if (settings.m_format == null || settings.m_format.isBlank()) {
            throw new InvalidSettingsException("Date&Time format must not be empty.");
        }

        try {
            DateTimeFormatter.ofPattern(settings.m_format);
        } catch (IllegalArgumentException ex) {
            throw new InvalidSettingsException(
                "Invalid date time format '%s'. Reason: %s".formatted(settings.m_format, ex.getMessage()), ex);
        }
    }

    @Override
    protected ColumnRearranger createColumnRearranger(final DataTableSpec spec,
        final DateTimeToStringNodeSettings modelSettings) throws InvalidSettingsException {

        DateTimeFormatStringHistoryManager.addFormatToStringHistoryIfNotPresent(modelSettings.m_format);

        var supportedColumns = DateTimeUtils.getCompatibleColumns(spec, DateTimeUtils.DATE_TIME_COLUMN_TYPES);
        var targetColumnNames = modelSettings.m_columnFilter.getSelected(supportedColumns, spec);

        final var messageBuilder = createMessageBuilder();

        return modelSettings.m_appendOrReplace.createRearranger(targetColumnNames, spec, (inputColumn, newName) -> {
            var newSpec = new DataColumnSpecCreator(newName, StringCellFactory.TYPE).createSpec();
            return new DateTimeToStringCellFactory(newSpec, modelSettings, inputColumn.index(), messageBuilder);
        }, modelSettings.m_outputColumnSuffix, () -> {
            if (messageBuilder.getIssueCount() > 0) {
                messageBuilder //
                    .withSummary("%s warning%s encountered.".formatted(messageBuilder.getIssueCount(),
                        messageBuilder.getIssueCount() == 1 ? "" : "s")) //
                    .addResolutions("Change the date and time pattern to match all provided dates.") //
                    .build() //
                    .ifPresent(this::setWarning);
            }
        });

    }

    static final class DateTimeToStringCellFactory extends SingleCellFactory {

        private final DateTimeFormatter m_formatter;

        private final String m_formatAbbr;

        private final int m_targetIndex;

        private final String m_targetNameAbbr;

        private final MessageBuilder m_messageBuilder;

        DateTimeToStringCellFactory(final DataColumnSpec newColSpec, final DateTimeToStringNodeSettings settings,
            final int targetIndex, final MessageBuilder messageBuilder) {
            super(newColSpec);

            var locale = Locale.forLanguageTag(settings.m_locale);
            m_formatter = DateTimeFormatter.ofPattern(settings.m_format, locale) //
                .withChronology((Chronology.ofLocale(locale)));
            m_targetIndex = targetIndex;
            m_formatAbbr = StringUtils.abbreviate(settings.m_format, 32);
            m_targetNameAbbr = StringUtils.abbreviate(newColSpec.getName(), 32);
            m_messageBuilder = messageBuilder;
        }

        @Override
        public DataCell getCell(final DataRow row, final long rowIndex) {
            var cell = row.getCell(m_targetIndex);

            if (cell.isMissing()) {
                return cell;
            }

            var temporalValue = TemporalCellUtils.getTemporalFromCell(row.getCell(m_targetIndex));

            try {
                return StringCellFactory.create( //
                    m_formatter.format(temporalValue) //
                );
            } catch (DateTimeException e) { // NOSONAR
                final var reason = String.format("Pattern \"%s\" does not match \"%s\".", m_formatAbbr,
                    StringUtils.abbreviate(temporalValue.toString(), 32));

                final var warningMessage =
                    String.format("Could not parse date in cell [%s, column \"%s\", row number %d]: %s",
                        StringUtils.abbreviate(row.getKey().getString(), 16), m_targetNameAbbr, rowIndex, reason);

                m_messageBuilder.addRowIssue(0, m_targetIndex, rowIndex, warningMessage);

                final var missingCellMessage = String.format("Could not parse date: %s", reason);
                return new MissingCell(missingCellMessage);
            }
        }

    }

}
