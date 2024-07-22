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

import java.util.function.Supplier;

import org.junit.jupiter.api.Test;
import org.knime.base.node.preproc.filter.row3.AbstractRowFilterNodeSettings.FilterCriterion.OperatorRef;
import org.knime.base.node.preproc.filter.row3.AbstractRowFilterNodeSettings.FilterCriterion.SelectedColumnRef;
import org.knime.base.node.preproc.filter.row3.FilterOperatorTest.TestInitializer;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataTableSpecCreator;
import org.knime.core.data.def.BooleanCell;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.LongCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings;
import org.knime.core.webui.node.dialog.defaultdialog.setting.columnselection.ColumnSelection;
import org.knime.core.webui.node.dialog.defaultdialog.widget.choices.SpecialColumns;
import org.knime.core.webui.node.dialog.defaultdialog.widget.dynamic.DynamicValuesInput;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.Reference;

/**
 * Tests that the settings initialize expected dynamic values input elements.
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
            new DataColumnSpecCreator("Long1", LongCell.TYPE).createSpec()) //
        .createSpec();

    @SuppressWarnings("static-method")
    @Test
    void testTypeAndOperatorInputRowID() {
        final var rowIdFilterInput = inputFor(SpecialColumns.ROWID.toColumnSelection(), FilterOperator.EQ);
        final var expected = DynamicValuesInput.forRowID();
        assertThat(rowIdFilterInput).as("The initial value input for RowIDs is 'RowID = \"\"'").isEqualTo(expected);
    }

    @SuppressWarnings("static-method")
    @Test
    void testTypeAndOperatorInputRowNumber() {
        final var rowNumberFilterInput = inputFor(SpecialColumns.ROW_NUMBERS.toColumnSelection(), FilterOperator.EQ);
        final var expected = DynamicValuesInput.forRowNumber();
        assertThat(rowNumberFilterInput).as("The initial value input for RowNumbers is 'RowNumber = \"1\"'")
            .isEqualTo(expected);
    }

    @SuppressWarnings("static-method")
    @Test
    void testTypeAndOperatorInputIntColumn() {
        final var rowNumberFilterInput = inputFor(new ColumnSelection("Int1", IntCell.TYPE), FilterOperator.EQ);
        final var expected = DynamicValuesInput.singleValueWithCaseMatchingForStringWithDefault(IntCell.TYPE);
        assertThat(rowNumberFilterInput).as("The initial value input for integer column is 'Int1 = \"?\"'")
            .isEqualTo(expected);
    }

    @SuppressWarnings("static-method")
    @Test
    void testTypeAndOperatorInputBoolColumn() {
        final var rowNumberFilterInput =
            inputFor(new ColumnSelection("Bool1", BooleanCell.TYPE), FilterOperator.IS_TRUE);
        // boolean columns have no input value, since we use IS_TRUE and IS_FALSE as operators
        final var expected = DynamicValuesInput.emptySingle();
        assertThat(rowNumberFilterInput).as("The initial value input for boolean column is 'Bool1 IS TRUE'")
            .isEqualTo(expected);
    }

    private static DynamicValuesInput inputFor(final ColumnSelection columnSelection, final FilterOperator operator) {
        final var ctx = DefaultNodeSettings.createDefaultNodeSettingsContext(new DataTableSpec[]{SPEC});
        final var provider = new AbstractRowFilterNodeSettings.FilterCriterion.TypeAndOperatorBasedInput();
        provider.init(new TestInitializer() {
            // - selected column value supplier
            // - current value supplier
            @SuppressWarnings("unchecked")
            @Override
            public <T> Supplier<T> getValueSupplier(final Class<? extends Reference<T>> ref) {
                if (ref.equals(AbstractRowFilterNodeSettings.FilterCriterion.SelectedColumnRef.class)) {
                    return () -> (T)columnSelection;
                }
                if (ref.equals(AbstractRowFilterNodeSettings.FilterCriterion.DynamicValuesInputRef.class)) {
                    return () -> (T)DynamicValuesInput.emptySingle();
                }
                throw new IllegalStateException("Unexpected dependency \"%s\"".formatted(ref.getName()));
            }

            // current operator state
            @SuppressWarnings("unchecked")
            @Override
            public <T> Supplier<T> computeFromValueSupplier(final Class<? extends Reference<T>> ref) {
                if (ref.equals(OperatorRef.class)) {
                    return () -> (T)operator;
                }
                if (ref.equals(SelectedColumnRef.class)) {
                    return () -> (T)columnSelection;
                }
                throw new IllegalStateException("Unexpected reference class \"%s\"".formatted(ref.getName()));
            }

            // expected to be called, since the TypeBasedOperatorChoice does not have OperatorRef as trigger,
            // only as dependency
            @Override
            public <T> void computeOnValueChange(final Class<? extends Reference<T>> id) {
                // expected to be called
            }
        });
        return provider.computeState(ctx);
    }

}
