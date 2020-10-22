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
 *   Oct 9, 2020 (ayazqureshi): created
 */
package org.knime.filehandling.utility.nodes.binaryobjects.writer;

import java.io.File;
import java.io.IOException;
import java.util.EnumSet;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.blob.BinaryObjectDataValue;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.context.ports.PortsConfiguration;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.util.CheckUtils;
import org.knime.filehandling.core.connections.FSLocationSpec;
import org.knime.filehandling.core.data.location.FSLocationValueMetaData;
import org.knime.filehandling.core.data.location.cell.FSLocationCellFactory;
import org.knime.filehandling.core.defaultnodesettings.filechooser.writer.SettingsModelWriterFileChooser;
import org.knime.filehandling.core.defaultnodesettings.status.NodeModelStatusConsumer;
import org.knime.filehandling.core.defaultnodesettings.status.StatusMessage.MessageType;

/**
 * The node model allowing to convert binary objects to files.
 *
 * @author Ayaz Ali Qureshi, KNIME GmbH, Berlin, Germany
 */
final class BinaryObjectsToFilesNodeModel extends NodeModel {

    final BinaryObjectsToFilesNodeConfig m_binaryObjectsToFileNodeConfig;

    private final SettingsModelString m_binaryColumn;

    private final SettingsModelWriterFileChooser m_fileWriterSelectionModel;

    private int m_inputTableIdx;

    private final NodeModelStatusConsumer m_statusConsumer;

    private static final String OUTPUT_LOCATION_COL_NAME = "output location";

    BinaryObjectsToFilesNodeModel(final PortsConfiguration config, final BinaryObjectsToFilesNodeConfig nodeSettings) {
        super(config.getInputPorts(), config.getOutputPorts());
        m_binaryObjectsToFileNodeConfig = nodeSettings;
        m_binaryColumn = m_binaryObjectsToFileNodeConfig.getBinaryObjectsSelectionColumnModel();
        m_fileWriterSelectionModel = m_binaryObjectsToFileNodeConfig.getFileSettingsModelWriterFileChooser();
        m_inputTableIdx =
            config.getInputPortLocation().get(BinaryObjectsToFilesNodeFactory.DATA_TABLE_INPUT_PORT_GRP_NAME)[0];
        m_statusConsumer = new NodeModelStatusConsumer(EnumSet.of(MessageType.ERROR, MessageType.WARNING));
    }

    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        final DataTableSpec inputTableSpec = (DataTableSpec)inSpecs[m_inputTableIdx];
        if (m_binaryColumn.getStringValue() == null) {
            autoGuess(inSpecs);
        }

        validate(inSpecs);

        //configure the SettingsModelWriterFileChooser
        m_fileWriterSelectionModel.configureInModel(inSpecs, m_statusConsumer);
        m_statusConsumer.setWarningsIfRequired(this::setWarningMessage);

        final BinaryObjectsToFilesCellFactory binaryObjectsToFilesCellFactory =
            new BinaryObjectsToFilesCellFactory(getNewColumnSpec(inputTableSpec));

        return new DataTableSpec[]{
            createColumnRearranger(inputTableSpec, binaryObjectsToFilesCellFactory).createSpec()};
    }

    /**
     * Automatically select the first column in input table which matches the expected type
     *
     * @param inSpecs An array of PortObjectSpec
     * @throws InvalidSettingsException If no column is found with desired data type
     */
    private void autoGuess(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        final DataTableSpec inputTableSpec = (DataTableSpec)inSpecs[m_inputTableIdx];
        m_binaryColumn.setStringValue(inputTableSpec.stream()//
            .filter(dcs -> dcs.getType().isCompatible(BinaryObjectDataValue.class))//
            .map(DataColumnSpec::getName)//
            .findFirst()//
            .orElseThrow(() -> new InvalidSettingsException("No applicable column available"))//
        );
        setWarningMessage(String.format("Auto-guessed column to convert '%s'", m_binaryColumn.getStringValue()));
    }

    /**
     * Validate the selected column in input table
     *
     * @param inSpecs An array of PortObjectSpec
     * @throws InvalidSettingsException If the selected column is not part of the input or doesn't have the correct data
     *             type
     */
    private void validate(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        final DataTableSpec inSpec = (DataTableSpec)inSpecs[m_inputTableIdx];
        final String pathColName = m_binaryColumn.getStringValue();
        final int colIndex = inSpec.findColumnIndex(pathColName);

        // check column existence
        CheckUtils.checkSetting(colIndex >= 0, "The selected column '%s' is not part of the input", pathColName);

        // check column type
        final DataColumnSpec pathColSpec = inSpec.getColumnSpec(colIndex);
        if (!pathColSpec.getType().isCompatible(BinaryObjectDataValue.class)) {
            throw new InvalidSettingsException(
                String.format("The selected column '%s' has the wrong type", pathColName));
        }

        if (inSpec.containsName(OUTPUT_LOCATION_COL_NAME)) {
            setWarningMessage(String.format("The name of the column to create is already taken, using '%s' instead.",
                getUniqueColumnName(inSpec)));
        }

    }

    /**
     * Returns a DataColumnSpec with relevant metadata and column name
     *
     * @param inSpec An array of PortObjectSpec
     * @return A new DataColumnSpec
     */

    private DataColumnSpec getNewColumnSpec(final DataTableSpec inSpec) {

        final FSLocationSpec location = m_fileWriterSelectionModel.getLocation();

        final FSLocationValueMetaData metaData = new FSLocationValueMetaData(location.getFileSystemCategory(),
            location.getFileSystemSpecifier().orElse(null));

        final DataColumnSpecCreator fsLocationSpec = new DataColumnSpecCreator(
            inSpec.containsName(OUTPUT_LOCATION_COL_NAME) ? getUniqueColumnName(inSpec) : OUTPUT_LOCATION_COL_NAME,
            FSLocationCellFactory.TYPE);

        fsLocationSpec.addMetaData(metaData, true);
        return fsLocationSpec.createSpec();
    }

    /**
     * Returns an instance of ColumnRearranger which appends the new column from BinaryObjectsToFilesCellFactory
     *
     * @param inSpec An array of PortObjectSpec
     * @param factory A object of {@link BinaryObjectsToFilesCellFactory}
     * @return An instance of ColumnRearranger
     */
    private static ColumnRearranger createColumnRearranger(final DataTableSpec inSpec,
        final BinaryObjectsToFilesCellFactory factory) {

        // now create output spec using a ColumnRearranger
        final ColumnRearranger colRearranger = new ColumnRearranger(inSpec);
        colRearranger.append(factory);

        return colRearranger;
    }

    /**
     * Return a new unique string based on the input spec, so a new column spec can be created
     *
     * @param inputSpec A DataTableSpec object
     * @return A unique String for creating a new column spec
     */
    private static String getUniqueColumnName(final DataTableSpec inputSpec) {
        return DataTableSpec.getUniqueColumnName(inputSpec, OUTPUT_LOCATION_COL_NAME);
    }

    @Override
    protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        // nothing to do here
    }

    @Override
    protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        // nothing to do here
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_binaryObjectsToFileNodeConfig.saveSettingsTo(settings);
    }

    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_binaryObjectsToFileNodeConfig.validateSettings(settings);
    }

    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_binaryObjectsToFileNodeConfig.loadValidatedSettingsFrom(settings);
    }

    @Override
    protected void reset() {
        // nothing to do here
    }

}
