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

package org.knime.base.node.meta.looper.columnlist2;

import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.migration.LoadDefaultsForAbsentFields;
import org.knime.node.parameters.persistence.Persistor;
import org.knime.node.parameters.persistence.legacy.EnumBooleanPersistor;
import org.knime.node.parameters.persistence.legacy.LegacyColumnFilterPersistor;
import org.knime.node.parameters.widget.choices.Label;
import org.knime.node.parameters.widget.choices.RadioButtonsWidget;
import org.knime.node.parameters.widget.choices.filter.ColumnFilter;
import org.knime.node.parameters.widget.choices.filter.ColumnFilterWidget;
import org.knime.node.parameters.widget.choices.util.AllColumnsProvider;
import org.knime.node.parameters.widget.choices.util.ColumnSelectionUtil;

/**
 * Node parameters for Column List Loop Start.
 *
 * @author Benjamin Moser, KNIME GmbH, Konstanz, Germany
 * @author AI Migration Pipeline v1.1
 */
@LoadDefaultsForAbsentFields
final class ColumnListLoopStartNodeParameters implements NodeParameters {

    ColumnListLoopStartNodeParameters() {
        // Default constructor
    }

    ColumnListLoopStartNodeParameters(final NodeParametersInput input) {
        m_columnFilter =
                new ColumnFilter(ColumnSelectionUtil.getAllColumnsOfFirstPort(input)).withIncludeUnknownColumns();
    }

    @SuppressWarnings("restriction")
    static final class ColumnFilterLegacyPersistor extends LegacyColumnFilterPersistor {
        ColumnFilterLegacyPersistor() {
            super("column-filter");
        }
    }

    @Persistor(ColumnListLoopStartNodeParameters.ColumnFilterLegacyPersistor.class)
    @ColumnFilterWidget(choicesProvider = AllColumnsProvider.class)
    @Widget(title = "Columns to iterate", description = """
            Choose the columns to iterate over -- each column that matches the include criteria defines one
            iteration (so the loop will run as often as there are columns included). Columns excluded are considered
            static and will always be passed into the loop body.
            """)
    ColumnFilter m_columnFilter = new ColumnFilter();

    @SuppressWarnings("restriction")
    static final class EmptyColumnListPolicyPersistor extends EnumBooleanPersistor<EmptyColumnListPolicy> {
        EmptyColumnListPolicyPersistor() {
            super("no_columns_policy", EmptyColumnListPolicy.class, EmptyColumnListPolicy.RUN_ONE_ITERATION);
        }
    }

    @Widget(title = "If include column list is empty", description = """
            Define the behavior if the include column list is empty. 'Run one iteration' will execute the loop body
            once with all 'excluded' columns passed unmodified into the loop body (so all columns in the input table).
            'Fail' will cause no loop iteration to be run; instead the loop start node will fail with an appropriate
            error message.
            """)
    @Persistor(EmptyColumnListPolicyPersistor.class)
    @RadioButtonsWidget
    EmptyColumnListPolicy m_emptyColumnListPolicy = EmptyColumnListPolicy.RUN_ONE_ITERATION;

    enum EmptyColumnListPolicy {
            @Label("Run one iteration")
            RUN_ONE_ITERATION, //
            @Label("Fail")
            FAIL
    }

}
