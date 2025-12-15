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
 *   15 Dec 2025 (robin): created
 */
package org.knime.base.node.viz.property.color;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.knime.base.node.viz.property.color.TestHelper.SPECIAL_COLORS;
import static org.knime.base.node.viz.property.color.TestHelper.createModelSpecWithNominalColorHandler;
import static org.knime.base.node.viz.property.color.TestHelper.createModelSpecWithNumericAbsoluteColorHandler;
import static org.knime.base.node.viz.property.color.TestHelper.createModelSpecWithNumericPercentageColorHandler;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.knime.base.node.viz.property.color.ColorDesignerTestHelper.TestExecuteInput;
import org.knime.base.node.viz.property.color.ColorDesignerTestHelper.TestExecuteOutput;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.MissingCell;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.property.ColorAttr;
import org.knime.core.data.property.ColorGradient;
import org.knime.core.data.property.ColorModelNominal;
import org.knime.core.data.property.ColorModelRange2;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.KNIMEException;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.viewproperty.ColorHandlerPortObject;
import org.knime.node.parameters.widget.choices.filter.ColumnFilter;

@SuppressWarnings("static-method")
final class ColorDesignerApplyNodeFactoryExecuteTest {

    @Test
    void testExecuteWithNominalModelAndColumnValues() throws CanceledExecutionException, KNIMEException {
        final var parameters = new ColorDesignerApplyNodeParameters();
        parameters.m_columnFilter = new ColumnFilter(TestHelper.COLUMNS_WITH_DOMAIN);
        parameters.m_applyToColumnNames = false;

        final var modelSpec = createModelSpecWithNominalColorHandler();
        final var table = TestHelper.createTestTable(
            new DataColumnSpec[]{new DataColumnSpecCreator("Column 1", StringCell.TYPE).createSpec(),
                new DataColumnSpecCreator("Column 2", StringCell.TYPE).createSpec()},
            new String[][]{{"A", "B"}, {"B", "B"}, {"A", "C"}, {"A", "D"}, {null, null}});
        final var modelPortObject = new ColorHandlerPortObject(modelSpec, "Color Model");

        final var executeInput = new TestExecuteInput(parameters, new PortObject[]{modelPortObject, table});
        final var executeOutput = new TestExecuteOutput();

        ColorDesignerApplyNodeFactory.execute(executeInput, executeOutput);

        final var outputModelObj = (ColorHandlerPortObject)executeOutput.m_outData[1];
        final var outputModel = outputModelObj.getSpec().getColumnSpec(0).getColorHandler().getColorModel();

        final var expectedColorMap = Map.<DataCell, ColorAttr> of( //
            new StringCell("A"), ColorPaletteOption.BREWER_SET1_COLORS9.getPaletteAsColorAttr()[0], //
            new StringCell("B"), ColorPaletteOption.BREWER_SET1_COLORS9.getPaletteAsColorAttr()[1], //
            new StringCell("C"), ColorPaletteOption.BREWER_SET1_COLORS9.getPaletteAsColorAttr()[2], //
            new StringCell("D"), ColorPaletteOption.BREWER_SET1_COLORS9.getPaletteAsColorAttr()[3], //
            new MissingCell(null), ColorPaletteDesignerNodeFactory.hexToColorAttr("#808080"));

        final var expectedColorModel = new ColorModelNominal(expectedColorMap,
            ColorPaletteOption.BREWER_SET1_COLORS9.getPaletteAsColorAttr(), Set.of(new MissingCell(null)));

        assertEquals(2, executeOutput.m_outData.length);
        assertEquals(BufferedDataTable.class, executeOutput.m_outData[0].getClass());
        assertEquals(ColorHandlerPortObject.class, outputModelObj.getClass());
        assertThat(outputModel).isEqualTo(expectedColorModel);
    }

