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

import java.util.Collections;

import org.knime.base.util.HalfDoubleMatrix;
import org.knime.base.util.HalfIntMatrix;
import org.knime.core.data.DataColumnProperties;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowKey;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.renderer.DataValueRenderer;
import org.knime.core.data.renderer.DoubleValueRenderer.FullPrecisionRendererFactory;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;

/**
 * @since 4.1
 * @author Benjamin Wilhelm, KNIME GmbH, Konstanz, Germany
 */
public final class CorrelationUtils {

    private static final String CORRELATION_VALUE_COL_NAME = "Correlation value";

    private static final String P_VALUE_COL_NAME = "p value";

    private static final String DEGREES_OF_FREEDOM_COL_NAME = "Degrees of freedom";

    /** Full precision renderer for double values */
    private static final String FULL_PRECISION_RENDERER = new FullPrecisionRendererFactory().getDescription();

    private CorrelationUtils() {
        // Utility class
    }

    public static DataTableSpec createCorrelationOutputTableSpec() {
        // Column spec creators
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

        return new DataTableSpec(corrColSpecCreator.createSpec(), pValColSpecCreator.createSpec(),
            dofColSpecCreator.createSpec());
    }

    public static BufferedDataTable createCorrelationOutputTable(final CorrelationResult correlationResult,
        final String[] includeNames, final ExecutionContext exec) throws CanceledExecutionException {
        final DataTableSpec outSpec = createCorrelationOutputTableSpec();
        final BufferedDataContainer dataContainer = exec.createDataContainer(outSpec);

        final HalfDoubleMatrix corrMatrix = correlationResult.getCorrelationMatrix();
        final HalfDoubleMatrix pValMatrix = correlationResult.getpValMatrix();
        final HalfIntMatrix dofMatrix = correlationResult.getDegreesOfFreedomMatrix();

        // Fill the table
        int numInc = includeNames.length;
        int rowIndex = 0;
        final double rowCount = numInc * (numInc - 1) / 2;
        for (int i = 0; i < numInc; i++) {
            for (int j = i + 1; j < numInc; j++) {
                final DoubleCell corr = new DoubleCell(corrMatrix.get(i, j));
                final DoubleCell pVal = new DoubleCell(pValMatrix.get(i, j));
                final IntCell dof = new IntCell(dofMatrix.get(i, j));
                final RowKey rowKey = new RowKey(getRowKey(includeNames[i], includeNames[j]));
                final DefaultRow row = new DefaultRow(rowKey, corr, pVal, dof);
                exec.checkCanceled();
                dataContainer.addRowToTable(row);
                exec.setProgress(++rowIndex / rowCount);
            }
        }
        dataContainer.close();
        return dataContainer.getTable();
    }

    private static String getRowKey(final String columnNameA, final String columnNameB) {
        return columnNameA + "_" + columnNameB;
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
}
