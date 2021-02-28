package org.knime.filehandling.core.connections;

import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.Set;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.util.FileSystemBrowser;
import org.knime.filehandling.core.connections.uriexport.PathURIExporter;
import org.knime.filehandling.core.connections.uriexport.URIExporter;
import org.knime.filehandling.core.connections.uriexport.URIExporterID;
import org.knime.filehandling.core.connections.uriexport.URIExporterIDs;
import org.knime.filehandling.core.connections.uriexport.URIExporterMapBuilder;

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
                logger.error(String.format("Exception closing %s: %s", fsName, ex.getMessage()), ex);
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

    /**
     * Returns whether this file system supports browsing. If this method returns false, then,
     * {@link #getFileSystemBrowser()} will return null.
     *
     * @return true if this file system supports browsing, false otherwise.
     */
    default boolean supportsBrowsing() {
        return true;
    }

    /**
     * Returns a default {@link URIExporter} or {@code null} if there is no {@link URI} representation of this
     * connection.
     *
     * @return default exporter or {@code null}
     */
    default URIExporter getDefaultURIExporter() {
        return getURIExporters().get(URIExporterIDs.DEFAULT);
    }

    /**
     * Returns all supported {@link URIExporter} of this connection by identifier.
     *
     * @return map supported exporters by identifier
     */
    default Map<URIExporterID, URIExporter> getURIExporters() {
        return new URIExporterMapBuilder() //
            .add(URIExporterIDs.DEFAULT, PathURIExporter.getInstance()) //
            .build();
    }

    /**
     * Export the relevant Panel for each URIExporter. This panel will contain different settings required to setup
     * different URIExporters
     *
     * @return URIExporterPanel of the URIExporter
     */
    default Set<URIExporterID> getURIExporterIDs() {
        return getURIExporters().keySet();
    }

    /**
     * Return the URI Exporter from the URIExporter ID
     *
     * @param uriExporterID URIExporterID object for the requested URIExporter
     *
     * @return URIExporter A URIExporter object
     */
    default URIExporter getURIExporter(final URIExporterID uriExporterID) {
        return getURIExporters().get(uriExporterID);
    }

    /**
     * Return the URI Exporter from the URIExporter ID and load the settings
     *
     * @param uriExporterID URIExporterID object for the requested URIExporter
     * @param settings NodeSettingsRO object to load settings for the URIExporter
     *
     * @return URIExporter A URIExporter object
     * @throws NotConfigurableException Thrown by enclosed in loadSettingsForModel
     * @throws InvalidSettingsException Thrown by enclosed in validateSettings
     */
    default URIExporter getURIExporter(final URIExporterID uriExporterID, final NodeSettingsRO settings)
        throws NotConfigurableException, InvalidSettingsException {
        final URIExporter uriExporter = getURIExporters().get(uriExporterID);

        if (settings != null) {
            uriExporter.validateSettings(settings);
            uriExporter.loadSettingsForModel(settings);
        }
        return uriExporter;
    }

}
