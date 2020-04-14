package org.knime.filehandling.core.testing.integrationtests.path;

import static org.junit.Assert.assertEquals;

import java.nio.file.FileSystem;
import java.nio.file.Path;

import org.junit.Test;
import org.knime.filehandling.core.testing.FSTestInitializer;
import org.knime.filehandling.core.testing.integrationtests.AbstractParameterizedFSTest;

public class CompareTest extends AbstractParameterizedFSTest {

    public CompareTest(final String fsType, final FSTestInitializer testInitializer) {
        super(fsType, testInitializer);
    }

    @Test
    public void testCompareRoot() {
        ignoreWithReason("S3 differentiates between paths with and without trailing slashes.", S3);
        ignoreWithReason("Google storage differentiates between paths with and without trailing slashes.", GS);
        final String that = "/";
        final String other = "";
        final FileSystem fileSystem = m_connection.getFileSystem();
        final Path path = fileSystem.getPath(that);
        final Path otherPath = fileSystem.getPath(other);

        assertEquals(that.compareTo(other), path.compareTo(otherPath));
    }

    @Test
    public void testCompareDifferent() {
        final String that = "/abc/def/";
        final String other = "/aaa/be/erk";
        final FileSystem fileSystem = m_connection.getFileSystem();
        final Path path = fileSystem.getPath(that);
        final Path otherPath = fileSystem.getPath(other);

        assertEquals(that.compareTo(other), path.compareTo(otherPath));
    }

    @Test
    public void testCompareChild() {
        final String that = "abc/def";
        final String other = "abc/def/hij";
        final FileSystem fileSystem = m_connection.getFileSystem();
        final Path path = fileSystem.getPath(that);
        final Path otherPath = fileSystem.getPath(other);

        assertEquals(that.compareTo(other), path.compareTo(otherPath));
    }

    @Test
    public void testCompareAbsolutRealtive() {
        final String that = "/abc/def/";
        final String other = "abc/def";
        final FileSystem fileSystem = m_connection.getFileSystem();
        final Path path = fileSystem.getPath(that);
        final Path otherPath = fileSystem.getPath(other);

        assertEquals(that.compareTo(other), path.compareTo(otherPath));
    }

    @Test
    public void testCompareEqual() {
        final String that = "/abc/def/";
        final FileSystem fileSystem = m_connection.getFileSystem();
        final Path path = fileSystem.getPath(that);

        assertEquals(that.compareTo(that), path.compareTo(path));
        assertEquals(0, path.compareTo(path));
    }
}
