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
 *   Aug 28, 2019 (Simon Schmid, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.base.node.preproc.probability.nominal.creator;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.knime.base.node.preproc.probability.nominal.ExceptionHandling;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.MissingCell;
import org.knime.core.data.NominalValue;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.container.SingleCellFactory;
import org.knime.core.data.filestore.FileStoreFactory;
import org.knime.core.data.probability.nominal.NominalDistributionCell;
import org.knime.core.data.probability.nominal.NominalDistributionCellFactory;
import org.knime.core.data.probability.nominal.NominalDistributionValueMetaData;
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
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.streamable.InputPortRole;
import org.knime.core.node.streamable.OutputPortRole;
import org.knime.core.node.streamable.PartitionInfo;
import org.knime.core.node.streamable.PortInput;
import org.knime.core.node.streamable.PortOutput;
import org.knime.core.node.streamable.StreamableFunction;
import org.knime.core.node.streamable.StreamableOperator;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.util.UniqueNameGenerator;

/**
 * Node model of the node that creates probability distributions of probability values.
 *
 * @author Simon Schmid, KNIME GmbH, Konstanz, Germany
 */
final class NominalDistributionCreatorNodeModel extends NodeModel {

    // Configuration keys
    static final String CFG_COLUMN_NAME = "column_name";
    static final String CFG_REMOVE_INCLUDED_COLUMNS = "remove_included_columns";
    static final String CFG_COLUMN_FILTER = "column_filter";
    static final String CFG_ENABLE_PRECISION = "enable_precision";
    static final String CFG_PRECISION = "precision";
    static final String CFG_MISSING_VALUE_HANDLING = "missing_value_handling";
    static final String CFG_INVALID_DISTRIBUTION_HANDLING = "invalid_distribution_handling";
    static final String CFG_SINGLE_STRING_COLUMN = "single_string_column";
    static final String CFG_COLUMN_TYPE = "column_type";

    /**
     */
    protected NominalDistributionCreatorNodeModel() {
        super(1, 1);
    }

    private final SettingsModelString m_columnNameModel = createColumnNameModel();

    private final SettingsModelBoolean m_removeIncludedColumns = createRemoveIncludedColsBooleanModel();

    private final SettingsModelColumnFilter2 m_columnFilterModel = createColumnFilterModel();

    private final SettingsModelBoolean m_allowUnpreciseProbabilities = createPrecisionBooleanModel();

    private final SettingsModelIntegerBounded m_precisionModel = createPrecisionModel(m_allowUnpreciseProbabilities);

    private final SettingsModelString m_missingValueHandling = createMissingValueHandlingModel();

    private final SettingsModelString m_invalidDistributionHandling = createInvalidDistributionHandlingModel();

    private final SettingsModelString m_stringFilterModel = createStringFilterModel();

    private final SettingsModelString m_columnTypeModel = createColumnTypeModel();

    static SettingsModelString createColumnNameModel() {
        return new SettingsModelString(CFG_COLUMN_NAME, "Probability Distribution");
    }

    static SettingsModelBoolean createRemoveIncludedColsBooleanModel() {
        return new SettingsModelBoolean(CFG_REMOVE_INCLUDED_COLUMNS, false);
    }

    @SuppressWarnings("unchecked")
    static SettingsModelColumnFilter2 createColumnFilterModel() {
        return new SettingsModelColumnFilter2(CFG_COLUMN_FILTER, DoubleValue.class);
    }

    static SettingsModelBoolean createPrecisionBooleanModel() {
        return new SettingsModelBoolean(CFG_ENABLE_PRECISION, true);
    }

    static SettingsModelIntegerBounded createPrecisionModel(final SettingsModelBoolean precisionBoolModel) {
        final SettingsModelIntegerBounded model = new SettingsModelIntegerBounded(
            CFG_PRECISION, 4, 1, Integer.MAX_VALUE);
        model.setEnabled(precisionBoolModel.getBooleanValue());
        precisionBoolModel.addChangeListener(l -> model.setEnabled(precisionBoolModel.getBooleanValue()));
        return model;
    }

    static SettingsModelString createMissingValueHandlingModel() {
        return new SettingsModelString(CFG_MISSING_VALUE_HANDLING, MissingValueHandling.FAIL.name());
    }

    static SettingsModelString createInvalidDistributionHandlingModel() {
        return new SettingsModelString(CFG_INVALID_DISTRIBUTION_HANDLING, ExceptionHandling.FAIL.name());
    }

    static SettingsModelString createStringFilterModel() {
        return new SettingsModelString(CFG_SINGLE_STRING_COLUMN, "");
    }

    static SettingsModelString createColumnTypeModel() {
        return new SettingsModelString(CFG_COLUMN_TYPE, ColumnType.getDefault().name());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {
        return new DataTableSpec[]{createColumnRearranger(inSpecs[0], null).createSpec()};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec)
        throws Exception {
        final BufferedDataTable data = inData[0];
        final ColumnRearranger cr = createColumnRearranger(data.getDataTableSpec(), exec);
        return new BufferedDataTable[]{exec.createColumnRearrangeTable(data, cr, exec)};
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
                final ColumnRearranger columnRearranger = createColumnRearranger((DataTableSpec)inSpecs[0], exec);
                final StreamableFunction func = columnRearranger.createStreamableFunction(0, 0);
                func.runFinal(inputs, outputs, exec);
            }
        };
    }

    private ColumnRearranger createColumnRearranger(final DataTableSpec spec, final ExecutionContext exec)
        throws InvalidSettingsException {
        ColumnRearranger columnRearranger = new ColumnRearranger(spec);
        // check input and create variables used by the cell factory
        CheckUtils.checkSetting(!m_columnNameModel.getStringValue().trim().isEmpty(),
            "The output column name must not be empty.");
        final int[] colIndices = getSourceColumns(spec);
        if (m_removeIncludedColumns.getBooleanValue()) {
            columnRearranger.remove(colIndices);
        }
        final DataColumnSpecCreator colSpecCreator = new UniqueNameGenerator(columnRearranger.createSpec())
            .newCreator(m_columnNameModel.getStringValue(), NominalDistributionCellFactory.TYPE);
        columnRearranger.append(getCellFactory(spec, colSpecCreator, colIndices, exec));
        return columnRearranger;
    }

    private boolean isStringColumn() {
        return ColumnType.valueOf(m_columnTypeModel.getStringValue()) == ColumnType.STRING_COLUMN;
    }

    private int[] getSourceColumns(final DataTableSpec spec) throws InvalidSettingsException {
        final int[] colIndices;
        if (isStringColumn()) {
            colIndices = new int[1];
            CheckUtils.checkSetting(spec.findColumnIndex(m_stringFilterModel.getStringValue()) != -1,
                "At least one nominal column must be selected.");
            colIndices[0] = spec.findColumnIndex(m_stringFilterModel.getStringValue());
        } else {
            final String[] includes = m_columnFilterModel.applyTo(spec).getIncludes();
            colIndices = spec.columnsToIndices(includes);
        }
        return colIndices;
    }

    private SingleCellFactory getCellFactory(final DataTableSpec spec, final DataColumnSpecCreator colSpecCreator,
        final int[] colIndices, final ExecutionContext exec) throws InvalidSettingsException {
        final MissingValueHandling missingValueHandling = getMissingValueHandling();
        if (isStringColumn()) {
            return getStringCellFactory(spec, colSpecCreator, colIndices[0], missingValueHandling, exec);
        } else {
            return getNumericCellFactory(spec, colSpecCreator, colIndices, missingValueHandling, exec);
        }
    }

    private ProbDistributionStringCellFactory getStringCellFactory(final DataTableSpec spec,
        final DataColumnSpecCreator colSpecCreator, final int colIndices,
        final MissingValueHandling missingValueHandling, final ExecutionContext exec) throws InvalidSettingsException {
        CheckUtils.checkSetting(m_stringFilterModel.getStringValue() != null, "At least one column must be selected.");
        DataColumnSpec chosenColumn = spec.getColumnSpec(m_stringFilterModel.getStringValue());
        final String[] possibleValues = getPossibleValues(chosenColumn);
        colSpecCreator.addMetaData(new NominalDistributionValueMetaData(possibleValues), false);
        return new ProbDistributionStringCellFactory(colSpecCreator.createSpec(), colIndices, missingValueHandling,
            exec);
    }

    private ProbDistributionCellFactory getNumericCellFactory(final DataTableSpec spec,
        final DataColumnSpecCreator colSpecCreator, final int[] colIndices,
        final MissingValueHandling missingValueHandling, final ExecutionContext exec) throws InvalidSettingsException {
        final ExceptionHandling invalidDistributionHandling = getExceptionHandling();
        CheckUtils.checkSetting(colIndices.length > 0, "At least one column must be selected.");
        CheckUtils.checkSetting(m_precisionModel.getIntValue() > 0, "The number of decimal digits must be > 0.");
        final double epsilon =
            m_allowUnpreciseProbabilities.getBooleanValue() ? Math.pow(10, -m_precisionModel.getIntValue()) : 0;
        CheckUtils.checkSetting(epsilon >= 0, "Epsilon must not be negative.");
        final String[] includes = m_columnFilterModel.applyTo(spec).getIncludes();
        colSpecCreator.addMetaData(new NominalDistributionValueMetaData(includes), false);
        return new ProbDistributionCellFactory(colSpecCreator.createSpec(), epsilon, invalidDistributionHandling,
            colIndices, missingValueHandling, exec);
    }

    private static String[] getPossibleValues(final DataColumnSpec chosenColumn) throws InvalidSettingsException {
        CheckUtils.checkSetting(chosenColumn.getType().isCompatible(NominalValue.class),
            "The picked column is not nominal.");
        CheckUtils.checkSetting(chosenColumn.getDomain().hasValues(),
            "The selected column '%s' does not have domain information available."
                + " Execute preceding nodes or use a Domain Calculator to calculate its domain.",
            chosenColumn.getName());
        Set<DataCell> possibleValues = chosenColumn.getDomain().getValues();
        Set<String> possibleStrings = possibleValues.stream().map(DataCell::toString).map(String::trim)
            .collect(Collectors.toCollection(LinkedHashSet::new));
        CheckUtils.checkSetting(possibleValues.size() == possibleStrings.size(),
            "Some of the possible values are equal after trimming whitespaces.");
        CheckUtils.checkSetting(!possibleStrings.contains(""),
            "After trimming at least one possible values is the empty string which is not supported.");
        return possibleStrings.toArray(new String[0]);
    }

    private MissingValueHandling getMissingValueHandling() throws InvalidSettingsException {
        try {
            return MissingValueHandling.valueOf(m_missingValueHandling.getStringValue());
        } catch (IllegalArgumentException e) {
            throw new InvalidSettingsException(
                "The selected missing value or invalid distribution handling strategy does not exist.");
        }
    }

    private ExceptionHandling getExceptionHandling() throws InvalidSettingsException {
        try {
            return ExceptionHandling.valueOf(m_invalidDistributionHandling.getStringValue());
        } catch (IllegalArgumentException e) {
            throw new InvalidSettingsException(
                "The selected missing value or invalid distribution handling strategy does not exist.");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_columnFilterModel.saveSettingsTo(settings);
        m_precisionModel.saveSettingsTo(settings);
        m_columnNameModel.saveSettingsTo(settings);
        m_allowUnpreciseProbabilities.saveSettingsTo(settings);
        m_missingValueHandling.saveSettingsTo(settings);
        m_invalidDistributionHandling.saveSettingsTo(settings);
        m_removeIncludedColumns.saveSettingsTo(settings);
        m_columnTypeModel.saveSettingsTo(settings);
        m_stringFilterModel.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_columnFilterModel.validateSettings(settings);
        m_precisionModel.validateSettings(settings);
        m_columnNameModel.validateSettings(settings);
        m_allowUnpreciseProbabilities.validateSettings(settings);
        m_missingValueHandling.validateSettings(settings);
        m_invalidDistributionHandling.validateSettings(settings);
        m_removeIncludedColumns.validateSettings(settings);
        m_columnTypeModel.validateSettings(settings);
        m_stringFilterModel.validateSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_columnFilterModel.loadSettingsFrom(settings);
        m_precisionModel.loadSettingsFrom(settings);
        m_columnNameModel.loadSettingsFrom(settings);
        m_allowUnpreciseProbabilities.loadSettingsFrom(settings);
        m_missingValueHandling.loadSettingsFrom(settings);
        m_invalidDistributionHandling.loadSettingsFrom(settings);
        m_removeIncludedColumns.loadSettingsFrom(settings);
        m_columnTypeModel.loadSettingsFrom(settings);
        m_stringFilterModel.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        // no op
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        // no op
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        // no op
    }

    /**
     * Abstract implementation of a cell factory that appends a {@link NominalDistributionCell} column to a table.
     *
     * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
     */
    private abstract class AbstractProbDistributionCellFactory extends SingleCellFactory {

        private boolean m_hasWarning = false;

        protected final MissingValueHandling m_missingHandling;

        protected final NominalDistributionCellFactory m_factory;

        protected AbstractProbDistributionCellFactory(final DataColumnSpec newColSpec,
            final MissingValueHandling missingHandling, final ExecutionContext exec) {
            super(newColSpec);
            m_missingHandling = missingHandling;
            if (exec != null) {
                m_factory = new NominalDistributionCellFactory(FileStoreFactory.createFileStoreFactory(exec),
                    getValues(newColSpec));
            } else {
                m_factory = null;
            }
        }

        protected void setWarningIfNotSet(final String message) {
            if (!m_hasWarning) {
                setWarningMessage(message);
                m_hasWarning = true;
            }
        }

        private String[] getValues(final DataColumnSpec spec) {
            final NominalDistributionValueMetaData metaData = NominalDistributionValueMetaData.extractFromSpec(spec);
            return metaData.getValues().toArray(new String[0]);
        }

    }

    /**
     * Appends a probability distribution for a string column.
     *
     * @author Perla Gjoka, KNIME GmbH, Konstanz, Germany
     */
    private final class ProbDistributionStringCellFactory extends AbstractProbDistributionCellFactory {

        private final int m_columnIndex;

        private ProbDistributionStringCellFactory(final DataColumnSpec newColSpec, final int columnIndex,
            final MissingValueHandling missingHandling, final ExecutionContext exec) {
            super(newColSpec, missingHandling, exec);
            m_columnIndex = columnIndex;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public DataCell getCell(final DataRow row) {
            assert m_factory != null : "The cell factory may only be null during the configuration phase.";
            DataCell stringCell = row.getCell(m_columnIndex);
            if (stringCell.isMissing()) {
                switch (m_missingHandling) {
                    case FAIL:
                    case ZERO:
                        throw new IllegalArgumentException(
                            "The row '" + row.getKey().getString() + "' contains missing values.");
                    case IGNORE:
                        setWarningIfNotSet(
                            "At least one row contains a missing value. Missing values will be in the output.");
                        return new MissingCell("Input row contains missing values.");
                }
            }
            return m_factory.createCell(stringCell.toString());
        }
    }

    /**
     * Appends a probability distribution column.
     *
     * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
     */
    private final class ProbDistributionCellFactory extends AbstractProbDistributionCellFactory {

        private final double m_epsilon;

        private final ExceptionHandling m_invalidHandling;

        private final int[] m_colIndices;

        boolean m_hasInvalidDistribution = false;

        private ProbDistributionCellFactory(final DataColumnSpec newColSpec, final double epsilon,
            final ExceptionHandling invalidDistributionHandling, final int[] colIndices,
            final MissingValueHandling missingValueHandling, final ExecutionContext exec) {
            super(newColSpec, missingValueHandling, exec);
            m_epsilon = epsilon;
            m_invalidHandling = invalidDistributionHandling;
            m_colIndices = colIndices;
        }

        @Override
        public DataCell getCell(final DataRow row) {
            assert m_factory != null : "The cell factory may only be null during the configuration phase.";
            double[] values = new double[m_colIndices.length];
            int i = 0;
            for (final int idx : m_colIndices) {
                final DataCell cell = row.getCell(idx);
                if (cell.isMissing()) {
                    switch (m_missingHandling) {
                        case FAIL:
                            throw new IllegalArgumentException(
                                "The row '" + row.getKey().getString() + "' contains missing values.");
                        case IGNORE:
                            setWarningIfNotSet(
                                "At least one row contains a missing value. Missing values will be in the "
                                    + "output.");
                            return new MissingCell("Input row contains missing values.");
                        case ZERO:
                            setWarningIfNotSet(
                                "At least one row contains a missing value. They have been treated as zeroes.");
                            values[i] = 0;
                            i++;
                            continue;
                    }
                }
                values[i] = ((DoubleValue)cell).getDoubleValue();
                i++;
            }
            try {
                return m_factory.createCell(values, m_epsilon);
            } catch (IllegalArgumentException e) {
                if (m_invalidHandling == ExceptionHandling.FAIL) {
                    throw new IllegalArgumentException(
                        "The distribution of row '" + row.getKey().getString() + "' is invalid: " + e.getMessage(), e);
                } else {
                    // set the same warning only once
                    if (!m_hasInvalidDistribution) {
                        setWarningMessage(
                            "The distribution of at least one row is invalid. Missing values will be in the output."
                                + " Hovering over the missing values display more details.");
                        m_hasInvalidDistribution = true;
                    }
                    return new MissingCell(e.getMessage());
                }
            }
        }
    }

}
