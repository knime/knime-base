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

package org.knime.base.node.viz.property.color;

import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.knime.base.node.viz.property.color.TestHelper.COLUMNS_WITH_DOMAIN;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.knime.base.node.viz.property.color.ColorDesignerTestHelper.TestConfigureInput;
import org.knime.base.node.viz.property.color.ColorDesignerTestHelper.TestConfigureOutput;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.MissingCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.property.ColorAttr;
import org.knime.core.data.property.ColorModel;
import org.knime.core.data.property.ColorModelNominal;
import org.knime.core.node.InvalidSettingsException;
import org.knime.node.parameters.widget.choices.filter.ColumnFilter;

@SuppressWarnings("static-method")
final class ColorPaletteDesignerNodeFactoryConfigureTest {

    @Test
    void testConfigureThrowsForEmptyInputTable() throws InvalidSettingsException {
        final var parameters = new ColorPaletteDesignerNodeParameters();
        final var configureInput = new TestConfigureInput(parameters, new DataTableSpec[]{new DataTableSpec()});
        final var configureOutput = new TestConfigureOutput();

        assertThrows(InvalidSettingsException.class,
            () -> ColorPaletteDesignerNodeFactory.configure(configureInput, configureOutput));
    }

    @Test
    void testConfigureThrowsForColumnNameColoringOnEmptyTable() {
        final var parameters = new ColorPaletteDesignerNodeParameters();
        parameters.m_applyTo = ColorPaletteDesignerNodeParameters.ApplyColorTo.COLUMNS;
        final var emptyTableSpec = new DataTableSpec();
        final var configureInput = new TestConfigureInput(parameters, new DataTableSpec[]{emptyTableSpec});
        final var configureOutput = new TestConfigureOutput();

        assertThrows(InvalidSettingsException.class,
            () -> ColorPaletteDesignerNodeFactory.configure(configureInput, configureOutput));
    }

    @Test
    void testConfigureWithoutInputTablePort() throws InvalidSettingsException {
        final var parameters = new ColorPaletteDesignerNodeParameters();
        final var configureInput = new TestConfigureInput(parameters, new DataTableSpec[0]);
        final var configureOutput = new TestConfigureOutput();

        ColorPaletteDesignerNodeFactory.configure(configureInput, configureOutput);

        assertEquals(configureOutput.m_outSpecs.length, 1);
        final var modelSpec = (DataTableSpec)configureOutput.m_outSpecs[0];
        final var receivedColorModel = modelSpec.getColumnSpec(0).getColorHandler().getColorModel();
        final var expectedColorMap = Map.of((DataCell)new MissingCell(null),
            ColorPaletteDesignerNodeFactory.hexToColorAttr(parameters.m_missingValueColor));
        final var expectedColorModel = new ColorModelNominal(expectedColorMap,
            ColorPaletteOption.BREWER_SET1_COLORS9.getPaletteAsColorAttr(), Set.of(new MissingCell(null)));
        assertEquals(receivedColorModel, expectedColorModel);
    }

    static Stream<Arguments> columnSelectionTestParameters() {
        return Stream.of(
            // Single column: only column 0, domain values A, B, C
            Arguments.of(new String[]{COLUMNS_WITH_DOMAIN[0]}, List.of("A", "B", "C"),
                new String[]{COLUMNS_WITH_DOMAIN[1]}),
            // Two columns: both columns, domain values A, B, C, D
            Arguments.of(COLUMNS_WITH_DOMAIN, List.of("A", "B", "C", "D"), new String[0]));
    }

