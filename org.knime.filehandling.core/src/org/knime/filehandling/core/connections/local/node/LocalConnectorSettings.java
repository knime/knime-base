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
 *   2021-07-23 (Alexander Bondaletov): created
 */
package org.knime.filehandling.core.connections.local.node;

import java.nio.file.InvalidPathException;
import java.nio.file.Paths;

import org.apache.commons.lang3.StringUtils;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

/**
 * Local FS Connector node settings.
 *
 * @author Alexander Bondaletov
 */
public class LocalConnectorSettings {

    private static final String KEY_USE_CUSTOM_WORKING_DIRECTORY = "useCustomWorkingDirectory";
    private static final String KEY_WORKING_DIRECTORY = "workingDirectory";

    private final SettingsModelBoolean m_useCustomWorkingDirectory;
    private final SettingsModelString m_workingDirectory;

    /**
     * Creates new instance.
     */
    public LocalConnectorSettings() {
        m_useCustomWorkingDirectory = new SettingsModelBoolean(KEY_USE_CUSTOM_WORKING_DIRECTORY, false);
        m_workingDirectory = new SettingsModelString(KEY_WORKING_DIRECTORY, "");

        m_workingDirectory.setEnabled(false);
    }

    /**
     * @return the useCustomWorkingDirectory model
     */
    public SettingsModelBoolean getUseCustomWorkingDirectoryModel() {
        return m_useCustomWorkingDirectory;
    }

    /**
     * @return the useCustomWorkingDirectory
     */
    public boolean isUseCustomWorkingDirectory() {
        return m_useCustomWorkingDirectory.getBooleanValue();
    }

    /**
     * @return the workingDirectory model
     */
    public SettingsModelString getWorkingDirectoryModel() {
        return m_workingDirectory;
    }

    /**
     * @return the workingDirectory
     */
    public String getWorkingDirectory() {
        return m_workingDirectory.getStringValue();
    }

    /**
     * Saves the settings in this instance to the given {@link NodeSettingsWO}
     *
     * @param settings
     *            Node settings.
     */
    public void saveSettingsTo(final NodeSettingsWO settings) {
        m_workingDirectory.saveSettingsTo(settings);
        m_useCustomWorkingDirectory.saveSettingsTo(settings);
    }

    /**
     * Validates the settings in a given {@link NodeSettingsRO}
     *
     * @param settings
     *            Node settings.
     * @throws InvalidSettingsException
     */
    public void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_workingDirectory.validateSettings(settings);
        m_useCustomWorkingDirectory.validateSettings(settings);

        LocalConnectorSettings temp = new LocalConnectorSettings();
        temp.validate();
    }

    /**
     * Validates the current settings.
     *
     * @throws InvalidSettingsException
     */
    public void validate() throws InvalidSettingsException {
        if (isUseCustomWorkingDirectory()) {
            String workDir = getWorkingDirectory();

            try {
                if (StringUtils.isBlank(workDir) || !Paths.get(workDir).isAbsolute()) {
                    throw new InvalidSettingsException("Working directory must be set to an absolute path.");
                }
            } catch (InvalidPathException e) {
                throw new InvalidSettingsException("Invalid working directory: " + e.getMessage(), e);
            }
        }
    }

    /**
     * Loads settings from the given {@link NodeSettingsRO}
     *
     * @param settings
     *            Node settings.
     * @throws InvalidSettingsException
     */
    public void loadSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_workingDirectory.loadSettingsFrom(settings);
        m_useCustomWorkingDirectory.loadSettingsFrom(settings);
    }
}
