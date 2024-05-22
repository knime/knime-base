/*
 * ------------------------------------------------------------------------
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
 * -------------------------------------------------------------------
 *
 * History 05.11.2006 (Tobias Koetter): created
 */
package org.knime.base.node.preproc.rowkey2;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import org.knime.base.data.append.column.AppendedColumnTable;
import org.knime.base.node.preproc.rowkey2.RowKeyNodeSettings.HandleDuplicateValuesMode;
import org.knime.base.node.preproc.rowkey2.RowKeyNodeSettings.HandleMissingValuesMode;
import org.knime.base.node.preproc.rowkey2.RowKeyNodeSettings.ReplacementMode;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.property.hilite.DefaultHiLiteMapper;
import org.knime.core.node.property.hilite.HiLiteHandler;
import org.knime.core.node.property.hilite.HiLiteTranslator;
import org.knime.core.node.streamable.BufferedDataTableRowOutput;
import org.knime.core.node.streamable.DataTableRowInput;
import org.knime.core.node.streamable.InputPortRole;
import org.knime.core.node.streamable.OutputPortRole;
import org.knime.core.node.streamable.PartitionInfo;
import org.knime.core.node.streamable.PortInput;
import org.knime.core.node.streamable.PortOutput;
import org.knime.core.node.streamable.RowInput;
import org.knime.core.node.streamable.RowOutput;
import org.knime.core.node.streamable.StreamableFunction;
import org.knime.core.node.streamable.StreamableOperator;
import org.knime.core.webui.node.impl.WebUINodeConfiguration;
import org.knime.core.webui.node.impl.WebUINodeModel;

/**
 * The node model of the row key manipulation node. The node allows the user
 * to replace the row key with another column and/or to append a new column
 * with the values of the current row key.
 *
 * @author Tobias Koetter, University of Konstanz
 * @since 2.6
 */
@SuppressWarnings("restriction") // WebUI
public class RowKeyNodeModel2 extends WebUINodeModel<RowKeyNodeSettings> {

    // our logger instance
    private static final NodeLogger LOGGER = NodeLogger.getLogger(RowKeyNodeModel2.class);

    /** The port were the model expects the in data. */
    public static final int DATA_IN_PORT = 0;

    /** The port which the model uses to return the data. */
    public static final int DATA_OUT_PORT = 0;

    private static final String INTERNALS_FILE_NAME = "hilite_mapping.xml.gz";

    /**
     * Node returns a new hilite handler instance.
     */
    private final HiLiteTranslator m_hilite = new HiLiteTranslator();

    // HACK: See configure. Already initialize here to fall back to default settings until configure has been called
    // (this is e.g. required by getOutHiLiteHandler, which is called right after node construction).
    private RowKeyNodeSettings m_modelSettings = new RowKeyNodeSettings();

    /**
     * Constructor for class RowKeyNodeModel.
     *
     * @param config the configuration for the node
     *
     * @since 5.3
     */
    protected RowKeyNodeModel2(final WebUINodeConfiguration config) {
        super(config, RowKeyNodeSettings.class);
    }

