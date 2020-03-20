package org.knime.filehandling.core.testing.integrationtests.path;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.nio.file.FileSystem;
import java.nio.file.Path;

import org.junit.Test;
import org.knime.filehandling.core.testing.FSTestInitializer;
import org.knime.filehandling.core.testing.integrationtests.AbstractParameterizedFSTest;

public class EndsStartsWithTest extends AbstractParameterizedFSTest {

    public EndsStartsWithTest(final String fsType, final FSTestInitializer testInitializer) {
        super(fsType, testInitializer);
    }

    @Test
    public void testEndWith() {
        final String that = "/abc/de/fg/de/";
        final String other = "fg/de";
        final FileSystem fileSystem = m_connection.getFileSystem();
        final Path path = fileSystem.getPath(that);
        final Path otherPath = fileSystem.getPath(other);

        assertTrue(path.endsWith(otherPath));
        assertFalse(otherPath.endsWith(path));
    }

    @Test
    public void testNotEndWith() {
        final String that = "/abc/de/fg/de/";
        final String other = "qe/de";
        final FileSystem fileSystem = m_connection.getFileSystem();
        final Path path = fileSystem.getPath(that);
        final Path otherPath = fileSystem.getPath(other);

        assertFalse(path.endsWith(otherPath));
        assertFalse(otherPath.endsWith(path));
    }

    @Test
    public void testNotEndWithLongerPath() {
        final String that = "qe/de";
        final String other = "/abc/de/fg/de/";
        final FileSystem fileSystem = m_connection.getFileSystem();
        final Path path = fileSystem.getPath(that);
        final Path otherPath = fileSystem.getPath(other);

        assertFalse(path.endsWith(otherPath));
        assertFalse(otherPath.endsWith(path));
    }

    @Test
    public void testStartsWith() {
        final String that = "/abc/de/fg/de/";
        final String other = "/abc/de";
        final FileSystem fileSystem = m_connection.getFileSystem();
        final Path path = fileSystem.getPath(that);
        final Path otherPath = fileSystem.getPath(other);

        assertTrue(path.startsWith(otherPath));
        assertFalse(otherPath.startsWith(path));
    }

    @Test
    public void testNotStartsWithLongerPath() {
        final String that = "qe/de";
        final String other = "/abc/de/fg/de/";
        final FileSystem fileSystem = m_connection.getFileSystem();
        final Path path = fileSystem.getPath(that);
        final Path otherPath = fileSystem.getPath(other);

        assertFalse(path.startsWith(otherPath));
        assertFalse(otherPath.startsWith(path));
    }

}
