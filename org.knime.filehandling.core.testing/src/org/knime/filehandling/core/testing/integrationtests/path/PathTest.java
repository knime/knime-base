package org.knime.filehandling.core.testing.integrationtests.path;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;
import org.knime.filehandling.core.testing.FSTestInitializer;
import org.knime.filehandling.core.testing.integrationtests.AbstractParameterizedFSTest;

public class PathTest extends AbstractParameterizedFSTest {

    public PathTest(final String fsType, final FSTestInitializer testInitializer) {
        super(fsType, testInitializer);
    }

    @Test
    public void testFileName() {
        final FileSystem fileSystem = getFileSystem();
        final Path path = fileSystem.getPath("path", "to", "file");

        assertEquals("file", path.getFileName().toString());
    }

    @Test
    public void testNameCount() {
        final FileSystem fileSystem = getFileSystem();
        final Path path = fileSystem.getPath("path", "to", "file");

        assertEquals(3, path.getNameCount());
    }

    @Test
    public void testNameCountFromString() {
        final FileSystem fileSystem = getFileSystem();
        final Path path = fileSystem.getPath(String.join(fileSystem.getSeparator(), "path", "", "to", "", "file"));

        assertEquals(3, path.getNameCount());
    }

    @Test
    public void testNameCountFromStringSeveralSeparatorsTrailing() {
        final FileSystem fileSystem = getFileSystem();
        final Path path = fileSystem.getPath(String.join(fileSystem.getSeparator(), "path", "to", "file", "", ""));

        assertEquals(3, path.getNameCount());
    }

