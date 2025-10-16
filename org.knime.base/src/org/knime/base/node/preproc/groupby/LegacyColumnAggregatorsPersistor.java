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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.knime.base.data.aggregation.ColumnAggregator;
import org.knime.base.node.preproc.groupby.GroupByNodeParameters.AggMethod;
import org.knime.base.node.preproc.groupby.GroupByNodeParameters.ColumnAggregatorElement;
import org.knime.base.node.preproc.groupby.GroupByNodeParameters.LegacyAggregationOperatorParameters;
import org.knime.base.node.preproc.groupby.GroupByNodeParameters.MissingValueOption;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.util.MutableInteger;
import org.knime.node.parameters.persistence.NodeParametersPersistor;

/**
 * Legacy persistor for column aggregators in GroupBy node.
 *
 * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
 */
final class LegacyColumnAggregatorsPersistor implements NodeParametersPersistor<ColumnAggregatorElement[]> {

    // key from ColumnAggregator
    private static final String CFG_OPERATOR_SETTINGS = "aggregationOperatorSettings";

    @Override
    public ColumnAggregatorElement[] load(final NodeSettingsRO settings) throws InvalidSettingsException {
        final var aggregators = ColumnAggregator.loadColumnAggregators(settings);
        // like #loadColumnAggregators did it, but it does not expose the data to us,
        // only to the custom operator dialogs
        final var operatorSettings = settings.containsKey(CFG_OPERATOR_SETTINGS)
            ? settings.getNodeSettings(CFG_OPERATOR_SETTINGS) : settings;

        final List<ColumnAggregatorElement> elements = new ArrayList<>();
        final var idMap = new HashMap<String, MutableInteger>();
        for (final var aggr : aggregators) {
            final var element = new ColumnAggregatorElement();
            element.m_column = aggr.getOriginalColName();
            element.m_aggregationMethod = new AggMethod(aggr.getId(), aggr.hasOptionalSettings());
            element.m_includeMissing =
                aggr.inclMissingCells() ? MissingValueOption.INCLUDE : MissingValueOption.EXCLUDE;
            if (aggr.hasOptionalSettings()) {
                final var settingsForOperator = operatorSettings.getNodeSettings(createSettingsKey(idMap, aggr));
                element.m_parameters = new LegacyAggregationOperatorParameters(settingsForOperator);
            }
            elements.add(element);
        }
        return elements.toArray(new ColumnAggregatorElement[0]);
    }

    private static String createSettingsKey(final Map<String, MutableInteger> idMap, final ColumnAggregator aggr) {
        // see ColumnAggregator#createSettingsKey
        final var id = String.join("_", aggr.getId(), aggr.getOriginalColName());
        final var count = idMap.get(id); // NOSONAR compute does not work, because we need to distinguish null and 0 from previous call
        if (count == null) {
            idMap.put(id, new MutableInteger(0));
            return id;
        }
        count.inc();
        return id + "_" + count.intValue();
    }

    @Override
    public void save(final ColumnAggregatorElement[] obj, final NodeSettingsWO settings) {

//        ColumnAggregator.saveColumnAggregators(settings, aggregators);
//        aggSettings.addInt("numAggregators", obj.length);
//
//        for (int i = 0; i < obj.length; i++) {
//            final NodeSettingsWO aggrSetting = aggSettings.addNodeSettings("aggregator_" + i);
//            try {
//                final ColumnAggregator aggr = new ColumnAggregator(
//                    obj[i].column,
//                    obj[i].aggregationMethod,
//                    obj[i].includeMissing);
//                aggr.saveSettingsTo(aggrSetting);
//            } catch (Exception ex) {
//                // Skip invalid aggregators
//            }
//        }
    }

    @Override
    public String[][] getConfigPaths() {
        return new String[][] {
            new String[] { "aggregationColumn", "columnNames" },
            new String[] { "aggregationColumn", "columnTypes" },
            new String[] { "aggregationColumn", "aggregationMethod" },
            new String[] { "aggregationColumn", "inclMissingVals" }
            };
    }
}