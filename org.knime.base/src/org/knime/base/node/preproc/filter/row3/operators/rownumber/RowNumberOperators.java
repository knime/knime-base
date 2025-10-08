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
 *   26 Sept 2025 (Manuel Hotz, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.base.node.preproc.filter.row3.operators.rownumber;

import java.util.List;
import java.util.function.LongFunction;
import java.util.function.LongPredicate;

import org.knime.base.data.filter.row.v2.FilterPartition;
import org.knime.base.node.preproc.filter.row3.operators.rownumber.RowNumberFilterSpec.Operator;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.webui.node.dialog.defaultdialog.internal.dynamic.extensions.filtervalue.FilterValueParameters;
import org.knime.core.webui.node.dialog.defaultdialog.internal.dynamic.extensions.filtervalue.builtin.EqualsOperator;
import org.knime.core.webui.node.dialog.defaultdialog.internal.dynamic.extensions.filtervalue.builtin.GreaterThanOperator;
import org.knime.core.webui.node.dialog.defaultdialog.internal.dynamic.extensions.filtervalue.builtin.GreaterThanOrEqualOperator;
import org.knime.core.webui.node.dialog.defaultdialog.internal.dynamic.extensions.filtervalue.builtin.LessThanOperator;
import org.knime.core.webui.node.dialog.defaultdialog.internal.dynamic.extensions.filtervalue.builtin.LessThanOrEqualOperator;
import org.knime.core.webui.node.dialog.defaultdialog.internal.dynamic.extensions.filtervalue.builtin.NotEqualsOperator;

/**
 * Provides the operators for row numbers.
 *
 * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
 */
public final class RowNumberOperators {

    private RowNumberOperators() {
        // utility
    }

    /**
     * Gets the operators for row numbers.
     *
     * @return equality operators for row numbers
     */
    public static List<RowNumberFilterOperator<? extends FilterValueParameters>> getOperators() {
        return List.of( //
            new RowNumberEquals(), //
            new RowNumberNotEquals(), //
            new RowNumberLessThan(), //
            new RowNumberLessThanOrEquals(), //
            new RowNumberGreaterThan(), //
            new RowNumberGreaterThanOrEquals(), //
            new RowNumberFirstNRows(), //
            new RowNumberLastNRows() //
        );
    }

    // this is a bit tedious, since we need to implement our marker interfaces for each of the operators that has one
    // otherwise, we could just instantiate based on RowNumberFilterSpec.Operator directly

    private abstract static class RowNumberFilterBase implements RowNumberFilterOperator<RowNumberParameters> {
        @Override
        public Class<RowNumberParameters> getNodeParametersClass() {
            return RowNumberParameters.class;
        }

        abstract RowNumberFilterSpec.Operator getOperator();

        @Override
        public LongPredicate createPredicate(final RowNumberParameters params, final long tableSize)
            throws InvalidSettingsException {
            return createRowNumberFilterSpec(params).toOffsetFilter(tableSize) //
                .asOffsetPredicate();
        }

        @Override
        public LongFunction<FilterPartition> createSliceFilter(final RowNumberParameters params)
            throws InvalidSettingsException {
            return createRowNumberFilterSpec(params).toFilterPartition();
        }

        @Override
        public boolean supportsSlicing() {
            return true;
        }

        private RowNumberFilterSpec createRowNumberFilterSpec(final RowNumberParameters params)
            throws InvalidSettingsException {
            return new RowNumberFilterSpec(getOperator(), params.m_value);
        }
    }

    private static class RowNumberEquals extends RowNumberFilterBase implements EqualsOperator {
        @Override
        Operator getOperator() {
            return RowNumberFilterSpec.Operator.EQ;
        }
    }

    private static class RowNumberNotEquals extends RowNumberFilterBase implements NotEqualsOperator {
        @Override
        Operator getOperator() {
            return RowNumberFilterSpec.Operator.NEQ;
        }
    }

    private static class RowNumberLessThan extends RowNumberFilterBase implements LessThanOperator {
        @Override
        Operator getOperator() {
            return RowNumberFilterSpec.Operator.LT;
        }
    }

    private static class RowNumberLessThanOrEquals extends RowNumberFilterBase implements LessThanOrEqualOperator {
        @Override
        Operator getOperator() {
            return RowNumberFilterSpec.Operator.LTE;
        }
    }

    private static class RowNumberGreaterThan extends RowNumberFilterBase implements GreaterThanOperator {
        @Override
        Operator getOperator() {
            return RowNumberFilterSpec.Operator.GT;
        }
    }

    private static class RowNumberGreaterThanOrEquals extends RowNumberFilterBase
        implements GreaterThanOrEqualOperator {
        @Override
        Operator getOperator() {
            return RowNumberFilterSpec.Operator.GTE;
        }
    }

    private static class RowNumberFirstNRows extends RowNumberFilterBase {
        @Override
        Operator getOperator() {
            return RowNumberFilterSpec.Operator.FIRST_N_ROWS;
        }

        @Override
        public String getId() {
            return "FIRST_N_ROWS";
        }

        @Override
        public String getLabel() {
            return "First n rows";
        }
    }

    private static class RowNumberLastNRows extends RowNumberFilterBase {
        @Override
        Operator getOperator() {
            return RowNumberFilterSpec.Operator.LAST_N_ROWS;
        }

        @Override
        public String getId() {
            return "LAST_N_ROWS";
        }

        @Override
        public String getLabel() {
            return "Last n rows";
        }
    }

}