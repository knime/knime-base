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
 *   14 May 2023 (jasper): created
 */
package org.knime.base.node.preproc.stringreplacer.dict2;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.knime.base.node.preproc.stringreplacer.CaseMatching;
import org.knime.base.node.preproc.stringreplacer.PatternType;
import org.knime.base.node.preproc.stringreplacer.ReplacementStrategy;
import org.knime.base.node.preproc.stringreplacer.dict2.DictReplacer.IllegalReplacementException;
import org.knime.base.node.preproc.stringreplacer.dict2.DictReplacer.IllegalSearchPatternException;
import org.knime.base.node.preproc.stringreplacer.dict2.StringReplacerDictNodeSettings.MultipleMatchHandling;

/**
 * This class tests the {@link StringReplacer} from the String Replacer (Dictionary) node
 *
 * @author Jasper Krauter, KNIME GmbH, Konstanz, Germany
 */
class StringReplacerTest {

    @Test
    void testProcessing() throws IllegalSearchPatternException, IllegalReplacementException {
        var settings = new StringReplacerDictNodeSettings();
        settings.m_patternType = PatternType.LITERAL;
        settings.m_multipleMatchHandling = MultipleMatchHandling.REPLACEALL;
        settings.m_replacementStrategy = ReplacementStrategy.ALL_OCCURRENCES;
        settings.m_caseMatching = CaseMatching.CASESENSITIVE;

        var sr1 = new StringReplacer(settings);

        // These replacements should be performed one-by-one, and applied to the last resulting string
        sr1.addToDictionary("dog", "cat");
        sr1.addToDictionary("cat", "bat");
        sr1.addToDictionary("T", "");

        assertEquals("", sr1.process(""), "Empty string is modified");
        assertEquals("he quick brown fox jumps over the lazy bat",
            sr1.process("The quick brown fox jumps over the lazy dog"), "Replacing dog with bat failed");

        settings.m_caseMatching = CaseMatching.CASEINSENSITIVE;

        assertEquals("he quick brown fox jumps over he lazy ba",
            sr1.process("he quick brown fox jumps over he lazy dog"), "Case sensitivity not working");
    }

    @Test
    void testSingleReplacements() {
        var settings = new StringReplacerDictNodeSettings();
        settings.m_patternType = PatternType.LITERAL;
        settings.m_replacementStrategy = ReplacementStrategy.ALL_OCCURRENCES;
        settings.m_caseMatching = CaseMatching.CASESENSITIVE;

        var sr = new StringReplacer(settings);
        assertEquals("baz bar baz", sr.processSingleReplacement("foo", "foo bar foo", "baz").get(),
            "simple replacement failed");
        assertEquals("foo bar", sr.processSingleReplacement("", "foo bar", "baz").get(), "empty pattern test failed");
        assertEquals(Optional.empty(), sr.processSingleReplacement("pattern", "", "replacement"),
            "empty string test failed");
        assertEquals(Optional.empty(), sr.processSingleReplacement("hello", "foo bar", "world"),
            "no match test failed");

        settings.m_caseMatching = CaseMatching.CASEINSENSITIVE;

        assertEquals("BAZ bar BAZ", sr.processSingleReplacement("foo", "foo bar Foo", "BAZ").get(),
            "case insensitive test failed");
        assertEquals(Optional.empty(), sr.processSingleReplacement("foo", "bar", "BAZ"),
            "case insensitive not found test failed");

        settings.m_replacementStrategy = ReplacementStrategy.WHOLE_STRING;
        assertEquals(Optional.empty(), sr.processSingleReplacement("foo", "fooo", "bar"),
            "part of the string was replaced even though WHOLE_STRING strategy is used");
        assertEquals("bar", sr.processSingleReplacement("fooo", "fooo", "bar").get(),
            "whole string replacement failed");

        settings.m_caseMatching = CaseMatching.CASESENSITIVE;
        assertEquals("BaZ", sr.processSingleReplacement("foo", "foo", "BaZ").get(), "whole string replacement failed");
        assertEquals(Optional.empty(), sr.processSingleReplacement("foo", "fooo", "BaZ"),
            "whole strng replacement shouldn't have replaced the string.");
    }

    @Test
    void testPreprocessing() {
        var sr = new StringReplacer(new StringReplacerDictNodeSettings());
        assertEquals("Hello, World! \soM?e$12W€irdçhåractærs",
            sr.compilePattern("Hello, World! \soM?e$12W€irdçhåractærs"),
            "compile method shouldn't have changed the string");
        assertEquals("Hello, World! \soM?e$12W€irdçhåractærs",
            sr.prepareReplacementString("Hello, World! \soM?e$12W€irdçhåractærs"),
            "prepare replacement method shouldn't have changed the string");
    }

}
