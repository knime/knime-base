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

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.StringValue;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.container.SingleCellFactory;
import org.knime.core.data.uri.URIDataCell;
import org.knime.core.data.uri.UriCellFactory;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.context.ports.PortsConfiguration;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.util.CheckUtils;
import org.knime.filehandling.core.connections.FSCategory;
import org.knime.filehandling.core.connections.FSConnection;
import org.knime.filehandling.core.connections.FSLocation;
import org.knime.filehandling.core.connections.FSPath;
import org.knime.filehandling.core.connections.location.FSPathProvider;
import org.knime.filehandling.core.connections.location.FSPathProviderFactory;
import org.knime.filehandling.core.connections.uriexport.URIExporter;
import org.knime.filehandling.core.connections.uriexport.URIExporterFactory;
import org.knime.filehandling.core.connections.uriexport.URIExporterID;
import org.knime.filehandling.core.data.location.FSLocationValue;
import org.knime.filehandling.core.data.location.cell.SimpleFSLocationCell;
import org.knime.filehandling.core.port.FileSystemPortObjectSpec;
import org.knime.filehandling.core.util.FSLocationColumnUtils;

/**
 * The node model allowing to convert paths to urls.
 *
 * @author Ayaz Ali Qureshi, KNIME GmbH, Berlin, Germany
 */
public class PathToUrlNodeModel extends NodeModel {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(PathToUrlNodeModel.class);

    private final int m_dataTablePortIndex;

    private final int m_fileSystemPortIndex;

    private final PathToUrlNodeConfig m_config;

    PathToUrlNodeModel(final PortsConfiguration portsConfig, final PathToUrlNodeConfig nodeSettings) {
        super(portsConfig.getInputPorts(), portsConfig.getOutputPorts());
        m_config = nodeSettings;
        m_dataTablePortIndex = nodeSettings.getDataTablePortIndex();
        m_fileSystemPortIndex = nodeSettings.getFileSystemConnectionPortIndex();
    }

