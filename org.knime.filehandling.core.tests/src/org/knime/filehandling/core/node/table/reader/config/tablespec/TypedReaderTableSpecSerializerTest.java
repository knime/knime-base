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
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsWO;
import org.knime.filehandling.core.node.table.reader.spec.TypedReaderColumnSpec;
import org.knime.filehandling.core.node.table.reader.spec.TypedReaderTableSpec;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Contains unit tests for the TypedReaderTableSpecSerializer.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
@RunWith(MockitoJUnitRunner.class)
public class TypedReaderTableSpecSerializerTest {

    @Mock
    private NodeSettingsSerializer<String> m_typeSerializer;

    private TypedReaderTableSpecSerializer<String> m_testInstance;

    /**
     * Initializes the test instance.
     */
    @Before
    public void init() {
        final TypedReaderColumnSpecSerializer<String> columnSerializer =
            new TypedReaderColumnSpecSerializer<>(m_typeSerializer);
        m_testInstance = new TypedReaderTableSpecSerializer<>(columnSerializer);
    }

    /**
     * Tests the save method.
     */
    @Test
    public void testSave() {
        final TypedReaderTableSpec<String> spec = TypedReaderTableSpec.<String> builder()//
            .addColumn("foo", "X", true)//
            .addColumn("bar", "Y", false)//
            .addColumn("baz", "Z", true)//
            .build();

        final NodeSettings settings = new NodeSettings("test");
        m_testInstance.save(spec, settings);

        final NodeSettings expected = new NodeSettings("test");
        expected.addInt("num_columns", 3);
        addColumn(expected, 0, "foo", true);
        addColumn(expected, 1, "bar", false);
        addColumn(expected, 2, "baz", true);
        assertEquals(expected, settings);
    }

    static void fillSettings(final NodeSettingsWO settings, final String[] name, final Boolean[] hasTypes) {
        settings.addInt("num_columns", name.length);
        for (int i = 0; i < name.length; i++) {
            addColumn(settings, i, name[i], hasTypes[i]);
        }
    }

    static void fillSettings(final NodeSettingsWO settings, final TypedReaderTableSpec<String> spec) {
        settings.addInt("num_columns", spec.size());
        for (int i = 0; i < spec.size(); i++) {
            final TypedReaderColumnSpec<String> col = spec.getColumnSpec(i);
            addColumn(settings, i, col.getName().orElseThrow(IllegalStateException::new), col.hasType());
        }
    }

    private static void addColumn(final NodeSettingsWO settings, final int idx, final String name,
        final boolean hasType) {
        final NodeSettingsWO columnSettings = settings.addNodeSettings("" + idx);
        columnSettings.addString("name", name);
        columnSettings.addBoolean("has_type", hasType);
        // the type serialization is mocked and not the responsibility of this serializer
        columnSettings.addNodeSettings("type");
    }

    /**
     * Tests the load method.
     *
     * @throws InvalidSettingsException never thrown
     */
    @Test
    public void testLoad() throws InvalidSettingsException {
        final NodeSettings settings = new NodeSettings("test");
        settings.addInt("num_columns", 3);
        addColumn(settings, 0, "foo", true);
        addColumn(settings, 1, "bar", false);
        addColumn(settings, 2, "baz", true);

        when(m_typeSerializer.load(any())).thenReturn("X", "Y", "Z");

        final TypedReaderTableSpec<String> loaded = m_testInstance.load(settings);

        final TypedReaderTableSpec<String> expected =
            TypedReaderTableSpec.<String> builder().addColumn("foo", "X", true)//
                .addColumn("bar", "Y", false)//
                .addColumn("baz", "Z", true)//
                .build();
        assertEquals(expected, loaded);
    }

    /**
     * Tests saving and then loading.
     *
     * @throws InvalidSettingsException never thrown
     */
    @Test
    public void testSaveLoad() throws InvalidSettingsException {
        final TypedReaderTableSpec<String> saved = TypedReaderTableSpec.<String> builder()//
            .addColumn("foo", "X", true)//
            .addColumn("bar", "Y", false)//
            .addColumn("baz", "Z", true)//
            .build();

        final NodeSettings settings = new NodeSettings("test");
        m_testInstance.save(saved, settings);

        when(m_typeSerializer.load(any())).thenReturn("X", "Y", "Z");

        final TypedReaderTableSpec<String> loaded = m_testInstance.load(settings);

        assertEquals(saved, loaded);
    }
}
