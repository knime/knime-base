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
 *   8 Oct 2025 (Generated): created
 */
package org.knime.base.node.preproc.filter.row3.operators;

import java.util.List;
import java.util.function.Predicate;
import java.util.function.ToIntBiFunction;

import org.knime.base.node.preproc.filter.row3.operators.defaults.DoubleParameters;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValue;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.IntValue;
import org.knime.core.data.LongValue;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.LongCell;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.webui.node.dialog.defaultdialog.internal.dynamic.extensions.filtervalue.FilterOperator;
import org.knime.core.webui.node.dialog.defaultdialog.internal.dynamic.extensions.filtervalue.FilterOperatorFamily;
import org.knime.core.webui.node.dialog.defaultdialog.internal.dynamic.extensions.filtervalue.FilterOperators;
import org.knime.core.webui.node.dialog.defaultdialog.internal.dynamic.extensions.filtervalue.FilterValueParameters;
import org.knime.core.webui.node.dialog.defaultdialog.internal.dynamic.extensions.filtervalue.builtin.ComparableOperatorFamily;
import org.knime.core.webui.node.dialog.defaultdialog.internal.dynamic.extensions.filtervalue.builtin.EqualsOperatorFamily;
import org.knime.core.webui.node.dialog.defaultdialog.internal.dynamic.extensions.filtervalue.valuefilter.ValueFilterValidationUtil;

/**
 * Operators for the Double data type.
 *
 * @author Generated
 */
@SuppressWarnings("restriction")
public final class DoubleFilterOperators implements FilterOperators {
    @Override
    public DataType getDataType() {
        return DoubleCell.TYPE;
    }

    @Override
    public List<FilterOperatorFamily<? extends FilterValueParameters>> getOperatorFamilies() {
        return List.of(new DoubleEqualityFamily(DoubleCell.TYPE, DoubleParameters.class),
            new DoubleComparisonFamily(DoubleCell.TYPE, DoubleParameters.class));
    }

    private static final class DoubleEqualityFamily extends EqualsOperatorFamily<DoubleCell, DoubleParameters> {

        private DoubleEqualityFamily(final DataType dataType, final Class<DoubleParameters> paramClass) {
            super(dataType, paramClass);
        }

        @Override
        public Predicate<DataValue> getEquality(final DataColumnSpec runtimeColumnSpec,
            final FilterOperator<DoubleParameters> operator, final DoubleParameters params)
            throws InvalidSettingsException {
            final var colType = runtimeColumnSpec.getType();
            final var isCompIntVal = colType.isCompatible(IntValue.class);
            final var isCompDoubleVal = colType.isCompatible(DoubleValue.class);
            // we explicitly do not allow LongValue here, as that would be a lossy conversion
            final var isCompLongVal = colType.isCompatible(LongValue.class);
            if ((!isCompDoubleVal && !isCompIntVal) || (isCompLongVal && !isCompIntVal)) {
                throw ValueFilterValidationUtil
                    .createInvalidSettingsException(builder -> builder.withSummary(ValueFilterValidationUtil
                        .getUnsupportedOperatorSummary(super.getDataType(), operator, runtimeColumnSpec))
                        .addResolutions(
                            // change input
                            ValueFilterValidationUtil.resolutionChangeInput(DoubleCell.TYPE, IntCell.TYPE),
                            // reconfigure
                            ValueFilterValidationUtil.resolutionSelectDifferentOperator(colType)));
            }
            final var doubleValue = params.createCell().getDoubleValue();
            if (isCompIntVal) {
                return dv -> ((IntValue)dv).getIntValue() == doubleValue; // NOSONAR
            }
            // isCompDoubleVal
            return dv -> ((DoubleValue)dv).getDoubleValue() == doubleValue; // NOSONAR
        }
    }

    private static final class DoubleComparisonFamily extends ComparableOperatorFamily<DoubleCell, DoubleParameters> {

        private DoubleComparisonFamily(final DataType dataType, final Class<DoubleParameters> parametersClass) {
            super(dataType, parametersClass);
        }

        @Override
        protected ToIntBiFunction<DataValue, DoubleCell> getComparator(final DataColumnSpec runtimeColumnSpec,
            final FilterOperator<DoubleParameters> operator) throws InvalidSettingsException {
            final var colType = runtimeColumnSpec.getType();
            final var isCompIntVal = colType.isCompatible(IntValue.class);
            final var isCompDoubleVal = colType.isCompatible(DoubleValue.class);
            // we explicitly do not allow LongValue here, as that would be a lossy conversion
            final var isCompLongVal = colType.isCompatible(LongValue.class);
            if (!(isCompIntVal || isCompDoubleVal) || (isCompLongVal && !isCompIntVal)) {
                throw ValueFilterValidationUtil
                    .createInvalidSettingsException(builder -> builder.withSummary(ValueFilterValidationUtil
                        .getUnsupportedOperatorSummary(super.getDataType(), operator, runtimeColumnSpec))
                        .addResolutions(
                            // change input
                            ValueFilterValidationUtil.resolutionChangeInput(IntCell.TYPE, DoubleCell.TYPE,
                                LongCell.TYPE),
                            // reconfigure
                            ValueFilterValidationUtil.resolutionSelectDifferentOperator(colType)));
            }
            if (isCompIntVal) {
                return (v, c) -> Double.compare(((IntValue)v).getIntValue(), c.getDoubleValue());
            }
            // isCompDoubleVal
            return (v, c) -> Double.compare(((DoubleValue)v).getDoubleValue(), c.getDoubleValue());
        }
    }
}