    /**
     * {@inheritDoc}
     * @since 5.3
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec,
        final RowKeyNodeSettings modelSettings) throws Exception {
        LOGGER.debug("Entering execute(inData, exec) of class RowKeyNodeModel");
        // check input data
        if (inData == null || inData.length != 1 || inData[DATA_IN_PORT] == null) {
            throw new IllegalArgumentException("No input data available.");
        }
        final BufferedDataTable data = inData[DATA_IN_PORT];
        BufferedDataTable outData = null;

        if (modelSettings.m_replaceRowKey) {
            // create outspec
            DataTableSpec outSpec = configure(data.getDataTableSpec(), modelSettings);

            // create table
            final BufferedDataContainer newContainer = exec.createDataContainer(outSpec, true);
            RowInput rowInput = new DataTableRowInput(data);
            RowOutput rowOutput = new BufferedDataTableRowOutput(newContainer);
            replaceKey(rowInput, rowOutput, outSpec.getNumColumns(), data.getRowCount(), exec, modelSettings);
            newContainer.close();
            outData =  newContainer.getTable();
        } else if (modelSettings.m_appendRowKey) {
            LOGGER.debug("The user only wants to append a new column with name " + modelSettings.m_appendedColumnName);
            // the user wants only a column with the given name which contains the rowkey as value
            final DataTableSpec tableSpec = data.getDataTableSpec();
            final ColumnRearranger c = RowKeyUtil2.createColumnRearranger(tableSpec, modelSettings.m_appendedColumnName,
                StringCell.TYPE);
            outData = exec.createColumnRearrangeTable(data, c, exec);
            exec.setMessage("New column created");
            LOGGER.debug("Column appended successfully");
        } else {
            // the user doesn't want to do anything at all so we simply return the given data
            outData = data;
            LOGGER.debug("The user hasn't selected a new RowID column and hasn't entered a new column name.");
        }
        LOGGER.debug("Exiting execute(inData, exec) of class RowKeyNodeModel.");
        return new BufferedDataTable[]{outData};
    }


    @Override
    public StreamableOperator createStreamableOperator(final PartitionInfo partitionInfo,
        final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        LOGGER.debug("Entering createStreamableOperator-method of class RowKeyNodeModel");

        final var tableSpec = (DataTableSpec) inSpecs[DATA_IN_PORT];
        if (m_modelSettings.m_replaceRowKey) {
            DataTableSpec outSpec = configure(tableSpec, m_modelSettings);
            return new StreamableOperator() {
                @Override
                public void runFinal(final PortInput[] inputs, final PortOutput[] outputs, final ExecutionContext exec)
                        throws Exception {
                    RowInput rowInput = (RowInput) inputs[DATA_IN_PORT];
                    RowOutput rowOutput = (RowOutput) outputs[DATA_OUT_PORT];
                    replaceKey(rowInput, rowOutput, outSpec.getNumColumns(), -1, exec, m_modelSettings);
                }
            };
        } else if (m_modelSettings.m_appendRowKey) {
            LOGGER.debug("The user only wants to append a new column with name " //
                + m_modelSettings.m_appendedColumnName);
            // the user wants only a column with the given name which contains the rowkey as value
            final ColumnRearranger c = RowKeyUtil2.createColumnRearranger(tableSpec,
                m_modelSettings.m_appendedColumnName, StringCell.TYPE);
            return c.createStreamableFunction();
        } else {
            //the user doesn't want to do anything at all so we simply pass the given data
            return new StreamableFunction() {
                @Override
                public DataRow compute(final DataRow input) throws Exception {
                    return input;
                }
            };
        }
    }

    @Override
    public InputPortRole[] getInputPortRoles() {
        // only streamable for now -> to be distributed more effort needed to ensure the uniqueness of row keys
        return new InputPortRole[]{InputPortRole.NONDISTRIBUTED_STREAMABLE};
    }

    @Override
    public OutputPortRole[] getOutputPortRoles() {
        return new OutputPortRole[]{OutputPortRole.NONDISTRIBUTED};
    }

    /* called if the row key is to be replaced */
    private void replaceKey(final RowInput rowInput, final RowOutput rowOutput, final int totalNoOfOutCols,
        final int totalNoOfRows, final ExecutionContext exec, final RowKeyNodeSettings modelSettings) throws Exception {

        LOGGER.debug("The user wants to replace the RowID with the column " + modelSettings.m_appendedColumnName
                + " optional appended column name" + modelSettings.m_appendRowKey);
        if (modelSettings.m_replaceRowKeyMode == ReplacementMode.USE_COLUMN) {
            // the user wants a new column as rowkey column
            final int colIdx = rowInput.getDataTableSpec().findColumnIndex(modelSettings.m_newRowKeyColumnV2);
            if (colIdx < 0) {
                if (modelSettings.m_newRowKeyColumnV2 == null) {
                    throw new InvalidSettingsException("No column selected for replacing the RowID.");
                }
                throw new InvalidSettingsException("No column with name: " + modelSettings.m_newRowKeyColumnV2
                        + " exists. Please select a valid column name.");
            }
        }
        DataColumnSpec newColSpec = null;
        if (modelSettings.m_appendRowKey) {
            newColSpec = createAppendRowKeyColSpec(modelSettings.m_appendedColumnName);
        }

        final var replaceByColumn = modelSettings.m_replaceRowKeyMode == ReplacementMode.USE_COLUMN;
        final boolean ensureUniqueness = replaceByColumn // Setting only takes effect if we replace by column.
            && modelSettings.m_handleDuplicatesMode == HandleDuplicateValuesMode.APPEND_COUNTER;
        final boolean replaceMissing = replaceByColumn // Setting only takes effect if we replace by column.
            && modelSettings.m_handleMissingsMode == HandleMissingValuesMode.REPLACE;
        final boolean removeRowKeyCol = replaceByColumn // Setting only takes effect if we replace by column.
            && modelSettings.m_removeRowKeyColumn;

        final RowKeyUtil2 util = new RowKeyUtil2();
        final var newRowKeyColumn = modelSettings.m_replaceRowKeyMode == ReplacementMode.USE_COLUMN //
            ? modelSettings.m_newRowKeyColumnV2 //
            : null;
        util.changeRowKey(rowInput, rowOutput, exec, newRowKeyColumn,
            modelSettings.m_appendRowKey, newColSpec, ensureUniqueness, replaceMissing, removeRowKeyCol,
            modelSettings.m_enableHilite, totalNoOfOutCols, totalNoOfRows);
        if (modelSettings.m_enableHilite) {
            m_hilite.setMapper(new DefaultHiLiteMapper(util.getHiliteMapping()));
        }
        final int missingValueCounter = util.getMissingValueCounter();
        final int duplicatesCounter = util.getDuplicatesCounter();
        final StringBuilder warningMsg = new StringBuilder();
        if (missingValueCounter > 0) {
            warningMsg.append(missingValueCounter + " missing value(s) replaced with "
                    + RowKeyUtil2.MISSING_VALUE_REPLACEMENT + ". ");
        }
        if (duplicatesCounter > 0) {
            warningMsg.append(duplicatesCounter + " duplicate(s) now unique.");
        }
        if (warningMsg.length() > 0) {
            setWarningMessage(warningMsg.toString());
        }
        LOGGER.debug("Row ID replaced successfully");
    }

