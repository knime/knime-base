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
 *   May 7, 2020 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.defaultnodesettings.filesystemchooser.config;

import java.util.EnumSet;
import java.util.Set;
import java.util.function.Consumer;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.util.FileSystemBrowser.FileSelectionMode;
import org.knime.filehandling.core.connections.DefaultFSLocationSpec;
import org.knime.filehandling.core.connections.FSCategory;
import org.knime.filehandling.core.connections.FSLocationSpec;
import org.knime.filehandling.core.defaultnodesettings.status.StatusMessage;

/**
 * Config for the local file system.</br>
 * Currently contains no specific information.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 * @noreference non-public API
 * @noinstantiate non-public API
 */
public final class LocalSpecificConfig extends AbstractConvenienceFileSystemSpecificConfig {

    private static final FSLocationSpec LOCATION_SPEC = new DefaultFSLocationSpec(FSCategory.LOCAL);

    /**
     * Constructor.
     *
     * @param active flag indicating whether this config is active (i.e. selectable for the user)
     */
    public LocalSpecificConfig(final boolean active) {
        super(active);
    }

    @Override
    public FSLocationSpec getLocationSpec() {
        return LOCATION_SPEC;
    }

    @Override
    public void loadInDialog(final NodeSettingsRO settings, final PortObjectSpec[] specs)
        throws NotConfigurableException {
        // nothing to load
    }

    @Override
    public void updateSpecifier(final FSLocationSpec locationSpec) {
        // nothing to overwrite
    }

    @Override
    public void validateInModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        // nothing to validate
    }

    @Override
    public void report(final Consumer<StatusMessage> statusConsumer) {
        // nothing to report
    }

    @Override
    public void loadInModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        // nothing to load
    }

    @Override
    public void save(final NodeSettingsWO settings) {
        // nothing to save
    }

    @Override
    public FileSystemSpecificConfig copy() {
        return new LocalSpecificConfig(isActive());
    }

    @Override
    public void validate(final FSLocationSpec location) throws InvalidSettingsException {
        // nothing to validate
    }

    @Override
    public Set<FileSelectionMode> getSupportedFileSelectionModes() {
        return EnumSet.allOf(FileSelectionMode.class);
    }

    @Override
    public boolean canConnect() {
        // a connection to the local file system is always possible
        return true;
    }

    @Override
    public String getFileSystemName() {
        return "Local File System";
    }

}
