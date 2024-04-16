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
 *   Mar 4, 2024 (kai): created
 */
package org.knime.base.node.preproc.rounddouble;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.knime.base.node.preproc.rounddouble.RoundDoubleNodeSettings.NumberMode;
import org.knime.base.node.preproc.rounddouble.RoundDoubleNodeSettings.OutputColumn;
import org.knime.base.node.preproc.rounddouble.RoundDoubleNodeSettings.RoundingMethod;
import org.knime.base.node.preproc.rounddouble.RoundDoubleNodeSettings.RoundingMethod.Advanced;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataTableSpecCreator;
import org.knime.core.data.RowKey;
import org.knime.core.data.def.BooleanCell;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.webui.node.dialog.defaultdialog.setting.columnfilter.ColumnFilter;
import org.knime.testing.core.ExecutionContextExtension;

/**
 * Tests the corresponding node model
 *
 * @author Kai Franze, KNIME GmbH, Germany
 */
@SuppressWarnings("restriction") // We can use 'ColumnFilter' in the test
@ExtendWith({ExecutionContextExtension.class})
final class RoundDoubleNodeModelTest {

    private static final RoundDoubleNodeFactory FACTORY = new RoundDoubleNodeFactory();

    private static final ExecutionMonitor MONITOR = new ExecutionMonitor();

    private static final String STRING_COLUMN_NAME = "String column";

    private static final String DOUBLE_COLUMN_NAME = "Double column";

    private static final String INTEGER_COLUMN_NAME = "Integer column";

    private static final String BOOLEAN_COLUMN_NAME = "Boolean column";

    private static final DataTableSpec INSPEC = new DataTableSpecCreator().addColumns( //
        new DataColumnSpecCreator(STRING_COLUMN_NAME, StringCell.TYPE).createSpec(), //
        new DataColumnSpecCreator(DOUBLE_COLUMN_NAME, DoubleCell.TYPE).createSpec(), //
        new DataColumnSpecCreator(INTEGER_COLUMN_NAME, IntCell.TYPE).createSpec(), //
        new DataColumnSpecCreator(BOOLEAN_COLUMN_NAME, BooleanCell.TYPE).createSpec()) //
        .createSpec();

    private RoundDoubleNodeModel m_model;

    private RoundDoubleNodeSettings m_settings;

    private BufferedDataTable m_inputTable;

    @BeforeEach
    void setUp(final ExecutionContext ctx) {
        m_model = FACTORY.createNodeModel();
        m_settings = new RoundDoubleNodeSettings();
        m_inputTable = getInputTable(ctx);
    }

    @AfterEach
    void tearDown() {
        m_model = null;
        m_settings = null;
        m_inputTable = null;
    }

    @Test
    void testExecutionAndAppend(final ExecutionContext ctx) throws Exception {
        m_settings.m_columnsToFormat =
            new ColumnFilter(new String[]{DOUBLE_COLUMN_NAME, INTEGER_COLUMN_NAME, BOOLEAN_COLUMN_NAME});
        m_settings.m_numberMode = NumberMode.SIGNIFICANT_DIGITS;
        m_settings.m_precision = 1;

        final var out = execute(m_inputTable, m_model, m_settings, ctx);

        assertOutputTableSpecification(out,
            new String[]{STRING_COLUMN_NAME, DOUBLE_COLUMN_NAME, INTEGER_COLUMN_NAME, BOOLEAN_COLUMN_NAME,
                DOUBLE_COLUMN_NAME + m_settings.m_suffix, INTEGER_COLUMN_NAME + m_settings.m_suffix,
                BOOLEAN_COLUMN_NAME + m_settings.m_suffix});

        try (final var iterator = out.iterator()) {
            final var row = iterator.next(); // We are only interested in the first row
            final var doubleResult = row.getCell(4); // Since we appended the columns
            final var intResult = row.getCell(5);
            final var boolResult = row.getCell(6);

            assertThat(doubleResult.getType()).as("Assert double cell type").isEqualTo(DoubleCell.TYPE);
            assertThat(((DoubleCell)doubleResult).getDoubleValue()).as("Assert double result").isEqualTo(100.0);
            assertThat(intResult.getType()).as("Assert integer cell type").isEqualTo(IntCell.TYPE);
            assertThat(((IntCell)intResult).getIntValue()).as("Assert integer result").isEqualTo(1000);
            assertThat(boolResult.getType()).as("Assert boolean cell type").isEqualTo(BooleanCell.TYPE);
            assertThat(((BooleanCell)boolResult).getBooleanValue()).as("Assert boolean result").isTrue();
        }
    }

    @Test
    void testExecutionAndReplace(final ExecutionContext ctx) throws Exception {
        m_settings.m_columnsToFormat =
            new ColumnFilter(new String[]{DOUBLE_COLUMN_NAME, INTEGER_COLUMN_NAME, BOOLEAN_COLUMN_NAME});
        m_settings.m_numberMode = NumberMode.INTEGER;
        m_settings.m_roundingMethod = new RoundingMethod(Advanced.TO_LARGER);
        m_settings.m_outputColumn = OutputColumn.REPLACE;

        final var out = execute(m_inputTable, m_model, m_settings, ctx);

        assertOutputTableSpecification(out,
            new String[]{STRING_COLUMN_NAME, DOUBLE_COLUMN_NAME, INTEGER_COLUMN_NAME, BOOLEAN_COLUMN_NAME});

        try (final var iterator = out.iterator()) {
            final var row = iterator.next(); // We are only interested in the first row
            final var intResult1 = row.getCell(1); // Since we replaced the columns
            final var intResult2 = row.getCell(2);
            final var intResult3 = row.getCell(3);

            assertThat(intResult1.getType()).as("Assert integer cell type").isEqualTo(IntCell.TYPE);
            assertThat(((IntCell)intResult1).getIntValue()).as("Assert first integer result").isEqualTo(124);
            assertThat(intResult2.getType()).as("Assert integer cell type").isEqualTo(IntCell.TYPE);
            assertThat(((IntCell)intResult2).getIntValue()).as("Assert second integer result").isEqualTo(1024);
            assertThat(intResult3.getType()).as("Assert integer cell type").isEqualTo(IntCell.TYPE);
            assertThat(((IntCell)intResult3).getIntValue()).as("Assert second integer result").isEqualTo(1);
        }
    }

    private static void assertOutputTableSpecification(final BufferedDataTable out, final String[] expected) {
        final var actual = out.getDataTableSpec().getColumnNames();
        assertThat(actual).as("Assert output column names").isEqualTo(expected);
    }

    private static BufferedDataTable getInputTable(final ExecutionContext ctx) {
        final var container = ctx.createDataContainer(INSPEC);
        container.addRowToTable(new DefaultRow(RowKey.createRowKey(0L), //
            StringCell.StringCellFactory.create("foo bar"), //
            DoubleCell.DoubleCellFactory.create(123.456789), //
            IntCell.IntCellFactory.create(1024), //
            BooleanCell.BooleanCellFactory.create(true)));
        container.close();
        return container.getTable();
    }

    private static BufferedDataTable execute(final BufferedDataTable inputTable, final RoundDoubleNodeModel model,
        final RoundDoubleNodeSettings settings, final ExecutionContext ctx) throws Exception {
        final var rearranger = model.createColumnRearranger(INSPEC, settings);
        return ctx.createColumnRearrangeTable(inputTable, rearranger, MONITOR);
    }

}
