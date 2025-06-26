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
package org.knime.base.node.preproc.filter.row3.predicates;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import java.util.OptionalInt;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.knime.base.data.filter.row.v2.IndexedRowReadPredicate;
import org.knime.core.data.DataCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.webui.node.dialog.defaultdialog.internal.dynamic.DynamicValuesInput;

/**
 * This unit test tests the construction and error handling of ordering predicates. Actually evaluating the predicates
 * is tested in Testflows via the Row Filter node.
 *
 * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
 */
class PatternMatchingPredicateFactoryTest {

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void testRowKey(final boolean isRegex) {
        assertThatCode(() -> PatternMatchingPredicateFactory.forRowKey(isRegex).createPredicate(OptionalInt.empty(),
            DynamicValuesInput.singleValueWithCaseMatchingForStringWithDefault(StringCell.TYPE)))
                .as("RowIDs can be matched with pattern") //
                .doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void testRowNumber(final boolean isRegex) {
        assertThatCode(() -> PatternMatchingPredicateFactory.forRowNumber(isRegex).createPredicate(OptionalInt.empty(),
            DynamicValuesInput.singleValueWithCaseMatchingForStringWithDefault(StringCell.TYPE)))
                .as("Row numbers can be matched with pattern") //
                .doesNotThrowAnyException();
    }

    private static IndexedRowReadPredicate createPredicate(final String columnName, final DataCell referenceCell,
        final boolean isRegex) throws InvalidSettingsException {
        return PatternMatchingPredicateFactory
            .forColumn(EqualityPredicateFactoryTest.SPEC.getColumnSpec(columnName).getType(), isRegex).orElseThrow()
            .createPredicate(OptionalInt.of(EqualityPredicateFactoryTest.SPEC.findColumnIndex(columnName)),
                DynamicValuesInput.singleValueWithInitialValue(referenceCell.getType(), referenceCell));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void testStringPredicate(final boolean isRegex) {
        assertThatCode(() -> createPredicate("String1", new StringCell("foo"), isRegex)) //
                .as("Strings can be pattern matched") //
                .doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void testStringPredicateException(final boolean isRegex) {
        final var colSpec = EqualityPredicateFactoryTest.SPEC.getColumnSpec("String1");
        final var colIdx = OptionalInt.of(EqualityPredicateFactoryTest.SPEC.findColumnIndex("String1"));
        final var factory = PatternMatchingPredicateFactory.forColumn(colSpec.getType(), isRegex) //
            .orElseThrow();

        // this exception comes from the DynamicValuesInput widget
        final var value = DynamicValuesInput.singleValueWithCaseMatchingForStringWithDefault(IntCell.TYPE);
        assertThatCode(() -> factory.createPredicate(colIdx, value)) //
                .as("Incompatible reference value for pattern") //
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Value at index 0 does not use case-matching settings");

        // now mock the widget to not raise the exception before, in order to hit the next exception
        final var valueSpy = spy(value);
        doReturn(true).when(valueSpy).isStringMatchCaseSensitive(0);
        assertThatCode(() -> factory.createPredicate(colIdx, valueSpy)) //
            .as("Pattern value must be string compatible") //
            .isInstanceOf(InvalidSettingsException.class) //
            .hasMessageContaining("Cannot obtain pattern for %s operator from reference value of type \"%s\""
                .formatted(isRegex ? "regex" : "wildcard", IntCell.TYPE));

        final var idx = OptionalInt.empty();
        final var strValue = DynamicValuesInput.singleValueWithCaseMatchingForStringWithDefault(StringCell.TYPE);
        assertThatCode(() -> factory.createPredicate(idx, strValue)) //
            .as("Column index is required") //
            .isInstanceOf(IllegalArgumentException.class) //
            .hasMessageContaining("Pattern match on column requires a column index, but none was provided");
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void testIntPredicate(final boolean isRegex) {
        assertThatCode(() -> createPredicate("Int1", new StringCell("1"), isRegex)) //
            .as("Integers can be pattern matched") //
            .doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void testLongPredicate(final boolean isRegex) {
        assertThatCode(() -> createPredicate("Long1", new StringCell("1"), isRegex)) //
            .as("Longs can be pattern matched") //
            .doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void testDoublePredicate(final boolean isRegex) {
        assertThat(PatternMatchingPredicateFactory
            .forColumn(EqualityPredicateFactoryTest.SPEC.getColumnSpec("Double1").getType(), isRegex)) //
                .as("Doubles cannot be pattern matched") //
                .isEmpty();
    }

}
