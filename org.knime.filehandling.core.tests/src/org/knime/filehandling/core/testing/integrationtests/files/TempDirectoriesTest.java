package org.knime.filehandling.core.testing.integrationtests.files;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.stream.Stream;

import org.junit.Test;
import org.knime.filehandling.core.connections.FSFiles;
import org.knime.filehandling.core.connections.FSPath;
import org.knime.filehandling.core.connections.base.BaseFileSystem;
import org.knime.filehandling.core.testing.FSTestInitializer;
import org.knime.filehandling.core.testing.integrationtests.AbstractParameterizedFSTest;
import org.knime.filehandling.core.util.IOESupplier;

/**
 * Tests temporary directory creation.
 *
 * @author Bjoern Lohrmann, KNIME GmbH
 */
public class TempDirectoriesTest extends AbstractParameterizedFSTest {

    public TempDirectoriesTest(final String fsType, final IOESupplier<FSTestInitializer> testInitializer)
        throws IOException {
        super(fsType, testInitializer);
    }

    @Test
    public void test_create_empty_temp_dir() throws IOException {
        final FSPath tempDir = FSFiles.createTempDirectory(m_testInitializer.getTestCaseScratchDir());
        assertTrue(Files.isDirectory(tempDir));

        final Path[] paths;
        try (final Stream<Path> dirStream = Files.list(tempDir)) {
            paths = dirStream.toArray(Path[]::new);
        }
        assertEquals(0, paths.length);
    }

    @Test
    public void test_create_temp_dir_with_prefix_and_suffix() throws IOException {
        final FSPath tempDir = FSFiles.createTempDirectory(m_testInitializer.getTestCaseScratchDir(), "testprefix", "testsuffix");
        assertTrue(Files.isDirectory(tempDir));
        String dirname = tempDir.getFileName().toString();
        if (dirname.endsWith("/")) {
            dirname = dirname.substring(0, dirname.length() - 1);
        }
        assertTrue(dirname.startsWith("testprefix"));
        assertTrue(dirname.endsWith("testsuffix"));
    }

    @Test
    public void test_create_temp_dir_cleanup() throws IOException {
        final FSPath tempDir = FSFiles.createTempDirectory(m_testInitializer.getTestCaseScratchDir());
        Files.createFile(tempDir.resolve("file1"));
        Files.createDirectory(tempDir.resolve("dir1"));
        Files.createFile(tempDir.resolve("dir1").resolve("file2"));

        assertTrue(Files.isDirectory(tempDir));
        if (getFileSystem() instanceof BaseFileSystem) {
            ((BaseFileSystem<?>)getFileSystem()).closeAllCloseables();
            assertTrue(Files.notExists(tempDir));
        }
    }

    @Test
    public void test_create_temp_dir_in_working_dir() throws IOException {
        final FSPath tempDir = FSFiles.createTempDirectory(m_connection.getFileSystem());
        assertTrue(Files.isDirectory(tempDir));
        assertEquals(m_connection.getFileSystem().getWorkingDirectory(), tempDir.getParent());
    }

    @Test
    public void test_create_temp_dir_using_relative_parent() throws IOException {
        final FSPath tempDir = FSFiles.createTempDirectory(getFileSystem().getPath("."));
        assertTrue(Files.isDirectory(tempDir));
        assertEquals(m_connection.getFileSystem().getWorkingDirectory(),
            tempDir.toAbsolutePath().normalize().getParent());
    }

    @Test(expected = NoSuchFileException.class)
    public void test_create_temp_dir_in_non_existent_folder() throws IOException {
        FSFiles.createTempDirectory((FSPath) m_testInitializer.getTestCaseScratchDir().resolve("doesnotexist"));
    }

}
