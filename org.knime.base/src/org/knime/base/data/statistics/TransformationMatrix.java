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
package org.knime.base.data.statistics;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.EigenDecomposition;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.knime.base.node.mine.transformation.util.TransformationUtils;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataType;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.ModelContentRO;
import org.knime.core.node.ModelContentWO;
import org.knime.core.node.util.CheckUtils;

/**
 * This class accepts a matrix and calculates its {@link EigenDecomposition}.
 *
 * @author Mark Ortmann, KNIME GmbH, Berlin, Germany
 * @since 3.8
 */
public final class TransformationMatrix {

    private static final String EIGENVECTOR_CONTENT_KEY = "eigenvector";

    private static final String EIGENVECTOR_ROW_KEYPREFIX = "row_";

    private static final String EIGENVALUES_KEY = "eigenvalues";

    private static final String CENTER_KEY = "center";

    private static final String NUM_EIGENVECTOR_ROWS_KEY = "number_rows";

    private static final String MAX_DIM_KEY = "max_dim_to_reduce_to";

    private final RealVector m_sortedEigenVals;

    private final RealMatrix m_sortedEigenVecs;

    private final RealVector m_centers;

    private final int m_maxDimToReduceTo;

    private TransformationMatrix(final double[] sortedEigenVals, final double[][] sortedEigenVecs,
        final double[] centers, final int maxDimToReduceTo) {
        CheckUtils.checkArgumentNotNull(sortedEigenVals, "Eigenvalues cannot be null");
        CheckUtils.checkArgumentNotNull(sortedEigenVecs, "Eigenvectors cannot be null");
        for (final double[] vec : sortedEigenVecs) {
            CheckUtils.checkArgumentNotNull(vec, "Eigenvector cannot be null");
        }
        CheckUtils.checkArgumentNotNull(centers, "Centers cannot be null");
        CheckUtils.checkArgument(sortedEigenVals.length == sortedEigenVecs.length,
            "Number of Eigenvectors and values differs");
        CheckUtils.checkArgument(sortedEigenVals.length <= centers.length, "Less centers than Eigenvalues/vectors");
        for (final double[] eigenVec : sortedEigenVecs) {
            CheckUtils.checkNotNull(eigenVec, "Eigenvectors cannot contain a null vector");
        }
        m_sortedEigenVals = new ArrayRealVector(sortedEigenVals, false);
        m_sortedEigenVecs = new Array2DRowRealMatrix(sortedEigenVecs, false);
        m_centers = new ArrayRealVector(centers, false);
        m_maxDimToReduceTo = maxDimToReduceTo;
    }

    /**
     * Constructor.
     *
     * @param matrix the matrix for which the eigenvalue decomposition has toe be calculated
     * @param maxDimToReduceTo the maximum possible number of dimensions to reduce to
     */
    public TransformationMatrix(final RealMatrix matrix, final int maxDimToReduceTo) {
        this(matrix, null, maxDimToReduceTo);
    }

    /**
     * Constructor.
     *
     * @param matrix the matrix for which the eigenvalue decomposition has toe be calculated
     * @param centers the centers for the columns, i.e., the column means
     */
    public TransformationMatrix(final RealMatrix matrix, final RealVector centers) {
        this(matrix, centers, Integer.MAX_VALUE);
    }

