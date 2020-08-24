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
 *   Apr 14, 2020 (Simon Schmid, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.defaultnodesettings.filtermode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiPredicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.knime.core.node.util.ButtonGroupEnumInterface;
import org.knime.filehandling.core.util.WildcardToRegexUtil;

/**
 * File and folder filter based on {@link FilterOptionsSettings}.
 *
 * @author Simon Schmid, KNIME GmbH, Konstanz, Germany
 * @author Tobias Urhaug, KNIME GmbH, Berlin, Germany
 * @author Mareike Hoeger, KNIME GmbH, Konstanz, Germany
 * @noreference non-public API
 * @noinstantiate non-public API
 */
@SuppressWarnings("javadoc")
public final class FileAndFolderFilter implements BiPredicate<Path, BasicFileAttributes> {

    /**
     * FilterType enumeration used for {@link FileAndFolderFilter}.
     *
     * @author Julian Bunzel, KNIME GmbH, Berlin, Germany
     * @author Simon Schmid, KNIME GmbH, Konstanz, Germany
     */
    enum FilterType implements ButtonGroupEnumInterface {

            /**
             * Only files or folders with names containing the wildcard pass this filter.
             */
            WILDCARD("Wildcard", "Only files/folders with names containing the wildcard pass this filter.",
                "Wildcard patterns contain '*' (sequence of characters) and '?' (one character)."),
            /**
             * Only files or folders with names that match the regex pass this filter.
             */
            REGEX("Regular expression", "Only files/folders with names that match the regex pass this filter.",
                "As an example, the regular expression [0-9]* matches any string of digits. "
                    + "For more examples see java.util.regex.Pattern");

        private final String m_displayText;

        private final String m_description;

        private final String m_inputTooltip;

        private FilterType(final String displayText, final String description, final String tooltip) {
            m_displayText = displayText;
            m_description = description;
            m_inputTooltip = tooltip;
        }

        /**
         * Returns true, if the argument represents a filter type in this enum.
         *
         * @param filterType filter type to check
         * @return true, if the argument represents a filter type in this enum
         */
        public static final boolean contains(final String filterType) {
            return Arrays.stream(values()).anyMatch(f -> f.name().equals(filterType));
        }

        @Override
        public String getText() {
            return m_displayText;
        }

        @Override
        public String getActionCommand() {
            return name();
        }

        @Override
        public String getToolTip() {
            return m_description;
        }

        /**
         * @return the inputTooltip
         */
        public String getInputTooltip() {
            return m_inputTooltip;
        }

        @Override
        public boolean isDefault() {
            return this == WILDCARD;
        }
    }

    /** Total number of filtered files */
    private int m_numberOfFilteredFiles;

    /** Total number of filtered folders */
    private int m_numberOfFilteredFolders;

    /** Total number of hidden files. */
    private int m_numberOfFilteredHiddenFiles;

    /** Total number of hidden folders. */
    private int m_numberOfFilteredHiddenFolders;

    private final FilterOptionsSettings m_filterOptionsSettings;

    private final List<String> m_extensions;

    private final Pattern m_regexFileName;

    private final Pattern m_regexFolderName;

    private final Path m_rootPath;

    /**
     * Constructor using a {@link FilterOptionsSettings} that contains all necessary parameters.
     *
     * @param rootPath the root path
     * @param filterOptionsSettings settings model containing necessary parameters
     */
    public FileAndFolderFilter(final Path rootPath, final FilterOptionsSettings filterOptionsSettings) {
        m_rootPath = rootPath;
        m_filterOptionsSettings = filterOptionsSettings;
        // make sure each extension starts with a dot
        m_extensions = Arrays.stream(m_filterOptionsSettings.getFilesExtensionExpression().split(";"))
            .map(ex -> ex.startsWith(".") ? ex : "." + ex).collect(Collectors.toList());
        m_regexFileName = createRegex(m_filterOptionsSettings.getFilesNameFilterType(),
            m_filterOptionsSettings.getFilesNameExpression(), m_filterOptionsSettings.isFilesNameCaseSensitive());
        m_regexFolderName = createRegex(m_filterOptionsSettings.getFoldersNameFilterType(),
            m_filterOptionsSettings.getFoldersNameExpression(), m_filterOptionsSettings.isFoldersNameCaseSensitive());
    }

    private static Pattern createRegex(final FilterType filterType, final String expression,
        final boolean caseSensitive) {
        final String regexString =
            filterType == FilterType.WILDCARD ? WildcardToRegexUtil.wildcardToRegex(expression, false) : expression;
        return caseSensitive ? Pattern.compile(regexString) : Pattern.compile(regexString, Pattern.CASE_INSENSITIVE);
    }

    private boolean isSatisfiedFileHidden(final Path path) {
        try {
            final boolean accept = m_filterOptionsSettings.isIncludeHiddenFiles() || !Files.isHidden(path);
            if (!accept) {
                ++m_numberOfFilteredHiddenFiles;
            }
            return accept;
        } catch (final IOException ex) {
            return true;
        }
    }

    private boolean isSatisfiedFolderHidden(final Path path, final boolean incCounter) {
        try {
            final boolean accept = m_filterOptionsSettings.isIncludeHiddenFolders() || !Files.isHidden(path);
            if (incCounter && !accept) {
                ++m_numberOfFilteredHiddenFolders;
            }
            return accept;
        } catch (final IOException ex) {
            return true;
        }
    }

    private boolean isSatisfiedFileExtension(final Path path) {
        if (!m_filterOptionsSettings.isFilterFilesByExtension()) {
            return true;
        }
        final String pathAsString = path.getFileName().toString();
        final boolean accept;
        if (m_filterOptionsSettings.isFilesExtensionCaseSensitive()) {
            accept = m_extensions.stream().anyMatch(pathAsString::endsWith);
        } else {
            accept = m_extensions.stream().anyMatch(ext -> pathAsString.toLowerCase().endsWith(ext.toLowerCase()));
        }
        if (!accept) {
            m_numberOfFilteredFiles++;
        }
        return accept;
    }

    private boolean isSatisfiedFileName(final Path path) {
        if (!m_filterOptionsSettings.isFilterFilesByName()) {
            return true;
        }
        final String pathAsString = path.getFileName().toString();
        final boolean accept = m_regexFileName.matcher(pathAsString).matches();
        if (!accept) {
            m_numberOfFilteredFiles++;
        }

        return accept;
    }

    private boolean isSatisfiedFolderName(final Path path, final boolean incCounter) {
        if (!m_filterOptionsSettings.isFilterFoldersByName()) {
            return true;
        }
        final String pathAsString = m_rootPath.relativize(path).toString();
        final boolean accept = m_regexFolderName.matcher(pathAsString).matches();
        if (incCounter && !accept) {
            m_numberOfFilteredFolders++;
        }
        return accept;
    }

    /**
     * Returns the number of filtered files.
     *
     * @return the number of filtered files
     */
    public int getNumberOfFilteredFiles() {
        return m_numberOfFilteredFiles;
    }

    /**
     * Returns the number of filtered folders.
     *
     * @return the number of filtered folders
     */
    public int getNumberOfFilteredFolders() {
        return m_numberOfFilteredFolders;
    }

    /**
     * Returns the number of filtered hidden files.
     *
     * @return the number of filtered hidden files
     */
    public int getNumberOfFilteredHiddenFiles() {
        return m_numberOfFilteredHiddenFiles;
    }

    /**
     * Returns the number of filtered hidden folders.
     *
     * @return the number of filtered hidden folders
     */
    public int getNumberOfFilteredHiddenFolders() {
        return m_numberOfFilteredHiddenFolders;
    }

    /**
     * Tests that the folder structure matches the selected folder filters.
     *
     * @param folder path to the folder to be tested
     * @return {@code true} if the folder matches the selected filters.
     */
    public boolean testFolderName(final Path folder) {
        return isSatisfiedFolderName(folder, false);
    }

    /**
     * Tells whether or not to visit the files in the provided folder with respect to the selected folder filters.
     *
     * @param folder the folder in question
     * @return {@code true} if the folder has to be visited, {@link false} otherwise
     */
    public boolean visitFolder(final Path folder) {
        return isSatisfiedFolderHidden(folder, false);
    }

    /**
     * Resets the counters of filtered files and filtered folders.
     */
    public void resetCounter() {
        m_numberOfFilteredFiles = 0;
        m_numberOfFilteredFolders = 0;
    }

    @Override
    public boolean test(final Path path, final BasicFileAttributes attrs) {
        if (attrs.isDirectory()) {
            return testFolder(path, true);
        }
        return attrs.isRegularFile() && //
            testFile(path);
    }

    private boolean testFolder(final Path path, final boolean incCounter) {
        return isSatisfiedFolderHidden(path, incCounter) && //
            isSatisfiedFolderName(path, incCounter);
    }

    private boolean testFile(final Path path) {
        return isSatisfiedFileHidden(path) && //
            isSatisfiedFileExtension(path) && //
            isSatisfiedFileName(path);
    }
}
