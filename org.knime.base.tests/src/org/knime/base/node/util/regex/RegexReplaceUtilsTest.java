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
 *   May 6, 2025 (david): created
 */
package org.knime.base.node.util.regex;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.knime.base.node.util.regex.CaseMatching.CASEINSENSITIVE;
import static org.knime.base.node.util.regex.CaseMatching.CASESENSITIVE;
import static org.knime.base.node.util.regex.PatternType.LITERAL;
import static org.knime.base.node.util.regex.PatternType.REGEX;
import static org.knime.base.node.util.regex.PatternType.WILDCARD;
import static org.knime.base.node.util.regex.RegexReplaceTestCaseBuilder.builder;
import static org.knime.base.node.util.regex.ReplacementStrategy.ALL_OCCURRENCES;
import static org.knime.base.node.util.regex.ReplacementStrategy.WHOLE_STRING;

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.knime.base.node.util.regex.RegexReplaceTestCaseBuilder.TestCase;
import org.knime.base.node.util.regex.RegexReplaceUtils.IllegalReplacementException;
import org.knime.base.node.util.regex.RegexReplaceUtils.IllegalSearchPatternException;

/**
 *
 * @author David Hickey, TNG Technology Consulting GmbH
 */
final class RegexReplaceUtilsTest {

    static final Stream<TestCase> TEST_CASES = Stream.of( //
        builder("", "xyz", "", "xyz").replacementStrategy(WHOLE_STRING).patternType(LITERAL).build(), //
        builder("", "xyz", "", null).replacementStrategy(ALL_OCCURRENCES).patternType(LITERAL).build(), //
        builder("", "xyz", "abc", null).replacementStrategy(ALL_OCCURRENCES).patternType(LITERAL).build(), //
        builder("abc", "xyz", "abc", "xyz").replacementStrategy(ALL_OCCURRENCES).build(), //
        builder("abc", "xyz", "abcabc", "xyzxyz").replacementStrategy(ALL_OCCURRENCES).build(), //
        builder("abc", "xyz", "abcabc", null).replacementStrategy(WHOLE_STRING).build(), //
        builder(".*", "xyz", "abc", "xyz").replacementStrategy(WHOLE_STRING).patternType(REGEX).build(), //
        builder(".*", "xyz", "literallyanything", "xyzxyz").replacementStrategy(ALL_OCCURRENCES).patternType(REGEX)
            .build(), //
        builder("*", "xyz", "abcabc", "xyz").replacementStrategy(ALL_OCCURRENCES).patternType(WILDCARD).build(), //
        builder("*", "xyz", "abc", "xyz").replacementStrategy(WHOLE_STRING).patternType(WILDCARD).build(), //
        builder("ABC", "xyz", "abc", "xyz").caseMatching(CASEINSENSITIVE).build(), //
        builder("ABC", "xyz", "abc", null).caseMatching(CASESENSITIVE).build(), //
        builder("ab(c)", "$1", "abc", "c").patternType(REGEX).build(), //
        builder("abc", "$1", "abc", "$1").patternType(WILDCARD, LITERAL).build(), //
        builder("", "$1", "", "$1").patternType(WILDCARD).build(), //
        builder("", "abc", "xyz", "abcxabcyabczabc").replacementStrategy(ALL_OCCURRENCES).patternType(REGEX, WILDCARD)
            .build() //
    ).flatMap(List::stream);

    static Stream<Arguments> provideTestCases() {
        return TEST_CASES.map(Arguments::of);
    }

    @SuppressWarnings("static-method")
    @ParameterizedTest
    @MethodSource("provideTestCases")
    void testReplacement(final TestCase testCase) throws IllegalSearchPatternException, IllegalReplacementException {
        var pattern =
            RegexReplaceUtils.compilePattern(testCase.searchPattern(), testCase.patternType(), testCase.caseMatching());
        var replacement = RegexReplaceUtils.processReplacementString(testCase.replacement(), testCase.patternType());
        var processedString = RegexReplaceUtils.doReplacement(pattern, testCase.replacementStrategy(),
            testCase.patternType(), testCase.input(), replacement).asOptional().orElse(null);

        assertEquals(testCase.expected(), processedString, "Replacement did not match expected result");
    }
}
