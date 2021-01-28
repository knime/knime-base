/*
 * ------------------------------------------------------------------------
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
 * ------------------------------------------------------------------------
 */

package org.knime.base.data.filter.row.dialog.util;

import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;

import org.knime.base.data.filter.row.dialog.OperatorParameters;
import org.knime.base.data.filter.row.dialog.OperatorValidation;
import org.knime.base.data.filter.row.dialog.ValidationResult;
import org.knime.base.data.filter.row.dialog.component.tree.model.TreeElement;
import org.knime.base.data.filter.row.dialog.component.tree.model.TreeGroup;
import org.knime.base.data.filter.row.dialog.model.AbstractElement;
import org.knime.base.data.filter.row.dialog.model.ColumnSpec;
import org.knime.base.data.filter.row.dialog.model.Condition;
import org.knime.base.data.filter.row.dialog.model.Group;
import org.knime.base.data.filter.row.dialog.model.Node;
import org.knime.base.data.filter.row.dialog.model.Operation;
import org.knime.base.data.filter.row.dialog.model.Operator;
import org.knime.base.data.filter.row.dialog.registry.OperatorKey;
import org.knime.base.data.filter.row.dialog.registry.OperatorRegistry;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;

/**
 * Helper class for a condition validation.
 *
 * @author Viktor Buria
 */
public final class NodeValidationHelper {

    /**
     * Error message when condition column specification is not defined.
     */
    public static final String ERROR_COLUMN_NOT_DEFINED = "Column is not defined";

    /**
     * Error message when condition column name doesn't exists in the input table specification.
     */
    private static final String ERROR_COLUMN_NOT_IN_INPUT_SPEC = "No column \"%s\" in the input specification";

    /**
     * Error message when condition column type is not the same as from the input table specification.
     */
    private static final String ERROR_COLUMN_HAS_INVALID_TYPE = "Column \"%s\" has the invalid data type \"%s\"";

    /**
     * Error message when condition operator is not defined.
     */
    private static final String ERROR_OPERATOR_NOT_DEFINED = "Operator is not defined";

    /**
     * Error message when function is not defined for the given column type and operator.
     */
    private static final String ERROR_FUNCTION_NOT_DEFINED =
        "Function is not defined for the \"%s\" column type and the \"%s\" operator";

    /**
     * Error message when group do not have children.
     */
    private static final String ERROR_GROUP_DOES_NOT_HAVE_CHILDREN = "Group must contains at least two child elements";

    /**
     * Error message when children elements have error(s).
     */
    private static final String ERROR_CHILDREN_HAVE_ERRORS = "Child elements have error(s)";

    private static final int MIN_CHILDREN_COUNT = 2;

    /**
     * Static factory method for a singular error result.
     *
     * @param message the error message
     * @return {@link ValidationResult}
     */
    public static ValidationResult createSingleErrorResult(final String message) {
        return new ValidationResult(ValidationResult.COMMON_ERROR_ID, message);
    }

    /**
     * Validates a {@link TreeNode} object.
     *
     * @param treeNode the {@link TreeNode}
     * @param tableSpec the {@linkplain DataTableSpec input table specification} to check
     * @param operatorRegistry the {@link OperatorRegistry}
     */
    public static void validateTreeNode(final DefaultMutableTreeNode treeNode, final DataTableSpec tableSpec,
        final OperatorRegistry<?> operatorRegistry) {
        if (treeNode == null) {
            return;
        }
        final Node node = NodeConverterHelper.convertToNode(treeNode);

        final Map<AbstractElement, ValidationResult> resultMap = validateNode(node, tableSpec, operatorRegistry);

        updateTreeNodeByValidationResults(treeNode, resultMap);
    }

    private static void updateTreeNodeByValidationResults(final DefaultMutableTreeNode treeNode,
        final Map<AbstractElement, ValidationResult> resultMap) {

        final TreeElement<?> nodeView = (TreeElement<?>)treeNode.getUserObject();

        nodeView.setValidationResult(resultMap.get(nodeView.getValue()));

        if (nodeView instanceof TreeGroup) {
            @SuppressWarnings("unchecked")
            final Enumeration<TreeNode> children = treeNode.children();
            while (children.hasMoreElements()) {
                final DefaultMutableTreeNode next = (DefaultMutableTreeNode)children.nextElement();

                updateTreeNodeByValidationResults(next, resultMap);
            }
        }
    }

