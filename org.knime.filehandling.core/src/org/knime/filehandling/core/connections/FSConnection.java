package org.knime.filehandling.core.connections;

import java.io.IOException;

import org.knime.core.node.util.FileSystemBrowser;

/**
 * Interface for file system connections.
 *
 * @author Tobias Urhaug, KNIME GmbH, Berlin, Germany
 * @since 4.1
 */
public interface FSConnection extends AutoCloseable {

    /**
     * Closes the file system in this connection and releases any resources allocated by it.
     * @since 4.2
     */
    @Override
    public default void close() {
        try (FSFileSystem<?> fileSystem = getFileSystem()) {
            fileSystem.ensureClosed();
            FSConnectionRegistry.getInstance().deregister(this);
        } catch (IOException ex) {
            // nothing to do here
        }
    }

    /**
     * Returns a file system for this connection.
     *
     * @return a file system for this connection
     */
    public FSFileSystem<?> getFileSystem();

    /**
     * Returns a file system browser for this connection.
     *
     * @return a file system browser for this connection
     */
    public FileSystemBrowser getFileSystemBrowser();
}
