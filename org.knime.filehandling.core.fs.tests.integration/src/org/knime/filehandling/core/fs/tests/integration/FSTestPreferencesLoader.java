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
package org.knime.filehandling.core.fs.tests.integration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Platform;
import org.knime.core.node.workflow.BatchExecutor;

/**
 * Loads an optional KNIME preferences file from environment variable {@code KNIME_FS_TEST_PREFERENCES} pointing to a
 * file or {@code fs-test.epf} placed in current workspace.
 *
 * @author Sascha Wolke, KNIME GmbH
 */
public final class FSTestPreferencesLoader {

    private static final String TEST_PREFERENCES_FILE_NAME = "fs-test.epf";

    private static final String TEST_PREFERENCES_ENV_VARIABLE_NAME = "KNIME_FS_TEST_PREFERENCES";

    private static final String DEFAULT_PATH_WORKSPACE =
        Platform.getLocation().append("/" + TEST_PREFERENCES_FILE_NAME).toString();

    private FSTestPreferencesLoader() { /* no public instance */ }

    /**
     * Try to load the preferences from environment variable pointing to a file or {@code fs-test.epf} in current
     * workspace or otherwise load nothing.
     */
    static void loadPreferences() {
        try {
            final Optional<Path> env = environmentVariablePath();
            final Optional<Path> workspaceFile = workspaceRootPath();

            if (env.isPresent()) {
                BatchExecutor.setPreferences(env.get().toFile());
            } else if (workspaceFile.isPresent()) {
                BatchExecutor.setPreferences(workspaceFile.get().toFile());
            }
        } catch (final IOException|CoreException e) {
            throw new RuntimeException("Failed to load test preferences.", e);
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
        final String envVariableValue = System.getenv(TEST_PREFERENCES_ENV_VARIABLE_NAME);

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
