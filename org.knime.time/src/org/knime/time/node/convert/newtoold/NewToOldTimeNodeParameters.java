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
 * ------------------------------------------------------------------------
 */

package org.knime.time.node.convert.newtoold;

import static org.knime.time.node.convert.newtoold.NewToOldTimeNodeModel.CFG_COLUMN_FILTER_KEY;
import static org.knime.time.node.convert.newtoold.NewToOldTimeNodeModel.CFG_OUTPUT_COLUMN_KEY;
import static org.knime.time.node.convert.newtoold.NewToOldTimeNodeModel.CFG_TIME_ZONE_POLICY_KEY;
import static org.knime.time.node.convert.newtoold.NewToOldTimeNodeModel.OPTION_APPEND;
import static org.knime.time.node.convert.newtoold.NewToOldTimeNodeModel.OPTION_REPLACE;
import static org.knime.time.node.convert.newtoold.NewToOldTimeNodeModel.TIME_ZONE_OPT1;
import static org.knime.time.node.convert.newtoold.NewToOldTimeNodeModel.TIME_ZONE_OPT2;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.webui.node.dialog.defaultdialog.internal.widget.TypedStringFilterWidgetInternal;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.migration.LoadDefaultsForAbsentFields;
import org.knime.node.parameters.persistence.NodeParametersPersistor;
import org.knime.node.parameters.persistence.Persistor;
import org.knime.node.parameters.persistence.legacy.LegacyColumnFilterPersistor;
import org.knime.node.parameters.updates.Effect;
import org.knime.node.parameters.updates.Effect.EffectType;
import org.knime.node.parameters.updates.EffectPredicate;
import org.knime.node.parameters.updates.EffectPredicateProvider;
import org.knime.node.parameters.updates.ParameterReference;
import org.knime.node.parameters.updates.ValueReference;
import org.knime.node.parameters.widget.choices.ChoicesProvider;
import org.knime.node.parameters.widget.choices.Label;
import org.knime.node.parameters.widget.choices.ValueSwitchWidget;
import org.knime.node.parameters.widget.choices.filter.ColumnFilter;
import org.knime.node.parameters.widget.text.TextInputWidget;
import org.knime.node.parameters.widget.text.TextInputWidgetValidation.PatternValidation.IsNotBlankValidation;
import org.knime.time.util.DateTimeUtils.DateTimeColumnProvider;

/**
 * Node parameters for Date&Time to legacy Date&Time.
 *
 * @author Tim Crundall, TNG Technology Consulting GmbH
 * @author AI Migration Pipeline v1.2
 */
@LoadDefaultsForAbsentFields
class NewToOldTimeNodeParameters implements NodeParameters {

    @SuppressWarnings("restriction")
    @Widget(title = "Column selection", description = "Only the included columns will be converted.")
    @ChoicesProvider(DateTimeColumnProvider.class)
    @TypedStringFilterWidgetInternal(hideTypeFilter = true)
    @Persistor(ColumnFilterPersistor.class)
    ColumnFilter m_columnSelection = new ColumnFilter();

    @Widget(title = "Time zone handling", description = """
            The legacy <b>Date&amp;Time</b> type does not support time zones. When converting values,
            you can choose how the time zone information should be handled:
            """)
    @ValueSwitchWidget
    @Persistor(TimeZonePolicyPersistor.class)
    TimeZonePolicy m_addOffsetOfTimeZone = TimeZonePolicy.DROP;

    private enum TimeZonePolicy {
            @Label(value = "Add as offset", description = """
                    Uses the provided time zone to convert the value to a UTC-based timestamp.
                    <ul>
                        <li><i>Input</i>: 2015-07-09T13:00:00+02:00[Europe/Berlin]</li>
                        <li><i>Output</i>: 09.Jul.2015 15:00:00</li>
                    </ul>
                    """)
            ADD_AS_OFFSET, @Label(value = "Drop", description = """
                    Removes any time zone information from the value.
                    <ul>
                        <li><i>Input</i>: 2015-07-09T13:00:00+02:00[Europe/Berlin]</li>
                        <li><i>Output</i>: 09.Jul.2015 13:00:00</li>
                    </ul>
                    """)
            DROP,
    }

