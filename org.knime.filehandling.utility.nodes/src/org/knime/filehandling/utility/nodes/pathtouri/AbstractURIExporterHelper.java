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
package org.knime.filehandling.utility.nodes.pathtouri;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.util.CheckUtils;
import org.knime.filehandling.core.connections.DefaultFSLocationSpec;
import org.knime.filehandling.core.connections.FSCategory;
import org.knime.filehandling.core.connections.FSConnection;
import org.knime.filehandling.core.connections.FSLocationSpec;
import org.knime.filehandling.core.connections.meta.FSDescriptorRegistry;
import org.knime.filehandling.core.connections.uriexport.URIExporter;
import org.knime.filehandling.core.connections.uriexport.URIExporterFactory;
import org.knime.filehandling.core.connections.uriexport.URIExporterID;
import org.knime.filehandling.core.connections.uriexport.URIExporterIDs;
import org.knime.filehandling.core.data.location.FSLocationValue;
import org.knime.filehandling.core.data.location.FSLocationValueMetaData;
import org.knime.filehandling.core.defaultnodesettings.status.DefaultStatusMessage;
import org.knime.filehandling.core.defaultnodesettings.status.StatusMessage;
import org.knime.filehandling.core.port.FileSystemPortObjectSpec;
import org.knime.filehandling.core.util.FSLocationColumnUtils;

/**
 * An abstract class which encapsulates common functions useful in setting up {@link URIExporter}s.
 *
 * @author Bjoern Lohrmann, KNIME GmbH
 */
abstract class AbstractURIExporterHelper {

    /**
     * Model that holds the currently chosen {@link URIExporterID}.
     */
    protected final SettingsModelString m_selectedExporterID;

    /**
     * Model that holds the currently chosen path column.
     */
    protected final SettingsModelString m_selectedColumn;

    /**
     * Index of the data table port.
     */
    protected final int m_dataTablePortIndex;

    /**
     * Index of the file system connection port, or -1 if the port has not been added.
     */
    protected final int m_fileSystemPortIndex;

    /**
     * {@link PortObjectSpec} array of the node.
     */
    protected PortObjectSpec[] m_portObjectSpecs;

    AbstractURIExporterHelper(final SettingsModelString selectedColumn, //
        final SettingsModelString selectedUriExporterModel, //
        final int fileSystemPortIndex, //
        final int dataTablePortIndex) {

        CheckUtils.checkArgument(dataTablePortIndex != -1, "Data table port index is required.");

        m_selectedColumn = selectedColumn;
        m_selectedExporterID = selectedUriExporterModel;
        m_dataTablePortIndex = dataTablePortIndex;
        m_fileSystemPortIndex = fileSystemPortIndex;
    }

    final void setPortObjectSpecs(final PortObjectSpec[] specs) {
        m_portObjectSpecs = specs;
    }

    final void validate(final Consumer<StatusMessage> warningMessageConsumer, final boolean overwriteInvalidSettings)
        throws InvalidSettingsException {
        validateFileSystemPort();
        validatePathColumn(warningMessageConsumer, overwriteInvalidSettings);
        validatePathColumnIsUsable(warningMessageConsumer);
        validateURIExporter(warningMessageConsumer, overwriteInvalidSettings);
    }

    /**
     * Validates that the input column is set, exists and is a path column. A column will be auto-guessed if either the
     * currently selected column is blank, or if it does not exist in the input table and overwriteInvalidSettings is
     * true.
     *
     * @param warningMessageConsumer Consumes warning messages about column auto-guessing.
     * @param overwriteInvalidSettings Whether a non-existent column should be overriden by autoguessing.
     * @throws InvalidSettingsException If no column could be guessed, either because there is none, or when
     *             overwriteInvalidSettings is false.
     */
    protected void validatePathColumn(final Consumer<StatusMessage> warningMessageConsumer,
        final boolean overwriteInvalidSettings) throws InvalidSettingsException {

        // empty settings are always auto-guessed
        if (StringUtils.isBlank(m_selectedColumn.getStringValue())) {
            autoGuessPathColumn(getDataTableSpec(), warningMessageConsumer);
            return;
        }

        String errorMessage = null;
        final DataColumnSpec pathColSpec = getPathColumnSpec();
        if (pathColSpec == null) {
            errorMessage =
                String.format("The selected column '%s' is not part of the input", m_selectedColumn.getStringValue());
        } else if (!pathColSpec.getType().isCompatible(FSLocationValue.class)) {
            errorMessage =
                String.format("The selected column '%s' has the wrong type", m_selectedColumn.getStringValue());
        }

        if (errorMessage != null) {
            if (overwriteInvalidSettings) {
                autoGuessPathColumn(getDataTableSpec(), warningMessageConsumer);
            } else {
                throw new InvalidSettingsException(errorMessage);
            }
        }
    }

