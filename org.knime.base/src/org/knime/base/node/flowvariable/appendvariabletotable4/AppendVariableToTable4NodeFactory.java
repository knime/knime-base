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
 *   Feb 7, 2025 (Martin Sillye, TNG Technology Consulting GmbH): created
 */
package org.knime.base.node.flowvariable.appendvariabletotable4;

import org.knime.core.node.port.flowvariable.FlowVariablePortObject;
import org.knime.core.webui.node.impl.WebUINodeConfiguration;
import org.knime.core.webui.node.impl.WebUINodeFactory;

/**
 * WebUI node factory for the "Variable to Table Column" node.
 *
 * @author Martin Sillye, TNG Technology Consulting GmbH
 */
@SuppressWarnings("restriction")
public class AppendVariableToTable4NodeFactory extends WebUINodeFactory<AppendVariableToTable4NodeModel> {

    /**
    * Default constructor.
    */
    public AppendVariableToTable4NodeFactory() {
        super(CONFIGURATION);
    }

    @Override
    public AppendVariableToTable4NodeModel createNodeModel() {
        return new AppendVariableToTable4NodeModel(CONFIGURATION);
    }

    private static final WebUINodeConfiguration CONFIGURATION = WebUINodeConfiguration.builder() //
        .name("Variable to Table Column") //
        .icon("append_variable.png") //
        .shortDescription("Appends one or more variables as new column(s) to the data table.") //
        .fullDescription("""
                Extracts variables that are carried along the flow and appends them to an input table.
                """) //
        .modelSettingsClass(AppendVariableToTable4NodeSettings.class) //
        .addExternalResource("""
                https://www.knime.com/self-paced-course/l2-ds-knime-analytics-platform-for-data-scientists\
                -advanced/lesson2
                """, //
            "KNIME Analytics Platform for Data Scientists (Advanced): Lesson 2. Flow Variables and Components") //
        .nodeType(NodeType.Other) //
        .addInputPort("Flow variables", FlowVariablePortObject.TYPE_OPTIONAL, "One or more flow variables (optional)")
        .addInputTable("Data table", "Data table to which the flow variables are appended as columns")
        .addOutputTable("Input table with additional columns", """
                The input table with additional columns, one for each selected variable. All values in the new \
                column will be the same.
                """) //
        .keywords("variable", "variables", "convert", "column") //
        .build();
}
