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
 *   Mar 28, 2025 (Paul Baernreuther): created
 */
package org.knime.time.node.convert.timestamptodatetime;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.LongValue;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.container.SingleCellFactory;
import org.knime.core.data.time.localdate.LocalDateCellFactory;
import org.knime.core.data.time.localdatetime.LocalDateTimeCellFactory;
import org.knime.core.data.time.localtime.LocalTimeCellFactory;
import org.knime.core.data.time.zoneddatetime.ZonedDateTimeCellFactory;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.webui.node.impl.WebUINodeConfiguration;
import org.knime.core.webui.node.impl.WebUISimpleStreamableFunctionNodeModel;

/**
 * The node model of the node which converts unix timestamps to the new date&time types.
 *
 * @author Paul Baernreuther
 */
@SuppressWarnings("restriction")
final class TimestampToDateTimeNodeModel2
    extends WebUISimpleStreamableFunctionNodeModel<TimestampToDateTimeNodeSettings> {

    TimestampToDateTimeNodeModel2(final WebUINodeConfiguration configuration) {
        super(configuration, TimestampToDateTimeNodeSettings.class);
    }

    @Override
    protected ColumnRearranger createColumnRearranger(final DataTableSpec inSpec,
        final TimestampToDateTimeNodeSettings settings) throws InvalidSettingsException {
        final var timestampColumns = inSpec.stream().filter(TimestampToDateTimeNodeSettings::isTimestampColumn)
            .map(DataColumnSpec::getName).toArray(String[]::new);
        final String[] includeList = settings.m_timestampColumns.getSelected(timestampColumns, inSpec);
        return settings.m_appendOrReplace.createRearranger(includeList, inSpec, (col, newName) -> {
            final var dataColumnSpec =
                new DataColumnSpecCreator(newName, settings.m_outputType.getDataType()).createSpec();
            return new TimestampToTimeCellFactory(dataColumnSpec, col.index(), settings);
        }, settings.m_outputColumnSuffix, () -> {
        });
    }

    /**
     * This cell factory converts a single Int or Long cell to a Date&Time cell.
     */
    static final class TimestampToTimeCellFactory extends SingleCellFactory {
        private final int m_colIndex;

        private final TimestampToDateTimeNodeSettings m_settings;

        /**
         * @param inSpec spec of the column after computation
         * @param colIndex index of the column to work on
         */
        TimestampToTimeCellFactory(final DataColumnSpec inSpec, final int colIndex,
            final TimestampToDateTimeNodeSettings settings) {
            super(inSpec);
            m_colIndex = colIndex;
            m_settings = settings;
        }

        @Override
        public DataCell getCell(final DataRow row) {
            final DataCell cell = row.getCell(m_colIndex);
            if (cell.isMissing()) {
                return cell;
            }
            final long input;
            //Interpret as LongValue (also works for Int columns)
            input = ((LongValue)cell).getLongValue();

            final var instant = switch (m_settings.m_granularity) {
                case SECONDS -> Instant.ofEpochSecond(input);
                case MILLISECONDS -> Instant.ofEpochSecond(input / 1000L, (input % 1000L) * (1000L * 1000L));
                case MICROSECONDS -> Instant.ofEpochSecond(input / (1000L * 1000L), (input % (1000L * 1000L)) * 1000L);
                case NANOSECONDS -> Instant.ofEpochSecond(input / (1000L * 1000L * 1000L),
                    input % (1000L * 1000L * 1000L));

            };
            return switch (m_settings.m_outputType) {
                case LOCAL_DATE -> LocalDateCellFactory.create(LocalDate.from(instant.atZone(ZoneId.of("UTC"))));
                case LOCAL_TIME -> LocalTimeCellFactory.create(LocalTime.from(instant.atZone(ZoneId.of("UTC"))));
                case LOCAL_DATE_TIME -> LocalDateTimeCellFactory
                    .create(LocalDateTime.from(instant.atZone(ZoneId.of("UTC"))));
                case ZONED_DATE_TIME -> ZonedDateTimeCellFactory
                    .create(ZonedDateTime.from(instant.atZone(m_settings.m_timeZone)));

            };

        }
    }

}
