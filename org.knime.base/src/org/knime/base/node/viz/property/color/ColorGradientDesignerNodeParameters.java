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
 *   28 Nov 2025 (Robin Gerling): created
 */
package org.knime.base.node.viz.property.color;

import java.awt.Color;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.IntStream;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.property.ColorGradient;
import org.knime.core.data.property.ColorGradientDefinitionUtil;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.webui.node.dialog.defaultdialog.util.updates.StateComputationFailureException;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.array.ArrayWidget;
import org.knime.node.parameters.array.ArrayWidget.ElementLayout;
import org.knime.node.parameters.layout.After;
import org.knime.node.parameters.layout.Before;
import org.knime.node.parameters.layout.Layout;
import org.knime.node.parameters.layout.Section;
import org.knime.node.parameters.updates.Effect;
import org.knime.node.parameters.updates.Effect.EffectType;
import org.knime.node.parameters.updates.EffectPredicate;
import org.knime.node.parameters.updates.EffectPredicateProvider;
import org.knime.node.parameters.updates.ParameterReference;
import org.knime.node.parameters.updates.StateProvider;
import org.knime.node.parameters.updates.ValueProvider;
import org.knime.node.parameters.updates.ValueReference;
import org.knime.node.parameters.widget.choices.ChoicesProvider;
import org.knime.node.parameters.widget.choices.ColumnChoicesProvider;
import org.knime.node.parameters.widget.choices.Label;
import org.knime.node.parameters.widget.choices.ValueSwitchWidget;
import org.knime.node.parameters.widget.choices.filter.ColumnFilter;
import org.knime.node.parameters.widget.choices.util.ColumnSelectionUtil;
import org.knime.node.parameters.widget.number.NumberInputWidget;
import org.knime.node.parameters.widget.number.NumberInputWidgetValidation;
import org.knime.node.parameters.widget.number.NumberInputWidgetValidation.MinValidation.IsNonNegativeValidation;
import org.knime.node.parameters.widget.text.TextInputWidget;
import org.knime.node.parameters.widget.text.TextInputWidgetValidation.PatternValidation.IsNotBlankValidation;

/**
 * The node parameters for the {@link ColorGradientDesignerNodeFactory}.
 *
 * @author Robin Gerling, KNIME GmbH, Konstanz, Germany
 */
final class ColorGradientDesignerNodeParameters implements NodeParameters {

    private static final StopValueColor[] DEFAULT_STOP_VALUE_COLORS =
        new StopValueColor[]{new StopValueColor(0, "#FF0000"), new StopValueColor(100, "#0000FF")};

    public ColorGradientDesignerNodeParameters() {
        // default constructor
    }

    public ColorGradientDesignerNodeParameters(final NodeParametersInput nodeParametersInput) {
        if (nodeParametersInput.getInPortTypes().length == 0) {
            // No table input port added.
            return;
        }
        nodeParametersInput.getInTableSpec(0).ifPresent(spec -> {
            final var doubleColumns = ColumnSelectionUtil.getDoubleColumnsOfFirstPort(nodeParametersInput);
            m_columnFilter = new ColumnFilter(doubleColumns);
        });
    }

    @Effect(predicate = HasTablePort.class, type = EffectType.SHOW)
    @Before(ColorGradientSection.class)
    @Section(title = "Values")
    interface ValuesSection {
    }

    @After(ValuesSection.class)
    @Before(SpecialColorsSection.class)
    @Section(title = "Color Gradient")
    interface ColorGradientSection {
    }

    @After(ColorGradientSection.class)
    @Section(title = "Special Colors")
    interface SpecialColorsSection {
    }

    @Widget(title = "Numeric columns",
        description = "Specify one or more numeric columns whose values will be mapped to a color gradient."
            + " The same gradient configuration will be applied to all selected columns.")
    @ChoicesProvider(NumericColumnChoicesProvider.class)
    @Layout(ValuesSection.class)
    ColumnFilter m_columnFilter = new ColumnFilter();

    @Widget(title = "Gradient",
        description = "Choose a predefined gradient (e.g., Viridis, Cividis) or define a custom gradient"
            + " by specifying individual color stops.")
    @ValueReference(BaseGradientReference.class)
    @Layout(ColorGradientSection.class)
    ColorGradientWrapper m_gradient = ColorGradientWrapper.CUSTOM;

