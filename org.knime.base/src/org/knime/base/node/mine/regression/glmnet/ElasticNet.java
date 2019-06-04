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
 *   31.03.2019 (Adrian): created
 */
package org.knime.base.node.mine.regression.glmnet;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import org.knime.base.node.mine.regression.glmnet.lambda.LambdaSequence;
import org.knime.core.node.util.CheckUtils;

/**
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
public final class ElasticNet {

    private final GlmNet m_glmnet;

    private final LambdaSequence m_lambdas;

    private final List<LinearModel> m_models;

    private final int m_maxActiveFeatures;

    private final int m_maxBacktrackingSteps;

    /**
     * Used to check if a feature is active or not (might be extracted into separate class in the future)
     */
    private final double m_epsilon;

    /**
     *
     */
    ElasticNet(final GlmNet glmnet, final LambdaSequence lambdas, final int maxActiveFeatures, final double epsilon,
        final int maxBacktrackingSteps) {
        m_glmnet = glmnet;
        m_lambdas = lambdas;
        m_models = new ArrayList<>(m_lambdas.length());
        m_maxActiveFeatures = maxActiveFeatures;
        m_epsilon = epsilon;
        m_maxBacktrackingSteps = maxBacktrackingSteps;
    }

    /**
     * Fits the regularization path
     */
    public void fit() {
        boolean belowMaxActiveFeatures = true;
        for (int i = 0; i < m_lambdas.length() && belowMaxActiveFeatures; i++) {
            final double lambda = m_lambdas.get(i);
            final LinearModel model = m_glmnet.fit(lambda);
            final long numActive = getNumActiveFeatures(model);
            if (numActive >= m_maxActiveFeatures) {
                // TODO implement backtracking to ensure that exactly maxActiveFeatures are active
                m_models.add(model);
                belowMaxActiveFeatures = false;
            } else {
                m_models.add(model);
            }
        }
    }

    private LinearModel backtrack(final int lambdaStep) {
        CheckUtils.checkState(lambdaStep > 0,
            "Exceeded the maximum number of features in the first step. This indicates an invalid configuration.");
        double lower = m_lambdas.get(lambdaStep);
        double upper = m_lambdas.get(lambdaStep - 1);
        m_glmnet.setModel(getLastModel());
        LinearModel backtracked = null;
        for (int i = 0; i < m_maxBacktrackingSteps; i++) {
            final double newLambda = m_lambdas.interpolate(lower, upper);
            backtracked = m_glmnet.fit(newLambda);
            final long numActive = getNumActiveFeatures(backtracked);
            if (numActive == m_maxActiveFeatures) {
                break;
            } else if (numActive > m_maxActiveFeatures) {
                m_glmnet.setModel(getLastModel());
                lower = newLambda;
            } else {
                upper = newLambda;
            }
        }
        assert backtracked != null : "backtracked can only be null if m_maxBacktracking < 1.";
        return backtracked;
    }

    private static long getNumActiveFeatures(final LinearModel model) {
        return IntStream.range(0, model.getNumCoefficients()).filter(i -> Math.abs(model.getCoefficient(i)) > 0)
            .count();
    }

    /**
     * @return the model for the last lambda
     */
    public LinearModel getLastModel() {
        CheckUtils.checkState(!m_models.isEmpty(),
            "The fit method has to be called before the final model can be fetched.");
        return m_models.get(m_models.size() - 1);
    }

    /**
     * @return the entire regularization path
     */
    public RegularizationPath<LinearModel> getRegularizationPath() {
        return new RegularizationPath<>(m_models, m_lambdas);
    }

}
