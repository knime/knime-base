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
 *   15 Dec 2023 (jasper): created
 */
package org.knime.base.node.preproc.regexsplit;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Extracts the number and names of the capture groups in a compiled {@link Pattern}. This is not public java API, so to
 * be able to use reflection on this, we requires <code>--add-opens=java.base/java.util.regex=ALL-UNNAMED</code>.
 *
 * TODO remove this class as soon as Java 21 is available
 * https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/regex/Pattern.html#namedGroups()
 *
 * @author Carl Witt, KNIME AG, Zurich, Switzerland
 */
final class CaptureGroupExtractor {

    /**
     * Abstracts a capture group in a regular expression
     *
     * @author Jasper Krauter, KNIME GmbH, Konstanz, Germany
     */
    record CaptureGroup(int index, Optional<String> name) {
        boolean isNamed() {
            return name.isPresent();
        }
    }

    /** A list of parsed capture groups */
    private final List<CaptureGroup> m_captureGroups;

    private CaptureGroupExtractor(final Pattern p) {

        // this includes named and unnamed groups
        final var numGroups = p.matcher("").groupCount();

        var groupNames = getGroupNames(p);
        // fill with anonymous groups where no name is given
        // the first group has index 1 - the implicit group 0 (the entire expression) does not count
        // against the groupCount() but makes the indexing of explicit groups one-based.
        m_captureGroups = IntStream.rangeClosed(1, numGroups)
            .mapToObj(i -> new CaptureGroup(i, Optional.ofNullable(groupNames.get(i)))) //
            .toList();
    }

    /**
     * Access the package local information of a pattern that states its named groups.
     *
     * @param p to analyze
     * @return mapping from group index (first group has index 1) to group name, only for the named groups
     * @throws NoSuchFieldException
     * @throws IllegalAccessException
     */
    private static Map<Integer, String> getGroupNames(final Pattern p) {
        try {
            var namedGroupsField = Pattern.class.getDeclaredField("namedGroups");
            namedGroupsField.setAccessible(true); // NOSONAR: writing a parser for regular expression patterns is worse
            @SuppressWarnings("unchecked")
            var namedGroups = (Map<String, Integer>)namedGroupsField.get(p);

            if (namedGroups == null) {
                namedGroups = Map.of();
            }

            return namedGroups.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry<String, Integer>::getValue, Map.Entry<String, Integer>::getKey));
        } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Execute the parser on the given pre-compiled pattern
     *
     * @param p pattern in question
     * @return A list of capture groups, in order of occurrence. The group index starts at one.
     */
    static List<CaptureGroup> parse(final Pattern p) {
        return new CaptureGroupExtractor(p).m_captureGroups;
    }

}
