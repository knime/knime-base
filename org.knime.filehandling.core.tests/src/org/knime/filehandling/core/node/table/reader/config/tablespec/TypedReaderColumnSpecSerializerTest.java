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
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Contains unit tests for the TypedReaderColumnSpecSerializer.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
@RunWith(MockitoJUnitRunner.class)
public class TypedReaderColumnSpecSerializerTest {

    @Mock
    private NodeSettingsSerializer<String> m_typeSerializer;

    private TypedReaderColumnSpecSerializer<String> m_testInstance;

    /**
     * Initializes the test instance.
     */
    @Before
    public void init() {
        m_testInstance = new TypedReaderColumnSpecSerializer<>(m_typeSerializer);
    }

    /**
     * Tests the save method.
     */
    @Test
    public void testSave() {
        final TypedReaderColumnSpec<String> spec = TypedReaderColumnSpec.createWithName("foo", "bar", true);
        final NodeSettingsWO settings = new NodeSettings("test");
        m_testInstance.save(spec, settings);
        final NodeSettings expected = new NodeSettings("test");
        fillSettings(expected, "foo", true);
        assertEquals(expected, settings);
    }

    static void fillSettings(final NodeSettingsWO settings, final String name, final boolean hasType) {
        settings.addBoolean("has_type", hasType);
        settings.addString("name", name);
        settings.addNodeSettings("type");
    }

    /**
     * Tests the load method.
     *
     * @throws InvalidSettingsException never thrown
     */
    @Test
    public void testLoad() throws InvalidSettingsException {
        final NodeSettings settings = new NodeSettings("test");
        settings.addString("name", "foo");
        settings.addBoolean("has_type", false);
        NodeSettings typeSettings = (NodeSettings)settings.addNodeSettings("type");
        when(m_typeSerializer.load(typeSettings)).thenReturn("bar");
        final TypedReaderColumnSpec<String> loaded = m_testInstance.load(settings);
        TypedReaderColumnSpec<String> expected = TypedReaderColumnSpec.createWithName("foo", "bar", false);
        assertEquals(expected, loaded);
    }

    /**
     * Tests if settings that are saved with the serializer can also be loaded by it.
     *
     * @throws InvalidSettingsException never thrown
     */
    @Test
    public void testSaveLoad() throws InvalidSettingsException {
        final TypedReaderColumnSpec<String> spec = TypedReaderColumnSpec.createWithName("foo", "bar", true);
        final NodeSettings settings = new NodeSettings("test");
        m_testInstance.save(spec, settings);
        when(m_typeSerializer.load(any())).thenReturn("bar");
        final TypedReaderColumnSpec<String> loaded = m_testInstance.load(settings);
        assertEquals(spec, loaded);

    }
}
