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
 *   Apr 26, 2019 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.base.node.meta.explain;

import java.util.Arrays;
import java.util.Collection;
import java.util.function.Consumer;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataTableSpecCreator;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.util.CheckUtils;

/**
 * Converts an {@link Explanation} into multiple {@link DataRow}s where each row provides the explanation values for a
 * single target (e.g. class probability). Assumes that the first column in the output is the
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
public final class ExplanationToMultiRowConverter implements ExplanationToDataCellsConverter {

    private final StringCell[] m_targetNames;

    private final boolean m_includeActualPredictionAndDeviation;

    /**
     * @param targetNames the names of the different targets (must match the number of targets)
     */
    public ExplanationToMultiRowConverter(final String[] targetNames,
        final boolean includeActualPredictionAndDeviation) {
        m_targetNames = Arrays.stream(targetNames).map(StringCell::new).toArray(StringCell[]::new);
        m_includeActualPredictionAndDeviation = includeActualPredictionAndDeviation;
    }

    public ExplanationToMultiRowConverter(final String[] targetNames) {
        this(targetNames, true);
    }

    /**
     * @param targetNames the names of the different targets (must match the number of targets)
     * @param includeActualPredictionAndDeviation
     */
    public ExplanationToMultiRowConverter(final StringCell[] targetNames,
        final boolean includeActualPredictionAndDeviation) {
        m_targetNames = targetNames.clone();
        m_includeActualPredictionAndDeviation = includeActualPredictionAndDeviation;
    }

    /**
     * @param targetNames
     */
    public ExplanationToMultiRowConverter(final StringCell[] targetNames) {
        this(targetNames, true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void convertAndWrite(final Explanation explanation, final Consumer<DataCell[]> consumer) {
        final int numTargets = explanation.getNumberOfTargets();
        CheckUtils.checkArgument(numTargets == m_targetNames.length, "Expected %s targets but the explanation has %s.",
            m_targetNames.length, numTargets);
        final int specialCols = getNumSpecialCols();
        final int numFeatures = explanation.getNumberOfFeatures();
        for (int i = 0; i < numTargets; i++) {
            final DataCell[] cells = new DataCell[numFeatures + specialCols];
            cells[0] = new StringCell(explanation.getRoiKey());
            cells[1] = m_targetNames[i];
            if (m_includeActualPredictionAndDeviation) {
                cells[2] = new DoubleCell(explanation.getActualPrediction(i));
                cells[3] = new DoubleCell(explanation.getDeviationFromMeanPrediction(i));
            }
            for (int j = 0; j < numFeatures; j++) {
                cells[j + specialCols] = new DoubleCell(explanation.getExplanationValue(i, j));
            }
            consumer.accept(cells);
        }
    }

    private int getNumSpecialCols() {
        return m_includeActualPredictionAndDeviation ? 4 : 2;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DataTableSpec createSpec(final Collection<String> featureNames) {
        final DataTableSpecCreator specCreator = new DataTableSpecCreator();
        specCreator.addColumns(new DataColumnSpecCreator("RowId", StringCell.TYPE).createSpec());
        specCreator.addColumns(new DataColumnSpecCreator("Target", StringCell.TYPE).createSpec());
        if (m_includeActualPredictionAndDeviation) {
            specCreator.addColumns(new DataColumnSpecCreator("Actual prediction", DoubleCell.TYPE).createSpec());
            specCreator
                .addColumns(new DataColumnSpecCreator("Deviation from mean prediction", DoubleCell.TYPE).createSpec());
        }
        for (final String featureName : featureNames) {
            specCreator.addColumns(new DataColumnSpecCreator(featureName, DoubleCell.TYPE).createSpec());
        }
        return specCreator.createSpec();
    }

}
