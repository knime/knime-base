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
 *   Apr 16, 2025 (david): created
 */
package org.knime.base.node.preproc.autobinner4;

import java.util.List;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings;
import org.knime.core.webui.node.dialog.defaultdialog.layout.Layout;
import org.knime.core.webui.node.dialog.defaultdialog.layout.Section;
import org.knime.core.webui.node.dialog.defaultdialog.setting.filter.column.ColumnFilter;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ArrayWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Label;
import org.knime.core.webui.node.dialog.defaultdialog.widget.NumberInputWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.RadioButtonsWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.TwinlistWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ValueSwitchWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Widget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.choices.ChoicesProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.choices.column.ColumnChoicesProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.Effect;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.Effect.EffectType;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.Predicate;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.PredicateProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.Reference;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.ValueReference;
import org.knime.core.webui.node.dialog.defaultdialog.widget.validation.NumberInputWidgetValidation;

/**
 *
 * @author David Hickey, TNG Technology Consulting GmbH
 */
@SuppressWarnings("restriction")
final class AutoBinnerNodeSettings implements DefaultNodeSettings {

    AutoBinnerNodeSettings() {
    }

    AutoBinnerNodeSettings(final DefaultNodeSettingsContext context) {
        var spec = context.getDataTableSpec(0);

        if (spec.isPresent()) {
            var dataTableSpec = spec.get();
            m_selectedColumns = new ColumnFilter(dataTableSpec.stream() //
                .filter(AutoBinnerNodeSettings::isNumericColumn) //
                .map(DataColumnSpec::getName) //
                .toArray(String[]::new));
        }
    }

    @Section(title = "Binning")
    interface BinningSection {
    }

    @Layout(BinningSection.class)
    @Widget(title = "Columns to bin", description = "TODO")
    @TwinlistWidget
    @ChoicesProvider(NumericColumnsProvider.class)
    ColumnFilter m_selectedColumns = new ColumnFilter();

    @Layout(BinningSection.class)
    @Widget(title = "Binning type", description = "TODO")
    @ValueSwitchWidget
    @ValueReference(BinningTypeRef.class)
    BinningType m_binningType = BinningType.EQUAL_WIDTH;

    @Layout(BinningSection.class)
    @Widget(title = "Number of bins", description = "TODO")
    @NumberInputWidget(minValidation = NumberOfBinsValidation.class)
    @Effect(predicate = NumberOfBinsShouldBeShown.class, type = EffectType.SHOW)
    int m_numberOfBins = 20;

    @Layout(BinningSection.class)
    @Widget(title = "", description = "")
    @ArrayWidget(elementTitle = "Cutoff", addButtonText = "New cutoff", showSortButtons = true)
    @Effect(predicate = BinningTypeIsCustomCutoffs.class, type = EffectType.SHOW)
    CustomCutoffsWidgetGroup[] m_customCutoffs = new CustomCutoffsWidgetGroup[]{ //
        new CustomCutoffsWidgetGroup() //
    };

    @Layout(BinningSection.class)
    @Widget(title = "", description = "")
    @ArrayWidget(elementTitle = "Quantile", addButtonText = "New quantile", showSortButtons = true)
    @Effect(predicate = BinningTypeIsCustomQuantiles.class, type = EffectType.SHOW)
    CustomQuantilesWidgetGroup[] m_customQuantiles = new CustomQuantilesWidgetGroup[]{ //
        new CustomQuantilesWidgetGroup() //
    };

    @Layout(BinningSection.class)
    @Widget(title = "Enforce integer cutoffs", description = "TODO")
    @Effect(predicate = BinningTypeIsNotCustomCutoffs.class, type = EffectType.SHOW)
    boolean m_enforceIntegerCutoffs = false;

    @Layout(BinningSection.class)
    @Widget(title = "Fix lower bound", description = "TODO")
    @Effect(predicate = BinningTypeIsCustomCutoffs.class, type = EffectType.SHOW)
    @ValueReference(FixLowerBoundRef.class)
    boolean m_fixLowerBound = false;

    @Layout(BinningSection.class)
    @Widget(title = "Lower bound", description = "TODO")
    @NumberInputWidget(minValidation = NumberGreaterThanZeroValidation.class)
    @Effect(predicate = IsFixedLowerBound.class, type = EffectType.SHOW)
    double m_fixedLowerBound = 0;

    @Layout(BinningSection.class)
    @Widget(title = "Fix upper bound", description = "TODO")
    @Effect(predicate = BinningTypeIsCustomCutoffs.class, type = EffectType.SHOW)
    @ValueReference(FixUpperBoundRef.class)
    boolean m_fixUpperBound = false;

    @Layout(BinningSection.class)
    @Widget(title = "Upper bound", description = "TODO")
    @NumberInputWidget(minValidation = NumberGreaterThanZeroValidation.class)
    @Effect(predicate = IsFixedUpperBound.class, type = EffectType.SHOW)
    double m_fixedUpperBound = 0;

