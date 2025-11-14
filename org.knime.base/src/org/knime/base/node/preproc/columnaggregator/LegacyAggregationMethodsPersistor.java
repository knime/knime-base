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

import java.util.ArrayList;
import java.util.List;

import org.knime.base.data.aggregation.AggregationMethods;
import org.knime.base.data.aggregation.NamedAggregationOperator;
import org.knime.base.node.preproc.groupby.common.LegacyAggregationOperatorParameters;
import org.knime.base.node.preproc.groupby.common.MissingValueOption;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.webui.node.dialog.defaultdialog.NodeParametersUtil;
import org.knime.node.parameters.persistence.NodeParametersPersistor;

/**
 * Legacy persistor for aggregation methods in Column Aggregator node.
 *
 * @author Paul Baernreuther, KNIME GmbH, Germany
 */
public final class LegacyAggregationMethodsPersistor implements NodeParametersPersistor<AggregationMethodElement[]> {

    @Override
    public AggregationMethodElement[] load(final NodeSettingsRO settings) throws InvalidSettingsException {
        final var subSettings = settings.getNodeSettings(ColumnAggregatorNodeModel.CFG_AGGREGATION_METHODS);
        final var methods = NamedAggregationOperator.loadOperators(subSettings);

        final List<AggregationMethodElement> elements = new ArrayList<>();
        for (final var method : methods) {
            final var element = new AggregationMethodElement();
            element.m_aggregationMethod = method.getId();
            element.m_resultColName = method.getName();
            element.m_includeMissing =
                method.inclMissingCells() ? MissingValueOption.INCLUDE : MissingValueOption.EXCLUDE;

            if (method.hasOptionalSettings()) {
                final var operatorSettings = new NodeSettings(method.getId());
                method.saveSettingsTo(operatorSettings);
                final var params = AggregationMethods.getInstance().getParametersClassFor(method.getId());
                if (params.isPresent()) {
                    final var optionalParamsClass = params.get();
                    element.m_parameters = NodeParametersUtil.loadSettings(operatorSettings, optionalParamsClass);
                } else {
                    // fallback for operators without custom parameters
                    element.m_parameters = new LegacyAggregationOperatorParameters(operatorSettings);
                }
            }
            elements.add(element);
        }
        return elements.toArray(new AggregationMethodElement[0]);
    }

    @SuppressWarnings("restriction")
    @Override
    public void save(final AggregationMethodElement[] elems, final NodeSettingsWO settings) {
        final List<NamedAggregationOperator> methods = new ArrayList<>();
        List<String> operatorIds = new ArrayList<>();
        List<String> resultColNames = new ArrayList<>();
        List<Boolean> includeMissingVals = new ArrayList<>();
        final var subSettings = settings.addNodeSettings(ColumnAggregatorNodeModel.CFG_AGGREGATION_METHODS);
        for (final var elem : elems) {
            if (elem.m_aggregationMethod == null) {
                continue;
            }
            operatorIds.add(elem.m_aggregationMethod);
            resultColNames.add(elem.m_resultColName);
            includeMissingVals.add(elem.m_includeMissing == MissingValueOption.INCLUDE);

            // Load optional settings into the operator if they exist
            if (elem.m_parameters != null) {
                final var key = createSettingsKey(elem.m_aggregationMethod, elem.m_resultColName);
                final var operatorSettings = subSettings.addNodeSettings(key);
                if (elem.m_parameters instanceof LegacyAggregationOperatorParameters legacyParams) {
                    final var extractedSettings = legacyParams.getNodeSettings();
                    extractedSettings.copyTo(operatorSettings);
                } else {
                    NodeParametersUtil.saveSettings(elem.m_parameters.getClass(), elem.m_parameters, operatorSettings);
                }

            }
        }
        subSettings.addStringArray("resultColName", resultColNames.toArray(new String[0]));
        subSettings.addStringArray("aggregationMethod", operatorIds.toArray(new String[0]));
        subSettings.addBooleanArray("inclMissingVals", toPrimitiveBooleanArray(includeMissingVals));

    }

    private static boolean[] toPrimitiveBooleanArray(final List<Boolean> booleanList) {
        final boolean[] primitiveArray = new boolean[booleanList.size()];
        for (int i = 0; i < booleanList.size(); i++) {
            primitiveArray[i] = booleanList.get(i);
        }
        return primitiveArray;
    }

    // copied from NamedAggregationOperator
    private static String createSettingsKey(final String id, final String name) {
        return id + "_" + name;
    }

    @Override
    public String[][] getConfigPaths() {
        return new String[][]{ //
            {ColumnAggregatorNodeModel.CFG_AGGREGATION_METHODS, "aggregationMethod"}, //
            {ColumnAggregatorNodeModel.CFG_AGGREGATION_METHODS, "resultColName"}, //
            {ColumnAggregatorNodeModel.CFG_AGGREGATION_METHODS, "inclMissingVals"} //
                // operator specific settings are not listed here yet.
        };
    }
}
