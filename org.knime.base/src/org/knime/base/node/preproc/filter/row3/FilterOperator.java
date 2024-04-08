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
 *   25 Mar 2024 (jasper): created
 */
package org.knime.base.node.preproc.filter.row3;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.IntValue;
import org.knime.core.data.LongValue;
import org.knime.core.data.StringValue;
import org.knime.core.data.convert.datacell.JavaToDataCellConverterRegistry;
import org.knime.core.data.def.BooleanCell;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.LongCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings.DefaultNodeSettingsContext;
import org.knime.core.webui.node.dialog.defaultdialog.setting.columnselection.ColumnSelection;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Label;
import org.knime.core.webui.node.dialog.defaultdialog.widget.choices.ChoicesUpdateHandler;
import org.knime.core.webui.node.dialog.defaultdialog.widget.choices.IdAndText;
import org.knime.core.webui.node.dialog.defaultdialog.widget.choices.SpecialColumns;
import org.knime.core.webui.node.dialog.defaultdialog.widget.handler.DeclaringDefaultNodeSettings;
import org.knime.core.webui.node.dialog.defaultdialog.widget.handler.WidgetHandlerException;

/**
 * Enumeration of filter operators for the row filter node. Additionally, encoded in this class is the selection logic
 * for which operators are applicable to which columns and comparison modes.
 *
 * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
 * @author Jasper Krauter, KNIME GmbH, Konstanz, Germany
 */
@SuppressWarnings("restriction") // new ui
enum FilterOperator {
        @Label("=") // RowID, RowIndex/Number, Long, Double, String
        EQ("=", Set.of(new IsEq()), Arity.UNARY), //
        @Label("≠") // RowID, RowIndex/Number, Long, Double, String
        NEQ("≠", Set.of(new IsEq()), Arity.UNARY), //
        @Label("<") // RowIndex/Number, Long, Double
        LT("<", Set.of(new IsOrdNumeric()), Arity.UNARY), //
        @Label("≤") // RowIndex/Number, Long, Double
        LTE("≤", Set.of(new IsOrdNumeric()), Arity.UNARY), //
        @Label(">") // RowIndex/Number, Long, Double
        GT(">", Set.of(new IsOrdNumeric()), Arity.UNARY), //
        @Label("≥") // RowIndex/Number, Long, Double
        GTE("≥", Set.of(new IsOrdNumeric()), Arity.UNARY), //
        @Label("is between") // RowIndex/Number, Long, Double
        BETWEEN("is between", Set.of(new IsOrdNumeric()), Arity.BINARY), //
        @Label("first n rows") // RowIndex/Number
        FIRST_N_ROWS("first n rows", Set.of(new IsRowNumber()), Arity.UNARY), //
        @Label("last n rows") // RowIndex/Number
        LAST_N_ROWS("last n rows", Set.of(new IsRowNumber()), Arity.UNARY), //
        @Label("matches regex") // RowID, String
        REGEX("matches regex", Set.of(new IsPatternMatchable()), Arity.UNARY), //
        @Label("matches wildcard") // RowID, String
        WILDCARD("matches wildcard", Set.of(new IsPatternMatchable()), Arity.UNARY), //
        @Label("is true") // Boolean
        IS_TRUE("is true", Set.of(new IsTruthy()), Arity.NULLARY), //
        @Label("is false") // Boolean
        IS_FALSE("is false", Set.of(new IsTruthy()), Arity.NULLARY), //
        @Label("is missing") // Every ordinary column
        IS_MISSING("is missing", Set.of(new IsMissing()), Arity.NULLARY);

    final String m_label;

    final Set<Capability> m_filters;

    final Arity m_arity;

    FilterOperator(final String label, final Set<Capability> filters, final Arity arity) {
        m_label = label;
        m_filters = filters;
        m_arity = arity;
    }

    boolean isEnabledFor(final SpecialColumns specialColumn, final DataType dataType, final CompareMode mode) {
        return m_filters.stream().anyMatch(f -> f.satisfiedBy(specialColumn, dataType, mode));
    }

    String label() {
        return m_label;
    }

    /**
     * Arity of the operator to determine whether to show zero, one, or two input fields.
     */
    enum Arity {
            NULLARY, UNARY, BINARY
    }

