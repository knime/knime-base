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
 *   22 Jan 2025 (Manuel Hotz, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.base.node.preproc.filter.row3.predicates;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.util.OptionalInt;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.knime.base.data.filter.row.v2.RowFilter;
import org.knime.core.data.BooleanValue;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.LongValue;
import org.knime.core.data.RowKey;
import org.knime.core.data.def.BooleanCell;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.LongCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.v2.RowRead;
import org.knime.core.data.v2.RowWriteCursor;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.webui.node.dialog.defaultdialog.widget.dynamic.DynamicValuesInput;
import org.knime.testing.core.ExecutionContextExtension;

/**
 * Tests for the boolean predicate factory.
 *
 * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
 */
@ExtendWith(ExecutionContextExtension.class)
final class BooleanPredicateFactoryTest {

    @SuppressWarnings("static-method")
    @ParameterizedTest
    @ValueSource(strings = {"true", "false"})
    void testBoolean(final boolean matchTrue) {
        final var factory = BooleanPredicateFactory.create(BooleanCell.TYPE, matchTrue).orElseThrow();
        final var idx = OptionalInt.of(EqualityPredicateFactoryTest.SPEC.findColumnIndex("Bool1"));
        assertThatCode(() -> factory.createPredicate(idx, null)) //
            .as("Booleans can be matched TRUE or FALSE") //
            .doesNotThrowAnyException();
    }

    @SuppressWarnings("static-method")
    @ParameterizedTest
    @ValueSource(strings = {"true", "false"})
    void testBooleanException(final boolean matchTrue) {
        final var factory = BooleanPredicateFactory.create(BooleanCell.TYPE, matchTrue).orElseThrow();
        final var idx = OptionalInt.empty();
        assertThatCode(() -> factory.createPredicate(idx, null)) //
            .as("Boolean predicate needs a column index") //
            .isInstanceOf(IllegalStateException.class) //
            .hasMessageContaining("Boolean predicate operates on column but did not get a column index");

        assertThat(BooleanPredicateFactory.create(StringCell.TYPE, matchTrue))
            .as("Boolean predicate operates only on BooleanCells") //
            .isEmpty();
    }

    @Test
    void testBooleanValueFiltering(final ExecutionContext exec) throws InvalidSettingsException, CanceledExecutionException {
        final var dts = new DataTableSpec( //
            new DataColumnSpecCreator("isEven", BooleanDummy.TYPE).createSpec(), //
            new DataColumnSpecCreator("longColumn", LongCell.TYPE).createSpec() //
        );
        final BufferedDataContainer container = exec.createDataContainer(dts);
        for (long i = 0; i < 50; i++) {
            container.addRowToTable( //
                new DefaultRow(RowKey.createRowKey(i), new BooleanDummy(i % 2 == 0), new LongCell(i)));
        }
        container.close();
        final var table = container.getTable();
        final var factory = BooleanPredicateFactory.create(BooleanDummy.TYPE, true).orElseThrow();

        // include only even numbers
        final var predicate = factory.createPredicate(OptionalInt.of(0),
            // actual reference value is ignored for Booleans, because we only have IS_TRUE and IS_FALSE operators
            // that don't compare with a reference value
            DynamicValuesInput.singleValueWithInitialValue(BooleanDummy.TYPE, new BooleanDummy(true)));
        try (final var cursor = table.cursor(); final var included = new RowWriteCursor() {

            @Override
            public void commit(final RowRead row) {
                assertThat(row.<LongValue> getValue(1).getLongValue() % 2).as("Only even numbers are included")
                    .isZero();
                assertThat(row.<BooleanValue> getValue(0).getBooleanValue()).as("Only TRUE values are included")
                    .isTrue();
            }

            @Override
            public void close() {
                // noop
            }
        }; final var excluded = new RowWriteCursor() {

            @Override
            public void commit(final RowRead row) {
                assertThat(row.<LongValue> getValue(1).getLongValue() % 2).as("Only odd numbers are excluded")
                    .isEqualTo(1);
                assertThat(row.<BooleanValue> getValue(0).getBooleanValue()).as("Only FALSE values are excluded")
                    .isFalse();
            }

            @Override
            public void close() {
                // noop
            }
        }) {
            RowFilter.filterOnPredicate(exec, cursor, table.size(), included, excluded, predicate, true);
        }
    }

    /**
     * A boolean dummy that should be usable in place of a real BooleanCell. For example, the Columnar Backend
     * does not supply XCell implementations, so casts to XCells fail.
     */
    @SuppressWarnings("serial")
    final class BooleanDummy extends DataCell implements BooleanValue {

        public static final DataType TYPE = DataType.getType(BooleanDummy.class);

        final boolean m_value;

        BooleanDummy(final boolean value) {
            m_value = value;
        }

        @Override
        public boolean getBooleanValue() {
            return m_value;
        }

        @Override
        public String toString() {
            throw new UnsupportedOperationException("Not expected to be called during test.");
        }

        @Override
        protected boolean equalsDataCell(final DataCell dc) {
            throw new UnsupportedOperationException("Not expected to be called during test.");
        }

        @Override
        public int hashCode() {
            throw new UnsupportedOperationException("Not expected to be called during test.");
        }

    }
}
