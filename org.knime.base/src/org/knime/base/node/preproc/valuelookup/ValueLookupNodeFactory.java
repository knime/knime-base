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
 *   21 Oct 2022 (jasper): created
 */
package org.knime.base.node.preproc.valuelookup;

import org.knime.core.node.NodeFactory;
import org.knime.core.webui.node.dialog.impl.WebUINodeConfiguration;
import org.knime.core.webui.node.dialog.impl.WebUINodeFactory;

/**
 * {@link NodeFactory} for the Value Lookup node, which looks up values in a dictionary table and adds them to an input
 * table
 *
 * @author Jasper Krauter, KNIME GmbH, Konstanz, Germany
 */
@SuppressWarnings("restriction") // New Node UI is not yet API
public final class ValueLookupNodeFactory extends WebUINodeFactory<ValueLookupNodeModel> {

    private static final WebUINodeConfiguration CONFIG = WebUINodeConfiguration.builder()//
        .name("Value Lookup")//
        .icon("./value_lookup.png")//
        .shortDescription("Uses a dictionary table to look up values and append the corresponding cells to a row.")//
        .fullDescription("The node has two inputs: a data table and a dictionary table. <br/>"
            + "From the data table select the column that is used to look up values in the dictionary table. <br/>"
            + "From the dictionary table select a column that contains the search keys or criteria. <br/>"
            + "When a lookup value matches an entry in the dictionary, cells from that row are appended to the data "
            + "table. In case multiple rows match you can choose if you want to use the first match or the last match. "
            + "If no rows match you can choose to insert missing values, match the next smaller value or match "
            + "the next larger value. <br/>"
            + "Missing values are treated as ordinary values, i.e. they are valid as lookup and replacement value. The "
            + "key column of the dictionary can also be a collection type. Then, the values in the collection act as "
            + "alternative lookup values for the associated row. <br/>"
            + "<br/>"
            + "In the output section you can select the columns in the dictionary table that shall be inserted in the "
            + "output data table.")//
        .modelSettingsClass(ValueLookupNodeSettings.class)//
        .addInputTable("Data Table", "The data table has a column that contains lookup values")//
        .addInputTable("Dictionary Table",
            "The dictionary table has a key column and value columns that will be inserted into the data table")//
        .addOutputTable("Data Table with additional columns",
            "The output table is the data table but with the added values from the dictionary table")//
        .keywords(
            "Cell replacer" // deprecated node replaced by this node, let users find the replacement node easily
        )
        .sinceVersion(5, 0, 0)
        .build();

    /**
     * Create a new factory instance (need this constructor for ser/de)
     */
    public ValueLookupNodeFactory() {
        super(CONFIG);
    }

    /**
     * Create a new factory instance provided a node configuration
     *
     * @param configuration
     */
    protected ValueLookupNodeFactory(final WebUINodeConfiguration configuration) {
        super(configuration);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ValueLookupNodeModel createNodeModel() {
        return new ValueLookupNodeModel(CONFIG, ValueLookupNodeSettings.class);
    }

}
