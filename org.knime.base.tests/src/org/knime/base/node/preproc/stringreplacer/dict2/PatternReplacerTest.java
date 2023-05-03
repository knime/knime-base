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
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.knime.base.node.preproc.stringreplacer.CaseMatching;
import org.knime.base.node.preproc.stringreplacer.PatternType;
import org.knime.base.node.preproc.stringreplacer.ReplacementStrategy;
import org.knime.base.node.preproc.stringreplacer.dict2.DictReplacer.IllegalReplacementException;
import org.knime.base.node.preproc.stringreplacer.dict2.DictReplacer.IllegalSearchPatternException;
import org.knime.base.node.preproc.stringreplacer.dict2.StringReplacerDictNodeSettings.MultipleMatchHandling;
import org.knime.filehandling.core.util.WildcardToRegexUtil;

/**
 * This class tests the {@link PatternReplacer} from the String Replacer (Dictionary) node
 *
 * @author Jasper Krauter, KNIME GmbH, Konstanz, Germany
 */
class PatternReplacerTest {

    @Test
    void testRegExProcessing() throws IllegalSearchPatternException, IllegalReplacementException {
        var settings = new StringReplacerDictNodeSettings();
        settings.m_patternType = PatternType.REGEX;
        settings.m_multipleMatchHandling = MultipleMatchHandling.REPLACEALL;
        settings.m_replacementStrategy = ReplacementStrategy.ALL_OCCURRENCES;
        settings.m_caseMatching = CaseMatching.CASESENSITIVE;
        var pr = new PatternReplacer(settings);

        // These replacements should be performed one-by-one, and applied to the last resulting string
        pr.addToDictionary(".og$", "cat");
        pr.addToDictionary(".at$", "bat");
        pr.addToDictionary("\\bT", "");

        assertEquals("", pr.process(""), "Empty string is modified");
        assertEquals("he quick brown fox jumps over the lazy bat",
            pr.process("The quick brown fox jumps over the lazy dog"), "Replacing dog with bat failed");
        assertEquals("foo", pr.process("foo"), "no pattern matches failed");

        settings.m_multipleMatchHandling = MultipleMatchHandling.REPLACEFIRST;
        assertEquals("The quick brown fox jumps over the lazy cat",
            pr.process("The quick brown fox jumps over the lazy dog"), "Only replacing the first pattern failed");

        settings.m_replacementStrategy = ReplacementStrategy.WHOLE_STRING;
        assertEquals("bat", pr.process("rat"), "whole string replacement failed");
        assertEquals("", pr.process(""), "whole string replacement in empty string failed");
    }

    /**
     * Superficial test of the wildcard-to-regex transform.
     *
     * The method relies on {@link WildcardToRegexUtil#wildcardToRegex(String, boolean)}, which is properly tested in
     * elsewhere.
     *
     * @throws IllegalSearchPatternException
     */
    @Test
    void testWildcardPatternCompilation() throws IllegalSearchPatternException {
        var settings = new StringReplacerDictNodeSettings();
        settings.m_patternType = PatternType.WILDCARD;
        settings.m_enableEscaping = false;
        var pr = new PatternReplacer(settings);

        assertEquals(".*", pr.compilePattern("*").pattern(), "Wildcard didn't get transformed to RegEx");
        assertEquals("what..\\\\.\\\\.*", pr.compilePattern("what??\\?\\*").pattern(),
            "Wildcard didn't get transformed to RegEx");

        settings.m_enableEscaping = true;
        assertEquals("what..\\?\\*", pr.compilePattern("what??\\?\\*").pattern(), "Escaping wasn't enabled");
    }

