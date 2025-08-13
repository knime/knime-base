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
 */
package org.knime.base.node.util.sampledata;

import java.util.Arrays;
import java.util.stream.Collectors;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.node.parameters.Advanced;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.layout.After;
import org.knime.node.parameters.layout.Layout;
import org.knime.node.parameters.layout.Section;
import org.knime.node.parameters.persistence.NodeParametersPersistor;
import org.knime.node.parameters.persistence.Persist;
import org.knime.node.parameters.persistence.Persistor;

/**
 * Modern WebUI settings replacing {@link SampleDataNodeDialog} while keeping all legacy configuration keys identical so
 * the {@link SampleDataNodeModel} continues to work unchanged.
 */
@SuppressWarnings("restriction")
final class SampleDataNodeSettings implements NodeParameters {

    /* ---------------- Sections ---------------- */

    @Section(title = "Cluster & Universe Structure", description = "Define the structure of the synthetic data set: number of clusters per universe and the dimensionality (number of attributes) per universe. Each entry corresponds positionally to one \"universe\"; i.e. the first number in each list belongs to Universe 0, the second to Universe 1, etc.")
    interface StructureSection { }

    @Section(title = "Distribution Parameters", description = "Control the statistical properties of the clusters: standard deviation within a cluster and the fraction of patterns that should be pure noise (i.e. not belonging to any cluster). Noise must be between 0.0 and 1.0.")
    @After(StructureSection.class)
    interface DistributionSection { }

    @Section(title = "Randomization", description = "Set the RNG seed for reproducible generation. The same seed with identical parameters yields identical output.")
    @Advanced
    @After(DistributionSection.class)
    interface RandomizationSection { }

    /* ---------------- Settings ---------------- */

    @Widget(title = "Cluster counts per universe", description = "Comma separated list of non-negative integers specifying how many clusters to generate in each universe. Example: 2,3,2 creates three universes with 2, 3 and 2 clusters respectively. Must have the same length as 'Universe sizes'. Empty entries or negative numbers are invalid.")
    @Layout(StructureSection.class)
    @Persistor(ClusterCountsPersistor.class)
    String m_clusterCountsCsv = intArrayToCsv(SampleDataNodeModel.CLUSTER_COUNT);

    @Widget(title = "Universe sizes (dimensions per universe)", description = "Comma separated list of positive integers specifying the dimensionality (number of numeric attributes) of each universe. Must have the same length as 'Cluster counts'. Each universe contributes its dimensions sequentially to the output table. Example: 2,2 means two universes with 2 dimensions each (total 4 numeric columns plus the cluster membership column). Values must be > 0.")
    @Layout(StructureSection.class)
    @Persistor(UniverseSizesPersistor.class)
    String m_universeSizesCsv = intArrayToCsv(SampleDataNodeModel.UNIVERSE_SIZE);

    @Widget(title = "Overall pattern count", description = "Total number of patterns (rows) to generate. Must be > 0. In the legacy dialog the default was 100; keeping that default for backward compatibility.")
    @Layout(StructureSection.class)
    @Persist(configKey = SampleDataNodeModel.CFGKEY_PATCOUNT)
    int m_patternCount = 100; // legacy dialog default (not model constant)

    @Widget(title = "Cluster standard deviation", description = "Standard deviation of the Gaussian noise within each cluster. Must be > 0. Smaller values produce tighter clusters; larger values produce more spread.")
    @Layout(DistributionSection.class)
    @Persist(configKey = SampleDataNodeModel.CFGKEY_DEV)
    double m_stdDeviation = SampleDataNodeModel.STD_DEVIATION;

    @Widget(title = "Noise fraction", description = "Fraction (0.0 – 1.0) of additional noise patterns that do not belong to any cluster. These rows get random attribute values (or NaN, depending on internal model flags). 0 disables noise.")
    @Layout(DistributionSection.class)
    @Persist(configKey = SampleDataNodeModel.CFGKEY_NOISE)
    double m_noiseFraction = SampleDataNodeModel.NOISEFRAC;