    /**
     * Validates a {@link Node} object.
     *
     * @param node the {@link Node}
     * @param tableSpec the {@linkplain DataTableSpec input table specification} to check
     * @param operatorRegistry the {@link OperatorRegistry}
     * @return the mapping between a {@link Node} object and a {@link ValidationResult}
     */
    public static Map<AbstractElement, ValidationResult> validateNode(final Node node, final DataTableSpec tableSpec,
        final OperatorRegistry<?> operatorRegistry) {
        final Map<AbstractElement, ValidationResult> result = new LinkedHashMap<>();

        validateNode(result, Objects.requireNonNull(node, "node"), tableSpec,
            Objects.requireNonNull(operatorRegistry, "operatorRegistry"));

        return result;
    }

    private static void validateNode(final Map<AbstractElement, ValidationResult> results, final Node node,
        final DataTableSpec tableSpec, final OperatorRegistry<?> operatorRegistry) {

        final AbstractElement value = node.getElement();

        if (value instanceof Condition) {
            final Condition condition = (Condition)value;
            results.put(condition, validateCondition(condition, tableSpec, operatorRegistry));

        } else if (value instanceof Group) {
            final Group group = (Group)value;
            final ValidationResult groupResult = new ValidationResult();

            final int childCount = node.getChildren().size();
            if (childCount < MIN_CHILDREN_COUNT) {
                groupResult.addError(ValidationResult.COMMON_ERROR_ID, ERROR_GROUP_DOES_NOT_HAVE_CHILDREN);
            }

            node.getChildren().forEach(child -> validateNode(results, child, tableSpec, operatorRegistry));

            if (node.getChildren().stream().anyMatch(child -> results.get(child.getElement()).hasErrors())) {
                groupResult.addError(ValidationResult.COMMON_ERROR_ID, ERROR_CHILDREN_HAVE_ERRORS);
            }
            results.put(group, groupResult);
        } else {
            throw new IllegalArgumentException("Unknown type of the Node paiload " + value.getClass().getSimpleName());
        }
    }

    private static ValidationResult validateCondition(final Condition condition, final DataTableSpec tableSpec,
        final OperatorRegistry<?> operatorRegistry) {
        Objects.requireNonNull(condition, "condition");
        Objects.requireNonNull(operatorRegistry, "operatorRegistry");

        final ColumnSpec columnSpec = condition.getColumnSpec();
        if (columnSpec == null) {
            return createSingleErrorResult(ERROR_COLUMN_NOT_DEFINED);
        }
        final String columnName = columnSpec.getName();
        final DataType columnType = columnSpec.getType();

        if (tableSpec != null) {
            final DataColumnSpec inputColumnSpec = tableSpec.getColumnSpec(columnName);

            if (inputColumnSpec == null) {
                return createSingleErrorResult(String.format(ERROR_COLUMN_NOT_IN_INPUT_SPEC, columnName));
            }
            if (!inputColumnSpec.getType().equals(columnType)) {
                return createSingleErrorResult(String.format(ERROR_COLUMN_HAS_INVALID_TYPE, columnName, columnType));
            }
        }

        final Operation operation = condition.getOperation();
        if (operation == null) {
            // Yes, it is the same error message as for the missing operator.
            return createSingleErrorResult(ERROR_OPERATOR_NOT_DEFINED);
        }

        final Operator operator = operation.getOperator();
        if (operator == null) {
            return createSingleErrorResult(ERROR_OPERATOR_NOT_DEFINED);
        }

        final OperatorKey key = OperatorKey.key(columnType, operator);

        if (!operatorRegistry.findFunction(key).isPresent()) {
            return createSingleErrorResult(String.format(ERROR_FUNCTION_NOT_DEFINED, columnType, operator.getName()));
        }

        final Optional<OperatorValidation> validation = operatorRegistry.findValidation(key);
        if (validation.isPresent()) {
            return validation.get().apply(new OperatorParameters(columnSpec, operator, operation.getValues()));
        }
        return new ValidationResult();
    }

    private NodeValidationHelper() {
        throw new UnsupportedOperationException();
    }

}
