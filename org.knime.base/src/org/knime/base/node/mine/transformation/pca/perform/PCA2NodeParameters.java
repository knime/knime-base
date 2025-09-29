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

package org.knime.base.node.mine.transformation.pca.perform;

import java.util.function.Supplier;

import org.knime.core.webui.node.dialog.defaultdialog.internal.widget.TypedStringFilterWidgetInternal;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.layout.After;
import org.knime.node.parameters.layout.Layout;
import org.knime.node.parameters.layout.Section;
import org.knime.node.parameters.migration.LoadDefaultsForAbsentFields;
import org.knime.node.parameters.persistence.Persist;
import org.knime.node.parameters.persistence.Persistor;
import org.knime.node.parameters.persistence.legacy.EnumBooleanPersistor;
import org.knime.node.parameters.persistence.legacy.LegacyColumnFilterPersistor;
import org.knime.node.parameters.updates.Effect;
import org.knime.node.parameters.updates.Effect.EffectType;
import org.knime.node.parameters.updates.EffectPredicate;
import org.knime.node.parameters.updates.EffectPredicateProvider;
import org.knime.node.parameters.updates.ParameterReference;
import org.knime.node.parameters.updates.StateProvider;
import org.knime.node.parameters.updates.ValueReference;
import org.knime.node.parameters.widget.choices.ChoicesProvider;
import org.knime.node.parameters.widget.choices.Label;
import org.knime.node.parameters.widget.choices.RadioButtonsWidget;
import org.knime.node.parameters.widget.choices.filter.ColumnFilter;
import org.knime.node.parameters.widget.choices.util.ColumnSelectionUtil;
import org.knime.node.parameters.widget.choices.util.CompatibleColumnsProvider.DoubleColumnsProvider;
import org.knime.node.parameters.widget.number.NumberInputWidget;
import org.knime.node.parameters.widget.number.NumberInputWidgetValidation.MaxValidation;
import org.knime.node.parameters.widget.number.NumberInputWidgetValidation.MinValidation.IsPositiveDoubleValidation;
import org.knime.node.parameters.widget.number.NumberInputWidgetValidation.MinValidation.IsPositiveIntegerValidation;

/**
 * Node parameters for PCA.
 *
 * @author Magnus Gohm, KNIME GmbH, Konstanz, Germany
 * @author AI Migration Pipeline v1.1
 */
@SuppressWarnings("restriction")
@LoadDefaultsForAbsentFields
class PCA2NodeParameters implements NodeParameters {

    /** The configuration key for the selection of the fixed number of dimensions to reduce to. */
    private static final String DIMENSION_SELECTED_CFG = "use_fixed_number_dimensions";

    /** The configuration key for the number of dimensions to reduce to. */
    private static final String K_CFG = "number_of_dimensions";

    /** The configuration key for the selection of the minimum information preservation to reduce to. */
    private static final String INFORMATION_PRESERVATION_CFG = "information_preservation";

    /** The configuration key for the used columns. */
    private static final String USED_COLS_CFG = "used_columns";

    /** The configuration key of the remove used columns flag. */
    private static final String REMOVE_USED_COLS_CFG = "remove_used_columns";

    /** The configuration key of the fail on missings flag. */
    private static final String FAIL_ON_MISSING_CFG = "fail_on_missings";

    /**
     * Constructor for persistence and conversion from JSON.
     */
    public PCA2NodeParameters() {
    }

    PCA2NodeParameters(final NodeParametersInput context) {
        this();
        m_columnFilter =
            new ColumnFilter(ColumnSelectionUtil.getDoubleColumnsOfFirstPort(context)).withIncludeUnknownColumns();
    }

    @Section(title = "Target Dimensions")
    interface TargetDimensionsSection {
    }

    @Section(title = "Column Selection")
    @After(value = TargetDimensionsSection.class)
    interface ColumnSelectionSection {
    }

    @Section(title = "Options")
    @After(value = ColumnSelectionSection.class)
    interface OptionsSection {
    }

    @Layout(TargetDimensionsSection.class)
    @Persistor(value = DimensionSelectionPersitor.class)
    @Widget(title = "Target dimensions",
        description = "Select the number of dimensions the input data is projected to.")
    @RadioButtonsWidget
    @ValueReference(value = DimensionSelectionMethodRef.class)
    DimensionSelectionMethod m_dimensionSelectionMethod = DimensionSelectionMethod.FIXED_DIMENSIONS;

