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
 *   11 Mar 2026 (Copilot): created
 */
package org.knime.time.node.filter.rowfilter;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataType;
import org.knime.core.data.time.localdate.LocalDateCell;
import org.knime.core.data.time.localdate.LocalDateCellFactory;
import org.knime.core.data.time.localtime.LocalTimeCell;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.webui.node.dialog.defaultdialog.setting.interval.DateInterval;
import org.knime.time.node.filter.rowfilter.DateTimeRangeFilterParameters.EndMode;
import org.knime.time.node.filter.rowfilter.LocalDateRangeFilterParameters.DateGranularity;

@SuppressWarnings({"restriction", "static-method"})
final class DateTimeRangeOperatorTest {

    @Test
    void testInclusiveDateTimeRange() throws InvalidSettingsException {
        final var params = createDateTimeRangeParams(LocalDate.of(2022, 1, 1), LocalDate.of(2022, 12, 31));
        final var operator = createOperator();
        final var spec = createDateSpec();
        final var predicate = operator.createPredicate(spec, DataType.getType(LocalDateCell.class), params);

        assertTrue(predicate.test(LocalDateCellFactory.create(LocalDate.of(2022, 1, 1))));
        assertTrue(predicate.test(LocalDateCellFactory.create(LocalDate.of(2022, 6, 15))));
        assertTrue(predicate.test(LocalDateCellFactory.create(LocalDate.of(2022, 12, 31))));
        assertFalse(predicate.test(LocalDateCellFactory.create(LocalDate.of(2021, 12, 31))));
        assertFalse(predicate.test(LocalDateCellFactory.create(LocalDate.of(2023, 1, 1))));
    }

    @Test
    void testExclusiveBounds() throws InvalidSettingsException {
        final var params = createDateTimeRangeParams(LocalDate.of(2022, 1, 1), LocalDate.of(2022, 12, 31));
        params.m_startInclusive = false;
        params.m_endInclusive = false;
        final var operator = createOperator();
        final var spec = createDateSpec();
        final var predicate = operator.createPredicate(spec, DataType.getType(LocalDateCell.class), params);

        assertFalse(predicate.test(LocalDateCellFactory.create(LocalDate.of(2022, 1, 1))));
        assertTrue(predicate.test(LocalDateCellFactory.create(LocalDate.of(2022, 6, 15))));
        assertFalse(predicate.test(LocalDateCellFactory.create(LocalDate.of(2022, 12, 31))));
    }

    @Test
    void testDurationMode() throws InvalidSettingsException {
        final var params = createDateTimeRangeParams(LocalDate.of(2022, 1, 1), LocalDate.of(2022, 1, 1));
        params.m_endMode = EndMode.DURATION;
        params.m_durationValue = DateInterval.of(0, 0, 0, 30);
        final var operator = createOperator();
        final var spec = createDateSpec();
        final var predicate = operator.createPredicate(spec, DataType.getType(LocalDateCell.class), params);

        assertTrue(predicate.test(LocalDateCellFactory.create(LocalDate.of(2022, 1, 1))));
        assertTrue(predicate.test(LocalDateCellFactory.create(LocalDate.of(2022, 1, 31))));
        assertFalse(predicate.test(LocalDateCellFactory.create(LocalDate.of(2022, 2, 1))));
    }

    @Test
    void testNumericalMode() throws InvalidSettingsException {
        final var params = createDateTimeRangeParams(LocalDate.of(2022, 1, 1), LocalDate.of(2022, 1, 1));
        params.m_endMode = EndMode.NUMERICAL;
        params.m_numericalValue = 1;
        params.m_granularity = DateGranularity.YEAR;
        final var operator = createOperator();
        final var spec = createDateSpec();
        final var predicate = operator.createPredicate(spec, DataType.getType(LocalDateCell.class), params);

        assertTrue(predicate.test(LocalDateCellFactory.create(LocalDate.of(2022, 1, 1))));
        assertTrue(predicate.test(LocalDateCellFactory.create(LocalDate.of(2022, 6, 15))));
        assertTrue(predicate.test(LocalDateCellFactory.create(LocalDate.of(2023, 1, 1))));
        assertFalse(predicate.test(LocalDateCellFactory.create(LocalDate.of(2023, 1, 2))));
    }

    @Test
    void testValidateRejectsEndBeforeStart() {
        final var params = createDateTimeRangeParams(LocalDate.of(2022, 12, 31), LocalDate.of(2022, 1, 1));
        params.m_endMode = EndMode.DATE_TIME;

        assertThrows(InvalidSettingsException.class, params::validate);
    }

    @Test
    void testValidateAcceptsValidRange() {
        final var params = createDateTimeRangeParams(LocalDate.of(2022, 1, 1), LocalDate.of(2022, 12, 31));
        params.m_endMode = EndMode.DATE_TIME;

        assertDoesNotThrow(params::validate);
    }

    @Test
    void testTypeMismatchThrows() {
        final var params = createDateTimeRangeParams(LocalDate.of(2022, 1, 1), LocalDate.of(2022, 12, 31));
        final var operator = createOperator();
        final var incompatibleSpec =
            new DataColumnSpecCreator("col", DataType.getType(LocalTimeCell.class)).createSpec();

        assertThrows(InvalidSettingsException.class,
            () -> operator.createPredicate(incompatibleSpec, DataType.getType(LocalDateCell.class), params));
    }

    private static LocalDateRangeFilterParameters createDateTimeRangeParams(final LocalDate start,
        final LocalDate end) {
        final var params = new LocalDateRangeFilterParameters();
        params.m_startValue = start;
        params.m_endValue = end;
        params.m_useExecutionTimeStart = false;
        params.m_useExecutionTimeEnd = false;
        params.m_startInclusive = true;
        params.m_endInclusive = true;
        params.m_endMode = EndMode.DATE_TIME;
        return params;
    }

    private static DateTimeRangeOperator<LocalDateRangeFilterParameters> createOperator() {
        return new DateTimeRangeOperator<>(DataType.getType(LocalDateCell.class), LocalDateRangeFilterParameters.class);
    }

    private static DataColumnSpec createDateSpec() {
        return new DataColumnSpecCreator("col", DataType.getType(LocalDateCell.class)).createSpec();
    }
}
