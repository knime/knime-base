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
 *   22 Nov 2023 (jasper): created
 */
package org.knime.base.node.preproc.regexsplit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.knime.base.node.preproc.regexsplit.CaptureGroupExtractor.CaptureGroup;
import org.knime.base.node.preproc.regexsplit.RegexSplitNodeSettings.CaseMatching;
import org.knime.core.node.InvalidSettingsException;

/**
 * Tests the {@link RegexSplitter}.
 *
 * @author Jasper Krauter, KNIME GmbH, Konstanz, Germany
 * @author Carl Witt, KNIME AG, Zurich, Switzerland
 */
@SuppressWarnings("javadoc")
class RegexSplitterTest {

    @Test
    void testBasicSplitting() throws InvalidSettingsException {
        final var splitter = RegexSplitter.fromSettings(getSettings("(a+)(b)?", true));
        assertTrue(splitter.apply("").isEmpty(), "Empty string should not match");
        assertEquals(List.of(Optional.of("a"), Optional.empty()), splitter.apply("a").orElseThrow(),
            "There should be two capture groups but only one capture, a.");
        assertEquals(List.of(Optional.of("a"), Optional.of("b")), splitter.apply("ab").orElseThrow(),
            "There should be the two captures a and b");
        assertTrue(splitter.apply("aba").isEmpty(), "aba should not match since we require the whole string to match");

        final var splitter2 = RegexSplitter.fromSettings(getSettings("(a+)(b)?", false));
        assertTrue(splitter2.apply("").isEmpty(), "Empty string should not match because we need at least one a");
        assertEquals(List.of(Optional.of("a"), Optional.empty()), splitter2.apply("a").orElseThrow(),
            "There should be two capture groups but only one capture, a.");
        assertEquals(List.of(Optional.of("a"), Optional.of("b")), splitter2.apply("ab").orElseThrow(),
            "There should be the two captures a and b");
        assertEquals(List.of(Optional.of("a"), Optional.of("b")), splitter2.apply("aba").orElseThrow(),
            "aba should match because whole string matching is not required");

        final var splitter3 = RegexSplitter.fromSettings(getSettings("([A-Za-z]{3})(\\d+)?", true));
        assertTrue(splitter3.apply("").isEmpty(), "Empty string should not match");
        assertEquals(List.of(Optional.of("abc"), Optional.empty()), splitter3.apply("abc").orElseThrow(),
            "There should be two capture groups but only one capture, abc.");
        assertEquals(List.of(Optional.of("xyz"), Optional.of("123")), splitter3.apply("xyz123").orElseThrow(),
            "There should be the two captures xyz and 123");
        assertTrue(splitter3.apply("aBc123").isPresent(), "aBc123 should match");
        assertTrue(splitter3.apply("abcd123").isEmpty(),
            "abcd123 should not match since we require the whole string to match");

        final var splitter4 = RegexSplitter.fromSettings(getSettings("([A-Za-z]{3})(\\d+)?", false));
        assertEquals(List.of(Optional.of("abc"), Optional.empty()), splitter4.apply("abcd123").orElseThrow(),
            "abcd123 should partially match");
    }

    @Test
    void testLookbehindMatching() throws InvalidSettingsException {
        final var splitter = RegexSplitter.fromSettings(getSettings("(?<!black )(cat)", false));
        assertTrue(splitter.apply("The black cat is not matched, no black cat is.").isEmpty(), "black cat should not match");
        assertEquals(List.of(Optional.of("cat")), splitter.apply("The black cat is not matched, the red cat is.").orElseThrow(),
            "There should be one capture group with cat");
    }

