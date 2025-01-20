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
 *   Oct 21, 2024 (kampmann): created
 */
package org.knime.time.node.manipulate.datetimeshift;

import java.util.Collection;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataValue;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings;
import org.knime.core.webui.node.dialog.defaultdialog.setting.columnfilter.ColumnFilter;
import org.knime.core.webui.node.dialog.defaultdialog.setting.interval.TimeInterval;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ChoicesWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.CompatibleColumnChoicesStateProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.CompatibleDataValueClassesSupplier;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Label;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ValueSwitchWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Widget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.Effect;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.Effect.EffectType;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.Predicate;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.PredicateProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.Reference;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.ValueReference;
import org.knime.time.node.manipulate.datetimeshift.DateTimeShiftSettingsUtils.NumberColumnProvider;
import org.knime.time.util.DateTimeUtils;
import org.knime.time.util.ReplaceOrAppend;
import org.knime.time.util.TimeBasedGranularityUnit;

/**
 * Settings for the Time Shift WebUI node.
 *
 * @author Tobias Kampmann, TNG Technology Consulting GmbH
 * @author David Hickey, TNG Technology Consulting GmbH
 */
@SuppressWarnings("restriction")
class TimeShiftNodeSettings implements DefaultNodeSettings {
    @Widget(title = "Date&time columns", description = "Only the included columns will be shifted.")
    @ChoicesWidget(choicesProvider = ColumnProvider.class)
    ColumnFilter m_columnFilter = new ColumnFilter();

    @Widget(title = "Shift mode", description = "Select the shift mode to use.")
    @ValueSwitchWidget
    @ValueReference(ShiftModeRef.class)
    ShiftMode m_shiftMode = ShiftMode.SHIFT_VALUE;

    @Widget(title = "Shift value", description = """
            Select to insert a format string to use as constant shift value. The inserted string \
            can be either in:
            <ul>
              <li>
                the ISO-8601 representation (see <a href= \
                "https://docs.oracle.com/javase/8/docs/api/java/time/Duration.html#parse-java.lang.CharSequence-"> \
                time-based duration format</a> for details)
              </li>
              <li>
                the short letter representation (e.g. '4H 5m 6s'). In this case, the letters H, m, and s \
                respectively indicate hours, minutes, and seconds.
              </li>
              <li>
                the long word representation (e.g. '4 hours 5 minutes 6 seconds).
              </li>
            </ul>
            """)
    @Effect(predicate = ShiftModeIsShiftValue.class, type = EffectType.SHOW)
    TimeInterval m_shiftDurationValue = TimeInterval.of(0, 1, 0, 0);

    @Widget(title = "Column", description = """
            Select to choose the shift value from a duration column.
            """)
    @ChoicesWidget(choices = DateTimeUtils.DurationColumnProvider.class)
    @Effect(predicate = ShiftModeIsDurationColumn.class, type = EffectType.SHOW)
    String m_durationColumn;

    @Widget(title = "Column", description = """
            Select to choose the shift value from a numerical column. The shift value will be scaled by \
            the selected granularity.
            """)
    @ChoicesWidget(choicesProvider = NumberColumnProvider.class)
    @Effect(predicate = ShiftModeIsNumericalColumn.class, type = EffectType.SHOW)
    String m_numericalColumn;

    @Widget(title = "Granularity", description = "The granularity (i.e. hours, seconds, etc) of the shift.")
    @Effect(predicate = ShiftModeIsNumericalColumn.class, type = EffectType.SHOW)
    TimeBasedGranularityUnit m_granularity = TimeBasedGranularityUnit.HOURS;

    @Widget(title = "Output columns", description = """
            Depending on the selection, the selected columns will be replaced \
            or appended to the input table.
            """)
    @ValueSwitchWidget
    @ValueReference(ReplaceOrAppend.ValueRef.class)
    ReplaceOrAppend m_replaceOrAppend = ReplaceOrAppend.REPLACE;

    @Widget(title = "Output column suffix", description = """
            The suffix that is appended to the column name. The suffix will be added to the original \
            column name.
            """)
    @Effect(predicate = ReplaceOrAppend.IsAppend.class, type = EffectType.SHOW)
    String m_outputColumnSuffix = " (Shifted)";

    /*
     * ------------------------------------------------------------------------
     * CONSTRUCTORS
     * ------------------------------------------------------------------------
     */

    protected TimeShiftNodeSettings() {
        this((DataTableSpec)null);
    }

    /**
     * @param context
     */
    protected TimeShiftNodeSettings(final DefaultNodeSettingsContext context) {
        this(context.getDataTableSpecs()[0]);
    }

    /**
     * @param spec
     */
    public TimeShiftNodeSettings(final DataTableSpec spec) {
        m_durationColumn = getFirstDurationColumn(spec);
        m_numericalColumn = NumberColumnProvider.getFirstNumberColumn(spec);

        if (spec != null) {
            m_columnFilter = new ColumnFilter(DateTimeUtils.getCompatibleTimeColumns(spec));
        }
    }

    /*
     * ------------------------------------------------------------------------
     * ENUMS
     * ------------------------------------------------------------------------
     */

    enum ShiftMode implements CompatibleDataValueClassesSupplier {
            @Label(value = "Shift value", description = "A time-based shift value.")
            SHIFT_VALUE, //
            @Label(value = "Duration column", //
                description = "A shift value from a duration column.")
            DURATION_COLUMN, //
            @Label(value = "Numerical column", //
                description = "Select a numerical column to scale a configurable time unit.")
            NUMERICAL_COLUMN;

        @Override
        public Collection<Class<? extends DataValue>> getCompatibleDataValueClasses() {
            return DateTimeUtils.TIME_COLUMN_TYPES;
        }
    }

    /*
     * ------------------------------------------------------------------------
     * PREDICATE PROVIDERS AMD REFERENCES
     * ------------------------------------------------------------------------
     */

    interface ShiftModeRef extends Reference<ShiftMode> {
    }

    static final class ShiftModeIsShiftValue implements PredicateProvider {

        @Override
        public Predicate init(final PredicateInitializer i) {
            return i.getEnum(ShiftModeRef.class).isOneOf(ShiftMode.SHIFT_VALUE);
        }
    }

    static final class ShiftModeIsDurationColumn implements PredicateProvider {

        @Override
        public Predicate init(final PredicateInitializer i) {
            return i.getEnum(ShiftModeRef.class).isOneOf(ShiftMode.DURATION_COLUMN);
        }
    }

    static final class ShiftModeIsNumericalColumn implements PredicateProvider {

        @Override
        public Predicate init(final PredicateInitializer i) {
            return i.getEnum(ShiftModeRef.class).isOneOf(ShiftMode.NUMERICAL_COLUMN);
        }
    }

    /*
     * ------------------------------------------------------------------------
     * STATE PROVIDERS
     * ------------------------------------------------------------------------
     */

    static final class ColumnProvider extends CompatibleColumnChoicesStateProvider<ShiftMode> {

        @Override
        protected Class<? extends Reference<ShiftMode>> getReferenceClass() {
            return ShiftModeRef.class;
        }
    }

    static String getFirstDurationColumn(final DataTableSpec spec) {
        if (spec == null) {
            return "";
        }
        return spec.stream() //
            .filter(DateTimeShiftSettingsUtils::isDurationColumn) //
            .findFirst() //
            .map(DataColumnSpec::getName) //
            .orElse("");
    }
}
