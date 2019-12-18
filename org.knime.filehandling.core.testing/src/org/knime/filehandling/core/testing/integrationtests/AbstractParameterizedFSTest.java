package org.knime.filehandling.core.testing.integrationtests;

import java.util.Collection;

import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.knime.filehandling.core.connections.FSConnection;
import org.knime.filehandling.core.testing.FSTestInitializer;

/**
 * The parent class of the test suite in our file system testing framework. All test classes which extend this class 
 * will automatically be parameterized by all registered {@link FSTestInitializer} and thus be tested on all file system 
 * implementations.
 * 
 * @author Tobias Urhaug, KNIME GmbH, Berlin, Germany
 */
@RunWith(Parameterized.class)
public abstract class AbstractParameterizedFSTest {
	
	@Parameters(name = "File System: {0}")
	public static Collection<Object[]> allFileSystemTestInitializers() {
		return FSTestParameters.get();
	}
	
	@Before
	public void beforeTestCase() {
		m_testInitializer.beforeTestCase();
	}
	
	@After
	public void afterTestCase() {
		m_testInitializer.afterTestCase();
	}
	
	protected final FSTestInitializer m_testInitializer;
	protected final FSConnection m_connection;
	
	/**
	 * Creates a new instance for a file system.
	 * 
	 * @param fsType the file system type
	 * @param testInitializer the initializer for the file system
	 */
	public AbstractParameterizedFSTest(final String fsType, final FSTestInitializer testInitializer) {
		m_testInitializer = testInitializer;
		m_connection = m_testInitializer.getFSConnection();
	}
	
}
