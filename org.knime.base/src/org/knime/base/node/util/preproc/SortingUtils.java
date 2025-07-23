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
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.StringValue;
import org.knime.core.data.sort.RowComparator;
import org.knime.core.data.sort.RowComparator.ColumnComparatorBuilder;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.booleanhelpers.DoNotPersistBoolean;
import org.knime.core.webui.node.dialog.defaultdialog.setting.columnselection.ColumnSelectionToStringWithRowIDChoiceMigration;
import org.knime.core.webui.node.dialog.defaultdialog.setting.singleselection.RowIDChoice;
import org.knime.core.webui.node.dialog.defaultdialog.setting.singleselection.StringOrEnum;
import org.knime.core.webui.node.dialog.defaultdialog.util.updates.StateComputationFailureException;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Modification;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.array.ArrayWidget;
import org.knime.node.parameters.migration.Migration;
import org.knime.node.parameters.persistence.Persist;
import org.knime.node.parameters.persistence.Persistor;
import org.knime.node.parameters.updates.Effect;
import org.knime.node.parameters.updates.ParameterReference;
import org.knime.node.parameters.updates.StateProvider;
import org.knime.node.parameters.updates.ValueProvider;
import org.knime.node.parameters.updates.ValueReference;
import org.knime.node.parameters.updates.Effect.EffectType;
import org.knime.node.parameters.updates.util.BooleanReference;
import org.knime.node.parameters.widget.choices.ChoicesProvider;
import org.knime.node.parameters.widget.choices.ColumnChoicesProvider;
import org.knime.node.parameters.widget.choices.Label;
import org.knime.node.parameters.widget.choices.ValueSwitchWidget;
import org.knime.node.parameters.widget.choices.util.AllColumnsProvider;

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
     * Can be used to provide a new (not already used) value.
     *
     * @author Martin Sillye, TNG Technology Consulting GmbH
     * @param <T1> Type of the SortingCriterionSettings
     */
    public abstract static class DefaultValueProvider<T1 extends SortingCriterionSettings>
        implements StateProvider<T1> {

        private Class<? extends ParameterReference<T1[]>> m_arrayRef;

        private Supplier<T1[]> m_array;

        /**
         * @param arrayRef Reference class to the SortingCriterion array.
         */
        protected DefaultValueProvider(final Class<? extends ParameterReference<T1[]>> arrayRef) {
            this.m_arrayRef = arrayRef;
        }

        @Override
        public void init(final StateProviderInitializer initializer) {
            this.m_array = initializer.computeFromValueSupplier(m_arrayRef);
            initializer.computeBeforeOpenDialog();
        }

        /**
         * @param context
         * @return optional value of the first available column spec.
         */
        protected Optional<DataColumnSpec> getFirstAvailableDataColumnSpec(final NodeParametersInput context) {
            final var spec = context.getInTableSpec(0);
            if (spec.isEmpty()) {
                return Optional.empty();
            }
            final var columns = getAlreadySelectedColumns();
            return spec.get().stream().filter(colSpec -> !columns.contains(colSpec.getName())).findFirst();
        }

        /**
         * @return a set of already selected columns
         */
        protected Set<String> getAlreadySelectedColumns() {
            return Arrays.stream(m_array.get()).map(T1::getColumn).filter(column -> column.getEnumChoice().isEmpty())
                .map(StringOrEnum::getStringChoice).collect(Collectors.toSet());
        }

    }

    /**
     * Default value provider for the {@link SortingCriterionSettings} class.
     *
     * @author Paul Bärnreuther
     */
    public abstract static class SortingCriterionDefaultValueProvider
        extends DefaultValueProvider<SortingCriterionSettings> {
        /**
         * @param arrayRef Reference class to the SortingCriterion array.
         */
        protected SortingCriterionDefaultValueProvider(
            final Class<? extends ParameterReference<SortingCriterionSettings[]>> arrayRef) {
            super(arrayRef);
        }

        @Override
        public SortingCriterionSettings computeState(final NodeParametersInput context)
            throws StateComputationFailureException {
            final var firstAvailableCol = getFirstAvailableDataColumnSpec(context);
            if (firstAvailableCol.isEmpty()) {
                return new SortingCriterionSettings();
            }
            return new SortingCriterionSettings(firstAvailableCol.get());
        }

    }

    /**
     * Can be used to define a {@link SortingOrder} and {@link StringComparison} for a column. Mostly used in
     * {@link ArrayWidget}.
     *
     * @author Martin Sillye, TNG Technology Consulting GmbH
     */
    public static class SortingCriterionSettings implements NodeParameters {

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
        public SortingCriterionSettings(final NodeParametersInput context) {
            this(context.getInTableSpec(0).flatMap(Optional::ofNullable)
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

        interface ColumnRef extends ParameterReference<StringOrEnum<RowIDChoice>>, Modification.Reference {
        }

        interface SortingOrderRef extends ParameterReference<SortingOrder>, Modification.Reference {
        }

        interface StringComparisonRef extends ParameterReference<StringComparison>, Modification.Reference {
        }

        /**
         * @return the column selection reference.
         */
        public static Class<? extends ParameterReference<StringOrEnum<RowIDChoice>>> getColumnRef() {
            return ColumnRef.class;
        }

        /**
         * Modifier base class to use for Sorting
         *
         * @author Martin Sillye, TNG Technology Consulting GmbH
         */
        public abstract static class SortingModification implements Modification.Modifier {

            /**
             * Get a modifier for the column field.
             *
             * @param group of widget
             * @return the {@link Modification.WidgetModifier} of the column field
             */
            protected Modification.WidgetModifier getColumnModifier(final Modification.WidgetGroupModifier group) {
                return group.find(ColumnRef.class);
            }

            /**
             * Get a modifier for the sorting order field.
             *
             * @param group of widget
             * @return the {@link Modification.WidgetModifier} of the sorting order field
             */
            protected Modification.WidgetModifier getSortingOrderModifier(final Modification.WidgetGroupModifier group) {
                return group.find(SortingOrderRef.class);
            }

            /**
             * Get a modifier for the string comparison field.
             *
             * @param group of widget
             * @return the {@link Modification.WidgetModifier} of the string comparison field
             */
            protected Modification.WidgetModifier getStringComparisonModifier(final Modification.WidgetGroupModifier group) {
                return group.find(StringComparisonRef.class);
            }

        }

        /**
         * Use this modification to prevent that the same column is used twice when these settings are used in an array.
         *
         */
        public abstract static class CriterionColumnChoicesModification extends SortingModification {

            private Class<? extends CriterionColumnChoicesProvider> m_provider;

            /**
             * @param provider the provider class to use for the column choices
             */
            protected CriterionColumnChoicesModification(
                final Class<? extends CriterionColumnChoicesProvider> provider) {
                m_provider = provider;
            }

            @Override
            public void modify(final Modification.WidgetGroupModifier group) {
                this.getColumnModifier(group).modifyAnnotation(ChoicesProvider.class).withValue(m_provider).modify();
            }

        }

        /**
         *
         * Use this provider to prevent that the same column is used twice when these settings are used in an array.
         *
         * @param <T> the used subtype of the {@link SortingCriterionSettings}
         * @see CriterionColumnChoicesProvider
         *
         */
        public abstract static class CriterionColumnChoicesProvider<T extends SortingCriterionSettings>
            implements ColumnChoicesProvider {

            private Supplier<T[]> m_criterions;

            private Supplier<StringOrEnum<RowIDChoice>> m_columSelection;

            private Class<? extends ParameterReference<T[]>> m_arrayRef;

            /**
             * @param arrayRef Reference class to the SortingCriterion array.
             */
            protected CriterionColumnChoicesProvider(final Class<? extends ParameterReference<T[]>> arrayRef) {
                this.m_arrayRef = arrayRef;
            }

            @Override
            public void init(final StateProviderInitializer initializer) {
                this.m_criterions = initializer.computeFromValueSupplier(m_arrayRef);
                this.m_columSelection = initializer.getValueSupplier(SortingCriterionSettings.getColumnRef());
                initializer.computeBeforeOpenDialog();
            }

            @Override
            public List<DataColumnSpec> columnChoices(final NodeParametersInput context) {
                final var spec = context.getInTableSpec(0);
                if (spec.isEmpty()) {
                    return Collections.emptyList();
                }
                final var columns = Arrays.stream(m_criterions.get()).map(SortingCriterionSettings::getColumn)
                    .filter(column -> column.getEnumChoice().isEmpty()).map(StringOrEnum::getStringChoice)
                    .filter(stringChoice -> !stringChoice.equals(this.m_columSelection.get().getStringChoice()))
                    .collect(Collectors.toSet());
                return spec.get().stream().filter(colSpec -> !columns.contains(colSpec.getName())).toList();
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
            public Boolean computeState(final NodeParametersInput context) {
                final var tableSpecOptional = context.getInTableSpec(0);
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
