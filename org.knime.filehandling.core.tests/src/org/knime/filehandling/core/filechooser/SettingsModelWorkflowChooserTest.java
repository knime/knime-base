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
 *   17.09.2019 (Mareike Hoeger, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.filechooser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import java.util.Set;

import org.junit.Test;
import org.knime.filehandling.core.connections.FSCategory;
import org.knime.filehandling.core.connections.RelativeTo;
import org.knime.filehandling.core.defaultnodesettings.filechooser.workflow.SettingsModelWorkflowChooser;
import org.knime.filehandling.core.defaultnodesettings.filesystemchooser.FixedPortsConfiguration;
import org.knime.filehandling.core.defaultnodesettings.filesystemchooser.config.RelativeToSpecificConfig;
import org.knime.filehandling.core.port.FileSystemPortObject;

/**
 *
 * @author Carl Witt, KNIME AG, Zurich, Switzerland
 *
 */
public class SettingsModelWorkflowChooserTest {

    private static final String FILE_SYSTEM_PORT_IDENTIFIER = "FILE_SYSTEM_CONNECTION_PORT_ID";

    private static final FixedPortsConfiguration WITHOUT_FS_PORT =
        new FixedPortsConfiguration.FixedPortsConfigurationBuilder()//
            .addFixedInputPortGroup("Inputs")//
            .build();

    private static final FixedPortsConfiguration WITH_FS_PORT =
        new FixedPortsConfiguration.FixedPortsConfigurationBuilder()//
            .addFixedInputPortGroup("Inputs")//
            .addFixedInputPortGroup(FILE_SYSTEM_PORT_IDENTIFIER, FileSystemPortObject.TYPE)//
            .build();

    /**
     * When creating a workflow chooser settings model and no file system connection port is present, the relative to
     * file system category should be set.
     */
    @Test
    public void defaultUnconnectedFileSystemIsRelative() {
        SettingsModelWorkflowChooser smwfc =
            new SettingsModelWorkflowChooser("testConfigName", FILE_SYSTEM_PORT_IDENTIFIER, WITHOUT_FS_PORT);
        assertEquals(FSCategory.RELATIVE, smwfc.getLocation().getFSCategory());
    }

    /**
     * When creating a workflow chooser settings model and a file system connection port is present, the connected file
     * system category should be set.
     */
    @Test
    public void defaultConnectedFileSystemIsConnected() {
        SettingsModelWorkflowChooser smwfc =
            new SettingsModelWorkflowChooser("testConfigName", FILE_SYSTEM_PORT_IDENTIFIER, WITH_FS_PORT);
        assertEquals(FSCategory.CONNECTED, smwfc.getLocation().getFSCategory());
    }

    /**
     * Make sure only allowed values are set on {@link RelativeToSpecificConfig}
     */
    @Test
    public void relativeToIsRestricted() {
        var relativeToConfig = new RelativeToSpecificConfig(true, RelativeTo.MOUNTPOINT,
            Set.of(RelativeTo.MOUNTPOINT, RelativeTo.WORKFLOW));
        assertThrows(IllegalArgumentException.class, () -> relativeToConfig.setRelativeTo(RelativeTo.SPACE));
    }

}
