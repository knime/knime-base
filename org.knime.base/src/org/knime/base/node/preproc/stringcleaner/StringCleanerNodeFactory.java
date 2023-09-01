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
package org.knime.base.node.preproc.stringcleaner;

import org.knime.core.node.NodeFactory;
import org.knime.core.webui.node.impl.WebUINodeConfiguration;
import org.knime.core.webui.node.impl.WebUINodeFactory;

/**
 * {@link NodeFactory} for the String Cleaner node.
 *
 * @author Jasper Krauter, KNIME GmbH, Konstanz, Germany
 * @since 5.2
 */
@SuppressWarnings("restriction") // New Node UI is not yet API
public final class StringCleanerNodeFactory extends WebUINodeFactory<StringCleanerNodeModel> {

    private static final WebUINodeConfiguration CONFIG = WebUINodeConfiguration.builder()//
        .name("String Cleaner")//
        .icon("stringcleaner.png")//
        .shortDescription("The String Cleaner provides tools for basic string cleaning")//
        .fullDescription("""
                The String Cleaner node provides tools for basic string cleaning operations like removing whitespace, \
                removing punctuation or padding. For more complex string manipulation operations, the String \
                Manipulation node should be used.
                All operations in this node are applied exactly in the order as they appear in the node dialog.
                The node is unicode-compatible. For reference on what characters are included in a category, see e.g. \
                the Wikipedia guides to \
                <a href="https://en.wikipedia.org/wiki/Unicode_character_property#General_Category">Character \
                categories</a> and <a href="https://en.wikipedia.org/wiki/Whitespace_character">Whitespace</a>.
                """)//
        .modelSettingsClass(StringCleanerNodeSettings.class)//
        .addInputTable("Input table", "Input table containing string columns")//
        .addOutputTable("Output table", "Output table with the selected columns modified")//
        .nodeType(NodeType.Manipulator)//
        .keywords("Manipulation", "Case", "Whitespace", "Pad", "Punctuation", "Character")//
        .sinceVersion(5, 2, 0)//
        .build();

    /**
     * Create a new factory instance (need this constructor for ser/de)
     */
    public StringCleanerNodeFactory() {
        super(CONFIG);
    }

    /**
     * Create a new factory instance provided a node configuration
     *
     * @param configuration
     */
    protected StringCleanerNodeFactory(final WebUINodeConfiguration configuration) {
        super(configuration);
    }

    @Override
    public StringCleanerNodeModel createNodeModel() {
        return new StringCleanerNodeModel(CONFIG, StringCleanerNodeSettings.class);
    }

}
