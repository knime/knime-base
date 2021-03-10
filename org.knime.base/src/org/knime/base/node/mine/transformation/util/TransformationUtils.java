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
package org.knime.base.node.mine.transformation.util;

import java.util.Optional;

import org.apache.commons.lang3.text.WordUtils;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.knime.base.data.statistics.TransformationMatrix;
import org.knime.base.node.mine.transformation.port.TransformationPortObjectSpec.TransformationType;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.RowKey;
import org.knime.core.data.container.AbstractCellFactory;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.util.UniqueNameGenerator;

/**
 * Utility class for transformation related operations.
 *
 * @author Mark Ortmann, KNIME GmbH, Berlin, Germany
 * @since 4.0
 */
public final class TransformationUtils {

    /** The missing value encounter exception. */
    public static final String MISSING_VALUE_EXCEPTION =
        "Missing values are not supported. Please de-select <Fail if missing values are encountered>.";

    private TransformationUtils() {
    }

    /**
     * Extracts the columns from a data row and stores them in a {@link RealVector}.
     *
     * @param row the data row
     * @param colIdx the column indices
     * @return An {@code Optional<RealVector>}. If any of the cells is missing the {@code Optional} is empty, otherwise
     *         it contains a {@code RealVector} storing the double values of the specified column indices
     */
    public static Optional<RealVector> rowToRealVector(final DataRow row, final int[] colIdx) {
        final RealVector d = new ArrayRealVector(colIdx.length);
        for (int c = 0; c < colIdx.length; c++) {
            final DataCell cell = row.getCell(colIdx[c]);
            if (cell.isMissing()) {
                return Optional.empty();
            }
            d.setEntry(c, ((DoubleValue)cell).getDoubleValue());
        }
        return Optional.of(d);
    }

    /**
     * Create a column rearranger that applies the LDA, if given
     *
     * @param inSpec the input table spec
     * @param transMatrix the {@code TransformationMatrix}
     * @param k number of dimensions to reduce to (number of rows in w)
     * @param removeUsedCols whether to remove the input data
     * @param usedColumnNames the names of the used columns, needed for removal
     * @param transType the {@code TransformationType}
     * @return the column re-arranger
     */
    public static ColumnRearranger createColumnRearranger(final DataTableSpec inSpec,
        final TransformationMatrix transMatrix, final int k, final boolean removeUsedCols,
        final String[] usedColumnNames, final TransformationType transType) {
        // use the columnrearranger to exclude the used columns if checked
        final ColumnRearranger cr = new ColumnRearranger(inSpec);

        if (removeUsedCols) {
            cr.remove(usedColumnNames);
        }

        // check that none of the newly put columns is already existing
        final UniqueNameGenerator ung = new UniqueNameGenerator(cr.createSpec());

        final DataColumnSpec[] specs = new DataColumnSpec[k];
        for (int i = 0; i < k; i++) {
            specs[i] = ung.newColumn(transType.getColPrefix() + i, DoubleCell.TYPE);
        }

        cr.append(new AbstractCellFactory(true, specs) {

            private final int[] m_colIdx = inSpec.columnsToIndices(usedColumnNames);

            @Override
            public DataCell[] getCells(final DataRow row) {
                return transMatrix.getProjection(row, m_colIdx, k, false);
            }
        });

        return cr;
    }

    /**
     * Creates the table spec for the output of a transformation like PCA or LDA.
     *
     * @param columnNames names of the input columns
     * @return table spec (first col for eigenvalues, others for components of eigenvectors)
     */
    public static DataTableSpec createDecompositionTableSpec(final String[] columnNames) {
        final DataColumnSpecCreator eigenvalueCol = new DataColumnSpecCreator("eigenvalue", DoubleCell.TYPE);

        final DataColumnSpec[] colsSpecs = new DataColumnSpec[columnNames.length + 1];
        colsSpecs[0] = eigenvalueCol.createSpec();

        for (int i = 1; i < colsSpecs.length; i++) {
            colsSpecs[i] = new DataColumnSpecCreator(columnNames[i - 1], DoubleCell.TYPE).createSpec();
        }
        return new DataTableSpec("spectral decomposition", colsSpecs);
    }

