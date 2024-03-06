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
 * ------------------------------------------------------------------------
 *
 * History
 *   07.05.2012 (kilian): created
 */
package org.knime.base.node.preproc.rounddouble;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.function.Function;

import org.knime.base.node.preproc.rounddouble.RoundDoubleNodeSettings.NumberMode;
import org.knime.base.node.preproc.rounddouble.RoundDoubleNodeSettings.OutputMode;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataType;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.container.AbstractCellFactory;
import org.knime.core.data.def.BooleanCell;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.LongCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.NodeLogger;

/**
 * Creating data cells containing the rounded values.
 *
 * @author Kilian Thiel, KNIME.com, Berlin, Germany
 * @author Kai Franze, KNIME GmbH, Germany
 */
final class RoundDoubleCellFactory extends AbstractCellFactory {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(RoundDoubleCellFactory.class);

    /**
     * If NumberMode.DECIMALS, it's the number of decimal places, if NumberMode.SIGNIFICANT_DIGITS, it's the number of
     * significant digits.
     */
    private final int m_precision;

    /**
     * How to round a number, can be one of {UP, HALF_UP, HALF_EVEN, HALF_DOWN, CEILING, FLOOR, DOWN}.
     */
    private final RoundingMode m_roundingMode;

    /**
     * Determines the output number format, can be one of {DECIMALS, SIGNIFICANT_DIGITS, INTEGER}.
     */
    private final NumberMode m_numberMode;

    /**
     * Determines the output cell data type, can be one of {AUTO, DOUBLE, STANDARD_STRING, PLAIN_STRING,
     * ENGINEERING_STRING}.
     */
    private final OutputMode m_outputMode;

    /*
     * The column indices to round
     */
    private final int[] m_colIndexToRound;

    private static final BigDecimal INT_MIN_VALUE = BigDecimal.valueOf(Integer.MIN_VALUE);

    private static final BigDecimal INT_MAX_VALUE = BigDecimal.valueOf(Integer.MAX_VALUE);

    private static final BigDecimal LONG_MIN_VALUE = BigDecimal.valueOf(Long.MIN_VALUE);

    private static final BigDecimal LONG_MAX_VALUE = BigDecimal.valueOf(Long.MAX_VALUE);

