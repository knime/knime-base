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
 *  propagation of KNIME.
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
 *   Sep 24, 2025 (Paul Bärnreuther): created
 */
package org.knime.base.node.preproc.filter.row3.operators.rownumber;

import java.util.function.LongFunction;
import java.util.function.LongPredicate;

import org.knime.base.data.filter.row.v2.FilterPartition;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.webui.node.dialog.defaultdialog.internal.dynamic.extensions.filtervalue.FilterOperatorDefinition;
import org.knime.core.webui.node.dialog.defaultdialog.internal.dynamic.extensions.filtervalue.FilterValueParameters;

/**
 * Interface for filter operators that can be applied to row numbers. Supports both efficient slicing (via OffsetFilter)
 * and predicate-based filtering.
 *
 * @param <T> type of the parameters
 * @author Paul Bärnreuther
 */
public interface RowNumberFilterOperator<T extends FilterValueParameters> extends FilterOperatorDefinition<T> {

    /**
     * Creates an efficient slice filter if the operator supports slicing optimization. This is used for performance
     * optimization when possible.
     *
     * To get the framework to call this method, {@link #supportsSlicing()} must return {@code true}.
     *
     * @param params the filter parameters
     * @return function from table size to {@link FilterPartition}
     *
     * @throws InvalidSettingsException if the parameters are invalid
     */
    default LongFunction<FilterPartition> createSliceFilter(final T params) throws InvalidSettingsException {
        if (supportsSlicing()) {
            throw new UnsupportedOperationException(
                "This operator claims to support slicing, but does not implement createSliceFilter.");
        }
        return null;
    }

    /**
     * Return {@code true} if this operator supports slicing, i.e. provides a non-null provider of a
     * {@link FilterPartition}.
     *
     * @return whether {@link #createSliceFilter} will return a non-null filter partition
     */
    default boolean supportsSlicing() {
        return false;
    }

    /**
     * Creates a predicate that tests row numbers (0-based indices). This is the fallback when slicing is not supported
     * or not applicable.
     *
     * @param params the filter parameters
     * @param optionalTableSize the table size or -1 on validation
     * @return predicate that tests row numbers
     * @throws InvalidSettingsException if the parameters are invalid
     */
    LongPredicate createPredicate(T params, long optionalTableSize) throws InvalidSettingsException;

}