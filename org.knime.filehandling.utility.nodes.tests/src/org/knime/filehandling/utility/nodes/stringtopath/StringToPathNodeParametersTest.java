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
 *   Feb 16, 2026 (paulbaernreuther): created
 */
package org.knime.filehandling.utility.nodes.stringtopath;

import java.io.FileInputStream;
import java.io.IOException;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.workflow.NodeContext;
import org.knime.core.webui.node.dialog.SettingsType;
import org.knime.core.webui.node.dialog.defaultdialog.NodeParametersUtil;
import org.knime.testing.node.dialog.DefaultNodeSettingsSnapshotTest;
import org.knime.testing.node.dialog.SnapshotTestConfiguration;
import org.knime.testing.util.WorkflowManagerUtil;

class StringToPathNodeParametersTest extends DefaultNodeSettingsSnapshotTest {

    StringToPathNodeParametersTest() {
        super(getConfig());
    }

    /**
     * Overwrite the node context since we need a node with one input port.
     *
     * @throws IOException
     */
    @BeforeAll
    static void mockNodeContext() throws IOException {
        final var wfm = WorkflowManagerUtil.createEmptyWorkflow();
        final var nodeContainer = WorkflowManagerUtil.createAndAddNode(wfm, new StringToPathNodeFactory());

        NodeContext.pushContext(nodeContainer);
    }

    @AfterAll
    static void popNodeContext() {
        NodeContext.removeLastContext();
    }

    private static SnapshotTestConfiguration getConfig() {
        return SnapshotTestConfiguration.builder() //
            .withInputPortObjectSpecs(createInputPortSpecs()) //
            .testJsonFormsForModel(StringToPathNodeParameters.class) //
            .testJsonFormsWithInstance(SettingsType.MODEL, () -> readSettings("StringToPathNodeParameters.xml")) //
            .testNodeSettingsStructure(() -> readSettings("StringToPathNodeParameters.xml")) //
            .testJsonFormsWithInstance(SettingsType.MODEL, () -> readSettings("StringToPathNodeParameters1.xml")) //
            .testNodeSettingsStructure(() -> readSettings("StringToPathNodeParameters1.xml")) //
            .build();
    }

    private static StringToPathNodeParameters readSettings(final String fileName) {
        try {
            var path = getSnapshotPath(StringToPathNodeParameters.class).getParent().resolve("node_settings")
                .resolve(fileName);
            try (var fis = new FileInputStream(path.toFile())) {
                var nodeSettings = NodeSettings.loadFromXML(fis);
                return NodeParametersUtil.loadSettings(nodeSettings.getNodeSettings(SettingsType.MODEL.getConfigKey()),
                    StringToPathNodeParameters.class);
            }
        } catch (IOException | InvalidSettingsException e) {
            throw new IllegalStateException(e);
        }
    }

    private static PortObjectSpec[] createInputPortSpecs() {
        return new PortObjectSpec[]{createDefaultTestTableSpec()};
    }

    private static DataTableSpec createDefaultTestTableSpec() {
        var pathColumnSpec = new DataColumnSpecCreator("String column", StringCell.TYPE).createSpec();
        return new DataTableSpec(pathColumnSpec);
    }
}
