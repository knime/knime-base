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

import java.util.Optional;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.config.Config;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.filehandling.core.filefilter.FileFilter.FilterType;

/**
 * SettingsModel for {@link DialogComponentFileChooser2}.
 *
 * @author Björn Lohrmann, KNIME GmbH, Berlin, Germany
 * @author Julian Bunzel, KNIME GmbH, Berlin, Germany
 */
public final class SettingsModelFileChooser2 extends SettingsModel implements Cloneable {

    /** Configuration key to store the selected file system. */
    private static final String FILE_SYSTEM_KEY = "filesystem";

    /** Configuration key to store the selected KNIME connection. */
    private static final String KNIME_FILESYSTEM_KEY = "knime_filesystem";

    /** Configuration key to store the path of the selected file or folder. */
    public static final String PATH_OR_URL_KEY = "path_or_url";

    /** Configuration key for the option to include sub folder. */
    private static final String INCLUDE_SUBFOLDERS_KEY = "include_subfolders";

    /** Configuration key for the option to filter files in selected folder. */
    private static final String FILTER_FILES_KEY = "filter_files";

    /** Configuration key to store the filter mode. */
    private static final String FILTER_MODE_KEY = "filter_mode";

    /** Configuration key to store the filter expression. */
    private static final String FILTER_EXPRESSION_KEY = "filter_expression";

    /** Configuration key to store the setting whether the filter expression is case sensitive or not. */
    private static final String FILTER_CASE_SENSITIVE_KEY = "filter_case_sensitive";

    /** The name of the configuration object. */
    private final String m_configName;

    /** The name of the selected file system. */
    private String m_fileSystem;

    /** The name of the selected KNIME connection. */
    private String m_knimeFileSystem;

    /** Path of selected file or folder. */
    private String m_pathOrURL;

    /** True, if sub-folders should be included. */
    private boolean m_includeSubfolders;

    /** True, if files should be filtered. */
    private boolean m_filterFiles;

    /** Mode used to filter files (e.g. regex or wildcard). */
    private String m_filterMode;

    /** The expression used to filter files in the selected folder/directory. */
    private String m_filterExpression;

    /** True, if expression to filter should work regardless the case of the filenames. */
    private boolean m_filterCaseSensitive;

    /** Config key for path of legacy settings object */
    private final String m_legacyPathConfigKey;

    /** The default path. */
    private static final String DEFAULT_PATH = "";

    /** The default filter. */
    private static final String DEFAULT_FILTER = FilterType.WILDCARD.getDisplayText();

    /** The default filter expression. */
    private static final String DEFAULT_FILTER_EXPRESSION = "*";

    /**
     * Creates a new instance of {@link SettingsModelFileChooser2} with default settings.
     *
     * @param configName the name of the config.
     */
    public SettingsModelFileChooser2(final String configName) {
        this(configName, FileSystemChoice.getLocalFsChoice().getId(),
            KNIMEConnection.WORKFLOW_RELATIVE_CONNECTION.getId(), DEFAULT_PATH, false, false, DEFAULT_FILTER,
            DEFAULT_FILTER_EXPRESSION, false, null);
    }

    /**
     * Creates a new instance of {@link SettingsModelFileChooser2} with default settings.
     *
     * @param configName the name of the config.
     * @param legacyPathConfigKey the legacy config key used to store the path
     */
    public SettingsModelFileChooser2(final String configName, final String legacyPathConfigKey) {
        this(configName, FileSystemChoice.getLocalFsChoice().getId(),
            KNIMEConnection.WORKFLOW_RELATIVE_CONNECTION.getId(), DEFAULT_PATH, false, false, DEFAULT_FILTER,
            DEFAULT_FILTER_EXPRESSION, false, legacyPathConfigKey);
    }

    /**
     * Creates a new instance of {@link SettingsModelFileChooser2}.
     *
     * @param configName the name of the configuration object.
     * @param fileSystemName the name of the selected file system
     * @param knimeConnection the name of the selected knime connection
     * @param pathOrURL the path of the selected file or folder
     * @param searchSubfolder true, if sub-folder should be included
     * @param filterFiles true, if files should be filtered
     * @param filterMode mode to filter files in case a multiple files should be read
     * @param filterExpression the expression used to filter files
     * @param caseSensitivity true, if expression should be sensitive to case
     * @param legacyPathConfigKey legacy config key of settings entry for path
     */
    public SettingsModelFileChooser2(final String configName,
        final String fileSystemName, final String knimeConnection, final String pathOrURL,
        final boolean searchSubfolder, final boolean filterFiles, final String filterMode,
        final String filterExpression, final boolean caseSensitivity, final String legacyPathConfigKey) {
        if ((configName == null) || "".equals(configName)) {
            throw new IllegalArgumentException("The configName must be a " + "non-empty string");
        }
        m_configName = configName;
        m_fileSystem = fileSystemName;
        m_knimeFileSystem = knimeConnection;
        m_pathOrURL = pathOrURL;
        m_includeSubfolders = searchSubfolder;
        m_filterFiles = filterFiles;
        m_filterMode = filterMode;
        m_filterExpression = filterExpression;
        m_filterCaseSensitive = caseSensitivity;
        m_legacyPathConfigKey = legacyPathConfigKey;
    }

