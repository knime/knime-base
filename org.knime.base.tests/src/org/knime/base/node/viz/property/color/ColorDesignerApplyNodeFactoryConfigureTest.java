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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.knime.base.node.viz.property.color.TestHelper.INITIIAL_COLOR_MAP;
import static org.knime.base.node.viz.property.color.TestHelper.SPECIAL_COLORS;
import static org.knime.base.node.viz.property.color.TestHelper.createModelSpecWithNominalColorHandler;
import static org.knime.base.node.viz.property.color.TestHelper.createModelSpecWithNumericPercentageColorHandler;

import java.awt.Color;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.knime.base.node.viz.property.color.ColorDesignerTestHelper.TestConfigureInput;
import org.knime.base.node.viz.property.color.ColorDesignerTestHelper.TestConfigureOutput;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.MissingCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.property.ColorGradient;
import org.knime.core.data.property.ColorHandler;
import org.knime.core.data.property.ColorModelNominal;
import org.knime.core.data.property.ColorModelRange;
import org.knime.core.data.property.ColorModelRange2;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.node.parameters.widget.choices.filter.ColumnFilter;

@SuppressWarnings("static-method")
final class ColorDesignerApplyNodeFactoryConfigureTest {

    private static Stream<Arguments> inputSpecsWithNullSpec() {
        return Stream.of( //
            Arguments.of(null, TestHelper.createNumericTestTableSpec()), //
            Arguments.of(createModelSpecWithNominalColorHandler(), null));
    }

    @MethodSource("inputSpecsWithNullSpec")
    @ParameterizedTest
    void testConfigureReturnsInputSpecsIfAnInputSpecIsNull(final DataTableSpec modelSpec, final DataTableSpec tableSpec)
        throws InvalidSettingsException {
        final var parameters = new ColorDesignerApplyNodeParameters();
        final var configureInput = new TestConfigureInput(parameters, new PortObjectSpec[]{modelSpec, tableSpec});
        final var configureOutput = new TestConfigureOutput();

        ColorDesignerApplyNodeFactory.configure(configureInput, configureOutput);

        final var resultTableSpec = configureOutput.m_outSpecs[0];
        final var resultModelSpec = configureOutput.m_outSpecs[1];
        assertThat(tableSpec).isEqualTo(resultTableSpec);
        assertThat(modelSpec).isEqualTo(resultModelSpec);
    }

    private static Stream<Arguments> incorrectModelSpec() {
        return Stream.of( //
            // multiple columns in model spec
            Arguments.of(new DataTableSpec(new DataColumnSpecCreator("Col1", StringCell.TYPE).createSpec(),
                new DataColumnSpecCreator("Col2", StringCell.TYPE).createSpec())),
            // no color handler in model spec
            Arguments.of(new DataTableSpec(new DataColumnSpecCreator("Col", StringCell.TYPE).createSpec())));
    }

    @MethodSource("incorrectModelSpec")
    @ParameterizedTest
    void testConfigureThrowsForIncorrectModelSpec(final DataTableSpec modelSpec) throws InvalidSettingsException {
        final var parameters = new ColorDesignerApplyNodeParameters();
        assertConfigureThrows(InvalidSettingsException.class, parameters, modelSpec);
    }

    @Test
    void testConfigureThrowsForUnsupprtedModels() {
        final var parameters = new ColorDesignerApplyNodeParameters();
        final var colorModel = new ColorModelRange(0, Color.WHITE, 100, Color.BLACK);
        final var colSpec = new DataColumnSpecCreator("Color", StringCell.TYPE).createSpec();
        final var colSpecWithHandler = new DataColumnSpecCreator(colSpec);
        colSpecWithHandler.setColorHandler(new ColorHandler(colorModel));
        final var modelSpec = new DataTableSpec(colSpecWithHandler.createSpec());

        assertConfigureThrows(InvalidSettingsException.class, parameters, modelSpec);
    }

    @Test
    void testConfigureThrowsForNominalModelWithoutSelectedColumnsAndColumNames() {
        final var parameters = new ColorDesignerApplyNodeParameters();
        parameters.m_columnFilter = new ColumnFilter(new String[0]);
        parameters.m_applyToColumnNames = false;

        final var modelSpec = createModelSpecWithNominalColorHandler();
        assertConfigureThrows(InvalidSettingsException.class, parameters, modelSpec);
    }

