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

package org.knime.base.node.preproc.columnaggregator;

import java.util.List;
import java.util.function.Supplier;

import org.knime.base.data.aggregation.AggregationMethods;
import org.knime.base.node.preproc.columnaggregator.AggregationMethodElement.AggregationMethodAvailableForAllSelectedColumns;
import org.knime.base.node.preproc.groupby.Sections;
import org.knime.base.node.preproc.groupby.common.GlobalAggregationMethodParameters;
import org.knime.core.webui.node.dialog.defaultdialog.internal.widget.PersistWithin.PersistEmbedded;
import org.knime.core.webui.node.dialog.defaultdialog.util.updates.StateComputationFailureException;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.array.ArrayWidget;
import org.knime.node.parameters.layout.Layout;
import org.knime.node.parameters.migration.LoadDefaultsForAbsentFields;
import org.knime.node.parameters.persistence.Persist;
import org.knime.node.parameters.persistence.Persistor;
import org.knime.node.parameters.persistence.legacy.LegacyColumnFilterPersistor;
import org.knime.node.parameters.updates.ParameterReference;
import org.knime.node.parameters.updates.StateProvider;
import org.knime.node.parameters.updates.ValueReference;
import org.knime.node.parameters.widget.choices.ChoicesProvider;
import org.knime.node.parameters.widget.choices.StringChoice;
import org.knime.node.parameters.widget.choices.filter.ColumnFilter;
import org.knime.node.parameters.widget.choices.util.AllColumnsProvider;

/**
 * Node parameters for Column Aggregator.
 *
 * @author Paul Baernreuther, KNIME GmbH, Germany
 * @author AI Migration Pipeline v1.2
 */
@LoadDefaultsForAbsentFields
@SuppressWarnings("restriction")
class ColumnAggregatorNodeParameters implements NodeParameters {

    @Widget(title = "Aggregation Columns", description = "Select one or more columns to aggregate.")
    @Persistor(ColumnFilterPersistor.class)
    @ValueReference(AggregationColumnsRef.class)
    @ChoicesProvider(AllColumnsProvider.class)
    ColumnFilter m_aggregationColumns = new ColumnFilter();

    interface AggregationColumnsRef extends ParameterReference<ColumnFilter> {
        // just a marker interface
    }

    static final class ColumnFilterPersistor extends LegacyColumnFilterPersistor {

        ColumnFilterPersistor() {
            super("aggregationColumns");
        }

    }

    @Persistor(LegacyAggregationMethodsPersistor.class)
    @Widget(title = "Aggregations",
        description = "Choose one or more aggregations to be applied to the selected aggregation columns. "
            + "Only aggregation methods that are compatible with those selected columns are available. "
            + "You can add the same method multiple times. However you have to change the name of the result column. ")
    @ArrayWidget(elementDefaultValueProvider = AggregationMethodElementDefaultProvider.class,
        addButtonText = "Add aggregation method")
    @Layout(Sections.Aggregation.class)
    AggregationMethodElement[] m_aggregationMethods = new AggregationMethodElement[0];

    static final class AggregationMethodElementDefaultProvider implements StateProvider<AggregationMethodElement> {

        private Supplier<List<StringChoice>> m_aggregationMethodsChoicesProvider;

        @Override
        public void init(final StateProviderInitializer initializer) {
            m_aggregationMethodsChoicesProvider =
                initializer.computeFromProvidedState(AggregationMethodAvailableForAllSelectedColumns.class);
        }

        @Override
        public AggregationMethodElement computeState(final NodeParametersInput parametersInput)
            throws StateComputationFailureException {
            final var choices = m_aggregationMethodsChoicesProvider.get();
            if (choices.isEmpty()) {
                throw new StateComputationFailureException();
            }
            return new AggregationMethodElement(AggregationMethods.getMethod4Id(choices.get(0).id()));
        }
    }

    @Widget(title = "Remove aggregation columns", description = """
            If selected the columns used to aggregate are filtered from
            the output table.
            """)
    @Layout(Sections.Output.class)
    boolean m_removeAggregationColumns;

    @Widget(title = "Remove retained columns", description = """
            If selected the columns that are not aggregate are filtered
            from the output table.
            """)
    @Layout(Sections.Output.class)
    boolean m_removeRetainedColumns;

    @PersistEmbedded
    GlobalAggregationMethodParameters m_globalAggregationMethodParameters = new GlobalAggregationMethodParameters();

    @Persist(configKey = ColumnAggregatorNodeModel.CFG_VALIDATE_AGGREGATION_METHODS)
    boolean m_validateAggregationColumns = true;

}
