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
 *   23 Jun 2023 (carlwitt): created
 */
package org.knime.base.node.preproc.stringreplacer;

import org.knime.core.webui.node.dialog.defaultdialog.widget.Label;

/**
 * @since 5.1
 */
@SuppressWarnings("restriction")
public enum ReplacementStrategy {
        /** Output the replacement pattern as is. */
        @Label("Whole string")
        WHOLE_STRING,

        /** Replace all matches of the search pattern with the replacement pattern. */
        @Label("All occurrences")
        ALL_OCCURRENCES;

    /** Recommended default setting. */
    public static final ReplacementStrategy DEFAULT = WHOLE_STRING;

    /** Displayed in dialogs as title for controls. */
    public static final String OPTION_NAME = "Replacement strategy";

    /** Displayed in dialogs as help text on controls. */
    public static final String OPTION_DESCRIPTION = """
            Select what to replace in case a string matches a pattern.
            <ul>
                <li>
                    <i>Whole string</i> replaces the entire string with the replacement string, requiring an
                    exact  match of the whole string.
                </li>
                <li>
                    <i>All occurrences</i> replaces all occurrences of the pattern with the replacement string.
                    Note that when e.g. matching on the RegEx-pattern <tt>.*</tt>, an empty string at the end
                    of the input is also matched and replaced. To avoid that, use e.g. the pattern <tt>^.*</tt>
                    to indicate that the match has to start at the beginning of the string.
                </li>
            </ul>
            """;
}