/*
 * ------------------------------------------------------------------------
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
 *
 * History
 *   10.05.2012 (kilian): created
 */
package org.knime.base.node.meta.looper.group;

import org.knime.core.webui.node.impl.WebUINodeConfiguration;
import org.knime.core.webui.node.impl.WebUINodeFactory;

/**
 *
 * @author Kilian Thiel, KNIME.com, Berlin, Germany
 */
@SuppressWarnings("restriction")
public class GroupLoopStartNodeFactory extends WebUINodeFactory<GroupLoopStartNodeModel> {

    private static final WebUINodeConfiguration CONFIG = WebUINodeConfiguration.builder()//
        .name("Group Loop Start")//
        .icon("./grouploopstart.png")//
        .shortDescription("""
                When starting a loop with this node, each iteration processes another
                group of rows.
                """) //
        .fullDescription("""
                When starting a loop with this node, each iteration processes another
                group of rows. Specify the column(s) to group on and the input data table
                is sorted based on the specified columns before looping starts, by default.
                Sorting can be switched off if input
                data table is already properly sorted based on the columns to group
                on. If sorting is switched off, but input table is not properly
                sorted execution will fail.
                """) //
        .modelSettingsClass(GroupLoopStartNodeSettings.class)//
        .addInputTable("Any Table", "Any input table. Each iteration will process one group of this table.")//
        .addOutputTable("Grouped Input", "The current group of the input table.")//
        .addExternalResource("https://docs.knime.com/latest/analytics_platform_flow_control_guide/index.html#loops",
            "KNIME Flow Control Guide: Section Loops")//
        .nodeType(NodeType.LoopStart)//
        .sinceVersion(5, 3, 0).build();

    /**
     * Create a new {@link GroupLoopStartNodeFactory}
     */
    public GroupLoopStartNodeFactory() {
        super(CONFIG);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GroupLoopStartNodeModel createNodeModel() {
        return new GroupLoopStartNodeModel(CONFIG);
    }
}
