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
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.util.ButtonGroupEnumInterface;
import org.knime.core.node.util.CheckUtils;

/**
 * Settings model for {@link DialogComponentFilterMode}.
 *
 * @author Simon Schmid, KNIME GmbH, Konstanz, Germany
 */
public final class SettingsModelFilterMode extends SettingsModel {

    private static final String MODEL_TYPE_ID = "SMID_FilterMode";

    private static final String CFG_FILTER_MODE = "filter_mode";

    private static final String CFG_INCLUDE_SUBFOLDERS = "include_subfolders";

    private static final String CFG_FILTER_OPTIONS = "filter_options";

    private static final boolean DEFAULT_INCLUDE_SUBFOLDERS = false;

    private static final FilterOptionsSettings DEFAULT_FILTER_OPTIONS_SETTINGS = new FilterOptionsSettings();

    private final String m_configName;

    private FilterMode m_filterMode;

    private boolean m_includeSubfolders;

    private FilterOptionsSettings m_filterOptionsSettings;

    /**
     * Constructor. Value for {@code includeSubfolders} will be set {@code false}.
     *
     * @param configName the config name
     * @param defaultFilterMode the default {@link FilterMode}
     */
    public SettingsModelFilterMode(final String configName, final FilterMode defaultFilterMode) {
        this(configName, defaultFilterMode, DEFAULT_INCLUDE_SUBFOLDERS);
    }

    /**
     * Constructor.
     *
     * @param configName the config name
     * @param defaultFilterMode the default {@link FilterMode}
     * @param defaultIncludeSubfolders the default value for include subfolders
     */
    public SettingsModelFilterMode(final String configName, final FilterMode defaultFilterMode,
        final boolean defaultIncludeSubfolders) {
        CheckUtils.checkArgumentNotNull(configName, "The config name must not be null.");
        CheckUtils.checkArgument(!configName.trim().isEmpty(), "The config name must not be empty.");
        CheckUtils.checkArgumentNotNull(defaultFilterMode, "The default filter mode must not be null.");
        m_configName = configName;
        m_filterMode = defaultFilterMode;
        m_includeSubfolders = defaultIncludeSubfolders;
        m_filterOptionsSettings = DEFAULT_FILTER_OPTIONS_SETTINGS;
    }

    private SettingsModelFilterMode(final SettingsModelFilterMode toCopy) {
        m_configName = toCopy.m_configName;
        m_filterMode = toCopy.m_filterMode;
        m_includeSubfolders = toCopy.m_includeSubfolders;
        m_filterOptionsSettings = toCopy.m_filterOptionsSettings;
    }

    /**
     * @return the filterMode
     */
    public FilterMode getFilterMode() {
        return m_filterMode;
    }

    /**
     * @param filterMode the filterMode to set
     */
    public void setFilterMode(final FilterMode filterMode) {
        CheckUtils.checkArgumentNotNull(filterMode, "The filter mode must not be null.");
        boolean notify = m_filterMode != filterMode;
        m_filterMode = filterMode;
        if (notify) {
            notifyChangeListeners();
        }
    }

    /**
     * @return the filterOptionsSettings
     */
    public FilterOptionsSettings getFilterOptionsSettings() {
        return m_filterOptionsSettings;
    }

    /**
     * @param filterOptionsSettings the filterOptionsSettings to set
     */
    public void setFilterOptionsSettings(final FilterOptionsSettings filterOptionsSettings) {
        CheckUtils.checkArgumentNotNull(filterOptionsSettings, "The filter options settings must not be null.");
        boolean notify = !m_filterOptionsSettings.equals(filterOptionsSettings);
        m_filterOptionsSettings = filterOptionsSettings;
        if (notify) {
            notifyChangeListeners();
        }
    }

    /**
     * @return the includeSubfolders
     */
    public boolean isIncludeSubfolders() {
        return m_includeSubfolders;
    }

