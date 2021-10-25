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
 *   02.08.2021 (jl): created
 */
package org.knime.filehandling.core.fs.knime.local.relativeto.export;

import org.knime.filehandling.core.connections.DefaultFSLocationSpec;
import org.knime.filehandling.core.connections.FSCategory;
import org.knime.filehandling.core.connections.FSLocationSpec;
import org.knime.filehandling.core.connections.RelativeTo;
import org.knime.filehandling.core.connections.meta.FSType;

/**
 * Contains constants related to the “relative to” file systems
 *
 * @author Sascha Wolke, KNIME GmbH
 * @noreference non-public API
 * @noextend non-public API
 */
public final class RelativeToFileSystemConstants {

    private RelativeToFileSystemConstants() {}

    /**
     * {@link FSLocationSpec} for the convenience relative-to workflow file system.
     */
    public static final FSLocationSpec CONVENIENCE_WORKFLOW_RELATIVE_FS_LOCATION_SPEC =
        new DefaultFSLocationSpec(FSCategory.RELATIVE, RelativeTo.WORKFLOW.getSettingsValue());
    /**
     * {@link FSLocationSpec} for the convenience relative-to workflow data area file system.
     */
    public static final FSLocationSpec CONVENIENCE_WORKFLOW_DATA_RELATIVE_FS_LOCATION_SPEC =
        new DefaultFSLocationSpec(FSCategory.RELATIVE, RelativeTo.WORKFLOW_DATA.getSettingsValue());
    /**
     * {@link FSLocationSpec} for the convenience relative-to mountpoint file system.
     */
    public static final FSLocationSpec CONVENIENCE_MOUNTPOINT_RELATIVE_FS_LOCATION_SPEC =
        new DefaultFSLocationSpec(FSCategory.RELATIVE, RelativeTo.MOUNTPOINT.getSettingsValue());
    /**
     * {@link FSLocationSpec} for the connected relative-to workflow file system.
     */
    public static final FSLocationSpec CONNECTED_WORKFLOW_RELATIVE_FS_LOCATION_SPEC =
        new DefaultFSLocationSpec(FSCategory.CONNECTED, FSType.RELATIVE_TO_WORKFLOW.getTypeId());
    /**
     * {@link FSLocationSpec} for the connected relative-to mountpoint file system.
     */
    public static final FSLocationSpec CONNECTED_MOUNTPOINT_RELATIVE_FS_LOCATION_SPEC =
        new DefaultFSLocationSpec(FSCategory.CONNECTED, FSType.RELATIVE_TO_MOUNTPOINT.getTypeId());
    /**
     * {@link FSLocationSpec} for the connected relative-to workflow data area file system.
     */
    public static final FSLocationSpec CONNECTED_WORKFLOW_DATA_RELATIVE_FS_LOCATION_SPEC =
        new DefaultFSLocationSpec(FSCategory.CONNECTED, FSType.RELATIVE_TO_WORKFLOW_DATA_AREA.getTypeId());
}
