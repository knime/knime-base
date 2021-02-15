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

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.knime.filehandling.core.node.table.reader.TRFTestingUtils.a;
import static org.knime.filehandling.core.node.table.reader.config.tablespec.Pre44TableSpecConfigSerializerTestUtils.COL1;
import static org.knime.filehandling.core.node.table.reader.config.tablespec.Pre44TableSpecConfigSerializerTestUtils.COL2;
import static org.knime.filehandling.core.node.table.reader.config.tablespec.Pre44TableSpecConfigSerializerTestUtils.INTERSECTION;
import static org.knime.filehandling.core.node.table.reader.config.tablespec.Pre44TableSpecConfigSerializerTestUtils.SPEC1;
import static org.knime.filehandling.core.node.table.reader.config.tablespec.TableSpecConfigTestingUtils.REGISTRY;
import static org.knime.filehandling.core.node.table.reader.config.tablespec.TableSpecConfigTestingUtils.getProductionPaths;

import org.junit.Before;
import org.junit.Test;
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
import org.knime.filehandling.core.node.table.reader.config.tablespec.TableSpecConfigSerializer.AdditionalParameters;
import org.knime.filehandling.core.node.table.reader.config.tablespec.TableSpecConfigTestingUtils.TableSpecConfigBuilder;
import org.knime.filehandling.core.node.table.reader.selector.ColumnFilterMode;
import org.knime.filehandling.core.node.table.reader.selector.RawSpec;
import org.knime.filehandling.core.node.table.reader.spec.TypedReaderColumnSpec;
import org.knime.filehandling.core.node.table.reader.spec.TypedReaderTableSpec;
import org.mockito.Mockito;

/**
 * Contains unit tests for V42TableSpecConfigSerializer.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
public class V42TableSpecConfigSerializerTest {

    private static final String MOST_GENERIC_EXTERNAL_TYPE = "X";

    private V42TableSpecConfigSerializer<String> m_testInstance;

    /**
     * Initializes the test instance.
     */
    @Before
    public void init() {
        m_testInstance = new V42TableSpecConfigSerializer<>(REGISTRY, MOST_GENERIC_EXTERNAL_TYPE);
    }

    /**
     * Tests the {@link TableSpecConfigSerializer#load(NodeSettingsRO)} implementation if SpecMergeMode was
     * Intersection.
     *
     * @throws InvalidSettingsException never thrown
     */
    @Test
    public void testLoadIntersection() throws InvalidSettingsException {
        final ProductionPath[] productionPaths = getProductionPaths(a("Y"), a(IntCell.TYPE));
        final DataTableSpec tableSpec = new DataTableSpec(a("B"), a(IntCell.TYPE));
        final NodeSettings settings = Pre44TableSpecConfigSerializerTestUtils.createSettings(tableSpec, productionPaths,
            null, null, null, null, null, null, null);

        final TableSpecConfig<String> loaded =
            m_testInstance.load(settings, TableSpecConfigSerializer.AdditionalParameters.create()
                .withColumnFilterMode(ColumnFilterMode.INTERSECTION));

        final ProductionPath[] expectedProductionPaths =
            getProductionPaths(a(MOST_GENERIC_EXTERNAL_TYPE, "Y", MOST_GENERIC_EXTERNAL_TYPE),
                a(StringCell.TYPE, IntCell.TYPE, StringCell.TYPE));

        TypedReaderColumnSpec<String> col3WithMostGenericType = TypedReaderColumnSpec.createWithName("C", "X", true);
        final TableSpecConfig<String> expected = new TableSpecConfigBuilder(EmptyConfigID.INSTANCE)//
            .withKeep(false, true, false)//
            .withProductionPaths(expectedProductionPaths)//
            .withSpecs(SPEC1, new TypedReaderTableSpec<>(asList(COL2, col3WithMostGenericType)))//
            .withCols(COL1, COL2, col3WithMostGenericType)//
            .withColumnFilterMode(ColumnFilterMode.INTERSECTION)//
            .withRawSpec(
                new RawSpec<>(new TypedReaderTableSpec<>(asList(COL1, COL2, col3WithMostGenericType)), INTERSECTION))//
            .build();
        assertEquals(expected, loaded);
    }

    /**
     * Tests the {@link TableSpecConfigSerializer#load(NodeSettingsRO)} implementation if SpecMergeMode was Union.
     *
     * @throws InvalidSettingsException never thrown
     */
    @Test
    public void testLoadUnion() throws InvalidSettingsException {
        final ProductionPath[] productionPaths =
            getProductionPaths(a("X", "Y", "Z"), a(StringCell.TYPE, IntCell.TYPE, DoubleCell.TYPE));
        final DataTableSpec tableSpec =
            new DataTableSpec(a("A", "B", "C"), a(StringCell.TYPE, IntCell.TYPE, DoubleCell.TYPE));
        final NodeSettings settings = Pre44TableSpecConfigSerializerTestUtils.createSettings(tableSpec, productionPaths,
            null, null, null, null, null, null, null);

        final TableSpecConfig<String> loaded =
            m_testInstance.load(settings, TableSpecConfigSerializer.AdditionalParameters.create()
                .withColumnFilterMode(ColumnFilterMode.UNION));

        final TableSpecConfig<String> expected = new TableSpecConfigBuilder(EmptyConfigID.INSTANCE)//
            .withKeep(true, true, true)//
            .withProductionPaths(productionPaths)//
            .withColumnFilterMode(ColumnFilterMode.UNION)//
            .build();
        assertEquals(expected, loaded);
    }

    /**
     * Tests if {@link TableSpecConfigSerializer#load(NodeSettingsRO, AdditionalParameters)} fails if no
     * {@link ColumnFilterMode} is set.
     *
     * @throws InvalidSettingsException not thrown
     */
    @Test(expected = IllegalStateException.class)
    public void testLoadWithNoColumnFilterMode() throws InvalidSettingsException {
        final ProductionPath[] productionPaths =
            getProductionPaths(a("X", "Y", "Z"), a(StringCell.TYPE, IntCell.TYPE, DoubleCell.TYPE));
        final DataTableSpec tableSpec =
            new DataTableSpec(a("A", "B", "C"), a(StringCell.TYPE, IntCell.TYPE, DoubleCell.TYPE));
        final NodeSettings settings = Pre44TableSpecConfigSerializerTestUtils.createSettings(tableSpec, productionPaths,
            null, null, null, null, null, null, null);
        m_testInstance.load(settings, AdditionalParameters.create());
    }

    /**
     * Tests if calling the {@link TableSpecConfigSerializer#load(NodeSettingsRO)} method results in an
     * {@link IllegalStateException} because no {@link ColumnFilterMode} is provided.
     *
     * @throws InvalidSettingsException not thrown
     */
    @Test(expected = IllegalStateException.class)
    public void testLoadWithoutAdditionalParameters() throws InvalidSettingsException {
        final ProductionPath[] productionPaths =
            getProductionPaths(a("X", "Y", "Z"), a(StringCell.TYPE, IntCell.TYPE, DoubleCell.TYPE));
        final DataTableSpec tableSpec =
            new DataTableSpec(a("A", "B", "C"), a(StringCell.TYPE, IntCell.TYPE, DoubleCell.TYPE));
        final NodeSettings settings = Pre44TableSpecConfigSerializerTestUtils.createSettings(tableSpec, productionPaths,
            null, null, null, null, null, null, null);
        m_testInstance.load(settings);
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
