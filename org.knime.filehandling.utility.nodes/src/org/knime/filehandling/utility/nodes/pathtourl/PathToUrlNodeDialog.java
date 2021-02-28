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
 *   Dec 22, 2020 (Ayaz Ali Qureshi, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.filehandling.utility.nodes.pathtourl;

import java.awt.Component;
import java.awt.GridBagLayout;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.StringValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.context.ports.PortsConfiguration;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentLabel;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.util.SwingWorkerWithContext;
import org.knime.filehandling.core.connections.DefaultFSLocationSpec;
import org.knime.filehandling.core.connections.FSConnection;
import org.knime.filehandling.core.connections.uriexport.URIExporter;
import org.knime.filehandling.core.connections.uriexport.URIExporterID;
import org.knime.filehandling.core.connections.uriexport.URIExporterIDs;
import org.knime.filehandling.core.connections.uriexport.URIExporterPanel;
import org.knime.filehandling.core.data.location.FSLocationValue;
import org.knime.filehandling.core.data.location.FSLocationValueMetaData;
import org.knime.filehandling.core.port.FileSystemPortObjectSpec;
import org.knime.filehandling.core.util.FSLocationColumnUtils;
import org.knime.filehandling.core.util.GBCBuilder;

/**
 *
 * @author Ayaz Ali Qureshi, KNIME GmbH, Berlin, Germany
 */
public class PathToUrlNodeDialog extends NodeDialogPane {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(PathToUrlNodeDialog.class);

    private final SettingsModelString m_selectedUriExporterModel;

    private final PathToUrlNodeConfig m_config;

    private final DialogComponentColumnNameSelection m_selectedColumnNameComponent;

    private final DialogComponentString m_appendColumnNameComponent;

    private final DialogComponentColumnNameSelection m_replaceColumnNameComponent;

    private URIExporterPanel<URIExporter> m_selectedUriExporterSettingsPanel;

    private final URIExporterSelectorPanel m_uriExporterSelectorPanel;

    private Component m_uriExporterSettingsPanel;

    private DialogComponentLabel m_selectedUriExporterDescComponent;

    private DialogComponentLabel m_selectedURIExporterNoSettingsLabel;

    private final JRadioButton m_generateRadio;

    private final JRadioButton m_replaceColumnRadio;

    private NodeSettingsRO m_nodeSettings;

    private PortObjectSpec[] m_portObjectSpecs;

    private static final String URL_FORMAT_LABEL = "URL Format";

    private static final String SELECTED_COLUMN_LABEL = "Path Column";

    private static final String NO_ADD_SETTING_REQ_LABEL = "No additional settings required for selected URI Exporter";

    /**
     * Initialize component and attach appropriate event listeners
     *
     * @param portsConfiguration PortsConfiguration object
     * @param nodeConfig {@link PathToUrlNodeConfig} object
     */
    @SuppressWarnings("unchecked")
    public PathToUrlNodeDialog(final PortsConfiguration portsConfiguration, final PathToUrlNodeConfig nodeConfig) {
        m_config = nodeConfig;
        m_selectedUriExporterModel = nodeConfig.getSelectedUriExporterModel();

        m_selectedColumnNameComponent = new DialogComponentColumnNameSelection(nodeConfig.getSelectedColumnNameModel(),
            SELECTED_COLUMN_LABEL, nodeConfig.getDataTablePortIndex(), FSLocationValue.class);

        m_appendColumnNameComponent = new DialogComponentString(nodeConfig.getAppendColumnNameModel(), "", true, 25);

        m_replaceColumnNameComponent = new DialogComponentColumnNameSelection(nodeConfig.getReplaceColumnNameModel(),
            "", nodeConfig.getDataTablePortIndex(), false, StringValue.class);

        //the description is updated through URI Exporter selector panel
        m_selectedUriExporterDescComponent = new DialogComponentLabel("");

        m_selectedURIExporterNoSettingsLabel = new DialogComponentLabel(NO_ADD_SETTING_REQ_LABEL);

        m_generateRadio = new JRadioButton("Append Column :");
        m_replaceColumnRadio = new JRadioButton("Replace Column :");

        final ButtonGroup buttonGrp = new ButtonGroup();
        buttonGrp.add(m_generateRadio);
        buttonGrp.add(m_replaceColumnRadio);

        m_generateRadio.addActionListener(l -> checkGeneratedColumnMode());
        m_replaceColumnRadio.addActionListener(l -> checkGeneratedColumnMode());

        m_uriExporterSelectorPanel = new URIExporterSelectorPanel();
        m_selectedUriExporterModel.addChangeListener(e -> updateUriExporterSettingsPanel());

        addTab("Settings", createSettingsDialog());

    }

    private void checkGeneratedColumnMode() {
        m_appendColumnNameComponent.getModel().setEnabled(m_generateRadio.isSelected());
        m_replaceColumnNameComponent.getModel().setEnabled(m_replaceColumnRadio.isSelected());
    }

    /**
     * Update the panel for displaying URI Exporter specific settings, also updates the description.
     */
    @SuppressWarnings("unchecked")
    private void updateUriExporterSettingsPanel() {

        URIExporter uriExporter = m_uriExporterSelectorPanel.getSelectedURIExporter();
        m_selectedUriExporterSettingsPanel = (URIExporterPanel<URIExporter>)uriExporter.getPanel();
        m_selectedUriExporterDescComponent.setText(uriExporter.getDescription());

        if (m_selectedUriExporterSettingsPanel != null) {
            //Invokes repaint on the new settings panel
            final Component uriSettingsPanel = createUriExporterSettings(m_uriExporterSettingsPanel);
            uriSettingsPanel.repaint();
        }

    }

    /**
     * Method to create a JPanel component for Settings Tab
     *
     * @return Component which can be added as Settings Tab in DialogPane
     */
    private Component createSettingsDialog() {
        final JPanel p = new JPanel(new GridBagLayout());

        final GBCBuilder gbc =
            new GBCBuilder().resetPos().setWeightX(1).setWeightY(0).anchorFirstLineStart().fillNone();
        p.add(m_selectedColumnNameComponent.getComponentPanel(), gbc.build());

        gbc.setWeightY(1).incY();

        p.add(createURIExporterSelectorSettings(), gbc.build());

        gbc.incY().fillHorizontal();

        p.add(createOutputSettings(), gbc.build());

        gbc.incY().fillBoth();

        m_uriExporterSettingsPanel = createUriExporterSettings(null);

        p.add(m_uriExporterSettingsPanel, gbc.build());

        p.add(Box.createGlue());

        return p;
    }

    /**
     * Method to create a JPanel component for output settings
     *
     * @return Component which can be added as Settings Tab in DialogPane
     */
    private Component createOutputSettings() {

        final JPanel p = new JPanel(new GridBagLayout());
        p.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Output"));

        final GBCBuilder gbc = new GBCBuilder().resetPos().setWeightX(0).setWeightY(0).anchorLineStart().fillNone();

        gbc.insetLeft(0).insets(0, 0, 0, -30);
        p.add(m_generateRadio, gbc.build());

        gbc.setWeightX(1).incX().insets(0, -30, 0, 0);
        p.add(m_appendColumnNameComponent.getComponentPanel(), gbc.build());

        gbc.setWeightY(1).incY().resetX().insets(0, 0, 0, -30);
        p.add(m_replaceColumnRadio, gbc.build());

        gbc.incX().insets(0, -30, 0, 0);
        p.add(m_replaceColumnNameComponent.getComponentPanel(), gbc.build());

        p.add(Box.createGlue());

        return p;
    }

    /**
     * Method to create a JPanel component for output settings yo
     *
     * @return Component which can be added as Settings Tab in DialogPane
     */
    private Component createURIExporterSelectorSettings() {

        final JPanel p = new JPanel(new GridBagLayout());

        final GBCBuilder gbc =
            new GBCBuilder().resetPos().setWeightX(0).setWeightY(1).anchorLineStart().fillHorizontal();

        //the label is embedded within this JPanel component
        p.add(m_uriExporterSelectorPanel, gbc.build());

        gbc.setWeightX(0).resetX().setWeightY(1).incY().fillNone();
        p.add(m_selectedUriExporterDescComponent.getComponentPanel(), gbc.build());

        p.add(Box.createGlue(), gbc.build());

        return p;
    }

    /**
     * Method to create a JPanel component for URIExporter
     *
     * @return Component which can be added as Settings Tab in DialogPane
     */
    private Component createUriExporterSettings(final Component uriExporterSettingsPanel) {

        final JPanel p;

        if (uriExporterSettingsPanel != null) {
            p = (JPanel)uriExporterSettingsPanel;
            p.removeAll();
        } else {
            p = new JPanel(new GridBagLayout());
        }

        p.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), URL_FORMAT_LABEL));

        final GBCBuilder gbc = new GBCBuilder().resetPos().setWeightX(1).setWeightY(1).anchorLineStart();

        if (m_selectedUriExporterSettingsPanel != null) {
            p.add(m_selectedUriExporterSettingsPanel, gbc.build());
        } else {
            gbc.anchorCenter().fillBoth();
            p.add(m_selectedURIExporterNoSettingsLabel.getComponentPanel(), gbc.build());
        }

        p.add(Box.createGlue());

        return p;
    }

    /**
     * This method extracts the FSConnections to display the list of supported URI Exporters. 1) If a FS port object is
     * explicitly connected to the node, simply get the FSConnection object using FileSystemPortObjectSpec 2) If no FS
     * port object is provided, use the location column meta data to retrieve FSLocationSpec and initialize FSConnection
     * objects using fake paths
     *
     *
     * @param specs An array of PortObjectSpec objects
     * @param settings NodeSettingsRO object
     * @throws InvalidSettingsException If no URI Exporters can be fetched from provided FS port or column meta data
     */
    private void updateListOfUriExporters(final PortObjectSpec[] specs, final NodeSettingsRO settings)
        throws InvalidSettingsException {

        final int fileSysPortIndex = m_config.getFileSystemPortIndex();
        final int dataTablePortIndex = m_config.getDataTablePortIndex();

        final DataTableSpec dataTablePortSpec = (DataTableSpec)specs[dataTablePortIndex];
        final DataColumnSpec pathColumnSpec = dataTablePortSpec.getColumnSpec(m_config.getSelectedColNameStringVal());

        Optional<String> warningMsg = FSLocationColumnUtils.validateFSLocationColumn(pathColumnSpec,
            fileSysPortIndex >= 0 ? (FileSystemPortObjectSpec)specs[fileSysPortIndex] : null);

        if (warningMsg.isPresent()) {
            setToolTip(warningMsg.get(), "Settings");
        }

        PathToUrlNodeDialogSwingWorker pathToUrlNodeDialogSwingWorker =
            new PathToUrlNodeDialogSwingWorker(specs, pathColumnSpec, settings);
        pathToUrlNodeDialogSwingWorker.execute();

    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        m_selectedColumnNameComponent.saveSettingsTo(settings);
        m_appendColumnNameComponent.saveSettingsTo(settings);
        m_replaceColumnNameComponent.saveSettingsTo(settings);
        m_config.saveGenerateColModeForDialog(settings, m_generateRadio.isSelected());
        m_selectedUriExporterModel.saveSettingsTo(settings);

        if (m_selectedUriExporterSettingsPanel != null) {
            m_selectedUriExporterSettingsPanel
                .saveSettingsForDialog(settings.addNodeSettings(URIExporter.CFG_URI_EXPORTER_SETTINGS));
        }
    }

    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final PortObjectSpec[] specs)
        throws NotConfigurableException {
        m_selectedColumnNameComponent.loadSettingsFrom(settings, specs);
        m_appendColumnNameComponent.loadSettingsFrom(settings, specs);
        m_replaceColumnNameComponent.loadSettingsFrom(settings, specs);
        m_config.loadGenerateColModeForDialog(settings);

        //select relevant radio buttons
        m_generateRadio.setSelected(m_config.isGenerateColAppendMode());
        m_replaceColumnRadio.setSelected(!m_config.isGenerateColAppendMode());

        if (m_config.getFileSystemPortIndex() >= 0 && !FileSystemPortObjectSpec
            .getFileSystemConnection(specs, m_config.getFileSystemPortIndex()).isPresent()) {
            //Throw exception if a FS connection is connected on port, but the FS Connection is not connected
            throw new NotConfigurableException("The connection on File System port is not connected.");
        }

        try {
            m_selectedUriExporterModel.loadSettingsFrom(settings);

            if (settings.containsKey(URIExporter.CFG_URI_EXPORTER_SETTINGS)) {
                //hold reference to NodeSettingsRO & PortObjectSpec to be used in onOpen() call
                m_nodeSettings = settings.getNodeSettings(URIExporter.CFG_URI_EXPORTER_SETTINGS);
            }

        } catch (InvalidSettingsException ex) {
            throw new NotConfigurableException(ex.getMessage(), ex);
        }

        m_portObjectSpecs = specs;

    }

    @Override
    public void onOpen() {
        try {
            updateListOfUriExporters(m_portObjectSpecs, m_nodeSettings);
        } catch (InvalidSettingsException e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    //SwingWorkerWithContext for creating a list of FSConnection’s and updating the URI Exporter Selector
    //and description
    //This will not block the main UI thread
    private final class PathToUrlNodeDialogSwingWorker
        extends SwingWorkerWithContext<Map<URIExporterID, URIExporter>, Void> {

        private final PortObjectSpec[] m_specs;

        private final DataColumnSpec m_pathColumnSpec;

        private final NodeSettingsRO m_settings;

        public PathToUrlNodeDialogSwingWorker(final PortObjectSpec[] specs, final DataColumnSpec pathColumnSpec,
            final NodeSettingsRO settings) {
            m_specs = specs;
            m_pathColumnSpec = pathColumnSpec;
            m_settings = settings;
        }

        @Override
        protected Map<URIExporterID, URIExporter> doInBackgroundWithContext()
            throws NotConfigurableException, InvalidSettingsException {

            //Get a list of FSConnection, maybe 1 or more
            final List<FSConnection> listOfConnections = getListOfConnections(m_specs, m_pathColumnSpec);
            if (!listOfConnections.isEmpty()) {

                //fetch common uri exporters between all the FSConnections
                final Set<URIExporterID> listOfExporterIds = getCommonListOfUriExporters(listOfConnections);
                final Map<URIExporterID, URIExporter> mapOfUriExporters = new HashMap<>();
                try (FSConnection fsConnection = listOfConnections.get(0)) {
                    //get the URIExporters
                    listOfExporterIds.forEach(uriExporterId -> mapOfUriExporters.put(uriExporterId,
                        fsConnection.getURIExporter(uriExporterId)));
                    listOfConnections.forEach(FSConnection::closeInBackground);
                    return mapOfUriExporters;
                } catch (IOException ex) {
                    LOGGER.error(ex.getMessage(), ex);
                    throw new NotConfigurableException(ex.getMessage(), ex);
                }

            }
            //Return map with an an empty URI exporter that requires no settings
            return Collections.singletonMap(URIExporterIDs.DEFAULT, new PathToUrlNodeConfig.DefaultURIExporter());
        }

        @SuppressWarnings("unchecked")
        @Override
        protected void doneWithContext() {
            Map<URIExporterID, URIExporter> uriExportersMap;
            try {
                uriExportersMap = super.get();
                m_uriExporterSelectorPanel.updateComponent(uriExportersMap, m_selectedUriExporterModel,
                    m_selectedUriExporterDescComponent);

                URIExporter uriExporter = m_uriExporterSelectorPanel.getSelectedURIExporter();
                m_selectedUriExporterSettingsPanel = (URIExporterPanel<URIExporter>)uriExporter.getPanel();
                m_selectedUriExporterSettingsPanel.loadSettingsForDialog(m_settings, m_specs);
                //Invokes repaint on the new settings panel
                final Component uriSettingsPanel = createUriExporterSettings(m_uriExporterSettingsPanel);
                uriSettingsPanel.repaint();

            } catch (InterruptedException ex) {
                LOGGER.error(ex.getMessage(), ex);
                Thread.currentThread().interrupt();
            } catch (ExecutionException | NotConfigurableException ex) {
                LOGGER.error(ex.getMessage(), ex);
            }

        }

        /**
         * Returns a list of FSConnections’s based on either the connected FSPortObject or initializing FSConnections
         * based on the column spec
         *
         * @param specs PortObjectSpec array
         * @param pathColumnSpec A DataColumnSpec object for the Path column
         * @return A list of FSConnection’s
         * @throws InvalidSettingsException Throws exception if FSConnection cannot be initialized
         */
        private List<FSConnection> getListOfConnections(final PortObjectSpec[] specs,
            final DataColumnSpec pathColumnSpec) throws InvalidSettingsException {

            //if file system port is connected
            if (m_config.getFileSystemPortIndex() >= 0) {

                try (FSConnection fsConnection = FileSystemPortObjectSpec
                    .getFileSystemConnection(specs, m_config.getFileSystemPortIndex()).orElse(null)) {
                    if (fsConnection != null) {
                        return Arrays.asList(fsConnection);
                    }
                } catch (IOException ex) {
                    //do nothing
                    throw new InvalidSettingsException(ex.getMessage(), ex);
                }
            } else {
                //use metadata from FSLocation columns meta data
                final Optional<FSLocationValueMetaData> optionalFSLocMetaData =
                    pathColumnSpec.getMetaDataOfType(FSLocationValueMetaData.class);
                if (optionalFSLocMetaData.isPresent()) {
                    final FSLocationValueMetaData fsLocMetaData = optionalFSLocMetaData.get();
                    final Set<DefaultFSLocationSpec> setOflocationSpecs = fsLocMetaData.getFSLocationSpecs();

                    if (!setOflocationSpecs.isEmpty()) {
                        return setOflocationSpecs.stream().map(PathToUrlNodeConfig::getFSConnectionWithFakePath)
                            .filter(Optional::isPresent).map(Optional::get).collect(Collectors.toList());
                    } else {
                        throw new InvalidSettingsException("Error initializing a valid file system connection.");
                    }

                }
            }
            return Arrays.asList();
        }

        /**
         * Return a set of the common URIExporterIDs in a list of FSConnection's list
         *
         * @param listOfConnections A list of FSConnection objects
         * @return A set of URIExporterID objects with common URIExporters in the incoming FSConnection's list
         */
        private Set<URIExporterID> getCommonListOfUriExporters(final List<FSConnection> listOfConnections) {
            final Set<URIExporterID> listOfCommonExporters = new HashSet<>();

            //if there is only one FSConnection, all its URIExporters can be supported
            if (listOfConnections.size() == 1) {
                return listOfConnections.get(0).getURIExporterIDs();
            }

            final Set<URIExporterID> tmpUriExporters = listOfConnections.remove(0).getURIExporterIDs();
            tmpUriExporters.forEach(exporterId -> {

                boolean isCommon =
                    listOfConnections.stream().filter(fsConn -> fsConn.getURIExporterIDs().contains(exporterId))
                        .count() == listOfConnections.size();

                if (isCommon) {
                    listOfCommonExporters.add(exporterId);
                }
            });

            return listOfCommonExporters;
        }

    }

}
