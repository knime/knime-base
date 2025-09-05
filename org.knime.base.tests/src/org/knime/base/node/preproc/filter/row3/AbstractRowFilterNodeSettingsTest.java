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
 *   27 Jun 2024 (manuelhotz): created
 */
package org.knime.base.node.preproc.filter.row3;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;
import org.knime.base.node.preproc.filter.row3.TestUtil.TestInitializer;
import org.knime.base.node.preproc.filter.row3.operators.missing.IsMissingFilterOperator;
import org.knime.base.node.preproc.filter.row3.operators.missing.IsNotMissingFilterOperator;
import org.knime.base.node.preproc.filter.row3.operators.pattern.RegexOperator;
import org.knime.base.node.preproc.filter.row3.operators.pattern.RegexPatternFilterOperator;
import org.knime.base.node.preproc.filter.row3.operators.pattern.WildcardOperator;
import org.knime.base.node.preproc.filter.row3.operators.pattern.WildcardPatternFilterOperator;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataTableSpecCreator;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValue;
import org.knime.core.data.StringValue;
import org.knime.core.data.def.BooleanCell;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.LongCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.webui.node.dialog.defaultdialog.NodeParametersUtil;
import org.knime.core.webui.node.dialog.defaultdialog.internal.dynamic.extensions.filtervalue.builtin.EqualsOperator;
import org.knime.core.webui.node.dialog.defaultdialog.internal.dynamic.extensions.filtervalue.builtin.GreaterThanOperator;
import org.knime.core.webui.node.dialog.defaultdialog.internal.dynamic.extensions.filtervalue.builtin.GreaterThanOrEqualOperator;
import org.knime.core.webui.node.dialog.defaultdialog.internal.dynamic.extensions.filtervalue.builtin.LessThanOperator;
import org.knime.core.webui.node.dialog.defaultdialog.internal.dynamic.extensions.filtervalue.builtin.LessThanOrEqualOperator;
import org.knime.core.webui.node.dialog.defaultdialog.internal.dynamic.extensions.filtervalue.builtin.NotEqualsNorMissingOperator;
import org.knime.core.webui.node.dialog.defaultdialog.internal.dynamic.extensions.filtervalue.builtin.NotEqualsOperator;
import org.knime.core.webui.node.dialog.defaultdialog.setting.singleselection.StringOrEnum;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.updates.ParameterReference;
import org.knime.node.parameters.widget.choices.StringChoice;

/**
 * Tests that the available operators for different data types are as expected. This is only needed for new operators
 * and new style parameters, not legacy ones, since we only ever load existing legacy configurations but don't create
 * new ones.
 *
 * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
 */
@SuppressWarnings("restriction") // webui
final class AbstractRowFilterNodeSettingsTest {

    private static final DataTableSpec SPEC = new DataTableSpecCreator() //
        .addColumns( //
            new DataColumnSpecCreator("Int1", IntCell.TYPE).createSpec(), //
            new DataColumnSpecCreator("Double1", DoubleCell.TYPE).createSpec(), //
            new DataColumnSpecCreator("Bool1", BooleanCell.TYPE).createSpec(), //
            new DataColumnSpecCreator("String1", StringCell.TYPE).createSpec(), //
            new DataColumnSpecCreator("Int2", IntCell.TYPE).createSpec(), //
            new DataColumnSpecCreator("Long1", LongCell.TYPE).createSpec(), //
            // DataValue only
            new DataColumnSpecCreator("NonNativeDV",
                DataType.createNonNativeType(List.of(DataValue.class), null, List.of())).createSpec(), //
            // Common interface is StringValue
            new DataColumnSpecCreator("NonNativeCommonString",
                DataType.createNonNativeType(List.of(StringValue.class), null, List.of())).createSpec())
        .createSpec();

    private static final NodeParametersInput CTX =
        NodeParametersUtil.createDefaultNodeSettingsContext(new DataTableSpec[]{SPEC});

    private static final String FIRST_N_ID = "FIRST_N_ROWS";

    private static final String LAST_N_ID = "LAST_N_ROWS";

    /*
     * ====== Tests for available operators for different data types. ======
     */

