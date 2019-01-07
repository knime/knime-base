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
 *   Oct 23, 2018 (simon): created
 */
package org.knime.base.node.meta.feature.selection.genetic;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

import io.jenetics.BitGene;
import io.jenetics.Crossover;
import io.jenetics.MultiPointCrossover;
import io.jenetics.SinglePointCrossover;
import io.jenetics.UniformCrossover;

/**
 * Enumeration of crossover strategies which can potentially been used in the genetic algorithm to perform feature
 * selection.
 *
 * @author Simon Schmid, KNIME, Austin, USA
 */
public enum CrossoverStrategy {

        /** Uniform Crossover */
        UNIFORM_CROSSOVER("Uniform", (byte)0),
        /** Single-point Crossover */
        SINGLE_POINT_CROSSOVER("Single-point", (byte)1),
        /** Two-point Crossover */
        TWO_POINT_CROSSOVER("Two-point", (byte)2);

    private final String m_name;

    private final byte m_persistByte;

    private CrossoverStrategy(final String name, final byte persistByte) {
        m_name = name;
        m_persistByte = persistByte;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return m_name;
    }

    /**
     * Save the selected strategy.
     *
     * @param settings the settings to save to
     */
    public void save(final NodeSettingsWO settings) {
        settings.addByte("ga_crossoverStrategy", m_persistByte);
    }

    /**
     * Loads a strategy.
     *
     * @param settings the settings to load the strategy from
     * @return the loaded strategy
     * @throws InvalidSettingsException if the selection strategy couldn't be loaded
     */
    public static CrossoverStrategy load(final NodeSettingsRO settings) throws InvalidSettingsException {
        byte persistByte = settings.getByte("ga_crossoverStrategy");
        for (final CrossoverStrategy strategy : values()) {
            if (persistByte == strategy.m_persistByte) {
                return strategy;
            }
        }
        throw new InvalidSettingsException(
            "The crossover selection strategy for the genetic algorithm could not be loaded.");
    }

    /**
     * Returns a new object of the given strategy.
     *
     * @param strategy the strategy
     * @param crossoverRate the crossover probability
     * @return the new strategy object
     * @throws IllegalArgumentException if the strategy is invalid
     */
    public static Crossover<BitGene, Double> getCrossover(final CrossoverStrategy strategy, final double crossoverRate) {
        switch (strategy) {
            case UNIFORM_CROSSOVER:
                return new UniformCrossover<>(crossoverRate);
            case SINGLE_POINT_CROSSOVER:
                return new SinglePointCrossover<>(crossoverRate);
            case TWO_POINT_CROSSOVER:
                return new MultiPointCrossover<>(crossoverRate, 2);
            default:
                throw new IllegalArgumentException("Crossover strategy not found: " + strategy);
        }
    }

}
