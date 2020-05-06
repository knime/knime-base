package org.knime.filehandling.core.testing.integrationtests;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.knime.filehandling.core.testing.FSTestInitializer;
import org.knime.filehandling.core.testing.FSTestInitializerManager;
import org.knime.filehandling.core.testing.FSTestInitializerProvider;

/**
 * Helper class which parameterizes the {@link AbstractParameterizedFSTest} class.
 * 
 * Automatically detects all registered {@link FSTestInitializerProvider} implementations and uses them to initialize
 * and configure corresponding {@link FSTestInitializer}. The configuration is a properties file resolved by {link
 * {@link FSTestPropertiesResolver}.
 * 
 * @author Tobias Urhaug, KNIME GmbH, Berlin, Germany
 */
public class FSTestParameters {

    /**
     * Returns a collection with a single two dimensional array, where each row in the array contains a file system name
     * (which is helpful for naming the parameterized tests) and the corresponding test initializer.
     * 
     * @return all registered test initializers in a format suitable for the Parameterized runner
     */
    public static Collection<Object[]> get() {
        final Properties fsTestProperties = FSTestPropertiesResolver.forIntegrationTests();

        final FSTestInitializerManager manager = FSTestInitializerManager.instance();
        final List<String> testInitializerKeys = new ArrayList<>();
        if (fsTestProperties.containsKey("test-fs")) {
            testInitializerKeys.add(fsTestProperties.getProperty("test-fs"));
        } else {
            testInitializerKeys.addAll(manager.getAllTestInitializerKeys());
        }

        final int numberOfFS = testInitializerKeys.size();
        final Object[][] fsTestInitializers = new Object[numberOfFS][2];

        for (int i = 0; i < numberOfFS; i++) {
            final String fsType = testInitializerKeys.get(i);
            fsTestInitializers[i][0] = fsType;
            fsTestInitializers[i][1] =
                manager.createInitializer(fsType, FSTestConfigurationReader.read(fsType, fsTestProperties));
        }

        return Arrays.asList(fsTestInitializers);
    }
}
