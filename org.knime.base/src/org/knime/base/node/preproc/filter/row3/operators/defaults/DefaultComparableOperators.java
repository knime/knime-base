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
 *   25 Sept 2025 (Manuel Hotz, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.base.node.preproc.filter.row3.operators.defaults;

import java.util.List;
import java.util.function.Predicate;

import org.knime.core.data.BoundedValue;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValue;
import org.knime.core.data.DataValueComparatorDelegator;
import org.knime.core.data.def.BooleanCell;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.webui.node.dialog.defaultdialog.internal.dynamic.extensions.filtervalue.FilterOperator;
import org.knime.core.webui.node.dialog.defaultdialog.internal.dynamic.extensions.filtervalue.FilterValueParameters;
import org.knime.core.webui.node.dialog.defaultdialog.internal.dynamic.extensions.filtervalue.builtin.GreaterThanOperator;
import org.knime.core.webui.node.dialog.defaultdialog.internal.dynamic.extensions.filtervalue.builtin.GreaterThanOrEqualOperator;
import org.knime.core.webui.node.dialog.defaultdialog.internal.dynamic.extensions.filtervalue.builtin.LessThanOperator;
import org.knime.core.webui.node.dialog.defaultdialog.internal.dynamic.extensions.filtervalue.builtin.LessThanOrEqualOperator;

/**
 * Default comparable operators for data types that have a comparator but don't have their own registered operators.
 *
 * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
 */
public final class DefaultComparableOperators {

    private DefaultComparableOperators() {
    }

    @SuppressWarnings("restriction")
    public static List<FilterOperator<? extends FilterValueParameters>> getOperators(final DataType dataType) {
        return List.of( //
            new LessThanDefault(), //
            new LessThanOrEqualDefault(), //
            new GreaterThanDefault(), //
            new GreaterThanOrEqualDefault()//
        );
    }

    private static final class LessThanDefault extends ComparableOperator implements LessThanOperator {

        @Override
        Comparison compare() {
            return (cmp, zero) -> cmp < zero;
        }
    }

    private static final class LessThanOrEqualDefault extends ComparableOperator implements LessThanOrEqualOperator {

        @Override
        Comparison compare() {
            return (cmp, zero) -> cmp <= zero;
        }
    }

    private static final class GreaterThanDefault extends ComparableOperator implements GreaterThanOperator {

        @Override
        Comparison compare() {
            return (cmp, zero) -> cmp > zero;
        }
    }

    private static final class GreaterThanOrEqualDefault extends ComparableOperator
        implements GreaterThanOrEqualOperator {

        @Override
        Comparison compare() {
            return (cmp, zero) -> cmp >= zero;
        }
    }

    interface Comparison {
        boolean test(final int comparisonResult, final int zero);
    }

    abstract static class ComparableOperator implements FilterOperator<SingleStringParameters> {

        abstract Comparison compare();

        @Override
        public Class<SingleStringParameters> getNodeParametersClass() {
            return SingleStringParameters.class;
        }

        @Override
        public Predicate<DataValue> createPredicate(final DataColumnSpec runtimeColumnSpec,
            final DataType configureColumnType, final SingleStringParameters filterParameters)
            throws InvalidSettingsException {
            final var comparator = new DataValueComparatorDelegator<>(runtimeColumnSpec.getType().getComparator());
            final var reference = filterParameters.createCellAs(runtimeColumnSpec.getType());
            return dv -> compare().test(comparator.compare(dv, reference), 0);
        }

    }

    /**
     * Checks if the given data type supports comparison operations on execution.
     *
     * @param dataType the data type to check
     * @return {@code true} if the data type has a comparator, {@code false} otherwise
     */
    public static boolean isSupported(final DataType dataType) {
        // legacy behavior where also non-"bounded" types were supported
        return dataType.getComparator() != null;
    }

    /**
     * Checks if the given data type is applicable for this operator family, i.e. if the operators of this family are
     * options for the given data type.
     *
     * For example, this check is used to determine whether to show the operators in the filter dialog when a column of
     * the given data type is selected.
     *
     * @param dataType data type to check
     * @return {@code true} if the operators of this family are applicable for the given data type, {@code false}
     *         otherwise
     */
    public static boolean isApplicable(final DataType dataType) {
        // we handle Boolean with IS_TRUE/IS_FALSE operators
        return !BooleanCell.TYPE.equals(dataType) && TypeMappingUtils.supportsDataType(dataType) && dataType.isCompatible(BoundedValue.class);
    }
}