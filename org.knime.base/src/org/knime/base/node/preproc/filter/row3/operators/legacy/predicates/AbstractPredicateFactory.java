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
package org.knime.base.node.preproc.filter.row3.operators.legacy.predicates;

import java.util.OptionalInt;
import java.util.function.UnaryOperator;

import org.knime.base.data.filter.row.v2.IndexedRowReadPredicate;
import org.knime.base.node.preproc.filter.row3.operators.legacy.DynamicValuesInput;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataType;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.message.Message;
import org.knime.core.node.message.MessageBuilder;
import org.knime.core.node.util.CheckUtils;

/**
 * Base class that should be used for predicate factories. It provides a helper method to get the input value and a nice
 * error message in case it is missing.
 *
 * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
 */
abstract class AbstractPredicateFactory implements PredicateFactory {

    protected static final NodeLogger LOGGER = NodeLogger.getLogger(AbstractPredicateFactory.class);

    protected AbstractPredicateFactory() { }

    /**
     * Predicate factory base for row key predicates.
     */
    abstract static class RowKeyPredicateFactory extends AbstractPredicateFactory {

        protected abstract IndexedRowReadPredicate createPredicate(final DynamicValuesInput inputValues)
            throws InvalidSettingsException;

        @Override
        public IndexedRowReadPredicate createPredicate(final OptionalInt columnIndex,
            final DynamicValuesInput inputValues) throws InvalidSettingsException {
            CheckUtils.checkArgument(columnIndex.isEmpty(), "Unexpected column index for RowID predicate");
            return createPredicate(inputValues);
        }
    }

    /**
     * Gets the input value at the specified index, throwing an exception if the value is missing.
     *
     * @param inputValues input values to access
     * @param inputValueIndex index of the input value
     * @return input value
     * @throws InvalidSettingsException if the input value is missing
     */
    protected static DataCell getCellAtOrThrow(final DynamicValuesInput inputValues, final int inputValueIndex)
        throws InvalidSettingsException {
        return inputValues.getCellAt(inputValueIndex)
            .orElseThrow(AbstractPredicateFactory::createMissingReferenceValueException);
    }

    /**
     * Throws the exception resulting from the configured message builder.
     *
     * @param builderFn function to configure the message builder
     * @return invalid settings exception containing mandatory summary, details, and potential resolutions from the
     *         builder
     */
    protected static InvalidSettingsException
        createInvalidSettingsException(final UnaryOperator<MessageBuilder> builderFn) {
        return builderFn.apply(Message.builder()).build().orElseThrow().toInvalidSettingsException();
    }

    /**
     * Creates a nice exception message with potential resolution.
     *
     * @return exception to throw
     */
    private static InvalidSettingsException createMissingReferenceValueException() {
        return Message.builder().withSummary("Reference value is missing")
            .addResolutions("Reconfigure the node to provide a reference value.").build().orElseThrow()
            .toInvalidSettingsException();
    }

    /* === UTILITY === */

    protected static StringBuilder appendElements(final StringBuilder prefix, final DataType[] elements) {
        CheckUtils.checkArgument(elements.length > 0, "Cannot append empty elements array");
        final var quote = "\"";
        if (elements.length == 1) {
            return prefix.append(quote).append(elements[0]).append(quote);
        }
        if (elements.length == 2) {
            return prefix.append(quote).append(elements[0]).append(quote) //
                .append(" or ").append(quote).append(elements[1]).append(quote);
        }
        for (var i = 0; i < elements.length - 1; i++) {
            prefix.append(quote).append(elements[i]).append(quote).append(", ");
            if (i == elements.length - 2) {
                prefix.append("or ");
            }
        }
        return prefix;
    }
}
