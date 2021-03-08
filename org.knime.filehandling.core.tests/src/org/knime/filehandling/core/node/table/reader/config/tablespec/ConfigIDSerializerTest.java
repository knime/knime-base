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

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettings;
import org.knime.filehandling.core.node.table.reader.config.tablespec.ConfigIDSerializer.EmptyConfigID;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Contains unit tests for ConfigIDSerializer.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
@RunWith(MockitoJUnitRunner.class)
public class ConfigIDSerializerTest {

    @Mock
    private ConfigIDLoader m_configIDLoader;

    @Mock
    private ConfigID m_configID;

    private ConfigIDSerializer m_testInstance;

    /**
     * Initializes the test instance.
     */
    @Before
    public void init() {
        m_testInstance = new ConfigIDSerializer(m_configIDLoader);
    }

    /**
     * Tests saving an empty config id.
     */
    @Test
    public void testSaveEmptyConfigID() {
        NodeSettings settings = new NodeSettings("test");
        ConfigIDSerializer.saveID(EmptyConfigID.INSTANCE, settings);
        NodeSettings expected = new NodeSettings("test");
        assertEquals(expected, settings);
    }

    /**
     * Tests saving a non-empty config id.
     */
    @Test
    public void testSave() {
        NodeSettings settings = new NodeSettings("test");
        ConfigIDSerializer.saveID(m_configID, settings);
        NodeSettings expected = new NodeSettings("test");
        expected.addNodeSettings("config_id");
        assertEquals(expected, settings);
    }

    /**
     * Tests loading from old setting i.e. settings that don't contain a config id.
     *
     * @throws InvalidSettingsException never thrown
     */
    @Test
    public void testLoadBackwardsCompatible() throws InvalidSettingsException {
        NodeSettings settings = new NodeSettings("test");
        ConfigID loaded = m_testInstance.loadID(settings);
        assertEquals(EmptyConfigID.INSTANCE, loaded);
    }

    /**
     * Tests loading a config id.
     *
     * @throws InvalidSettingsException never thrown
     */
    @Test
    public void testLoad() throws InvalidSettingsException {
        NodeSettings settings = new NodeSettings("test");
        settings.addNodeSettings("config_id");
        when(m_configIDLoader.createFromSettings(any())).thenReturn(m_configID);
        ConfigID loaded = m_testInstance.loadID(settings);
        assertEquals(m_configID, loaded);
    }

    /**
     * Tests that calling save on the EmptyConfigID results in an exception.
     */
    @Test (expected = IllegalStateException.class)
    public void testSavingEmptyConfigID() {
        EmptyConfigID.INSTANCE.save(new NodeSettings("test"));
    }

    /**
     * Tests the {@link ConfigID#isCompatible(ConfigID)} implementation of EmptyConfigID.
     */
    @Test
    public void testEmptyConfigIDIsCompatible() {
        assertEquals(false, EmptyConfigID.INSTANCE.isCompatible(null));
        assertEquals(true, EmptyConfigID.INSTANCE.isCompatible(EmptyConfigID.INSTANCE));
        assertEquals(true, EmptyConfigID.INSTANCE.isCompatible(Mockito.mock(ConfigID.class)));
    }
}
