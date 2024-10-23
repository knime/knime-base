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
 *   7 Jan 2025 (Manuel Hotz, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.base.data.filter.row.v2;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.knime.base.data.filter.row.v2.OffsetFilter.Operator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.IntValue;
import org.knime.core.data.LongValue;
import org.knime.core.data.RowKey;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.LongCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.v2.RowRead;
import org.knime.core.data.v2.RowWriteCursor;
import org.knime.core.data.v2.value.ValueInterfaces.IntWriteValue;
import org.knime.core.data.v2.value.ValueInterfaces.LongWriteValue;
import org.knime.core.data.v2.value.ValueInterfaces.StringWriteValue;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.streamable.DataTableRowInput;
import org.knime.core.node.streamable.RowOutput;
import org.knime.testing.core.ExecutionContextExtension;

/**
 * Tests for the {@link RowFilter}. Most of the testing is still done in testflows.
 *
 * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
 */
@ExtendWith(ExecutionContextExtension.class)
class RowFilterTest {

    private static final DataTableSpec SPEC = new DataTableSpec( //
        new String[]{"Int1", "Long1", "String1"}, //
        new DataType[]{IntCell.TYPE, LongCell.TYPE, StringCell.TYPE} //
    );

    private static BufferedDataTable inputTable;

    @BeforeEach
    void prepareInputData(final ExecutionContext exec) throws IOException {
        try (final var out = exec.createRowContainer(SPEC); final var cursor = out.createCursor()) {
            final var rowBuf = out.createRowBuffer();
            for (int i = 0; i < 10; i++) {

                rowBuf.setRowKey(RowKey.createRowKey((long)i));
                // map even values to itself and odd values to their negative
                rowBuf.<IntWriteValue> getWriteValue(0).setValue(new IntCell(i % 2 == 0 ? i : -i));
                rowBuf.<LongWriteValue> getWriteValue(1).setValue(new LongCell(i));
                rowBuf.<StringWriteValue> getWriteValue(2).setValue(new StringCell("I am row " + i));

                cursor.commit(rowBuf);
            }
            inputTable = out.finish();
        }
    }

    private static final IndexedRowReadPredicate evenValues = new IndexedRowReadPredicate() {
        @Override
        public boolean test(final long index, final RowRead read) {
            return read.<LongCell> getValue(1).getLongValue() % 2 == 0;
        }
    };

    private static final IndexedRowReadPredicate oddValues = new IndexedRowReadPredicate() {
        @Override
        public boolean test(final long index, final RowRead read) {
            return read.<LongCell> getValue(1).getLongValue() % 2 != 0;
        }
    };

    @Test
    @SuppressWarnings("resource") // our output is not a resource
    void testFilterOnPredicate(final ExecutionContext exec)
        throws CanceledExecutionException, InvalidSettingsException {

        try (final var in = inputTable.cursor()) {
            RowFilter.filterOnPredicate(exec, in, inputTable.size(),
                // EVEN
                new TestingRowWriteCursor(row -> {
                    // even Long1 values have their Int1 value equal
                    final var intVal = row.<IntValue> getValue(0);
                    final var longVal = row.<LongValue> getValue(1);
                    assertThat(longVal.getLongValue()) //
                        .as("Even Long1 values have their Int1 value equal").isEqualTo(intVal.getIntValue());
                }),
                // ODD
                new TestingRowWriteCursor(row -> {
                    // odd Long1 values have their Int1 value negated
                    final var intVal = row.<IntValue> getValue(0);
                    final var longVal = row.<LongValue> getValue(1);
                    assertThat(longVal.getLongValue()) //
                        .as("Odd Long1 values have their Int1 value negated").isEqualTo(-intVal.getIntValue());
                }), //
                evenValues, true);
        }

        // test union of predicates returns the whole table
        try (final var in = inputTable.cursor()) {
            final var counter = new AtomicLong();
            RowFilter.filterOnPredicate(exec, in, inputTable.size(),
                // EVEN
                new TestingRowWriteCursor(row -> {
                    counter.incrementAndGet();
                }),
                // rejected rows should not occur
                new TestingRowWriteCursor(row -> {
                    fail("Unexpected rejected row: " + row.getRowKey());
                }), //
                evenValues.or(oddValues), true);
            assertThat(counter.get()).as("All rows should be included").isEqualTo(inputTable.size());
        }
    }

    @Test
    void testFilterRange(final ExecutionContext exec)
        throws CanceledExecutionException, InvalidSettingsException, InterruptedException {
        final var in = new DataTableRowInput(inputTable);
        try {
            RowFilter.filterRange(exec, in,
                // incl         excl  incl
                // [0,1,2,3,4], [5],  [6,7,8,9]
                new TestingRowOutput(row -> {
                    assertThat(row.getKey().getString()) //
                        .as("Everything except Row5 is included").isNotEqualTo("Row5");
                }), new TestingRowOutput(row -> {
                    assertThat(row.getKey().getString()) //
                        .as("Only Row5 is excluded").isEqualTo("Row5");
                 // use unknown table size to trigger early-close behavior as it is used in Streaming
                }), FilterPartition.computePartition(new OffsetFilter(Operator.NEQ, 5), -1));
        } finally {
            in.close();
        }
    }

    @Test
    void testFilterOnPredicateOutput(final ExecutionContext exec)
        throws CanceledExecutionException, InvalidSettingsException, InterruptedException {

        final var in = new DataTableRowInput(inputTable);
        try {
            RowFilter.filterOnPredicate(exec, in,
                // EVEN
                new TestingRowOutput(row -> {
                    // even Long1 values have their Int1 value equal
                    final var intVal = (IntValue)row.getCell(0);
                    final var longVal = (LongValue)row.getCell(1);
                    assertThat(longVal.getLongValue()) //
                        .as("Even Long1 values have their Int1 value equal").isEqualTo(intVal.getIntValue());
                }),
                // ODD
                new TestingRowOutput(row -> {
                    // odd Long1 values have their Int1 value negated
                    final var intVal = (IntValue)row.getCell(0);
                    final var longVal = (LongValue)row.getCell(1);
                    assertThat(longVal.getLongValue()) //
                        .as("Odd Long1 values have their Int1 value negated").isEqualTo(-intVal.getIntValue());
                }), evenValues, true);
        } finally {
            in.close();
        }
    }

    private static final class TestingRowOutput extends RowOutput {

        private final Consumer<DataRow> m_rowConsumer;

        TestingRowOutput(final Consumer<DataRow> rowConsumer) {
            this.m_rowConsumer = rowConsumer;
        }

        @Override
        public final void push(final DataRow row) throws InterruptedException {
            m_rowConsumer.accept(row);
        }

        @Override
        public final void close() throws InterruptedException {
            // no-op
        }

    }

    private static final class TestingRowWriteCursor implements RowWriteCursor {

        private final Consumer<RowRead> m_rowConsumer;

        TestingRowWriteCursor(final Consumer<RowRead> rowConsumer) {
            this.m_rowConsumer = rowConsumer;
        }

        @Override
        public void commit(final RowRead row) {
            m_rowConsumer.accept(row);
        }

        @Override
        public void close() {
            // no-op
        }

    }

}
