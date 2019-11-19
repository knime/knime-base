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
 *   15 Nov 2019 (Alexander): created
 */
package org.knime.base.predict;

/**
 * Options for PMML predictors, determining their output.
 * @author Alexander Fillbrunn, KNIME GmbH, Konstanz, Germany
 * @since 4.1
 */
public class PMMLClassificationPredictorOptions {
    private String m_customPredictionName;
    private boolean m_includeProbabilities;
    private String m_propColumnSuffix;

    /**
     * Creates a new instance of {@code PMMLMlpPredictorOptions}.
     * @param customPredictionName the name of the prediction column
     * or null if it should be inferred from the PMML
     * @param includeProbabilities whether to output class probabilities
     * @param propColumnSuffix column name suffix for output class probabilities
     */
    public PMMLClassificationPredictorOptions(final String customPredictionName,
        final boolean includeProbabilities, final String propColumnSuffix) {
        m_customPredictionName = customPredictionName;
        m_includeProbabilities = includeProbabilities;
        m_propColumnSuffix = propColumnSuffix;
    }

    /**
     * Creates a new instance of {@code PMMLMlpPredictorOptions}
     * where the prediction column name is inferred from the PMML.
     * @param includeProbabilities whether to output class probabilities
     * @param propColumnSuffix column name suffix for output class probabilities
     */
    public PMMLClassificationPredictorOptions(final boolean includeProbabilities, final String propColumnSuffix) {
        this(null, includeProbabilities, propColumnSuffix);
    }

    /**
     * Creates a new instance of {@code PMMLMlpPredictorOptions}, determining that probabilities
     * are not returned and the default name from the PMML is used for the prediction column.
     */
    public PMMLClassificationPredictorOptions() {
        this(null, false, null);
    }

    /**
     * @return whether the column with the prediction should have a custom name
     */
    public boolean hasCustomPredictionColumnName() {
        return m_customPredictionName != null;
    }

    /**
     * @return the name of the prediction column if
     * {@link #hasCustomPredictionName} is true
     */
    public String getPredictionColumnName() {
        return m_customPredictionName;
    }

    /**
     * @return whether to output class probabilities
     */
    public boolean includeClassProbabilities() {
        return m_includeProbabilities;
    }

    /**
     * @return column name suffix for output class probabilities
     */
    public String getPropColumnSuffix() {
        return m_propColumnSuffix;
    }
}
