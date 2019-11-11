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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.knime.core.node.util.ButtonGroupEnumInterface;
import org.knime.filehandling.core.defaultnodesettings.SettingsModelFileChooser2;

/**
 * File Filter.
 *
 * @author Tobias Urhaug, KNIME GmbH, Berlin, Germany
 * @author Mareike Hoeger, KNIME GmbH, Konstanz, Germany
 */
public class FileFilter implements Predicate<Path> {

    /**
     * FilterType enumeration used for {@link FileFilter}.
     *
     * @author Julian Bunzel, KNIME GmbH, Berlin, Germany
     */
    public enum FilterType implements ButtonGroupEnumInterface {

            /**
             * Only files with names containing the wildcard pass this filter.
             */
            WILDCARD("Wildcard", "Only files with names containing the wildcard pass this filter.",
                "Wildcard patterns contain '*' (sequence of characters) and '?' (one character)"),
            /**
             * Only files with names that matches the regex pass this filter.
             */
            REGEX("Regular expression", "Only files with names that matches the regex pass this filter.",
                "Regual expression e.g.[0-9]* matches any string of digits, for more examples in see java.util.regex.Pattern");

        /** The display text */
        private final String m_displayText;

        private final String m_description;

        private final String m_inputTooltip;

        /**
         * Creates a new instance of {@code FilterType}
         *
         * @param displayText The display text
         */
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

        /**
         * {@inheritDoc}
         */
        @Override
        public String getText() {
            return m_displayText;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getActionCommand() {
            return name();
        }

        /**
         * {@inheritDoc}
         */
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

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isDefault() {
            return this.equals(WILDCARD);
        }
    }

    /** Total number of filtered files */
    private int m_numberOfFilteredFiles;

    private final FileFilterSettings m_fileFilterSettings;

    /**
     * Create a new instance of {@code FileFilter} using a {@link SettingsModelFileChooser2} that contains all necessary
     * parameters.
     *
     * @param fileFilterSettings settings model containing necessary parameters
     */
    public FileFilter(final FileFilterSettings fileFilterSettings) {
        m_fileFilterSettings = fileFilterSettings;
    }

    private final boolean isSatisfiedFilterHidden(final Path path) {
        try {
            return !(m_fileFilterSettings.filterHiddenFiles() && Files.isHidden(path));
        } catch (final IOException ex) {
            return true;
        }
    }

    private final boolean isSatisfiedFileExtension(final Path path) {
        if (!m_fileFilterSettings.filterFilesByExtension()) {
            return true;
        }
        boolean accept = false;

        final String pathAsString = path.getFileName().toString();
        final List<String> extensions = Arrays.stream(m_fileFilterSettings.getFilterExpressionExtension().split(";")) //
            .map(ex -> "." + ex).collect(Collectors.toList());
        if (m_fileFilterSettings.isFilterCaseSensitiveExtension()) {
            accept = extensions.stream().anyMatch(pathAsString::endsWith);
        } else {
            accept = extensions.stream()//
                .anyMatch(ext -> pathAsString.toLowerCase().endsWith(ext.toLowerCase()));
        }
        if (!accept) {
            m_numberOfFilteredFiles++;
        }

        return accept;
    }

    /**
     * Method to check whether the path satisfies the filter requirements.
     *
     * @param path the path to check
     * @return true, if the path satisfies the filter requirements
     */
    private final boolean isSatisfiedFileName(final Path path) {
        if (!m_fileFilterSettings.filterFilesByName()) {
            return true;
        }
        // toString might not be the correct method
        final String pathAsString;
        final FilterType filterMode = m_fileFilterSettings.getFilterType();
        final String filterExpression = m_fileFilterSettings.getFilterExpressionName();
        final String regexString =
            filterMode.equals(FilterType.WILDCARD) ? wildcardToRegex(filterExpression, false) : filterExpression;
        final Pattern regex = m_fileFilterSettings.isFilterCaseSensitiveName() ? Pattern.compile(regexString)
            : Pattern.compile(regexString, Pattern.CASE_INSENSITIVE);
        boolean accept = false;

        pathAsString = path.getFileName().toString();
        accept = regex.matcher(pathAsString).matches();

        if (!accept) {
            m_numberOfFilteredFiles++;
        }

        return accept;
    }

    /**
     * Returns the number of filtered files.
     *
     * @return the number of filtered files
     */
    public final int getNumberOfFilteredFiles() {
        return m_numberOfFilteredFiles;
    }

    /**
     * Resets the count.
     */
    public final void resetCount() {
        m_numberOfFilteredFiles = 0;
    }

    /**
     * Converts a wildcard pattern containing '*' and '?' as meta characters into a regular expression. Optionally, the
     * backslash can be enabled as escape character for the wildcards. In this case a backslash has a special meaning
     * and needs may need to be escaped itself.
     *
     * @param wildcard a wildcard expression
     * @param enableEscaping {@code true} if the wildcards may be escaped (i.e. they loose their special meaning) by
     *            prepending a backslash
     * @return the corresponding regular expression
     */
    private static final String wildcardToRegex(final String wildcard, final boolean enableEscaping) {
        // FIXME: This method is copied from org.knime.base.util.WildcardMatcher
        // (we don't want to import org.knime.base)
        // This needs to be replaced by a more convenient solutions
        final StringBuilder buf = new StringBuilder(wildcard.length() + 20);

        for (int i = 0; i < wildcard.length(); i++) {
            final char c = wildcard.charAt(i);
            switch (c) {
                case '*':
                    if (enableEscaping && (i > 0) && (wildcard.charAt(i - 1) == '\\')) {
                        buf.append('*');
                    } else {
                        buf.append(".*");
                    }
                    break;
                case '?':
                    if (enableEscaping && (i > 0) && (wildcard.charAt(i - 1) == '\\')) {
                        buf.append('?');
                    } else {
                        buf.append(".");
                    }
                    break;
                case '\\':
                    if (enableEscaping) {
                        buf.append(c);
                        break;
                    }
                case '^':
                case '$':
                case '[':
                case ']':
                case '{':
                case '}':
                case '(':
                case ')':
                case '|':
                case '+':
                case '.':
                    buf.append("\\");
                    buf.append(c);
                    break;
                default:
                    buf.append(c);
            }
        }

        return buf.toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean test(final Path path) {
        return Files.isRegularFile(path) && //
            isSatisfiedFilterHidden(path) && //
            isSatisfiedFileExtension(path) && //
            isSatisfiedFileName(path);
    }

}