    @Layout(TargetDimensionsSection.class)
    @Persist(configKey = K_CFG)
    @Widget(title = "Number of dimensions", description = "The number of dimensions to reduce the data to.")
    @NumberInputWidget(minValidation = IsPositiveIntegerValidation.class,
        maxValidationProvider = NumberOfSelectedColumnsMaxValidation.class)
    @Effect(predicate = NumberOfDimensionsSelected.class, type = EffectType.SHOW)
    int m_numberOfDimensions = 2;

    @Layout(TargetDimensionsSection.class)
    @Persist(configKey = INFORMATION_PRESERVATION_CFG)
    @Widget(title = "Information preservation (%)",
        description = "The minimum percentage of information to preserve in the reduced dimensions.")
    @NumberInputWidget(minValidation = IsPositiveDoubleValidation.class,
        maxValidation = IsLessThanOrEqual100Percent.class)
    @Effect(predicate = MinimumInformationPreservationSelected.class, type = EffectType.SHOW)
    double m_informationPreservation = 100.0;

    @Layout(ColumnSelectionSection.class)
    @Persistor(ColumnSelectionPersitor.class)
    @Widget(title = "Columns",
        description = "Select the columns that are included by the PCA, i.e the original features.")
    @TypedStringFilterWidgetInternal(hideTypeFilter = true)
    @ChoicesProvider(DoubleColumnsProvider.class)
    @ValueReference(value = ColumnFilterRef.class)
    ColumnFilter m_columnFilter = new ColumnFilter();

    @Layout(OptionsSection.class)
    @Widget(title = "Remove original data columns",
        description = "If checked, the columns containing the input data are removed.")
    @Persist(configKey = REMOVE_USED_COLS_CFG)
    boolean m_removeOriginalColumns;

    @Layout(OptionsSection.class)
    @Widget(title = "Fail if missing values are encountered", description = """
            If checked, execution fails, when the selected columns contain missing values. By default, rows
            containing missing values are ignored and not considered during the computation.
            """)
    @Persist(configKey = FAIL_ON_MISSING_CFG)
    boolean m_failOnMissings;

    static final class DimensionSelectionMethodRef implements ParameterReference<DimensionSelectionMethod> {
    }

    static final class ColumnFilterRef implements ParameterReference<ColumnFilter> {
    }

    static class NumberOfDimensionsSelected implements EffectPredicateProvider {
        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getEnum(DimensionSelectionMethodRef.class).isOneOf(DimensionSelectionMethod.FIXED_DIMENSIONS);
        }
    }

    static class MinimumInformationPreservationSelected implements EffectPredicateProvider {
        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getEnum(DimensionSelectionMethodRef.class)
                .isOneOf(DimensionSelectionMethod.INFORMATION_PRESERVATION);
        }
    }

    static final class IsLessThanOrEqual100Percent extends MaxValidation {

        @Override
        public double getMax() {
            return 100;
        }

    }

    static final class NumberOfSelectedColumnsMaxValidation implements StateProvider<MaxValidation> {

        private Supplier<ColumnFilter> m_columnFilterSupplier;

        @Override
        public void init(final StateProviderInitializer i) {
            m_columnFilterSupplier = i.computeFromValueSupplier(ColumnFilterRef.class);
            i.computeBeforeOpenDialog();
        }

        @Override
        public MaxValidation computeState(final NodeParametersInput context) {
            final var numericColumns = ColumnSelectionUtil.getDoubleColumnsOfFirstPort(context);
            final var max = m_columnFilterSupplier.get().filter(numericColumns).length;

            return new MaxValidation() {
                @Override
                protected double getMax() {
                    return max;
                }

                @Override
                public String getErrorMessage() {
                    if (max == 0) {
                        return "No columns available.";
                    }
                    if (max == 1) {
                        return "Only 1 column available.";
                    }
                    return String.format("Only %d columns available.", max);
                }
            };
        }

    }

    static final class DimensionSelectionPersitor extends EnumBooleanPersistor<DimensionSelectionMethod> {

        protected DimensionSelectionPersitor() {
            super(DIMENSION_SELECTED_CFG, DimensionSelectionMethod.class, DimensionSelectionMethod.FIXED_DIMENSIONS);
        }

    }

    static final class ColumnSelectionPersitor extends LegacyColumnFilterPersistor {
        ColumnSelectionPersitor() {
            super(USED_COLS_CFG);
        }
    }

    /**
     * Target dimension selection method.
     */
    enum DimensionSelectionMethod {
            @Label(value = "Dimension(s) to reduce to", description = "Select a fixed number of target dimensions. "
                + "It must be lower than or equal to the number of input columns.")
            FIXED_DIMENSIONS, @Label(value = "Minimum information fraction",
                description = "Specify the minimal amount of information to be preserved.")
            INFORMATION_PRESERVATION
    }

}
