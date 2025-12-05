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
package org.knime.base.node.preproc.filter.row3.operators.legacy;

import java.util.OptionalInt;

import org.knime.core.data.v2.IndexedRowReadPredicate;
import org.knime.core.data.v2.RowRead;
import org.knime.core.node.InvalidSettingsException;

/**
 * Factory for predicates that can be used to filter indexed {@link RowRead rows}.
 *
 * @deprecated This interface is part of the legacy row filter implementation used for backwards-compatibility.
 *
 * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
 */
@FunctionalInterface
@Deprecated(since = "5.10", forRemoval = false)
public interface PredicateFactory {

    /**
     * Creates a filter predicate given a column index and a reference value input. In case the predicate does not
     * operate on a column, an empty optional should be passed.
     *
     * <br><b>Note:</b>
     * Implementations may throw {@link IllegalArgumentException} if the column index is <i>invalid</i>.
     * Invalid means that it is provided but not used by the predicate factory or if it is not provided but required by
     * the factory.
     *
     * @param columnIndex column index to filter on, or empty optional if not operating on a column
     * @param inputValues input values to use as reference
     * @return predicate predicate that can be used to filter rows
     * @throws InvalidSettingsException in case the input values are missing or invalid
     * @throws IllegalArgumentException in case the column index argument is invalid
     */
    IndexedRowReadPredicate createPredicate(OptionalInt columnIndex, final DynamicValuesInput inputValues) // NOSONAR make usage of column/no-column explicit
        throws InvalidSettingsException;

}
