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
package org.knime.base.node.meta.explain.shap.node;

import java.util.Optional;
import java.util.Random;

import org.knime.base.node.meta.explain.node.Settings;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.util.filter.column.DataColumnSpecFilterConfiguration;

/**
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
public class ShapLoopStartSettings implements Settings {

    /**
     *
     */
    private static final String CFG_SAMPLING_WEIGHT_COLUMN = "samplingWeightColumn";

    private static final String CFG_SEED = "seed";

    private static final String CFG_USE_SEED = "useSeed";

    private static final String CFG_EXPLANATION_SET_SIZE = "explanationSetSize";

    private static final String CFG_TREAT_COLLECTIONS_AS_SINGLE_FEATURE = "treatAllColumnsAsSingleFeature";

    private static final int DEF_EXPLANATION_SET_SIZE = 1000;

    private static final String CFG_FEATURE_COLS = "featureColumns";

    private int m_explanationSetSize = 1000;

    private boolean m_useSeed = false;

    private long m_seed = newSeed();

    private boolean m_treatAllColumnsAsSingleFeature = false;

    private String m_samplingWeightColumn = null;


    private DataColumnSpecFilterConfiguration m_featureCols = createFeatureCols();


    @Override
    public void loadSettingsInDialog(final NodeSettingsRO settings, final DataTableSpec[] inSpecs) {
        m_explanationSetSize = settings.getInt(CFG_EXPLANATION_SET_SIZE, DEF_EXPLANATION_SET_SIZE);
        m_useSeed = settings.getBoolean(CFG_USE_SEED, false);
        m_seed = settings.getLong(CFG_SEED, newSeed());
        m_featureCols.loadConfigurationInDialog(settings, inSpecs[0]);
        m_treatAllColumnsAsSingleFeature = settings.getBoolean(CFG_TREAT_COLLECTIONS_AS_SINGLE_FEATURE, false);
        m_samplingWeightColumn = settings.getString(CFG_SAMPLING_WEIGHT_COLUMN, null);
    }

    @Override
    public void loadSettingsInModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_explanationSetSize = settings.getInt(CFG_EXPLANATION_SET_SIZE);
        m_useSeed = settings.getBoolean(CFG_USE_SEED);
        m_seed = settings.getLong(CFG_SEED);
        m_featureCols.loadConfigurationInModel(settings);
        m_treatAllColumnsAsSingleFeature = settings.getBoolean(CFG_TREAT_COLLECTIONS_AS_SINGLE_FEATURE);
        m_samplingWeightColumn = settings.getString(CFG_SAMPLING_WEIGHT_COLUMN);
    }

    @Override
    public void saveSettings(final NodeSettingsWO settings) {
        settings.addInt(CFG_EXPLANATION_SET_SIZE, m_explanationSetSize);
        settings.addBoolean(CFG_USE_SEED, m_useSeed);
        settings.addLong(CFG_SEED, m_seed);
        m_featureCols.saveConfiguration(settings);
        settings.addBoolean(CFG_TREAT_COLLECTIONS_AS_SINGLE_FEATURE, m_treatAllColumnsAsSingleFeature);
        settings.addString(CFG_SAMPLING_WEIGHT_COLUMN, m_samplingWeightColumn);
    }


    /**
     * @return the explanationDatasetSize
     */
    public int getExplanationSetSize() {
        return m_explanationSetSize;
    }

    /**
     * @param explanationDatasetSize the explanationDatasetSize to set
     */
    void setExplanationSetSize(final int explanationDatasetSize) {
        m_explanationSetSize = explanationDatasetSize;
    }

    /**
     * @return the useSeed
     */
    public boolean isUseSeed() {
        return m_useSeed;
    }

    /**
     * @param useSeed the useSeed to set
     */
    void setUseSeed(final boolean useSeed) {
        m_useSeed = useSeed;
    }

    /**
     * @return the seed
     */
    public long getSeed() {
        return isUseSeed() ? getManualSeed() : newSeed();
    }

    long getManualSeed() {
        return m_seed;
    }

    /**
     * @param seed the seed to set
     */
    void setSeed(final long seed) {
        m_seed = seed;
    }

    /**
     * @return the featureCols
     */
    public DataColumnSpecFilterConfiguration getFeatureCols() {
        return m_featureCols;
    }

    /**
     * @param featureCols the featureCols to set
     */
    void setFeatureCols(final DataColumnSpecFilterConfiguration featureCols) {
        m_featureCols = featureCols;
    }

    private static DataColumnSpecFilterConfiguration createFeatureCols() {
        return new DataColumnSpecFilterConfiguration(CFG_FEATURE_COLS);
    }


    private static long newSeed() {
        return new Random().nextLong();
    }


    /**
     * @return the treatAllColumnsAsSingleFeature
     */
    public boolean isTreatAllColumnsAsSingleFeature() {
        return m_treatAllColumnsAsSingleFeature;
    }

    /**
     * @param treatAllColumnsAsSingleFeature the treatAllColumnsAsSingleFeature to set
     */
    void setTreatAllColumnsAsSingleFeature(final boolean treatAllColumnsAsSingleFeature) {
        m_treatAllColumnsAsSingleFeature = treatAllColumnsAsSingleFeature;
    }

    /**
     * @return the weightColumn
     */
    public Optional<String> getWeightColumn() {
        return Optional.ofNullable(m_samplingWeightColumn);
    }

    /**
     * @param weightColumn the weightColumn to set
     */
    public void setWeightColumn(final String weightColumn) {
        m_samplingWeightColumn = weightColumn;
    }

}
