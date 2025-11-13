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
 *   Nov 12, 2025 (Paul Bärnreuther): created
 */
package org.knime.base.node.preproc.pivot;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DataTypeRegistry;
import org.knime.core.data.def.BooleanCell;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.LongCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.webui.node.dialog.SettingsType;
import org.knime.core.webui.node.dialog.defaultdialog.NodeParametersUtil;
import org.knime.testing.node.dialog.DefaultNodeSettingsSnapshotTest;
import org.knime.testing.node.dialog.SnapshotTestConfiguration;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

class Pivot2NodeParametersTest extends DefaultNodeSettingsSnapshotTest {

    static final PortObjectSpec[] TEST_TABLE_SPECS = new PortObjectSpec[]{//
        new DataTableSpec(
            new String[]{"Universe_0_0", "Universe_0_1", "Universe_1_0", "Universe_1_1", "Cluster Membership"}, //
            new DataType[]{DoubleCell.TYPE, DoubleCell.TYPE, DoubleCell.TYPE, DoubleCell.TYPE, StringCell.TYPE}) //
    };

    Pivot2NodeParametersTest() {
        super(getConfig());
    }

    MockedStatic<DataTypeRegistry> m_mockedStaticRegistry;

    private static final Set<DataType> DATA_TYPES =
        Set.of(StringCell.TYPE, LongCell.TYPE, IntCell.TYPE, DoubleCell.TYPE, BooleanCell.TYPE);

    @BeforeEach
    void setDataTypes() {
        // need to mock the data type registry. Use the real one because
        // it's got some important fields that are hard to mock.
        var realRegistry = DataTypeRegistry.getInstance();

        // let's intercept the call to realRegistry.availableDataTypes...
        var registrySpy = Mockito.spy(realRegistry);

        // ... and return our own set of data types. But it has side effects so call real method first
        Mockito.when(registrySpy.availableDataTypes()).thenAnswer(i -> {
            i.callRealMethod();
            return DATA_TYPES;
        });

        m_mockedStaticRegistry = Mockito.mockStatic(DataTypeRegistry.class, Mockito.CALLS_REAL_METHODS);
        m_mockedStaticRegistry.when(DataTypeRegistry::getInstance).thenReturn(registrySpy);

        // just quickly check that the mocking has worked
        var mockCallResult = DataTypeRegistry.getInstance().availableDataTypes();
        if (mockCallResult.size() != DATA_TYPES.size()) {
            throw new IllegalStateException("Mocking of DataTypeRegistry failed: expected " + DATA_TYPES + " but got "
                + mockCallResult + " (lengths: " + DATA_TYPES.size() + " vs " + mockCallResult.size() + ")");
        }
    }

    @AfterEach
    void resetDataTypes() {
        m_mockedStaticRegistry.close();
    }

    private static SnapshotTestConfiguration getConfig() {
        return SnapshotTestConfiguration.builder() //
            .withInputPortObjectSpecs(TEST_TABLE_SPECS) //
            .testJsonFormsForModel(Pivot2NodeParameters.class) //
            .testJsonFormsWithInstance(SettingsType.MODEL, () -> readSettings()) //
            .testNodeSettingsStructure(() -> readSettings()) //
            .build();
    }

    private static Pivot2NodeParameters readSettings() {
        try {
            var path = getSnapshotPath(Pivot2NodeParameters.class).getParent().resolve("node_settings")
                .resolve("Pivot2NodeParameters.xml");
            try (var fis = new FileInputStream(path.toFile())) {
                var nodeSettings = NodeSettings.loadFromXML(fis);
                return NodeParametersUtil.loadSettings(nodeSettings.getNodeSettings(SettingsType.MODEL.getConfigKey()),
                    Pivot2NodeParameters.class);
            }
        } catch (IOException | InvalidSettingsException e) {
            throw new IllegalStateException(e);
        }
    }

}
