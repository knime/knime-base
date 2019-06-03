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
 *   May 31, 2019 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.base.node.mine.manifold.tsne;

import java.util.ArrayList;
import java.util.List;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.MissingValue;
import org.knime.core.data.MissingValueException;
import org.knime.core.data.container.CloseableRowIterator;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.util.CheckUtils;

/**
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
final class TsneData {

    private final List<Integer> m_missingIndices = new ArrayList<>();

    private final double[][] m_data;

    private final boolean m_failOnMissingValues;

    TsneData(final BufferedDataTable featureTable, final boolean failOnMissingValues) {
        m_failOnMissingValues = failOnMissingValues;
        m_data = readIntoDoubleArray(featureTable);
    }

    double[][] getData() {
        return m_data;
    }

    List<Integer> getMissingIndices() {
        return m_missingIndices;
    }

    private double[][] readIntoDoubleArray(final BufferedDataTable features) {
        final int nCols = features.getDataTableSpec().getNumColumns();
        CheckUtils.checkArgument(features.size() <= Integer.MAX_VALUE,
            "The input table can only have up to Integer.MAX_VALUE rows.");
        final int nRows = (int)features.size();
        final List<double[]> data = new ArrayList<>(nRows);

        try (final CloseableRowIterator rowIter = features.iterator()) {
            for (int i = 0; i < nRows; i++) {
                final DataRow row = rowIter.next();
                final double[] vector = readRow(nCols, i, row);
                if (vector != null) {
                    data.add(vector);
                }
            }
            assert !rowIter.hasNext();
        }
        return data.toArray(new double[data.size()][]);
    }

    private double[] readRow(final int nCols, final int i, final DataRow row) {
        final double[] vector = new double[nCols];
        for (int j = 0; j < nCols; j++) {
            final DataCell cell = row.getCell(j);
            if (cell.isMissing()) {
                handleMissingValue(row, i, j);
                // we can't read the row so we return null to indicate that something went wrong
                return null;
            }
            // we currently only support DoubleValues as features, hence this cast should be safe
            vector[j] = ((DoubleValue)row.getCell(j)).getDoubleValue();
        }
        return vector;
    }

    private void handleMissingValue(final DataRow row, final int rowIdx, final int colIdx) {
        if (m_failOnMissingValues) {
            throw new MissingValueException((MissingValue)row.getCell(colIdx), "Missing value detected in row " + row);
        } else {
            // remember the missing idx
            m_missingIndices.add(rowIdx);
        }
    }
}
