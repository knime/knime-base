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
 *   Mar 4, 2026 (Thomas Reifenberger): created
 */
package org.knime.base.node.preproc.manipulator;

import java.io.FileInputStream;
import java.io.IOException;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.webui.node.dialog.SettingsType;
import org.knime.core.webui.node.dialog.defaultdialog.NodeParametersUtil;
import org.knime.testing.node.dialog.DefaultNodeSettingsSnapshotTest;
import org.knime.testing.node.dialog.SnapshotTestConfiguration;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

/**
 * Snapshot test for {@link TableManipulatorNodeParameters}.
 *
 * @author Thomas Reifenberger, TNG Technology Consulting GmbH
 */
@SuppressWarnings("restriction")
final class TableManipulatorNodeParametersTest extends DefaultNodeSettingsSnapshotTest {

    private static MockedStatic<TableManipulatorSpecific> m_mock;

    @BeforeAll
    static void beforeAll() {
        /*
         Restrict the data types to a small, static set. This is needed because the available data types differ
         between environments based on the installed extensions (especially local vs. Jenkins), which would cause
         snapshot tests to fail.
        */
        final var productionPathProviderForTesting = new TableManipulatorProductionPathProviderForTesting();
        m_mock = Mockito.mockStatic(TableManipulatorSpecific.class, Mockito.CALLS_REAL_METHODS);
        m_mock.when(TableManipulatorSpecific::getProductionPathProvider).thenReturn(productionPathProviderForTesting);
    }

    @AfterAll
    static void afterAll() {
        m_mock.close();
    }

    TableManipulatorNodeParametersTest() {
        super(getConfig());
    }

    private static SnapshotTestConfiguration getConfig() {
        return SnapshotTestConfiguration.builder() //
            .withInputPortObjectSpecs(createInputPortSpecs()) //
            .testJsonFormsForModel(TableManipulatorNodeParameters.class) //
            .testJsonFormsWithInstance(SettingsType.MODEL, () -> readSettings()) //
            .testNodeSettingsStructure(() -> readSettings()) //
            // Tests for migration from old settings model
            .testNodeSettingsStructure(() -> readSettings("_4_7_2_empty")) //
            .testNodeSettingsStructure(() -> readSettings("_4_7_2_configured")) //
            .testNodeSettingsStructure(() -> readSettings("_4_7_2_multi")) //
            .testNodeSettingsStructure(() -> readSettings("_5_8_0_empty")) //
            .testNodeSettingsStructure(() -> readSettings("_5_8_0_configured")) //
            .testNodeSettingsStructure(() -> readSettings("_5_8_0_multi")) //
            // End of migration tests
            .build();
    }

    private static PortObjectSpec[] createInputPortSpecs() {
        return new PortObjectSpec[]{
            new DataTableSpec(new String[]{"Universe_0_0", "Universe_0_1", "Universe_1_0", "Universe_1_1"},
                new DataType[]{DoubleCell.TYPE, DoubleCell.TYPE, DoubleCell.TYPE, DoubleCell.TYPE})};
    }

    private static TableManipulatorNodeParameters readSettings() {
        return readSettings("");
    }

    private static TableManipulatorNodeParameters readSettings(final String suffix) {
        try {
            var path = getSnapshotPath(TableManipulatorNodeParameters.class).getParent().resolve("node_settings")
                .resolve("TableManipulatorNodeParameters" + suffix + ".xml");
            try (var fis = new FileInputStream(path.toFile())) {
                var nodeSettings = NodeSettings.loadFromXML(fis);
                return NodeParametersUtil.loadSettings(nodeSettings.getNodeSettings(SettingsType.MODEL.getConfigKey()),
                    TableManipulatorNodeParameters.class);
            }
        } catch (IOException | InvalidSettingsException e) {
            throw new IllegalStateException(e);
        }
    }
}
