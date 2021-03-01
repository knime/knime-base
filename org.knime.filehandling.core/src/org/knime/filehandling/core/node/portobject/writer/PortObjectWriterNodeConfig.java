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
 */
package org.knime.filehandling.core.node.portobject.writer;

import java.util.Arrays;
import java.util.Objects;

import org.knime.core.node.context.NodeCreationConfiguration;
import org.knime.core.node.util.CheckUtils;
import org.knime.filehandling.core.defaultnodesettings.EnumConfig;
import org.knime.filehandling.core.defaultnodesettings.filechooser.writer.FileOverwritePolicy;
import org.knime.filehandling.core.defaultnodesettings.filechooser.writer.SettingsModelWriterFileChooser;
import org.knime.filehandling.core.node.portobject.AbstractPortObjectIONodeConfigBuilder;
import org.knime.filehandling.core.node.portobject.PortObjectIONodeConfig;
import org.knime.filehandling.core.node.portobject.reader.PortObjectReaderNodeConfig;

/**
 * Configuration class for port object writer nodes that can be extended with additional configurations.
 *
 * @author Simon Schmid, KNIME GmbH, Konstanz, Germany
 * @noreference non-public API
 * @noextend non-public API
 * @noinstantiate non-public API
 */
public class PortObjectWriterNodeConfig extends PortObjectIONodeConfig<SettingsModelWriterFileChooser> {

    /**
     * Constructor. Should only be used by extending classes, all other clients should use the builder provided by
     * {@link #builder(NodeCreationConfiguration)}.
     *
     * @param builder holding the configuration (can be created via {@link #builder(NodeCreationConfiguration)})
     */
    protected PortObjectWriterNodeConfig(final PortObjectWriterNodeConfigBuilder builder) {
        super(new SettingsModelWriterFileChooser(CFG_FILE_CHOOSER, builder.getPortConfig(),
            CONNECTION_INPUT_PORT_GRP_NAME, builder.getFilterModeConfig(), builder.m_overwritePolicyConfig,
            builder.getConvenienceFS(), builder.getFileSuffixes()));
    }

    /**
     * Creates a {@link PortObjectWriterNodeConfigBuilder builder} for the creation of
     * {@link PortObjectReaderNodeConfig} objects.
     *
     * @param creationConfig the {@link NodeCreationConfiguration} of the current node
     * @return a {@link PortObjectWriterNodeConfigBuilder builder} for {@link PortObjectWriterNodeConfig} objects
     */
    public static PortObjectWriterNodeConfigBuilder builder(final NodeCreationConfiguration creationConfig) {
        return new PortObjectWriterNodeConfigBuilder(creationConfig);
    }

    /**
     * A builder of {@link PortObjectWriterNodeConfig} objects.
     *
     * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
     */
    public static class PortObjectWriterNodeConfigBuilder
        extends AbstractPortObjectIONodeConfigBuilder<PortObjectWriterNodeConfigBuilder> {

        private EnumConfig<FileOverwritePolicy> m_overwritePolicyConfig =
            EnumConfig.create(FileOverwritePolicy.FAIL, FileOverwritePolicy.OVERWRITE);

        /**
         * Constructor.
         *
         * @param creationConfig the {@link NodeCreationConfiguration} of the current node
         */
        protected PortObjectWriterNodeConfigBuilder(final NodeCreationConfiguration creationConfig) {
            super(creationConfig);
        }

        @Override
        protected PortObjectWriterNodeConfigBuilder getThis() {
            return this;
        }

        /**
         * Builds the config.
         *
         * @return a fresh {@link PortObjectWriterNodeConfig} that uses values currently configured in this builder
         */
        public PortObjectWriterNodeConfig build() {
            return new PortObjectWriterNodeConfig(this);
        }

        /**
         * Sets the provided {@link FileOverwritePolicy defaultPolicy} as default as well as the provided
         * {@link FileOverwritePolicy} otherPolicies as the other available policies.
         *
         * @param defaultPolicy the policy that should be selected by default
         * @param otherPolicies other available policies
         * @return this builder
         */
        public PortObjectWriterNodeConfigBuilder withFileOverwritePolicies(final FileOverwritePolicy defaultPolicy,
            final FileOverwritePolicy... otherPolicies) {
            CheckUtils.checkArgumentNotNull(defaultPolicy, "The defaultPolicy must not be null.");
            CheckUtils.checkArgumentNotNull(otherPolicies, "The otherPolicies might be empty but never null.");
            CheckUtils.checkArgument(Arrays.stream(otherPolicies).noneMatch(Objects::isNull),
                "None of the otherPolicies must be null.");
            checkNoNullsInArray(otherPolicies, "otherPolicies");
            m_overwritePolicyConfig = EnumConfig.create(defaultPolicy, otherPolicies);
            return this;
        }

    }

}
