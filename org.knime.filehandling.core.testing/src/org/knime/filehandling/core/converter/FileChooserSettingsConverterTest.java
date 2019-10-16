package org.knime.filehandling.core.converter;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;
import org.knime.filehandling.core.defaultnodesettings.FileChooserSettingsConverter;
import org.knime.filehandling.core.defaultnodesettings.FileSystemChoice;
import org.knime.filehandling.core.defaultnodesettings.KNIMEConnection;
import org.knime.filehandling.core.defaultnodesettings.SettingsModelFileChooser2;

/**
 * Junit test for testing the settings conversion for legacy path or url
 * strings.
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
     * Tests a local path with file:// input.
     */
    @Test
    public void localPathWithProtocolConfigTest() {
        m_config.setPathOrURL("file://" + PATH_STRING);
        FileChooserSettingsConverter.convert(m_config);
        assertEquals(m_config.getFileSystemChoice(), FileSystemChoice.getLocalFsChoice());
        assertEquals(m_config.getPathOrURL(), PATH_STRING);
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
        m_config.setPathOrURL("knime://knime.workflow" + PATH_STRING);
        FileChooserSettingsConverter.convert(m_config);
        assertEquals(m_config.getFileSystemChoice(), FileSystemChoice.getKnimeFsChoice());
        assertEquals(m_config.getKNIMEFileSystem(), KNIMEConnection.WORKFLOW_RELATIVE_CONNECTION.getId());
        assertEquals(m_config.getPathOrURL(), PATH_STRING);
    }

    /**
     * Tests a node relative URL input.
     */
    @Test
    public void knimeNodeConfigTest() {
        m_config.setPathOrURL("knime://knime.node" + PATH_STRING);
        FileChooserSettingsConverter.convert(m_config);
        assertEquals(m_config.getFileSystemChoice(), FileSystemChoice.getKnimeFsChoice());
        assertEquals(m_config.getKNIMEFileSystem(), KNIMEConnection.NODE_RELATIVE_CONNECTION.getId());
        assertEquals(m_config.getPathOrURL(), PATH_STRING);
    }

    /**
     * Tests a mountpoint relative URL input.
     */
    @Test
    public void knimeMountPointConfigTest() {
        m_config.setPathOrURL("knime://knime.mountpoint" + PATH_STRING);
        FileChooserSettingsConverter.convert(m_config);
        assertEquals(m_config.getFileSystemChoice(), FileSystemChoice.getKnimeFsChoice());
        assertEquals(m_config.getKNIMEFileSystem(), KNIMEConnection.MOUNTPOINT_RELATIVE_CONNECTION.getId());
        assertEquals(m_config.getPathOrURL(), PATH_STRING);
    }

    /**
     * Tests a not existing absolute mountpoint URL.
     */
    @Test
    public void knimeNotExistingMountPointConfigTest() {
        m_config.setPathOrURL("knime://" + "DoesNotExsist" + PATH_STRING);
        FileChooserSettingsConverter.convert(m_config);
        assertEquals(m_config.getFileSystemChoice(), FileSystemChoice.getKnimeFsChoice());
        assertEquals(m_config.getKNIMEFileSystem(), "DoesNotExsist");
        assertEquals(m_config.getPathOrURL(), PATH_STRING);
    }

    /**
     * Tests a absolute mountpoint URL.
     */
    @Test
    public void knimeExistingMountPointConfigTest() {
        m_config.setPathOrURL("knime://"
                + KNIMEConnection.getOrCreateMountpointAbsoluteConnection("testMountpoint").getId() + PATH_STRING);
        FileChooserSettingsConverter.convert(m_config);
        assertEquals(m_config.getFileSystemChoice(), FileSystemChoice.getKnimeFsChoice());
        assertEquals(m_config.getKNIMEFileSystem(),
                KNIMEConnection.getOrCreateMountpointAbsoluteConnection("testMountpoint").getId());
        assertEquals(m_config.getPathOrURL(), PATH_STRING);
    }

}
