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
 *   Jun 12, 2020 (bjoern): created
 */
package org.knime.filehandling.core.fs.testinitializer;

import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

/**
 *
 * @author Bjoern Lohrmann
 */
public class FSTestConfig {

    /**
     * Key in fs-test.properties file to specify the file system to test.
     */
    public static final String KEY_TEST_FS = "test-fs";

    /**
     * Key in fs-test.properties file to specify the local directory with test fixtures.
     */
    public static final String KEY_FIXTURE_DIR = "fixture-dir";

    private final Properties m_properties;

    /**
     * @param fsTestProperties
     */
    public FSTestConfig(final Properties fsTestProperties) {
        m_properties = fsTestProperties;
    }

    /**
     * Reads all the properties with prefix fsType into a map. The suffix of each property will be removed from the keys
     * in the map.
     *
     * @param fsType the file system type for which the configuration is read
     * @return a map containing all the fsType specific properties
     */
    @SuppressWarnings("unchecked")
    public Map<String, String> getSettingsForFSType(final String fsType) {
        final Map<String, String> configuration = new HashMap<>();
        final String fsTypePrefix = fsType + ".";

        for (String key : Collections.list((Enumeration<String>)m_properties.propertyNames())) {
            if (key.startsWith(fsTypePrefix)) {
                final String value = m_properties.getProperty(key);
                final String keyWithoutFSType = key.substring(fsTypePrefix.length());
                configuration.put(keyWithoutFSType, value);
            }
        }
        return configuration;
    }

    /**
     * Returns the file system type to test, if one is configured in the given properties.
     *
     * @return an {@link Optional} that holds the file system type as a String, or is empty if the given properties did
     *         not specify a file system type to test.
     */
    public Optional<String> getFSTypeToTest() {
        return getOptionalProperty(KEY_TEST_FS);
    }

    /**
     * Returns the configured fixture directory if present.
     *
     * @return an {@link Optional} that holds the fixture directory as a String, or is empty if the given properties did
     *         not specify a fixture directory.
     */
    public Optional<String> getFixtureDir() {
        return getOptionalProperty(KEY_FIXTURE_DIR);
    }

    private Optional<String> getOptionalProperty(final String property) {
        if (m_properties.containsKey(property)) {
            final String fixtureDir = m_properties.getProperty(property);
            if (fixtureDir != null && !fixtureDir.isEmpty()) {
                return Optional.of(fixtureDir);
            }
        }
        return Optional.empty();
    }
}