    /**
     * Creates instance of <code>RoundDoubleCellFactory</code> with specified precision.
     *
     * @param precision The decimal place to round to.
     * @param numberMode The mode of the precision to round to (decimal place, significant figures).
     * @param roundingMode The mode to round the double values. additional column or if the old values will be replaced.
     * @param outputMode Specifies whether rounded values will be represented as strings or doubles.
     * @param colIndexToRound The indices of the columns containing the values to round.
     * @param newColSpecs The specs of the new columns (replaced or appended).
     */
    RoundDoubleCellFactory(final int precision, final NumberMode numberMode, final RoundingMode roundingMode,
        final OutputMode outputMode, final int[] colIndexToRound, final DataColumnSpec[] newColSpecs) {
        super(newColSpecs);
        if (roundingMode == null) {
            throw new IllegalArgumentException("Rounding mode is missing.");
        }
        if (colIndexToRound == null) {
            throw new IllegalArgumentException("Column indices to round are missing.");
        }
        if (numberMode == null) {
            throw new IllegalArgumentException("Number mode is missing.");
        }
        m_precision = precision;
        m_roundingMode = roundingMode;
        m_outputMode = outputMode;
        m_colIndexToRound = colIndexToRound;
        m_numberMode = numberMode;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DataCell[] getCells(final DataRow row) {
        final var newCells = new DataCell[m_colIndexToRound.length];
        final var noCols = row.getNumCells();
        var nextIndexToRound = 0;
        var currIndexToRound = -1;

        // walk through all columns and round if specified
        for (var i = 0; i < noCols; i++) {

            // get next index of column to round (if still columns to round
            // are available).
            if (nextIndexToRound < m_colIndexToRound.length) {
                currIndexToRound = m_colIndexToRound[nextIndexToRound];
            }

            // if value needs to be rounded
            if (i == currIndexToRound) {
                final var outCell = round(row.getCell(i), m_outputMode, m_numberMode, m_roundingMode, m_precision);
                // increment index of included column indices
                newCells[nextIndexToRound] = outCell;
                nextIndexToRound = nextIndexToRound + 1;
            }
        }

        return newCells;
    }

    private static DataCell round(final DataCell inCell, final OutputMode outputMode, final NumberMode numberMode,
        final RoundingMode roundingMode, final int precision) {
        final var inType = inCell.getType();
        final DataCell outCell;
        if (inCell.isMissing()) {
            outCell = DataType.getMissingCell();
        } else {
            final var value = ((DoubleValue)inCell).getDoubleValue();

            // check for infinity or nan
            if (Double.isInfinite(value) || Double.isNaN(value)) {
                switch (outputMode) {
                    case AUTO:
                        outCell = inCell; // Just keep the original cell
                        break;
                    case DOUBLE:
                        // this isn't nice as we shouldn't have NaN and Inf in the input ...
                        // but that's a problem somewhere else
                        outCell = new DoubleCell(value);
                        break;
                    default:
                        outCell = new StringCell(Double.toString(value));
                }
            } else {
                // do not use constructor, see AP-7016
                final var bd = BigDecimal.valueOf(value).stripTrailingZeros();
                final var rounded = switch (numberMode) {
                    case DECIMALS -> bd.setScale(precision, roundingMode);
                    case SIGNIFICANT_DIGITS -> bd.round(new MathContext(precision, roundingMode));
                    case INTEGER -> bd.setScale(0, roundingMode);
                };
                outCell = applyOutputMode(rounded, outputMode, inType, numberMode);
            }
        }
        return outCell;
    }

    private static DataCell applyOutputMode(final BigDecimal rounded, final OutputMode outputMode,
        final DataType inType, final NumberMode numberMode) {
        return switch (outputMode) {
            case AUTO -> numberMode == NumberMode.INTEGER //
                ? computeIntCellWithoutOverflow(rounded) //
                : inferOutputDataCellFromInputType(rounded, inType);
            case DOUBLE -> {
                final var roundedValue = rounded.doubleValue();
                yield Double.isNaN(roundedValue) ? DataType.getMissingCell() : new DoubleCell(roundedValue);
            }
            case STANDARD_STRING -> new StringCell(rounded.toString());
            case PLAIN_STRING -> new StringCell(rounded.toPlainString());
            case ENGINEERING_STRING -> new StringCell(rounded.toEngineeringString());
        };
    }

    private static DataCell computeIntCellWithoutOverflow(final BigDecimal rounded) {
        return computeCellWithoutOverflow(rounded, INT_MIN_VALUE, INT_MAX_VALUE, "Integer",
            bd -> IntCell.IntCellFactory.create(bd.intValue()));
    }

    private static DataCell computeCellWithoutOverflow(final BigDecimal rounded, final BigDecimal min,
        final BigDecimal max, final String type, final Function<BigDecimal, DataCell> bdToCell) {
        if (rounded.compareTo(min) < 0) {
            LOGGER.warn("Cannot cast <%s> to %s, since it is smaller than %s.MIN_VALUE".formatted(rounded, type, type));
            return DataType.getMissingCell();
        }
        if (rounded.compareTo(max) > 0) {
            LOGGER.warn("Cannot cast <%s> to %s, since it is larger than %s.MAX_VALUE".formatted(rounded, type, type));
            return DataType.getMissingCell();
        }
        return bdToCell.apply(rounded);
    }

    /**
     * Automatically detects the {@link DataType} of the output cell and creates an instance of it.
     *
     * @return Defaults to {@link DoubleCell} unless the input data type is {@link IntCell}, {@link LongCell},
     *         {@link BooleanCell}.
     */
    private static DataCell inferOutputDataCellFromInputType(final BigDecimal rounded, final DataType inType) {
        if (inType.equals(IntCell.TYPE)) {
            return computeIntCellWithoutOverflow(rounded);
        }
        if (inType.equals(LongCell.TYPE)) {
            return computeCellWithoutOverflow(rounded, LONG_MIN_VALUE, LONG_MAX_VALUE, "Long",
                bd -> LongCell.LongCellFactory.create(bd.longValue()));
        }
        if (inType.equals(BooleanCell.TYPE)) {
            // Only `false` if `p == 0`, `true` otherwise; similar to JS behavior.
            return BooleanCell.BooleanCellFactory.create(rounded.compareTo(BigDecimal.ZERO) != 0);
        }
        return DoubleCell.DoubleCellFactory.create(rounded.doubleValue());
    }

}
