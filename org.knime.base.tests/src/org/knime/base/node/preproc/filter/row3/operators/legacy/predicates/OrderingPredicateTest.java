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
 *   15 Jul 2025 (Manuel Hotz, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.base.node.preproc.filter.row3.operators.legacy.predicates;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.OptionalInt;

import org.junit.jupiter.api.Test;
import org.knime.base.node.preproc.filter.row3.operators.legacy.DynamicValuesInput;
import org.knime.base.node.preproc.filter.row3.operators.legacy.LegacyFilterOperator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataValue;
import org.knime.core.data.RowKey;
import org.knime.core.data.RowKeyValue;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.LongCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.v2.DataRowRowRead;
import org.knime.core.node.InvalidSettingsException;

/**
 * Class testing evaluation of ordering-based predicates.
 *
 * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
 */
class OrderingPredicateTest {

    /**
     * Test for backwards compatibility of comparing long and double values.
     * @throws InvalidSettingsException not expected during test execution
     */
    @Test
    void testOrderingLongDoubleHandling() throws InvalidSettingsException {
        final var op = LegacyFilterOperator.GT;

        final var ref = 42.0d;
        final var factory = OrderingPredicateFactory.create(LongCell.TYPE, op).orElseThrow();
        // Compare Long column with double reference value -> must be allowed for backwards compatibility
        assertTrue(
            factory
                .createPredicate(OptionalInt.of(0),
                    DynamicValuesInput.singleValueWithInitialValue(DoubleCell.TYPE, new DoubleCell(ref)))
                .test(0, new TestRow(new DefaultRow(RowKey.createRowKey(0L), new LongCell(43)))),
            "Expected 43 to be greater than 42.0d");

        final var longRef = 42L;
        final var doubleFactory = OrderingPredicateFactory.create(DoubleCell.TYPE, op).orElseThrow();
        // Compare Double column with long reference value -> still not allowed (as in previous versions)
        assertThatCode(() -> doubleFactory //
            .createPredicate(OptionalInt.of(0), //
                DynamicValuesInput.singleValueWithInitialValue(LongCell.TYPE, new LongCell(longRef))) //
            .test(0, new TestRow(new DefaultRow(RowKey.createRowKey(0L), new DoubleCell(43.0d))))) //
                .as("Comparing incompatible types").isInstanceOf(InvalidSettingsException.class) //
                .hasMessageContaining(OrderingPredicateFactoryTest.TYPE_MISMATCH_EXCEPTION_MESSAGE //
                    .formatted("input column", DoubleCell.TYPE.getName(), LongCell.TYPE.getName(), op));
    }

    @Test
    void testLongColumn() throws InvalidSettingsException {
        final var op = LegacyFilterOperator.GT;

        final var ref = 42.0d;
        final var factory = OrderingPredicateFactory.create(LongCell.TYPE, op).orElseThrow();
        // Compare Long column with int reference value
        assertTrue(
            factory
                .createPredicate(OptionalInt.of(0),
                    DynamicValuesInput.singleValueWithInitialValue(IntCell.TYPE, new IntCell((int)ref)))
                .test(0, new TestRow(new DefaultRow(RowKey.createRowKey(0L), new LongCell(43)))),
            "Expected 43 to be greater than 42.0d");

        // Compare Long column with String reference value
        assertThatCode(() -> factory //
            .createPredicate(OptionalInt.of(0), //
                DynamicValuesInput.singleValueWithInitialValue(StringCell.TYPE, new StringCell("42"))) //
            .test(0, new TestRow(new DefaultRow(RowKey.createRowKey(0L), new LongCell(43))))) //
                .as("Comparing incompatible types").isInstanceOf(InvalidSettingsException.class) //
                .hasMessageContaining(OrderingPredicateFactoryTest.TYPE_MISMATCH_EXCEPTION_MESSAGE //
                    .formatted("input column", LongCell.TYPE.getName(), StringCell.TYPE.getName(), op));
    }

    private static final class TestRow implements DataRowRowRead {

        final DataRow m_row;

        TestRow(final DataRow row) {
            m_row = row;
        }

        @Override
        public RowKeyValue getRowKey() {
            return m_row.getKey();
        }

        @Override
        public int getNumColumns() {
            return m_row.getNumCells();
        }

        @SuppressWarnings("unchecked")
        @Override
        public <D extends DataValue> D getValue(final int index) {
            return (D)m_row.getCell(index);
        }

        @Override
        public boolean isMissing(final int index) {
            return m_row.getCell(index).isMissing();
        }

    }

}
