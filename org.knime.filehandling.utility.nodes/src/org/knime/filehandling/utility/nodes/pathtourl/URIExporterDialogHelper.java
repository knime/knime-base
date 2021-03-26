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
 *   Mar 19, 2021 (Bjoern Lohrmann, KNIME GmbH): created
 */
package org.knime.filehandling.utility.nodes.pathtourl;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.util.CheckUtils;
import org.knime.filehandling.core.connections.DefaultFSLocationSpec;
import org.knime.filehandling.core.connections.FSCategory;
import org.knime.filehandling.core.connections.FSConnection;
import org.knime.filehandling.core.connections.FSLocation;
import org.knime.filehandling.core.connections.FSLocationSpec;
import org.knime.filehandling.core.connections.uriexport.URIExporterConfig;
import org.knime.filehandling.core.connections.uriexport.URIExporterFactory;
import org.knime.filehandling.core.connections.uriexport.URIExporterID;
import org.knime.filehandling.core.connections.uriexport.URIExporterMetaInfo;
import org.knime.filehandling.core.connections.uriexport.URIExporterPanel;
import org.knime.filehandling.core.data.location.FSLocationValueMetaData;
import org.knime.filehandling.core.defaultnodesettings.FileSystemHelper;

/**
 *
 * @author Bjoern Lohrmann, KNIME GmbH
 */
public class URIExporterDialogHelper extends AbstractURIExporterHelper {

    private final Map<URIExporterID, URIExporterFactory> m_availableExporterFactories;

    private final Map<URIExporterID, URIExporterConfig> m_exporterConfigs;

    private final Map<URIExporterID, URIExporterPanel> m_exporterPanels;

    URIExporterDialogHelper(final SettingsModelString selectedColumn, //
        final SettingsModelString selectedUriExporterModel, //
        final int fileSystemPortIndex, //
        final int dataTablePortIndex) {

        super(selectedColumn, selectedUriExporterModel, fileSystemPortIndex, dataTablePortIndex);
        m_availableExporterFactories = new HashMap<>();
        m_exporterConfigs = new HashMap<>();
        m_exporterPanels = new HashMap<>();
    }

    @Override
    public void init() {
        initAvailableExporterFactories();
        initExporterConfigsAndPanels();
    }

    private void initExporterConfigsAndPanels() {
        m_exporterConfigs.clear();
        m_exporterPanels.clear();

        for (Entry<URIExporterID, URIExporterFactory> entry : m_availableExporterFactories.entrySet()) {
            final URIExporterConfig config = entry.getValue().initConfig();
            final URIExporterPanel panel = entry.getValue().createPanel(config);
            loadDefaultConfigViaPanel(config, panel);
            m_exporterConfigs.put(entry.getKey(), config);
            m_exporterPanels.put(entry.getKey(), panel);
        }
    }

    private void loadDefaultConfigViaPanel(final URIExporterConfig config, final URIExporterPanel panel) {
        final NodeSettings toLoad = new NodeSettings("defaults");
        // save default values if there is nothing to load. This is so we can load the exporter config
        // through DialogComponents in the exporter panel.
        config.saveSettingsForExporter(toLoad);
        try {
            panel.loadSettingsFrom(toLoad, m_portObjectSpecs);
        } catch (NotConfigurableException e) {
            throw new IllegalStateException("URIExporterPanel failed to load default settings. This is a bug.", e);
        }
    }

    private void initAvailableExporterFactories() {
        m_availableExporterFactories.clear();

        // Get a list of FSConnection, maybe 1 or more
        List<FSConnection> connections = null;
        try {
            connections = getListOfConnections();
            if (!connections.isEmpty()) {
                //fetch common uri exporters between all the FSConnections
                for (URIExporterID exporterID : getCommonURIExporterIDs(connections)) {
                    m_availableExporterFactories.put(exporterID, connections.get(0).getURIExporterFactory(exporterID));
                }
            }

        } finally {
            if (connections != null) {
                closeQuietly(connections);
            }
        }
    }

    private static void closeQuietly(final List<FSConnection> connections) {
        connections.stream().forEach(c -> {
            try {
                c.close();
            } catch (IOException e) { // NOSONAR never happens, can be ignored
            }
        });
    }