    @Override
    protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        // Nothing to do here
    }

    @Override
    protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        // Nothing to do here
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_config.saveSettingsForModel(settings);
    }

    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_config.validateSettingsForModel(settings);
    }

    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_config.loadValidatedSettingsForModel(settings);
    }

    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        final DataTableSpec inputTableSpec = (DataTableSpec)inSpecs[m_dataTablePortIndex];
        if (StringUtils.isEmpty(m_config.getPathColumnName())) {
            autoGuess(inSpecs);
        }

        // validate the settings
        validateSettings(inSpecs);

        final int pathColIndx = inputTableSpec.findColumnIndex(m_config.getPathColumnName());
        final DataColumnSpec pathColumnSpec = inputTableSpec.getColumnSpec(pathColIndx);
        Optional<String> warningMsg = FSLocationColumnUtils.validateFSLocationColumn(pathColumnSpec,
            m_fileSystemPortIndex >= 0 ? (FileSystemPortObjectSpec)inSpecs[m_fileSystemPortIndex] : null);
        warningMsg.ifPresent(this::setWarningMessage);

        if (m_fileSystemPortIndex >= 0
            && !FileSystemPortObjectSpec.getFileSystemConnection(inSpecs, m_fileSystemPortIndex).isPresent()) {
            //Throw exception if a FS connection is connected on port, but the FS Connection is not connected
            throw new InvalidSettingsException("The connection on File System port is not connected.");
        }

        try (final PathToUrlCellFactory urlPathToUrlCellFactory =
            new PathToUrlCellFactory(getNewColumnSpec(inputTableSpec), pathColIndx, null, null, null, null)) {
            return new DataTableSpec[]{createColumnRearranger(inputTableSpec, urlPathToUrlCellFactory).createSpec()};
        } catch (Exception e) {
            //Rethrow the exception
            throw new InvalidSettingsException(e);
        }
    }

    private void validateSettings(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        final DataTableSpec inSpec = (DataTableSpec)inSpecs[m_dataTablePortIndex];
        final String pathColName = m_config.getPathColumnName();
        final int pathColumnIdx = inSpec.findColumnIndex(pathColName);

        // check column existence
        CheckUtils.checkSetting(pathColumnIdx >= 0, "The selected column '%s' is not part of the input", pathColName);

        // check column type
        final DataColumnSpec pathColSpec = inSpec.getColumnSpec(pathColumnIdx);
        if (!pathColSpec.getType().isCompatible(FSLocationValue.class)) {
            throw new InvalidSettingsException(
                String.format("The selected column '%s' has the wrong type", pathColName));
        }

        if (m_config.getURIExporterID() == null) {
            throw new InvalidSettingsException("The selected URL format is invalid");
        }

        if (m_config.shouldAppendColumn()) {
            // Is column name empty?
            if (m_config.getAppendColumnName() == null || m_config.getAppendColumnName().trim().isEmpty()) {
                throw new InvalidSettingsException("The name of the column to create cannot be empty");
            }
            if (inSpec.containsName(m_config.getAppendColumnName())) {
                setWarningMessage(
                    String.format("The name of the column to create is already taken, using '%s' instead.",
                        getUniqueColumnName(inSpec, m_config.getAppendColumnName())));
            }
        }
    }

    @Override
    protected PortObject[] execute(final PortObject[] inObjects, final ExecutionContext exec) throws Exception {

//        final PortObjectSpec[] inSpecs = Arrays.stream(inObjects)//
//            .map(PortObject::getSpec)//
//            .toArray(PortObjectSpec[]::new);
//
//        final DataTableSpec inputTableSpec = (DataTableSpec)inSpecs[m_dataTablePortIndex];
//        final int pathColIndx = inputTableSpec.findColumnIndex(m_config.getSelectedColNameStringVal());
//        final DataColumnSpec pathColumnSpec = inputTableSpec.getColumnSpec(pathColIndx);
//
//        //check if the column spec matches the connected files-system
//        Optional<String> warningMsg = FSLocationColumnUtils.validateFSLocationColumn(pathColumnSpec,
//            m_fileSystemPortIndex >= 0 ? (FileSystemPortObjectSpec)inSpecs[m_fileSystemPortIndex] : null);
//        warningMsg.ifPresent(this::setWarningMessage);
//
//        try (FSConnection fsConnection = m_fileSystemPortIndex >= 0
//            ? FileSystemPortObjectSpec.getFileSystemConnection(inSpecs, m_fileSystemPortIndex).orElse(null) : null) {
//
//            try (final PathToUrlCellFactory factory = new PathToUrlCellFactory(getNewColumnSpec(inputTableSpec),
//                pathColIndx, fsConnection, m_config.getselectedUriExporterStringVal(), m_config.getUriExporterNodeSettingsRO(), exec)) {
//                final BufferedDataTable outputBufferTable =
//                    exec.createColumnRearrangeTable((BufferedDataTable)inObjects[m_dataTablePortIndex],
//                        createColumnRearranger(inputTableSpec, factory), exec);
//                return new PortObject[]{outputBufferTable};
//            }
//        }
        throw new Exception("Not implemented");
    }

    /**
     * Automatically select the first column in input table which matches the expected type
     *
     * @param inSpecs An array of PortObjectSpec
     * @throws InvalidSettingsException If no column is found with desired data type
     */
    private void autoGuess(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        final DataTableSpec inputTableSpec = (DataTableSpec)inSpecs[m_dataTablePortIndex];
        m_config.getPathColumnNameModel().setStringValue(inputTableSpec.stream()//
            .filter(dcs -> dcs.getType().isCompatible(FSLocationValue.class))//
            .map(DataColumnSpec::getName)//
            .findFirst()//
            .orElseThrow(() -> new InvalidSettingsException("No applicable column available"))//
        );
        setWarningMessage(String.format("Auto-guessed column to convert '%s'", m_config.getPathColumnName()));

        if (!m_config.shouldAppendColumn()) {
            m_config.getReplaceColumnNameModel().setStringValue(inputTableSpec.stream()//
                .filter(dcs -> dcs.getType().isCompatible(StringValue.class))//
                .map(DataColumnSpec::getName)//
                .findFirst()//
                .orElseThrow(() -> new InvalidSettingsException("No applicable column available"))//
            );
            setWarningMessage(
                String.format("Auto-guessed column to replace '%s'", m_config.getReplaceColColumnName()));
        }
    }

    /**
     * Returns an instance of ColumnRearranger which appends the new column from PathToUrlCellFactory
     *
     * @param inSpec An array of PortObjectSpec
     * @param factory A object of {@link PathToUrlCellFactory}
     * @return An instance of ColumnRearranger
     */
    private ColumnRearranger createColumnRearranger(final DataTableSpec inSpec, final PathToUrlCellFactory factory) {

        final ColumnRearranger rearranger = new ColumnRearranger(inSpec);
        // Either replace or append a column depending on users choice
        if (!m_config.shouldAppendColumn()) {
            rearranger.replace(factory, m_config.getReplaceColColumnName());
        } else {
            rearranger.append(factory);
        }
        return rearranger;
    }

    /**
     * Returns a DataColumnSpec array with column name of URL column
     *
     * @param inSpec An array of PortObjectSpec
     * @return A new object of DataColumnSpec
     */
    private DataColumnSpec getNewColumnSpec(final DataTableSpec inSpec) {

        final String columnName = !m_config.shouldAppendColumn() ? m_config.getReplaceColColumnName()
            : getUniqueColumnName(inSpec, m_config.getAppendColumnName());
        return new DataColumnSpecCreator(columnName, URIDataCell.TYPE).createSpec();
    }

    /**
     * Return a new unique string based on the input spec, so a new column spec can be created
     *
     * @param inputSpec A DataTableSpec object
     * @param String The suggested name for the column
     * @return A unique String for creating a new column spec
     */
    private static String getUniqueColumnName(final DataTableSpec inputSpec, final String inputColName) {
        return DataTableSpec.getUniqueColumnName(inputSpec, inputColName);
    }

    @Override
    protected void reset() {
        // Nothing to do here
    }

    // A Factory that extends SingleCellFactory to create a column with URIDataCell
    private static class PathToUrlCellFactory extends SingleCellFactory implements Closeable {

        private final int m_colIdx;

        private final FSConnection m_fsConnection;

        private final String m_selectedUriExporterId;

        private final NodeSettingsRO m_settingsRO;

        PathToUrlCellFactory(final DataColumnSpec newColSpec, final int colIdx, final FSConnection fsConnection,
            final String selectedUriExporterId, final NodeSettingsRO nodeSettings, final ExecutionContext exec) {
            super(newColSpec);
            m_colIdx = colIdx;
            if (exec != null) {
                m_fsConnection = fsConnection;
                m_selectedUriExporterId = selectedUriExporterId;
                m_settingsRO = nodeSettings;
            } else {
                m_fsConnection = null;
                m_selectedUriExporterId = "";
                m_settingsRO = null;
            }
        }

        @Override
        public void close() throws IOException {
            // we can catch those exceptions as they cannot occur and even if we don't need to care about them here
            if (m_fsConnection != null) {
                try {
                    m_fsConnection.close();
                } catch (final IOException e) {
                    LOGGER.debug("Unable to close fs location factory", e);
                }
            }

        }

        @Override
        public DataCell getCell(final DataRow row) {
            return createURICell(row);
        }

        /**
         * Convert the path to the selected URI exporter, perform basic checks for missing cell and valid connected file
         * system when the Path column contains a FSLocation of connected FSCategory
         *
         * @param row incoming row to process
         * @return An instance of URIDataCell wrapped by DataCell
         */
        private DataCell createURICell(final DataRow row) {

            //Check if the column is missing and return a missing DataCell object
            if (row.getCell(m_colIdx).isMissing()) {
                return DataType.getMissingCell();
            }

            DataCell uriCell;
            final DataCell tempCell = row.getCell(m_colIdx);

            final SimpleFSLocationCell incomingPathCell = (SimpleFSLocationCell)tempCell;
            final FSLocation fsLocation = incomingPathCell.getFSLocation();

            if (m_fsConnection == null
                && FSCategory.valueOf(fsLocation.getFileSystemCategory()) == FSCategory.CONNECTED) {
                throw new RuntimeException("Path column contains cells which require a valid File System port."); //NOSONAR
            }

            try (FSPathProviderFactory fsPathProviderFactory = FSPathProviderFactory
                .newFactory(m_fsConnection != null ? Optional.of(m_fsConnection) : Optional.empty(), fsLocation)) {
                uriCell = convertPathCellToURI(fsPathProviderFactory, fsLocation);
            } catch (final Exception ex) {
                LOGGER.error(ex);
                throw new RuntimeException(ex.getMessage(), ex.getCause()); //NOSONAR
            }

            return uriCell;
        }

        /**
         * Validate & load URI Exporter and convert the path to the selected URI exporter
         *
         * @param fsPathProviderFactory An instance of FSPathProviderFactory
         * @param fsLocation An instance of FSLocation
         *
         * @return An instance of URIDataCell wrapped by DataCell
         *
         * @throws IOException Throws an IOException when the selected URIExporter is not supported by the File system
         * @throws URISyntaxException Throws an URISyntaxException when URI exporter is unable to convert the path
         * @throws InvalidSettingsException Exception thrown if unable to validate URIExporter settings
         */
        private DataCell convertPathCellToURI(final FSPathProviderFactory fsPathProviderFactory,
            final FSLocation fsLocation)
            throws IOException, URISyntaxException, InvalidSettingsException {
            DataCell uriCell;
            try (FSPathProvider pathProvider = fsPathProviderFactory.create(fsLocation)) {
                FSPath path = pathProvider.getPath();

                final URIExporterFactory exporterFactory =
                    pathProvider.getFSConnection().getURIExporterFactory(new URIExporterID(m_selectedUriExporterId));

                final URIExporter uriExporter = exporterFactory.createExporter(null); // FIXME
                URI newCellURI = uriExporter.toUri(path);
                uriCell = UriCellFactory.create(newCellURI.toString());
            }

            return uriCell;
        }

    }
}
