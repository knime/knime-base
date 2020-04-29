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

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.config.Config;
import org.knime.filehandling.core.defaultnodesettings.filtermode.FileAndFolderFilter.FilterType;

/**
 * This class stores the data from the {@link FilterOptionsPanel}.
 *
 * @author Simon Schmid, KNIME GmbH, Konstanz, Germany
 * @author Mareike Hoeger, KNIME GmbH, Konstanz, Germany
 */
@SuppressWarnings("javadoc")
final class FilterOptionsSettings {

    /** Configuration key for the option to filter files by extension in selected folder. */
    private static final String CFG_FILES_FILTER_BY_EXTENSION = "filter_files_extension";

    /** Configuration key to store the filter expression for the file extension filter. */
    private static final String CFG_FILES_EXTENSION_EXPRESSION = "files_extension_expression";

    /** Configuration key to store whether the filter expression for file extensions is case sensitive or not. */
    private static final String CFG_FILES_EXTENSION_CASE_SENSITIVE = "files_extension_case_sensitive";

    /** Configuration key for the option to filter files by name in selected folder. */
    private static final String CFG_FILES_FILTER_BY_NAME = "filter_files_name";

    /** Configuration key to store the filter expression for the file name filter. */
    private static final String CFG_FILES_NAME_EXPRESSION = "files_name_expression";

    /** Configuration key to store whether the filter expression for file names is case sensitive or not. */
    private static final String CFG_FILES_NAME_CASE_SENSITIVE = "files_name_case_sensitive";

    /** Configuration key to store the file name filter type. */
    private static final String CFG_FILES_NAME_FILTER_TYPE = "files_name_filter_type";

    /** Configuration key for the option to include hidden files. */
    private static final String CFG_INCLUDE_HIDDEN_FILES = "include_hidden_files";

    /** Configuration key for the option to filter folders by name in selected folder. */
    private static final String CFG_FOLDERS_FILTER_BY_NAME = "filter_folders_name";

    /** Configuration key to store the filter expression for the folder name filter. */
    private static final String CFG_FOLDERS_NAME_EXPRESSION = "folders_name_expression";

    /** Configuration key to store whether the filter expression for folder names is case sensitive or not. */
    private static final String CFG_FOLDERS_NAME_CASE_SENSITIVE = "folders_name_case_sensitive";

    /** Configuration key to store the folder name filter type. */
    private static final String CFG_FOLDERS_NAME_FILTER_TYPE = "folders_name_filter_type";

    /** Configuration key for the option to include hidden folders. */
    private static final String CFG_INCLUDE_HIDDEN_FOLDERS = "include_hidden_folders";

    /** True, if hidden files should be included. */
    private boolean m_includeHiddenFiles = false;

    /** True, if files should be filtered by extension. */
    private boolean m_filterFilesByExtension;

    /** The expression used to filter files by extension in the selected folder/directory. */
    private String m_filesExtensionExpression;

    /** True, if expression to filter should work regardless the case of the file extension. */
    private boolean m_filesExtensionCaseSensitive = false;

    /** True, if files should be filtered by name. */
    private boolean m_filterFilesByName = false;

    /** The expression used to filter files in the selected folder/directory. */
    private String m_filesNameExpression = DEFAULT_NAME_EXPRESSION;

    /** True, if expression to filter should work regardless the case of the file names. */
    private boolean m_filesNameCaseSensitive = false;

    /** Mode used to filter files (e.g. regex or wildcard). */
    private FilterType m_filesNameFilterType = DEFAULT_FILTER;

    /** True, if hidden folders should be included. */
    private boolean m_includeHiddenFolders = false;

    /** True, if folders should be filtered by name. */
    private boolean m_filterFoldersByName = false;

    /** The expression used to filter folders in the selected folder/directory. */
    private String m_foldersNameExpression = DEFAULT_NAME_EXPRESSION;

    /** True, if expression to filter should work regardless the case of the folder names. */
    private boolean m_foldersNameCaseSensitive = false;

    /** Mode used to filter folders (e.g. regex or wildcard). */
    private FilterType m_foldersNameFilterType = DEFAULT_FILTER;

