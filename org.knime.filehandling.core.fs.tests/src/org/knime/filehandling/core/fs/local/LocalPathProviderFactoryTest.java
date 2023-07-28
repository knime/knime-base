package org.knime.filehandling.core.fs.local;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import org.junit.Test;
import org.knime.filehandling.core.connections.DefaultFSConnectionFactory;
import org.knime.filehandling.core.connections.DefaultFSLocationSpec;
import org.knime.filehandling.core.connections.FSCategory;
import org.knime.filehandling.core.connections.FSLocation;
import org.knime.filehandling.core.connections.FSLocationFactory;
import org.knime.filehandling.core.connections.location.FSPathProviderFactory;
import org.knime.filehandling.core.fs.location.FSPathProviderFactoryTestBase;

/**
 * Unit tests that test FSLocation support for the (local) platform default file system.
 *
 * @author Bjoern Lohrmann, KNIME GmbH
 */
public class LocalPathProviderFactoryTest extends FSPathProviderFactoryTestBase {

    /**
     * Tests reading a local file.
     *
     * @throws IOException
     */
    @Test
    public void test_local_fs_location() throws IOException {

        final byte[] bytesToWrite = "bla".getBytes();
        final Path tmpFile = m_tempFolder.newFile("tempfile").toPath();
        Files.write(tmpFile, bytesToWrite);

        final FSLocation loc = new FSLocation(FSCategory.LOCAL, tmpFile.toString());

        testReadFSLocation(Optional.empty(), loc, bytesToWrite);
    }

    /**
     * Tests resolving a local FSLocation to a local path.
     *
     * @throws IOException
     */
    @Test
    public void testResolveToLocal() throws IOException {
        try (final var conn = DefaultFSConnectionFactory.createLocalFSConnection()) {
            final var factory = new FSLocationFactory(new DefaultFSLocationSpec(FSCategory.LOCAL), Optional.of(conn));
            final var fsLocation = factory.createLocation("foo");

            try (final var pathFactory = FSPathProviderFactory.newFactory(Optional.empty(), fsLocation);
                    final var pathProvider = pathFactory.create(fsLocation);
                    final var fs = conn.getFileSystem()) {

                    final var fsPath = fs.getPath(fsLocation).toAbsolutePath();
                    final var resolvedPath = pathProvider.getPath().resolveToLocal();
                    assertTrue("LOCAL FSLocation should resolve to local path", resolvedPath.isPresent());
                    assertEquals("LOCAL FSLocation should already be local", fsPath,
                        resolvedPath.get());
            }
        }
    }
}
