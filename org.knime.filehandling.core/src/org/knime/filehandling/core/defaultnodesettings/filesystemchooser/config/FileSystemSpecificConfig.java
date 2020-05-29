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
 *   May 6, 2020 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.defaultnodesettings.filesystemchooser.config;

import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import javax.swing.event.ChangeListener;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.context.DeepCopy;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.util.FileSystemBrowser;
import org.knime.filehandling.core.connections.FSConnection;
import org.knime.filehandling.core.connections.FSLocationSpec;
import org.knime.filehandling.core.defaultnodesettings.filesystemchooser.status.StatusMessage;
import org.knime.filehandling.core.defaultnodesettings.filesystemchooser.status.StatusReporter;

/**
 * Config for a specific file system.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
public interface FileSystemSpecificConfig extends DeepCopy<FileSystemSpecificConfig>, StatusReporter {

    /**
     * Returns the {@link FSLocationSpec} corresponding ot the current configuration.</br>
     * NOTE: This spec might be invalid if the configuration is in an invalid state.
     *
     * @return the {@link FSLocationSpec} corresponding to the current configuration
     */
    FSLocationSpec getLocationSpec();

    /**
     * Loads the configuration from {@link NodeSettingsRO settings} in the dialog and sets defaults if some settings are
     * missing.
     *
     * @param settings to load from
     * @param specs input {@link PortObjectSpec specs} of the node
     * @throws NotConfigurableException if the node can't be configured e.g. due to incompatible specs
     */
    void loadInDialog(final NodeSettingsRO settings, PortObjectSpec[] specs) throws NotConfigurableException;

    /**
     * Overwrites the settings with the provided {@link FSLocationSpec}.
     *
     * @param locationSpec to overwrite the configuration with
     */
    void overwriteWith(final FSLocationSpec locationSpec);

    /**
     * Validates if the configuration represented by the provided {@link FSLocationSpec} is valid.
     *
     * @param location to validate
     * @throws InvalidSettingsException if the provided location is invalid
     */
    void validate(final FSLocationSpec location) throws InvalidSettingsException;

    /**
     * Getter for the file system connection.</br>
     * In case of convenience file systems, this method always returns {@link Optional#empty()}.
     *
     * @return the file system connection
     */
    Optional<FSConnection> getConnection();

    /**
     * Returns the file selection modes supported by this file system.
     *
     * @return the file selection modes supported by this file system
     */
    Set<FileSystemBrowser.FileSelectionMode> getSupportedFileSelectionModes();

    /**
     * Updates the config with the provided <b>specs</b> in the NodeModel of a node.</br>
     * To be called in the {@code configure} method of the NodeModel.
     *
     * @param specs the input specs of the node
     * @param statusMessageConsumer consumer for non-fatal problems and status messages
     * @throws InvalidSettingsException if the specs are incompatible with the configuration and execution is impossible
     */
    void configureInModel(final PortObjectSpec[] specs, Consumer<StatusMessage> statusMessageConsumer)
        throws InvalidSettingsException;

    /**
     * Validates if the configuration stored in {@link NodeSettingsRO settings} is valid WITHOUT actually overwriting
     * the values in this configuration.
     *
     * @param settings to validate
     * @throws InvalidSettingsException if the configuration stored in {@link NodeSettingsRO settings} is invalid
     */
    void validateInModel(final NodeSettingsRO settings) throws InvalidSettingsException;

    /**
     * Loads the configuration from {@link NodeSettingsRO settings} and fails if parts of the configuration are invalid
     * or not available.
     *
     * @param settings to load from
     * @throws InvalidSettingsException if part of the configuration is invalid or missing
     */
    void loadInModel(final NodeSettingsRO settings) throws InvalidSettingsException;

    /**
     * Saves the configuration to the provided {@link NodeSettingsWO}.
     *
     * @param settings to save to
     */
    void save(final NodeSettingsWO settings);

    /**
     * Adds the provided {@link ChangeListener} to the list of listeners.
     *
     * @param listener {@link ChangeListener} to add
     */
    void addChangeListener(ChangeListener listener);

}
