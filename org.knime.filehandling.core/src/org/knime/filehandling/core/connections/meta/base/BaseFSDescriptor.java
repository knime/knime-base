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
 *   Apr 28, 2021 (bjoern): created
 */
package org.knime.filehandling.core.connections.meta.base;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.knime.core.node.util.CheckUtils;
import org.knime.filehandling.core.connections.meta.FSCapabilities;
import org.knime.filehandling.core.connections.meta.FSConnectionConfig;
import org.knime.filehandling.core.connections.meta.FSConnectionFactory;
import org.knime.filehandling.core.connections.meta.FSDescriptor;
import org.knime.filehandling.core.connections.uriexport.URIExporterFactory;
import org.knime.filehandling.core.connections.uriexport.URIExporterFactoryMapBuilder;
import org.knime.filehandling.core.connections.uriexport.URIExporterID;
import org.knime.filehandling.core.testing.FSTestInitializerProvider;

/**
 * @author Bjoern Lohrmann, KNIME GmbH
 */
public final class BaseFSDescriptor implements FSDescriptor {

    private final FSConnectionFactory<?> m_connectionFactory;

    private final String m_separator;

    private final FSCapabilities m_capabilities;

    private final Map<URIExporterID, URIExporterFactory> m_uriExporterFactories;

    private final FSTestInitializerProvider m_testInitializerProvider;

    BaseFSDescriptor(final FSConnectionFactory<?> connectionFactory, //
        final String separator, //
        final FSCapabilities capabilities, //
        final Map<URIExporterID, URIExporterFactory> uriExporterFactories, //
        final FSTestInitializerProvider testInitializerProvider) {

        m_connectionFactory = connectionFactory;
        m_separator = separator;
        m_capabilities = capabilities;
        m_uriExporterFactories = uriExporterFactories;
        m_testInitializerProvider = testInitializerProvider;
    }

    @Override
    public FSConnectionFactory<?> getConnectionFactory() {
        return m_connectionFactory;
    }

    @Override
    public String getSeparator() {
        return m_separator;
    }

    @Override
    public FSCapabilities getCapabilities() {
        return m_capabilities;
    }

    @Override
    public Set<URIExporterID> getURIExporters() {
        return m_uriExporterFactories.keySet();
    }

    @Override
    public URIExporterFactory getURIExporterFactory(final URIExporterID exporterId) {
        return m_uriExporterFactories.get(exporterId);
    }

    @Override
    public Optional<FSTestInitializerProvider> getFSTestInitializerProvider() {
        return Optional.ofNullable(m_testInitializerProvider);
    }

    public static class Builder {

        private String m_separator = "/";

        private FSConnectionFactory m_connectionFactory = null;

        private BaseFSCapabilities.Builder m_capabilitiesBuilder = new BaseFSCapabilities.Builder();

        private URIExporterFactoryMapBuilder m_uriExporterMapBuilder = new URIExporterFactoryMapBuilder();

        private FSTestInitializerProvider m_testInitializerProvider = null;

        public Builder withSeparator(final String separator) {
            m_separator = separator;
            return this;
        }

        public Builder withCanBrowse(final boolean canBrowse) {
            m_capabilitiesBuilder.withCanBrowse(canBrowse);
            return this;
        }

        public Builder withCanListDirectories(final boolean canListDirectories) {
            m_capabilitiesBuilder.withCanListDirectories(canListDirectories);
            return this;
        }

        public Builder withCanCreateDirectories(final boolean canCreateDirectories) {
            m_capabilitiesBuilder.withCanCreateDirectories(canCreateDirectories);
            return this;
        }

        public Builder withCanDeleteDirectories(final boolean canDeleteDirectories) {
            m_capabilitiesBuilder.withCanDeleteDirectories(canDeleteDirectories);
            return this;
        }

        public Builder withCanGetPosixAttributes(final boolean canGetPosixAttributes) {
            m_capabilitiesBuilder.withCanGetPosixAttributes(canGetPosixAttributes);
            return this;
        }

        public Builder withCanSetPosixAttributes(final boolean canSetPosixAttributes) {
            m_capabilitiesBuilder.withCanSetPosixAttributes(canSetPosixAttributes);
            return this;
        }

