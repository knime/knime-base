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

import org.knime.base.node.util.regex.RegexReplaceUtils;
import org.knime.base.node.util.regex.RegexReplaceUtils.IllegalReplacementException;
import org.knime.base.node.util.regex.RegexReplaceUtils.IllegalSearchPatternException;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.util.UniqueNameGenerator;

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
     * @param settings the settings to use for the renaming.
     * @return a map of old names to new names.
     * @throws IllegalSearchPatternException if the pattern string is not a valid regex.
     * @throws IllegalReplacementException if the replacement string is not valid, e.g. if it contains backreferences
     *             that aren't specified in the pattern string.
     */
    private static Map<String, String> columnRenameMappings(final String[] oldNames,
        final ColumnNameReplacerNodeSettings settings)
        throws IllegalSearchPatternException, IllegalReplacementException {

        var pattern = RegexReplaceUtils.compilePattern(settings.m_pattern, settings.m_patternType,
            settings.m_caseSensitivity, settings.m_enableEscapingWildcard, settings.m_properlySupportUnicodeCharacters);

        final var replacement =
            RegexReplaceUtils.processReplacementString(settings.m_replacement, settings.m_patternType);

        LinkedHashMap<String, String> nameMapping = new LinkedHashMap<>(oldNames.length);
        for (int i = 0; i < oldNames.length; i++) {
            final var oldName = oldNames[i];
            var replacementWithIndex = getReplaceStringWithIndex(replacement, i);
            var replacementResult = RegexReplaceUtils.doReplacement(pattern, settings.m_replacementStrategy,
                settings.m_patternType, oldName, replacementWithIndex);
            settings.addToRenamesMap(nameMapping, oldName, replacementResult);
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
     * Use a {@link UniqueNameGenerator} to fix collisions in a map of old names to new names. Any duplicate target
     * names will be adjusted to be unique.
     *
     * @param extantColumnNames the set of all columns that exist prior to the renaming.
     * @param renames a map of old names to new names.
     * @param comparisonNames the set of names to compare against when checking for uniqueness.
     * @param warningMessageConsumer to warn the user if collisions were found and fixed.
     * @return a map of old names to new names with all collisions resolved
     */
    private static Map<String, String> fixCollisions(final Map<String, String> renames,
        final Set<String> comparisonNames, final Consumer<String> warningMessageConsumer) {

        var uniqueNameGenerator = new UniqueNameGenerator(comparisonNames);

        var fixed = new LinkedHashMap<String, String>();
        var hadCollisions = false;
        for (var entry : renames.entrySet()) {
            final var renamedName = entry.getValue();
            var newName = uniqueNameGenerator.newName(entry.getValue());
            if (!hadCollisions && !renamedName.trim().equals(newName)) {
                hadCollisions = true;
                warningMessageConsumer
                    .accept("Pattern replace resulted in duplicate column names. Conflicts were resolved by adding "
                        + "\"(#index)\" suffix.");
            }
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

        Map<String, String> renameMapping;
        try {
            renameMapping = columnRenameMappings(originalNames, //
                settings //
            );
        } catch (IllegalSearchPatternException e) {
            // we should be covered by validateSettings, so this is an implementation error
            throw new IllegalStateException("Implementation error: " + e.getMessage(), e);
        } catch (IllegalReplacementException e) {
            throw new InvalidSettingsException("Error in replacement string: " + e.getCauseMessage(), e);
        }

        final var comparisonNames =
            settings.getColumnsToCompareAgainstForCollisions(Set.of(originalNames), renameMapping);
        return ColumnNameReplacerUtils.fixCollisions(renameMapping, comparisonNames, warningMessageConsumer);
    }

}
