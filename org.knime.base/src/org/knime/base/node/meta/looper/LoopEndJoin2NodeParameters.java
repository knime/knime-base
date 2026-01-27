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

package org.knime.base.node.meta.looper;

import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.migration.LoadDefaultsForAbsentFields;
import org.knime.node.parameters.persistence.Persist;

/**
 * Node parameters for Loop End (Column Append).
 *
 * @author Magnus Gohm, KNIME GmbH, Konstanz, Germany
 * @author AI Migration Pipeline v1.2
 */
@LoadDefaultsForAbsentFields
final class LoopEndJoin2NodeParameters implements NodeParameters {

    @Persist(configKey = LoopEndJoinNodeConfiguration.CFG_KEY_HAS_SAME_ROWS_IN_EACH_ITERATION)
    @Widget(title = "Loop has same RowIDs in each iteration", description = """
            Check this box if the tables in each iteration have the same number of rows and the same row
            ordering. If this option is selected, the node does not use an expensive join (requires table
            sorting) but only puts tables side-by-side. This option does not have any influence on the output
            table. If the tables do not have the same RowIDs and this option is selected, the node will fail
            during execution.
            """)
    boolean m_hasSameRowsInEachIteration;

    @Persist(configKey = LoopEndJoinNodeConfiguration.CFG_KEY_PROPAGATE_LOOP_VARIABLES)
    @Widget(title = "Propagate modified loop variables", description = """
            If checked, variables whose values are modified within the loop are exported by this node. These
            variables must be declared outside the loop, i.e. injected into the loop from a side-branch or be
            available upstream of the corresponding loop start node. For the latter, any modification of a
            variable is passed back to the start node in subsequent iterations (e.g. moving sum calculation).
            Note that variables defined by the loop start node itself are excluded as these usually represent
            loop controls (e.g. "currentIteration").
            """)
    boolean m_propagateLoopVariables;

}
