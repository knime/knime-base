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
 *   Feb 3, 2025 (Martin Sillye, TNG Technology Consulting GmbH): created
 */
package org.knime.base.node.util.preproc;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.StringValue;
import org.knime.core.data.sort.RowComparator;
import org.knime.core.data.sort.RowComparator.ColumnComparatorBuilder;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings;
import org.knime.core.webui.node.dialog.defaultdialog.layout.WidgetGroup;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.api.Migration;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.api.Persist;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.api.Persistor;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.booleanhelpers.DoNotPersistBoolean;
import org.knime.core.webui.node.dialog.defaultdialog.setting.columnselection.ColumnSelectionToStringWithRowIDChoiceMigration;
import org.knime.core.webui.node.dialog.defaultdialog.setting.singleselection.RowIDChoice;
import org.knime.core.webui.node.dialog.defaultdialog.setting.singleselection.StringOrEnum;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ArrayWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Label;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ValueSwitchWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Widget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.choices.ChoicesProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.choices.column.AllColumnsProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.BooleanReference;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.Effect;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.Effect.EffectType;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.Reference;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.StateProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.ValueProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.ValueReference;

/**
 * Utility class related to sorting and ranking
 *
 * @author Martin Sillye, TNG Technology Consulting GmbH
 */
@SuppressWarnings("restriction")
public final class SortingUtils {

    private SortingUtils() {
        // Utility class
    }

    /**
     * The order of the sorting
     *
     * @author Martin Sillye, TNG Technology Consulting GmbH
     */
    public enum SortingOrder {
            /**
             * Ascending order
             */
            @Label(value = "Ascending", description = """
                    The smallest or earliest in the order will appear at the top of the list. E.g., for numbers \
                    the sort is smallest to largest, for dates the sort will be oldest dates to most recent.""")
            ASCENDING, //
            /**
             * Descending order
             */
            @Label(value = "Descending", description = """
                    The largest or latest in the order will appear at the top of the list. E.g., for numbers the \
                    sort is largest to smallest, for dates the sort will be most recent dates to oldest.""")
            DESCENDING;
    }

    /**
     * Comparison method for strings
     *
     * @author Martin Sillye, TNG Technology Consulting GmbH
     */
    public enum StringComparison {
            /**
             * Natural comparison
             */
            @Label(value = "Natural", description = """
                    Sorts strings by treating the numeric parts of a string as one character. For example, \
                    results in sort order “'Row1', 'Row2', 'Row10'”.""")
            NATURAL, //
            /**
             * Lexicographic comparison
             */
            @Label(value = "Lexicographic", description = """
                    Sorts strings so that each digit is treated as a separated character. For example, results \
                    in sort order “'Row1', 'Row10', 'Row2'”.""")
            LEXICOGRAPHIC;
    }

    /**
     * Can be used to define a {@link SortingOrder} and {@link StringComparison} for a column. Mostly used in
     * {@link ArrayWidget}.
     *
     * @author Martin Sillye, TNG Technology Consulting GmbH
     */
    public static class SortingCriterionSettings implements DefaultNodeSettings {

        /**
         * Constructor to set each field.
         *
         * @param column
         * @param sortingOrder
         * @param stringComparison
         */
        public SortingCriterionSettings(final StringOrEnum<RowIDChoice> column, final SortingOrder sortingOrder,
            final StringComparison stringComparison) {
            m_column = column;
            m_sortingOrder = sortingOrder;
            m_stringComparison = stringComparison;
        }

        /**
         * Default constructor
         */
        public SortingCriterionSettings() {
            this((DataColumnSpec)null);
        }

        /**
         * Constructor tries to retrieve a {@link DataColumnSpec} from the context and use it as default value for the
         * column.
         *
         * @param context
         */
        public SortingCriterionSettings(final DefaultNodeSettingsContext context) {
            this(context.getDataTableSpec(0).flatMap(Optional::ofNullable)
                .map(spec -> spec.getNumColumns() == 0 ? null : spec.getColumnSpec(0)).flatMap(Optional::ofNullable)
                .orElse(null));
        }

