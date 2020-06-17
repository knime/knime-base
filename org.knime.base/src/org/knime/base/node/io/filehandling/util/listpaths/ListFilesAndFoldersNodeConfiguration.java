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
 *   Jun 18, 2020 (Mark Ortmann, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.base.node.io.filehandling.util.listpaths;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.filehandling.core.defaultnodesettings.filechooser.reader.SettingsModelReaderFileChooser;

/**
 * The list files and folders node configuration.
 *
 * @author Mark Ortmann, KNIME GmbH, Berlin, Germany
 */
final class ListFilesAndFoldersNodeConfiguration {

    private static final String CFG_INCLUDE_ROOT = "include_root_folder";

    private static final String CFG_ADD_DIR_INDICATOR = "add_dir_indicator_column";

    private boolean m_includeRootDir;

    private boolean m_addDirIndicatorColumn;

    private final SettingsModelReaderFileChooser m_fileChooserSettings;

    ListFilesAndFoldersNodeConfiguration(final SettingsModelReaderFileChooser fileChooserSettings) {
        m_includeRootDir = false;
        m_addDirIndicatorColumn = false;
        m_fileChooserSettings = fileChooserSettings;
    }

    boolean addDirIndicatorColumn() {
        return m_addDirIndicatorColumn;
    }

    void addDirIndicatorColumn(final boolean addDirIndicatorColumn) {
        m_addDirIndicatorColumn = addDirIndicatorColumn;
    }

    void includeRootDir(final boolean includeRootDir) {
        m_includeRootDir = includeRootDir;
    }

    boolean includeRootDir() {
        return m_includeRootDir;
    }

    SettingsModelReaderFileChooser getFileChooserSettings() {
        return m_fileChooserSettings;
    }

    void loadSettingsForDialog(final NodeSettingsRO settings) {
        includeRootDir(settings.getBoolean(CFG_INCLUDE_ROOT, false));
        addDirIndicatorColumn(settings.getBoolean(CFG_ADD_DIR_INDICATOR, false));
    }

    void loadSettingsForModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_fileChooserSettings.loadSettingsFrom(settings);
        includeRootDir(settings.getBoolean(CFG_INCLUDE_ROOT));
        addDirIndicatorColumn(settings.getBoolean(CFG_ADD_DIR_INDICATOR));
    }

    void saveSettingsForDialog(final NodeSettingsWO settings) {
        settings.addBoolean(CFG_ADD_DIR_INDICATOR, addDirIndicatorColumn());
        settings.addBoolean(CFG_INCLUDE_ROOT, includeRootDir());
    }

    void saveSettingsForModel(final NodeSettingsWO settings) {
        saveSettingsForDialog(settings);
        m_fileChooserSettings.saveSettingsTo(settings);
    }

    void validateSettingsForModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_fileChooserSettings.validateSettings(settings);
        settings.getBoolean(CFG_ADD_DIR_INDICATOR);
        settings.getBoolean(CFG_INCLUDE_ROOT);
    }

}
