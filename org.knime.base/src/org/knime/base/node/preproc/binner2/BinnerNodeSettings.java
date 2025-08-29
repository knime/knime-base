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
package org.knime.base.node.preproc.binner2;

import java.util.List;

import org.knime.base.node.preproc.binner2.BinnerNodeSettingsEnums.BinBoundaryExactMatchBehaviour;
import org.knime.base.node.preproc.binner2.BinnerNodeSettingsEnums.BinNaming;
import org.knime.base.node.preproc.binner2.BinnerNodeSettingsEnums.BinningType;
import org.knime.base.node.preproc.binner2.BinnerNodeSettingsEnums.NumberFormatSettingsGroup;
import org.knime.base.node.preproc.binner2.BinnerNodeSettingsPredicates.BinNamesIsNumbered;
import org.knime.base.node.preproc.binner2.BinnerNodeSettingsPredicates.BinningTypeIsCustomCutoffs;
import org.knime.base.node.preproc.binner2.BinnerNodeSettingsPredicates.BinningTypeIsCustomQuantiles;
import org.knime.base.node.preproc.binner2.BinnerNodeSettingsPredicates.BinningTypeIsNotCustomCutoffs;
import org.knime.base.node.preproc.binner2.BinnerNodeSettingsPredicates.NumberOfBinsShouldBeShown;
import org.knime.base.node.preproc.binner2.BinnerNodeSettingsPredicates.ShouldDisplayCustomNumberFormatSettings;
import org.knime.base.node.preproc.binner2.BinnerNodeSettingsPredicates.ShouldShowFixedLowerBoundField;
import org.knime.base.node.preproc.binner2.BinnerNodeSettingsPredicates.ShouldShowFixedUpperBoundField;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.node.parameters.Advanced;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.array.ArrayWidget;
import org.knime.node.parameters.layout.Layout;
import org.knime.node.parameters.layout.Section;
import org.knime.node.parameters.persistence.Persist;
import org.knime.node.parameters.updates.Effect;
import org.knime.node.parameters.updates.Effect.EffectType;
import org.knime.node.parameters.updates.EffectPredicate;
import org.knime.node.parameters.updates.EffectPredicateProvider;
import org.knime.node.parameters.updates.ParameterReference;
import org.knime.node.parameters.updates.ValueReference;
import org.knime.node.parameters.widget.choices.ChoicesProvider;
import org.knime.node.parameters.widget.choices.ColumnChoicesProvider;
import org.knime.node.parameters.widget.choices.Label;
import org.knime.node.parameters.widget.choices.RadioButtonsWidget;
import org.knime.node.parameters.widget.choices.ValueSwitchWidget;
import org.knime.node.parameters.widget.choices.filter.ColumnFilter;
import org.knime.node.parameters.widget.choices.filter.TwinlistWidget;
import org.knime.node.parameters.widget.number.NumberInputWidget;
import org.knime.node.parameters.widget.number.NumberInputWidgetValidation;
import org.knime.node.parameters.widget.number.NumberInputWidgetValidation.MinValidation.IsNonNegativeValidation;
import org.knime.node.parameters.widget.number.NumberInputWidgetValidation.MinValidation.IsPositiveIntegerValidation;
import org.knime.node.parameters.widget.text.TextInputWidget;
import org.knime.node.parameters.widget.text.TextInputWidgetValidation.PatternValidation.IsNotEmptyValidation;

/**
 * Settings for the new webUI binner node.
 *
 * @author David Hickey, TNG Technology Consulting GmbH
 */
@SuppressWarnings("restriction")
final class BinnerNodeSettings implements NodeParameters {

    BinnerNodeSettings() {
    }

    BinnerNodeSettings(final NodeParametersInput context) {
        var spec = context.getInTableSpec(0);

        if (spec.isPresent()) {
            var dataTableSpec = spec.get();
            m_selectedColumns = new ColumnFilter(dataTableSpec.stream() //
                .filter(BinnerNodeSettings::isNumericColumn) //
                .map(DataColumnSpec::getName) //
                .toArray(String[]::new)).withIncludeUnknownColumns();
        }
    }

    @Section(title = "Binning")
    interface BinningSection {
    }

    @Layout(BinningSection.class)
    @Widget(title = "Columns to bin", description = """
            Select the numeric columns to apply binning to. Only the \
            selected columns will be transformed.
            """)
    @TwinlistWidget
    @ChoicesProvider(NumericColumnsProvider.class)
    ColumnFilter m_selectedColumns = new ColumnFilter().withIncludeUnknownColumns();

    @Layout(BinningSection.class)
    @Widget(title = "Binning type", description = "Select the method used to define bin intervals.")
    @RadioButtonsWidget
    @ValueReference(BinningTypeRef.class)
    BinningType m_binningType = BinningType.EQUAL_WIDTH;

    @Layout(BinningSection.class)
    @Widget(title = "Number of bins", description = """
            Specifies the number of bins to create. Used with Equal \
            width and Equal frequency binning.
            """)
    @NumberInputWidget(minValidation = IsPositiveIntegerValidation.class)
    @Effect(predicate = NumberOfBinsShouldBeShown.class, type = EffectType.SHOW)
    int m_numberOfBins = 20;

