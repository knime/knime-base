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
 *   May 22, 2020 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.defaultnodesettings.filechooser;

import java.util.Objects;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.filehandling.core.connections.FSLocation;
import org.knime.filehandling.core.connections.FSLocationSpec;
import org.knime.filehandling.core.data.location.internal.FSLocationUtils;
import org.knime.filehandling.core.defaultnodesettings.filesystemchooser.config.AbstractLocationSpecConfig;
import org.knime.filehandling.core.defaultnodesettings.filesystemchooser.config.FSLocationSpecConfig;

/**
 * {@link FSLocationSpecConfig} implementation that works with full-fledged {@link FSLocation FSLocations}.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
final class FSLocationConfig extends AbstractLocationSpecConfig<FSLocation, FSLocationConfig> {

    static final String CFG_LOCATION = "location";

    FSLocationConfig() {

    }

    private FSLocationConfig(final FSLocationConfig toCopy) {
        super(toCopy);
    }

    @Override
    public void loadSettingsForDialog(final NodeSettingsRO settings) {
        try {
            loadSettingsForModel(settings);
        } catch (InvalidSettingsException ex) {
            // keep using the old value
        }
    }

    @Override
    public void loadSettingsForModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        setLocationSpec(loadLocation(settings));
    }

    private static FSLocation loadLocation(final NodeSettingsRO settings) throws InvalidSettingsException {
        return FSLocationUtils.loadFSLocation(settings.getConfig(CFG_LOCATION));
    }

    @Override
    public void validateForModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        loadLocation(settings);
    }

    @Override
    public void save(final NodeSettingsWO settings) {
        FSLocationUtils.saveFSLocation(getLocationSpec(), settings.addNodeSettings(CFG_LOCATION));
    }

    @Override
    public FSLocationConfig copy() {
        return new FSLocationConfig(this);
    }

    void setPath(final String path) {
        final FSLocation locationSpec = getLocationSpec();
        if (!Objects.equals(locationSpec.getPath(), path)) {
            // sets the new location and notifies the listeners
            setLocationSpec(new FSLocation(locationSpec.getFileSystemType(),
                locationSpec.getFileSystemSpecifier().orElse(null), path));
        }
    }

    @Override
    protected FSLocation adapt(final FSLocationSpec locationSpec) {
        if (locationSpec instanceof FSLocation) {
            return (FSLocation)locationSpec;
        } else {
            final FSLocation current = getLocationSpec();
            final String path = current == null ? "" : current.getPath();
            return new FSLocation(locationSpec.getFileSystemType(), locationSpec.getFileSystemSpecifier().orElse(null),
                path);
        }
    }

}
