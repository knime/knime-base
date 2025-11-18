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
 */

package org.knime.base.node.preproc.columnaggregator;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ArrayUtils;
import org.knime.base.data.aggregation.GlobalSettings;
import org.knime.base.data.aggregation.GlobalSettings.AggregationContext;
import org.knime.base.data.aggregation.NamedAggregationOperator;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.container.CellFactory;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.filestore.FileStoreFactory;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelColumnFilter2;
import org.knime.core.node.defaultnodesettings.SettingsModelInteger;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.streamable.InputPortRole;
import org.knime.core.node.streamable.OutputPortRole;
import org.knime.core.node.streamable.PartitionInfo;
import org.knime.core.node.streamable.PortInput;
import org.knime.core.node.streamable.PortOutput;
import org.knime.core.node.streamable.StreamableOperator;
import org.knime.core.node.util.CheckUtils;


/**
 * {@link NodeModel} implementation of the column aggregator node.
 *
 * @author Tobias Koetter, University of Konstanz
 */
public class ColumnAggregatorNodeModel extends NodeModel {

    /**Configuration key for the aggregation method settings.*/
    protected static final String CFG_AGGREGATION_METHODS = "aggregationMethods";

    private static final String ERROR_NO_AGG_METHOD_SPECIFIED = "No aggregation method specified. At least one "
        + "aggregation method is required to perform an aggregation.";
    private static final String ERROR_NO_AGG_COLS_SPECIFIED = "No aggregated column(s) specified. At least one column "
        + "is needed to perform an aggregation.";

    /**
     * Feature flag key introduced with 5.10 since with the introduction of {@link ColumnAggregatorNodeParameters} it is
     * possible to apply aggregation methods that are missing if the user has added additional columns to aggregate.
     */
    static final String CFG_VALIDATE_AGGREGATION_METHODS = "validateAggregationMethods";

    private boolean m_validateAggregationMethods;

    private final SettingsModelColumnFilter2 m_aggregationCols = createAggregationColsModel();

    private final SettingsModelBoolean m_removeRetainedCols = createRemoveRetainedColsModel();

    private final SettingsModelBoolean m_removeAggregationCols =createRemoveAggregationColsModel();

    private final SettingsModelIntegerBounded m_maxUniqueValues = createMaxUniqueValsModel();

    private final SettingsModelString m_valueDelimiter = createValueDelimiterModel();

    private final List<NamedAggregationOperator> m_methods = new ArrayList<>();
    //used to now the implementation version of the node
    private final SettingsModelInteger m_version = createVersionModel();

    /**
     * @return version model
     */
    static SettingsModelInteger createVersionModel() {
        return new SettingsModelInteger("nodeVersion", 1);
    }

    /**
     * @return the maximum unique values model
     */
    static SettingsModelIntegerBounded createMaxUniqueValsModel() {
        return new SettingsModelIntegerBounded("maxNoneNumericalVals", 10000, 1,
                Integer.MAX_VALUE);
    }

    /**
     * @return the value delimiter model
     */
    static SettingsModelString createValueDelimiterModel() {
        return new SettingsModelString("valueDelimiter",
                GlobalSettings.STANDARD_DELIMITER);
    }

    /**
     * @return the remove aggregation column model
     */
    static SettingsModelBoolean createRemoveAggregationColsModel() {
        return new SettingsModelBoolean("removeAggregationColumns", false);
    }

    /**
     * @return the remove aggregation column model
     */
    static SettingsModelBoolean createRemoveRetainedColsModel() {
        return new SettingsModelBoolean("removeRetainedColumns", false);
    }

    /**
     * @return the aggregation column model
     */
    static SettingsModelColumnFilter2 createAggregationColsModel() {
        return new SettingsModelColumnFilter2("aggregationColumns");
    }

