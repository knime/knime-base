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

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.knime.core.data.BooleanValue;
import org.knime.core.data.DataValue;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.LongValue;
import org.knime.core.data.StringValue;
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
        LT("<", Set.of(new IsOrd()), Arity.UNARY), //
        @Label(">") // RowIndex/Number, Long, Double
        GT(">", Set.of(new IsOrd()), Arity.UNARY), //
        @Label("≤") // RowIndex/Number, Long, Double
        LTE("≤", Set.of(new IsOrd()), Arity.UNARY), //
        @Label("≥") // RowIndex/Number, Long, Double
        GTE("≥", Set.of(new IsOrd()), Arity.UNARY), //
        @Label("is between") // RowIndex/Number, Long, Double
        BETWEEN("is between", Set.of(new IsOrd()), Arity.BINARY), //
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

    boolean isEnabledFor(final SpecialColumns specialColumn, final CompareMode mode) {
        return m_filters.stream().anyMatch(f -> f.satisfiedBy(specialColumn, mode));
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
     * Enumeration of the different compare modes, mostly driven by UI elements we can provide in the dialog.
     */
    enum CompareMode {
            BOOLEAN_VALUE(BooleanValue.class, "Boolean value"), //
            LONG_VALUE(LongValue.class, "Integer value"), //
            DOUBLE_VALUE(DoubleValue.class, "Floating point value"), //
            STRING_VALUE(StringValue.class, "String representation");

        final Class<? extends DataValue> m_valueClass;

        final String m_label;

        CompareMode(final Class<? extends DataValue> clazz, final String label) {
            m_valueClass = clazz;
            m_label = label;
        }

        /**
         * Choose the favourite compare mode for a list of implemented datavalue classes.
         *
         * @param classes
         * @return a suitable compare mode, if found
         */
        static Optional<CompareMode> favorite(final List<Class<? extends DataValue>> classes) {
            for (var c : classes) {
                final var opt = Arrays.stream(CompareMode.values()).filter(m -> m.m_valueClass.equals(c)).findFirst();
                if (opt.isPresent()) {
                    return opt;
                }
            }
            return Optional.empty();
        }
    }

    static final class ColumnSelectionDependency {
        @DeclaringDefaultNodeSettings(RowFilter3NodeSettings.class)
        ColumnSelection m_column;
    }

    /**
     * Compute choices for the compare mode based on the selected column (and it's type).
     */
    static class TypeBasedCompareModeChoices implements ChoicesUpdateHandler<ColumnSelectionDependency> {

        @Override
        public IdAndText[] update(final ColumnSelectionDependency dep, final DefaultNodeSettingsContext context)
            throws WidgetHandlerException {
            if (dep.m_column == null) {
                return new IdAndText[0];
            }

            final var types = dep.m_column.m_compatibleTypes;
            if (types == null || types.length == 0) {
                return new IdAndText[0];
            }

            return Arrays.stream(CompareMode.values())
                .filter(v -> List.of(types).contains(ColumnSelection.getTypeClassIdentifier(v.m_valueClass)))
                .map(v -> new IdAndText(v.name(), v.m_label)).toArray(IdAndText[]::new);
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

        @Override
        public IdAndText[] update(final ColumnSelectionAndCompareModeDependency settings,
            final DefaultNodeSettingsContext context) throws WidgetHandlerException {
            if (settings.m_column == null) {
                return new IdAndText[0];
            }

            final var specialColumn = Arrays.stream(SpecialColumns.values())
                .filter(t -> t.getId().equals(settings.m_column.getSelected())).findFirst();

            // filter on top-level type
            return Arrays.stream(FilterOperator.values()) //
                .filter(op -> op.isEnabledFor(specialColumn.orElse(null), settings.m_compareOn)) //
                .map(op -> new IdAndText(op.name(), op.label())) //
                .toArray(IdAndText[]::new);

        }
    }

    sealed interface Capability permits IsOrd, IsEq, IsMissing, IsTruthy, IsPatternMatchable, IsRowNumber {
        /**
         * Check if the capability is satisfied by the given special column and compare mode.
         *
         * @param specialColumn can be null
         * @param mode can be null
         * @return whether the capability is satisfied
         */
        boolean satisfiedBy(final SpecialColumns specialColumn, final CompareMode mode);
    }

    static final class IsOrd implements Capability {
        @Override
        public boolean satisfiedBy(final SpecialColumns specialColumn, final CompareMode mode) {
            return mode != null && (mode.m_valueClass == LongValue.class || mode.m_valueClass == DoubleValue.class);
        }
    }

    static final class IsEq implements Capability {
        @Override
        public boolean satisfiedBy(final SpecialColumns specialColumn, final CompareMode mode) {
            // booleans are handled with "is true" and "is false" operators
            return mode != null && mode != CompareMode.BOOLEAN_VALUE;
        }
    }

    static final class IsTruthy implements Capability {
        @Override
        public boolean satisfiedBy(final SpecialColumns specialColumn, final CompareMode mode) {
            return mode == CompareMode.BOOLEAN_VALUE;
        }
    }

    static final class IsMissing implements Capability {
        @Override
        public boolean satisfiedBy(final SpecialColumns specialColumn, final CompareMode mode) {
            // All non-special columns can be checked for missing values
            return specialColumn == null; // mode can also be null in this case
        }
    }

    static final class IsPatternMatchable implements Capability {
        @Override
        public boolean satisfiedBy(final SpecialColumns specialColumn, final CompareMode mode) {
            return mode == CompareMode.STRING_VALUE;
        }
    }

    static final class IsRowNumber implements Capability {
        @Override
        public boolean satisfiedBy(final SpecialColumns specialColumn, final CompareMode mode) {
            return specialColumn == SpecialColumns.ROW_NUMBERS;
        }
    }

}
