package org.knime.filehandling.core.testing;

import java.util.Map;

/**
 * The interface backing an extension point which allows any file system implementation to register a provider needed
 * for integration with our file system testing framework.
 *
 * A provider for a file system is responsible for reading all implementation specific properties from a configuration
 * map and to instantiate a {@link FSTestInitializer} using these properties.
 *
 * @author Tobias Urhaug, KNIME GmbH, Berlin, Germany
 */
public interface FSTestInitializerProvider {

    /**
     * Default connection timeout for remote initializers.
     */
    public static final int CONNECTION_TIMEOUT = 30 * 1000;

	/**
     * Creates a {@link FSTestInitializer} configured for testing purposes. The configuration map is implementation
     * dependent.
     *
     * @param configuration configuration of the test connection
     * @return a configured FSConnection
     */
    public FSTestInitializer setup(Map<String, String> configuration);

    /**
     * Returns the file system type.
     *
     * @return the file system type
     */
    public String getFSType();

}
