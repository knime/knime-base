package org.knime.filehandling.core.testing.integrationtests;

import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * This class is responsible for reading the test configuration for a specific file system and put all the properties in
 * a map.
 * 
 * @author Tobias Urhaug, KNIME GmbH, Berlin, Germany
 *
 */
public class FSTestConfigurationReader {

    @SuppressWarnings("unchecked")
    /**
     * Reads all the properties with prefix fsType into a map. The suffix of each property will be removed from the keys
     * in the map.
     * 
     * @param fsType         the file system type for which the configuration is read
     * @param testProperties the test properties
     * @return a map containing all the fsType specific properties
     */
    public static Map<String, String> read(final String fsType, Properties testProperties) {
        final Map<String, String> configuration = new HashMap<>();
        final String fsTypePrefix = fsType + ".";

        for (String key : Collections.list((Enumeration<String>)testProperties.propertyNames())) {
            if (key.startsWith(fsTypePrefix)) {
                final String value = testProperties.getProperty(key);
                final String keyWithoutFSType = key.substring(fsTypePrefix.length());
                configuration.put(keyWithoutFSType, value);
            }
        }

        if (configuration.isEmpty()) {
            final String errMsg = String.format(//
                "No properties for the file system '%s' found in the properties file '%s'.", //
                fsType, //
                FSTestPropertiesResolver.TEST_PROPERTIES_FILE_NAME //
            );
            throw new IllegalStateException(errMsg);
        }

        return configuration;
    }

}
