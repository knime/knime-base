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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.knime.base.node.viz.property.color.TestHelper.NUMERIC_COLUMNS_WITH_DOMAIN;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.knime.base.node.viz.property.color.ColorDesignerTestHelper.TestExecuteInput;
import org.knime.base.node.viz.property.color.ColorDesignerTestHelper.TestExecuteOutput;
import org.knime.base.node.viz.property.color.ColorGradientDesignerNodeParameters.ColorGradientWrapper;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.MissingCell;
import org.knime.core.data.property.ColorGradient;
import org.knime.core.data.property.ColorModelRange2;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.KNIMEException;
import org.knime.core.node.port.viewproperty.ColorHandlerPortObject;
import org.knime.node.parameters.widget.choices.filter.ColumnFilter;
import org.knime.testing.util.TableTestUtil;
import org.knime.testing.util.TableTestUtil.TableBuilder;

/**
 * Tests for {@link ColorGradientDesignerNodeFactory#execute}.
 *
 * @author Robin Gerling, KNIME GmbH, Konstanz, Germany
 */
@SuppressWarnings("static-method")
final class ColorGradientDesignerNodeFactoryExecuteTest {

    @Test
    void testExecuteWithoutInputTableProducesOnlyColorModel() throws CanceledExecutionException, KNIMEException {
        final var parameters = new ColorGradientDesignerNodeParameters();
        final var executeInput = new TestExecuteInput(parameters, new BufferedDataTable[0]);
        final var executeOutput = new TestExecuteOutput();

        ColorGradientDesignerNodeFactory.execute(executeInput, executeOutput);

        assertEquals(1, executeOutput.m_outData.length);
        assertEquals(ColorHandlerPortObject.class, executeOutput.m_outData[0].getClass());
    }

    @Test
    void testExecuteWithInputTableProducesBothOutputs() throws CanceledExecutionException, KNIMEException {
        final var parameters = new ColorGradientDesignerNodeParameters();
        parameters.m_gradient = ColorGradientWrapper.CIVIDIS;
        parameters.m_columnFilter = new ColumnFilter(NUMERIC_COLUMNS_WITH_DOMAIN);

        final var inputTable = mock(BufferedDataTable.class);
        final var inputSpec = TestHelper.createNumericTestTableSpec();
        when(inputTable.getDataTableSpec()).thenReturn(inputSpec);

        final var executeInput = new TestExecuteInput(parameters, new BufferedDataTable[]{inputTable});
        final var executeOutput = new TestExecuteOutput();

        ColorGradientDesignerNodeFactory.execute(executeInput, executeOutput);

        final var outputModelObj = (ColorHandlerPortObject)executeOutput.m_outData[1];
        final var outputModel = outputModelObj.getSpec().getColumnSpec(0).getColorHandler().getColorModel();
        final var specialColors = ColorGradientDesignerNodeFactoryConfigureTest.createSpecialColors(parameters);
        final var expectedColorModel =
            new ColorModelRange2(specialColors, ColorGradient.CIVIDIS).applyToDomain(-50, 50);

        assertEquals(2, executeOutput.m_outData.length);
        assertEquals(BufferedDataTable.class, executeOutput.m_outData[0].getClass());
        assertEquals(ColorHandlerPortObject.class, outputModelObj.getClass());
        assertEquals(expectedColorModel, outputModel);
    }

    @Test
    void testExecuteThrowsWhenNoDomainAndTableIsEmpty() {
        testExecuteThrows(new Object[0][], "because they do not have a domain and the input table is empty");
    }

    @Test
    void testExecuteThrowsWhenNoDomainAndSelectedColumnsContainOnlyNonNumericValuesNaN() {
        testExecuteThrows(new Object[][]{{Double.NaN}},
            "because they do not have a domain and the column values are either missing or NaN");
    }

    @Test
    void testExecuteThrowsWhenNoDomainAndSelectedColumnsContainOnlyNonNumericValuesMissing() {
        testExecuteThrows(new Object[][]{{new MissingCell(null)}},
            "because they do not have a domain and the column values are either missing or NaN");
    }

    private static BufferedDataTable[] createTestTable(final DataColumnSpec[] colSpecs, final Object[][] rowValues) {
        final var tableBuilder = new TableBuilder(new DataTableSpec(colSpecs));
        for (final var rowValue : rowValues) {
            tableBuilder.addRow(rowValue);
        }
        return new BufferedDataTable[]{tableBuilder.buildDataTable()};
    }

    private static void testExecuteThrows(final Object[][] rowValues, final String messagePart) {
        testExecuteThrows(createTestTable(new DataColumnSpec[]{TableTestUtil.createColumnWithNoDomain()}, rowValues),
            messagePart);
    }

    private static void testExecuteThrows(final BufferedDataTable[] tables, final String messagePart) {
        final var parameters = new ColorGradientDesignerNodeParameters();
        final var columns = tables[0].getDataTableSpec().getColumnNames();
        parameters.m_columnFilter = new ColumnFilter(columns);
        final var executeInput = new TestExecuteInput(parameters, tables);
        final var executeOutput = new TestExecuteOutput();
        final var ex = assertThrows(KNIMEException.class,
            () -> ColorGradientDesignerNodeFactory.execute(executeInput, executeOutput));
        assertTrue(ex.getMessage().contains(messagePart));
    }
}
