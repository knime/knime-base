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
 *   Apr 24, 2025 (david): created
 */
package org.knime.base.node.util.regex;

import java.util.Optional;
import java.util.function.BooleanSupplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.knime.filehandling.core.util.WildcardToRegexUtil;

/**
 * Utility class for replacing regex patterns in strings in a way that maintains consistency with the String Replacer
 * and Columne Name Replacer nodes.
 *
 * @author David Hickey, TNG Technology Consulting GmbH
 * @since 5.5
 */
public final class RegexReplaceUtils {

    private RegexReplaceUtils() {
        // utility class
    }

    /**
     * Replaces the given pattern in the input string with the replacement string.
     *
     * @param pattern the regex pattern that should be replaced. See
     *            {@link #compilePattern(String, PatternType, CaseMatching, boolean)} to compile a regex which will have
     *            a set of flags corresponding to the settings. Note that if the flags in the pattern are inconsistent
     *            with the settings passed as the other parameters to this method, the resulting behaviour will be
     *            potentially very buggy.
     * @param replacementStrategy the replacement strategy
     * @param patternType the pattern type. The behaviour will be different depending on which type is used.
     * @param input the input string to search and make the replacements within
     * @param replacement the replacement string. It is assumed to have already been processed - see
     *            {@link #processReplacementString(String, PatternType)}.
     * @return the input string with the replacements made
     * @throws IllegalReplacementException if the replacement argument is malformed. For example, if it contains a
     *             back-reference to a group that doesn't exist in the pattern.
     */
    public static ReplacementResult doReplacement( //
        final Pattern pattern, //
        final ReplacementStrategy replacementStrategy, //
        final PatternType patternType, //
        final String input, //
        final String replacement //
    ) throws IllegalReplacementException {
        var stringPattern = pattern.pattern();

        if (stringPattern.isEmpty() && patternType == PatternType.LITERAL) {
            if (replacementStrategy == ReplacementStrategy.ALL_OCCURRENCES || !input.isEmpty()) {
                return ReplacementResult.notReplaced();
            }

            return ReplacementResult.replaced(unquoteReplacement(replacement));
        }

        var matcher = pattern.matcher(input);

        BooleanSupplier contains = () -> {
            var findResult = matcher.find();
            matcher.reset();
            return findResult;
        };
        BooleanSupplier matches = () -> {
            var matchResult = matcher.matches();
            matcher.reset();
            return matchResult;
        };

        try {
            if (replacementStrategy == ReplacementStrategy.ALL_OCCURRENCES && contains.getAsBoolean()) {
                return ReplacementResult.replaced(switch (patternType) {
                    // Intentionally don't alter any regex behaviour here so we can rely on the Java Pattern Doc
                    case REGEX, LITERAL -> matcher.replaceAll(replacement);
                    case WILDCARD -> {
                        // Special handling here means that we will only replace "*" once, whereas if
                        // we just used replaceAll then "*" would insert the replacement string twice
                        // (because regex weirdness).
                        yield matches.getAsBoolean() //
                            ? unquoteReplacement(replacement) //
                            : matcher.replaceAll(replacement);
                    }
                });
            } else if (replacementStrategy == ReplacementStrategy.WHOLE_STRING && matches.getAsBoolean()) {
                // this approach is needed to ensure that the regex `.*` will only
                // find one match in the whole string.
                var sb = new StringBuilder();
                matcher.matches(); // used purely for the side effect of matching
                matcher.appendReplacement(sb, replacement);
                return ReplacementResult.replaced(sb.toString());
            } else {
                return ReplacementResult.notReplaced();
            }
        } catch (IndexOutOfBoundsException | IllegalArgumentException e) {
            // Most likely a back-reference to a group that doesn't exist in the pattern
            throw new IllegalReplacementException(pattern, replacement, input, e);
        }
    }

    /**
     * 'un-escape' an escaped replacement string that has been quoted using {@link Matcher#quoteReplacement(String)}.
     * Will convert stuff like \$1 -> $1.
     *
     * @param quotedReplacement the replacement string that has been quoted
     * @return the unquoted replacement string
     */
    private static String unquoteReplacement(final String quotedReplacement) {
        return "".replaceAll("", quotedReplacement);
    }

    /**
     * Processes the replacement string depending on the pattern type. If the pattern type is anything other than REGEX,
     * the replacement string will be escaped so that anything that looks like a back-reference will be treated as a
     * literal.
     *
     * @param replacement the replacement string
     * @param patternType the pattern type
     * @return the processed replacement string with any back-references escaped
     */
    public static String processReplacementString(final String replacement, final PatternType patternType) {
        return switch (patternType) {
            case WILDCARD, LITERAL -> Matcher.quoteReplacement(replacement);
            case REGEX -> replacement;
        };
    }

