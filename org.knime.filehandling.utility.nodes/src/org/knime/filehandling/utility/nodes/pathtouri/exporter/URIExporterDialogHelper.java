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
package org.knime.filehandling.utility.nodes.pathtouri.exporter;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Supplier;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.filehandling.core.connections.uriexport.URIExporterConfig;
import org.knime.filehandling.core.connections.uriexport.URIExporterFactory;
import org.knime.filehandling.core.connections.uriexport.URIExporterID;
import org.knime.filehandling.core.connections.uriexport.URIExporterMetaInfo;
import org.knime.filehandling.core.connections.uriexport.URIExporterPanel;

/**
 * URI exporter dialog helper.
 *
 * @author Bjoern Lohrmann, KNIME GmbH
 */
public final class URIExporterDialogHelper {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(URIExporterDialogHelper.class);

    private final Supplier<Map<URIExporterID, URIExporterFactory>> m_availableExporterFactorySupplier;

    private final Map<URIExporterID, URIExporterConfig> m_exporterConfigs;

    private final Map<URIExporterID, URIExporterPanel> m_exporterPanels;

    /**
     * {@link PortObjectSpec} array of the node.
     */
    private PortObjectSpec[] m_portObjectSpecs;

    /**
     * Constructor.
     *
     * @param exporterFactorySupplier supplier of URI exporter factories
     */
    public URIExporterDialogHelper(final Supplier<Map<URIExporterID, URIExporterFactory>> exporterFactorySupplier) {
        m_availableExporterFactorySupplier = exporterFactorySupplier;
        m_exporterConfigs = new HashMap<>();
        m_exporterPanels = new HashMap<>();
    }

    private static void loadDefaultConfigViaPanel(final URIExporterFactory factory, final URIExporterPanel panel,
        final PortObjectSpec[] specs) {
        final var toLoad = new NodeSettings("defaults");
        // save default values if there is nothing to load. This is so we can load the exporter config
        // through DialogComponents in the exporter panel.
        final URIExporterConfig tmpDefaultConfig = factory.initConfig();
        tmpDefaultConfig.saveSettingsForExporter(toLoad);
        try {
            // this loads the default values into the panel and its underlying URIExporterConfig object
            panel.loadSettingsFrom(toLoad, specs);
        } catch (NotConfigurableException e) {
            throw new IllegalStateException("URIExporterPanel failed to load default settings. This is a bug.", e);
        }
    }

    /**
     * Initializes exporter configurations.
     */
    public void initExporterConfigsAndPanels() {
        m_exporterConfigs.clear();
        m_exporterPanels.clear();

        for (Entry<URIExporterID, URIExporterFactory> entry : m_availableExporterFactorySupplier.get().entrySet()) {
            final URIExporterConfig config = entry.getValue().initConfig();
            final URIExporterPanel panel = entry.getValue().createPanel(config);
            loadDefaultConfigViaPanel(entry.getValue(), panel, m_portObjectSpecs);
            m_exporterConfigs.put(entry.getKey(), config);
            m_exporterPanels.put(entry.getKey(), panel);
        }
    }

    /**
     * Load selected exporter settings.
     *
     * @param settings {@link NodeSettingsRO}
     * @param selectedID selected exporter ID
     * @param specs {@link PortObjectSpec}s
     */
    public void loadSettingsFrom(final NodeSettingsRO settings,
        final URIExporterID selectedID, final PortObjectSpec[] specs) {

        m_portObjectSpecs = specs;
        initExporterConfigsAndPanels();

        if (settings.getChildCount() > 0 && isAvailable(selectedID)) {
            try {
                m_exporterPanels.get(selectedID).loadSettingsFrom(settings, m_portObjectSpecs);
            } catch (NotConfigurableException ex) {
                LOGGER.warn(String.format("Unable to load saved settings for the URL format %s, reverting to defaults.",
                    selectedID), ex);
                loadDefaultConfigViaPanel(m_availableExporterFactorySupplier.get().get(selectedID),
                    m_exporterPanels.get(selectedID), m_portObjectSpecs);
            }
        }
    }

    /**
     * Save selected exporter settings.
     *
     * @param addNodeSettings {@link NodeSettingsWO}
     * @param selectedID selected exporter ID
     * @param errorMessage specific error message if selectedID is unavailable
     * @throws InvalidSettingsException if settings are invalid
     */
    public void saveSettingsTo(final NodeSettingsWO addNodeSettings,
        final URIExporterID selectedID, final String errorMessage) throws InvalidSettingsException {

        if (!isAvailable(selectedID)) {
            throw new InvalidSettingsException(errorMessage);
        }
        m_exporterPanels.get(selectedID).saveSettingsTo(addNodeSettings);
    }

    /**
     * Get map of exporter id to factory.
     *
     * @return map of exporter factories
     */
    public Map<URIExporterID, URIExporterFactory> getAvailableExporterFactories() {
        return m_availableExporterFactorySupplier.get();
    }

    /**
     * Get map of exporter id to panel.
     *
     * @return map of exporter panels
     */
    public Map<URIExporterID, URIExporterPanel> getAvailableExporterPanels() {
        return m_exporterPanels;
    }

    /**
     * Check whether exporter id is available.
     *
     * @param exporterID {@link URIExporterID}
     * @return true if exporter id is available, otherwise false
     */
    public boolean isAvailable(final URIExporterID exporterID) {
        return m_availableExporterFactorySupplier.get().containsKey(exporterID);
    }

    /**
     * Get meta info of exporter.
     *
     * @param exporterID {@link URIExporterID}
     * @return meta info
     */
    public URIExporterMetaInfo getExporterMetaInfo(final URIExporterID exporterID) {
        return m_availableExporterFactorySupplier.get().get(exporterID).getMetaInfo();
    }

}
