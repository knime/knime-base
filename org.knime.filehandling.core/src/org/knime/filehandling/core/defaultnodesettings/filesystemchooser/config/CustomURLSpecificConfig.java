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
import org.knime.filehandling.core.connections.FSCategory;
import org.knime.filehandling.core.connections.FSLocationSpec;
import org.knime.filehandling.core.defaultnodesettings.status.DefaultStatusMessage;
import org.knime.filehandling.core.defaultnodesettings.status.StatusMessage;
import org.knime.filehandling.core.defaultnodesettings.status.StatusMessage.MessageType;

/**
 * Config for the Custom URL file system.</br>
 * Holds the timeout used for establishing the connection and reading from it.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
public final class CustomURLSpecificConfig extends AbstractConvenienceFileSystemSpecificConfig {

    private static final String CFG_CUSTOM_URL_TIMEOUT = "custom_url_timeout";

    private static final StatusMessage TIMEOUT_NEGATIVE_MSG =
        new DefaultStatusMessage(MessageType.ERROR, "The specified custom URL timeout is negative.");

    private static final int DEFAULT = 1000;

    private int m_timeout = DEFAULT;

    /**
     * Constructor.
     *
     * @param active flag indicating whether this config is active (i.e. selectable for the user)
     */
    public CustomURLSpecificConfig(final boolean active) {
        super(active);
    }

    private CustomURLSpecificConfig(final CustomURLSpecificConfig toCopy) {
        super(toCopy.isActive());
        m_timeout = toCopy.m_timeout;
    }

    /**
     * Returns the timeout for establishing a connection, as well as reading/writing from/to the file system.
     *
     * @return the timeout in milliseconds
     */
    public int getTimeout() {
        return m_timeout;
    }

    /**
     * Sets the provided <b>timeout</b> and notifies the listeners if the value changed.
     *
     * @param timeout the new timeout
     */
    public void setTimeout(final int timeout) {
        if (m_timeout != timeout) {
            m_timeout = timeout;
            notifyListeners();
        }
    }

    @Override
    public FSLocationSpec getLocationSpec() {
        return new DefaultFSLocationSpec(FSCategory.CUSTOM_URL, Long.toString(m_timeout));
    }

    @Override
    public void loadInDialog(final NodeSettingsRO settings, final PortObjectSpec[] specs)
        throws NotConfigurableException {
        setTimeout(settings.getInt(CFG_CUSTOM_URL_TIMEOUT, DEFAULT));
    }

    @Override
    public void overwriteWith(final FSLocationSpec locationSpec) {
        final int timeout = Integer.parseInt(locationSpec.getFileSystemSpecifier()
            .orElseThrow(() -> new IllegalArgumentException("No timeout for custom URL file system provided.")));
        CheckUtils.checkArgument(timeout >= 0, "The custom URL timeout must not be negative.");
        setTimeout(timeout);
    }

    @Override
    public void validateInModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        validateTimeout(settings.getInt(CFG_CUSTOM_URL_TIMEOUT));
    }

    @Override
    public void report(final Consumer<StatusMessage> statusConsumer) {
        if (m_timeout < 0) {
            statusConsumer.accept(TIMEOUT_NEGATIVE_MSG);
        }
    }

    private static void validateTimeout(final int timeout) throws InvalidSettingsException {
        CheckUtils.checkSetting(timeout >= 0, "The custom URL timeout must not be negative.");
    }

    @Override
    public void loadInModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        final int timeout = settings.getInt(CFG_CUSTOM_URL_TIMEOUT);
        validateTimeout(timeout);
        m_timeout = timeout;
    }

    @Override
    public void save(final NodeSettingsWO settings) {
        settings.addInt(CFG_CUSTOM_URL_TIMEOUT, m_timeout);
    }

    @Override
    public FileSystemSpecificConfig copy() {
        return new CustomURLSpecificConfig(this);
    }

    @Override
    public void validate(final FSLocationSpec location) throws InvalidSettingsException {
        final Optional<String> specifier = location.getFileSystemSpecifier();
        if (specifier.isPresent()) {
            try {
                int timeout = Integer.parseInt(specifier.get());
                CheckUtils.checkSetting(timeout >= 0, "The timeout must be non-negative but was %s.", timeout);
            } catch (NumberFormatException ex) {
                throw new InvalidSettingsException(
                    String.format("The specfied timeout '%s' is not a valid number.", specifier.get()));
            }
        } else {
            throw new InvalidSettingsException("No timeout specified for custom url file system.");
        }
    }

    @Override
    public Set<FileSelectionMode> getSupportedFileSelectionModes() {
        return EnumSet.of(FileSelectionMode.FILES_ONLY);
    }

}
