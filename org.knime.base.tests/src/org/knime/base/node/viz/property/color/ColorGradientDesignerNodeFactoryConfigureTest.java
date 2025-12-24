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
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.knime.base.node.viz.property.color.TestHelper.NUMERIC_COLUMNS_WITHOUT_DOMAIN;
import static org.knime.base.node.viz.property.color.TestHelper.NUMERIC_COLUMNS_WITH_DOMAIN;

import java.awt.Color;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.knime.base.node.viz.property.color.ColorDesignerTestHelper.TestConfigureInput;
import org.knime.base.node.viz.property.color.ColorDesignerTestHelper.TestConfigureOutput;
import org.knime.base.node.viz.property.color.ColorGradientDesignerNodeParameters.ColorGradientWrapper;
import org.knime.base.node.viz.property.color.ColorGradientDesignerNodeParameters.ValueScale;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.property.ColorModelRange2;
import org.knime.core.data.property.ColorModelRange2.SpecialColorType;
import org.knime.core.node.InvalidSettingsException;
import org.knime.node.parameters.widget.choices.filter.ColumnFilter;
import org.knime.testing.util.TableTestUtil;

/**
 * Tests for {@link ColorGradientDesignerNodeFactory#configure}.
 *
 * @author Robin Gerling, KNIME GmbH, Konstanz, Germany
 */
@SuppressWarnings("static-method")
final class ColorGradientDesignerNodeFactoryConfigureTest {

    static Map<SpecialColorType, Color>
        createSpecialColors(final ColorGradientDesignerNodeParameters parameters) {
        return Map.of(//
            SpecialColorType.MISSING, Color.decode(parameters.m_missingValueColor), //
            SpecialColorType.NAN, Color.decode(parameters.m_nanColor), //
            SpecialColorType.NEGATIVE_INFINITY, Color.decode(parameters.m_negativeInfinityColor), //
            SpecialColorType.BELOW_MIN, Color.decode(parameters.m_belowMinColor), //
            SpecialColorType.ABOVE_MAX, Color.decode(parameters.m_aboveMaxColor), //
            SpecialColorType.POSITIVE_INFINITY, Color.decode(parameters.m_positiveInfinityColor));
    }

    private static double[] createStopValues(final ColorGradientDesignerNodeParameters parameters) {
        return Stream.of(parameters.m_customGradient).mapToDouble(svc -> svc.m_stopValue).toArray();
    }

    private static Color[] createStopColors(final ColorGradientDesignerNodeParameters parameters) {
        return Stream.of(parameters.m_customGradient).map(svc -> Color.decode(svc.m_color)).toArray(Color[]::new);
    }

    @Test
    void testConfigureThrowsForEmptyInputTable() throws InvalidSettingsException {
        final var parameters = new ColorGradientDesignerNodeParameters();
        final var configureInput = new TestConfigureInput(parameters, new DataTableSpec[]{new DataTableSpec()});
        final var configureOutput = new TestConfigureOutput();

        assertThrows(InvalidSettingsException.class,
            () -> ColorGradientDesignerNodeFactory.configure(configureInput, configureOutput));
    }

    @Test
    void testConfigureThrowsForNoNumericColumns() {
        final var parameters = new ColorGradientDesignerNodeParameters();
        parameters.m_columnFilter = new ColumnFilter(new String[]{"String Column"});
        final var testTableSpec = TestHelper.createNumericTestTableSpec();
        final var configureInput = new TestConfigureInput(parameters, new DataTableSpec[]{testTableSpec});
        final var configureOutput = new TestConfigureOutput();

        assertThrows(InvalidSettingsException.class,
            () -> ColorGradientDesignerNodeFactory.configure(configureInput, configureOutput));
    }

    @Test
    void testConfigureThrowsWhenPercentageStopsAndDomainMinimumIsNegativeInfinity() {
        final var parameters = new ColorGradientDesignerNodeParameters();
        parameters.m_columnFilter = new ColumnFilter(new String[]{"Double Column 1"});
        final var tableSpec = new DataTableSpec(
            TableTestUtil.createColumnSpecWithDomain(Double.NEGATIVE_INFINITY, 50.0, "Double Column 1"));
        final var configureInput = new TestConfigureInput(parameters, new DataTableSpec[]{tableSpec});
        final var configureOutput = new TestConfigureOutput();
        final var exception = assertThrows(InvalidSettingsException.class,
            () -> ColorGradientDesignerNodeFactory.configure(configureInput, configureOutput));
        assertTrue(exception.getMessage().contains("because their domain includes infinity"));
    }

