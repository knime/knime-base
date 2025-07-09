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
 *   Jul 9, 2025 (marcbux): created
 */
package org.knime.base.node.preproc.filter.constvalcol;

import java.util.Optional;

import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings;
import org.knime.core.webui.node.dialog.defaultdialog.setting.filter.column.ColumnFilter;
import org.knime.core.webui.node.dialog.defaultdialog.util.column.ColumnSelectionUtil;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Label;
import org.knime.core.webui.node.dialog.defaultdialog.widget.NumberInputWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Widget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.choices.column.AllColumnsProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.choices.column.ColumnFilterWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.Effect;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.Effect.EffectType;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.Predicate;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.PredicateProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.Reference;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.ValueReference;
import org.knime.core.webui.node.dialog.defaultdialog.widget.validation.NumberInputWidgetValidation.MinValidation;

@SuppressWarnings("restriction")
class ConstantValueColumnFilterNodeSettings implements DefaultNodeSettings {

    enum FilterMode {
            @Label(value = "all constant value columns", description = """
                    Remove all constant value columns from the columns considered for filtering,
                    independent of the specific value they contain in duplicates.
                    """)
            ALL, @Label(value = "constant value columns that only contain", description = """
                    Remove only those constant value columns from the columns considered for filtering
                    that only contain a certain specific value.
                    """)
            BY_VALUE,
    }

    interface FilterNumericRef extends Reference<FilterMode> {
    }

    static final class IsFilterByValueProvider implements PredicateProvider {
        @Override
        public Predicate init(final PredicateInitializer i) {
            return i.getEnum(FilterNumericRef.class).isOneOf(FilterMode.BY_VALUE);
        }
    }

    @Widget(title = "Remove from the selected columns",
        description = "Choose whether to remove all constant value columns or only those containing a specific value.")
    @ValueReference(FilterNumericRef.class)
    FilterMode m_filterMode = FilterMode.ALL;

    @Widget(title = "numeric values of",
        description = "Remove only those constant value columns that only contain a certain specific numeric value.")
    @Effect(type = EffectType.SHOW, predicate = IsFilterByValueProvider.class)
    Optional<Double> m_filterNumeric = Optional.empty();

    @Widget(title = "String values of",
        description = "Remove only those constant value columns that only contain a certain specific textual value.")
    @Effect(type = EffectType.SHOW, predicate = IsFilterByValueProvider.class)
    Optional<String> m_filterString = Optional.empty();

    @Widget(title = "missing values",
        description = "Remove only those constant value columns that only contain empty cells / missing values.")
    @Effect(type = EffectType.SHOW, predicate = IsFilterByValueProvider.class)
    boolean m_filterMissing;

    @Widget(title = "Considered columns",
        description = "This list contains the column names of the input table that are to be considered for filtering.")
    @ColumnFilterWidget(choicesProvider = AllColumnsProvider.class)
    ColumnFilter m_consideredColumns;

    static final class IsPositiveValidation extends MinValidation {
        @Override
        public double getMin() {
            return 1;
        }
    }

    @Widget(title = "Minimum number of rows", description = """
            The minimum number of rows a table must have to be considered for filtering. If the table size is below the
            specified value, the table will not be filtered / altered.
            """, advanced = true)
    @NumberInputWidget(minValidation = IsPositiveValidation.class)
    long m_minRows = 1;

    ConstantValueColumnFilterNodeSettings() {
    }

    ConstantValueColumnFilterNodeSettings(final DefaultNodeSettingsContext context) {
        final var inputSpec = context.getDataTableSpec(0);
        if (inputSpec.isPresent()) {
            m_consideredColumns = new ColumnFilter(ColumnSelectionUtil.getAllColumnsOfFirstPort(context));
        }
    }
}
