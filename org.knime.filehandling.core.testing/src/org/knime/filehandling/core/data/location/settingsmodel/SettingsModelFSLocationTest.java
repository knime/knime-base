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
 *   Mar 16, 2020 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.data.location.settingsmodel;

import static org.junit.Assert.*;
import static org.knime.filehandling.core.connections.FSLocation.NULL;
import static org.knime.filehandling.core.data.location.internal.FSLocationUtils.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import javax.swing.event.ChangeListener;

import org.junit.Before;
import org.junit.Test;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.filehandling.core.connections.FSLocation;
import org.mockito.Mockito;

/**
 * Unit tests for {@link SettingsModelFSLocation}.
 * 
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
public final class SettingsModelFSLocationTest {

	private interface Saver {
		void accept(SettingsModelFSLocation model, NodeSettingsWO settings) throws InvalidSettingsException;
	}

	private static final FSLocation DEFAULT_LOCATION = mockLocation(true);

	private static final String CFG_NAME = "test";

	private static final String MOCK_FS_TYPE = "berta";

	private static final String MOCK_FS_SPECIFIER = "hans";

	private static final String MOCK_PATH = "guenther";

	private static FSLocation mockLocation(final boolean includeSpecifier) {
		return new FSLocation(MOCK_FS_TYPE, includeSpecifier ? MOCK_FS_SPECIFIER : null, MOCK_PATH);
	}

	private SettingsModelFSLocation m_testInstance;

	@Before
	public void init() {
		m_testInstance = new SettingsModelFSLocation(CFG_NAME, DEFAULT_LOCATION);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testConstructorFailsOnNullName() {
		new SettingsModelFSLocation(null, DEFAULT_LOCATION);
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void testConstructorFailsOnNullLocation() throws Exception {
		new SettingsModelFSLocation("name", null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testConstructorFailsOnEmptyName() {
		new SettingsModelFSLocation("", DEFAULT_LOCATION);
	}

	@Test
	public void testGetSet() {
		ChangeListener mockListener = mock(ChangeListener.class);
		m_testInstance.addChangeListener(mockListener);
		assertEquals(DEFAULT_LOCATION, m_testInstance.getLocation());
		m_testInstance.setLocation(NULL);
		assertEquals(NULL, m_testInstance.getLocation());
		m_testInstance.setLocation(NULL);
		verify(mockListener, Mockito.times(1)).stateChanged(any());
	}

	@Test
	public void testCreateClone() {
		final SettingsModelFSLocation clone = m_testInstance.createClone();
		assertEquals(m_testInstance.getLocation(), clone.getLocation());
		assertEquals(m_testInstance.getConfigName(), clone.getConfigName());
		assertFalse(m_testInstance == clone);
	}

	@Test
	public void testGetModelTypeID() {
		assertEquals("SMID_FSLocation", m_testInstance.getModelTypeID());
	}

	@Test
	public void testGetConfigName() {
		assertEquals(CFG_NAME, m_testInstance.getConfigName());
	}

	@Test
	public void testLoadSettingsForDialogSuccess() throws NotConfigurableException, InvalidSettingsException {
		// full location
		NodeSettingsRO settings = mockNodeSettingsRO(true, true);
		m_testInstance.loadSettingsForDialog(settings, null);
		assertEquals(new FSLocation("foo", "bar", "bla"), m_testInstance.getLocation());
		// no specifier
		settings = mockNodeSettingsRO(true, false);
		m_testInstance.loadSettingsForDialog(settings, null);
		assertEquals(new FSLocation("foo", "bla"), m_testInstance.getLocation());
		// no location
		settings = mockNodeSettingsRO(false, false);
		m_testInstance.loadSettingsForDialog(settings, null);
		assertEquals(NULL, m_testInstance.getLocation());
	}

	private static NodeSettingsRO mockNodeSettingsRO(final boolean present, final boolean includeSpecifier)
			throws InvalidSettingsException {
		NodeSettingsRO settings = mock(NodeSettingsRO.class);
		NodeSettingsRO locationSettings = mock(NodeSettingsRO.class);
		when(settings.getNodeSettings(CFG_NAME)).thenReturn(locationSettings);
		when(locationSettings.getBoolean(CFG_LOCATION_PRESENT)).thenReturn(present);
		if (present) {
			when(locationSettings.getString(CFG_FS_TYPE)).thenReturn("foo");
			if (includeSpecifier) {
				when(locationSettings.getString(CFG_FS_SPECIFIER, null))
						.thenReturn("bar");
			}
			when(locationSettings.getString(CFG_PATH)).thenReturn("bla");
		}
		return settings;
	}

	@Test
	public void testLoadSettingsForDialogFailure() throws NotConfigurableException {
		NodeSettingsRO settings = new NodeSettings("test");
		m_testInstance.loadSettingsForDialog(settings, null);
		// should be the default
		assertEquals(DEFAULT_LOCATION, m_testInstance.getLocation());
	}

	@Test
	public void testSaveSettingsForDialog() throws InvalidSettingsException {
		testSave((m, s) -> m.saveSettingsForDialog(s));
	}

	@Test
	public void testValidateSettingsForModelSuccess() throws InvalidSettingsException {
		m_testInstance.validateSettingsForModel(mockNodeSettingsRO(true, true));
		m_testInstance.validateSettingsForModel(mockNodeSettingsRO(true, false));
		m_testInstance.validateSettingsForModel(mockNodeSettingsRO(false, false));
	}

	@Test(expected = InvalidSettingsException.class)
	public void testValidateSettingsForModel() throws InvalidSettingsException {
		m_testInstance.validateSettingsForModel(new NodeSettings("test"));
	}

	@Test
	public void testLoadSettingsForModelSuccess() throws InvalidSettingsException {
		// full location
		NodeSettingsRO settings = mockNodeSettingsRO(true, true);
		m_testInstance.loadSettingsForModel(settings);
		assertEquals(new FSLocation("foo", "bar", "bla"), m_testInstance.getLocation());
		// no specifier
		settings = mockNodeSettingsRO(true, false);
		m_testInstance.loadSettingsForModel(settings);
		assertEquals(new FSLocation("foo", "bla"), m_testInstance.getLocation());
		// no location
		settings = mockNodeSettingsRO(false, false);
		m_testInstance.loadSettingsForModel(settings);
		assertEquals(NULL, m_testInstance.getLocation());
	}

	@Test(expected = InvalidSettingsException.class)
	public void testLoadSettingsForModelFailure() throws InvalidSettingsException {
		final NodeSettingsRO settings = new NodeSettings("test");
		m_testInstance.loadSettingsForModel(settings);
	}

	@Test
	public void testSaveSettingsForModel() throws InvalidSettingsException {
		testSave((m, s) -> m.saveSettingsForModel(s));
	}

	private void testSave(final Saver saveConsumer) throws InvalidSettingsException {
		testSaveFull(saveConsumer);
		testSaveNoSpecifier(saveConsumer);
		testSaveNoLocation(saveConsumer);
	}

	private void testSaveFull(Saver saveConsumer) throws InvalidSettingsException {
		NodeSettingsWO settings = mock(NodeSettingsWO.class);
		NodeSettingsWO locationSettings = mock(NodeSettingsWO.class);
		when(settings.addNodeSettings(CFG_NAME)).thenReturn(locationSettings);
		saveConsumer.accept(m_testInstance, settings);
		verify(locationSettings).addBoolean(CFG_LOCATION_PRESENT, true);
		verify(locationSettings).addString(CFG_FS_TYPE, MOCK_FS_TYPE);
		verify(locationSettings).addString(CFG_FS_SPECIFIER, MOCK_FS_SPECIFIER);
		verify(locationSettings).addString(CFG_PATH, MOCK_PATH);
	}

	private void testSaveNoSpecifier(Saver saveConsumer) throws InvalidSettingsException {
		NodeSettingsWO settings = mock(NodeSettingsWO.class);
		NodeSettingsWO locationSettings = mock(NodeSettingsWO.class);
		when(settings.addNodeSettings(CFG_NAME)).thenReturn(locationSettings);
		m_testInstance.setLocation(mockLocation(false));
		saveConsumer.accept(m_testInstance, settings);
		verify(locationSettings).addBoolean(CFG_LOCATION_PRESENT, true);
		verify(locationSettings).addString(CFG_FS_TYPE, MOCK_FS_TYPE);
		verify(locationSettings, never()).addString(eq(CFG_FS_SPECIFIER), anyString());
		verify(locationSettings).addString(CFG_PATH, MOCK_PATH);
	}

	private void testSaveNoLocation(Saver saveConsumer) throws InvalidSettingsException {
		NodeSettingsWO settings = mock(NodeSettingsWO.class);
		NodeSettingsWO locationSettings = mock(NodeSettingsWO.class);
		when(settings.addNodeSettings(CFG_NAME)).thenReturn(locationSettings);
		m_testInstance.setLocation(NULL);
		saveConsumer.accept(m_testInstance, settings);
		verify(locationSettings).addBoolean(CFG_LOCATION_PRESENT, false);
		verifyNoMoreInteractions(locationSettings);
	}

	@Test
	public void testToString() {
		assertEquals("SettingsModelFSLocation (" + CFG_NAME + ")", m_testInstance.toString());
	}
}