        /**
         * Sets the column field to the specified column on to the RowID.
         *
         * @param colSpec
         */
        public SortingCriterionSettings(final DataColumnSpec colSpec) {
            m_column = colSpec == null ? new StringOrEnum<>(RowIDChoice.ROW_ID) : new StringOrEnum<>(colSpec.getName());
        }

        interface ColumnRef extends Reference<StringOrEnum<RowIDChoice>>, Modification.Reference {
        }

        interface SortingOrderRef extends Reference<SortingOrder>, Modification.Reference {
        }

        interface StringComparisonRef extends Reference<StringComparison>, Modification.Reference {
        }

        /**
         * @return the column selection reference.
         */
        public static Class<? extends Reference<StringOrEnum<RowIDChoice>>> getColumnRef() {
            return ColumnRef.class;
        }

        /**
         * Modifier base class to use for Sorting
         *
         * @author Martin Sillye, TNG Technology Consulting GmbH
         */
        public abstract static class SortingModification implements WidgetGroup.Modifier {

            /**
             * Get a modifier for the column field.
             *
             * @param group of widget
             * @return the {@link WidgetModifier} of the column field
             */
            protected WidgetModifier getColumnModifier(final WidgetGroupModifier group) {
                return group.find(ColumnRef.class);
            }

            /**
             * Get a modifier for the sorting order field.
             *
             * @param group of widget
             * @return the {@link WidgetModifier} of the sorting order field
             */
            protected WidgetModifier getSortingOrderModifier(final WidgetGroupModifier group) {
                return group.find(SortingOrderRef.class);
            }

            /**
             * Get a modifier for the string comparison field.
             *
             * @param group of widget
             * @return the {@link WidgetModifier} of the string comparison field
             */
            protected WidgetModifier getStringComparisonModifier(final WidgetGroupModifier group) {
                return group.find(StringComparisonRef.class);
            }

        }

        @Widget(title = "Column", description = """
                Sort rows by the values in this column. If you set multiple sorting criteria, the table is \
                sorted by the first criterion. The following criteria are only considered, if the comparison \
                by all previous criteria results in a tie.""")
        @ChoicesProvider(AllColumnsProvider.class)
        @ValueReference(ColumnRef.class)
        @Persist(configKey = "columnV2")
        @Migration(ColumnSelectionMigration.class)
        @Modification.WidgetReference(ColumnRef.class)
        StringOrEnum<RowIDChoice> m_column;

        static final class ColumnSelectionMigration extends ColumnSelectionToStringWithRowIDChoiceMigration {
            ColumnSelectionMigration() {
                super("column");
            }
        }

        static final class IsStringColumnProvider implements StateProvider<Boolean> {

            private Supplier<StringOrEnum<RowIDChoice>> m_columnSupplier;

            @Override
            public void init(final StateProviderInitializer i) {
                i.computeBeforeOpenDialog();
                m_columnSupplier = i.computeFromValueSupplier(ColumnRef.class);
            }

            @Override
            public Boolean computeState(final DefaultNodeSettingsContext context) {
                final var tableSpecOptional = context.getDataTableSpec(0);
                if (tableSpecOptional.isEmpty()) {
                    return false;
                }
                final var tableSpec = tableSpecOptional.get();
                final var column = m_columnSupplier.get();
                if (column.getEnumChoice().isPresent()) {
                    return true;
                }
                final var colSpec = tableSpec.getColumnSpec(column.getStringChoice());
                if (colSpec == null) {
                    return false;
                }
                return colSpec.getType().isCompatible(StringValue.class);
            }

        }

        static final class IsStringColumn implements BooleanReference {
        }

