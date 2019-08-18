package org.knime.filehandling.core.filechooser;

import java.io.IOException;
import java.net.URISyntaxException;

import org.junit.Test;
import org.knime.core.node.util.FileSystemBrowser;
import org.knime.filehandling.core.connections.S3Connection;
import org.knime.filehandling.core.connections.S3FileSystemBrowser;

public class FSVTest {

	@Test
	public void createFileSystemView() throws IOException, URISyntaxException {
		// final S3Connection conn = new S3Connection();
		// final NioFileSystemView nio = new NioFileSystemView(conn);
		// final JFileChooser fchooser = new JFileChooser(nio);
		// fchooser.showSaveDialog(null);
		final S3Connection conn = new S3Connection();
		final S3FileSystemBrowser browser = conn.getFileSystemBrowser();
		browser.openDialogAndGetSelectedFileName(FileSystemBrowser.FileSelectionMode.FILES_AND_DIRECTORIES,
				FileSystemBrowser.DialogType.OPEN_DIALOG, null, null, "", new String[] { "" });
	}
}
