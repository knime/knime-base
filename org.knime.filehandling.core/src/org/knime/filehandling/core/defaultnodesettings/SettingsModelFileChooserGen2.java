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
 *   Aug 15, 2019 (bjoern): created
 */
package org.knime.filehandling.core.defaultnodesettings;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.config.Config;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObjectSpec;

/**
 * SettingsModel for {@link DialogComponentFileChooserGen2}.
 *
 * @author Bjoern Lohrmann, KNIME GmbH, Berlin, Germany
 * @author Julian Bunzel, KNIME GmbH, Berlin, Germany
 */
public final class SettingsModelFileChooserGen2 extends SettingsModel {

    /** Configuration key for the option to select a specific file system. */
    private static final String READ_FROM_FILE_SYSTEM = "read_from_file_system";

    /** Configuration key to store the selected file system. */
    private static final String SELECTED_FILE_SYSTEM = "selected_file_system";

    /** Configuration key to store the path of the selected file or folder. */
    private static final String FILE_OR_FOLDER_PATH = "path_of_file_or_folder";

    /** Configuration key for the option to search sub-folders. */
    private static final String SEARCH_SUB_FOLDERS = "search_subfolders";

    /** Configuration key for the option to filter files in selected folder. */
    private static final String FILTER_FILES_IN_FOLDER = "filter_files_in_folder";

    /** Configuration key to store the filter mode. */
    private static final String FILTER_MODE = "filter_mode";

    /** Configuration key to store the filter expression. */
    private static final String FILTER_EXPRESSION = "filter_expression";

    /** Configuration key to store the setting whether the filter expression is case sensitive or not. */
    private static final String FILTER_CASE_SENSITIVITY = "filter_case_sensitivity";

    /** The name of the configuration object. */
    private final String m_configName;

    /** True, if file/folder should be read from a file system other than local. */
    private boolean m_readFromFileSystem = false;

    /** The name of the selected file system. */
    private String m_fileSystemFlowVarName = "Local File System";

    /** Path of selected file or folder. */
    private String m_pathOfFileOrFolder;

    /** True, if sub-folders should be included. */
    private boolean m_searchSubfolder;

    /** True, if files should be filtered. */
    private boolean m_filterFiles;

    /** Mode used to filter files (e.g. regex or wildcard). */
    private String m_filterMode;

    /** The expression used to filter files in the selected folder/directory. */
    private String m_filterExpression;

    /** True, if expression to filter should work regardless the case of the filenames. */
    private boolean m_filterCaseSensitivity;

    // TODO For what again?
    private final SettingsModelString m_path = new SettingsModelString("path", "");

    /**
     * Creates a new instance of {@link SettingsModelFileChooserGen2} with default settings.
     *
     * @param configName the name of the config.
     */
    public SettingsModelFileChooserGen2(final String configName) {
        this(configName, false, "", "", false, false, "wildcard", "*", false);
    }

    /**
     * Creates a new instance of {@link SettingsModelFileChooserGen2}.
     *
     * @param configName the name of the configuration object.
     * @param readFromFileSystem true, if file/folder should be read from a file system other than local
     * @param fileSystemName the name of the selected file system
     * @param pathOfFileOrFolder the path of the selected file or folder
     * @param searchSubfolder true, if sub-folder should be included
     * @param filterFiles true, if files should be filtered
     * @param filterMode mode to filter files in case a multiple files should be read
     * @param filterExpression the expression used to filter files
     * @param caseSensitivity true, if expression should be sensitive to case
     */
    public SettingsModelFileChooserGen2(final String configName, final boolean readFromFileSystem,
        final String fileSystemName, final String pathOfFileOrFolder, final boolean searchSubfolder,
        final boolean filterFiles, final String filterMode, final String filterExpression,
        final boolean caseSensitivity) {
        if ((configName == null) || "".equals(configName)) {
            throw new IllegalArgumentException("The configName must be a " + "non-empty string");
        }
        m_configName = configName;
        m_readFromFileSystem = readFromFileSystem;
        m_fileSystemFlowVarName = fileSystemName;
        m_pathOfFileOrFolder = pathOfFileOrFolder;
        m_searchSubfolder = searchSubfolder;
        m_filterFiles = filterFiles;
        m_filterMode = filterMode;
        m_filterExpression = filterExpression;
        m_filterCaseSensitivity = caseSensitivity;
    }

