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

import java.util.Collections;
import java.util.Enumeration;
import java.util.Objects;
import java.util.Set;

import org.knime.base.data.filter.row.dialog.component.ColumnRole;
import org.knime.base.data.filter.row.dialog.model.AbstractElement;
import org.knime.base.data.filter.row.dialog.model.ColumnSpec;
import org.knime.base.data.filter.row.dialog.model.Condition;
import org.knime.base.data.filter.row.dialog.model.Group;
import org.knime.base.data.filter.row.dialog.model.GroupType;
import org.knime.base.data.filter.row.dialog.model.Node;
import org.knime.base.data.filter.row.dialog.model.Operation;
import org.knime.base.data.filter.row.dialog.model.Operator;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataType;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.config.ConfigRO;
import org.knime.core.node.config.ConfigWO;

import com.google.common.collect.Sets;

/**
 * Saves and restores a {@link Node} structure.
 *
 * @author Viktor Buria
 */
public final class NodeTreeSerializer {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(NodeTreeSerializer.class);

    // Group
    private static final String GROUP_CONFIG = "group_";

    private static final String GROUP_ID = "id";

    // Group Type
    private static final String GROUP_TYPE_CONFIG = "groupType";

    private static final String GROUP_TYPE_ID = "id";

    private static final String GROUP_TYPE_NAME = "name";

    // Condition
    private static final String CONDITION_CONFIG = "condition_";

    private static final String CONDITION_ID = "id";

    // Column Specification
    private static final String COLUMN_SPEC_CONFIG = "columnSpec";

    private static final String COLUMN_SPEC_NAME = "name";

    private static final String COLUMN_SPEC_DATA_TYPE = "dataType";

    private static final String COLUMN_POSSIBLE_VALUES = "possibleValues";

    private static final String COLUMN_ROLE = "columnRole";

    // Operation
    private static final String OPERATION_CONFIG = "operation";

    private static final String OPERATION_VALUES = "values";

    // Operator
    private static final String OPERATOR_CONFIG = "operator";

    private static final String OPERATOR_ID = "id";

    private static final String OPERATOR_NAME = "name";

    /**
     * Saves node's state into the configuration object.
     *
     * @param config the {@link ConfigWO} to store a node
     * @param node the {@link Node}
     */
    public static void saveNodeToConfig(final ConfigWO config, final Node node) {
        Objects.requireNonNull(config, "config");
        if (node == null) {
            LOGGER.debug("Saving the empty node into configuration.");
            return;
        }

        final AbstractElement value = node.getElement();
        if (value instanceof Group) {
            final Group group = (Group)value;
            final ConfigWO groupConfig = config.addConfig(GROUP_CONFIG + group.getId());
            groupConfig.addInt(GROUP_ID, group.getId());

            final GroupType groupType = group.getType();
            if (groupType != null) {
                final ConfigWO groupTypeConfig = groupConfig.addConfig(GROUP_TYPE_CONFIG);
                groupTypeConfig.addString(GROUP_TYPE_ID, groupType.getId());
                groupTypeConfig.addString(GROUP_TYPE_NAME, groupType.getName());
            }

            node.getChildren().forEach(childNode -> saveNodeToConfig(groupConfig, childNode));

        } else if (value instanceof Condition) {
            final Condition condition = (Condition)value;
            final ConfigWO conditionConfig = config.addConfig(CONDITION_CONFIG + condition.getId());
            conditionConfig.addInt(CONDITION_ID, condition.getId());

            final ColumnSpec columnSpec = condition.getColumnSpec();
            if (columnSpec != null) {
                final ConfigWO columnSpecConfig = conditionConfig.addConfig(COLUMN_SPEC_CONFIG);
                columnSpecConfig.addString(COLUMN_SPEC_NAME, columnSpec.getName());
                columnSpecConfig.addDataType(COLUMN_SPEC_DATA_TYPE, columnSpec.getType());
                final Set<DataCell> possibleValues = columnSpec.getPossibleValues();
                columnSpecConfig.addDataCellArray(COLUMN_POSSIBLE_VALUES,
                    possibleValues.toArray(new DataCell[possibleValues.size()]));
                columnSpecConfig.addString(COLUMN_ROLE, columnSpec.getRole().name());
            }

            final Operation operation = condition.getOperation();
            if (operation != null) {
                final ConfigWO operationConfig = conditionConfig.addConfig(OPERATION_CONFIG);
                operationConfig.addStringArray(OPERATION_VALUES, operation.getValues());

                final Operator operator = operation.getOperator();
                if (operator != null) {
                    final ConfigWO operatorConfig = operationConfig.addConfig(OPERATOR_CONFIG);
                    operatorConfig.addString(OPERATOR_ID, operator.getId());
                    operatorConfig.addString(OPERATOR_NAME, operator.getName());
                }
            }
        } else {
            throw new IllegalArgumentException("Unknown type of an element: " + node.getClass().getSimpleName());
        }
    }

