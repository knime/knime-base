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
 *   Jun 7, 2019 (Perla Gjoka): created
 */
package org.knime.base.node.preproc.filter.row2.operator;

import java.util.function.Function;
import java.util.function.Predicate;

import org.knime.base.data.filter.row.dialog.OperatorValidation;
import org.knime.base.data.filter.row.dialog.panel.PatternMatchPanel;
import org.knime.base.data.filter.row.dialog.panel.SingleFieldPanel;
import org.knime.base.data.filter.row.dialog.panel.SingleNumericFieldPanel;
import org.knime.base.data.filter.row.dialog.panel.TwoNumericFieldsPanel;
import org.knime.base.data.filter.row.dialog.registry.AbstractOperatorRegistry;
import org.knime.base.data.filter.row.dialog.registry.OperatorValue;
import org.knime.base.data.filter.row.dialog.validation.SingleOperandValidation;
import org.knime.core.data.DataCell;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.def.BooleanCell;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.util.CheckUtils;

/**
 * Registry for all operators supported by the KNIME Row Filter node.
 *
 * @author Perla Gjoka, KNIME GmbH, Konstanz, Germany
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
public final class KnimeRowFilterOperatorRegistry extends AbstractOperatorRegistry<KnimeOperatorFunction> {

    private static final String DOUBLE_TYPE_ERROR_TEMPLATE = "The provided parameter %s is not a DoubleValue.";

    private static final Predicate<DataCell> IS_MISSING = DataCell::isMissing;

    private static final Predicate<DataCell> IS_NOT_MISSING = c -> !c.isMissing();

    private static final KnimeRowFilterOperatorRegistry INSTANCE = new KnimeRowFilterOperatorRegistry();

    /** @return registry singleton */
    public static KnimeRowFilterOperatorRegistry getInstance() {
        return INSTANCE;
    }

    private KnimeRowFilterOperatorRegistry() {
        registerGeneralOperators();
        registerOperatorsForNumericalColumns();
        registerOperatorsForStringColumns();
        registerOperatorsForBooleanColumns();
    }

    private void registerGeneralOperators() {
        // MISSING
        KnimeOperatorFunction checkMissing = new KnimeNoParameterOperatorFunction(IS_MISSING);
        addDefaultOperator(KnimeOperator.MISSING.getOperator(), OperatorValue.builder(checkMissing).build());

        // NOT_MISSING
        KnimeOperatorFunction checkNotMissing = new KnimeNoParameterOperatorFunction(IS_NOT_MISSING);
        addDefaultOperator(KnimeOperator.NOT_MISSING.getOperator(), OperatorValue.builder(checkNotMissing).build());
    }

    private void registerOperatorsForBooleanColumns() {
        final OperatorValidation oneOperandValidation = new SingleOperandValidation();
        Function<String, DataCell> stringToBooleanCell =
            s -> Boolean.parseBoolean(s) ? BooleanCell.TRUE : BooleanCell.FALSE;

        //EQUAL
        KnimeOperatorFunction booleanEquals =
            new KnimeOneParameterOperatorFunction(stringToBooleanCell, p -> p::equals);
        addBooleanOperator(KnimeOperator.EQUAL.getOperator(), OperatorValue.builder(booleanEquals)
            .withValidation(oneOperandValidation).withPanel(SingleFieldPanel::new).build());

        //NOT EQUAL
        KnimeOperatorFunction notEqualBooleanFunction = new KnimeOneParameterOperatorFunction(stringToBooleanCell,
            KnimeRowFilterOperatorRegistry::createNotEqualsPredicate);
        addBooleanOperator(KnimeOperator.NOT_EQUAL.getOperator(), OperatorValue.builder(notEqualBooleanFunction)
            .withValidation(oneOperandValidation).withPanel(SingleFieldPanel::new).build());
    }

    private void registerOperatorsForStringColumns() {
        final OperatorValidation oneOperandValidation = new SingleOperandValidation();
        Function<String, DataCell> stringToStringCell = StringCell::new;

        // EQUAL
        KnimeOperatorFunction stringEquals = new KnimeOneParameterOperatorFunction(stringToStringCell, p -> p::equals);
        addStringOperator(KnimeOperator.EQUAL.getOperator(), OperatorValue.builder(stringEquals)
            .withValidation(oneOperandValidation).withPanel(SingleFieldPanel::new).build());

        // NOT EQUAL
        KnimeOperatorFunction notEqualStringFunction = new KnimeOneParameterOperatorFunction(stringToStringCell,
            KnimeRowFilterOperatorRegistry::createNotEqualsPredicate);
        addStringOperator(KnimeOperator.NOT_EQUAL.getOperator(), OperatorValue.builder(notEqualStringFunction)
            .withValidation(oneOperandValidation).withPanel(SingleFieldPanel::new).build());

        //PATTERN MATCHING
        KnimeOperatorFunction patternMatching = KnimePatternMatchOperatorFunction.INSTANCE;
        addStringOperator(KnimeOperator.PATTERN_MATCHING.getOperator(), OperatorValue.builder(patternMatching)
            .withValidation(new KnimePatternMatchOperandValidation()).withPanel(PatternMatchPanel::new).build());

    }

    private void registerOperatorsForNumericalColumns() {
        final OperatorValidation numericOperandValidation = new NumericOperandValidation();
        final OperatorValidation twoNumericOperandValidation = new BetweenOperandValidation();
        Function<String, DataCell> stringToDoubleCell = s -> new DoubleCell(Double.parseDouble(s));
        // equals
        KnimeOperatorFunction doubleEquals = new KnimeOneParameterOperatorFunction(stringToDoubleCell,
            KnimeRowFilterOperatorRegistry::createEqualPredicateNumeric, RangeExtractorUtil::equalInclusive);
        addNumericOperator(KnimeOperator.EQUAL.getOperator(), OperatorValue.builder(doubleEquals)
            .withValidation(numericOperandValidation).withPanel(SingleNumericFieldPanel::new).build());

        // not equals
        KnimeOperatorFunction notEqualDoubleFunction = new KnimeOneParameterOperatorFunction(stringToDoubleCell,
            KnimeRowFilterOperatorRegistry::createNotEqualsPredicateNumeric);
        addNumericOperator(KnimeOperator.NOT_EQUAL.getOperator(), OperatorValue.builder(notEqualDoubleFunction)
            .withValidation(numericOperandValidation).withPanel(SingleNumericFieldPanel::new).build());

        //GREATER
        KnimeOperatorFunction greaterFunction = new KnimeOneParameterOperatorFunction(stringToDoubleCell,
            KnimeRowFilterOperatorRegistry::createGreaterPredicate, RangeExtractorUtil::startingExclusive);
        addNumericOperator(KnimeOperator.GREATER.getOperator(), OperatorValue.builder(greaterFunction)
            .withValidation(numericOperandValidation).withPanel(SingleNumericFieldPanel::new).build());

        //GREATER_OR_EQUAL
        KnimeOperatorFunction greaterOrEqualFunction = new KnimeOneParameterOperatorFunction(stringToDoubleCell,
            KnimeRowFilterOperatorRegistry::createGreaterOrEqualPredicate, RangeExtractorUtil::startingInclusive);
        addNumericOperator(KnimeOperator.GREATER_OR_EQUAL.getOperator(), OperatorValue.builder(greaterOrEqualFunction)
            .withValidation(numericOperandValidation).withPanel(SingleNumericFieldPanel::new).build());

        //LESS
        KnimeOperatorFunction lessFunction = new KnimeOneParameterOperatorFunction(stringToDoubleCell,
            KnimeRowFilterOperatorRegistry::createLessPredicate, RangeExtractorUtil::endingExclusive);
        addNumericOperator(KnimeOperator.LESS.getOperator(), OperatorValue.builder(lessFunction)
            .withValidation(numericOperandValidation).withPanel(SingleNumericFieldPanel::new).build());

        //LESS_OR_EQUAL
        KnimeOperatorFunction lessOrEqualFunction = new KnimeOneParameterOperatorFunction(stringToDoubleCell,
            KnimeRowFilterOperatorRegistry::createLessOrEqualPredicate, RangeExtractorUtil::endingInclusive);
        addNumericOperator(KnimeOperator.LESS_OR_EQUAL.getOperator(), OperatorValue.builder(lessOrEqualFunction)
            .withValidation(numericOperandValidation).withPanel(SingleNumericFieldPanel::new).build());

        //BETWEEN
        KnimeOperatorFunction betweenFunction = new KnimeTwoParameterOperatorFunction(stringToDoubleCell,
            KnimeRowFilterOperatorRegistry::createBetweenPredicate, RangeExtractorUtil::between);
        addNumericOperator(KnimeOperator.BETWEEN.getOperator(), OperatorValue.builder(betweenFunction)
            .withValidation(twoNumericOperandValidation).withPanel(TwoNumericFieldsPanel::new).build());
    }

    /**
     * @return true if the cell is not missing, if the cell we are testing is numeric and the predicate test for
     *         equality is passed
     */
    private static Predicate<DataCell> createEqualPredicateNumeric(final DataCell parameter) {
        CheckUtils.checkArgument(parameter instanceof DoubleValue, DOUBLE_TYPE_ERROR_TEMPLATE, parameter);
        final DoubleValue reference = (DoubleValue)parameter;
        final Predicate<DataCell> notMissingPredicate = IS_NOT_MISSING;
        return c -> notMissingPredicate.test(c) && c instanceof DoubleValue && equalsNumeric((DoubleValue)c, reference);
    }

    /** @return true if the data cell is equal to the operand chosen by the user. */
    private static boolean equalsNumeric(final DoubleValue value, final DoubleValue reference) {
        return value.getDoubleValue() == reference.getDoubleValue();
    }

    /**
     * @return true if the data cell is not equal to the operand chosen by the user. Used in the case of boolean cell or
     *         string cell.
     */
    private static Predicate<DataCell> createNotEqualsPredicate(final DataCell parameter) {
        return c -> !parameter.equals(c);
    }

    /**
     * @return true if the cell is missing or if the cell we testing is numeric and the predicate test for equality is
     *         passed. Also the missing cell is considered to be not equal.
     **/
    private static Predicate<DataCell> createNotEqualsPredicateNumeric(final DataCell parameter) {
        CheckUtils.checkArgument(parameter instanceof DoubleValue, DOUBLE_TYPE_ERROR_TEMPLATE, parameter);
        final DoubleValue reference = (DoubleValue)parameter;
        final Predicate<DataCell> missingPredicate = IS_MISSING;
        return c -> missingPredicate.test(c) || c instanceof DoubleValue && notEqualsNumeric((DoubleValue)c, reference);
    }

    /** @return true if the data cell is not equal to the operand chosen by the user. */
    private static boolean notEqualsNumeric(final DoubleValue value, final DoubleValue reference) {
        return value.getDoubleValue() != reference.getDoubleValue();
    }

    /**
     * @return true if the cell is not missing, if the cell we testing is numeric and the predicate test for greater is
     *         passed.
     */
    private static Predicate<DataCell> createGreaterPredicate(final DataCell parameter) {
        CheckUtils.checkArgument(parameter instanceof DoubleValue, DOUBLE_TYPE_ERROR_TEMPLATE, parameter);
        final DoubleValue reference = (DoubleValue)parameter;
        final Predicate<DataCell> notMissingPredicate = IS_NOT_MISSING;
        return c -> notMissingPredicate.test(c) && c instanceof DoubleValue && greater((DoubleValue)c, reference);
    }

    /** @return true if the data cell is greater compared to the operand chosen by the user. */
    private static boolean greater(final DoubleValue value, final DoubleValue reference) {
        return value.getDoubleValue() > reference.getDoubleValue();
    }

    /**
     * @return true if the cell is not missing, if the cell we testing is numeric and the predicate test for
     *         greaterOrEqual is passed.
     */
    private static Predicate<DataCell> createGreaterOrEqualPredicate(final DataCell parameter) {
        CheckUtils.checkArgument(parameter instanceof DoubleValue, DOUBLE_TYPE_ERROR_TEMPLATE, parameter);
        final DoubleValue reference = (DoubleValue)parameter;
        final Predicate<DataCell> notMissingPredicate = IS_NOT_MISSING;
        return c -> notMissingPredicate.test(c) && c instanceof DoubleValue
            && greaterOrEqual((DoubleValue)c, reference);
    }

    /** @return true if the data cell is greater or equal compared to the operand chosen by the user. */
    private static boolean greaterOrEqual(final DoubleValue value, final DoubleValue reference) {
        return value.getDoubleValue() >= reference.getDoubleValue();
    }

    /**
     * @return true if the cell is not missing, if the cell we testing is numeric and the predicate test for less is
     *         passed.
     */
    private static Predicate<DataCell> createLessPredicate(final DataCell parameter) {
        CheckUtils.checkArgument(parameter instanceof DoubleValue, DOUBLE_TYPE_ERROR_TEMPLATE, parameter);
        final DoubleValue reference = (DoubleValue)parameter;
        final Predicate<DataCell> notMissingPredicate = IS_NOT_MISSING;
        return c -> notMissingPredicate.test(c) && c instanceof DoubleValue && less((DoubleValue)c, reference);
    }

    /** @return true if the data cell is smaller compared to the operand chosen by the user. */
    private static boolean less(final DoubleValue value, final DoubleValue reference) {
        return value.getDoubleValue() < reference.getDoubleValue();
    }

    /**
     * @return true if the cell is not missing, if the cell we testing is numeric and the predicate test for lessOrEqual
     *         is passed.
     */
    private static Predicate<DataCell> createLessOrEqualPredicate(final DataCell parameter) {
        CheckUtils.checkArgument(parameter instanceof DoubleValue, DOUBLE_TYPE_ERROR_TEMPLATE, parameter);
        final DoubleValue reference = (DoubleValue)parameter;
        final Predicate<DataCell> notMissingPredicate = IS_NOT_MISSING;
        return c -> notMissingPredicate.test(c) && c instanceof DoubleValue && lessOrEqual((DoubleValue)c, reference);
    }

    /** @return true if the data cell is smaller or equal compared to the operand chosen by the user. */
    private static boolean lessOrEqual(final DoubleValue value, final DoubleValue reference) {
        return value.getDoubleValue() <= reference.getDoubleValue();
    }

    /**
     * @return true if the cell is not missing, if the cell we testing is numeric and the predicate test between is
     *         passed.
     */
    private static Predicate<DataCell> createBetweenPredicate(final DataCell parameter1, final DataCell parameter2) {
        CheckUtils.checkArgument(parameter1 instanceof DoubleValue, DOUBLE_TYPE_ERROR_TEMPLATE, parameter1);
        final DoubleValue reference1 = (DoubleValue)parameter1;
        CheckUtils.checkArgument(parameter2 instanceof DoubleValue, DOUBLE_TYPE_ERROR_TEMPLATE, parameter2);
        final DoubleValue reference2 = (DoubleValue)parameter2;
        final Predicate<DataCell> notMissingPredicate = IS_NOT_MISSING;
        return c -> notMissingPredicate.test(c) && c instanceof DoubleValue
            && between((DoubleValue)c, reference1, reference2);
    }

    /** @return true if the data cell is between the two operands chosen by the user. */
    private static boolean between(final DoubleValue value, final DoubleValue reference1,
        final DoubleValue reference2) {
        return value.getDoubleValue() >= reference1.getDoubleValue()
            && value.getDoubleValue() <= reference2.getDoubleValue();
    }

}