    /**
     * Constructor.
     *
     * @param matrix the matrix for which the eigenvalue decomposition has toe be calculated
     * @param centers the centers for the columns, i.e., the column means
     * @param maxDimToReduceTo the maximum possible number of dimensions to reduce to
     */
    public TransformationMatrix(final RealMatrix matrix, final RealVector centers, final int maxDimToReduceTo) {
        CheckUtils.checkNotNull(matrix, "Matrix cannot be null");
        CheckUtils.checkArgument(maxDimToReduceTo > 0, "Max dimensions has to be greater than zero");
        final EigenDecomposition eigenDecomp = new EigenDecomposition(matrix);
        final double[] eVals = eigenDecomp.getRealEigenvalues();
        int[] permutation = IntStream.range(0, eVals.length)//
            .boxed()//
            .sorted((i, j) -> Double.compare(eVals[j], eVals[i]))//
            .mapToInt(x -> x)//
            .toArray();
        m_sortedEigenVals = new ArrayRealVector(Arrays.stream(permutation)//
            .mapToDouble(i -> eVals[i])//
            .toArray(), false);
        m_sortedEigenVecs = normalizeEigenvectorsAndSigns(eigenDecomp, permutation);
        // if no means are provided use a 0 mean array
        if (centers == null) {
            m_centers = new ArrayRealVector(eVals.length);
        } else {
            m_centers = centers;
        }
        m_maxDimToReduceTo = Math.min(maxDimToReduceTo, m_centers.getDimension());
    }

    private static RealMatrix normalizeEigenvectorsAndSigns(final EigenDecomposition eigenDecomp,
        final int[] permutation) {
        final double[][] normEigenVecs = Arrays.stream(permutation)//
            .mapToObj(
                i -> eigenDecomp.getEigenvector(i).mapMultiply(1.0 / eigenDecomp.getEigenvector(i).getNorm()).toArray())
            .toArray(double[][]::new);
        return new Array2DRowRealMatrix(normEigenVecs, false);
    }

    /**
     * Returns the maximum possible number of dimensions to reduce to.
     *
     * @return the maximum number of dimensions to reduce to
     */
    public int getMaxDimToReduceTo() {
        return m_maxDimToReduceTo;
    }

    /**
     * Returns the column centers of the matrix, i.e., the column means.
     *
     * @return the columns centers.
     */
    public RealVector getCenters() {
        return m_centers;
    }

    /**
     * Returns the non-increasingly sorted eigenvalues.
     *
     * @return the non-increasingly sorted eigenvalues
     */
    public RealVector getSortedEigenValues() {
        return m_sortedEigenVals;
    }

    /**
     * Returns the eigenvectors sorted according to the eigenvalues. Each row of this matrix corresponds to an
     * eigenvector.
     *
     * @return the eigenvector matrix
     */
    public RealMatrix getSortedEigenVectors() {
        return m_sortedEigenVecs;
    }

    /**
     * Calculates the projection of the data in the row.
     *
     * @param row the row to calculate the projection for. Included fields are those that were given the constructor.
     * @param colIdx the column indices
     * @param failOnMissings flag indicating whether to fail if missing values are encountered (@code{true}) or skip
     *            those rows ({@code false})
     * @return an array of double cells that constitutes the projection of the data.
     * @throws InvalidSettingsException when there are missing values
     *
     */
    public DataCell[] getProjection(final DataRow row, final int[] colIdx, final boolean failOnMissings)
        throws InvalidSettingsException {
        return getProjection(row, colIdx, colIdx.length, failOnMissings);
    }

    /**
     * Calculates the projection of the data in the row.
     *
     * @param row the row to calculate the projection for. Included fields are those that were given the constructor.
     * @param colIdx the column indices
     * @param dim the number of dimensions of the projection
     * @param failOnMissings flag indicating whether to fail if missing values are encountered (@code{true}) or skip
     *            those rows ({@code false})
     * @return the projected data
     * @throws InvalidSettingsException when there are missing values
     *
     */
    public DataCell[] getProjection(final DataRow row, final int[] colIdx, final int dim, final boolean failOnMissings)
        throws InvalidSettingsException {
        final Optional<RealVector> rVec = TransformationUtils.rowToRealVector(row, colIdx);
        if (!rVec.isPresent()) {
            if (failOnMissings) {
                throw new IllegalArgumentException(TransformationUtils.MISSING_VALUE_EXCEPTION);
            }
            return Stream.generate(DataType::getMissingCell).limit(dim).toArray(DataCell[]::new);
        }
        return writeCells(calculateProjection(rVec.get()), dim);
    }

