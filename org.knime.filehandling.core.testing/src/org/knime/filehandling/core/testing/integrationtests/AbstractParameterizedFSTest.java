package org.knime.filehandling.core.testing.integrationtests;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.knime.filehandling.core.connections.FSConnection;
import org.knime.filehandling.core.testing.FSTestInitializer;

/**
 * The parent class of the test suite in our file system testing framework. All
 * test classes which extend this class will automatically be parameterized by
 * all registered {@link FSTestInitializer} and thus be tested on all file
 * system implementations.
 *
 * @author Tobias Urhaug, KNIME GmbH, Berlin, Germany
 */
@RunWith(Parameterized.class)
public abstract class AbstractParameterizedFSTest {

    protected static final String LOCAL = "local";
    protected static final String S3 = "s3";

    @Parameters(name = "File System: {0}")
    public static Collection<Object[]> allFileSystemTestInitializers() {
        return FSTestParameters.get();
    }

    @Before
    public void beforeTestCase() throws IOException {
        m_testInitializer.beforeTestCase();
    }

    @After
    public void afterTestCase() throws IOException {
        m_testInitializer.afterTestCase();
    }

    protected final FSTestInitializer m_testInitializer;
    protected final FSConnection m_connection;
    protected final String m_fsType;

    /**
     * Creates a new instance for a file system.
     * 
     * @param fsType
     *            the file system type
     * @param testInitializer
     *            the initializer for the file system
     */
    public AbstractParameterizedFSTest(final String fsType, final FSTestInitializer testInitializer) {
        m_testInitializer = testInitializer;
        m_connection = m_testInitializer.getFSConnection();
        m_fsType = fsType;
    }

    /**
     * Ignores a test case for the specified file systems.
     * 
     * This is helpful in the case where a general test case is true for most file
     * system, but not all. Every file system given as a parameter will be ignored.
     * 
     * @param fileSystems
     *            the file systems to be ignored for this test case
     */
    public void ignore(final String... fileSystems) {
        final boolean shouldBeIgnored = Arrays.stream(fileSystems).anyMatch(fileSystem -> fileSystem.equals(m_fsType));
        final String errMsg = String.format("Test case has been ignored for the file system '%s'", m_fsType);
        Assume.assumeFalse(errMsg, shouldBeIgnored);
    }

    /**
     * Ignores a test case for the specified file system and provides a reason why.
     * 
     * This is helpful in the case where a general test case is true for most file
     * systems, but not all.
     * 
     * @param fileSystem
     *            the file system to be ignored for this test case
     */
    public void ignoreWithReason(final String reason, final String fileSystem) {
        final boolean shouldBeIgnored = fileSystem.equals(m_fsType);
        Assume.assumeFalse(reason, shouldBeIgnored);
    }

    /**
     * Ignores a test case for all FileSystems except the specified file systems.
     *
     * This is helpful in the case where a general test case is true for most file
     * system, but not all. Every file system that is not given as a parameter will
     * be ignored.
     *
     * @param fileSystems
     *            the file systems to be ignored for this test case
     */
    public void ignoreAllExcept(final String... fileSystems) {
        final boolean shouldBeIgnored = !Arrays.stream(fileSystems).anyMatch(fileSystem -> fileSystem.equals(m_fsType));
        final String errMsg = String.format("Test case has been ignored for the file system '%s'", m_fsType);
        Assume.assumeFalse(errMsg, shouldBeIgnored);
    }

}
