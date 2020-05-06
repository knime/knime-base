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
 */
package org.knime.filehandling.core.converter;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;
import org.knime.filehandling.core.defaultnodesettings.FileChooserSettingsConverter;
import org.knime.filehandling.core.defaultnodesettings.FileSystemChoice;
import org.knime.filehandling.core.defaultnodesettings.KNIMEConnection;
import org.knime.filehandling.core.defaultnodesettings.SettingsModelFileChooser2;

/**
 * Junit test for testing the settings conversion for legacy path or url strings.
 *
 * @author Mareike Hoeger, KNIME GmbH, Konstanz, Germany
 *
 */
public class FileChooserSettingsConverterTest {

    private SettingsModelFileChooser2 m_config;

    private static final String PATH_STRING = "/test/path";

    /**
     * Creates a new clean configuration before each test.
     */
    @Before
    public void init() {
        m_config = new SettingsModelFileChooser2("testConfig");
    }

    /**
     * Tests a local path input.
     */
    @Test
    public void localPathConfigTest() {
        m_config.setPathOrURL(PATH_STRING);
        FileChooserSettingsConverter.convert(m_config);
        assertEquals(m_config.getFileSystemChoice(), FileSystemChoice.getLocalFsChoice());
        assertEquals(m_config.getPathOrURL(), PATH_STRING);
    }

    /**
     * Tests a local path input.
     */
    @Test
    public void localPathWindowsStyleConfigTest() {
        final String windowsPath = "C:" + PATH_STRING;
        m_config.setPathOrURL(windowsPath);
        FileChooserSettingsConverter.convert(m_config);
        assertEquals(m_config.getFileSystemChoice(), FileSystemChoice.getLocalFsChoice());
        assertEquals(m_config.getPathOrURL(), windowsPath);
    }

    /**
     * Tests a local path with file:// input.
     */
    @Test
    public void localPathWithProtocolConfigTest() {
        final String fileURL = "file://" + PATH_STRING;
        m_config.setPathOrURL(fileURL);
        FileChooserSettingsConverter.convert(m_config);
        assertEquals(m_config.getFileSystemChoice(), FileSystemChoice.getCustomFsUrlChoice());
        assertEquals(m_config.getPathOrURL(), fileURL);
    }

    /**
     * Tests a local path with file:/ input.
     */
    @Test
    public void localPathWithProtocolOneSlashConfigTest() {
        final String fielURL = "file:/" + PATH_STRING;
        m_config.setPathOrURL(fielURL);
        FileChooserSettingsConverter.convert(m_config);
        assertEquals(m_config.getFileSystemChoice(), FileSystemChoice.getCustomFsUrlChoice());
        assertEquals(m_config.getPathOrURL(), fielURL);
    }

    /**
     * Tests a custom URL with a protocol that has no handler registered.
     */
    @Test
    public void customURLWithoutHandlerConfigTest() {
        m_config.setPathOrURL("customURL:/" + PATH_STRING);
        FileChooserSettingsConverter.convert(m_config);
        assertEquals(FileSystemChoice.getCustomFsUrlChoice(), m_config.getFileSystemChoice());
        assertEquals("customURL:/" + PATH_STRING, m_config.getPathOrURL());
    }

    /**
     * Tests a custom URL with a protocol.
     */
    @Test
    public void customURLConfigTest() {
        m_config.setPathOrURL("http:/" + PATH_STRING);
        FileChooserSettingsConverter.convert(m_config);
        assertEquals(FileSystemChoice.getCustomFsUrlChoice(), m_config.getFileSystemChoice());
        assertEquals("http:/" + PATH_STRING, m_config.getPathOrURL());
    }

    /**
     * Tests a workspace relative URL input.
     */
    @Test
    public void knimeWorkspaceConfigTest() {
        final String workflowURL = "knime://knime.workflow" + PATH_STRING;
        m_config.setPathOrURL(workflowURL);
        FileChooserSettingsConverter.convert(m_config);
        assertEquals(m_config.getFileSystemChoice(), FileSystemChoice.getCustomFsUrlChoice());
        assertEquals(m_config.getPathOrURL(), workflowURL);
    }

    /**
     * Tests a node relative URL input.
     */
    @Test
    public void knimeNodeConfigTest() {
        final String nodeURL = "knime://knime.node" + PATH_STRING;
        m_config.setPathOrURL(nodeURL);
        FileChooserSettingsConverter.convert(m_config);
        assertEquals(m_config.getFileSystemChoice(), FileSystemChoice.getCustomFsUrlChoice());
        assertEquals(m_config.getPathOrURL(), nodeURL);
    }

    /**
     * Tests a mountpoint relative URL input.
     */
    @Test
    public void knimeMountPointConfigTest() {
        final String mountpointURL = "knime://knime.mountpoint" + PATH_STRING;
        m_config.setPathOrURL(mountpointURL);
        FileChooserSettingsConverter.convert(m_config);
        assertEquals(m_config.getFileSystemChoice(), FileSystemChoice.getCustomFsUrlChoice());
        assertEquals(m_config.getPathOrURL(), mountpointURL);
    }

    /**
     * Tests a not existing absolute mountpoint URL.
     */
    @Test
    public void knimeNotExistingMountPointConfigTest() {
        final String remoteMountpointNotExistingURL = "knime://knime.mountpoint" + PATH_STRING;
        m_config.setPathOrURL(remoteMountpointNotExistingURL);
        FileChooserSettingsConverter.convert(m_config);
        assertEquals(m_config.getFileSystemChoice(), FileSystemChoice.getCustomFsUrlChoice());
        assertEquals(m_config.getPathOrURL(), remoteMountpointNotExistingURL);
    }

    /**
     * Tests a absolute mountpoint URL.
     */
    @Test
    public void knimeExistingMountPointConfigTest() {
        final String remoteMountpointURL = "knime://"
            + KNIMEConnection.getOrCreateMountpointAbsoluteConnection("testMountpoint").getId() + PATH_STRING;
        m_config.setPathOrURL(remoteMountpointURL);
        FileChooserSettingsConverter.convert(m_config);
        assertEquals(m_config.getFileSystemChoice(), FileSystemChoice.getCustomFsUrlChoice());
        assertEquals(m_config.getPathOrURL(), remoteMountpointURL);
    }

}
