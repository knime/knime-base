package org.knime.filehandling.core.connections;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.apache.commons.lang3.Validate;

/**
 * A registry for file system connections.
 *
 * @author Tobias Urhaug, KNIME GmbH, Berlin, Germany
 */
public class FSConnectionRegistry {

	private static FSConnectionRegistry INSTANCE;

	private final Map<String, FSConnection> m_connections;

	private FSConnectionRegistry() {
		m_connections = new HashMap<>();
	}

	/**
	 * Returns the instance of this registry.
	 *
	 * @return the instance of this registry
	 */
	public static synchronized FSConnectionRegistry getInstance() {
		if (INSTANCE == null) {
			INSTANCE = new FSConnectionRegistry();
		}
		return INSTANCE;
	}

	/**
	 * Registers a connection to the registry and returns its unique key.
	 * @param key the unique key for the session
	 * @param connection the connection to be registered
	 * @see FSConnectionRegistry#getKey()
	 */
	public synchronized void register(final String key, final FSConnection connection) {
		Validate.notNull(key, "key not allowed to be null");
		Validate.notNull(connection, "Connection not allowed to be null");
		final FSConnection existingConnection = m_connections.get(key);
		if (existingConnection != null) {
		    if (!existingConnection.equals(connection)) {
		        throw new IllegalArgumentException("Different connection with key: " + key + " already exists");
		    }
		    return;
		}
		m_connections.put(key, connection);
	}

	/**
	 * @return a new unique key that can be used to register a {@link FSConnection}
	 */
	public String getKey() {
	    return UUID.randomUUID().toString();
	}

	/**
	 * Retrieve a connection for the given key.
	 *
	 * @param key key for the connection to be retrieved
	 * @return Optional containing the connection, if present
	 */
	public synchronized Optional<FSConnection> retrieve(final String key) {
		return Optional.ofNullable(m_connections.get(key));
	}

	/**
	 * Deregister connection from the registry.
	 *
	 * @param key key for the connection to be deregistered
	 * @return the connection that has been deregistered, null if the key is not in the registry.
	 */
	public synchronized FSConnection deregister(final String key) {
		return m_connections.remove(key);
	}

	/**
	 * Checks if the provided key has a connection in the registry.
	 *
	 * @param key key for the connection
	 * @return true if the key is in the registry
	 */
	public synchronized boolean contains(final String key) {
		return m_connections.containsKey(key);
	}

}
