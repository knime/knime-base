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
 *   26.05.2019 (Adrian): created
 */
package org.knime.base.node.mine.regression.glmnet;

import org.knime.base.node.mine.regression.glmnet.cycle.FeatureCycleFactories;
import org.knime.base.node.mine.regression.glmnet.data.Data;
import org.knime.base.node.mine.regression.glmnet.lambda.LambdaSequence;
import org.knime.base.node.mine.regression.glmnet.lambda.LambdaSequences;
import org.knime.core.node.util.CheckUtils;

/**
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
public final class ElasticNetBuilder {

    private final Data m_data;

    private double m_lambdaEpsilon = 0.001;

    private int m_lambdaSteps = 100;

    private double m_alpha = 0.5;

    private double m_glmnetEpsilon = 1e-4;

    private double m_maxFeaturesEpsilon = 1e-5;

    private int m_maxIterationsPerLambda = 1000;

    private int m_maxActiveFeatures = Integer.MAX_VALUE;

    private int m_maxBacktrackingSteps = 100;

    private LambdaSequence m_lambdas = null;

    /**
     * @param data the data to learn on
     */
    public ElasticNetBuilder(final Data data) {
        m_data = data;
    }

    /**
     * @return builds the ElasticNet from the configuration represented by the state of this builder
     */
    public ElasticNet build() {
        if (m_lambdas == null) {
            m_lambdas = buildLambdaSequence();
        }
        final GlmNet glmnet = new GlmNet(m_data, NaiveUpdater.INSTANCE, m_alpha, m_glmnetEpsilon, FeatureCycleFactories
            .createRoundRobinFeaturCycleFactory(m_data.getNumFeatures(), m_maxIterationsPerLambda));
        return new ElasticNet(glmnet, m_lambdas, m_maxActiveFeatures, m_maxFeaturesEpsilon, m_maxBacktrackingSteps);
    }

    private LambdaSequence buildLambdaSequence() {
        return LambdaSequences.epsilonLogScale(m_lambdaEpsilon, m_lambdaSteps, m_alpha, m_data);
    }

    /**
     * @param lambdaEpsilon the lambdaEpsilon to set
     */
    public void setLambdaEpsilon(final double lambdaEpsilon) {
        CheckUtils.checkArgument(lambdaEpsilon > 0, "Epsilon must be greater than 0.");
        m_lambdaEpsilon = lambdaEpsilon;
    }

    /**
     * @param lambdaSteps the lambdaSteps to set
     */
    public void setLambdaSteps(final int lambdaSteps) {
        CheckUtils.checkArgument(lambdaSteps > 0, "At least one lambda step must be performed.");
        m_lambdaSteps = lambdaSteps;
    }

    /**
     * @param alpha the alpha to set
     */
    public void setAlpha(final double alpha) {
        CheckUtils.checkArgument(alpha >= 0 && alpha <= 1, "Alpha must be in [0, 1].");
        m_alpha = alpha;
    }

    /**
     * @param glmnetEpsilon the glmnetEpsilon to set
     */
    public void setBetaChangeEpsilon(final double glmnetEpsilon) {
        CheckUtils.checkArgument(glmnetEpsilon >= 0, "The beta changed epsilon must be non-negative.");
        m_glmnetEpsilon = glmnetEpsilon;
    }

    /**
     * @param maxFeaturesEpsilon the maxFeaturesEpsilon to set
     */
    public void setMaxActiveFeaturesEpsilon(final double maxFeaturesEpsilon) {
        m_maxFeaturesEpsilon = maxFeaturesEpsilon;
    }

    /**
     * @param maxIterations the maxIterations to set
     */
    public void setMaxIterationsPerLambda(final int maxIterations) {
        CheckUtils.checkArgument(maxIterations > 0, "maxIterations per lambda must be at least 1.");
        m_maxIterationsPerLambda = maxIterations;
    }

    /**
     * @param maxActiveFeatures the maxActiveFeatures to set
     */
    public void setMaxActiveFeatures(final int maxActiveFeatures) {
        CheckUtils.checkArgument(maxActiveFeatures > 0, "The maximal number of active features must be at least 1.");
        m_maxActiveFeatures = maxActiveFeatures;
    }

    /**
     * @param maxBacktrackingSteps the maxBacktrackingSteps to set
     */
    public void setMaxBacktrackingSteps(final int maxBacktrackingSteps) {
        CheckUtils.checkArgument(maxBacktrackingSteps > 0, "The number of backtracking steps must be larger than 0.");
        m_maxBacktrackingSteps = maxBacktrackingSteps;
    }

    /**
     * @param lambdas the lambdas to set
     */
    public void setLambdas(final LambdaSequence lambdas) {
        CheckUtils.checkArgumentNotNull(lambdas);
        m_lambdas = lambdas;
    }

}
