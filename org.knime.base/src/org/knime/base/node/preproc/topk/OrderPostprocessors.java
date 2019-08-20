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
 *   Aug 20, 2019 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.base.node.preproc.topk;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.knime.base.data.filter.column.FilterColumnRow;
import org.knime.core.data.DataRow;
import org.knime.core.data.LongValue;
import org.knime.core.node.util.CheckUtils;

/**
 * Static factory class for OrderPostprocessor implementations. Also contains some methods that are useful in that
 * context.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
final class OrderPostprocessors {

    private OrderPostprocessors() {
        // static factory class
    }

    static OrderPostprocessor noOp() {
        return (s, c) -> s;
    }

    static OrderPostprocessor sort(final Comparator<DataRow> comparator) {
        return (s, c) -> {
            final List<DataRow> temp = new ArrayList<>(s);
            Collections.sort(temp, comparator);
            return temp;
        };
    }

    static OrderPostprocessor removeLastColumn() {
        return (s, c) -> {
            final Iterator<DataRow> iter = s.iterator();
            CheckUtils.checkArgument(iter.hasNext(), "The selection must contain at least one row.");
            final int[] filterColumns = IntStream.range(0, iter.next().getNumCells() - 1).toArray();
            return s.stream().map(r -> new FilterColumnRow(r, filterColumns)).collect(Collectors.toList());
        };
    }

    @SuppressWarnings("null") // we explicitly check that the processors are not null
    static OrderPostprocessor chain(final OrderPostprocessor...postprocessors) {
        CheckUtils.checkNotNull(postprocessors);
        CheckUtils.checkArgument(postprocessors.length > 1, "At least two postprocessors are required for chaining.");
        return (s, c) -> {
            Collection<DataRow> selection = s;
            for (OrderPostprocessor processor : postprocessors) {
                CheckUtils.checkState(processor != null, "Postprocessors used in chains must not be null.");
                selection = processor.postprocessSelection(selection, c);
            }
            return selection;
        };
    }

    static int compareInputOrderColumn(final DataRow r1, final DataRow r2) {
        assert r1.getNumCells() == r2.getNumCells();
        final int orderIdx = r1.getNumCells() - 1;
        final LongValue inOrder1 = (LongValue)r1.getCell(orderIdx);
        final LongValue inOrder2 = (LongValue)r2.getCell(orderIdx);
        return Long.compare(inOrder1.getLongValue(), inOrder2.getLongValue());
    }

    @SafeVarargs
    static <T> Comparator<T> chain(final Comparator<T>... comparators) {
        CheckUtils.checkNotNull(comparators);
        CheckUtils.checkArgument(comparators.length > 1, "At least two comparators are required for chaining.");
        return (o1, o2) -> {
            for (Comparator<T> comparator : comparators) {
                CheckUtils.checkState(comparator != null, "Comparators used in chains must not be null.");
                @SuppressWarnings("null") // the line above ensures that comparator is not null
                final int comparison = comparator.compare(o1, o2);
                if (comparison != 0) {
                    return comparison;
                }
            }
            // the objects are equivalent according to all comparators
            return 0;
        };
    }

}
