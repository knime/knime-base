package org.knime.filehandling.core.testing.integrationtests;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Stream;

import org.eclipse.core.runtime.Platform;

/**
 * Resolves the test properties file for our file system tests.
 * 
 * @author Tobias Urhaug, KNIME GmbH, Berlin, Germany
 *
 */
public class FSTestPropertiesResolver {

    protected static final String TEST_PROPERTIES_FILE_NAME = "fs-test.properties";
    private static final String TEST_PROPERTIES_ENV_VARIABLE_NAME = "KNIME_FS_TEST_PROPERTIES";
    private final static String DEFAULT_PATH_WORKSPACE = 
            Platform.getLocation().append("/" + TEST_PROPERTIES_FILE_NAME).toString();

    /**
     * Resolves the properties for integration tests from the environment variable 
     * {@link TEST_PROPERTIES_ENV_VARIABLE_NAME}
     * 
     * @return the test properties
     */
    public static Properties forIntegrationTests() {
        final Path propertiesPath = 
                environmentVariablePath()
                .orElseThrow(() -> new IllegalStateException("The env variable '" + TEST_PROPERTIES_ENV_VARIABLE_NAME + 
                        "' must be set, pointing to a properties file containing the FS test configration."));
        return readProperties(propertiesPath);
    }

    /**
     * Resolves the properties for workflow tests from one of the following locations, in the given order: <br>
     * <br>
     *  - The workspace root.
     *  - The environment variable {@link TEST_PROPERTIES_ENV_VARIABLE_NAME}.
     * 
     * @return the test properties
     */
    public static Properties forWorkflowTests() {
        final Path propertiesPath = 
                findFirst(
                        Stream.of(
                                workspaceRootPath(), 
                                environmentVariablePath()
                                )
                        );
        return readProperties(propertiesPath);
    }

    private static Path findFirst(Stream<Optional<Path>> locations) {
        Optional<Path> path = 
                locations
                .filter(Optional::isPresent)
                .map(Optional::get)
                .findFirst();

        return path.orElseThrow(() -> new IllegalStateException("Could not find fs fs-test.properties file."));
    }

    private static Properties readProperties(final Path propertiesPath) {
        try (InputStream propertiesStream = Files.newInputStream(propertiesPath)) {
            final Properties properties = new Properties();
            properties.load(propertiesStream);
            return properties;
        } catch (IOException e) {
            throw new UncheckedIOException(String.format("Could not read properties from: '%s'", propertiesPath), e);
        }
    }

    private static Optional<Path> workspaceRootPath() {
        final File testPropertiesInWorkspace = new File(DEFAULT_PATH_WORKSPACE);
        if (testPropertiesInWorkspace.exists() && testPropertiesInWorkspace.canRead()) {
            return Optional.of(testPropertiesInWorkspace.toPath());
        }
        return Optional.empty();
    }

    private static Optional<Path> environmentVariablePath() {
        final String envVariableValue = System.getenv(TEST_PROPERTIES_ENV_VARIABLE_NAME);
        
        if (envVariableValue == null) {
            return Optional.empty();
        }
                
        final File testPropertiesFromEnv = new File(envVariableValue);
        if (testPropertiesFromEnv.exists() && testPropertiesFromEnv.canRead()) {
            return Optional.of(testPropertiesFromEnv.toPath());
        }
        
        return Optional.empty();
    }

}
