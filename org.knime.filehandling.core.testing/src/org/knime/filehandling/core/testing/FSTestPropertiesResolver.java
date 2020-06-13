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
 */
package org.knime.filehandling.core.testing;

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

    /**
     * Expected file name for the file system test configuration.
     */
    public static final String TEST_PROPERTIES_FILE_NAME = "fs-test.properties";

    private static final String TEST_PROPERTIES_ENV_VARIABLE_NAME = "KNIME_FS_TEST_PROPERTIES";

    private final static String DEFAULT_PATH_WORKSPACE =
        Platform.getLocation().append("/" + TEST_PROPERTIES_FILE_NAME).toString();

    /**
     * Resolves the properties for integration tests from the environment variable KNIME_FS_TEST_PROPERTIES.
     *
     * @return the test properties
     */
    public static Properties forIntegrationTests() {
        final Path propertiesPath = environmentVariablePath()
            .orElseThrow(() -> new IllegalStateException("The env variable '" + TEST_PROPERTIES_ENV_VARIABLE_NAME
                + "' must be set, pointing to a properties file containing the FS test configration."));
        return readProperties(propertiesPath);
    }

    /**
     * Resolves and reads the properties files for workflow tests.
     *
     * <p>
     * The steps to locate the properties file are as follows:
     *
     * <ul>
     * <li>First check for a file called fs-test.properties in the KNIME workspace root.</li>
     * <li>If none was found, it checks whether the environment variable KNIME_FS_TEST_PROPERTIES points to a properties
     * file.</li>
     * </ul>
     *
     * @return the test properties
     */
    public static Properties forWorkflowTests() {
        final Path propertiesPath = findFirst( //
            Stream.of( //
                workspaceRootPath(), //
                environmentVariablePath() //
            ) //
        );
        return readProperties(propertiesPath);
    }

    private static Path findFirst(final Stream<Optional<Path>> locations) {
        Optional<Path> path = locations //
            .filter(Optional::isPresent) //
            .map(Optional::get) //
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
