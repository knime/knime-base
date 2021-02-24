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
 *   May 2, 2019 (Mark Ortmann, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.base.data.statistics.calculation;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Stream;

import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math3.stat.descriptive.moment.Mean;
import org.knime.base.data.statistics.TransformationMatrix;
import org.knime.base.node.mine.transformation.util.TransformationUtils;
import org.knime.core.data.DataRow;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.util.CheckUtils;

/**
 * Calculates the principal component analysis (PCA).
 *
 * @author Mark Ortmann, KNIME GmbH, Berlin, Germany
 * @since 4.0
 */
public final class PCA {

    /** The covariance matrix. */
    private RealMatrix m_covMtx;

    /** The number of missing rows. */
    private int m_missings;

    /**
     * Calculates the PCA.
     *
     * @param exec the execution context
     * @param inTable the input table
     * @param colNames the columns for which the PCA has to be computed
     * @param failOnMissingValues flag indicating if the computation should fail if a row contains missing values
     *            ({@code true}), or just ignore these rows ({@code false})
     * @return the PCA
     * @throws CanceledExecutionException - If the execution gets canceled
     */
    public TransformationMatrix calcTransformationMatrix(final ExecutionContext exec, final BufferedDataTable inTable,
        final String[] colNames, final boolean failOnMissingValues) throws CanceledExecutionException {
        final int[] colIdx = inTable.getSpec().columnsToIndices(colNames);
        final RealVector means =
            calculateColMeans(exec.createSubExecutionContext(0.33), inTable, colIdx, failOnMissingValues);
        calcCovarMatrix(exec.createSubExecutionContext(0.33), inTable, colIdx, means);
        correctCovarMatrix(inTable.size());
        final TransformationMatrix transMatrix = new TransformationMatrix(m_covMtx, means);
        exec.setProgress(1);
        return transMatrix;
    }

    /**
     * Calculates the column means in order to center the input table columns.
     *
     * @param exec the execution context
     * @param inTable the input table
     * @param colIdx the columns for which the column means have to be computed
     * @param failOnMissingValuesflag indicating if the computation should fail if a row contains missing values
     *            ({@code true}), or just ignore these rows ({@code false})
     * @return the column means
     * @throws CanceledExecutionException - If the execution gets canceled
     */
    private RealVector calculateColMeans(final ExecutionContext exec, final BufferedDataTable inTable,
        final int[] colIdx, final boolean failOnMissingValues) throws CanceledExecutionException {
        final long nRow = inTable.size();
        final int nCols = colIdx.length;
        double curRow = 0;
        final Mean[] means = Stream.generate(Mean::new).limit(nCols).toArray(Mean[]::new);
        // calculate column means
        for (final DataRow row : inTable) {
            exec.checkCanceled();
            final Optional<RealVector> optRowVec = TransformationUtils.rowToRealVector(row, colIdx);
            if (!optRowVec.isPresent()) {
                ++m_missings;
                if (failOnMissingValues) {
                    throw new IllegalArgumentException(TransformationUtils.MISSING_VALUE_EXCEPTION);
                }
                continue;
            }
            final RealVector rowVec = optRowVec.get();
            for (int i = 0; i < nCols; i++) {
                exec.setProgress(++curRow / nRow);
                means[i].increment(rowVec.getEntry(i));
            }
        }
        return new ArrayRealVector(Arrays.stream(means).mapToDouble(Mean::getResult).toArray());
    }

    /**
     * Calculate the covariance matrix.
     *
     * @param exec the execution context.
     * @param inTable the input table
     * @param colIdx the columns for which the covariance matrix has to be calculated
     * @param means the column means used to center the matrix
     * @throws CanceledExecutionException - If the execution gets canceled
     */
    private void calcCovarMatrix(final ExecutionContext exec, final BufferedDataTable inTable, final int[] colIdx,
        final RealVector means) throws CanceledExecutionException {
        final long nRow = inTable.size();
        double curRow = 0;
        for (final DataRow row : inTable) {
            exec.checkCanceled();
            final Optional<RealVector> optVec = TransformationUtils.rowToRealVector(row, colIdx);
            optVec.ifPresent(v -> updateCovarMatrix(v.combineToSelf(1, -1, means)));
            exec.setProgress(++curRow / nRow);
        }
    }

    /**
     * Updates the covariance matrix.
     *
     * @param vec the vector to be added to the covariance matrix
     */
    private void updateCovarMatrix(final RealVector vec) {
        final RealMatrix outerProd = vec.outerProduct(vec);
        m_covMtx = m_covMtx == null ? outerProd : m_covMtx.add(outerProd);
    }

    /**
     * Finalizes the covariance calculation by scaling it's entries.
     *
     * @param nRows the number of rows in the input table
     */
    private void correctCovarMatrix(final long nRows) {
        final double nonMissings = nRows - m_missings;
        CheckUtils.checkArgument(nonMissings >= 2, "The table has to contain at least two rows with valid values");
        m_covMtx = m_covMtx.scalarMultiply(1 / (nonMissings - 1));
    }

    /**
     * Returns the covariance matrix.
     *
     * @return the covariance matrix if it has been computed
     */
    public Optional<RealMatrix> getCovMatrix() {
        return Optional.ofNullable(m_covMtx);
    }

}
