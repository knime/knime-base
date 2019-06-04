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
 *   Jun 4, 2019 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.base.node.mine.regression.glmnet;

import org.knime.base.node.mine.regression.glmnet.data.Data;

/**
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
final class Snapshoter {

    private final Data m_data;

    Snapshoter(final Data data) {
        m_data = data;
    }

    LinearModel destandardize(final double[] coefficients) {
        final double[] denormalizedCoeffs = new double[coefficients.length];
        double intercept = m_data.getTargetMean();
        final double targetStdv = m_data.getTargetStdv();
        for (int i = 0; i < denormalizedCoeffs.length; i++) {
            final double beta = coefficients[i] / m_data.getStdv(i);
            denormalizedCoeffs[i] = beta * targetStdv;
            intercept -= beta * m_data.getMean(i);
        }

//        double intercept = m_data.getWeightedMeanTarget();
        return new LinearModel(intercept, denormalizedCoeffs);
    }



    double[] standardize(final LinearModel model) {
        double intercept = model.getIntercept();
        final double targetStdv = m_data.getTargetStdv();
        double[] coefficients = new double[model.getNumCoefficients()];
        for (int i = 0; i < coefficients.length; i++) {
            double stdv = m_data.getStdv(i);
            final double beta = model.getCoefficient(i) / targetStdv;
            coefficients[i] = beta * stdv;
            intercept += beta * m_data.getMean(i);
        }
        intercept = intercept - m_data.getTargetMean();
        System.out.println("Intercept after standardize: " + intercept);
        return coefficients;
    }

}
