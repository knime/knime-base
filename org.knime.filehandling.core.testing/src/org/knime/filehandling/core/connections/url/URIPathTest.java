package org.knime.filehandling.core.connections.url;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.Collections;

import org.junit.Before;
import org.junit.Test;

public class URIPathTest {

	private URIFileSystemProvider m_fsProvider;

	@Before
	public void setup() {
		m_fsProvider = new URIFileSystemProvider(1000);
	}

	@Test
	public void resolveRelativePath() throws URISyntaxException, IOException {
		final String scheme = "knime";
		final String authority = "knime.mountpoint";
		final String absolute = "/absolute";
		final String relative = "relative";
		final URI uri = new URI(scheme, authority, absolute, null, null);
		final URIPath uriPath = new URIPath(m_fsProvider.getOrCreateFileSystem(uri, Collections.emptyMap()), uri);
		final Path resolved = uriPath.resolve(relative);
		assertEquals(resolved.toString(), String.format("%s://%s%s/%s", scheme, authority, absolute, relative));
	}

}
