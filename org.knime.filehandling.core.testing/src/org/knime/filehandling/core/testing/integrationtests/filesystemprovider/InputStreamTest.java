package org.knime.filehandling.core.testing.integrationtests.filesystemprovider;

import static org.junit.Assert.assertEquals;

import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.knime.filehandling.core.testing.FSTestInitializer;
import org.knime.filehandling.core.testing.integrationtests.AbstractParameterizedFSTest;

/**
 * Test class for input streams operations on file systems.
 * 
 * @author Tobias Urhaug, KNIME GmbH, Berlin, Germany
 *
 */
public class InputStreamTest extends AbstractParameterizedFSTest {

    public InputStreamTest(String fsType, FSTestInitializer testInitializer) {
        super(fsType, testInitializer);
    }

    @Test
    public void test_read_from_input_stream() throws Exception {
        String testContent = "This is read by an input stream!!";
        Path file = m_testInitializer.createFileWithContent(testContent, "dir", "fileName");

        String result;
        try (InputStream inputStream = Files.newInputStream(file)) {
            result = IOUtils.toString(inputStream, Charset.defaultCharset());
        }

        assertEquals(testContent, result);
    }

    @Test(expected = NoSuchFileException.class)
    public void test_read_from_input_stream_non_existing_file() throws Exception {
        Path file = m_testInitializer.createFile("dir", "fileName");
        Path nonExistingFile = file.getParent().resolve("non-existing");
        try (InputStream inputStream = Files.newInputStream(nonExistingFile)) {

        }
    }
}