    /**
     * List of supported operators for row ID / row key.
     */
    @SuppressWarnings("static-method")
    @Test
    void testRowKeyOperatorChoices() {
        final var operatorChoices = createChoicesFor(new StringOrEnum<RowIdentifiers>(RowIdentifiers.ROW_ID));
        assertThat(operatorChoices).as("Operator choices for RowID") //
            .extracting(c -> c.id()) //
            .containsExactly( //
                EqualsOperator.ID, //
                NotEqualsOperator.ID, //
                RegexOperator.ID, //
                WildcardOperator.ID);
    }

    /**
     * List of supported operators for row numbers.
     */
    @SuppressWarnings("static-method")
    @Test
    void testRowNumberOperators() {
        final var choices = createChoicesFor(new StringOrEnum<RowIdentifiers>(RowIdentifiers.ROW_NUMBER));
        // we explicitly enumerate the expected IDs
        final var expected = List.of(//
            EqualsOperator.ID, NotEqualsOperator.ID, //
            LessThanOperator.ID, LessThanOrEqualOperator.ID, GreaterThanOperator.ID, GreaterThanOrEqualOperator.ID, //
            FIRST_N_ID, LAST_N_ID, //
            RegexOperator.ID, //
            WildcardOperator.ID);

        assertThat(choices).as("Operator choices for Row Number") //
            .extracting(c -> c.id()) //
            .containsExactly(expected.toArray(String[]::new));
    }

    /**
     * List of supported operators for integer columns.
     */
    @SuppressWarnings("static-method")
    @Test
    void testIntegerOperators() {
        final var choices = createChoicesFor(new StringOrEnum<RowIdentifiers>("Int1"));
        // we explicitly enumerate the expected IDs
        final var expected = List.of(//
            EqualsOperator.ID, NotEqualsOperator.ID, NotEqualsNorMissingOperator.ID, //
            LessThanOperator.ID, LessThanOrEqualOperator.ID, GreaterThanOperator.ID, GreaterThanOrEqualOperator.ID, //
            RegexOperator.ID, WildcardOperator.ID, //
            IsMissingFilterOperator.getInstance().getId(), IsNotMissingFilterOperator.getInstance().getId() //
        );

        assertThat(choices).as("Operator choices for Integer column") //
            .extracting(c -> c.id()) //
            .containsExactly(expected.toArray(String[]::new));
    }

    /**
     * List of supported operators for double columns.
     */
    @SuppressWarnings("static-method")
    @Test
    void testDoubleOperators() {
        final var choices = createChoicesFor(new StringOrEnum<RowIdentifiers>("Double1"));
        // we explicitly enumerate the expected IDs
        final var expected = List.of(//
            EqualsOperator.ID, NotEqualsOperator.ID, NotEqualsNorMissingOperator.ID, //
            LessThanOperator.ID, LessThanOrEqualOperator.ID, GreaterThanOperator.ID, GreaterThanOrEqualOperator.ID, //
            IsMissingFilterOperator.getInstance().getId(), IsNotMissingFilterOperator.getInstance().getId() //
        );

        assertThat(choices).as("Operator choices for Double column") //
            .extracting(c -> c.id()) //
            .containsExactly(expected.toArray(String[]::new));
    }

    /**
     * List of supported operators for boolean columns.
     */
    @SuppressWarnings("static-method")
    @Test
    void testBooleanOperators() {
        final var choices = createChoicesFor(new StringOrEnum<RowIdentifiers>("Bool1"));
        // we explicitly enumerate the expected IDs
        final var expected = List.of(//
            // boolean does not have "equal" etc, but only special operators
            "IS_TRUE", "IS_FALSE", IsMissingFilterOperator.getInstance().getId(),
            IsNotMissingFilterOperator.getInstance().getId() //
        );

        assertThat(choices).as("Operator choices for Boolean column") //
            .extracting(c -> c.id()) //
            .containsExactly(expected.toArray(String[]::new));
    }

    /**
     * List of supported operators for string columns.
     */
    @SuppressWarnings("static-method")
    @Test
    void testStringOperators() {
        final var choices = createChoicesFor(new StringOrEnum<RowIdentifiers>("String1"));
        // we explicitly enumerate the expected IDs
        final var expected = List.of(//
            EqualsOperator.ID, NotEqualsOperator.ID, NotEqualsNorMissingOperator.ID, //
            // comparator based operators are not offered (but supported for backwards compatibility on execute)
            RegexPatternFilterOperator.getInstance().getId(), WildcardPatternFilterOperator.getInstance().getId(), //
            IsMissingFilterOperator.getInstance().getId(), IsNotMissingFilterOperator.getInstance().getId() //
        );

        assertThat(choices).as("Operator choices for String column") //
            .extracting(c -> c.id()) //
            .containsExactly(expected.toArray(String[]::new));
    }