    /** The default filter. */
    private static final FilterType DEFAULT_FILTER = FilterType.WILDCARD;

    /** The default filter expression. */
    private static final String DEFAULT_EXTENSION_EXPRESSION = "";

    /** The default filter expression. */
    private static final String DEFAULT_NAME_EXPRESSION = "*";

    /**
     * Default constructor for filter settings.
     */
    FilterOptionsSettings() {
        this(new String[0]);
    }

    private FilterOptionsSettings(final boolean filterFilesByExtension, final String filesExtensionExpression) {
        m_filterFilesByExtension = filterFilesByExtension;
        m_filesExtensionExpression = filesExtensionExpression;
    }

    /**
     * Filter settings with pre-set file suffixes to filter.
     *
     * @param fileSuffixes possibly empty array for file suffixes that should be filtered
     */
    FilterOptionsSettings(final String[] fileSuffixes) {
        this(fileSuffixes.length > 0,
            fileSuffixes.length > 0 ? String.join(";", fileSuffixes) : DEFAULT_EXTENSION_EXPRESSION);
    }

    /**
     * @return the includeHiddenFiles
     */
    boolean isIncludeHiddenFiles() {
        return m_includeHiddenFiles;
    }

    /**
     * @param includeHiddenFiles the includeHiddenFiles to set
     */
    void setIncludeHiddenFiles(final boolean includeHiddenFiles) {
        m_includeHiddenFiles = includeHiddenFiles;
    }

    /**
     * @return the filterFilesByExtension
     */
    boolean isFilterFilesByExtension() {
        return m_filterFilesByExtension;
    }

    /**
     * @param filterFilesByExtension the filterFilesByExtension to set
     */
    void setFilterFilesByExtension(final boolean filterFilesByExtension) {
        m_filterFilesByExtension = filterFilesByExtension;
    }

    /**
     * @return the filesExtensionExpression
     */
    String getFilesExtensionExpression() {
        return m_filesExtensionExpression;
    }

    /**
     * @param filesExtensionExpression the filesExtensionExpression to set
     */
    void setFilesExtensionExpression(final String filesExtensionExpression) {
        m_filesExtensionExpression = filesExtensionExpression;
    }

    /**
     * @return the filesExtensionCaseSensitive
     */
    boolean isFilesExtensionCaseSensitive() {
        return m_filesExtensionCaseSensitive;
    }

    /**
     * @param filesExtensionCaseSensitive the filesExtensionCaseSensitive to set
     */
    void setFilesExtensionCaseSensitive(final boolean filesExtensionCaseSensitive) {
        m_filesExtensionCaseSensitive = filesExtensionCaseSensitive;
    }

    /**
     * @return the filterFilesByName
     */
    boolean isFilterFilesByName() {
        return m_filterFilesByName;
    }

    /**
     * @param filterFilesByName the filterFilesByName to set
     */
    void setFilterFilesByName(final boolean filterFilesByName) {
        m_filterFilesByName = filterFilesByName;
    }

    /**
     * @return the filesNameExpression
     */
    String getFilesNameExpression() {
        return m_filesNameExpression;
    }

    /**
     * @param filesNameExpression the filesNameExpression to set
     */
    void setFilesNameExpression(final String filesNameExpression) {
        m_filesNameExpression = filesNameExpression;
    }

    /**
     * @return the filesNameCaseSensitive
     */
    public boolean isFilesNameCaseSensitive() {
        return m_filesNameCaseSensitive;
    }

    /**
     * @param filesNameCaseSensitive the filesNameCaseSensitive to set
     */
    void setFilesNameCaseSensitive(final boolean filesNameCaseSensitive) {
        m_filesNameCaseSensitive = filesNameCaseSensitive;
    }

    /**
     * @return the filesNameFilterType
     */
    FilterType getFilesNameFilterType() {
        return m_filesNameFilterType;
    }

    /**
     * @param filesNameFilterType the filesNameFilterType to set
     */
    void setFilesNameFilterType(final FilterType filesNameFilterType) {
        m_filesNameFilterType = filesNameFilterType;
    }

