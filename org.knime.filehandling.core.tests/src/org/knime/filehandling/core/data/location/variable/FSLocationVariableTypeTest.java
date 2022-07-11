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
 *   Feb 27, 2020 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.data.location.variable;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.knime.filehandling.core.connections.FSLocation.NULL;
import static org.knime.filehandling.core.data.location.internal.FSLocationSerializationUtils.CFG_FS_SPECIFIER;
import static org.knime.filehandling.core.data.location.internal.FSLocationSerializationUtils.CFG_FS_CATEGORY;
import static org.knime.filehandling.core.data.location.internal.FSLocationSerializationUtils.CFG_LOCATION_PRESENT;
import static org.knime.filehandling.core.data.location.internal.FSLocationSerializationUtils.CFG_PATH;
import static org.knime.filehandling.core.data.location.internal.FSLocationSerializationUtilsTest.FS_SPECIFIER;
import static org.knime.filehandling.core.data.location.internal.FSLocationSerializationUtilsTest.FS_CATEGORY;
import static org.knime.filehandling.core.data.location.internal.FSLocationSerializationUtilsTest.LOCATION_WITH_SPECIFIER;
import static org.knime.filehandling.core.data.location.internal.FSLocationSerializationUtilsTest.PATH;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.Test;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.config.Config;
import org.knime.core.node.workflow.FlowVariable;
import org.knime.core.node.workflow.VariableType.InvalidConfigEntryException;
import org.knime.filehandling.core.connections.FSCategory;
import org.knime.filehandling.core.connections.FSLocation;
import org.knime.filehandling.core.connections.RelativeTo;

/**
 * Unit tests for {@link FSLocationVariableType}.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
public class FSLocationVariableTypeTest {

    private static final String KEY = "test";

    private static final FSLocationVariableType TEST_INSTANCE = FSLocationVariableType.INSTANCE;

    @Test
    public void testGetSimpleType() {
        assertEquals(FSLocation.class, TEST_INSTANCE.getSimpleType());
    }

    private static FSLocation get(final FlowVariable flowVar) {
        return flowVar.getValue(FSLocationVariableType.INSTANCE);
    }

    @Test
    public void testloadValue() throws InvalidSettingsException {
        NodeSettings settings = new NodeSettings("Test");
        FSLocation location = new FSLocation(FSCategory.RELATIVE, //
            RelativeTo.WORKFLOW_DATA.getSettingsValue(), //
            "baz");
        FlowVariable before = new FlowVariable("var", TEST_INSTANCE, location);
        before.save(settings);
        FlowVariable after = FlowVariable.load(settings);
        assertEquals(location, get(after));
    }

    @Test
    public void testSaveValue() {
        NodeSettingsWO settings = mock(NodeSettingsWO.class);
        TEST_INSTANCE.saveValue(settings, TEST_INSTANCE.newValue(LOCATION_WITH_SPECIFIER));
        verify(settings).addBoolean(CFG_LOCATION_PRESENT, true);
        verify(settings).addString(CFG_FS_CATEGORY, FS_CATEGORY);
        verify(settings).addString(CFG_FS_SPECIFIER, FS_SPECIFIER);
        verify(settings).addString(CFG_PATH, PATH);
    }

    @Test
    public void testNewValue() {
        FlowVariable flowVar = new FlowVariable("var", TEST_INSTANCE, LOCATION_WITH_SPECIFIER);
        assertEquals(LOCATION_WITH_SPECIFIER, get(flowVar));
    }

    @Test
    public void testGetIdentifier() {
        assertEquals("FSLocation", TEST_INSTANCE.getIdentifier());
    }

    @Test
    public void testDefaultValue() {
        assertEquals(NULL, get(new FlowVariable("var", TEST_INSTANCE)));
    }

    @Test
    public void testCanOverwrite() {
        Config config = mockEmptyConfig();
        assertTrue(TEST_INSTANCE.canOverwrite(config, KEY));
    }

    @Test
    public void testOverwriteSuccess() throws InvalidSettingsException, InvalidConfigEntryException {
        Config config = mockEmptyConfig();
        TEST_INSTANCE.overwrite(LOCATION_WITH_SPECIFIER, config, KEY);
        Config locationConfig = config.getConfig(KEY);
        assertTrue(locationConfig.getBoolean(CFG_LOCATION_PRESENT));
        assertEquals(FS_CATEGORY, locationConfig.getString(CFG_FS_CATEGORY));
        assertEquals(FS_SPECIFIER, locationConfig.getString(CFG_FS_SPECIFIER));
        assertEquals(PATH, locationConfig.getString(CFG_PATH));
    }

    @Test(expected = InvalidConfigEntryException.class)
    public void testOverwriteFailure() throws InvalidConfigEntryException {
        Config config = new NodeSettings(KEY);
        TEST_INSTANCE.overwrite(LOCATION_WITH_SPECIFIER, config, KEY);
    }

    @Test
    public void testCanCreateFrom() {
        Config config = mockEmptyConfig();
        assertTrue(TEST_INSTANCE.canCreateFrom(config, KEY));
    }

    private static Config mockEmptyConfig() {
        Config config = new NodeSettings(KEY);
        Config locationConfig = config.addConfig(KEY);
        locationConfig.addBoolean(CFG_LOCATION_PRESENT, false);
        return config;
    }

    @Test
    public void testCreateFromSuccess() throws InvalidSettingsException, InvalidConfigEntryException {
        Config config = mockEmptyConfig();
        assertEquals(NULL, TEST_INSTANCE.createFrom(config, KEY));
    }

    @Test(expected = InvalidConfigEntryException.class)
    public void testCreateFromFailure() throws InvalidSettingsException, InvalidConfigEntryException {
        Config config = new NodeSettings(KEY);
        TEST_INSTANCE.createFrom(config, KEY);
    }

}
