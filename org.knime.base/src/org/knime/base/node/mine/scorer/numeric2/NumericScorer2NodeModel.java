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
 * ------------------------------------------------------------------------
 *
 * History
 *   23.10.2013 (gabor): created
 */
package org.knime.base.node.mine.scorer.numeric2;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.workflow.FlowVariable;

/**
 * This is the model implementation of NumericScorer. Computes the distance between the a numeric column's values and
 * predicted values.
 *
 * @author Gabor Bakos
 * @author Eric Axt
 * @since 4.0
 */
class NumericScorer2NodeModel extends NodeModel {

    private static final String INTERNALS_XML_GZ = "internals.xml.gz";

    private final NumericScorer2Settings m_numericScorerSettings = new NumericScorer2Settings();

    private Metrics metrics;

    /**
     * Constructor for the node model.
     */
    protected NumericScorer2NodeModel() {
        super(1, 1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec)
        throws Exception {

        metrics = new Metrics(inData[0], exec, m_numericScorerSettings, this::setWarningMessage);
        final BufferedDataContainer container =
            exec.createDataContainer(createOutputSpec(inData[0].getDataTableSpec()));
        container.addRowToTable(new DefaultRow("R^2", metrics.getRSquare()));
        container.addRowToTable(new DefaultRow("mean absolute error", metrics.getMeanAbsError()));
        container.addRowToTable(new DefaultRow("mean squared error", metrics.getMeanSquaredError()));
        container.addRowToTable(new DefaultRow("root mean squared error", metrics.getRmsd()));
        container.addRowToTable(new DefaultRow("mean signed difference", metrics.getMeanSignedDifference()));
        container
            .addRowToTable(new DefaultRow("mean absolute percentage error", metrics.getMeanAbsolutePercentageError()));
        container.addRowToTable(new DefaultRow("adjusted R^2", metrics.getAdjustedRSquare()));
        container.close();

        int skippedRowCount = metrics.getSkippedRowCount();
        if (skippedRowCount > 0) {
            setWarningMessage(
                "Skipped " + skippedRowCount + " rows, because the reference column contained missing values there.");
        }

        pushFlowVars(false);
        return new BufferedDataTable[]{container.getTable()};
    }

    /**
     * @param spec Input table spec.
     * @return Output table spec.
     */
    private DataTableSpec createOutputSpec(final DataTableSpec spec) {
        final String o = m_numericScorerSettings.getOutputColumnName();
        final String output =
            m_numericScorerSettings.doOverride() ? o : m_numericScorerSettings.getPredictionColumnName();
        return new DataTableSpec("Scores", new DataColumnSpecCreator(output, DoubleCell.TYPE).createSpec());
    }

    /**
     * Pushes the results to flow variables.
     *
     * @param isConfigureOnly true enable overwriting check
     */
    private void pushFlowVars(final boolean isConfigureOnly) {

        if (m_numericScorerSettings.doFlowVariables()) {
            final Map<String, FlowVariable> vars = getAvailableFlowVariables();

            final String prefix = m_numericScorerSettings.getFlowVariablePrefix();
            final String rsquareName = prefix + "R^2";
            final String adjustedRSquareName = prefix + "adjusted R^2";
            final String meanAbsName = prefix + "mean absolute error";
            final String meanSquareName = prefix + "mean squared error";
            final String rootmeanName = prefix + "root mean squared error";
            final String meanSignedName = prefix + "mean signed difference";
            final String meanAPEName = prefix + "mean absolute percentage error";
            if (isConfigureOnly
                && (vars.containsKey(rsquareName) || vars.containsKey(meanAbsName) || vars.containsKey(meanSquareName)
                    || vars.containsKey(rootmeanName) || vars.containsKey(meanSignedName)
                    || vars.containsKey(meanAPEName) || vars.containsKey(adjustedRSquareName))) {
                addWarning("A flow variable was replaced!");
            }

            final double rsquare = isConfigureOnly ? 0.0 : metrics.getRSquare();
            final double adjustedRSquare = isConfigureOnly ? 0.0 : metrics.getAdjustedRSquare();
            final double meanAbs = isConfigureOnly ? 0.0 : metrics.getMeanAbsError();
            final double meanSquare = isConfigureOnly ? 0 : metrics.getMeanSquaredError();
            final double rootmean = isConfigureOnly ? 0 : metrics.getRmsd();
            final double meanSigned = isConfigureOnly ? 0 : metrics.getMeanSignedDifference();
            final double meanAPE = isConfigureOnly ? 0 : metrics.getMeanAbsolutePercentageError();
            pushFlowVariableDouble(rsquareName, rsquare);
            pushFlowVariableDouble(adjustedRSquareName, adjustedRSquare);
            pushFlowVariableDouble(meanAbsName, meanAbs);
            pushFlowVariableDouble(meanSquareName, meanSquare);
            pushFlowVariableDouble(rootmeanName, rootmean);
            pushFlowVariableDouble(meanSignedName, meanSigned);
            pushFlowVariableDouble(meanAPEName, meanAPE);
        }
    }

    /**
     * @param string
     */
    private void addWarning(final String string) {
        final String warningMessage = getWarningMessage();
        if ((warningMessage == null) || warningMessage.isEmpty()) {
            setWarningMessage(string);
        } else {
            setWarningMessage(warningMessage + "\n" + string);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        metrics.reset();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {
        final DataColumnSpec reference = inSpecs[0].getColumnSpec(m_numericScorerSettings.getReferenceColumnName());
        if (reference == null) {
            if (m_numericScorerSettings.getReferenceColumnName().equals(NumericScorer2Settings.DEFAULT_REFERENCE)) {
                throw new InvalidSettingsException("No columns selected for reference");
            }
            throw new InvalidSettingsException(
                "No such column in input table: " + m_numericScorerSettings.getReferenceColumnName());
        }
        if (!reference.getType().isCompatible(DoubleValue.class)) {
            throw new InvalidSettingsException("The reference column ("
                + m_numericScorerSettings.getReferenceColumnName() + ") is not double valued: " + reference.getType());
        }
        final DataColumnSpec predicted = inSpecs[0].getColumnSpec(m_numericScorerSettings.getPredictionColumnName());
        if (predicted == null) {
            if (m_numericScorerSettings.getPredictionColumnName().equals(NumericScorer2Settings.DEFAULT_PREDICTED)) {
                throw new InvalidSettingsException("No columns selected for prediction");
            }
            throw new InvalidSettingsException(
                "No such column in input table: " + m_numericScorerSettings.getPredictionColumnName());
        }
        if (!predicted.getType().isCompatible(DoubleValue.class)) {
            throw new InvalidSettingsException("The prediction column ("
                + m_numericScorerSettings.getPredictionColumnName() + ") is not double valued: " + predicted.getType());
        }
        pushFlowVars(true);
        return new DataTableSpec[]{createOutputSpec(inSpecs[0])};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_numericScorerSettings.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_numericScorerSettings.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_numericScorerSettings.validateSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File internDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        final File f = new File(internDir, INTERNALS_XML_GZ);
        try (InputStream in = new GZIPInputStream(new BufferedInputStream(new FileInputStream(f)))) {
            final NodeSettingsRO set = NodeSettings.loadFromXML(in);
            metrics = new Metrics(set);
        } catch (final InvalidSettingsException ise) {
            throw new IOException("Unable to read internals", ise);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File internDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        final NodeSettings set = new NodeSettings("scorer");
        metrics.save(set);
        try (GZIPOutputStream os = new GZIPOutputStream(
            new BufferedOutputStream(new FileOutputStream(new File(internDir, INTERNALS_XML_GZ))))) {
            set.saveToXML(os);
        }
    }

}
