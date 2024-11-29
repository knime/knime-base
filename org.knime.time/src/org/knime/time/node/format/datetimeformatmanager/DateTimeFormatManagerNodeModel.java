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
 *   Dez 6, 2024 (tobias): created
 */
package org.knime.time.node.format.datetimeformatmanager;

import java.time.format.DateTimeFormatter;
import java.util.Collection;

import org.apache.commons.lang3.LocaleUtils;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataTableSpecCreator;
import org.knime.core.data.DataValue;
import org.knime.core.data.property.ValueFormatHandler;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.webui.node.dialog.defaultdialog.history.DateTimeFormatStringHistoryManager;
import org.knime.core.webui.node.impl.WebUINodeConfiguration;
import org.knime.core.webui.node.impl.WebUINodeModel;

/**
 * The node model of the node which attaches a date&time formatter to columns.
 *
 * @author Tobias Kampmann, TNG Technology Consulting GmbH, Germany
 */
@SuppressWarnings("restriction")
final class DateTimeFormatManagerNodeModel extends WebUINodeModel<DateTimeFormatManagerNodeSettings> {

    /**
     * @param configuration
     */
    protected DateTimeFormatManagerNodeModel(final WebUINodeConfiguration configuration) {
        super(configuration, DateTimeFormatManagerNodeSettings.class);
    }

    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs,
        final DateTimeFormatManagerNodeSettings modelSettings) throws InvalidSettingsException {

        return new DataTableSpec[]{attachFormatterToSelectedColumns(inSpecs[0], modelSettings)};
    }

    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec,
        final DateTimeFormatManagerNodeSettings modelSettings) throws Exception {

        var inputSpec = inData[0].getDataTableSpec();
        var resultSpec = attachFormatterToSelectedColumns(inputSpec, modelSettings);

        if (inputSpec.equals(resultSpec)) {
            return new BufferedDataTable[]{inData[0]};
        }

        return new BufferedDataTable[]{exec.createSpecReplacerTable(inData[0], resultSpec)};
    }

    @Override
    protected void validateSettings(final DateTimeFormatManagerNodeSettings settings) throws InvalidSettingsException {
        if (settings.m_format == null || settings.m_format.isBlank()) {
            throw new InvalidSettingsException("Date&Time format must not be empty.");
        }

        try {
            DateTimeFormatter.ofPattern(settings.m_format);
        } catch (IllegalArgumentException e) { // NOSONAR
            throw new InvalidSettingsException(
                "Invalid date&time format '%s'. Reason: %s.".formatted(settings.m_format, e.getMessage()));
        }
    }

    private static DataTableSpec attachFormatterToSelectedColumns(final DataTableSpec spec,
        final DateTimeFormatManagerNodeSettings settings) throws InvalidSettingsException {

        DateTimeFormatStringHistoryManager.addFormatToStringHistoryIfNotPresent(settings.m_format);

        var handler = new ValueFormatHandler(new DateTimeDataValueFormatter( //
            settings.m_format, //
            LocaleUtils.toLocale(settings.m_locale), settings.m_alignmentSuggestion) //
        );

        final var tableSpecCreator = new DataTableSpecCreator(spec);
        final String[] targetColumns =
            getCompatibleColumns(spec, DateTimeFormatManagerNodeSettings.DATE_TIME_COLUMN_TYPES);

        for (var columnName : targetColumns) {

            final var columnSpec = spec.getColumnSpec(columnName);
            if (columnSpec == null || !isDateTimeColumn(columnSpec)) {
                continue; // skip columns that do not exist anymore
            }
            final var columnSpecCreator = new DataColumnSpecCreator(columnSpec);
            columnSpecCreator.setValueFormatHandler(handler);
            final var outputColumnSpec = columnSpecCreator.createSpec();
            tableSpecCreator.replaceColumn(spec.findColumnIndex(columnName), outputColumnSpec);
        }
        return tableSpecCreator.createSpec();
    }

    static String[] getCompatibleColumns(final DataTableSpec spec,
        final Collection<Class<? extends DataValue>> valueClasses) {
        return spec.stream().filter(s -> valueClasses.stream().anyMatch(s.getType()::isCompatible))
            .map(DataColumnSpec::getName).toArray(String[]::new);
    }

    static boolean isDateTimeColumn(final DataColumnSpec columnSpec) {
        return DateTimeFormatManagerNodeSettings.DATE_TIME_COLUMN_TYPES.stream()
            .anyMatch(columnSpec.getType()::isCompatible);
    }

}
