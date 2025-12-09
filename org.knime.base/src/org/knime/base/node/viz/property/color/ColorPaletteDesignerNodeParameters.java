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
import java.util.List;
import java.util.function.Supplier;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
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
import org.knime.node.parameters.widget.text.TextInputWidget;
import org.knime.node.parameters.widget.text.TextInputWidgetValidation.PatternValidation.IsNotBlankValidation;

/**
 * The node parameters for the {@link ColorPaletteDesignerNodeFactory}.
 *
 * @author Robin Gerling, KNIME GmbH, Konstanz, Germany
 */
class ColorPaletteDesignerNodeParameters implements NodeParameters {

    public ColorPaletteDesignerNodeParameters() {
        // default constructor
    }

    public ColorPaletteDesignerNodeParameters(final NodeParametersInput nodeParametersInput) {
        if (nodeParametersInput.getInPortTypes().length == 0) {
            // No table input port added.
            return;
        }
        nodeParametersInput.getInTableSpec(0)
            .ifPresent(spec -> m_columnFilter = new ColumnFilter(getColumnSpecsWithDomain(spec)));
    }

    @Effect(predicate = HasTablePort.class, type = EffectType.SHOW)
    @Before(ColorPaletteSection.class)
    @Section(title = "Values")
    interface ValuesSection {
    }

    @After(ValuesSection.class)
    @Before(AssignedColorsSection.class)
    @Section(title = "Color Palette")
    interface ColorPaletteSection {
    }

    @After(ColorPaletteSection.class)
    @Before(SpecialColorsSection.class)
    @Section(title = "Assigned Colors")
    interface AssignedColorsSection {
    }

    @After(AssignedColorsSection.class)
    @Section(title = "Special Colors")
    interface SpecialColorsSection {
    }

    @Widget(title = "Apply color to", description = "Determine where to apply the color mapping to:")
    @ValueReference(ApplyColorToReference.class)
    @ValueSwitchWidget
    @Layout(ValuesSection.class)
    ApplyColorTo m_applyTo = ApplyColorTo.VALUES;

    @Widget(title = "Categorical columns",
        description = "Specify one or more category columns whose unique values will be colorized."
            + " When multiple columns are selected, the palette spans the combined set of unique values.")
    @Effect(predicate = HasTablePortApplyColorToValues.class, type = EffectType.SHOW)
    @ChoicesProvider(DomainColumnChoicesProvider.class)
    @Layout(ValuesSection.class)
    ColumnFilter m_columnFilter = new ColumnFilter();

    @Widget(title = "Base palette",
        description = "Colors are assigned from this palette in the order listed. If the number of values exceeds the"
            + " number of colors, the palette is repeated.")
    @ValueReference(BasePaletteReference.class)
    @Layout(ColorPaletteSection.class)
    ColorPaletteOption m_basePalette = ColorPaletteOption.BREWER_SET1_COLORS9;

    @Widget(title = "Custom palette",
        description = "Enter colors in the desired order. These colors are used for automatic mapping and repeat"
            + " if more values exist than listed colors.")
    @ArrayWidget(addButtonText = "Add color", elementLayout = ElementLayout.HORIZONTAL_SINGLE_LINE,
        showSortButtons = true)
    @Effect(predicate = IsCustomPalette.class, type = EffectType.SHOW)
    @Layout(ColorPaletteSection.class)
    @ValueReference(CustomPaletteReference.class)
    @ValueProvider(CustomPaletteProvider.class)
    CustomColor[] m_customPalette = new CustomColor[0];

    @Widget(title = "Assigned colors",
        description = "Define specific color assignments for particular values (or column names). These"
            + " assignments take precedence over the base palette.")
    @ArrayWidget(addButtonText = "Add color rule", elementTitle = "Color rule")
    @Layout(AssignedColorsSection.class)
    ColorRule[] m_assignedColors = new ColorRule[0];

    @Widget(title = "Missing value color", description = "Define the color assigned to missing values in the data.")
    @Layout(SpecialColorsSection.class)
    @TextInputWidget(patternValidation = IsNotBlankValidation.class)
    String m_missingValueColor = "#D30D52";

    enum ApplyColorTo {
            @Label(value = "Values in columns",
                description = "Apply the colors to the unique values in the selected columns.")
            VALUES, //
            @Label(value = "Column names", description = "Apply the colors to the column names of the input table.")
            COLUMNS
    }