    /**
     * Automatically select the first column in the input table which matches the expected type.
     *
     * @param inSpecs An array of {@link PortObjectSpec}s.
     * @throws InvalidSettingsException If no column is found with desired data type.
     */
    private void autoGuessPathColumn(final DataTableSpec inputTableSpec,
        final Consumer<StatusMessage> warningMessageConsumer) throws InvalidSettingsException {
        m_selectedColumn.setStringValue(inputTableSpec.stream()//
            .filter(dcs -> dcs.getType().isCompatible(FSLocationValue.class))//
            .map(DataColumnSpec::getName)//
            .findFirst()//
            .orElseThrow(() -> new InvalidSettingsException("No path column available"))//
        );
        warningMessageConsumer.accept(
            DefaultStatusMessage.mkWarning("Auto-guessed input column '%s'", m_selectedColumn.getStringValue()));
    }

    /**
     * Validates that the configured path column is compatible with the (optional) port object file system connection.
     *
     * @param warningMessageConsumer
     * @throws InvalidSettingsException
     */
    protected void validatePathColumnIsUsable(final Consumer<StatusMessage> warningMessageConsumer)
        throws InvalidSettingsException {

        final Optional<String> warningMsg =
            FSLocationColumnUtils.validateFSLocationColumn(getPathColumnSpec(), getFileSystemPortObjectSpec());
        if (warningMsg.isPresent()) {
            warningMessageConsumer.accept(DefaultStatusMessage.mkWarning(warningMsg.get()));
        }

        // FSLocationColumnUtils.validateFSLocationColumn() only warns when the column metadata contains a CONNECTED
        // FSLocationSpec, but no port object connection is available. However we need to fail because we cannot open
        // the dialog.
        if (m_fileSystemPortIndex == -1) {
            final FSLocationSpec connectedSpec = getPathColumnSpec().getMetaDataOfType(FSLocationValueMetaData.class) //
                .orElseThrow(IllegalStateException::new) //
                .getFSLocationSpecs() //
                .stream() //
                .filter(spec -> spec.getFSCategory() == FSCategory.CONNECTED) //
                .findAny() //
                .orElse(null);

            if (connectedSpec != null) {
                throw new InvalidSettingsException(String.format(
                    "The selected column '%s' seems to contain a path referencing a file system that requires to be connected (%s).", //
                    m_selectedColumn.getStringValue(), //
                    connectedSpec.getFileSystemSpecifier().orElse("")));
            }
        }
    }

    /**
     * Validates that a file system can be retrieved if a file system input port is connected.
     *
     * @throws InvalidSettingsException If no file system could be retrieved although the port is connected.
     */
    protected void validateFileSystemPort() throws InvalidSettingsException {
        final FileSystemPortObjectSpec fsPortObjectSpec = getFileSystemPortObjectSpec();
        if (fsPortObjectSpec != null) {
            final Optional<FSConnection> portObjectConnection = fsPortObjectSpec.getFileSystemConnection();
            CheckUtils.checkSetting(portObjectConnection.isPresent(), //
                "No file system connection available. Please execute the connector node first.");
        }
    }

