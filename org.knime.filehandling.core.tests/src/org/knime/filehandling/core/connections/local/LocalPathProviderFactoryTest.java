package org.knime.filehandling.core.connections.local;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import org.junit.Test;
import org.knime.filehandling.core.connections.FSCategory;
import org.knime.filehandling.core.connections.FSLocation;
import org.knime.filehandling.core.connections.location.FSPathProviderFactoryTestBase;

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
}
