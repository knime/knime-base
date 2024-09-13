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
 *   27 Aug 2024 (Manuel Hotz, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.base.node.preproc.filter.row3.predicates;

import java.util.Optional;
import java.util.function.DoublePredicate;
import java.util.function.IntPredicate;
import java.util.function.LongPredicate;
import java.util.function.Predicate;

import org.knime.core.data.BooleanValue;
import org.knime.core.data.DataType;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.IntValue;
import org.knime.core.data.LongValue;
import org.knime.core.data.StringValue;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.LongCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.v2.RowRead;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.message.Message;
import org.knime.core.webui.node.dialog.defaultdialog.widget.dynamic.DynamicValuesInput;

/**
 * Factory for equality predicates ({@code EQ}, {@code NEQ}). The actual implementation is chosen based on the passsed
 * data type.
 *
 * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
 */
public abstract class EqualityPredicateFactory extends AbstractPredicateFactory {

    /**
     * {@code true} if the predicate should match "equals", {@code false} if it should match "not equals".
     */
    protected final boolean m_matchEqual;

    private EqualityPredicateFactory(final boolean matchEqual) {
        m_matchEqual = matchEqual;
    }

    /**
     * Creates an equality predicate factory for the given column data type.
     *
     * @param columnDataType data type of the column to compare
     * @param matchEqual {@code true} if the equality predicate should match "equals", {@code false} if it should match
     *            "not equals"
     * @return factory for the given data type, or empty optional if the data type is not supported
     */
    public static Optional<PredicateFactory> create(final DataType columnDataType, final boolean matchEqual) {
        final var preferredValueClass = columnDataType.getPreferredValueClass();

        if (preferredValueClass.equals(BooleanValue.class)) {
            // Boolean has IS_TRUE and IS_FALSE operators and does not use these predicate factories
            return Optional.empty();
        }

        final PredicateFactory predicateFactory;
        if (preferredValueClass.equals(LongValue.class)) {
            predicateFactory = new EqualityLongPredicateFactory(matchEqual);
        } else if (preferredValueClass.equals(IntValue.class)) {
            predicateFactory = new EqualityIntPredicateFactory(matchEqual);
        } else if (preferredValueClass.equals(DoubleValue.class)) {
            predicateFactory = new EqualityDoublePredicateFactory(matchEqual);
        } else if (preferredValueClass.equals(StringValue.class)) {
            predicateFactory = new EqualityStringPredicateFactory(matchEqual);
        } else {
            predicateFactory = new EqualityDataCellPredicateFactory(matchEqual);
        }
        return Optional.of(predicateFactory);
    }

    private static final class EqualityDataCellPredicateFactory extends EqualityPredicateFactory {

        private EqualityDataCellPredicateFactory(final boolean matchEqual) {
            super(matchEqual);
        }

        @Override
        public Predicate<RowRead> createPredicate(final int columnIndex, final DynamicValuesInput inputValues)
            throws InvalidSettingsException {
            final var refCell = getCellAtOrThrow(inputValues, 0);
            return m_matchEqual ? (rowRead -> rowRead.getValue(columnIndex).materializeDataCell().equals(refCell))
                : (rowRead -> !rowRead.getValue(columnIndex).materializeDataCell().equals(refCell));
        }

    }

    private static final class EqualityIntPredicateFactory extends EqualityPredicateFactory {

        private EqualityIntPredicateFactory(final boolean matchEqual) {
            super(matchEqual);
        }

        @Override
        public Predicate<RowRead> createPredicate(final int columnIndex, final DynamicValuesInput inputValues)
            throws InvalidSettingsException {
            final var refCell = getCellAtOrThrow(inputValues, 0);
            if (refCell instanceof IntCell intCell) {
                // comparing Integer column with int value
                final var ref = intCell.getIntValue();
                final var predicate = new IntValuePredicate(ref, m_matchEqual);
                LOGGER.debug("Creating equality predicate for Integer column with Integer reference value");
                return rowRead -> predicate.test(rowRead.<IntValue>getValue(columnIndex).getIntValue());
            }

            if (refCell instanceof LongCell longCell) {
                // comparing Integer column with long value
                final var ref = longCell.getLongValue();
                return comparingWithLongValue(columnIndex, ref);
            }

            if (refCell instanceof DoubleCell doubleCell) {
                // comparing Integer column with double value
                final var ref = doubleCell.getDoubleValue();
                return comparingWithDoubleValue(columnIndex, ref);
            }

            final var refCellType = refCell.getType();
            final var inEq = m_matchEqual ? "in" : "";
            throw Message.builder()
                .withSummary("Cannot compare column of type \"%s\" with a value of type \"%s\" for %sequality"
                    .formatted(IntCell.TYPE, refCellType, inEq))
                .addResolutions(
                    "Reconfigure the node to provide a reference value of \"%s\", \"%s\", or \"%s\" type."
                        .formatted(IntCell.TYPE, LongCell.TYPE, DoubleCell.TYPE),
                    "Convert the input column to \"%s\" using a converter node, e.g. an expression node"
                        .formatted(refCellType))
                .build().orElseThrow().toInvalidSettingsException();
        }

        private Predicate<RowRead> comparingWithLongValue(final int columnIndex, final long ref) {
            // two cases where we can already determine that the predicate will never/always match
            if (ref > Integer.MAX_VALUE) {
                LOGGER.debug("Creating static predicate for Integer column with Long reference value for "
                    + "ref > Integer.MAX_VALUE");
                return m_matchEqual ? PredicateFactory.ALWAYS_FALSE : PredicateFactory.ALWAYS_TRUE;
            }
            if (ref < Integer.MIN_VALUE) {
                LOGGER.debug("Creating static predicate for Integer column with Long reference value for "
                    + "ref < Integer.MIN_VALUE");
                return m_matchEqual ? PredicateFactory.ALWAYS_TRUE : PredicateFactory.ALWAYS_FALSE;
            }

            // long value is inside of the int domain, so matches are possible
            final var predicate = new IntValuePredicate((int)ref, m_matchEqual);
            LOGGER.debug("Creating equality predicate for Integer column with Long reference value");
            return rowRead -> predicate.test(rowRead.<IntValue>getValue(columnIndex).getIntValue());
        }

        private Predicate<RowRead> comparingWithDoubleValue(final int columnIndex, final double ref) {
            // two cases where we can already determine that the predicate will never/always match
            if (ref > Integer.MAX_VALUE) {
                LOGGER.debug("Creating static predicate for Integer column with Double reference value for "
                    + "ref > Integer.MAX_VALUE");
                return m_matchEqual ? PredicateFactory.ALWAYS_FALSE : PredicateFactory.ALWAYS_TRUE;
            }
            if (ref < Integer.MIN_VALUE) {
                LOGGER.debug("Creating static predicate for Integer column with Double reference value for "
                    + "ref < Integer.MIN_VALUE");
                return m_matchEqual ? PredicateFactory.ALWAYS_TRUE : PredicateFactory.ALWAYS_FALSE;
            }

            // double value is inside of the int domain, so matches are possible
            LOGGER.debug("Creating equality predicate for Integer column with Double reference value");
            final var predicate = new DoubleValuePredicate(ref, m_matchEqual);
            return rowRead -> predicate.test(rowRead.<IntValue>getValue(columnIndex).getIntValue());
        }

    }

    private static final class EqualityLongPredicateFactory extends EqualityPredicateFactory {

        private EqualityLongPredicateFactory(final boolean matchEqual) {
            super(matchEqual);
        }

        @Override
        public Predicate<RowRead> createPredicate(final int columnIndex, final DynamicValuesInput inputValues)
            throws InvalidSettingsException {
            final var refCell = getCellAtOrThrow(inputValues, 0);
            long ref;
            if (refCell instanceof IntCell intCell) {
                LOGGER.debug("Creating equality predicate for Long column with Integer reference value");
                ref = intCell.getIntValue();
            } else if (refCell instanceof LongCell longCell) {
                LOGGER.debug("Creating equality predicate for Long column with Long reference value");
                ref = longCell.getLongValue();
            } else {
                final var refCellType = refCell.getType();
                final var inEq = m_matchEqual ? "in" : "";
                throw Message.builder()
                    .withSummary("Cannot compare column of type \"%s\" with a value of type \"%s\" for %sequality"
                        .formatted(LongCell.TYPE, refCellType, inEq))
                    .addResolutions(
                        "Reconfigure the node to provide a reference value of \"%s\" or \"%s\" type."
                            .formatted(IntCell.TYPE, LongCell.TYPE),
                        "Convert the input column to \"%s\" using a converter node, e.g. an expression node"
                            .formatted(refCellType))
                    .build().orElseThrow().toInvalidSettingsException();
            }
            final var predicate = new LongValuePredicate(ref, m_matchEqual);
            return rowRead -> predicate.test(rowRead.<LongValue> getValue(columnIndex).getLongValue());
        }
    }

    private static final class EqualityDoublePredicateFactory extends EqualityPredicateFactory {

        private EqualityDoublePredicateFactory(final boolean matchEqual) {
            super(matchEqual);
        }

        @Override
        public Predicate<RowRead> createPredicate(final int columnIndex, final DynamicValuesInput inputValues)
            throws InvalidSettingsException {
            final var refCell = getCellAtOrThrow(inputValues, 0);
            double ref;
            if (refCell instanceof IntCell intCell) {
                LOGGER.debug("Creating equality predicate for Double column with Integer reference value");
                ref = intCell.getIntValue();
            } else if (refCell instanceof DoubleCell doubleCell) {
                LOGGER.debug("Creating equality predicate for Double column with Double reference value");
                ref = doubleCell.getDoubleValue();
            } else {
                final var refCellType = refCell.getType();
                final var inEq = m_matchEqual ? "in" : "";
                throw Message.builder()
                    .withSummary("Cannot compare column of type \"%s\" with a value of type \"%s\" for %sequality"
                        .formatted(DoubleCell.TYPE, refCellType, inEq))
                    .addResolutions(
                        "Reconfigure the node to provide a reference value of \"%s\" or \"%s\" type."
                            .formatted(IntCell.TYPE, DoubleCell.TYPE),
                        "Convert the input column to \"%s\" using a converter node, e.g. an expression node"
                            .formatted(refCellType))
                    .build().orElseThrow().toInvalidSettingsException();
            }
            final var predicate = new DoubleValuePredicate(ref, m_matchEqual);
            return rowRead -> predicate.test(rowRead.<DoubleValue> getValue(columnIndex).getDoubleValue());
        }

    }

    private static final class EqualityStringPredicateFactory extends EqualityPredicateFactory {

        private EqualityStringPredicateFactory(final boolean matchEqual) {
            super(matchEqual);
        }

        @Override
        public Predicate<RowRead> createPredicate(final int columnIndex, final DynamicValuesInput inputValues)
            throws InvalidSettingsException {

            final var isRowKey = columnIndex < 0;
            final var refCell = getCellAtOrThrow(inputValues, 0);
            final var refCellType = refCell.getType();
            if (!refCellType.isCompatible(StringValue.class)) {
                final var inEq = m_matchEqual ? "in" : "";
                final var keyOrCol = isRowKey ? "RowID" : "column";
                final var builder = Message.builder()
                    .withSummary("Cannot compare %s of type \"%s\" with a value of type \"%s\" for %sequality"
                        .formatted(keyOrCol, StringCell.TYPE, refCellType, inEq))
                    .addResolutions(
                        "Reconfigure the node to provide a reference value of \"%s\" type.".formatted(StringCell.TYPE));
                if (!isRowKey) {
                    builder.addResolutions(
                        "Convert the input column to \"%s\" using a converter node, e.g. an expression node"
                            .formatted(refCellType));
                }
                throw builder.build().orElseThrow().toInvalidSettingsException();
            }
            final var refValue = ((StringValue)refCell).getStringValue();
            final var isCaseSensitive = inputValues.isStringMatchCaseSensitive(0);
            final var predicate = StringPredicate.equality(refValue, isCaseSensitive);
            if (isRowKey) {
                LOGGER.debug("Creating equality predicate for RowID with String reference value");
                return rowRead -> m_matchEqual == predicate.test(rowRead.getRowKey().getString());
            }
            LOGGER.debug("Creating equality predicate for String column with String reference value");
            return rowRead -> //
                m_matchEqual == predicate.test(rowRead.<StringValue> getValue(columnIndex).getStringValue());
        }

    }

    private static final class IntValuePredicate implements IntPredicate {

        private final IntPredicate m_predicate;

        private IntValuePredicate(final int ref, final boolean matchEqual) {
            m_predicate = matchEqual ? (i -> i == ref) : (i -> i != ref);
        }

        @Override
        public boolean test(final int value) {
            return m_predicate.test(value);
        }

    }

    private static final class LongValuePredicate implements LongPredicate {

        private final LongPredicate m_predicate;

        private LongValuePredicate(final long ref, final boolean matchEqual) {
            m_predicate = matchEqual ? (l -> l == ref) : (l -> l != ref);
        }

        @Override
        public boolean test(final long value) {
            return m_predicate.test(value);
        }

    }

    private static final class DoubleValuePredicate implements DoublePredicate {

        private DoublePredicate m_predicate;

        private DoubleValuePredicate(final double ref, final boolean matchEqual) {
            m_predicate = matchEqual ? (d -> Double.doubleToLongBits(d) == Double.doubleToLongBits(ref))
                : (d -> Double.doubleToLongBits(d) != Double.doubleToLongBits(ref));
        }

        @Override
        public boolean test(final double value) {
            return m_predicate.test(value);
        }

    }

}