    private String createUnsupportedExporterMessage(final URIExporterID exporterId) {
        final String msg;
        final Optional<String> connectedFsType =
            FileSystemPortObjectSpec.getFileSystemType(m_portObjectSpecs, m_fileSystemPortIndex);
        if (connectedFsType.isPresent()) {
            msg = String.format("The chosen URL format '%s' is not supported by the connected file system '%s'.", //
                exporterId, connectedFsType.get());
        } else {
            msg = String.format("The chosen URL format '%s' is not supported by the file system(s) in column '%s'.", //
                exporterId, getPathColumnSpec().getName());
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

        final URIExporterID exporterId = getSelectedExporterId();
        final boolean exporterIsUnsupported = !getURIExporterIDToFactory().containsKey(exporterId);

        if (exporterIsUnsupported) {
            if (overwriteInvalidSettings) {
                autoGuessURIExporter(warningMessageConsumer);
            } else {
                throw new InvalidSettingsException(createUnsupportedExporterMessage(exporterId));
            }
        }
    }

    private void autoGuessURIExporter(final Consumer<StatusMessage> warningMessageConsumer) {
        m_selectedExporterID.setStringValue(URIExporterIDs.DEFAULT.toString());
        warningMessageConsumer.accept(DefaultStatusMessage.mkWarning("Auto-guessed default URL format"));
    }

    /**
     * @return the selectedExporterID
     */
    protected URIExporterID getSelectedExporterId() {
        return new URIExporterID(m_selectedExporterID.getStringValue());
    }

    /**
     *
     * @return the {@link DataTableSpec} of the ingoing data table.
     */
    protected DataTableSpec getDataTableSpec() {
        return (DataTableSpec)m_portObjectSpecs[m_dataTablePortIndex];
    }

    /**
     * @return DataColumnSpec of the selected Path column
     */
    protected DataColumnSpec getPathColumnSpec() {
        return getDataTableSpec().getColumnSpec(m_selectedColumn.getStringValue());
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
     * Returns a map of URIExporterID to URIExporterFactory
     *
     * @return A map of URIExporterID to URIExporterFactory
     */
    protected Map<URIExporterID, URIExporterFactory> getURIExporterIDToFactory() {
        if (getFileSystemPortObjectSpec() != null) {
            return fetchURIExporterIDToFactory(getFileSystemPortObjectSpec().getFSLocationSpec());
        } else {
            return getCommonURIExporterIDToFactory();
        }
    }

    /**
     * @return whether the selected column has connected FS Locations or not
     */
    protected boolean containsConnectedFSLocation() {
        final DataColumnSpec pathColSpec = getPathColumnSpec();
        final Set<DefaultFSLocationSpec> setOflocationSpecs =
            pathColSpec.getMetaDataOfType(FSLocationValueMetaData.class) //
                .orElseThrow(IllegalStateException::new) //
                .getFSLocationSpecs();

        return !setOflocationSpecs.stream().filter(spec -> spec.getFSCategory() == FSCategory.CONNECTED)
            .collect(Collectors.toList()).isEmpty();
    }

    private Map<URIExporterID, URIExporterFactory> getCommonURIExporterIDToFactory() {
        //use path column meta data to ad-hoc instantiate FSConnections
        final DataColumnSpec pathColSpec = getPathColumnSpec();
        final Set<DefaultFSLocationSpec> setOflocationSpecs =
            pathColSpec.getMetaDataOfType(FSLocationValueMetaData.class) //
                .orElseThrow(IllegalStateException::new) //
                .getFSLocationSpecs();

        final var listOfMaps = setOflocationSpecs.stream() //
                .map(AbstractURIExporterHelper::fetchURIExporterIDToFactory) //
                .collect(Collectors.toList());
        final Map<URIExporterID, URIExporterFactory> result = listOfMaps.get(0);

        if (listOfMaps.size() > 1) {
            for (Map<URIExporterID, URIExporterFactory> idToFactory : listOfMaps.subList(1, listOfMaps.size())) {
                result.keySet().retainAll(idToFactory.keySet());
            }
        }
        // This should never happen, because all file system should have the default exporter
        // in common
        CheckUtils.checkState(!result.isEmpty(), "No URL formats available.");
        return result;
    }

    private static Map<URIExporterID, URIExporterFactory> fetchURIExporterIDToFactory(final FSLocationSpec spec) {
        final var descriptor = FSDescriptorRegistry.getFSDescriptor(spec.getFSType());
        CheckUtils.checkArgument(descriptor.isPresent(), //
            "No file system descriptor available for " + spec.getFSType().getName());

        return descriptor.get().getURIExporters().stream() // NOSONAR we check before
                .collect(Collectors.toMap(Function.identity(), id -> descriptor.get().getURIExporterFactory(id)));
    }

    abstract void loadSettingsFrom(final NodeSettingsRO exporterConfig) throws InvalidSettingsException;
}
