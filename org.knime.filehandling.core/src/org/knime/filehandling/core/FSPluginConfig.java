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
 *   Feb 28, 2020 (bjoern): created
 */
package org.knime.filehandling.core;

import org.eclipse.core.runtime.Platform;
import org.knime.filehandling.core.defaultnodesettings.FileChooserHelper;

/**
 * Class to access preferences of the file handling plugin. Use {@link #load()} to read the preferences.
 *
 * @author Bjoern Lohrmann, KNIME GmbH
 */
public class FSPluginConfig {

    private static final String KEY_ALLOW_LOCAL_FS_ACCESS_ON_SERVER = "allow_local_fs_access_on_server";

    private static final boolean DEFAULT_ALLOW_LOCAL_FS_ACCESS_ON_SERVER = false;

    private final boolean m_allowLocalFsAccessOnServer;

    FSPluginConfig(final boolean allowLocalFsAccessOnServer) {
        m_allowLocalFsAccessOnServer = allowLocalFsAccessOnServer;
    }

    /**
     * @return true when local file system access via {@link FileChooserHelper} shall be allowed, when executing on a
     *         KNIME Server Executor, false otherwise.
     */
    public boolean allowLocalFsAccessOnServer() {
        return m_allowLocalFsAccessOnServer;
    }

    /**
     *
     * @return a new {@link FSPluginConfig} instance that holds the preferences
     */
    public static FSPluginConfig load() {
        final boolean allowLocalFsAccessOnServer = Platform.getPreferencesService().getBoolean(
            FSPluginActivator.getDefault().getBundle().getSymbolicName(),
            KEY_ALLOW_LOCAL_FS_ACCESS_ON_SERVER,
            DEFAULT_ALLOW_LOCAL_FS_ACCESS_ON_SERVER, null);

        return new FSPluginConfig(allowLocalFsAccessOnServer);
    }
}
