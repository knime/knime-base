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
 *   Oct 22, 2020 (Mark Ortmann, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.base.node.meta.looper.variable.start;

import org.knime.base.node.flowvariable.tablerowtovariable3.TableToVariable3NodeSettings;
import org.knime.core.node.NodeFactory;
import org.knime.core.webui.node.impl.WebUINodeConfiguration;
import org.knime.core.webui.node.impl.WebUINodeFactory;

/**
 * The {@link NodeFactory} creating the 'Table Row To Variable Loop Start' node.
 *
 * @author Mark Ortmann, KNIME GmbH, Berlin, Germany
 */
@SuppressWarnings("restriction")
public class LoopStartVariable3NodeFactory extends WebUINodeFactory<LoopStartVariable2NodeModel> {

    private static final WebUINodeConfiguration CONFIG = WebUINodeConfiguration.builder()//
        .name("Table Row To Variable Loop Start")//
        .icon("./loopstartvariable.png")//
        .shortDescription("Iterates over an input data table, whereby each row defines an iteration "
            + "with variable settings taken from the values in that row.")//
        .fullDescription(
            "This node uses each row of a data table to define new variable values for each loop iteration. "
                + "The names of the variables are defined by the column names.")//
        .modelSettingsClass(TableToVariable3NodeSettings.class)//
        .addInputTable("Parameters table", "The table whose rows will constitute variables' "
            + "values for each iteration.")//
        .addOutputTable("Variables Connection", "Holds created flow variables.")//
        // no since version, not sure since when this node is available
        .build();

    /** Set the node description */
    public LoopStartVariable3NodeFactory() {
        super(CONFIG);
    }

    @Override
    public LoopStartVariable2NodeModel createNodeModel() {
        return new LoopStartVariable2NodeModel(false);
    }

}
