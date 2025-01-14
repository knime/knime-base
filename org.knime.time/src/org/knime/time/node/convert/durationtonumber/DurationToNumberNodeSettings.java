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
package org.knime.time.node.convert.durationtonumber;

import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.time.duration.DurationValue;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings;
import org.knime.core.webui.node.dialog.defaultdialog.setting.columnfilter.ColumnFilter;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ChoicesWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ColumnChoicesStateProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Label;
import org.knime.core.webui.node.dialog.defaultdialog.widget.TextMessage;
import org.knime.core.webui.node.dialog.defaultdialog.widget.TextMessage.InputPreviewMessageProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ValueSwitchWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Widget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.Effect;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.Effect.EffectType;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.Reference;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.ValueReference;
import org.knime.time.util.DurationPeriodFormatUtils;
import org.knime.time.util.ReplaceOrAppend;
import org.knime.time.util.SettingsDataUtil;
import org.knime.time.util.TimeBasedGranularityUnit;

/**
 * New settings for the node which converts durations to numbers.
 *
 * @author David Hickey, TNG Technology Consulting GmbH
 */
@SuppressWarnings("restriction")
final class DurationToNumberNodeSettings implements DefaultNodeSettings {

    DurationToNumberNodeSettings() {
    }

    DurationToNumberNodeSettings(final DefaultNodeSettingsContext context) {
        var spec = context.getDataTableSpec(0);

        if (spec.isPresent()) {
            m_filter = new ColumnFilter(spec.get().stream() //
                .filter(ColumnProvider.IS_COMPATIBLE_COLUMN) //
                .map(DataColumnSpec::getName) //
                .toArray(String[]::new) //
            );
        }
    }

    @Widget(title = "Duration columns", description = "The columns to convert to a number.")
    @ChoicesWidget(choicesProvider = ColumnProvider.class)
    @ValueReference(ColumnFilterRef.class)
    ColumnFilter m_filter = new ColumnFilter();

    @TextMessage(value = InputPreviewMessage.class)
    Void m_firstCell;

    @Widget(title = "Output columns", description = """
            Depending on this setting, the output columns will either replace the modified columns, or be \
            appended to the table with a suffix.
            """)
    @ValueSwitchWidget
    @ValueReference(ReplaceOrAppend.ValueRef.class)
    ReplaceOrAppend m_appendOrReplaceColumn = ReplaceOrAppend.REPLACE;

    @Widget(title = "Output column suffix",
        description = "The suffix to append to the column names of the new columns.")
    @Effect(predicate = ReplaceOrAppend.IsAppend.class, type = EffectType.SHOW)
    String m_suffix = " (Number)"; // space is intentional

    @Widget(title = "Rounding behaviour", description = "The behaviour for rounding the number.")
    @ValueSwitchWidget
    RoundingBehaviour m_roundingBehaviour = RoundingBehaviour.INTEGER;

    @Widget(title = "Unit of the number", description = "The unit of the number to output.")
    TimeBasedGranularityUnit m_unit = TimeBasedGranularityUnit.HOURS;

    enum RoundingBehaviour {
            @Label(value = "No decimals", description = """
                    The output will be the truncated calculation, i.e. the duration \
                    <i>1 hour 30 minutes 30 seconds</i> will be converted to \
                    <i>1 hour</i> or <i>90 minutes</i> or <i>5430 seconds</i> \
                    etc., depending upon the value of the unit of the number. \
                    """)
            INTEGER, //
            @Label(value = "Decimals", description = """
                    The output will be the exact calculation, i.e. the duration \
                    <i>1 hour 30 minutes 30 seconds</i> will be converted to \
                    <i>1.508 hours</i> or <i>90.5 minutes</i> or <i>5430 seconds</i> \
                    etc., depending upon the value of the unit of the number. \
                    """)
            DOUBLE;
    }

    interface ColumnFilterRef extends Reference<ColumnFilter> {
    }

    static final class ColumnProvider implements ColumnChoicesStateProvider {

        static final Predicate<DataColumnSpec> IS_COMPATIBLE_COLUMN =
            c -> c.getType().isCompatible(DurationValue.class);

        @Override
        public DataColumnSpec[] columnChoices(final DefaultNodeSettingsContext context) {
            return context.getDataTableSpec(0).map(DataTableSpec::stream) //
                .orElseGet(Stream::empty) //
                .filter(IS_COMPATIBLE_COLUMN) //
                .toArray(DataColumnSpec[]::new);
        }
    }

    static final class InputPreviewMessage implements InputPreviewMessageProvider {

        private Supplier<ColumnFilter> m_columnFilter;

        @Override
        public void init(final StateProviderInitializer initializer) {
            InputPreviewMessageProvider.super.init(initializer);
            m_columnFilter = initializer.computeFromValueSupplier(ColumnFilterRef.class);
        }

        @Override
        public Optional<ColumnFilter> getFilter() {
            return Optional.of(m_columnFilter.get());
        }

        @Override
        public Optional<String> getFirstDataCellPreview(final DataTable dt, final String[] selectedCols) {
            var colNum = SettingsDataUtil.getFirstColumnIndexFromSelectedColumnArray(dt.getDataTableSpec(),
                ColumnProvider.IS_COMPATIBLE_COLUMN, selectedCols);

            if (colNum.isPresent()) {
                var cell = SettingsDataUtil.getFirstNonMissingCellInColumn(dt, colNum.get());

                if (cell.isEmpty()) {
                    return Optional.empty();
                }

                if (cell.get() instanceof DurationValue dv) {
                    return Optional.of(DurationPeriodFormatUtils.formatDurationShort(dv.getDuration()));
                }
            }

            return Optional.empty();
        }

    }
}
