package org.knime.filehandling.core.connections;

import java.io.IOException;

import org.knime.core.node.NodeLogger;
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
     *
     * @since 4.2
     */
    @SuppressWarnings("resource")
    @Override
    public default void close() {
        try {
            getFileSystem().ensureClosed();
        } catch (IOException ex) {
            NodeLogger.getLogger(this.getClass()).error("Exception closing file system: " + ex.getMessage(), ex);
        } finally {
            FSConnectionRegistry.getInstance().deregister(this);
        }
    }

    /**
     * Returns a file system for this connection.
     *
     * @return a file system for this connection
     */
    FSFileSystem<?> getFileSystem();

    /**
     * Returns a file system browser for this connection.
     *
     * @return a file system browser for this connection
     */
    FileSystemBrowser getFileSystemBrowser();
}
