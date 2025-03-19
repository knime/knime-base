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
 *   Mar 21, 2025 (david): created
 */
package org.knime.base.node.preproc.columnrenameregex;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

import org.knime.core.node.KNIMEException;
import org.knime.core.util.UniqueNameGenerator;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Label;

/**
 * Utilities for renaming columns using regular expressions.
 *
 * @author David Hickey, TNG Technology Consulting GmbH
 */
@SuppressWarnings("restriction")
public final class RegexRenameUtils {

    /**
     * take all regex chars with special meanings except for ? and *, and escape them. Then, use this to create a
     * character class than can be used to escape other regexes.
     */
    private static final Pattern ESCAPER_PATTERN =
        Pattern.compile("[" + "<([{\\^-=$!|]})+.>".replaceAll(".", "\\\\$0") + "]"); // NOSONAR regex is correct

    /**
     * Given a wildcard pattern where '*' matches any sequence of characters and '?' matches any single character,
     * convert it to a safe regex pattern.
     *
     * @param wildcard a wildcard pattern.
     * @return a regex pattern that matches the same strings as the wildcard pattern would.
     */
    public static String wildcardStringToRegexString(final String wildcard) {
        return ESCAPER_PATTERN.matcher(wildcard).replaceAll("\\\\$0") //
            // replace * with .* and ? with .
            .replace("*", ".*") //
            .replace("?", ".");
    }

    /**
     * Compute a map of old column names to new column names using a regular expression pattern. Note that:
     * <ul>
     * <li>the output might have multiple identical values, which will seriously break execution. See
     * {@link #fixCollisions} to adjust that.</li>
     * <li>this function won't escape the replacement. If you need that, e.g. because you're doing a wildcard
     * replacement, you should use {@link Matcher#quoteReplacement(String)} before calling this function.</li>
     * <li>The output is a {@link LinkedHashMap} which means that it will always iterate in the same order as the
     * provided names.</li>
     *
     * @param oldNames
     * @param replacerPattern
     * @param replacement a replacement string that will be used to replace the matched parts of the old column names.
     * @return a map of old names to new names.
     */
    public static LinkedHashMap<String, String> columnRenameMappings(final String[] oldNames,
        final Pattern replacerPattern, final String replacement) {

        try {
            return Arrays.stream(oldNames) //
                .collect(Collectors.toMap( //
                    Function.identity(), //
                    n -> replacerPattern.matcher(n).replaceAll(replacement), //
                    (a, b) -> a, //
                    LinkedHashMap::new //
                ));
        } catch (IndexOutOfBoundsException e) {
            // if the replacement string contains a backreference that doesn't exist, the pattern
            // will throw an IndexOutOfBoundsException.
            throw new KNIMEException("Replacement replacement string has an invalid group: " + replacement, e)
                .toUnchecked();
        }
    }

    /**
     * Check if any columns actually got renamed. May be a good idea to warn if this returns true, because that means
     * the output is the same
     *
     * @param renames
     * @return false if any column was renamed, true otherwise
     */
    public static boolean renamesAreAllSame(final Map<String, String> renames) {
        return renames.entrySet().stream().allMatch(e -> e.getKey().equals(e.getValue()));
    }

    /**
     * Check if the new column names have any collisions, i.e. if there are duplicates in the new set of column names,
     * which would usually cause node execution to fail. If this returns true, you could use {@link #fixCollisions} to
     * adjust the names to enforce uniqueness.
     *
     * @param renames a map of old names to new names.
     * @return true if there are any collisions in the new names.
     */
    public static boolean renamesHaveCollisions(final Map<String, String> renames) {
        return renames.values().stream().distinct().count() < renames.size();
    }