    /**
     * @param origSpec the original table specification (could be <code>null</code>)
     * @param appendRowKey <code>true</code> if a new column should be created
     * @param newColName the name of the new column to append
     * @param replaceRowKeyMode if and how the row key should be replaced
     * @param newRowKeyCol the name of the row key column
     * @param removeRowKeyCol removes the selected row key column if set to <code>true</code>
     * @param replaceRowKey
     * @throws InvalidSettingsException if the settings are invalid
     * @since 5.3
     */
    protected static void validateInput(final DataTableSpec origSpec, final boolean appendRowKey,
            final String newColName, final ReplacementMode replaceRowKeyMode, final String newRowKeyCol,
            final boolean removeRowKeyCol, final boolean replaceRowKey) throws InvalidSettingsException {
        if (replaceRowKeyMode == ReplacementMode.USE_COLUMN) {
            if (newRowKeyCol == null) {
                throw new InvalidSettingsException("No column selected for replacing the RowID.");
            }
            if (origSpec != null && !origSpec.containsName(newRowKeyCol)) {
                throw new InvalidSettingsException("Selected column: '" + newRowKeyCol + "' not found in input table.");
            }
        }
        if (appendRowKey) {
            if (newColName == null || newColName.trim().length() < 1) {
                throw new InvalidSettingsException("Please provide a valid" + " name for the new column.");
            }
            if (origSpec != null && origSpec.containsName(newColName)
                && (!replaceRowKey || !removeRowKeyCol || !newRowKeyCol.equals(newColName))) {
                throw new InvalidSettingsException("Column with name: '" + newColName + "' already exists.");
            }
        }
    }

    /**
     * @param newColName the name of the column to add
     * @return the column specification
     * @throws InvalidSettingsException if the name is invalid or exists in the
     * original table specification
     */
    private static DataColumnSpec createAppendRowKeyColSpec(final String newColName) {
        final DataColumnSpecCreator colSpecCreater = new DataColumnSpecCreator(newColName, StringCell.TYPE);
        return colSpecCreater.createSpec();
    }

    @Override
    protected void reset() {
        m_hilite.setMapper(null);
    }

