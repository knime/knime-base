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

import org.apache.commons.math3.stat.descriptive.moment.Mean;
import org.apache.commons.math3.stat.descriptive.summary.SumOfSquares;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
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
 */
class NumericScorer2NodeModel extends NodeModel {

    private static final String MEAN_SIGNED_DIFFERENCE = "meanSignedDifference";

    private static final String RMSD = "rmsd";

    private static final String MEAN_SQUARED_ERROR = "meanSquaredError";

    private static final String MEAN_ABS_ERROR = "meanAbsError";

    private static final String R2 = "R2";

    private static final String MEAN_ABSOLUTE_PERCENTAGE_ERROR = "MAPE";

    private static final String NORMALIZED_MEAN_ABSOLUTE_PERCENTAGE_ERROR = "nMAPE";

    private static final String WEIGHTED_ABSOLUTE_PERCENTAGE_ERROR = "WAPE";

    private static final String INTERNALS_XML_GZ = "internals.xml.gz";

    private final NumericScorer2Settings m_numericScorerSettings = new NumericScorer2Settings();

    private double m_rSquare = Double.NaN;

    private double m_meanAbsError = Double.NaN;

    private double m_meanSquaredError = Double.NaN;

    private double m_rmsd = Double.NaN;

    private double m_meanSignedDifference = Double.NaN;

    private double m_meanAbsolutePercentageError = Double.NaN;

    private double m_normalizedMeanAbsolutePercentageError = Double.NaN;

    private double m_weightedAbsolutePercentageError = Double.NaN;

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
        final DataTableSpec spec = inData[0].getSpec();
        final BufferedDataContainer container = exec.createDataContainer(createOutputSpec(spec));
        final int referenceIdx = spec.findColumnIndex(m_numericScorerSettings.getReferenceColumnName());
        final int predictionIdx = spec.findColumnIndex(m_numericScorerSettings.getPredictionColumnName());
        final Mean meanObserved = new Mean();
        final Mean meanPredicted = new Mean();
        final Mean absError = new Mean();
        final Mean squaredError = new Mean();
        final Mean signedDiff = new Mean();
        final SumOfSquares ssTot = new SumOfSquares();
        final SumOfSquares ssRes = new SumOfSquares();

        final Mean meanAPE = new Mean();
        final Mean nomalizedMAPE = new Mean();
        // for WAPE
        double total = 0;
        double totalDiff = 0;

        boolean skipMAPE = false;
        boolean skipNMAPE = false;

        int skippedRowCount = 0;
        for (final DataRow row : inData[0]) {
            final DataCell refCell = row.getCell(referenceIdx);
            final DataCell predCell = row.getCell(predictionIdx);
            if (refCell.isMissing()) {
                skippedRowCount++;
                continue;
            }
            final double ref = ((DoubleValue)refCell).getDoubleValue();
            if (predCell.isMissing()) {
                throw new IllegalArgumentException("Missing value in prediction column in row: " + row.getKey());
            }
            final double pred = ((DoubleValue)predCell).getDoubleValue();
            meanObserved.increment(ref);
            meanPredicted.increment(pred);
            absError.increment(Math.abs(ref - pred));
            squaredError.increment((ref - pred) * (ref - pred));
            signedDiff.increment(pred - ref);

            // APE family
            // div by zero prevention:
            if (ref == 0) { // can't calculate MAPE
                skipMAPE = true;
                setWarningMessage("Can't calculate Mean Absolute Percentage error: target value is 0! " + row.getKey());
            } else if ((ref < 0) && (pred == 0)) { // can't calculate nMape
                skipNMAPE = true;
                setWarningMessage(
                    "Can't calculate Normalized Mean Absolute Percentage error: target value is below 0 and predicted value is 0! "
                        + row.getKey());
            }

            if (!skipMAPE) {
                meanAPE.increment(Math.abs(ref - pred) / Math.abs(ref));
            }
            if (!skipNMAPE) {
                nomalizedMAPE.increment(Math.abs(ref - pred) / Math.abs(Math.max(ref, pred)));
            }

            // WAPE
            total += ref;
            totalDiff += Math.abs(ref - pred);
        }
        for (final DataRow row : inData[0]) {
            final DataCell refCell = row.getCell(referenceIdx);
            final DataCell predCell = row.getCell(predictionIdx);
            if (refCell.isMissing()) {
                continue;
            }
            final double ref = ((DoubleValue)refCell).getDoubleValue();
            final double pred = ((DoubleValue)predCell).getDoubleValue();
            ssTot.increment(ref - meanObserved.getResult());
            ssRes.increment(ref - pred);
        }
        // create final values
        m_rSquare = 1 - (ssRes.getResult() / ssTot.getResult());
        m_meanAbsError = absError.getResult();
        m_meanSquaredError = squaredError.getResult();
        m_rmsd = Math.sqrt(squaredError.getResult());
        m_meanSignedDifference = signedDiff.getResult();
        m_meanAbsolutePercentageError = skipMAPE ? Double.NaN : meanAPE.getResult();
        m_normalizedMeanAbsolutePercentageError = skipNMAPE ? Double.NaN : nomalizedMAPE.getResult();

