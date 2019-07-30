package org.knime.filehandling.connections;

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
	 *
	 * @param connection the connection to be registered
	 * @return a generated key for the registration
	 */
	public synchronized String register(final FSConnection connection) {
		Validate.notNull(connection, "Connection not allowed to be null");
		return keyOf(connection).orElse(registerInternal(connection));
	}

	private Optional<String> keyOf(final FSConnection connection) {
		return
			m_connections.entrySet() //
				.stream() //
				.filter(entry -> entry.getValue() == connection) //
				.findFirst() //
				.map(entry -> entry.getKey()); //
	}

	private String registerInternal(final FSConnection connection) {
		String key = UUID.randomUUID().toString();
		m_connections.put(key, connection);
		return key;
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
	 * Deregisters a connection from the registry.
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
