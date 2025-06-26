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

import java.util.OptionalInt;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.knime.base.data.filter.row.v2.IndexedRowReadPredicate;
import org.knime.base.node.preproc.filter.row3.FilterDummyDataCellExtension;
import org.knime.base.node.preproc.filter.row3.FilterDummyDataCellExtension.FilterDummyCell;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataTableSpecCreator;
import org.knime.core.data.def.BooleanCell;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.LongCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.webui.node.dialog.defaultdialog.internal.dynamic.DynamicValuesInput;

/**
 * This unit test tests the construction and error handling of equality predicates.
 * Actually evaluating the predicates is tested in Testflows via the Row Filter node.
 *
 * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
 */
@ExtendWith(FilterDummyDataCellExtension.class)
final class EqualityPredicateFactoryTest {

    static final DataTableSpec SPEC = new DataTableSpecCreator() //
        .addColumns( //
            new DataColumnSpecCreator("Int1", IntCell.TYPE).createSpec(), //
            new DataColumnSpecCreator("Double1", DoubleCell.TYPE).createSpec(), //
            new DataColumnSpecCreator("Bool1", BooleanCell.TYPE).createSpec(), //
            new DataColumnSpecCreator("String1", StringCell.TYPE).createSpec(), //
            new DataColumnSpecCreator("Int2", IntCell.TYPE).createSpec(), //
            new DataColumnSpecCreator("Long1", LongCell.TYPE).createSpec(), //
            new DataColumnSpecCreator("Dummy1", FilterDummyCell.TYPE).createSpec()) //
        .createSpec();

    @SuppressWarnings("static-method")
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void testRowKey(final boolean matchEqual) {
        final var factoryFactory = EqualityPredicateFactory.forRowKey(matchEqual);
        assertThat(factoryFactory) //
            .as("RowID can be filtered with EQ/NEQ") //
            .isPresent();

        final var factory = factoryFactory.get();
        assertThatCode(() -> factory.createPredicate(OptionalInt.empty(), DynamicValuesInput.forRowID())) //
            .as("RowID can be filtered with EQ/NEQ") //
            .doesNotThrowAnyException();

    }

    private static IndexedRowReadPredicate createPredicate(final String columnName, final DataCell referenceCell,
        final boolean matchEqual) throws InvalidSettingsException {
        return EqualityPredicateFactory.create(SPEC.getColumnSpec(columnName).getType(), matchEqual).orElseThrow()
            .createPredicate(OptionalInt.of(SPEC.findColumnIndex(columnName)),
                DynamicValuesInput.singleValueWithInitialValue(referenceCell.getType(), referenceCell));
    }

    @SuppressWarnings("static-method")
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void testInt(final boolean matchEqual) throws InvalidSettingsException {


        // Int column with Int reference
        assertThatCode(() -> createPredicate("Int1", new IntCell(1), matchEqual)) //
                .as("Comparing Int column with Int reference via equality") //
                .doesNotThrowAnyException();

        // Int column with Long reference
        assertThatCode(() -> createPredicate("Int1", new LongCell(1), matchEqual)) //
            .as("Comparing Int column with Long reference via equality") //
            .doesNotThrowAnyException();

        // Int column with Long value outside of Int range should short-circuit
        final var shortCircuit = matchEqual ? IndexedRowReadPredicate.FALSE : IndexedRowReadPredicate.TRUE;
        assertThat(createPredicate("Int1", new LongCell(Long.MAX_VALUE), matchEqual))
            .as("Comparing Int column via EQ with largest Long value does short-circuit to %s"
                .formatted(matchEqual ? "FALSE" : "TRUE")) //
            .isEqualTo(shortCircuit);
        assertThat(createPredicate("Int1", new LongCell(Long.MIN_VALUE), matchEqual))
            .as("Comparing Int column via EQ with smallest Long value does short-circuit to %s"
                .formatted(matchEqual ? "FALSE" : "TRUE")) //
            .isEqualTo(shortCircuit);

        // Int column with Double reference
        assertThatCode(() -> createPredicate("Int1", new DoubleCell(1.0), matchEqual)) //
            .as("Comparing Int column with Double reference via equality") //
            .doesNotThrowAnyException();

        // Int column with Double value outside of Int range should short-circuit
        assertThat(createPredicate("Int1", new DoubleCell(Double.MAX_VALUE), matchEqual))
            .as("Comparing Int column via EQ with largest, finite Double value does short-circuit to %s"
                .formatted(matchEqual ? "FALSE" : "TRUE")) //
            .isEqualTo(shortCircuit);
        assertThat(createPredicate("Int1", new DoubleCell(-Double.MAX_VALUE), matchEqual))
            .as("Comparing Int column via EQ with largest, negative, finite Long value does short-circuit to %s"
                .formatted(matchEqual ? "FALSE" : "TRUE")) //
            .isEqualTo(shortCircuit);

        // Int column with String reference should throw exception
        assertThatCode(() -> createPredicate("Int1", new StringCell("foo"), matchEqual)) //
            .as("Comparing Int column with String reference via equality") //
            .isInstanceOf(InvalidSettingsException.class) //
            .hasMessageContaining(
                "Cannot compare input column of type \"" + IntCell.TYPE.getName() + "\" with a value of type \""
                    + StringCell.TYPE.getName() + "\" for %sequality".formatted(matchEqual ? "" : "in"));
    }

