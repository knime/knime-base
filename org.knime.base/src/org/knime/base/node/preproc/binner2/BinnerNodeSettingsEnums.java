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
 *   Jul 18, 2025 (david): created
 */
package org.knime.base.node.preproc.binner2;

import java.math.RoundingMode;

import org.knime.core.util.binning.BinningSettings;
import org.knime.core.util.binning.BinningSettings.BinNamingUtils.BinNamingNumberFormatter;
import org.knime.core.util.binning.BinningSettings.BinNamingUtils.BinNamingNumberFormatterUtils;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.widget.choices.Label;
import org.knime.node.parameters.widget.choices.ValueSwitchWidget;
import org.knime.node.parameters.widget.number.NumberInputWidget;
import org.knime.node.parameters.widget.number.NumberInputWidgetValidation.MinValidation.IsPositiveIntegerValidation;

/**
 * Enums used in the settings for this node. Most of them are duplicates of enums in {@link BinningSettings}, but they
 * must be copied here in order to add the {@link Label} annotations required for the UI.
 *
 * @author David Hickey, TNG Technology Consulting GmbH
 */
final class BinnerNodeSettingsEnums {

    private BinnerNodeSettingsEnums() {
        // utility class
    }

    enum BinNaming {
            @Label(value = BinningSettings.BinNamingUtils.NAME_NUMBERED,
                description = BinningSettings.BinNamingUtils.DESC_NUMBERED)
            NUMBERED, //
            @Label(value = BinningSettings.BinNamingUtils.NAME_BORDERS,
                description = BinningSettings.BinNamingUtils.DESC_BORDERS)
            BORDERS, //
            @Label(value = BinningSettings.BinNamingUtils.NAME_MIDPOINTS,
                description = BinningSettings.BinNamingUtils.DESC_MIDPOINTS)
            MIDPOINTS;
    }

    enum BinningType {
            @Label(value = BinningSettings.BinningMethod.EqualWidth.NAME_EQUAL_WIDTH,
                description = BinningSettings.BinningMethod.EqualWidth.DESC_EQUAL_WIDTH)
            EQUAL_WIDTH, //

            @Label(value = BinningSettings.BinningMethod.EqualCount.NAME_EQUAL_FREQUENCY,
                description = BinningSettings.BinningMethod.EqualCount.DESC_EQUAL_FREQUENCY)
            EQUAL_FREQUENCY, //

            @Label(value = BinningSettings.BinningMethod.FixedBoundaries.NAME_CUSTOM_CUTOFFS,
                description = BinningSettings.BinningMethod.FixedBoundaries.DESC_CUSTOM_CUTOFFS)
            CUSTOM_CUTOFFS, //

            @Label(value = BinningSettings.BinningMethod.FixedQuantiles.NAME_CUSTOM_QUANTILES,
                description = BinningSettings.BinningMethod.FixedQuantiles.DESC_CUSTOM_QUANTILES)
            CUSTOM_QUANTILES;

    }

    enum RoundingDirection {
            @Label(value = BinNamingNumberFormatterUtils.RoundingModeUtils.NAME_UP,
                description = BinNamingNumberFormatterUtils.RoundingModeUtils.DESC_UP)
            UP(RoundingMode.UP), //

            @Label(value = BinNamingNumberFormatterUtils.RoundingModeUtils.NAME_DOWN,
                description = BinNamingNumberFormatterUtils.RoundingModeUtils.DESC_DOWN)
            DOWN(RoundingMode.DOWN), //

            @Label(value = BinNamingNumberFormatterUtils.RoundingModeUtils.NAME_CEILING,
                description = BinNamingNumberFormatterUtils.RoundingModeUtils.DESC_CEILING)
            CEILING(RoundingMode.CEILING), //

            @Label(value = BinNamingNumberFormatterUtils.RoundingModeUtils.NAME_FLOOR,
                description = BinNamingNumberFormatterUtils.RoundingModeUtils.DESC_FLOOR)
            FLOOR(RoundingMode.FLOOR), //

            @Label(value = BinNamingNumberFormatterUtils.RoundingModeUtils.NAME_HALF_UP,
                description = BinNamingNumberFormatterUtils.RoundingModeUtils.DESC_HALF_UP)
            HALF_UP(RoundingMode.HALF_UP), //

            @Label(value = BinNamingNumberFormatterUtils.RoundingModeUtils.NAME_HALF_DOWN,
                description = BinNamingNumberFormatterUtils.RoundingModeUtils.DESC_HALF_DOWN)
            HALF_DOWN(RoundingMode.HALF_DOWN), //

            @Label(value = BinNamingNumberFormatterUtils.RoundingModeUtils.NAME_HALF_EVEN,
                description = BinNamingNumberFormatterUtils.RoundingModeUtils.DESC_HALF_EVEN)
            HALF_EVEN(RoundingMode.HALF_EVEN);

        final RoundingMode m_baseValue;

