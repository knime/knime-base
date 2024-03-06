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
 *   Jan 22, 2024 (kai): created
 */
package org.knime.base.node.preproc.rounddouble;

import java.math.RoundingMode;
import java.util.stream.Stream;

import org.knime.base.node.preproc.rounddouble.RoundDoublePersistors.NumberModePersistor;
import org.knime.base.node.preproc.rounddouble.RoundDoublePersistors.OutputColumnPersistor;
import org.knime.base.node.preproc.rounddouble.RoundDoublePersistors.OutputModePersistor;
import org.knime.base.node.preproc.rounddouble.RoundDoublePersistors.RoundingMethodPersistor;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings;
import org.knime.core.webui.node.dialog.defaultdialog.layout.After;
import org.knime.core.webui.node.dialog.defaultdialog.layout.Layout;
import org.knime.core.webui.node.dialog.defaultdialog.layout.Section;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.field.Persist;
import org.knime.core.webui.node.dialog.defaultdialog.rule.Effect;
import org.knime.core.webui.node.dialog.defaultdialog.rule.Effect.EffectType;
import org.knime.core.webui.node.dialog.defaultdialog.rule.OneOfEnumCondition;
import org.knime.core.webui.node.dialog.defaultdialog.rule.Signal;
import org.knime.core.webui.node.dialog.defaultdialog.setting.columnfilter.ColumnFilter;
import org.knime.core.webui.node.dialog.defaultdialog.setting.columnfilter.LegacyColumnFilterPersistor;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ChoicesWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ColumnChoicesProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Label;
import org.knime.core.webui.node.dialog.defaultdialog.widget.NumberInputWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.RadioButtonsWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.TextInputWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ValueSwitchWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Widget;

/**
 * Node settings for the 'Number Rounder' node
 *
 * @author Kai Franze, KNIME GmbH, Germany
 * @since 5.3
 */
@SuppressWarnings("restriction")
public final class RoundDoubleNodeSettings implements DefaultNodeSettings {

    // Layout
    interface InputSelection {

    }

    @After(InputSelection.class)
    interface Rounding {

        interface Format {

        }

        @After(Format.class)
        interface Mode {

        }

    }

    @After(Rounding.class)
    interface Output {

        interface Columns {

        }

        @After(Columns.class)
        @Section(title = "Advanced settings", advanced = true)
        interface Advanced {

        }

    }

    // Types
    enum NumberMode {
            @Label(value = "Decimals",
                description = "Rounds numeric values up to the specified number of decimal places.")
            DECIMALS,

            @Label(value = "Significant digits", description = "Only keeps the specified number of significant digits.")
            SIGNIFICANT_DIGITS,

            @Label(value = "Integer", description = "Converts numeric values to integer.")
            INTEGER;

        static class IsInteger extends OneOfEnumCondition<NumberMode> {

            @Override
            public NumberMode[] oneOf() {
                return new NumberMode[]{INTEGER};
            }

        }
    }

    enum RoundingMethod { // This will be mapped to 'java.math.RoundingMode'.
            @Label(value = ".5 to even digit", description = """
                    Rounds towards the 'nearest neighbor' unless both neighbors are equidistant,
                    in which case, round towards the even neighbor.
                    """)
            HALF_TO_EVEN_DIGIT,

            @Label(value = ".5 towards zero", description = """
                    Rounds towards 'nearest neighbor' unless both neighbors are equidistant,
                    in which case round down.
                    """)
            HALF_TOWARDS_ZERO,

            @Label(value = ".5 away from zero", description = """
                    Rounds towards 'nearest neighbor' unless both neighbors are equidistant,
                    in which case round up.
                    """)
            HALF_AWAY_FROM_ZERO,

            @Label(value = "towards zero", description = """
                    Rounds towards zero.
                    Never increments the digit prior to a discarded fraction (i.e., truncates).
                    """)
            TOWARDS_ZERO,

            @Label(value = "away from zero", description = """
                    Rounds away from zero.
                    Always increments the digit prior to a non-zero discarded fraction.
                    """)
            AWAY_FROM_ZERO,

            @Label(value = "to larger", description = """
                    Rounds towards positive infinity.
                    If the result is positive, behaves as for 'away from zero';
                    if negative, behaves as for 'towards zero'.
                    """)
            TO_LARGER,

            @Label(value = "to smaller", description = """
                    Rounds towards negative infinity.
                    If the result is positive, behave as for 'towards zero';
                    if negative, behave as for 'away from zero'.
                    """)
            TO_SMALLER
    }

    enum OutputColumn {
            @Label(value = "Replace", description = "Replaces the input columns")
            REPLACE,

            @Label(value = "Append with suffix", description = "Appends additional output columns")
            APPEND;

        static class IsReplace extends OneOfEnumCondition<OutputColumn> {

            @Override
            public OutputColumn[] oneOf() {
                return new OutputColumn[]{REPLACE};
            }

        }
    }