    @SuppressWarnings("static-method")
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void testLong(final boolean matchEqual) {
        // Long column with Long reference
        assertThatCode(() -> createPredicate("Long1", new LongCell(1), matchEqual)) //
            .as("Comparing Long column with Long reference via equality") //
            .doesNotThrowAnyException();

        // Long column with Int reference
        assertThatCode(() -> createPredicate("Long1", new IntCell(1), matchEqual)) //
            .as("Comparing Long column with Int reference via equality") //
            .doesNotThrowAnyException();

        // Long column with Double reference should throw exception
        assertThatCode(() -> createPredicate("Long1", new DoubleCell(1.0), matchEqual)) //
            .as("Comparing Long column with Double reference via equality") //
            .isInstanceOf(InvalidSettingsException.class) //
            .hasMessageContaining(
                "Cannot compare input column of type \"" + LongCell.TYPE.getName() + "\" with a value of type \""
                    + DoubleCell.TYPE.getName() + "\" for %sequality".formatted(matchEqual ? "" : "in"));
    }

    @SuppressWarnings("static-method")
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void testDouble(final boolean matchEqual) {
        // Double column with Double reference
        assertThatCode(() -> createPredicate("Double1", new DoubleCell(1.0), matchEqual)) //
            .as("Comparing Double column with Double reference via equality") //
            .doesNotThrowAnyException();

        // Double column with Int reference
        assertThatCode(() -> createPredicate("Double1", new IntCell(1), matchEqual)) //
            .as("Comparing Double column with Int reference via equality") //
            .doesNotThrowAnyException();

        // Double column with Long reference should throw exception
        assertThatCode(() -> createPredicate("Double1", new LongCell(1), matchEqual)) //
            .as("Comparing Double column with Long reference via equality") //
            .isInstanceOf(InvalidSettingsException.class) //
            .hasMessageContaining(
                "Cannot compare input column of type \"" + DoubleCell.TYPE.getName() + "\" with a value of type \""
                    + LongCell.TYPE.getName() + "\" for %sequality".formatted(matchEqual ? "" : "in"));
    }

    @SuppressWarnings("static-method")
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void testString(final boolean matchEqual) {
        // String cell with String reference
        assertThatCode(() -> createPredicate("String1", new StringCell("foo"), matchEqual)) //
            .as("Comparing String column with String reference via equality") //
            .doesNotThrowAnyException();

        // String cell with incompatible data cell should throw exception (i.e. dummy is not string-compatible)
        assertThatCode(() -> createPredicate("String1", new FilterDummyCell("1"), matchEqual)) //
            .as("Comparing String column with incompatible data cell reference via equality") //
            .isInstanceOf(InvalidSettingsException.class) //
            .hasMessageContaining("Cannot compare input column of type \"String\" "
                + "with a value of type \"FilterDummyCell\" for %sequality".formatted(matchEqual ? "" : "in"));

    }

    @SuppressWarnings("static-method")
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void testDataCell(final boolean matchEqual) {
        // Dummy column with Dummy reference
        assertThatCode(() -> createPredicate("Dummy1", new FilterDummyCell("1"), matchEqual)) //
            .as("Comparing Dummy column with Dummy reference via equality") //
            .doesNotThrowAnyException();
    }


    @SuppressWarnings("static-method")
    @Test
    void testBooleanUnsupported() {
        assertThat(EqualityPredicateFactory.create(BooleanCell.TYPE, true)) //
            .as("Boolean columns cannot be filtered with EQ/NEQ") //
            .isEmpty();
    }

}