    /**
     * Creates the eigen decomposition table.
     *
     * @param exec the execution context
     * @param transMtx the {@code TransformationMatrix}
     * @param usedColumnNames the used column names
     * @return a table storing the eigen decomposition
     * @throws CanceledExecutionException - If the execution got canceled
     */
    public static BufferedDataTable createEigenDecompositionTable(final ExecutionContext exec,
        final TransformationMatrix transMtx, final String[] usedColumnNames) throws CanceledExecutionException {
        final DataTableSpec outSpec = createDecompositionTableSpec(usedColumnNames);
        final BufferedDataContainer result = exec.createDataContainer(outSpec);
        final RealVector sortedEigenValues = transMtx.getSortedEigenValues();
        final RealMatrix sortedEigenVectors = transMtx.getSortedEigenVectors();
        final int dim = sortedEigenValues.getDimension();
        for (int i = 0; i < dim; i++) {
            exec.checkCanceled();
            final int idx = i;
            exec.setProgress((double)i / dim, () -> "Adding Eigenvalue-Eigenvector pair " + idx + "/" + dim + ".");

            final DataCell[] values = new DataCell[sortedEigenVectors.getColumnDimension() + 1];
            values[0] = new DoubleCell(sortedEigenValues.getEntry(i));
            final double[] vector = sortedEigenVectors.getRow(i);
            for (int j = 0; j < vector.length; j++) {
                values[j + 1] = new DoubleCell(vector[j]);
            }
            result.addRowToTable(new DefaultRow(new RowKey(i + ". eigenvector"), values));
        }
        result.close();
        return result.getTable();
    }

    /**
     * Brings the given text into a html-format such that it can be shown e.g. as a warning message in the config
     * dialog.
     *
     * @param text the text
     * @return wrapped text
     */
    public static String wrapText(final String text) {
        return "<html>" + WordUtils.wrap(text, 75, "<br/>", true) + "</html>";
    }

    /**
     * Calculates the number of required dimension in order to satisfy the given information preservation constraint.
     *
     * @param transMtx the {@code TransformationMatrix}
     * @param infPreservation the information preservation value in range [1,100]
     * @return the number of dimension required in order to satisfy the required information preservation constraint
     */
    public static int calcDimForGivenInfPreservation(final TransformationMatrix transMtx,
        final double infPreservation) {
        return calcDimForGivenInfPreservation(transMtx.getSortedEigenValues(), transMtx.getMaxDimToReduceTo(),
            infPreservation);
    }

    /**
     * Calculates the number of required dimension in order to satisfy the given information preservation constraint.
     *
     * @param eigenValues the non-increasingly sorted eigenvalues
     * @param maxDimToReduceTo the maximum possible number of dimension to reduce to
     * @param infPreservation the information preservation value in range [0.01,100]
     * @return the number of dimension required in order to satisfy the required information preservation constraint
     */
    public static int calcDimForGivenInfPreservation(final RealVector eigenValues, final int maxDimToReduceTo,
        final double infPreservation) {
        if (infPreservation >= 100.0) {
            return maxDimToReduceTo;
        }
        double norm = 0;
        for (int i = 0; i < maxDimToReduceTo; i++) {
            norm += eigenValues.getEntry(i);
        }
        double sum = 0;
        final double cutOff = infPreservation / 100d;
        for (int i = 0; i < maxDimToReduceTo; i++) {
            sum += eigenValues.getEntry(i);
            if (sum / norm >= cutOff) { //NOSONAR eigenvalues > 0 and maxDimToReduceTo > 0
                return i + 1;
            }
        }
        return maxDimToReduceTo;
    }

}
