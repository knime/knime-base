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
import java.time.temporal.TemporalAccessor;
import java.util.Locale;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.MissingCell;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.container.SingleCellFactory;
import org.knime.core.data.def.StringCell.StringCellFactory;
import org.knime.core.data.time.localdate.LocalDateValue;
import org.knime.core.data.time.localdatetime.LocalDateTimeValue;
import org.knime.core.data.time.localtime.LocalTimeValue;
import org.knime.core.data.time.zoneddatetime.ZonedDateTimeValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.util.UniqueNameGenerator;
import org.knime.core.webui.node.dialog.defaultdialog.history.DateTimeFormatStringHistoryManager;
import org.knime.core.webui.node.impl.WebUINodeConfiguration;
import org.knime.core.webui.node.impl.WebUISimpleStreamableFunctionNodeModel;
import org.knime.time.util.ReplaceOrAppend;

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

        var rearranger = new ColumnRearranger(spec);

        var supportedColumns = spec.stream() //
            .filter(DateTimeToStringNodeSettings.DateAndTimeColumnProvider.IS_COMPATIBLE_COLUMN) //
            .map(DataColumnSpec::getName) //
            .toArray(String[]::new);

        var targetColumnNames = modelSettings.m_columnFilter.getSelected(supportedColumns, spec);
        var targetColumnIndices = spec.columnsToIndices(targetColumnNames);

        if (modelSettings.m_appendOrReplace == ReplaceOrAppend.APPEND) {
            var uniqueNameGenerator = new UniqueNameGenerator(spec);

            for (var i = 0; i < targetColumnNames.length; ++i) {
                var newName = uniqueNameGenerator.newName(targetColumnNames[i] + modelSettings.m_outputColumnSuffix);
                var newSpec = new DataColumnSpecCreator(newName, StringCellFactory.TYPE).createSpec();

                rearranger.append(new DateTimeToStringCellFactory(newSpec, modelSettings, targetColumnIndices[i]));
            }
        } else {
            for (var i = 0; i < targetColumnNames.length; ++i) {
                var newSpec = new DataColumnSpecCreator(targetColumnNames[i], StringCellFactory.TYPE).createSpec();

                rearranger.replace(new DateTimeToStringCellFactory(newSpec, modelSettings, targetColumnIndices[i]),
                    targetColumnIndices[i]);
            }
        }

        return rearranger;
    }

    static final class DateTimeToStringCellFactory extends SingleCellFactory {

        private final DateTimeFormatter m_formatter;

        private final int m_targetIndex;

        DateTimeToStringCellFactory(final DataColumnSpec newColSpec, final DateTimeToStringNodeSettings settings,
            final int targetIndex) {
            super(newColSpec);

            var locale = Locale.forLanguageTag(settings.m_locale);
            m_formatter = DateTimeFormatter.ofPattern(settings.m_format, locale) //
                .withChronology((Chronology.ofLocale(locale)));
            m_targetIndex = targetIndex;
        }

        @Override
        public DataCell getCell(final DataRow row) {
            var cell = row.getCell(m_targetIndex);

            if (cell.isMissing()) {
                return cell;
            }

            var temporalValue = extractTemporalFromDataCell(row.getCell(m_targetIndex));

            try {
                return StringCellFactory.create( //
                    m_formatter.format(temporalValue) //
                );
            } catch (DateTimeException e) { // NOSONAR
                return new MissingCell(e.getMessage());
            }
        }

        private static TemporalAccessor extractTemporalFromDataCell(final DataCell cell) {
            if (cell instanceof LocalDateValue ld) {
                return ld.getLocalDate();
            } else if (cell instanceof LocalTimeValue lt) {
                return lt.getLocalTime();
            } else if (cell instanceof LocalDateTimeValue ldt) {
                return ldt.getLocalDateTime();
            } else if (cell instanceof ZonedDateTimeValue zdt) {
                return zdt.getZonedDateTime();
            } else {
                throw new IllegalArgumentException("Unsupported data cell type: " + cell.getClass().getName());
            }
        }
    }

}
