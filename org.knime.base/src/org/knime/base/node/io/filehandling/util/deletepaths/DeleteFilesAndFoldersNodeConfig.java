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
 *   Aug 3, 2020 (Timmo Waller-Ehrat, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.base.node.io.filehandling.util.deletepaths;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.context.ports.PortsConfiguration;
import org.knime.filehandling.core.defaultnodesettings.filechooser.reader.SettingsModelReaderFileChooser;
import org.knime.filehandling.core.defaultnodesettings.filtermode.SettingsModelFilterMode.FilterMode;

/**
 * Configuration of the "Delete Files/Folders" node.
 *
 * @author Timmo Waller-Ehrat, KNIME GmbH, Konstanz, Germany
 */
final class DeleteFilesAndFoldersNodeConfig {

    private static final String CFG_FILE_CHOOSER = "file_chooser";

    private static final String CFG_ABORT_IF_FAILS = "abort_if_delete_fails";

    private static final boolean DEFAULT_ABORT_IF_FAILS = true;

    private final SettingsModelReaderFileChooser m_fileChooserSettings;

    private boolean m_abortIfFails;

    DeleteFilesAndFoldersNodeConfig(final PortsConfiguration portsConfiguration) {
        m_fileChooserSettings = new SettingsModelReaderFileChooser(CFG_FILE_CHOOSER, portsConfiguration,
            DeleteFilesAndFoldersNodeFactory.CONNECTION_INPUT_PORT_GRP_NAME, FilterMode.FILE);

        m_fileChooserSettings.getFilterModeModel().setIncludeSubfolders(true);

        m_abortIfFails = DEFAULT_ABORT_IF_FAILS;
    }

    boolean isAbortedIfFails() {
        return m_abortIfFails;
    }

    void setAbortIfFails(final boolean abortIfFails) {
        m_abortIfFails = abortIfFails;
    }

    SettingsModelReaderFileChooser getFileChooserSettings() {
        return m_fileChooserSettings;
    }

    void loadSettingsForModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_fileChooserSettings.loadSettingsFrom(settings);
        m_abortIfFails = settings.getBoolean(CFG_ABORT_IF_FAILS, DEFAULT_ABORT_IF_FAILS);
    }

    void saveSettingsForModel(final NodeSettingsWO settings) {
        m_fileChooserSettings.saveSettingsTo(settings);
        settings.addBoolean(CFG_ABORT_IF_FAILS, m_abortIfFails);
    }

    void saveSettingsForDialog(final NodeSettingsWO settings) {
        settings.addBoolean(CFG_ABORT_IF_FAILS, m_abortIfFails);
    }

    void validateSettingsForModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_fileChooserSettings.validateSettings(settings);
        settings.getBoolean(CFG_ABORT_IF_FAILS);
    }

    void loadSettingsForDialog(final NodeSettingsRO settings) {
        m_abortIfFails = settings.getBoolean(CFG_ABORT_IF_FAILS, DEFAULT_ABORT_IF_FAILS);
    }
}
