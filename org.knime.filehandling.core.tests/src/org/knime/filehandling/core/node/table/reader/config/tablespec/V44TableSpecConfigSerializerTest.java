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
 *   Feb 10, 2021 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.node.table.reader.config.tablespec;

import static java.util.stream.Collectors.toList;
import static org.junit.Assert.assertEquals;
import static org.knime.filehandling.core.node.table.reader.config.tablespec.TableSpecConfigTestingUtils.COL1;
import static org.knime.filehandling.core.node.table.reader.config.tablespec.TableSpecConfigTestingUtils.COL2;
import static org.knime.filehandling.core.node.table.reader.config.tablespec.TableSpecConfigTestingUtils.COL3;
import static org.knime.filehandling.core.node.table.reader.config.tablespec.TableSpecConfigTestingUtils.PATH1;
import static org.knime.filehandling.core.node.table.reader.config.tablespec.TableSpecConfigTestingUtils.ROOT_PATH;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.knime.core.data.convert.map.ProductionPath;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.filehandling.core.node.table.reader.config.tablespec.TableSpecConfigTestingUtils.TableSpecConfigBuilder;
import org.knime.filehandling.core.node.table.reader.selector.ColumnFilterMode;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Contains unit tests for V44TableSpecConfigSerializer.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
@RunWith(MockitoJUnitRunner.class)
public class V44TableSpecConfigSerializerTest {

    @Mock
    private ConfigIDLoader m_configIDLoader;

    @Mock
    private ConfigID m_configID;

    @Mock
    private NodeSettingsSerializer<String> m_typeSerializer;

    @Mock
    private ProductionPathSerializer m_productionPathSerializer;

    @Mock
    private TableSpecConfig<String> m_tableSpecConfig;

    private V44TableSpecConfigSerializer<String> m_testInstance;

    /**
     * Initializes the test instance.
     */
    @Before
    public void init() {
        m_testInstance =
            new V44TableSpecConfigSerializer<>(m_productionPathSerializer, m_configIDLoader, m_typeSerializer);
    }

    /**
     * Tests the {@link TableSpecConfigSerializer#save(Object, NodeSettingsWO)} method.
     *
     * @throws InvalidSettingsException not thrown
     */
    @Test
    public void testSave() throws InvalidSettingsException {
        final TableSpecConfig<String> config = builder()//
            .withEnforceTypes(true)//
            .withUnknownPosition(3)//
            .withKeepUnknown(false)//
            .withPositions(1, 2, 0)//
            .withKeep(false, true, true)//
            .withColumnFilterMode(ColumnFilterMode.INTERSECTION)//
            .build();
        final NodeSettings settings = new NodeSettings("test");
        m_testInstance.save(config, settings);
        verify(m_configID).save(settings.getNodeSettings("config_id"));
        final NodeSettings expected = createExpectedSavedSettings(config);
        assertEquals(expected, settings);
    }

    private static NodeSettings createExpectedSavedSettings(final TableSpecConfig<String> config) {
        final NodeSettings expected = new NodeSettings("test");
        expected.addString("source_group_id", ROOT_PATH);
        expected.addNodeSettings("config_id");
        final NodeSettingsWO expectedIndividualSettings = expected.addNodeSettings("individual_specs");
        TypedReaderTableSpecSerializerTest.fillSettings(expectedIndividualSettings.addNodeSettings(PATH1),
            TableSpecConfigTestingUtils.SPEC1);
        TypedReaderTableSpecSerializerTest.fillSettings(
            expectedIndividualSettings.addNodeSettings(TableSpecConfigTestingUtils.PATH2),
            TableSpecConfigTestingUtils.SPEC2);
        TableTransformationSerializerTest.fillSettings(expected.addNodeSettings("table_transformation"), false, 3, true,
            ColumnFilterMode.INTERSECTION, config.getTableTransformation().stream().collect(toList()));
        return expected;
    }

    private TableSpecConfigBuilder builder() {
        return new TableSpecConfigTestingUtils.TableSpecConfigBuilder(m_configID);
    }

    /**
     * Tests the {@link TableSpecConfigSerializer#load(NodeSettingsRO)} method.
     *
     * @throws InvalidSettingsException never thrown
     */
    @Test
    public void testLoad() throws InvalidSettingsException {
        TableSpecConfigBuilder builder = builder();
        final TableSpecConfig<String> config = builder//
            .withEnforceTypes(true)//
            .withUnknownPosition(3)//
            .withKeepUnknown(false)//
            .withPositions(1, 2, 0)//
            .withKeep(false, true, true)//
            .withColumnFilterMode(ColumnFilterMode.INTERSECTION)//
            .build();
        final NodeSettings settings = createExpectedSavedSettings(config);
        stubForLoading(builder);

        final TableSpecConfig<String> loaded = m_testInstance.load(settings);
        assertEquals(config, loaded);
    }

    /**
     * Tests loading with additional parameters i.e. the skip empty columns option.
     *
     * @throws InvalidSettingsException never thrown
     */
    @Test
    public void testLoadWithAdditionalParameters() throws InvalidSettingsException {
        TableSpecConfigBuilder builder = builder();
        final TableSpecConfig<String> config = builder//
            .withEnforceTypes(true)//
            .withUnknownPosition(3)//
            .withKeepUnknown(false)//
            .withPositions(1, 2, 0)//
            .withKeep(false, true, true)//
            .withSkipEmptyColumns(true)//
            .withColumnFilterMode(ColumnFilterMode.INTERSECTION)//
            .build();
        final NodeSettings settings = createExpectedSavedSettings(config);
        stubForLoading(builder);

        final TableSpecConfig<String> loaded = m_testInstance.load(settings,
            TableSpecConfigSerializer.AdditionalParameters.create().withSkipEmptyColumns(true));

        assertEquals(config, loaded);
    }

    private void stubForLoading(final TableSpecConfigBuilder builder) throws InvalidSettingsException {
        when(m_configIDLoader.createFromSettings(any())).thenReturn(m_configID);
        when(m_typeSerializer.load(any())).thenReturn(//
            COL1.getType(), COL2.getType(), COL3.getType(), // for TableTransformation
            COL1.getType(), COL2.getType(), COL2.getType(), COL3.getType()); // for individual specs
        when(m_productionPathSerializer.loadProductionPath(any(), any())).thenReturn(builder.m_prodPaths[0],
            Arrays.stream(builder.m_prodPaths).skip(1).toArray(ProductionPath[]::new));
    }

    /**
     * Tests that serializing and then deserializing works.
     *
     * @throws InvalidSettingsException never thrown
     */
    @Test
    public void testSaveLoad() throws InvalidSettingsException {
        final TableSpecConfigBuilder builder = builder();
        final TableSpecConfig<String> config = builder//
            .withEnforceTypes(false)//
            .withUnknownPosition(2)//
            .withKeepUnknown(true)//
            .withPositions(0, 2, 1)//
            .withKeep(false, true, false)//
            .withColumnFilterMode(ColumnFilterMode.UNION)//
            .build();
        final NodeSettings settings = new NodeSettings("test");
        m_testInstance.save(config, settings);

        stubForLoading(builder);
        final TableSpecConfig<String> loaded = m_testInstance.load(settings);
        assertEquals(config, loaded);
    }

}
