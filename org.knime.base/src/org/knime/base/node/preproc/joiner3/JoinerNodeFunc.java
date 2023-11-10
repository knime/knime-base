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
 *   Oct 22, 2023 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.base.node.preproc.joiner3;

import org.knime.core.data.join.JoinTableSettings;
import org.knime.core.data.join.JoinTableSettings.JoinColumn;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.func.NodeFuncApi;
import org.knime.core.node.func.SimpleNodeFunc;
import org.knime.core.node.port.PortObjectSpec;

/**
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 * @since 5.2
 */
public final class JoinerNodeFunc implements SimpleNodeFunc {

    private static final String JOIN_COLUMN = "join_column";

    private static final String RIGHT = "right";

    private static final String LEFT = "left";

    @Override
    public Class<? extends NodeFactory<?>> getNodeFactoryClass() {
        return Joiner3NodeFactory.class;
    }

    @Override
    public void saveSettings(final NodeSettingsRO arguments, final PortObjectSpec[] inputSpecs,
        final NodeSettingsWO settings) throws InvalidSettingsException {
        var joinSettings = new Joiner3Settings();
        var joinColumns = new JoinColumn[]{new JoinTableSettings.JoinColumn(arguments.getString(JOIN_COLUMN))};
        joinSettings.setLeftJoinColumns(joinColumns);
        joinSettings.setRightJoinColumns(joinColumns);
        joinSettings.m_mergeJoinColumnsModel.setBooleanValue(true);
        joinSettings.saveSettingsTo(settings);
    }

    @Override
    public NodeFuncApi getApi() {
        return NodeFuncApi.builder("inner_join")//
            .withInputTable(LEFT, "The left table to be joined.")//
            .withInputTable(RIGHT, "The right table to be joined.")//
            .withStringArgument(JOIN_COLUMN, "The name of the column to join on. Must exist in both tables.")//
            .withOutputTable("joined_table",
                "The joined table containing all columns from left and right (column names are uniquified).")//
            .withDescription(
                "Performs an inner join on the tables left and right on the column named join_column which must exist in both left and right.")
            .build();
    }

}
