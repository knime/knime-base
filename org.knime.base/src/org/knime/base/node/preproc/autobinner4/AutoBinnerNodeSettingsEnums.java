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
package org.knime.base.node.preproc.autobinner4;

import org.knime.core.util.binning.auto.AutoBinningSettings;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.widget.choices.Label;
import org.knime.node.parameters.widget.choices.ValueSwitchWidget;
import org.knime.node.parameters.widget.number.NumberInputWidget;
import org.knime.node.parameters.widget.number.NumberInputWidgetValidation.MinValidation.IsPositiveIntegerValidation;

/**
 * Enums used in the settings for this node. Most of them are duplicates of enums in {@link AutoBinningSettings}, but
 * they must be copied here in order to add the {@link Label} annotations required for the UI.
 *
 * @author David Hickey, TNG Technology Consulting GmbH
 */
final class AutoBinnerNodeSettingsEnums {

    private AutoBinnerNodeSettingsEnums() {
        // utility class
    }

    enum BinNaming {
            @Label(value = AutoBinningSettings.BinNaming.NAME_NUMBERED,
                description = AutoBinningSettings.BinNaming.DESC_NUMBERED)
            NUMBERED(AutoBinningSettings.BinNaming.NUMBERED), //
            @Label(value = AutoBinningSettings.BinNaming.NAME_BORDERS,
                description = AutoBinningSettings.BinNaming.DESC_BORDERS)
            BORDERS(AutoBinningSettings.BinNaming.BORDERS), //
            @Label(value = AutoBinningSettings.BinNaming.NAME_MIDPOINTS,
                description = AutoBinningSettings.BinNaming.DESC_MIDPOINTS)
            MIDPOINTS(AutoBinningSettings.BinNaming.MIDPOINTS);

        final AutoBinningSettings.BinNaming m_binName;

        BinNaming(final AutoBinningSettings.BinNaming binNaming) {
            m_binName = binNaming;
        }
    }

    enum BinningType {
            @Label(value = AutoBinningSettings.BinningType.NAME_EQUAL_WIDTH,
                description = AutoBinningSettings.BinningType.DESC_EQUAL_WIDTH)
            EQUAL_WIDTH(AutoBinningSettings.BinningType.EQUAL_WIDTH), //

            @Label(value = AutoBinningSettings.BinningType.NAME_EQUAL_FREQUENCY,
                description = AutoBinningSettings.BinningType.DESC_EQUAL_FREQUENCY)
            EQUAL_FREQUENCY(AutoBinningSettings.BinningType.EQUAL_FREQUENCY), //

            @Label(value = AutoBinningSettings.BinningType.NAME_CUSTOM_CUTOFFS,
                description = AutoBinningSettings.BinningType.DESC_CUSTOM_CUTOFFS)
            CUSTOM_CUTOFFS(AutoBinningSettings.BinningType.CUSTOM_CUTOFFS), //

            @Label(value = AutoBinningSettings.BinningType.NAME_CUSTOM_QUANTILES,
                description = AutoBinningSettings.BinningType.DESC_CUSTOM_QUANTILES)
            CUSTOM_QUANTILES(AutoBinningSettings.BinningType.CUSTOM_QUANTILES);

        final AutoBinningSettings.BinningType m_baseValue;

        BinningType(final AutoBinningSettings.BinningType value) {
            m_baseValue = value;
        }
    }

    enum RoundingDirection {
            @Label(value = AutoBinningSettings.NumberFormatSettingsGroup.RoundingDirection.NAME_UP,
                description = AutoBinningSettings.NumberFormatSettingsGroup.RoundingDirection.DESC_UP)
            UP(AutoBinningSettings.NumberFormatSettingsGroup.RoundingDirection.UP), //

            @Label(value = AutoBinningSettings.NumberFormatSettingsGroup.RoundingDirection.NAME_DOWN,
                description = AutoBinningSettings.NumberFormatSettingsGroup.RoundingDirection.DESC_DOWN)
            DOWN(AutoBinningSettings.NumberFormatSettingsGroup.RoundingDirection.DOWN), //

            @Label(value = AutoBinningSettings.NumberFormatSettingsGroup.RoundingDirection.NAME_CEILING,
                description = AutoBinningSettings.NumberFormatSettingsGroup.RoundingDirection.DESC_CEILING)
            CEILING(AutoBinningSettings.NumberFormatSettingsGroup.RoundingDirection.CEILING), //

            @Label(value = AutoBinningSettings.NumberFormatSettingsGroup.RoundingDirection.NAME_FLOOR,
                description = AutoBinningSettings.NumberFormatSettingsGroup.RoundingDirection.DESC_FLOOR)
            FLOOR(AutoBinningSettings.NumberFormatSettingsGroup.RoundingDirection.FLOOR), //

            @Label(value = AutoBinningSettings.NumberFormatSettingsGroup.RoundingDirection.NAME_HALF_UP,
                description = AutoBinningSettings.NumberFormatSettingsGroup.RoundingDirection.DESC_HALF_UP)
            HALF_UP(AutoBinningSettings.NumberFormatSettingsGroup.RoundingDirection.HALF_UP), //

