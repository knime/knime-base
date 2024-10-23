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
import java.util.function.Function;

import org.knime.core.data.DataType;
import org.knime.core.data.DataValue;
import org.knime.core.data.IntValue;
import org.knime.core.data.LongValue;
import org.knime.core.data.StringValue;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.LongCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.message.Message;
import org.knime.core.webui.node.dialog.defaultdialog.widget.dynamic.DynamicValuesInput;

final class PatternMatchingPredicateFactory extends AbstractPredicateFactory {

    private final boolean m_isRegex;
    private final IndexedRowReadFunction<String> m_toStringFn;

    private PatternMatchingPredicateFactory(final boolean isRegex, final IndexedRowReadFunction<String> toStringFn) {
        m_isRegex = isRegex;
        m_toStringFn = toStringFn;
    }

    /**
     * Creates a factory for pattern matching predicates on row key.
     * @param isRegex whether the pattern is a regex or a wildcard
     * @return predicate factory for pattern matching on row key
     */
    static PredicateFactory forRowKey(final boolean isRegex) {
        return new PatternMatchingPredicateFactory(isRegex, (idx, row) -> row.getRowKey().getString());
    }

    /**
     * Creates a factory for pattern matching predicates on 1-based row number.
     * @param isRegex whether the pattern is a regex or a wildcard
     * @return predicate factory for pattern matching on row number string representation
     */
    static PredicateFactory forRowNumber(final boolean isRegex) {
        // map from 0-based index to 1-based row number
        return new PatternMatchingPredicateFactory(isRegex, (idx, row) -> Long.toString(idx + 1));
    }

    /**
     * Creates a factory for pattern matching predicates on a column.
     * @param columnIndex column index
     * @param columnDataType column data type
     * @param isRegex whether the pattern is a regex or a wildcard
     * @return predicate factory for pattern matching on the specified column
     */
    static Optional<PredicateFactory> forColumn(final DataType columnDataType,
            final boolean isRegex) {
        if (!isSupported(columnDataType)) {
            return Optional.empty();
        }

        final Function<DataValue, String> toStringFunction;
        final var preferredValueClass = columnDataType.getPreferredValueClass();
        if (preferredValueClass.equals(LongValue.class)) {
            toStringFunction = value -> Long.toString(((LongValue)value).getLongValue());
        } else if (preferredValueClass.equals(IntValue.class)) {
            toStringFunction = value -> Integer.toString(((IntValue)value).getIntValue());
        } else {
            toStringFunction = value -> ((StringValue)value).getStringValue();
        }
        final Function<Integer, IndexedRowReadFunction<String>> fn = columnIndex -> IndexedRowReadFunction
            .<String> wrap(rowRead -> toStringFunction.apply(rowRead.getValue(columnIndex)));
        return Optional.of(new PatternMatchingPredicateFactory(isRegex, fn));
    }

    private static boolean isSupported(final DataType type) {
        return type.isCompatible(StringValue.class) || type.isCompatible(IntValue.class)
            || type.isCompatible(LongValue.class);
    }

    // TODO solve problem that RowID/Row number don't have columnIndex, so `createPredicate(columnIndex, ...)` does
    //      not make sense
    //      should be more like: 1) create predicate factory for working on T, then 2) create a predicate given
    //      - an accessor for IndexedRowRead -> T
    //      - input value T (currently uses DynamicValuesInput directly, but to move that to core, we need to lose the
    //        core-ui dependency
    @Override
    public IndexedRowReadPredicate createPredicate(final int columnIndex, final DynamicValuesInput inputValues)
        throws InvalidSettingsException {
        final var valueIndex = 0; // we have only one value
        final var patternCell = getCellAtOrThrow(inputValues, valueIndex);
        if (!isSupported(patternCell.getType())) {
            final var regexOrWildcard = m_isRegex ? "regex" : "wildcard";
            throw Message.builder() //
                .withSummary("Cannot obtain pattern for %s operator from reference value of type \"%s\""
                    .formatted(regexOrWildcard, patternCell.getType()))
                .addResolutions("Reconfigure the node to use a pattern value of type \"%s\", \"%s\", or \"%s\""
                    .formatted(StringCell.TYPE, IntCell.TYPE, LongCell.TYPE))
                .build().orElseThrow().toInvalidSettingsException();
        }
        final var pattern = ((StringValue)patternCell).getStringValue();
        final var isCaseSensitive = inputValues.isStringMatchCaseSensitive(valueIndex);
        final var patternPredicate = StringPredicate.pattern(pattern, m_isRegex, isCaseSensitive);
        return (idx, row) -> patternPredicate.test(m_toStringFn.apply(idx, row));
    }

}
