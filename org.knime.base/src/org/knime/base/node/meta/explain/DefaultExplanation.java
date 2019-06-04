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
 *   Apr 25, 2019 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.base.node.meta.explain;

/**
 * The default implementation of {@link Explanation}. The class itself is immutable and can only be created via the
 * provided builder class {@link DefaultExplanationBuilder}.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
public final class DefaultExplanation extends AbstractExplanation {

    private final double[] m_actualPredictions;

    private final double[] m_deviationFromMeanPredictions;

    /**
     * Instances of this class should only be instantiated via {@link DefaultExplanationBuilder}.
     *
     * @param roiKey the identifier of the instance for which to explain the prediction
     * @param explanationValues
     * @param numTargets
     * @param numFeatures
     */
    private DefaultExplanation(final String roiKey, final Matrix explanationValues, final double[] actualPredictions,
        final double[] deviationFromMeanPredictions, final int numTargets, final int numFeatures) {
        super(roiKey, explanationValues, numTargets, numFeatures);
        m_actualPredictions = actualPredictions;
        m_deviationFromMeanPredictions = deviationFromMeanPredictions;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getActualPrediction(final int target) {
        return m_actualPredictions[target];
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getDeviationFromMeanPrediction(final int target) {
        return m_deviationFromMeanPredictions[target];
    }

    /**
     * Builder class for {@link DefaultExplanation}. Allows to incrementally add the explanation values for different
     * target feature combinations. Instances of this class can build only a single DefaultExplanation and will throw
     * exceptions if any of their methods are called after the build has occurred.
     *
     * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
     */
    public static final class DefaultExplanationBuilder extends AbstractExplanationBuilder {

        private final double[] m_actualPredictions;

        private final double[] m_deviationFromMeanPredictions;

        /**
         * Creates a mutable {@link DefaultExplanationBuilder} instance that can be used to create a single
         * {@link DefaultExplanation}.
         *
         * @param roiKey the key of the row of interest
         * @param numTargets the number of targets (e.g. the number of class probabilities) to explain
         * @param numFeatures the number of features the explained model uses
         */
        public DefaultExplanationBuilder(final String roiKey, final int numTargets, final int numFeatures) {
            super(roiKey, numTargets, numFeatures);
            m_actualPredictions = new double[numTargets];
            m_deviationFromMeanPredictions = new double[numTargets];
        }

        /**
         * Builds the DefaultExplanation using the explanationValues provided via the setExplanationValue method so far.
         * This method can be only called once!
         *
         * @return the DefaultExplanation reflecting the state of the Builder at the point of method execution
         * @throws IllegalStateException if the method is called twice
         */
        public DefaultExplanation build() {
            checkBuilt();
            return new DefaultExplanation(getRoiKey(), getExplanationValues(), m_actualPredictions,
                m_deviationFromMeanPredictions, getNumTargets(), getNumFeatures());
        }


        /**
         * @param target the target the predictions correspond to
         * @param actualPrediction prediction of the current roi
         * @param meanPrediction the mean prediction over the sampling set
         */
        public void setActualPredictionAndDeviation(final int target, final double actualPrediction,
            final double meanPrediction) {
            m_actualPredictions[target] = actualPrediction;
            m_deviationFromMeanPredictions[target] = actualPrediction - meanPrediction;
        }
    }

}
