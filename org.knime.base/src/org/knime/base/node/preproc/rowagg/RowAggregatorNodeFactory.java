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
 *   19 Dec 2022 (Manuel Hotz, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.base.node.preproc.rowagg;

import org.knime.core.webui.node.dialog.impl.WebUINodeConfiguration;
import org.knime.core.webui.node.dialog.impl.WebUINodeFactory;

/**
 * NodeFactory of the Row Aggregator node.
 *
 * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
 */
@SuppressWarnings("restriction")
public final class RowAggregatorNodeFactory extends WebUINodeFactory<RowAggregatorNodeModel> {

    private static final WebUINodeConfiguration CONFIG = WebUINodeConfiguration.builder()//
        .name("Row Aggregator")//
        .icon("./Row-aggregator.png")//
        .shortDescription("Aggregate frequency columns.")//
        .fullDescription("Aggregate frequency columns using an aggregation function. "
            + "The rows can optionally be grouped by a category column. Some aggregation functions "
            + "support using a weight from a weight column."
            + "<br/>"
            + "If you need more sophisticated functionality, you can use the "
            + "<a href=\"https://hub.knime.com/knime/extensions/org.knime.features.base/latest/org.knime.base.node."
            + "preproc.groupby.GroupByNodeFactory\">GroupBy</a> node.")//
        .modelSettingsClass(RowAggregatorSettings.class)//
        .addInputTable("Input Table", "The table to aggregate.")//
        .addOutputTable("Aggregated table", "The aggregated table.")//
        .addOutputTable("Totals table", "Contains the single-row table with \"grand total\" values if, and only if, "
            + "the corresponding output setting is enabled and a category column is selected. Otherwise, the output is "
            + "inactive.")
        .keywords("Group by")
        .build();

    /**
     * Constructor.
     */
    public RowAggregatorNodeFactory() {
        super(CONFIG);
    }

    @Override
    public RowAggregatorNodeModel createNodeModel() {
        return new RowAggregatorNodeModel(CONFIG);
    }
}
