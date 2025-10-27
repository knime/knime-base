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
 *   Aug 30, 2019 (benjamin): created
 */
package org.knime.base.node.preproc.correlation;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import org.knime.base.util.HalfDoubleMatrix;
import org.knime.base.util.HalfIntMatrix;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnProperties;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.MissingCell;
import org.knime.core.data.RowKey;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.renderer.DataValueRenderer;
import org.knime.core.data.renderer.DoubleValueRenderer.FullPrecisionRendererFactory;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.node.parameters.widget.choices.Label;

/**
 * @since 4.1
 * @author Benjamin Wilhelm, KNIME GmbH, Konstanz, Germany
 * @noreference This class is not intended to be referenced by clients (except for KNIME core plug-ins).
 */
public final class CorrelationUtils {

    /** The name of the column containing the correlation statistic */
    public static final String CORRELATION_VALUE_COL_NAME = "Correlation value";

    /** The name of the column containing the p-value */
    public static final String P_VALUE_COL_NAME = "p value";

    /** The name of the column containing the degrees of freedom */
    public static final String DEGREES_OF_FREEDOM_COL_NAME = "Degrees of freedom";

    /** The name of the column containing the first column name */
    public static final String FIRST_COL_NAME_COL_NAME = "First column name";

    /** The name of the column containing the second column name */
    public static final String SECOND_COL_NAME_COL_NAME = "Second column name";

    /** Full precision renderer for double values */
    private static final String FULL_PRECISION_RENDERER = new FullPrecisionRendererFactory().getDescription();

    private CorrelationUtils() {
        // Utility class
    }

    /**
     * @return the specs for a table containing correlation results
     */
    public static DataTableSpec createCorrelationOutputTableSpec() {
        // Column spec creators
        final DataColumnSpecCreator firstColSpecCreator =
            new DataColumnSpecCreator(FIRST_COL_NAME_COL_NAME, StringCell.TYPE);
        final DataColumnSpecCreator secondColSpecCreator =
            new DataColumnSpecCreator(SECOND_COL_NAME_COL_NAME, StringCell.TYPE);
        final DataColumnSpecCreator corrColSpecCreator =
            new DataColumnSpecCreator(CORRELATION_VALUE_COL_NAME, DoubleCell.TYPE);
        final DataColumnSpecCreator pValColSpecCreator = new DataColumnSpecCreator(P_VALUE_COL_NAME, DoubleCell.TYPE);
        final DataColumnSpecCreator dofColSpecCreator =
            new DataColumnSpecCreator(DEGREES_OF_FREEDOM_COL_NAME, IntCell.TYPE);

        // Set the full precision renderer for the p value column
        final DataColumnProperties fullPrecRendererProps = new DataColumnProperties(
            Collections.singletonMap(DataValueRenderer.PROPERTY_PREFERRED_RENDERER, FULL_PRECISION_RENDERER));
        pValColSpecCreator.setProperties(fullPrecRendererProps);
        corrColSpecCreator.setProperties(fullPrecRendererProps);

        return new DataTableSpec(firstColSpecCreator.createSpec(), secondColSpecCreator.createSpec(),
            corrColSpecCreator.createSpec(), pValColSpecCreator.createSpec(), dofColSpecCreator.createSpec());
    }

