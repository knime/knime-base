/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ---------------------------------------------------------------------
 *
 * History
 *   Jul 23, 2019 (Tobias Urhaug, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.filehandling.core.connections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.nio.file.FileSystem;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.junit.Test;
import org.knime.filehandling.core.connections.FSConnection;
import org.knime.filehandling.core.connections.FSConnectionRegistry;

/**
 * Test suite for the {@link FSConnectionRegistry}.
 * 
 * @author Tobias Urhaug, KNIME GmbH, Berlin, Germany
 */
public class FSConnectionRegistryTest {

	/**
	 * Tests that an instance is not null.
	 */
    @Test
    public void a_registry_instance_is_not_null() {
        assertTrue(FSConnectionRegistry.getInstance() != null);
    }

    /**
     * Tests registering a null connection.
     */
    @Test (expected = NullPointerException.class)
    public void registering_a_null_connection_is_not_allowed() {
    	FSConnectionRegistry connections = FSConnectionRegistry.getInstance();
    	connections.register(null);
    }
    
    /**
     * Tests that registering a key gives a key back.
     */
    @Test
    public void a_registered_connection_gets_a_key() {
    	FSConnectionRegistry connections = FSConnectionRegistry.getInstance();
    	TestConnection testConnection = new TestConnection();
    	assertTrue(connections.register(testConnection) != null);
    }
    
    /**
     * Tests that a registry contains a registered connection.
     */
    @Test
    public void the_registry_contains_a_registered_connection() {
    	FSConnectionRegistry connections = FSConnectionRegistry.getInstance();
    	TestConnection testConnection = new TestConnection();

    	String key = connections.register(testConnection);
    	
		assertTrue(connections.contains(key));
    }
    
    /**
     * Tests that a registered connection can be retrieved with its key.
     */
    @Test
    public void a_registered_connection_can_be_retrieved_by_its_key() {
    	FSConnectionRegistry connections = FSConnectionRegistry.getInstance();
    	TestConnection testConnection = new TestConnection();
    	
    	String key = connections.register(testConnection);
    	
    	assertEquals(testConnection, connections.retrieve(key).get());
    }
    
    /**
     * Tests that multiple registered connections have different keys.
     */
    @Test
    public void registered_connections_have_unique_keys() {
    	FSConnectionRegistry connections = FSConnectionRegistry.getInstance();
    	TestConnection testConnection1 = new TestConnection();
    	TestConnection testConnection2 = new TestConnection();
    	TestConnection testConnection3 = new TestConnection();
    	
    	String key1 = connections.register(testConnection1);
    	String key2 = connections.register(testConnection2);
    	String key3 = connections.register(testConnection3);
    	
    	Set<String> keySet = new HashSet<>(Arrays.asList(key1, key2, key3));

    	assertTrue(keySet.size() == 3);
    }
    
    /**
     * Tests that the keys retrive the correct connections.
     */
    @Test
    public void multiple_registered_connections_can_be_retrieved_by_their_keys() {
    	FSConnectionRegistry connections = FSConnectionRegistry.getInstance();
    	TestConnection testConnection1 = new TestConnection();
    	TestConnection testConnection2 = new TestConnection();
    	
    	String key1 = connections.register(testConnection1);
    	String key2 = connections.register(testConnection2);
    	
    	assertEquals(testConnection1, connections.retrieve(key1).get());
    	assertEquals(testConnection2, connections.retrieve(key2).get());
    }
    
    /**
     * Tests that a connection object cannot be registered more than once.
     */
    @Test
    public void a_connection_can_only_be_registered_once() {
    	FSConnectionRegistry connections = FSConnectionRegistry.getInstance();
    	TestConnection testConnection = new TestConnection();
    	
    	String key = connections.register(testConnection);
    	String keyWhenAlreadyRegistered = connections.register(testConnection);
    	
    	assertTrue(key == keyWhenAlreadyRegistered);
    }

    /**
     * Tests that retrieving a connection for a key not in the registry returns an empty optional.
     */
    @Test
    public void retrieving_a_key_not_in_the_registry_returns_empty_optional() {
    	FSConnectionRegistry connections = FSConnectionRegistry.getInstance();
    	
    	Optional<FSConnection> connection = connections.retrieve("not in registry");
    	
    	assertFalse(connection.isPresent());
    }
    
    /**
     * Tests that a registered connection can be deregistered.
     */
    @Test
    public void a_registered_connection_can_be_deregistered() {
    	FSConnectionRegistry connections = FSConnectionRegistry.getInstance();
    	TestConnection testConnection = new TestConnection();
    	
    	String key = connections.register(testConnection);
    	connections.deregister(key);
    	
    	assertFalse(connections.contains(key));
    }
    
    /**
     * Tests that deregistering a non existing connection returns null.
     */
    @Test
    public void deregistering_a_key_not_in_the_registry_returns_null() {
    	FSConnectionRegistry connections = FSConnectionRegistry.getInstance();
    	
    	FSConnection connection = connections.deregister("a not registered key");
    	
    	assertEquals(null, connection);
    }
    
    private static class TestConnection implements FSConnection {

		@Override
		public FileSystem getFileSystem() {
			return null;
		}
    	
    }

}
