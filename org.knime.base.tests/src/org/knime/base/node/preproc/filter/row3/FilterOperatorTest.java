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
 *   31 Mar 2024 (jasper): created
 */
package org.knime.base.node.preproc.filter.row3;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.knime.base.node.preproc.filter.row3.AbstractRowFilterNodeSettings.TypeBasedOperatorChoices;
import org.knime.base.node.preproc.filter.row3.AbstractRowFilterNodeSettings.TypeBasedOperatorsProvider;
import org.knime.base.node.preproc.filter.row3.predicates.PredicateFactories;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataTableSpecCreator;
import org.knime.core.data.DataType;
import org.knime.core.data.def.BooleanCell;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.LongCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings.DefaultNodeSettingsContext;
import org.knime.core.webui.node.dialog.defaultdialog.setting.columnselection.ColumnSelection;
import org.knime.core.webui.node.dialog.defaultdialog.widget.choices.SpecialColumns;
import org.knime.core.webui.node.dialog.defaultdialog.widget.dynamic.DynamicValuesInput;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.ButtonReference;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.Reference;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.StateProvider;

/**
 * Tests for FilterOperator choices.
 *
 * @author Jasper Krauter, KNIME GmbH, Konstanz, Germany
 */
@SuppressWarnings({"restriction", "static-method"})
@ExtendWith(FilterDummyDataCellExtension.class)
final class FilterOperatorTest {

    private static final DataTableSpec SPEC = new DataTableSpecCreator() //
        .addColumns( //
            new DataColumnSpecCreator("Int1", IntCell.TYPE).createSpec(), //
            new DataColumnSpecCreator("Double1", DoubleCell.TYPE).createSpec(), //
            new DataColumnSpecCreator("Bool1", BooleanCell.TYPE).createSpec(), //
            new DataColumnSpecCreator("String1", StringCell.TYPE).createSpec(), //
            new DataColumnSpecCreator("Int2", IntCell.TYPE).createSpec(), //
            new DataColumnSpecCreator("Long1", LongCell.TYPE).createSpec()) //
        .createSpec();

    @Test
    void testOperatorChoices() {
        assertThat(operatorChoicesFor("String1", StringCell.TYPE))
            .as("The list of operators for a string column is what is expected").containsExactlyInAnyOrder( //
                FilterOperator.IS_MISSING, //
                FilterOperator.IS_NOT_MISSING, //
                FilterOperator.EQ, //
                FilterOperator.NEQ, //
                FilterOperator.REGEX, //
                FilterOperator.WILDCARD //
            );
        assertThat(operatorChoicesFor("Int1", IntCell.TYPE))
            .as("The list of operators for an integer column is what is expected").containsExactlyInAnyOrder( //
                FilterOperator.IS_MISSING, //
                FilterOperator.IS_NOT_MISSING, //
                FilterOperator.EQ, //
                FilterOperator.NEQ, //
                FilterOperator.GT, //
                FilterOperator.GTE, //
                FilterOperator.LT, //
                FilterOperator.LTE, //
                FilterOperator.WILDCARD, //
                FilterOperator.REGEX //
            );
        assertThat(operatorChoicesFor("Long1", LongCell.TYPE))
            .as("The list of operators for int cells is the same as for long cells")
            .isEqualTo(operatorChoicesFor("Int1", IntCell.TYPE));
        assertThat(operatorChoicesFor("Double1", DoubleCell.TYPE))
            .as("The list of operators for a double column is what is expected").containsExactlyInAnyOrder( //
                FilterOperator.IS_MISSING, //
                FilterOperator.IS_NOT_MISSING, //
                FilterOperator.EQ, //
                FilterOperator.NEQ, //
                FilterOperator.GT, //
                FilterOperator.GTE, //
                FilterOperator.LT, //
                FilterOperator.LTE);
        assertThat(operatorChoicesFor("Bool1", BooleanCell.TYPE))
            .as("The list of operators for a boolean column is what is expected").containsExactlyInAnyOrder( //
                FilterOperator.IS_MISSING, //
                FilterOperator.IS_NOT_MISSING, //
                FilterOperator.IS_TRUE, //
                FilterOperator.IS_FALSE //
            );
        assertThat(operatorChoicesFor("Unknown Column", DataType.getType(DataType.getMissingCell().getClass())))
            .as("The list of operators for an unknown column type is what is expected").containsExactlyInAnyOrder( //
                FilterOperator.IS_MISSING, //
                FilterOperator.IS_NOT_MISSING //
            );
    }

