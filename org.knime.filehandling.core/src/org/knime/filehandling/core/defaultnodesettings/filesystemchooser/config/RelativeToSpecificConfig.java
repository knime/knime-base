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
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.node.util.FileSystemBrowser.FileSelectionMode;
import org.knime.filehandling.core.connections.DefaultFSLocationSpec;
import org.knime.filehandling.core.connections.FSLocationSpec;
import org.knime.filehandling.core.defaultnodesettings.FileSystemChoice.Choice;
import org.knime.filehandling.core.defaultnodesettings.filesystemchooser.RelativeTo;
import org.knime.filehandling.core.defaultnodesettings.status.StatusMessage;

/**
 * Config specific to the "relative to" file system.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
public final class RelativeToSpecificConfig extends AbstractConvenienceFileSystemSpecificConfig {

    private static final String CFG_RELATIVE_TO = "relative_to";

    private static final RelativeTo DEFAULT = RelativeTo.WORKFLOW;

    private RelativeTo m_relativeTo = DEFAULT;

    /**
     * Constructor.
     */
    public RelativeToSpecificConfig() {

    }

    /**
     * Copy constructor.
     *
     * @param toCopy instance to copy
     */
    private RelativeToSpecificConfig(final RelativeToSpecificConfig toCopy) {
        m_relativeTo = toCopy.m_relativeTo;
    }

    /**
     * Returns the currently selected {@link RelativeTo} option.
     *
     * @return the {@link RelativeTo} option
     */
    public RelativeTo getRelativeTo() {
        return m_relativeTo;
    }

    /**
     * Sets the provided {@link RelativeTo} option and notifies the listeners if the value changed.
     *
     * @param relativeTo to set
     */
    public void setRelativeTo(final RelativeTo relativeTo) {
        if (m_relativeTo != relativeTo) {
            m_relativeTo = CheckUtils.checkArgumentNotNull(relativeTo, "The relativeTo argument must not be null.");
            notifyListeners();
        }
    }

    @Override
    public FSLocationSpec getLocationSpec() {
        return new DefaultFSLocationSpec(Choice.KNIME_FS, m_relativeTo.toString());
    }

    @Override
    public void loadInDialog(final NodeSettingsRO settings, final PortObjectSpec[] specs)
        throws NotConfigurableException {
        setRelativeTo(RelativeTo.fromString(settings.getString(CFG_RELATIVE_TO, DEFAULT.toString())));
    }

    @Override
    public void validateInModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        readRelativeTo(settings);
    }

    private static RelativeTo readRelativeTo(final NodeSettingsRO settings) throws InvalidSettingsException {
        try {
            return RelativeTo.fromString(settings.getString(CFG_RELATIVE_TO));
        } catch (IllegalArgumentException iae) {
            throw new InvalidSettingsException("Can't load relative to: " + iae.getMessage(), iae);
        }
    }

    @Override
    public void report(final Consumer<StatusMessage> statusConsumer) {
        // always valid
    }

    @Override
    public void loadInModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_relativeTo = readRelativeTo(settings);
    }

    @Override
    public void save(final NodeSettingsWO settings) {
        settings.addString(CFG_RELATIVE_TO, m_relativeTo.toString());
    }

    @Override
    public void overwriteWith(final FSLocationSpec locationSpec) {
        setRelativeTo(
            RelativeTo.fromString(locationSpec.getFileSystemSpecifier().orElseThrow(() -> new IllegalArgumentException(
                String.format("The provided FSLocation '%s' does not provide a relative-to option.", locationSpec)))));
    }

    @Override
    public FileSystemSpecificConfig copy() {
        return new RelativeToSpecificConfig(this);
    }

    @Override
    public void validate(final FSLocationSpec location) throws InvalidSettingsException {
        final Optional<String> specifier = location.getFileSystemSpecifier();
        CheckUtils.checkSetting(specifier.isPresent(),
            "No relative to option specified for the relative to file system.");
        try {
            RelativeTo.fromString(specifier.get());
        } catch (IllegalArgumentException iae) {
            throw new InvalidSettingsException(
                String.format("Unsupported relative to option '%s' encountered.", specifier.get()), iae);
        }
    }

    @Override
    public Set<FileSelectionMode> getSupportedFileSelectionModes() {
        return EnumSet.allOf(FileSelectionMode.class);
    }

}
