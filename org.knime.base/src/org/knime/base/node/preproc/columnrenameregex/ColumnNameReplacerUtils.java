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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.knime.base.node.util.regex.CaseMatching;
import org.knime.base.node.util.regex.PatternType;
import org.knime.base.node.util.regex.RegexReplaceUtils;
import org.knime.base.node.util.regex.RegexReplaceUtils.IllegalReplacementException;
import org.knime.base.node.util.regex.RegexReplaceUtils.IllegalSearchPatternException;
import org.knime.base.node.util.regex.ReplacementStrategy;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.util.UniqueNameGenerator;

import com.google.common.collect.Sets;

/**
 * Utilities for renaming columns and fixing resulting name collisions.
 *
 * @author David Hickey, TNG Technology Consulting GmbH
 * @since 5.5
 */
public final class ColumnNameReplacerUtils {

    private ColumnNameReplacerUtils() {
        // utility class
    }

    /**
     * NOTE: you probably want {@link #columnRenameMappings} instead of this method, unless you need to maintain
     * backwards compatibility with an old node that had this bug.
     *
     * Compute a map of old column names to new column names using a regular expression pattern. Note that:
     * <ul>
     * <li>the output might have multiple identical values, which will seriously break execution. See
     * {@link #fixCollisions} to adjust that.</li>
     * <li>this function won't escape the replacement. If you need that, e.g. because you're doing a wildcard
     * replacement, you should use {@link Matcher#quoteReplacement(String)} before calling this function.</li>
     * <li>The output is a {@link LinkedHashMap} which means that it will always iterate in the same order as the
     * provided names.</li>
     * <li>for column names where no replacement took place, there will be no entry in the map. A replacement with the
     * same string is not considered the same as no replacement!</li>
     * </ul>
     *
     * @param oldNames the list of column names that we want to rename.
     * @param patternString a string that will be compiled to a regex and used to match the old column names.
     * @param patternType the type of pattern to use (literal, wildcard, regex).
     * @param caseMatching the case matching to use for the pattern (case sensitive or insensitive).
     * @param replacementStrategy the strategy to use for replacement (e.g. replace all, replace whole string).
     * @param escapeWildcards whether to escape wildcards. If true, this means that wildcard parameters may use their
     *            special meaning when escaped, that is, the wildcard string "\*" would match a literal asterisk if this
     *            parameter is true. If it is false, it would match a literal backslash followed by any number of
     *            characters. This is only relevant for WILDCARD patterns.
     * @param supportUnicodeCase whether to support unicode case matching. This should be true unless this would
     *            introduce breaking changes regarding backwards-compatibility
     * @param replacement a replacement string that will be used to replace the matched parts of the old column names.
     *            It will be escaped if necessary (i.e. if the pattern type is anything other than regex).
     * @return a map of old names to new names.
     * @throws IllegalSearchPatternException if the pattern string is not a valid regex.
     * @throws IllegalReplacementException if the replacement string is not valid, e.g. if it contains backreferences
     *             that aren't specified in the pattern string.
     *
     * @deprecated Use {@link #columnRenameMappings} instead, unless you need this for backwards compatibility.
     *
     * @since 5.6
     */
    @Deprecated
    public static Map<String, String> columnRenameMappingsWithWildcardBug(final String[] oldNames,
        final String patternString, final PatternType patternType, final CaseMatching caseMatching,
        final ReplacementStrategy replacementStrategy, final boolean escapeWildcards, final boolean supportUnicodeCase,
        String replacement) throws IllegalSearchPatternException, IllegalReplacementException {

        var pattern = RegexReplaceUtils.compilePattern(patternString, patternType, caseMatching, escapeWildcards,
            supportUnicodeCase);

        replacement =
            RegexReplaceUtils.processReplacementStringWithWildcardBackwardCompatibility(replacement, patternType);

        LinkedHashMap<String, String> nameMapping = new LinkedHashMap<>(oldNames.length);
        for (int i = 0; i < oldNames.length; i++) {
            final var oldName = oldNames[i];
            var replacementWithIndex = getReplaceStringWithIndex(replacement, i);
            var replacementResult = RegexReplaceUtils.doReplacement(pattern, replacementStrategy, patternType, oldName,
                replacementWithIndex);
            if (replacementResult.wasReplaced()) {
                // if the replacement was successful, add the new name to the map
                nameMapping.put(oldName, replacementResult.result());
            }
        }

        return nameMapping;
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
     * <li>for column names where no replacement took place, there will be no entry in the map. A replacement with the
     * same string is not considered the same as no replacement!</li>
     * </ul>
     *
     * @param oldNames the list of column names that we want to rename.
     * @param patternString a string that will be compiled to a regex and used to match the old column names.
     * @param patternType the type of pattern to use (literal, wildcard, regex).
     * @param caseMatching the case matching to use for the pattern (case sensitive or insensitive).
     * @param replacementStrategy the strategy to use for replacement (e.g. replace all, replace whole string).
     * @param escapeWildcards whether to escape wildcards. If true, this means that wildcard parameters may use their
     *            special meaning when escaped, that is, the wildcard string "\*" would match a literal asterisk if this
     *            parameter is true. If it is false, it would match a literal backslash followed by any number of
     *            characters. This is only relevant for WILDCARD patterns.
     * @param supportUnicodeCase whether to support unicode case matching. This should be true unless this would
     *            introduce breaking changes regarding backwards-compatibility
     * @param replacement a replacement string that will be used to replace the matched parts of the old column names.
     *            It will be escaped if necessary (i.e. if the pattern type is anything other than regex).
     * @return a map of old names to new names.
     * @throws IllegalSearchPatternException if the pattern string is not a valid regex.
     * @throws IllegalReplacementException if the replacement string is not valid, e.g. if it contains backreferences
     *             that aren't specified in the pattern string.
     */
    public static Map<String, String> columnRenameMappings(final String[] oldNames, final String patternString,
        final PatternType patternType, final CaseMatching caseMatching, final ReplacementStrategy replacementStrategy,
        final boolean escapeWildcards, final boolean supportUnicodeCase, String replacement)
        throws IllegalSearchPatternException, IllegalReplacementException {

        var pattern = RegexReplaceUtils.compilePattern(patternString, patternType, caseMatching, escapeWildcards,
            supportUnicodeCase);

        replacement = RegexReplaceUtils.processReplacementString(replacement, patternType);

        LinkedHashMap<String, String> nameMapping = new LinkedHashMap<>(oldNames.length);
        for (int i = 0; i < oldNames.length; i++) {
            final var oldName = oldNames[i];
            var replacementWithIndex = getReplaceStringWithIndex(replacement, i);
            var replacementResult = RegexReplaceUtils.doReplacement(pattern, replacementStrategy, patternType, oldName,
                replacementWithIndex);
            if (replacementResult.wasReplaced()) {
                // if the replacement was successful, add the new name to the map
                nameMapping.put(oldName, replacementResult.result());
            }
        }

        return nameMapping;
    }

    private static String getReplaceStringWithIndex(final String replace, final int index) {
        if (!replace.contains("$i")) {
            return replace;
        }
        /* replace every $i by index .. unless it is escaped */
        // check starts with $i
        String result = replace.replaceAll("^\\$i", Integer.toString(index));
        // any subsequent occurrence, which is not escaped
        return result.replaceAll("([^\\\\])\\$i", "$1" + index);
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
     * @param extantColumnNames the set of all columns that exist prior to the renaming.
     * @param renames a map of old names to new names.
     * @return a map of old names to new names with all collisions resolved
     */
    public static Map<String, String> fixCollisions(final Set<String> extantColumnNames,
        final Map<String, String> renames) {

        // we should consider only extant names that aren't about to get replaced
        final Set<String> extantUnchangedNames = Sets.difference(extantColumnNames, renames.keySet());

        var uniqueNameGenerator = new UniqueNameGenerator(extantUnchangedNames);

        var fixed = new LinkedHashMap<String, String>();
        for (var entry : renames.entrySet()) {
            var newName = uniqueNameGenerator.newName(entry.getValue());
            fixed.put(entry.getKey(), newName);
        }

        return fixed;
    }

    /**
     * Use the settings to create a {@link Pattern} that can be used to rename columns.
     *
     * @param settings the settings to use
     * @return a pattern that can be used to rename columns, with flags set according to the settings
     *
     * @throws IllegalSearchPatternException if the pattern is invalid
     */
    public static Pattern createColumnRenamePattern(final ColumnNameReplacerNodeSettings settings)
        throws IllegalSearchPatternException {
        return RegexReplaceUtils.compilePattern( //
            settings.m_pattern, //
            settings.m_patternType, //
            settings.m_caseSensitivity, //
            settings.m_enableEscapingWildcard, //
            settings.m_properlySupportUnicodeCharacters //
        );
    }

    /**
     * @param originalNames the original names of the potentially to-be-renamed columns
     * @param settings the node settings
     * @param warningMessageConsumer a consumer for warning messages
     * @return a mapping of renamings from original names to new names
     * @throws InvalidSettingsException if there is an error in the replacement string
     */
    public static Map<String, String> createColumnRenameMappings(final String[] originalNames,
        final ColumnNameReplacerNodeSettings settings, final Consumer<String> warningMessageConsumer)
        throws InvalidSettingsException {

        return createColumnRenameMappingsInternal( //
            originalNames, //
            settings, //
            warningMessageConsumer //
        );
    }

    private static Map<String, String> callColumnRenameMappingMethod( //
        final String[] originalNames, //
        final ColumnNameReplacerNodeSettings settings //
    ) throws InvalidSettingsException, IllegalSearchPatternException, IllegalReplacementException {
        if (settings.m_useNewFixedWildcardBehavior) {
            return ColumnNameReplacerUtils.columnRenameMappings( //
                originalNames, //
                settings.m_pattern, //
                settings.m_patternType, //
                settings.m_caseSensitivity, //
                settings.m_replacementStrategy, //
                settings.m_enableEscapingWildcard, //
                settings.m_properlySupportUnicodeCharacters, //
                settings.m_replacement //
            );
        } else {
            return ColumnNameReplacerUtils.columnRenameMappingsWithWildcardBug( //
                originalNames, //
                settings.m_pattern, //
                settings.m_patternType, //
                settings.m_caseSensitivity, //
                settings.m_replacementStrategy, //
                settings.m_enableEscapingWildcard, //
                settings.m_properlySupportUnicodeCharacters, //
                settings.m_replacement //
            );
        }
    }

    private static Map<String, String> createColumnRenameMappingsInternal( //
        final String[] originalNames, //
        final ColumnNameReplacerNodeSettings settings, //
        final Consumer<String> warningMessageConsumer) throws InvalidSettingsException {

        Map<String, String> renameMapping;
        try {
            renameMapping = callColumnRenameMappingMethod( //
                originalNames, //
                settings //
            );
        } catch (IllegalSearchPatternException e) {
            // we should be covered by validateSettings, so this is an implementation error
            throw new IllegalStateException("Implementation error: " + e.getMessage(), e);
        } catch (IllegalReplacementException e) {
            throw new InvalidSettingsException("Error in replacement string: " + e.getCauseMessage(), e);
        }

        if (renameMapping.isEmpty()) {
            warningMessageConsumer.accept("Pattern did not match any column names. Input remains unchanged.");
        } else if (ColumnNameReplacerUtils.renamesHaveCollisions(renameMapping)) {
            // if there are now duplicate column names, we should warn. But we'll use a unique name generator
            // so we don't actually have an error.
            warningMessageConsumer
                .accept("Pattern replace resulted in duplicate column names. Conflicts were resolved by adding "
                    + "\"(#index)\" suffix.");
        }

        return ColumnNameReplacerUtils.fixCollisions(Set.of(originalNames), renameMapping);
    }
}
