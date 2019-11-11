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
 *   11.11.2019 (Mareike Hoeger, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.filefilter;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.config.Config;
import org.knime.filehandling.core.filefilter.FileFilter.FilterType;

/**
 * This class stores the data from the {@link FileFilterPanel}.
 *
 * @author Mareike Hoeger, KNIME GmbH, Konstanz, Germany
 */
public class FileFilterSettings {

    /** Configuration key for the option to filter files by extension in selected folder. */
    private static final String FILTER_FILES_EXTENSION_KEY = "filter_files_extension";

    /** Configuration key to store the filter expression for the extension filter. */
    private static final String FILTER_EXPRESSION_EXTENSION_KEY = "filter_expression_extension";

    /** Configuration key to store the setting whether the filter expression is case sensitive or not. */
    private static final String FILTER_CASE_SENSITIVE_EXTENSION_KEY = "filter_case_sensitive_extension";

    /** Configuration key for the option to filter files by name in selected folder. */
    private static final String FILTER_FILES_NAME_KEY = "filter_files_name";

    /** Configuration key to store the filter expression for the name filter. */
    private static final String FILTER_EXPRESSION_NAME_KEY = "filter_expression_name";

    /** Configuration key to store the setting whether the filter expression for names is case sensitive or not. */
    private static final String FILTER_CASE_SENSITIVE_NAME_KEY = "filter_case_sensitive_name";

    /** Configuration key to store the filter mode. */
    private static final String FILTER_MODE_KEY = "filter_mode";

    /** Configuration key for the option to filter hidden files. */
    private static final String FILTER_HIDDEN_FILES_KEY = "filter_hidden_files";

    /** True, if hidden files should be filtered. */
    private boolean m_filterHiddenFiles;

    /** True, if files should be filtered by extension. */
    private boolean m_filterFilesByExtension;

    /** The expression used to filter files by extension in the selected folder/directory. */
    private String m_filterExpressionExtension;

    /** True, if expression to filter should work regardless the case of the extension. */
    private boolean m_filterCaseSensitiveExtension;

    /** True, if files should be filtered by name. */
    private boolean m_filterFilesByName;

    /** The expression used to filter files in the selected folder/directory. */
    private String m_filterExpressionName;

    /** True, if expression to filter should work regardless the case of the filenames. */
    private boolean m_filterCaseSensitiveName;

    /** Mode used to filter files (e.g. regex or wildcard). */
    private FilterType m_filterType;

    /** The default filter. */
    private static final FilterType DEFAULT_FILTER = FilterType.WILDCARD;

    /** The default filter expression. */
    private static final String DEFAULT_FILTER_EXPRESSION = "*";

    /**
     * Default constructor for filter settings;
     */
    public FileFilterSettings() {
        this(true, false, DEFAULT_FILTER_EXPRESSION, false, false, DEFAULT_FILTER_EXPRESSION, false, DEFAULT_FILTER);
    }

    private FileFilterSettings(final boolean filterHiddenFiles, final boolean filterFilesByExtension,
        final String filterExpressionExtension, final boolean filterCaseSensitiveExtension,
        final boolean filterFilesByName, final String filterExpressionName, final boolean filterCaseSensitiveName,
        final FilterType filterMode) {
        m_filterHiddenFiles = filterHiddenFiles;
        m_filterFilesByExtension = filterFilesByExtension;
        m_filterExpressionExtension = filterExpressionExtension;
        m_filterCaseSensitiveExtension = filterCaseSensitiveExtension;
        m_filterFilesByName = filterFilesByName;
        m_filterExpressionName = filterExpressionName;
        m_filterCaseSensitiveName = filterCaseSensitiveName;
        m_filterType = filterMode;
    }

    /**
     * Filter settings with pre-set suffixes to filter.
     * @param suffixes possibly empty array for suffixes that should be filtered
     */
    public FileFilterSettings(final String[] suffixes) {
        this(true, suffixes.length > 0, suffixes.length > 0 ? String.join(";", suffixes) : DEFAULT_FILTER_EXPRESSION,
            false, false, DEFAULT_FILTER_EXPRESSION, false, DEFAULT_FILTER);
    }

    /**
     * @return the filterHiddenFiles
     */
    public boolean filterHiddenFiles() {
        return m_filterHiddenFiles;
    }

    /**
     * @return the filterFilesByExtension
     */
    public boolean filterFilesByExtension() {
        return m_filterFilesByExtension;
    }

    /**
     * @return the filterExpressionExtension
     */
    public String getFilterExpressionExtension() {
        return m_filterExpressionExtension;
    }

    /**
     * @return the filterCaseSensitiveExtension
     */
    public boolean isFilterCaseSensitiveExtension() {
        return m_filterCaseSensitiveExtension;
    }

    /**
     * @return the filterFilesByName
     */
    public boolean filterFilesByName() {
        return m_filterFilesByName;
    }

    /**
     * @return the filterExpressionName
     */
    public String getFilterExpressionName() {
        return m_filterExpressionName;
    }

    /**
     * @return the filterCaseSensitiveName
     */
    public boolean isFilterCaseSensitiveName() {
        return m_filterCaseSensitiveName;
    }

    /**
     * @return the filterMode
     */
    public FilterType getFilterType() {
        return m_filterType;
    }

    /**
     * @param filterHiddenFiles the filterHiddenFiles to set
     */
    public void setFilterHiddenFiles(final boolean filterHiddenFiles) {
        m_filterHiddenFiles = filterHiddenFiles;
    }

    /**
     * @param filterFilesByExtension the filterFilesByExtension to set
     */
    public void setFilterFilesByExtension(final boolean filterFilesByExtension) {
        m_filterFilesByExtension = filterFilesByExtension;
    }

    /**
     * @param filterExpressionExtension the filterExpressionExtension to set
     */
    public void setFilterExpressionExtension(final String filterExpressionExtension) {
        m_filterExpressionExtension = filterExpressionExtension;
    }

    /**
     * @param filterCaseSensitiveExtension the filterCaseSensitiveExtension to set
     */
    public void setFilterCaseSensitiveExtension(final boolean filterCaseSensitiveExtension) {
        m_filterCaseSensitiveExtension = filterCaseSensitiveExtension;
    }

    /**
     * @param filterFilesByName the filterFilesByName to set
     */
    public void setFilterFilesByName(final boolean filterFilesByName) {
        m_filterFilesByName = filterFilesByName;
    }

    /**
     * @param filterExpressionName the filterExpressionName to set
     */
    public void setFilterExpressionName(final String filterExpressionName) {
        m_filterExpressionName = filterExpressionName;
    }

    /**
     * @param filterCaseSensitiveName the filterCaseSensitiveName to set
     */
    public void setFilterCaseSensitiveName(final boolean filterCaseSensitiveName) {
        m_filterCaseSensitiveName = filterCaseSensitiveName;
    }

    /**
     * @param filterMode the filterMode to set
     */
    public void setFilterType(final FilterType filterMode) {
        m_filterType = filterMode;
    }

    /**
     * Saves the the file filter settings to the given {@link Config}.
     *
     * @param config the configuration to save to.
     */
    public void saveToConfig(final Config config) {

        config.addBoolean(FILTER_FILES_EXTENSION_KEY, filterFilesByExtension());
        config.addString(FILTER_EXPRESSION_EXTENSION_KEY, getFilterExpressionExtension());
        config.addBoolean(FILTER_CASE_SENSITIVE_EXTENSION_KEY, isFilterCaseSensitiveExtension());

        config.addBoolean(FILTER_FILES_NAME_KEY, filterFilesByName());
        config.addString(FILTER_EXPRESSION_NAME_KEY, getFilterExpressionName());
        config.addBoolean(FILTER_CASE_SENSITIVE_NAME_KEY, isFilterCaseSensitiveName());
        config.addString(FILTER_MODE_KEY, getFilterType().name());

        config.addBoolean(FILTER_HIDDEN_FILES_KEY, filterHiddenFiles());
    }

    /**
     * Loads the filter configuration from the given {@link Config} into the {@link FileFilterSettings}.
     *
     * @param config the configuration to load the values from
     */
    public void loadFromConfig(final Config config) {

        setFilterFilesByExtension(config.getBoolean(FILTER_FILES_EXTENSION_KEY, filterFilesByExtension()));
        setFilterExpressionExtension(config.getString(FILTER_EXPRESSION_EXTENSION_KEY, getFilterExpressionExtension()));
        setFilterCaseSensitiveExtension(
            config.getBoolean(FILTER_CASE_SENSITIVE_EXTENSION_KEY, isFilterCaseSensitiveExtension()));

        setFilterFilesByName(config.getBoolean(FILTER_FILES_NAME_KEY, filterFilesByName()));
        setFilterExpressionName(config.getString(FILTER_EXPRESSION_NAME_KEY, getFilterExpressionName()));
        setFilterCaseSensitiveName(config.getBoolean(FILTER_CASE_SENSITIVE_NAME_KEY, isFilterCaseSensitiveName()));
        setFilterType(FilterType.valueOf(config.getString(FILTER_MODE_KEY, getFilterType().name())));

        setFilterHiddenFiles(config.getBoolean(FILTER_HIDDEN_FILES_KEY, filterHiddenFiles()));
    }

    /**
     * Validates the {@link Config}, i.e. checks if all configuration keys are available.
     *
     * @param config the configuration to validate.
     * @throws InvalidSettingsException if keys are not available.
     */
    public void validate(final Config config) throws InvalidSettingsException {
        config.getBoolean(FILTER_FILES_EXTENSION_KEY);
        config.getString(FILTER_EXPRESSION_EXTENSION_KEY);
        config.getBoolean(FILTER_CASE_SENSITIVE_EXTENSION_KEY);

        config.getBoolean(FILTER_FILES_NAME_KEY);
        config.getString(FILTER_EXPRESSION_NAME_KEY);
        config.getBoolean(FILTER_CASE_SENSITIVE_NAME_KEY);
        final String filterMode = config.getString(FILTER_MODE_KEY);
        if (!FilterType.contains(filterMode)) {
            throw new InvalidSettingsException("\"" + filterMode + "\" is not a valid filter type.");
        }

        config.getBoolean(FILTER_HIDDEN_FILES_KEY);
    }

}