    @Test
    void testOperatorChoicesForSpecialColumns() {
        assertThat(operatorChoicesFor(SpecialColumns.ROW_NUMBERS))
            .as("The list of operators for the row numbers is what is expected").containsExactlyInAnyOrder( //
                FilterOperator.EQ, //
                FilterOperator.NEQ, //
                FilterOperator.GT, //
                FilterOperator.GTE, //
                FilterOperator.LT, //
                FilterOperator.LTE, //
                FilterOperator.LAST_N_ROWS, //
                FilterOperator.FIRST_N_ROWS, //
                FilterOperator.WILDCARD, //
                FilterOperator.REGEX //
            );
        assertThat(operatorChoicesFor(SpecialColumns.ROWID))
            .as("The list of operators for the row id column is what is expected").containsExactlyInAnyOrder( //
                FilterOperator.EQ, //
                FilterOperator.NEQ, //
                FilterOperator.REGEX, //
                FilterOperator.WILDCARD //
            );
    }

    static FilterOperator[] operatorChoicesFor(final String col, final DataType type) {
        return operatorChoicesFor(new ColumnSelection(col, type));
    }

    static FilterOperator[] operatorChoicesFor(final SpecialColumns col) {
        return operatorChoicesFor(col.toColumnSelection());
    }

    static FilterOperator[] operatorChoicesFor(final ColumnSelection columnSelection) {
        final var ctx = DefaultNodeSettings.createDefaultNodeSettingsContext(new DataTableSpec[]{SPEC});

        final var provider = new TypeBasedOperatorsProvider();
        provider.init(new TestInitializer() {

            @SuppressWarnings("unchecked")
            @Override
            public <T> Supplier<T> computeFromValueSupplier(final Class<? extends Reference<T>> ref) {
                if (ref.equals(AbstractRowFilterNodeSettings.FilterCriterion.SelectedColumnRef.class)) {
                    return () -> (T)columnSelection;
                }
                throw new IllegalStateException("Unexpected dependency \"%s\"".formatted(ref.getName()));
            }

            @Override
            public void computeAfterOpenDialog() {
                // Do nothing
            }

        });

        final var choices = new TypeBasedOperatorChoices();
        choices.init(new TestInitializer() {
            @SuppressWarnings("unchecked")
            @Override
            public <T> Supplier<T>
                computeFromProvidedState(final Class<? extends StateProvider<T>> stateProviderClass) {
                if (stateProviderClass.equals(TypeBasedOperatorsProvider.class)) {
                    return () -> (T)provider.computeState(ctx);
                }
                throw new IllegalStateException(
                    "Unexpected provider class \"%s\"".formatted(stateProviderClass.getName()));
            }

        });

        return Arrays.stream(choices.computeState(ctx)).map(idAndText -> FilterOperator.valueOf(idAndText.id()))
            .toArray(FilterOperator[]::new);
    }

    static class TestInitializer implements StateProvider.StateProviderInitializer {

        @Override
        public <T> Supplier<T> computeFromValueSupplier(final Class<? extends Reference<T>> ref) {
            throw new IllegalStateException("Not expected to be called during test.");
        }

        @Override
        public <T> Supplier<T> getValueSupplier(final Class<? extends Reference<T>> ref) {
            throw new IllegalStateException("Not expected to be called during test.");
        }

        @Override
        public <T> void computeOnValueChange(final Class<? extends Reference<T>> id) {
            fail("Not expected to be called during test.");
        }

        @Override
        public <T> Supplier<T> computeFromProvidedState(final Class<? extends StateProvider<T>> stateProviderClass) {
            throw new IllegalStateException("Not expected to be called during test.");
        }

        @Override
        public void computeOnButtonClick(final Class<? extends ButtonReference> ref) {
            fail("Not expected to be called during test.");
        }

        @Override
        public void computeBeforeOpenDialog() {
            throw new IllegalStateException("Not expected to be called during test.");
        }

        @Override
        public void computeAfterOpenDialog() {
            fail("Not expected to be called during test.");
        }