    @ParameterizedTest
    @MethodSource("columnSelectionTestParameters")
    void testConfigureWithColumnValues(final String[] selectedColumns, final List<String> expectedDomainValues,
        final String[] columnsWithoutColorHandler) throws InvalidSettingsException {
        final var testTableSpec = TestHelper.createTestTableSpec();
        final var parameters = new ColorPaletteDesignerNodeParameters();
        parameters.m_columnFilter = new ColumnFilter(selectedColumns);
        final var configureInput = new TestConfigureInput(parameters, new DataTableSpec[]{testTableSpec});
        final var configureOutput = new TestConfigureOutput();

        ColorPaletteDesignerNodeFactory.configure(configureInput, configureOutput);

        assertEquals(configureOutput.m_outSpecs.length, 2);
        final var modelSpec = (DataTableSpec)configureOutput.m_outSpecs[1];
        final var palette = parameters.m_basePalette.getPaletteAsColorAttr();
        final var receivedColorModel = modelSpec.getColumnSpec(0).getColorHandler().getColorModel();
        final var domainValues = expectedDomainValues.stream().map(StringCell::new).toArray(DataCell[]::new);
        final var colorMap = IntStream.range(0, domainValues.length).boxed().collect(Collectors.toMap( //
            i -> domainValues[i], //
            i -> palette[i], //
            (a, b) -> a, HashMap::new));
        colorMap.put(new MissingCell(null),
            ColorPaletteDesignerNodeFactory.hexToColorAttr(parameters.m_missingValueColor));
        final var expectedColorModel = new ColorModelNominal(colorMap, palette, Set.of(new MissingCell(null)));
        assertEquals(receivedColorModel, expectedColorModel);

        final var outputTableSpec = (DataTableSpec)configureOutput.m_outSpecs[0];
        assertEquals(outputTableSpec.getNumColumns(), testTableSpec.getNumColumns());
        for (final var columnName : selectedColumns) {
            assertEquals(outputTableSpec.getColumnSpec(columnName).getColorHandler().getColorModel(),
                expectedColorModel);
        }
        for (final var columnName : columnsWithoutColorHandler) {
            assertNull(outputTableSpec.getColumnSpec(columnName).getColorHandler());
        }
    }

    static Stream<Arguments> assignedColorTestParameters() {
        return Stream.of(
            // Value in domain: assign custom color to "A"
            Arguments.of("#123456", "A", List.of("B", "C", "D")),
            // Color from palette: assign palette[0] to "A", palette assignment continues from 0
            Arguments.of(
                ColorModel
                    .colorToHexString(ColorPaletteOption.BREWER_SET1_COLORS9.getPaletteAsColorAttr()[0].getColor()),
                "A", List.of("B", "C", "D")),
            // New value: assign color to "X" which is not in domain
            Arguments.of("#00FF00", "X", List.of("A", "B", "C", "D")));
    }

    @ParameterizedTest
    @MethodSource("assignedColorTestParameters")
    void testAssignedColor(final String assignedColorHex, final String assignedValue,
        final List<String> defaultPaletteValues) throws InvalidSettingsException {
        final var parameters = new ColorPaletteDesignerNodeParameters();
        final var testTableSpec = TestHelper.createTestTableSpec();
        parameters.m_columnFilter = new ColumnFilter(COLUMNS_WITH_DOMAIN);
        final var palette = parameters.m_basePalette.getPaletteAsColorAttr();

        parameters.m_assignedColors = new ColorPaletteDesignerNodeParameters.ColorRule[]{
            new ColorPaletteDesignerNodeParameters.ColorRule(assignedColorHex, assignedValue)};
        final var configureInput = new TestConfigureInput(parameters, new DataTableSpec[]{testTableSpec});
        final var configureOutput = new TestConfigureOutput();

        ColorPaletteDesignerNodeFactory.configure(configureInput, configureOutput);

        assertEquals(configureOutput.m_outSpecs.length, 2);
        final var modelSpec = (DataTableSpec)configureOutput.m_outSpecs[1];
        final var receivedColorModel = modelSpec.getColumnSpec(0).getColorHandler().getColorModel();

        final var assignedValueCell = new StringCell(assignedValue);
        final var colorMap = new HashMap<DataCell, ColorAttr>();
        colorMap.put(assignedValueCell, ColorPaletteDesignerNodeFactory.hexToColorAttr(assignedColorHex));
        colorMap.put(new MissingCell(null),
            ColorPaletteDesignerNodeFactory.hexToColorAttr(parameters.m_missingValueColor));

        IntStream.range(0, defaultPaletteValues.size()).forEach(i -> {
            final var value = defaultPaletteValues.get(i);
            colorMap.put(new StringCell(value), palette[i]);
        });

        final var expectedColorModel =
            new ColorModelNominal(colorMap, palette, Set.of(new MissingCell(null), assignedValueCell));
        assertEquals(receivedColorModel, expectedColorModel);
    }