            @Label(value = AutoBinningSettings.NumberFormatSettingsGroup.RoundingDirection.NAME_HALF_DOWN,
                description = AutoBinningSettings.NumberFormatSettingsGroup.RoundingDirection.DESC_HALF_DOWN)
            HALF_DOWN(AutoBinningSettings.NumberFormatSettingsGroup.RoundingDirection.HALF_DOWN), //

            @Label(value = AutoBinningSettings.NumberFormatSettingsGroup.RoundingDirection.NAME_HALF_EVEN,
                description = AutoBinningSettings.NumberFormatSettingsGroup.RoundingDirection.DESC_HALF_EVEN)
            HALF_EVEN(AutoBinningSettings.NumberFormatSettingsGroup.RoundingDirection.HALF_EVEN);

        final AutoBinningSettings.NumberFormatSettingsGroup.RoundingDirection m_baseValue;

        RoundingDirection(final AutoBinningSettings.NumberFormatSettingsGroup.RoundingDirection value) {
            m_baseValue = value;
        }
    }

    enum PrecisionMode {
            @Label(value = AutoBinningSettings.NumberFormatSettingsGroup.PrecisionMode.NAME_DECIMAL_PLACES,
                description = AutoBinningSettings.NumberFormatSettingsGroup.PrecisionMode.DESC_DECIMAL_PLACES)
            DECIMAL_PLACES(AutoBinningSettings.NumberFormatSettingsGroup.PrecisionMode.DECIMAL_PLACES), //

            @Label(value = AutoBinningSettings.NumberFormatSettingsGroup.PrecisionMode.NAME_SIGNIFICANT_FIGURES,
                description = AutoBinningSettings.NumberFormatSettingsGroup.PrecisionMode.DESC_SIGNIFICANT_FIGURES)
            SIGNIFICANT_FIGURES(AutoBinningSettings.NumberFormatSettingsGroup.PrecisionMode.SIGNIFICANT_FIGURES);

        final AutoBinningSettings.NumberFormatSettingsGroup.PrecisionMode m_baseValue;

        PrecisionMode(final AutoBinningSettings.NumberFormatSettingsGroup.PrecisionMode value) {
            m_baseValue = value;
        }
    }

    enum NumberFormat {
            @Label(value = AutoBinningSettings.NumberFormatSettingsGroup.NumberFormat.NAME_STANDARD_STRING,
                description = AutoBinningSettings.NumberFormatSettingsGroup.NumberFormat.DESC_STANDARD_STRING)
            STANDARD_STRING(AutoBinningSettings.NumberFormatSettingsGroup.NumberFormat.STANDARD_STRING), //
            @Label(value = AutoBinningSettings.NumberFormatSettingsGroup.NumberFormat.NAME_PLAIN_STRING,
                description = AutoBinningSettings.NumberFormatSettingsGroup.NumberFormat.DESC_PLAIN_STRING)
            PLAIN_STRING(AutoBinningSettings.NumberFormatSettingsGroup.NumberFormat.PLAIN_STRING), //
            @Label(value = AutoBinningSettings.NumberFormatSettingsGroup.NumberFormat.NAME_ENGINEERING_STRING,
                description = AutoBinningSettings.NumberFormatSettingsGroup.NumberFormat.DESC_ENGINEERING_STRING)
            ENGINEERING_STRING(AutoBinningSettings.NumberFormatSettingsGroup.NumberFormat.ENGINEERING_STRING);

        final AutoBinningSettings.NumberFormatSettingsGroup.NumberFormat m_baseValue;

        NumberFormat(final AutoBinningSettings.NumberFormatSettingsGroup.NumberFormat value) {
            m_baseValue = value;
        }
    }

    enum BinBoundaryExactMatchBehaviour {
            @Label(value = AutoBinningSettings.BinBoundaryExactMatchBehaviour.NAME_TO_LOWER_BIN,
                description = AutoBinningSettings.BinBoundaryExactMatchBehaviour.DESC_TO_LOWER_BIN)
            TO_LOWER_BIN(AutoBinningSettings.BinBoundaryExactMatchBehaviour.TO_LOWER_BIN), //
            @Label(value = AutoBinningSettings.BinBoundaryExactMatchBehaviour.NAME_TO_UPPER_BIN,
                description = AutoBinningSettings.BinBoundaryExactMatchBehaviour.DESC_TO_UPPER_BIN)
            TO_UPPER_BIN(AutoBinningSettings.BinBoundaryExactMatchBehaviour.TO_UPPER_BIN);

        final AutoBinningSettings.BinBoundaryExactMatchBehaviour m_baseValue;

        BinBoundaryExactMatchBehaviour(final AutoBinningSettings.BinBoundaryExactMatchBehaviour value) {
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

        AutoBinningSettings.NumberFormatSettingsGroup toBaseValue() {
            return new AutoBinningSettings.NumberFormatSettingsGroup( //
                m_numberFormat.m_baseValue, //
                m_precision, //
                m_precisionMode.m_baseValue, //
                m_roundingMode.m_baseValue //
            );
        }
    }
}