    @Section(title = "Output")
    interface OutputSection {
    }

    @Layout(OutputSection.class)
    @Widget(title = "Bin names/values", description = "TODO")
    @RadioButtonsWidget
    @ValueReference(BinNamesRef.class)
    BinNames m_binNames = BinNames.NUMBERED;

    @Layout(OutputSection.class)
    @Widget(title = "Prefix", description = "TODO")
    @Effect(predicate = BinNamesIsNumbered.class, type = EffectType.SHOW)
    String m_prefix = "Bin ";

    @Layout(OutputSection.class)
    @Widget(title = "Lower outlier value", description = "TODO")
    @Effect(predicate = IsFixedLowerBound.class, type = EffectType.SHOW)
    String m_lowerOutlierValue = "Lower outlier";

    @Layout(OutputSection.class)
    @Widget(title = "Upper outlier value", description = "TODO")
    @Effect(predicate = IsFixedUpperBound.class, type = EffectType.SHOW)
    String m_upperOutlierValue = "Upper outlier";

    @Layout(OutputSection.class)
    @Widget(title = "Number format", description = "TODO", advanced = true)
    @ValueSwitchWidget
    @ValueReference(NumberFormat.Ref.class)
    NumberFormat m_numberFormat = NumberFormat.COLUMN_FORMAT;

    @Layout(OutputSection.class)
    @Effect(predicate = NumberFormat.IsCustom.class, type = EffectType.SHOW)
    @Widget(title = "Custom format", description = "TODO", advanced = true)
    NumberFormatSettingsGroup m_numberFormatSettings = new NumberFormatSettingsGroup();

    @Layout(OutputSection.class)
    @Widget(title = "Output columns", description = "TODO")
    @ValueSwitchWidget
    @ValueReference(ReplaceOrAppend.Ref.class)
    ReplaceOrAppend m_replaceOrAppend = ReplaceOrAppend.REPLACE;

    @Layout(OutputSection.class)
    @Widget(title = "Suffix", description = "TODO")
    @Effect(predicate = ReplaceOrAppend.IsAppend.class, type = EffectType.SHOW)
    String m_suffix = " (Binned)";

    private static boolean isNumericColumn(final DataColumnSpec cs) {
        return cs.getType().isCompatible(DoubleValue.class);
    }

    enum BinningType {
            @Label("Equal width")
            EQUAL_WIDTH, //
            @Label("Equal frequency")
            EQUAL_FREQUENCY, //
            @Label("Custom cutoffs")
            CUSTOM_CUTOFFS, //
            @Label("Custom quantiles")
            CUSTOM_QUANTILES;
    }

    enum MatchType {
            @Label(value = "To lower bin",
                description = "Values that fall on the bin border will be assigned to the lower bin")
            TO_LOWER_BIN, //
            @Label(value = "To upper bin",
                description = "Values that fall on the bin border will be assigned to the upper bin")
            TO_UPPER_BIN;
    }

    enum BinNames {
            @Label(value = "Numbered", description = "Bins will be named by their number, e.g. Bin 1")
            NUMBERED, //
            @Label(value = "Borders", description = "Bins will be named by their borders, e.g. [0.0, 1.0)")
            BORDERS, //
            @Label(value = "Midpoints", description = "Bins will be named by their midpoints, e.g. 0.5")
            MIDPOINTS;

        String computedName(final int index, final boolean lowerBoundOpen, final double lowerBound,
            final boolean upperBoundOpen, final double upperBound) {
            return switch (this) {
                case NUMBERED -> "Bin " + (index + 1);
                case BORDERS -> openChar(lowerBoundOpen) + lowerBound + ", " + upperBound + closeChar(upperBoundOpen);
                case MIDPOINTS -> String.valueOf((lowerBound + upperBound) / 2);
            };
        }

        private static String openChar(final boolean open) {
            return open ? "(" : "[";
        }

        private static String closeChar(final boolean open) {
            return open ? ")" : "]";
        }
    }

    enum NumberFormat {
            COLUMN_FORMAT, //
            CUSTOM;

        final static class Ref implements Reference<NumberFormat> {
        }

        final static class IsCustom implements PredicateProvider {

            @Override
            public Predicate init(final PredicateInitializer i) {
                return i //
                    .getEnum(Ref.class) //
                    .isOneOf(NumberFormat.CUSTOM);
            }
        }
    }

    enum ReplaceOrAppend {
            @Label("Replace")
            REPLACE, //
            @Label("Append")
            APPEND;

        final static class Ref implements Reference<ReplaceOrAppend> {
        }

        final static class IsAppend implements PredicateProvider {

            @Override
            public Predicate init(final PredicateInitializer i) {
                return i //
                    .getEnum(Ref.class) //
                    .isOneOf(ReplaceOrAppend.APPEND);
            }
        }
    }

    static final class NumericColumnsProvider implements ColumnChoicesProvider {

