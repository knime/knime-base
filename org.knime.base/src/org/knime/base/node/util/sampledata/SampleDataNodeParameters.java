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
 * ------------------------------------------------------------------------
 */

package org.knime.base.node.util.sampledata;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.migration.LoadDefaultsForAbsentFields;
import org.knime.node.parameters.persistence.NodeParametersPersistor;
import org.knime.node.parameters.persistence.Persist;
import org.knime.node.parameters.persistence.Persistor;
import org.knime.node.parameters.widget.number.NumberInputWidget;
import org.knime.node.parameters.widget.number.NumberInputWidgetValidation.MaxValidation;
import org.knime.node.parameters.widget.number.NumberInputWidgetValidation.MinValidation.IsNonNegativeValidation;
import org.knime.node.parameters.widget.text.TextInputWidget;

/**
 * Node parameters for Data Generator.
 *
 * @author Daniel Bogenrieder, KNIME GmbH, Konstanz, Germany
 * @author AI Migration Pipeline v1.0
 */
@LoadDefaultsForAbsentFields
class SampleDataNodeParameters implements NodeParameters {

    @Persistor(ClusterCountPersistor.class)
    @Widget(title = "Cluster count",
        description = "Comma-separated list defining the number of clusters to generate in each universe (e.g., \"2, 3\" creates 2 clusters in the first universe and 3 in the second).")
    @TextInputWidget
    String m_clusterCount = "2, 2";

    @Persistor(UniverseSizesPersistor.class)
    @Widget(title = "Universe sizes",
        description = "Comma-separated list specifying the number of attributes (dimensions) for each universe (e.g., \"2, 3\" means first universe has 2 attributes, second has 3).")
    @TextInputWidget
    String m_universeSizes = "2, 2";

    @Persist(configKey = SampleDataNodeModel.CFGKEY_PATCOUNT)
    @Widget(title = "Pattern count",
        description = "Total number of data patterns to generate across all clusters and universes.")
    @NumberInputWidget(minValidation = IsNonNegativeValidation.class)
    int m_patternCount = 5400;

    @Persist(configKey = SampleDataNodeModel.CFGKEY_DEV)
    @Widget(title = "Standard deviation",
        description = "Controls the spread of data points within each cluster. Smaller values create tighter clusters.")
    @NumberInputWidget(minValidation = IsNonNegativeValidation.class)
    double m_standardDeviation = 0.1;

    @Persist(configKey = SampleDataNodeModel.CFGKEY_NOISE)
    @Widget(title = "Noise fraction",
        description = "Proportion of randomly distributed data points that don't belong to any cluster. Value between 0 (no noise) and 1 (all noise).")
    @NumberInputWidget(minValidation = IsNonNegativeValidation.class, maxValidation = OneMaxValidation.class)
    double m_noiseFraction = 0.0;

    @Persist(configKey = SampleDataNodeModel.CFGKEY_SEED)
    @Widget(title = "Random seed",
        description = "Fixed value to ensure reproducible data generation. Use the same seed to generate identical datasets.")
    @NumberInputWidget
    int m_randomSeed = 1;

    /**
     * Validation class for maximum value of 1.0.
     */
    static class OneMaxValidation extends MaxValidation {
        @Override
        public double getMax() {
            return 1.0;
        }
    }

    /**
     * Persistor for cluster count field that converts between comma-separated string and int array.
     */
    abstract static class StringToIntArrayPersistor implements NodeParametersPersistor<String> {

        String m_configKey;

        StringToIntArrayPersistor(final String configKey) {
            m_configKey = configKey;
        }

        @Override
        public String load(final NodeSettingsRO settings) throws InvalidSettingsException {
            int[] array = settings.getIntArray(m_configKey, new int[]{2, 2});
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < array.length; i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                sb.append(array[i]);
            }
            return sb.toString();
        }

        @Override
        public void save(final String value, final NodeSettingsWO settings) {
            int[] array = parseIntArray(value);
            settings.addIntArray(m_configKey, array);
        }

        @Override
        public String[][] getConfigPaths() {
            return new String[][]{{m_configKey}};
        }
    }

    final static class ClusterCountPersistor extends StringToIntArrayPersistor {

        ClusterCountPersistor() {
            super(SampleDataNodeModel.CFGKEY_CLUSCOUNT);
        }
    }

    final static class UniverseSizesPersistor extends StringToIntArrayPersistor {

        UniverseSizesPersistor() {
            super(SampleDataNodeModel.CFGKEY_UNISIZE);
        }
    }

    /**
     * Helper method to parse comma-separated string into int array.
     */
    private static int[] parseIntArray(final String text) {
        try {
            String[] split = text.split(",");
            int[] result = new int[split.length];
            for (int i = 0; i < result.length; i++) {
                result[i] = Integer.parseInt(split[i].trim());
            }
            return result;
        } catch (Exception e) {
            throw new RuntimeException("Invalid format for comma-separated integers: " + text, e);
        }
    }
}
