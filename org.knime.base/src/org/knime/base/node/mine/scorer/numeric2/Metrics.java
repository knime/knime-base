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
 *   Sep 23, 2021 (eric.axt): created
 */
package org.knime.base.node.mine.scorer.numeric2;

import java.util.function.Consumer;

import org.apache.commons.math3.stat.descriptive.moment.Mean;
import org.apache.commons.math3.stat.descriptive.summary.SumOfSquares;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DoubleValue;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 *
 * @author eric.axt
 * @since 4.5
 */
public class Metrics {

    private static final String MEAN_SIGNED_DIFFERENCE = "meanSignedDifference";

    private static final String RMSD = "rmsd";

    private static final String MEAN_SQUARED_ERROR = "meanSquaredError";

    private static final String MEAN_ABS_ERROR = "meanAbsError";

    private static final String R2 = "R2";

    private static final String ADJUSTED_R2 = "adjustedR2";

    private static final String MEAN_ABSOLUTE_PERCENTAGE_ERROR = "MAPE";

    private  double m_rSquare;

    private double m_adjustedrSquare;

    private  double m_meanAbsError;

    private  double m_meanSquaredError;

    private  double m_rmsd;

    private  double m_meanSignedDifference;

    private  double m_meanAbsolutePercentageError;

    private int m_skippedRowCount;

     Metrics() {
        m_adjustedrSquare = Double.NaN;
        m_rSquare = Double.NaN;
        m_meanAbsError = Double.NaN;
        m_meanSquaredError = Double.NaN;
        m_rmsd = Double.NaN;
        m_meanSignedDifference = Double.NaN;
        m_meanAbsolutePercentageError = Double.NaN;
    }

    /**
     * @param set
     * @throws InvalidSettingsException
     */
    Metrics(final NodeSettingsRO set) throws InvalidSettingsException {
        m_rSquare = set.getDouble(R2);
        m_adjustedrSquare = set.getDouble(ADJUSTED_R2, Double.NaN);
        m_meanAbsError = set.getDouble(MEAN_ABS_ERROR);
        m_meanSquaredError = set.getDouble(MEAN_SQUARED_ERROR);
        m_rmsd = set.getDouble(RMSD);
        m_meanSignedDifference = set.getDouble(MEAN_SIGNED_DIFFERENCE);
        m_meanAbsolutePercentageError = set.getDouble(MEAN_ABSOLUTE_PERCENTAGE_ERROR);
    }

    /**
     * @param inData
     * @param exec Calculates the metrics for numeric scorer node
     * @param m_numericScorerSettings
     * @param warningMessage
     */
    Metrics(final BufferedDataTable inData, final ExecutionContext exec,
        final NumericScorer2Settings m_numericScorerSettings, final Consumer<String> warningMessage) {
        var spec = inData.getDataTableSpec();

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
        boolean skipMAPE = false;

        m_skippedRowCount = 0;
        for (final DataRow row : inData) {
            final DataCell refCell = row.getCell(referenceIdx);
            final DataCell predCell = row.getCell(predictionIdx);

            if (refCell.isMissing()) {
                m_skippedRowCount++;
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
            if (!skipMAPE && (ref == 0)) { // can't calculate MAPE
                skipMAPE = true;
                warningMessage
                    .accept("Can't calculate Mean Absolute Percentage error: target value is 0! " + row.getKey());
            }

            if (!skipMAPE) {
                meanAPE.increment(Math.abs(ref - pred) / Math.abs(ref));
            }
        }
        for (final DataRow row : inData) {
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

        final int p = m_numericScorerSettings.getNumberOfPredictors().getIntValue();
        var n = inData.size();
        // create final values
        m_rSquare = 1 - (ssRes.getResult() / ssTot.getResult());
        m_adjustedrSquare = 1 - (((1 - m_rSquare) * (n - 1)) / (n - p - 1));
        m_meanAbsError = absError.getResult();
        m_meanSquaredError = squaredError.getResult();
        m_rmsd = Math.sqrt(squaredError.getResult());
        m_meanSignedDifference = signedDiff.getResult();
        m_meanAbsolutePercentageError = skipMAPE ? Double.NaN : meanAPE.getResult();

    }

    void save(final NodeSettingsWO set) {
        set.addDouble(R2, m_rSquare);
        set.addDouble(ADJUSTED_R2, m_adjustedrSquare);
        set.addDouble(MEAN_ABS_ERROR, m_meanAbsError);
        set.addDouble(MEAN_SQUARED_ERROR, m_meanSquaredError);
        set.addDouble(RMSD, m_rmsd);
        set.addDouble(MEAN_SIGNED_DIFFERENCE, m_meanSignedDifference);
        set.addDouble(MEAN_ABSOLUTE_PERCENTAGE_ERROR, m_meanAbsolutePercentageError);
    }

    /**
     * @reset values
     */
    final void reset() {
        m_adjustedrSquare = Double.NaN;
        m_rSquare = Double.NaN;
        m_meanAbsError = Double.NaN;
        m_meanSquaredError = Double.NaN;
        m_rmsd = Double.NaN;
        m_meanSignedDifference = Double.NaN;
        m_meanAbsolutePercentageError = Double.NaN;
    }

    /**
     * @return skipped row count
     */
    final int getSkippedRowCount() {
        return m_skippedRowCount;
    }

    /**
     * @return r square
     */
    final double getRSquare() {
        return m_rSquare;
    }

    /**
     * @return adjusted r square
     */
    final double getAdjustedRSquare() {
        return m_adjustedrSquare;
    }

    /**
     * @return mean abs error
     */
    final double getMeanAbsError() {
        return m_meanAbsError;
    }

    /**
     * @return mean squared error
     */
    final double getMeanSquaredError() {
        return m_meanSquaredError;
    }

    /**
     * @return rmsd
     */
    final double getRmsd() {
        return m_rmsd;
    }

    /**
     * @return mean signed difference
     */
    final double getMeanSignedDifference() {
        return m_meanSignedDifference;
    }

    /**
     * @return mean absolute percentageError
     */
    final double getMeanAbsolutePercentageError() {
        return m_meanAbsolutePercentageError;
    }

}
