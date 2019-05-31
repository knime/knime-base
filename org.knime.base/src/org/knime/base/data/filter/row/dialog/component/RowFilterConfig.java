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

package org.knime.base.data.filter.row.dialog.component;

import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;

import org.knime.base.data.filter.row.dialog.ValidationResult;
import org.knime.base.data.filter.row.dialog.model.AbstractElement;
import org.knime.base.data.filter.row.dialog.model.Condition;
import org.knime.base.data.filter.row.dialog.model.Group;
import org.knime.base.data.filter.row.dialog.model.GroupType;
import org.knime.base.data.filter.row.dialog.model.Node;
import org.knime.base.data.filter.row.dialog.registry.OperatorRegistry;
import org.knime.base.data.filter.row.dialog.util.NodeTreeSerializer;
import org.knime.base.data.filter.row.dialog.util.NodeValidationHelper;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.config.ConfigRO;
import org.knime.core.node.config.ConfigWO;

/**
 * Configuration object for the {@link RowFilterComponent}.
 *
 * @author Viktor Buria
 * @since 4.0
 */
public class RowFilterConfig {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(RowFilterConfig.class);

    private static final String ERROR_INVALID_CONDITIONS = "Configuration has invalid condition(s).";

    private final String m_configName;

    private final GroupType[] m_groupTypes;

    private Node m_root;

    /**
     * Creates a {@link RowFilterConfig}.
     *
     * @param configName the name of of Row Filter configuration
     * @param groupTypes the available group types.
     */
    public RowFilterConfig(final String configName, final GroupType[] groupTypes) {
        m_configName = Objects.requireNonNull(configName, "configName");
        m_groupTypes = Objects.requireNonNull(groupTypes, "groupTypes");
    }

    /**
     * Gets available group types.
     *
     * @return an array of the {@link GroupType}
     */
    public GroupType[] getGroupTypes() {
        return m_groupTypes;
    }

    /**
     * Gets the root element of a node tree structure.
     *
     * @return the root {@link Node}
     */
    public Node getRoot() {
        return m_root;
    }

    /**
     * Sets the root element of a node tree structure.
     *
     * @param root the root {@link Node} to set
     */
    public void setRoot(final Node root) {
        m_root = root;
    }

    /**
     * Gets the last added group ID.
     *
     * @return the group ID
     */
    public int findLastGroupId() {
        if (m_root == null) {
            return 0;
        }
        return findMaxId(m_root, 0, element -> element instanceof Group);
    }

    /**
     * Gets the last added condition ID.
     *
     * @return the condition ID
     */
    public int findLastConditionId() {
        if (m_root == null) {
            return 0;
        }
        return findMaxId(m_root, 0, element -> element instanceof Condition);
    }

    private static int findMaxId(final Node node, final int id, final Predicate<AbstractElement> predicate) {
        int maxId = id;

        if (predicate.test(node.getElement())) {
            maxId = Math.max(node.getElement().getId(), maxId);
        }

        for (Node child : node.getChildren()) {
            maxId = Math.max(findMaxId(child, maxId, predicate), maxId);
        }

        return maxId;
    }

    /**
     * Saves current configuration to the Node settings.
     *
     * @param settings the {@link NodeSettingsWO}
     */
    public void saveSettingsTo(final NodeSettingsWO settings) {
        final ConfigWO config = settings.addConfig(m_configName);
        NodeTreeSerializer.saveNodeToConfig(config, m_root);
    }

    /**
     * Validates configuration before loading from the Node settings.
     *
     * @param settings the {@link NodeSettingsRO}
     * @param tableSpec the {@linkplain DataTableSpec input table specification} to check
     * @param operatorRegistry the {@link OperatorRegistry} object
     * @throws InvalidSettingsException if settings are invalid or can't be retrieved
     */
    public void validateSettings(final NodeSettingsRO settings, final DataTableSpec tableSpec,
        final OperatorRegistry<?> operatorRegistry) throws InvalidSettingsException {
        final ConfigRO config = settings.getConfig(m_configName);

        final Node root;
        try {
            root = NodeTreeSerializer.loadNodeFromConfig(config);
        } catch (InvalidSettingsException e) {
            throw new InvalidSettingsException(ERROR_INVALID_CONDITIONS, e);
        }

        validateNode(root, tableSpec, operatorRegistry);
    }

    /**
     * Validates the current state of {@link RowFilterComponent}.
     *
     * @param tableSpec the {@linkplain DataTableSpec input table specification} to check
     * @param operatorRegistry the {@link OperatorRegistry} object
     * @throws InvalidSettingsException if any issue found during the validation process
     */
    public void validate(final DataTableSpec tableSpec, final OperatorRegistry<?> operatorRegistry)
        throws InvalidSettingsException {

        validateNode(getRoot(), tableSpec, operatorRegistry);
    }

    private static void validateNode(final Node node, final DataTableSpec tableSpec,
        final OperatorRegistry<?> operatorRegistry) throws InvalidSettingsException {
        if (node == null) {
            return;
        }

        final Map<AbstractElement, ValidationResult> results =
            NodeValidationHelper.validateNode(node, tableSpec, operatorRegistry);

        final StringBuilder builder = new StringBuilder();
        results.forEach((element, result) -> {
            if (result.hasErrors()) {
                if (element instanceof Condition) {
                    builder.append("\nCondition #").append(element.getId()).append(":\n");
                } else {
                    builder.append("\nGroup #").append(element.getId()).append(":\n");
                }
                result.getErrors().forEach(error -> builder.append("\t * ").append(error.getError()));
            }
        });

        final String errorMessage = builder.toString();
        if (!errorMessage.isEmpty()) {
            LOGGER.warn(errorMessage);
            throw new InvalidSettingsException(ERROR_INVALID_CONDITIONS);
        }
    }

    /**
     * Loads configuration from the Node settings.
     *
     * @param settings the {@link NodeSettingsRO}
     * @throws InvalidSettingsException if settings are invalid or can't be retrieved
     */
    public void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        final ConfigRO config = settings.getConfig(m_configName);
        m_root = NodeTreeSerializer.loadNodeFromConfig(config);
    }

}
