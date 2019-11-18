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
 *   Nov 8, 2019 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.base.node.preproc.filter.row2;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import org.knime.base.data.filter.row.dialog.OperatorParameters;
import org.knime.base.data.filter.row.dialog.component.DefaultGroupTypes;
import org.knime.base.data.filter.row.dialog.model.AbstractElement;
import org.knime.base.data.filter.row.dialog.model.ColumnSpec;
import org.knime.base.data.filter.row.dialog.model.Condition;
import org.knime.base.data.filter.row.dialog.model.Group;
import org.knime.base.data.filter.row.dialog.model.GroupType;
import org.knime.base.data.filter.row.dialog.model.Node;
import org.knime.base.data.filter.row.dialog.model.Operation;
import org.knime.base.data.filter.row.dialog.model.Operator;
import org.knime.base.data.filter.row.dialog.registry.OperatorKey;
import org.knime.base.node.preproc.filter.row2.operator.KnimeOperatorFunction;
import org.knime.base.node.preproc.filter.row2.operator.KnimeRowFilterOperatorRegistry;
import org.knime.base.node.preproc.filter.row2.operator.RowPredicate;
import org.knime.base.node.preproc.filter.row2.operator.RowPredicateFactory;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;

/**
 * Builds a {@link RowPredicate} from a tree of conditions.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
final class RowPredicateUtil {

    private RowPredicateUtil() {
        // static utility class
    }

    private static final KnimeRowFilterOperatorRegistry OPERATOR_REGISTRY =
        KnimeRowFilterOperatorRegistry.getInstance();

    /**
     * Checks if the user has set only one condition or a group of them. If the condition is single, then it is handled
     * by the method: consumeCondition. If the conditions are a group, then they are handled by the method:
     * consumeGroup.
     */
    static RowPredicate consumeNode(final Node node, final DataTableSpec tableSpec) throws InvalidSettingsException {
        final AbstractElement value = node.getElement();

        if (value instanceof Condition) {
            final Condition condition = (Condition)value;
            assert node.getChildren().isEmpty() : "A condition should be a leaf in the query tree.";
            return consumeCondition(condition, tableSpec);

        } else if (value instanceof Group) {
            return consumeGroup((Group)value, node.getChildren().iterator(), tableSpec);
        } else {
            throw new InvalidSettingsException(String.format("Unknown element %s encountered", value));
        }
    }

    /**
     * Checks how many conditions are in the group. The structure of the group is like a tree, so it iterates through
     * all its branches. After having consumed all the conditions in the group of conditions it combines them, by
     * calling the method combine group.
     */
    private static RowPredicate consumeGroup(final Group group, final Iterator<Node> childIterator,
        final DataTableSpec tableSpec) throws InvalidSettingsException {
        //find the first predicate
        assert childIterator.hasNext() : "Encountered empty group.";
        // create list
        List<RowPredicate> rowPredicateList = new ArrayList<>();

        while (childIterator.hasNext()) {
            // add to list
            final RowPredicate rowPredicate = consumeNode(childIterator.next(), tableSpec);
            rowPredicateList.add(rowPredicate);
        }
        // combine list
        return combineGroup(group.getType(), rowPredicateList);
    }

    /**
     * Iterates through all the list of RowPredicates created by the consume group and based on the condition, combines
     * them either by logical And or logical Or.
     */
    private static RowPredicate combineGroup(final GroupType type, final List<RowPredicate> predicates) {
        if (type.equals(DefaultGroupTypes.AND)) {
            return RowPredicate.and(predicates.iterator());
        } else if (type.equals(DefaultGroupTypes.OR)) {
            return RowPredicate.or(predicates.iterator());
        } else {
            throw new IllegalArgumentException("Unknown group type " + type);
        }
    }

    /**
     * When the condition is singular, after gathering all the user's choices, and filtered the column of the table
     * needed, a predicate is created, to show if the condition is fulfilled or not for the filtered data cell.
     */
    private static RowPredicate consumeCondition(final Condition condition, final DataTableSpec tableSpec)
        throws InvalidSettingsException {
        final ColumnSpec columnSpec = condition.getColumnSpec();
        final Operation operation = condition.getOperation();
        final Operator operator = operation.getOperator();
        final OperatorParameters parameters = new OperatorParameters(columnSpec, operator, operation.getValues());
        final OperatorKey key = OperatorKey.key(columnSpec.getType(), operator);
        final Optional<KnimeOperatorFunction> function = OPERATOR_REGISTRY.findFunction(key);

        if (!function.isPresent()) {
            throw new InvalidSettingsException("Can't find operator function by Key " + key);
        } else {

            final RowPredicateFactory rowPredicateFactory = function.get().apply(parameters);
            return rowPredicateFactory.createPredicate(tableSpec);
        }
    }

}
