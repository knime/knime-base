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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 *
 * @author david
 */
final class RegexReplaceTestCaseBuilder {

    record TestCase( //
        String searchPattern, //
        String replacement, //
        String input, //
        String expected, //
        PatternType patternType, //
        CaseMatching caseMatching, //
        ReplacementStrategy replacementStrategy //
    ) {
    }

    private String m_searchPattern;

    private String m_replacement;

    private String m_input;

    private String m_expected;

    private PatternType[] m_patternTypes = PatternType.values();

    private List<CaseMatching> m_caseMatchings = Arrays.asList(CaseMatching.values());

    private List<ReplacementStrategy> m_replacementStrategies = Arrays.asList(ReplacementStrategy.values());

    public RegexReplaceTestCaseBuilder(final String searchPattern, final String replacement, final String input,
        final String expected) {
        this.m_searchPattern = searchPattern;
        this.m_replacement = replacement;
        this.m_input = input;
        this.m_expected = expected;
    }

    public RegexReplaceTestCaseBuilder patternType(final PatternType... patternTypes) {
        this.m_patternTypes = patternTypes;
        return this;
    }

    public RegexReplaceTestCaseBuilder caseMatching(final CaseMatching caseMatching) {
        this.m_caseMatchings = List.of(caseMatching);
        return this;
    }

    public RegexReplaceTestCaseBuilder replacementStrategy(final ReplacementStrategy strategy) {
        this.m_replacementStrategies = List.of(strategy);
        return this;
    }

    public List<TestCase> build() {
        List<TestCase> result = new ArrayList<>();
        for (PatternType pt : m_patternTypes) {
            for (CaseMatching cm : m_caseMatchings) {
                for (ReplacementStrategy rs : m_replacementStrategies) {
                    result.add(new TestCase(m_searchPattern, m_replacement, m_input, m_expected, pt, cm, rs));
                }
            }
        }
        return result;
    }

    static RegexReplaceTestCaseBuilder builder(final String searchPattern, final String replacement, final String input,
        final String expected) {
        return new RegexReplaceTestCaseBuilder(searchPattern, replacement, input, expected);
    }
}
