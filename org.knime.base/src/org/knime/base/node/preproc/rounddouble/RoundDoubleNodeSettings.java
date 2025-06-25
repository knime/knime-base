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

import static org.knime.core.webui.node.dialog.defaultdialog.util.column.ColumnSelectionUtil.getDoubleColumnsOfFirstPort;

import java.math.RoundingMode;

import org.knime.base.node.preproc.rounddouble.RoundDoubleMigrations.NumberModeMigration;
import org.knime.base.node.preproc.rounddouble.RoundDoubleMigrations.OutputColumnMigration;
import org.knime.base.node.preproc.rounddouble.RoundDoubleMigrations.OutputModeMigration;
import org.knime.base.node.preproc.rounddouble.RoundDoubleMigrations.RoundingMethodMigration;
import org.knime.base.node.preproc.rounddouble.RoundDoubleNodeSettings.RoundingMethod.Standard;
import org.knime.base.node.util.LegacyColumnFilterMigration;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings;
import org.knime.core.webui.node.dialog.defaultdialog.layout.WidgetGroup;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.api.Migration;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.api.Persist;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.api.PersistableSettings;
import org.knime.core.webui.node.dialog.defaultdialog.setting.filter.column.ColumnFilter;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Label;
import org.knime.core.webui.node.dialog.defaultdialog.widget.NumberInputWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.RadioButtonsWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ValueSwitchWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Widget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.choices.ChoicesProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.choices.column.CompatibleColumnsProvider.DoubleColumnsProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.Effect;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.Effect.EffectType;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.Predicate;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.PredicateProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.Reference;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.ValueReference;
import org.knime.core.webui.node.dialog.defaultdialog.widget.validation.NumberInputWidgetValidation.MaxValidation;
import org.knime.core.webui.node.dialog.defaultdialog.widget.validation.NumberInputWidgetValidation.MinValidation.IsNonNegativeValidation;

/**
 * Node settings for the 'Number Rounder' node
 *
 * @author Kai Franze, KNIME GmbH, Germany
 * @since 5.3
 */
@SuppressWarnings("restriction")
public final class RoundDoubleNodeSettings implements DefaultNodeSettings {

    // Types
    enum NumberMode {
            @Label(value = "Decimals", description = """
                    Rounds numeric values up to the specified number of decimal places.
                    """)
            DECIMALS("Decimal places"),

            @Label(value = "Significant digits", description = "Only keeps the specified number of significant digits.")
            SIGNIFICANT_DIGITS("Significant figures"),

            @Label(value = "Integer", description = """
                    Converts numeric values to integer. Note that automatically converting <em>Number (double)</em> or
                    <em>Number (long)</em> input columns to <em>Number (integer)</em> output columns might yield
                    missing values due to integer overflows.
                    """)
            INTEGER;

        private String m_persistKey;

        NumberMode() {
        }

        NumberMode(final String persistKey) {
            m_persistKey = persistKey;
        }

        String getPersistKey() {
            return m_persistKey != null ? m_persistKey : name();
        }

    }

    static final class RoundingMethod implements WidgetGroup, PersistableSettings {

        enum Standard {
                @Label(value = "Standard (.5 away from zero)", description = """
                        This is the standard rounding method. It rounds towards the 'nearest neighbor'. If both
                        neighbors are equidistant, it rounds up.
                        """)
                HALF_AWAY_FROM_ZERO,

                @Label(value = "Others", description = """
                        Provides a number of advanced rounding methods to choose from.
                        """)
                OTHER;
        }

        enum Advanced {
                @Label(value = "Away from zero (Round Up)", description = """
                        Rounds away from zero. Increments the last remaining digit by one, if there were more digits.
                        """)
                AWAY_FROM_ZERO,

                @Label(value = "Towards zero (Round Down)", description = """
                        Rounds towards zero. Drops excess digits.
                        """)
                TOWARDS_ZERO,

                @Label(value = "To larger (Round Ceiling)", description = """
                        Rounds towards positive infinity. If the result is positive, behaves as for
                        'Away from zero (Round Up)'; if negative, behaves as for 'Towards zero (Round Down)'.
                        """)
                TO_LARGER,

                @Label(value = "To smaller (Round Floor)", description = """
                        Rounds towards negative infinity. If the result is positive, behaves as for
                        'Towards zero (Round Down)'; if negative, behaves as for 'Away from zero (Round Up)'.
                        """)
                TO_SMALLER,

                @Label(value = ".5 towards zero", description = """
                        Rounds towards the 'nearest neighbor'. If both neighbors are equidistant, it rounds down.
                        """)
                HALF_TOWARDS_ZERO,

                @Label(value = ".5 to nearest even digit", description = """
                        Rounds towards the 'nearest neighbor'. If both neighbors are equidistant, it rounds towards the
                        'even neighbor’.
                        """)
                HALF_TO_EVEN_DIGIT
        }

        RoundingMethod() {
        }

        RoundingMethod(final Advanced advanced) {
            m_standard = Standard.OTHER;
            m_advanced = advanced;
        }

        interface StandardRef extends Reference<Standard> {
        }

        static final class IsOtherRoundingMethod implements PredicateProvider {
            @Override
            public Predicate init(final PredicateInitializer i) {
                return i.getEnum(StandardRef.class).isOneOf(Standard.OTHER);
            }
        }

        @Widget(title = "Rounding method", description = """
                Select if you want to use the standard rounding method or one of the other available rounding methods.
                """)
        @ValueSwitchWidget
        @ValueReference(StandardRef.class)
        Standard m_standard = Standard.HALF_AWAY_FROM_ZERO;