    /**
     * @param includeSubfolders the includeSubfolders to set
     */
    public void setIncludeSubfolders(final boolean includeSubfolders) {
        boolean notify = m_includeSubfolders != includeSubfolders;
        m_includeSubfolders = includeSubfolders;
        if (notify) {
            notifyChangeListeners();
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    protected SettingsModelFilterMode createClone() {
        return new SettingsModelFilterMode(this);
    }

    @Override
    protected String getModelTypeID() {
        return MODEL_TYPE_ID;
    }

    @Override
    protected String getConfigName() {
        return m_configName;
    }

    @Override
    protected void loadSettingsForDialog(final NodeSettingsRO settings, final PortObjectSpec[] specs)
        throws NotConfigurableException {
        try {
            final NodeSettingsRO nodeSettings = settings.getNodeSettings(m_configName);
            m_filterMode = FilterMode.valueOf(nodeSettings.getString(CFG_FILTER_MODE, m_filterMode.name()));
            m_includeSubfolders = nodeSettings.getBoolean(CFG_INCLUDE_SUBFOLDERS, DEFAULT_INCLUDE_SUBFOLDERS);
            m_filterOptionsSettings.loadFromConfigForDialog(nodeSettings.getConfig(CFG_FILTER_OPTIONS));
        } catch (InvalidSettingsException ex) {
            // nothing to do
        }
    }

    @Override
    protected void saveSettingsForDialog(final NodeSettingsWO settings) throws InvalidSettingsException {
        saveSettingsForModel(settings);
    }

    @Override
    protected void validateSettingsForModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        final NodeSettingsRO nodeSettings = settings.getNodeSettings(m_configName);
        FilterMode.valueOf(nodeSettings.getString(CFG_FILTER_MODE));
        nodeSettings.getBoolean(CFG_INCLUDE_SUBFOLDERS);
        FilterOptionsSettings.validate(nodeSettings.getConfig(CFG_FILTER_OPTIONS));
    }

    @Override
    protected void loadSettingsForModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        final NodeSettingsRO nodeSettings = settings.getNodeSettings(m_configName);
        m_filterMode = FilterMode.valueOf(nodeSettings.getString(CFG_FILTER_MODE));
        m_includeSubfolders = nodeSettings.getBoolean(CFG_INCLUDE_SUBFOLDERS);
        m_filterOptionsSettings.loadFromConfigForModel(nodeSettings.getConfig(CFG_FILTER_OPTIONS));
    }

    @Override
    protected void saveSettingsForModel(final NodeSettingsWO settings) {
        final NodeSettingsWO nodeSettings = settings.addNodeSettings(m_configName);
        nodeSettings.addString(CFG_FILTER_MODE, m_filterMode.name());
        nodeSettings.addBoolean(CFG_INCLUDE_SUBFOLDERS, m_includeSubfolders);
        m_filterOptionsSettings.saveToConfig(nodeSettings.addConfig(CFG_FILTER_OPTIONS));
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " ('" + m_configName + "')";
    }

    /**
     * Enumeration of modes for file and folder filtering.
     *
     * @author Simon Schmid, KNIME GmbH, Konstanz, Germany
     */
    public enum FilterMode implements ButtonGroupEnumInterface {
            /** Only one file */
            FILE("File"),
            /** Only one folder */
            FOLDER("Folder"),
            /** Several files in a folder */
            FILES_IN_FOLDERS("Files in folder"),
            /** Multiple folders */
            FOLDERS("Folders"),
            /** Multiple files and folders */
            FILES_AND_FOLDERS("Files and folders");

        private final String m_label;

        private FilterMode(final String label) {
            m_label = label;
        }

        @Override
        public String getText() {
            return m_label;
        }

        @Override
        public String getActionCommand() {
            return name();
        }

        @Override
        public String getToolTip() {
            return null;
        }

        @Override
        public boolean isDefault() {
            return this == FILE;
        }
    }
}
