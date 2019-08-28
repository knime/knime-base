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
 *   Aug 15, 2019 (Tobias Urhaug, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.filehandling.core.filefilter;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.knime.base.util.WildcardMatcher;
import org.knime.filehandling.core.defaultnodesettings.SettingsModelFileChooser2;

/**
 * File Filter.
 *
 * @author Tobias Urhaug, KNIME GmbH, Berlin, Germany
 */
public class FileFilter {

    /**
     * FilterType enumeration used for {@link FileFilter}.
     *
     * @author Julian Bunzel, KNIME GmbH, Berlin, Germany
     */
    public enum FilterType {
            /**
             * Only files with certain extensions pass this filter.
             */
            EXTENSIONS("File extension(s)"),
            /**
             * Only files with names containing the wildcard pass this filter.
             */
            WILDCARD("Wildcard"),
            /**
             * Only files with names that matches the regex pass this filter.
             */
            REGEX("Regular expression");

        private final String m_displayText;

        private FilterType(final String displayText) {
            m_displayText = displayText;
        }

        /**
         * Returns the display text of an enum type.
         *
         * @return the display text of an enum type
         */
        public final String getDisplayText() {
            return m_displayText;
        }

        /**
         * Returns the FilterType based on a string.
         *
         * @param displayText The display text used to retrieve the FilterType
         * @return the FilterType
         */
        public static final FilterType fromDisplayText(final String displayText) {
            // Throw something else.
            return Arrays.stream(FilterType.values()).filter((f) -> f.getDisplayText().equals(displayText)).findFirst()
                .get();
        }
    }

    private final FilterType m_filterType;

    private final String m_filterExpression;

    private final boolean m_caseSensitive;

    private final List<String> m_extensions;

    private final Pattern m_regEx;

    /**
     * Create a new instance of {@code FileFilter}.
     *
     * @param filterType the file filter type
     * @param filterExpression the file filter expression
     * @param caseSensitive case sensitive
     */
    public FileFilter(final FilterType filterType, final String filterExpression, final boolean caseSensitive) {
        m_filterType = filterType;
        m_filterExpression = filterExpression;
        m_caseSensitive = caseSensitive;
        String regexPattern = null;
        switch (m_filterType) {
            case EXTENSIONS:
                m_extensions = Arrays.asList(m_filterExpression.split(";"));
                break;
            case WILDCARD:
                regexPattern = WildcardMatcher.wildcardToRegex(m_filterExpression);
                m_extensions = Collections.emptyList();
                break;
            case REGEX:
                regexPattern = m_filterExpression;
                m_extensions = Collections.emptyList();
                break;
            default:
                throw new IllegalStateException("Unknown filter: " + m_filterType);
        }

        if (regexPattern != null) {
            m_regEx = m_caseSensitive ? Pattern.compile(regexPattern)
                : Pattern.compile(regexPattern, Pattern.CASE_INSENSITIVE);
        } else {
            m_regEx = null;
        }
    }

    /**
     * Create a new instance of {@code FileFilter} using a {@link SettingsModelFileChooser2} that contains all
     * necessary parameters.
     *
     * @param settings settings model containing necessary parameters
     */
    public FileFilter(final SettingsModelFileChooser2 settings) {
        // FIXME: ensure that the filtertype in settingsmodel has been validated
        this(FilterType.fromDisplayText(settings.getFilterMode()), settings.getFilterExpression(),
            settings.getCaseSensitive());
    }

    /**
     * Filters a list of paths according to the settings.
     *
     * @param paths List of paths
     * @return List of filtered paths
     */
    public final List<Path> filterFiles(final List<Path> paths) {
        return paths.stream().filter(this::satisfiesFilter).collect(Collectors.toList());
    }

    private final boolean satisfiesFilter(final Path path) {
        // toString might not be the correct method
        final String pathAsString = path.toString();
        switch (m_filterType) {
            case EXTENSIONS:
                if (m_caseSensitive) {
                    return m_extensions.stream().anyMatch(ext -> pathAsString.endsWith(ext));
                } else {
                    return m_extensions.stream()//
                        .anyMatch(ext -> pathAsString.toLowerCase().endsWith(ext.toLowerCase()));
                }
            case WILDCARD:
                // no break
            case REGEX:
                return m_regEx.matcher(pathAsString).matches();
            default:
                return false;
        }
    }
}
