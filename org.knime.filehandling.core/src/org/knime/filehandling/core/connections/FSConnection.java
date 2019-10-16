package org.knime.filehandling.core.connections;

import java.nio.file.FileSystem;

import org.knime.core.node.util.FileSystemBrowser;

/**
 * Interface for file system connections.
 *
 * @author Tobias Urhaug, KNIME GmbH, Berlin, Germany
 */
public interface FSConnection {

	/**
	 * Returns a file system for this connection.
	 *
	 * @return a file system for this connection
	 */
	public FileSystem getFileSystem();

	/**
	 * Returns a file system browser for this connection.
	 *
	 * @return a file system browser for this connection
	 */
	public FileSystemBrowser getFileSystemBrowser();

}
