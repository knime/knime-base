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
 *   23 Jan 2023 (manuelhotz): created
 */
package org.knime.base.node.preproc.rowagg.aggregation;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.knime.base.data.aggregation.AggregationOperator;
import org.knime.base.data.aggregation.GlobalSettings;
import org.knime.base.data.aggregation.GlobalSettings.AggregationContext;
import org.knime.base.data.aggregation.GlobalSettings.GlobalSettingsBuilder;
import org.knime.base.data.aggregation.OperatorColumnSettings;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.IntValue;
import org.knime.core.data.RowKey;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.LongCell;

/**
 * Tests for the {@link DataValueAggregate} and {@link DataValueAggregate}.
 *
 * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
 */
final class DataValueAggregateTest {

    private static final String PLACEHOLDER_ID = "TestOp_v1";
    private static final String PLACEHOLDER_LABEL = "TestOp";
    private static final String PLACEHOLDER_DESC = "Description...";


    @SuppressWarnings({"static-method", "deprecation"})
    @Test
    void testFailureModes() {
        final var dcs = new DataColumnSpecCreator("Test", IntCell.TYPE).createSpec();
        final var dts = new DataTableSpec(dcs);
        final var gs = createGlobalSettings()
                .setDataTableSpec(dts)
                .setGroupColNames(Collections.emptyList())
                .build();
        final var ocs = new OperatorColumnSettings(false, dcs);

        final var outer = DataValueAggregate.create()
                .withOperatorInfo(PLACEHOLDER_ID, PLACEHOLDER_LABEL, PLACEHOLDER_DESC)
                .withSupportedClass(IntValue.class)
                .withWeightedAggregate(dcs.getName(), MultiplyNumeric::new, SumNumeric::new)
                .build(gs, ocs);

        // outer DataValueAggregate operator cannot aggregate data (it's just a "definition" of an aggregate operator)
        assertThrows(UnsupportedOperationException.class, () -> outer.computeInternal(null));
        assertThrows(UnsupportedOperationException.class, () -> outer.getResultInternal());
        assertThrows(UnsupportedOperationException.class, () -> outer.resetInternal());

        // inner DataValueAggregateOperator supports aggregation but not being used as the "definition"
        final var inner = outer.createInstance(gs, ocs);
        assertThrows(UnsupportedOperationException.class, () -> inner.getDescription());
        assertThrows(UnsupportedOperationException.class, () -> inner.createInstance(gs, ocs));
        // calling old method but having the operator configured as weighted should not be possible
        final var testCell = new IntCell(42);
        assertThrows(IllegalStateException.class, () -> inner.compute(testCell));

    }

    @SuppressWarnings("static-method")
    @Test
    void testOverflowBehavior() {
        final var dcs = new DataColumnSpecCreator("Test", IntCell.TYPE).createSpec();
        final var dts = new DataTableSpec(dcs);
        final var gs = createGlobalSettings()
                .setDataTableSpec(dts)
                .setGroupColNames(Collections.emptyList())
                .build();
        final var ocs = new OperatorColumnSettings(false, dcs);

        final var outer = DataValueAggregate.create()
                .withOperatorInfo(PLACEHOLDER_ID, PLACEHOLDER_LABEL, PLACEHOLDER_DESC)
                .withSupportedClass(IntValue.class)
                .withWeightedAggregate(dcs.getName(), MultiplyNumeric::new, SumNumeric::new)
                .build(gs, ocs);

        // let (weighted) combiner overflow

        var inner = outer.createInstance(gs, ocs);
        inner.compute(new DefaultRow(RowKey.createRowKey(1L), new IntCell(Integer.MAX_VALUE)), 0);
        assertEquals("Numeric overflow multiplying values for column of type "
            + "\"Number (integer)\" and column of type \"Number (integer)\"."
            + " Consider converting the input column(s) to \"Number (long)\".",
            inner.getSkipMessage(), "Skip message should inform user about numeric overflow");
        assertEquals(DataType.getMissingCell(), inner.getResult(), "Expected missing cell after numeric overflow");


        // checking for overflow _on result_, but not accumulator overflow

        inner = outer.createInstance(gs, ocs);
        final var val = (int) Math.sqrt(Integer.MAX_VALUE);
        inner.compute(new DefaultRow(RowKey.createRowKey(1L), new IntCell(val)), 0);
        final var remaining = Integer.MAX_VALUE - val * val;
        final var rowVal = (int) Math.sqrt(remaining);
        inner.compute(new DefaultRow(RowKey.createRowKey(2L), new IntCell(rowVal + 1)), 0);
        assertEquals(DataType.getMissingCell(), inner.getResult(), "Expected missing cell after numeric overflow");
        assertEquals("Numeric overflow of aggregation result for input type \"Number (integer)\"."
            + " Consider converting the input column to \"Number (long)\".",
                inner.getSkipMessage(), "Skip message should inform user about numeric overflow");


        // checking for overflow of SUM accumulator
        final var longDcs = new DataColumnSpecCreator("Test", LongCell.TYPE).createSpec();
        final var longGs = createGlobalSettings()
                .setDataTableSpec(new DataTableSpec(longDcs))
                .setGroupColNames(Collections.emptyList())
                .build();
        final var longOcs = new OperatorColumnSettings(false, longDcs);
        inner = outer.createInstance(longGs, longOcs);

        final var longVal = (long) Math.sqrt(Long.MAX_VALUE);
        // fill accumulator with values that still fit into long after multiplication
        inner.compute(new DefaultRow(RowKey.createRowKey(1L), new LongCell(longVal)), 0);
        // overflow sum accumulator itself (not combiner and not result)
        final var longRemaining = Long.MAX_VALUE - longVal * longVal;
        final var longRowVal = (long)Math.sqrt(longRemaining);
        inner.compute(new DefaultRow(RowKey.createRowKey(2L), new LongCell(longRowVal + 1)), 0);
        assertEquals(DataType.getMissingCell(), inner.getResult(), "Expected missing cell after numeric overflow");
        assertEquals("Numeric overflow aggregating values for column of type \"Number (long)\".",
                inner.getSkipMessage(), "Skip message should inform user about aggregation overflow");
    }

