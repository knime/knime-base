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
 *   Feb 11, 2021 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.node.table.reader.config.tablespec;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;

import java.util.EnumMap;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettings;
import org.knime.filehandling.core.node.table.reader.config.tablespec.TableSpecConfigSerializer.AdditionalParameters;
import org.knime.filehandling.core.node.table.reader.config.tablespec.TableSpecConfigSerializer.TableSpecConfigSerializerVersion;
import org.knime.filehandling.core.node.table.reader.selector.ColumnFilterMode;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Contains unit tests for VersionedTableSpecConfigSerializer.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
@RunWith(MockitoJUnitRunner.class)
public class VersionedTableSpecConfigSerializerTest {

    @Mock
    private TableSpecConfigSerializer<String> m_v42Serializer;

    @Mock
    private TableSpecConfigSerializer<String> m_v43Serializer;

    @Mock
    private TableSpecConfigSerializer<String> m_v44Serializer;

    private VersionedTableSpecConfigSerializer<String> m_testInstance;

    /**
     * Initializes the test instance.
     */
    @Before
    public void init() {
        EnumMap<TableSpecConfigSerializerVersion, TableSpecConfigSerializer<String>> serializers =
            new EnumMap<>(TableSpecConfigSerializerVersion.class);
        serializers.put(TableSpecConfigSerializerVersion.V4_2, m_v42Serializer);
        serializers.put(TableSpecConfigSerializerVersion.V4_3, m_v43Serializer);
        serializers.put(TableSpecConfigSerializerVersion.V4_4, m_v44Serializer);
        m_testInstance = new VersionedTableSpecConfigSerializer<>(serializers, TableSpecConfigSerializerVersion.V4_4);
    }

    /**
     * Tests if an {@link IllegalStateException} is thrown when loading settings with an unsupported version.
     *
     * @throws InvalidSettingsException thrown because the settings seem to be 4.2
     */
    @Test(expected = InvalidSettingsException.class)
    public void testLoadUnsupportedVersion() throws InvalidSettingsException {
        EnumMap<TableSpecConfigSerializerVersion, TableSpecConfigSerializer<String>> serializers =
            new EnumMap<>(TableSpecConfigSerializerVersion.class);
        serializers.put(TableSpecConfigSerializerVersion.V4_3, m_v43Serializer);
        serializers.put(TableSpecConfigSerializerVersion.V4_4, m_v44Serializer);
        final VersionedTableSpecConfigSerializer<String> testInstance =
            new VersionedTableSpecConfigSerializer<>(serializers, TableSpecConfigSerializerVersion.V4_4);
        NodeSettings settings = new NodeSettings("test");
        testInstance.load(settings);
    }

    /**
     * Tests loading settings that appear to be from 4.2.
     *
     * @throws InvalidSettingsException never thrown
     */
    @Test
    public void testLoad42() throws InvalidSettingsException {
        NodeSettings settings = new NodeSettings("test");
        final AdditionalParameters params = TableSpecConfigSerializer.AdditionalParameters.create()//
            .withColumnFilterMode(ColumnFilterMode.INTERSECTION);
        m_testInstance.load(settings, params);
        verify(m_v42Serializer).load(settings, params);
    }

    /**
     * Tests loading settings that appear to be from 4.3.
     *
     * @throws InvalidSettingsException never thrown
     */
    @Test
    public void testLoad43() throws InvalidSettingsException {
        NodeSettings settings = new NodeSettings("test");
        settings.addBoolean("include_unknown_columns_Internals", true);
        m_testInstance.load(settings);
        verify(m_v43Serializer).load(settings);
    }

    /**
     * Tests loading settings that appear to be from 4.4.
     *
     * @throws InvalidSettingsException never thrown
     */
    @Test
    public void testLoad44() throws InvalidSettingsException {
        NodeSettings settings = new NodeSettings("test");
        settings.addString("version", TableSpecConfigSerializerVersion.V4_4.name());
        m_testInstance.load(settings);
        verify(m_v44Serializer).load(settings);
    }

    /**
     * Tests if settings are saved with the latest version i.e. atm 4.4.
     *
     * @throws InvalidSettingsException never thrown
     */
    @Test
    public void testSave44() throws InvalidSettingsException {
        @SuppressWarnings("unchecked")
        TableSpecConfig<String> config = Mockito.mock(TableSpecConfig.class);
        final NodeSettings settings = new NodeSettings("test");
        m_testInstance.save(config, settings);
        assertEquals("V4_4", settings.getString("version"));
        verify(m_v44Serializer).save(config, settings);
    }
}