    /**Constructor for class ColumnAggregatorNodeModel.
     */
    protected ColumnAggregatorNodeModel() {
        super(1, 1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        CheckUtils.checkSetting(!m_methods.isEmpty(), ERROR_NO_AGG_METHOD_SPECIFIED);
        //check if at least one of the columns exists in the input table
        final var inSpec = inSpecs[0];
        final var filterResult = m_aggregationCols.applyTo(inSpec);
        final List<String> selectedCols = Arrays.asList(filterResult.getIncludes());
        if (selectedCols == null || selectedCols.isEmpty()) {
            throw new InvalidSettingsException(ERROR_NO_AGG_COLS_SPECIFIED);
        }
        validateAggregationMethods(inSpec, selectedCols);
        //configure also all aggregation operators to check if they can be
        //applied to the given input table
        NamedAggregationOperator.configure(inSpec, m_methods);
        final var cellFactory = new AggregationCellFactory(inSpec, selectedCols, GlobalSettings.DEFAULT, m_methods,
            Arrays.asList(getColsToRemove(inSpec)));
        return new DataTableSpec[]{ createRearranger(inSpec, cellFactory).createSpec() };
    }

    private void validateAggregationMethods(final DataTableSpec inSpec, final List<String> selectedCols)
        throws InvalidSettingsException {
        if (m_validateAggregationMethods) {
            final var selectedColsSet = selectedCols.stream().collect(Collectors.toSet());
            final var dataTypes = inSpec.stream().filter(col -> selectedColsSet.contains(col.getName()))
                .map(DataColumnSpec::getType).collect(Collectors.toSet());
            for (final var method : m_methods) {
                final var incompatibleDataTypes =
                    dataTypes.stream().filter(Predicate.not(method::isCompatible)).toList();
                if (!incompatibleDataTypes.isEmpty()) {
                    final var dataTypeNames =
                        incompatibleDataTypes.stream().map(DataType::getName).collect(Collectors.joining(", "));
                    throw new InvalidSettingsException(String.format(
                        "The aggregation method '%s' is not compatible with the data types of chosen aggregation columns: %s",
                        method.getName(), dataTypeNames));
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
        final BufferedDataTable table = inData[0];
        final DataTableSpec origSpec = table.getSpec();
        final var filterResult = m_aggregationCols.applyTo(origSpec);
        final List<String> selectedCols =
            Arrays.asList(filterResult.getIncludes());
        final var globalSettings = GlobalSettings.builder()
                .setFileStoreFactory(
                        FileStoreFactory.createWorkflowFileStoreFactory(exec))
                .setGroupColNames(selectedCols)
                .setMaxUniqueValues(m_maxUniqueValues.getIntValue())
                .setValueDelimiter(getDefaultValueDelimiter())
                .setDataTableSpec(origSpec)
                .setNoOfRows(table.size())
                .setAggregationContext(AggregationContext.COLUMN_AGGREGATION).build();
        final var cellFactory = new AggregationCellFactory(
                origSpec, selectedCols, globalSettings, m_methods, Arrays.asList(getColsToRemove(origSpec)));
        final ColumnRearranger cr =
            createRearranger(origSpec, cellFactory);
        final BufferedDataTable out =
            exec.createColumnRearrangeTable(table, cr, exec);
        return new BufferedDataTable[]{out};
    }

    private ColumnRearranger createRearranger(final DataTableSpec oSpec, final CellFactory cellFactory) {
        final var cr = new ColumnRearranger(oSpec);
        cr.append(cellFactory);
        final String[] colsToRemove = getColsToRemove(oSpec);
        if (colsToRemove.length != 0) {
            cr.remove(colsToRemove);
        }
        return cr;
    }

    /** Determines which columns are not retained in the output according to the user configuration. */
    private String[] getColsToRemove(final DataTableSpec inSpec) {
        final var filterResult = m_aggregationCols.applyTo(inSpec);
        if (m_removeAggregationCols.getBooleanValue() && m_removeRetainedCols.getBooleanValue()) {
            return ArrayUtils.addAll(filterResult.getIncludes(), filterResult.getExcludes());
        } else if (m_removeAggregationCols.getBooleanValue()) {
            return filterResult.getIncludes();
        } else if (m_removeRetainedCols.getBooleanValue()) {
            return filterResult.getExcludes();
        }
        return ArrayUtils.EMPTY_STRING_ARRAY;
    }

    private String getDefaultValueDelimiter() {
        if (m_version.getIntValue() > 0) {
            return m_valueDelimiter.getJavaUnescapedStringValue();
        }
        //this is the old implementation that uses the escaped value delimiter
        return m_valueDelimiter.getStringValue();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_aggregationCols.saveSettingsTo(settings);
        m_removeRetainedCols.saveSettingsTo(settings);
        m_removeAggregationCols.saveSettingsTo(settings);
        m_valueDelimiter.saveSettingsTo(settings);
        m_maxUniqueValues.saveSettingsTo(settings);
        final NodeSettingsWO subSettings =
            settings.addNodeSettings(CFG_AGGREGATION_METHODS);
        NamedAggregationOperator.saveMethods(subSettings, m_methods);
        m_version.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_aggregationCols.validateSettings(settings);
        m_removeRetainedCols.validateSettings(settings);
        m_removeAggregationCols.validateSettings(settings);
        m_valueDelimiter.validateSettings(settings);
        m_maxUniqueValues.validateSettings(settings);
        CheckUtils.checkSetting(settings.containsKey(CFG_AGGREGATION_METHODS),
            "Settings for aggregation methods could not be found.");
        //check for duplicate column names
        final NodeSettingsRO subSettings = settings.getNodeSettings(ColumnAggregatorNodeModel.CFG_AGGREGATION_METHODS);
        final List<NamedAggregationOperator> methods = NamedAggregationOperator.loadOperators(subSettings);
        CheckUtils.checkSetting(!methods.isEmpty(), ERROR_NO_AGG_METHOD_SPECIFIED);
        final Map<String, Integer> colNames = new HashMap<>(methods.size());
        var colIdx = 1;
        for (final NamedAggregationOperator method : methods) {
            final Integer oldIdx = colNames.put(method.getName(), Integer.valueOf(colIdx));
            CheckUtils.checkSetting(oldIdx == null, "Duplicate column name \"%s\" found at position %d and %d.",
                    method.getName(), oldIdx, colIdx);
            colIdx++;
        }
        //validate the sub settings of all operators that require additional settings
        NamedAggregationOperator.validateSettings(subSettings, methods);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_aggregationCols.loadSettingsFrom(settings);
        m_removeRetainedCols.loadSettingsFrom(settings);
        m_removeAggregationCols.loadSettingsFrom(settings);
        m_valueDelimiter.loadSettingsFrom(settings);
        m_maxUniqueValues.loadSettingsFrom(settings);
        m_methods.clear();
        final NodeSettingsRO subSettings = settings.getNodeSettings(
                ColumnAggregatorNodeModel.CFG_AGGREGATION_METHODS);
        m_methods.addAll(NamedAggregationOperator.loadOperators(subSettings));
        try {
            m_version.loadSettingsFrom(settings);
        } catch (InvalidSettingsException e) { // NOSONAR backwards compatibility
            //this flag was introduced in 2.10 to mark the implementation version
            m_version.setIntValue(0);
        }
        m_validateAggregationMethods = settings.getBoolean(CFG_VALIDATE_AGGREGATION_METHODS, false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        // nothing to do
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir,
            final ExecutionMonitor exec)
            throws IOException, CanceledExecutionException {
        // nothing to do
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir,
            final ExecutionMonitor exec)
            throws IOException, CanceledExecutionException {
        // nothing to do

    }

    /* ================= STREAMING ================= */

    /**
     * {@inheritDoc}
     */
    @Override
    public StreamableOperator createStreamableOperator(final PartitionInfo partitionInfo,
        final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        return new StreamableOperator() {

            @Override
            public void runFinal(final PortInput[] inputs, final PortOutput[] outputs, final ExecutionContext exec)
                throws Exception {
                final DataTableSpec origSpec = (DataTableSpec)inSpecs[0];
                final var filterResult = m_aggregationCols.applyTo(origSpec);
                final List<String> selectedCols = Arrays.asList(filterResult.getIncludes());
                final var globalSettings = GlobalSettings.builder()
                        .setFileStoreFactory(FileStoreFactory.createWorkflowFileStoreFactory(exec))
                        .setGroupColNames(selectedCols)
                        .setMaxUniqueValues(m_maxUniqueValues.getIntValue())
                        .setValueDelimiter(getDefaultValueDelimiter())
                        .setDataTableSpec(origSpec)
                        .setNoOfRows(-1)
                        .setAggregationContext(AggregationContext.COLUMN_AGGREGATION).build();
                final var cellFactory = new AggregationCellFactory(origSpec,
                    selectedCols, globalSettings, m_methods, Arrays.asList(getColsToRemove(origSpec)));
                createRearranger(origSpec, cellFactory).createStreamableFunction().runFinal(inputs, outputs, exec);
            }
        };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public InputPortRole[] getInputPortRoles() {
        return new InputPortRole[]{InputPortRole.DISTRIBUTED_STREAMABLE};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public OutputPortRole[] getOutputPortRoles() {
        return new OutputPortRole[]{OutputPortRole.DISTRIBUTED};
    }

}