    /**
     * Loads node's state from the configuration object.
     *
     * @param config the {@link ConfigRO}
     * @return the serialized {@link Node} object
     * @throws InvalidSettingsException if a node can't be restored
     */
    public static Node loadNodeFromConfig(final ConfigRO config) throws InvalidSettingsException {
        Objects.requireNonNull(config, "config");

        Node root = null;
        final Enumeration<?> children = config.children();
        while (children.hasMoreElements()) {
            final Object child = children.nextElement();
            if (child instanceof ConfigRO) {
                final ConfigRO childConfig = (ConfigRO)child;
                if (isGroup(childConfig)) {
                    root = loadGroup(childConfig);

                } else if (isCondition(childConfig)) {
                    root = loadCondition(childConfig);

                } else {
                    throw new IllegalArgumentException("Unknown type of the configuration " + config.getKey());
                }
            }
        }

        return root;
    }

    private static boolean isGroup(final ConfigRO config) {
        return config.getKey().startsWith(GROUP_CONFIG);
    }

    private static Node loadGroup(final ConfigRO config) throws InvalidSettingsException {
        final Node groupNode = new Node(createGroup(config));

        final Enumeration<?> children = config.children();
        while (children.hasMoreElements()) {

            final Object child = children.nextElement();
            if (child instanceof ConfigRO) {

                final ConfigRO childConfig = (ConfigRO)child;
                if (isGroup(childConfig)) {
                    groupNode.addChild(loadGroup(childConfig));
                } else if (isCondition(childConfig)) {
                    groupNode.addChild(loadCondition(childConfig));
                }
            }
        }
        return groupNode;
    }

    private static Group createGroup(final ConfigRO config) throws InvalidSettingsException {
        final Group group = new Group(config.getInt(GROUP_ID));

        final ConfigRO groupTypeConfig =
            getSubConfig(config, GROUP_TYPE_CONFIG, "Type is missing for the group #" + group.getId() + ".");

        group.setType(createGroupType(groupTypeConfig));
        return group;
    }

    private static GroupType createGroupType(final ConfigRO config) throws InvalidSettingsException {
        return new GroupType(config.getString(GROUP_TYPE_ID), config.getString(GROUP_TYPE_NAME));
    }

    private static boolean isCondition(final ConfigRO config) {
        return config.getKey().startsWith(CONDITION_CONFIG);
    }

    private static Node loadCondition(final ConfigRO config) throws InvalidSettingsException {
        return new Node(createCondition(config));
    }

    private static Condition createCondition(final ConfigRO config) throws InvalidSettingsException {
        final Condition condition = new Condition(config.getInt(CONDITION_ID));

        final ConfigRO columnSpecConfig = getSubConfig(config, COLUMN_SPEC_CONFIG,
            "Column is not defined for the condition #" + condition.getId() + ".");

        condition.setColumnSpec(createColumnSpec(columnSpecConfig));

        final ConfigRO operationConfig = getSubConfig(config, OPERATION_CONFIG,
            "Operation is not defined for the the condition #" + condition.getId() + ".");

        condition.setOperation(createOperation(operationConfig));
        return condition;
    }

    private static ColumnSpec createColumnSpec(final ConfigRO config) throws InvalidSettingsException {
        final String name = config.getString(COLUMN_SPEC_NAME);
        final DataType type = config.getDataType(COLUMN_SPEC_DATA_TYPE);
        return new ColumnSpec(name, type, getPossibleValues(config), getColumnRole(config));
    }

    private static Set<DataCell> getPossibleValues(final ConfigRO config) {
        final DataCell[] possibleValues = config.getDataCellArray(COLUMN_POSSIBLE_VALUES, (DataCell[]) null);
        return possibleValues == null ? Collections.emptySet() : Sets.newHashSet(possibleValues);
    }

    private static ColumnRole getColumnRole(final ConfigRO config) {
        final String columnRoleId = config.getString(COLUMN_ROLE, null);
        return columnRoleId == null ? ColumnRole.ORDINARY : ColumnRole.valueOf(columnRoleId);
    }

    private static Operation createOperation(final ConfigRO config) throws InvalidSettingsException {
        final ConfigRO operatorConfig = getSubConfig(config, OPERATOR_CONFIG, "Operator is not defined.");

        final Operator operator = operatorConfig == null ? null : createOperator(operatorConfig);

        final Operation operation = new Operation();
        operation.setOperator(operator);
        try {
            final String[] values = config.getStringArray(OPERATION_VALUES);
            operation.setValues(values);
        } catch (InvalidSettingsException e) {
            LOGGER.debug("Retrieving the operation values failed.", e);
            //ignore
        }
        return operation;
    }

    private static Operator createOperator(final ConfigRO config) throws InvalidSettingsException {
        return new Operator(config.getString(OPERATOR_ID), config.getString(OPERATOR_NAME));
    }

    private static ConfigRO getSubConfig(final ConfigRO config, final String subConfigName, final String errorMessage)
        throws InvalidSettingsException {
        final ConfigRO subConfig;
        try {
            subConfig = config.getConfig(subConfigName);
        } catch (InvalidSettingsException e) {
            throw new InvalidSettingsException(errorMessage, e);
        }
        return subConfig;
    }

    private NodeTreeSerializer() {
        throw new UnsupportedOperationException();
    }

}