    /**
     * Enumeration of the different value comparison modes.
     */
    enum CompareMode {
            TYPE_MAPPING, AS_STRING, INTEGRAL, DECIMAL, BOOL;
    }

    static final class ColumnSelectionDependency {
        @DeclaringDefaultNodeSettings(RowFilter3NodeSettings.class)
        ColumnSelection m_column;
    }

    /**
     * Compute choices for the compare mode based on the selected column (and it's type).
     */
    static class TypeBasedCompareModeChoices implements ChoicesUpdateHandler<ColumnSelectionDependency> {

        private static final IdAndText AS_STRING =
            new IdAndText(CompareMode.AS_STRING.name(), StringCell.TYPE.getName());

        @Override
        public IdAndText[] update(final ColumnSelectionDependency dep, final DefaultNodeSettingsContext context)
            throws WidgetHandlerException {
            if (dep.m_column == null) {
                return new IdAndText[0];
            }
            final var columnName = dep.m_column.getSelected();
            if (SpecialColumns.ROWID.getId().equals(columnName)) {
                return new IdAndText[]{AS_STRING};
            } else if (SpecialColumns.ROW_NUMBERS.getId().equals(columnName)) {
                return new IdAndText[0];
            }
            final var columnType = context.getDataTableSpec(0).orElseThrow().getColumnSpec(columnName).getType();
            final var modes = new ArrayDeque<>();
            modes.add(AS_STRING);

            if (StringCell.TYPE.equals(columnType)) {
                return modes.toArray(IdAndText[]::new);
            }

            if (BooleanCell.TYPE.equals(columnType)) {
                modes.addFirst(new IdAndText(CompareMode.BOOL.name(), BooleanCell.TYPE.getName()));
                return modes.toArray(IdAndText[]::new);
            }

            if (IntCell.TYPE.equals(columnType) || LongCell.TYPE.equals(columnType)) {
                modes.addFirst(new IdAndText(CompareMode.DECIMAL.name(), DoubleCell.TYPE.getName()));
                modes.addFirst(new IdAndText(CompareMode.INTEGRAL.name(), columnType.getName()));
                return modes.toArray(IdAndText[]::new);
            }

            if (DoubleCell.TYPE.equals(columnType)) {
                modes.addFirst(new IdAndText(CompareMode.DECIMAL.name(), columnType.getName()));
                return modes.toArray(IdAndText[]::new);
            }

            if (columnType.isCompatible(DoubleValue.class)) {
                modes.addFirst(new IdAndText(CompareMode.DECIMAL.name(), DoubleCell.TYPE.getName()));
            }

            if (columnType.isCompatible(IntValue.class) || columnType.isCompatible(LongValue.class)) {
                modes.addFirst(new IdAndText(CompareMode.INTEGRAL.name(), LongCell.TYPE.getName()));
            }

            final var registry = JavaToDataCellConverterRegistry.getInstance();
            final var hasConverter = !registry.getConverterFactories(String.class, columnType).isEmpty();
            if (hasConverter) {
                modes.addFirst(new IdAndText(CompareMode.TYPE_MAPPING.name(), columnType.getName()));
            }
            return modes.toArray(IdAndText[]::new);
        }
    }

    static final class ColumnSelectionAndCompareModeDependency {
        @DeclaringDefaultNodeSettings(RowFilter3NodeSettings.class)
        ColumnSelection m_column;

        @DeclaringDefaultNodeSettings(RowFilter3NodeSettings.class)
        CompareMode m_compareOn;
    }

    /**
     * Compute choices for the filter operator based on the selected column and compare mode
     */
    static class TypeBasedOperatorChoices implements ChoicesUpdateHandler<ColumnSelectionAndCompareModeDependency> {

        private static final IdAndText[] EMPTY = new IdAndText[0];

