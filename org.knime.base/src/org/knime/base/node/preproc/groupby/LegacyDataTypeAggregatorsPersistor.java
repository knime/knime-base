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
 *  you the additional permission to use and Propagate KNIME together with
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
import java.util.Arrays;
import java.util.List;

import org.knime.base.data.aggregation.AggregationMethods;
import org.knime.base.data.aggregation.dialogutil.type.DataTypeAggregator;
import org.knime.base.node.preproc.groupby.OptionalParameters.LegacyAggregationOperatorParameters;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.node.parameters.persistence.NodeParametersPersistor;

/**
 * Legacy persistor for data type aggregators in GroupBy node.
 *
 * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
 */
final class LegacyDataTypeAggregatorsPersistor implements NodeParametersPersistor<DataTypeAggregatorElement[]> {

    @Override
    public DataTypeAggregatorElement[] load(final NodeSettingsRO settings) throws InvalidSettingsException {
        final var aggregators =
            DataTypeAggregator.loadAggregators(settings, GroupByNodeModel.CFG_DATA_TYPE_AGGREGATORS);

        // the optional operator settings are stored in each operator's config object, not as a "sidecar"
        final List<DataTypeAggregatorElement> elements = new ArrayList<>();
        for (final var aggr : aggregators) {
            final var element = new DataTypeAggregatorElement();
            element.m_dataType = aggr.getDataType();
            element.m_aggregationMethod = aggr.getId();
            element.m_includeMissing =
                aggr.inclMissingCells() ? MissingValueOption.INCLUDE : MissingValueOption.EXCLUDE;
            if (aggr.hasOptionalSettings()) {
                // since these are not in a "sidecar" nodesettings, they all are under the same key
                final var operatorSettings = new NodeSettings("functionSettings");
                aggr.saveSettingsTo(operatorSettings);
                element.m_parameters = new LegacyAggregationOperatorParameters(operatorSettings);
            }
            elements.add(element);
        }
        return elements.toArray(new DataTypeAggregatorElement[0]);
    }

    @Override
    public void save(final DataTypeAggregatorElement[] elems, final NodeSettingsWO settings) {
        final var aggregators = Arrays.stream(elems).map(LegacyDataTypeAggregatorsPersistor::mapToAggregator).toList();
        DataTypeAggregator.saveAggregators(settings, GroupByNodeModel.CFG_DATA_TYPE_AGGREGATORS, aggregators);
    }

    static DataTypeAggregator mapToAggregator(final DataTypeAggregatorElement elem) {
        final var method = AggregationMethods.getMethod4Id(elem.m_aggregationMethod);
        final var type = elem.m_dataType;
        final var includeMissing = elem.m_includeMissing == MissingValueOption.INCLUDE;
        final var agg = new DataTypeAggregator(type, method, includeMissing);
        if (elem.m_parameters instanceof LegacyAggregationOperatorParameters legacyParams) {
            try {
                agg.loadValidatedSettings(legacyParams.getNodeSettings());
            } catch (InvalidSettingsException e) {
                throw new IllegalStateException("Failed to map optional settings", e);
            }
        }
        return agg;
    }

    @Override
    public String[][] getConfigPaths() {
        return new String[][]{ //
            new String[]{GroupByNodeModel.CFG_DATA_TYPE_AGGREGATORS}, // TODO each operator is in a config with f_<idx>
                //            new String[]{GroupByNodeModel.CFG_DATA_TYPE_AGGREGATORS, "dataType"}, //
                //            new String[]{GroupByNodeModel.CFG_DATA_TYPE_AGGREGATORS, "inclMissingValues"}, //
                //            new String[]{GroupByNodeModel.CFG_DATA_TYPE_AGGREGATORS, "aggregationMethod"}, //
                //            new String[]{GroupByNodeModel.CFG_DATA_TYPE_AGGREGATORS, "functionSettings"} //
        };
    }
}