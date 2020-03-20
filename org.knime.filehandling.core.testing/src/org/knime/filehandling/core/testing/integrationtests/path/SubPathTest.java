package org.knime.filehandling.core.testing.integrationtests.path;

import static org.junit.Assert.assertEquals;

import java.nio.file.FileSystem;
import java.nio.file.Path;

import org.junit.Test;
import org.knime.filehandling.core.testing.FSTestInitializer;
import org.knime.filehandling.core.testing.integrationtests.AbstractParameterizedFSTest;

public class SubPathTest extends AbstractParameterizedFSTest {

    public SubPathTest(final String fsType, final FSTestInitializer testInitializer) {
        super(fsType, testInitializer);
    }

    @Test
    public void testesubPath() {
        final String that = "/0/1/2/3";
        final FileSystem fileSystem = m_connection.getFileSystem();
        final Path path = fileSystem.getPath(that);

        assertEquals("0/1/2/3", path.subpath(0, 4).toString());
    }

    @Test
    public void testesubPath2() {
        final String that = "/0/1/2/3";
        final FileSystem fileSystem = m_connection.getFileSystem();
        final Path path = fileSystem.getPath(that);

        assertEquals("0/1/2", path.subpath(0, 3).toString());
    }

    @Test
    public void testesubPath2a() {
        final String that = "0/1/2/3";
        final FileSystem fileSystem = m_connection.getFileSystem();
        final Path path = fileSystem.getPath(that);

        assertEquals("0/1/2", path.subpath(0, 3).toString());
    }

    @Test
    public void testesubPath3() {
        final String that = "/ii";
        final FileSystem fileSystem = m_connection.getFileSystem();
        final Path path = fileSystem.getPath(that);

        assertEquals("ii", path.subpath(0, 1).toString());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testesubPath4() {
        final String that = "/0/1/2/3";
        final FileSystem fileSystem = m_connection.getFileSystem();
        final Path path = fileSystem.getPath(that);

        path.subpath(-1, 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testesubPath5() {
        final String that = "/0/1/2/3";
        final FileSystem fileSystem = m_connection.getFileSystem();
        final Path path = fileSystem.getPath(that);

        path.subpath(2, 1);
    }

    @SuppressWarnings("resource")
    @Test(expected = IllegalArgumentException.class)
    public void testesubPath6() {
        final String that = "/0/1/2/3";
        final FileSystem fileSystem = m_connection.getFileSystem();
        final Path path = fileSystem.getPath(that);

        path.subpath(4, 0);
    }
}
