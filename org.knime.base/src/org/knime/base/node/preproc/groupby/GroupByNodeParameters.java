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
 * ------------------------------------------------------------------------
 */

package org.knime.base.node.preproc.groupby;

import java.util.List;
import java.util.function.Supplier;

import org.knime.base.data.aggregation.GlobalSettings;
import org.knime.base.node.preproc.groupby.GroupByNodeModel.TypeMatch;
import org.knime.core.node.NodeLogger;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Modification;
import org.knime.node.parameters.Advanced;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.array.ArrayWidget;
import org.knime.node.parameters.layout.After;
import org.knime.node.parameters.layout.Layout;
import org.knime.node.parameters.layout.Section;
import org.knime.node.parameters.migration.LoadDefaultsForAbsentFields;
import org.knime.node.parameters.migration.Migration;
import org.knime.node.parameters.persistence.Persist;
import org.knime.node.parameters.persistence.Persistor;
import org.knime.node.parameters.persistence.legacy.LegacyStringFilter;
import org.knime.node.parameters.updates.Effect;
import org.knime.node.parameters.updates.Effect.EffectType;
import org.knime.node.parameters.updates.EffectPredicate;
import org.knime.node.parameters.updates.EffectPredicateProvider;
import org.knime.node.parameters.updates.ParameterReference;
import org.knime.node.parameters.updates.StateProvider;
import org.knime.node.parameters.updates.ValueProvider;
import org.knime.node.parameters.updates.ValueReference;
import org.knime.node.parameters.widget.choices.ChoicesProvider;
import org.knime.node.parameters.widget.choices.EnumChoicesProvider;
import org.knime.node.parameters.widget.choices.ValueSwitchWidget;
import org.knime.node.parameters.widget.choices.util.AllColumnsProvider;
import org.knime.node.parameters.widget.number.NumberInputWidget;
import org.knime.node.parameters.widget.text.TextInputWidget;

/**
 * Node parameters for GroupBy.
 *
 * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
 */
@LoadDefaultsForAbsentFields
@SuppressWarnings("restriction") // internal API use
class GroupByNodeParameters implements NodeParameters {

    static final NodeLogger LOGGER = NodeLogger.getLogger(GroupByNodeParameters.class);

    private interface Sections {
        @Section(title = "Group", description = "...")
        interface Group {
        }

        @Section(title = "Aggregation", description = "...")
        @After(Group.class)
        interface Aggregation {
        }

        @Section(title = "Type- and pattern-based aggregation", description = "...", sideDrawer = true,
                sideDrawerSetText = "Type- and pattern-based aggregations")
        @After(Aggregation.class)
        @Advanced // TODO if no manual aggregations, the dialog looks confusing, especially if in a side drawer
        interface TypeAndPatternAggregations {
        }

        @Section(title = "Output", description = "...")
        @After(TypeAndPatternAggregations.class)
        interface Output {
        }

        @Section(title = "Performance", description = "...")
        @After(Output.class)
        @Advanced
        interface Performance {
        }
    }

    @Layout(Sections.Group.class)
    @Persist(configKey = GroupByNodeModel.CFG_GROUP_BY_COLUMNS)
    @Modification(GroupByColumnsModification.class)
    LegacyStringFilter m_groupByColumns = new LegacyStringFilter(new String[0], new String[0]);

    static final class GroupByColumnsModification extends LegacyStringFilter.LegacyStringFilterModification {
        GroupByColumnsModification() {
            super(false, "Group settings", "DESCRIPTION", "Available column(s)", "Group column(s)",
                AllColumnsProvider.class);
        }
    }

    @Layout(Sections.Aggregation.class)
    @Widget(title = "Manual", description = "...")
    @ArrayWidget(addButtonText = "Add manual")
    @Persistor(LegacyColumnAggregatorsPersistor.class)
    @Migration(LegacyColumnAggregatorsMigration.class)
    ColumnAggregatorElement[] m_columnAggregators = new ColumnAggregatorElement[0];

    @Layout(Sections.TypeAndPatternAggregations.class)
    @Widget(title = "Pattern", description = "...")
    @ArrayWidget(addButtonText = "Add pattern")
    @Persistor(LegacyPatternAggregatorsPersistor.class)
    PatternAggregatorElement[] m_patternAggregators = new PatternAggregatorElement[0];

