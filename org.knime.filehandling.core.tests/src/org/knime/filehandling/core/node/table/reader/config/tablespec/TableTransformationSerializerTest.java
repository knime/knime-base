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
import static org.knime.filehandling.core.node.table.reader.config.tablespec.TableSpecConfigTestingUtils.RAW_SPEC;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.knime.core.data.convert.map.ProductionPath;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsWO;
import org.knime.filehandling.core.node.table.reader.DefaultTableTransformation;
import org.knime.filehandling.core.node.table.reader.ImmutableColumnTransformation;
import org.knime.filehandling.core.node.table.reader.ImmutableTableTransformation;
import org.knime.filehandling.core.node.table.reader.selector.ColumnFilterMode;
import org.knime.filehandling.core.node.table.reader.selector.ColumnTransformation;
import org.knime.filehandling.core.node.table.reader.selector.TableTransformation;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Contains unit tests for TableTransformationSerializer.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
@RunWith(MockitoJUnitRunner.class)
public class TableTransformationSerializerTest {

    @Mock
    private NodeSettingsSerializer<String> m_typeSerializer;

    @Mock
    private ProductionPathSerializer m_productionPathSerializer;

    private TableTransformationSerializer<String> m_testInstance;

    /**
     * Initializes the test instance.
     */
    @Before
    public void init() {
        final TypedReaderColumnSpecSerializer<String> specSerializer =
            new TypedReaderColumnSpecSerializer<>(m_typeSerializer);
        final ColumnTransformationSerializer<String> columnTransformationSerializer =
            new ColumnTransformationSerializer<>(specSerializer, m_productionPathSerializer);
        m_testInstance = new TableTransformationSerializer<>(columnTransformationSerializer);
    }

    /**
     * Tests the save method.
     */
    @Test
    public void testSave() {
        final ProductionPath[] productionPaths = new ProductionPath[]{new ProductionPath(null, null),
            new ProductionPath(null, null), new ProductionPath(null, null)};
        final ColumnTransformation<String> trans1 =
            new ImmutableColumnTransformation<>(TableSpecConfigTestingUtils.COL1, productionPaths[0], false, 1, "foo");
        final ColumnTransformation<String> trans2 =
            new ImmutableColumnTransformation<>(TableSpecConfigTestingUtils.COL2, productionPaths[1], true, 2, "bar");
        final ColumnTransformation<String> trans3 =
            new ImmutableColumnTransformation<>(TableSpecConfigTestingUtils.COL3, productionPaths[2], false, 0, "baz");
        final List<ColumnTransformation<String>> transformations = Arrays.asList(trans1, trans2, trans3);
        final TableTransformation<String> tableTransformation = new DefaultTableTransformation<>(RAW_SPEC,
            transformations, ColumnFilterMode.INTERSECTION, true, 1, true, true);
        final NodeSettings saved = new NodeSettings("test");
        m_testInstance.save(tableTransformation, saved);

        final NodeSettings expected = new NodeSettings("test");
        fillSettings(expected, true, true, 1, true, ColumnFilterMode.INTERSECTION, transformations);
        assertEquals(expected, saved);
    }

    static void fillSettings(final NodeSettingsWO settings, final boolean keepUnknown, final boolean skipEmptyColumns, final int positionForUnknown,
        final boolean enforceTypes, final ColumnFilterMode columnFilterMode,
        final List<? extends ColumnTransformation<String>> transformations) {
        final NodeSettingsWO columnsSettings = settings.addNodeSettings("columns");
        columnsSettings.addInt("num_columns", transformations.size());
        columnsSettings.addIntArray("intersection_indices", 1);
        for (int i = 0; i < transformations.size(); i++) {
            final ColumnTransformation<String> colTransformation = transformations.get(i);
            ColumnTransformationSerializerTest.fillSettings(columnsSettings.addNodeSettings("" + i),
                colTransformation.keep(), colTransformation.getPosition(), colTransformation.getName(),
                colTransformation.getExternalSpec().getName().orElseThrow(IllegalStateException::new),
                colTransformation.getExternalSpec().hasType());
        }
        settings.addBoolean("keep_unknown", keepUnknown);
        settings.addBoolean("skip_empty_columns", skipEmptyColumns);
        settings.addInt("position_for_unknown", positionForUnknown);
        settings.addBoolean("enforce_types", enforceTypes);
        settings.addString("column_filter_mode", columnFilterMode.name());
    }

