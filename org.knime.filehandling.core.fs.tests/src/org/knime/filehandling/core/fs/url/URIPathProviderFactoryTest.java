package org.knime.filehandling.core.fs.url;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.filehandling.core.connections.FSCategory;
import org.knime.filehandling.core.connections.FSLocation;
import org.knime.filehandling.core.connections.FSPath;
import org.knime.filehandling.core.connections.location.FSPathProvider;
import org.knime.filehandling.core.connections.location.FSPathProviderFactory;
import org.knime.filehandling.core.fs.location.FSPathProviderFactoryTestBase;
import org.knime.filehandling.core.tests.common.workflow.WorkflowTestUtil;

/**
 * Unit tests that test FSLocation support for the Custom URL file system.
 *
 * @author Bjoern Lohrmann, KNIME GmbH
 */
public class URIPathProviderFactoryTest extends FSPathProviderFactoryTestBase {

    /**
     * Tests reading from a https:// URL.
     *
     * @throws IOException
     */
    @Test
    public void test_https_location() throws IOException {
        final String url = "https://update.knime.com/analytics-platform/4.2/index.html";

        final byte[] expectedBytes = readUrl(url);

        final FSLocation loc = new FSLocation(FSCategory.CUSTOM_URL, "5000", url);

        testReadFSLocation(Optional.empty(), loc, expectedBytes, "/analytics-platform/4.2/index.html");
    }

    private static byte[] readUrl(final String url) throws IOException, MalformedURLException {
        final byte[] expectedBytes;
        try (InputStream in = new URL(url).openConnection().getInputStream()) {
            expectedBytes = IOUtils.toByteArray(in);
        }
        return expectedBytes;
    }

    /**
     * Tests reading from a https:// URL with query and fragment.
     *
     * @throws IOException
     */
    @Test
    public void test_https_location_with_query_and_fragment() throws IOException {
        final String url = "https://update.knime.com/analytics-platform/4.2/index.html?bla=blub#section-1.1";

        final byte[] expectedBytes = readUrl(url);

        final FSLocation loc = new FSLocation(FSCategory.CUSTOM_URL, "1000", url);

        testReadFSLocation(Optional.empty(), loc, expectedBytes, "/analytics-platform/4.2/index.html");
    }

    /**
     * Tests reading from a https:// URL with query and fragment that have encoded characters (those must be preserved).
     *
     * @throws IOException
     * @throws URISyntaxException
     */
    @Test
    public void test_https_location_with_encoded_query_and_fragment() throws IOException, URISyntaxException {
        final String url = "https://knime.com/path/index.html?bla=%2B%3D%2F#%2B%3D%2F";
        final FSLocation loc = new FSLocation(FSCategory.CUSTOM_URL, "1000", url);

        try (FSPathProviderFactory factory = FSPathProviderFactory.newFactory(Optional.empty(), loc)) {
            try (FSPathProvider pathProvider = factory.create(loc)) {
                final FSPath path = pathProvider.getPath();

                assertEquals("/path/index.html", path.toString());
                assertEquals(loc, path.toFSLocation());
                assertEquals(URI.create(url), ((URIPath)path).getURI());


                // FileSystem.getPath(String) does not allow to pass query or fragment, only FileSystem.getPath(FSLocation) does
                // The query and fragment become part of the path and will be encoded
                final String otherPathString = "/path/index.html?bla=%2B%3D%2F#%2B%3D%2F";
                final FSPath otherPath = path.getFileSystem().getPath(otherPathString);
                assertEquals(otherPathString, otherPath.toString());
                assertEquals(new URI("https", "knime.com", otherPathString, null, null), ((URIPath)otherPath).getURI());
            }
        }

    }

    /**
     * Tests reading from a local file.
     *
     * @throws IOException
     */
    @Test
    public void test_local_file_location() throws IOException {

        final byte[] expectedBytes = "blablabla".getBytes();
        final Path localTestFile = m_tempFolder.newFile("testfile").toPath();
        Files.write(localTestFile, expectedBytes);

        final String url = localTestFile.toUri().toURL().toString();
        final FSLocation loc = new FSLocation(FSCategory.CUSTOM_URL, "1000", url);

        testReadFSLocation(loc, //
            expectedBytes, //
            localTestFile.toUri().getPath(), //
            localTestFile.toUri().toURL().toString());
    }

    /**
     * Tests reading from a local file with spaces in the path.
     *
     * @throws IOException
     */
    @Test
    public void test_local_file_location_with_unencoded_spaces() throws IOException {
        final byte[] expectedBytes = "blablabla".getBytes();
        final Path localTestFile = m_tempFolder.newFile("testfile with spaces").toPath();
        Files.write(localTestFile, expectedBytes);

        // try unencoded url with spaces
        // Note that in this case we will have a file: URL such as file:/tmp/bla...
        final String url = localTestFile.getParent().toUri().toURL().toString() + "testfile with spaces";
        final FSLocation loc = new FSLocation(FSCategory.CUSTOM_URL, "1000", url);

        testReadFSLocation(loc, //
            expectedBytes, //
            localTestFile.toUri().getPath(), //
            localTestFile.toUri().toURL().toString());
    }