    @SuppressWarnings({"static-method", "deprecation"})
    @Test
    void testComputation() {
        final var dcs = new DataColumnSpecCreator("i", IntCell.TYPE).createSpec();
        final var dts = new DataTableSpec(dcs);
        final var gs = createGlobalSettings()
                .setDataTableSpec(dts)
                .setGroupColNames(Collections.emptyList())
                .build();
        final var ocs = new OperatorColumnSettings(false, dcs);

        // sum of squared values
        final var outer = DataValueAggregate.create()
                .withOperatorInfo(PLACEHOLDER_ID, PLACEHOLDER_LABEL, PLACEHOLDER_DESC)
                .withSupportedClass(IntValue.class)
                .withWeightedAggregate(dcs.getName(), MultiplyNumeric::new, SumNumeric::new)
                .build(gs, ocs);

        assertEquals(PLACEHOLDER_ID, outer.getId(), "Incorrect operator ID");
        assertEquals(PLACEHOLDER_LABEL, outer.getLabel(), "Incorrect operator label");
        assertEquals(PLACEHOLDER_DESC, outer.getDescription(), "Incorrect operator description");

        assertEquals(List.of(dcs.getName()), outer.getAdditionalColumnNames(), "Incorrect weight column name");
        // multiplication: Int x Int -> Int
        assertEquals(IntCell.TYPE, outer.getDataType(dcs.getType()), "Incorrect result data type");

        final var inner = assertDoesNotThrow(() -> outer.createInstance(gs, ocs), "Outer aggregate failed to produce "
                + "inner operator");
        assertEquals(PLACEHOLDER_ID, inner.getId(), "Incorrect operator ID");
        assertEquals(PLACEHOLDER_LABEL, inner.getLabel(), "Incorrect operator label");
        assertEquals(List.of(dcs.getName()), inner.getAdditionalColumnNames(), "Incorrect weight column name");

        // nothing computed yet, should result in missing cell
        assertEquals(DataType.getMissingCell(), inner.getResult());

        // compute SUM(i * i) aggregate over one row, skipping missing cells
        inner.compute(new DefaultRow(RowKey.createRowKey(1L), new IntCell(42)), 0);
        assertEquals(new IntCell(42 * 42), inner.getResult(), "Incorrect result for Sum of squared values");
        inner.reset();
        assertEquals(DataType.getMissingCell(), inner.getResult(), "Expected missing cell after #reset");


        final var unweighted = DataValueAggregate.create().withOperatorInfo("Sum1.0", "Sum", "Desc...")
                .withSupportedClass(IntValue.class)
                .withAggregate(SumNumeric::new)
                .build(gs, ocs).createInstance(gs, ocs);
        unweighted.compute(new DefaultRow(RowKey.createRowKey(1L), new IntCell(42)), 0);
        unweighted.compute(new DefaultRow(RowKey.createRowKey(2L), DataType.getMissingCell()), 0);
        // should still work with "old" API as per javadoc
        unweighted.compute(new IntCell(42));
        assertEquals(new IntCell(84), unweighted.getResult(), "Incorrect result for Sum of values");
    }

