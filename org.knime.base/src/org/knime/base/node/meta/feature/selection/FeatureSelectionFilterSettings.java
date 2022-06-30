/*
 * ------------------------------------------------------------------------
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
 *   27.02.2008 (thor): created
 */
package org.knime.base.node.meta.feature.selection;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.knime.base.node.meta.feature.backwardelim.BWElimModel;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.util.Pair;

/**
 * This class contains the settings for the feature selection filter node.
 *
 * @author Thorsten Meinl, University of Konstanz
 */
public class FeatureSelectionFilterSettings {
    private static final String SELECTED_FEATURES_KEY = "selectedFeatures";

    private static final String NR_OF_FEATURES_KEY = "nrOfFeatures";

    private static final String INCLUDE_TARGET_KEY = "includeTargetColumn";

    private static final String THRESHOLD_MODE_KEY = "thresholdMode";

    private static final String ERROR_THRESHOLD_KEY = "errorThreshold";

    private static final String BEST_SCORE_MODE_KEY = "bestScoreMode";

    private int m_nrOfFeatures;

    private Collection<String> m_selectedFeatures;

    private boolean m_includeTargetColumn;

    private double m_errorThreshold;

    private boolean m_thresholdMode;

    private boolean m_bestScoreMode;

    /**
     * Sets the error threshold for automatic feature selection.
     *
     * @param d the threshold, usually between 0 and 1
     */
    public void errorThreshold(final double d) {
        m_errorThreshold = d;
    }

    /**
     * Returns the error threshold for automatic feature selection.
     *
     * @return the threshold, usually between 0 and 1
     */
    public double errorThreshold() {
        return m_errorThreshold;
    }

    /**
     * Sets if the features are selected manually or dynamically by an error threshold.
     *
     * @param b <code>true</code> if an error threshold should be used, <code>false</code> if the features are selected
     *            manually by level
     */
    public void thresholdMode(final boolean b) {
        m_thresholdMode = b;
    }

    /**
     * Returns if the features are selected manually or dynamically by an error threshold.
     *
     * @return <code>true</code> if an error threshold should be used, <code>false</code> if the features are selected
     *         manually by level
     */
    public boolean thresholdMode() {
        return m_thresholdMode;
    }

    /**
     * Sets if the selected features are the best score features or not.
     *
     * @param b {@code true} if the best score option is chosen, {@code false} otherwise.
     */
    public void bestScoreMode(final boolean b) {
        m_bestScoreMode = b;
    }

    /**
     * Returns if the feature selected is the best score feature.
     *
     * @return {@code true} if the best score feature selection is chosen or {@code false} otherwise.
     */
    public boolean bestScoreMode() {
        return m_bestScoreMode;
    }

    /**
     * Returns if the target column should be included in the output table.
     *
     * @return <code>true</code> if it should be included, <code>false</code> otherwise
     */
    public boolean includeConstantColumns() {
        return m_includeTargetColumn;
    }

    /**
     * sets if the target column should be included in the output table.
     *
     * @param b <code>true</code> if it should be included, <code>false</code> otherwise
     */
    public void includeConstantColumns(final boolean b) {
        m_includeTargetColumn = b;
    }

    /**
     * Returns a list with the columns that should be included in the output table. This list also includes the target
     * column if it should be included.
     *
     * @param model the feature elimination model used
     * @return a list with column names
     *
     * @see #includeConstantColumns()
     */
    public List<String> includedColumns(final FeatureSelectionModel model) {
        final List<String> l = new ArrayList<>();
        if (m_thresholdMode) {
            Pair<Double, Collection<String>> p = findMinimalSet(model, m_errorThreshold);
            if (p != null) {
                l.addAll(p.getSecond());
            }
        } else if (m_bestScoreMode) {
            Optional<Pair<Double, Collection<String>>> p = model.featureLevels().stream().min(createComparator(model));
            if (p.isPresent()) {
                l.addAll(p.get().getSecond());
            }
        } else if (m_selectedFeatures == null) {
            for (Pair<Double, Collection<String>> p : model.featureLevels()) {
                Collection<String> incFeatures = p.getSecond();
                if (incFeatures.size() == m_nrOfFeatures) {
                    l.addAll(incFeatures);
                    break;
                }
            }
        } else {
            l.addAll(m_selectedFeatures);
        }
        if (m_includeTargetColumn) {
            l.addAll(Arrays.asList(model.getConstantColumns()));
        }
        return l;
    }

    /**
     * This method exists for backwards compatibility: Older versions of this node stored only the number of features,
     * not an actual list.
     *
     * @return the stored number of features
     */
    int nrOfFeatures() {
        return m_nrOfFeatures;
    }

    /**
     * Returns the list of included features for the selected level. This is not necessarily the same with the list
     * of included columns in the {@link BWElimModel} as the latter only contains columns that are present in the input
     * table while the number of features is the "level" that comes out from the elimination loop.
     *
     * @return the list of included features
     */
    public Collection<String> selectedFeatures() {
        return m_selectedFeatures;
    }