        @Override
        public DefaultNodeSettingsContext getContext() {
            throw new IllegalStateException("Not expected to be called during test.");
        }
    }

    @ParameterizedTest
    @EnumSource(names = {"IS_MISSING", "IS_NOT_MISSING", "FIRST_N_ROWS", "LAST_N_ROWS"})
    void testOperatorsWithoutPredicateFactory(final FilterOperator operator) {
        assertThat(PredicateFactories.getValuePredicateFactory(operator, null))
        .as("Operator %s has no value predicate factory".formatted(operator))
        .isEmpty();
    }

    private abstract static class BaseTester {

        abstract Stream<FilterOperator> getOperators();

        final boolean test(final SpecialColumns specialColumn, final DataType type) {
            // tests that the operator is not hidden and can be applied (i.e. has a predicate factory)
            return getOperators() //
                .map(op -> !op.isHidden(specialColumn, type) && op.isApplicableFor(specialColumn, type))
                .allMatch(b -> b);
        }
    }

    private static final class PatternMatchable extends BaseTester {
        @Override
        Stream<FilterOperator> getOperators() {
            return Stream.of(FilterOperator.REGEX, FilterOperator.WILDCARD);
        }
    }

    @Test
    void testIsPatternMatchable() {
        final var tester = new PatternMatchable();
        assertThat(tester) //
            .as("RowID is pattern-matchable") //
            .returns(true, t -> t.test(SpecialColumns.ROWID, StringCell.TYPE)) //
            .as("Normal string column is pattern-matchable") //
            .returns(true, t -> t.test(null, StringCell.TYPE)) //
            .as("Long column is pattern-matchable") //
            .returns(true, t -> t.test(null, LongCell.TYPE)) //
            .as("Int column is pattern-matchable") //
            .returns(true, t -> t.test(null, IntCell.TYPE)) //
            .as("Boolean column is not pattern-matchable") //
            .returns(false, t -> t.test(null, BooleanCell.TYPE));

        // the pattern-match operators should require a String input type
        tester.getOperators().allMatch(op -> op.getRequiredInputType().map(t -> t == StringCell.TYPE).orElse(false));
    }

    private static final class IsEq extends BaseTester {
        @Override
        Stream<FilterOperator> getOperators() {
            return Stream.of(FilterOperator.EQ, FilterOperator.NEQ);
        }
    }

    @Test
    void testIsEq() {
        final var tester = new IsEq();
        assertThat(tester) //
            .as("RowID is eq-able") //
            .returns(true, t -> t.test(SpecialColumns.ROWID, StringCell.TYPE)) //
            .as("Normal string column is eq-able") //
            .returns(true, t -> t.test(null, StringCell.TYPE)) //
            .as("Long column is eq-able") //
            .returns(true, t -> t.test(null, LongCell.TYPE)) //
            .as("Int column is eq-able") //
            .returns(true, t -> t.test(null, IntCell.TYPE)) //
            .as("Double column is eq-able") //
            .returns(true, t -> t.test(null, DoubleCell.TYPE)) //
            .as("Boolean column is not eq-able, should use dedicated operators for that") //
            .returns(false, t -> t.test(null, BooleanCell.TYPE))
            .as("Custom type column is eq-able, if type-mapping has String->Cell converter") //
            .returns(true, t -> t.test(null, FilterDummyDataCellExtension.FilterDummyCell.TYPE));
    }

    private static final class IsTruthy extends BaseTester {

        @Override
        Stream<FilterOperator> getOperators() {
            return Stream.of(FilterOperator.IS_TRUE, FilterOperator.IS_FALSE);
        }
    }

    @Test
    void testIsTruthy() {
        final var tester = new IsTruthy();
        assertThat(tester) //
            .as("RowID is not truthy") //
            .returns(false, t -> t.test(SpecialColumns.ROWID, StringCell.TYPE)) //
            .as("Normal string column is not truthy") //
            .returns(false, t -> t.test(null, StringCell.TYPE)) //
            .as("Long column is not truthy") //
            .returns(false, t -> t.test(null, LongCell.TYPE)) //
            .as("Int column is not truthy") //
            .returns(false, t -> t.test(null, IntCell.TYPE)) //
            .as("Double column is not truthy") //
            .returns(false, t -> t.test(null, DoubleCell.TYPE)) //
            .as("Boolean column is truthy") //
            .returns(true, t -> t.test(null, BooleanCell.TYPE));
    }

