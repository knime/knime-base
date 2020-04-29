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
 *   Apr 29, 2020 (Simon Schmid, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.util;

/**
 * Simple class to convert wildcard patterns into regular expressions.
 *
 * @author Thorsten Meinl, University of Konstanz
 * @author Simon Schmid, KNIME GmbH, Konstanz, Germany
 */
public final class WildcardToRegexUtil {

    /**
     * Converts a wildcard pattern containing '*' and '?' as meta characters into a regular expression.
     *
     * @param wildcard a wildcard expression
     * @return the corresponding regular expression
     */
    public static String wildcardToRegex(final String wildcard) {
        return wildcardToRegex(wildcard, false);
    }

    /**
     * Converts a wildcard pattern containing '*' and '?' as meta characters into a regular expression. Optionally, the
     * backslash can be enabled as escape character for the wildcards. In this case, a backslash has a special meaning
     * and may need to be escaped itself.
     *
     * @param wildcard a wildcard expression
     * @param enableEscaping {@code true} if the wildcards may be escaped (i.e. they loose their special meaning) by
     *            prepending a backslash
     * @return the corresponding regular expression
     */
    public static String wildcardToRegex(final String wildcard, final boolean enableEscaping) {
        StringBuilder buf = new StringBuilder(wildcard.length() + 20);

        for (int i = 0; i < wildcard.length(); i++) {
            char c = wildcard.charAt(i);
            switch (c) {
                case '*':
                    if (enableEscaping && (i > 0) && (wildcard.charAt(i - 1) == '\\')) {
                        buf.append('*');
                    } else {
                        buf.append(".*");
                    }
                    break;
                case '?':
                    if (enableEscaping && (i > 0) && (wildcard.charAt(i - 1) == '\\')) {
                        buf.append('?');
                    } else {
                        buf.append(".");
                    }
                    break;
                case '\\':
                    if (enableEscaping) {
                        buf.append(c);
                        break;
                    }
                case '^':
                case '$':
                case '[':
                case ']':
                case '{':
                case '}':
                case '(':
                case ')':
                case '|':
                case '+':
                case '.':
                    buf.append("\\");
                    buf.append(c);
                    break;
                default:
                    buf.append(c);
            }
        }

        return buf.toString();
    }
}
