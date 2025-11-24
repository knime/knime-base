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
 *   Nov 20, 2025 (Paul Bärnreuther): created
 */
package org.knime.base.node.io.filehandling.webui.reader2;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.webui.node.dialog.defaultdialog.internal.file.FileSystemOption;
import org.knime.filehandling.core.connections.FSCategory;
import org.knime.filehandling.core.connections.FSConnection;
import org.knime.filehandling.core.connections.FSLocation;
import org.knime.filehandling.core.connections.FSLocationSpec;
import org.knime.filehandling.core.connections.RelativeTo;
import org.knime.filehandling.core.defaultnodesettings.status.DefaultStatusMessage;
import org.knime.filehandling.core.defaultnodesettings.status.StatusMessage;
import org.knime.filehandling.core.defaultnodesettings.status.StatusMessage.MessageType;
import org.knime.filehandling.core.port.FileSystemPortObjectSpec;
import org.knime.filehandling.core.util.CheckNodeContextUtil;

/**
 * Configuration class for reader path validation and model configuration. Handles validation for different file system
 * types and provides error/warning messages for various configuration states.
 *
 * @author Paul Bärnreuther
 * @since 5.10
 */
@SuppressWarnings("restriction")
public class ReaderPathConfiguration {

    private static final String NO_LOCATION_ERROR = "Please specify a %s";

    private static final String TRAILING_OR_LEADING_WHITESPACE_ERROR =
        "The path contains leading and/or trailing whitespaces, please remove them.";

    private static final String FILE_SYSTEM_NOT_SUPPORTED_ERROR = "The file system '%s' is not supported by this node.";

    private static final String WORKFLOW_RELATIVE_LOCATIONS_IN_SHARED_COMPONENT_ERROR =
        "Nodes in a shared component don't have access to workflow-relative locations";

    private static final String CONNECTED_FILE_SYSTEM_DOES_NOT_SUPPORT_FOLDERS_ERROR =
        "The connected file system is not compatible with this node because it doesn't support folders.";

    private static final String CONNECTED_NAMED_FILE_SYSTEM_DOES_NOT_SUPPORT_FOLDERS_ERROR =
        "The connected file system '%s' is not compatible with this node because it doesn't support folders.";

    private static final String NO_FILE_SYSTEM_CONNECTED_ERROR = "No file system connected at port %s.";

    private final Supplier<FSLocation> m_fsLocationSupplier;

    private final Supplier<Boolean> m_locationIsDirectorySupplier;

    private final EnumMap<FileSystemOption, Boolean> m_fileSystemAvailability;

    private final int m_portIdx;

    private Consumer<FSLocationSpec> m_changeFSLocationSpec;

    ReaderPathConfiguration(final Supplier<FSLocation> fsLocationSupplier,
        final Consumer<FSLocationSpec> changeFSLocationSpec, final Supplier<Boolean> locationIsDirectorySupplier,
        final FileSystemOption[] fileSystemChoice, final int portIdx) {
        m_fsLocationSupplier = fsLocationSupplier;
        m_changeFSLocationSpec = changeFSLocationSpec;
        m_locationIsDirectorySupplier = locationIsDirectorySupplier;

        // Build availability map from provided file system options
        m_fileSystemAvailability = new EnumMap<>(FileSystemOption.class);
        Arrays.stream(fileSystemChoice).forEach(option -> m_fileSystemAvailability.put(option, true));
        m_portIdx = portIdx;
    }

    record ReaderPathConfigResult(Optional<FSConnection> portFSConnection) {
    }

    /**
     * Configures the path in the model, validating the file system configuration and path settings.
     *
     * @param specs input port object specs
     * @param statusMessageConsumer consumer for status messages (warnings/errors)
     * @return the result of the configuration containing the file system connection if applicable
     * @throws InvalidSettingsException if the configuration is invalid
     */
    public ReaderPathConfigResult validateAndExtractPortFSConnection(final PortObjectSpec[] specs,
        final Consumer<StatusMessage> statusMessageConsumer) throws InvalidSettingsException {
        final var fsLocation = adjustAndGetFSLocation(specs);
        final var portFSConnection =
            validateFileSystemAndExtractPortFSConnection(specs, statusMessageConsumer, fsLocation);
        checkLocation(fsLocation);
        return new ReaderPathConfigResult(portFSConnection);
    }

    private FSLocation adjustAndGetFSLocation(final PortObjectSpec[] specs) {
        getNewFSLocationSpec(specs).ifPresent(m_changeFSLocationSpec::accept);
        return m_fsLocationSupplier.get();
    }

    private Optional<FSLocationSpec> getNewFSLocationSpec(final PortObjectSpec[] specs) {
        if (m_portIdx != -1 && specs.length > m_portIdx && specs[m_portIdx] != null) {
            return Optional.of(((FileSystemPortObjectSpec)specs[m_portIdx]).getFSLocationSpec());
        }
        return Optional.empty();

    }