    @Widget(title = "Value scale", description = "Select how values should be scaled across the gradient range:")
    @Layout(ColorGradientSection.class)
    @ValueReference(ValueScaleReference.class)
    @Effect(predicate = IsCustomGradient.class, type = EffectType.SHOW)
    @ValueSwitchWidget
    ValueScale m_valueScale = ValueScale.PERCENTAGE;

    @Widget(title = "Custom gradient",
        description = "Define your own gradient using one or more color stops."
            + " Each stop consists of a value and a color.")
    @ArrayWidget(addButtonText = "Add color", elementLayout = ElementLayout.HORIZONTAL_SINGLE_LINE,
        showSortButtons = true)
    @Effect(predicate = IsCustomGradient.class, type = EffectType.SHOW)
    @Layout(ColorGradientSection.class)
    @ValueReference(CustomGradientReference.class)
    @ValueProvider(CustomGradientProvider.class)
    StopValueColor[] m_customGradient = DEFAULT_STOP_VALUE_COLORS;

    @Widget(title = "Missing value color", description = "Define the color assigned to missing values in the data.")
    @Layout(SpecialColorsSection.class)
    @TextInputWidget(patternValidation = IsNotBlankValidation.class)
    String m_missingValueColor = "#D30D52";

    @Widget(title = "Not a number (NaN) color",
        description = "Define the color assigned to NaN (Not a Number) values in the data.")
    @Layout(SpecialColorsSection.class)
    @TextInputWidget(patternValidation = IsNotBlankValidation.class)
    String m_nanColor = "#77563C";

    @Widget(title = "Negative infinity color",
        description = "Define the color assigned to values representing negative infinity.")
    @Layout(SpecialColorsSection.class)
    @TextInputWidget(patternValidation = IsNotBlankValidation.class)
    String m_negativeInfinityColor = "#DD691B";

    @Widget(title = "Bottom out-of-bounds color",
        description = "Define the color assigned to values below the defined gradient range.")
    @Layout(SpecialColorsSection.class)
    @TextInputWidget(patternValidation = IsNotBlankValidation.class)
    String m_belowMinColor = "#FF9632";

    @Widget(title = "Top out-of-bounds color",
        description = "Define the color assigned to values above the defined gradient range.")
    @Layout(SpecialColorsSection.class)
    @TextInputWidget(patternValidation = IsNotBlankValidation.class)
    String m_aboveMaxColor = "#FF9632";

    @Widget(title = "Positive infinity color",
        description = "Define the color assigned to values representing positive infinity.")
    @Layout(SpecialColorsSection.class)
    @TextInputWidget(patternValidation = IsNotBlankValidation.class)
    String m_positiveInfinityColor = "#DD691B";

    enum ValueScale {
            @Label(value = "Percentage",
                description = "Map values based on normalized percentage from 0% (minimum) to 100% (maximum). If"
                    + " the node has an input port, during execution, the joint domain of the selected columns will be"
                    + " used to transform the scale into an absolute scale.")
            PERCENTAGE,

            @Label(value = "Absolute values",
                description = "Map values based on their actual numeric values without normalization.")
            ABSOLUTE
    }

    private static final class BaseGradientReference implements ParameterReference<ColorGradientWrapper> {
    }

    private static final class CustomGradientReference implements ParameterReference<StopValueColor[]> {
    }

    private static final class ValueScaleReference implements ParameterReference<ValueScale> {
    }

