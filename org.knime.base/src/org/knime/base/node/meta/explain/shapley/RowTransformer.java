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
 *   Apr 12, 2019 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.base.node.meta.explain.shapley;

import org.knime.base.node.meta.explain.feature.FeatureVector;
import org.knime.base.node.meta.explain.feature.KnimeFeatureVectorIterator;
import org.knime.base.node.meta.explain.shapley.ShapleyValuesKeys.SVKeyGen;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;

/**
 * Produces the table output of the Shapley Values Loop Start node.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
final class RowTransformer {

    private final KnimeFeatureVectorIterator m_iter;

    private final ShapleyValues m_algo;

    private final DataTableSpec m_featureTableSpec;

    private final int m_chunkSize;

    private final SVKeyGen m_keyGen = ShapleyValuesKeys.createGenerator();

    public RowTransformer(final KnimeFeatureVectorIterator iter, final DataTableSpec featureTableSpec,
        final ShapleyValues algo, final int chunkSize) {
        m_iter = iter;
        m_algo = algo;
        m_chunkSize = chunkSize;
        m_featureTableSpec = featureTableSpec;
    }

    public boolean hasNext() {
        return m_iter.hasNext();
    }

    /**
     * @param exec {@link ExecutionContext} for progress report and table creation
     * @return the table for the next loop iteration
     * @throws CanceledExecutionException if the execution is canceled
     */
    public BufferedDataTable next(final ExecutionContext exec) throws CanceledExecutionException {
        final double total = m_chunkSize;
        final BufferedDataContainer container = exec.createDataContainer(m_featureTableSpec);
        for (int i = 0; i < m_chunkSize && m_iter.hasNext(); i++) {
            exec.checkCanceled();
            createTransformedRowsForNextInputRow(container);
            exec.setProgress(i / total);
        }
        container.close();
        return container.getTable();
    }

    private void createTransformedRowsForNextInputRow(final BufferedDataContainer container) {
        final FeatureVector x = m_iter.next();
        // the first row in the batch is the original row in order to get the actual predictions
        container.addRowToTable(x.get());
        for (int foi = 0; foi < x.size(); foi++) {
            m_algo.prepare(x, foi, m_keyGen, p -> container.addRowToTable(p.get()));
        }
    }

    public void close() {
        m_iter.close();
    }

}