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
 *   25 Jul 2023 (Manuel Hotz, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.base.node.preproc.groupby;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.Arrays;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.knime.base.data.aggregation.ColumnAggregator;
import org.knime.base.data.aggregation.GlobalSettings;
import org.knime.base.data.aggregation.GlobalSettings.AggregationContext;
import org.knime.core.data.BooleanValue;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.LongValue;
import org.knime.core.data.RowKey;
import org.knime.core.data.StringValue;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.testing.core.ExecutionContextExtension;


/**
 * Tests for the {@link BigGroupByTable} (currently only making sure "unusual" data cells can be aggregated).
 *
 * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
 */
@ExtendWith({ExecutionContextExtension.class})
class BigGroupByTableTest {

    /**
     * Dummy data cell exhibiting the "unusual cell" behavior in BigGroupByTable: the comparator may return 0
     * for two cell instances, but they may in fact not be equal.
     *
     * The comparator is taken from the preferred interface, in this case StringValue, which operates only on one
     * field.
     * However, equalsDataCell/hashCode operate on both fields.
     *
     * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
     */
    private static final class DummyDataCell extends DataCell implements StringValue, BooleanValue {

        private static final long serialVersionUID = 1L;

        public static final DataType TYPE = DataType.getType(DummyDataCell.class);

        private String m_value;
        private boolean m_isFoo;

        DummyDataCell(final String value, final boolean isFoo) {
            m_value = value;
            m_isFoo = isFoo;
        }

        @Override
        public String toString() {
            return m_value != null ? "DummyDataCell[\"%s, %b\"]".formatted(m_value, m_isFoo)
                : "DummyDataCell[,%b]".formatted(m_isFoo);
        }

        @Override
        protected boolean equalsDataCell(final DataCell dc) {
            return dc instanceof DummyDataCell ddc
                    && StringUtils.equals(m_value, ddc.m_value) && (ddc.m_isFoo == m_isFoo);
        }

        @Override
        public int hashCode() {
            return new HashCodeBuilder(13, 37).append(m_value).append(m_isFoo).toHashCode();
        }

        @Override
        public String getStringValue() {
            return m_value;
        }

        @Override
        public boolean getBooleanValue() {
            return m_isFoo;
        }

    }

    @Test
    void testMultipleGroupsInSameChunk(final ExecutionContext ctx) throws CanceledExecutionException {
        // using the DummyDataCell from above provokes the BigGroupByTable special case of multiple
        // non-equal groups being in the same chunk, where all comparators returned 0
        final var dts = new DataTableSpec(new String[] { "dummy" }, new DataType[] { DummyDataCell.TYPE});
        final var container = ctx.createDataContainer(dts);
        container.addRowToTable(new DefaultRow(RowKey.createRowKey(10L), new DummyDataCell("one", true)));
        container.addRowToTable(new DefaultRow(RowKey.createRowKey(11L), new DummyDataCell("one", false)));
        container.addRowToTable(new DefaultRow(RowKey.createRowKey(12L), new DummyDataCell("two", true)));
        container.close();
        final var tbl = container.getTable();
        // sorter does not like immutable collections (e.g. List#of)...
        final var groupBy = Arrays.asList("dummy");
        final var agg = new ColumnAggregator[0];
        final var gs = GlobalSettings.builder().setAggregationContext(AggregationContext.ROW_AGGREGATION)
                .setDataTableSpec(dts)
                .setGroupColNames(groupBy)
                .setValueDelimiter(GlobalSettings.STANDARD_DELIMITER)
                .build();
        final var gbt = new BigGroupByTable(ctx, tbl, groupBy, agg, "COUNT", gs, false,
            ColumnNamePolicy.KEEP_ORIGINAL_NAME, false);
        final var aggregated = gbt.getBufferedTable();

        assertEquals(3, aggregated.size(), "Output should contain three rows");
        try (final var c = aggregated.iterator()) {
            for (int i = 0; i < 3; i++) {
                final var row = c.next();
                assertEquals(RowKey.createRowKey((long)i), row.getKey(), "Row keys should not be reused.");
                final var count = ((LongValue)row.getCell(1)).getLongValue();
                assertEquals(1L, count, "Aggregate should contain one source row");
            }
            assertFalse(c.hasNext(), "Table should not contain more rows.");
        }
    }
}
