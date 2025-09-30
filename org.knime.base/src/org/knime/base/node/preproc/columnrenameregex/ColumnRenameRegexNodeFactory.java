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
 *   Mar 19, 2025 (david): created
 */
package org.knime.base.node.preproc.columnrenameregex;

import org.knime.core.webui.node.impl.WebUINodeConfiguration;
import org.knime.core.webui.node.impl.WebUINodeFactory;

/**
 * The factory for the Column Name Replacer node (formerly Column Rename (Regex)).
 *
 * @author David Hickey, TNG Technology Consulting GmbH
 */
@SuppressWarnings("restriction")
public final class ColumnRenameRegexNodeFactory extends WebUINodeFactory<ColumnNameReplacerNodeModel> {

    /**
     * Constructor for the node factory.
     */
    public ColumnRenameRegexNodeFactory() {
        super(CONFIGURATION);
    }

    /**
     * @since 5.5
     */
    @Override
    public ColumnNameReplacerNodeModel createNodeModel() {
        return new ColumnNameReplacerNodeModel(CONFIGURATION);
    }

    /**
     * @since 5.5
     */
    public static final String SHORT_DESCRIPTION =
        "Renames all columns based on a regular expression search &amp; replace pattern.";

    /**
     * @since 5.5
     */
    public static final String FULL_DESCRIPTION = """
              <p>
              Renames all columns based on a search \
              &amp; replace pattern. The search pattern is a regular expression, \
              literal, or wildcard expression.
            </p>
            <p>
             In the simplest case, you can search and replace string literals.
             E.g. if the input columns are called "Foo 1", "Foo 2", "Foo 3",
             etc and the search string is "Foo", the replacement is "Bar", the
             output would be "Bar 1", "Bar 2", "Bar 3".
            </p>
            <p>
              More complicated cases contain <a href=\
              "http://download.oracle.com/javase/6/docs/api/java/util/regex/Pattern.html#cg">
              capturing groups</a>, i.e. expressions in parentheses that, if
              matched in a column name, are saved. The groups can be referenced
              in the replacement string using <tt>$g</tt>, whereby <tt>g</tt>
              is a number 0-9. These placeholders will be replaced by the
              original occurrence in the input column name.
              For instance, to rename the columns that are produced by the
              Data Generator node (they follow a scheme
              <tt>Universe_&lt;number1&gt;_&lt;number2&gt;</tt>) to
              <tt>&lt;number2&gt; (Uni &lt;number1&gt;)</tt>, you would use as
              search string: "Universe_(\\d+)_(\\d+)" and as replacement:
              "$2 (Uni $1)".
            </p>
            <p>
              The special sequence <tt>$i</tt> represents the current column
              index (unless escaped by '\\' (backslash)). E.g. in order to
              precede each column name with the column index, use as search
              string "(^.+$)", capturing the entire column name in a group,
              and as replacement "$i: $1".
            </p>
            <p>
              Further documentation regarding regular expressions can be found
              in the Java API documentation, in particular the classes <a href=\
              "http://download.oracle.com/javase/6/docs/api/java/util/regex/Pattern.html">
              Pattern</a> and <a href=\
              "http://download.oracle.com/javase/6/docs/api/java/util/regex/Matcher.html">
              Matcher</a>.
            </p>
            """;

    static final WebUINodeConfiguration CONFIGURATION = WebUINodeConfiguration.builder() //
        .name("Column Name Replacer") //
        .icon("column_rename_regex.png") //
        .shortDescription(SHORT_DESCRIPTION) //
        .fullDescription(FULL_DESCRIPTION) //
        .modelSettingsClass(ColumnNameReplacerNodeParametersWithLegacyReplacementStrategy1.class) //
        .keywords("regex", "replace", "rename", "column", "Column Rename (Regex)", "Column Rename (Replace)") //
        .addInputTable("Input table", "The table with columns to rename.") //
        .addOutputTable("Output table", "The table with renamed columns.") //
        .build();
}
