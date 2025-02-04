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

import java.util.Optional;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.StringValue;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings;
import org.knime.core.webui.node.dialog.defaultdialog.setting.columnselection.ColumnSelection;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ArrayWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ChoicesWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Label;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ValueSwitchWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Widget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.choices.ColumnChoicesProviderUtil.AllColumnChoicesProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.choices.SpecialColumns;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.Effect;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.Effect.EffectType;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.Predicate;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.PredicateProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.Reference;
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
            @Label(value = "Ascending", description = """
                    The smallest or earliest in the order will appear at the top of the list. E.g., for numbers \
                    the sort is smallest to largest, for dates the sort will be oldest dates to most recent.""")
            ASCENDING, @Label(value = "Descending", description = """
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
            @Label(value = "Natural", description = """
                    Sorts strings by treating the numeric parts of a string as one character. For example, \
                    results in sort order “'Row1', 'Row2', 'Row10'”.""")
            NATURAL, @Label(value = "Lexicographic", description = """
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
    public static final class SortingCriterionSettings implements DefaultNodeSettings {

        /**
         * Constructor to set each field.
         *
         * @param column
         * @param sortingOrder
         * @param stringComparison
         */
        public SortingCriterionSettings(final ColumnSelection column, final SortingOrder sortingOrder,
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
            m_column = colSpec == null ? SpecialColumns.ROWID.toColumnSelection() : new ColumnSelection(colSpec);
        }

        interface ColumnRef extends Reference<ColumnSelection> {
        }

        static final class IsStringColumn implements PredicateProvider {
            @Override
            public Predicate init(final PredicateInitializer i) {
                return i.getColumnSelection(ColumnRef.class).hasColumnType(StringValue.class);
            }
        }

        @Widget(title = "Column", description = """
                Sort rows by the values in this column. If you set multiple sorting criteria, the table is \
                sorted by the first criterion. The following criteria are only considered, if the comparison \
                by all previous criteria results in a tie.""")
        @ChoicesWidget(choices = AllColumnChoicesProvider.class, showRowKeysColumn = true)
        @ValueReference(ColumnRef.class)
        ColumnSelection m_column;

        @Widget(title = "Order", description = "Specifies the sorting order:")
        @ValueSwitchWidget
        SortingOrder m_sortingOrder = SortingOrder.ASCENDING;

        @Widget(title = "String comparison", description = "Specifies which type of sorting to apply to the strings:",
            advanced = true)
        @Effect(predicate = IsStringColumn.class, type = EffectType.SHOW)
        @ValueSwitchWidget
        StringComparison m_stringComparison = StringComparison.NATURAL;

        /**
         * @return the column
         */
        public ColumnSelection getColumn() {
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
}
