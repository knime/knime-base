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
 *   20 Aug 2025 (Robin Gerling, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.base.node.mine.scorer.entrop;

import static org.knime.node.parameters.widget.choices.util.ColumnSelectionUtil.getFirstStringColumn;

import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.migration.LoadDefaultsForAbsentFields;
import org.knime.node.parameters.persistence.Persist;
import org.knime.node.parameters.widget.choices.ChoicesProvider;
import org.knime.node.parameters.widget.choices.util.AllColumnsProvider;

/**
 * WebUI settings for the Entropy Scorer node.
 *
 * Backwards compatible with legacy settings via config keys in {@link EntropyNodeModel}.
 *
 * @author Robin Gerling, KNIME GmbH, Konstanz, Germany
 * @author AI Migration Pipeline v1.1
 * @since 5.7
 */
@SuppressWarnings("restriction")
@LoadDefaultsForAbsentFields
final class NewEntropyNodeParameters implements NodeParameters {

    /** Column containing the reference clustering (first input table). */
    @Widget(title = "Reference column",
        description = "Column containing the reference clustering from the first input table.")
    @ChoicesProvider(ReferenceStringColumns.class)
    @Persist(configKey = EntropyNodeModel.CFG_REFERENCE_COLUMN)
    String m_referenceColumn;

    /** Column containing the cluster IDs to evaluate (second input table). */
    @Widget(title = "Clustering column",
        description = "Column containing the cluster IDs to evaluate from the second input table.")
    @ChoicesProvider(ClusteringStringColumns.class)
    @Persist(configKey = EntropyNodeModel.CFG_CLUSTERING_COLUMN)
    String m_clusteringColumn;

    /** Default ctor for serialization. */
    NewEntropyNodeParameters() {
    }

    /** Initialize defaults from connected specs. */
    NewEntropyNodeParameters(final NodeParametersInput ctx) {
        m_referenceColumn = getFirstStringColumnAtPort(ctx, EntropyNodeModel.INPORT_REFERENCE);
        m_clusteringColumn = getFirstStringColumnAtPort(ctx, EntropyNodeModel.INPORT_CLUSTERING);
    }

    private static String getFirstStringColumnAtPort(final NodeParametersInput ctx, final int portInd) {
        return ctx.getInTableSpec(portInd).flatMap(spec -> getFirstStringColumn(spec).map(c -> c.getName()))
            .orElse(null);
    }

    static class ReferenceStringColumns extends AllColumnsProvider {
        @Override
        public int getInputTableIndex() {
            return EntropyNodeModel.INPORT_REFERENCE;
        }
    }

    static class ClusteringStringColumns extends AllColumnsProvider {
        @Override
        public int getInputTableIndex() {
            return EntropyNodeModel.INPORT_CLUSTERING;
        }
    }
}