    @Test
    void testExecuteThrowsWhenPercentageStopsAndDomainMaximumIsPositiveInfinity() {
        final var parameters = new ColorGradientDesignerNodeParameters();
        parameters.m_columnFilter = new ColumnFilter(new String[]{"Double Column 1"});
        final var tableSpec =
            new DataTableSpec(TableTestUtil.createColumnSpecWithDomain(0, Double.POSITIVE_INFINITY, "Double Column 1"));
        final var configureInput = new TestConfigureInput(parameters, new DataTableSpec[]{tableSpec});
        final var configureOutput = new TestConfigureOutput();
        final var exception = assertThrows(InvalidSettingsException.class,
            () -> ColorGradientDesignerNodeFactory.configure(configureInput, configureOutput));
        assertTrue(exception.getMessage().contains("because their domain includes infinity"));
    }

    @Test
    void testConfigureWithoutInputTablePortAndCustomPercentageGradient() throws InvalidSettingsException {
        final var parameters = new ColorGradientDesignerNodeParameters();
        final var expectedColorModel = new ColorModelRange2(createSpecialColors(parameters),
            createStopValues(parameters), createStopColors(parameters), true);
        assertColorModelWithoutInputTable(parameters, expectedColorModel);
    }

    @Test
    void testConfigureWithoutInputTablePortAndCustomAbsoluteGradient() throws InvalidSettingsException {
        final var parameters = new ColorGradientDesignerNodeParameters();
        parameters.m_valueScale = ValueScale.ABSOLUTE;
        final var expectedColorModel = new ColorModelRange2(createSpecialColors(parameters),
            createStopValues(parameters), createStopColors(parameters), false);
        assertColorModelWithoutInputTable(parameters, expectedColorModel);
    }

    @Test
    void testConfigureWithoutInputTablePortAndPredefinedGradient() throws InvalidSettingsException {
        final var parameters = new ColorGradientDesignerNodeParameters();
        parameters.m_gradient = ColorGradientWrapper.VIRIDIS;
        final var expectedColorModel =
            new ColorModelRange2(createSpecialColors(parameters), ColorGradientWrapper.VIRIDIS.getColorGradient());
        assertColorModelWithoutInputTable(parameters, expectedColorModel);
    }

    private static void assertColorModelWithoutInputTable(final ColorGradientDesignerNodeParameters parameters,
        final ColorModelRange2 expectedColorModel) throws InvalidSettingsException {
        final var configureInput = new TestConfigureInput(parameters, new DataTableSpec[0]);
        final var configureOutput = new TestConfigureOutput();

        ColorGradientDesignerNodeFactory.configure(configureInput, configureOutput);

        assertEquals(1, configureOutput.m_outSpecs.length);
        final var modelSpec = (DataTableSpec)configureOutput.m_outSpecs[0];
        final var receivedColorModel = modelSpec.getColumnSpec(0).getColorHandler().getColorModel();
        assertEquals(receivedColorModel, expectedColorModel);
    }

    @Test
    void testConfigureWithNumericColumnWithoutDomainAndCustomPercentageStopGradient() throws InvalidSettingsException {
        final var selectedColumns = NUMERIC_COLUMNS_WITHOUT_DOMAIN;
        final var parameters = new ColorGradientDesignerNodeParameters();
        parameters.m_columnFilter = new ColumnFilter(selectedColumns);
        final var expectedColorModel = new ColorModelRange2(createSpecialColors(parameters),
            createStopValues(parameters), createStopColors(parameters), true);
        assertColorModelWithInputTable(parameters, expectedColorModel, selectedColumns);
    }

    @Test
    void testConfigureWithNumericColumnWithoutDomainAndCustomAbsoluteStopGradient() throws InvalidSettingsException {
        final var selectedColumns = NUMERIC_COLUMNS_WITHOUT_DOMAIN;
        final var parameters = new ColorGradientDesignerNodeParameters();
        parameters.m_valueScale = ValueScale.ABSOLUTE;
        parameters.m_columnFilter = new ColumnFilter(selectedColumns);

        final var expectedColorModel = new ColorModelRange2(createSpecialColors(parameters),
            createStopValues(parameters), createStopColors(parameters), false);
        assertColorModelWithInputTable(parameters, expectedColorModel, selectedColumns);
    }

    @Test
    void testConfigureWithNumericColumnWithoutDomainAndPredefinedGradient() throws InvalidSettingsException {
        final var selectedColumns = NUMERIC_COLUMNS_WITHOUT_DOMAIN;
        final var parameters = new ColorGradientDesignerNodeParameters();
        parameters.m_gradient = ColorGradientWrapper.MATPLOTLIB_TWILIGHT;
        parameters.m_columnFilter = new ColumnFilter(selectedColumns);

        final var expectedColorModel = new ColorModelRange2(createSpecialColors(parameters),
            ColorGradientWrapper.MATPLOTLIB_TWILIGHT.getColorGradient());
        assertColorModelWithInputTable(parameters, expectedColorModel, selectedColumns);
    }

