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
 *   Jun 26, 2025 (david): created
 */
package org.knime.base.node.preproc.binner2;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.knime.base.node.preproc.binner2.BinnerNodeSettings.CustomCutoffs;
import org.knime.base.node.preproc.binner2.BinnerNodeSettings.CustomQuantilesWidgetGroup;
import org.knime.base.node.preproc.binner2.BinnerNodeSettingsEnums.BinBoundaryExactMatchBehaviour;
import org.knime.base.node.preproc.binner2.BinnerNodeSettingsEnums.BinningType;
import org.knime.core.node.InvalidSettingsException;

/**
 * Tests for {@link BinnerNodeModel4}.
 *
 * @author David Hickey, TNG Technology Consulting GmbH
 */
final class BinnerNodeModelTest {

    BinnerNodeModel m_model;

    @BeforeEach
    void setup() {
        m_model = new BinnerNodeModel(BinnerNodeFactory.CONFIGURATION);
    }

    @Nested
    class ValidateSettingsTests {
        @Test
        void testTooFewCustomCutoffs() throws InvalidSettingsException {
            var settings = new BinnerNodeSettings();

            settings.m_binningType = BinningType.CUSTOM_CUTOFFS;
            settings.m_customCutoffs = new CustomCutoffs[]{new CustomCutoffs()};

            assertThrows(InvalidSettingsException.class, () -> {
                m_model.validateSettings(settings);
            });

            // should be fine if we add another cutoff
            settings.m_customCutoffs = new CustomCutoffs[]{ //
                new CustomCutoffs(0.5, BinBoundaryExactMatchBehaviour.TO_LOWER_BIN), //
                new CustomCutoffs(0.75, BinBoundaryExactMatchBehaviour.TO_LOWER_BIN) //
            };
            m_model.validateSettings(settings); // should not throw
        }

        @Test
        void testTooFewCustomQuantiles() throws InvalidSettingsException {
            var settings = new BinnerNodeSettings();

            settings.m_binningType = BinningType.CUSTOM_QUANTILES;
            settings.m_customQuantiles = new CustomQuantilesWidgetGroup[]{new CustomQuantilesWidgetGroup()};

            assertThrows(InvalidSettingsException.class, () -> {
                m_model.validateSettings(settings);
            });

            // should be fine if we add another quantile
            settings.m_customQuantiles = new CustomQuantilesWidgetGroup[]{ //
                new CustomQuantilesWidgetGroup(0.5, BinBoundaryExactMatchBehaviour.TO_LOWER_BIN), //
                new CustomQuantilesWidgetGroup(0.75, BinBoundaryExactMatchBehaviour.TO_LOWER_BIN) //
            };
            m_model.validateSettings(settings); // should not throw
        }

        @Test
        void testEqualWidthAndTooFewBins() throws InvalidSettingsException {
            var settings = new BinnerNodeSettings();

            settings.m_binningType = BinningType.EQUAL_WIDTH;
            settings.m_numberOfBins = 0;

            assertThrows(InvalidSettingsException.class, () -> {
                m_model.validateSettings(settings);
            });

            // should be fine if we set the number of bins to 1
            settings.m_numberOfBins = 1;
            m_model.validateSettings(settings); // should not throw
        }

        @Test
        void testEqualCountAndTooFewBins() throws InvalidSettingsException {
            var settings = new BinnerNodeSettings();

            settings.m_binningType = BinningType.EQUAL_FREQUENCY;
            settings.m_numberOfBins = 0;

            assertThrows(InvalidSettingsException.class, () -> {
                m_model.validateSettings(settings);
            });

            // should be fine if we set the number of bins to 1
            settings.m_numberOfBins = 1;
            m_model.validateSettings(settings); // should not throw
        }

        @Test
        void testCustomCutoffsAreOrdered() throws InvalidSettingsException {
            var settings = new BinnerNodeSettings();

            settings.m_binningType = BinningType.CUSTOM_CUTOFFS;
            settings.m_customCutoffs = new CustomCutoffs[]{ //
                new CustomCutoffs(0.5, BinBoundaryExactMatchBehaviour.TO_LOWER_BIN), //
                new CustomCutoffs(0.3, BinBoundaryExactMatchBehaviour.TO_LOWER_BIN) //
            };

            assertThrows(InvalidSettingsException.class, () -> {
                m_model.validateSettings(settings);
            });

            // should be fine if we switch the cutoffs
            settings.m_customCutoffs = new CustomCutoffs[]{ //
                new CustomCutoffs(0.3, BinBoundaryExactMatchBehaviour.TO_LOWER_BIN), //
                new CustomCutoffs(0.5, BinBoundaryExactMatchBehaviour.TO_LOWER_BIN) //
            };
            m_model.validateSettings(settings); // should not throw
        }

        @Test
        void testCustomQuantilesAreOrdered() throws InvalidSettingsException {
            var settings = new BinnerNodeSettings();

            settings.m_binningType = BinningType.CUSTOM_QUANTILES;
            settings.m_customQuantiles = new CustomQuantilesWidgetGroup[]{ //
                new CustomQuantilesWidgetGroup(0.5, BinBoundaryExactMatchBehaviour.TO_LOWER_BIN), //
                new CustomQuantilesWidgetGroup(0.3, BinBoundaryExactMatchBehaviour.TO_LOWER_BIN) //
            };

            assertThrows(InvalidSettingsException.class, () -> {
                m_model.validateSettings(settings);
            });

            // should be fine if we switch the quantiles
            settings.m_customQuantiles = new CustomQuantilesWidgetGroup[]{ //
                new CustomQuantilesWidgetGroup(0.3, BinBoundaryExactMatchBehaviour.TO_LOWER_BIN), //
                new CustomQuantilesWidgetGroup(0.5, BinBoundaryExactMatchBehaviour.TO_LOWER_BIN) //
            };

            m_model.validateSettings(settings); // should not throw
        }

        @Test
        void testFixedLowerBoundGreaterThanUpperBound() throws InvalidSettingsException {
            var settings = new BinnerNodeSettings();

            settings.m_fixLowerBound = true;
            settings.m_fixUpperBound = true;
            settings.m_fixedLowerBound = 5.0;
            settings.m_fixedUpperBound = 3.0;

            assertThrows(InvalidSettingsException.class, () -> {
                m_model.validateSettings(settings);
            });

            // should be fine if we switch the bounds
            settings.m_fixedLowerBound = 3.0;
            settings.m_fixedUpperBound = 5.0;

            m_model.validateSettings(settings); // should not throw

            // should also be fine if the bounds are incorrect but the booleans are false
            settings.m_fixLowerBound = false;
            settings.m_fixUpperBound = false;
            settings.m_fixedLowerBound = 5.0;
            settings.m_fixedUpperBound = 3.0;

            m_model.validateSettings(settings); // should not throw
        }

        @Test
        void testValidatePassesWithSensibleSettings() throws InvalidSettingsException {
            var settings = new BinnerNodeSettings();

            settings.m_binningType = BinningType.EQUAL_WIDTH;
            settings.m_numberOfBins = 5;

            settings.m_fixLowerBound = true;
            settings.m_fixUpperBound = true;
            settings.m_fixedLowerBound = 0.0;
            settings.m_fixedUpperBound = 10.0;

            m_model.validateSettings(settings); // should not throw
        }
    }

}