    @Widget(title = "Random seed", description = "Seed for the random number generator. Use a fixed value for reproducible results or change it to obtain different random samples.", advanced = true)
    @Layout(RandomizationSection.class)
    @Persist(configKey = SampleDataNodeModel.CFGKEY_SEED)
    int m_seed = 1; // legacy dialog default

    /* ---------------- Helper & Persistors ---------------- */

    private static String intArrayToCsv(final int[] values) {
        return Arrays.stream(values).mapToObj(Integer::toString).collect(Collectors.joining(","));
    }

    private static int[] parseCsvToIntArray(final String csv, final boolean allowZero, final boolean allowNegative)
        throws InvalidSettingsException {
        if (csv == null || csv.trim().isEmpty()) {
            throw new InvalidSettingsException("Value list must not be empty");
        }
        final var parts = csv.split("\\s*,\\s*");
        final int[] out = new int[parts.length];
        for (int i = 0; i < parts.length; i++) {
            try {
                int v = Integer.parseInt(parts[i]);
                if (!allowNegative && v < 0) {
                    throw new InvalidSettingsException("Negative value not allowed: " + v);
                }
                if (!allowZero && v <= 0) {
                    throw new InvalidSettingsException("Non-positive value not allowed: " + v);
                }
                out[i] = v;
            } catch (NumberFormatException e) {
                throw new InvalidSettingsException("Not an integer: '" + parts[i] + "'", e);
            }
        }
        return out;
    }

    /** Persistor converting between a CSV string field and the int[] NodeSettings value for cluster counts. */
    static final class ClusterCountsPersistor implements NodeParametersPersistor<String> {
        @Override
        public String load(final NodeSettingsRO settings) throws InvalidSettingsException {
            final int[] arr = settings.getIntArray(SampleDataNodeModel.CFGKEY_CLUSCOUNT,
                SampleDataNodeModel.CLUSTER_COUNT);
            return intArrayToCsv(arr);
        }

        @Override
        public void save(final String value, final NodeSettingsWO settings) {
            try {
                final int[] arr = parseCsvToIntArray(value, true, false); // clusters may be zero-sized
                settings.addIntArray(SampleDataNodeModel.CFGKEY_CLUSCOUNT, arr);
            } catch (InvalidSettingsException e) {
                // store fallback (legacy default) to avoid corrupt settings, and rethrow if desired by framework
                settings.addIntArray(SampleDataNodeModel.CFGKEY_CLUSCOUNT, SampleDataNodeModel.CLUSTER_COUNT);
            }
        }

        @Override
        public String[][] getConfigPaths() {
            return new String[][] { { SampleDataNodeModel.CFGKEY_CLUSCOUNT } };
        }
    }

    /** Persistor converting between a CSV string field and the int[] NodeSettings value for universe sizes. */
    static final class UniverseSizesPersistor implements NodeParametersPersistor<String> {
        @Override
        public String load(final NodeSettingsRO settings) throws InvalidSettingsException {
            final int[] arr = settings.getIntArray(SampleDataNodeModel.CFGKEY_UNISIZE,
                SampleDataNodeModel.UNIVERSE_SIZE);
            return intArrayToCsv(arr);
        }

        @Override
        public void save(final String value, final NodeSettingsWO settings) {
            try {
                final int[] arr = parseCsvToIntArray(value, false, false); // must be > 0
                settings.addIntArray(SampleDataNodeModel.CFGKEY_UNISIZE, arr);
            } catch (InvalidSettingsException e) {
                settings.addIntArray(SampleDataNodeModel.CFGKEY_UNISIZE, SampleDataNodeModel.UNIVERSE_SIZE);
            }
        }

        @Override
        public String[][] getConfigPaths() {
            return new String[][] { { SampleDataNodeModel.CFGKEY_UNISIZE } };
        }
    }
}
