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
 *   Aug 21, 2019 (Perla Gjoka): created
 */
package org.knime.base.node.preproc.filter.row2.operator;

import java.util.function.Supplier;

import org.knime.base.data.filter.row.dialog.component.ColumnRole;
import org.knime.base.data.filter.row.dialog.model.ColumnSpec;
import org.knime.core.node.util.CheckUtils;

import com.google.common.collect.Range;

/**
 * Contains utility methods for the handling of ranges.
 *
 * @author Perla Gjoka, KNIME GmbH, Konstanz, Germany
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
final class RangeExtractorUtil {

    private RangeExtractorUtil() {
        // static utility class
    }

    /**
     * @param spec holds the Column Spec of the column chosen by the user.
     * @param parameters holds the parameters picked by the user in the dialog.
     */
    static Range<Long> all(final ColumnSpec spec, final String[] parameters) {
        return Range.all();
    }

    static Range<Long> equalInclusive(final ColumnSpec spec, final String[] parameters) {
        return decorateWithIdxCheck(spec, () -> equalInclusive(parameters));
    }

    private static Range<Long> equalInclusive(final String[] parameters) {
        checkOneParameter(parameters);
        long parsed = Long.parseLong(parameters[0]);
        return Range.closed(parsed, parsed);
    }

    static Range<Long> startingInclusive(final ColumnSpec spec, final String[] parameters) {
        return decorateWithIdxCheck(spec, () -> startingInclusive(parameters));
    }

    private static Range<Long> startingInclusive(final String[] parameters) {
        checkOneParameter(parameters);
        return Range.atLeast(Long.parseLong(parameters[0]));
    }

    static Range<Long> startingExclusive(final ColumnSpec spec, final String[] parameters) {
        return decorateWithIdxCheck(spec, () -> startingExclusive(parameters));
    }

    private static Range<Long> startingExclusive(final String[] parameters) {
        checkOneParameter(parameters);
        return Range.greaterThan(Long.parseLong(parameters[0]));
    }

    static Range<Long> endingInclusive(final ColumnSpec spec, final String[] parameters) {
        return decorateWithIdxCheck(spec, () -> endingInclusive(parameters));
    }

    private static Range<Long> endingInclusive(final String[] parameters) {
        checkOneParameter(parameters);
        return Range.atMost(Long.parseLong(parameters[0]));
    }

    static Range<Long> endingExclusive(final ColumnSpec spec, final String[] parameters) {
        return decorateWithIdxCheck(spec, () -> endingExclusive(parameters));
    }

    private static Range<Long> endingExclusive(final String[] parameters) {
        checkOneParameter(parameters);
        return Range.lessThan(Long.parseLong(parameters[0]));
    }

    static Range<Long> between(final ColumnSpec spec, final String[] parameters) {
        return decorateWithIdxCheck(spec, () -> between(parameters));
    }

    private static Range<Long> between(final String[] parameters) {
        CheckUtils.checkArgument(parameters.length == 2, "Expected two parameters but received %s.", parameters.length);
        return Range.closed(Long.parseLong(parameters[0]), Long.parseLong(parameters[1]));
    }

    private static Range<Long> decorateWithIdxCheck(final ColumnSpec spec, final Supplier<Range<Long>> rangeSupplier) {
        if (spec.getRole() == ColumnRole.ROW_INDEX) {
            return rangeSupplier.get();
        }
        return Range.all();
    }

    private static void checkOneParameter(final String[] parameters) {
        CheckUtils.checkArgument(parameters.length == 1, "Expected one parameter, but received %s", parameters.length);
    }
}