    /**
     * List of supported operators for non-native columns that only have DataValue.
     */
    @SuppressWarnings("static-method")
    @Test
    void testNonNativeOperators() {
        final var choices = createChoicesFor(new StringOrEnum<RowIdentifiers>("NonNativeDV"));
        // Non-native columns should only support missing value operators
        final var expected = List.of(//
            IsMissingFilterOperator.getInstance().getId(), IsNotMissingFilterOperator.getInstance().getId() //
        );

        assertThat(choices).as("Operator choices for NonNative column") //
            .extracting(c -> c.id()) //
            .containsExactly(expected.toArray(String[]::new));
    }

    /**
     * List of supported operators for non-native columns that have StringValue as common interface.
     */
    @SuppressWarnings("static-method")
    @Test
    void testNonNativeStringValueOperators() {
        final var choices = createChoicesFor(new StringOrEnum<RowIdentifiers>("NonNativeCommonString"));
        final var expected = List.of(//
            RegexPatternFilterOperator.getInstance().getId(), WildcardPatternFilterOperator.getInstance().getId(), //
            IsMissingFilterOperator.getInstance().getId(), IsNotMissingFilterOperator.getInstance().getId() //
        );

        assertThat(choices).as("Operator choices for NonNative column with StringValue") //
            .extracting(c -> c.id()) //
            .containsExactly(expected.toArray(String[]::new));
    }

    /**
     * List of operators for unknown column.
     */
    @SuppressWarnings("static-method")
    @Test
    void testUnknownColumnOperators() {
        final var choices = createChoicesFor(new StringOrEnum<RowIdentifiers>("UnknownColumn"));

        assertThat(choices).as("Operator choices for unknown column") //
            .extracting(c -> c.id()) //
            .isEmpty();
        final var choicesFromPreviousStringColumn =
            createChoicesFor(new StringOrEnum<RowIdentifiers>("UnknownColumn"), StringCell.TYPE);
        assertThat(choicesFromPreviousStringColumn).as("Operator choices for unknown column, previous String") //
            .extracting(c -> c.id()) //
            .isNotEmpty();

    }

    /**
     * Gets the available operator choices for the given column choice. This method sets up the necessary provider
     * initialization and makes the dependencies for the computation available based on the passed argument.
     *
     * @param columnChoice the column choice (column or RowID/row number)
     * @return the available operator choices
     */
    private static List<StringChoice> createChoicesFor(final StringOrEnum<RowIdentifiers> columnChoice) {
        return createChoicesFor(columnChoice, null);
    }

    private static List<StringChoice> createChoicesFor(final StringOrEnum<RowIdentifiers> columnChoice,
        final DataType previousColumnType) {
        final var beforeOpenDialogCalled = new AtomicBoolean();
        final var provider = new AbstractRowFilterNodeSettings.FilterCriterion.OperatorsProvider();
        // the test initializer simulates part of the dialog framework and injects our column choice, which is a
        // dependency of the operator choices provider
        provider.init(new TestInitializer() {

            @Override
            public void computeBeforeOpenDialog() {
                beforeOpenDialogCalled.set(true);
            }

            @SuppressWarnings("unchecked")
            @Override
            public <T> Supplier<T> computeFromValueSupplier(final Class<? extends ParameterReference<T>> ref) {
                // the available operators depend only on the selected "column"
                if (ref.equals(AbstractRowFilterNodeSettings.FilterCriterion.SelectedColumnRef.class)) {
                    return () -> (T)columnChoice;
                }
                throw new IllegalStateException("Unexpected dependency: " + ref);
            }

            @SuppressWarnings("unchecked")
            @Override
            public <T> Supplier<T> getValueSupplier(final Class<? extends ParameterReference<T>> ref) {
                if (ref.equals(AbstractRowFilterNodeSettings.FilterCriterion.ColumnDataTypeRef.class)) {
                    return () -> (T)previousColumnType;
                }
                throw new IllegalStateException("Unexpected dependency: " + ref);
            }

        });
        assertThat(beforeOpenDialogCalled).as("computeBeforeOpenDialog has been called").isTrue();
        return provider.computeState(CTX);
    }

}