    @Widget(title = "Output columns", description = """
            Whether to replace the original columns with the converted columns, or to append the
            converted columns with new names created by appending the specified suffix to the
            original column names.
            """)
    @ValueSwitchWidget
    @ValueReference(ReplaceOrAppend.Ref.class)
    @Persistor(OutputColumnPersistor.class)
    ReplaceOrAppend m_replaceOrAppend = ReplaceOrAppend.REPLACE;

    @Widget(title = "Suffix",
        description = "Text appended to column names when new columns are created using the Append option.")
    @Effect(predicate = ReplaceOrAppend.IsAppend.class, type = EffectType.SHOW)
    @TextInputWidget(patternValidation = IsNotBlankValidation.class)
    String m_suffix = "(old Date&Time)";

    private enum ReplaceOrAppend {
            @Label(value = "Replace", description = "Replace each input column with the converted dates.")
            REPLACE, //
            @Label(value = "Append with suffix", description = """
                    For each input column append a new column with the converted dates.
                    """)
            APPEND;

        static final class Ref implements ParameterReference<ReplaceOrAppend> {
        }

        static final class IsAppend implements EffectPredicateProvider {

            @Override
            public EffectPredicate init(final PredicateInitializer i) {
                return i //
                    .getEnum(Ref.class) //
                    .isOneOf(ReplaceOrAppend.APPEND);
            }
        }
    }

    @SuppressWarnings("restriction")
    private static final class ColumnFilterPersistor extends LegacyColumnFilterPersistor {
        ColumnFilterPersistor() {
            super(CFG_COLUMN_FILTER_KEY);
        }
    }

    private static final class TimeZonePolicyPersistor implements NodeParametersPersistor<TimeZonePolicy> {

        @Override
        public TimeZonePolicy load(final NodeSettingsRO settings) throws InvalidSettingsException {
            var timeZonePolicy = settings.getString(CFG_TIME_ZONE_POLICY_KEY);
            return switch (timeZonePolicy) {
                case TIME_ZONE_OPT2 -> TimeZonePolicy.DROP;
                case TIME_ZONE_OPT1 -> TimeZonePolicy.ADD_AS_OFFSET;
                default -> throw new InvalidSettingsException(
                    "Unrecognised value for " + CFG_TIME_ZONE_POLICY_KEY + ": " + timeZonePolicy);
            };
        }

        @Override
        public void save(final TimeZonePolicy param, final NodeSettingsWO settings) {
            settings.addString(CFG_TIME_ZONE_POLICY_KEY,
                param == TimeZonePolicy.DROP ? TIME_ZONE_OPT2 : TIME_ZONE_OPT1);
        }

        @Override
        public String[][] getConfigPaths() {
            return new String[][]{{CFG_TIME_ZONE_POLICY_KEY}};
        }
    }

    private static final class OutputColumnPersistor implements NodeParametersPersistor<ReplaceOrAppend> {
        @Override
        public ReplaceOrAppend load(final NodeSettingsRO settings) throws InvalidSettingsException {
            var outputColumnMode = settings.getString(CFG_OUTPUT_COLUMN_KEY);
            return switch (outputColumnMode) {
                case OPTION_APPEND -> ReplaceOrAppend.APPEND;
                case OPTION_REPLACE -> ReplaceOrAppend.REPLACE;
                default -> throw new InvalidSettingsException(
                    "Unrecognised value for " + CFG_OUTPUT_COLUMN_KEY + ": " + outputColumnMode);
            };
        }

        @Override
        public void save(final ReplaceOrAppend param, final NodeSettingsWO settings) {
            settings.addString(CFG_OUTPUT_COLUMN_KEY, param == ReplaceOrAppend.APPEND ? OPTION_APPEND : OPTION_REPLACE);
        }

        @Override
        public String[][] getConfigPaths() {
            return new String[][]{{CFG_OUTPUT_COLUMN_KEY}};
        }
    }
}
