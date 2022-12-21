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
import org.knime.base.data.aggregation.GlobalSettings;
import org.knime.base.data.aggregation.GlobalSettings.AggregationContext;
import org.knime.base.data.aggregation.GlobalSettings.GlobalSettingsBuilder;
import org.knime.base.data.aggregation.OperatorColumnSettings;
import org.knime.base.node.preproc.rowagg.aggregation.DataValueAggregate.DataValueAggregateOperator;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.IntValue;
import org.knime.core.data.RowKey;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.LongCell;

/**
 * Tests for the {@link DataValueAggregate} and {@link DataValueAggregateOperator}.
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
                .withAggregate(Sum::new)
                .withWeighting(dcs.getName(), Multiply::new)
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
                .withAggregate(Sum::new)
                .withWeighting(dcs.getName(), Multiply::new)
                .build(gs, ocs);

        // let (weighted) combiner overflow

        var inner = outer.createInstance(gs, ocs);
        inner.compute(new DefaultRow(RowKey.createRowKey(1L), new IntCell(Integer.MAX_VALUE)), 0);
        assertEquals("Numeric overflow computing weighted aggregate value for weight column \"Test\" of type "
            + "\"Number (integer)\" and input column \"Test\" of type \"Number (integer)\".",
            inner.getSkipMessage());
        assertEquals(DataType.getMissingCell(), inner.getResult(), "Expected missing cell after numeric overflow");


        // checking for overflow _on result_, but not accumulator overflow

        inner = outer.createInstance(gs, ocs);
        final var val = (int) Math.sqrt(Integer.MAX_VALUE);
        inner.compute(new DefaultRow(RowKey.createRowKey(1L), new IntCell(val)), 0);
        final var remaining = Integer.MAX_VALUE - val * val;
        final var rowVal = (int) Math.sqrt(remaining);
        inner.compute(new DefaultRow(RowKey.createRowKey(2L), new IntCell(rowVal + 1)), 0);
        assertEquals(DataType.getMissingCell(), inner.getResult(), "Expected missing cell after numeric overflow");
        assertEquals("Numeric overflow of aggregation result for input column \"Test\" of type \"Number (integer)\". "
            + "Consider converting the input column to \"Number (long)\".",
                inner.getSkipMessage());


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
        assertEquals("Numeric overflow of aggregation result for input column \"Test\" of type \"Number (long)\".",
                inner.getSkipMessage());
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
                .withAggregate(Sum::new)
                .withWeighting(dcs.getName(), Multiply::new)
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
                .withAggregate(Sum::new)
                .build(gs, ocs).createInstance(gs, ocs);
        unweighted.compute(new DefaultRow(RowKey.createRowKey(1L), new IntCell(42)), 0);
        unweighted.compute(new DefaultRow(RowKey.createRowKey(2L), DataType.getMissingCell()), 0);
        // should still work with "old" API as per javadoc
        unweighted.compute(new IntCell(42));
        assertEquals(new IntCell(84), unweighted.getResult(), "Incorrect result for Sum of values");
    }

    @SuppressWarnings({"static-method", "deprecation"})
    @Test
    void testMissing() {
        final var miss = DataType.getMissingCell();
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
                .withAggregate(Sum::new)
                .withWeighting(weightCol.getName(), Multiply::new)
                .build(gs, ocs).createInstance(gs, ocs);
        assertThrows(IllegalStateException.class, () -> weightedAgg.compute(miss));
        weightedAgg.compute(new DefaultRow(RowKey.createRowKey(0L), miss, miss), 0);
        weightedAgg.compute(new DefaultRow(RowKey.createRowKey(1L), new IntCell(2), miss), 0);
        weightedAgg.compute(new DefaultRow(RowKey.createRowKey(2L), miss, new IntCell(2)), 0);
        weightedAgg.compute(new DefaultRow(RowKey.createRowKey(3L), new IntCell(2), new LongCell(2)), 0);
        assertEquals(new LongCell(4), weightedAgg.getResult(), "Wrong result while handling missing cells");


        final var unweighted = DataValueAggregate.create()
                .withOperatorInfo(PLACEHOLDER_ID, PLACEHOLDER_LABEL, PLACEHOLDER_DESC)
                .withSupportedClass(IntValue.class)
                .withAggregate(Sum::new)
                .build(gs, ocs).createInstance(gs, ocs);
        assertDoesNotThrow(() -> unweighted.compute(miss));
        assertDoesNotThrow(() -> unweighted.compute(new IntCell(2)));
        unweighted.compute(new DefaultRow(RowKey.createRowKey(0L), miss, miss), 0);
        unweighted.compute(new DefaultRow(RowKey.createRowKey(1L), new IntCell(2), miss), 0);
        unweighted.compute(new DefaultRow(RowKey.createRowKey(2L), miss, new IntCell(2)), 0);
        unweighted.compute(new DefaultRow(RowKey.createRowKey(3L), new IntCell(2), new LongCell(100)), 0);
        assertEquals(new IntCell(6), unweighted.getResult(), "Wrong result while handling missing cells");
    }



    private static GlobalSettingsBuilder createGlobalSettings() {
        return GlobalSettings.builder()
        .setAggregationContext(AggregationContext.ROW_AGGREGATION)
        .setValueDelimiter(GlobalSettings.STANDARD_DELIMITER);
    }
}