    /**
     * @return the includeHiddenFolders
     */
    boolean isIncludeHiddenFolders() {
        return m_includeHiddenFolders;
    }

    /**
     * @param includeHiddenFolders the includeHiddenFolders to set
     */
    void setIncludeHiddenFolders(final boolean includeHiddenFolders) {
        m_includeHiddenFolders = includeHiddenFolders;
    }

    /**
     * @return the filterFoldersByName
     */
    boolean isFilterFoldersByName() {
        return m_filterFoldersByName;
    }

    /**
     * @param filterFoldersByName the filterFoldersByName to set
     */
    void setFilterFoldersByName(final boolean filterFoldersByName) {
        m_filterFoldersByName = filterFoldersByName;
    }

    /**
     * @return the foldersNameExpression
     */
    String getFoldersNameExpression() {
        return m_foldersNameExpression;
    }

    /**
     * @param foldersNameExpression the foldersNameExpression to set
     */
    void setFoldersNameExpression(final String foldersNameExpression) {
        m_foldersNameExpression = foldersNameExpression;
    }

    /**
     * @return the foldersNameCaseSensitive
     */
    boolean isFoldersNameCaseSensitive() {
        return m_foldersNameCaseSensitive;
    }

    /**
     * @param foldersNameCaseSensitive the foldersNameCaseSensitive to set
     */
    void setFoldersNameCaseSensitive(final boolean foldersNameCaseSensitive) {
        m_foldersNameCaseSensitive = foldersNameCaseSensitive;
    }

    /**
     * @return the foldersNameFilterType
     */
    FilterType getFoldersNameFilterType() {
        return m_foldersNameFilterType;
    }

    /**
     * @param foldersNameFilterType the foldersNameFilterType to set
     */
    void setFoldersNameFilterMode(final FilterType foldersNameFilterType) {
        m_foldersNameFilterType = foldersNameFilterType;
    }

    /**
     * Saves the the file filter settings to the given {@link Config}.
     *
     * @param config the configuration to save to
     */
    void saveToConfig(final Config config) {
        config.addBoolean(CFG_FILES_FILTER_BY_EXTENSION, m_filterFilesByExtension);
        config.addString(CFG_FILES_EXTENSION_EXPRESSION, m_filesExtensionExpression);
        config.addBoolean(CFG_FILES_EXTENSION_CASE_SENSITIVE, m_filesExtensionCaseSensitive);
        config.addBoolean(CFG_FILES_FILTER_BY_NAME, m_filterFilesByName);
        config.addString(CFG_FILES_NAME_EXPRESSION, m_filesNameExpression);
        config.addBoolean(CFG_FILES_NAME_CASE_SENSITIVE, m_filesNameCaseSensitive);
        config.addString(CFG_FILES_NAME_FILTER_TYPE, m_filesNameFilterType.name());
        config.addBoolean(CFG_INCLUDE_HIDDEN_FILES, m_includeHiddenFiles);

        config.addBoolean(CFG_FOLDERS_FILTER_BY_NAME, m_filterFoldersByName);
        config.addString(CFG_FOLDERS_NAME_EXPRESSION, m_foldersNameExpression);
        config.addBoolean(CFG_FOLDERS_NAME_CASE_SENSITIVE, m_foldersNameCaseSensitive);
        config.addString(CFG_FOLDERS_NAME_FILTER_TYPE, m_foldersNameFilterType.name());
        config.addBoolean(CFG_INCLUDE_HIDDEN_FOLDERS, m_includeHiddenFolders);
    }