    @SuppressWarnings({"static-method", "deprecation"})
    @Test
    void testLegacyCompute() {
        final var miss = DataType.getMissingCell();
        final var datum = new IntCell(42);
        final var dcs = new DataColumnSpecCreator("i", IntCell.TYPE).createSpec();
        final var weightCol = new DataColumnSpecCreator("weight", LongCell.TYPE).createSpec();
        final var dts = new DataTableSpec(dcs, weightCol);
        final var gs = createGlobalSettings()
                .setDataTableSpec(dts)
                .setGroupColNames(Collections.emptyList())
                .build();
        final var ocs = new OperatorColumnSettings(true, dcs);

        final var weightedAgg = DataValueAggregate.create()
                .withOperatorInfo(PLACEHOLDER_ID, PLACEHOLDER_LABEL, PLACEHOLDER_DESC)
                .withSupportedClass(IntValue.class)
                .withWeightedAggregate(weightCol.getName(), MultiplyNumeric::new, SumNumeric::new)
                .build(gs, ocs).createInstance(gs, ocs);
        assertThrows(IllegalStateException.class, () -> weightedAgg.compute(datum),
            "Should complain if configuring weighted aggregate but calling old function that does not consider "
            + "weight value.");

        final var unweighted = DataValueAggregate.create()
                .withOperatorInfo(PLACEHOLDER_ID, PLACEHOLDER_LABEL, PLACEHOLDER_DESC)
                .withSupportedClass(IntValue.class)
                .withAggregate(SumNumeric::new)
                .build(gs, ocs).createInstance(gs, ocs);
        assertDoesNotThrow(() -> unweighted.compute(miss),
            "Should not throw when not configuring weighted aggregate");
        assertDoesNotThrow(() -> unweighted.compute(datum),
            "Should not throw when not configuring weighted aggregate");
    }

    // ==== Tests for Missing Cell Handling
    //
    // If the aggregate operator is configured to ignore missing cells (as is currently always the case for the Row
    //   Aggregator node):

    // Expected behavior: if the input to a (column) aggregate function (SUM, AVG,... not COUNT, which just counts
    //   rows) is (effectively) empty, the aggregate should return a missing cell

    // Corner cases:
    // - never get a missing cell if there exists at least one row for which all input columns (aggregated and
    //   weight) are non-missing
    // - see a missing cell only if for all input rows at least one of the aggregated or weight column contains
    //   a missing cell, i.e. the input to the aggregate function, which is applied to the result of the weighting
    //   is a column containing only missing cells, which are all ignored, hence the input is empty and result
    //   in a missing cell

    /**
     * Tests behavior when some or all cells of the aggregated column are missing.
     */
    @SuppressWarnings("static-method")
    @Test
    void testMissingUnweighted() {
        final var miss = DataType.getMissingCell();
        final var aggregatedColumnSomeMissing = new DataColumnSpecCreator("someMissing", IntCell.TYPE).createSpec();
        final var aggregatedColumnAllMissing = new DataColumnSpecCreator("allMissing", IntCell.TYPE).createSpec();
        final var dts = new DataTableSpec(
            aggregatedColumnSomeMissing,
            aggregatedColumnAllMissing
        );
        final var gs = createGlobalSettings().setDataTableSpec(dts).setGroupColNames(Collections.emptyList()).build();

        final var input = List.of(
            //                                       aggSomeMiss | aggAllMiss
            new DefaultRow(RowKey.createRowKey(0L), new IntCell(4), miss),
            new DefaultRow(RowKey.createRowKey(1L), new IntCell(2), miss),
            new DefaultRow(RowKey.createRowKey(2L), miss          , miss),
            new DefaultRow(RowKey.createRowKey(3L), miss          , miss)
        );

        // Case 1: some missing -> non-missing cell result
        final var expected = new IntCell(6);
        final var agg = createOperator(gs,
            aggregatedColumnSomeMissing, // agg col
            null, // weight col
            IntValue.class,
            dts, input);
        assertEquals(expected, agg.getResult(), "Wrong result when column contains some missing cells");

        // Case 2: all missing -> missing cell result
        final var aggAllMiss = createOperator(gs,
            aggregatedColumnAllMissing, // agg col
            null, // weight col
            IntValue.class,
            dts, input);
        assertEquals(miss, aggAllMiss.getResult(), "Wrong result when agg column contains only missing cells");
    }

