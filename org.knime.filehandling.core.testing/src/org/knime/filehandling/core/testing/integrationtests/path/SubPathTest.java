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
    public void testSubpath() {
        final String that = "/0/1/2/3";
        final String subpath = "0/1/2/3";
        final FileSystem fileSystem = m_connection.getFileSystem();
        final Path path = fileSystem.getPath(that);
        final Path expectedSubpath = fileSystem.getPath(subpath);

        assertEquals(expectedSubpath, path.subpath(0, 4));
        assertEquals(expectedSubpath.toString(), path.subpath(0, 4).toString());
    }

    @Test
    public void testSubpath2() {
        final String that = "/0/1/2/3";
        final String subpath = "0/1/2/";
        final FileSystem fileSystem = m_connection.getFileSystem();
        final Path path = fileSystem.getPath(that);

        final Path expectedSubpath = fileSystem.getPath(subpath);

        assertEquals(expectedSubpath, path.subpath(0, 3));
        assertEquals(expectedSubpath.toString(), path.subpath(0, 3).toString());
    }

    @Test
    public void testSubpath2a() {
        final String that = "0/1/2/3";
        final String subpath = "0/1/2/";
        final FileSystem fileSystem = m_connection.getFileSystem();
        final Path path = fileSystem.getPath(that);

        final Path expectedSubpath = fileSystem.getPath(subpath);

        assertEquals(expectedSubpath, path.subpath(0, 3));
        assertEquals(expectedSubpath.toString(), path.subpath(0, 3).toString());
    }

    @Test
    public void testSubpath3() {
        final String that = "/ii";
        final FileSystem fileSystem = m_connection.getFileSystem();
        final Path path = fileSystem.getPath(that);

        assertEquals("ii", path.subpath(0, 1).toString());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSubpath4() {
        final String that = "/0/1/2/3";
        final FileSystem fileSystem = m_connection.getFileSystem();
        final Path path = fileSystem.getPath(that);

        path.subpath(-1, 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSubpathBeginAfterEnd() {
        final String that = "/0/1/2/3";
        final FileSystem fileSystem = m_connection.getFileSystem();
        final Path path = fileSystem.getPath(that);

        path.subpath(2, 1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSubpathBeginAfterEnd2() {
        final String that = "/0/1/2/3";
        final FileSystem fileSystem = m_connection.getFileSystem();
        final Path path = fileSystem.getPath(that);

        path.subpath(4, 0);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testSubpathBeginEqualsEnd() {
        final String that = "/0/1/2/3";
        final FileSystem fileSystem = m_connection.getFileSystem();
        final Path path = fileSystem.getPath(that);

        path.subpath(0, 0);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testSubpathEndTooHigh() {
        final String that = "/0/1/2/3";
        final FileSystem fileSystem = m_connection.getFileSystem();
        final Path path = fileSystem.getPath(that);

        path.subpath(0, 5);
    }

}
