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
 *   Mar 23, 2021 (Bjoern Lohrmann, KNIME GmbH): created
 */
package org.knime.filehandling.utility.nodes.pathtouri.exporter;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.util.CheckUtils;
import org.knime.filehandling.core.connections.FSConnection;
import org.knime.filehandling.core.connections.FSLocationSpec;
import org.knime.filehandling.core.connections.meta.FSDescriptorRegistry;
import org.knime.filehandling.core.connections.uriexport.URIExporter;
import org.knime.filehandling.core.connections.uriexport.URIExporterConfig;
import org.knime.filehandling.core.connections.uriexport.URIExporterFactory;
import org.knime.filehandling.core.connections.uriexport.URIExporterID;
import org.knime.filehandling.core.connections.uriexport.URIExporterIDs;
import org.knime.filehandling.core.defaultnodesettings.status.DefaultStatusMessage;
import org.knime.filehandling.core.defaultnodesettings.status.StatusMessage;
import org.knime.filehandling.core.port.FileSystemPortObjectSpec;

/**
 * An abstract class which encapsulates common functions useful in setting up {@link URIExporter}s.
 *
 * @author Bjoern Lohrmann, KNIME GmbH
 */
public abstract class AbstractURIExporterModelHelper {

    /**
     * Model that holds the currently chosen {@link URIExporterID}.
     */
    protected final SettingsModelString m_selectedExporterID;

    /**
     * Index of the file system connection port, or -1 if the port has not been added.
     */
    protected final int m_fileSystemPortIndex;

    /**
     * {@link PortObjectSpec} array of the node.
     */
    protected PortObjectSpec[] m_portObjectSpecs;

    private NodeSettingsRO m_nodeSettings;

    /**
     * Constructor.
     *
     * @param selectedUriExporterModel selected URI exporter model
     * @param fileSystemPortIndex file system port index
     */
    protected AbstractURIExporterModelHelper(final SettingsModelString selectedUriExporterModel, //
        final int fileSystemPortIndex) {
        m_selectedExporterID = selectedUriExporterModel;
        m_fileSystemPortIndex = fileSystemPortIndex;
    }

    /**
     * Set port object specifications.
     *
     * @param specs {@link PortObjectSpec}
     */
    public final void setPortObjectSpecs(final PortObjectSpec[] specs) {
        m_portObjectSpecs = specs;
    }

    /**
     * Validates input port, selected {@link URIExporterID}.
     *
     * @param warningMessageConsumer message consumer
     * @param overwriteInvalidSettings whether to ignore invalid settings
     * @throws InvalidSettingsException if configurations are invalid
     */
    public abstract void validate(final Consumer<StatusMessage> warningMessageConsumer, final boolean overwriteInvalidSettings)
        throws InvalidSettingsException;

    /**
     * Validates that a file system can be retrieved if a file system input port is connected.
     *
     * @throws InvalidSettingsException If no file system could be retrieved although the port is connected.
     */
    protected void validateFileSystemPort() throws InvalidSettingsException {
        final var fsPortObjectSpec = getFileSystemPortObjectSpec();
        if (fsPortObjectSpec != null) {
            final Optional<FSConnection> portObjectConnection = fsPortObjectSpec.getFileSystemConnection();
            CheckUtils.checkSetting(portObjectConnection.isPresent(), //
                "No file system connection available. Please execute the connector node first.");
        }
    }

    /**
     * Return specific unsupported exporter message.
     *
     * @param exporterId {@link URIExporterID}
     * @return unsupported exporter message
     */
    protected abstract String getUnsupportedExporterMessage(final URIExporterID exporterId);

    private String createUnsupportedExporterMessage(final URIExporterID exporterId) {
        final String msg;
        final Optional<String> connectedFsType =
            FileSystemPortObjectSpec.getFileSystemType(m_portObjectSpecs, m_fileSystemPortIndex);
        if (connectedFsType.isPresent()) {
            msg = String.format("The chosen URL format '%s' is not supported by the connected file system '%s'.", //
                exporterId, connectedFsType.get());
        } else {
            msg = getUnsupportedExporterMessage(exporterId);
        }
        return msg;
    }

