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
import java.util.List;

import org.knime.base.data.aggregation.AggregationMethods;
import org.knime.base.data.aggregation.dialogutil.pattern.PatternAggregator;
import org.knime.base.node.preproc.groupby.OptionalParameters.LegacyAggregationOperatorParameters;
import org.knime.base.node.util.regex.PatternType;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.node.parameters.persistence.NodeParametersPersistor;

/**
 * Legacy persistor for pattern aggregators in GroupBy node.
 *
 * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
 */
final class LegacyPatternAggregatorsPersistor implements NodeParametersPersistor<PatternAggregatorElement[]> {

    @Override
    public PatternAggregatorElement[] load(final NodeSettingsRO settings) throws InvalidSettingsException {
        final var aggregators = PatternAggregator.loadAggregators(settings, GroupByNodeModel.CFG_PATTERN_AGGREGATORS);

        // optional operator settings are stored in each operator's config object, not as a "sidecar"
        final List<PatternAggregatorElement> aggregatorElements = new ArrayList<>();
        for (final PatternAggregator aggr : aggregators) {
            final var elem = new PatternAggregatorElement();
            elem.m_pattern = aggr.getInputPattern();
            elem.m_aggregationMethod = aggr.getId();
            elem.m_isRegex = aggr.isRegex() ? PatternType.REGEX : PatternType.WILDCARD;
            elem.m_includeMissing = aggr.inclMissingCells() ? MissingValueOption.INCLUDE : MissingValueOption.EXCLUDE;
            if (aggr.hasOptionalSettings()) {
                final var operatorSettings = new NodeSettings("functionSettings");
                aggr.saveSettingsTo(operatorSettings);
                elem.m_parameters = new LegacyAggregationOperatorParameters(operatorSettings);
            }
            aggregatorElements.add(elem);
        }
        return aggregatorElements.toArray(PatternAggregatorElement[]::new);
    }

    @Override
    public void save(final PatternAggregatorElement[] elems, final NodeSettingsWO settings) {
        final var aggregators = Arrays.stream(elems).map(LegacyPatternAggregatorsPersistor::mapToAggregator).toList();
        PatternAggregator.saveAggregators(settings, GroupByNodeModel.CFG_PATTERN_AGGREGATORS, aggregators);
    }

    static PatternAggregator mapToAggregator(final PatternAggregatorElement elem) {
        final var method = AggregationMethods.getMethod4Id(elem.m_aggregationMethod);
        final var includeMissing = elem.m_includeMissing == MissingValueOption.INCLUDE;
        final var agg =
            new PatternAggregator(elem.m_pattern, elem.m_isRegex == PatternType.REGEX, method, includeMissing);
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
            new String[]{GroupByNodeModel.CFG_PATTERN_AGGREGATORS} // TODO each operator is in a config with f_<idx>
                //            new String[]{GroupByNodeModel.CFG_PATTERN_AGGREGATORS, "inputPattern"}, //
                //            new String[]{GroupByNodeModel.CFG_PATTERN_AGGREGATORS, "isRegularExpression"}, //
                //            new String[]{GroupByNodeModel.CFG_PATTERN_AGGREGATORS, "inclMissingValues"}, //
                //            new String[]{GroupByNodeModel.CFG_PATTERN_AGGREGATORS, "aggregationMethod"}, //
                //            new String[]{GroupByNodeModel.CFG_PATTERN_AGGREGATORS, "functionSettings"} //
        };
    }
}