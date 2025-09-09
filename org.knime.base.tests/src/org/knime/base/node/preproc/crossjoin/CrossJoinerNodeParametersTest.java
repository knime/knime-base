/*
 * ------------------------------------------------------------------------
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
 * ---------------------------------------------------------------------
 */
package org.knime.base.node.preproc.crossjoin;

import java.io.FileInputStream;
import java.io.IOException;

import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.webui.node.dialog.SettingsType;
import org.knime.core.webui.node.dialog.defaultdialog.NodeParametersUtil;
import org.knime.testing.node.dialog.DefaultNodeSettingsSnapshotTest;
import org.knime.testing.node.dialog.SnapshotTestConfiguration;

@SuppressWarnings("restriction")
final class CrossJoinerNodeParametersTest extends DefaultNodeSettingsSnapshotTest {

    CrossJoinerNodeParametersTest() {
        super(getConfig());
    }

    private static SnapshotTestConfiguration getConfig() {
        return SnapshotTestConfiguration.builder() //
            .withInputPortObjectSpecs(createInputPortSpecs()) // two input tables
            .testJsonFormsForModel(CrossJoinerNodeParameters.class) // model json generation
            .testJsonFormsWithInstance(SettingsType.MODEL, () -> readSettings()) // with provided instance
            .testNodeSettingsStructure(() -> readSettings()) // ensure structure matches
            .build();
    }

    private static CrossJoinerNodeParameters readSettings() {
        try {
            var path = getSnapshotPath(CrossJoinerNodeParameters.class).getParent().resolve("node_settings")
                .resolve("CrossJoinerNodeParameters.xml");
            try (var fis = new FileInputStream(path.toFile())) {
                var nodeSettings = NodeSettings.loadFromXML(fis);
                return NodeParametersUtil.loadSettings(
                    nodeSettings.getNodeSettings(SettingsType.MODEL.getConfigKey()),
                    CrossJoinerNodeParameters.class);
            }
        } catch (IOException | InvalidSettingsException e) {
            throw new IllegalStateException(e);
        }
    }

    private static PortObjectSpec[] createInputPortSpecs() {
        // Create two simple table specs; content columns aren't interpreted by this node's parameters
        return new PortObjectSpec[] { createDefaultTestTableSpec(), createDefaultTestTableSpec() };
    }

    private static DataTableSpec createDefaultTestTableSpec() {
        return new DataTableSpec(
            new DataColumnSpecCreator("col1", StringCell.TYPE).createSpec());
    }
}