    private static final class ApplyColorToReference implements ParameterReference<ApplyColorTo> {
    }

    private static final class BasePaletteReference implements ParameterReference<ColorPaletteOption> {
    }

    private static final class CustomPaletteReference implements ParameterReference<CustomColor[]> {
    }

    private static final class HasTablePort implements EffectPredicateProvider {
        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getConstant(nodeParametersInput -> nodeParametersInput.getInPortSpecs().length > 0);
        }
    }

    private static final class HasTablePortApplyColorToValues implements EffectPredicateProvider {
        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getPredicate(HasTablePort.class)
                .and(i.getEnum(ApplyColorToReference.class).isOneOf(ApplyColorTo.VALUES));
        }
    }

    private static final class IsCustomPalette implements EffectPredicateProvider {
        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getEnum(BasePaletteReference.class).isOneOf(ColorPaletteOption.CUSTOM);
        }
    }

    private static final class CustomPaletteProvider implements StateProvider<CustomColor[]> {
        private Supplier<CustomColor[]> m_customPalette;

        private Supplier<ColorPaletteOption> m_basePalette;

        @Override
        public void init(final StateProviderInitializer initializer) {
            m_customPalette = initializer.getValueSupplier(CustomPaletteReference.class);
            m_basePalette = initializer.computeFromValueSupplier(BasePaletteReference.class);
        }

        @SuppressWarnings("restriction")
        @Override
        public CustomColor[] computeState(final NodeParametersInput parametersInput)
            throws StateComputationFailureException {
            if (m_basePalette.get() == ColorPaletteOption.CUSTOM && m_customPalette.get().length == 0) {
                return new CustomColor[]{new CustomColor()};
            }
            throw new StateComputationFailureException();
        }
    }

    private static final String BLACK = "#000000";

    static final class CustomColor implements NodeParameters {

        CustomColor() {
        }

        CustomColor(final String color) {
            m_color = color;
        }

        @Widget(title = "Color", description = "Specify the color to be used in the custom palette.")
        @TextInputWidget(patternValidation = IsNotBlankValidation.class)
        String m_color = BLACK;

    }

    static final class ColorRule implements NodeParameters {

        ColorRule() {
        }

        ColorRule(final String color, final String matchingValue) {
            m_color = color;
            m_matchingValue = matchingValue;
        }

        @Widget(title = "Color", description = "Specify the color to be used for the corresponding value.")
        @TextInputWidget(patternValidation = IsNotBlankValidation.class)
        String m_color = BLACK;

        @Widget(title = "Matching value",
            description = "Enter the exact value (or column name) that should receive the assigned color.")
        String m_matchingValue = "";

    }

    private static final class DomainColumnChoicesProvider implements ColumnChoicesProvider {

        @Override
        public List<DataColumnSpec> columnChoices(final NodeParametersInput context) {
            final var specs = context.getInTableSpecs();
            if (specs.length == 0 || specs[0] == null) {
                return List.of();
            }
            return getColumnSpecsWithDomain(specs[0]);
        }

    }

    private static List<DataColumnSpec> getColumnSpecsWithDomain(final DataTableSpec tableSpec) {
        return tableSpec.stream().filter(colSpec -> colSpec.getDomain().hasValues()).toList();
    }

    @Override
    public void validate() throws InvalidSettingsException {
        if (m_basePalette == ColorPaletteOption.CUSTOM) {
            final var customPalette = m_customPalette;
            if (customPalette.length == 0) {
                throw new InvalidSettingsException("The custom base palette must contain at least one color.");
            }
            for (final var customColor : customPalette) {
                throwOnInvalidColor(customColor.m_color,
                    String.format("The custom base palette contains an invalid color (\"%s\").", customColor.m_color));
            }
        }

        throwOnInvalidColor(m_missingValueColor,
            String.format("The missing value color \"%s\" is invalid.", m_missingValueColor));

        for (final var assignedColor : m_assignedColors) {
            throwOnInvalidColor(assignedColor.m_color,
                String.format("The color \"%s\" assigned to the value \"%s\" is invalid.", assignedColor.m_color,
                    assignedColor.m_matchingValue));
        }
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

}
