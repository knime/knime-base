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
 *   21 Jan 2025 (Manuel Hotz, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.base.node.preproc.filter.row3.operators.legacy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.util.EnumSet;
import java.util.OptionalInt;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.knime.base.node.preproc.filter.row3.FilterDummyDataCellExtension;
import org.knime.base.node.preproc.filter.row3.operators.legacy.DynamicValuesInput;
import org.knime.base.node.preproc.filter.row3.operators.legacy.LegacyFilterOperator;
import org.knime.base.node.preproc.filter.row3.operators.legacy.OrderingPredicateFactory;
import org.knime.core.data.DataCell;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.LongCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.v2.IndexedRowReadPredicate;
import org.knime.core.node.InvalidSettingsException;

/**
 * This unit test tests the construction and error handling of ordering predicates. Actually evaluating the predicates
 * is tested in Testflows via the Row Filter node.
 *
 * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
 */
final class OrderingPredicateFactoryTest {

    /**
     * Expected message template for type mismatches between input column and reference value.
     */
    static final String TYPE_MISMATCH_EXCEPTION_MESSAGE =
        "Cannot apply ordering comparison to %s of type \"%s\" and reference value of type \"%s\"";

    private static final EnumSet<LegacyFilterOperator> getSupported() {
        return EnumSet.of(LegacyFilterOperator.LT, LegacyFilterOperator.LTE, LegacyFilterOperator.GT, LegacyFilterOperator.GTE);
    }

    private static final Stream<Arguments> getUnsupportedOperators() {
        return EnumSet.complementOf(getSupported()).stream().map(Arguments::of);
    }

    private static final Stream<Arguments> getSupportedOperators() {
        return getSupported().stream().map(Arguments::of);
    }

    @SuppressWarnings("static-method")
    @ParameterizedTest
    @MethodSource("getSupportedOperators")
    void testSupportedOperators(final LegacyFilterOperator op) {
        assertThat(OrderingPredicateFactory.Ordering.fromOperator(op)) //
            .as("Filter operator %s is supported".formatted(op)) //
            .isPresent();
    }

    @SuppressWarnings("static-method")
    @ParameterizedTest
    @MethodSource("getUnsupportedOperators")
    void testUnsupportedOperators(final LegacyFilterOperator op) {
        assertThat(OrderingPredicateFactory.Ordering.fromOperator(op)) //
            .as("Filter operator %s is not supported".formatted(op)) //
            .isEmpty();
    }

    private static IndexedRowReadPredicate createPredicate(final String columnName, final DataCell referenceCell,
        final LegacyFilterOperator operator) throws InvalidSettingsException {
        return OrderingPredicateFactory
            .create(EqualityPredicateFactoryTest.SPEC.getColumnSpec(columnName).getType(), operator).orElseThrow()
            .createPredicate(OptionalInt.of(EqualityPredicateFactoryTest.SPEC.findColumnIndex(columnName)),
                DynamicValuesInput.singleValueWithInitialValue(referenceCell.getType(), referenceCell));
    }

    @SuppressWarnings("static-method")
    @ParameterizedTest
    @MethodSource("getSupportedOperators")
    void testInt(final LegacyFilterOperator operator) {

        // Int column with Int reference
        assertThatCode(() -> createPredicate("Int1", new IntCell(1), operator)) //
            .as("Comparing Int column with Int reference via " + operator) //
            .doesNotThrowAnyException();

        // Int column with Long reference
        assertThatCode(() -> createPredicate("Int1", new LongCell(1), operator)) //
            .as("Comparing Int column with Long reference via " + operator) //
            .doesNotThrowAnyException();

        // TODO(performance) test short-circuit behavior when implemented

        // Int column with Double reference
        assertThatCode(() -> createPredicate("Int1", new DoubleCell(1.0), operator)) //
            .as("Comparing Int column with Double reference via " + operator) //
            .doesNotThrowAnyException();

        // TODO(performance) test short-circuit behavior when implemented

        // Int column with String reference should throw exception
        assertThatCode(() -> createPredicate("Int1", new StringCell("foo"), operator)) //
            .as("Comparing Int column with String reference via " + operator) //
            .isInstanceOf(InvalidSettingsException.class) //
            .hasMessageContaining("Cannot apply ordering comparison to input column of type \"" + IntCell.TYPE.getName()
                + "\" and reference value of type \"" + StringCell.TYPE.getName() + "\"");
    }

    @SuppressWarnings("static-method")
    @ParameterizedTest
    @MethodSource("getSupportedOperators")
    void testLong(final LegacyFilterOperator operator) {

        // Long column with Int reference
        assertThatCode(() -> createPredicate("Long1", new IntCell(1), operator)) //
            .as("Comparing Long column with Int reference via " + operator) //
            .doesNotThrowAnyException();

        // Long column with Long reference
        assertThatCode(() -> createPredicate("Long1", new LongCell(1), operator)) //
            .as("Comparing Long column with Long reference via " + operator) //
            .doesNotThrowAnyException();

        // Long column with Double reference must be allowed (backwards-compatibility)
        assertThatCode(() -> createPredicate("Long1", new DoubleCell(1.0), operator)) //
            .as("Comparing Long column with Double reference via " + operator) //
            .doesNotThrowAnyException();

        // Long column with String reference should throw exception
        assertThatCode(() -> createPredicate("Long1", new StringCell("foo"), operator)) //
            .as("Comparing Long column with String reference via " + operator) //
            .isInstanceOf(InvalidSettingsException.class) //
            .hasMessageContaining("Cannot apply ordering comparison to input column of type \""
                + LongCell.TYPE.getName() + "\" and reference value of type \"" + StringCell.TYPE.getName() + "\"");
    }

    @SuppressWarnings("static-method")
    @ParameterizedTest
    @MethodSource("getSupportedOperators")
    void testDouble(final LegacyFilterOperator operator) {

        // Double column with Int reference
        assertThatCode(() -> createPredicate("Double1", new IntCell(1), operator)) //
            .as("Comparing Double column with Int reference via " + operator) //
            .doesNotThrowAnyException();

        // Double column with Long reference should throw exception
        assertThatCode(() -> createPredicate("Double1", new LongCell(1), operator)) //
            .as("Comparing Double column with Long reference via " + operator) //
            .isInstanceOf(InvalidSettingsException.class) //
            .hasMessageContaining("Cannot apply ordering comparison to input column of type \""
                + DoubleCell.TYPE.getName() + "\" and reference value of type \"" + LongCell.TYPE.getName() + "\"");

        // Double column with Double reference
        assertThatCode(() -> createPredicate("Double1", new DoubleCell(1.0), operator)) //
            .as("Comparing Double column with Double reference via " + operator) //
            .doesNotThrowAnyException();

        // Double column with String reference should throw exception
        assertThatCode(() -> createPredicate("Double1", new StringCell("foo"), operator)) //
            .as("Comparing Double column with String reference via " + operator) //
            .isInstanceOf(InvalidSettingsException.class) //
            .hasMessageContaining("Cannot apply ordering comparison to input column of type \""
                + DoubleCell.TYPE.getName() + "\" and reference value of type \"" + StringCell.TYPE.getName() + "\"");
    }

    @SuppressWarnings("static-method")
    @ParameterizedTest
    @MethodSource("getSupportedOperators")
    void testDataCell(final LegacyFilterOperator operator) {
        // test with a dummy cell type
        assertThatCode(() -> createPredicate("Dummy1", new FilterDummyDataCellExtension.FilterDummyCell("1"), operator)) //
            .as("Comparing FilterDummyCell column with FilterDummyCell reference via " + operator) //
            .doesNotThrowAnyException();

        // test dummy cell with incompatible reference value, e.g. String, throws exception
        assertThatCode(() -> createPredicate("Dummy1", new StringCell("foo"), operator)) //
            .as("Comparing FilterDummyCell column with String reference via " + operator) //
            .isInstanceOf(InvalidSettingsException.class) //
            .hasMessageContaining("Column type \"FilterDummyCell\" is not a super type of reference type \""
                + StringCell.TYPE.getName() + "\"");
    }

    @SuppressWarnings("static-method")
    @ParameterizedTest
    @MethodSource("getSupportedOperators")
    void testBooleanUnsupported(final LegacyFilterOperator operator) {
        // Boolean columns are unsupported
        assertThat(OrderingPredicateFactory
            .create(EqualityPredicateFactoryTest.SPEC.getColumnSpec("Bool1").getType(), operator)) //
            .as("Comparing Boolean column via " + operator) //
            .isEmpty();
    }

}
