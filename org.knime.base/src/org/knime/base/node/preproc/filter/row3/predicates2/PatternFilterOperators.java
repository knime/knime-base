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
package org.knime.base.node.preproc.filter.row3.predicates2;

import java.util.function.Function;
import java.util.function.LongPredicate;
import java.util.function.Predicate;

import org.knime.base.data.filter.row.v2.IndexedRowReadPredicate;
import org.knime.base.node.preproc.filter.row3.CaseSensitivity;
import org.knime.base.node.preproc.filter.row3.FilterOperator;
import org.knime.base.node.preproc.filter.row3.FilterOperator.RowIDFilterOperator;
import org.knime.base.node.preproc.filter.row3.FilterOperator.RowNumberFilterOperator;
import org.knime.base.node.preproc.filter.row3.predicates.StringPredicate;
import org.knime.base.node.preproc.filter.row3.predicates2.PatternFilterOperators.PatternFilterOperator.PatternFilterParameters;
import org.knime.base.node.preproc.filter.row3.predicates2.PatternFilterOperators.RowNumberPatternFilterOperator.RowNumberPatternFilterParameters;
import org.knime.core.data.BooleanValue;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValue;
import org.knime.core.data.IntValue;
import org.knime.core.data.LongValue;
import org.knime.core.data.RowKeyValue;
import org.knime.core.data.StringValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.webui.node.dialog.defaultdialog.internal.dynamic.extensions.filtervalue.FilterValueParameters;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.widget.choices.ValueSwitchWidget;

/**
 * Factory for creating predicates that test whether a string representation of a value matches a pattern (regex or
 * wildcard). The string representation is supplied by a function that operates on an indexed row read.
 *
 * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
 */
public final class PatternFilterOperators {

    private PatternFilterOperators() {
        // Utility
    }

    public static boolean isSupported(final DataType type) {
        final var preferredValueClass = type.getPreferredValueClass();
        return !BooleanValue.class.equals(preferredValueClass) // booleans have only IS_TRUE and IS_FALSE operators
            && (type.isCompatible(StringValue.class) //
                || type.isCompatible(IntValue.class) || type.isCompatible(LongValue.class));
    }

    public static final class PatternFilterOperator implements
        FilterOperator.ColumnFilterOperator<PatternFilterParameters>, RowIDFilterOperator<PatternFilterParameters> {

        private final boolean m_isRegex;

        public PatternFilterOperator(final boolean isRegex) {
            m_isRegex = isRegex;
        }

        @Override
        public Class<PatternFilterParameters> getParametersClass() {
            return PatternFilterParameters.class;
        }

        @Override
        public Predicate<RowKeyValue> translateToPredicate(final PatternFilterParameters params) {
            final var predicate = getPredicate(params, m_isRegex);
            return rowKey -> predicate.test(rowKey.getString());
        }

        @Override
        public IndexedRowReadPredicate translateToPredicate(final PatternFilterParameters params, final int columnIndex,
            final DataType dataType) throws InvalidSettingsException {
            final var toStringFunction = toStringFunction(dataType);
            final var predicate = getPredicate(params, m_isRegex);
            return (idx, row) -> predicate.test(toStringFunction.apply(row.getValue(columnIndex)));
        }

        public static final class PatternFilterParameters implements FilterValueParameters {

            @Widget(title = "Case matching", description = "Whether to consider case when matching the pattern.")
            @ValueSwitchWidget
            public CaseSensitivity m_caseSensitivity = CaseSensitivity.CASE_INSENSITIVE;

            @Widget(title = "Pattern", description = "The pattern to filter for.")
            public String m_pattern = "";
        }

        private static StringPredicate getPredicate(final PatternFilterParameters params, final boolean isRegex) {
            return StringPredicate.pattern(params.m_pattern, isRegex,
                params.m_caseSensitivity == CaseSensitivity.CASE_SENSITIVE);
        }

        /**
         * String serialization function based on the data type's {@link DataType#getPreferredValueClass preferred value
         * class} for integral numeric types, otherwise type must be compatible {@link StringValue}.
         *
         * @param columnDataType column data type
         * @return method that returns a string from a given data value
         * @throws InvalidSettingsException if the data type is not supported because it cannot produce a string value
         */
        private static Function<DataValue, String> toStringFunction(final DataType columnDataType)
            throws InvalidSettingsException {
            final var preferredValueClass = columnDataType.getPreferredValueClass();
            if (preferredValueClass.equals(LongValue.class)) {
                return value -> Long.toString(((LongValue)value).getLongValue());
            } else if (preferredValueClass.equals(IntValue.class)) {
                return value -> Integer.toString(((IntValue)value).getIntValue());
            }
            if (!columnDataType.isCompatible(StringValue.class)) {
                throw new InvalidSettingsException("The type \"%s\" is not supported by the pattern filter operator."
                    .formatted(columnDataType.getName()));
            }
            return value -> ((StringValue)value).getStringValue();
        }

    }

    public static final class RowNumberPatternFilterOperator
        implements RowNumberFilterOperator<RowNumberPatternFilterParameters> {
        private final boolean m_isRegex;

        public RowNumberPatternFilterOperator(final boolean isRegex) {
            m_isRegex = isRegex;
        }

        @Override
        public Class<RowNumberPatternFilterParameters> getParametersClass() {
            return RowNumberPatternFilterParameters.class;
        }

        @Override
        public LongPredicate translateToPredicate(final RowNumberPatternFilterParameters params) {
            final var predicate = StringPredicate.pattern(params.m_pattern, m_isRegex, false);
            // map from 0-based index to 1-based row number
            return rowNum -> predicate.test(Long.toString(rowNum + 1));
        }

        public static final class RowNumberPatternFilterParameters implements FilterValueParameters {

            @Widget(title = "Pattern", description = "The pattern to filter for.")
            public String m_pattern = "";

        }
    }

}
