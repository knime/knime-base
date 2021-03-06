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
 *   16.03.2016 (adrian): created
 */
package org.knime.base.node.meta.feature.selection;

import java.util.List;

import org.knime.base.node.meta.feature.selection.genetic.GeneticStrategy;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 *
 * @author Adrian Nembach, KNIME.com
 */
public class FeatureSelectionStrategies {

    /**
     * Contains the available feature selection strategies
     *
     * @author Adrian Nembach, KNIME.com
     */
    public enum Strategy {
            /**
             * Forward Feature Selection. Starts from an empty set and iteratively adds the feature that provides the
             * best gain
             */
            ForwardFeatureSelection("Forward Feature Selection", StrategyType.Sequential, (byte)0),
            /**
             * Backward Feature Elimination. Starts from the full set and iteratively removes the feature whose removal
             * yields the smallest loss.
             */
            BackwardFeatureElimination("Backward Feature Elimination", StrategyType.Sequential, (byte)1),
            /**
             * Genetic Algorithm for Feature Selection. Binary chromosomes represent which features to include and which
             * to exclude.
             */
            GeneticAlgorithm("Genetic Algorithm", StrategyType.Genetic, (byte)2),
            /**
             * Random strategy. Tries randomly generated feature subsets.
             */
            Random("Random", StrategyType.Random, (byte)3);

        private final String m_string;

        private final StrategyType m_type;

        private final byte m_persistByte;

        private Strategy(final String name, final StrategyType type, final byte persistByte) {
            m_string = name;
            m_type = type;
            m_persistByte = persistByte;
        }

        @Override
        public String toString() {
            return m_string;
        }

        /**
         * @return the type of the strategy
         */
        public StrategyType getType() {
            return m_type;
        }

        /**
         * save the selected strategy
         *
         * @param settings the settings to save to
         */
        public void save(final NodeSettingsWO settings) {
            settings.addByte("selectionStrategy", m_persistByte);
        }

        /**
         * Loads a strategy
         *
         * @param settings the settings to load the strategy from
         * @return the loaded strategy
         * @throws InvalidSettingsException if the selection strategy couldn't be loaded
         */
        public static Strategy load(final NodeSettingsRO settings) throws InvalidSettingsException {
            byte persistByte = settings.getByte("selectionStrategy");
            for (Strategy strategy : values()) {
                if (persistByte == strategy.m_persistByte) {
                    return strategy;
                }
            }
            throw new InvalidSettingsException("The feature selection strategy could not be loaded.");
        }

    }

    /**
     * Creates a FeatureSelectionStrategy with the provided parameters
     *
     * @param settings the settings
     * @param featureColumns a list containing indices of features that are interpreted by a {@link ColumnHandler}
     * @return the specified feature selection strategy
     */
    public static FeatureSelectionStrategy createFeatureSelectionStrategy(
        final FeatureSelectionLoopStartSettings settings, final List<Integer> featureColumns) {
        final Strategy strategy = settings.getStrategy();
        switch (strategy) {
            case ForwardFeatureSelection:
                return new FFSStrategy(settings.getNrFeaturesThreshold(), featureColumns);
            case BackwardFeatureElimination:
                return new FBSStrategy(settings.getNrFeaturesThreshold(), featureColumns);
            case GeneticAlgorithm:
                return new GeneticStrategy.Builder(settings.getPopSize(), settings.getMaxNumGenerations(),
                    featureColumns.size()).nrFeaturesLowerBound(settings.getNrFeaturesLowerBound())
                        .nrFeaturesUpperBound(settings.getNrFeaturesUpperBound())
                        .seed(settings.isUseRandomSeed() ? settings.getRandomSeed() : System.currentTimeMillis())
                        .survivorsFraction(settings.getSurvivorsFraction()).crossoverRate(settings.getCrossoverRate())
                        .mutationRate(settings.getMutationRate()).elitismRate(settings.getElitismRate())
                        .earlyStopping(settings.getEarlyStoppingGenetic())
                        .selectionStrategy(settings.getSelectionStrategy())
                        .crossoverStrategy(settings.getCrossoverStrategy()).build();
            case Random:
                return new RandomStrategy.Builder(settings.getMaxNumIterations(), featureColumns)
                    .nrFeaturesLowerBound(settings.getNrFeaturesLowerBound())
                    .nrFeaturesUpperBound(settings.getNrFeaturesUpperBound())
                    .seed(settings.isUseRandomSeed() ? settings.getRandomSeed() : System.currentTimeMillis())
                    .earlyStopping(settings.getEarlyStoppingRandom()).tolerance(settings.getEarlyStoppingTolerance())
                    .build();
            default:
                throw new IllegalArgumentException("The FeatureSelectionStrategy \"" + strategy + "\" is unknown.");
        }
    }

    /**
     * The type of a {@link Strategy}. Each strategy type has to define its dialog pane (in
     * {@link FeatureSelectionLoopStartNodeDialogPane}) specifying the specific strategy settings.
     *
     * @author Simon Schmid, KNIME, Austin, USA
     */
    enum StrategyType {

            /**
             * FFS and BFE
             */
            Sequential("Sequential Algorithm Settings", false),
            /**
             * Genetic Algorithm
             */
            Genetic("Genetic Algorithm Settings", true),
            /**
             * Random
             */
            Random("Random Algorithm Settings", true);

        private final String m_dialogPanelTitle;

        private final boolean m_hasAdvancedSettings;

        /**
         *
         */
        private StrategyType(final String dialogPanelTitle, final boolean hasAdvancedSettings) {
            m_dialogPanelTitle = dialogPanelTitle;
            m_hasAdvancedSettings = hasAdvancedSettings;
        }

        /**
         * @return dialog panel title
         */
        public String getDialogPanelTitle() {
            return m_dialogPanelTitle;
        }

        /**
         * @return the hasAdvancedSettings
         */
        public boolean hasAdvancedSettings() {
            return m_hasAdvancedSettings;
        }
    }

}