        if (total == 0) {
            setWarningMessage("Can't calculate Weighted Absolute Percentage error: sum of all values is 0!");
            m_weightedAbsolutePercentageError = Double.NaN;
        } else {
            m_weightedAbsolutePercentageError = totalDiff / Math.abs(total);
        }

        container.addRowToTable(new DefaultRow("R^2", m_rSquare));
        container.addRowToTable(new DefaultRow("mean absolute error", m_meanAbsError));
        container.addRowToTable(new DefaultRow("mean squared error", m_meanSquaredError));
        container.addRowToTable(new DefaultRow("root mean squared error", m_rmsd));
        container.addRowToTable(new DefaultRow("mean signed difference", m_meanSignedDifference));
        container.addRowToTable(new DefaultRow("mean absolute percentage error", m_meanAbsolutePercentageError));
        container.addRowToTable(
            new DefaultRow("normalized mean absolute percentage error", m_normalizedMeanAbsolutePercentageError));
        container
            .addRowToTable(new DefaultRow("weighted absolute percentage error", m_weightedAbsolutePercentageError));

        container.close();
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
            final String meanAbsName = prefix + "mean absolute error";
            final String meanSquareName = prefix + "mean squared error";
            final String rootmeanName = prefix + "root mean squared error";
            final String meanSignedName = prefix + "mean signed difference";
            final String meanAPEName = prefix + "mean absolute percentage error";
            final String normalizedMAPEName = prefix + "normalized mean absolute percentage error";
            final String weightedAPEName = prefix + "weighted absolute percentage error";
            if (isConfigureOnly && (vars.containsKey(rsquareName) || vars.containsKey(meanAbsName)
                || vars.containsKey(meanSquareName) || vars.containsKey(rootmeanName)
                || vars.containsKey(meanSignedName) || vars.containsKey(meanAPEName)
                || vars.containsKey(normalizedMAPEName) || vars.containsKey(weightedAPEName))) {
                addWarning("A flow variable was replaced!");
            }

