package org.knime.filehandling.connections;

import java.nio.file.FileSystem;

/**
 * Interface for file system connections.
 * 
 * @author Tobias Urhaug, KNIME GmbH, Berlin, Germany
 */
public interface FSConnection {
	
	public FileSystem getFileSystem();

}