        @Override
        public IdAndText[] update(final ColumnSelectionAndCompareModeDependency settings,
            final DefaultNodeSettingsContext context) throws WidgetHandlerException {
            if (settings.m_column == null) {
                return EMPTY;
            }
            final var column = settings.m_column.getSelected();
            final var specialColumn =
                Arrays.stream(SpecialColumns.values()).filter(t -> t.getId().equals(column)).findFirst().orElse(null);
            if (SpecialColumns.NONE == specialColumn) {
                return EMPTY;
            }
            final var dataType = getColumnType(specialColumn,
                () -> context.getDataTableSpec(0).map(dts -> dts.getColumnSpec(column)).map(DataColumnSpec::getType));
            if (dataType.isEmpty()) {
                // we don't know the column, but we know that columns always can contain missing cells
                return new IdAndText[]{
                    new IdAndText(FilterOperator.IS_MISSING.name(), FilterOperator.IS_MISSING.label())};
            }
            // filter on top-level type
            return Arrays.stream(FilterOperator.values()) //
                .filter(op -> op.isEnabledFor(specialColumn, dataType.get(), settings.m_compareOn)) //
                .map(op -> new IdAndText(op.name(), op.label())) //
                .toArray(IdAndText[]::new);
        }

        private static Optional<DataType> getColumnType(final SpecialColumns optionalSpecialColumn,
            final Supplier<Optional<DataType>> columnDataTypeSupplier) {
            if (optionalSpecialColumn == null) {
                return columnDataTypeSupplier.get();
            }
            return Optional.ofNullable(switch (optionalSpecialColumn) {
                case ROWID -> StringCell.TYPE;
                case ROW_NUMBERS -> LongCell.TYPE;
                case NONE -> throw new IllegalArgumentException(
                    "Unsupported special column type \"%s\"".formatted(optionalSpecialColumn.name()));
            });
        }
    }

    sealed interface Capability permits IsOrdNumeric, IsEq, IsMissing, IsTruthy, IsPatternMatchable, IsRowNumber {
        /**
         * Check if the capability is satisfied by the given special column, column data type and compare mode.
         *
         * @param specialColumn {@code null} in case of a normal column, otherwise {@link SpecialColumns#ROWID} or
         *            {@link SpecialColumns#ROW_NUMBERS}.
         * @param dataType non-{@code null} data type
         * @param mode non-{@code null} comparison mode
         * @return whether the capability is satisfied
         */
        boolean satisfiedBy(SpecialColumns specialColumn, DataType dataType, CompareMode mode);
    }

    // order for numeric types
    static final class IsOrdNumeric implements Capability {
        @Override
        public boolean satisfiedBy(final SpecialColumns specialColumn, final DataType dataType,
            final CompareMode mode) {
            // booleans don't get numeric treatment
            return mode != CompareMode.AS_STRING
                && ((dataType.isCompatible(LongValue.class) || dataType.isCompatible(DoubleValue.class))
                    && dataType != BooleanCell.TYPE);
        }
    }

    static final class IsEq implements Capability {
        @Override
        public boolean satisfiedBy(final SpecialColumns specialColumn, final DataType dataType,
            final CompareMode mode) {
            // string-based filtering can always use equality
            return mode == CompareMode.AS_STRING && dataType.isCompatible(StringValue.class)
                // booleans are handled with "is true" and "is false" operators
                || dataType != BooleanCell.TYPE;
        }
    }

    static final class IsTruthy implements Capability {
        @Override
        public boolean satisfiedBy(final SpecialColumns specialColumn, final DataType dataType,
            final CompareMode mode) {
            return dataType == BooleanCell.TYPE;
        }
    }

    static final class IsMissing implements Capability {
        @Override
        public boolean satisfiedBy(final SpecialColumns specialColumn, final DataType dataType,
            final CompareMode mode) {
            // All non-special columns can be checked for missing values
            return specialColumn == null;
        }
    }

    static final class IsPatternMatchable implements Capability {
        @Override
        public boolean satisfiedBy(final SpecialColumns specialColumn, final DataType dataType,
            final CompareMode mode) {
            return mode == CompareMode.AS_STRING && dataType.isCompatible(StringValue.class);
        }
    }

    static final class IsRowNumber implements Capability {
        @Override
        public boolean satisfiedBy(final SpecialColumns specialColumn, final DataType dataType,
            final CompareMode mode) {
            return specialColumn == SpecialColumns.ROW_NUMBERS;
        }
    }

}
