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
        final FileSystem fs = getFileSystem();
        final Path path = fs.getPath(fs.getRootDirectories().iterator().next().toString(), "0", "1", "2", "3");
        final Path expectedSubpath = fs.getPath("0", "1", "2", "3");

        assertEquals(expectedSubpath, path.subpath(0, 4));
        assertEquals(expectedSubpath.toString(), path.subpath(0, 4).toString());
    }

    @Test
    public void testSubpath2() {
        final FileSystem fs = getFileSystem();
        final Path path = fs.getPath(fs.getRootDirectories().iterator().next().toString(), "0", "1", "2", "3");
        final Path expectedSubpath = fs.getPath("0", "1", "2", fs.getSeparator());

        assertEquals(expectedSubpath, path.subpath(0, 3));
        assertEquals(expectedSubpath.toString(), path.subpath(0, 3).toString());
    }

    @Test
    public void testSubpath2a() {
        final FileSystem fs = getFileSystem();
        final Path path = fs.getPath("0", "1", "2", "3");
        final Path expectedSubpath = fs.getPath("0", "1", "2", fs.getSeparator());

        assertEquals(expectedSubpath, path.subpath(0, 3));
        assertEquals(expectedSubpath.toString(), path.subpath(0, 3).toString());
    }

    @Test
    public void testSubpath3() {
        final FileSystem fs = getFileSystem();
        final Path path = fs.getPath(fs.getRootDirectories().iterator().next().toString(), "ii");
        final Path expectedSubpath = fs.getPath("ii");

        assertEquals(expectedSubpath, path.subpath(0, 1));
        assertEquals(expectedSubpath.toString(), path.subpath(0, 1).toString());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSubpath4() {
        getFileSystem().getPath("0", "1").subpath(-1, 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSubpathBeginAfterEnd() {
        getFileSystem().getPath("0", "1", "2", "3").subpath(2, 1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSubpathBeginAfterEnd2() {
        getFileSystem().getPath("0", "1", "2", "3").subpath(4, 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSubpathBeginEqualsEnd() {
        getFileSystem().getPath("0", "1", "2", "3").subpath(0, 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSubpathEndTooHigh() {
        getFileSystem().getPath("0", "1", "2", "3").subpath(0, 5);
    }

}
