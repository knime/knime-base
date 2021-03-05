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
package org.knime.filehandling.utility.nodes.compress.truncator;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

/**
 * Settings storing all information to truncate a path.
 *
 * @author Mark Ortmann, KNIME GmbH, Berlin, Germany
 */
public final class TruncationSettings {

    private TruncatePathOption m_truncatePathOption;

    private final SettingsModelString m_truncateRegex;

    private static final String CFG_TRUNCATE_PATH_OPTION = "source_folder_truncation";

    private static final String CFG_TRUNCATE_REGEX = "truncate_regex";

    private static final String DEFAULT_REGEX = ".*";

    /**
     * Constructor.
     */
    public TruncationSettings() {
        m_truncatePathOption = TruncatePathOption.getDefault();
        m_truncateRegex = new SettingsModelString(CFG_TRUNCATE_REGEX, DEFAULT_REGEX) {

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
     * @param flattenHierarchy flag indicating whether or not to flatten the hierarchy
     * @return the {@link PathTruncator}
     */
    public PathTruncator getPathTruncator(final boolean flattenHierarchy) {
        return m_truncatePathOption.createPathTruncator(flattenHierarchy, getTruncateRegexModel().getStringValue());
    }

    /**
     * Sets the {@link TruncatePathOption}.
     *
     * @param truncatePathOption the option to set
     */
    public final void setTruncatePathOption(final TruncatePathOption truncatePathOption) {
        m_truncatePathOption = truncatePathOption;
    }

    TruncatePathOption getTruncatePathOption() {
        return m_truncatePathOption;
    }

    SettingsModelString getTruncateRegexModel() {
        return m_truncateRegex;
    }

    /**
     * Read the expected values from the settings object, without assigning them to the internal variables.
     *
     * @param settings the object to read the value(s) from
     * @throws InvalidSettingsException if the value(s) in the settings object are invalid.
     */
    public void validateSettingsForModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        TruncatePathOption.valueOf(settings.getString(CFG_TRUNCATE_PATH_OPTION));
        m_truncateRegex.validateSettings(settings);
    }

    /**
     * Write value(s) of this settings to the {@link NodeSettingsWO}.
     *
     * @param settings the settings to write to
     */
    public void saveSettingsForModel(final NodeSettingsWO settings) {
        saveSettingsForDialog(settings);
        m_truncateRegex.saveSettingsTo(settings);
    }

    /**
     * Read values from the configuration object. If the value is not stored in the config, an exception will be thrown.
     *
     * @param settings the settings to read from
     * @throws InvalidSettingsException if load fails.
     */
    public void loadSettingsForModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        setTruncatePathOption(TruncatePathOption.valueOf(settings.getString(CFG_TRUNCATE_PATH_OPTION)));
        m_truncateRegex.loadSettingsFrom(settings);
    }

    /**
     * Writes values that are not controlled via {@link SettingsModel}s to the configuration object.
     *
     * @param settings the settings to read from
     */
    public void saveSettingsForDialog(final NodeSettingsWO settings) {
        settings.addString(CFG_TRUNCATE_PATH_OPTION, getTruncatePathOption().name());
    }

    /**
     * Read values that are not controlled via {@link SettingsModel}s from the configuration object.
     *
     * @param settings the settings to read from
     */
    public void loadSettingsForDialog(final NodeSettingsRO settings) {
        setTruncatePathOption(TruncatePathOption
            .valueOf(settings.getString(CFG_TRUNCATE_PATH_OPTION, TruncatePathOption.getDefault().name())));
    }

}
