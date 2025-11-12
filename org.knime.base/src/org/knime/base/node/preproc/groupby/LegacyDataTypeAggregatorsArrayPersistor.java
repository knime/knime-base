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
 *   21 Oct 2025 (Manuel Hotz, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.base.node.preproc.groupby;

import java.util.Comparator;
import java.util.List;

import org.knime.base.data.aggregation.AggregationMethods;
import org.knime.base.data.aggregation.AggregationOperatorParameters;
import org.knime.base.data.aggregation.dialogutil.type.DataTypeAggregator;
import org.knime.base.node.preproc.groupby.LegacyDataTypeAggregatorsArrayPersistor.DataTypeAggregatorElementDTO;
import org.knime.base.node.preproc.groupby.LegacyDataTypeAggregatorsArrayPersistor.IndexedElement;
import org.knime.core.data.DataType;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.webui.node.dialog.defaultdialog.NodeParametersUtil;
import org.knime.core.webui.node.dialog.defaultdialog.internal.persistence.ArrayPersistor;
import org.knime.core.webui.node.dialog.defaultdialog.internal.persistence.ElementFieldPersistor;

/**
 * Persistor for legacy data type aggregators array.
 *
 * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
 */
@SuppressWarnings("restriction")
final class LegacyDataTypeAggregatorsArrayPersistor
    implements ArrayPersistor<IndexedElement, DataTypeAggregatorElementDTO> {

    private static final String F_ARRAY_INDEX = "f_${array_index}";

    private DataTypeAggregator[] m_aggregators;

    static final class DataTypePersistor
        implements ElementFieldPersistor<DataType, IndexedElement, DataTypeAggregatorElementDTO> {

        @Override
        public DataType load(final NodeSettingsRO nodeSettings, final IndexedElement loadContext)
            throws InvalidSettingsException {
            return loadContext.getAggregator().getDataType();
        }

        @Override
        public void save(final DataType param, final DataTypeAggregatorElementDTO saveDTO) {
            saveDTO.m_element.m_dataType = param;
        }

        @Override
        public String[][] getConfigPaths() {
            return new String[][]{
                new String[]{GroupByNodeModel.CFG_DATA_TYPE_AGGREGATORS, F_ARRAY_INDEX, "dataType"}};
        }
    }

    static final class AggregationMethodPersistor
        implements ElementFieldPersistor<String, IndexedElement, DataTypeAggregatorElementDTO> {

        @Override
        public String load(final NodeSettingsRO nodeSettings, final IndexedElement loadContext)
            throws InvalidSettingsException {
            return loadContext.getAggregator().getId();
        }

        @Override
        public void save(final String param, final DataTypeAggregatorElementDTO saveDTO) {
            saveDTO.m_element.m_aggregationMethod = param;
        }

        @Override
        public String[][] getConfigPaths() {
            return new String[][]{
                new String[]{GroupByNodeModel.CFG_DATA_TYPE_AGGREGATORS, F_ARRAY_INDEX, "aggregationMethod"}};
        }
    }

    static final class MissingValueOptionPersistor
        implements ElementFieldPersistor<MissingValueOption, IndexedElement, DataTypeAggregatorElementDTO> {

        @Override
        public MissingValueOption load(final NodeSettingsRO nodeSettings, final IndexedElement loadContext)
            throws InvalidSettingsException {
            return loadContext.getAggregator().inclMissingCells() ? MissingValueOption.INCLUDE
                : MissingValueOption.EXCLUDE;
        }

        @Override
        public void save(final MissingValueOption param, final DataTypeAggregatorElementDTO saveDTO) {
            saveDTO.m_element.m_includeMissing = param;
        }

        @Override
        public String[][] getConfigPaths() {
            return new String[][]{
                new String[]{GroupByNodeModel.CFG_DATA_TYPE_AGGREGATORS, F_ARRAY_INDEX, "inclMissingVals"}};
        }
    }

    static final class OperatorParametersPersistor
        implements ElementFieldPersistor<AggregationOperatorParameters, IndexedElement, DataTypeAggregatorElementDTO> {

        @Override
        public AggregationOperatorParameters load(final NodeSettingsRO nodeSettings, final IndexedElement loadContext)
            throws InvalidSettingsException {
            final var aggr = loadContext.getAggregator();
            if (!aggr.hasOptionalSettings()) {
                return null;
            }
            final var cfg = nodeSettings //
                .getNodeSettings(GroupByNodeModel.CFG_DATA_TYPE_AGGREGATORS) //
                .getNodeSettings("f_" + loadContext.m_index) //
                .getNodeSettings("functionSettings");
            final var paramClass = AggregationMethods.getInstance().getParametersClassFor(aggr.getId()).orElse(null);
            if (paramClass != null) {
                return NodeParametersUtil.loadSettings(cfg, paramClass);
            }
            return new LegacyAggregationOperatorParameters(cfg);
        }

        @Override
        public String[][] getConfigPaths() {
            // TODO not possible to specify arbitrary nested keys dynamically
            return new String[0][];
        }

        @Override
        public void save(final AggregationOperatorParameters param, final DataTypeAggregatorElementDTO saveDTO) {
            saveDTO.m_element.m_parameters = param;
        }
    }

    static class IndexedElement {

        private final DataTypeAggregator[] m_aggregators;

        private final int m_index;

        IndexedElement(final DataTypeAggregator[] aggregators, final int index) {
            m_aggregators = CheckUtils.checkNotNull(aggregators);
            CheckUtils.check(index < m_aggregators.length, IllegalArgumentException::new,
                () -> "Index %d of array element out of bounds: %d".formatted(index, m_aggregators.length));
            m_index = index;
        }

        DataTypeAggregator getAggregator() {
            return m_aggregators[m_index];
        }

    }

    static final class DataTypeAggregatorElementDTO {

        private final int m_index;

        private final DataTypeAggregatorElement m_element;

        DataTypeAggregatorElementDTO(final int index) {
            m_index = index;
            m_element = new DataTypeAggregatorElement();
        }

        DataTypeAggregatorElement getElement() {
            return m_element;
        }
    }

    @Override
    public int getArrayLength(final NodeSettingsRO nodeSettings) throws InvalidSettingsException {
        m_aggregators = DataTypeAggregator.loadAggregators(nodeSettings, GroupByNodeModel.CFG_DATA_TYPE_AGGREGATORS)
            .toArray(DataTypeAggregator[]::new);
        return m_aggregators.length;
    }

    @Override
    public IndexedElement createElementLoadContext(final int index) {
        CheckUtils.checkState(m_aggregators != null, "Expected that array length was queried before.");
        return new IndexedElement(m_aggregators, index);
    }

    @Override
    public DataTypeAggregatorElementDTO createElementSaveDTO(final int index) {
        return new DataTypeAggregatorElementDTO(index);
    }

    @Override
    public void save(final List<DataTypeAggregatorElementDTO> savedElements, final NodeSettingsWO nodeSettings) {
        final var aggs = savedElements.stream() //
            .sorted(Comparator.comparingInt(ie -> ie.m_index)) //
            .map(DataTypeAggregatorElementDTO::getElement) //
            .map(LegacyDataTypeAggregatorsArrayPersistor::mapToAggregator) //
            .toList();
        DataTypeAggregator.saveAggregators(nodeSettings, GroupByNodeModel.CFG_DATA_TYPE_AGGREGATORS, aggs);
    }

    static DataTypeAggregator mapToAggregator(final DataTypeAggregatorElement elem) {
        final var method = AggregationMethods.getMethod4Id(elem.m_aggregationMethod);
        final var type = elem.m_dataType;
        final var includeMissing = elem.m_includeMissing == MissingValueOption.INCLUDE;
        final var agg = new DataTypeAggregator(type, method, includeMissing);
        // inject optional settings into aggregator instance
        final var params = elem.m_parameters;
        if (params != null) {
            final NodeSettings functionSettings;
            if (elem.m_parameters instanceof LegacyAggregationOperatorParameters legacyParams) {
                // the fallback just wraps
                functionSettings = legacyParams.getNodeSettings();
            } else {
                // must be custom parameters via extension point
                final var settingsToSaveInto = new NodeSettings("functionSettings");
                NodeParametersUtil.saveSettings(elem.m_parameters.getClass(), elem.m_parameters, settingsToSaveInto);
                functionSettings = settingsToSaveInto;
            }
            try {
                agg.loadValidatedSettings(functionSettings);
            } catch (InvalidSettingsException e) {
                throw new IllegalStateException("Failed to map optional settings", e);
            }
        }
        return agg;
    }
}
