package org.knime.filehandling.core.connections.location;

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
    public TemporaryFolder m_tempFolder = new TemporaryFolder();

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
     * Reads all bytes from the given {@link FSLocation} and compares them to the given expected bytes. Also the string
     * representation of the resulting {@link FSPath} object is compared against the expected path string.
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
                assertArrayEquals(expectedBytes, Files.readAllBytes(path));
            }
        }
    }
}
