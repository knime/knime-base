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

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataValue;
import org.knime.core.data.time.localdatetime.LocalDateTimeValue;
import org.knime.core.data.time.localtime.LocalTimeValue;
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
public class TimeRoundNodeSettings implements DefaultNodeSettings {

    @Widget(title = "Date&time columns", description = "Only the included columns will be shifted.")
    @ChoicesWidget(choices = TimeColumnProvider.class)
    @Layout(DateTimeRoundNodeLayout.Top.class)
    ColumnFilter m_columnFilter = new ColumnFilter();

    @Widget(title = "Round to", description = """
            The rounding strategy to apply, either round to the first, last or nearest value \
            of the chosen rounding precision.
            """)
    @Layout(DateTimeRoundNodeLayout.FirstHorizontal.class)
    TimeRoundingStrategy m_timeRoundingStrategy = TimeRoundingStrategy.FIRST_POINT_IN_TIME;

    @Widget(title = "Rounding precision", description = """
            The rounding precision. The date will be rounded to the first or last \
            value of the chosen precision. Represented as a duration, i.e., PT1H for \
            one hour.
            """)
    @OverwriteDialogTitle("of")
    @Layout(DateTimeRoundNodeLayout.FirstHorizontal.class)
    RoundTimePrecision m_timeRoundingPrecision = RoundTimePrecision.HOURS_1;

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

    TimeRoundNodeSettings() {
        this((DataTableSpec)null);
    }

    TimeRoundNodeSettings(final DefaultNodeSettingsContext ctx) {
        this(ctx.getDataTableSpec(0).orElse(null));
    }

    TimeRoundNodeSettings(final DataTableSpec spec) {
        if (spec != null) {
            m_columnFilter = new ColumnFilter(DateTimeRoundModelUtils.getCompatibleColumns(spec, TIME_COLUMN_TYPES));
        }
    }

    enum TimeRoundingStrategy {
            @Label(value = "First point in time",
                description = "Round to the first point in time of the selected duration. "
                    + "E.g., rounding 18:45.215 to one hour yields 18:00.")
            FIRST_POINT_IN_TIME, //
            @Label(value = "Last point in time",
                description = "Round to the last point in time of the selected duration. "
                    + "E.g., rounding 18:45.215 to one hour yields 19:00.")
            LAST_POINT_IN_TIME, //
            @Label(value = "Nearest point in time",
                description = "Round to the nearest point in time of the selected duration. "
                    + "E.g., Last/First is chosen depending on which one is closer to the to be rounded time")
            NEAREST_POINT_IN_TIME;
    }

    enum RoundTimePrecision {
            @Label("24 Hours")
            HOURS_24(Duration.ofHours(24)), //
            @Label("12 Hours")
            HOURS_12(Duration.ofHours(12)), //
            @Label("6 Hours")
            HOURS_6(Duration.ofHours(6)), //
            @Label("4 Hours")
            HOURS_4(Duration.ofHours(4)), //
            @Label("3 Hours")
            HOURS_3(Duration.ofHours(3)), //
            @Label("2 Hours")
            HOURS_2(Duration.ofHours(2)), //
            @Label("1 Hour")
            HOURS_1(Duration.ofHours(1)), //
            @Label("30 Minutes")
            MINUTES_30(Duration.ofMinutes(30)), //
            @Label("20 Minutes")
            MINUTES_20(Duration.ofMinutes(20)), //
            @Label("15 Minutes")
            MINUTES_15(Duration.ofMinutes(15)), //
            @Label("12 Minutes")
            MINUTES_12(Duration.ofMinutes(12)), //
            @Label("10 Minutes")
            MINUTES_10(Duration.ofMinutes(10)), //
            @Label("6 Minutes")
            MINUTES_6(Duration.ofMinutes(6)), //
            @Label("5 Minutes")
            MINUTES_5(Duration.ofMinutes(5)), //
            @Label("4 Minutes")
            MINUTES_4(Duration.ofMinutes(4)), //
            @Label("3 Minutes")
            MINUTES_3(Duration.ofMinutes(3)), //
            @Label("2 Minutes")
            MINUTES_2(Duration.ofMinutes(2)), //
            @Label("1 Minute")
            MINUTES_1(Duration.ofMinutes(1)), //
            @Label("30 Seconds")
            SECONDS_30(Duration.ofSeconds(30)), //
            @Label("20 Seconds")
            SECONDS_20(Duration.ofSeconds(20)), //
            @Label("15 Seconds")
            SECONDS_15(Duration.ofSeconds(15)), //
            @Label("12 Seconds")
            SECONDS_12(Duration.ofSeconds(12)), //
            @Label("10 Seconds")
            SECONDS_10(Duration.ofSeconds(10)), //
            @Label("6 Seconds")
            SECONDS_6(Duration.ofSeconds(6)), //
            @Label("5 Seconds")
            SECONDS_5(Duration.ofSeconds(5)), //
            @Label("4 Seconds")
            SECONDS_4(Duration.ofSeconds(4)), //
            @Label("3 Seconds")
            SECONDS_3(Duration.ofSeconds(3)), //
            @Label("2 Seconds")
            SECONDS_2(Duration.ofSeconds(2)), //
            @Label("1 Second")
            SECONDS_1(Duration.ofSeconds(1)), //
            @Label("500 Milliseconds")
            MILLISECONDS_500(Duration.ofMillis(500)), //
            @Label("250 Milliseconds")
            MILLISECONDS_250(Duration.ofMillis(250)), //
            @Label("125 Milliseconds")
            MILLISECONDS_125(Duration.ofMillis(125)), //
            @Label("100 Milliseconds")
            MILLISECONDS_100(Duration.ofMillis(100)), //
            @Label("50 Milliseconds")
            MILLISECONDS_50(Duration.ofMillis(50)), //
            @Label("25 Milliseconds")
            MILLISECONDS_25(Duration.ofMillis(25)), //
            @Label("20 Milliseconds")
            MILLISECONDS_20(Duration.ofMillis(20)), //
            @Label("10 Milliseconds")
            MILLISECONDS_10(Duration.ofMillis(10)), //
            @Label("5 Milliseconds")
            MILLISECONDS_5(Duration.ofMillis(5)), //
            @Label("2 Milliseconds")
            MILLISECONDS_2(Duration.ofMillis(2)), //
            @Label("1 Millisecond")
            MILLISECONDS_1(Duration.ofMillis(1)); //

        private final Duration m_duration;

        RoundTimePrecision(final Duration duration) {
            this.m_duration = duration;
        }

        public Duration getDuration() {
            return m_duration;
        }

        public static RoundTimePrecision getByDuration(final Duration duration) {
            for (RoundTimePrecision rtp : RoundTimePrecision.values()) {
                if (rtp.getDuration().equals(duration)) {
                    return rtp;
                }
            }

            throw new IllegalArgumentException("No RoundTimePrecision for duration: " + duration + " found. "
                + " Possible values are: " + getAllAllowedDurations() + ".");
        }

        static final List<String> getAllAllowedDurations() {
            return Arrays.stream(RoundTimePrecision.values()) //
                .map(RoundTimePrecision::getDuration) //
                .map(Duration::toString) //
                .collect(Collectors.toList());
        }

    }

    static final List<Class<? extends DataValue>> TIME_COLUMN_TYPES =
        List.of(LocalTimeValue.class, ZonedDateTimeValue.class, LocalDateTimeValue.class);

    static final class TimeColumnProvider extends CompatibleColumnChoicesProvider {
        TimeColumnProvider() {
            super(TimeRoundNodeSettings.TIME_COLUMN_TYPES);
        }
    }
}
