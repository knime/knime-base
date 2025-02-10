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
package org.knime.time.node.convert.stringtodatetime;

import java.time.chrono.Chronology;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAccessor;
import java.util.Locale;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.MissingCell;
import org.knime.core.data.StringValue;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.container.SingleCellFactory;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.KNIMEException;
import org.knime.core.node.message.MessageBuilder;
import org.knime.core.util.UniqueNameGenerator;
import org.knime.core.webui.node.dialog.defaultdialog.history.DateTimeFormatStringHistoryManager;
import org.knime.core.webui.node.impl.WebUINodeConfiguration;
import org.knime.core.webui.node.impl.WebUISimpleStreamableFunctionNodeModel;
import org.knime.time.util.ActionIfExtractionFails;
import org.knime.time.util.DateTimeType;
import org.knime.time.util.ReplaceOrAppend;
import org.knime.time.util.TemporalCellUtils;

/**
 * New node model for the "DateTimeToString" node.
 *
 * @author David Hickey, TNG Technology Consulting GmbH
 */
@SuppressWarnings("restriction")
final class StringToDateTimeNodeModel2 extends WebUISimpleStreamableFunctionNodeModel<StringToDateTimeNodeSettings> {

    /**
     * @param configuration
     */
    protected StringToDateTimeNodeModel2(final WebUINodeConfiguration configuration) {
        super(configuration, StringToDateTimeNodeSettings.class);
    }

    @Override
    protected void validateSettings(final StringToDateTimeNodeSettings settings) throws InvalidSettingsException {
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
        final StringToDateTimeNodeSettings modelSettings) throws InvalidSettingsException {

        DateTimeFormatStringHistoryManager.addFormatToStringHistoryIfNotPresent(modelSettings.m_format);

        var rearranger = new ColumnRearranger(spec);

        var supportedColumns = spec.stream() //
            .filter(colSpec -> colSpec.getType().isCompatible(StringValue.class)) //
            .map(DataColumnSpec::getName) //
            .toArray(String[]::new);

        var targetColumnNames = modelSettings.m_columnFilter.getSelected(supportedColumns, spec);
        var targetColumnIndices = spec.columnsToIndices(targetColumnNames);

        var warnings = createMessageBuilder();

        if (modelSettings.m_appendOrReplace == ReplaceOrAppend.APPEND) {
            var uniqueNameGenerator = new UniqueNameGenerator(spec);

            for (var i = 0; i < targetColumnNames.length; ++i) {
                var newName = uniqueNameGenerator.newName(targetColumnNames[i] + modelSettings.m_outputColumnSuffix);
                var newSpec =
                    new DataColumnSpecCreator(newName, modelSettings.m_selectedType.getDataType()).createSpec();

                rearranger
                    .append(new StringToDateTimeCellFactory(newSpec, modelSettings, targetColumnIndices[i], warnings));
            }
        } else {
            for (var i = 0; i < targetColumnNames.length; ++i) {
                var newSpec =
                    new DataColumnSpecCreator(targetColumnNames[i], modelSettings.m_selectedType.getDataType())
                        .createSpec();

                rearranger.replace(
                    new StringToDateTimeCellFactory(newSpec, modelSettings, targetColumnIndices[i], warnings),
                    targetColumnIndices[i]);
            }
        }

        return rearranger;
    }

    final class StringToDateTimeCellFactory extends SingleCellFactory {

        private final DateTimeFormatter m_parser;

        private final String m_pattern;

        private final DateTimeType m_targetType;

        private final int m_targetIndex;

        private final boolean m_failOnParseError;

        private final MessageBuilder m_warningListener;

        StringToDateTimeCellFactory(final DataColumnSpec newColSpec, final StringToDateTimeNodeSettings settings,
            final int targetIndex, final MessageBuilder warninglistener) {
            super(newColSpec);

            var locale = Locale.forLanguageTag(settings.m_locale);
            m_parser = DateTimeFormatter.ofPattern(settings.m_format, locale) //
                .withChronology((Chronology.ofLocale(locale)));
            m_pattern = settings.m_format;
            m_targetType = settings.m_selectedType;
            m_targetIndex = targetIndex;

            m_failOnParseError = settings.m_onError == ActionIfExtractionFails.FAIL;

            m_warningListener = warninglistener;
        }

        @Override
        public DataCell getCell(final DataRow row, final long rowIndex) {
            var cell = row.getCell(m_targetIndex);

            if (cell.isMissing()) {
                return cell;
            }

            var stringValue = ((StringValue)row.getCell(m_targetIndex)).getStringValue();

            try {
                return TemporalCellUtils.createTemporalDataCell( //
                    parseString(stringValue, m_parser, m_targetType) //
                );
            } catch (DateTimeParseException ex) { // NOSONAR
                var message = "Failed to parse string '%s' with format '%s' at row %d" //
                    .formatted(stringValue, m_pattern, rowIndex);

                if (m_failOnParseError) {
                    throw new KNIMEException(message, ex).toUnchecked();
                }

                // emit warning and return missing cell
                m_warningListener.addRowIssue(0, m_targetIndex, rowIndex, message);
                return new MissingCell(message);
            }
        }

        private static TemporalAccessor parseString(final String str, final DateTimeFormatter formatter,
            final DateTimeType targetType) {

            return formatter.parse(str, targetType.getQuery());
        }

        @Override
        public void afterProcessing() {
            if (m_warningListener.getIssueCount() > 0) {
                m_warningListener //
                    .withSummary("%s warning%s encountered.".formatted(m_warningListener.getIssueCount(),
                        m_warningListener.getIssueCount() == 1 ? "" : "s")) //
                    .build() //
                    .ifPresent(StringToDateTimeNodeModel2.this::setWarning);
            }
        }
    }
}
