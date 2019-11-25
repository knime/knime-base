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
import org.knime.core.node.util.ButtonGroupEnumInterface;
import org.knime.core.node.util.FileSystemBrowser.FileSelectionMode;
import org.knime.filehandling.core.filefilter.FileFilterSettings;
import org.knime.filehandling.core.port.FileSystemPortObjectSpec;

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

    /** Configuration key to store the selected KNIME mountpoint connection. */
    private static final String KNIME_MOUNTPOINT_FILESYSTEM_KEY = "knime_mountpoint_filesystem";

    /** Configuration key to store the path of the selected file or folder. */
    public static final String PATH_OR_URL_KEY = "path_or_url";

    /** Configuration key for the option to include sub folder. */
    private static final String INCLUDE_SUBFOLDERS_KEY = "include_subfolders";

    private static final String FILE_OR_FOLDER_MODEL_KEY = "fileOrFolderModel";

    private final SettingsModelString m_fileOrFolderSettingsModel =
        new SettingsModelString(FILE_OR_FOLDER_MODEL_KEY, FileOrFolderEnum.FILE.name());

    /** The name of the configuration object. */
    private final String m_configName;

    /** The name of the selected file system. */
    private String m_fileSystem;

    /** The name of the selected KNIME mountpoint connection. */
    private String m_knimeMountpointFileSystem;

    /** The name of the selected KNIME connection. */
    private String m_knimeFileSystem;

    /** Path of selected file or folder. */
    private String m_pathOrURL;

    /** True, if sub-folders should be included. */
    private boolean m_includeSubfolders;

    private FileFilterSettings m_fileFilterSettings;

    private final String[] m_defaultSuffixes;

    /** The default path. */
    private static final String DEFAULT_PATH = "";

    /** The default filesystem choice. */
    private static final FileSystemChoice DEFAULT_FS_CHOICE = FileSystemChoice.getLocalFsChoice();

    /**
     * Creates a new instance of {@link SettingsModelFileChooser2} with default settings.
     *
     * @param configName the name of the config.
     */
    public SettingsModelFileChooser2(final String configName) {
        this(configName, "", KNIMEConnection.WORKFLOW_RELATIVE_CONNECTION.getId(), null, DEFAULT_PATH, false,
            new FileFilterSettings(), new String[0]);
    }

    /**
     * Creates a new instance of {@link SettingsModelFileChooser2} with default settings.
     *
     * @param configName the name of the config.
     * @param suffixes the list of default suffixes the dialog should filter on
     */
    public SettingsModelFileChooser2(final String configName, final String[] suffixes) {
        this(configName, "", KNIMEConnection.WORKFLOW_RELATIVE_CONNECTION.getId(), null, DEFAULT_PATH, false,
            new FileFilterSettings(suffixes), suffixes);
    }

    /**
     * Creates a new instance of {@link SettingsModelFileChooser2}.
     *
     * @param configName the name of the configuration object.
     * @param fileSystemName the name of the selected file system
     * @param knimeConnection the name of the selected knime connection
     * @param pathOrURL the path of the selected file or folder
     * @param searchSubfolder true, if sub-folder should be included
     * @param fileFilterSettings the filter settings for the file filter
     * @param suffixes the list of default suffixes the dialog should filter on
     */

    public SettingsModelFileChooser2(final String configName, final String fileSystemName, final String knimeConnection,
        final String knimeMountpointConnection, final String pathOrURL, final boolean searchSubfolder,
        final FileFilterSettings fileFilterSettings, final String[] suffixes) {
        if ((configName == null) || "".equals(configName)) {
            throw new IllegalArgumentException("The configName must be a " + "non-empty string");
        }
        m_configName = configName;
        m_fileSystem = fileSystemName;
        m_knimeFileSystem = knimeConnection;
        m_knimeMountpointFileSystem = knimeMountpointConnection;
        m_pathOrURL = pathOrURL;
        m_includeSubfolders = searchSubfolder;
        m_fileFilterSettings = fileFilterSettings;
        m_defaultSuffixes = suffixes;
        //notify change listeners when the file or folder setting changes since this affects the returned paths
        m_fileOrFolderSettingsModel.addChangeListener(e -> notifyChangeListeners());
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
     * @param newValue the knimeMountpointFileSystem to set
     */
    public void setKnimeMountpointFileSystem(final String newValue) {
        boolean sameValue;

        if (newValue == null) {
            sameValue = (m_knimeMountpointFileSystem == null);
        } else {
            sameValue = newValue.equals(m_knimeMountpointFileSystem);
        }
        m_knimeMountpointFileSystem = newValue;

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
        final boolean sameValue = (m_includeSubfolders == newValue);
        m_includeSubfolders = newValue;
        if (!sameValue) {
            notifyChangeListeners();
        }
    }

    /**
     * Sets new file filter with filter options (filter type, filter expression, case sensitivity).
     *
     * @param filterSettings the filter options to set
     */
    public void setFilterSettings(final FileFilterSettings filterSettings) {
        final boolean sameValue = (m_fileFilterSettings.equals(filterSettings));
        m_fileFilterSettings = filterSettings;
        if (!sameValue) {
            //notify change listeners when the file settings changes since this affects the returned paths
            notifyChangeListeners();
        }
    }

    /**
     * Returns the name of the selected connection.
     *
     * @return The name of the selected connection
     */
    private String getFileSystem() {
        return m_fileSystem;
    }

    /**
     * @return the selected connection as a {@link FileSystemChoice}.
     */
    public FileSystemChoice getFileSystemChoice() {
        return m_fileSystem.isEmpty() ? DEFAULT_FS_CHOICE : FileSystemChoice.getChoiceFromId(m_fileSystem);
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
     * @return the knimeMountpointFileSystem
     */
    public String getKnimeMountpointFileSystem() {
        return m_knimeMountpointFileSystem;
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

    @SuppressWarnings("unchecked")
    @Override
    protected SettingsModelFileChooser2 createClone() {
        return clone();
    }

    @Override
    public SettingsModelFileChooser2 clone() {
        try {
            return (SettingsModelFileChooser2)super.clone();
        } catch (final CloneNotSupportedException ex) {
            // never happens
            return null;
        }
    }

    @Override
    protected String getModelTypeID() {
        return "SMID_FileChooserGen2";
    }

    @Override
    public String getConfigName() {
        return m_configName;
    }

    /**
     * Returns the settings model for the file or folder button group
     *
     * @return the fileOrFolderSettingsModel the settings model for the file or folder button group
     */
    public SettingsModelString getFileOrFolderSettingsModel() {
        return m_fileOrFolderSettingsModel;
    }

    @Override
    protected void loadSettingsForDialog(final NodeSettingsRO settings, final PortObjectSpec[] specs)
        throws NotConfigurableException {
        Config config;
        try {
            config = settings.getConfig(m_configName);
            setPathOrURL(config.getString(PATH_OR_URL_KEY, m_pathOrURL));
            setFileSystem(config.getString(FILE_SYSTEM_KEY, m_fileSystem));
            if (m_fileSystem.isEmpty() && specs.length > 0
                && specs[specs.length - 1] instanceof FileSystemPortObjectSpec) {
                final FileSystemPortObjectSpec fspos = (FileSystemPortObjectSpec)specs[specs.length - 1];
                setFileSystem(fspos.getFileSystemType());
            }
            setKNIMEFileSystem(config.getString(KNIME_FILESYSTEM_KEY, m_knimeFileSystem));
            setKnimeMountpointFileSystem(
                config.getString(KNIME_MOUNTPOINT_FILESYSTEM_KEY, m_knimeMountpointFileSystem));
            setIncludeSubfolders(config.getBoolean(INCLUDE_SUBFOLDERS_KEY, m_includeSubfolders));
            setFilterSettings(m_fileFilterSettings);
            m_fileOrFolderSettingsModel.loadSettingsFrom(settings);
        } catch (final InvalidSettingsException ex) {
            throw new NotConfigurableException(ex.getMessage());
        }

    }

    /**
     * Returns the filter options as {@link FileFilterSettings}.
     *
     * @return the {@link FileFilterSettings}
     */
    public FileFilterSettings getFileFilterSettings() {
        return m_fileFilterSettings;
    }

    /**
     * Returns the default suffixes the dialog should use for filtering files.
     *
     * @return the defaultSuffixes the dialog should use for filtering files
     */
    public String[] getDefaultSuffixes() {
        return m_defaultSuffixes;
    }

    /**
     * Returns whether reading files from folder is enabled.
     *
     * @return whether reading files from folder is enabled.
     */
    public boolean readFilesFromFolder() {
        return m_fileOrFolderSettingsModel.getStringValue().equals(FileOrFolderEnum.FILE_IN_FOLDER.name());
    }

    @Override
    protected void validateSettingsForModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        final Config config = settings.getConfig(m_configName);
        //FIXME Check whether KNIME Mountpoint is valid
        final String path = config.getString(PATH_OR_URL_KEY);
        if (path == null || path.isEmpty()) {
            throw new InvalidSettingsException("No location provided! Please enter a valid location.");
        }
        config.getString(FILE_SYSTEM_KEY);
        config.getString(KNIME_FILESYSTEM_KEY);
        config.getString(KNIME_MOUNTPOINT_FILESYSTEM_KEY);
        config.getBoolean(INCLUDE_SUBFOLDERS_KEY);
        m_fileFilterSettings.validate(config);
    }

    @Override
    protected void loadSettingsForModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        final Config config = settings.getConfig(m_configName);
        m_pathOrURL = config.getString(PATH_OR_URL_KEY);
        m_fileSystem = config.getString(FILE_SYSTEM_KEY);
        m_knimeFileSystem = config.getString(KNIME_FILESYSTEM_KEY);
        m_knimeMountpointFileSystem = config.getString(KNIME_MOUNTPOINT_FILESYSTEM_KEY);
        m_includeSubfolders = config.getBoolean(INCLUDE_SUBFOLDERS_KEY);
        m_fileFilterSettings.loadFromConfig(config);
        m_fileOrFolderSettingsModel.loadSettingsFrom(settings);
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
        config.addString(KNIME_MOUNTPOINT_FILESYSTEM_KEY, getKnimeMountpointFileSystem());
        config.addString(PATH_OR_URL_KEY, getPathOrURL());
        config.addBoolean(INCLUDE_SUBFOLDERS_KEY, getIncludeSubfolders());
        m_fileFilterSettings.saveToConfig(config);
        m_fileOrFolderSettingsModel.saveSettingsTo(settings);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " ('" + m_configName + "')";
    }

    /**
     * This enum holds the options for the reading mode
     *
     * @author Mareike Hoeger, KNIME GmbH, Konstanz, Germany
     */
    public enum FileOrFolderEnum implements ButtonGroupEnumInterface {

            /** Selection of a single file */
            FILE("File", "Select single file", FileSelectionMode.FILES_ONLY),
            /** Selection of a folder */
            FILE_IN_FOLDER("Files in folder", "Select files in a folder", FileSelectionMode.FILES_AND_DIRECTORIES);

        private String m_label;

        private String m_desc;

        private FileSelectionMode m_selectionMode;

        private FileOrFolderEnum(final String label, final String desc, final FileSelectionMode mode) {
            m_label = label;
            m_desc = desc;
            m_selectionMode = mode;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getText() {
            return m_label;
        }

        /**
         * @return the selectionMode
         */
        public FileSelectionMode getSelectionMode() {
            return m_selectionMode;
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
            return m_desc;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isDefault() {
            return FILE.equals(this);
        }
    }

}
