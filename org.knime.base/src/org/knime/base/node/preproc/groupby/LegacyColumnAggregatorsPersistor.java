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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.knime.base.data.aggregation.AggregationMethods;
import org.knime.base.data.aggregation.ColumnAggregator;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.util.MutableInteger;
import org.knime.core.webui.node.dialog.defaultdialog.NodeParametersUtil;
import org.knime.node.parameters.persistence.NodeParametersPersistor;

/**
 * Legacy persistor for column aggregators in GroupBy node.
 *
 * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
 */
final class LegacyColumnAggregatorsPersistor implements NodeParametersPersistor<ColumnAggregatorElement[]> {

    // The implementation follows ColumnAggregator persistence closely

    private static final String CNFG_AGGREGATION_OPERATOR_SETTINGS = "aggregationOperatorSettings";

    private static final String CNFG_AGGR_COL_SECTION = "aggregationColumn";

    private static final String CNFG_COL_NAMES = "columnNames";

    private static final String CNFG_COL_TYPES = "columnTypes";

    private static final String CNFG_AGGR_METHOD = "aggregationMethod";

    private static final String CNFG_INCL_MISSING = "inclMissingVals";

    @Override
    public ColumnAggregatorElement[] load(final NodeSettingsRO settings) throws InvalidSettingsException {
        final var aggregators = ColumnAggregator.loadColumnAggregators(settings);

        // the ColumnAggregator object does not make the operator-specific settings available
        final List<ColumnAggregatorElement> elements = new ArrayList<>();
        final var idMap = new HashMap<String, MutableInteger>();
        for (final var aggr : aggregators) {
            final var element = new ColumnAggregatorElement();
            element.m_column = aggr.getOriginalColName();
            element.m_dataType = aggr.getOriginalDataType();
            element.m_aggregationMethod = aggr.getId();
            element.m_includeMissing =
                aggr.inclMissingCells() ? MissingValueOption.INCLUDE : MissingValueOption.EXCLUDE;
            if (aggr.hasOptionalSettings()) {
                final var operatorSettings =
                    new NodeSettings(createSettingsKey(idMap, aggr.getId(), aggr.getOriginalColName()));
                aggr.saveSettingsTo(operatorSettings);
                final var params = AggregationMethods.getInstance().getParametersClassFor(aggr.getId());
                if (params.isPresent()) {
                    final var optionalParamsClass = params.get();
                    element.m_parameters = NodeParametersUtil.loadSettings(operatorSettings, optionalParamsClass);
                } else {
                    // nothing custom, so fallback
                    element.m_parameters = new LegacyAggregationOperatorParameters(operatorSettings);
                }
            }
            elements.add(element);
        }
        return elements.toArray(new ColumnAggregatorElement[0]);
    }

    @SuppressWarnings("restriction")
    @Override
    public void save(final ColumnAggregatorElement[] elems, final NodeSettingsWO settings) {
        final var aggregators = Arrays.stream(elems) //
                // TODO remove this workaround (next line) if we can disable the "Add method" button
                .filter(agg -> agg.m_column != null && agg.m_dataType != null)
                .map(LegacyColumnAggregatorsPersistor::mapToAggregator).toList();
        ColumnAggregator.saveColumnAggregators(settings, aggregators);
        final var operatorSettings = settings.addNodeSettings(CNFG_AGGREGATION_OPERATOR_SETTINGS);
        // we need to recreate the key for each operator's optional settings, because the fallback extraction
        // names it "extracted model settings"
        final var idMap = new HashMap<String, MutableInteger>();
        for (final var elem : elems) {
            if (elem.m_parameters == null) {
                continue;
            }
            final var settingsToSaveInto =
                    new NodeSettings(createSettingsKey(idMap, elem.m_aggregationMethod, elem.m_column));
            if (elem.m_parameters instanceof LegacyAggregationOperatorParameters legacyParams) {
                final var extractedSettings = legacyParams.getNodeSettings();
                extractedSettings.copyTo(settingsToSaveInto);
            } else {
                final var params = elem.m_parameters;
                NodeParametersUtil.saveSettings(params.getClass(), params, settingsToSaveInto);
            }
            operatorSettings.addNodeSettings(settingsToSaveInto);
        }
    }

    private static ColumnAggregator mapToAggregator(final ColumnAggregatorElement colAggElem) {
        // we just need the objects to save to settings, not construct an executable aggregator
        final var dcs = new DataColumnSpecCreator(colAggElem.m_column, colAggElem.m_dataType).createSpec();
        final var method = AggregationMethods.getMethod4Id(colAggElem.m_aggregationMethod);
        final var includeMissing = colAggElem.m_includeMissing == MissingValueOption.INCLUDE;
        return new ColumnAggregator(dcs, method, includeMissing);
    }


    private static String createSettingsKey(final Map<String, MutableInteger> idMap, final String operatorID,
        final String originalColName) {
        // see ColumnAggregator#createSettingsKey
        final var id = String.join("_", operatorID, originalColName);
        final var count = idMap.get(id); // NOSONAR compute does not work, because we need to distinguish null and 0 from previous call
        if (count == null) {
            idMap.put(id, new MutableInteger(0));
            return id;
        }
        count.inc();
        return id + "_" + count.intValue();
    }
    @Override
    public String[][] getConfigPaths() {
        return new String[][]{ //
            new String[]{CNFG_AGGR_COL_SECTION, CNFG_COL_NAMES}, //
            new String[]{CNFG_AGGR_COL_SECTION, CNFG_COL_TYPES}, //
            new String[]{CNFG_AGGR_COL_SECTION, CNFG_AGGR_METHOD}, //
            new String[]{CNFG_AGGR_COL_SECTION, CNFG_INCL_MISSING} //
        };
    }
}