    private static final class HasTablePort implements EffectPredicateProvider {
        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getConstant(nodeParametersInput -> nodeParametersInput.getInPortSpecs().length > 0);
        }
    }

    private static final class IsCustomGradient implements EffectPredicateProvider {
        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getEnum(BaseGradientReference.class).isOneOf(ColorGradientWrapper.CUSTOM);
        }
    }

    private static final class NumericColumnChoicesProvider implements ColumnChoicesProvider {

        @Override
        public List<DataColumnSpec> columnChoices(final NodeParametersInput context) {
            final var specs = context.getInTableSpecs();
            if (specs.length == 0) {
                return List.of();
            }
            return ColumnSelectionUtil.getDoubleColumnsOfFirstPort(context);
        }

    }

    static final class StopValueColor implements NodeParameters {

        StopValueColor() {
        }

        StopValueColor(final double stopValue, final String color) {
            m_stopValue = stopValue;
            m_color = color;
        }

        @Widget(title = "Stop value", description = "The position of the color stop along the gradient range.")
        @NumberInputWidget(minValidationProvider = StopValueMinValidation.class,
            maxValidationProvider = StopValueMaxValidation.class)
        double m_stopValue;

        @Widget(title = "Color", description = "The color to apply at the specified stop.")
        @TextInputWidget(patternValidation = IsNotBlankValidation.class)
        String m_color = "#000000";
    }

    private static final class CustomGradientProvider implements StateProvider<StopValueColor[]> {
        private Supplier<StopValueColor[]> m_customGradient;

        private Supplier<ColorGradientWrapper> m_baseGradient;

        @Override
        public void init(final StateProviderInitializer initializer) {
            m_customGradient = initializer.getValueSupplier(CustomGradientReference.class);
            m_baseGradient = initializer.computeFromValueSupplier(BaseGradientReference.class);
        }

        @SuppressWarnings("restriction")
        @Override
        public StopValueColor[] computeState(final NodeParametersInput parametersInput)
            throws StateComputationFailureException {
            if (m_baseGradient.get() == ColorGradientWrapper.CUSTOM && m_customGradient.get().length == 0) {
                return DEFAULT_STOP_VALUE_COLORS;
            }
            throw new StateComputationFailureException();
        }
    }

    private abstract static class StopValueValidation<T extends NumberInputWidgetValidation>
        implements StateProvider<T> {

        private T m_percentageValidation;

        StopValueValidation(final T percentageValidation) {
            m_percentageValidation = percentageValidation;
        }

        private Supplier<ValueScale> m_valueScale;

        @Override
        public void init(final StateProviderInitializer initializer) {
            initializer.computeBeforeOpenDialog();
            m_valueScale = initializer.computeFromValueSupplier(ValueScaleReference.class);
        }

        @Override
        public T computeState(final NodeParametersInput parametersInput) {
            return m_valueScale.get() == ValueScale.PERCENTAGE ? m_percentageValidation : null;
        }
    }

    private static final class StopValueMinValidation
        extends StopValueValidation<NumberInputWidgetValidation.MinValidation> {
        StopValueMinValidation() {
            super(new IsNonNegativeValidation());
        }
    }

    private static final class StopValueMaxValidation
        extends StopValueValidation<NumberInputWidgetValidation.MaxValidation> {
        StopValueMaxValidation() {
            super(new IsAtMax100Validation());
        }
    }

    private static final class IsAtMax100Validation extends NumberInputWidgetValidation.MaxValidation {
        @Override
        protected double getMax() {
            return 100;
        }
    }

    @Override
    public void validate() throws InvalidSettingsException {
        if (m_gradient == ColorGradientWrapper.CUSTOM) {
            if (m_customGradient.length < 2) {
                throw new InvalidSettingsException("The custom gradient must contain at least two colors.");
            }
            for (final var customColor : m_customGradient) {
                throwOnInvalidColor(customColor.m_color,
                    String.format("The custom gradient contains an invalid color (\"%s\").", customColor.m_color));
            }
            CheckUtils.check(
                IntStream.range(1, m_customGradient.length)
                    .allMatch(i -> m_customGradient[i - 1].m_stopValue <= m_customGradient[i].m_stopValue),
                InvalidSettingsException::new, () -> "Stop values must be sorted in non-decreasing order.");
            if (m_valueScale == ValueScale.PERCENTAGE) {
                CheckUtils.check(
                    Arrays.stream(m_customGradient).map(svc -> svc.m_stopValue).allMatch(v -> v >= 0 && v <= 100),
                    InvalidSettingsException::new, () -> "Stop values must be in the range [0, 100].");
            }
        }

        throwOnInvalidColor(m_missingValueColor,
            String.format("The missing value color \"%s\" is invalid.", m_missingValueColor));
        throwOnInvalidColor(m_nanColor, String.format("The \"not a number\" color \"%s\" is invalid.", m_nanColor));
        throwOnInvalidColor(m_negativeInfinityColor,
            String.format("The \"negative infinity\" color \"%s\" is invalid.", m_negativeInfinityColor));
        throwOnInvalidColor(m_belowMinColor,
            String.format("The \"bottom out-of-bounds\" color \"%s\" is invalid.", m_belowMinColor));
        throwOnInvalidColor(m_aboveMaxColor,
            String.format("The \"top out-of-bounds\" color \"%s\" is invalid.", m_aboveMaxColor));
        throwOnInvalidColor(m_positiveInfinityColor,
            String.format("The positive infinity color \"%s\" is invalid.", m_positiveInfinityColor));

    }

    private static void throwOnInvalidColor(final String color, final String errorMessage)
        throws InvalidSettingsException {
        if (color.isBlank()) {
            throw new InvalidSettingsException(errorMessage);
        }
        try {
            Color.decode(color);
        } catch (final NumberFormatException e) {
            throw new InvalidSettingsException(errorMessage, e);
        }
    }

    enum ColorGradientWrapper {

            @Label(ColorGradientDefinitionUtil.CUSTOM_NAME)
            CUSTOM(ColorGradient.CUSTOM),

            @Label(ColorGradientDefinitionUtil.CIVIDIS_NAME)
            CIVIDIS(ColorGradient.CIVIDIS),

            @Label(ColorGradientDefinitionUtil.VIRIDIS_NAME)
            VIRIDIS(ColorGradient.VIRIDIS),

            @Label(ColorGradientDefinitionUtil.INFERNO_NAME)
            INFERNO(ColorGradient.INFERNO),

            @Label(ColorGradientDefinitionUtil.MAGMA_NAME)
            MAGMA(ColorGradient.MAGMA),

            @Label(ColorGradientDefinitionUtil.PLASMA_NAME)
            PLASMA(ColorGradient.PLASMA),

            @Label(ColorGradientDefinitionUtil.GRAYSCALE_NAME)
            GRAYSCALE(ColorGradient.GRAYSCALE),

            @Label(ColorGradientDefinitionUtil.SHORTENED_GRAYSCALE_NAME)
            SHORTENED_GRAYSCALE(ColorGradient.SHORTENED_GRAYSCALE),

            @Label(ColorGradientDefinitionUtil.PURPLE_ORANGE_5_NAME)
            PURPLE_ORANGE_5(ColorGradient.PURPLE_ORANGE_5),

            @Label(ColorGradientDefinitionUtil.PURPLE_ORANGE_11_NAME)
            PURPLE_ORANGE_11(ColorGradient.PURPLE_ORANGE_11),

            @Label(ColorGradientDefinitionUtil.RED_BLUE_5_NAME)
            RED_BLUE_5(ColorGradient.RED_BLUE_5),

            @Label(ColorGradientDefinitionUtil.RED_BLUE_11_NAME)
            RED_BLUE_11(ColorGradient.RED_BLUE_11),

            @Label(ColorGradientDefinitionUtil.PURPLE_GREEN_5_NAME)
            PURPLE_GREEN_5(ColorGradient.PURPLE_GREEN_5),

            @Label(ColorGradientDefinitionUtil.PURPLE_GREEN_11_NAME)
            PURPLE_GREEN_11(ColorGradient.PURPLE_GREEN_11),

            @Label(ColorGradientDefinitionUtil.MATPLOTLIB_TWILIGHT_NAME)
            MATPLOTLIB_TWILIGHT(ColorGradient.MATPLOTLIB_TWILIGHT),

            @Label(ColorGradientDefinitionUtil.MATPLOTLIB_TWILIGHT_SHIFTED_NAME)
            MATPLOTLIB_TWILIGHT_SHIFTED(ColorGradient.MATPLOTLIB_TWILIGHT_SHIFTED);

        private final ColorGradient m_colorGradient;

        ColorGradientWrapper(final ColorGradient colorGradient) {
            m_colorGradient = colorGradient;
        }

        ColorGradient getColorGradient() {
            return m_colorGradient;
        }
    }
}