    private Optional<FSConnection> validateFileSystemAndExtractPortFSConnection(final PortObjectSpec[] specs,
        final Consumer<StatusMessage> statusMessageConsumer, final FSLocation fsLocation)
        throws InvalidSettingsException {

        final FSCategory fsCategory = fsLocation.getFSCategory();

        switch (fsCategory) {
            case LOCAL:
                checkFileSystemAvailable(FileSystemOption.LOCAL, FSCategory.LOCAL.name());
                break;
            case CONNECTED:
                return validateAndExtractConnectedFileSystem(specs, statusMessageConsumer);
            case RELATIVE:
                validateRelativePath(fsLocation);
                break;
            case CUSTOM_URL:
                checkFileSystemAvailable(FileSystemOption.CUSTOM_URL, FSCategory.CUSTOM_URL.name());
                break;
            default:
                throw new InvalidSettingsException(String.format(FILE_SYSTEM_NOT_SUPPORTED_ERROR, fsCategory.name()));
        }
        return Optional.empty();
    }

    /**
     * Checks if a file system option is available based on the configuration.
     */
    private void checkFileSystemAvailable(final FileSystemOption option, final String fsCategoryForErrorMessage)
        throws InvalidSettingsException {
        if (!m_fileSystemAvailability.getOrDefault(option, false).booleanValue()) {
            throw new InvalidSettingsException(
                String.format(FILE_SYSTEM_NOT_SUPPORTED_ERROR, fsCategoryForErrorMessage));
        }
    }

    /**
     * Validates connected file system from input port.
     *
     * @return
     */
    @SuppressWarnings("resource")
    private Optional<FSConnection> validateAndExtractConnectedFileSystem(final PortObjectSpec[] specs,
        final Consumer<StatusMessage> statusMessageConsumer) throws InvalidSettingsException {

        checkFileSystemAvailable(FileSystemOption.CONNECTED, FSCategory.CONNECTED.name());

        if (m_portIdx == -1) {
            throw new IllegalStateException("No port index specified for connected file system.");
        }

        // Find the connected file system port (typically port 0 if present)
        CheckUtils.checkSetting(specs.length > m_portIdx && specs[m_portIdx] != null,
            String.format(NO_FILE_SYSTEM_CONNECTED_ERROR, m_portIdx));

        // Check if connection is available and issue warning if not
        Optional<FSConnection> connection = FileSystemPortObjectSpec.getFileSystemConnection(specs, m_portIdx);
        if (connection.isEmpty()) {
            statusMessageConsumer.accept(new DefaultStatusMessage(MessageType.WARNING,
                "No connection available. Execute the connector node first."));
            return Optional.empty();
        }
        if (m_locationIsDirectorySupplier.get().booleanValue() && !connection.get().supportsBrowsing()) {
            Optional<String> fsName = FileSystemPortObjectSpec.getFileSystemType(specs, m_portIdx);
            if (fsName.isPresent()) {
                throw new InvalidSettingsException(
                    String.format(CONNECTED_NAMED_FILE_SYSTEM_DOES_NOT_SUPPORT_FOLDERS_ERROR, fsName.get()));
            } else {
                throw new InvalidSettingsException(CONNECTED_FILE_SYSTEM_DOES_NOT_SUPPORT_FOLDERS_ERROR);
            }
        }
        return connection;
    }

    private void validateRelativePath(final FSLocation fsLocation) throws InvalidSettingsException {
        final Optional<String> specifier = fsLocation.getFileSystemSpecifier();
        CheckUtils.checkSetting(specifier.isPresent(), "No relative option provided.");

        final RelativeTo relativeTo = RelativeTo.fromSettingsValue(specifier.get()); // NOSONAR isPresent checked above
        switch (relativeTo) {
            case SPACE:
                // FileSystemOption.SPACE maps to RELATIVE/SPACE
                checkFileSystemAvailable(FileSystemOption.SPACE, "Relative to " + RelativeTo.SPACE.getLabel());
                break;
            case WORKFLOW_DATA:
                // FileSystemOption.EMBEDDED maps to RELATIVE/WORKFLOW_DATA
                checkFileSystemAvailable(FileSystemOption.EMBEDDED,
                    "Relative to " + RelativeTo.WORKFLOW_DATA.getLabel());
                break;
            default:
                throw new InvalidSettingsException(
                    String.format(FILE_SYSTEM_NOT_SUPPORTED_ERROR, "Relative to " + relativeTo.getLabel()));
        }

        // Validate component project restrictions
        CheckUtils.checkSetting(relativeTo == RelativeTo.MOUNTPOINT || !CheckNodeContextUtil.isInComponentProject(),
            WORKFLOW_RELATIVE_LOCATIONS_IN_SHARED_COMPONENT_ERROR);
    }

    private void checkLocation(final FSLocation toBeCheckedFSLocation) throws InvalidSettingsException {
        final String path = toBeCheckedFSLocation.getPath();

        if (path == null || path.isEmpty()) {
            final String locationLabel = m_locationIsDirectorySupplier.get().booleanValue() ? "folder" : "file";
            throw new InvalidSettingsException(String.format(NO_LOCATION_ERROR, locationLabel));
        }

        if (!path.equals(path.trim())) {
            throw new InvalidSettingsException(TRAILING_OR_LEADING_WHITESPACE_ERROR);
        }
    }

}
