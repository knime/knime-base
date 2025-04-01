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
 * ---------------------------------------------------------------------
 *
 * History
 *   18.06.2007 (thor): created
 */
package org.knime.base.node.preproc.stringreplacer;

import org.knime.core.webui.node.impl.WebUINodeConfiguration;
import org.knime.core.webui.node.impl.WebUINodeFactory;

/**
 * This is the factory for the string replacer node that creates all necessary objects.
 *
 * @author Thorsten Meinl, University of Konstanz
 * @author Carsten Haubold, KNIME GmbH, Konstanz, Germany
 */
@SuppressWarnings("restriction")
public class StringReplacerNodeFactory extends WebUINodeFactory<StringReplacerNodeModel> {

    private static final WebUINodeConfiguration CONFIGURATION = WebUINodeConfiguration.builder() //
        .name("String Replacer") //
        .icon("./stringreplace.png") //
        .shortDescription("Replaces strings if they match a certain pattern.") //
        .fullDescription("""
                    This node replaces strings in a selected target column.
                In the configuration dialog, a pattern and a replacement text are specified.
                If the pattern doesn't match, the result string equals the input string.
                You can choose to modify strings in-place or add the result strings to a new column.
                    """) //
        .modelSettingsClass(StringReplacerNodeSettings.class) //
        .nodeType(NodeType.Manipulator) //
        .addInputTable("Input", "The input table contains the string column to perform the replacement on.") //
        .addOutputTable("Input with replaced strings",
            "Input table with updated string column or an additional column.") //
        .keywords("RegEx", "Find replace") //
        .build();

    /**
     * Create a new {@link StringReplacerNodeFactory}
     */
    public StringReplacerNodeFactory() {
        super(CONFIGURATION);
    }

    /**
     * @since 5.5
     */
    @Override
    public StringReplacerNodeModel createNodeModel() {
        return new StringReplacerNodeModel(CONFIGURATION);
    }
}