        @Widget(title = "Other rounding methods", description = "Select the advanced rounding method to apply.")
        @RadioButtonsWidget
        @Effect(predicate = IsOtherRoundingMethod.class, type = EffectType.SHOW)
        Advanced m_advanced = Advanced.AWAY_FROM_ZERO;
    }

    enum OutputColumn {
            @Label(value = "Replace", description = "Replaces the input columns")
            REPLACE,

            @Label(value = "Append with suffix", description = "Appends additional output columns")
            APPEND;
    }

    enum OutputMode {
            @Label(value = "Auto", description = """
                    Sets output column types automatically based on input column types.
                    """)
            AUTO,

            @Label(value = "Double", description = """
                    Sets all output column types to real numbers.
                    """)
            DOUBLE("Double"),

            @Label(value = "Standard string", description = """
                    Returns the string representation of a number using scientific notation if an exponent is needed.
                    """)
            STANDARD_STRING("Standard String"),

            @Label(value = "Plain string", description = """
                    Returns a string representation of a number without an exponent field.
                    """)
            PLAIN_STRING("Plain String (no exponent)"),

            @Label(value = "Engineering string", description = """
                    Returns a string representation of a number, using engineering notation if an exponent is needed.
                    """)
            ENGINEERING_STRING("Engineering String");

        private String m_persistKey;

        OutputMode() {
        }

        OutputMode(final String persistKey) {
            m_persistKey = persistKey;
        }

        String getPersistKey() {
            return m_persistKey != null ? m_persistKey : name();
        }
    }

    // Settings
    @Widget(title = "Columns to round", description = "Select the numeric input columns to round.")
    @ChoicesProvider(DoubleColumnsProvider.class)
    @Migration(ColumnsToFormatMigration.class)
    ColumnFilter m_columnsToFormat;

    static final class ColumnsToFormatMigration extends LegacyColumnFilterMigration {
        ColumnsToFormatMigration() {
            super("StringColNames");
        }
    }

    interface NumberModeRef extends Reference<NumberMode> {
    }

    static final class NumberModeIsInteger implements PredicateProvider {
        @Override
        public Predicate init(final PredicateInitializer i) {
            return i.getEnum(NumberModeRef.class).isOneOf(NumberMode.INTEGER);
        }
    }

    @Widget(title = "Rounding mode", description = "Select the rounding mode to apply.")
    @ValueSwitchWidget
    @ValueReference(NumberModeRef.class)
    @Migration(NumberModeMigration.class)
    NumberMode m_numberModeV2 = NumberMode.DECIMALS;

    static final class PrecisionMaxValidation extends MaxValidation {
        @Override
        protected double getMax() {
            return 350;
        }
    }

    @Widget(title = "Rounding to digits", description = """
            When rounding to <b>Decimals</b>, this sets the number of decimal places to keep.<br/>
            When rounding to <b>Significant digits</b>, this sets the number of significant digits to keep.
            """)
    @NumberInputWidget(minValidation = IsNonNegativeValidation.class, maxValidation = PrecisionMaxValidation.class)
    @Effect(predicate = NumberModeIsInteger.class, type = EffectType.HIDE)
    @Persist(configKey = "PrecisionNumer")
    int m_precision = 3;

    @Migration(RoundingMethodMigration.class)
    RoundingMethod m_roundingMethod = new RoundingMethod();

    interface OutputColumnRef extends Reference<OutputColumn> {
    }

    static final class OutputColumnIsReplace implements PredicateProvider {
        @Override
        public Predicate init(final PredicateInitializer i) {
            return i.getEnum(OutputColumnRef.class).isOneOf(OutputColumn.REPLACE);
        }
    }

    @Widget(title = "Output columns", description = "Configure output column behavior.")
    @ValueSwitchWidget
    @ValueReference(OutputColumnRef.class)
    @Migration(OutputColumnMigration.class)
    OutputColumn m_outputColumn = OutputColumn.APPEND;

    @Widget(title = "Output column suffix", description = "Set the suffix to append to the new column names.")
    @Effect(predicate = OutputColumnIsReplace.class, type = EffectType.HIDE)
    @Persist(configKey = "ColumnSuffix")
    String m_suffix = " (Rounded)";

    @Widget(title = "Output mode (legacy)", advanced = true, description = """
            Determines the formatting of the output columns.
            """)
    @Migration(OutputModeMigration.class)
    OutputMode m_outputMode = OutputMode.AUTO;

    // Utilities

    static RoundingMode getRoundingModeFromMethod(final RoundingMethod roundingMethod) {
        if (roundingMethod.m_standard == Standard.HALF_AWAY_FROM_ZERO) {
            return RoundingMode.HALF_UP;
        }
        return switch (roundingMethod.m_advanced) {
            case AWAY_FROM_ZERO -> RoundingMode.UP;
            case HALF_TO_EVEN_DIGIT -> RoundingMode.HALF_EVEN;
            case HALF_TOWARDS_ZERO -> RoundingMode.HALF_DOWN;
            case TO_LARGER -> RoundingMode.CEILING;
            case TO_SMALLER -> RoundingMode.FLOOR;
            case TOWARDS_ZERO -> RoundingMode.DOWN;
        };
    }

    // Constructors
    RoundDoubleNodeSettings(final DefaultNodeSettingsContext ctx) {
        m_columnsToFormat = new ColumnFilter(getDoubleColumnsOfFirstPort(ctx)).withIncludeUnknownColumns();
    }

    RoundDoubleNodeSettings() {
        // Required by framework for serialization / de-serialization
    }
}