    enum OutputMode {
            @Label(value = "Auto", description = "Sets output column types automatically based on input column types.")
            AUTO,

            @Label(value = "Double", description = "Sets all output column types to real numbers.")
            DOUBLE,

            @Label(value = "Standard string", description = """
                    Returns the string representation of a number using scientific notation if an exponent is needed.
                    """)
            STANDARD_STRING,

            @Label(value = "Plain string", description = """
                    Returns a string representation of a number without an exponent field.
                    """)
            PLAIN_STRING,

            @Label(value = "Engineering string", description = """
                    Returns a string representation of a number, using engineering notation if an exponent is needed.
                    """)
            ENGINEERING_STRING
    }

    // Settings
    @Widget(title = "Columns to round", description = "Select the numeric input columns to round.")
    @ChoicesWidget(choices = NumberColumns.class)
    @Layout(InputSelection.class)
    @Persist(configKey = "StringColNames", customPersistor = LegacyColumnFilterPersistor.class)
    ColumnFilter m_columnsToFormat;

    @Widget(title = "Rounding mode", description = "Select the rounding mode to apply.")
    @ValueSwitchWidget
    @Layout(Rounding.Format.class)
    @Signal(condition = NumberMode.IsInteger.class)
    @Persist(configKey = RoundDoubleLegacyConfigKeys.NUMBER_MODE, customPersistor = NumberModePersistor.class)
    NumberMode m_numberMode = NumberMode.DECIMALS;

    @Widget(title = "Rounding to digits", description = """
            When rounding to <b>Decimals</b>, this sets the number of decimal places to keep.<br/>
            When rounding to <b>Significant digits</b>, this sets the number of significant digits to keep.
            """)
    @NumberInputWidget(min = 0, max = 350)
    @Layout(Rounding.Format.class)
    @Effect(signals = NumberMode.IsInteger.class, type = EffectType.HIDE)
    @Persist(configKey = RoundDoubleLegacyConfigKeys.PRECISION_NUMBER)
    int m_precision = 3;

    @Widget(title = "Rounding method", description = "Select the rounding method to apply.")
    @RadioButtonsWidget
    @Layout(Rounding.Mode.class)
    @Persist(configKey = RoundDoubleLegacyConfigKeys.ROUNDING_MODE, customPersistor = RoundingMethodPersistor.class)
    RoundingMethod m_roundingMethod = RoundingMethod.HALF_AWAY_FROM_ZERO;

    @Widget(title = "Output columns", description = "Configure output column behavior.")
    @ValueSwitchWidget
    @Layout(Output.Columns.class)
    @Signal(condition = OutputColumn.IsReplace.class)
    @Persist(configKey = RoundDoubleLegacyConfigKeys.APPEND_COLUMNS, customPersistor = OutputColumnPersistor.class)
    OutputColumn m_outputColumn = OutputColumn.APPEND;

    @Widget(title = "Output column suffix", description = "Set the suffix to append to the new column names.")
    @TextInputWidget
    @Layout(Output.Columns.class)
    @Effect(signals = OutputColumn.IsReplace.class, type = EffectType.HIDE)
    @Persist(configKey = RoundDoubleLegacyConfigKeys.COLUMN_SUFFIX)
    String m_suffix = " (Rounded)";

    @Widget(title = "Output mode (legacy)", description = "Determines the formatting of the output columns.")
    @Layout(Output.Advanced.class)
    @Persist(configKey = RoundDoubleLegacyConfigKeys.OUTPUT_TYPE, customPersistor = OutputModePersistor.class)
    OutputMode m_outputMode = OutputMode.AUTO;

    // Utilities
    static final class NumberColumns implements ColumnChoicesProvider {

        @Override
        public DataColumnSpec[] columnChoices(final DefaultNodeSettingsContext context) {
            return context.getDataTableSpec(0)//
                .map(DataTableSpec::stream)//
                .orElseGet(Stream::empty)//
                .filter(RoundDoubleNodeModel::isTargetColumn)//
                .toArray(DataColumnSpec[]::new);
        }

    }

    static RoundingMode getRoundingModeFromMethod(final RoundingMethod roundingMethod) {
        return switch (roundingMethod) {
            case AWAY_FROM_ZERO -> RoundingMode.UP;
            case HALF_AWAY_FROM_ZERO -> RoundingMode.HALF_UP;
            case HALF_TO_EVEN_DIGIT -> RoundingMode.HALF_EVEN;
            case HALF_TOWARDS_ZERO -> RoundingMode.HALF_DOWN;
            case TO_LARGER -> RoundingMode.CEILING;
            case TO_SMALLER -> RoundingMode.FLOOR;
            case TOWARDS_ZERO -> RoundingMode.DOWN;
        };
    }

    // Constructors
    RoundDoubleNodeSettings(final DefaultNodeSettingsContext ctx) {
        m_columnsToFormat = ColumnFilter.createDefault(NumberColumns.class, ctx);
    }

    RoundDoubleNodeSettings() {
        // Required by framework for serialization / de-serialization
    }

}
