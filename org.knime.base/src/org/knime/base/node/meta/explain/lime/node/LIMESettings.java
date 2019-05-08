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
package org.knime.base.node.meta.explain.lime.node;

import java.util.Random;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.util.filter.column.DataColumnSpecFilterConfiguration;

/**
 * TODO unify settings for Shapley Values and ModelExplainer
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
public class LIMESettings {

    /**
     *
     */
    private static final String CFG_KERNEL_WIDTH = "kernelWidth";

    /**
     *
     */
    private static final String CFG_KERNEL_WIDTH_DEFINED = "useManualKernelWidth";

    /**
     *
     */
    private static final String CFG_SAMPLE_AROUND_INSTANCE = "sampleAroundInstance";

    /**
     *
     */
    private static final String CFG_SEED = "seed";

    /**
     *
     */
    private static final String CFG_USE_SEED = "useSeed";

    /**
     *
     */
    private static final String CFG_EXPLANATION_SET_SIZE = "explanationSetSize";

    private static final String CFG_DONT_USE_ELEMENT_NAMES = "dontUseElementNames";

    /**
     *
     */
    private static final int DEF_EXPLANATION_SET_SIZE = 1000;

    private static final String CFG_FEATURE_COLS = "featureColumns";

    private int m_explanationSetSize = 1000;

    private boolean m_useSeed = false;

    private long m_seed = newSeed();

    private boolean m_dontUseElementNames = false;

    private boolean m_sampleAroundInstance = false;

    private boolean m_kernelWidthDefined = false;

    private double m_kernelWidth = 1.0;

    private DataColumnSpecFilterConfiguration m_featureCols = createFeatureCols();

    void loadSettingsDialog(final NodeSettingsRO settings, final DataTableSpec inSpec) {
        m_explanationSetSize = settings.getInt(CFG_EXPLANATION_SET_SIZE, DEF_EXPLANATION_SET_SIZE);
        m_useSeed = settings.getBoolean(CFG_USE_SEED, false);
        m_seed = settings.getLong(CFG_SEED, newSeed());
        m_featureCols.loadConfigurationInDialog(settings, inSpec);
        m_dontUseElementNames = settings.getBoolean(CFG_DONT_USE_ELEMENT_NAMES, false);
        m_sampleAroundInstance = settings.getBoolean(CFG_SAMPLE_AROUND_INSTANCE, false);
        m_kernelWidthDefined = settings.getBoolean(CFG_KERNEL_WIDTH_DEFINED, false);
        m_kernelWidth = settings.getDouble(CFG_KERNEL_WIDTH, 1.0);
    }

    void loadSettingsModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_explanationSetSize = settings.getInt(CFG_EXPLANATION_SET_SIZE);
        m_useSeed = settings.getBoolean(CFG_USE_SEED);
        m_seed = settings.getLong(CFG_SEED);
        m_featureCols.loadConfigurationInModel(settings);
        m_dontUseElementNames = settings.getBoolean(CFG_DONT_USE_ELEMENT_NAMES);
        m_sampleAroundInstance = settings.getBoolean(CFG_SAMPLE_AROUND_INSTANCE);
        m_kernelWidthDefined = settings.getBoolean(CFG_KERNEL_WIDTH_DEFINED);
        m_kernelWidth = settings.getDouble(CFG_KERNEL_WIDTH);
    }

    void saveSettings(final NodeSettingsWO settings) {
        settings.addInt(CFG_EXPLANATION_SET_SIZE, m_explanationSetSize);
        settings.addBoolean(CFG_USE_SEED, m_useSeed);
        settings.addLong(CFG_SEED, m_seed);
        m_featureCols.saveConfiguration(settings);
        settings.addBoolean(CFG_DONT_USE_ELEMENT_NAMES, m_dontUseElementNames);
        settings.addBoolean(CFG_SAMPLE_AROUND_INSTANCE, m_sampleAroundInstance);
        settings.addBoolean(CFG_KERNEL_WIDTH_DEFINED, m_kernelWidthDefined);
        settings.addDouble(CFG_KERNEL_WIDTH, m_kernelWidth);
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
     * @return the dontUseElementNames
     */
    public boolean isDontUseElementNames() {
        return m_dontUseElementNames;
    }

    /**
     * @return the sampleAroundInstance
     */
    public boolean isSampleAroundInstance() {
        return m_sampleAroundInstance;
    }

    /**
     * @param sampleAroundInstance the sampleAroundInstance to set
     */
    void setSampleAroundInstance(final boolean sampleAroundInstance) {
        m_sampleAroundInstance = sampleAroundInstance;
    }

    /**
     * @return the kernelWidthDefined
     */
    public boolean isUseManualKernelWidth() {
        return m_kernelWidthDefined;
    }

    /**
     * @param kernelWidthDefined the kernelWidthDefined to set
     */
    void setUseManualKernelWidth(final boolean kernelWidthDefined) {
        m_kernelWidthDefined = kernelWidthDefined;
    }

    /**
     * @return the kernelWidth
     */
    public double getKernelWidth() {
        return m_kernelWidth;
    }

    /**
     * @param kernelWidth the kernelWidth to set
     */
    void setKernelWidth(final double kernelWidth) {
        m_kernelWidth = kernelWidth;
    }

}


