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
 *   2 Apr 2024 (jasper): created
 */
package org.knime.base.node.preproc.filter.row3;

import java.util.Optional;
import java.util.function.Predicate;

import org.knime.core.data.LongValue;
import org.knime.core.node.InvalidSettingsException;

/**
 * Predicate for filtering rows based on (long) integer numbers.
 *
 * @author Jasper Krauter, KNIME GmbH, Konstanz, Germany
 */
final class LongValuePredicate extends RowPredicate<Optional<Long>> {
    protected LongValuePredicate(final int colIndex, final RowFilter3NodeSettings settings)
        throws InvalidSettingsException {
        super(row -> {
            final var cell = row.getCell(colIndex);
            if (cell instanceof LongValue lv) {
                return Optional.of(lv.getLongValue());
            } else {
                return Optional.empty();
            }
        }, wrapOptional(buildPredicate(settings)));
    }

    private static Predicate<Long> buildPredicate(final RowFilter3NodeSettings settings)
        throws InvalidSettingsException {
        final var lb = settings.m_anchors.m_integer.m_bounds.m_lowerBound;
        final var ub = settings.m_anchors.m_integer.m_bounds.m_upperBound;
        final var val = settings.m_anchors.m_integer.m_value;

        return switch (settings.m_operator) {
            case EQ -> l -> Long.compare(l, val) == 0;
            case NEQ -> l -> Long.compare(l, val) != 0;
            case LT -> l -> Long.compare(l, val) < 0;
            case LTE -> l -> Long.compare(l, val) <= 0;
            case GT -> l -> Long.compare(l, val) > 0;
            case GTE -> l -> Long.compare(l, val) >= 0;
            case BETWEEN -> l -> Long.compare(l, lb) >= 0 && Long.compare(l, ub) <= 0;
            default -> throw new InvalidSettingsException(
                "Unexpected operator for integer numeric condition: " + settings.m_operator);
        };
    }
}