    @Test
    void testSingleReplacements() throws IllegalReplacementException, IllegalSearchPatternException {
        var settings = new StringReplacerDictNodeSettings();
        settings.m_patternType = PatternType.REGEX;
        settings.m_replacementStrategy = ReplacementStrategy.ALL_OCCURRENCES;
        settings.m_caseMatching = CaseMatching.CASESENSITIVE;
        var pr = new PatternReplacer(settings);

        assertEquals("bar", pr.processSingleReplacement(pr.compilePattern("^.*"), "foo", "bar").get(),
            "Simple replacement of ^.* failed");
        assertEquals("barbar", pr.processSingleReplacement(pr.compilePattern("((.*))"), "foo", "bar").get(),
            "Double replacement of .* failed");
        assertEquals("new + old", pr.processSingleReplacement(pr.compilePattern("^(.+l.*)"), "old", "new + $1").get(),
            "Back-reference failed");
        assertThrows(IllegalReplacementException.class,
            () -> pr.processSingleReplacement(pr.compilePattern("^(.*)"), "foo", "bar $42"),
            "Panic on invalid capture group failed");
        assertThrows(IllegalReplacementException.class,
            () -> pr.processSingleReplacement(pr.compilePattern("^(?<named>.*)"), "foo", "bar ${whoops}"),
            "Panic on invalid named capture group failed");

        assertEquals(Optional.empty(),
            pr.processSingleReplacement(pr.compilePattern("x+"), "No letters after w to see here", "foo"),
            "no match returned something anyways");

        settings.m_replacementStrategy = ReplacementStrategy.WHOLE_STRING;
        assertEquals("bar", pr.processSingleReplacement(pr.compilePattern("(.*)"), "foo", "bar").get(),
            "Whole string replacement of (.*) failed");
        assertEquals("oldnewold", pr.processSingleReplacement(pr.compilePattern("(.ld+)"), "old", "$1new$1").get(),
            "Whole string replacement with back-references failed");
        assertEquals(Optional.empty(),
            pr.processSingleReplacement(pr.compilePattern(".*pp.*?q.*"), "kaulquappe", "foo"),
            "Whole string matching with no match failed");
    }

    @Test
    void testCaseInsensitivity() throws IllegalReplacementException, IllegalSearchPatternException {
        var settings = new StringReplacerDictNodeSettings();
        settings.m_patternType = PatternType.REGEX;
        settings.m_replacementStrategy = ReplacementStrategy.ALL_OCCURRENCES;
        settings.m_caseMatching = CaseMatching.CASEINSENSITIVE;
        var pr = new PatternReplacer(settings);

        assertEquals("foobarbaz",
            pr.processSingleReplacement(pr.compilePattern("^er{2}or"), "eRrORbarbaz", "foo").get(),
            "Simple case-insensitive replacement failed");
        assertEquals("Trondheim is a nice city.",
            pr.processSingleReplacement(pr.compilePattern("BÆÆRGEN"), "Bæærgen is a nice city.", "Trondheim").get(),
            "Unicode case-insenstive matching failed"); // to be fair, Bergen is really nice, but they speak funny
        assertEquals(Optional.empty(), pr.processSingleReplacement(pr.compilePattern("AO"), "åø", "foo"),
            "Replacing only actual case matches failed");
    }

    @Test
    void testPreprocessing() throws IllegalSearchPatternException {
        var settings = new StringReplacerDictNodeSettings();
        settings.m_patternType = PatternType.REGEX;
        var pr = new PatternReplacer(settings);

        assertEquals("abc.*?test", pr.compilePattern("abc.*?test").pattern(),
            "Pattern is somehow changed while compiling");
        assertThrows(IllegalSearchPatternException.class, () -> pr.compilePattern("**"),
            "Invalid pattern didn't throw");

        assertEquals("shouldn't $1 $1234 ${be} altered",
            pr.prepareReplacementString("shouldn't $1 $1234 ${be} altered"), "replacement string was altered");

        settings.m_patternType = PatternType.WILDCARD;
        assertEquals("shouldn't \\$1 \\$1234 \\${be} altered",
            pr.prepareReplacementString("shouldn't $1 $1234 ${be} altered"),
            "replacement string wasn't stripped from back-references");
    }

    String singleReplace(final String input, final String pattern, final String replace,
        final PatternType PatternType, final boolean caseSensitive,
        final ReplacementStrategy replacementStrategy)
        throws IllegalSearchPatternException, IllegalReplacementException {
        var settings = new StringReplacerDictNodeSettings();
        settings.m_patternType = PatternType;
        settings.m_caseMatching = caseSensitive ? CaseMatching.CASESENSITIVE : CaseMatching.CASEINSENSITIVE;
        settings.m_replacementStrategy = replacementStrategy;

        var replacer = PatternType == org.knime.base.node.preproc.stringreplacer.PatternType.LITERAL ? new StringReplacer(settings)
            : new PatternReplacer(settings);
        replacer.addToDictionary(pattern, replace);

        return replacer.process(input);
    }

