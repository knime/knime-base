package org.knime.filehandling.core.fs.location;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Optional;

import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import org.knime.filehandling.core.connections.FSConnection;
import org.knime.filehandling.core.connections.FSLocation;
import org.knime.filehandling.core.connections.FSPath;
import org.knime.filehandling.core.connections.location.FSPathProvider;
import org.knime.filehandling.core.connections.location.FSPathProviderFactory;

/**
 * Base class for unit tests for {@link FSPathProviderFactory}. This class encapsulates the pattern that must be used
 * when reading from an {@link FSLocation}.
 *
 * @author Bjoern Lohrmann, KNIME GmbH
 */
public class FSPathProviderFactoryTestBase {

    /**
     * Local temp folder to use in unit test cases.
     */
    @Rule
    public TemporaryFolder m_tempFolder = new TemporaryFolder(); // NOSONAR must be public, used by subclasses

    /**
     * Reads all bytes from the given {@link FSLocation} and compares them to the given expected bytes.
     *
     * @param optionalPortObjectConnection An optional port object connection.
     * @param loc The {@link FSLocation} to read.
     * @param expectedBytes The bytes that are expected to be read.
     * @throws IOException
     */
    protected void testReadFSLocation(final Optional<FSConnection> optionalPortObjectConnection, final FSLocation loc,
        final byte[] expectedBytes) throws IOException {

        testReadFSLocation(optionalPortObjectConnection, loc, expectedBytes, loc.getPath());
    }

    /**
     * Makes the following checks:
     *
     * <ul>
     * <li>Reads all bytes from the given {@link FSLocation} compares them to the given expected bytes.</li>
     * <li>Checks that {@link FSPath#toString()} of the resulting path equals expected path string.</li>
     * <li>Checks that {@link FSPath#toFSLocation()} of the resulting path equals the provided {@link FSLocation}.</li>
     * </ul>
     *
     * @param optionalPortObjectConnection An optional port object connection.
     * @param loc The {@link FSLocation} to read.
     * @param expectedBytes The bytes that are expected to be read.
     * @param expectedPath The expected string representation of the resulting {@link FSPath} object.
     * @throws IOException
     */
    protected void testReadFSLocation(final Optional<FSConnection> optionalPortObjectConnection, final FSLocation loc,
        final byte[] expectedBytes, final String expectedPath) throws IOException {

        try (FSPathProviderFactory factory = FSPathProviderFactory.newFactory(optionalPortObjectConnection, loc)) {
            try (FSPathProvider pathProvider = factory.create(loc)) {
                final FSPath path = pathProvider.getPath();
                assertEquals(expectedPath, path.toString());
                assertEquals(loc, path.toFSLocation());
                assertArrayEquals(expectedBytes, Files.readAllBytes(path));
            }
        }
    }

    /**
     * Makes the following checks:
     *
     * <ul>
     * <li>Reads all bytes from the given {@link FSLocation} compares them to the given expected bytes.</li>
     * <li>Checks that {@link FSPath#toString()} of the resulting path equals expected path string.</li>
     * <li>Checks that {@link FSPath#toFSLocation()} of the resulting path equals the provided {@link FSLocation}, except that the path
     * part of the {@link FSLocation} must equals expectedFSLocationPath.</li>
     * </ul>
     *
     * @param loc The {@link FSLocation} to read.
     * @param expectedBytes The bytes that are expected to be read.
     * @param expectedPath The expected string representation of the resulting {@link FSPath} object.
     * @param expectedFSLocationPath The expected path part of {@link FSPath#toFSLocation()} of the resulting {@link FSPath} object.
     * @throws IOException
     */
    protected void testReadFSLocation(final FSLocation loc, final byte[] expectedBytes, final String expectedPath, final String expectedFSLocationPath) throws IOException {
        try (FSPathProviderFactory factory = FSPathProviderFactory.newFactory(Optional.empty(), loc)) {
            try (FSPathProvider pathProvider = factory.create(loc)) {
                final FSPath path = pathProvider.getPath();
                assertEquals(expectedPath, path.toString());

                final FSLocation pathLoc = path.toFSLocation();
                assertEquals(loc.getFSCategory(), pathLoc.getFSCategory());
                assertEquals(loc.getFileSystemSpecifier(), pathLoc.getFileSystemSpecifier());
                assertEquals(expectedFSLocationPath, pathLoc.getPath());

                assertArrayEquals(expectedBytes, Files.readAllBytes(path));
            }
        }
    }
}