        @ValueProvider(IsStringColumnProvider.class)
        @ValueReference(IsStringColumn.class)
        @Persistor(DoNotPersistBoolean.class)
        boolean m_isStringColumn;

        @Widget(title = "Order", description = "Specifies the sorting order:")
        @ValueSwitchWidget
        @Modification.WidgetReference(SortingOrderRef.class)
        SortingOrder m_sortingOrder = SortingOrder.ASCENDING;

        @Widget(title = "String comparison", description = "Specifies which type of sorting to apply to the strings:",
            advanced = true)
        @Effect(predicate = IsStringColumn.class, type = EffectType.SHOW)
        @ValueSwitchWidget
        @Modification.WidgetReference(StringComparisonRef.class)
        StringComparison m_stringComparison = StringComparison.NATURAL;

        /**
         * @return the column
         */
        public StringOrEnum<RowIDChoice> getColumn() {
            return m_column;
        }

        /**
         * @return the sortingOrder
         */
        public SortingOrder getSortingOrder() {
            return m_sortingOrder;
        }

        /**
         * @return the stringComparison
         */
        public StringComparison getStringComparison() {
            return m_stringComparison;
        }

    }

    /**
     * Obtain columns specified in sorting criteria but missing in data table spec.
     *
     * @param sortingCriteria
     * @param spec
     * @return missing columns
     */
    public static List<String> getMissing(final SortingCriterionSettings[] sortingCriteria, final DataTableSpec spec) {
        return Stream.of(sortingCriteria)//
            .map(SortingCriterionSettings::getColumn)//
            .filter(col -> col.getEnumChoice().isEmpty())//
            .map(StringOrEnum::getStringChoice)//
            .filter(name -> spec.findColumnIndex(name) == -1)//
            .toList();
    }

    /**
     * Converts the given sorting criteria into a row comparator for the given data table.
     *
     * @param spec data table to compare rows of
     * @param sortingCriteria to convert
     * @param missingsLast whether to always put missing cells at the end of the table, regardless of sort order
     * @return a row comparator to compare rows of given data table with
     */
    public static RowComparator toRowComparator(final DataTableSpec spec,
        final SortingCriterionSettings[] sortingCriteria, final boolean missingsLast) {
        final var rc = RowComparator.on(spec);
        Arrays.stream(sortingCriteria).forEach(criterion -> {
            final var ascending = criterion.getSortingOrder() == SortingOrder.ASCENDING;
            final var alphaNum = criterion.getStringComparison() == StringComparison.NATURAL;
            resolveColumnName(spec, criterion.getColumn()).ifPresentOrElse(
                col -> rc.thenComparingColumn(col,
                    c -> configureColumnComparatorBuilder(spec, missingsLast, ascending, alphaNum, col, c)),
                () -> rc.thenComparingRowKey(
                    k -> k.withDescendingSortOrder(!ascending).withAlphanumericComparison(alphaNum)));
        });
        return rc.build();
    }

    private static ColumnComparatorBuilder configureColumnComparatorBuilder(final DataTableSpec spec,
        final boolean missingsLast, final boolean ascending, final boolean alphaNum, final int col,
        final ColumnComparatorBuilder c) {
        var compBuilder = c.withDescendingSortOrder(!ascending);
        if (spec.getColumnSpec(col).getType().isCompatible(StringValue.class)) {
            compBuilder.withAlphanumericComparison(alphaNum);
        }
        return compBuilder.withMissingsLast(missingsLast);
    }

    private static OptionalInt resolveColumnName(final DataTableSpec dts, final StringOrEnum<RowIDChoice> column) {
        if (column.getEnumChoice().isPresent()) {
            return OptionalInt.empty();
        }
        final var colName = column.getStringChoice();
        final var idx = dts.findColumnIndex(colName);
        if (idx == -1) {
            throw new IllegalArgumentException(
                "The column identifier \"" + colName + "\" does not refer to a known column.");
        }
        return OptionalInt.of(idx);
    }
}
