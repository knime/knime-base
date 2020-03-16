package org.knime.filehandling.core.testing.integrationtests.path;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.nio.file.FileSystem;
import java.nio.file.Path;

import org.junit.Test;
import org.knime.filehandling.core.testing.FSTestInitializer;
import org.knime.filehandling.core.testing.integrationtests.AbstractParameterizedFSTest;

public class PathTest extends AbstractParameterizedFSTest {

    public PathTest(final String fsType, final FSTestInitializer testInitializer) {
        super(fsType, testInitializer);
    }

    @Test
    public void testFileName() {
        final FileSystem fileSystem = m_connection.getFileSystem();
        final Path path = fileSystem.getPath("path", "to", "file");

        assertEquals("file", path.getFileName().toString());
    }

    @Test
    public void testNameCount() {
        final FileSystem fileSystem = m_connection.getFileSystem();
        final Path path = fileSystem.getPath("path", "to", "file");

        assertEquals(3, path.getNameCount());
    }

    @Test
    public void testNameCountFromString() {
        final FileSystem fileSystem = m_connection.getFileSystem();
        final Path path = fileSystem.getPath(String.join(fileSystem.getSeparator(), "path", "", "to", "", "file"));

        assertEquals(3, path.getNameCount());
    }

    @Test
    public void testNameCountFromStringSeveralSeparatorsTrailing() {
        final FileSystem fileSystem = m_connection.getFileSystem();
        final Path path = fileSystem.getPath(String.join(fileSystem.getSeparator(), "path", "to", "file", "", ""));

        assertEquals(3, path.getNameCount());
    }

    @Test
    public void testRealtivize() {
        final FileSystem fileSystem = m_connection.getFileSystem();
        final Path pathToRelativizeTo = fileSystem.getPath("abc/de/");
        final Path path = fileSystem.getPath("de/fg/");
        assertEquals(path, pathToRelativizeTo.relativize(pathToRelativizeTo.resolve(path)));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRealtivizeAbsolutToRelative() {
        final FileSystem fileSystem = m_connection.getFileSystem();
        final Path pathToRelativizeTo = fileSystem.getPath("abc/de/");
        final Path path = fileSystem.getPath("/de/fg/");
        assertEquals(path, pathToRelativizeTo.relativize(pathToRelativizeTo.resolve(path)));
    }

    @Test
    public void testRealtivizeRealtiveWithToParent() {
        final FileSystem fileSystem = m_connection.getFileSystem();
        final Path pathToRelativizeTo = fileSystem.getPath("abc/de/");
        final Path path = fileSystem.getPath("../../de/fg/");
        assertEquals(path, pathToRelativizeTo.relativize(pathToRelativizeTo.resolve(path)));
    }

    @Test
    public void testRealtivizeRealtiveWithToParentInBasePath() {
        final FileSystem fileSystem = m_connection.getFileSystem();
        final Path pathToRelativizeTo = fileSystem.getPath("abc/de/fg/../../");
        final Path path = fileSystem.getPath("../../de/fg/");
        assertEquals(path, pathToRelativizeTo.relativize(pathToRelativizeTo.resolve(path)));
    }

    @Test
    public void testRealtivizeRealtiveWithToParentInBasePath2() {
        final FileSystem fileSystem = m_connection.getFileSystem();
        final Path pathToRelativizeTo = fileSystem.getPath("../../../abc/de/fg/");
        final Path path = fileSystem.getPath("../../de/fg/");
        assertEquals(path, pathToRelativizeTo.relativize(pathToRelativizeTo.resolve(path)));
    }

    @Test
    public void testNormalize() {
        final FileSystem fileSystem = m_connection.getFileSystem();
        final Path path = fileSystem.getPath("/a/b/../../abc/././de/");

        assertEquals("/abc/de", path.normalize().toString());
    }

    @Test
    public void testNormalizeToEmpty() {
        final FileSystem fileSystem = m_connection.getFileSystem();
        final Path path = fileSystem.getPath("de/..");

        assertEquals("", path.normalize().toString());
    }

    @Test
    public void testPathEquals() {
		final FileSystem fileSystem = m_connection.getFileSystem();
		final Path first = fileSystem.getPath("some-dir", "first-file.txt");
		final Path sameFirst = fileSystem.getPath("some-dir", "first-file.txt");
		final Path second = fileSystem.getPath("some-dir", "other-file.txt");
		final Path third = fileSystem.getPath("other-dir", "first-file.txt");

		assertEquals(first, first);
		assertEquals(first, sameFirst);
		assertNotEquals(first, second);
		assertNotEquals(first, third);
		assertNotEquals(second, third);
    }

}
