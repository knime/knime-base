package org.knime.filehandling.core.connections;

import java.io.IOException;

import org.knime.core.node.NodeLogger;
import org.knime.core.node.util.FileSystemBrowser;

/**
 * Interface for file system connections.
 *
 * @author Tobias Urhaug, KNIME GmbH, Berlin, Germany
 * @noreference non-public API
 * @noimplement non-public API
 */
public interface FSConnection extends AutoCloseable {

    /**
     * Closes the file system in this connection and releases any resources allocated by it using a background thread.
     * Any errors during closing will be logged to the {@link NodeLogger} of the {@link FSConnection} implementation.
     *
     * @since 4.2
     */
    public default void closeInBackground() {

        final NodeLogger logger = NodeLogger.getLogger(this.getClass());
        final String fsName = getClass().getSimpleName();

        final String threadName = fsName + "Closer";

        final Runnable closeRunnable = () -> {
            try {
                FSConnection.this.close();
            } catch (Exception ex) {
                logger.error(String.format("Exception closing %s: %s", fsName, ex.getMessage()));
            }
        };

        new Thread(closeRunnable, threadName).start();
    }

    /**
     * Closes the file system in this connection and releases any resources allocated by it.
     *
     * @throws IOException when something went wrong while closing the file system.
     * @since 4.2
     */
    @SuppressWarnings("resource")
    @Override
    public default void close() throws IOException {
        try {
            getFileSystem().ensureClosed();
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
