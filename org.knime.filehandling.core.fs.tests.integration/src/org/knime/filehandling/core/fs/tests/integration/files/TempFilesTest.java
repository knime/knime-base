package org.knime.filehandling.core.fs.tests.integration.files;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.StandardOpenOption;

import org.junit.Assume;
import org.junit.Test;
import org.knime.filehandling.core.connections.FSFiles;
import org.knime.filehandling.core.connections.FSPath;
import org.knime.filehandling.core.connections.base.BaseFileSystem;
import org.knime.filehandling.core.fs.tests.integration.AbstractParameterizedFSTest;
import org.knime.filehandling.core.testing.FSTestInitializer;
import org.knime.filehandling.core.util.IOESupplier;

/**
 * Tests temporary file creation.
 *
 * @author Bjoern Lohrmann, KNIME GmbH
 */
public class TempFilesTest extends AbstractParameterizedFSTest {

    public TempFilesTest(final String fsType, final IOESupplier<FSTestInitializer> testInitializer) throws IOException {
        super(fsType, testInitializer);
    }

    @Test
    public void test_create_temp_file_simple() throws IOException {
        Assume.assumeTrue(m_connection.getFSDescriptor().getCapabilities().canWriteFiles());

        final FSPath tempFile = FSFiles.createTempFile(m_testInitializer.getTestCaseScratchDir());
        assertTrue(Files.isRegularFile(tempFile));
        assertArrayEquals(new byte[0], Files.readAllBytes(tempFile));

        final byte[] expectedBytes = "bla".getBytes();
        Files.write(tempFile, expectedBytes, StandardOpenOption.TRUNCATE_EXISTING);
        assertArrayEquals(expectedBytes, Files.readAllBytes(tempFile));
    }

    @Test
    public void test_create_temp_file_with_prefix_and_suffix() throws IOException {
        Assume.assumeTrue(m_connection.getFSDescriptor().getCapabilities().canWriteFiles());

        final FSPath tempFile = FSFiles.createTempFile(m_testInitializer.getTestCaseScratchDir(), "testprefix", "testsuffix");
        assertTrue(Files.isRegularFile(tempFile));
        assertArrayEquals(new byte[0], Files.readAllBytes(tempFile));
        assertTrue(tempFile.getFileName().toString().startsWith("testprefix"));
        assertTrue(tempFile.getFileName().toString().endsWith("testsuffix"));
    }

    @Test
    public void test_create_temp_file_cleanup() throws IOException {
        Assume.assumeTrue(m_connection.getFSDescriptor().getCapabilities().canWriteFiles());

        final FSPath tempFile = FSFiles.createTempFile(m_testInitializer.getTestCaseScratchDir(), "testprefix", "testsuffix");

        assertTrue(Files.isRegularFile(tempFile));
        if (getFileSystem() instanceof BaseFileSystem) {
            ((BaseFileSystem<?>)getFileSystem()).closeAllCloseables();
            assertTrue(Files.notExists(tempFile));
        }
    }

    @SuppressWarnings("resource")
    @Test
    public void test_create_temp_file_in_working_dir() throws IOException {
        Assume.assumeTrue(m_connection.getFSDescriptor().getCapabilities().canWriteFiles());

        ignoreWithReason("The working directory in knime-local-relative-workflow is not actually a directory a file",
            KNIME_LOCAL_RELATIVE_WORKFLOW);

        final FSPath tempFile = FSFiles.createTempFile(m_connection.getFileSystem());
        assertTrue(Files.isRegularFile(tempFile));
        assertEquals(m_connection.getFileSystem().getWorkingDirectory(), tempFile.getParent());
    }

    @Test
    public void test_create_temp_file_using_relative_parent() throws IOException {
        Assume.assumeTrue(m_connection.getFSDescriptor().getCapabilities().canWriteFiles());

        ignoreWithReason("The working directory in knime-local-relative-workflow is not actually a directory a file",
            KNIME_LOCAL_RELATIVE_WORKFLOW);

        final FSPath tempFile = FSFiles.createTempFile(getFileSystem().getPath("."));
        assertTrue(Files.isRegularFile(tempFile));
        assertEquals(m_connection.getFileSystem().getWorkingDirectory(),
            tempFile.toAbsolutePath().normalize().getParent());
    }

    @Test(expected = NoSuchFileException.class)
    public void test_create_temp_file_in_non_existent_folder() throws IOException {
        Assume.assumeTrue(m_connection.getFSDescriptor().getCapabilities().canWriteFiles());

        FSFiles.createTempFile((FSPath) m_testInitializer.getTestCaseScratchDir().resolve("doesnotexist"));
    }
}