    /**
     * Loads the filter configuration from the given {@link Config} into the {@link FilterOptionsSettings}.
     *
     * @param config the configuration to load the values from
     * @throws InvalidSettingsException if keys are not available
     */
    void loadFromConfigForModel(final Config config) throws InvalidSettingsException {
        m_filterFilesByExtension = config.getBoolean(CFG_FILES_FILTER_BY_EXTENSION);
        m_filesExtensionExpression = config.getString(CFG_FILES_EXTENSION_EXPRESSION);
        m_filesExtensionCaseSensitive = config.getBoolean(CFG_FILES_EXTENSION_CASE_SENSITIVE);
        m_filterFilesByName = config.getBoolean(CFG_FILES_FILTER_BY_NAME);
        m_filesNameExpression = config.getString(CFG_FILES_NAME_EXPRESSION);
        m_filesNameCaseSensitive = config.getBoolean(CFG_FILES_NAME_CASE_SENSITIVE);
        m_filesNameFilterType = FilterType.valueOf(config.getString(CFG_FILES_NAME_FILTER_TYPE));
        m_includeHiddenFiles = config.getBoolean(CFG_INCLUDE_HIDDEN_FILES);

        m_filterFoldersByName = config.getBoolean(CFG_FOLDERS_FILTER_BY_NAME);
        m_foldersNameExpression = config.getString(CFG_FOLDERS_NAME_EXPRESSION);
        m_foldersNameCaseSensitive = config.getBoolean(CFG_FOLDERS_NAME_CASE_SENSITIVE);
        m_foldersNameFilterType = FilterType.valueOf(config.getString(CFG_FOLDERS_NAME_FILTER_TYPE));
        m_includeHiddenFolders = config.getBoolean(CFG_INCLUDE_HIDDEN_FOLDERS);
    }

    /**
     * Loads the filter configuration from the given {@link Config} into the {@link FilterOptionsSettings}.
     *
     * @param config the configuration to load the values from
     */
    void loadFromConfigForDialog(final Config config) {
        m_filterFilesByExtension = config.getBoolean(CFG_FILES_FILTER_BY_EXTENSION, m_filterFilesByExtension);
        m_filesExtensionExpression = config.getString(CFG_FILES_EXTENSION_EXPRESSION, m_filesExtensionExpression);
        m_filesExtensionCaseSensitive =
            config.getBoolean(CFG_FILES_EXTENSION_CASE_SENSITIVE, m_filesExtensionCaseSensitive);
        m_filterFilesByName = config.getBoolean(CFG_FILES_FILTER_BY_NAME, m_filterFilesByName);
        m_filesNameExpression = config.getString(CFG_FILES_NAME_EXPRESSION, m_filesNameExpression);
        m_filesNameCaseSensitive = config.getBoolean(CFG_FILES_NAME_CASE_SENSITIVE, m_filesNameCaseSensitive);
        m_filesNameFilterType =
            FilterType.valueOf(config.getString(CFG_FILES_NAME_FILTER_TYPE, m_filesNameFilterType.name()));
        m_includeHiddenFiles = config.getBoolean(CFG_INCLUDE_HIDDEN_FILES, m_includeHiddenFiles);

        m_filterFoldersByName = config.getBoolean(CFG_FOLDERS_FILTER_BY_NAME, m_filterFoldersByName);
        m_foldersNameExpression = config.getString(CFG_FOLDERS_NAME_EXPRESSION, m_foldersNameExpression);
        m_foldersNameCaseSensitive = config.getBoolean(CFG_FOLDERS_NAME_CASE_SENSITIVE, m_foldersNameCaseSensitive);
        m_foldersNameFilterType =
            FilterType.valueOf(config.getString(CFG_FOLDERS_NAME_FILTER_TYPE, m_foldersNameFilterType.name()));
        m_includeHiddenFolders = config.getBoolean(CFG_INCLUDE_HIDDEN_FOLDERS, m_includeHiddenFolders);
    }

