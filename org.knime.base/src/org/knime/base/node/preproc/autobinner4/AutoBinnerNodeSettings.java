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

import org.knime.base.node.util.binning.AutoBinningSettings.BinBoundaryExactMatchBehaviour;
import org.knime.base.node.util.binning.AutoBinningSettings.BinNaming;
import org.knime.base.node.util.binning.AutoBinningSettings.BinningType;
import org.knime.base.node.util.binning.AutoBinningSettings.NumberFormatSettingsGroup;
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
 * Settings for the new webUI binner node.
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
    @Widget(title = "Columns to bin", description = "Only the included columns will be binned.")
    @TwinlistWidget
    @ChoicesProvider(NumericColumnsProvider.class)
    ColumnFilter m_selectedColumns = new ColumnFilter();

    @Layout(BinningSection.class)
    @Widget(title = "Binning type", description = """
            The algorithm to use when creating the \
            bins, or the bins may be specified manually.
            """)
    @ValueSwitchWidget
    @ValueReference(BinningTypeRef.class)
    BinningType m_binningType = BinningType.EQUAL_WIDTH;

    @Layout(BinningSection.class)
    @Widget(title = "Number of bins", description = "The number of bins to create.")
    @NumberInputWidget(minValidation = NumberOfBinsValidation.class)
    @Effect(predicate = NumberOfBinsShouldBeShown.class, type = EffectType.SHOW)
    int m_numberOfBins = 20;

    @Layout(BinningSection.class)
    @Widget(title = "Custom cutoffs", description = """
            The bin boundaries may be defined manually \
            by entering the values. The behaviour when a \
            value is exactly equal to a cutoff can be \
            configured separately for each boundary.
            """) // this title and description aren't displayed
    @ArrayWidget(elementTitle = "Cutoff", addButtonText = "New cutoff", showSortButtons = true)
    @Effect(predicate = BinningTypeIsCustomCutoffs.class, type = EffectType.SHOW)
    CustomCutoffsWidgetGroup[] m_customCutoffs = new CustomCutoffsWidgetGroup[]{ //
        new CustomCutoffsWidgetGroup() //
    };

    @Layout(BinningSection.class)
    @Widget(title = "Custom quantiles", description = """
            The bin boundaries may be defined manually \
            by entering the quantiles. The behaviour when a \
            value is exactly equal to a quantile can be \
            configured separately for each quantile.
            """) // this title and description aren't displayed
    @ArrayWidget(elementTitle = "Quantile", addButtonText = "New quantile", showSortButtons = true)
    @Effect(predicate = BinningTypeIsCustomQuantiles.class, type = EffectType.SHOW)
    CustomQuantilesWidgetGroup[] m_customQuantiles = new CustomQuantilesWidgetGroup[]{ //
        new CustomQuantilesWidgetGroup() //
    };

    @Layout(BinningSection.class)
    @Widget(title = "Enforce integer cutoffs", description = """
            If enabled, the cutoffs between bins will be \
            rounded to the nearest integer. Not applicable \
            if the cutoffs are defined manually.
            """)
    @Effect(predicate = BinningTypeIsNotCustomCutoffs.class, type = EffectType.SHOW)
    boolean m_enforceIntegerCutoffs = false;

    @Layout(BinningSection.class)
    @Widget(title = "Fix lower bound", description = """
            If enabled, values below the lower bound \
            will be sorted into a special bin with a name \
            specified by the 'Lower outlier value' setting. \
            """)
    @Effect(predicate = BinningTypeIsNotCustom.class, type = EffectType.SHOW)
    @ValueReference(FixLowerBoundRef.class)
    boolean m_fixLowerBound = false;

    @Layout(BinningSection.class)
    @Widget(title = "Lower bound", description = """
            The lower bound below which values will be \
            sorted into a special bin with a name \
            specified by the 'Lower outlier value' setting.
            """)
    @NumberInputWidget(minValidation = NumberGreaterThanZeroValidation.class)
    @Effect(predicate = IsFixedLowerBound.class, type = EffectType.SHOW)
    double m_fixedLowerBound = 0;

    @Layout(BinningSection.class)
    @Widget(title = "Fix upper bound", description = """
            If enabled, values above the upper bound \
            will be sorted into a special bin with a name \
            specified by the 'Upper outlier value' setting. \
            """)
    @Effect(predicate = BinningTypeIsNotCustom.class, type = EffectType.SHOW)
    @ValueReference(FixUpperBoundRef.class)
    boolean m_fixUpperBound = false;

    @Layout(BinningSection.class)
    @Widget(title = "Upper bound", description = """
            The upper bound above which values will be \
            sorted into a special bin with a name \
            specified by the 'Upper outlier value' setting.
            """)
    @NumberInputWidget(minValidation = NumberGreaterThanZeroValidation.class)
    @Effect(predicate = IsFixedUpperBound.class, type = EffectType.SHOW)
    double m_fixedUpperBound = 0;

    @Section(title = "Output")
    interface OutputSection {
    }

    @Layout(OutputSection.class)
    @Widget(title = "Bin names/values", description = """
            The method that will be used when naming \
            the bins.
            """)
    @RadioButtonsWidget
    @ValueReference(BinNamesRef.class)
    BinNaming m_binNames = BinNaming.NUMBERED;

    @Layout(OutputSection.class)
    @Widget(title = "Prefix", description = """
            If the bin names are numbered, this \
            prefix will be prepended to the bin names. \
            For example "Bin 1", "Bin 2", etc. \
            """)
    @Effect(predicate = BinNamesIsNumbered.class, type = EffectType.SHOW)
    String m_prefix = "Bin ";

    @Layout(OutputSection.class)
    @Widget(title = "Lower outlier value", description = """
            Values that are below the lower bound of the \
            lowermost bin will be labeled with this value in \
            place of a bin name.
            """)
    @Effect(predicate = ShouldShowLowerOutlierName.class, type = EffectType.SHOW)
    String m_lowerOutlierValue = "Lower outlier";

    @Layout(OutputSection.class)
    @Widget(title = "Upper outlier value", description = """
            Values that are above the upper bound of the uppermost \
            bin will be labeled with this value in place of a bin \
            name.
            """)
    @Effect(predicate = ShouldShowUpperOutlierName.class, type = EffectType.SHOW)
    String m_upperOutlierValue = "Upper outlier";

    @Layout(OutputSection.class)
    @Widget(title = "Number format", advanced = true, description = """
            The format that will be used to display numbers \
            in the bin values in the output table.
            """)
    @ValueSwitchWidget
    @ValueReference(NumberFormat.Ref.class)
    NumberFormat m_numberFormat = NumberFormat.COLUMN_FORMAT;

    @Layout(OutputSection.class)
    @Effect(predicate = NumberFormat.IsCustom.class, type = EffectType.SHOW)
    @Widget(title = "Custom format", advanced = true, description = """
            The exact format for numbers in the bin values, \
            when a custom format is selected.
            """)
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
    @Widget(title = "Suffix", description = """
            If the binned columns are appended, this \
            suffix will be appended to the original column names.
            """)
    @Effect(predicate = ReplaceOrAppend.IsAppend.class, type = EffectType.SHOW)
    String m_suffix = " (Binned)";

    private static boolean isNumericColumn(final DataColumnSpec cs) {
        return cs.getType().isCompatible(DoubleValue.class);
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
            @Label(value = "Replace", description = "Replace the original columns with the binned columns.")
            REPLACE, //
            @Label(value = "Append", description = """
                    Create new columns with the binned values, \
                    whose names are the original column names \
                    with a suffix appended.
                    """)
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

    /* * * * * * * *
     * Predicates  *
     * * * * * * * */

    static final class NumberOfBinsShouldBeShown implements PredicateProvider {

        @Override
        public Predicate init(final PredicateInitializer i) {
            return i //
                .getEnum(BinningTypeRef.class) //
                .isOneOf(BinningType.CUSTOM_CUTOFFS, BinningType.CUSTOM_QUANTILES) //
                .negate();
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

    static final class BinningTypeIsNotCustom implements PredicateProvider {

        @Override
        public Predicate init(final PredicateInitializer i) {
            return i //
                .getEnum(BinningTypeRef.class) //
                .isOneOf(BinningType.EQUAL_WIDTH, BinningType.EQUAL_FREQUENCY);
        }
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

    static final class ShouldShowUpperOutlierName implements PredicateProvider {
        @Override
        public Predicate init(final PredicateInitializer i) {
            return i //
                .getBoolean(FixUpperBoundRef.class).isTrue() //
                .or(i.getEnum(BinningTypeRef.class).isOneOf(BinningType.CUSTOM_CUTOFFS, BinningType.CUSTOM_QUANTILES));
        }
    }

    static final class ShouldShowLowerOutlierName implements PredicateProvider {
        @Override
        public Predicate init(final PredicateInitializer i) {
            return i //
                .getBoolean(FixLowerBoundRef.class).isTrue() //
                .or(i.getEnum(BinningTypeRef.class).isOneOf(BinningType.CUSTOM_CUTOFFS, BinningType.CUSTOM_QUANTILES));
        }
    }

    static final class BinNamesIsNumbered implements PredicateProvider {

        @Override
        public Predicate init(final PredicateInitializer i) {
            return i //
                .getEnum(BinNamesRef.class) //
                .isOneOf(BinNaming.NUMBERED);
        }
    }

    /* * * * * * * * *
     * References  * *
     * * * * * * * * */

    static final class BinningTypeRef implements Reference<BinningType> {
    }

    static final class FixLowerBoundRef implements Reference<Boolean> {
    }

    static final class FixUpperBoundRef implements Reference<Boolean> {
    }

    static final class BinNamesRef implements Reference<BinNaming> {
    }

    private static final String CUTOFF_DESC = """
            The exact value at which the bin boundary \
            should be placed.
            """;

    private static final String EXACT_MATCH_DESC = """
            The behaviour when a value is exactly equal \
            to the cutoff between two bins.
            """;

    static class CustomCutoffsWidgetGroup implements DefaultNodeSettings {
        @Widget(title = "Cutoff", description = CUTOFF_DESC)
        @NumberInputWidget
        double m_cutoff = 0;

        @Widget(title = "Exact match", description = EXACT_MATCH_DESC)
        @ValueSwitchWidget
        BinBoundaryExactMatchBehaviour m_matchType = BinBoundaryExactMatchBehaviour.TO_LOWER_BIN;
    }

    static class CustomQuantilesWidgetGroup implements DefaultNodeSettings {
        @Widget(title = "Quantile", description = CUTOFF_DESC)
        @NumberInputWidget( //
            minValidation = NumberGreaterThanZeroValidation.class, //
            maxValidation = NumberLessThanOneValidation.class //
        )
        double m_quantile = 0;

        @Widget(title = "Exact match", description = EXACT_MATCH_DESC)
        @ValueSwitchWidget
        BinBoundaryExactMatchBehaviour m_matchType = BinBoundaryExactMatchBehaviour.TO_LOWER_BIN;
    }

    /* * * * * * * * * *
     * Validations * * *
     * * * * * * * * * */

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
}
