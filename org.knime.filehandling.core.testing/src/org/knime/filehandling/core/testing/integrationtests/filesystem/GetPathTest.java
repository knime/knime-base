package org.knime.filehandling.core.testing.integrationtests.filesystem;

import static org.junit.Assert.assertEquals;

import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.util.stream.IntStream;

import org.junit.Test;
import org.knime.filehandling.core.testing.FSTestInitializer;
import org.knime.filehandling.core.testing.integrationtests.AbstractParameterizedFSTest;

/**
 * Test class for copy operations on file systems.
 * 
 * @author Bjoern Lohrmann, KNIME GmbH, Berlin, Germany
 *
 */
public class GetPathTest extends AbstractParameterizedFSTest {

    public GetPathTest(final String fsType, final FSTestInitializer testInitializer) {
        super(fsType, testInitializer);
    }

    @Test
    public void test_get_path_testing_root() throws Exception {
        final FileSystem fileSystem = m_connection.getFileSystem();
        assertEquals(m_testInitializer.getRoot(), fileSystem.getPath(m_testInitializer.getRoot().toString()));
    }

    @Test
    public void test_get_path_testing_root2() throws Exception {
        final FileSystem fileSystem = m_connection.getFileSystem();
        final Path testingRoot = m_testInitializer.getRoot();
        final String[] pathComponents = IntStream.range(0, testingRoot.getNameCount())//
                .mapToObj(i -> testingRoot.getName(i).toString())//
                .toArray(String[]::new);

        final Path resultingPath = fileSystem.getPath(testingRoot.getRoot().toString(), pathComponents);
        assertEquals(testingRoot, resultingPath);
    }

    @Test
    public void test_get_path_appending_file() {
        final FileSystem fileSystem = m_connection.getFileSystem();
        final Path testingRoot = m_testInitializer.getRoot();
        assertEquals(testingRoot.resolve("file"), fileSystem.getPath(testingRoot.toString(), "file"));
    }
    
    @Test
    public void test_get_path_appending_folders() {
        final FileSystem fileSystem = m_connection.getFileSystem();
        final Path testingRoot = m_testInitializer.getRoot();
        
        assertEquals(testingRoot.resolve("folder1").resolve("folder2" + fileSystem.getSeparator()), 
                fileSystem.getPath(testingRoot.toString(), "folder1", "folder2" + fileSystem.getSeparator()));
    }

}