    /**
     * Validates the {@link Config}, i.e. checks if all configuration keys are available.
     *
     * @param config the configuration to validate.
     * @throws InvalidSettingsException if keys are not available.
     */
    static void validate(final Config config) throws InvalidSettingsException {
        config.getBoolean(CFG_FILES_FILTER_BY_EXTENSION);
        config.getString(CFG_FILES_EXTENSION_EXPRESSION);
        config.getBoolean(CFG_FILES_EXTENSION_CASE_SENSITIVE);
        config.getBoolean(CFG_FILES_FILTER_BY_NAME);
        config.getString(CFG_FILES_NAME_EXPRESSION);
        config.getBoolean(CFG_FILES_NAME_CASE_SENSITIVE);
        final String fileFilterMode = config.getString(CFG_FILES_NAME_FILTER_TYPE);
        if (!FilterType.contains(fileFilterMode)) {
            throw new InvalidSettingsException("'" + fileFilterMode + "' is not a valid filter mode.");
        }
        config.getBoolean(CFG_INCLUDE_HIDDEN_FILES);

        config.getBoolean(CFG_FOLDERS_FILTER_BY_NAME);
        config.getString(CFG_FOLDERS_NAME_EXPRESSION);
        config.getBoolean(CFG_FOLDERS_NAME_CASE_SENSITIVE);
        final String folderFilterMode = config.getString(CFG_FOLDERS_NAME_FILTER_TYPE);
        if (!FilterType.contains(folderFilterMode)) {
            throw new InvalidSettingsException("'" + folderFilterMode + "' is not a valid filter mode.");
        }
        config.getBoolean(CFG_INCLUDE_HIDDEN_FOLDERS);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (m_filesExtensionCaseSensitive ? 1231 : 1237);
        result = prime * result + ((m_filesExtensionExpression == null) ? 0 : m_filesExtensionExpression.hashCode());
        result = prime * result + (m_filesNameCaseSensitive ? 1231 : 1237);
        result = prime * result + ((m_filesNameExpression == null) ? 0 : m_filesNameExpression.hashCode());
        result = prime * result + ((m_filesNameFilterType == null) ? 0 : m_filesNameFilterType.hashCode());
        result = prime * result + (m_filterFilesByExtension ? 1231 : 1237);
        result = prime * result + (m_filterFilesByName ? 1231 : 1237);
        result = prime * result + (m_filterFoldersByName ? 1231 : 1237);
        result = prime * result + (m_foldersNameCaseSensitive ? 1231 : 1237);
        result = prime * result + ((m_foldersNameExpression == null) ? 0 : m_foldersNameExpression.hashCode());
        result = prime * result + ((m_foldersNameFilterType == null) ? 0 : m_foldersNameFilterType.hashCode());
        result = prime * result + (m_includeHiddenFiles ? 1231 : 1237);
        result = prime * result + (m_includeHiddenFolders ? 1231 : 1237);
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof FilterOptionsSettings)) {
            return false;
        }
        final FilterOptionsSettings other = (FilterOptionsSettings)obj;
        if (m_filesExtensionCaseSensitive != other.m_filesExtensionCaseSensitive) {
            return false;
        }
        if (m_filesExtensionExpression == null) {
            if (other.m_filesExtensionExpression != null) {
                return false;
            }
        } else if (!m_filesExtensionExpression.equals(other.m_filesExtensionExpression)) {
            return false;
        }
        if (m_filesNameCaseSensitive != other.m_filesNameCaseSensitive) {
            return false;
        }
        if (m_filesNameExpression == null) {
            if (other.m_filesNameExpression != null) {
                return false;
            }
        } else if (!m_filesNameExpression.equals(other.m_filesNameExpression)) {
            return false;
        }
        if (m_filesNameFilterType != other.m_filesNameFilterType) {
            return false;
        }
        if (m_filterFilesByExtension != other.m_filterFilesByExtension) {
            return false;
        }
        if (m_filterFilesByName != other.m_filterFilesByName) {
            return false;
        }
        if (m_filterFoldersByName != other.m_filterFoldersByName) {
            return false;
        }
        if (m_foldersNameCaseSensitive != other.m_foldersNameCaseSensitive) {
            return false;
        }
        if (m_foldersNameExpression == null) {
            if (other.m_foldersNameExpression != null) {
                return false;
            }
        } else if (!m_foldersNameExpression.equals(other.m_foldersNameExpression)) {
            return false;
        }
        if (m_foldersNameFilterType != other.m_foldersNameFilterType) {
            return false;
        }
        if (m_includeHiddenFiles != other.m_includeHiddenFiles) {
            return false;
        }
        if (m_includeHiddenFolders != other.m_includeHiddenFolders) {
            return false;
        }
        return true;
    }

}