    /**
     * Sets a new value for the option to read from a specific file system other than the local one.
     *
     * @param newValue Set true, if file/folder should be read from a specific file system other than the local one
     */
    public void setReadFromFileSystem(final boolean newValue) {
        boolean sameValue = (m_readFromFileSystem == newValue);
        m_readFromFileSystem = newValue;
        if (!sameValue) {
            notifyChangeListeners();
        }
    }

    /**
     * Sets the name of the file system to select the file/folder from.
     *
     * @param newValue the name of the file system to select the file/folder from
     */
    public void setFileSystemName(final String newValue) {
        boolean sameValue;

        if (newValue == null) {
            sameValue = (m_fileSystemFlowVarName == null);
        } else {
            sameValue = newValue.equals(m_fileSystemFlowVarName);
        }
        m_fileSystemFlowVarName = newValue;

        if (!sameValue) {
            notifyChangeListeners();
        }
    }

    /**
     * Sets the path of the file/folder.
     *
     * @param newValue The path of the file/folder
     */
    public void setPathOfFileOrFolder(final String newValue) {
        boolean sameValue;

        if (newValue == null) {
            sameValue = (m_pathOfFileOrFolder == null);
        } else {
            sameValue = newValue.equals(m_pathOfFileOrFolder);
        }
        m_pathOfFileOrFolder = newValue;

        if (!sameValue) {
            notifyChangeListeners();
        }
    }

    /**
     * Sets a new value for the option to search sub folder for files.
     *
     * @param newValue Set true, if sub folder should be searched for files
     */
    public void setSearchSubfolder(final boolean newValue) {
        boolean sameValue = (m_searchSubfolder == newValue);
        m_searchSubfolder = newValue;
        if (!sameValue) {
            notifyChangeListeners();
        }
    }

    /**
     * Sets a new value for the option to filter files in a selected folder.
     *
     * @param newValue Set true, if files in a selected folder should be filtered
     */
    public void setFilterFiles(final boolean newValue) {
        boolean sameValue = (m_filterFiles == newValue);
        m_filterFiles = newValue;
        if (!sameValue) {
            notifyChangeListeners();
        }
    }

    /**
     * Sets the filter mode (e.g. wildcard or regular expression) to filter files in a selected folder.
     *
     * @param newValue The filter mode to filter files in a selected folder.
     */
    public void setFilterMode(final String newValue) {
        boolean sameValue;

        if (newValue == null) {
            sameValue = (m_filterMode == null);
        } else {
            sameValue = newValue.equals(m_filterMode);
        }
        m_filterMode = newValue;

        if (!sameValue) {
            notifyChangeListeners();
        }
    }

    /**
     * Sets the filter expression.
     *
     * @param newValue The filter expression
     */
    public void setFilterExpression(final String newValue) {
        boolean sameValue;

        if (newValue == null) {
            sameValue = (m_filterExpression == null);
        } else {
            sameValue = newValue.equals(m_filterExpression);
        }
        m_filterExpression = newValue;

        if (!sameValue) {
            notifyChangeListeners();
        }
    }

    /**
     * Sets the new value for the case sensitivity option.
     *
     * @param newValue New value for the case sensitivity option
     */
    public void setCaseSensitivity(final boolean newValue) {
        boolean sameValue = (m_filterCaseSensitivity == newValue);
        m_filterCaseSensitivity = newValue;
        if (!sameValue) {
            notifyChangeListeners();
        }
    }

    /**
     * Returns true, if file/folder should be read from a file system other than the local one.
     *
     * @return True, if file/folder should be read from a file system other than the local one
     */
    public boolean getReadFromFileSystem() {
        return m_readFromFileSystem;
    }

    /**
     * Returns the name of the selected file system.
     *
     * @return The name of the selected file system
     */
    public String getFileSystemName() {
        return m_fileSystemFlowVarName;
    }

    /**
     * Returns the path of the selected file or folder.
     *
     * @return The path of the selected file or folder
     */
    public String getPathOfFileOrFolder() {
        return m_pathOfFileOrFolder;
    }

    /**
     * Returns true, if sub folders should be included while searching files.
     *
     * @return True, if sub folders should be included while searching files
     */
    public boolean getSearchSubfolder() {
        return m_searchSubfolder;
    }

    /**
     * Returns true, if files should be filtered.
     *
     * @return True, if files should be filtered.
     */
    public boolean getFilterFiles() {
        return m_filterFiles;
    }

    /**
     * Returns the mode to filter files.
     *
     * @return The mode to filter files
     */
    public String getFilterMode() {
        return m_filterMode;
    }

