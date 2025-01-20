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
 *   Dec 10, 2024 (Tobias Kampmann): created
 */
package org.knime.time.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAccessor;

import org.knime.core.data.DataCell;
import org.knime.core.data.time.localdate.LocalDateCell;
import org.knime.core.data.time.localdate.LocalDateCellFactory;
import org.knime.core.data.time.localdatetime.LocalDateTimeCell;
import org.knime.core.data.time.localdatetime.LocalDateTimeCellFactory;
import org.knime.core.data.time.localtime.LocalTimeCell;
import org.knime.core.data.time.localtime.LocalTimeCellFactory;
import org.knime.core.data.time.zoneddatetime.ZonedDateTimeCell;
import org.knime.core.data.time.zoneddatetime.ZonedDateTimeCellFactory;

/**
 *
 * @author Tobias Kampmann
 */
public final class TemporalCellUtils {

    TemporalCellUtils() {
        // utility class
    }

    /**
     * Convenience function to create a new data cell from a Temporal. Supported Temporal types: ZonedDateTime,
     * LocalDateTime, LocalTime, LocalDate
     *
     * @param temporal
     *
     * @return The new data cell
     */
    public static DataCell createTemporalDataCell(final TemporalAccessor temporal) {

        if (temporal instanceof ZonedDateTime zonedDateTime) {
            return ZonedDateTimeCellFactory.create(zonedDateTime);
        } else if (temporal instanceof LocalDateTime localDateTime) {
            return LocalDateTimeCellFactory.create(localDateTime);
        } else if (temporal instanceof LocalTime localTime) {
            return LocalTimeCellFactory.create(localTime);
        } else if (temporal instanceof LocalDate localDate) {
            return LocalDateCellFactory.create(localDate);
        } else {
            throw new IllegalArgumentException("Unsupported Temporal type: " + temporal.getClass());
        }
    }

    /**
     * Convenience function to get a Temporal from a DataCell.
     *
     * @param cell The cell to get the Temporal from. Allowed: LocalTimeValue, LocalDateValue, LocalDateTimeValue,
     *            ZonedDateTime
     * @return The Temporal from the cell
     */
    public static Temporal getTemporalFromCell(final DataCell cell) {

        if (cell instanceof ZonedDateTimeCell zonedDateTimeCell) {
            return zonedDateTimeCell.getZonedDateTime();
        } else if (cell instanceof LocalDateTimeCell localDateTimeCell) {
            return localDateTimeCell.getLocalDateTime();
        } else if (cell instanceof LocalTimeCell localTimeCell) {
            return localTimeCell.getLocalTime();
        } else if (cell instanceof LocalDateCell localDateCell) {
            return localDateCell.getLocalDate();
        } else {
            throw new IllegalArgumentException("Unsupported Temporal type: " + cell.getClass());
        }
    }
}