    private static final class IsRowNumber extends BaseTester {

        private static final FilterOperator[] SLICED_OPS =
            {FilterOperator.EQ, FilterOperator.NEQ, FilterOperator.GT, FilterOperator.GTE, FilterOperator.LT,
                FilterOperator.LTE, FilterOperator.FIRST_N_ROWS, FilterOperator.LAST_N_ROWS};

        private static final FilterOperator[] VALUE_OPS = {FilterOperator.WILDCARD, FilterOperator.REGEX};

        @Override
        Stream<FilterOperator> getOperators() {
            return Stream.concat(Arrays.stream(SLICED_OPS), Arrays.stream(VALUE_OPS));
        }

        void validate(final DynamicValuesInput input) throws InvalidSettingsException {
            final var criterion = new AbstractRowFilterNodeSettings.FilterCriterion();
            criterion.m_column = SpecialColumns.ROW_NUMBERS.toColumnSelection();
            criterion.m_predicateValues = input;
            for (final var op : SLICED_OPS) {
                criterion.m_operator = op;
                RowNumberFilterSpec.toFilterSpec(criterion);
            }
            for (final var op : VALUE_OPS) {
                criterion.m_operator = op;
                assertThat(PredicateFactories.getRowNumberPredicateFactory(op))
                    .as("Missing factory for operator \"%s\"", op).isPresent();
            }
        }
    }

    @Test
    void testIsRowNumber() {
        final var tester = new IsRowNumber();
        assertThat(tester) //
            .as("Row number is row number") //
            .returns(true, t -> t.test(SpecialColumns.ROW_NUMBERS, IntCell.TYPE)) //
            .as("RowID is not row number") //
            .returns(false, t -> t.test(SpecialColumns.ROWID, StringCell.TYPE)) //
            .as("Normal string column is not row number").returns(false, t -> t.test(null, StringCell.TYPE)); //

        assertThatCode(() -> tester.validate(DynamicValuesInput.forRowNumber(LongCell.TYPE))) //
            .as("Row number with default value validates") //
            .doesNotThrowAnyException();
    }

    private static final class IsOrd extends BaseTester {

        @Override
        Stream<FilterOperator> getOperators() {
            return Stream.of(FilterOperator.GT, FilterOperator.GTE, FilterOperator.LT, FilterOperator.LTE);
        }

    }

    @Test
    void testIsOrd() {
        final var tester = new IsOrd();
        // assert that long is ord but string and boolean are not ord
        assertThat(tester) //
            .as("Long cell is Ord") //
            .returns(true, t -> t.test(null, LongCell.TYPE)) //
            .as("Boolean cell is not Ord") //
            .returns(false, t -> t.test(null, BooleanCell.TYPE)) //
            .as("String cell is not Ord)") //
            .returns(false, t -> t.test(null, StringCell.TYPE)) //
            .as("RowID is not Ord") //
            .returns(false, t -> t.test(SpecialColumns.ROWID, StringCell.TYPE)) //
        ;
    }

    private static final class CanBeMissing extends BaseTester {
        @Override
        Stream<FilterOperator> getOperators() {
            return Stream.of(FilterOperator.IS_MISSING, FilterOperator.IS_NOT_MISSING);
        }
    }

    @Test
    void testCanBeMissing() {
        // assert that anything except RowID and RowNumber can be missing
        final var tester = new CanBeMissing();
        assertThat(tester) //
            .as("Long cell can be missing") //
            .returns(true, t -> t.test(null, LongCell.TYPE)) //
            .as("String cell can be missing") //
            .returns(true, t -> t.test(null, StringCell.TYPE)) //
            .as("Boolean cell can be missing") //
            .returns(true, t -> t.test(null, BooleanCell.TYPE)) //
            .as("RowID cannot be missing") //
            .returns(false, t -> t.test(SpecialColumns.ROWID, StringCell.TYPE)) //
            .as("RowNumber cannot be missing") //
            .returns(false, t -> t.test(SpecialColumns.ROW_NUMBERS, IntCell.TYPE));
    }
}
