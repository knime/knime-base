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
 *   Mar 28, 2025 (paulbaernreuther): created
 */
package org.knime.time.node.convert.timestamptodatetime;

import java.time.ZoneId;
import java.util.List;
import java.util.Set;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.IntValue;
import org.knime.core.data.LongValue;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings;
import org.knime.core.webui.node.dialog.defaultdialog.setting.filter.column.ColumnFilter;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Label;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ValueSwitchWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Widget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.choices.ChoicesProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.choices.column.ColumnChoicesProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.Effect;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.Effect.EffectType;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.ValueReference;
import org.knime.time.util.DateTimeType;
import org.knime.time.util.ReplaceOrAppend;

@SuppressWarnings("restriction")
final class TimestampToDateTimeNodeSettings implements DefaultNodeSettings {

    TimestampToDateTimeNodeSettings() {
    }

    TimestampToDateTimeNodeSettings(final DefaultNodeSettingsContext context) {
        var spec = context.getDataTableSpec(0);

        if (spec.isPresent()) {
            final var timestampColumns = spec.get().stream().filter(TimestampToDateTimeNodeSettings::isTimestampColumn)
                .map(DataColumnSpec::getName).toArray(String[]::new);
            m_timestampColumns = new ColumnFilter(timestampColumns);
        }
    }

    /**
     * This method determines which columns the user can pick as timestamp columns. We deliberately limit the options to
     * all columns whose preferred value is int or long (and not just everything compatible with {@link LongValue}
     */
    static final boolean isTimestampColumn(final DataColumnSpec col) {
        return Set.of(IntValue.class, LongValue.class).contains(col.getType().getPreferredValueClass());
    }

    static final class TimestampColumnsProvider implements ColumnChoicesProvider {

        @Override
        public List<DataColumnSpec> columnChoices(final DefaultNodeSettingsContext context) {
            return context.getDataTableSpec(0).stream().flatMap(DataTableSpec::stream)
                .filter(TimestampToDateTimeNodeSettings::isTimestampColumn).toList();
        }
    }

    @ChoicesProvider(TimestampColumnsProvider.class)
    @Widget(title = "Timestamp columns", description = "The columns to convert to date&amp;time columns.")
    ColumnFilter m_timestampColumns = new ColumnFilter();

    enum TimestampGranularity {
            @Label("Seconds")
            SECONDS, //
            @Label("Milliseconds")
            MILLISECONDS, //
            @Label("Microseconds")
            MICROSECONDS, //
            @Label("Nanoseconds")
            NANOSECONDS;
    }

    @Widget(title = "Timestamp granularity", description = "The unit of the input unix timestamp.")
    TimestampGranularity m_granularity = TimestampGranularity.SECONDS;

    @ValueReference(DateTimeType.Ref.class)
    @ValueSwitchWidget
    @Widget(title = "Output type", description = "Select the type of the new columns.")
    DateTimeType m_outputType = DateTimeType.LOCAL_DATE;

    @Effect(predicate = DateTimeType.IsZonedDateTime.class, type = EffectType.SHOW)
    @Widget(title = "Timezone", description = "Select the timezone of the new columns.")
    ZoneId m_timeZone = ZoneId.systemDefault();

    @ValueSwitchWidget
    @ValueReference(ReplaceOrAppend.ValueRef.class)
    @Widget(title = "Output columns", description = """
            Depending on this setting, the output columns will either replace the modified columns, or be \
            appended to the table with a suffix.
            """)
    ReplaceOrAppend m_appendOrReplace = ReplaceOrAppend.REPLACE;

    @Widget(title = "Output column suffix",
        description = "The suffix to append to the column names of the new columns.")
    @Effect(predicate = ReplaceOrAppend.IsAppend.class, type = EffectType.SHOW)
    String m_outputColumnSuffix = " (Date)";

}
