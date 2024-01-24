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
 *   24 Jan 2024 (Manuel Hotz, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.base.node.preproc.filter.row;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import org.knime.core.data.BooleanValue;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataValue;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.IntValue;
import org.knime.core.data.StringValue;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.Persistor;
import org.knime.core.webui.node.dialog.defaultdialog.rule.Effect;
import org.knime.core.webui.node.dialog.defaultdialog.rule.Effect.EffectType;
import org.knime.core.webui.node.dialog.defaultdialog.rule.Signal;
import org.knime.core.webui.node.dialog.defaultdialog.setting.columnselection.ColumnSelection;
import org.knime.core.webui.node.dialog.defaultdialog.setting.columnselection.IsColumnOfTypeCondition;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ChoicesWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ColumnChoicesProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Label;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ValueSwitchWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Widget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.choices.ChoicesUpdateHandler;
import org.knime.core.webui.node.dialog.defaultdialog.widget.choices.IdAndText;
import org.knime.core.webui.node.dialog.defaultdialog.widget.handler.DeclaringDefaultNodeSettings;
import org.knime.core.webui.node.dialog.defaultdialog.widget.handler.WidgetHandlerException;


/**
 * Settings for the {@link RowFilterNodeModel}.
 *
 * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
 */
@SuppressWarnings("restriction") // webui
@Persistor(value = LegacyRowFilterPersistor.class)
final class RowFilterNodeSettings implements DefaultNodeSettings {

    @Widget(title = "Filter column")
    @ChoicesWidget(choices = AllColumns.class, showRowKeysColumn = true)
    @Signal(id = StringColumnSelected.class, condition = StringColumnSelected.class)
    //@Signal(id = IntegerColumnSelected.class, condition = IntegerColumnSelected.class)
    // TODO use ColumnSelection once it (better) supports RowID and Row Number
    ColumnSelection m_targetSelection = new ColumnSelection();

    private static final class AllColumns implements ColumnChoicesProvider {

        @Override
        public DataColumnSpec[] columnChoices(final DefaultNodeSettingsContext context) {
            return context.getDataTableSpec(0)//
                .stream()//
                .flatMap(DataTableSpec::stream)//
                .toArray(DataColumnSpec[]::new);
        }
    }

    @Widget(title = "Operator")
    @ChoicesWidget(choicesUpdateHandler = TypeBasedOperatorChoices.class)
    FilterOperator m_operator = FilterOperator.EQ;

    private static final class DataTypeFilterSettings {
        @DeclaringDefaultNodeSettings(RowFilterNodeSettings.class)
        ColumnSelection m_targetSelection;
    }

    private static class TypeBasedOperatorChoices implements ChoicesUpdateHandler<DataTypeFilterSettings> {

        @Override
        public IdAndText[] update(final DataTypeFilterSettings settings, final DefaultNodeSettingsContext context)
            throws WidgetHandlerException {
            final var filterColumn = settings.m_targetSelection;
            if (filterColumn == null) {
                return new IdAndText[0];
            }

            final var types = filterColumn.m_compatibleTypes;
            if (types == null || types.length == 0) {
                return new IdAndText[0];
            }

            final var fullyQualifiedTypeNames = Arrays.stream(filterColumn.m_compatibleTypes)
                    .collect(Collectors.toSet());
            return Arrays.stream(FilterOperator.values()).filter(op -> op.isEnabledFor(fullyQualifiedTypeNames))
                    .map(op -> new IdAndText(op.name(), op.label()))
                    .toArray(IdAndText[]::new);
        }

    }

    sealed interface OperatorRequirement permits IsOrd, IsEq, IsMissing, IsTruthy {
        boolean appliesTo(Set<String> fullyQualifiedValueClassesNames);
    }

    private static final class IsOrd implements OperatorRequirement {
        // Row Index, Row Number, and numeric data types are orderable
        @Override
        public boolean appliesTo(final Set<String> fqValueClassesNames) {
            return fqValueClassesNames.contains(ColumnSelection.getTypeClassIdentifier(DoubleValue.class));
         }
    }

    private static final class IsEq implements OperatorRequirement {
        // RowID, Row Index, Row Number, and all data types are equals comparable
        // TODO old Row Filter would convert everything with toString
        @Override
        public boolean appliesTo(final Set<String> fqValueClassesNames) {
            return true;
        }
    }

    private static final class IsTruthy implements OperatorRequirement {

        @Override
        public boolean appliesTo(final Set<String> fqValueClassesNames) {
            return fqValueClassesNames.contains(ColumnSelection.getTypeClassIdentifier(BooleanValue.class));
        }

    }

    private static final class IsMissing implements OperatorRequirement {
        // Normal columns are is "missing comparable"
        @Override
        public boolean appliesTo(final Set<String> fqValueClassesNames) {
            // TODO marker missing to disable it for RowID/RowIndex/RowNumber
            return true;
        }
    }

    // TODO old Row Filter would convert everything with toString
    enum FilterOperator {
        @Label("=") // RowID, RowIndex/Number, Int, Long, Double, String
        EQ("=", Set.of(new IsEq())),
        @Label("≠") // RowID, RowIndex/Number, Int, Long, Double, String
        NEQ("≠", Set.of()),
        @Label("<") // RowIndex/Number, Int, Long, Double
        LT("<", Set.of(new IsOrd())),
        @Label(">") // RowIndex/Number, Int, Long, Double
        GT(">", Set.of(new IsOrd())),
        @Label("≤") // RowIndex/Number, Int, Long, Double
        LTE("≤", Set.of(new IsOrd())),
        @Label("≥") // RowIndex/Number, Int, Long, Double
        GTE("≥", Set.of(new IsOrd())),
        @Label("is between") // RowIndex/Number, Int, Long, Double
        BETWEEN("is between", Set.of(new IsOrd())),
        @Label("is true") // Boolean
        IS_TRUE("is true", Set.of(new IsTruthy())),
        @Label("is false") // Boolean
        IS_FALSE("is false", Set.of(new IsTruthy())),
        @Label("is missing") // RowID, RowIndex/Number, Int, Long, Double, String
        IS_MISSING("is missing", Set.of(new IsMissing()));

        private final String m_label;
        private final Set<OperatorRequirement> m_filters;

        FilterOperator(final String label, final Set<OperatorRequirement> filters) {
            m_label = label;
            m_filters = filters;
        }

        boolean isEnabledFor(final Set<String> fullyQualifiedTypeNames) {
            return m_filters.stream().anyMatch(f -> f.appliesTo(fullyQualifiedTypeNames));
        }

        String label() {
            return m_label;
        }
    }

    static final class StringColumnSelected extends IsColumnOfTypeCondition {
        @Override
        public Class<? extends DataValue> getDataValueClass() {
            return StringValue.class;
        }
    }

    static final class IntegerColumnSelected extends IsColumnOfTypeCondition {
        @Override
        public Class<? extends DataValue> getDataValueClass() {
            return IntValue.class;
        }
    }

    @Widget(title = "String matching")
    @Effect(signals = RowFilterNodeSettings.StringColumnSelected.class, type = EffectType.SHOW)
    @ValueSwitchWidget
    StringMatchingMode m_stringMatching = StringMatchingMode.LITERAL;

    enum StringMatchingMode {
        LITERAL,
        WILDCARDS,
        REGEX
    }

    @Widget(title = "String value")
    @Effect(signals = RowFilterNodeSettings.StringColumnSelected.class, type = EffectType.SHOW)
    String m_stringValue;

    @Widget(title = "Number value")
    //@Effect(signals = RowFilterNodeSettings.IntegerColumnSelected.class, type = EffectType.SHOW)
    Number m_numberValue;

    @Widget(title = "Include/exclude")
    @ValueSwitchWidget
    OutputMode m_outputMode = OutputMode.INCLUDE;

    enum OutputMode {
        @Label("Include")
        INCLUDE,
        @Label("Exclude")
        EXCLUDE
    }

}
