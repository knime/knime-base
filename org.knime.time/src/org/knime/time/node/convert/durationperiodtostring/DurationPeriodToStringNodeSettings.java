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

import java.util.Optional;
import java.util.function.Supplier;

import org.knime.core.data.DataTable;
import org.knime.core.data.time.duration.DurationValue;
import org.knime.core.data.time.period.PeriodValue;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.persistence.Persist;
import org.knime.node.parameters.persistence.Persistor;
import org.knime.node.parameters.persistence.legacy.LegacyColumnFilterPersistor;
import org.knime.node.parameters.updates.Effect;
import org.knime.node.parameters.updates.Effect.EffectType;
import org.knime.node.parameters.updates.ParameterReference;
import org.knime.node.parameters.updates.ValueReference;
import org.knime.node.parameters.widget.choices.ChoicesProvider;
import org.knime.node.parameters.widget.choices.ValueSwitchWidget;
import org.knime.node.parameters.widget.choices.filter.ColumnFilter;
import org.knime.node.parameters.widget.choices.util.ColumnSelectionUtil;
import org.knime.node.parameters.widget.message.TextMessage;
import org.knime.time.node.convert.InputPreviewMessageProvider;
import org.knime.time.util.DateTimeUtils;
import org.knime.time.util.DurationPeriodFormatUtils;
import org.knime.time.util.DurationPeriodStringFormat;
import org.knime.time.util.ReplaceOrAppend;
import org.knime.time.util.SettingsDataUtil;

/**
 * Settings for the Duration/Period To String node.
 *
 * @author David Hickey, TNG Technology Consulting GmbH
 */
@SuppressWarnings("restriction")
final class DurationPeriodToStringNodeSettings implements NodeParameters {

    DurationPeriodToStringNodeSettings() {
    }

    DurationPeriodToStringNodeSettings(final NodeParametersInput context) {
        var spec = context.getInTableSpec(0);

        if (spec.isPresent()) {
            m_filter = new ColumnFilter(
                ColumnSelectionUtil.getCompatibleColumns(spec.get(), DateTimeUtils.INTERVAL_COLUMN_TYPES));
        }
    }

    static final class FilterPersistor extends LegacyColumnFilterPersistor {

        FilterPersistor() {
            super("col_select");
        }
    }

    @Widget(title = "Duration columns", description = "The columns to convert to a string.")
    @ChoicesProvider(DateTimeUtils.IntervalColumnProvider.class)
    @Persistor(FilterPersistor.class)
    @ValueReference(ColumnFilterRef.class)
    ColumnFilter m_filter = new ColumnFilter();

    @TextMessage(value = InputPreviewMessage.class)
    Void m_firstCell;

    @Widget(title = "Output format", description = "The format of the output string.")
    @ValueSwitchWidget
    @Persistor(DurationPeriodStringFormat.LegacyPersistor.class)
    @ValueReference(DurationPeriodStringFormat.Ref.class)
    DurationPeriodStringFormat m_outputFormat = DurationPeriodStringFormat.ISO;

    @Widget(title = "Output columns", description = """
            Depending on this setting, the output columns will either replace the modified columns, or be \
            appended to the table with a suffix.
            """)
    @ValueSwitchWidget
    @Persistor(ReplaceOrAppend.Persistor.class)
    @ValueReference(ReplaceOrAppend.ValueRef.class)
    ReplaceOrAppend m_appendOrReplaceColumn = ReplaceOrAppend.REPLACE;

    @Widget(title = "Output column suffix", description = """
            The suffix to append to the column names of the new columns.
            """)
    @Persist(configKey = "suffix")
    @Effect(predicate = ReplaceOrAppend.IsAppend.class, type = EffectType.SHOW)
    String m_suffix = "";

    interface ColumnFilterRef extends ParameterReference<ColumnFilter> {
    }

    static final class InputPreviewMessage implements InputPreviewMessageProvider {

        private Supplier<ColumnFilter> m_columnFilter;

        @Override
        public void init(final StateProviderInitializer initializer) {
            InputPreviewMessageProvider.super.init(initializer);
            m_columnFilter = initializer.computeFromValueSupplier(ColumnFilterRef.class);
        }

        @Override
        public Optional<String> getFirstDataCellPreview(final DataTable dt, final String[] selectedCols) {
            var colNum = SettingsDataUtil.getFirstColumnIndexFromSelectedColumnArray(dt.getDataTableSpec(),
                DateTimeUtils.IntervalColumnProvider::isCompatibleType, selectedCols);

            if (colNum.isPresent()) {
                var cell = SettingsDataUtil.getFirstNonMissingCellInColumn(dt, colNum.get());

                if (cell.isEmpty()) {
                    return Optional.empty();
                }

                if (cell.get() instanceof DurationValue dv) {
                    return Optional.of(DurationPeriodFormatUtils.formatDurationShort(dv.getDuration()));
                } else if (cell.get() instanceof PeriodValue pv) {
                    return Optional.of(DurationPeriodFormatUtils.formatPeriodShort(pv.getPeriod()));
                }
            }

            return Optional.empty();
        }

        @Override
        public Optional<ColumnFilter> getFilter() {
            return Optional.of(m_columnFilter.get());
        }
    }
}
