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
 * History
 *   Feb 17, 2007 (wiswedel): created
 */
package org.knime.base.node.preproc.correlation.compute2;

import java.io.File;
import java.io.IOException;

import org.knime.base.node.preproc.correlation.CorrelationUtils;
import org.knime.base.node.preproc.correlation.CorrelationUtils.CorrelationResult;
import org.knime.base.node.preproc.correlation.pmcc.PMCCPortObjectAndSpec;
import org.knime.base.node.preproc.correlation.pmcc.PValueAlternative;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.NominalValue;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.BufferedDataTableHolder;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelColumnFilter2;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.node.util.filter.NameFilterConfiguration.FilterResult;

/**
 * @author Bernd Wiswedel, University of Konstanz
 * @author Benjamin Wilhelm, KNIME GmbH, Konstanz, Germany
 */
final class CorrelationCompute2NodeModel extends NodeModel implements BufferedDataTableHolder {

    /** Progress of the first step */
    private static final double PROG_STEP1 = 0.48;

    /** Progress of the second step */
    private static final double PROG_STEP2 = 0.48;

    /** Progress of the last step */
    private static final double PROG_FINISH = 1 - PROG_STEP1 - PROG_STEP2;

    private static final NodeLogger LOGGER = NodeLogger.getLogger(CorrelationCompute2NodeModel.class);

    /**
     * Factory method to instantiate a default settings object, used in constructor and in dialog.
     *
     * @return A new default settings object.
     */
    @SuppressWarnings("unchecked")
    static SettingsModelColumnFilter2 createColumnFilterModel() {
        return new SettingsModelColumnFilter2("include-list", DoubleValue.class, NominalValue.class);
    }

    /**
     * Factory method to create the bounded range model for the possible values count.
     *
     * @return A new model.
     */
    static SettingsModelIntegerBounded createNewPossValueCounterModel() {
        return new SettingsModelIntegerBounded("possibleValuesCount", 50, 2, Integer.MAX_VALUE);
    }

    /**
     * Factory method to create the string model for the p-value alternative.
     *
     * @return A new model.
     */
    static SettingsModelString createPValAlternativeModel() {
        return new SettingsModelString("pvalAlternative", PValueAlternative.TWO_SIDED.name());
    }

    private SettingsModelColumnFilter2 m_columnFilterModel;

    private final SettingsModelIntegerBounded m_maxPossValueCountModel;

    private final SettingsModelString m_pValAlternativeModel;

    private BufferedDataTable m_correlationTable;

