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
 *   Feb 24, 2020 (Simon Schmid, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.data.location;

import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

import org.knime.core.data.meta.DataColumnMetaData;
import org.knime.core.data.meta.DataColumnMetaDataSerializer;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.config.Config;
import org.knime.core.node.config.ConfigRO;
import org.knime.core.node.config.ConfigWO;
import org.knime.core.node.util.CheckUtils;
import org.knime.filehandling.core.connections.DefaultFSLocationSpec;

/**
 * Holds the information about the file system types and specifiers by holding a set of {@link DefaultFSLocationSpec}.
 *
 * @author Simon Schmid, KNIME GmbH, Konstanz, Germany
 * @since 4.2
 * @noreference non-public API
 * @noinstantiate non-public API
 */
public final class FSLocationValueMetaData implements DataColumnMetaData {

    private final Set<DefaultFSLocationSpec> m_specs;

    /**
     * Creates a {@link FSLocationValueMetaData} instance.
     *
     * @param fileSystemType the file system type
     * @param fileSystemSpecifier the file system specifier, can be {@code null}
     */
    public FSLocationValueMetaData(final String fileSystemType, final String fileSystemSpecifier) {
        m_specs = new HashSet<>();
        m_specs.add(new DefaultFSLocationSpec(fileSystemType, fileSystemSpecifier));
    }

    /**
     * Creates a {@link FSLocationValueMetaData} instance.
     *
     * @param fsLocationSpecs the set of {@link DefaultFSLocationSpec}s
     */
    public FSLocationValueMetaData(final Set<DefaultFSLocationSpec> fsLocationSpecs) {
        m_specs = new HashSet<>(fsLocationSpecs);
    }

    /**
     * Returns the set of {@link DefaultFSLocationSpec}s hold by this meta data instance.
     *
     * @return the set of {@link DefaultFSLocationSpec}s
     */
    public Set<DefaultFSLocationSpec> getFSLocationSpecs() {
        return m_specs;
    }

    /**
     * Serializer for {@link FSLocationValueMetaData} objects.
     *
     * @author Simon Schmid, KNIME GmbH, Konstanz, Germany
     */
    public static final class PathValueMetaDataSerializer
        implements DataColumnMetaDataSerializer<FSLocationValueMetaData> {

        @Override
        public Class<FSLocationValueMetaData> getMetaDataClass() {
            return FSLocationValueMetaData.class;
        }

        private static final String CFG_ENTRY = "fs_location_spec_";

        private static final String CFG_FS_CATEGORY = "fs_category";

        private static final String CFG_FS_SPECIFIER = "fs_specifier";

        /**
         * Serializes the given {@link FSLocationValueMetaData} to the given {@link ConfigWO}.
         *
         * @param fsLocationValueMetaData The {@link FSLocationValueMetaData} to serialize
         * @param config the {@link ConfigWO} to serialize to
         */
        @Override
        public void save(final FSLocationValueMetaData fsLocationValueMetaData, final ConfigWO config) {
            CheckUtils.checkNotNull(fsLocationValueMetaData,
                "The FSLocationValueMetaData provided to the serializer must not be null.");
            int idx = 0;
            for (final DefaultFSLocationSpec spec : fsLocationValueMetaData.getFSLocationSpecs()) {
                final Config subConfig = config.addConfig(CFG_ENTRY + idx);
                subConfig.addString(CFG_FS_CATEGORY, spec.getFileSystemCategory());
                subConfig.addString(CFG_FS_SPECIFIER, spec.getFileSystemSpecifier().orElse(null));
                idx++;
            }
        }

        /**
         * Deserializes and returns a new {@link FSLocationValueMetaData} to the given {@link ConfigRO}.
         *
         * @param config the {@link ConfigRO} to deserialize from
         * @return the deserialized {@link FSLocationValueMetaData}
         * @throws InvalidSettingsException if something went wrong during deserialization (e.g. missing entries in the
         *             {@link ConfigRO}).
         */
        @Override
        public FSLocationValueMetaData load(final ConfigRO config) throws InvalidSettingsException {
            final Set<DefaultFSLocationSpec> set = new HashSet<>();
            // with v4.3 and earlier, we only saved one FSLocation spec by saving its category and specifier; for sake
            // of backwards compatibility, we check if that's the case here
            if (config.containsKey(CFG_FS_CATEGORY)) {
                set.add(loadDefaultFSLocationSpec(config));
            } else {
                @SuppressWarnings("unchecked")
                final Enumeration<ConfigRO> children = (Enumeration<ConfigRO>)config.children();
                while (children.hasMoreElements()) {
                    final ConfigRO subConfig = children.nextElement();
                    set.add(loadDefaultFSLocationSpec(subConfig));
                }
            }
            return new FSLocationValueMetaData(set);
        }

        private static DefaultFSLocationSpec loadDefaultFSLocationSpec(final ConfigRO config)
            throws InvalidSettingsException {
            final String fileSystemCategory = config.getString(CFG_FS_CATEGORY);
            final String fileSystemSpecifier = config.getString(CFG_FS_SPECIFIER);
            return new DefaultFSLocationSpec(fileSystemCategory, fileSystemSpecifier);
        }

    }
}
