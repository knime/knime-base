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
 *   Nov 11, 2022 (bjoern): created
 */
package org.knime.filehandling.core.connections.workflowaware;

import java.util.EnumSet;

import org.knime.filehandling.core.connections.workflowaware.WorkflowAwareErrorHandling.Operation;
import org.knime.filehandling.core.connections.workflowaware.WorkflowAwareErrorHandling.WorkflowAwareFSException;

/**
 * The kind of entity encountered by a {@link WorkflowAware} file system.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
public enum Entity {
        /**
         * A normal KNIME workflow.
         */
        WORKFLOW("Workflow", EnumSet.of(Operation.GET_WORKFLOW, Operation.PUT_WORKFLOW)),
        /**
         * A (shared) component.
         */
        COMPONENT("Component", EnumSet.of(Operation.GET_WORKFLOW, Operation.PUT_WORKFLOW)),
        /**
         * A workflow group (essentially a folder).
         */
        WORKFLOW_GROUP("Workflow group", EnumSet.of(Operation.LIST_FOLDER_CONTENT, Operation.CREATE_FOLDER)),
        /**
         * A (shared) meta node.
         */
        METANODE("Metanode", EnumSet.of(Operation.GET_WORKFLOW, Operation.PUT_WORKFLOW)),
        /**
         * A Hub Space.
         */
        SPACE("Space", EnumSet.of(Operation.CREATE_FOLDER, Operation.LIST_FOLDER_CONTENT)),
        /**
         * KNIME Server can't distinguish between a component and a metanode, hence we need to inform the user
         * that we might be dealing with either of the two.
         * TODO remove once the hub replaces the server and we can distinguish between components and metanodes.
         */
        WORKFLOW_TEMPLATE("Metanode/Component", EnumSet.of(Operation.GET_WORKFLOW, Operation.PUT_WORKFLOW)),
        /**
         * A data item. Essentially anything that isn't one of the other entities.
         */
        DATA("Data item", EnumSet.of(Operation.NEW_INPUT_STREAM, Operation.NEW_OUTPUT_STREAM));


    private final EnumSet<Operation> m_supportedOperations;

    private final String m_readableString;

    private Entity(final String readableString, final EnumSet<Operation> supportedOperations) {
        m_supportedOperations = supportedOperations;
        m_readableString = readableString;
    }

    boolean supports(final Operation operation) {
        return m_supportedOperations.contains(operation);
    }

    /**
     * Checks whether this entity supports the provided operation.
     *
     * @param path used for error messages
     * @param operation to check for support
     * @throws WorkflowAwareFSException if this entity doesn't support the provided operation
     */
    public void checkSupport(final String path, final Operation operation) throws WorkflowAwareFSException {
        if (!supports(operation)) {
            final String reason = operation.createErrorMessage(this);
            throw new WorkflowAwareFSException(path, reason, this, operation);
        }
    }

    @Override
    public String toString() {
        return m_readableString;
    }
}