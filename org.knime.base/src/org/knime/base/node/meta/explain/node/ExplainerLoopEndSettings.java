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
 *   Apr 15, 2019 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.base.node.meta.explain.node;

import java.util.Set;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.util.filter.column.DataColumnSpecFilterConfiguration;

/**
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
public class ExplainerLoopEndSettings implements Settings {

    /**
     *
     */
    private static final String CFG_MAX_ACTIVE_FEATURES = "maxActiveFeatures";

    /**
     *
     */
    private static final String CFG_REGULARIZE_EXPLANATIONS = "regularizeExplanations";

    /**
     *
     */
    private static final String CFG_ALPHA = "alpha";

    /**
     *
     */
    private static final double DEF_ALPHA = 0.5;

    /**
     *
     */
    private static final boolean DEF_LIMIT_ACTIVE_FEATURES = false;

    /**
     *
     */
    private static final int DEF_MAX_ACTIVE_FEATURES = 10;

    /**
     *
     */
    private static final String CFG_PREDICTION_COLUMN_SELECTION_MODE = "predictionColumnSelectionMode";

    /**
     * Enum that indicates whether prediction columns should be selected automatically or are specified by the user
     * manually.
     *
     * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
     */
    public enum PredictionColumnSelectionMode {
            /**
             * Any appended numerical non-feature columns is considered as prediction column.
             */
            AUTOMATIC,
            /**
             * Prediction columns are manually supplied by the user.
             */
            MANUAL;
    }

    // When adding new options be sure to add them to the load and save methods

    private static final String CFG_PREDICTION_COLUMNS = "predictionColumns";

    private static final String CFG_USE_ELEMENT_NAMES = "useElementNames";

    private boolean m_useElementNames = true;

    private DataColumnSpecFilterConfiguration m_predictionCols = createPredictionCols();

    private PredictionColumnSelectionMode m_predictionColumnSelectionMode = PredictionColumnSelectionMode.AUTOMATIC;

    private int m_maxActiveFeatures = 10;

    private boolean m_regularizeExplanations = false;

    private double m_alpha = 0.5;

    private final Set<ExplainerLoopEndSettingsOptions> m_options;

    /**
     * @param options
     *
     */
    public ExplainerLoopEndSettings(final Set<ExplainerLoopEndSettingsOptions> options) {
        m_options = options;
    }

    @Override
    public void loadSettingsInDialog(final NodeSettingsRO settings, final DataTableSpec[] inSpecs) {
        m_predictionCols.loadConfigurationInDialog(settings, inSpecs[0]);
        if (m_options.contains(ExplainerLoopEndSettingsOptions.UseElementNames)) {
            m_useElementNames = settings.getBoolean(CFG_USE_ELEMENT_NAMES, false);
        }
        m_predictionColumnSelectionMode = PredictionColumnSelectionMode.valueOf(
            settings.getString(CFG_PREDICTION_COLUMN_SELECTION_MODE, PredictionColumnSelectionMode.AUTOMATIC.name()));
        if (m_options.contains(ExplainerLoopEndSettingsOptions.Regularization)) {
            m_maxActiveFeatures = settings.getInt(CFG_MAX_ACTIVE_FEATURES, DEF_MAX_ACTIVE_FEATURES);
            m_regularizeExplanations = settings.getBoolean(CFG_REGULARIZE_EXPLANATIONS, DEF_LIMIT_ACTIVE_FEATURES);
            m_alpha = settings.getDouble(CFG_ALPHA, DEF_ALPHA);
        }
    }

    @Override
    public void loadSettingsInModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_predictionCols.loadConfigurationInModel(settings);
        if (m_options.contains(ExplainerLoopEndSettingsOptions.UseElementNames)) {
            m_useElementNames = settings.getBoolean(CFG_USE_ELEMENT_NAMES);
        }
        m_predictionColumnSelectionMode =
            PredictionColumnSelectionMode.valueOf(settings.getString(CFG_PREDICTION_COLUMN_SELECTION_MODE));
        if (m_options.contains(ExplainerLoopEndSettingsOptions.Regularization)) {
            m_maxActiveFeatures = settings.getInt(CFG_MAX_ACTIVE_FEATURES);
            m_regularizeExplanations = settings.getBoolean(CFG_REGULARIZE_EXPLANATIONS);
            m_alpha = settings.getDouble(CFG_ALPHA);
        }
    }

    @Override
    public void saveSettings(final NodeSettingsWO settings) {
        m_predictionCols.saveConfiguration(settings);
        if (m_options.contains(ExplainerLoopEndSettingsOptions.UseElementNames)) {
            settings.addBoolean(CFG_USE_ELEMENT_NAMES, m_useElementNames);
        }
        settings.addString(CFG_PREDICTION_COLUMN_SELECTION_MODE, m_predictionColumnSelectionMode.name());
        if (m_options.contains(ExplainerLoopEndSettingsOptions.Regularization)) {
            settings.addInt(CFG_MAX_ACTIVE_FEATURES, m_maxActiveFeatures);
            settings.addBoolean(CFG_REGULARIZE_EXPLANATIONS, m_regularizeExplanations);
            settings.addDouble(CFG_ALPHA, m_alpha);
        }
    }

    /**
     * @return the predictionColumnSelectionMode
     */
    public PredictionColumnSelectionMode getPredictionColumnSelectionMode() {
        return m_predictionColumnSelectionMode;
    }

    /**
     * @param predictionColumnSelectionMode the predictionColumnSelectionMode to set
     */
    void setPredictionColumnSelectionMode(final PredictionColumnSelectionMode predictionColumnSelectionMode) {
        m_predictionColumnSelectionMode = predictionColumnSelectionMode;
    }

    /**
     * @return the featureCols
     */
    public DataColumnSpecFilterConfiguration getPredictionCols() {
        return m_predictionCols;
    }

    /**
     * @param featureCols the featureCols to set
     */
    void setPredictionCols(final DataColumnSpecFilterConfiguration featureCols) {
        m_predictionCols = featureCols;
    }

    private static DataColumnSpecFilterConfiguration createPredictionCols() {
        return new DataColumnSpecFilterConfiguration(CFG_PREDICTION_COLUMNS);
    }

    /**
     * @return the dontUseElementNames
     */
    public boolean isUseElementNames() {
        return m_useElementNames;
    }

    /**
     * @param useElementNames
     */
    void setUseElementNames(final boolean useElementNames) {
        m_useElementNames = useElementNames;
    }

    /**
     * @return the maxActiveFeatures
     */
    public int getMaxActiveFeatures() {
        return m_maxActiveFeatures;
    }

    /**
     * @param maxActiveFeatures the maxActiveFeatures to set
     */
    void setMaxActiveFeatures(final int maxActiveFeatures) {
        m_maxActiveFeatures = maxActiveFeatures;
    }

    /**
     * @return the limitActiveFeatures
     */
    public boolean isRegularizeExplanations() {
        return m_regularizeExplanations;
    }

    /**
     * @param limitActiveFeatures the limitActiveFeatures to set
     */
    void setRegularizeExplanations(final boolean limitActiveFeatures) {
        m_regularizeExplanations = limitActiveFeatures;
    }

    /**
     * @return the alpha
     */
    public double getAlpha() {
        return m_alpha;
    }

    /**
     * @param alpha the alpha to set
     */
    void setAlpha(final double alpha) {
        m_alpha = alpha;
    }

}
