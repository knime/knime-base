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
 *   Feb 8, 2021 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.node.table.reader.config.tablespec;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.knime.core.data.convert.map.ProductionPath;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsWO;
import org.knime.filehandling.core.node.table.reader.ImmutableColumnTransformation;
import org.knime.filehandling.core.node.table.reader.selector.ColumnTransformation;
import org.knime.filehandling.core.node.table.reader.spec.TypedReaderColumnSpec;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Contains unit tests for ColumnTransformationSerializer.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
@RunWith(MockitoJUnitRunner.class)
public class ColumnTransformationSerializerTest {

    @Mock
    private ProductionPathSerializer m_productionPathSerializer;

    @Mock
    private NodeSettingsSerializer<String> m_typeSerializer;

    private ColumnTransformationSerializer<String> m_testInstance;

    /**
     * Initializes the test instance.
     */
    @Before
    public void init() {
        final TypedReaderColumnSpecSerializer<String> columnSpecSerializer =
            new TypedReaderColumnSpecSerializer<>(m_typeSerializer);
        m_testInstance = new ColumnTransformationSerializer<>(columnSpecSerializer, m_productionPathSerializer);
    }

    /**
     * Tests the save method.
     */
    @Test
    public void testSave() {
        final TypedReaderColumnSpec<String> externalSpec = TypedReaderColumnSpec.createWithName("foo", "X", true);
        final ProductionPath pp = new ProductionPath(null, null);
        final ColumnTransformation<String> transformation =
            new ImmutableColumnTransformation<>(externalSpec, pp, true, 3, "bar");

        final NodeSettings settings = new NodeSettings("test");
        m_testInstance.save(transformation, settings);

        final NodeSettings expected = new NodeSettings("test");
        fillSettings(expected, true, 3, "bar", "foo", true);
        TypedReaderColumnSpecSerializerTest.fillSettings(expected.addNodeSettings("external_spec"), "foo", true);
        expected.addBoolean("keep", true);
        expected.addInt("position", 3);
        expected.addString("name", "bar");
        expected.addNodeSettings("production_path");
        assertEquals(expected, settings);
        verify(m_productionPathSerializer).saveProductionPath(eq(pp), any(), eq(""));
    }

    static void fillSettings(final NodeSettingsWO settings, final boolean keep, final int position, final String name,
        final String originalName, final boolean hasType) {
        TypedReaderColumnSpecSerializerTest.fillSettings(settings.addNodeSettings("external_spec"), originalName,
            hasType);
        settings.addBoolean("keep", keep);
        settings.addInt("position", position);
        settings.addString("name", name);
        settings.addNodeSettings("production_path");
    }

    /**
     * Tests the load method.
     *
     * @throws InvalidSettingsException never thrown
     */
    @Test
    public void testLoad() throws InvalidSettingsException {
        final NodeSettings settings = new NodeSettings("test");
        fillSettings(settings, false, 1, "baz", "foo", true);
        final ProductionPath pp = new ProductionPath(null, null);
        when(m_productionPathSerializer.loadProductionPath(any(), any())).thenReturn(pp);
        when(m_typeSerializer.load(any())).thenReturn("X");
        final ColumnTransformation<String> loaded = m_testInstance.load(settings);
        TypedReaderColumnSpec<String> externalSpec = TypedReaderColumnSpec.createWithName("foo", "X", true);
        final ColumnTransformation<String> expected =
            new ImmutableColumnTransformation<>(externalSpec, pp, false, 1, "baz");
        assertEquals(expected, loaded);
    }

    /**
     * Tests saving and then loading the saved settings again.
     *
     * @throws InvalidSettingsException never thrown
     */
    public void testSaveLoad() throws InvalidSettingsException {
        final TypedReaderColumnSpec<String> externalSpec = TypedReaderColumnSpec.createWithName("foo", "X", true);
        final ProductionPath pp = new ProductionPath(null, null);
        final ColumnTransformation<String> saved =
            new ImmutableColumnTransformation<>(externalSpec, pp, true, 3, "bar");
        verify(m_productionPathSerializer).saveProductionPath(eq(pp), any(), eq(""));

        final NodeSettings settings = new NodeSettings("test");
        m_testInstance.save(saved, settings);
        when(m_productionPathSerializer.loadProductionPath(any(), any())).thenReturn(pp);
        when(m_typeSerializer.load(any())).thenReturn("X");
        final ColumnTransformation<String> loaded = m_testInstance.load(settings);
        assertEquals(loaded, saved);


    }

}