            final double rsquare = isConfigureOnly ? 0.0 : m_rSquare;
            final double meanAbs = isConfigureOnly ? 0.0 : m_meanAbsError;
            final double meanSquare = isConfigureOnly ? 0 : m_meanSquaredError;
            final double rootmean = isConfigureOnly ? 0 : m_rmsd;
            final double meanSigned = isConfigureOnly ? 0 : m_meanSignedDifference;
            final double meanAPE = isConfigureOnly ? 0 : m_meanAbsolutePercentageError;
            final double normalizedMAPE = isConfigureOnly ? 0 : m_normalizedMeanAbsolutePercentageError;
            final double weightedAPE = isConfigureOnly ? 0 : m_weightedAbsolutePercentageError;
            pushFlowVariableDouble(rsquareName, rsquare);
            pushFlowVariableDouble(meanAbsName, meanAbs);
            pushFlowVariableDouble(meanSquareName, meanSquare);
            pushFlowVariableDouble(rootmeanName, rootmean);
            pushFlowVariableDouble(meanSignedName, meanSigned);
            pushFlowVariableDouble(meanAPEName, meanAPE);
            pushFlowVariableDouble(normalizedMAPEName, normalizedMAPE);
            pushFlowVariableDouble(weightedAPEName, weightedAPE);
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
        m_rSquare = m_meanAbsError = m_meanSquaredError = m_rmsd = m_meanSignedDifference //
            = m_meanAbsolutePercentageError = m_normalizedMeanAbsolutePercentageError //
                = m_weightedAbsolutePercentageError = Double.NaN;
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
            m_rSquare = set.getDouble(R2);
            m_meanAbsError = set.getDouble(MEAN_ABS_ERROR);
            m_meanSquaredError = set.getDouble(MEAN_SQUARED_ERROR);
            m_rmsd = set.getDouble(RMSD);
            m_meanSignedDifference = set.getDouble(MEAN_SIGNED_DIFFERENCE);

            m_meanAbsolutePercentageError = set.getDouble(MEAN_ABSOLUTE_PERCENTAGE_ERROR);
            m_normalizedMeanAbsolutePercentageError = set.getDouble(NORMALIZED_MEAN_ABSOLUTE_PERCENTAGE_ERROR);
            m_weightedAbsolutePercentageError = set.getDouble(WEIGHTED_ABSOLUTE_PERCENTAGE_ERROR);
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
        set.addDouble(R2, m_rSquare);
        set.addDouble(MEAN_ABS_ERROR, m_meanAbsError);
        set.addDouble(MEAN_SQUARED_ERROR, m_meanSquaredError);
        set.addDouble(RMSD, m_rmsd);
        set.addDouble(MEAN_SIGNED_DIFFERENCE, m_meanSignedDifference);
        set.addDouble(MEAN_ABSOLUTE_PERCENTAGE_ERROR, m_meanAbsolutePercentageError);
        set.addDouble(NORMALIZED_MEAN_ABSOLUTE_PERCENTAGE_ERROR, m_normalizedMeanAbsolutePercentageError);
        set.addDouble(WEIGHTED_ABSOLUTE_PERCENTAGE_ERROR, m_weightedAbsolutePercentageError);

        try (GZIPOutputStream os = new GZIPOutputStream(
            new BufferedOutputStream(new FileOutputStream(new File(internDir, INTERNALS_XML_GZ))))) {
            set.saveToXML(os);
        }
    }

    /**
     * @return the R^2 value
     */
    public double getRSquare() {
        return m_rSquare;
    }

    /**
     * @return the mean absolute error
     */
    public double getMeanAbsError() {
        return m_meanAbsError;
    }

    /**
     * @return the mean squared error
     */
    public double getMeanSquaredError() {
        return m_meanSquaredError;
    }

    /**
     * @return the root mean squared deviation
     */
    public double getRmsd() {
        return m_rmsd;
    }

    /**
     * @return the mean signed difference
     */
    public double getMeanSignedDifference() {
        return m_meanSignedDifference;
    }

    /**
     * @return the weighted absolute percentage error
     */
    public double getWeightedAbsoluteErrorPercentage() {
        return m_weightedAbsolutePercentageError;
    }

    /**
     * @return the mean absolute percentage error
     */
    public double getMeanAbsolutePercentageError() {
        return m_meanAbsolutePercentageError;
    }

    /**
     * @return the normalized mean absolute percentage error
     */
    public double getNormalizedMeanAbsolutePercentageError() {
        return m_normalizedMeanAbsolutePercentageError;
    }

}