    @Test
    void testExecuteWithNominalModelAndColumnNames() throws CanceledExecutionException, KNIMEException {
        final var parameters = new ColorDesignerApplyNodeParameters();
        parameters.m_columnFilter = new ColumnFilter(new String[0]);
        parameters.m_applyToColumnNames = true;

        final var modelSpec = createModelSpecWithNominalColorHandler();
        final var table = TestHelper.createTestTable(
            new DataColumnSpec[]{
                TestHelper.createColumnWithDomain("Column 1", StringCell.TYPE, List.of("A", "B", "C"))},
            new String[0][]);
        final var modelPortObject = new ColorHandlerPortObject(modelSpec, "Color Model");

        final var executeInput = new TestExecuteInput(parameters, new PortObject[]{modelPortObject, table});
        final var executeOutput = new TestExecuteOutput();

        ColorDesignerApplyNodeFactory.execute(executeInput, executeOutput);

        final var outputTable = (BufferedDataTable)executeOutput.m_outData[0];
        final var outputModelObj = (ColorHandlerPortObject)executeOutput.m_outData[1];
        final var outputModel = outputModelObj.getSpec().getColumnSpec(0).getColorHandler().getColorModel();

        final var expectedColorMap = Map.<DataCell, ColorAttr> of( //
            new StringCell("A"), ColorPaletteOption.BREWER_SET1_COLORS9.getPaletteAsColorAttr()[0], //
            new StringCell("B"), ColorPaletteOption.BREWER_SET1_COLORS9.getPaletteAsColorAttr()[1], //
            new StringCell("Column 1"), ColorPaletteOption.BREWER_SET1_COLORS9.getPaletteAsColorAttr()[2], //
            new MissingCell(null), ColorPaletteDesignerNodeFactory.hexToColorAttr("#808080"));

        final var expectedColorModel = new ColorModelNominal(expectedColorMap,
            ColorPaletteOption.BREWER_SET1_COLORS9.getPaletteAsColorAttr(), Set.of(new MissingCell(null)));

        assertEquals(2, executeOutput.m_outData.length);
        assertEquals(BufferedDataTable.class, outputTable.getClass());
        assertEquals(ColorHandlerPortObject.class, outputModelObj.getClass());
        assertThat(outputModel).isEqualTo(expectedColorModel);
    }

    @Test
    void testExecuteWithRangeModelPercentageBased() throws CanceledExecutionException, KNIMEException {
        final var parameters = new ColorDesignerApplyNodeParameters();
        final var column = "Double Column 1";
        parameters.m_columnFilter = new ColumnFilter(new String[]{column});
        parameters.m_applyToColumnNames = false;

        final var modelSpec = createModelSpecWithNumericPercentageColorHandler();

        final var table = TestHelper.createTestTable(
            new DataColumnSpec[]{new DataColumnSpecCreator(column, DoubleCell.TYPE).createSpec()},
            new Double[][]{{0.0}, {50.0}, {20.0}});
        final var modelPortObject = new ColorHandlerPortObject(modelSpec, "Color Model");

        final var executeInput = new TestExecuteInput(parameters, new PortObject[]{modelPortObject, table});
        final var executeOutput = new TestExecuteOutput();

        ColorDesignerApplyNodeFactory.execute(executeInput, executeOutput);

        final var outputModelObj = (ColorHandlerPortObject)executeOutput.m_outData[1];
        final var outputModel = outputModelObj.getSpec().getColumnSpec(0).getColorHandler().getColorModel();

        final var expectedColorModel =
            new ColorModelRange2(SPECIAL_COLORS, ColorGradient.PURPLE_GREEN_5).applyToDomain(0, 50);

        assertEquals(2, executeOutput.m_outData.length);
        assertEquals(BufferedDataTable.class, executeOutput.m_outData[0].getClass());
        assertEquals(ColorHandlerPortObject.class, outputModelObj.getClass());
        assertThat(outputModel).isEqualTo(expectedColorModel);
    }

    @Test
    void testExecuteWithRangeModelAbsoluteBased() throws CanceledExecutionException, KNIMEException {
        final var parameters = new ColorDesignerApplyNodeParameters();
        final var column = "Double Column 1";
        parameters.m_columnFilter = new ColumnFilter(new String[]{column});
        parameters.m_applyToColumnNames = false;

        final var modelSpec = createModelSpecWithNumericAbsoluteColorHandler();

        final var table = TestHelper.createTestTable(
            new DataColumnSpec[]{new DataColumnSpecCreator(column, DoubleCell.TYPE).createSpec()},
            new Double[][]{{0.0}, {50.0}, {20.0}});
        final var modelPortObject = new ColorHandlerPortObject(modelSpec, "Color Model");

        final var executeInput = new TestExecuteInput(parameters, new PortObject[]{modelPortObject, table});
        final var executeOutput = new TestExecuteOutput();

        ColorDesignerApplyNodeFactory.execute(executeInput, executeOutput);

        final var outputModelObj = (ColorHandlerPortObject)executeOutput.m_outData[1];
        final var outputModelFromPort = outputModelObj.getSpec().getColumnSpec(0).getColorHandler().getColorModel();

        assertEquals(2, executeOutput.m_outData.length);
        assertEquals(BufferedDataTable.class, executeOutput.m_outData[0].getClass());
        assertEquals(ColorHandlerPortObject.class, outputModelObj.getClass());
        // For absolute-based model, the model should remain unchanged
        final var colorModel = modelSpec.getColumnSpec(0).getColorHandler().getColorModel();
        assertThat(outputModelFromPort).isEqualTo(colorModel);
    }

}
