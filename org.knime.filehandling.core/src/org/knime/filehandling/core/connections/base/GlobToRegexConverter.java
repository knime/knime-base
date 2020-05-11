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
 *   04.02.2020 (Mareike Hoeger, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.connections.base;

import java.nio.file.FileSystem;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Converts a Java glob expression to a RegEx as defined by {@link FileSystem#getPathMatcher(String)} This class is
 * basically a refactoring of the sun.nio.fs.Globs class.
 *
 * @author Mareike Hoeger, KNIME GmbH
 * @since 4.2
 */
public class GlobToRegexConverter {

    private static final String REGEX_META_CHARS = ".^$+{[]|()\\*?";

    private static boolean isRegexMeta(final char c) {
        return REGEX_META_CHARS.indexOf(c) != -1;
    }

    /**
     * Converts a glob pattern to a regex considering the given path separator.
     *
     * @param glob the glob to convert
     * @param pathSeparator the path separator for the file system
     * @return the regex pattern
     */
    public static Pattern convert(final String glob, final char pathSeparator) {
        final StringBuilder regex = new StringBuilder("^");
        boolean inGroup = false;
        int index = 0;
        while (index < glob.length()) {

            final char currentChar = glob.charAt(index);
            index++;
            switch (currentChar) {
                case '\\':
                    if (index == glob.length()) {
                        throw new PatternSyntaxException("No character to escape", glob, index - 1);
                    }
                    index = escapeNextCharacter(glob, regex, index);
                    break;
                case '[':
                    final int closingBracketIndex = glob.indexOf(']', index);
                    if (closingBracketIndex == -1) {
                        throw new PatternSyntaxException("Missing ']", glob, index);
                    }
                    regex.append("[");
                    index = convertBracketExpressionStart(glob, regex, index);
                    convertBracketExpression(glob.substring(index, closingBracketIndex), pathSeparator, regex);
                    index = closingBracketIndex + 1;
                    break;
                case '{':
                    if (inGroup) {
                        throw new PatternSyntaxException("Cannot nest groups", glob, index - 1);
                    }
                    regex.append("(?:(?:");
                    inGroup = true;
                    break;
                case '}':
                    if (inGroup) {
                        //Group end
                        regex.append("))");
                        inGroup = false;
                    } else {
                        //simple curly bracket
                        regex.append('}');
                    }
                    break;
                case ',':
                    if (inGroup) {
                        //Group or
                        regex.append(")|(?:");
                    } else {
                        //simple comma
                        regex.append(',');
                    }
                    break;
                case '*':

                    if (next(glob, index) == '*') {
                        // ** crosses directory boundaries so match any character
                        regex.append(".*");
                        index++;
                        break;
                    }

                    // within directory boundary match all but path separator
                    regex.append("[^");
                    regex.append(escapeChar(pathSeparator));
                    regex.append("]*");

                    break;
                case '?':
                    //Any character but path separator
                    regex.append("[^");
                    regex.append(escapeChar(pathSeparator));
                    regex.append("]");
                    break;

                default:
                    regex.append(escapeChar(currentChar));
            }
        }

        if (inGroup) {
            throw new PatternSyntaxException("Missing '}", glob, index - 1);
        }
        regex.append('$');

        return Pattern.compile(regex.toString());

    }

    private static int convertBracketExpressionStart(final String glob, final StringBuilder regex, int index) {
        if (next(glob, index) == '^') {
            // escape the regex negation char if it appears
            regex.append("\\^");
            index++;
        } else {
            // negation
            if (next(glob, index) == '!') {
                regex.append('^');
                index++;
            }
            // hyphen allowed at start
            if (next(glob, index) == '-') {
                regex.append('-');
                index++;
            }
        }

        return index;
    }

    private static int convertBracketExpression(final String glob, final char pathSeparator,
        final StringBuilder regex) {
        boolean hasRangeStart = false;
        char last = 0;
        int index = 0;
        while (index < glob.length()) {
            char c = glob.charAt(index);
            index++;

            if (c == pathSeparator) {
                throw new PatternSyntaxException("Explicit 'path separator' in class", glob, index - 1);
            }

            final char next = next(glob, index);
            if (c == '\\' || c == '[' || (c == '&' && next == '&')) {
                // escape '\', '[' or "&&" for regex class
                regex.append('\\');
            }

            regex.append(c);

            if (c == '-') {
                if (!hasRangeStart) {
                    throw new PatternSyntaxException("Invalid range", glob, index - 1);
                }

                c = next(glob, index);
                index++;

                if (c < last) {
                    throw new PatternSyntaxException("Invalid range", glob, index - 3);
                }
                regex.append(c);
                hasRangeStart = false;
            } else {
                hasRangeStart = true;
                last = c;
            }
        }
        regex.append("]");
        return index;
    }

    private static char next(final String glob, final int index) {
        if (index >= glob.length()) {
            return '\0';
        }
        return glob.charAt(index);
    }

    private static int escapeNextCharacter(final String glob, final StringBuilder regex, int index) {

        final char next = glob.charAt(index);
        index++;
        if (isRegexMeta(next)) {
            regex.append('\\');
        }
        regex.append(next);
        return index;
    }

    private static String escapeChar(final char character) {
        final StringBuilder escaped = new StringBuilder();

        if (isRegexMeta(character)) {
            escaped.append('\\');
        }
        escaped.append(character);

        return escaped.toString();
    }

}