    @Test
    void testColumnNameColoring() throws InvalidSettingsException {
        final var parameters = new ColorPaletteDesignerNodeParameters();
        parameters.m_applyTo = ColorPaletteDesignerNodeParameters.ApplyColorTo.COLUMNS;
        final var testTableSpec = TestHelper.createTestTableSpec();
        final var configureInput = new TestConfigureInput(parameters, new DataTableSpec[]{testTableSpec});
        final var configureOutput = new TestConfigureOutput();

        ColorPaletteDesignerNodeFactory.configure(configureInput, configureOutput);

        assertEquals(configureOutput.m_outSpecs.length, 2);
        final var modelSpec = (DataTableSpec)configureOutput.m_outSpecs[1];
        final var palette = parameters.m_basePalette.getPaletteAsColorAttr();
        final var receivedColorModel = modelSpec.getColumnSpec(0).getColorHandler().getColorModel();

        final var colorMap = new HashMap<DataCell, ColorAttr>();
        colorMap.put(new StringCell("Column 0"), palette[0]);
        colorMap.put(new StringCell("Column 1"), palette[1]);
        colorMap.put(new StringCell("Column 2"), palette[2]);
        colorMap.put(new StringCell("Column 3"), palette[3]);
        colorMap.put(new MissingCell(null),
            ColorPaletteDesignerNodeFactory.hexToColorAttr(parameters.m_missingValueColor));

        final var expectedColorModel = new ColorModelNominal(colorMap, palette, Set.of(new MissingCell(null)));
        assertEquals(receivedColorModel, expectedColorModel);
        final var tableSpec = (DataTableSpec)configureOutput.m_outSpecs[0];
        assertEquals(receivedColorModel, tableSpec.getColumnNamesColorHandler().get().getColorModel());
    }

    @Test
    void testCustomPaletteWithColors() throws InvalidSettingsException {
        final var parameters = new ColorPaletteDesignerNodeParameters();
        parameters.m_basePalette = ColorPaletteOption.CUSTOM;
        final var color1 = "#FF0000";
        final var color2 = "#00FF00";
        final var color3 = "#0000FF";
        parameters.m_customPalette = new ColorPaletteDesignerNodeParameters.CustomColor[]{
            new ColorPaletteDesignerNodeParameters.CustomColor(color1),
            new ColorPaletteDesignerNodeParameters.CustomColor(color2),
            new ColorPaletteDesignerNodeParameters.CustomColor(color3)};

        final var testTableSpec = TestHelper.createTestTableSpec();
        parameters.m_columnFilter = new ColumnFilter(COLUMNS_WITH_DOMAIN);
        final var configureInput = new TestConfigureInput(parameters, new DataTableSpec[]{testTableSpec});
        final var configureOutput = new TestConfigureOutput();

        ColorPaletteDesignerNodeFactory.configure(configureInput, configureOutput);

        assertEquals(configureOutput.m_outSpecs.length, 2);
        final var modelSpec = (DataTableSpec)configureOutput.m_outSpecs[1];
        final var palette = new ColorAttr[]{ColorPaletteDesignerNodeFactory.hexToColorAttr(color1),
            ColorPaletteDesignerNodeFactory.hexToColorAttr(color2),
            ColorPaletteDesignerNodeFactory.hexToColorAttr(color3)};
        final var receivedColorModel = modelSpec.getColumnSpec(0).getColorHandler().getColorModel();

        // Build expected color map: values get colors from custom palette, D wraps around to first color
        final var colorMap = new HashMap<DataCell, ColorAttr>();
        colorMap.put(new StringCell("A"), palette[0]);
        colorMap.put(new StringCell("B"), palette[1]);
        colorMap.put(new StringCell("C"), palette[2]);
        colorMap.put(new StringCell("D"), palette[0]); // Wraps around
        colorMap.put(new MissingCell(null),
            ColorPaletteDesignerNodeFactory.hexToColorAttr(parameters.m_missingValueColor));

        final var expectedColorModel = new ColorModelNominal(colorMap, palette, Set.of(new MissingCell(null)));
        assertEquals(receivedColorModel, expectedColorModel);
    }

}
