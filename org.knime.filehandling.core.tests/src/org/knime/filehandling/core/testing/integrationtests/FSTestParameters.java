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
package org.knime.filehandling.core.testing.integrationtests;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.knime.filehandling.core.testing.FSTestConfig;
import org.knime.filehandling.core.testing.FSTestInitializer;
import org.knime.filehandling.core.testing.FSTestInitializerManager;
import org.knime.filehandling.core.testing.FSTestInitializerProvider;
import org.knime.filehandling.core.testing.FSTestPropertiesResolver;
import org.knime.filehandling.core.util.IOESupplier;

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
     * @return all registered test initializers in a format suitable for the Parameterized runner, i.e. index 0 holds a
     *         String with the file system type and index 1 holds a {@link IOESupplier} that supplies a
     *         {@link FSTestInitializer} instance.
     */
    public static Collection<Object[]> getTestInitializers() {
        final FSTestConfig testConfig = new FSTestConfig(FSTestPropertiesResolver.forIntegrationTests());

        final List<String> testInitializerKeys = getFileSystemTypesToTest(testConfig);
        final FSTestInitializerManager manager = FSTestInitializerManager.instance();

        final int numberOfFS = testInitializerKeys.size();
        final Object[][] fsTestInitializers = new Object[numberOfFS][2];

        for (int i = 0; i < numberOfFS; i++) {
            final String fsType = testInitializerKeys.get(i);
            final IOESupplier<FSTestInitializer<?,?>> initializerSupplier =
                () -> manager.createInitializer(fsType, testConfig.getSettingsForFSType(fsType));

            fsTestInitializers[i][0] = fsType;
            fsTestInitializers[i][1] = new CachingSupplier(initializerSupplier);
        }

        return Arrays.asList(fsTestInitializers);
    }

    private static List<String> getFileSystemTypesToTest(final FSTestConfig testConfig) {
        final List<String> testInitializerKeys = new ArrayList<>();

        final Optional<String> fsToTest = testConfig.getFSTypeToTest();
        if (fsToTest.isPresent()) {
            testInitializerKeys.add(fsToTest.get());
        } else {
            testInitializerKeys.addAll(FSTestInitializerManager.instance().getAllTestInitializerKeys());
        }
        return testInitializerKeys;
    }

    private static class CachingSupplier implements IOESupplier<FSTestInitializer<?,?>> {

        private FSTestInitializer<?,?> m_toSupply;

        private final IOESupplier<FSTestInitializer<?,?>> m_newInstanceSupplier;

        CachingSupplier(final IOESupplier<FSTestInitializer<?,?>> newInstanceSupplier) {
            m_newInstanceSupplier = newInstanceSupplier;
        }

        @Override
        public synchronized FSTestInitializer<?,?> get() throws IOException {
            if (m_toSupply == null) {
                m_toSupply = m_newInstanceSupplier.get();
            }

            return m_toSupply;
        }
    }
}