    @Test
    void testCaptureGroupCounting() throws InvalidSettingsException {
        assertGroups("");
        assertGroups("()", cg(1));
        assertGroups("a()b()", cg(1), cg(2));
        assertGroups("(\\d{4})-(\\d{2})-(\\d{2})", cg(1), cg(2), cg(3));
        assertGroups("^(?<username>[a-zA-Z0-9._%+-]+)@(?<domain>[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,})$", //
            cg(1, "username"), cg(2, "domain"));
        assertGroups("<(?<tag>\\w+)(\\s+(?<attribute>\\w+)\\s*=\\s*['\"](?<value>[^'\"]*)['\"])*\\s*/?>", //
            cg(1, "tag"), cg(2), cg(3, "attribute"), cg(4, "value"));
        assertGroups("(?<a>(?<b>(?<c>(?<d>)(?<e>))(?<f>))(?<g>))", //
            cg(1, "a"), cg(2, "b"), cg(3, "c"), cg(4, "d"), cg(5, "e"), cg(6, "f"), cg(7, "g"));
        assertGroups(
            "(?<octet1>\\b(?:\\d{1,2}|1\\d{2}|2[0-4]\\d|25[0-5])\\b)"
                + "(?:\\.(?<octet2>\\b(?:\\d{1,2}|1\\d{2}|2[0-4]\\d|25[0-5])\\b)){3}", //
            cg(1, "octet1"), cg(2, "octet2"));
        assertGroups("^\\+(?<countryCode>\\d{1,3})-(?<areaCode>\\d{1,5})-(?<number>\\d{1,10})$", //
            cg(1, "countryCode"), cg(2, "areaCode"), cg(3, "number"));
        assertGroups(
            "^(?<cardNumber>\\d{4}-\\d{4}-\\d{4}-\\d{4})\\s+Exp\\s+(?<expiryMonth>\\d{2})/(?<expiryYear>\\d{2})$", //
            cg(1, "cardNumber"), cg(2, "expiryMonth"), cg(3, "expiryYear"));
        assertGroups("^(?<protocol>https?)://(?<domain>[\\w.-]+)(?:/(?<path>[\\w/]+)?)?$", //
            cg(1, "protocol"), cg(2, "domain"), cg(3, "path"));
        assertGroups("(\\d(\\w(\\d)))", cg(1), cg(2), cg(3));
        assertGroups("(a|b)?", cg(1));
        assertGroups("(x)+", cg(1));
        assertGroups("(?<=@)\\w+(?=\\.)");
        assertGroups("(?:\\d+)-(\\w+)", cg(1));
        assertGroups("(?<name>\\d+)-(\\w+)", cg(1, "name"), cg(2));
        assertGroups("(?<name>\\d+)-(?<NAME>\\w+)", cg(1, "name"), cg(2, "NAME"));
        assertGroups("(\\w+)\\s\\1", cg(1));
        assertGroups("(\\d+)-(\\d+)", cg(1), cg(2));
        assertGroups("^[A-Za-z]{3}(?:(?=.*&)\".*\"|(?!.*&).*)$");
        assertGroups("^[a-z]{3}([^&]*$|\".*\"$)", cg(1));
    }

    /**
     * Tests the counting of non-capturing groups.
     * @throws InvalidSettingsException
     */
    @Test
    void testNonCapturingGroupCounting() throws InvalidSettingsException {
        // (?idmsux-idmsux:X)      X, as a non-capturing group with the given flags i d m s u x on - off
        assertGroups("(?idmsux-idmsux:X)");
        // (?:X)   X, as a non-capturing group
        assertGroups("(?:X)");
        // (?>X)   X, as an independent, non-capturing group
        assertGroups("(?>X)");
    }