    /**
     * Tests reading from a local file with encoded spaces in the path.
     *
     * @throws IOException
     */
    @Test
    public void test_local_file_location_with_encoded_spaces() throws IOException {
        final byte[] expectedBytes = "blablabla".getBytes();
        final Path localTestFile = m_tempFolder.newFile("testfile with spaces").toPath();
        Files.write(localTestFile, expectedBytes);

        // try encoded url with spaces
        final String url = localTestFile.toUri().toURL().toString();
        final FSLocation loc = new FSLocation(FSCategory.CUSTOM_URL, "1000", url);

        testReadFSLocation(Optional.empty(), //
            loc, //
            expectedBytes, //
            localTestFile.toUri().getPath());
    }

    /**
     * Tests reading from a local file using file:///path/to/file
     *
     * @throws IOException
     */
    @Test
    public void test_local_file_location_with_triple_slashes() throws IOException {
        final byte[] expectedBytes = "blablabla".getBytes();
        final Path localTestFile = m_tempFolder.newFile("testfile with spaces").toPath();
        Files.write(localTestFile, expectedBytes);

        // this provides a url like file:/path/to/file, but we want file:///path/to/file which should also work
        final String url = localTestFile.toUri().toURL().toString();
        final FSLocation loc = new FSLocation(FSCategory.CUSTOM_URL, "1000", url.replace("file:", "file://"));
        testReadFSLocation(Optional.empty(), //
            loc, //
            expectedBytes, //
            localTestFile.toUri().getPath());
    }

    /**
     * Tests reading from knime://knime.workflow/ URL with unencoded spaces.
     *
     * @throws IOException
     */
    @Test
    public void test_knime_workflow_relative_with_unencoded_spaces() throws IOException {
        final WorkflowManager workflowManager =
            WorkflowTestUtil.createAndLoadDummyWorkflow(m_tempFolder.newFolder("mountpoint-root").toPath());

        try {
            final byte[] expectedBytes = "bla".getBytes();
            final Path tmpFile =
                workflowManager.getContext().getMountpointRoot().toPath().resolve("tempfile with spaces");
            Files.write(tmpFile, expectedBytes);

            final FSLocation loc = new FSLocation(FSCategory.CUSTOM_URL, "1000",
                "knime://knime.workflow/../tempfile with spaces");

            testReadFSLocation(loc, expectedBytes, "../tempfile with spaces", "knime://knime.workflow/../tempfile%20with%20spaces");
        } finally {
            WorkflowTestUtil.shutdownWorkflowManager(workflowManager);
        }
    }

    /**
     * Tests reading from knime://knime.workflow/ URL with encoded spaces.
     *
     * @throws IOException
     */
    @Test
    public void test_knime_workflow_relative_with_encoded_spaces() throws IOException {
        final WorkflowManager workflowManager =
            WorkflowTestUtil.createAndLoadDummyWorkflow(m_tempFolder.newFolder("mountpoint-root").toPath());

        try {
            final byte[] expectedBytes = "bla".getBytes();
            final Path tmpFile =
                workflowManager.getContext().getMountpointRoot().toPath().resolve("tempfile with spaces");
            Files.write(tmpFile, expectedBytes);

            final FSLocation loc = new FSLocation(FSCategory.CUSTOM_URL, "1000",
                "knime://knime.workflow/../tempfile%20with%20spaces");

            testReadFSLocation(Optional.empty(), loc, expectedBytes, "../tempfile with spaces");
        } finally {
            WorkflowTestUtil.shutdownWorkflowManager(workflowManager);
        }
    }

    /**
     * Tests reading from knime://knime.mountpoint/ URL with unencoded spaces.
     *
     * @throws IOException
     */
    @Test
    public void test_knime_mountpoint_relative_with_unencoded_spaces() throws IOException {
        final WorkflowManager workflowManager =
            WorkflowTestUtil.createAndLoadDummyWorkflow(m_tempFolder.newFolder("mountpoint-root").toPath());

        try {
            final byte[] expectedBytes = "bla".getBytes();
            final Path tmpFile =
                workflowManager.getContext().getMountpointRoot().toPath().resolve("tempfile with spaces");
            Files.write(tmpFile, expectedBytes);

            final FSLocation loc = new FSLocation(FSCategory.CUSTOM_URL, "1000",
                "knime://knime.mountpoint/tempfile with spaces");

            testReadFSLocation(loc, expectedBytes, "tempfile with spaces", "knime://knime.mountpoint/tempfile%20with%20spaces");
        } finally {
            WorkflowTestUtil.shutdownWorkflowManager(workflowManager);
        }
    }

    /**
     * Tests reading from knime://knime.mountpoint/ URL with encoded spaces.
     *
     * @throws IOException
     */
    @Test
    public void test_knime_mountpoint_relative_with_encoded_spaces() throws IOException {
        final WorkflowManager workflowManager =
            WorkflowTestUtil.createAndLoadDummyWorkflow(m_tempFolder.newFolder("mountpoint-root").toPath());

        try {
            final byte[] expectedBytes = "bla".getBytes();
            final Path tmpFile =
                workflowManager.getContext().getMountpointRoot().toPath().resolve("tempfile with spaces");
            Files.write(tmpFile, expectedBytes);

            final FSLocation loc = new FSLocation(FSCategory.CUSTOM_URL, "1000",
                "knime://knime.mountpoint/tempfile%20with%20spaces");

            testReadFSLocation(Optional.empty(), loc, expectedBytes, "tempfile with spaces");
        } finally {
            WorkflowTestUtil.shutdownWorkflowManager(workflowManager);
        }
    }
}
