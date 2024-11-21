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
 *   Nov 22, 2024 (Tobias Kampmann): created
 */
package org.knime.time.node.manipulate.datetimeround;

import java.time.Period;
import java.util.List;

import org.knime.core.data.DataValue;
import org.knime.core.data.time.localdate.LocalDateValue;
import org.knime.core.data.time.localdatetime.LocalDateTimeValue;
import org.knime.core.data.time.zoneddatetime.ZonedDateTimeValue;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings;
import org.knime.core.webui.node.dialog.defaultdialog.layout.Layout;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.field.Persist;
import org.knime.core.webui.node.dialog.defaultdialog.setting.columnfilter.ColumnFilter;
import org.knime.core.webui.node.dialog.defaultdialog.setting.columnfilter.LegacyColumnFilterPersistor;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ChoicesWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Widget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.choices.ColumnChoicesProviderUtil.CompatibleColumnChoicesProvider;

/**
 *
 * @author Tobias Kampmann
 */
@SuppressWarnings("restriction")
public class DateRoundNodeSettings implements DefaultNodeSettings {

    @Widget(title = "Time columns", description = "Only the included columns will be shifted.")
    @Persist(customPersistor = LegacyColumnFilterPersistor.class)
    @ChoicesWidget(choices = DateColumnProvider.class)
    @Layout(DateTimeRoundNodeLayout.Top.class)
    ColumnFilter m_columnFilter = new ColumnFilter();

    @Widget(title = "Round to", description = """
            The rounding strategy to apply, either round to the first or last value \
            of the chosen rounding precision.
            """)
    @Layout(DateTimeRoundNodeLayout.FirstHorizontal.class)
    DateRoundingStrategy m_dateRoundingStrategy = DateRoundingStrategy.FIRST;

    @Widget(title = "target", description = """
            Option to exclude weekends from the rounding. A weekend is defined as \
            Saturday and Sunday.
            """)
    @Layout(DateTimeRoundNodeLayout.FirstHorizontal.class)
    DayOrWeekday m_dayOrWeekDay = DayOrWeekday.DAY;

    @Widget(title = "of", description = """
            Option to shift the date to the previous or next date in the chosen \
            resolution.
            """)
    @Layout(DateTimeRoundNodeLayout.SecondHorizontal.class)
    ShiftMode m_shiftMode = ShiftMode.THIS;

    @Widget(title = "precision", description = """
            The rounding precision. The date will be rounded to the first or last \
            value of the chosen precision.
            """)
    @Layout(DateTimeRoundNodeLayout.SecondHorizontal.class)
    RoundDatePrecision m_dateRoundingPrecision = RoundDatePrecision.MONTH;

    enum DateRoundingStrategy {
            FIRST, LAST;
    }

    enum DayOrWeekday {
            DAY, WEEKDAY;
    }

    enum RoundDatePrecision {

            DECADE(Period.ofYears(10)), //
            YEAR(Period.ofYears(1)), //
            QUARTER(Period.ofMonths(3)), //
            MONTH(Period.ofMonths(1)), //
            WEEK(Period.ofWeeks(1)); //

        private final Period m_period;

        RoundDatePrecision(final Period period) {
            this.m_period = period;
        }

        public Period getPeriod() {
            return m_period;
        }
    }

    /**
     * Supported column types
     */
    public static final List<Class<? extends DataValue>> DATE_COLUMN_TYPES =
        List.of(LocalDateValue.class, ZonedDateTimeValue.class, LocalDateTimeValue.class);

    static final class DateColumnProvider extends CompatibleColumnChoicesProvider {
        DateColumnProvider() {
            super(DATE_COLUMN_TYPES);
        }
    }
}