        RoundingDirection(final RoundingMode value) {
            m_baseValue = value;
        }
    }

    enum PrecisionMode {
            @Label(value = BinNamingNumberFormatterUtils.PrecisionMode.NAME_DECIMAL_PLACES,
                description = BinNamingNumberFormatterUtils.PrecisionMode.DESC_DECIMAL_PLACES)
            DECIMAL_PLACES(BinNamingNumberFormatterUtils.PrecisionMode.DECIMAL_PLACES), //

            @Label(value = BinNamingNumberFormatterUtils.PrecisionMode.NAME_SIGNIFICANT_FIGURES,
                description = BinNamingNumberFormatterUtils.PrecisionMode.DESC_SIGNIFICANT_FIGURES)
            SIGNIFICANT_FIGURES(BinNamingNumberFormatterUtils.PrecisionMode.SIGNIFICANT_FIGURES);

        final BinNamingNumberFormatterUtils.PrecisionMode m_baseValue;

        PrecisionMode(final BinNamingNumberFormatterUtils.PrecisionMode value) {
            m_baseValue = value;
        }
    }

    enum NumberFormat {
            @Label(value = BinNamingNumberFormatterUtils.CustomNumberFormat.NAME_STANDARD_STRING,
                description = BinNamingNumberFormatterUtils.CustomNumberFormat.DESC_STANDARD_STRING)
            STANDARD_STRING(BinNamingNumberFormatterUtils.CustomNumberFormat.STANDARD_STRING), //
            @Label(value = BinNamingNumberFormatterUtils.CustomNumberFormat.NAME_PLAIN_STRING,
                description = BinNamingNumberFormatterUtils.CustomNumberFormat.DESC_PLAIN_STRING)
            PLAIN_STRING(BinNamingNumberFormatterUtils.CustomNumberFormat.PLAIN_STRING), //
            @Label(value = BinNamingNumberFormatterUtils.CustomNumberFormat.NAME_ENGINEERING_STRING,
                description = BinNamingNumberFormatterUtils.CustomNumberFormat.DESC_ENGINEERING_STRING)
            ENGINEERING_STRING(BinNamingNumberFormatterUtils.CustomNumberFormat.ENGINEERING_STRING);

        final BinNamingNumberFormatterUtils.CustomNumberFormat m_baseValue;

        NumberFormat(final BinNamingNumberFormatterUtils.CustomNumberFormat value) {
            m_baseValue = value;
        }
    }

    enum BinBoundaryExactMatchBehaviour {
            @Label(value = BinningSettings.BinBoundary.BinBoundaryExactMatchBehaviour.NAME_TO_LOWER_BIN,
                description = BinningSettings.BinBoundary.BinBoundaryExactMatchBehaviour.DESC_TO_LOWER_BIN)
            TO_LOWER_BIN(BinningSettings.BinBoundary.BinBoundaryExactMatchBehaviour.TO_LOWER_BIN), //
            @Label(value = BinningSettings.BinBoundary.BinBoundaryExactMatchBehaviour.NAME_TO_UPPER_BIN,
                description = BinningSettings.BinBoundary.BinBoundaryExactMatchBehaviour.DESC_TO_UPPER_BIN)
            TO_UPPER_BIN(BinningSettings.BinBoundary.BinBoundaryExactMatchBehaviour.TO_UPPER_BIN);

        final BinningSettings.BinBoundary.BinBoundaryExactMatchBehaviour m_baseValue;

        BinBoundaryExactMatchBehaviour(final BinningSettings.BinBoundary.BinBoundaryExactMatchBehaviour value) {
            m_baseValue = value;
        }
    }

    static final class NumberFormatSettingsGroup implements NodeParameters {

        @Widget(title = "Number format", description = """
                The format used for numbers in the bins with regard to \
                how fractions and exponents are displayed.
                """)
        @ValueSwitchWidget
        NumberFormat m_numberFormat = NumberFormat.STANDARD_STRING;

        @Widget(title = "Precision", description = """
                The number of digits to use for the precision of \
                numbers in the bins.
                """)
        @NumberInputWidget(minValidation = IsPositiveIntegerValidation.class)
        int m_precision = 3;

        @Widget(title = "Precision mode", description = """
                Whether to use a fixed number of decimal places \
                or a fixed number of significant figures when \
                rounding numbers in the bins.
                """)
        @ValueSwitchWidget
        PrecisionMode m_precisionMode = PrecisionMode.DECIMAL_PLACES;

        @Widget(title = "Rounding mode", description = """
                The rounding mode to use when rounding numbers \
                in the bins.
                """)
        RoundingDirection m_roundingMode = RoundingDirection.HALF_UP;

        BinNamingNumberFormatter toFormatter() {
            return BinNamingNumberFormatterUtils.createCustomNumberFormatter(m_numberFormat.m_baseValue, //
                m_precision, //
                m_precisionMode.m_baseValue, //
                m_roundingMode.m_baseValue);

        }
    }
}
