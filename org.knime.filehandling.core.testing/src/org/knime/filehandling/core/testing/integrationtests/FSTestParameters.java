package org.knime.filehandling.core.testing.integrationtests;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Platform;
import org.knime.filehandling.core.testing.FSTestInitializer;
import org.knime.filehandling.core.testing.FSTestInitializerManager;
import org.knime.filehandling.core.testing.FSTestInitializerProvider;
import org.osgi.framework.Bundle;

/**
 * Helper class which parameterizes the {@link AbstractParameterizedFSTest} class.
 * 
 * Automatically detects all registered {@link FSTestInitializerProvider} implementations and uses them to initialize 
 * and configure corresponding {@link FSTestInitializer}. The configuration is a properties file read either from a 
 * location provided by the KNIME_FS_TEST_CONFIGURATION environment variable or the default 
 * resources/fs-test-config.properties file in this fragment.
 * 
 * @author Tobias Urhaug, KNIME GmbH, Berlin, Germany
 */
public class FSTestParameters {
	
	private static final String TEST_PROPERTIES_FILE_NAME = "fs-test-config.properties";
	private static final String TEST_PROPERTIES_ENV_VARIABLE_NAME = "KNIME_FS_TEST_CONFIGURATION";
	
	/**
	 * Returns a collection with a single two dimensional array, where each row in the array contains a file system 
	 * name (which is helpful for naming the parameterized tests) and the corresponding test initializer.
	 * 
	 * @return all registered test initializers in a format suitable for the Parameterized runner
	 */
	public static Collection<Object[]> get() {
		final FSTestInitializerManager manager = FSTestInitializerManager.instance();
		final List<String> testInitializerKeys = manager.getAllTestInitializerKeys();

		final int numberOfFS = testInitializerKeys.size();
		final Object[][] fsTestInitializers = new Object[numberOfFS][2];
		
		for (int i = 0; i < numberOfFS; i++) {
			final String fsType = testInitializerKeys.get(i);
			fsTestInitializers[i][0] = fsType;
			fsTestInitializers[i][1] = manager.createInitializer(fsType, readConfiguration(fsType));
		}
		
		return Arrays.asList(fsTestInitializers);
	}

	private static Map<String, String> readConfiguration(final String fsType) {
		final Properties testProperties = readProperties();

		final Map<String, String> configuration = new HashMap<>();
		@SuppressWarnings("unchecked")
		Enumeration<String> propertyNames = (Enumeration<String>) testProperties.propertyNames();
		while (propertyNames.hasMoreElements()) {
			final String key = propertyNames.nextElement();
			if (key.startsWith(fsType)) {
				final String value = testProperties.getProperty(key);
				final String keyWithoutFSType = key.replace(fsType + ".", "");
				configuration.put(keyWithoutFSType, value);
			}
		}
		
		if (configuration.isEmpty()) {
			final String errMsg = 
				String.format(
					"No properties for the file system '%s' found in the properties file '%s'.", 
					fsType, 
					TEST_PROPERTIES_FILE_NAME
				);
			throw new IllegalStateException(errMsg);
		}
		
		return configuration;
	}

	private static Properties readProperties() {
		final Path propertiesPath = getPropertiesPath();
		try (InputStream propertiesStream = Files.newInputStream(propertiesPath)) {
			final Properties properties = new Properties();
			properties.load(propertiesStream);
			return properties;
		} catch (IOException e) {
			throw new RuntimeIOException(String.format("Could not read properties from: '%s'", propertiesPath), e);
		}
	}

	private static Path getPropertiesPath() {
		return environmentVariablePropertiesPath().orElse(defaultPropertiesPath());
	}

	private static Optional<Path> environmentVariablePropertiesPath() {
		final String propertiesLocation = System.getenv(TEST_PROPERTIES_ENV_VARIABLE_NAME);
		if (propertiesLocation == null) {
			return Optional.empty();
		}
		
		return Optional.ofNullable(Paths.get(propertiesLocation + File.separator + TEST_PROPERTIES_FILE_NAME));
	}

	private static Path defaultPropertiesPath() {
		final Bundle bundle = Platform.getBundle("org.knime.filehandling.core.testing");
		final URL propertiesURL = bundle.getEntry("resources/" + TEST_PROPERTIES_FILE_NAME);
		try {
			return Paths.get(FileLocator.resolve(propertiesURL).toURI());
		} catch (URISyntaxException | IOException e1) {
			final String errMsg = 
				String.format("Could not locate a file '%s' in the resources folder.", TEST_PROPERTIES_FILE_NAME);
		    throw new IllegalStateException(errMsg);
		}
	}

}
