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

package org.knime.base.node.preproc.correlation.compute2;

import java.util.List;

import org.knime.base.node.preproc.correlation.CorrelationUtils.ColumnPairFilter;
import org.knime.base.node.preproc.correlation.pmcc.PValueAlternative;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.NominalValue;
import org.knime.core.webui.node.dialog.defaultdialog.internal.widget.TypedStringFilterWidgetInternal;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.migration.LoadDefaultsForAbsentFields;
import org.knime.node.parameters.persistence.Persist;
import org.knime.node.parameters.persistence.Persistor;
import org.knime.node.parameters.persistence.legacy.LegacyColumnFilterPersistor;
import org.knime.node.parameters.widget.choices.RadioButtonsWidget;
import org.knime.node.parameters.widget.choices.filter.ColumnFilter;
import org.knime.node.parameters.widget.choices.filter.ColumnFilterWidget;
import org.knime.node.parameters.widget.choices.util.ColumnSelectionUtil;
import org.knime.node.parameters.widget.choices.util.CompatibleColumnsProvider;
import org.knime.node.parameters.widget.number.NumberInputWidget;
import org.knime.node.parameters.widget.number.NumberInputWidgetValidation;

/**
 * Node parameters for Linear Correlation.
 *
 * @author Timothy Crundall, KNIME GmbH, Germany
 * @author AI Migration Pipeline v1.2
 */
@LoadDefaultsForAbsentFields
class CorrelationCompute2NodeParameters implements NodeParameters {

    CorrelationCompute2NodeParameters() {
        // Default constructor
    }

    CorrelationCompute2NodeParameters(final NodeParametersInput input) {
        m_columnFilter = new ColumnFilter(
            ColumnSelectionUtil.getCompatibleColumnsOfFirstPort(input, DoubleValue.class, NominalValue.class))
            .withIncludeUnknownColumns();
    }

    private static final class ColumnFilterLegacyPersistor extends LegacyColumnFilterPersistor {
        ColumnFilterLegacyPersistor() {
            super(CorrelationCompute2NodeModel.CFG_INCLUDE_LIST);
        }
    }

    private static final class NumericNominalColumnsProvider extends CompatibleColumnsProvider {
        private NumericNominalColumnsProvider() {
            super(List.of(DoubleValue.class, NominalValue.class));
        }
    }

    @Persistor(ColumnFilterLegacyPersistor.class)
    @ColumnFilterWidget(choicesProvider = NumericNominalColumnsProvider.class)
    @Widget(title = "Column selection", description = """
            Select the columns to be included in the correlation analysis. Only numeric and nominal columns are \
            available. Use the include/exclude lists to specify which columns should be processed. \
            """)
    @TypedStringFilterWidgetInternal(hideTypeFilter = true)
    ColumnFilter m_columnFilter = new ColumnFilter();

    @Widget(title = "Output column pairs", description = """
            Select which column pairs of the selected columns should be included in the correlation measure \
            table. If only compatible column pairs are included <tt>numeric &lt;-&gt; nominal</tt> pairs will be \
            excluded. If only pairs with a valid correlation are included all pairs for which the correlation \
            cannot be computed are excluded. \
            """)
    @Persist(configKey = CorrelationCompute2NodeModel.CFG_COLUMN_PAIRS_FILTER)
    @RadioButtonsWidget
    ColumnPairFilter m_outputColumnPairs = ColumnPairFilter.COMPATIBLE_PAIRS;

    @Widget(title = "Possible values count", description = """
            Select an upper bound for the number of possible values for each of the nominal columns. If more \
            values are encountered in a nominal column, the column will be ignored (no correlation values will \
            be computed). \
            """)
    @Persist(configKey = CorrelationCompute2NodeModel.CFG_POSSIBLE_VALUES_COUNT)
    @NumberInputWidget(minValidation = MinimumOutputColumnPairs.class)
    int m_possibleValuesCount = 50;

    @Widget(title = "p-value", description = """
            Select which p-value should be computed for Pearson's product-moment coefficient. \
            <ul> \
            <li>"two-sided" corresponds to the probability of obtaining a correlation value that is at least \
            as extreme as the observed correlation.</li> \
            <li>"one-sided (right)" corresponds to the probability of obtaining a correlation value that shows \
            even greater <b>positive</b> association.</li> \
            <li>"one-sided (left)" corresponds to the probability of obtaining a correlation value that shows \
            even greater <b>negative</b> association.</li> \
            </ul> \
            Note that the p-value for Pearson's chi square test is always one-sided. \
            """)
    @Persist(configKey = CorrelationCompute2NodeModel.CFG_PVAL_ALTERNATIVE)
    @RadioButtonsWidget
    PValueAlternative m_pValue = PValueAlternative.TWO_SIDED;

    static final class MinimumOutputColumnPairs extends NumberInputWidgetValidation.MinValidation {
        @Override
        protected double getMin() {
            return 2;
        }
    }
}
