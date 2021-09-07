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
 *   Sep 7, 2021 (bjoern): created
 */
package org.knime.filehandling.core.connections;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.knime.filehandling.core.connections.meta.FSType;

/**
 *
 * @author Bjoern Lohrmann KNIME GmbH
 */
public class FSLocationTest {

    @Test
    public void identical_fs_locations_are_equal() {
        final var fsLoc = new FSLocation(FSCategory.LOCAL, "/bla");
        assertEquals(fsLoc, fsLoc);
    }

    @Test
    public void fs_locations_with_equal_content_are_equal() {
        assertEquals(new FSLocation(FSCategory.LOCAL, "/bla"), new FSLocation(FSCategory.LOCAL, "/bla"));
        assertEquals(new FSLocation(FSCategory.CONNECTED, FSType.LOCAL_FS.getTypeId(), "/bla"), //
            new FSLocation(FSCategory.CONNECTED, "local", "/bla"));

        assertEquals(new FSLocation(FSCategory.RELATIVE, RelativeTo.MOUNTPOINT.getSettingsValue(), "/bla"), //
            new FSLocation(FSCategory.RELATIVE, RelativeTo.MOUNTPOINT.getSettingsValue(), "/bla"));
        assertEquals(new FSLocation(FSCategory.CONNECTED, FSType.RELATIVE_TO_MOUNTPOINT.getTypeId(), "bla"), //
            new FSLocation(FSCategory.CONNECTED, FSType.RELATIVE_TO_MOUNTPOINT.getTypeId(), "bla"));

        assertEquals(new FSLocation(FSCategory.CUSTOM_URL, "1000", "https://bla.com/"), //
            new FSLocation(FSCategory.CUSTOM_URL, "1000", "https://bla.com/"));

        assertEquals(new FSLocation(FSCategory.MOUNTPOINT, "LOCAL", "/bla"), //
            new FSLocation(FSCategory.MOUNTPOINT, "LOCAL", "/bla"));
        assertEquals(new FSLocation(FSCategory.CONNECTED, FSType.MOUNTPOINT.getTypeId(), "/bla"), //
            new FSLocation(FSCategory.CONNECTED, FSType.MOUNTPOINT.getTypeId(), "/bla"));
    }

    @Test
    public void local_fs_locations_convenience_connected_can_be_equal() {
        assertEquals(new FSLocation(FSCategory.LOCAL, "/bla"), //
            new FSLocation(FSCategory.CONNECTED, "local", "/bla"));
    }

    @Test
    public void relative_mountpoint_fs_locations_convenience_connected_can_be_equal() {
        assertEquals(new FSLocation(FSCategory.RELATIVE, RelativeTo.MOUNTPOINT.getSettingsValue(), "bla"), //
            new FSLocation(FSCategory.CONNECTED, FSType.RELATIVE_TO_MOUNTPOINT.getTypeId(), "bla"));
    }

    @Test
    public void relative_workflow_fs_locations_convenience_connected_can_be_equal() {
        assertEquals(new FSLocation(FSCategory.RELATIVE, RelativeTo.WORKFLOW.getSettingsValue(), "../bla"), //
            new FSLocation(FSCategory.CONNECTED, FSType.RELATIVE_TO_WORKFLOW.getTypeId(), "../bla"));
    }

    @Test
    public void relative_workflowdataarea_fs_locations_convenience_connected_can_be_equal() {
        assertEquals(new FSLocation(FSCategory.RELATIVE, RelativeTo.WORKFLOW_DATA.getSettingsValue(), "/bla"), //
            new FSLocation(FSCategory.CONNECTED, FSType.RELATIVE_TO_WORKFLOW_DATA_AREA.getTypeId(), "/bla"));
    }

    @Test
    public void custom_url_fs_locations_can_be_equal_despite_different_timeout() {
        assertEquals(new FSLocation(FSCategory.CUSTOM_URL, "1000", "https://bla.com/"), //
            new FSLocation(FSCategory.CUSTOM_URL, "2000", "https://bla.com/"));
    }

    @Test
    public void equal_fs_locations_should_have_same_hashcode() {
        final FSLocation first = new FSLocation(FSCategory.LOCAL, "/bla");
        final FSLocation second = new FSLocation(FSCategory.CONNECTED, FSType.LOCAL_FS.getTypeId(), "/bla");
        assertEquals(first.hashCode(), second.hashCode());
    }
}
