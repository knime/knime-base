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
 *   15 Nov 2023 (jasper): created
 */
package org.knime.base.node.preproc.regexsplit;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import org.knime.base.node.preproc.regexsplit.CaptureGroupExtractor.CaptureGroup;
import org.knime.base.node.preproc.regexsplit.RegexSplitNodeSettings.CaseMatching;
import org.knime.core.node.InvalidSettingsException;

/**
 * This is an auxilary class for the String Splitter (Regex) Node. Provided a node settings instance, it compiles the
 * pattern and offers a method to apply the splitting operation to an input string.
 *
 * @author Jasper Krauter, KNIME GmbH, Konstanz, Germany
 */
final class RegexSplitter {

    private final Pattern m_pattern;

    private final List<CaptureGroup> m_groups;

    private final boolean m_matchWholeString;

    private RegexSplitter(final Pattern p, final boolean matchWholeString) {
        m_pattern = p;
        m_groups = CaptureGroupExtractor.parse(p);
        m_matchWholeString = matchWholeString;
    }

    @SuppressWarnings("deprecation") // Some settings are deprecated -- ISE will be thrown
    static RegexSplitter fromSettings(final RegexSplitNodeSettings settings) throws InvalidSettingsException {
        try {
            var flags = 0;
            if (settings.m_isUnixLines) {
                flags |= Pattern.UNIX_LINES;
            }
            if (settings.m_caseMatching == CaseMatching.CASEINSENSITIVE) {
                flags |= Pattern.CASE_INSENSITIVE;
            }
            if (settings.m_isComments) {
                throw new InvalidSettingsException("""
                        The flag `isComments` is set for this node, but it is not supported by this node. The flag is
                        most likely set by a flow variable. Disable it to use this node.""");
            }
            if (settings.m_isMultiLine) {
                flags |= Pattern.MULTILINE;
            }
            if (settings.m_isLiteral) {
                throw new InvalidSettingsException("""
                        The flag `isLiteral` is set for this node, but it is not supported by this node. The flag is
                        likely set by a flow variable. Disable it to use this node.""");
            }
            if (settings.m_isDotAll) {
                flags |= Pattern.DOTALL;
            }
            if (settings.m_isUnicodeCase) {
                flags |= Pattern.UNICODE_CASE;
            }
            if (settings.m_isCanonEQ) {
                flags |= Pattern.CANON_EQ;
            }
            if (settings.m_isUnicodeCharacterClass) {
                flags |= Pattern.UNICODE_CHARACTER_CLASS;
            }
            return new RegexSplitter(Pattern.compile(settings.m_pattern, flags),
                settings.m_requireWholeMatch);
        } catch (IllegalArgumentException e) { // PatternSyntaxException is an IllegalArgumentException
            throw new InvalidSettingsException("Invalid Pattern: " + settings.m_pattern + ": " + e.getMessage(),
                e);
        }
    }

    List<CaptureGroup> getCaptureGroups() {
        return m_groups;
    }

    /**
     * Apply the splitting operation to an input string
     *
     * @param input the string to split
     * @return A list of captures. A capture can be either the contained String or {@link Optional#empty()} if it's not
     *         present in the match. If the input string doesn't match, only {@link Optional#empty()} is returned
     */
    Optional<List<Optional<String>>> apply(final String input) {
        final var matcher = m_pattern.matcher(input);
        final var found = m_matchWholeString ? matcher.matches() : matcher.find();
        if (found) {
            final var results = new ArrayList<Optional<String>>(matcher.groupCount());
            for (var i = 1; i <= matcher.groupCount(); ++i) {
                final var match = matcher.group(i);
                results.add(match == null ? Optional.empty() : Optional.of(match));
            }
            return Optional.of(results);
        }
        return Optional.empty();
    }

}