    @Layout(Sections.TypeAndPatternAggregations.class)
    @Widget(title = "Type", description = "...")
    @ArrayWidget(addButtonText = "Add type")
    @Persistor(LegacyDataTypeAggregatorsPersistor.class)
    @ValueReference(TypeAggregationsRef.class)
    DataTypeAggregatorElement[] m_dataTypeAggregators = new DataTypeAggregatorElement[0];

    static final class TypeAggregationsRef implements ParameterReference<DataTypeAggregatorElement[]> {
    }

    @Layout(Sections.TypeAndPatternAggregations.class)
    @Effect(predicate = HasTypeAggregations.class, type = EffectType.SHOW)
    @Widget(title = "Type matching", description = "...", advanced = true)
    @ChoicesProvider(TypeMatchChoicesProvider.class)
    @ValueSwitchWidget
    @Persistor(LegacyTypeMatchPersistor.class)
    TypeMatch m_typeMatch = TypeMatch.STRICT;

    static final class TypeMatchChoicesProvider implements EnumChoicesProvider<TypeMatch> {
        @Override
        public List<TypeMatch> choices(final NodeParametersInput context) {
            return List.of(TypeMatch.values());
        }
    }

    static final class HasTypeAggregations implements EffectPredicateProvider {
        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            // neither TrueCondition, nor ConstantPredicate is exported
            return i.getArray(TypeAggregationsRef.class).containsElementSatisfying(el -> el.always());
        }
    }

    @Layout(Sections.Output.class)
    @Widget(title = "Column naming", description = "...")
    @ChoicesProvider(ColumnNamePolicyChoicesProvider.class)
    @Persistor(LegacyColumnNamePolicyPersistor.class)
    ColumnNamePolicy m_columnNamePolicy = ColumnNamePolicy.getDefault();

    static final class ColumnNamePolicyChoicesProvider implements EnumChoicesProvider<ColumnNamePolicy> {
        @Override
        public List<ColumnNamePolicy> choices(final NodeParametersInput context) {
            return List.of(ColumnNamePolicy.values());
        }
    }

    @Layout(Sections.Output.class)
    @Widget(title = "Maximum unique values per group", description = "...")
    @NumberInputWidget()
    @Persist(configKey = GroupByNodeModel.CFG_MAX_UNIQUE_VALUES)
    int m_maxUniqueValues = 100000;

    @Layout(Sections.Output.class)
    @Widget(title = "Value delimiter",
        description = "The value delimiter used by aggregation methods such as concatenate.")
    @TextInputWidget
    @Persist(configKey = GroupByNodeModel.CFG_VALUE_DELIMITER)
    String m_valueDelimiter = GlobalSettings.STANDARD_DELIMITER;

    @Layout(Sections.Performance.class)
    @Widget(title = "Enable hiliting", description = "...")
    @Persist(configKey = GroupByNodeModel.CFG_ENABLE_HILITE)
    boolean m_enableHiliting;

    @Layout(Sections.Performance.class)
    @Widget(title = "Process in memory", description = "...")
    @ValueReference(ProcessInMemoryRef.class)
    @Persist(configKey = GroupByNodeModel.CFG_IN_MEMORY)
    boolean m_processInMemory;

    static final class ProcessInMemoryRef implements ParameterReference<Boolean> {
        // empty class used for EffectPredicateProvider
    }

    @Layout(Sections.Performance.class)
    @Widget(title = "Retain row order", description = "...")
    @Effect(predicate = ProcessInMemoryEffect.class, type = Effect.EffectType.DISABLE)
    @ValueProvider(ProcessInMemoryEffect.class)
    @ValueReference(RetainOrderRef.class)
    @Persist(configKey = GroupByNodeModel.CFG_RETAIN_ORDER)
    boolean m_retainOrder;

    static final class RetainOrderRef implements ParameterReference<Boolean> {
    }

    static final class ProcessInMemoryEffect implements EffectPredicateProvider, StateProvider<Boolean> {

        private Supplier<Boolean> m_processInMemoryChange;

        private Supplier<Boolean> m_currentRetain;

        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getBoolean(ProcessInMemoryRef.class).isTrue();
        }

        @Override
        public void init(final StateProviderInitializer init) {
            m_processInMemoryChange = init.computeFromValueSupplier(ProcessInMemoryRef.class);
            m_currentRetain = init.getValueSupplier(RetainOrderRef.class);
        }

        @Override
        public Boolean computeState(final NodeParametersInput input) {
            // in-memory implies retain order
            // otherwise, we keep its current value
            return m_processInMemoryChange.get() || m_currentRetain.get();
        }
    }
}