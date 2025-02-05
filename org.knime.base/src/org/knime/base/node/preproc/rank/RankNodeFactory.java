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
 *   Feb 5, 2025 (Martin Sillye, TNG Technology Consulting GmbH): created
 */
package org.knime.base.node.preproc.rank;

import org.knime.core.webui.node.impl.WebUINodeConfiguration;
import org.knime.core.webui.node.impl.WebUINodeFactory;

/**
 * WebUI node factory for the "Rank" node.
 *
 * @author Martin Sillye, TNG Technology Consulting GmbH
 */
@SuppressWarnings("restriction")
public final class RankNodeFactory extends WebUINodeFactory<RankNodeModel> {

    /**
     * Default constructor
     */
    public RankNodeFactory() {
        super(CONFIGURATION);
    }

    @Override
    public RankNodeModel createNodeModel() {
        return new RankNodeModel(CONFIGURATION);
    }

    private static final WebUINodeConfiguration CONFIGURATION = WebUINodeConfiguration.builder() //
        .name("Rank") //
        .icon("rank.png") //
        .shortDescription("""
                Calculates rankings for the selected groups based on the selected ranking attribute(s) and \
                ranking mode.
                """) //
        .fullDescription("""
                    <p>
                        This node assigns ranks to rows based on user-defined ordering criteria. The ranking is \
                        determined by selecting one or more columns, with the option to specify ascending or \
                        descending order. If multiple criteria are set, the table is first sorted by the primary \
                        criterion, and subsequent criteria are used only in the case of ties.
                    </p>
                    <p>
                        Additionally, ranking can be performed within groups by selecting one or more category \
                        columns, where each unique combination of category values defines a separate ranking group.
                    </p>
                """) //
        .modelSettingsClass(RankNodeSettings.class) //
        .nodeType(NodeType.Manipulator) //
        .addInputTable("Data", "Data that is to be ranked") //
        .addOutputTable("Ranked Data", "Table containing an additional rank column") //
        .keywords("rank", "ranking", "attributes", "group", "grouping") //
        .build();
}