    @Test
    void testConfigureThrowsForRangeModelWithoutSelectedColumns() {
        final var parameters = new ColorDesignerApplyNodeParameters();
        parameters.m_columnFilter = new ColumnFilter(new String[0]);

        final var modelSpec = createModelSpecWithNumericPercentageColorHandler();
        assertConfigureThrows(InvalidSettingsException.class, parameters, modelSpec);
    }

    private static void assertConfigureThrows(final Class<? extends Exception> exception,
        final ColorDesignerApplyNodeParameters parameters, final PortObjectSpec modelSpec) {
        final var tableSpec = TestHelper.createNumericTestTableSpec();

        final var configureInput = new TestConfigureInput(parameters, new PortObjectSpec[]{modelSpec, tableSpec});
        final var configureOutput = new TestConfigureOutput();

        assertThrows(exception, () -> ColorDesignerApplyNodeFactory.configure(configureInput, configureOutput));
    }

    @Test
    void testConfigureWithNominalModelAndColumnValues() throws InvalidSettingsException {
        final var parameters = new ColorDesignerApplyNodeParameters();
        parameters.m_columnFilter = new ColumnFilter(TestHelper.COLUMNS_WITH_DOMAIN);

        final var modelSpec = createModelSpecWithNominalColorHandler();
        final var tableSpec = TestHelper.createTestTableSpec();

        final var configureInput = new TestConfigureInput(parameters, new PortObjectSpec[]{modelSpec, tableSpec});
        final var configureOutput = new TestConfigureOutput();

        ColorDesignerApplyNodeFactory.configure(configureInput, configureOutput);

        final var outputTableSpec = (DataTableSpec)configureOutput.m_outSpecs[0];
        final var outputModelSpec = (DataTableSpec)configureOutput.m_outSpecs[1];

        final var resultMap = new HashMap<>(INITIIAL_COLOR_MAP);
        resultMap.put(new StringCell("C"), ColorPaletteOption.BREWER_SET1_COLORS9.getPaletteAsColorAttr()[2]);
        resultMap.put(new StringCell("D"), ColorPaletteOption.BREWER_SET1_COLORS9.getPaletteAsColorAttr()[3]);

        final var resultColorModel = new ColorModelNominal(resultMap,
            ColorPaletteOption.BREWER_SET1_COLORS9.getPaletteAsColorAttr(), Set.of(new MissingCell(null)));

        assertThat(outputModelSpec.getColumnSpec(0).getColorHandler().getColorModel()).isEqualTo(resultColorModel);
        Arrays.stream(TestHelper.COLUMNS_WITH_DOMAIN).forEach(column -> {
            assertThat(outputTableSpec.getColumnSpec(column).getColorHandler().getColorModel())
                .isEqualTo(resultColorModel);
        });
    }

    @Test
    void testConfigureWithNominalModelAndColumnNames() throws InvalidSettingsException {
        final var parameters = new ColorDesignerApplyNodeParameters();
        parameters.m_applyToColumnNames = true;
        parameters.m_columnFilter = new ColumnFilter(new String[0]);

        final var modelSpec = createModelSpecWithNominalColorHandler();
        final var tableSpec = TestHelper.createTestTableSpec();

        final var configureInput = new TestConfigureInput(parameters, new PortObjectSpec[]{modelSpec, tableSpec});
        final var configureOutput = new TestConfigureOutput();

        ColorDesignerApplyNodeFactory.configure(configureInput, configureOutput);

        final var outputTableSpec = (DataTableSpec)configureOutput.m_outSpecs[0];
        final var outputModelSpec = (DataTableSpec)configureOutput.m_outSpecs[1];

        final var resultMap = new HashMap<>(INITIIAL_COLOR_MAP);
        resultMap.put(new StringCell("Column 0"), ColorPaletteOption.BREWER_SET1_COLORS9.getPaletteAsColorAttr()[2]);
        resultMap.put(new StringCell("Column 1"), ColorPaletteOption.BREWER_SET1_COLORS9.getPaletteAsColorAttr()[3]);
        resultMap.put(new StringCell("Column 2"), ColorPaletteOption.BREWER_SET1_COLORS9.getPaletteAsColorAttr()[4]);
        resultMap.put(new StringCell("Column 3"), ColorPaletteOption.BREWER_SET1_COLORS9.getPaletteAsColorAttr()[5]);

        final var resultColorModel = new ColorModelNominal(resultMap,
            ColorPaletteOption.BREWER_SET1_COLORS9.getPaletteAsColorAttr(), Set.of(new MissingCell(null)));

        assertThat(outputModelSpec.getColumnSpec(0).getColorHandler().getColorModel()).isEqualTo(resultColorModel);
        outputTableSpec.forEach(colSpec -> {
            assertThat(colSpec.getColorHandler()).isNull();
        });
        assertThat(outputTableSpec.getColumnNamesColorHandler().get().getColorModel()).isEqualTo(resultColorModel);
    }

