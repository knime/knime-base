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
 *   Sep 1, 2008 (wiswedel): created
 */
package org.knime.base.node.preproc.regexsplit;

import org.knime.core.webui.node.impl.WebUINodeConfiguration;
import org.knime.core.webui.node.impl.WebUINodeFactory;

/**
 * Factory for the String Splitter (Regex) node. The node was previously called "Regex Split", therefore the classes
 * carry that name
 *
 * @author wiswedel, University of Konstanz
 * @author Jasper Krauter, KNIME GmbH, Konstanz, Germany
 */
@SuppressWarnings("restriction")
public final class RegexSplitNodeFactory extends WebUINodeFactory<RegexSplitNodeModel> {

    private static final WebUINodeConfiguration CONFIG = WebUINodeConfiguration.builder()//
        .name("String Splitter (Regex)")//
        .icon("./regexsplit.png")//
        .shortDescription("Splits an input string (column) into multiple groups according to a regular expression.")//
        .fullDescription(
            """
                    <p>
                        This node splits the string content of a selected column into logical groups using regular
                        expressions. A capturing group is usually identified by a pair of parentheses, whereby the
                        pattern in such parentheses is a regular expression. Optionally, a group can be named. See
                        <i>Pattern</i> for more information. For each input, the capture groups are the output values.
                        Those can be appended to the table in different ways; by default, every group will correspond to
                        one additional output column.
                    </p>

                    <p>
                        A short introduction to groups and capturing is given
                        in the
                        <a href="https://docs.oracle.com/javase/8/docs/api/java/util/regex/Pattern.html#cg">
                            Java API
                        </a>
                        . Some examples are given below:
                    </p>

                    <h4>Parsing Patent Numbers</h4>
                    <p>
                        Patent identifiers such as "US5443036-X21" consisting of
                        a (at most) two-letter country code ("US"), a patent
                        number ("5443036") and possibly some application code
                        ("X21"), which is separated by a dash or a space
                        character, can be grouped by the expression
                        <tt>([A-Za-z]{1,2})([0-9]+)[ \\-]?(.*$)</tt>.
                        Each of the parenthesized terms corresponds to the
                        aforementioned properties. For named output columns,
                        we can add group names to the pattern:
                        <ul>
                            <li><tt>(?&lt;CC&gt;[A-Za-z]{1,2})</tt> is now identified with "CC" in the output.</li>
                            <li><tt>(?&lt;patentNumber&gt;[0-9]+)</tt> is now identified with "patentNumber".</li>
                            <li><tt>[ \\-]?</tt> is and was never a capturing group so it remains unchanged.</li>
                            <li><tt>(?&lt;applicationCode&gt;.*$)</tt> is now identified with "applicationCode".</li>
                        </ul>
                        Named and unnamed groups can also be mixed in one pattern.
                    </p>

                    <h4>Strip File URLs</h4>
                    <p>
                        This is particularly useful when this node is used to
                        parse the file URL of a file reader node (the URL is
                        exposed as a flow variable and then exported to a table
                        using a Variable to Table node). The format of such
                        URLs is similar to "file:c:\\some\\directory\\foo.csv".
                        Using the pattern
                        <tt>[A-Za-z]*:(.*[/\\\\])(?&lt;filename&gt;([^\\.]*)\\.(.*$))</tt>
                        generates four groups: The first group identifies the directory
                        and is denoted by <tt>(.*[/\\\\])</tt>. It consumes all characters
                        until a final slash or backslash is encountered; in the example,
                        this refers to "c:\\some\\directory\\". The second group
                        represents the file name, whereby it encapsulates the
                        third and fourth group. The third group (<tt>([^\\.]*)</tt>)
                        consumes all characters after the directory,
                        which are not a dot '.' (which is "foo" in the
                        above example). The pattern expects a single dot
                        (final which is ignored) and finally the fourth group <tt>(.*$)</tt>,
                        which reads until the end of the string and indicates
                        the file suffix ('csv'). The groups for the above
                        example are
                        <ol>
                            <li>Group <i>1</i>: c:\\some\\directory</li>
                            <li>Group <i>filename</i>: foo.csv</li>
                            <li>Group <i>3</i>: foo</li>
                            <li>Group <i>4</i>: csv</li>
                        </ol>
                    </p>

                    <h4>Email Address Extraction</h4>
                    <p>
                        Let's consider a scenario where you have a list of email addresses.
                        Using the pattern <tt>(?&lt;username&gt;.+)@(?&lt;domain&gt;.+)</tt>,
                        you can extract the username and domain from the addresses.
                        The groups for the email address "john.doe@example.com" are:
                        <ul>
                            <li>Group <i>username</i>: john.doe</li>
                            <li>Group <i>domain</i>: example.com</li>
                        </ul>
                    </p>
                    """)
        .modelSettingsClass(RegexSplitNodeSettings.class)//
        .addInputTable("Data Table", "Input table with string column to be split.")//
        .addOutputTable("Input with split columns",
            "Input table amended by additional column representing the pattern groups.")//
        .nodeType(NodeType.Manipulator).keywords("Regex Split", "Extract", "Regex", "Pattern", "Group", "Capture")
        .sinceVersion(5, 3, 0).build();

    /** Default constructor (required for instantiation) */
    public RegexSplitNodeFactory() {
        super(CONFIG);
    }

    @Override
    public RegexSplitNodeModel createNodeModel() {
        return new RegexSplitNodeModel(CONFIG);
    }
}