        public Builder withCanCheckAccessReadOnFiles(final boolean canCheckAccessReadOnFiles) {
            m_capabilitiesBuilder.withCanCheckAccessReadOnFiles(canCheckAccessReadOnFiles);
            return this;
        }

        public Builder withCanCheckAccessReadOnDirectories(final boolean canCheckAccessReadOnDirectories) {
            m_capabilitiesBuilder.withCanCheckAccessReadOnDirectories(canCheckAccessReadOnDirectories);
            return this;
        }

        public Builder withCanCheckAccessWriteOnFiles(final boolean canCheckAccessWriteOnFiles) {
            m_capabilitiesBuilder.withCanCheckAccessWriteOnFiles(canCheckAccessWriteOnFiles);
            return this;
        }

        public Builder withCanCheckAccessWriteOnDirectories(final boolean canCheckAccessWriteOnDirectories) {
            m_capabilitiesBuilder.withCanCheckAccessWriteOnDirectories(canCheckAccessWriteOnDirectories);
            return this;
        }

        public Builder withCanCheckAccessExecuteOnFiles(final boolean canCheckAccessExecuteOnFiles) {
            m_capabilitiesBuilder.withCanCheckAccessExecuteOnFiles(canCheckAccessExecuteOnFiles);
            return this;
        }

        public Builder withCanCheckAccessExecuteOnDirectories(final boolean canCheckAccessExecuteOnDirectories) {
            m_capabilitiesBuilder.withCanCheckAccessExecuteOnDirectories(canCheckAccessExecuteOnDirectories);
            return this;
        }

        public Builder withCanWriteFiles(final boolean canWriteFiles) {
            m_capabilitiesBuilder.withCanWriteFiles(canWriteFiles);
            return this;
        }

        public Builder withCanDeleteFiles(final boolean canDeleteFiles) {
            m_capabilitiesBuilder.withCanDeleteFiles(canDeleteFiles);
            return this;
        }

        public Builder withIsWorkflowAware(final boolean isWorkflowAware) {
            m_capabilitiesBuilder.withIsWorkflowAware(isWorkflowAware);
            return this;
        }

        public Builder withURIExporterFactory(final URIExporterID exporterID, final URIExporterFactory factory) {
            m_uriExporterMapBuilder.add(exporterID, factory);
            return this;
        }

        public <C extends FSConnectionConfig> Builder withConnectionFactory(final FSConnectionFactory<C> connectionFactory) {
            m_connectionFactory = connectionFactory;
            return this;
        }

        public <C extends FSConnectionConfig> Builder
            withConnectionFactory(final FSConnectionFactory<C> connectionFactory, final C defaultConfig) {
            m_connectionFactory = withDefaultConfig(connectionFactory, defaultConfig);
            return this;
        }

        /**
         * Wraps the given {@link FSConnectionFactory} with protection against a null config, i.e. supplies the given
         * default config if the one provided to the factory is null.
         *
         * @param factory The factory to wrap with null-protection.
         * @param defaultConfig
         * @return a factory that wraps the given one with null-protection.
         */
        private static <C extends FSConnectionConfig> FSConnectionFactory<C>
            withDefaultConfig(final FSConnectionFactory<C> factory, final C defaultConfig) {

            return config -> {
                if (config == null) {
                    return factory.createConnection(defaultConfig);
                } else {
                    return factory.createConnection(config);
                }
            };
        }

        public Builder withTestInitializerProvider(final FSTestInitializerProvider testInitializerProvider) {
            m_testInitializerProvider = testInitializerProvider;
            return this;
        }

        public FSDescriptor build() {
            CheckUtils.checkArgument(StringUtils.isNotBlank(m_separator), "Separator must not be blank");
            CheckUtils.checkArgumentNotNull(m_connectionFactory, "Connection factory must not be null");

            return new BaseFSDescriptor(m_connectionFactory, //
                m_separator, //
                m_capabilitiesBuilder.build(), //
                m_uriExporterMapBuilder.build(),
                m_testInitializerProvider);
        }
    }
}
