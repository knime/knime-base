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
 *   18 Jan 2023 (Manuel Hotz, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.base.node.preproc.rowagg;


import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.knime.base.data.aggregation.GlobalSettings;
import org.knime.base.data.aggregation.GlobalSettings.AggregationContext;
import org.knime.base.data.aggregation.GlobalSettings.GlobalSettingsBuilder;
import org.knime.base.data.aggregation.OperatorColumnSettings;
import org.knime.base.node.preproc.rowagg.RowAggregatorNodeModel.AggregationFunction;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataTableSpecCreator;
import org.knime.core.data.DataType;
import org.knime.core.data.RowKey;
import org.knime.core.data.def.BooleanCell;
import org.knime.core.data.def.ComplexNumberCell;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.FuzzyNumberCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.LongCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModelWarningListener;
import org.knime.core.node.message.Message;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.inactive.InactiveBranchPortObject;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.util.Pair;
import org.knime.core.webui.node.dialog.impl.ColumnFilter;
import org.knime.testing.core.ExecutionContextExtension;

/**
 * Tests for the {@code RowAggregatorNodeModel}.
 *
 * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
 */
@SuppressWarnings("restriction") // WebUI* still in preview
@ExtendWith({ExecutionContextExtension.class})
final class RowAggregatorNodeModelTest {

    private static final int AGG_OUT = 0;
    private static final int TOTALS_OUT = 1;

    private static final String AGG_COL_NAME = "AGG_COL";
    private static final String WEIGHT_COL_NAME = "WEIGHT_COL";
    private static final String CLASS_COL_NAME = "class";
    private static final String OCCURRENCE_COUNT_COL_NAME = "OCCURRENCE_COUNT";

    private static final RowAggregatorNodeFactory FACTORY = new RowAggregatorNodeFactory();

    private static final BiFunction<ExecutionContext, DataTableSpec, BufferedDataTable> EMPTY_TABLE =
            (ctx, dts) -> fromRows(ctx, dts, Stream.empty());

    private static final BiFunction<ExecutionContext, DataTableSpec, BufferedDataTable> MISSINGS_ROW =
            (ctx, dts) -> fromRows(ctx, dts, Stream.of(new DefaultRow(RowKey.createRowKey(0L),
                Arrays.stream(dts.getColumnNames()).map(c -> DataType.getMissingCell()).collect(Collectors.toList()))));
    private static final Consumer<DataRow> MISSINGS_ROW_EXPECTED = row -> {
            final var numCells = row.getNumCells();
            for (int i = 0; i < numCells; i++) {
                final var c = row.getCell(i);
                assertEquals(DataType.getMissingCell(), c, String.format("Expected missing cell at column %d.", i));
            }
        };

    private static final String EX_MSG_EMPTY_IN_OUT = "Empty input should result in empty output.";
    private static final Supplier<String> EX_MSG_SAME_STRUCTURE = () -> "Expected same types and names for aggregate";
    private static final Supplier<String> EX_MSG_EXP_AC_SPEC =
            () -> "Expected and actual aggregated table specs differ.";
    private static final Supplier<String> EX_MSG_EXP_AC_TOTALS_SPEC =
            () -> "Expected and actual totals table specs differ.";

    private RowAggregatorNodeModel m_rowAgg;
    private RowAggregatorSettings m_settings;

    @BeforeEach
    void setUp() {
        m_rowAgg = FACTORY.createNodeModel();
        m_settings = new RowAggregatorSettings();
    }

    @AfterEach
    void tearDown() {
        m_rowAgg = null;
        m_settings = null;
    }

    // ===== Test for the node model behavior

    // Aggregation behavior for each aggregated column / COUNT*
    //
    // INPUT                | AGG       | GroupBy | OUTPUT 1 (By Group or Totals)       |  OUTPUT 2 (Totals)
    // ---------------------------------------------------------------------------------------------------
    // Empty                | COUNT*    | Yes     | empty output                        | COUNT=0
    // Empty                | COUNT*    | No      | COUNT=0                             | INACTIVE
    // Non-empty            | COUNT*    | Yes     | each group: size                    | COUNT=size(table)
    // Non-empty            | COUNT*    | No      | COUNT=size(table)                   | INACTIVE

    // Empty                | AGG(col)  | Yes     | empty output                        | single row, missing cell
    // Empty                | AGG(col)  | No      | single row, missing cell            | INACTIVE
    // Non-empty            | AGG(col)  | Yes     | each group: miss if all miss or agg | single row, miss if
    //                                                                                      all miss or agg
    // Non-empty            | AGG(col)  | No      | single row, miss if all miss or agg | INACTIVE


    /**
     * Test aggregating with group-by column.
     *
     * @param ctx execution context
     * @throws Exception execution cancelled or configuration
     */
    @Test
    void testExecuteGroupByAggregate(final ExecutionContext ctx) throws Exception {
        // tests the Group-by aggregate behavior:
        // Input                | Output 1                            | Output 2
        // ----------------------------------------------------------------------------------------
        // Empty                | empty output                        | single row, missing cell
        // Non-empty            | each group: miss if all miss or agg | single row, miss if
        //                                                                all miss or agg

        final var colNames = new String[] { AGG_COL_NAME, CLASS_COL_NAME };
        final var colTypes = new DataType[] { IntCell.TYPE, StringCell.TYPE };
        final var dts = Column.toSpec(colNames, colTypes);

        final var expectedNames = new String[] { CLASS_COL_NAME, AGG_COL_NAME };
        final var expectedTypes = new DataType[] { StringCell.TYPE, IntCell.TYPE };
        final var expectedDts = Column.toSpec(expectedNames, expectedTypes);

        // grand total result has no class column
        final var expectedTotalsNames = new String[] { AGG_COL_NAME };
        final var expectedTotalsTypes = new DataType[] { IntCell.TYPE };
        final var expectedTotalsDts = Column.toSpec(expectedTotalsNames, expectedTotalsTypes);


        // sum up each of the columns
        groupByAggregate(m_settings,
            AggregationFunction.SUM,
            CLASS_COL_NAME,
            except(colNames, Set.of(CLASS_COL_NAME)),
            null, // no weight
            true
        );
        m_rowAgg.configure(new PortObjectSpec[] { dts }, m_settings);


        // test empty input results in...
        final var resultOnEmpty = execute(ctx, m_rowAgg, new DataTable[] { EMPTY_TABLE.apply(ctx, dts) }, m_settings);
        // ... empty output on AGG port and ...
        assertEqualStructure(expectedDts, (DataTableSpec)resultOnEmpty[AGG_OUT].getSpec(), EX_MSG_EXP_AC_SPEC);
        assertEmpty(resultOnEmpty, AGG_OUT, EX_MSG_EMPTY_IN_OUT);
        // ... single row with missing cell at TOTALS port
        assertEqualStructure(expectedTotalsDts, (DataTableSpec)resultOnEmpty[TOTALS_OUT].getSpec(),
            EX_MSG_EXP_AC_TOTALS_SPEC);
        assertTablesEqual(MISSINGS_ROW.apply(ctx, expectedTotalsDts), resultOnEmpty, TOTALS_OUT);


        // test only missing input, should result in missing cell(s) as output
        assertMissingInMissingOut(ctx, m_rowAgg, m_settings, dts, expectedDts, expectedTotalsDts);


        // test sum of values [1,4] (=10) (ignoring missing cells)
        final var input = fromRows(ctx, dts,
            IntStream.range(1, 6).mapToObj(i -> {
                if (i < 5) {
                    return new DefaultRow(RowKey.createRowKey((long)i),
                        List.of(new IntCell(i), new StringCell(evenOdd(i))));
                } else {
                    // one missing cell at the end that should be ignored
                    return new DefaultRow(RowKey.createRowKey((long)i),
                        List.of(DataType.getMissingCell(), new StringCell(evenOdd(i))));
                }
            }
        ));
        final var result = execute(ctx, m_rowAgg, new DataTable[] { input }, m_settings);
        final DataTableSpec aggResultSpec = (DataTableSpec)result[AGG_OUT].getSpec();

        assertEqualStructure(expectedDts, aggResultSpec, EX_MSG_SAME_STRUCTURE);
        // sum of [1, 4] groupbed by "class", with no missing cells
        final var expected = fromRows(ctx, expectedDts, Stream.of(
            new DefaultRow(RowKey.createRowKey(0L), List.of(new StringCell("even"), new IntCell(6))),
            new DefaultRow(RowKey.createRowKey(1L), List.of(new StringCell("odd"), new IntCell(4)))
        ));
        assertTablesEqual(expected, result, AGG_OUT);


        final DataTableSpec totalsResultSpec = (DataTableSpec)result[TOTALS_OUT].getSpec();
        assertEqualStructure(expectedTotalsDts, totalsResultSpec, () -> "Expected same types and names for totals");
        // sum of [1, 4], with no missing cells
        final var expectedTotals = fromRows(ctx, expectedTotalsDts, Stream.of(
            new DefaultRow(RowKey.createRowKey(0L), List.of(new IntCell(10)))
        ));
        assertTablesEqual(expectedTotals, result, TOTALS_OUT);
    }


    /**
     * Test that second port is inactive if no totals requested.
     *
     * @param ctx execution context
     * @throws Exception execution cancelled or configuration
     */
    @Test
    void testExecuteGroupByAggregateNoTotals(final ExecutionContext ctx) throws Exception {
        final var colNames = new String[] { AGG_COL_NAME, CLASS_COL_NAME };
        final var colTypes = new DataType[] { IntCell.TYPE, StringCell.TYPE };
        final var dts = Column.toSpec(colNames, colTypes);

        // sum up each of the columns
        groupByAggregate(m_settings,
            AggregationFunction.SUM,
            CLASS_COL_NAME,
            except(colNames, Set.of(CLASS_COL_NAME)),
            null, // no weight
            false // TOTALS port should be INACTIVE
        );
        m_rowAgg.configure(new PortObjectSpec[] { dts }, m_settings);

        // test that second port is inactive if configured
        final var resultOnEmpty = execute(ctx, m_rowAgg, new DataTable[] { EMPTY_TABLE.apply(ctx, dts) }, m_settings);
        assertInactive(resultOnEmpty, TOTALS_OUT);
    }


    /**
     * Test aggregating without group-by column.
     *
     * @param ctx execution context
     * @throws Exception execution cancelled or configuration
     */
    @Test
    void testExecuteTotalsAggregate(final ExecutionContext ctx) throws Exception {
        // tests the totals-only aggregate behavior:
        // Input                | Output 1                            | Output 2
        // ----------------------------------------------------------------------------------------
        // Empty                | single row, missing cell            | INACTIVE
        // Non-empty            | single row, miss if all miss or agg | INACTIVE

        final var colNames = new String[] { AGG_COL_NAME, CLASS_COL_NAME };
        final var colTypes = new DataType[] { IntCell.TYPE, StringCell.TYPE };
        final var dts = Column.toSpec(colNames, colTypes);

        final var expectedNames = new String[] { AGG_COL_NAME };
        final var expectedTypes = new DataType[] { IntCell.TYPE };
        final var expectedDts = Column.toSpec(expectedNames, expectedTypes);


        // sum up each of the columns
        groupByAggregate(m_settings,
            AggregationFunction.SUM,
            null,
            except(colNames, Set.of(CLASS_COL_NAME)),
            null, // no weight
            false // ignored
        );
        m_rowAgg.configure(new PortObjectSpec[] { dts }, m_settings);


        // test empty input results in...
        final var resultOnEmpty = execute(ctx, m_rowAgg, new DataTable[] { EMPTY_TABLE.apply(ctx, dts) }, m_settings);
        // ... single row with missing cell at AGG port ...
        assertEqualStructure(expectedDts, (DataTableSpec)resultOnEmpty[AGG_OUT].getSpec(), EX_MSG_EXP_AC_SPEC);
        assertTablesEqual(MISSINGS_ROW.apply(ctx, expectedDts), resultOnEmpty, AGG_OUT);
        // ... and INACTIVE at TOTALS port
        assertInactive(resultOnEmpty, TOTALS_OUT);


        // test only missing input, should result in missing cell(s) as output
        assertMissingInMissingOut(ctx, m_rowAgg, m_settings, dts, expectedDts, null);


        // test sum of values [1,4] (=10) (ignoring missing cells)
        final var input = fromRows(ctx, dts,
            IntStream.range(1, 6).mapToObj(i -> {
                if (i < 5) {
                    return new DefaultRow(RowKey.createRowKey((long)i),
                        List.of(new IntCell(i), new StringCell(evenOdd(i))));
                } else {
                    // one missing cell at the end that should be ignored
                    return new DefaultRow(RowKey.createRowKey((long)i),
                        List.of(DataType.getMissingCell(), new StringCell(evenOdd(i))));
                }
            }
        ));
        final var result = execute(ctx, m_rowAgg, new DataTable[] { input }, m_settings);
        final DataTableSpec aggResultSpec = (DataTableSpec)result[AGG_OUT].getSpec();

        assertEqualStructure(expectedDts, aggResultSpec, EX_MSG_SAME_STRUCTURE);
        // sum of [1, 4], with no missing cells
        final var expected = fromRows(ctx, expectedDts, Stream.of(
            new DefaultRow(RowKey.createRowKey(0L), new IntCell(10))
        ));
        assertTablesEqual(expected, result, AGG_OUT);
        // ... totals still inactive
        assertInactive(result, TOTALS_OUT);
    }

    /**
     * Test counting with group-by column.
     *
     * @param ctx execution context
     * @throws Exception execution cancelled or configuration
     */
    @Test
    void testExecuteGroupByCount(final ExecutionContext ctx) throws Exception {
        // tests the Group-by aggregate behavior:
        // Input                | Output 1                            | Output 2
        // ----------------------------------------------------------------------------------------
        // Empty                | empty output                        | COUNT=0
        // Non-empty            | each group: size                    | COUNT=size(table)

        final var colNames = new String[] { AGG_COL_NAME, CLASS_COL_NAME };
        final var colTypes = new DataType[] { IntCell.TYPE, StringCell.TYPE };
        final var dts = Column.toSpec(colNames, colTypes);

        final var expectedNames = new String[] { CLASS_COL_NAME, OCCURRENCE_COUNT_COL_NAME };
        final var expectedTypes = new DataType[] { StringCell.TYPE, LongCell.TYPE };
        final var expectedDts = Column.toSpec(expectedNames, expectedTypes);

        // grand total result has no class column
        final var expectedTotalsNames = new String[] { OCCURRENCE_COUNT_COL_NAME };
        final var expectedTotalsTypes = new DataType[] { LongCell.TYPE };
        final var expectedTotalsDts = Column.toSpec(expectedTotalsNames, expectedTotalsTypes);


        // sum up each of the columns
        groupByAggregate(m_settings,
            AggregationFunction.COUNT,
            CLASS_COL_NAME,
            except(colNames, Set.of(CLASS_COL_NAME)),
            null, // no weight
            true
        );
        m_rowAgg.configure(new PortObjectSpec[] { dts }, m_settings);


        // test empty input results in...
        final var resultOnEmpty = execute(ctx, m_rowAgg, new DataTable[] { EMPTY_TABLE.apply(ctx, dts) }, m_settings);
        // ... empty output on AGG port and ...
        assertEqualStructure(expectedDts, (DataTableSpec)resultOnEmpty[AGG_OUT].getSpec(), EX_MSG_EXP_AC_SPEC);
        assertEmpty(resultOnEmpty, AGG_OUT, EX_MSG_EMPTY_IN_OUT);
        // ... single row COUNT=0 at TOTALS port
        assertEqualStructure(expectedTotalsDts, (DataTableSpec)resultOnEmpty[TOTALS_OUT].getSpec(),
            EX_MSG_EXP_AC_TOTALS_SPEC);
        assertSingleRowResult(resultOnEmpty, TOTALS_OUT, row -> {
            final var count = ((LongCell) row.getCell(0)).getLongValue();
            assertEquals(0, count, "Expected COUNT=0");
        });

        // test cound should also count missing cells
        final var input = fromRows(ctx, dts,
            IntStream.range(1, 6).mapToObj(i -> {
                if (i < 5) {
                    return new DefaultRow(RowKey.createRowKey((long)i),
                        List.of(new IntCell(i), new StringCell(evenOdd(i))));
                } else {
                    // one missing cell at the end that should be ignored
                    return new DefaultRow(RowKey.createRowKey((long)i),
                        List.of(DataType.getMissingCell(), new StringCell(evenOdd(i))));
                }
            }
        ));
        final var result = execute(ctx, m_rowAgg, new DataTable[] { input }, m_settings);
        final DataTableSpec aggResultSpec = (DataTableSpec)result[AGG_OUT].getSpec();

        assertEqualStructure(expectedDts, aggResultSpec, EX_MSG_SAME_STRUCTURE);
        // counts groupbed by "class", including missing cells
        final var expected = fromRows(ctx, expectedDts, Stream.of(
            new DefaultRow(RowKey.createRowKey(0L), List.of(new StringCell("even"), new LongCell(2))),
            new DefaultRow(RowKey.createRowKey(1L), List.of(new StringCell("odd"), new LongCell(3)))
        ));
        assertTablesEqual(expected, result, AGG_OUT);


        final DataTableSpec totalsResultSpec = (DataTableSpec)result[TOTALS_OUT].getSpec();
        assertEqualStructure(expectedTotalsDts, totalsResultSpec, () -> "Expected same types and names for totals");
        final var expectedTotals = fromRows(ctx, expectedTotalsDts, Stream.of(
            new DefaultRow(RowKey.createRowKey(0L), List.of(new LongCell(5)))
        ));
        assertTablesEqual(expectedTotals, result, TOTALS_OUT);
    }


    /**
     * Test aggregating without group-by column but with weight column.
     *
     * @param ctx execution context
     * @throws Exception execution cancelled or configuration
     */
    @Test
    void testExecuteTotalsWeightedAggregate(final ExecutionContext ctx) throws Exception {
        final var colNames = new String[] { AGG_COL_NAME, WEIGHT_COL_NAME, CLASS_COL_NAME };
        final var colTypes = new DataType[] { IntCell.TYPE, BooleanCell.TYPE, StringCell.TYPE };
        final var dts = Column.toSpec(colNames, colTypes);

        final var expectedNames = new String[] { AGG_COL_NAME };
        final var expectedTypes = new DataType[] { IntCell.TYPE };
        final var expectedDts = Column.toSpec(expectedNames, expectedTypes);

        // sum up each of the columns, using the boolean column effectively as a filter
        groupByAggregate(m_settings,
            AggregationFunction.SUM,
            null,
            except(colNames, Set.of(CLASS_COL_NAME, WEIGHT_COL_NAME)),
            WEIGHT_COL_NAME,
            false // ignored
        );
        m_rowAgg.configure(new PortObjectSpec[] { dts }, m_settings);


        // test empty input results in...
        final var resultOnEmpty = execute(ctx, m_rowAgg, new DataTable[] { EMPTY_TABLE.apply(ctx, dts) }, m_settings);
        // ... single row with missing cell at AGG port ...
        assertEqualStructure(expectedDts, (DataTableSpec)resultOnEmpty[AGG_OUT].getSpec(), EX_MSG_EXP_AC_SPEC);
        assertTablesEqual(MISSINGS_ROW.apply(ctx, expectedDts), resultOnEmpty, AGG_OUT);
        // ... and INACTIVE at TOTALS port
        assertInactive(resultOnEmpty, TOTALS_OUT);

        // test only missing input, should result in missing cell(s) as output
        assertMissingInMissingOut(ctx, m_rowAgg, m_settings, dts, expectedDts, null);

        // test sum of even values [1,4] (=6) (ignoring missing cells)
        final var input = fromRows(ctx, dts,
            IntStream.range(1, 6).mapToObj(i -> {
                if (i < 5) {
                    return new DefaultRow(RowKey.createRowKey((long)i),
                        List.of(new IntCell(i), booleanCell(isEven(i)), new StringCell(evenOdd(i))));
                } else {
                    // one missing cell at the end that should be ignored
                    return new DefaultRow(RowKey.createRowKey((long)i),
                        List.of(DataType.getMissingCell(), booleanCell(isEven(i)), new StringCell(evenOdd(i))));
                }
            }
        ));
        final var result = execute(ctx, m_rowAgg, new DataTable[] { input }, m_settings);
        final DataTableSpec aggResultSpec = (DataTableSpec)result[AGG_OUT].getSpec();

        assertEqualStructure(expectedDts, aggResultSpec, EX_MSG_SAME_STRUCTURE);
        // sum of even values in [1, 4], with no missing cells
        final var expected = fromRows(ctx, expectedDts, Stream.of(
            new DefaultRow(RowKey.createRowKey(0L), new IntCell(6))
        ));
        assertTablesEqual(expected, result, AGG_OUT);
        // ... totals still inactive
        assertInactive(result, TOTALS_OUT);
    }

    // ===== Tests for the aggregation functions

    @SuppressWarnings("static-method")
    @Test
    void testSumAggregates(final ExecutionContext ctx) {
        // test sum of values [1,4] (=10) (ignoring missing cells)
        final var testInput = createTestInput(ctx);
        final var dts = testInput.getFirst();
        final var input = testInput.getSecond();
        final var colNames = dts.getColumnNames();
        final var colTypes = IntStream.range(0, dts.getNumColumns())
                .mapToObj(i -> dts.getColumnSpec(i))
                .map(DataColumnSpec::getType)
                .toArray(DataType[]::new);

        // aggregate grand total sum for each of the types
        final var method = AggregationFunction.SUM;
        final var results = new DataCell[] {  new IntCell(10), new LongCell(10), new DoubleCell(10) };
        final var gs = createGlobalSettingsBuilder()
                .setDataTableSpec(dts)
                .setGroupColNames(Collections.emptyList())
                .setNoOfRows(input.size()).build();
        for (int i = 0; i < colNames.length; i++) {
            final var colIdx = i;
            final var type = colTypes[colIdx];
            final var ocs = new OperatorColumnSettings(false, dts.getColumnSpec(colIdx));

            final var aggOp = method.getOperator(null, gs, ocs).createInstance(gs, ocs);

            input.forEach(row -> aggOp.compute(row, colIdx));
            final var res = aggOp.getResult();
            assertEquals(type, res.getType(), "Aggregation result type differs");
            assertEquals(results[i], res, "Aggregation result differs");
        }
    }

    @SuppressWarnings("static-method")
    @Test
    void testWeightedSumAggregates(final ExecutionContext ctx) {
        // test sum of values [1,4] weighted by itself (type should stay same)
        final var testInput = createTestInput(ctx);
        final var dts = testInput.getFirst();
        final var input = testInput.getSecond();
        final var colNames = dts.getColumnNames();
        final var colTypes = IntStream.range(0, dts.getNumColumns())
                .mapToObj(i -> dts.getColumnSpec(i))
                .map(DataColumnSpec::getType)
                .toArray(DataType[]::new);

        // aggregate grand total weighted sum for each of the types
        final var method = AggregationFunction.SUM;
        final var results = new DataCell[] {  new IntCell(30), new LongCell(30), new DoubleCell(30) };
        final var gs = createGlobalSettingsBuilder()
                .setDataTableSpec(dts)
                .setGroupColNames(Collections.emptyList())
                .setNoOfRows(input.size()).build();
        for (int i = 0; i < colNames.length; i++) {
            final var colIdx = i;
            final var type = colTypes[colIdx];
            final var ocs = new OperatorColumnSettings(false, dts.getColumnSpec(colIdx));

            final var aggOp = method.getOperator(colNames[colIdx], gs, ocs).createInstance(gs, ocs);

            input.forEach(row -> aggOp.compute(row, colIdx));
            final var res = aggOp.getResult();
            assertEquals(type, res.getType(), "Aggregation result type differs");
            assertEquals(results[i], res, "Aggregation result differs");
        }
    }

    @SuppressWarnings("static-method")
    @Test
    void testAvgAggregates(final ExecutionContext ctx) {
        // test avg of values [1,4] (ignoring missing cells)
        final var testInput = createTestInput(ctx);
        final var dts = testInput.getFirst();
        final var input = testInput.getSecond();
        final var colNames = dts.getColumnNames();

        // aggregate grand total average for each of the types
        final var method = AggregationFunction.AVERAGE;
        final var results = new DataCell[] {  new DoubleCell(2.5), new DoubleCell(2.5), new DoubleCell(2.5) };
        final var gs = createGlobalSettingsBuilder()
                .setDataTableSpec(dts)
                .setGroupColNames(Collections.emptyList())
                .setNoOfRows(input.size()).build();
        for (int i = 0; i < colNames.length; i++) {
            final var colIdx = i;
            final var ocs = new OperatorColumnSettings(false, dts.getColumnSpec(colIdx));
            final var aggOp = method.getOperator(null, gs, ocs).createInstance(gs, ocs);
            input.forEach(row -> aggOp.compute(row, colIdx));
            final var res = aggOp.getResult();
            assertEquals(DoubleCell.TYPE, res.getType(), "Aggregation result type differs");
            assertEquals(results[i], res, "Aggregation result differs");
        }
    }

    @SuppressWarnings("static-method")
    @Test
    void testWeightedAverageAggregates(final ExecutionContext ctx) {
        // test average of values [1,4] weighted by itself (type should stay same)
        final var testInput = createTestInput(ctx);
        final var dts = testInput.getFirst();
        final var input = testInput.getSecond();
        final var colNames = dts.getColumnNames();

        // aggregate grand total weighted sum for each of the types
        final var method = AggregationFunction.AVERAGE;
        final var results = new DataCell[] {  new DoubleCell(7.5), new DoubleCell(7.5), new DoubleCell(7.5) };
        final var gs = createGlobalSettingsBuilder()
                .setDataTableSpec(dts)
                .setGroupColNames(Collections.emptyList())
                .setNoOfRows(input.size()).build();
        for (int i = 0; i < colNames.length; i++) {
            final var colIdx = i;
            final var ocs = new OperatorColumnSettings(false, dts.getColumnSpec(colIdx));

            final var aggOp = method.getOperator(colNames[colIdx], gs, ocs).createInstance(gs, ocs);

            input.forEach(row -> aggOp.compute(row, colIdx));
            final var res = aggOp.getResult();
            assertEquals(DoubleCell.TYPE, res.getType(), "Aggregation result type differs");
            assertEquals(results[i], res, "Aggregation result differs");
        }
    }

    /**
     * Tests that cells of types not compatible with the supported number types (double, int, long),
     * are not retained by the numeric choices provider (indirectly by testing the method that does the actual
     * filtering).
     *
     * @param ctx execution context
     */
    @SuppressWarnings("static-method")
    @Test
    void testFilterUnsupportedColumnTypes(final ExecutionContext ctx) {
        final var in = new DataTableSpecCreator().addColumns(
            new DataColumnSpecCreator("boolean", BooleanCell.TYPE).createSpec(), // compatible
            new DataColumnSpecCreator("int", IntCell.TYPE).createSpec(),
            new DataColumnSpecCreator("long", LongCell.TYPE).createSpec(),
            new DataColumnSpecCreator("double", DoubleCell.TYPE).createSpec(),
            new DataColumnSpecCreator("string", StringCell.TYPE).createSpec(), // not compatible
            new DataColumnSpecCreator("complexNumber", ComplexNumberCell.TYPE).createSpec(), // not compatible
            new DataColumnSpecCreator("fuzzyNumber", FuzzyNumberCell.TYPE).createSpec() // not compatible
        ).createSpec();

        final var expected = new String[] { "boolean", "int", "long", "double" };

        final var resAggColumn = in.stream().filter(RowAggregatorNodeModel::isAggregatableColumn)//
                .map(DataColumnSpec::getName)//
                .toArray(String[]::new);
        assertArrayEquals(expected, resAggColumn,
            "Filter should only retain 'numeric-compatible' columns for aggregate column");

        final var resWeightColumn = in.stream().filter(RowAggregatorNodeModel::isWeightColumn)//
                .map(DataColumnSpec::getName)//
                .toArray(String[]::new);
        assertArrayEquals(expected, resWeightColumn,
                "Filter should only retain 'numeric-compatible' columns for weight column");
    }

    /**
     * Tests incorrect usage of {@code AggregationFunction.getOperator()} method for weight-less agg functions.
     */
    @Test
    @SuppressWarnings("static-method")
    void testGetOperatorInvalidCombinations() {
        assertThrows(IllegalStateException.class,
            () -> RowAggregatorNodeModel.AggregationFunction.COUNT.getOperator("weight", null, null));
        assertThrows(UnsupportedOperationException.class,
            () -> RowAggregatorNodeModel.AggregationFunction.MIN.getOperator("weight", null, null));
    }

    // ==== InvalidSettingsException conditions

    @Test
    void testNoColumnsWarning() throws InvalidSettingsException {
        final var dts = Column.toSpec(Stream.empty());
        groupByAggregate(m_settings, AggregationFunction.COUNT, null, null, null, false);
        final var count = new AtomicInteger();
        m_rowAgg.addWarningListener(new NodeModelWarningListener() {
            @Override
            public void warningChanged(final Message warning) {
                count.incrementAndGet();
                assertEquals("Input table should contain at least one column.", warning.getSummary(),
                    "Unexpected warning message set");
            }
        });
        m_rowAgg.configure(new PortObjectSpec[] { dts }, m_settings);
        assertEquals(1, count.get(), "Did not set expected number of warnings during configure");
    }

    @Test
    void testEmptyAggregatedColumnsInvalidSettingsExceptions() {
        final var dts = createTestDts();
        groupByAggregate(m_settings,
            AggregationFunction.SUM,
            null,
            new String[0], // this should be responsible for "missing frequency columns" exception
            null,
            false
        );
        assertThrows(InvalidSettingsException.class,
            () -> m_rowAgg.configure(new PortObjectSpec[] { dts }, m_settings));
    }

    @Test
    void testSingleMissingAggregatedColumnsInvalidSettingsExceptions() {
        final var dts = createTestDts();
        groupByAggregate(m_settings,
            AggregationFunction.SUM,
            null,
            new String[] { "missingAgg1" }, // this should be responsible for "missing frequency columns" exception
            null,
            false
        );
        final var msg = assertThrows(InvalidSettingsException.class,
            () -> m_rowAgg.configure(new PortObjectSpec[] { dts }, m_settings)).getMessage();
        assertTrue(msg.contains("column:"), "Expected singular form of \"column\" for single missing column.");
    }

    @Test
    void testMultipleMissingAggregatedColumnsInvalidSettingsExceptions() {
        final var dts = createTestDts();
        groupByAggregate(m_settings,
            AggregationFunction.SUM,
            null,
            new String[] { "missingAgg1", "missingAgg2" }, // this should be responsible for "missing frequency columns" exception
            null,
            false
        );
        final var msg = assertThrows(InvalidSettingsException.class,
            () -> m_rowAgg.configure(new PortObjectSpec[] { dts }, m_settings)).getMessage();
        assertTrue(msg.contains("columns:"), "Expected plural of \"column\" for multiple missing columns.");
    }

    @Test
    void testWeightColumnInvalidSettingsExceptions() {
        final var dts = createTestDts();
        groupByAggregate(m_settings,
            AggregationFunction.SUM,
            null,
            new String[] { AGG_COL_NAME },
            "missingWeightColumnName", // responsible for "missing weight column" exception
            false
        );
        assertThrows(InvalidSettingsException.class,
            () -> m_rowAgg.configure(new PortObjectSpec[] { dts }, m_settings));
    }

    @Test
    void testGroupByColumnInvalidSettingsExceptions() {
        final var dts = createTestDts();
        groupByAggregate(m_settings,
            AggregationFunction.SUM,
            "missingGroupByColumnName",  // responsible for "missing groupBy column" exception
            new String[] { AGG_COL_NAME },
            null,
            false
        );
        assertThrows(InvalidSettingsException.class,
            () -> m_rowAgg.configure(new PortObjectSpec[] { dts }, m_settings));
    }

    @Test
    void testAggregatedColumnsPlaceholderInvalidSettingsExceptions() {
        final var dts = createTestDts();
        groupByAggregate(m_settings,
            AggregationFunction.SUM,
            null,
            // this should be responsible for "missing frequency columns" exception since it contains a UI placeholder
            new String[] {"<none>"},
            null,
            false
        );
        assertThrows(InvalidSettingsException.class,
            () -> m_rowAgg.configure(new PortObjectSpec[] { dts }, m_settings));
    }

    // ===== Test Helpers

    private static DataTableSpec createTestDts() {
        final var colNames = new String[] { AGG_COL_NAME, WEIGHT_COL_NAME, CLASS_COL_NAME };
        final var colTypes = new DataType[] { IntCell.TYPE, BooleanCell.TYPE, StringCell.TYPE };
        final var dts = Column.toSpec(colNames, colTypes);
        return dts;
    }

    private static GlobalSettingsBuilder createGlobalSettingsBuilder() {
        return GlobalSettings.builder()
                .setAggregationContext(AggregationContext.ROW_AGGREGATION)
                .setValueDelimiter(GlobalSettings.STANDARD_DELIMITER);
    }

    private static Pair<DataTableSpec, BufferedDataTable> createTestInput(final ExecutionContext ctx) {
        final var colNames = new String[] { "Integer", "Long", "Double"};
        final var colTypes = new DataType[] { IntCell.TYPE, LongCell.TYPE, DoubleCell.TYPE};
        final var dts = Column.toSpec(colNames, colTypes);

        final var input = fromRows(ctx, dts,
            IntStream.range(1, 6).mapToObj(i -> {
                if (i < 5) {
                    return new DefaultRow(RowKey.createRowKey((long)i),
                        List.of(new IntCell(i), new LongCell(i), new DoubleCell(i)));
                } else {
                    // one missing cell at the end that should be ignored
                    return new DefaultRow(RowKey.createRowKey((long)i),
                        List.of(DataType.getMissingCell(), DataType.getMissingCell(), DataType.getMissingCell()));
                }
            }
        ));
        return Pair.create(dts,  input);
    }

    private static String[] except(final String[] s, final Set<String> except) {
        return Arrays.stream(s).filter(c -> !except.contains(c)).toArray(String[]::new);
    }

    private static RowAggregatorSettings groupByAggregate(final RowAggregatorSettings settings,
            final AggregationFunction agg, final String groupByColumn, final String[] aggregatedColumns,
            final String weightColumn, final boolean grandTotal) {
        settings.m_aggregationMethod = agg;
        if (groupByColumn != null) {
            settings.m_categoryColumn = groupByColumn;
        }
        settings.m_frequencyColumns = new ColumnFilter(aggregatedColumns != null ? aggregatedColumns : new String[0]);
        if (weightColumn != null) {
            settings.m_weightColumn = weightColumn;
        }
        settings.m_grandTotals = grandTotal;
        return settings;
    }

    private static void assertMissingInMissingOut(final ExecutionContext ctx, final RowAggregatorNodeModel rowAgg,
        final RowAggregatorSettings settings, final DataTableSpec inputDts, final DataTableSpec aggOutDts,
        final DataTableSpec totalsOutDts) throws Exception {

        final var inputMissing = fromRows(ctx, inputDts,
            Stream.of(missingCells(0, inputDts), missingCells(1, inputDts)));
        final var resultMissing = execute(ctx, rowAgg, new DataTable[] { inputMissing }, settings);

        assertEqualStructure(aggOutDts, (DataTableSpec)resultMissing[AGG_OUT].getSpec(), EX_MSG_EXP_AC_SPEC);
        assertSingleRowResult(resultMissing, AGG_OUT, MISSINGS_ROW_EXPECTED);

        if (totalsOutDts != null) {
            assertEqualStructure(totalsOutDts, (DataTableSpec)resultMissing[TOTALS_OUT].getSpec(),
                EX_MSG_EXP_AC_TOTALS_SPEC);
            assertSingleRowResult(resultMissing, TOTALS_OUT, MISSINGS_ROW_EXPECTED);
        } else {
            assertInactive(resultMissing, TOTALS_OUT);
        }
    }


    private static BufferedDataTable fromRows(final ExecutionContext ctx, final DataTableSpec spec,
            final Stream<DataRow> rows) {
        final var container = ctx.createDataContainer(spec);
        rows.forEach(container::addRowToTable);
        container.close();
        return container.getTable();
    }

    private static DataRow missingCells(final long rowKeyValue, final DataTableSpec spec) {
        return new DefaultRow(RowKey.createRowKey(rowKeyValue), Arrays.stream(spec.getColumnNames())
            .map(c -> DataType.getMissingCell()).collect(Collectors.toList()));
    }

    private static BooleanCell booleanCell(final boolean v) {
        return v ? BooleanCell.TRUE : BooleanCell.FALSE;
    }

    private static void assertSingleRowResult(final PortObject[] results, final int idx,
        final Consumer<DataRow> rowValidator) {
        final var table = (BufferedDataTable)results[idx];
        try (final var it = table.iterator()) {
            assertTrue(it.hasNext(), String.format("Table[%d] does not contain a row.", idx));
            rowValidator.accept(it.next());
            assertFalse(it.hasNext(), String.format("Table[%d] does not contain exactly one row.", idx));
        }
    }

    private static void assertEqualStructure(final DataTableSpec expected, final DataTableSpec actual,
        final Supplier<String> messageSupplier) {
        if (!expected.equalStructure(actual)) {
            fail(messageSupplier.get() + String.format(" ==> expected: %s but was: %s", expected, actual));
        }
    }

    private static void assertRowsEqual(final DataRow expected, final DataRow actual,
        final Supplier<String> failureMessage) {
        if (expected == actual) {
            return;
        }
        final var expectedNum = expected.getNumCells();
        final var actualNum = actual.getNumCells();
        if (expectedNum != actualNum) {
            fail(failureMessage.get());
        }
        for (int i = 0; i < expectedNum; i++) {
            if (!expected.getCell(i).equals(actual.getCell(i))) {
                fail(failureMessage.get());
            }
        }
    }

    private static void assertTablesEqual(final BufferedDataTable expected, final PortObject[] result, final int idx) {
        final var actual = (BufferedDataTable)result[idx];
        final var expectedSpec = expected.getSpec();
        final var actualSpec = actual.getSpec();
        assertEqualStructure(expectedSpec, actualSpec,
            () -> String.format("Specs of expected and actual table differ: %s vs. %s", expectedSpec, actualSpec));

        try (final var exIt = expected.iterator();
                final var acIt = actual.iterator()) {
            while (exIt.hasNext()) {
                assertTrue(acIt.hasNext(), "Actual table has no next row, but expected table does.");
                final var next = exIt.next();
                final var aNext = acIt.next();
                assertRowsEqual(next, aNext, () -> String.format("Expected row: \"%s\" but was: \"%s\"", next, aNext));
            }
            if (acIt.hasNext()) {
                fail(String.format("Actual table has next row, but expected table does not. Row: %s",
                    acIt.next()));
            }
        }
    }

    private static void assertEmpty(final PortObject[] results, final int idx, final String failureMessage) {
        final var table = (BufferedDataTable)results[idx];
        try (final var it = table.iterator()) {
            if (it.hasNext()) {
                final var row = it.next();
                final var spec = table.getDataTableSpec();
                fail(String.format("%s. Table[%d] %s; first row: %s", failureMessage, idx, spec, row));
            }
        }
    }

    private static void assertInactive(final PortObject[] results, final int idx) {
        assertEquals(InactiveBranchPortObject.INSTANCE, results[idx],
            String.format("Expected inactive port object at output port %s", idx));
    }

    private static PortObject[] execute(final ExecutionContext ctx, final RowAggregatorNodeModel rowAgg,
        final DataTable[] in, final RowAggregatorSettings settings) throws Exception {
        return rowAgg.execute(ctx.createBufferedDataTables(in, ctx), ctx, settings);
    }

    static final class Column {

        final String m_name;
        final DataType m_type;

        Column(final String name, final DataType type) {
            m_name = name;
            m_type = type;
        }

        static DataTableSpec toSpec(final Stream<Column> columns) {
            return new DataTableSpec(columns
                .map(c -> new DataColumnSpecCreator(c.m_name, c.m_type).createSpec())
                .toArray(DataColumnSpec[]::new)
            );
        }

        static DataTableSpec toSpec(final String[] names, final DataType[] types) {
            if (names.length != types.length) {
                throw new IllegalArgumentException(
                    String.format("Names \"%s\" and types \"%s\" must be of same length.", Arrays.toString(names),
                        Arrays.toString(types)));
            }
            return toSpec(IntStream.range(0, names.length).mapToObj(i -> new Column(names[i], types[i])));
        }
    }

    private static boolean isEven(final int i) {
        CheckUtils.checkArgument(i > 0, "Expected positive integer.");
        return (i & 1) == 0;
    }

    private static String evenOdd(final int i) {
        return isEven(i) ? "even" : "odd";
    }
}