    @Layout(BinningSection.class)
    @Widget(title = "Custom cutoffs", description = """
            Specifies the exact value at which the bin boundary \
            should be placed.
            """) // this title and description aren't displayed in dialogue, only node desc
    @ArrayWidget(elementTitle = "Cutoff", addButtonText = "New cutoff", showSortButtons = true)
    @Effect(predicate = BinningTypeIsCustomCutoffs.class, type = EffectType.SHOW)
    CustomCutoffs[] m_customCutoffs = new CustomCutoffs[]{ //
        new CustomCutoffs() //
    };

    @Layout(BinningSection.class)
    @Widget(title = "Custom quantiles", description = """
            The bin boundaries may be defined manually \
            by entering the quantiles. The behaviour when a \
            value is exactly equal to a quantile can be \
            configured separately for each quantile.
            """) // this title and description aren't displayed in dialogue, only node desc
    @ArrayWidget(elementTitle = "Quantile", addButtonText = "New quantile", showSortButtons = true)
    @Effect(predicate = BinningTypeIsCustomQuantiles.class, type = EffectType.SHOW)
    CustomQuantilesWidgetGroup[] m_customQuantiles = new CustomQuantilesWidgetGroup[]{ //
        new CustomQuantilesWidgetGroup() //
    };

    @Layout(BinningSection.class)
    @Widget(title = "Enforce integer cutoffs", description = """
            If enabled, bin boundaries will be rounded to the \
            nearest integer. Not available when using custom cutoffs.
            """)
    @Effect(predicate = BinningTypeIsNotCustomCutoffs.class, type = EffectType.SHOW)
    boolean m_enforceIntegerCutoffs;

    @Layout(BinningSection.class)
    @Widget(title = "Fix lower bound", description = """
            If enabled, values below the specified lower bound will \
            be assigned to a dedicated outlier bin. The bin will have \
            the name specified by the 'Lower outlier value' setting.
            """)
    @Effect(predicate = BinningTypeIsNotCustomCutoffs.class, type = EffectType.SHOW)
    @ValueReference(FixLowerBoundRef.class)
    @Persist(configKey = "fixedLowerBound_is_present") // TODO(UIEXT-2850) replace with Optional<Integer>
    boolean m_fixLowerBound;

    @Layout(BinningSection.class)
    @Widget(title = "Lower bound", description = """
            Sets the minimum value for binning. Values below this will \
            be assigned to the lower outlier bin.
            """)
    @Effect(predicate = ShouldShowFixedLowerBoundField.class, type = EffectType.SHOW)
    double m_fixedLowerBound;

    @Layout(BinningSection.class)
    @Widget(title = "Fix upper bound", description = """
            If enabled, values above the specified upper bound will \
            be assigned to a dedicated outlier bin. The bin will have \
            the name specified by the 'Upper outlier value' setting.
            """)
    @Effect(predicate = BinningTypeIsNotCustomCutoffs.class, type = EffectType.SHOW)
    @ValueReference(FixUpperBoundRef.class)
    @Persist(configKey = "fixedUpperBound_is_present") // TODO(UIEXT-2850) replace with Optional<Integer>
    boolean m_fixUpperBound;

    @Layout(BinningSection.class)
    @Widget(title = "Upper bound", description = """
            Sets the maximum value for binning. Values above this will \
            be assigned to the upper outlier bin.
            """)
    @Effect(predicate = ShouldShowFixedUpperBoundField.class, type = EffectType.SHOW)
    double m_fixedUpperBound;

    @Section(title = "Output")
    interface OutputSection {
    }

    @Layout(OutputSection.class)
    @Widget(title = "Bin names/values", description = "Selects how bins are labeled in the output.")
    @RadioButtonsWidget
    @ValueReference(BinNamesRef.class)
    BinNaming m_binNames = BinNaming.NUMBERED;

    @Layout(OutputSection.class)
    @Widget(title = "Prefix", description = """
            Text to prepend to bin labels when using numbered \
            naming (e.g., Bin 1, Bin 2).
            """)
    @Effect(predicate = BinNamesIsNumbered.class, type = EffectType.SHOW)
    String m_prefix = "Bin ";

    @Layout(OutputSection.class)
    @Widget(title = "Lower outlier value", description = "Label assigned to values below the fixed lower bound.")
    String m_lowerOutlierValue = "Lower outlier";

    @Layout(OutputSection.class)
    @Widget(title = "Upper outlier value", description = "Label assigned to values above the fixed upper bound.")
    String m_upperOutlierValue = "Upper outlier";

    @Layout(OutputSection.class)
    @Widget(title = "Number format", advanced = true, description = """
            The format that will be used to display numbers \
            in the bin values in the output table.
            """)
    @ValueSwitchWidget
    @ValueReference(NumberFormat.Ref.class)
    @Effect(predicate = BinNamesIsNumbered.class, type = EffectType.HIDE)
    NumberFormat m_numberFormat = NumberFormat.COLUMN_FORMAT;