    @Test
    public void testRealtivize() {
        final FileSystem fileSystem = getFileSystem();
        final Path pathToRelativizeTo = fileSystem.getPath("abc/de/");
        final Path path = fileSystem.getPath("de/fg/");
        assertEquals(path, pathToRelativizeTo.relativize(pathToRelativizeTo.resolve(path)));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRealtivizeAbsolutToRelative() {
        final FileSystem fileSystem = getFileSystem();
        final Path pathToRelativizeTo = fileSystem.getPath("abc/de/");
        final Path path = fileSystem.getPath("/de/fg/");
        assertEquals(path, pathToRelativizeTo.relativize(pathToRelativizeTo.resolve(path)));
    }

    @Test
    public void testRealtivizeRealtiveWithToParent() {
        final FileSystem fileSystem = getFileSystem();
        final Path pathToRelativizeTo = fileSystem.getPath("abc/de/");
        final Path path = fileSystem.getPath("../../de/fg/");
        assertEquals(path, pathToRelativizeTo.relativize(pathToRelativizeTo.resolve(path)));
    }

    @Test
    public void testRealtivizeRealtiveWithToParentInBasePath() {
        final FileSystem fileSystem = getFileSystem();
        final Path pathToRelativizeTo = fileSystem.getPath("abc/de/fg/../../");
        final Path path = fileSystem.getPath("../../de/fg/");
        assertEquals(path, pathToRelativizeTo.relativize(pathToRelativizeTo.resolve(path)));
    }

    @Test
    public void testRealtivizeRealtiveWithToParentInBasePath2() {
        final FileSystem fileSystem = getFileSystem();
        final Path pathToRelativizeTo = fileSystem.getPath("../../../abc/de/fg/");
        final Path path = fileSystem.getPath("../../de/fg/");
        assertEquals(path, pathToRelativizeTo.relativize(pathToRelativizeTo.resolve(path)));
    }

    @Test
    public void testRealtivizeRealtiveWithEmptyPath() {
        final FileSystem fileSystem = getFileSystem();
        final String that = "as/de/";
        final String other = "";
        final Path path = fileSystem.getPath(that);
        final Path otherPath = fileSystem.getPath(other);

        final Path backForth = path.relativize(path.resolve(other));
        assertEquals(otherPath, backForth);
    }

    @Test
    public void testRelativizeAgainstRootPath() {
        final FileSystem fileSystem = getFileSystem();
        final Path root = fileSystem.getPath("/");
        final Path somePath = fileSystem.getPath("/some/path");
        final Path relativePath = root.relativize(somePath);
        assertTrue(somePath.isAbsolute());
        assertFalse(relativePath.isAbsolute());
        assertEquals("some/path", relativePath.toString());
    }

    @Test
    public void testRealtivizeRealtiveEmptyPath() {
        ignoreWithReason("S3 differentiates between paths with and without trailing slashes.", S3);
        ignoreWithReason("Google storage differentiates between paths with and without trailing slashes.", GS);
        final FileSystem fileSystem = getFileSystem();
        final String that = "";
        final String other = "as/de/";
        final Path path = fileSystem.getPath(that);
        final Path otherPath = fileSystem.getPath(other);

        final Path backForth = path.relativize(path.resolve(other));
        assertEquals(otherPath.toString(), backForth.toString());
    }

    @Test
    public void testRelativizeRelativePathToEmpty() {
        final FileSystem fileSystem = getFileSystem();
        final String that = "ab/cd";
        final String other = "ab/cd";
        final Path path = fileSystem.getPath(that);
        final Path otherPath = fileSystem.getPath(other);

        assertEquals(fileSystem.getPath(""), path.relativize(otherPath));
    }

    @Test
    public void testRelativizeRelativePathSimple() {
        final FileSystem fileSystem = getFileSystem();
        final String that = "ab/cd";
        final String other = "ab/xy";
        final Path path = fileSystem.getPath(that);
        final Path otherPath = fileSystem.getPath(other);

        assertEquals(fileSystem.getPath("../xy"), path.relativize(otherPath));
    }

    @Test
    public void testRelativizeAbsolutePathSimple() {
        final FileSystem fileSystem = getFileSystem();
        final String that = "/ab/cd";
        final String other = "/ab/xy";
        final Path path = fileSystem.getPath(that);
        final Path otherPath = fileSystem.getPath(other);

        assertEquals(fileSystem.getPath("../xy"), path.relativize(otherPath));
    }

    @Test
    public void testRelativizeRelativePathWithDotDot() {
        final FileSystem fileSystem = getFileSystem();
        final String that = "ab/cd";
        final String other = "ab/xy/../xv";
        final Path path = fileSystem.getPath(that);
        final Path otherPath = fileSystem.getPath(other);

        assertEquals(fileSystem.getPath("../xy/../xv"), path.relativize(otherPath));
    }

    @Test
    public void testRelativizeAbsolutePathWithDotDot() {
        final FileSystem fileSystem = getFileSystem();
        final String that = "/ab/cd";
        final String other = "/ab/xy/../xv";
        final Path path = fileSystem.getPath(that);
        final Path otherPath = fileSystem.getPath(other);

        assertEquals(fileSystem.getPath("../xy/../xv"), path.relativize(otherPath));
    }

    @Test
    public void testRelativizeStartsWith() {
        final FileSystem fileSystem = getFileSystem();
        final String that = "/ab/cd";
        final String other = "/ab/cd/../../xv";
        final Path path = fileSystem.getPath(that);
        final Path otherPath = fileSystem.getPath(other);

        assertEquals(fileSystem.getPath("../../xv"), path.relativize(otherPath));
    }

    @Test
    public void testRelativizeStartsWith2() {
        final FileSystem fileSystem = getFileSystem();
        final String that = "/ab/cd/../../xv";
        final String other = "/ab/cd";
        final Path path = fileSystem.getPath(that);
        final Path otherPath = fileSystem.getPath(other);

        assertEquals(fileSystem.getPath("../../.."), path.relativize(otherPath));
    }

    @Test
    public void testNormalize() {
        final FileSystem fileSystem = getFileSystem();
        final Path path = fileSystem.getPath("/a/b/../../abc/././de");

        assertEquals("/abc/de", path.normalize().toString());
    }

    @Test
    public void testNormalizeAbsolute() {
        final FileSystem fileSystem = getFileSystem();
        final Path path = fileSystem.getPath("/../b");

        assertEquals("/b", path.normalize().toString());
    }

    @Test
    public void testNormalizeAbsolute2() {
        final FileSystem fileSystem = getFileSystem();
        final Path path = fileSystem.getPath("/../../b");

        assertEquals("/b", path.normalize().toString());
    }

    @Test
    public void testNormalizeRelative() {
        final FileSystem fileSystem = getFileSystem();
        final Path path = fileSystem.getPath("../b");

        assertEquals("../b", path.normalize().toString());
    }

    @Test
    public void testNormalizeRelative2() {
        final FileSystem fileSystem = getFileSystem();
        final Path path = fileSystem.getPath("../../b");

        assertEquals("../../b", path.normalize().toString());
    }

    @Test
    public void testNormalizeToEmpty() {
        ignoreWithReason("S3 differentiates between paths with and without trailing slashes.", S3);
        ignoreWithReason("Google storage differentiates between paths with and without trailing slashes.", GS);
        final FileSystem fileSystem = getFileSystem();
        final Path path = fileSystem.getPath("de/..");

        assertEquals("", path.normalize().toString());
    }

    @Test
    public void testNormalizeToEmptyObjectStore() {
        ignoreAllExcept(S3, GS);

        final FileSystem fileSystem = getFileSystem();
        final Path path = fileSystem.getPath("de/..");

        assertEquals("./", path.normalize().toString());
    }

    @Test
    public void testPathEquals() {
        final FileSystem fileSystem = getFileSystem();
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

    public void testNormalizeToEmpty2() {
        ignoreWithReason("S3 differentiates between paths with and without trailing slashes.", S3);
        ignoreWithReason("Google storage differentiates between paths with and without trailing slashes.", GS);
        final FileSystem fileSystem = getFileSystem();
        final Path path = fileSystem.getPath(".");

        assertEquals("", path.normalize().toString());
    }

    public void testNormalizeToEmpty2BlobStore() {
        ignoreAllExcept(S3, GS);
        final FileSystem fileSystem = getFileSystem();
        final Path path = fileSystem.getPath(".");

        assertEquals("/", path.normalize().toString());
    }

    @Test
    public void testEquals() {
        ignoreWithReason("S3 differentiates between paths with and without trailing slashes.", S3);
        ignoreWithReason("Google storage differentiates between paths with and without trailing slashes.", GS);
        final FileSystem fileSystem = getFileSystem();
        final String that = "as/de/";
        final String other = "as/de";
        final Path path = fileSystem.getPath(that);
        final Path otherPath = fileSystem.getPath(other);

        assertEquals(path, otherPath);
    }

    @Test
    public void testNotEqualRealtiveAndAbsolutPath() {
        final FileSystem fileSystem = getFileSystem();
        final String that = "/abcd";
        final String other = "abcd";
        final Path path = fileSystem.getPath(that);
        final Path otherPath = fileSystem.getPath(other);

        assertNotEquals(path, otherPath);
    }

    @Test
    public void testGetParent() {
        final FileSystem fileSystem = getFileSystem();
        final Path path = fileSystem.getPath("/a/b/c/d");

        assertEquals(fileSystem.getPath("/a/b/c/"), path.getParent());
    }

    @Test
    public void testGetParentRelative() {
        final FileSystem fileSystem = getFileSystem();
        final Path path = fileSystem.getPath("a/b/c/d");

        assertEquals(fileSystem.getPath("a/b/c/"), path.getParent());
    }

    @Test
    public void testGetParentEmptyPath() {
        final FileSystem fileSystem = getFileSystem();
        final Path path = fileSystem.getPath("");

        assertEquals(null, path.getParent());
    }

    @Test
    public void testGetParentDot() {
        final FileSystem fileSystem = getFileSystem();
        final Path path = fileSystem.getPath(".");

        assertEquals(null, path.getParent());
    }

    @Test
    public void testGetParentDotSlash() {
        final FileSystem fileSystem = getFileSystem();
        final Path path = fileSystem.getPath("./");

        assertEquals(null, path.getParent());
    }

    @Test
    public void testGetParentAbsolutToNull2() {
        final FileSystem fileSystem = getFileSystem();
        final Path path = fileSystem.getPath("/");

        assertEquals(null, path.getParent());
    }

    @Test
    public void testGetName() {
        ignoreWithReason("S3 differentiates between paths with and without trailing slashes.", S3);
        ignoreWithReason("Google storage differentiates between paths with and without trailing slashes.", GS);
        final FileSystem fileSystem = getFileSystem();
        final Path path = fileSystem.getPath("/abc/de/fg");

        assertEquals("abc", path.getName(0).toString());
        assertEquals("de", path.getName(1).toString());
        assertEquals("fg", path.getName(2).toString());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetNameNonexistent() {
        final FileSystem fileSystem = getFileSystem();
        final Path path = fileSystem.getPath("/abc");

        path.getName(1).toString();
    }

    @Test
    public void testGetNameBlobStore() {
        ignoreAllExcept(S3, GS);
        final FileSystem fileSystem = getFileSystem();
        final Path path = fileSystem.getPath("/abc/de/fg");

        assertEquals("abc/", path.getName(0).toString());
        assertEquals("de/", path.getName(1).toString());
        assertEquals("fg", path.getName(2).toString());
    }

    @Test
    public void testGetRootSimple() {
        final Path path = m_testInitializer.makePath("a", "b");
        final Set<Path> roots = new HashSet<>();
        getFileSystem().getRootDirectories().forEach(roots::add);
        assertTrue(roots.contains(path.getRoot()));
    }

    @Test
    public void testGetRootNull() {
        // a relative path should have a null root
        final Path path = getFileSystem().getPath("a", "b");
        assertNull(path.getRoot());
    }

}
