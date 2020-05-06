package org.knime.filehandling.core.testing.integrationtests.path;

import static org.junit.Assert.assertEquals;

import java.nio.file.FileSystem;
import java.nio.file.Path;

import org.junit.Test;
import org.knime.filehandling.core.testing.FSTestInitializer;
import org.knime.filehandling.core.testing.integrationtests.AbstractParameterizedFSTest;

public class ToStringTest extends AbstractParameterizedFSTest {

    public ToStringTest(final String fsType, final FSTestInitializer testInitializer) {
        super(fsType, testInitializer);
    }

    @Test
    public void testToStringRoot() {
        final FileSystem fileSystem = m_connection.getFileSystem();
        final String that = fileSystem.getSeparator();
        final Path path = fileSystem.getPath(that);

        assertEquals(that, path.toString());
    }

    @Test
    public void testToStringTrailingSlash() {
        ignoreWithReason("S3 differentiates between paths with and without trailing slashes.", S3);
        ignoreWithReason("Google storage differentiates between paths with and without trailing slashes.", GS);
        final FileSystem fileSystem = m_connection.getFileSystem();
        final String that = "qwehekweq" + fileSystem.getSeparator();
        final Path path = fileSystem.getPath(that);

        assertEquals("qwehekweq", path.toString());
    }

    @Test
    public void testToStringTrailingSlash2() {
        ignoreWithReason("S3 differentiates between paths with and without trailing slashes.", S3);
        ignoreWithReason("Google storage differentiates between paths with and without trailing slashes.", GS);
        final FileSystem fileSystem = m_connection.getFileSystem();
        final String sep = fileSystem.getSeparator();
        final String that = String.join(sep, "", "ab", "cd", "ef", "g");
        final Path path = fileSystem.getPath(that + sep);

        assertEquals(that, path.toString());
    }

    @Test
    public void testToStringEmpty() {
        ignoreWithReason("S3 differentiates between paths with and without trailing slashes.", S3);
        ignoreWithReason("Google storage differentiates between paths with and without trailing slashes.", GS);
        final String that = "";
        final FileSystem fileSystem = m_connection.getFileSystem();
        final Path path = fileSystem.getPath(that);

        assertEquals("", path.toString());
    }

    @Test
    public void testToStringSymbolicThisDir() {
        ignoreWithReason("S3 differentiates between paths with and without trailing slashes.", S3);
        ignoreWithReason("Google storage differentiates between paths with and without trailing slashes.", GS);
        final String that = ".";
        final FileSystem fileSystem = m_connection.getFileSystem();
        final Path path = fileSystem.getPath(that);

        assertEquals(".", path.toString());
    }

    @Test
    public void testToStringSymbolicThisDirBlobStore() {
        ignoreAllExcept(S3, GS);
        final String that = ".";
        final FileSystem fileSystem = m_connection.getFileSystem();
        final Path path = fileSystem.getPath(that);

        assertEquals("./", path.toString());
    }

}