    /**
     * Sets the name of the file system to select the file/folder from.
     *
     * @param newValue the name of the file system to select the file/folder from
     */
    public void setFileSystem(final String newValue) {
        boolean sameValue;

        if (newValue == null) {
            sameValue = (m_fileSystem == null);
        } else {
            sameValue = newValue.equals(m_fileSystem);
        }
        m_fileSystem = newValue;

        if (!sameValue) {
            notifyChangeListeners();
        }
    }

    /**
     * Sets the name of the KNIME connection.
     *
     * @param newValue the name of the KNIME connection
     */
    public void setKNIMEFileSystem(final String newValue) {
        boolean sameValue;

        if (newValue == null) {
            sameValue = (m_knimeFileSystem == null);
        } else {
            sameValue = newValue.equals(m_knimeFileSystem);
        }
        m_knimeFileSystem = newValue;

        if (!sameValue) {
            notifyChangeListeners();
        }
    }

    /**
     * Sets the path of the file/folder.
     *
     * @param newValue The path of the file/folder
     */
    public void setPathOrURL(final String newValue) {
        boolean sameValue;

        if (newValue == null) {
            sameValue = (m_pathOrURL == null);
        } else {
            sameValue = newValue.equals(m_pathOrURL);
        }
        m_pathOrURL = newValue;

        if (!sameValue) {
            notifyChangeListeners();
        }
    }