    @Layout(OutputSection.class)
    @Effect(predicate = ShouldDisplayCustomNumberFormatSettings.class, type = EffectType.SHOW)
    @Widget(title = "Custom format", description = """
            The exact format for numbers in the bin values, \
            when a custom format is selected.
            """)
    @Advanced
    NumberFormatSettingsGroup m_numberFormatSettings = new NumberFormatSettingsGroup();

    @Layout(OutputSection.class)
    @Widget(title = "Output columns", description = """
            Whether to replace the original columns with the binned \
            columns, or to append the binned columns with new \
            names created by appending the specified suffix to \
            the original column names.
            """)
    @ValueSwitchWidget
    @ValueReference(ReplaceOrAppend.Ref.class)
    ReplaceOrAppend m_replaceOrAppend = ReplaceOrAppend.REPLACE;

    @Layout(OutputSection.class)
    @Widget(title = "Suffix",
        description = "Text appended to column names when new columns are created using the Append option.")
    @Effect(predicate = ReplaceOrAppend.IsAppend.class, type = EffectType.SHOW)
    @TextInputWidget(minLengthValidation = IsNotEmptyValidation.class)
    String m_suffix = " (Binned)";

    private static boolean isNumericColumn(final DataColumnSpec cs) {
        return cs.getType().isCompatible(DoubleValue.class);
    }

    enum NumberFormat {
            COLUMN_FORMAT, //
            CUSTOM;

        static final class Ref implements ParameterReference<NumberFormat> {
        }

        static final class IsCustom implements EffectPredicateProvider {

            @Override
            public EffectPredicate init(final PredicateInitializer i) {
                return i //
                    .getEnum(Ref.class) //
                    .isOneOf(NumberFormat.CUSTOM);
            }
        }
    }

    enum ReplaceOrAppend {
            @Label(value = "Replace", description = "Replaces the original column with the binned result.")
            REPLACE, //
            @Label(value = "Append", description = "Adds a new column with the binned values.")
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

    static final class NumericColumnsProvider implements ColumnChoicesProvider {

        @Override
        public List<DataColumnSpec> columnChoices(final NodeParametersInput context) {
            return context.getInTableSpec(0) //
                .stream() //
                .flatMap(DataTableSpec::stream) //
                .filter(BinnerNodeSettings::isNumericColumn) //
                .toList();
        }
    }

    /* * * * * * * * *
     * References  * *
     * * * * * * * * */

    static final class BinningTypeRef implements ParameterReference<BinningType> {
    }

    static final class FixLowerBoundRef implements ParameterReference<Boolean> {
    }

    static final class FixUpperBoundRef implements ParameterReference<Boolean> {
    }

    static final class BinNamesRef implements ParameterReference<BinNaming> {
    }

    private static final String CUTOFF_DESC = """
            Specifies the exact value at which the bin boundary \
            should be placed.
            """;

    private static final String QUANTILE_DESC = """
            Specifies a quantile value (between 0 and 1) used as \
            a bin boundary.
            """;

    private static final String EXACT_MATCH_DESC = """
            Defines how to assign values that match a cutoff \
            exactly.
            """;

    static class CustomCutoffs implements NodeParameters {
        @Widget(title = "Cutoff", description = CUTOFF_DESC)
        @NumberInputWidget
        double m_cutoff;

        @Widget(title = "Exact match", description = EXACT_MATCH_DESC)
        @ValueSwitchWidget
        BinBoundaryExactMatchBehaviour m_matchType = BinBoundaryExactMatchBehaviour.TO_LOWER_BIN;

        CustomCutoffs() {
            // Default constructor
        }

        /**
         * Constructor used in tests.
         *
         * @param cutoff the cutoff value
         * @param matchType the exact match behaviour
         */
        CustomCutoffs(final double cutoff, final BinBoundaryExactMatchBehaviour matchType) {
            m_cutoff = cutoff;
            m_matchType = matchType;
        }
    }

    static class CustomQuantilesWidgetGroup implements NodeParameters {
        @Widget(title = "Quantile", description = QUANTILE_DESC)
        @NumberInputWidget( //
            minValidation = IsNonNegativeValidation.class, //
            maxValidation = NumberIsAtMostOneValidation.class //
        )
        double m_quantile;

        @Widget(title = "Exact match", description = EXACT_MATCH_DESC)
        @ValueSwitchWidget
        BinBoundaryExactMatchBehaviour m_matchType = BinBoundaryExactMatchBehaviour.TO_LOWER_BIN;

        CustomQuantilesWidgetGroup() {
            // Default constructor
        }

        /**
         * Constructor used in tests.
         *
         * @param quantile the quantile value
         * @param matchType the exact match behaviour
         */
        CustomQuantilesWidgetGroup(final double quantile, final BinBoundaryExactMatchBehaviour matchType) {
            m_quantile = quantile;
            m_matchType = matchType;
        }
    }

    /* * * * * * * * * *
     * Validations * * *
     * * * * * * * * * */
    static final class NumberIsAtMostOneValidation extends NumberInputWidgetValidation.MaxValidation {
        @Override
        protected double getMax() {
            return 1;
        }
    }
}