    /**
     * One input, one output.
     */
    CorrelationCompute2NodeModel() {
        super(new PortType[]{BufferedDataTable.TYPE},
            new PortType[]{BufferedDataTable.TYPE, PMCCPortObjectAndSpec.TYPE});
        m_maxPossValueCountModel = createNewPossValueCounterModel();
        m_pValAlternativeModel = createPValAlternativeModel();
    }

    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        DataTableSpec in = (DataTableSpec)inSpecs[0];
        CheckUtils.checkSetting(
            in.containsCompatibleType(DoubleValue.class) || in.containsCompatibleType(NominalValue.class),
            "No double or nominal compatible columns in input");
        final String[] includesNames;
        if (m_columnFilterModel == null) {
            m_columnFilterModel = createColumnFilterModel();
            // auto-configure, no previous configuration
            m_columnFilterModel.loadDefaults(in);
            includesNames = m_columnFilterModel.applyTo(in).getIncludes();
            setWarningMessage(
                "Auto configuration: Using all suitable " + "columns (in total " + includesNames.length + ")");
        } else {
            FilterResult applyTo = m_columnFilterModel.applyTo(in);
            includesNames = applyTo.getIncludes();
        }
        if (includesNames.length == 0) {
            throw new InvalidSettingsException("No columns selected");
        }
        return new PortObjectSpec[]{CorrelationUtils.createCorrelationOutputTableSpec(),
            new PMCCPortObjectAndSpec(includesNames)};
    }

    @Override
    protected PortObject[] execute(final PortObject[] inData, final ExecutionContext exec) throws Exception {
        final BufferedDataTable in = (BufferedDataTable)inData[0];
        final DataTableSpec inSpec = in.getDataTableSpec();

        // Filter selected columns
        ColumnRearranger filteredTableRearranger = new ColumnRearranger(inSpec);
        String[] includeNames = m_columnFilterModel.applyTo(inSpec).getIncludes();
        filteredTableRearranger.keepOnly(includeNames);
        final BufferedDataTable filteredTable =
            exec.createColumnRearrangeTable(in, filteredTableRearranger, exec.createSilentSubExecutionContext(0.0));
        final DataTableSpec filteredTableSpec = filteredTable.getDataTableSpec();

        // Create the correlation computer
        CorrelationComputer2 calculator =
            new CorrelationComputer2(filteredTableSpec, m_maxPossValueCountModel.getIntValue());

        // Calculate statistics on the table
        exec.setMessage("Calculating table statistics");
        ExecutionContext execStep1 = exec.createSubExecutionContext(PROG_STEP1);
        calculator.calculateStatistics(filteredTable, execStep1);
        execStep1.setProgress(1.0);

        // Calculate the correlation
        exec.setMessage("Calculating correlation values");
        ExecutionMonitor execStep2 = exec.createSubExecutionContext(PROG_STEP2);
        CorrelationResult correlationResult =
            calculator.calculateOutput(filteredTable, execStep2, selectedPValAlternative());
        execStep2.setProgress(1.0);

        // Create the output
        exec.setMessage("Assembling output");
        ExecutionContext execFinish = exec.createSubExecutionContext(PROG_FINISH);
        PMCCPortObjectAndSpec pmccModel = new PMCCPortObjectAndSpec(includeNames,
            correlationResult.getCorrelationMatrix(), correlationResult.getpValMatrix(),
            correlationResult.getDegreesOfFreedomMatrix(), selectedPValAlternative());
        BufferedDataTable out = CorrelationUtils.createCorrelationOutputTable(correlationResult, includeNames,
            execFinish.createSubExecutionContext(0.5));
        m_correlationTable = pmccModel.createCorrelationMatrix(execFinish.createSubExecutionContext(0.5));

        // Warning handling
        String missValueString = calculator.getNumericMissingValueWarning(4);
        StringBuilder warning = null;
        if (missValueString != null) {
            LOGGER.debug(calculator.getNumericMissingValueWarning(1000));
            warning = new StringBuilder(missValueString);
        }
        String constantColString = calculator.getNumericConstantColumnPairs(4);
        if (constantColString != null) {
            LOGGER.debug(calculator.getNumericConstantColumnPairs(1000));
            if (warning == null) {
                warning = new StringBuilder(constantColString);
            } else {
                warning.append("\n");
                warning.append(constantColString);
            }
        }
        if (warning != null) {
            setWarningMessage(warning.toString());
        }

        return new PortObject[]{out, pmccModel};
    }

    private PValueAlternative selectedPValAlternative() {
        return PValueAlternative.valueOf(m_pValAlternativeModel.getStringValue());
    }

    @Override
    protected void reset() {
        // nothing to do
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        if (m_columnFilterModel != null) {
            m_columnFilterModel.saveSettingsTo(settings);
            m_maxPossValueCountModel.saveSettingsTo(settings);
            m_pValAlternativeModel.saveSettingsTo(settings);
        }
    }

    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        createColumnFilterModel().validateSettings(settings);
        m_maxPossValueCountModel.validateSettings(settings);
        m_pValAlternativeModel.validateSettings(settings);
    }

    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        if (m_columnFilterModel == null) {
            m_columnFilterModel = createColumnFilterModel();
        }
        m_columnFilterModel.loadSettingsFrom(settings);
        m_maxPossValueCountModel.loadSettingsFrom(settings);
        m_pValAlternativeModel.loadSettingsFrom(settings);
    }

    /**
     * Getter for correlation table to display. <code>null</code> if not executed.
     *
     * @return the correlationTable
     */
    public DataTable getCorrelationTable() {
        return m_correlationTable;
    }

    @Override
    protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        // nothing to do
    }

    @Override
    protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        // nothing to do
    }

    @Override
    public BufferedDataTable[] getInternalTables() {
        return new BufferedDataTable[]{m_correlationTable};
    }

    @Override
    public void setInternalTables(final BufferedDataTable[] tables) {
        m_correlationTable = tables[0];
    }
}
