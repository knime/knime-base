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
 *   27 Aug 2024 (Manuel Hotz, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.base.node.preproc.filter.row3.predicates;

import java.util.function.Predicate;
import java.util.regex.Pattern;

import org.knime.filehandling.core.util.WildcardToRegexUtil;

/**
 * String predicate supporting equality comarison or pattern matching.
 *
 * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
 */
final class StringPredicate implements Predicate<String> {

    private final Predicate<String> m_predicate;

    private StringPredicate(final Predicate<String> predicate) {
        m_predicate = predicate;
    }

    @Override
    public boolean test(final String str) {
        return m_predicate.test(str);
    }

    /**
     * Creates an equality string predicate.
     *
     * @param referenceValue value to compare with
     * @param isCaseSensitive whether the comparison should be case-sensitive or not
     * @return equality string predicate
     */
    static StringPredicate equality(final String referenceValue, final boolean isCaseSensitive) {
        final Predicate<String> predicate = isCaseSensitive ? referenceValue::equals : referenceValue::equalsIgnoreCase;
        return new StringPredicate(predicate);
    }

    /**
     * Creates a string predicate that supports regex or wildcard patterns.
     *
     * @param pattern pattern to match
     * @param isRegex {@code true} if the pattern represents a regex, {@code false} if it represents a wildcard
     * @param isCaseSensitive whether the match should be case-sensitive or not
     * @return pattern string predicate
     */
    static StringPredicate pattern(final String pattern, final boolean isRegex,
        final boolean isCaseSensitive) {
        final var regexPattern = isRegex ? pattern : WildcardToRegexUtil.wildcardToRegex(pattern);
        var flags = Pattern.DOTALL | Pattern.MULTILINE;
        flags |= isCaseSensitive ? 0 : Pattern.CASE_INSENSITIVE;
        final var regex = Pattern.compile(regexPattern, flags);
        return new StringPredicate(stringValue -> regex.matcher(stringValue).matches());
    }

}