    @Override
    protected void setInHiLiteHandler(final int inIndex, final HiLiteHandler hiLiteHdl) {
        m_hilite.removeAllToHiliteHandlers();
        m_hilite.addToHiLiteHandler(hiLiteHdl);
    }

    @Override
    protected HiLiteHandler getOutHiLiteHandler(final int outIndex) {
        if (m_modelSettings.m_appendRowKey && !m_modelSettings.m_replaceRowKey) {
            // use the original hilite handler if the row keys do not change
            return getInHiLiteHandler(outIndex);
        } else {
            return m_hilite.getFromHiLiteHandler();
        }
    }

    /**
     * {@inheritDoc}
     * @since 5.3
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs, final RowKeyNodeSettings modelSettings)
        throws InvalidSettingsException {

        // HACK: Cache settings locally to make createStreamableOperator, getOutHiLiteHandler, and other methods that
        // don't get settings passed in work.
        m_modelSettings = modelSettings;

        // check the input data
        assert (inSpecs != null && inSpecs.length == 1 && inSpecs[DATA_IN_PORT] != null);
        DataTableSpec spec = inSpecs[DATA_IN_PORT];
        return new DataTableSpec[]{configure(spec, modelSettings)};
    }

    /**
     * Determines the outspec. Called from within the configure and execute-method.
     *
     * @param spec the original table spec
     * @return the out spec
     * @throws InvalidSettingsException
     */
    private static DataTableSpec configure(final DataTableSpec spec, final RowKeyNodeSettings modelSettings)
        throws InvalidSettingsException {

        final var newRowKeyColumn = modelSettings.m_replaceRowKeyMode == ReplacementMode.USE_COLUMN //
            ? modelSettings.m_newRowKeyColumnV2 //
            : null;

        validateInput(spec, modelSettings.m_appendRowKey, modelSettings.m_appendedColumnName,
            modelSettings.m_replaceRowKeyMode, newRowKeyColumn, modelSettings.m_removeRowKeyColumn,
            modelSettings.m_appendRowKey);
        DataTableSpec resSpec = spec;
        if (modelSettings.m_replaceRowKeyMode == ReplacementMode.USE_COLUMN && modelSettings.m_removeRowKeyColumn) {
            resSpec = RowKeyUtil2.createTableSpec(resSpec, newRowKeyColumn);
        }
        if (modelSettings.m_appendRowKey) {
            final DataColumnSpec colSpec = createAppendRowKeyColSpec(modelSettings.m_appendedColumnName);
            resSpec = AppendedColumnTable.getTableSpec(resSpec, colSpec);
        }
        return resSpec;
    }

    /**
     * {@inheritDoc}
     * @since 5.3
     */
    @Override
    protected void validateSettings(final RowKeyNodeSettings settings) throws InvalidSettingsException {
        validateInput(null, settings.m_appendRowKey, settings.m_appendedColumnName, settings.m_replaceRowKeyMode,
            settings.m_newRowKeyColumnV2, settings.m_removeRowKeyColumn, settings.m_appendRowKey);
    }

    @Override
    protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec) throws IOException {
        if (m_modelSettings.m_enableHilite) {
            try (final var inputStream = new FileInputStream(new File(nodeInternDir, INTERNALS_FILE_NAME))) {
                final NodeSettingsRO config = NodeSettings.loadFromXML(inputStream);
                m_hilite.setMapper(DefaultHiLiteMapper.load(config));
                m_hilite.addToHiLiteHandler(getInHiLiteHandler(0));
            } catch (final InvalidSettingsException ex) {
                throw new IOException(ex.getMessage(), ex);
            }
        }
    }

    @Override
    protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec) throws IOException {
        if (m_modelSettings.m_enableHilite) {
            final NodeSettings config = new NodeSettings("hilite_mapping");
            final DefaultHiLiteMapper mapper = (DefaultHiLiteMapper) m_hilite.getMapper();
            if (mapper != null) {
                mapper.save(config);
            }
            try (final var outputStream = new FileOutputStream(new File(nodeInternDir, INTERNALS_FILE_NAME))) {
                config.saveToXML(outputStream);
            }
        }
    }
}