    /**
     * From the old String Replacer node, was used to escape back-references in the replacement string. However it
     * introduced some bugs, and we have now replaced it with a call {@link Matcher#quoteReplacement(String)}, but we
     * need to keep this around for backwards compatibility.
     *
     * @deprecated Don't use this for new nodes. Use {@link #processReplacementString(String, PatternType)} instead.
     */
    @Deprecated
    private static final Pattern LEGACY_BACKREF_PATTERN = Pattern.compile("(\\$\\d+|\\$\\{[A-Za-z][A-Za-z0-9]*\\})");

    /**
     * Old versions of the string replacer node (and friends) had a bug where wildcard replacements would replace double
     * backslashes with a single backslash. So e.g. if you had input "abc", target pattern "abc", and replacement string
     * "\\cde", the result would be "\cde" instead of "\\cde". This method is here to preserve that bug, because
     * backwards compatibility is important.
     *
     * However, this method should ONLY be used if you need that backward compatibility. Please don't touch it
     * otherwise.
     *
     * @param replacement the replacement string. It is assumed to have already been processed
     * @param patternType the pattern type. The behaviour will be different depending on which type is used.
     *
     * @return the result of the replacement
     *
     * @deprecated Don't use this for new nodes. Use <i>only</i> if you absolutely need to maintain backwards
     *             compatibility with the old bug in the wildcard replacement.
     *
     * @since 5.6
     */
    @Deprecated
    public static String processReplacementStringWithWildcardBackwardCompatibility(final String replacement,
        final PatternType patternType) {
        return switch (patternType) {
            case WILDCARD -> LEGACY_BACKREF_PATTERN.matcher(replacement).replaceAll("\\\\$1");
            default -> processReplacementString(replacement, patternType);
        };
    }

    /**
     * See {@link #compilePattern(String, PatternType, CaseMatching, boolean)} which should be used in case unicode is
     * to be properly supported.
     *
     * @param pattern the pattern to compile
     * @param patternType the pattern type. If it is LITERAL, the pattern will be escaped so that it only matches
     *            strings literally equal to the pattern. If it is WILDCARD, the pattern will be converted to a regex
     *            pattern before compiling.
     * @param caseMatching the case matching option. If it is CASEINSENSITIVE, the pattern will be compiled with the
     *            CASE_INSENSITIVE flag.
     * @param escapeWildcards whether to escape wildcards. If true, this means that wildcard parameters may use their
     *            special meaning when escaped, that is, the wildcard string "\*" would match a literal asterisk if this
     *            parameter is true. If it is false, it would match a literal backslash followed by any number of
     *            characters. This is only relevant for WILDCARD patterns.
     * @param supportUnicodeCase whether to support unicode case matching. This should be true unless this would
     *            introduce breaking changes regarding backwards-compatibility.
     * @return the compiled pattern
     * @throws IllegalSearchPatternException if the search pattern is invalid and could not be compiled
     */
    public static Pattern compilePattern( //
        String pattern, //
        final PatternType patternType, //
        final CaseMatching caseMatching, //
        final boolean escapeWildcards, //
        final boolean supportUnicodeCase //
    ) throws IllegalSearchPatternException {
        int flags = settingsToFlags(patternType, caseMatching, supportUnicodeCase);
        if (patternType == PatternType.WILDCARD) {
            pattern = WildcardToRegexUtil.wildcardToRegex(pattern, escapeWildcards);
        } else if (patternType == PatternType.LITERAL) {
            pattern = pattern.isEmpty() ? pattern : Pattern.quote(pattern);
        }

        try {
            return Pattern.compile(pattern, flags);
        } catch (PatternSyntaxException e) {
            throw new IllegalSearchPatternException(pattern, e);
        }
    }

    private static int settingsToFlags(final PatternType patternType, final CaseMatching caseMatching,
        final boolean supportUnicodeCase) {
        int flags = 0;
        if (caseMatching == CaseMatching.CASEINSENSITIVE) {
            flags |= Pattern.CASE_INSENSITIVE;
            if (supportUnicodeCase) {
                flags |= Pattern.UNICODE_CASE;
            }
        }
        if (patternType == PatternType.WILDCARD) {
            flags |= Pattern.MULTILINE | Pattern.DOTALL;
        }
        return flags;
    }