    /**
     * Validates that the configured {@link URIExporterID} is applicable to the file system(s). If the
     * {@link URIExporterID} is blank, then it will be autoguessed.
     *
     * @param warningMessageConsumer Consumes warning messages about auto-guessing.
     * @param overwriteInvalidSettings Whether a non-applicable {@link URIExporterID} should be overriden by
     *            autoguessing.
     * @throws InvalidSettingsException If no {@link URIExporterID} could be guessed because overwriteInvalidSettings is
     *             false.
     */
    protected void validateURIExporter(final Consumer<StatusMessage> warningMessageConsumer,
        final boolean overwriteInvalidSettings) throws InvalidSettingsException {

        if (StringUtils.isBlank(m_selectedExporterID.getStringValue())) {
            autoGuessURIExporter(warningMessageConsumer);
            return;
        }

        final var selectedID = new URIExporterID(m_selectedExporterID.getStringValue());
        final boolean exporterIsUnsupported = !getURIExporterIDToFactory().containsKey(selectedID);

        if (exporterIsUnsupported) {
            if (overwriteInvalidSettings) {
                autoGuessURIExporter(warningMessageConsumer);
            } else {
                throw new InvalidSettingsException(createUnsupportedExporterMessage(selectedID));
            }
        }
    }

    private void autoGuessURIExporter(final Consumer<StatusMessage> warningMessageConsumer) {
        m_selectedExporterID.setStringValue(URIExporterIDs.DEFAULT.toString());
        warningMessageConsumer.accept(DefaultStatusMessage.mkWarning("Auto-guessed default URL format"));
    }

    /**
     * @return FileSystemPortObjectSpec of the connected file system otherwise return null
     */
    protected FileSystemPortObjectSpec getFileSystemPortObjectSpec() {
        if (m_fileSystemPortIndex != -1) {
            return (FileSystemPortObjectSpec)m_portObjectSpecs[m_fileSystemPortIndex];
        } else {
            return null;
        }
    }

    /**
     * @return {@link FSLocationSpec}s according to input port or data.
     */
    protected abstract Set<FSLocationSpec> getFSLocationSpecs();

    /**
     * Returns a map of URIExporterID to URIExporterFactory
     *
     * @return A map of URIExporterID to URIExporterFactory
     */
    public Map<URIExporterID, URIExporterFactory> getURIExporterIDToFactory() {
        final var listOfMaps = getFSLocationSpecs().stream() //
                .map(AbstractURIExporterModelHelper::fetchURIExporterIDToFactory) //
                .collect(Collectors.toList());

        final Map<URIExporterID, URIExporterFactory> result = listOfMaps.isEmpty() ? Map.of() : listOfMaps.get(0);
        if (listOfMaps.size() > 1) {
            for (Map<URIExporterID, URIExporterFactory> idToFactory : listOfMaps.subList(1, listOfMaps.size())) {
                result.keySet().retainAll(idToFactory.keySet());
            }
        }
        return result;
    }

    private static Map<URIExporterID, URIExporterFactory> fetchURIExporterIDToFactory(final FSLocationSpec spec) {
        final var descriptor = FSDescriptorRegistry.getFSDescriptor(spec.getFSType());
        CheckUtils.checkArgument(descriptor.isPresent(), //
            "No file system descriptor available for " + spec.getFSType().getName());

        return descriptor.get().getURIExporters().stream() // NOSONAR we check before
                .collect(Collectors.toMap(Function.identity(), id -> descriptor.get().getURIExporterFactory(id)));
    }

    /**
     * Used in model to save exporter settings.
     *
     * @param settings exporter settings
     */
    public void saveSettingsTo(final NodeSettingsWO settings) {
        if (m_nodeSettings != null) {
            m_nodeSettings.copyTo(settings);
        }
    }

    /**
     * Used in model to load exporter settings.
     *
     * @param exporterConfig exporter settings
     * @throws InvalidSettingsException in case of an invalid settings
     */
    public void loadSettingsFrom(final NodeSettingsRO exporterConfig) throws InvalidSettingsException {
        //store a local copy so that settings can be loaded
        m_nodeSettings = exporterConfig;
    }

    /**
     * Creates {@link URIExporter} based on {@link FSConnection}.
     *
     * @param connection {@link FSConnection}
     * @return created URI exporter
     * @throws InvalidSettingsException
     */
    public URIExporter createExporter(final FSConnection connection) throws InvalidSettingsException {
        final var selectedID = new URIExporterID(m_selectedExporterID.getStringValue());
        final var factory = connection.getURIExporterFactory(selectedID);
        final URIExporterConfig config = factory.initConfig();
        config.loadSettingsForExporter(m_nodeSettings);
        config.validate();
        return factory.createExporter(config);
    }
}
