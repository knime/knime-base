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
 *   Jun 5, 2020 (bjoern): created
 */
package org.knime.filehandling.core.testing;

import java.util.Map;
import java.util.UUID;

/**
 * Base implementation of a {@link FSTestInitializerProvider}. At the moment, this class only provides a static utility
 * method to randomize the working directory name of the file system.
 *
 * @author Bjoern Lohrmann, KNIME GmbH
 * @noreference non-public API
 * @noextend non-public API
 */
public abstract class DefaultFSTestInitializerProvider implements FSTestInitializerProvider {

    /**
     * Generates a randomized path with the given prefix. Implementations should call this method to randomize
     * the working directory path of the file system (prior to its instantiation).
     *
     * @param workingDirPrefix The prefix of the working directory, which should be an absolute path.
     * @param sep The path separator used by the concrete file system.
     * @return a randomized path with the given prefix
     */
    protected static String generateRandomizedWorkingDir(final String workingDirPrefix, final String sep) {
        return String.format("%s%s%s%s", //
            workingDirPrefix, //
            sep, //
            UUID.randomUUID().toString(), //
            sep);
    }

    /**
     * Get a parameter from the given configuration or fail with an exception if parameter is missing.
     *
     * @param config configuration with file system parameters
     * @param key request configuration key
     * @return value of given key in configuration
     * @throws IllegalArgumentException if configuration does not contain given key
     */
    protected String getParameter(final Map<String, String> config, final String key) {
        if (!config.containsKey(key)) {
            throw new IllegalArgumentException(
                "Required parameter '" + getFSType() + "." + key + "' not found in fs params.");
        } else {
            return config.get(key);
        }
    }
}
