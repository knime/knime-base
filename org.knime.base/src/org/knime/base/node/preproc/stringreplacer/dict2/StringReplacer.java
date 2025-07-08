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

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

import org.knime.base.node.util.regex.CaseMatching;
import org.knime.base.node.util.regex.PatternType;
import org.knime.base.node.util.regex.RegexReplaceUtils;
import org.knime.base.node.util.regex.RegexReplaceUtils.IllegalReplacementException;
import org.knime.base.node.util.regex.RegexReplaceUtils.IllegalSearchPatternException;
import org.knime.base.node.util.regex.RegexReplaceUtils.ReplacementResult;
import org.knime.base.node.util.regex.ReplacementStrategy;

/**
 * Replacer dictionary implementation that has plain strings as lookup keys
 *
 * @author Jasper Krauter, KNIME GmbH, Konstanz, Germany
 */
public final class StringReplacer extends DictReplacer<String> {

    private final CachingRegexReplacer m_replacer = new CachingRegexReplacer();

    StringReplacer(final StringReplacerDictNodeSettings modelSettings) {
        super(modelSettings);
    }

    @Override
    protected String compilePattern(final String pattern) {
        return pattern;
    }

    @Override
    protected String prepareReplacementString(final String replacement) {
        return replacement;
    }

    @Override
    protected Optional<String> processSingleReplacement(final String pattern, final String input,
        final String replacement) {

        // This node was designed so that if the replacement pattern is empty, it makes no
        // replacement but reports that a replacement was made. We include this line here
        // to keep this behaviour for backwards compatibility.
        if (pattern.isEmpty()) {
            return Optional.of(input);
        }

        try {
            return m_replacer.doReplacement( //
                m_settings.m_useNewFixedWildcardBehavior, //
                pattern, //
                PatternType.LITERAL, //
                m_settings.m_caseMatching, //
                m_settings.m_replacementStrategy, //
                input, //
                replacement //
            ).asOptional();
        } catch (IllegalReplacementException | IllegalSearchPatternException e) {
            // neither of these should ever happen
            throw new IllegalStateException( //
                "Implementation error: unexpected exception while performing replacement: " + e.getMessage(), e //
            );
        }
    }

    /**
     * Caches compiled regex patterns for performance. If you expect to provide the same pattern as a string multiple
     * times, you can use this class to cache the compiled pattern and reuse it, which will avoid a substantial
     * performance hit.
     */
    private static class CachingRegexReplacer {

        private record PatternCacheKey( //
            String patternString, //
            PatternType patternType, //
            CaseMatching caseMatching //
        ) {
        }

        private final Map<PatternCacheKey, Pattern> m_cache = new HashMap<>();

        /**
         * <p>
         * Caching proxy for
         * {@link #doReplacement(String, PatternType, CaseMatching, ReplacementStrategy, String, String)}. See that
         * method for more details.
         * </p>
         * <p>
         * The specific combination of pattern string, pattern type and case matching is cached so calling this method
         * again with those same parameters will reuse the cached pattern (even if the 'input', 'replacement', and
         * 'replacementStrategy' arguments are different).
         * </p>
         */
        @SuppressWarnings("javadoc")
        public ReplacementResult doReplacement( //
            final boolean useNewFixedWildcardBehavior, //
            final String patternString, //
            final PatternType patternType, //
            final CaseMatching caseMatching, //
            final ReplacementStrategy replacementStrategy, //
            final String input, //
            final String replacement //
        ) throws IllegalReplacementException, IllegalSearchPatternException {
            var pattern = getFromCacheOrCompile(patternString, patternType, caseMatching);
            var quotedReplacement = useNewFixedWildcardBehavior
                ? RegexReplaceUtils.processReplacementString(replacement, patternType)
                : RegexReplaceUtils.processReplacementStringWithWildcardBackwardCompatibility(replacement, patternType);

            return RegexReplaceUtils.doReplacement( //
                pattern, //
                replacementStrategy, //
                patternType, //
                input, //
                quotedReplacement //
            );

        }

        private Pattern getFromCacheOrCompile( //
            final String patternString, //
            final PatternType patternType, //
            final CaseMatching caseMatching //
        ) throws IllegalSearchPatternException {
            var key = new PatternCacheKey(patternString, patternType, caseMatching);

            if (!m_cache.containsKey(key)) {
                var compiled = RegexReplaceUtils.compilePattern(patternString, patternType, caseMatching, false);
                m_cache.put(key, compiled);
            }

            return m_cache.get(key);
        }
    }
}