    @Test
    void testConfigureWithRangeModelPercentageBased() throws InvalidSettingsException {
        final var parameters = new ColorDesignerApplyNodeParameters();
        final var column = "Double Column 1";
        parameters.m_columnFilter = new ColumnFilter(new String[]{column});

        final var colorModel = new ColorModelRange2(SPECIAL_COLORS, ColorGradient.PURPLE_GREEN_5);
        final var colorHandler = new ColorHandler(colorModel);
        final var colSpec = new DataColumnSpecCreator("Color", StringCell.TYPE);
        colSpec.setColorHandler(colorHandler);
        final var modelSpec = new DataTableSpec(colSpec.createSpec());
        final var tableSpec = TestHelper.createNumericTestTableSpec();

        final var configureInput = new TestConfigureInput(parameters, new PortObjectSpec[]{modelSpec, tableSpec});
        final var configureOutput = new TestConfigureOutput();

        ColorDesignerApplyNodeFactory.configure(configureInput, configureOutput);

        final var outputTableSpec = (DataTableSpec)configureOutput.m_outSpecs[0];
        final var outputModelSpec = (DataTableSpec)configureOutput.m_outSpecs[1];

        final var resultColorModel =
            new ColorModelRange2(SPECIAL_COLORS, ColorGradient.PURPLE_GREEN_5).applyToDomain(0, 50);
        assertThat(outputModelSpec.getColumnSpec(0).getColorHandler().getColorModel()).isEqualTo(resultColorModel);
        assertThat(outputTableSpec.getColumnSpec(column).getColorHandler().getColorModel()).isEqualTo(resultColorModel);
    }

    @Test
    void testConfigureWithRangeModelAbsoluteBased() throws InvalidSettingsException {
        final var parameters = new ColorDesignerApplyNodeParameters();
        final var column = "Double Column 1";
        parameters.m_columnFilter = new ColumnFilter(new String[]{column});

        final var colorModel = new ColorModelRange2(SPECIAL_COLORS, new double[]{10, 20, 30, 40, 50},
            new Color[]{Color.CYAN, Color.GREEN, Color.YELLOW, Color.ORANGE, Color.RED}, false);
        final var colorHandler = new ColorHandler(colorModel);
        final var colSpec = new DataColumnSpecCreator("Color", StringCell.TYPE);
        colSpec.setColorHandler(colorHandler);
        final var modelSpec = new DataTableSpec(colSpec.createSpec());
        final var tableSpec = TestHelper.createNumericTestTableSpec();

        final var configureInput = new TestConfigureInput(parameters, new PortObjectSpec[]{modelSpec, tableSpec});
        final var configureOutput = new TestConfigureOutput();

        ColorDesignerApplyNodeFactory.configure(configureInput, configureOutput);

        final var outputTableSpec = (DataTableSpec)configureOutput.m_outSpecs[0];
        final var outputModelSpec = (DataTableSpec)configureOutput.m_outSpecs[1];

        assertThat(outputModelSpec).isEqualTo(modelSpec);
        assertThat(outputTableSpec.getColumnSpec(column).getColorHandler().getColorModel()).isEqualTo(colorModel);
    }

}
