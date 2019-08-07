package org.knime.filehandling.core.filechooser;

import java.io.IOException;
import java.net.URISyntaxException;

import javax.swing.JFileChooser;

import org.junit.Test;
import org.knime.filehandling.core.connections.S3Connection;

public class FSVTest {

	@Test
	public void createFileSystemView() throws IOException, URISyntaxException {
		final S3Connection conn = new S3Connection();
		final NioFileSystemView nio = new NioFileSystemView(conn);
		final JFileChooser fchooser = new JFileChooser(nio);
		fchooser.showSaveDialog(null);
	}
}