    /**
     * Return a set of the common URIExporterIDs in a list of FSConnection's list
     *
     * @param connections A list of FSConnection objects
     * @return A set of URIExporterID objects with common URIExporters in the incoming FSConnection's list
     */
    private static Set<URIExporterID> getCommonURIExporterIDs(final List<FSConnection> connections) {
        @SuppressWarnings("resource")
        final Set<URIExporterID> commonIDs = new HashSet<>(connections.get(0).getURIExporterIDs());

        for (FSConnection connection : connections.subList(1, connections.size())) {
            commonIDs.retainAll(connection.getURIExporterIDs());
        }

        // This should never happen, because all file system should have the default exporter
        // in common
        CheckUtils.checkState(!commonIDs.isEmpty(), "No URL formats available.");

        return commonIDs;
    }

    /**
     * Returns a list of FSConnections’s based on either the connected FSPortObject or initializing FSConnections based
     * on the column spec
     *
     * @param specs PortObjectSpec array
     * @param pathColumnSpec A DataColumnSpec object for the Path column
     * @return A list of FSConnection’s
     * @throws InvalidSettingsException Throws exception if FSConnection cannot be initialized
     */
    @SuppressWarnings("resource")
    private List<FSConnection> getListOfConnections() {
        if (getFileSystemPortObjectSpec() != null) {
            return Collections.singletonList(getFileSystemPortObjectSpec().getFileSystemConnection().get()); // NOSONAR we check before
        } else {
            return getConvenienceConnections();
        }
    }

    private List<FSConnection> getConvenienceConnections() {
        //use path column meta data to ad-hoc instantiate FSConnections
        final DataColumnSpec pathColSpec = getPathColumnSpec();
        final Set<DefaultFSLocationSpec> setOflocationSpecs =
            pathColSpec.getMetaDataOfType(FSLocationValueMetaData.class) //
                .orElseThrow(IllegalStateException::new) //
                .getFSLocationSpecs();

        return setOflocationSpecs.stream() //
            .map(URIExporterDialogHelper::createPseudoConnection) //
            .collect(Collectors.toList());
    }

    /**
     * Creates a pseudo convenience FSConnection objects using fake paths by properties from FSLocationSpec parameter.
     * In case of CUSTOM_URL use a placeholder URL, since only the URI Exporters are used the provided URL is
     * inconsequential.
     *
     * @param locationSpec Instance of FSLocationSpec
     * @return Optional<FSConnection> An object of FSConnection
     */
    private static FSConnection createPseudoConnection(final FSLocationSpec locationSpec) {

        final Optional<String> fileSysSpecifier = locationSpec.getFileSystemSpecifier();
        final String fakePathStringVal =
            locationSpec.getFSCategory() == FSCategory.CUSTOM_URL ? "https://www.knime.com/" : ".";
        final FSLocation fakeFSLocation =
            new FSLocation(locationSpec.getFSCategory(), fileSysSpecifier.orElse(null), fakePathStringVal);

        return FileSystemHelper.retrieveFSConnection(Optional.empty(), fakeFSLocation).get(); // NOSONAR
    }

    @Override
    void loadSettingsFrom(final NodeSettingsRO settings) {
        final URIExporterID selectedID = new URIExporterID(m_selectedExporterID.getStringValue());

        if (settings.getChildCount() > 0 && isAvailable(selectedID)) {
            try {
                m_exporterPanels.get(selectedID).loadSettingsFrom(settings, m_portObjectSpecs);
            } catch (NotConfigurableException e) {
                // FIXME settings did not apply, set error msg (maybe? not sure yet)
            }
        }
    }

    void saveSettingsTo(final NodeSettingsWO addNodeSettings) throws InvalidSettingsException {
        final URIExporterID selectedID = new URIExporterID(m_selectedExporterID.getStringValue());

        if (!isAvailable(selectedID)) {
            throw new InvalidSettingsException(
                "Chosen URL format is not available for given file system connection or path column.");
        }
        m_exporterPanels.get(selectedID).saveSettingsTo(addNodeSettings);
    }

    Map<URIExporterID, URIExporterFactory> getAvailableExporterFactories() {
        return m_availableExporterFactories;
    }

    URIExporterPanel getExporterPanel(final URIExporterID exporterID) {
        return m_exporterPanels.get(exporterID);
    }

    public boolean isAvailable(final URIExporterID exporterID) {
        return m_availableExporterFactories.containsKey(exporterID);
    }

    public URIExporterMetaInfo getExporterMetaInfo(final URIExporterID exporterID) {
        return m_availableExporterFactories.get(exporterID).getMetaInfo();
    }

}
