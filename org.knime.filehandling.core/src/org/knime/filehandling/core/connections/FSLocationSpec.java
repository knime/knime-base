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
 *   Apr 24, 2020 (bjoern): created
 */
package org.knime.filehandling.core.connections;

import java.util.Optional;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.config.ConfigRO;
import org.knime.core.node.config.ConfigWO;
import org.knime.core.node.util.CheckUtils;
import org.knime.filehandling.core.connections.meta.FSType;
import org.knime.filehandling.core.connections.meta.FSTypeRegistry;

/**
 * Interface that provides information about the kind of file system that a {@link FSLocation} requires.
 *
 * @author Bjoern Lohrmann, KNIME GmbH
 * @see FSLocation
 * @noreference non-public API
 * @noimplement non-public API
 */
public interface FSLocationSpec {

    /**
     * {@code NULL} instance.
     */
    public static FSLocationSpec NULL = new FSLocationSpec() {

        @Override
        public String getFileSystemCategory() {
            return null;
        }

        @Override
        public Optional<String> getFileSystemSpecifier() {
            return Optional.empty();
        }

    };

    /**
     * Returns the file system category. The returned value is only the string representation of the enum return by
     * {@link #getFSCategory()}
     *
     * @return the file system category as a string.
     */
    String getFileSystemCategory();

    /**
     * Returns the {@link FSCategory file system category}.
     *
     * @return the file system category as an enum.
     */
    default FSCategory getFSCategory() {
        return FSCategory.valueOf(getFileSystemCategory());
    }

    default FSType getFSType() {
        switch (getFSCategory()) {
            case LOCAL:
                return FSType.LOCAL_FS;
            case RELATIVE:
                final RelativeTo relativeTo = RelativeTo.fromSettingsValue(getFileSystemSpecifier().orElseThrow(IllegalStateException::new));
                return relativeTo.toFSType();
            case MOUNTPOINT:
                return FSType.MOUNTPOINT;
            case CUSTOM_URL:
                return FSType.CUSTOM_URL;
            case CONNECTED:
                final String specifier = getFileSystemSpecifier().orElseThrow(IllegalStateException::new);
                final String typeId = specifier.split(":")[0];
                return FSTypeRegistry.getFSType(typeId).orElseThrow(IllegalStateException::new);
            case HUB_SPACE:
                return FSType.HUB_SPACE;
            default:
                throw new IllegalStateException("Unknown file system category: " + getFSCategory());
        }
    }


    /**
     * Returns the optional file system specifier.
     *
     * @return the file system specifier
     */
    Optional<String> getFileSystemSpecifier();

    @Override
    String toString();

    /**
     * Checks if the provided {@link FSLocationSpec} objects are equal regarding their file system category and
     * specifier.
     *
     * @param first {@link FSLocationSpec}
     * @param second {@link FSLocationSpec}
     * @return {@code true} if either both are {@code null} or have the same category and specifier
     */
    static boolean areEqual(final FSLocationSpec first, final FSLocationSpec second) {
        if (first == second) {
            // both are identical (including null)
            return true;
        }
        if (first == null || second == null) {
            // both can't be null because then the previous if-switch would have returned already
            return false;
        }
        return first.getFileSystemCategory().equals(second.getFileSystemCategory())
            && first.getFileSystemSpecifier().equals(second.getFileSystemSpecifier());
    }

    /**
     * Abstract superclass for {@link FSLocationSpec} serializers.
     *
     * @author Bjoern Lohrmann, KNIME GmbH
     * @param <T> The concrete subtype of FSLocationSpec to (de)serialize.
     * @since 4.2
     */
    public abstract static class FSLocationSpecSerializer<T extends FSLocationSpec> {

        private static final String CFG_FS_CATEGORY = "fs_category";

        private static final String CFG_FS_SPECIFIER = "fs_specifier";

        /**
         * Serializes the given {@link FSLocationSpec} to the given {@link ConfigWO}.
         *
         * @param fsLocationSpec The {@link FSLocationSpec} to serialize.
         * @param config The {@link ConfigWO} to seralize to.
         */
        public void save(final T fsLocationSpec, final ConfigWO config) {
            CheckUtils.checkNotNull(fsLocationSpec, "The FSLocationSpec provided to the serializer must not be null.");
            config.addString(CFG_FS_CATEGORY, fsLocationSpec.getFileSystemCategory());
            config.addString(CFG_FS_SPECIFIER, fsLocationSpec.getFileSystemSpecifier().orElse(null));
        }

        /**
         * Deserializes a new {@link FSLocationSpec} to the given {@link ConfigRO}.
         *
         * @param config The {@link ConfigRO} to deseralize from.
         * @return the deserialized {@link FSLocationSpec}.
         * @throws InvalidSettingsException If something went wrong during deserialization (e.g. missing entries in the
         *             {@link ConfigRO}).
         */
        public T load(final ConfigRO config) throws InvalidSettingsException {
            final String fileSystemCategory = config.getString(CFG_FS_CATEGORY);
            final String fileSystemSpecifier = config.getString(CFG_FS_SPECIFIER);
            return createFSLocationSpec(fileSystemCategory, fileSystemSpecifier, config);
        }

        /**
         * Abstract method to to create the concrete instance of a {@link FSLocationSpec} subclass.
         *
         * @param fileSystemCategory The file system category.
         * @param fileSystemSpecifier The file system specifier.
         * @param config The {@link ConfigRO} to (optionally) deserialize further fields from.
         * @return an instance of {@link FSLocationSpec} subclass.
         */
        protected abstract T createFSLocationSpec(String fileSystemCategory, String fileSystemSpecifier,
            ConfigRO config);
    }
}
