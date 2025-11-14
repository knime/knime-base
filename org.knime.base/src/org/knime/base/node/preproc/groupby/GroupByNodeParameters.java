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

import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.knime.base.node.preproc.groupby.common.ColumnAggregatorElement;
import org.knime.base.node.preproc.groupby.common.ColumnNamePolicyParameters;
import org.knime.base.node.preproc.groupby.common.DataTypeAggregationParameters;
import org.knime.base.node.preproc.groupby.common.GlobalAggregationMethodParameters;
import org.knime.base.node.preproc.groupby.common.GroupByPerformanceParameters;
import org.knime.base.node.preproc.groupby.common.LegacyColumnAggregatorsMigration;
import org.knime.base.node.preproc.groupby.common.LegacyColumnAggregatorsPersistor;
import org.knime.base.node.preproc.groupby.common.LegacyPatternAggregatorsArrayPersistor;
import org.knime.base.node.preproc.groupby.common.PatternAggregatorElement;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.webui.node.dialog.defaultdialog.internal.persistence.PersistArray;
import org.knime.core.webui.node.dialog.defaultdialog.internal.widget.PersistWithin.PersistEmbedded;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Modification;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.array.ArrayWidget;
import org.knime.node.parameters.layout.Layout;
import org.knime.node.parameters.migration.LoadDefaultsForAbsentFields;
import org.knime.node.parameters.migration.Migration;
import org.knime.node.parameters.persistence.Persist;
import org.knime.node.parameters.persistence.Persistor;
import org.knime.node.parameters.persistence.legacy.LegacyStringFilter;
import org.knime.node.parameters.persistence.legacy.LegacyStringFilter.ColumnBasedExclListProvider;
import org.knime.node.parameters.updates.ParameterReference;
import org.knime.node.parameters.updates.ValueReference;
import org.knime.node.parameters.widget.choices.ChoicesStateProvider;
import org.knime.node.parameters.widget.choices.ColumnChoicesProvider;
import org.knime.node.parameters.widget.choices.TypedStringChoice;
import org.knime.node.parameters.widget.choices.util.AllColumnsProvider;

/**
 * Node parameters for GroupBy.
 *
 * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
 */
@LoadDefaultsForAbsentFields
@SuppressWarnings("restriction") // internal API use
final class GroupByNodeParameters implements NodeParameters {

    @Persist(configKey = GroupByNodeModel.CFG_GROUP_BY_COLUMNS)
    @Modification(GroupByColumnsModification.class)
    @ValueReference(GroupByColumnsRef.class)
    LegacyStringFilter m_groupByColumns = new LegacyStringFilter(new String[0], new String[0]);

    interface GroupByColumnsRef extends ParameterReference<LegacyStringFilter> {
    }

    static final class ExclListProvider extends ColumnBasedExclListProvider {

        @Override
        public Class<? extends ChoicesStateProvider<TypedStringChoice>> getChoicesProviderClass() {
            return AllColumnsProvider.class;
        }

    }

    static final class GroupByColumnsModification extends LegacyStringFilter.LegacyStringFilterModification {
        GroupByColumnsModification() {
            super(false, "Group columns", """
                    Select one or more column(s) according to which the group(s)
                    is/are created.
                    """, "Group columns", "Available columns", AllColumnsProvider.class, ExclListProvider.class);
        }
    }

    static final class NonGroupColumnsProvider implements ColumnChoicesProvider {

        private Supplier<LegacyStringFilter> m_groupByColumnsSupplier;

        @Override
        public void init(final StateProviderInitializer initializer) {
            ColumnChoicesProvider.super.init(initializer);
            m_groupByColumnsSupplier = initializer.computeFromValueSupplier(GroupByColumnsRef.class);
        }

        @Override
        public List<DataColumnSpec> columnChoices(final NodeParametersInput context) {
            final var tableSpec = context.getInTableSpec(0);
            if (tableSpec.isEmpty()) {
                return List.of();
            }
            final var inclSet =
                Arrays.stream(m_groupByColumnsSupplier.get().m_twinList.m_inclList).collect(Collectors.toSet());
            return tableSpec.get().stream().filter(colSpec -> !inclSet.contains(colSpec.getName())).toList();
        }
    }

    @Layout(Sections.Aggregation.class)
    @Widget(title = "Manual", description = """
            Select one or more column(s) for aggregation from the available
            columns list. Change the aggregation method in the Aggregation
            column of the table. You can add the same column multiple
            times.
            """)
    @Modification(ColumnAggregatorElementModifier.class)
    @ArrayWidget(addButtonText = "Add manual",
        // TODO disable "add" button based on input (e.g. no table connected)
        elementDefaultValueProvider = DefaultColumnAggregatorElementProvider.class)
    @Persistor(LegacyColumnAggregatorsPersistor.class) // No array persistor...
    @Migration(LegacyColumnAggregatorsMigration.class) // ...because then we could not deprecate keys here
    ColumnAggregatorElement[] m_columnAggregators = new ColumnAggregatorElement[0];

    static final class ColumnAggregatorElementModifier extends ColumnAggregatorElement.ColumnAggregatorElementModifier {

        ColumnAggregatorElementModifier() {
            super(NonGroupColumnsProvider.class);
        }

    }

    static final class DefaultColumnAggregatorElementProvider
        extends ColumnAggregatorElement.DefaultColumnAggregatorElementProvider {

        DefaultColumnAggregatorElementProvider() {
            super(NonGroupColumnsProvider.class);
        }
    }

    @Layout(Sections.PatternAggregation.class)
    @Widget(title = "Pattern Based Aggregation", description = """
            <p>The search pattern can either be a string with wildcards or a
            <a href="https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/regex/Pattern.html#sum">
            regular expression</a>.</p>

            Supported wildcards are <code>*</code> (matches any number of characters) and <code>?</code>
            (matches one character) e.g. <code>KNI*</code>
            would match all strings that start with "KNI" such as "KNIME" whereas <code>KNI?</code> would match only
            strings that start with "KNI" followed by a fourth character.
            """)
    @ArrayWidget(addButtonText = "Add pattern")
    @PersistArray(LegacyPatternAggregatorsArrayPersistor.class)
    PatternAggregatorElement[] m_patternAggregators = new PatternAggregatorElement[0];

    @PersistEmbedded
    DataTypeAggregationParameters m_dataTypeAggregationParameters = new DataTypeAggregationParameters();

    @PersistEmbedded
    ColumnNamePolicyParameters m_columnNamePolicyParameters = new ColumnNamePolicyParameters();

    @PersistEmbedded
    GlobalAggregationMethodParameters m_globalAggregationMethodParameters = new GlobalAggregationMethodParameters();

    @PersistEmbedded
    GroupByPerformanceParameters m_performanceParameters = new GroupByPerformanceParameters();

}
