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
 *   Mar 18, 2020 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.data.location.internal;

import java.util.Optional;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.config.ConfigRO;
import org.knime.core.node.config.ConfigWO;
import org.knime.filehandling.core.connections.DefaultFSLocationSpec;
import org.knime.filehandling.core.connections.FSLocation;
import org.knime.filehandling.core.connections.FSLocationSpec;

/**
 * Utility class for {@link FSLocation} objects.
 *
 * TODO decide if this functionality should move into {@link FSLocation}.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
public final class FSLocationUtils {

    private FSLocationUtils() {
        // static utility class
    }

    /**
     * Config key for the path part of an {@link FSLocation}.
     */
    public static final String CFG_PATH = "path";

    /**
     * Config key for the file system specifier part of an {@link FSLocation}.
     */
    public static final String CFG_FS_SPECIFIER = "file_system_specifier";

    /**
     * Config key for the file system type part of an {@link FSLocation}.
     */
    public static final String CFG_FS_TYPE = "file_system_type";

    /**
     * Config key for the boolean indicating whether a location is present or not.
     */
    public static final String CFG_LOCATION_PRESENT = "location_present";

    /**
     * Loads a {@link FSLocation} from the provided {@link ConfigRO config}.
     *
     * @param config the {@link ConfigRO} to load from
     * @return a new {@link FSLocation} corresponding to the values stored in {@link ConfigRO config}
     * @throws InvalidSettingsException if {@link ConfigRO} does not contain a {@link FSLocation} or is otherwise
     *             invalid
     */
    public static FSLocation loadFSLocation(final ConfigRO config) throws InvalidSettingsException {
        if (config.getBoolean(CFG_LOCATION_PRESENT)) {
            final String fsType = config.getString(CFG_FS_TYPE);
            final String fsSpecifier = config.getString(CFG_FS_SPECIFIER, null);
            final String path = config.getString(CFG_PATH);
            return new FSLocation(fsType, fsSpecifier, path);
        } else {
            return FSLocation.NULL;
        }
    }

    /**
     *
     * Loads a {@link FSLocationSpec} from the provided {@link ConfigRO config}.
     *
     * @param config the {@link ConfigRO} to load from
     * @return a new {@link FSLocationSpec} corresponding to the values stored in {@link ConfigRO config}
     * @throws InvalidSettingsException if {@link ConfigRO} does not contain a {@link FSLocationSpec} or is otherwise
     *             invalid
     */
    public static FSLocationSpec loadFSLocationSpec(final ConfigRO config) throws InvalidSettingsException {
        if (config.getBoolean(CFG_LOCATION_PRESENT)) {
            final String fsType = config.getString(CFG_FS_TYPE);
            final String fsSpecifier = config.getString(CFG_FS_SPECIFIER, null);
            return new DefaultFSLocationSpec(fsType, fsSpecifier);
        } else {
            return FSLocationSpec.NULL;
        }
    }

    /**
     * Saves the provided {@link FSLocation location} into {@link ConfigWO config}.
     *
     * @param location the {@link FSLocation} to save
     * @param config the {@link ConfigWO} to save to
     */
    public static void saveFSLocation(final FSLocation location, final ConfigWO config) {
        config.addBoolean(CFG_LOCATION_PRESENT, location != FSLocation.NULL);
        if (location != FSLocation.NULL) {
            config.addString(CFG_FS_TYPE, location.getFileSystemType());
            Optional<String> fileSystemSpecifier = location.getFileSystemSpecifier();
            if (fileSystemSpecifier.isPresent()) {
                config.addString(CFG_FS_SPECIFIER, fileSystemSpecifier.get());
            }
            config.addString(CFG_PATH, location.getPath());
        }
    }

    public static void saveFSLocationSpec(final FSLocationSpec locationSpec, final ConfigWO config) {
        config.addBoolean(CFG_LOCATION_PRESENT, locationSpec != FSLocationSpec.NULL);
        if (locationSpec != FSLocationSpec.NULL) {
            config.addString(CFG_FS_TYPE, locationSpec.getFileSystemType());
            Optional<String> fileSystemSpecifier = locationSpec.getFileSystemSpecifier();
            if (fileSystemSpecifier.isPresent()) {
                config.addString(CFG_FS_SPECIFIER, fileSystemSpecifier.get());
            }
        }
    }

    /**
     * Tests if the provided {@link ConfigRO config} represents an {@link FSLocation}. It is assumed that a config is a
     * {@link FSLocation} if it contains only the correct key-value pairs. If it contains any additional configurations,
     * it is not considered to be a {@link FSLocation}.
     *
     * @param config the {@link ConfigRO} to check
     * @return {@code true} if {@link ConfigRO config} corresponds to a {@link FSLocation}
     */
    public static boolean isFSLocation(final ConfigRO config) {
        try {
            if (config.getBoolean(CFG_LOCATION_PRESENT)) {
                config.getString(CFG_FS_TYPE);
                config.getString(CFG_PATH);
                // the specifier is not necessarily present
                final String fsSpecifier = config.getString(CFG_FS_SPECIFIER, null);
                // we can't overwrite if there are more children than expected
                return config.getChildCount() == (fsSpecifier == null ? 3 : 4);
            } else {
                // the config represents a FSLocation if it contains only the flag
                return config.getChildCount() == 1;
            }
        } catch (InvalidSettingsException ex) {
            // the config entry does not represent a SettingsModelFSLocation
        }
        return false;
    }

    public static boolean isFSLocationSpec(final ConfigRO config) {
        try {
            if (config.getBoolean(CFG_LOCATION_PRESENT)) {
                config.getString(CFG_FS_TYPE);
                // the specifier is not necessarily present
                config.getString(CFG_FS_SPECIFIER, null);
                return true;
            } else {
                // the config represents a FSLocationSpec if it contains only the flag
                return config.getChildCount() == 1;
            }
        } catch (InvalidSettingsException ex) {
            // the config entry does not represent an FSLocationSpec
        }
        return false;
    }

}