    @Test
    void testConfigureWithNumericColumnWithDomainAndCustomPercentageStopGradient() throws InvalidSettingsException {
        final var selectedColumns = new String[]{NUMERIC_COLUMNS_WITH_DOMAIN[0]};
        final var parameters = new ColorGradientDesignerNodeParameters();
        parameters.m_columnFilter = new ColumnFilter(selectedColumns);
        final var expectedColorModel = new ColorModelRange2(createSpecialColors(parameters), new double[]{0, 50},
            createStopColors(parameters), false);
        assertColorModelWithInputTable(parameters, expectedColorModel, selectedColumns);
    }

    @Test
    void testConfigureWithNumericColumnWithDomainAndCustomAbsoluteStopGradient() throws InvalidSettingsException {
        final var selectedColumns = new String[]{NUMERIC_COLUMNS_WITH_DOMAIN[1]};
        final var parameters = new ColorGradientDesignerNodeParameters();
        parameters.m_valueScale = ValueScale.ABSOLUTE;
        parameters.m_columnFilter = new ColumnFilter(selectedColumns);

        final var expectedColorModel = new ColorModelRange2(createSpecialColors(parameters),
            createStopValues(parameters), createStopColors(parameters), false);
        assertColorModelWithInputTable(parameters, expectedColorModel, selectedColumns);
    }

    @Test
    void testConfigureWithNumericColumnWithDomainAndPredefinedGradient() throws InvalidSettingsException {
        final var selectedColumns = new String[]{NUMERIC_COLUMNS_WITH_DOMAIN[1]};
        final var parameters = new ColorGradientDesignerNodeParameters();
        parameters.m_gradient = ColorGradientWrapper.PURPLE_ORANGE_5;
        parameters.m_columnFilter = new ColumnFilter(selectedColumns);

        final var expectedColorModel = new ColorModelRange2(createSpecialColors(parameters),
            ColorGradientWrapper.PURPLE_ORANGE_5.getColorGradient()).applyToDomain(-50, 25);
        assertColorModelWithInputTable(parameters, expectedColorModel, selectedColumns);
    }

    @Test
    void testConfigureWithNumericColumnsWithDomainAndPredefinedGradient() throws InvalidSettingsException {
        final var selectedColumns = NUMERIC_COLUMNS_WITH_DOMAIN;
        final var parameters = new ColorGradientDesignerNodeParameters();
        parameters.m_gradient = ColorGradientWrapper.PURPLE_ORANGE_5;
        parameters.m_columnFilter = new ColumnFilter(selectedColumns);

        final var expectedColorModel = new ColorModelRange2(createSpecialColors(parameters),
            // uses combined domains of both columns (1. 0<->50 2. -50<->25 Result: -50<->50
            ColorGradientWrapper.PURPLE_ORANGE_5.getColorGradient()).applyToDomain(-50, 50);
        assertColorModelWithInputTable(parameters, expectedColorModel, selectedColumns);
    }

    private static void assertColorModelWithInputTable(final ColorGradientDesignerNodeParameters parameters,
        final ColorModelRange2 expectedColorModel, final String[] columnsWithColorModel)
        throws InvalidSettingsException {
        final var testTableSpec = TestHelper.createNumericTestTableSpec();
        final var configureInput = new TestConfigureInput(parameters, new DataTableSpec[]{testTableSpec});
        final var configureOutput = new TestConfigureOutput();

        ColorGradientDesignerNodeFactory.configure(configureInput, configureOutput);

        assertEquals(2, configureOutput.m_outSpecs.length);
        final var outputTableSpec = (DataTableSpec)configureOutput.m_outSpecs[0];
        final var modelSpec = (DataTableSpec)configureOutput.m_outSpecs[1];

        final var receivedColorModel = modelSpec.getColumnSpec(0).getColorHandler().getColorModel();
        assertEquals(receivedColorModel, expectedColorModel);
        for (final var columnName : testTableSpec.getColumnNames()) {
            final var columnSpec = outputTableSpec.getColumnSpec(columnName);
            if (Stream.of(columnsWithColorModel).anyMatch(col -> col.equals(columnName))) {
                final var colorModel = columnSpec.getColorHandler().getColorModel();
                assertEquals(colorModel, expectedColorModel);
            } else {
                assertNull(columnSpec.getColorHandler());
            }
        }
    }
}