        @Override
        public List<DataColumnSpec> columnChoices(final DefaultNodeSettingsContext context) {
            return context.getDataTableSpec(0) //
                .stream() //
                .flatMap(DataTableSpec::stream) //
                .filter(AutoBinnerNodeSettings::isNumericColumn) //
                .toList();
        }
    }

    static final class BinningTypeRef implements Reference<BinningType> {
    }

    static final class NumberOfBinsValidation extends NumberInputWidgetValidation.MinValidation {

        @Override
        protected double getMin() {
            return 2;
        }
    }

    static final class NumberGreaterThanZeroValidation extends NumberInputWidgetValidation.MinValidation {

        @Override
        protected double getMin() {
            return 0;
        }
    }

    static final class NumberLessThanOneValidation extends NumberInputWidgetValidation.MaxValidation {

        @Override
        protected double getMax() {
            return 1;
        }
    }

    static final class NumberOfBinsShouldBeShown implements PredicateProvider {

        @Override
        public Predicate init(final PredicateInitializer i) {
            return i //
                .getEnum(BinningTypeRef.class) //
                .isOneOf(BinningType.CUSTOM_CUTOFFS, BinningType.CUSTOM_QUANTILES);
        }
    }

    static final class BinningTypeIsCustomCutoffs implements PredicateProvider {

        @Override
        public Predicate init(final PredicateInitializer i) {
            return i //
                .getEnum(BinningTypeRef.class) //
                .isOneOf(BinningType.CUSTOM_CUTOFFS);
        }
    }

    static final class BinningTypeIsCustomQuantiles implements PredicateProvider {

        @Override
        public Predicate init(final PredicateInitializer i) {
            return i //
                .getEnum(BinningTypeRef.class) //
                .isOneOf(BinningType.CUSTOM_QUANTILES);
        }
    }

    static final class BinningTypeIsNotCustomCutoffs implements PredicateProvider {

        @Override
        public Predicate init(final PredicateInitializer i) {
            return i //
                .getPredicate(BinningTypeIsCustomCutoffs.class) //
                .negate();
        }
    }

    static final class FixLowerBoundRef implements Reference<Boolean> {
    }

    static final class FixUpperBoundRef implements Reference<Boolean> {
    }

    static final class IsFixedLowerBound implements PredicateProvider {

        @Override
        public Predicate init(final PredicateInitializer i) {
            return i //
                .getBoolean(FixLowerBoundRef.class) //
                .isTrue();
        }
    }

    static final class IsFixedUpperBound implements PredicateProvider {

        @Override
        public Predicate init(final PredicateInitializer i) {
            return i //
                .getBoolean(FixUpperBoundRef.class) //
                .isTrue();
        }
    }

    static final class BinNamesRef implements Reference<BinNames> {
    }

    static final class BinNamesIsNumbered implements PredicateProvider {

        @Override
        public Predicate init(final PredicateInitializer i) {
            return i //
                .getEnum(BinNamesRef.class) //
                .isOneOf(BinNames.NUMBERED);
        }
    }

    static final class NumberFormatSettingsGroup implements DefaultNodeSettings {

        @Widget(title = "Number format", description = "TODO")
        @ValueSwitchWidget
        NumberFormat m_numberFormat = NumberFormat.STANDARD_STRING;

        @Widget(title = "Precision", description = "TODO")
        @NumberInputWidget(minValidation = NumberGreaterThanZeroValidation.class)
        int m_precision = 3;

        @Widget(title = "Precision mode", description = "TODO")
        @ValueSwitchWidget
        PrecisionMode m_precisionMode = PrecisionMode.DECIMAL_PLACES;

        @Widget(title = "Rounding mode", description = "TODO")
        RoundingMode m_roundingMode = RoundingMode.UP;

        enum NumberFormat {
                STANDARD_STRING, //
                PLAIN_STRING, //
                ENGINEERING_STRING;
        }

        enum PrecisionMode {
                DECIMAL_PLACES, //
                SIGNIFICANT_FIGURES;
        }

        enum RoundingMode {
                UP, //
                DOWN, //
                CEILING, //
                FLOOR, //
                HALF_UP, //
                HALF_DOWN, //
                HALF_EVEN;
        }
    }

    static class CustomCutoffsWidgetGroup implements DefaultNodeSettings {
        @Widget(title = "Cutoff", description = "TODO")
        @NumberInputWidget
        double m_cutoff = 0;

        @Widget(title = "Exact match", description = "TODO")
        @ValueSwitchWidget
        MatchType m_matchType = MatchType.TO_LOWER_BIN;
    }

    static class CustomQuantilesWidgetGroup implements DefaultNodeSettings {
        @Widget(title = "Quantile", description = "TODO")
        @NumberInputWidget( //
            minValidation = NumberGreaterThanZeroValidation.class, //
            maxValidation = NumberLessThanOneValidation.class //
        )
        double m_quantile = 0;

        @Widget(title = "Exact match", description = "TODO")
        @ValueSwitchWidget
        MatchType m_matchType = MatchType.TO_LOWER_BIN;
    }
}