    @Test
    void testCaptureGroupCountingWithEscapedParentheses() throws InvalidSettingsException {
        // The outer parentheses are literal parentheses \(escaped\)
        assertGroups("\\(escaped\\)");
        // escape parenthesis via range quoting
        // pattern \Q(\E no group \Q)\E
        assertGroups("\\Q(\\E no group \\Q)\\E");

        // pattern \(?<notAGroupBecause>escaped\)
        assertGroups("\\(?<notAGroupBecause>escaped\\)");
        // The outer parentheses are literal parentheses,
        // the inner parentheses constitute a capture group since the backslashes are also escaped
        // pattern \(\\(escaped\\)\)
        assertGroups("\\(\\\\(escaped\\\\)\\)", cg(1));
        // inner parentheses are escaped
        // pattern (\(\d+\))-(\w+)
        assertGroups("(\\(\\d+\\))-(\\w+)", cg(1), cg(2));
        // only one outer group
        // pattern (a|\(b\))
        assertGroups("(a|\\(b\\))", cg(1));
        // pattern (\(\w+\))+
        assertGroups("(\\(\\w+\\))+", cg(1));
        // pattern (?<=\(a\))\d+(?=\(b\))
        assertGroups("(?<=\\(a\\))\\d+(?=\\(b\\))");
        assertGroups("(\\w+)\\s\\1", cg(1));
        assertGroups("(a)?(?:\\(?<esc>b\\)|c)", cg(1));
        // one named group, one non-capturing group (?:X)
        // pattern (?<one>a)?(?:\(?<esc>b\)|c)
        assertGroups("(?<one>a)?(?:\\(?<esc>b\\)|c)", cg(1, "one"));
        assertGroups("(?:\\(\\d+\\))-(\\w+)", cg(1));
        assertGroups("[\\(\\)](?<x>\\w+)[\\(\\)]", cg(1, "x"));
        assertGroups("(?<=\\(\\w+\\))-(\\w+)(?=\\(\\d+\\))", cg(1));
    }

    @Test
    void testISEOnInvalidFlags() throws InvalidSettingsException {
        assertThrows(InvalidSettingsException.class,
            () -> RegexSplitter.fromSettings(getSettings("abc", false, Pattern.COMMENTS)),
            "Comments flag is not supported");
        assertThrows(InvalidSettingsException.class,
            () -> RegexSplitter.fromSettings(getSettings("abc", false, Pattern.LITERAL)),
            "Comments flag is not supported");
        // This shouldn't throw
        RegexSplitter
            .fromSettings(getSettings("abc", false, Pattern.CANON_EQ | Pattern.CASE_INSENSITIVE | Pattern.DOTALL
                | Pattern.MULTILINE | Pattern.UNICODE_CASE | Pattern.UNICODE_CHARACTER_CLASS | Pattern.UNIX_LINES));
    }

    // ========== UTIL ==========

    static CaptureGroup cg(final int i) {
        return cg(i, null);
    }

    static CaptureGroup cg(final int i, final String s) {
        return new CaptureGroup(i, Optional.ofNullable(s));
    }

    void assertGroups(final String pattern, final CaptureGroup... groups) throws InvalidSettingsException {
        final var splitter = RegexSplitter.fromSettings(getSettings(pattern, true));
        final var expected = List.of(groups);
        assertEquals(expected, splitter.getCaptureGroups(), "The Groups for " + pattern + " are counted correctly");
    }

    private static RegexSplitNodeSettings getSettings(final String pattern, final boolean requireWholeMatch) {
        return getSettings(pattern, requireWholeMatch, 0);
    }

    @SuppressWarnings("deprecation") // set the isLiteral Setting
    private static RegexSplitNodeSettings getSettings(final String pattern, final boolean requireWholeMatch,
        final int flags) {
        final var s = new RegexSplitNodeSettings();
        s.m_pattern = pattern;
        s.m_requireWholeMatch = requireWholeMatch;
        s.m_isUnixLines = (flags & Pattern.UNIX_LINES) != 0;
        s.m_caseMatching =
            (flags & Pattern.CASE_INSENSITIVE) != 0 ? CaseMatching.CASEINSENSITIVE : CaseMatching.CASESENSITIVE;
        s.m_isComments = (flags & Pattern.COMMENTS) != 0;
        s.m_isMultiLine = (flags & Pattern.MULTILINE) != 0;
        s.m_isLiteral = (flags & Pattern.LITERAL) != 0;
        s.m_isDotAll = (flags & Pattern.DOTALL) != 0;
        s.m_isUnicodeCase = (flags & Pattern.UNICODE_CASE) != 0;
        s.m_isCanonEQ = (flags & Pattern.CANON_EQ) != 0;
        s.m_isUnicodeCharacterClass = (flags & Pattern.UNICODE_CHARACTER_CLASS) != 0;
        return s;
    }
}