    /**
     * Use a {@link UniqueNameGenerator} to fix collisions in a map of old names to new names. Any duplicate target
     * names will be adjusted to be unique.
     *
     * @param renames a map of old names to new names.
     * @return a map of old names to new names with all collisions resolved
     */
    public static LinkedHashMap<String, String> fixCollisions(final Map<String, String> renames) {
        var uniqueNameGenerator = new UniqueNameGenerator(Set.of());

        var fixed = new LinkedHashMap<String, String>();
        for (var entry : renames.entrySet()) {
            var newName = uniqueNameGenerator.newName(entry.getValue());
            fixed.put(entry.getKey(), newName);
        }

        return fixed;
    }

    /**
     * Create a pattern that can be used to rename columns based on the provided settings.
     *
     * @param patternString the pattern string to use. Could be a literal, regex, or wildcard pattern, depending on the
     *            value of the patternType parameter.
     * @param patternType the type of the pattern. Literal, regex, or wildcard.
     * @param caseSensitivity whether the pattern should be case sensitive or not.
     * @param replacementStrategy whether the pattern should match the whole string, or every single matching substring.
     * @return a compiled pattern with an appropriate set of flags and adjustments based on the arguments to the
     *         function.
     * @throws PatternSyntaxException if the pattern string is invalid.
     */
    public static Pattern createColumnRenamePattern( //
        final String patternString, //
        final PatternType patternType, //
        final CaseSensitivity caseSensitivity, //
        final ReplacementStrategy replacementStrategy //
    ) throws PatternSyntaxException {
        var flags = Pattern.UNICODE_CHARACTER_CLASS;
        if (caseSensitivity == CaseSensitivity.CASE_INSENSITIVE) {
            flags |= Pattern.CASE_INSENSITIVE;
        }

        var patternStringProcessed = switch (patternType) {
            case REGEX -> patternString;
            case WILDCARD -> RegexRenameUtils.wildcardStringToRegexString(patternString);
            case LITERAL -> Pattern.quote(patternString);
        };

        if (replacementStrategy == ReplacementStrategy.WHOLE_STRING) {
            // in this case we want to match the entire column name. So, prepend ^ and append $
            // this will even work find if the user has already included them, because multiple
            // ^ at the start or multiple $ don't affect the behaviour for replacement here.
            patternStringProcessed = "^" + patternStringProcessed + "$";
        }

        return Pattern.compile(patternStringProcessed, flags);
    }

    @SuppressWarnings("javadoc")
    public enum PatternType {
            @Label(value = "Literal", description = """
                    Replace the exact string specified in the pattern with the \
                    replacement text.
                    """)
            LITERAL, //
            @Label(value = "Wildcard", description = """
                    Use wildcards to match multiple characters. The wildcard \
                    character "*" matches any number of characters and the \
                    wildcard character "?" matches exactly one character.
                    """)
            WILDCARD, //
            @Label(value = "Regular Expression", description = """
                    Use a regular expression to match the pattern. This allows for \
                    more complex pattern matching. See <a href="https://docs.oracle.com\
                    /en/java/javase/17/docs/api/java.base/java/util/regex/Pattern.html">\
                    the Java documentation</a> for more information on regular expressions.
                    """)
            REGEX;
    }

    @SuppressWarnings("javadoc")
    public enum CaseSensitivity {
            @Label(value = "Case Sensitive", description = """
                    The pattern is case sensitive, so "Example" will not match \
                    "example".
                    """)
            CASE_SENSITIVE, //
            @Label(value = "Case Insensitive", description = """
                    The pattern is case insensitive, so "Example" will match \
                    "example".
                    """)
            CASE_INSENSITIVE;
    }

    @SuppressWarnings("javadoc")
    public enum ReplacementStrategy {
            @Label(value = "Whole string", description = """
                    replaces the entire string with the replacement string, \
                    requiring an exact match of the whole string.
                    """)
            WHOLE_STRING, //
            @Label(value = "All occurrences", description = """
                    All occurrences replaces all occurrences of the pattern with \
                    the replacement string. Note that when e.g. matching on the \
                    RegEx-pattern .*, an empty string at the end of the input is \
                    also matched and replaced. To avoid that, use e.g. the pattern \
                    ^.* to indicate that the match has to start at the beginning \
                    of the string.
                    """)
            ALL_OCCURENCES;
    }
}
