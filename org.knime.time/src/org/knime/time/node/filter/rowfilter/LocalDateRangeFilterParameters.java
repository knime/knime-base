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
 *   5 Mar 2026 (Paul Bärnreuther): created
 */
package org.knime.time.node.filter.rowfilter;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAmount;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValue;
import org.knime.core.data.time.localdate.LocalDateCell;
import org.knime.core.data.time.localdate.LocalDateCellFactory;
import org.knime.core.data.time.localdate.LocalDateValue;
import org.knime.core.webui.node.dialog.defaultdialog.setting.interval.DateInterval;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Modification;
import org.knime.node.parameters.updates.ParameterReference;
import org.knime.time.node.filter.rowfilter.DateTimeRangeFilterParameters.TemporalGranularity;
import org.knime.time.util.Granularity;

/**
 * Range filter parameters for {@link LocalDate} columns.
 *
 * @author Paul Bärnreuther
 */
@SuppressWarnings("restriction")
@Modification({LocalDateRangeFilterParameters.LocalDateRangeModifier.class,
    LocalDateRangeFilterParameters.LocalDateEndValueValidationModifier.class})
public class LocalDateRangeFilterParameters
    extends DateTimeRangeFilterParameters<LocalDate, LocalDateRangeFilterParameters.DateGranularity, DateInterval> {

    /**
     * Granularity options for date-only range filtering.
     */
    enum DateGranularity implements TemporalGranularity {
            /** Year. */
            YEAR(Granularity.YEAR),
            /** Month. */
            MONTH(Granularity.MONTH),
            /** Week. */
            WEEK(Granularity.WEEK),
            /** Day. */
            DAY(Granularity.DAY);

        private final Granularity m_granularity;

        DateGranularity(final Granularity granularity) {
            m_granularity = granularity;
        }

        @Override
        public TemporalAmount getPeriodOrDuration(final long value) {
            return m_granularity.getPeriodOrDuration(value);
        }
    }

    LocalDateRangeFilterParameters() {
        m_startValue = LocalDate.now();
        m_endValue = LocalDate.now();
        m_granularity = DateGranularity.YEAR;
        m_durationValue = DateInterval.of(0, 0, 0, 1);
    }

    @Override
    LocalDate extractFromValueOrNull(final DataValue value) {
        if (value instanceof LocalDateValue v) {
            return v.getLocalDate();
        }
        return null;
    }

    @Override
    LocalDate extractFromExecutionTime(final ZonedDateTime executionTime) {
        return executionTime.toLocalDate();
    }

    @Override
    DataCell createCell(final LocalDate value) {
        return LocalDateCellFactory.create(value);
    }

    @Override
    DataType getCellDataType() {
        return DataType.getType(LocalDateCell.class);
    }

    static final class LocalDateRangeModifier extends UseExecutionTimeTitleAndDescriptionModifier {

        @Override
        String getTemporalTypeName() {
            return "date";
        }
    }

    @Override
    protected int compareStartAndEnd(final LocalDate startValue, final LocalDate endValue) {
        return startValue.compareTo(endValue);
    }

    static final class LocalDateEndValueValidationModifier extends EndValueValidationModifier<LocalDate> {

        interface StartValueRef extends ParameterReference<LocalDate> {
        }

        @Override
        protected Class<? extends ParameterReference<LocalDate>> getStartValueRef() {
            return StartValueRef.class;
        }

        @Override
        protected Class<? extends EndValueValidation<LocalDate>> getValidationClass() {
            return LocalDateEndValueValidation.class;
        }

        static final class LocalDateEndValueValidation
            extends EndValueValidationModifier.EndValueValidation<LocalDate> {

            @Override
            protected Class<? extends ParameterReference<LocalDate>> getStartValueRef() {
                return StartValueRef.class;
            }

            @Override
            protected int compareStartAndEnd(final LocalDate startValue, final LocalDate endValue) {
                return startValue.compareTo(endValue);
            }

        }

    }
}
