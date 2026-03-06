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
 *   5 Mar 2026 (Paul Bärnreuther): created
 */
package org.knime.time.node.filter.rowfilter;

import java.util.List;
import java.util.function.Predicate;
import java.util.function.ToIntBiFunction;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValue;
import org.knime.core.data.DataValueComparatorDelegator;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.webui.node.dialog.defaultdialog.internal.dynamic.extensions.filtervalue.FilterOperator;
import org.knime.core.webui.node.dialog.defaultdialog.internal.dynamic.extensions.filtervalue.FilterOperatorFamily;
import org.knime.core.webui.node.dialog.defaultdialog.internal.dynamic.extensions.filtervalue.FilterValidationUtil;
import org.knime.core.webui.node.dialog.defaultdialog.internal.dynamic.extensions.filtervalue.FilterValueParameters.SingleCellValueParameters;
import org.knime.core.webui.node.dialog.defaultdialog.internal.dynamic.extensions.filtervalue.builtin.GreaterThanOperator;
import org.knime.core.webui.node.dialog.defaultdialog.internal.dynamic.extensions.filtervalue.builtin.GreaterThanOrEqualOperator;
import org.knime.core.webui.node.dialog.defaultdialog.internal.dynamic.extensions.filtervalue.builtin.LessThanOperator;
import org.knime.core.webui.node.dialog.defaultdialog.internal.dynamic.extensions.filtervalue.builtin.LessThanOrEqualOperator;

/**
 * Comparison operators for date/time types with date/time-specific labels. These replace the generic
 * {@link LessThanOperator}, {@link LessThanOrEqualOperator}, {@link GreaterThanOperator}, and
 * {@link GreaterThanOrEqualOperator} with "Is before", "Is before or at", "Is after", and "Is after or at".
 *
 * @param <C> data cell type to compare with
 * @param <P> type of filter parameters
 * @author Paul Bärnreuther
 */
@SuppressWarnings("restriction")
public class DateTimeComparisonOperators<C extends DataCell, P extends SingleCellValueParameters<C>>
    implements FilterOperatorFamily<P> {

    private final DataType m_dataType;

    private final Class<P> m_parametersClass;

    /**
     * @param dataType the data type this family is for
     * @param parametersClass the class of parameters used for all operators of this family
     */
    public DateTimeComparisonOperators(final DataType dataType, final Class<P> parametersClass) {
        m_dataType = dataType;
        m_parametersClass = parametersClass;
    }

    private ToIntBiFunction<DataValue, C> getComparator(final DataColumnSpec runtimeColumnSpec,
        final FilterOperator<P> operator) throws InvalidSettingsException {
        final var type = runtimeColumnSpec.getType();
        if (!type.isCompatible(m_dataType.getPreferredValueClass())) {
            throw FilterValidationUtil.createInvalidSettingsException(builder -> builder
                .withSummary("Operator \"%s\" for column \"%s\" expects data of type \"%s\", but got \"%s\""
                    .formatted(operator.getLabel(), runtimeColumnSpec.getName(), m_dataType.getName(), type.getName()))
                .addResolutions("Select a different operator that is compatible with the column's data type \"%s\"."
                    .formatted(type.getName())));
        }
        final var comparator = new DataValueComparatorDelegator<>(m_dataType.getComparator());
        return comparator::compare;
    }

    @Override
    public List<FilterOperator<P>> getOperators() {
        return List.of(new BeforeOperator(), new BeforeOrAtOperator(), new AfterOperator(), new AfterOrAtOperator());
    }

    private final class BeforeOperator implements FilterOperator<P>, LessThanOperator {

        @Override
        public String getLabel() {
            return "Is before";
        }

        @Override
        public Predicate<DataValue> createPredicate(final DataColumnSpec runtimeColumnSpec,
            final DataType configureColumnType, final P filterParameters) throws InvalidSettingsException {
            final var value = filterParameters.createCell();
            final var comparator = getComparator(runtimeColumnSpec, this);
            return dv -> comparator.applyAsInt(dv, value) < 0;
        }

        @Override
        public Class<P> getNodeParametersClass() {
            return m_parametersClass;
        }
    }

    private final class BeforeOrAtOperator implements FilterOperator<P>, LessThanOrEqualOperator {

        @Override
        public String getLabel() {
            return "Is before or at";
        }

        @Override
        public Predicate<DataValue> createPredicate(final DataColumnSpec runtimeColumnSpec,
            final DataType configureColumnType, final P filterParameters) throws InvalidSettingsException {
            final var value = filterParameters.createCell();
            final var comparator = getComparator(runtimeColumnSpec, this);
            return dv -> comparator.applyAsInt(dv, value) <= 0;
        }

        @Override
        public Class<P> getNodeParametersClass() {
            return m_parametersClass;
        }
    }

    private final class AfterOperator implements FilterOperator<P>, GreaterThanOperator {

        @Override
        public String getLabel() {
            return "Is after";
        }

        @Override
        public Predicate<DataValue> createPredicate(final DataColumnSpec runtimeColumnSpec,
            final DataType configureColumnType, final P filterParameters) throws InvalidSettingsException {
            final var value = filterParameters.createCell();
            final var comparator = getComparator(runtimeColumnSpec, this);
            return dv -> comparator.applyAsInt(dv, value) > 0;
        }

        @Override
        public Class<P> getNodeParametersClass() {
            return m_parametersClass;
        }
    }

    private final class AfterOrAtOperator implements FilterOperator<P>, GreaterThanOrEqualOperator {

        @Override
        public String getLabel() {
            return "Is after or at";
        }

        @Override
        public Predicate<DataValue> createPredicate(final DataColumnSpec runtimeColumnSpec,
            final DataType configureColumnType, final P filterParameters) throws InvalidSettingsException {
            final var value = filterParameters.createCell();
            final var comparator = getComparator(runtimeColumnSpec, this);
            return dv -> comparator.applyAsInt(dv, value) >= 0;
        }

        @Override
        public Class<P> getNodeParametersClass() {
            return m_parametersClass;
        }
    }
}
