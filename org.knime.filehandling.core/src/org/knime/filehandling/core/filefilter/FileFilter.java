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

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

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
            return Arrays.stream(values()).filter((f) -> f.getDisplayText().equals(displayText)).findFirst().get();
        }

        /**
         * Returns true, if the argument represents a filter type in this enum.
         *
         * @param filterType filter type to check
         * @return true, if the argument represents a filter type in this enum
         */
        public static final boolean contains(final String filterType) {
            return Arrays.stream(values()).anyMatch(f -> f.getDisplayText().equals(filterType));
        }
    }

    private int m_numberOfFilteredFiles;

    private final FilterType m_filterType;

    private final List<String> m_extensions;

    private final Pattern m_regex;

    private final boolean m_caseSensitive;

    /**
     * Create a new instance of {@code FileFilter}.
     *
     * @param filterType the file filter type
     * @param filterExpression the file filter expression
     * @param caseSensitive case sensitive
     */
    public FileFilter(final FilterType filterType, final String filterExpression, final boolean caseSensitive) {
        m_filterType = filterType;
        String regexPattern = null;
        switch (filterType) {
            case EXTENSIONS:
                m_extensions = Arrays.asList(filterExpression.split(";"));
                break;
            case WILDCARD:
                regexPattern = WildcardMatcher.wildcardToRegex(filterExpression);
                m_extensions = Collections.emptyList();
                break;
            case REGEX:
                regexPattern = filterExpression;
                m_extensions = Collections.emptyList();
                break;
            default:
                throw new IllegalStateException("Unknown filter: " + filterType);
        }

        m_caseSensitive = caseSensitive;
        if (regexPattern != null) {
            m_regex = m_caseSensitive ? Pattern.compile(regexPattern)
                : Pattern.compile(regexPattern, Pattern.CASE_INSENSITIVE);
        } else {
            m_regex = null;
        }
    }

    /**
     * Create a new instance of {@code FileFilter} using a {@link SettingsModelFileChooser2} that contains all necessary
     * parameters.
     *
     * @param settings settings model containing necessary parameters
     */
    public FileFilter(final SettingsModelFileChooser2 settings) {
        this(FilterType.fromDisplayText(settings.getFilterMode()), settings.getFilterExpression(),
            settings.getCaseSensitive());
    }

    /**
     * @param entry
     * @param filterType
     * @param extensions
     * @param regex
     * @param caseSensitive
     * @return
     */
    public final boolean isSatisfied(final Path entry) {
        // toString might not be the correct method
        final String pathAsString = entry.toString();
        boolean accept = false;
        if (Files.isRegularFile(entry)) {
            switch (m_filterType) {
                case EXTENSIONS:
                    if (m_caseSensitive) {
                        accept = m_extensions.stream().anyMatch(ext -> pathAsString.endsWith(ext));
                    } else {
                        accept = m_extensions.stream()//
                                .anyMatch(ext -> pathAsString.toLowerCase().endsWith(ext.toLowerCase()));
                    }
                    break;
                case WILDCARD:
                    // no break
                case REGEX:
                    accept = m_regex.matcher(pathAsString).matches();
                    break;
                default:
                    accept = false;
            }
        }
        if (!accept) {
            m_numberOfFilteredFiles++;
        }
        return accept;
    }

    public final int getNumberOfFilteredFiles() {
        return m_numberOfFilteredFiles;
    }

    public final void resetCount() {
        m_numberOfFilteredFiles = 0;
    }

//    public final DirectoryStream.Filter<Path> createFilter() {
//        return new DirectoryStream.Filter<Path>() {
//
//            @Override
//            public boolean accept(final Path entry) throws IOException {
//                return isSatisfied(entry);
//            }
//        };
//    }


//    /**
//     * @param dir
//     * @return
//     * @throws IOException
//     */
//    public final List<Path> listAndFilterFiles(final Path dir) throws IOException {
//        final List<Path> filteredPaths = new ArrayList<>();
//        if (Files.isDirectory(dir)) {
//            try (final DirectoryStream<Path> dirStream = Files.newDirectoryStream(dir, createFilter())) {
//                for (final Path path : dirStream) {
//                    filteredPaths.add(path);
//                }
//            }
//            return filteredPaths;
//        } else {
//            throw new IOException(dir.toString() + " is not a directory.");
//        }
//    }
}
