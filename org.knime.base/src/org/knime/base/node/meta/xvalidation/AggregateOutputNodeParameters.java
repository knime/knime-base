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

package org.knime.base.node.meta.xvalidation;

import java.util.List;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.NominalValue;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.migration.LoadDefaultsForAbsentFields;
import org.knime.node.parameters.persistence.Persist;
import org.knime.node.parameters.widget.choices.ChoicesProvider;
import org.knime.node.parameters.widget.choices.util.CompatibleColumnsProvider;

/**
 * Node parameters for X-Aggregator.
 *
 * @author Magnus Gohm, KNIME GmbH, Konstanz, Germany
 * @author AI Migration Pipeline v1.1
 */
@LoadDefaultsForAbsentFields
final class AggregateOutputNodeParameters implements NodeParameters {

    /**
     * Default constructor.
     */
    AggregateOutputNodeParameters() {
    }

    /**
     * Determines a target and prediction column from the context if possible.
     *
     * @param context to retrieve the data table spec from
     */
    AggregateOutputNodeParameters(final NodeParametersInput context) {
        final var compatibleCols = context.getInTableSpec(0).stream().flatMap(DataTableSpec::stream).filter(
            spec -> (spec.getType().isCompatible(NominalValue.class) || spec.getType().isCompatible(DoubleValue.class)))
            .toList();
        if (!compatibleCols.isEmpty()) {
            if (compatibleCols.size() > 1) {
                m_targetColumn = compatibleCols.get(0).getName();
                m_predictionColumn = compatibleCols.get(1).getName();
            } else {
                m_targetColumn = compatibleCols.get(0).getName();
                m_predictionColumn = compatibleCols.get(0).getName();
            }
        }
    }

    static final class NominalAndDoubleColumnsProvider extends CompatibleColumnsProvider {
        protected NominalAndDoubleColumnsProvider() {
            super(List.of(NominalValue.class, DoubleValue.class));
        }
    }

    @Widget(title = "Target column", description = "Column containing the true class label")
    @Persist(configKey = AggregateSettings.CFG_KEY_TARGET_COLUMN)
    @ChoicesProvider(NominalAndDoubleColumnsProvider.class)
    String m_targetColumn;

    @Widget(title = "Prediction column", description = "Column containing the prediction label")
    @Persist(configKey = AggregateSettings.CFG_KEY_PREDICTION_COLUMN)
    @ChoicesProvider(NominalAndDoubleColumnsProvider.class)
    String m_predictionColumn;

    @Widget(title = "Add column with fold id",
        description = "If selected an additional column is added to the first output table that "
            + "contains the fold's id in which the row was produced.")
    @Persist(configKey = AggregateSettings.CFG_KEY_ADD_FOLD_ID)
    boolean m_addFoldId;
}