    /**
     * Tests behavior when some or all cells of the aggregated and/or weight column are missing.
     */
    @SuppressWarnings("static-method")
    @Test
    void testMissingWeighted() {
        final var miss = DataType.getMissingCell();


        final var aggregatedColumnSomeMissing = new DataColumnSpecCreator("someMissing", IntCell.TYPE).createSpec();
        final var aggregatedColumnAllMissing = new DataColumnSpecCreator("allMissing", IntCell.TYPE).createSpec();
        final var weightColumnSomeMissing = new DataColumnSpecCreator("weightSome", IntCell.TYPE).createSpec();
        final var weightColumnAllMissing = new DataColumnSpecCreator("weightAll", IntCell.TYPE).createSpec();
        final var dts = new DataTableSpec(
            aggregatedColumnSomeMissing,
            aggregatedColumnAllMissing,
            weightColumnSomeMissing,
            weightColumnAllMissing
        );
        final var gs = createGlobalSettings().setDataTableSpec(dts).setGroupColNames(Collections.emptyList()).build();

        final var input = List.of(
            //                                   aggSomeMiss | aggAllMiss | weightSomeMiss | weightAllMiss
            new DefaultRow(RowKey.createRowKey(0L), new IntCell(4), miss, new IntCell(2), miss), // both 1&3 present
            new DefaultRow(RowKey.createRowKey(1L), new IntCell(2), miss, miss          , miss), // only 1 present
            new DefaultRow(RowKey.createRowKey(2L), miss          , miss, new IntCell(2), miss), // only 3 present
            new DefaultRow(RowKey.createRowKey(3L), miss          , miss, miss          , miss)  // neither 1&3 present
        );

        // Case 1: both columns contain _some_ missing cells -> non-empty, non-missing result
        final var expected = new IntCell(8);
        final var agg = createOperator(gs,
            aggregatedColumnSomeMissing, // agg col
            weightColumnSomeMissing, // weight col
            IntValue.class,
            dts, input);
        assertEquals(expected, agg.getResult(), "Wrong result when both columns contain some missing cells");

        // Case 2: agg col contains only missing cells but weight column contains _some_ missing cells -> missing cell
        final var aggAllMissAgg = createOperator(gs,
            aggregatedColumnAllMissing, // agg col
            weightColumnSomeMissing, // weight col
            IntValue.class,
            dts, input);
        assertEquals(miss, aggAllMissAgg.getResult(), "Wrong result when agg column contains only missing cells");

        // Case 3: agg col contains _some_ missing cells but weight column contains only missing cells -> missing cell
        final var aggAllMissW = createOperator(gs,
            aggregatedColumnSomeMissing, // agg col
            weightColumnAllMissing, // weight col
            IntValue.class,
            dts, input);
        assertEquals(miss, aggAllMissW.getResult(), "Wrong result when weight column contains only missing cells");

        // Case 4: both cols contain _only_ missing cells -> missing cell
        final var aggAllMiss = createOperator(gs,
            aggregatedColumnAllMissing, // agg col
            weightColumnAllMissing, // weight col
            IntValue.class,
            dts, input);
        assertEquals(miss, aggAllMiss.getResult(), "Wrong result when both columns contain only missing cells");
    }

    // Test Helpers

    private static AggregationOperator createOperator(final GlobalSettings gs,
            final DataColumnSpec aggCol, final DataColumnSpec weightCol, final Class<IntValue> supportedClass,
            final DataTableSpec dts, final List<DefaultRow> input) {
        final var ocs = new OperatorColumnSettings(false, aggCol);
        final var weightColumnName = weightCol != null ? weightCol.getName() : null;
        var aggOpBuilder = DataValueAggregate.create()
                .withOperatorInfo(PLACEHOLDER_ID, PLACEHOLDER_LABEL, PLACEHOLDER_DESC)
                .withSupportedClass(supportedClass)
                .withWeightedAggregate(weightColumnName, weightColumnName != null ? MultiplyNumeric::new : null,
                    SumNumeric::new);
        final var aggOp = aggOpBuilder.build(gs, ocs).createInstance(gs, ocs);
        final var i = dts.findColumnIndex(aggCol.getName());
        for (final var r : input) {
            aggOp.compute(r, i);
        }
        return aggOp;
    }

    private static GlobalSettingsBuilder createGlobalSettings() {
        return GlobalSettings.builder()
        .setAggregationContext(AggregationContext.ROW_AGGREGATION)
        .setValueDelimiter(GlobalSettings.STANDARD_DELIMITER);
    }
}
