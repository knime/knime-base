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
 *   2 May 2023 (jasper): created
 */
package org.knime.base.node.preproc.stringreplacer.dict2;

import java.util.Optional;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.knime.base.node.preproc.stringreplacer.CaseMatching;
import org.knime.base.node.preproc.stringreplacer.PatternType;
import org.knime.base.node.preproc.stringreplacer.ReplacementStrategy;
import org.knime.filehandling.core.util.WildcardToRegexUtil;

/**
 * Replacer dictionary implementation that has compiled RegEx patterns as lookup keys
 *
 * @author Jasper Krauter, KNIME GmbH, Konstanz, Germany
 */
final class PatternReplacer extends DictReplacer<Pattern> {

    /**
     * A pre-compiled {@link Pattern} that matches a back-reference in a replacement string and captures them.
     *
     * See https://docs.oracle.com/javase/8/docs/api/java/util/regex/Pattern.html#cg
     *
     * We match on numerical and named back-references as specified in the above doc
     */
    private static final Pattern backreferencePattern = //
        Pattern.compile("(\\$\\d+|\\$\\{[A-Za-z][A-Za-z0-9]*\\})"); //NOSONAR: only match spec

    /**
     * The default flags that will be used when compiling patterns
     */
    private final int m_flags;

    /**
     * Creates a new PatternReplacer instance. Reads {@link StringReplacerDictNodeSettings#m_caseMatching} at
     * instantiation and sets RegEx flags accordingly.
     *
     * @param modelSettings the settings of the String Replacer (Dictionary) node instance
     */
    PatternReplacer(final StringReplacerDictNodeSettings modelSettings) {
        super(modelSettings);
        var flags = 0;
        if (m_settings.m_patternType == PatternType.WILDCARD) {
            flags |= Pattern.MULTILINE | Pattern.DOTALL;
        }
        if (m_settings.m_caseMatching == CaseMatching.CASEINSENSITIVE) {
            flags |= Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE;
        }
        m_flags = flags;
    }

    @Override
    protected Pattern compilePattern(String pattern) throws IllegalSearchPatternException {
        if (m_settings.m_patternType == PatternType.WILDCARD) {
            pattern = WildcardToRegexUtil.wildcardToRegex(pattern, m_settings.m_enableEscaping);
        }
        try {
            return Pattern.compile(pattern, m_flags);
        } catch (PatternSyntaxException e) {
            throw new IllegalSearchPatternException("Invalid RegEx pattern: " + System.lineSeparator() + e.getMessage(),
                e);
        }
    }

    /**
     * Removes back-references from the replacement string if wildcard matching is enabled. {@inheritDoc}
     */
    @Override
    protected String prepareReplacementString(final String replacement) {
        if (m_settings.m_patternType == PatternType.WILDCARD) {
            return backreferencePattern.matcher(replacement).replaceAll("\\\\$1");
        } else {
            return replacement;
        }
    }

    @Override
    protected Optional<String> processSingleReplacement(final Pattern pattern, final String input,
        final String replacement) throws IllegalReplacementException {
        var matcher = pattern.matcher(input);
        try {
            if (m_settings.m_replacementStrategy == ReplacementStrategy.ALL_OCCURRENCES && matcher.find()) {
                matcher.reset(); // matcher.find() has changed the state of the matcher
                return Optional.of(switch (m_settings.m_patternType) {
                    // Intentionally don't alter any regex behaviour here so we can rely on the Java Pattern Doc
                    case REGEX -> matcher.replaceAll(replacement);
                    // Replace e.g. "*" only once, otherwise fall back to replaceAll
                    case WILDCARD -> matcher.matches() ? replacement : matcher.replaceAll(replacement);
                    default -> throw new IllegalStateException(
                        "The PatternReplacer can only handle RegEx and Wildcard Replacements");
                });
            } else {
                matcher.reset();
                if (m_settings.m_replacementStrategy == ReplacementStrategy.WHOLE_STRING && matcher.matches()) {
                    // Here, there used to be a check whether the pattern is `.*`
                    // This has been removed, since m.matches() already indicates that the whole string matches.
                    // We use the state of the matcher now (i.e. start() and end() point to the start and end of the
                    // string) to build a new string from the replacement. The replacement might contain
                    // back-references.
                    var sb = new StringBuilder();
                    matcher.appendReplacement(sb, replacement);
                    return Optional.of(sb.toString());
                } else {
                    // Nothing to replace
                    return Optional.empty();
                }
            }
        } catch (IndexOutOfBoundsException | IllegalArgumentException e) {
            // Most likely a back-reference to a group that doesn't exist in the pattern
            throw new IllegalReplacementException("Could not replace pattern \"" + pattern.pattern() + "\" in \""
                + input + "\" with \"" + replacement + "\": " + e.getMessage(), e);
        }
    }
}