    /**
     * Returns the expression used to filter files.
     *
     * @return The expression used to filter files
     */
    public String getFilterExpression() {
        return m_filterExpression;
    }

    /**
     * Returns true, if case sensitivity option is selected.
     *
     * @return True, if case sensitivity option is selected
     */
    public boolean getCaseSensitivity() {
        return m_filterCaseSensitivity;
    }

    /**
     * @return the path
     */
    public SettingsModelString getPath() {
        return m_path;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected SettingsModelFileChooserGen2 createClone() {
        return new SettingsModelFileChooserGen2(m_configName);
    }

    @Override
    protected String getModelTypeID() {
        return "SMID_FileChooserGen2";
    }

    @Override
    protected String getConfigName() {
        return m_configName;
    }

    @Override
    protected void loadSettingsForDialog(final NodeSettingsRO settings, final PortObjectSpec[] specs)
        throws NotConfigurableException {
        final Config config;
        try {
            config = settings.getConfig(m_configName);
            setReadFromFileSystem(config.getBoolean(READ_FROM_FILE_SYSTEM, m_readFromFileSystem));
            setFileSystemName(config.getString(SELECTED_FILE_SYSTEM, m_fileSystemFlowVarName));
            setPathOfFileOrFolder(config.getString(FILE_OR_FOLDER_PATH, m_pathOfFileOrFolder));
            setSearchSubfolder(config.getBoolean(SEARCH_SUB_FOLDERS, m_searchSubfolder));
            setFilterFiles(config.getBoolean(FILTER_FILES_IN_FOLDER, m_filterFiles));
            setFilterMode(config.getString(FILTER_MODE, m_filterMode));
            setFilterExpression(config.getString(FILTER_EXPRESSION, m_filterExpression));
            setCaseSensitivity(config.getBoolean(FILTER_CASE_SENSITIVITY, m_filterCaseSensitivity));
        } catch (final InvalidSettingsException ise) {
            throw new NotConfigurableException(ise.getMessage());
        }
    }

    @Override
    protected void saveSettingsForDialog(final NodeSettingsWO settings) throws InvalidSettingsException {
        saveSettingsForModel(settings);
    }

    @Override
    protected void validateSettingsForModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        final Config config = settings.getConfig(m_configName);
        config.getBoolean(READ_FROM_FILE_SYSTEM);
        config.getString(SELECTED_FILE_SYSTEM);
        config.getString(FILE_OR_FOLDER_PATH);
        config.getBoolean(SEARCH_SUB_FOLDERS);
        config.getBoolean(FILTER_FILES_IN_FOLDER);
        config.getString(FILTER_MODE);
        config.getString(FILTER_EXPRESSION);
        config.getBoolean(FILTER_CASE_SENSITIVITY);
    }

    @Override
    protected void loadSettingsForModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        final Config config = settings.getConfig(m_configName);
        setReadFromFileSystem(config.getBoolean(READ_FROM_FILE_SYSTEM));
        setFileSystemName(config.getString(SELECTED_FILE_SYSTEM));
        setPathOfFileOrFolder(config.getString(FILE_OR_FOLDER_PATH));
        setSearchSubfolder(config.getBoolean(SEARCH_SUB_FOLDERS));
        setFilterFiles(config.getBoolean(FILTER_FILES_IN_FOLDER));
        setFilterMode(config.getString(FILTER_MODE));
        setFilterExpression(config.getString(FILTER_EXPRESSION));
        setCaseSensitivity(config.getBoolean(FILTER_CASE_SENSITIVITY));
    }

    @Override
    protected void saveSettingsForModel(final NodeSettingsWO settings) {
        final Config config = settings.addConfig(m_configName);
        config.addBoolean(READ_FROM_FILE_SYSTEM, getReadFromFileSystem());
        config.addString(SELECTED_FILE_SYSTEM, getFileSystemName());
        config.addString(FILE_OR_FOLDER_PATH, getPathOfFileOrFolder());
        config.addBoolean(SEARCH_SUB_FOLDERS, getSearchSubfolder());
        config.addBoolean(FILTER_FILES_IN_FOLDER, getFilterFiles());
        config.addString(FILTER_MODE, getFilterMode());
        config.addString(FILTER_EXPRESSION, getFilterExpression());
        config.addBoolean(FILTER_CASE_SENSITIVITY, getCaseSensitivity());
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " ('" + m_configName + "')";
    }
}