    /**
     * Create a table containing the correlation results.
     *
     * @param correlationResult the correlation results
     * @param includeNames the names of the included columns
     * @param compatibleColumnPairs the indices of compatible columns for each column (can be <code>null</code> if
     *            <code>columnPairFilter</code> is not <code>ColumnPairFilter.COMPATIBLE_PAIRS</code>)
     * @param columnPairFilter describes which column pairs should be included
     * @param exec a execution context to track the progress
     * @return the table containing the correlation results
     * @throws CanceledExecutionException if the execution was canceled
     */
    public static BufferedDataTable createCorrelationOutputTable(final CorrelationResult correlationResult,
        final String[] includeNames, final Collection<Integer>[] compatibleColumnPairs,
        final ColumnPairFilter columnPairFilter, final ExecutionContext exec) throws CanceledExecutionException {
        final DataTableSpec outSpec = createCorrelationOutputTableSpec();
        final BufferedDataContainer dataContainer = exec.createDataContainer(outSpec);

        final HalfDoubleMatrix corrMatrix = correlationResult.getCorrelationMatrix();
        final HalfDoubleMatrix pValMatrix = correlationResult.getpValMatrix();
        final HalfIntMatrix dofMatrix = correlationResult.getDegreesOfFreedomMatrix();

        // Fill the table
        int numInc = includeNames.length;
        int rowIndex = 0;
        final double rowCount = numInc * (numInc - 1) / 2.;
        for (int i = 0; i < numInc; i++) {
            for (int j = i + 1; j < numInc; j++) {
                // Correlation and p-value
                final double corr = corrMatrix.get(i, j);
                final double pVal = pValMatrix.get(i, j);

                // Skip column pair if it is not valid (and should therefore not be included)
                if ((ColumnPairFilter.VALID_CORRELATION.equals(columnPairFilter) //
                    && Double.isNaN(corr)) //
                    || (ColumnPairFilter.COMPATIBLE_PAIRS.equals(columnPairFilter) //
                        && !compatibleColumnPairs[i].contains(j))) {
                    continue;
                }

                // Cells
                final DataCell corrCell =
                    Double.isNaN(corr) ? new MissingCell("Correlation could not be computed.") : new DoubleCell(corr);
                final DataCell pValCell =
                    Double.isNaN(pVal) ? new MissingCell("P-value could not be computed.") : new DoubleCell(pVal);
                final DataCell dofCell = new IntCell(dofMatrix.get(i, j));

                // Column names
                final String secondCol = includeNames[j];
                final String firstCol = includeNames[i];
                final StringCell firstColCell = new StringCell(firstCol);
                final StringCell secondColCell = new StringCell(secondCol);

                // Assemble row
                final RowKey rowKey = new RowKey("Row" + rowIndex);
                final DefaultRow row = new DefaultRow(rowKey, firstColCell, secondColCell, corrCell, pValCell, dofCell);

                // Add row and update progress
                exec.checkCanceled();
                dataContainer.addRowToTable(row);
                exec.setProgress(++rowIndex / rowCount);
            }
        }
        exec.setProgress(1);
        dataContainer.close();
        return dataContainer.getTable();
    }

    /**
     * A result of a correlation computation.
     *
     * @noreference This class is not intended to be referenced by clients (except for KNIME core plug-ins).
     */
    public static class CorrelationResult {

        private final HalfDoubleMatrix m_correlationMatrix;

        private final HalfDoubleMatrix m_pValMatrix;

        private final HalfIntMatrix m_degreesOfFreedomMatrix;

        /**
         * Create a new correlation result.
         *
         * @param correlationMatrix the matrix of correlation values
         * @param pValMatrix the matrix of p values
         * @param degreesOfFreedomMatrix the matrix of degree of freedom values
         */
        public CorrelationResult(final HalfDoubleMatrix correlationMatrix, final HalfDoubleMatrix pValMatrix,
            final HalfIntMatrix degreesOfFreedomMatrix) {
            m_correlationMatrix = correlationMatrix;
            m_pValMatrix = pValMatrix;
            m_degreesOfFreedomMatrix = degreesOfFreedomMatrix;
        }

        /**
         * @return the correlation matrix
         */
        public HalfDoubleMatrix getCorrelationMatrix() {
            return m_correlationMatrix;
        }

        /**
         * @return the p value matrix
         */
        public HalfDoubleMatrix getpValMatrix() {
            return m_pValMatrix;
        }

        /**
         * @return the degrees of freedom matrix
         */
        public HalfIntMatrix getDegreesOfFreedomMatrix() {
            return m_degreesOfFreedomMatrix;
        }
    }

    /**
     * Describes which pairs of columns should be included in a correlation output table.
     */
    public enum ColumnPairFilter {

            /** All column pairs */
            @Label("All column pairs")
            ALL("Include all column pairs"),

            /** Only pairs of compatible columns */
            @Label("Only column pairs of compatible columns")
            COMPATIBLE_PAIRS("Include only column pairs of compatible columns"),

            /** Only pairs with valid correlation */
            @Label("Only column pairs with a valid correlation")
            VALID_CORRELATION("Include only column pairs with a valid correlation");

        private final String m_desc;

        private ColumnPairFilter(final String desc) {
            m_desc = desc;
        }

        @Override
        public String toString() {
            return m_desc;
        }

        /**
         * @return the names of all possible values (in the same order as {@link #descriptions()})
         */
        public static String[] names() {
            return Arrays.stream(ColumnPairFilter.values()).map(ColumnPairFilter::name).toArray(String[]::new);
        }

        /**
         * @return the descriptions of all possible values (in the same order as {@link #names()})
         */
        public static String[] descriptions() {
            return Arrays.stream(ColumnPairFilter.values()).map(ColumnPairFilter::toString).toArray(String[]::new);
        }
    }
}