    @Test
    @SuppressWarnings({"squid:S2698", "squid:S5961"}) // no annotations for every single assertion
    void testMoreUnusualPatterns() throws IllegalSearchPatternException, IllegalReplacementException {
        assertEquals("Vitamin T",
            singleReplace("Vitamin T", "T", "C", PatternType.LITERAL, false, ReplacementStrategy.WHOLE_STRING));
        assertEquals("ViCamin C",
            singleReplace("Vitamin T", "T", "C", PatternType.LITERAL, false, ReplacementStrategy.ALL_OCCURRENCES));
        assertEquals("Vitamin C",
            singleReplace("Vitamin T", "T", "C", PatternType.LITERAL, true, ReplacementStrategy.ALL_OCCURRENCES));
        assertEquals("Vitamin T",
            singleReplace("Vitamin T", "t", "C", PatternType.LITERAL, false, ReplacementStrategy.WHOLE_STRING));
        assertEquals("Vicamin c",
            singleReplace("Vitamin T", "t", "c", PatternType.LITERAL, false, ReplacementStrategy.ALL_OCCURRENCES));
        assertEquals("Vicamin T",
            singleReplace("Vitamin T", "t", "c", PatternType.LITERAL, true, ReplacementStrategy.ALL_OCCURRENCES));
        assertEquals("Viamin ",
            singleReplace("Vitamin T", "t", "", PatternType.LITERAL, false, ReplacementStrategy.ALL_OCCURRENCES));
        assertEquals("Vitamin T",
            singleReplace("Vi*amin T", "*", "t", PatternType.LITERAL, false, ReplacementStrategy.ALL_OCCURRENCES));
        assertEquals("Vitamin C",
            singleReplace("Vitamin ?", "?", "C", PatternType.LITERAL, false, ReplacementStrategy.ALL_OCCURRENCES));
        assertEquals("(.*)[a-z]123", singleReplace("(.*)[a-z]123", "(.*)[a-z]", "", PatternType.LITERAL, false,
            ReplacementStrategy.WHOLE_STRING));
        assertEquals("itamin C", singleReplace("Vitamin T", "?itamin T", "itamin C", PatternType.WILDCARD, true,
            ReplacementStrategy.WHOLE_STRING));
        assertEquals("itamin C", singleReplace("Vitamin T", "?itamin T", "itamin C", PatternType.WILDCARD, true,
            ReplacementStrategy.ALL_OCCURRENCES));
        assertEquals("itamin C", singleReplace("Vitamin T", "Vita?in T", "itamin C", PatternType.WILDCARD, true,
            ReplacementStrategy.ALL_OCCURRENCES));
        assertEquals("C", singleReplace("Vitamin T", "Vitamin ?", "C", PatternType.WILDCARD, true,
            ReplacementStrategy.ALL_OCCURRENCES));
        assertEquals("Vitamin T",
            singleReplace("Vitamin T", " ?", "C", PatternType.WILDCARD, false, ReplacementStrategy.WHOLE_STRING));
        assertEquals("Vitamin C", singleReplace("Vitamin T", " ?", " C", PatternType.WILDCARD, false,
            ReplacementStrategy.ALL_OCCURRENCES));
        assertEquals("VitaminC", singleReplace("Vitamin T", " ?", "C", PatternType.WILDCARD, false,
            ReplacementStrategy.ALL_OCCURRENCES));
        assertEquals("Vitamin c", singleReplace("Vitamin T", " ?", " c", PatternType.WILDCARD, false,
            ReplacementStrategy.ALL_OCCURRENCES));
        assertEquals("Vitaminc", singleReplace("Vitamin T", " ?", "c", PatternType.WILDCARD, false,
            ReplacementStrategy.ALL_OCCURRENCES));
        assertEquals("Vitamin T",
            singleReplace("Vitamin T", " ?", " C", PatternType.WILDCARD, true, ReplacementStrategy.WHOLE_STRING));
        assertEquals("Vitamin T",
            singleReplace("Vitamin T", " ?", "C", PatternType.WILDCARD, true, ReplacementStrategy.WHOLE_STRING));
        assertEquals("Vitamin C",
            singleReplace("Vitamin T", " ?", " C", PatternType.WILDCARD, true, ReplacementStrategy.ALL_OCCURRENCES));
        assertEquals("VitaminC",
            singleReplace("Vitamin T", " ?", "C", PatternType.WILDCARD, true, ReplacementStrategy.ALL_OCCURRENCES));
        assertEquals("Vitamin c",
            singleReplace("Vitamin T", " ?", " c", PatternType.WILDCARD, true, ReplacementStrategy.ALL_OCCURRENCES));
        assertEquals("Vitaminc",
            singleReplace("Vitamin T", " ?", "c", PatternType.WILDCARD, true, ReplacementStrategy.ALL_OCCURRENCES));
        assertEquals("C",
            singleReplace("Vitamin T", "*", "C", PatternType.WILDCARD, false, ReplacementStrategy.WHOLE_STRING));
        assertEquals("C",
            singleReplace("Vitamin T", "*", "C", PatternType.WILDCARD, false, ReplacementStrategy.WHOLE_STRING));
        assertEquals("Vitamin C", singleReplace("Vitamin T", " *", " C", PatternType.WILDCARD, false,
            ReplacementStrategy.ALL_OCCURRENCES));
        assertEquals("VitaminC", singleReplace("Vitamin T", " *", "C", PatternType.WILDCARD, false,
            ReplacementStrategy.ALL_OCCURRENCES));
        assertEquals("Vitamin c", singleReplace("Vitamin T", " *", " c", PatternType.WILDCARD, false,
            ReplacementStrategy.ALL_OCCURRENCES));
        assertEquals("Vitaminc", singleReplace("Vitamin T", " *", "c", PatternType.WILDCARD, false,
            ReplacementStrategy.ALL_OCCURRENCES));
        assertEquals("C",
            singleReplace("Vitamin T", "*", "C", PatternType.WILDCARD, true, ReplacementStrategy.WHOLE_STRING));
        assertEquals("C",
            singleReplace("Vitamin T", "*", "C", PatternType.WILDCARD, true, ReplacementStrategy.WHOLE_STRING));
        assertEquals("Vitamin C",
            singleReplace("Vitamin T", " *", " C", PatternType.WILDCARD, true, ReplacementStrategy.ALL_OCCURRENCES));
        assertEquals("VitaminC",
            singleReplace("Vitamin T", " *", "C", PatternType.WILDCARD, true, ReplacementStrategy.ALL_OCCURRENCES));
        assertEquals("Vitamin c",
            singleReplace("Vitamin T", " *", " c", PatternType.WILDCARD, true, ReplacementStrategy.ALL_OCCURRENCES));
        assertEquals("Vitaminc",
            singleReplace("Vitamin T", " *", "c", PatternType.WILDCARD, true, ReplacementStrategy.ALL_OCCURRENCES));
        assertEquals("C",
            singleReplace("Vitamin T", "*", "C", PatternType.WILDCARD, true, ReplacementStrategy.ALL_OCCURRENCES));
        assertEquals("C", singleReplace("Vitamin T", "**", "C", PatternType.WILDCARD, true,
            ReplacementStrategy.ALL_OCCURRENCES));
        assertEquals("C", singleReplace("Vitamin T", "?*", "C", PatternType.WILDCARD, true,
            ReplacementStrategy.ALL_OCCURRENCES));
        assertEquals("C",
            singleReplace("Vitamin T", "?*", "C", PatternType.WILDCARD, true, ReplacementStrategy.WHOLE_STRING));
        assertEquals("Vitamin T", singleReplace("Vitamin T", "v*min", "C", PatternType.WILDCARD, true,
            ReplacementStrategy.WHOLE_STRING));
        assertEquals("Vitamin T", singleReplace("Vitamin T", "v*min", "C", PatternType.WILDCARD, false,
            ReplacementStrategy.WHOLE_STRING));
        assertEquals("C T", singleReplace("Vitamin T", "v*min", "C", PatternType.WILDCARD, false,
            ReplacementStrategy.ALL_OCCURRENCES));
        ;
    }

}
