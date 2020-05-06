package org.knime.filehandling.core.connections.knimeremote;

import static org.junit.Assert.assertEquals;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import org.junit.Before;
import org.junit.Test;

public class KNIMERemotePathTest {

    private KNIMERemoteFileSystemProvider m_fsProvider;

    private KNIMERemoteFileSystem m_fs;

    @Before
    public void setup() {
        m_fsProvider = new KNIMERemoteFileSystemProvider();
        m_fs = new KNIMERemoteFileSystem(m_fsProvider, URI.create("knime://LOCAL"), false);
    }

    @Test
    public void get_url_when_hash_sign_in_path() throws URISyntaxException {
        get_url_from_path("/somepathwith#hashsign");
    }

    @Test
    public void get_url_when_hash_signs_in_path() throws URISyntaxException {
        get_url_from_path("/some#path#with#hash#signs");
    }

    private void get_url_from_path(String path) throws URISyntaxException {
        final KNIMERemotePath knimePath = new KNIMERemotePath(m_fs, path);
        final URL url = knimePath.toURL();
        final URI uri = url.toURI();
        assertEquals(path, uri.getPath());
    }

}
