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

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataValue;
import org.knime.core.data.time.localdate.LocalDateValue;
import org.knime.core.data.time.localdatetime.LocalDateTimeValue;
import org.knime.core.data.time.zoneddatetime.ZonedDateTimeValue;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings;
import org.knime.core.webui.node.dialog.defaultdialog.layout.Layout;
import org.knime.core.webui.node.dialog.defaultdialog.setting.columnfilter.ColumnFilter;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ChoicesWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Label;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ValueSwitchWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Widget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.choices.ColumnChoicesProviderUtil.CompatibleColumnChoicesProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.internal.OverwriteDialogTitle;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.Effect;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.Effect.EffectType;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.ValueReference;
import org.knime.time.util.ReplaceOrAppend;

/**
 *
 * @author Tobias Kampmann
 */
@SuppressWarnings("restriction")
public class DateRoundNodeSettings implements DefaultNodeSettings {

    @Widget(title = "Date&time columns", description = "Only the included columns will be shifted.")
    @ChoicesWidget(choices = DateColumnProvider.class)
    @Layout(DateTimeRoundNodeLayout.Top.class)
    ColumnFilter m_columnFilter = new ColumnFilter();

    @Widget(title = "Round to", description = """
            The rounding strategy to apply, either round to the first or last value \
            of the chosen rounding precision.
            """)
    @Layout(DateTimeRoundNodeLayout.FirstHorizontal.class)
    DateRoundingStrategy m_dateRoundingStrategy = DateRoundingStrategy.FIRST;

    @Widget(title = "Exclude weekends", description = """
            Option to exclude weekends from the rounding. A weekend is defined as \
            Saturday and Sunday.
            """)
    @OverwriteDialogTitle("")
    @Layout(DateTimeRoundNodeLayout.FirstHorizontal.class)
    DayOrWeekday m_dayOrWeekDay = DayOrWeekday.DAY;

    @Widget(title = "Additional shift", description = """
            Option to shift the date to the previous or next date in the chosen \
            resolution.
            """)
    @OverwriteDialogTitle("of")
    @Layout(DateTimeRoundNodeLayout.SecondHorizontal.class)
    ShiftMode m_shiftMode = ShiftMode.THIS;

    @Widget(title = "Rounding precision", description = """
            The rounding precision. The date will be rounded to the first or last \
            value of the chosen precision.
            """)
    @Layout(DateTimeRoundNodeLayout.SecondHorizontal.class)
    @OverwriteDialogTitle("")
    RoundDatePrecision m_dateRoundingPrecision = RoundDatePrecision.MONTH;

    @Widget(title = "Output columns",
        description = "Depending on the selection, the selected columns will be replaced "
            + "or appended to the input table.")
    @ValueSwitchWidget
    @ValueReference(ReplaceOrAppend.ValueRef.class)
    @Layout(DateTimeRoundNodeLayout.Bottom.class)
    ReplaceOrAppend m_replaceOrAppend = ReplaceOrAppend.REPLACE;

    @Widget(title = "Output column suffix",
        description = "The suffix that is appended to the column name. "
            + "The suffix will be added to the original column name separated by a space.")
    @Effect(predicate = ReplaceOrAppend.IsAppend.class, type = EffectType.SHOW)
    @Layout(DateTimeRoundNodeLayout.Bottom.class)
    String m_outputColumnSuffix = " (Rounded)";

    DateRoundNodeSettings() {
        this((DataTableSpec)null);
    }

    DateRoundNodeSettings(final DefaultNodeSettingsContext ctx) {
        this(ctx.getDataTableSpec(0).orElse(null));
    }

    DateRoundNodeSettings(final DataTableSpec spec) {
        if (spec != null) {
            m_columnFilter = new ColumnFilter(DateTimeRoundModelUtils.getCompatibleColumns(spec, DATE_COLUMN_TYPES));
        }
    }

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
     * Enumeration for the shift mode. Additional option to shift the date to the previous or next date in the chosen
     * resolution.
     */
    enum ShiftMode {
            /**
             * Shift to the previous value. 12.12.24 rounded to the first day of the 'previous' month will result in
             * 1.11.24.
             */
            @Label(value = "Previous", description = "Shift to the previous value. 12.12.24 rounded to the "
                + "first day of the 'previous' month will result in 1.11.24.")
            PREVIOUS,
            /**
             * Option to not shift the value. Shift to the this value, i.e., no shift at all.
             */
            @Label(value = "This", description = "Shift to the this value, i.e., no shift at all.")
            THIS,
            /**
             * Shift to the next value. 12.12.24 rounded to the first day of the 'next' month will result in 1.1.25.
             */
            @Label(value = "Next", description = "Shift to the next value. 12.12.24 rounded to the "
                + "first day of the 'next' month will result in 1.1.25.")
            NEXT;
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
