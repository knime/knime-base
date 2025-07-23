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
 *   Jan 7, 2025 (david): created
 */
package org.knime.time.node.format.durationperiodformatmanager;

import org.knime.base.node.viz.format.AlignmentSuggestionOption;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.widget.choices.ChoicesProvider;
import org.knime.node.parameters.widget.choices.ValueSwitchWidget;
import org.knime.node.parameters.widget.choices.filter.ColumnFilter;
import org.knime.node.parameters.widget.choices.util.ColumnSelectionUtil;
import org.knime.time.util.DateTimeUtils;
import org.knime.time.util.DurationPeriodStringFormat;

/**
 * The settings for the DurationPeriodFormatManager node.
 *
 * @author David Hickey, TNG Technology Consulting GmbH
 */
@SuppressWarnings("restriction")
final class DurationPeriodFormatManagerNodeSettings implements NodeParameters {

    DurationPeriodFormatManagerNodeSettings() {
    }

    /**
     * Selects all compatible columns by default.
     *
     * @param context the settings context
     */
    DurationPeriodFormatManagerNodeSettings(final NodeParametersInput context) {
        var spec = context.getInTableSpec(0);

        if (spec.isPresent()) {
            m_columnFilter = new ColumnFilter(
                ColumnSelectionUtil.getCompatibleColumns(spec.get(), DateTimeUtils.INTERVAL_COLUMN_TYPES));
        }
    }

    @Widget(title = "Duration columns", description = "The duration columns to create a formatter for.")
    @ChoicesProvider(DateTimeUtils.IntervalColumnProvider.class)
    ColumnFilter m_columnFilter = new ColumnFilter();

    @Widget(title = "Duration format", description = "The format of the output string.")
    @ValueSwitchWidget
    DurationPeriodStringFormat m_format = DurationPeriodStringFormat.ISO;

    @Widget(title = "Alignment suggestion",
        description = "Position the value horizontally in compatible views like the Table View.")
    @ValueSwitchWidget
    AlignmentSuggestionOption m_alignment = AlignmentSuggestionOption.LEFT;
}
