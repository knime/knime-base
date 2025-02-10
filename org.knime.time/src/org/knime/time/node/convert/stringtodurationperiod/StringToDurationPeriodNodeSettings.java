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
 *   Nov 26, 2024 (david): created
 */
package org.knime.time.node.convert.stringtodurationperiod;

import org.knime.core.data.DataType;
import org.knime.core.data.StringValue;
import org.knime.core.data.time.duration.DurationCellFactory;
import org.knime.core.data.time.period.PeriodCellFactory;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings;
import org.knime.core.webui.node.dialog.defaultdialog.setting.columnfilter.ColumnFilter;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ChoicesWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Label;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ValueSwitchWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Widget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.choices.ColumnChoicesProviderUtil.StringColumnChoicesProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.Effect;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.Effect.EffectType;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.ValueReference;
import org.knime.time.util.ActionIfExtractionFails;
import org.knime.time.util.DateTimeUtils;
import org.knime.time.util.ReplaceOrAppend;

/**
 * Settings for the String to Duration/Period node.
 *
 * @author David Hickey, TNG Technology Consulting GmbH
 */
@SuppressWarnings("restriction")
final class StringToDurationPeriodNodeSettings implements DefaultNodeSettings {

    StringToDurationPeriodNodeSettings(final DefaultNodeSettingsContext context) {
        var spec = context.getDataTableSpec(0);

        if (spec.isPresent()) {
            m_columnFilter = new ColumnFilter(DateTimeUtils.getCompatibleColumns(spec.get(), StringValue.class));
        }
    }

    StringToDurationPeriodNodeSettings() {
        // default constructor is needed
    }

    @Widget(title = "String columns", description = "The string columns to convert to a duration/period.")
    @ChoicesWidget(choices = StringColumnChoicesProvider.class)
    ColumnFilter m_columnFilter = new ColumnFilter();

    @Widget(title = "Duration type", description = "The type of the duration/period to parse.")
    @ValueSwitchWidget
    DurationPeriodType m_durationType = DurationPeriodType.AUTO_DETECT;

    @Widget(title = "If extraction fails", description = "Action to take if the extraction fails.")
    @ValueSwitchWidget
    ActionIfExtractionFails m_actionIfExtractionFails = ActionIfExtractionFails.SET_MISSING;

    @Widget(title = "Output columns",
        description = "Whether to append a new output column, or replace the input column.")
    @ValueSwitchWidget
    @ValueReference(ReplaceOrAppend.ValueRef.class)
    ReplaceOrAppend m_replaceOrAppend = ReplaceOrAppend.REPLACE;

    @Widget(title = "Output column suffix", description = "The suffix to append to the output column name.")
    @Effect(predicate = ReplaceOrAppend.IsAppend.class, type = EffectType.SHOW)
    String m_appendedSuffix = " (Duration)";

    enum DurationPeriodType {
            @Label(value = "Auto-detected", description = """
                    Automatically detect whether a date-based or a time-based duration should be \
                    parsed. The type will be determined based on the first cell in each column \
                    which can be successfully parsed. Automatically detect whether the input is \
                    date-based or time-based.
                    """)
            AUTO_DETECT(null), //
            @Label(value = "Date-based", description = """
                    All included columns will be converted to date-based duration columns, with \
                    data type "Period".
                    """)
            PERIOD(PeriodCellFactory.TYPE), //
            @Label(value = "Time-based", description = """
                    All included columns will be converted to time-based duration columns, with \
                    data type "Duration".
                    """)
            DURATION(DurationCellFactory.TYPE);

        final DataType m_associatedDataTypeIfApplicable;

        DurationPeriodType(final DataType associatedDataTypeIfApplicable) {
            m_associatedDataTypeIfApplicable = associatedDataTypeIfApplicable;
        }
    }
}
