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
 *   01 Mar 2024 (DBogenrieder): created
 */
package org.knime.base.node.preproc.rowkey2;

import java.io.FileInputStream;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.webui.node.dialog.SettingsType;
import org.knime.core.webui.node.dialog.defaultdialog.NodeParametersUtil;
import org.knime.testing.node.dialog.DefaultNodeSettingsSnapshotTest;
import org.knime.testing.node.dialog.SnapshotTestConfiguration;

/**
 * Snapshot test for the row key node
 *
 * @author Daniel Bogenrieder, KNIME GmbH, Konstanz, Germany
 */
@SuppressWarnings("restriction")
public class RowKeyNodeSettingsTest extends DefaultNodeSettingsSnapshotTest {
    @SuppressWarnings("javadoc")
    protected RowKeyNodeSettingsTest() {
        super(getConfig());
    }

    static DataTableSpec createDefaultTestTableSpec() {
        return new DataTableSpec(new String[]{"test1", "test2"}, new DataType[]{IntCell.TYPE, StringCell.TYPE});
    }

    private static SnapshotTestConfiguration getConfig() {
        return SnapshotTestConfiguration.builder() //
                .withInputPortObjectSpecs(new PortObjectSpec[] {createDefaultTestTableSpec()}) //
                .testJsonFormsForModel(RowKeyNodeSettings.class) //
                .testJsonFormsWithInstance(SettingsType.MODEL, () -> readSettings("GenerateNewRowKeys.xml")) //
                .testNodeSettingsStructure(() -> readSettings("GenerateNewRowKeys.xml")) //
                .testJsonFormsWithInstance(SettingsType.MODEL, () -> readSettings("ReplaceAndAppend.xml")) //
                .testNodeSettingsStructure(() -> readSettings("ReplaceAndAppend.xml")) //
                .build();
    }

    private static RowKeyNodeSettings readSettings(final String filename) throws Exception {
        var path = getSnapshotPath(RowKeyNodeSettings.class).getParent().resolve("node_settings").resolve("RowKeyNodeSettings").resolve(filename);
        try (var fis = new FileInputStream(path.toFile())) {
            var nodeSettings = NodeSettings.loadFromXML(fis);
            return NodeParametersUtil.loadSettings(nodeSettings.getNodeSettings(SettingsType.MODEL.getConfigKey()), RowKeyNodeSettings.class);
        }
    }
}