    /**
     * Tests the load method.
     *
     * @throws InvalidSettingsException never thrown
     */
    @Test
    public void testLoad() throws InvalidSettingsException {
        final ProductionPath[] productionPaths = new ProductionPath[]{new ProductionPath(null, null),
            new ProductionPath(null, null), new ProductionPath(null, null)};
        final ImmutableColumnTransformation<String> trans1 =
            new ImmutableColumnTransformation<>(TableSpecConfigTestingUtils.COL1, productionPaths[0], true, 1, "foo");
        final ImmutableColumnTransformation<String> trans2 =
            new ImmutableColumnTransformation<>(TableSpecConfigTestingUtils.COL2, productionPaths[1], false, 2, "bar");
        final ImmutableColumnTransformation<String> trans3 =
            new ImmutableColumnTransformation<>(TableSpecConfigTestingUtils.COL3, productionPaths[2], false, 0, "baz");
        final List<ImmutableColumnTransformation<String>> transformations = Arrays.asList(trans1, trans2, trans3);
        final NodeSettings settings = new NodeSettings("test");
        fillSettings(settings, false, true, 2, false, ColumnFilterMode.UNION, transformations);
        stubForLoading(productionPaths);
        final TableTransformation<String> loaded = m_testInstance.load(settings);
        final TableTransformation<String> expected = new ImmutableTableTransformation<>(transformations, RAW_SPEC,
            ColumnFilterMode.UNION, 2, false, false, true);
        assertEquals(loaded, expected);
    }

    private void stubForLoading(final ProductionPath[] productionPaths) throws InvalidSettingsException {
        final ProductionPath[] remainder = Arrays.stream(productionPaths)//
                .skip(1)//
                .toArray(ProductionPath[]::new);

        when(m_productionPathSerializer.loadProductionPath(any(), eq(""))).thenReturn(productionPaths[0], remainder);
        when(m_typeSerializer.load(any())).thenReturn("X", "Y", "Z");
    }

    /**
     * Tests saving and then loading the saved settings again.
     *
     * @throws InvalidSettingsException never thrown
     */
    @Test
    public void testSaveLoad() throws InvalidSettingsException {
        final ProductionPath[] productionPaths = new ProductionPath[]{new ProductionPath(null, null),
            new ProductionPath(null, null), new ProductionPath(null, null)};
        final ImmutableColumnTransformation<String> trans1 =
            new ImmutableColumnTransformation<>(TableSpecConfigTestingUtils.COL1, productionPaths[0], false, 1, "foo");
        final ImmutableColumnTransformation<String> trans2 =
            new ImmutableColumnTransformation<>(TableSpecConfigTestingUtils.COL2, productionPaths[1], true, 2, "bar");
        final ImmutableColumnTransformation<String> trans3 =
            new ImmutableColumnTransformation<>(TableSpecConfigTestingUtils.COL3, productionPaths[2], false, 0, "baz");
        final List<ImmutableColumnTransformation<String>> transformations = Arrays.asList(trans1, trans2, trans3);
        final TableTransformation<String> saved = new ImmutableTableTransformation<>(transformations, RAW_SPEC,
            ColumnFilterMode.INTERSECTION, 1, true, true, false);

        final NodeSettings settings = new NodeSettings("test");
        m_testInstance.save(saved, settings);

        stubForLoading(productionPaths);

        final TableTransformation<String> loaded = m_testInstance.load(settings);
        assertEquals(saved, loaded);

    }
}
