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
 *   Nov 18, 2024 (david): created
 */
package org.knime.time.node.convert.durationperiodtostring;

import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataValue;
import org.knime.core.data.time.duration.DurationValue;
import org.knime.core.data.time.period.PeriodValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.field.NodeSettingsPersistorWithConfigKey;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.field.Persist;
import org.knime.core.webui.node.dialog.defaultdialog.setting.columnfilter.ColumnFilter;
import org.knime.core.webui.node.dialog.defaultdialog.setting.columnfilter.LegacyColumnFilterPersistor;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ChoicesWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ColumnChoicesStateProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Label;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ValueSwitchWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Widget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.Effect;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.Effect.EffectType;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.Reference;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.ValueReference;
import org.knime.time.util.ReplaceOrAppend;

/**
 * Settings for the Duration/Period To String node.
 *
 * @author David Hickey, TNG Technology Consulting GmbH
 */
@SuppressWarnings("restriction")
final class DurationPeriodToStringNodeSettings implements DefaultNodeSettings {

    DurationPeriodToStringNodeSettings() {
    }

    DurationPeriodToStringNodeSettings(final DefaultNodeSettingsContext context) {
        var spec = context.getDataTableSpec(0);

        if (spec.isPresent()) {
            m_filter = new ColumnFilter(spec.get().stream() //
                .filter(ColumnProvider.IS_COMPATIBLE_TYPE) //
                .map(DataColumnSpec::getName) //
                .toArray(String[]::new) //
            );
        }
    }

    @Widget(title = "Duration columns", description = "The columns to convert to a string.")
    @ChoicesWidget(choicesProvider = ColumnProvider.class)
    @Persist(configKey = "col_select", customPersistor = LegacyColumnFilterPersistor.class)
    ColumnFilter m_filter = new ColumnFilter();

    @Widget(title = "Output format", description = "The format of the output string.")
    @ValueSwitchWidget
    @Persist(configKey = "format", customPersistor = OutputFormat.Persistor.class)
    @ValueReference(OutputFormat.Ref.class)
    OutputFormat m_outputFormat = OutputFormat.ISO;

    @Widget(title = "Output columns", description = """
            Depending on this setting, the output columns will either replace the modified columns, or be \
            appended to the table with a suffix.
            """)
    @ValueSwitchWidget
    @Persist(customPersistor = ReplaceOrAppend.Persistor.class)
    @ValueReference(ReplaceOrAppend.ValueRef.class)
    ReplaceOrAppend m_appendOrReplaceColumn = ReplaceOrAppend.REPLACE;

    @Widget(title = "Suffix of appended column", description = """
            The suffix to append to the column names of the new columns.
            """)
    @Persist(configKey = "suffix")
    @Effect(predicate = ReplaceOrAppend.IsAppend.class, type = EffectType.SHOW)
    String m_suffix = "";

    enum OutputFormat {
            @Label(value = "ISO 8601", description = """
                    The durations will be formatted using the ISO-8601 representation, e.g. \
                    'P2Y3M5D'.
                    """)
            ISO("ISO-8601 representation"), //
            @Label(value = "Whole words", description = """
                    The durations will be formatted using words to represent them, e.g. '2 \
                    years 3 months 5 days'.
                    """)
            WORDS("Long representation"), //
            @Label(value = "Single letters", description = """
                    The durations will be formatted using letters to represent them, e.g. \
                    '2y 3M 5d' (Date-based duration: y: years, M: months, d: days; Time-based \
                    duration: H: hours, m: minutes, s: seconds).
                    """)
            LETTERS("Short representation");

        private final String m_oldConfigValue;

        OutputFormat(final String oldConfigValue) {
            this.m_oldConfigValue = oldConfigValue;
        }

        private static OutputFormat getByOldConfigValue(final String oldConfigValue) {
            return Arrays.stream(values()) //
                .filter(v -> v.m_oldConfigValue.equals(oldConfigValue)) //
                .findFirst() //
                .orElseThrow(
                    () -> new IllegalArgumentException("No OutputFormat for old config value: " + oldConfigValue));
        }

        interface Ref extends Reference<OutputFormat> {
        }

        static final class Persistor extends NodeSettingsPersistorWithConfigKey<OutputFormat> {

            @Override
            public OutputFormat load(final NodeSettingsRO settings) throws InvalidSettingsException {
                var oldConfigValue = settings.getString(getConfigKey());

                try {
                    return OutputFormat.getByOldConfigValue(oldConfigValue);
                } catch (final IllegalArgumentException e) {
                    var validValuesJoinedByComma = Arrays.stream(OutputFormat.values()) //
                        .map(v -> v.m_oldConfigValue) //
                        .collect(Collectors.joining(", "));

                    throw new InvalidSettingsException("Invalid output format setting '%s'. Valid values are: %s"
                        .formatted(oldConfigValue, validValuesJoinedByComma), e);
                }
            }

            @Override
            public void save(final OutputFormat obj, final NodeSettingsWO settings) {
                settings.addString(getConfigKey(), obj.m_oldConfigValue);
            }
        }
    }

    static final class ColumnProvider implements ColumnChoicesStateProvider {

        private static final List<Class<? extends DataValue>> COMPATIBLE_TYPES =
            List.of(DurationValue.class, PeriodValue.class);

        static final Predicate<DataColumnSpec> IS_COMPATIBLE_TYPE =
            columnSpec -> COMPATIBLE_TYPES.stream().anyMatch(columnSpec.getType()::isCompatible);

        @Override
        public DataColumnSpec[] columnChoices(final DefaultNodeSettingsContext context) {
            return context.getDataTableSpec(0).map(DataTableSpec::stream) //
                .orElseGet(Stream::empty) //
                .filter(IS_COMPATIBLE_TYPE) //
                .toArray(DataColumnSpec[]::new);
        }
    }
}