    /**
     * Sets a new value for the option to search sub folder for files.
     *
     * @param newValue Set true, if sub folder should be searched for files
     */
    public void setIncludeSubfolders(final boolean newValue) {
        boolean sameValue = (m_includeSubfolders == newValue);
        m_includeSubfolders = newValue;
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
     * Sets new values for the filter conditions (filter type, filter expression, case sensitivity)
     *
     * @param filterType the filter type
     * @param filterExpression filter expression
     * @param caseSensitive case sensitivity option
     */
    public void setFilterConditions(final FilterType filterType, final String filterExpression,
        final boolean caseSensitive) {
        final String filterTypeText = filterType.getDisplayText();
        boolean sameValue;
        if (filterTypeText == null) {
            sameValue = (m_filterMode == null);
        } else {
            sameValue = filterTypeText.equals(m_filterMode);
        }
        m_filterMode = filterTypeText;

        if (filterExpression == null) {
            sameValue = (m_filterExpression == null) && sameValue;
        } else {
            sameValue = filterExpression.equals(m_filterExpression) && sameValue;
        }
        m_filterExpression = filterExpression;

        sameValue = (m_filterCaseSensitive == caseSensitive) && sameValue;
        m_filterCaseSensitive = caseSensitive;

        if (!sameValue) {
            notifyChangeListeners();
        }
    }

    /**
     * Returns the name of the selected connection.
     *
     * @return The name of the selected connection
     */
    public String getFileSystem() {
        return m_fileSystem;
    }

    /**
     * @return the selected connection as a {@link FileSystemChoice}.
     */
    public FileSystemChoice getFileSystemChoice() {
        return FileSystemChoice.getChoiceFromId(m_fileSystem);
    }

    /**
     * Returns the name of the selected KNIME connection.
     *
     * @return The name of the selected KNIME connection
     */
    public String getKNIMEFileSystem() {
        return m_knimeFileSystem;
    }

    /**
     * Returns the path of the selected file or folder.
     *
     * @return The path of the selected file or folder
     */
    public String getPathOrURL() {
        return m_pathOrURL;
    }

    /**
     * Returns true, if sub folders should be included while searching files.
     *
     * @return True, if sub folders should be included while searching files
     */
    public boolean getIncludeSubfolders() {
        return m_includeSubfolders;
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
    public boolean getCaseSensitive() {
        return m_filterCaseSensitive;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected SettingsModelFileChooser2 createClone() {
        return clone();
    }

    @Override
    public SettingsModelFileChooser2 clone() {
        try {
            return (SettingsModelFileChooser2)super.clone();
        } catch (CloneNotSupportedException ex) {
            // never happens
            return null;
        }
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
        try {
            final Config config = settings.getConfig(m_configName);
            if (legacyConfigKeyExists()) {
                setPathOrURL(settings.getString(m_legacyPathConfigKey, m_pathOrURL));
            } else {
                setPathOrURL(config.getString(PATH_OR_URL_KEY, m_pathOrURL));
            }
            setFileSystem(config.getString(FILE_SYSTEM_KEY, m_fileSystem));
            setKNIMEFileSystem(config.getString(KNIME_FILESYSTEM_KEY, m_knimeFileSystem));
            setIncludeSubfolders(config.getBoolean(INCLUDE_SUBFOLDERS_KEY, m_includeSubfolders));
            setFilterFiles(config.getBoolean(FILTER_FILES_KEY, m_filterFiles));
            setFilterConditions(FilterType.fromDisplayText(config.getString(FILTER_MODE_KEY, m_filterMode)),
                config.getString(FILTER_EXPRESSION_KEY, m_filterExpression),
                config.getBoolean(FILTER_CASE_SENSITIVE_KEY, m_filterCaseSensitive));
        } catch (InvalidSettingsException ex) {
            if (m_legacyPathConfigKey != null) {
                setPathOrURL(settings.getString(m_legacyPathConfigKey, m_pathOrURL));
                FileChooserSettingsConverter.convert(this);
            } else {
                throw new NotConfigurableException(ex.getMessage());
            }
        }
    }

    private final boolean legacyConfigKeyExists() {
        return m_legacyPathConfigKey != null && !m_legacyPathConfigKey.isEmpty();
    }

    /**
     * Returns the legacy configuration key for storing the path in the SettingsModel of a node if present.
     * Otherwise an empty Optional will be returned.
     *
     * @return the legacy configuration key if present
     */
    public final Optional<String> getLegacyConfigKey() {
        return legacyConfigKeyExists() ? Optional.of(m_legacyPathConfigKey) : Optional.empty();
    }

    @Override
    protected void validateSettingsForModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        Config config;
        try {
            config = settings.getConfig(m_configName);
        } catch (final InvalidSettingsException e) {
            if (legacyConfigKeyExists()) {
                settings.getString(m_legacyPathConfigKey);
                FileChooserSettingsConverter.convert(this);
                return;
            }
            throw e;
        }
        //FIXME Check whether KNIME Mountpoint is valid
        if (legacyConfigKeyExists()) {
            settings.getString(m_legacyPathConfigKey);
        } else {
            config.getString(PATH_OR_URL_KEY);
        }
        config.getString(FILE_SYSTEM_KEY);
        config.getString(KNIME_FILESYSTEM_KEY);
        config.getBoolean(INCLUDE_SUBFOLDERS_KEY);
        config.getBoolean(FILTER_FILES_KEY);
        config.getString(FILTER_EXPRESSION_KEY);
        config.getBoolean(FILTER_CASE_SENSITIVE_KEY);
        // Validate filter mode
        final String filterMode = config.getString(FILTER_MODE_KEY);
        if (!FilterType.contains(filterMode)) {
            throw new InvalidSettingsException("\"" + filterMode + "\" is not a valid filter type.");
        }
    }

    @Override
    protected void loadSettingsForModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        final Config config;
        try {
            config = settings.getConfig(m_configName);
        } catch (final InvalidSettingsException e) {
            if (legacyConfigKeyExists()) {
                m_pathOrURL = settings.getString(m_legacyPathConfigKey);
                FileChooserSettingsConverter.convert(this);
                return;
            }
            throw e;
        }
        m_pathOrURL =
            legacyConfigKeyExists() ? settings.getString(m_legacyPathConfigKey) : config.getString(PATH_OR_URL_KEY);
        m_fileSystem = config.getString(FILE_SYSTEM_KEY);
        m_knimeFileSystem = config.getString(KNIME_FILESYSTEM_KEY);
        m_includeSubfolders = config.getBoolean(INCLUDE_SUBFOLDERS_KEY);
        m_filterFiles = config.getBoolean(FILTER_FILES_KEY);
        m_filterMode = config.getString(FILTER_MODE_KEY);
        m_filterExpression = config.getString(FILTER_EXPRESSION_KEY);
        m_filterCaseSensitive = config.getBoolean(FILTER_CASE_SENSITIVE_KEY);

        notifyChangeListeners();
    }

    @Override
    protected void saveSettingsForDialog(final NodeSettingsWO settings) throws InvalidSettingsException {
        saveSettingsForModel(settings);
    }

    @Override
    protected void saveSettingsForModel(final NodeSettingsWO settings) {
        final Config config = settings.addConfig(m_configName);
        config.addString(FILE_SYSTEM_KEY, getFileSystem());
        config.addString(KNIME_FILESYSTEM_KEY, getKNIMEFileSystem());
        if (legacyConfigKeyExists()) {
            settings.addString(m_legacyPathConfigKey, getPathOrURL());
        } else {
            config.addString(PATH_OR_URL_KEY, getPathOrURL());
        }
        config.addBoolean(INCLUDE_SUBFOLDERS_KEY, getIncludeSubfolders());
        config.addBoolean(FILTER_FILES_KEY, getFilterFiles());
        config.addString(FILTER_MODE_KEY, getFilterMode());
        config.addString(FILTER_EXPRESSION_KEY, getFilterExpression());
        config.addBoolean(FILTER_CASE_SENSITIVE_KEY, getCaseSensitive());
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " ('" + m_configName + "')";
    }
}
