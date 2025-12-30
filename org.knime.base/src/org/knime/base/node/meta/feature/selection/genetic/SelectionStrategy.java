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
import org.knime.node.parameters.widget.choices.Label;

import io.jenetics.ExponentialRankSelector;
import io.jenetics.Gene;
import io.jenetics.LinearRankSelector;
import io.jenetics.RouletteWheelSelector;
import io.jenetics.Selector;
import io.jenetics.StochasticUniversalSelector;
import io.jenetics.TournamentSelector;

/**
 * Enumeration of selection strategies which can potentially been used in the genetic algorithm to perform feature
 * selection.
 *
 * @author Simon Schmid, KNIME, Austin, USA
 */
public enum SelectionStrategy {

        /** Tournament Selection. */
        @Label("Tournament")
        TOURNAMENT_SELECTION("Tournament", (byte)0) {

            @Override
            <G extends Gene<?, G>, C extends Number & Comparable<? super C>> Selector<G, C> getSelector() {
                return new TournamentSelector<>();
            }
        },
        /** Roulette Wheel Selection. */
        @Label("Roulette Wheel")
        ROULETTEWHEEL_SELECTION("Roulette Wheel", (byte)1) {

            @Override
            <G extends Gene<?, G>, C extends Number & Comparable<? super C>> Selector<G, C> getSelector() {
                return new RouletteWheelSelector<>();
            }
        },
        /** Stochastic Universal Selection. */
        @Label("Stochastic Universal")
        STOCHASTIC_UNIVERSAL_SELECTION("Stochastic Universal", (byte)2) {

            @Override
            <G extends Gene<?, G>, C extends Number & Comparable<? super C>> Selector<G, C> getSelector() {
                return new StochasticUniversalSelector<>();
            }
        },
        /** Linear Rank Selection. */
        @Label("Linear Rank")
        LINEAR_RANK_SELECTION("Linear Rank", (byte)3) {

            @Override
            <G extends Gene<?, G>, C extends Number & Comparable<? super C>> Selector<G, C> getSelector() {
                return new LinearRankSelector<>();
            }
        },
        /** Exponential Rank Selection. */
        @Label("Exponential Rank")
        EXPONENTIAL_RANK_SELECTION("Exponential Rank", (byte)4) {

            @Override
            <G extends Gene<?, G>, C extends Number & Comparable<? super C>> Selector<G, C> getSelector() {
                return new ExponentialRankSelector<>();
            }
        };

    private final String m_name;

    private final byte m_persistByte;

    private SelectionStrategy(final String name, final byte persistByte) {
        m_name = name;
        m_persistByte = persistByte;
    }

    abstract <G extends Gene<?, G>, C extends Number & Comparable<? super C>> Selector<G, C> getSelector();

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
        settings.addByte("ga_selectionStrategy", m_persistByte);
    }

    /**
     * Loads a strategy.
     *
     * @param settings the settings to load the strategy from
     * @return the loaded strategy
     * @throws InvalidSettingsException if the selection strategy couldn't be loaded
     */
    public static SelectionStrategy load(final NodeSettingsRO settings) throws InvalidSettingsException {
        byte persistByte = settings.getByte("ga_selectionStrategy");
        for (final SelectionStrategy strategy : values()) {
            if (persistByte == strategy.m_persistByte) {
                return strategy;
            }
        }
        throw new InvalidSettingsException("The selection strategy for the genetic algorithm could not be loaded.");
    }

}
