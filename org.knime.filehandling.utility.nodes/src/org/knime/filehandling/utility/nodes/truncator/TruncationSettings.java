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
 *   Mar 9, 2021 (Mark Ortmann, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.filehandling.utility.nodes.truncator;

import java.nio.file.Path;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.filehandling.core.defaultnodesettings.filtermode.SettingsModelFilterMode.FilterMode;

/**
 * Settings storing all information to truncate a path.
 *
 * @author Mark Ortmann, KNIME GmbH, Berlin, Germany
 */
public class TruncationSettings {

    private TruncatePathOption m_truncatePathOption;

    private final SettingsModelString m_folderTruncateString;

    private static final String CFG_TRUNCATE_FOLDER_PREFIX = "folder_prefix";

    private static final String DEFAULT_FOLDER_PREFIX = "<folder prefix>";

    private final String m_truncateOptionConfigKey;

    /**
     * Constructor.
     *
     * @param truncateOptionConfigKey the config key used to store the selected {@link TruncatePathOption}
     */
    public TruncationSettings(final String truncateOptionConfigKey) {
        m_truncateOptionConfigKey = truncateOptionConfigKey;
        m_truncatePathOption = TruncatePathOption.getDefault();
        m_folderTruncateString = new SettingsModelString(CFG_TRUNCATE_FOLDER_PREFIX, DEFAULT_FOLDER_PREFIX) {

            @Override
            protected void validateSettingsForModel(final NodeSettingsRO settings) throws InvalidSettingsException {
                super.validateSettingsForModel(settings);
                final String val = settings.getString(getKey());
                if (val == null || val.isEmpty()) {
                    throw new InvalidSettingsException("The truncation regular expression cannot be empty");
                }
            }
        };
    }

    /**
     * Returns the {@link PathTruncator}.
     *
     * @param basePath the base file/folder, i.e., the path specifying the prefix that has to be truncated
     * @param filterMode the {@link FilterMode} specifying the base path is a file or folder and the internals for
     *            various {@link PathTruncator}s
     * @return the {@link PathTruncator}
     */
    public PathTruncator getPathTruncator(final Path basePath, final FilterMode filterMode) {
        return m_truncatePathOption.createPathTruncator(basePath, getFolderTruncateModel().getStringValue(),
            filterMode);
    }

    /**
     * Sets the {@link TruncatePathOption}.
     *
     * @param truncatePathOption the option to set
     */
    final void setTruncatePathOption(final TruncatePathOption truncatePathOption) {
        m_truncatePathOption = truncatePathOption;
    }

    /**
     * Returns the stored truncation path option.
     *
     * @return the stored {@link TruncatePathOption}
     */
    public final TruncatePathOption getTruncatePathOption() {
        return m_truncatePathOption;
    }

    SettingsModelString getFolderTruncateModel() {
        return m_folderTruncateString;
    }

    /**
     * Read the expected values from the settings object, without assigning them to the internal variables.
     *
     * @param settings the object to read the value(s) from
     * @throws InvalidSettingsException if the value(s) in the settings object are invalid.
     */
    public void validateSettingsForModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        TruncatePathOption.valueOf(settings.getString(m_truncateOptionConfigKey));
        m_folderTruncateString.validateSettings(settings);
    }

    /**
     * Write value(s) of this settings to the {@link NodeSettingsWO}.
     *
     * @param settings the settings to write to
     */
    public void saveSettingsForModel(final NodeSettingsWO settings) {
        saveSettingsForDialog(settings);
        m_folderTruncateString.saveSettingsTo(settings);
    }

    /**
     * Read values from the configuration object. If the value is not stored in the config, an exception will be thrown.
     *
     * @param settings the settings to read from
     * @throws InvalidSettingsException if load fails.
     */
    public void loadSettingsForModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        setTruncatePathOption(TruncatePathOption.valueOf(settings.getString(m_truncateOptionConfigKey)));
        m_folderTruncateString.loadSettingsFrom(settings);
    }

    /**
     * Writes values that are not controlled via {@link SettingsModel}s to the configuration object.
     *
     * @param settings the settings to read from
     */
    public void saveSettingsForDialog(final NodeSettingsWO settings) {
        settings.addString(m_truncateOptionConfigKey, getTruncatePathOption().name());
    }

    /**
     * Read values that are not controlled via {@link SettingsModel}s from the configuration object.
     *
     * @param settings the settings to read from
     */
    public void loadSettingsForDialog(final NodeSettingsRO settings) {
        setTruncatePathOption(TruncatePathOption
            .valueOf(settings.getString(m_truncateOptionConfigKey, TruncatePathOption.getDefault().name())));
    }

}
