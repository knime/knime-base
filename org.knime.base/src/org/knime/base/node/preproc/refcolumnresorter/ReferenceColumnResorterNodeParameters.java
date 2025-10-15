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

package org.knime.base.node.preproc.refcolumnresorter;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.StringValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.migration.LoadDefaultsForAbsentFields;
import org.knime.node.parameters.persistence.NodeParametersPersistor;
import org.knime.node.parameters.persistence.Persist;
import org.knime.node.parameters.persistence.Persistor;
import org.knime.node.parameters.updates.ParameterReference;
import org.knime.node.parameters.updates.ValueProvider;
import org.knime.node.parameters.updates.ValueReference;
import org.knime.node.parameters.updates.legacy.ColumnNameAutoGuessValueProvider;
import org.knime.node.parameters.widget.choices.ChoicesProvider;
import org.knime.node.parameters.widget.choices.Label;
import org.knime.node.parameters.widget.choices.ValueSwitchWidget;
import org.knime.node.parameters.widget.choices.util.ColumnSelectionUtil;
import org.knime.node.parameters.widget.choices.util.CompatibleColumnsProvider.StringColumnsProvider;

/**
 * Node parameters for Reference Column Resorter.
 *
 * @author Magnus Gohm, KNIME GmbH, Konstanz, Germany
 * @author AI Migration Pipeline v1.2
 */
@LoadDefaultsForAbsentFields
@SuppressWarnings("restriction")
class ReferenceColumnResorterNodeParameters implements NodeParameters {

    ReferenceColumnResorterNodeParameters() {
    }

    ReferenceColumnResorterNodeParameters(final NodeParametersInput context) {
        final var firstCol = context.getInTableSpec(0).stream().flatMap(DataTableSpec::stream).findFirst();
        if (firstCol.isPresent()) {
            m_orderColumn = firstCol.get().getName();
        }
    }

    @Widget(title = "Order column",
            description = "The column in the second input storing the ordered list of column names in the first input.")
    @ChoicesProvider(StringColumnsProvider.class)
    @ValueReference(OrderColumnRef.class)
    @ValueProvider(OrderColumnProvider.class)
    @Persist(configKey = ReferenceColumnResorterNodeModel.CFGKEY_ORDERCOL)
    String m_orderColumn;

    @Widget(title = "Strategy for unspecified columns",
            description = "Strategy for handling columns whose positions are not specified by the second input:")
    @ValueSwitchWidget
    @Persistor(UnspecifiedStrategyPersistor.class)
    UnspecifiedStrategy m_strategy = UnspecifiedStrategy.LAST;

    static final class OrderColumnRef implements ParameterReference<String> {
    }

    static final class OrderColumnProvider extends ColumnNameAutoGuessValueProvider {

        protected OrderColumnProvider() {
            super(OrderColumnRef.class);
        }

        @Override
        protected Optional<DataColumnSpec> autoGuessColumn(final NodeParametersInput parametersInput) {
            return ColumnSelectionUtil.getFirstCompatibleColumnOfFirstPort(parametersInput, StringValue.class);
        }

    }

    static final class UnspecifiedStrategyPersistor implements NodeParametersPersistor<UnspecifiedStrategy> {

        @Override
        public UnspecifiedStrategy load(final NodeSettingsRO settings) throws InvalidSettingsException {
            final String strategy = settings.getString(ReferenceColumnResorterNodeModel.CFGKEY_STRATEGY);
            return switch (strategy) {
                case ReferenceColumnResorterNodeModel.FIRST_STRATEGY -> UnspecifiedStrategy.FIRST;
                case ReferenceColumnResorterNodeModel.LAST_STRATEGY -> UnspecifiedStrategy.LAST;
                case ReferenceColumnResorterNodeModel.DROP_STRATEGY -> UnspecifiedStrategy.DROP;
                default -> throw new InvalidSettingsException(createInvalidSettingsExceptionMessage(strategy));
            };
        }

        @Override
        public void save(final UnspecifiedStrategy obj, final NodeSettingsWO settings) {
            final String strategy = switch (obj) {
                case FIRST -> ReferenceColumnResorterNodeModel.FIRST_STRATEGY;
                case LAST -> ReferenceColumnResorterNodeModel.LAST_STRATEGY;
                case DROP -> ReferenceColumnResorterNodeModel.DROP_STRATEGY;
            };
            settings.addString(ReferenceColumnResorterNodeModel.CFGKEY_STRATEGY, strategy);
        }

        @Override
        public String[][] getConfigPaths() {
            return new String[][]{{ReferenceColumnResorterNodeModel.CFGKEY_STRATEGY}};
        }

        private String createInvalidSettingsExceptionMessage(final String name) {
            var values = Arrays.stream(ReferenceColumnResorterNodeModel.AVAILABLE_STRATEGIES)
                    .collect(Collectors.joining(", "));
            return String.format("Invalid value '%s'. Possible values: %s", name, values);
        }

    }

    enum UnspecifiedStrategy {
        @Label(value = "Last", description = "Put columns whose positions are not specified by the second input in "
            + "their original order at the last positions.")
        LAST,

        @Label(value = "First", description = "Put columns whose positions are not specified by the second input in "
            + "their original order at the first positions.")
        FIRST,

        @Label(value = "Drop", description = "Drop the columns whose positions are not specified by the second input.")
        DROP
    }

}
