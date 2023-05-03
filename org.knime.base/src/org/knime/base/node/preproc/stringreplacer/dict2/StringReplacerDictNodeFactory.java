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
package org.knime.base.node.preproc.stringreplacer.dict2;

import org.knime.core.node.NodeFactory;
import org.knime.core.webui.node.impl.WebUINodeConfiguration;
import org.knime.core.webui.node.impl.WebUINodeFactory;

/**
 * {@link NodeFactory} for the String Replacer (Dictionary) node
 *
 * @author Jasper Krauter, KNIME GmbH, Konstanz, Germany
 */
@SuppressWarnings("restriction") // New Node UI is not yet API
public final class StringReplacerDictNodeFactory extends WebUINodeFactory<StringReplacerDictNodeModel> {

    private static final WebUINodeConfiguration CONFIG = WebUINodeConfiguration.builder()//
        .name("String Replacer (Dictionary)")//
        .icon("./string_replace_dict.png")//
        .shortDescription(
            "Uses a dictionary table to look up patterns and replacements and modifies strings accordingly.")//
        .fullDescription("""
            This node replaces strings in selected columns.
            The dictionary table provides the patterns to search for and the values to replace matches with.
            The pattern column in the dictionary can be a collection, with multiple patterns for each replacement value.
            If no pattern matches, the result string equals the input string.
            You can choose to modify strings in-place or add the result strings in new columns.<br/>
            The node uses Java regular expressions, see the
            <a href="http://docs.oracle.com/javase/6/docs/api/java/util/regex/Pattern.html">Java API</a> for details.
            """)//
        .modelSettingsClass(StringReplacerDictNodeSettings.class)//
        .addInputTable("Data Table", "The data table contains the string columns to perform the replacement on.")//
        .addInputTable("Dictionary Table",
            "The dictionary table has a pattern column and a replacement column that will be used to perform the "
                + "replacements")//
        .addOutputTable("Data Table with updated columns", """
            The dictionary table has a pattern column that defines what to search for and a replacement column
            that defines what to replace matches with.
            """)//
        .keywords("Dictionary", "String", "Substitute", "Find", "Replace", "Regular Expression", "RegEx", "Wildcard")
        .sinceVersion(5, 1, 0)//
        .build();

    /**
     * Create a new factory instance (need this constructor for ser/de)
     */
    public StringReplacerDictNodeFactory() {
        super(CONFIG);
    }

    /**
     * Create a new factory instance provided a node configuration
     *
     * @param configuration
     */
    protected StringReplacerDictNodeFactory(final WebUINodeConfiguration configuration) {
        super(configuration);
    }

    @Override
    public StringReplacerDictNodeModel createNodeModel() {
        return new StringReplacerDictNodeModel(CONFIG, StringReplacerDictNodeSettings.class);
    }

}