    private RealVector calculateProjection(final RealVector rVec) {
        return m_sortedEigenVecs.operate(rVec.combineToSelf(1, -1, m_centers));
    }

    private static DataCell[] writeCells(final RealVector res, final int dim) {
        final DataCell[] cells = new DoubleCell[dim];
        for (int i = 0; i < dim; i++) {
            cells[i] = new DoubleCell(res.getEntry(i));
        }
        return cells;
    }

    /**
     * @param row the row to calculate the projection for. Included fields are those that were given the constructor.
     * @param colIdx the column indices
     * @param dim the number of dimensions of the projection
     * @param failOnMissings flag indicating whether to fail if missing values are encountered (@code{true}) or skip
     *            those rows ({@code false})
     * @return the reversed projection of the data
     */
    public DataCell[] reverseProjection(final DataRow row, final int[] colIdx, final int dim,
        final boolean failOnMissings) {
        final Optional<RealVector> rVec = TransformationUtils.rowToRealVector(row, colIdx);
        if (!rVec.isPresent()) {
            if (failOnMissings) {
                throw new IllegalArgumentException(TransformationUtils.MISSING_VALUE_EXCEPTION);
            }
            return Stream.generate(DataType::getMissingCell).limit(dim).toArray(DataCell[]::new);
        }
        return writeCells(reverseProjection(rVec.get(), dim), dim);
    }

    private RealVector reverseProjection(final RealVector rVec, final int dim) {
        final double[] out = new double[dim];
        for (int col = 0; col < dim; ++col) {
            double sum = m_centers.getEntry(col);
            for (int i = 0; i < rVec.getDimension(); ++i) {
                sum += m_sortedEigenVecs.getEntry(i, col) * rVec.getEntry(i);
            }
            out[col] = sum;
        }
        return new ArrayRealVector(out, false);
    }

    /**
     * Saves the transformation matrix to the given model.
     *
     * @param model the model to save the matrix to
     */
    public void save(final ModelContentWO model) {
        model.addInt(MAX_DIM_KEY, m_maxDimToReduceTo);
        model.addDoubleArray(EIGENVALUES_KEY, m_sortedEigenVals.toArray());
        model.addDoubleArray(CENTER_KEY, m_centers.toArray());
        model.addInt(NUM_EIGENVECTOR_ROWS_KEY, m_sortedEigenVecs.getRowDimension());
        storeEigenvectors(model.addModelContent(EIGENVECTOR_CONTENT_KEY));
    }

    private void storeEigenvectors(final ModelContentWO eigenVecModel) {
        for (int i = 0; i < m_sortedEigenVecs.getRowDimension(); i++) {
            eigenVecModel.addDoubleArray(EIGENVECTOR_ROW_KEYPREFIX + i, m_sortedEigenVecs.getRow(i));
        }
    }

    /**
     * Retrieves the transformation from the given model.
     *
     * @param model the model holding the transformation matrix
     * @return the {@code TransformationMatrix}
     * @throws InvalidSettingsException
     */
    public static TransformationMatrix load(final ModelContentRO model) throws InvalidSettingsException {
        final double[] eigenVals = model.getDoubleArray(EIGENVALUES_KEY);
        final double[][] eigenVecs =
            loadEigenvectors(model.getModelContent(EIGENVECTOR_CONTENT_KEY), model.getInt(NUM_EIGENVECTOR_ROWS_KEY));
        return new TransformationMatrix(eigenVals, eigenVecs, model.getDoubleArray(CENTER_KEY),
            model.getInt(MAX_DIM_KEY));
    }

    private static double[][] loadEigenvectors(final ModelContentRO eigenVecModel, final int numEigenVecs)
        throws InvalidSettingsException {
        final double[][] eigenVecs = new double[numEigenVecs][];
        for (int i = 0; i < numEigenVecs; i++) {
            eigenVecs[i] = eigenVecModel.getDoubleArray(EIGENVECTOR_ROW_KEYPREFIX + i);
        }
        return eigenVecs;
    }
}