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
 *   Feb 9, 2021 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.node.table.reader.config.tablespec;

import static org.junit.Assert.assertEquals;
import static org.knime.filehandling.core.node.table.reader.TRFTestingUtils.a;
import static org.knime.filehandling.core.node.table.reader.config.tablespec.TableSpecConfigTestingUtils.getProductionPaths;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.convert.map.ProductionPath;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.filehandling.core.node.table.reader.config.tablespec.ConfigIDSerializer.EmptyConfigID;
import org.knime.filehandling.core.node.table.reader.config.tablespec.TableSpecConfigTestingUtils.TableSpecConfigBuilder;
import org.knime.filehandling.core.node.table.reader.selector.ColumnFilterMode;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Contains unit tests for V43TableSpecConfigSerializer.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
@RunWith(MockitoJUnitRunner.class)
public class V43TableSpecConfigSerializerTest {

    @Mock
    private ProductionPathSerializer m_productionPathSerializer;

    private V43TableSpecConfigSerializer<String> m_testInstance;

    /**
     * Initializes members used for testing.
     */
    @Before
    public void init() {
        m_testInstance = new V43TableSpecConfigSerializer<>(m_productionPathSerializer);
    }

    /**
     * Tests the {@link TableSpecConfigSerializer#load(NodeSettingsRO)} implementation.
     *
     * @throws InvalidSettingsException never thrown
     */
    @Test
    public void testLoad() throws InvalidSettingsException {
        final ProductionPath[] productionPaths =
            getProductionPaths(a("X", "Y", "Z"), a(StringCell.TYPE, IntCell.TYPE, DoubleCell.TYPE));
        final int[] positions = {2, 1, 0};
        final boolean[] keep = {true, false, true};
        final ColumnFilterMode columnFilterMode = ColumnFilterMode.INTERSECTION;
        String[] originalNames = a("A", "B", "C");
        final DataTableSpec tableSpec =
            new DataTableSpec(originalNames, a(StringCell.TYPE, IntCell.TYPE, DoubleCell.TYPE));
        final NodeSettings settings = Pre44TableSpecConfigSerializerTestUtils.createSettings(tableSpec, productionPaths,
            originalNames, positions, keep, columnFilterMode, false, 1, true);

        NodeSettingsRO productionPathSettings = settings.getNodeSettings("production_paths_Internals");
        when(m_productionPathSerializer.loadProductionPath(productionPathSettings, "production_path_0"))
            .thenReturn(productionPaths[0]);
        when(m_productionPathSerializer.loadProductionPath(productionPathSettings, "production_path_1"))
            .thenReturn(productionPaths[1]);
        when(m_productionPathSerializer.loadProductionPath(productionPathSettings, "production_path_2"))
            .thenReturn(productionPaths[2]);

        final TableSpecConfig<String> loaded = m_testInstance.load(settings);

        final TableSpecConfig<String> expected = new TableSpecConfigBuilder(EmptyConfigID.INSTANCE)//
            .withPositions(2, 1, 0)//
            .withKeep(true, false, true)//
            .withColumnFilterMode(ColumnFilterMode.INTERSECTION)//
            .withUnknownPosition(1)//
            .withKeepUnknown(false)//
            .withEnforceTypes(true)//
            .build();
        assertEquals(expected, loaded);
    }

    /**
     * Tests the {@link TableSpecConfigSerializer#load(NodeSettingsRO)} implementation.
     *
     * @throws InvalidSettingsException never thrown
     */
    @Test
    public void testLoadWithAdditionalParameters() throws InvalidSettingsException {
        final ProductionPath[] productionPaths =
            getProductionPaths(a("X", "Y", "Z"), a(StringCell.TYPE, IntCell.TYPE, DoubleCell.TYPE));
        final int[] positions = {2, 1, 0};
        final boolean[] keep = {true, false, true};
        final ColumnFilterMode columnFilterMode = ColumnFilterMode.INTERSECTION;
        String[] originalNames = a("A", "B", "C");
        final DataTableSpec tableSpec =
            new DataTableSpec(originalNames, a(StringCell.TYPE, IntCell.TYPE, DoubleCell.TYPE));
        final NodeSettings settings = Pre44TableSpecConfigSerializerTestUtils.createSettings(tableSpec, productionPaths,
            originalNames, positions, keep, columnFilterMode, false, 1, true);

        NodeSettingsRO productionPathSettings = settings.getNodeSettings("production_paths_Internals");
        when(m_productionPathSerializer.loadProductionPath(productionPathSettings, "production_path_0"))
            .thenReturn(productionPaths[0]);
        when(m_productionPathSerializer.loadProductionPath(productionPathSettings, "production_path_1"))
            .thenReturn(productionPaths[1]);
        when(m_productionPathSerializer.loadProductionPath(productionPathSettings, "production_path_2"))
            .thenReturn(productionPaths[2]);

        final TableSpecConfig<String> loaded =
            m_testInstance.load(settings, TableSpecConfigSerializer.AdditionalParameters.create());

        final TableSpecConfig<String> expected = new TableSpecConfigBuilder(EmptyConfigID.INSTANCE)//
            .withPositions(2, 1, 0)//
            .withKeep(true, false, true)//
            .withColumnFilterMode(ColumnFilterMode.INTERSECTION)//
            .withUnknownPosition(1)//
            .withKeepUnknown(false)//
            .withEnforceTypes(true)//
            .build();
        assertEquals(expected, loaded);
    }

    /**
     * Tests if calling the unsupported {@link TableSpecConfigSerializer#save(Object, NodeSettingsWO)} method results in
     * an {@link IllegalStateException}.
     */
    @SuppressWarnings("unchecked")
    @Test(expected = IllegalStateException.class)
    public void testSave() {
        m_testInstance.save(Mockito.mock(TableSpecConfig.class), Mockito.mock(NodeSettingsWO.class));
    }

}