    /**
     * Compiles a pattern according to the given pattern and options. The flags are set according to the options.
     *
     * @param pattern the pattern to compile
     * @param patternType the pattern type. If it is LITERAL, the pattern will be escaped so that it only matches
     *            strings literally equal to the pattern. If it is WILDCARD, the pattern will be converted to a regex
     *            pattern before compiling.
     * @param caseMatching the case matching option. If it is CASEINSENSITIVE, the pattern will be compiled with the
     *            CASE_INSENSITIVE flag.
     * @param escapeWildcards whether to escape wildcards. If true, this means that wildcard parameters may use their
     *            special meaning when escaped, that is, the wildcard string "\*" would match a literal asterisk if this
     *            parameter is true. If it is false, it would match a literal backslash followed by any number of
     *            characters. This is only relevant for WILDCARD patterns.
     * @return the compiled pattern
     * @throws IllegalSearchPatternException if the search pattern is invalid and could not be compiled
     */
    public static Pattern compilePattern( //
        final String pattern, //
        final PatternType patternType, //
        final CaseMatching caseMatching, //
        final boolean escapeWildcards //
    ) throws IllegalSearchPatternException {
        return compilePattern(pattern, patternType, caseMatching, escapeWildcards, true);
    }

    /**
     * The result of a pattern replacement. It contains the result string and a flag that indicates whether the
     * replacement was made or not.
     *
     * @param result the result string
     * @param wasReplaced whether the replacement was made or not
     */
    public static record ReplacementResult( //
        String result, //
        boolean wasReplaced //
    ) {

        static ReplacementResult replaced(final String result) {
            return new ReplacementResult(result, true);
        }

        static ReplacementResult notReplaced() {
            return new ReplacementResult("", false);
        }

        /**
         * Returns the result as an Optional. If the pattern was replaced, the result is present. Otherwise, it is
         * empty.
         *
         * @return the result as an Optional
         */
        public Optional<String> asOptional() {
            return wasReplaced ? Optional.of(result) : Optional.empty();
        }
    }

    /**
     * Exception that indicates that pattern is malformed and could not be compiled.
     */
    public static class IllegalSearchPatternException extends Exception {
        private static final long serialVersionUID = -4102522911614414788L;

        private final String m_pattern;

        private final PatternSyntaxException m_syntaxException;

        /**
         * Constructs a new exception with the specified detail message and cause.
         *
         * @param pattern the invalid pattern
         * @param cause the cause of the exception
         */
        public IllegalSearchPatternException(final String pattern, final PatternSyntaxException cause) {
            super(cause);
            m_syntaxException = cause;
            m_pattern = pattern;
        }

        /**
         * @return the invalid pattern
         */
        public String getPattern() {
            return m_pattern;
        }

        /**
         * @return the plain syntax error without context information.
         */
        public PatternSyntaxException getSyntaxException() {
            return m_syntaxException;
        }

        /**
         * We restrict the message to the pattern on purpose here. Use {@link #getSyntaxException()} to get the plain
         * error.
         *
         * @return the message with the pattern included
         */
        @Override
        public String getMessage() {
            return String.format("Invalid pattern \"%s\".", getPattern());
        }
    }

    /**
     * Exception that indicates that a pattern replacement could not be executed (e.g. there is not enough capture
     * groups).
     */
    public static class IllegalReplacementException extends Exception {
        private static final long serialVersionUID = -34519310831437955L;

        private final Pattern m_pattern;

        private final String m_replacement;

        private final String m_input;

        /**
         * Constructs a new exception with the specified detail message and cause.
         *
         * @param pattern the pattern used for the replacement
         * @param replacement the invalid replacement string
         * @param input the input string
         * @param cause the cause of the exception
         */
        public IllegalReplacementException(final Pattern pattern, final String replacement, final String input,
            final Throwable cause) {
            super(cause.getMessage());
            m_pattern = pattern;
            m_replacement = replacement;
            m_input = input;
        }

        /**
         * @return the pattern used for the replacement
         */
        public Pattern getPattern() {
            return m_pattern;
        }

        /**
         * @return the invalid replacement string
         */
        public String getReplacement() {
            return m_replacement;
        }

        /**
         * @return the input string
         */
        public String getInput() {
            return m_input;
        }

        /**
         * @return the message with the pattern, replacement and input included
         */
        @Override
        public String getMessage() {
            return String.format("Could not replace pattern \"%s\" in \"%s\" with \"%s\": %s", getPattern().pattern(),
                getInput(), getReplacement(), super.getMessage());
        }

        /**
         * @return the plain error message without context information.
         */
        public String getCauseMessage() {
            return super.getMessage();
        }
    }
}