    /**
     * Sets the list of included features for the selected level. This is not necessarily the same with the list of
     * included columns in the {@link BWElimModel} as the latter only contains columns that are present in the input
     * table while the number of features is the "level" that comes out from the elimination loop.
     *
     * @param features the number of included features
     */
    public void selectedFeatures(final Collection<String> features) {
        m_selectedFeatures = features;
        m_nrOfFeatures = features.size();
    }

    /**
     * Saves the settings from this object into the passed node settings object.
     *
     * @param settings a node settings object
     */
    public void saveSettings(final NodeSettingsWO settings) {
        if (m_selectedFeatures != null) {
            settings.addStringArray(SELECTED_FEATURES_KEY, m_selectedFeatures.toArray(String[]::new));
        }
        settings.addInt(NR_OF_FEATURES_KEY, m_nrOfFeatures);
        settings.addBoolean(INCLUDE_TARGET_KEY, m_includeTargetColumn);
        settings.addBoolean(THRESHOLD_MODE_KEY, m_thresholdMode);
        settings.addDouble(ERROR_THRESHOLD_KEY, m_errorThreshold);
        settings.addBoolean(BEST_SCORE_MODE_KEY, m_bestScoreMode);
    }

    /**
     * Loads the settings from passed node settings object into this object.
     *
     * @param settings a node settings object
     * @throws InvalidSettingsException if a settings is missing
     */
    public void loadSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_nrOfFeatures = settings.getInt(NR_OF_FEATURES_KEY);
        if (settings.containsKey(SELECTED_FEATURES_KEY)) {
            m_selectedFeatures = Arrays.asList(settings.getStringArray(SELECTED_FEATURES_KEY));
        }
        // if the amount of selected features doesn't match the "nrOfFeatures" setting, let the latter take precedence
        // for backwards compatibility. It's likely set via a flow variable.
        if (m_selectedFeatures != null && m_selectedFeatures.size() != m_nrOfFeatures) {
            m_selectedFeatures = null;
        }

        m_includeTargetColumn = settings.getBoolean(INCLUDE_TARGET_KEY);

        /** @since 2.4 */
        m_thresholdMode = settings.getBoolean(THRESHOLD_MODE_KEY, false);
        m_errorThreshold = settings.getDouble(ERROR_THRESHOLD_KEY, 0.5);

        /** @since 4.1 */
        m_bestScoreMode = settings.getBoolean(BEST_SCORE_MODE_KEY, false);
    }

    /**
     * Loads the settings from passed node settings object into this object using default values if a settings is
     * missing.
     *
     * @param settings a node settings object
     */
    public void loadSettingsForDialog(final NodeSettingsRO settings) {
        m_nrOfFeatures = settings.getInt(NR_OF_FEATURES_KEY, -1);
        if (settings.containsKey(SELECTED_FEATURES_KEY)) {
            m_selectedFeatures = Arrays.asList(settings.getStringArray(SELECTED_FEATURES_KEY, new String[]{}));//NOSONAR
        }
        // if the amount of selected features doesn't match the "nrOfFeatures" setting, let the latter take precedence
        // for backwards compatibility. It's likely set via a flow variable.
        if (m_selectedFeatures != null && m_selectedFeatures.size() != m_nrOfFeatures) {
            m_selectedFeatures = null;
        }

        m_includeTargetColumn = settings.getBoolean(INCLUDE_TARGET_KEY, false);
        m_thresholdMode = settings.getBoolean(THRESHOLD_MODE_KEY, false);
        m_errorThreshold = settings.getDouble(ERROR_THRESHOLD_KEY, 0.5);
        m_bestScoreMode = settings.getBoolean(BEST_SCORE_MODE_KEY, false);
    }

    static Pair<Double, Collection<String>> findMinimalSet(final FeatureSelectionModel model, final double threshold) {
        List<Pair<Double, Collection<String>>> col = new ArrayList<>(model.featureLevels());
        // sort subsets by score
        Collections.sort(col, createComparator(model));

        Pair<Double, Collection<String>> selectedLevel = null;
        for (Pair<Double, Collection<String>> p : col) {
            final boolean betterThanThreshold =
                model.isMinimize() ? p.getFirst() <= threshold : p.getFirst() >= threshold;
            if (betterThanThreshold
                && ((selectedLevel == null) || (selectedLevel.getSecond().size() > p.getSecond().size()))) {
                selectedLevel = p;
            }
        }
        return selectedLevel;
    }

    private static Comparator<Pair<Double, Collection<String>>> createComparator(final FeatureSelectionModel model) {
        return (o1, o2) -> {
            final int comp = o1.getFirst().compareTo(o2.getFirst());
            return model.isMinimize() ? comp : -comp;
        };
    }
}
