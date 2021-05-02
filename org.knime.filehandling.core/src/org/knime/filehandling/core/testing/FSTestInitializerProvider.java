package org.knime.filehandling.core.testing;

import java.io.IOException;
import java.util.Map;

import org.knime.filehandling.core.connections.FSConnection;
import org.knime.filehandling.core.connections.FSLocationSpec;
import org.knime.filehandling.core.connections.meta.FSType;

/**
 * The interface backing an extension point which allows any file system implementation to register a provider needed
 * for integration with our file system testing framework.
 *
 * A provider for a file system is responsible for reading all implementation specific properties from a configuration
 * map and to instantiate a {@link FSTestInitializer} using these properties.
 *
 * @author Tobias Urhaug, KNIME GmbH, Berlin, Germany
 * @noreference non-public API
 * @noimplement non-public API
 */
public interface FSTestInitializerProvider {

    /**
     * Default connection timeout for remote initializers.
     */
    public static final int CONNECTION_TIMEOUT = 30 * 1000;

    /**
     * Creates a {@link FSTestInitializer} configured for testing purposes, including opening an {@link FSConnection}.
     * The configuration map is implementation dependent.
     *
     * @param configuration Configuration of the test connection
     * @return a configured {@link FSTestInitializer}.
     * @throws IllegalArgumentException If configuration in the given map contained was invalid or unusable.
     * @throws IOException If something went wrong while opening the {@link FSConnection}.
     */
    public FSTestInitializer setup(Map<String, String> configuration) throws IOException;

    /**
     * Returns the file system type.
     *
     * @return the file system type
     */
    public FSType getFSType();

    /**
     * Creates the {@link FSLocationSpec} of the file system that would be created by {@link #setup(Map)}.
     * Implementations must not perform any network I/O in this method, hence it also does not throw an
     * {@link IOException}.
     *
     * @param configuration Configuration of the test connection.
     * @return {@link FSLocationSpec} of the file system that would be created by {@link #setup(Map)}.
     * @throws IllegalArgumentException If configuration in the given map contained was invalid or unusable.
     */
    public FSLocationSpec createFSLocationSpec(Map<String, String> configuration);

}
