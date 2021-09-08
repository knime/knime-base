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
 *   Mar 18, 2020 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.data.location.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.knime.filehandling.core.connections.FSLocation.NULL;
import static org.knime.filehandling.core.data.location.internal.FSLocationUtils.CFG_FS_SPECIFIER;
import static org.knime.filehandling.core.data.location.internal.FSLocationUtils.CFG_FS_CATEGORY;
import static org.knime.filehandling.core.data.location.internal.FSLocationUtils.CFG_LOCATION_PRESENT;
import static org.knime.filehandling.core.data.location.internal.FSLocationUtils.CFG_PATH;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.config.ConfigRO;
import org.knime.core.node.config.ConfigWO;
import org.knime.filehandling.core.connections.FSCategory;
import org.knime.filehandling.core.connections.FSLocation;
import org.knime.filehandling.core.connections.RelativeTo;

/**
 * Unit tests for {@link FSLocationUtils}.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
public class FSLocationUtilsTest {

    public static final String FS_CATEGORY = FSCategory.RELATIVE.toString();

    public static final String FS_SPECIFIER = RelativeTo.WORKFLOW_DATA.getSettingsValue();

    public static final String PATH = "bla";

    public static final FSLocation LOCATION_WITHOUT_SPECIFIER = new FSLocation(FS_CATEGORY, PATH);

    public static final FSLocation LOCATION_WITH_SPECIFIER = new FSLocation(FS_CATEGORY, FS_SPECIFIER, PATH);

    private static ConfigRO mockConfigRO(final boolean present, final boolean includeSpecifier, final boolean mockChildCount)
        throws InvalidSettingsException {
        ConfigRO config = mock(ConfigRO.class);
        when(config.getBoolean(CFG_LOCATION_PRESENT)).thenReturn(present);
        if (present) {
            when(config.getString(CFG_FS_CATEGORY)).thenReturn(FS_CATEGORY);
            if (includeSpecifier) {
                when(config.getString(CFG_FS_SPECIFIER, null)).thenReturn(FS_SPECIFIER);
            }
            when(config.getString(CFG_PATH)).thenReturn(PATH);
        }
        if (mockChildCount) {
            int childCount = includeSpecifier ? 4 : 3;
            when(config.getChildCount()).thenReturn(present ? childCount : 1);
        }
        return config;
    }

    @Test
    public void testIsFSLocation() throws InvalidSettingsException {
        testIsLocationNoLocationPresent();
        testIsLocationWithSpecifier();
        testIsLocationWithoutSpecifier();
        testIsLocationInvalid();
    }

    private static void testIsLocationInvalid() throws InvalidSettingsException {
        ConfigRO config = mock(ConfigRO.class);
        when(config.getBoolean(CFG_LOCATION_PRESENT)).thenThrow(new InvalidSettingsException(""));
        assertFalse(FSLocationUtils.isFSLocation(config));
    }

    private static void testIsLocationWithoutSpecifier() throws InvalidSettingsException {
        ConfigRO config = mockConfigRO(true, false, false);
        when(config.getChildCount()).thenReturn(3, 7);
        assertTrue(FSLocationUtils.isFSLocation(config));
        assertFalse(FSLocationUtils.isFSLocation(config));
    }

    private static void testIsLocationWithSpecifier() throws InvalidSettingsException {
        ConfigRO config = mockConfigRO(true, true, false);
        when(config.getChildCount()).thenReturn(4, 7);
        assertTrue(FSLocationUtils.isFSLocation(config));
        assertFalse(FSLocationUtils.isFSLocation(config));
    }

    private static void testIsLocationNoLocationPresent() throws InvalidSettingsException {
        ConfigRO config = mock(ConfigRO.class);
        when(config.getBoolean(CFG_LOCATION_PRESENT)).thenReturn(false);
        when(config.getChildCount()).thenReturn(1, 2);
        assertTrue(FSLocationUtils.isFSLocation(config));
        assertFalse(FSLocationUtils.isFSLocation(config));
    }

    @Test
    public void testLoad() throws InvalidSettingsException {
        testLoadPresent();
        testLoadAbsent();
    }

    private static void testLoadPresent() throws InvalidSettingsException {
        ConfigRO config = mockConfigRO(true, true, false);
        assertEquals(LOCATION_WITH_SPECIFIER, FSLocationUtils.loadFSLocation(config));
    }

    private static void testLoadAbsent() throws InvalidSettingsException {
        ConfigRO config = mockConfigRO(false, true, false);
        assertEquals(NULL, FSLocationUtils.loadFSLocation(config));
    }

    @Test
    public void testSave() {
        testSaveWithSpecifier();
        testSaveWithoutSpecifier();
        testSaveNoLocation();
    }

    private static void testSaveWithSpecifier() {
        ConfigWO config = mock(ConfigWO.class);
        FSLocationUtils.saveFSLocation(LOCATION_WITH_SPECIFIER, config);
        verify(config).addBoolean(CFG_LOCATION_PRESENT, true);
        verify(config).addString(CFG_FS_CATEGORY, FS_CATEGORY);
        verify(config).addString(CFG_FS_SPECIFIER, FS_SPECIFIER);
        verify(config).addString(CFG_PATH, PATH);
        verifyNoMoreInteractions(config);
    }

    private static void testSaveWithoutSpecifier() {
        ConfigWO config = mock(ConfigWO.class);
        FSLocationUtils.saveFSLocation(LOCATION_WITHOUT_SPECIFIER, config);
        verify(config).addBoolean(CFG_LOCATION_PRESENT, true);
        verify(config).addString(CFG_FS_CATEGORY, FS_CATEGORY);
        verify(config).addString(CFG_PATH, PATH);
        verifyNoMoreInteractions(config);
    }

    private static void testSaveNoLocation() {
        ConfigWO config = mock(ConfigWO.class);
        FSLocationUtils.saveFSLocation(NULL, config);
        verify(config).addBoolean(CFG_LOCATION_PRESENT, false);
        verifyNoMoreInteractions(config);
    }
}
