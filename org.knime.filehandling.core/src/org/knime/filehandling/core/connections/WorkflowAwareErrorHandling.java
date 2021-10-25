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
 *   May 6, 2021 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.connections;

import java.nio.file.AccessMode;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.stream.Collectors;

/**
 * Contains methods and classes for error handling in {@link WorkflowAware} file systems.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
public final class WorkflowAwareErrorHandling {

    private WorkflowAwareErrorHandling() {
        // static utility class
    }

    /**
     * The kind of entity encountered by a {@link WorkflowAware} file system.
     *
     * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
     */
    public enum Entity {
            /**
             * A normal KNIME workflow.
             */
            WORKFLOW("Workflow", EnumSet.noneOf(Operation.class)),
            /**
             * A (shared) component.
             */
            COMPONENT("Component", EnumSet.noneOf(Operation.class)),
            /**
             * A workflow group (essentially a folder).
             */
            WORKFLOW_GROUP("Workflow group", EnumSet.of(Operation.LIST_FOLDER_CONTENT, Operation.CREATE_FOLDER)),
            /**
             * A (shared) meta node.
             */
            METANODE("Metanode", EnumSet.noneOf(Operation.class)),
            /**
             * A Hub Space.
             */
            SPACE("Space", EnumSet.of(Operation.CREATE_FOLDER, Operation.LIST_FOLDER_CONTENT)),
            /**
             * KNIME Server can't distinguish between a component and a metanode, hence we need to inform the user
             * that we might be dealing with either of the two.
             * TODO remove once the hub replaces the server and we can distinguish between components and metanodes.
             */
            WORKFLOW_TEMPLATE("Metanode/Component", EnumSet.noneOf(Operation.class)),
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

    /**
     * The different kinds of operations that can be performed in a {@link WorkflowAware} file system.
     *
     * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
     */
    public enum Operation {
            /**
             * Opening an input stream e.g. via
             * {@link Files#newInputStream(java.nio.file.Path, java.nio.file.OpenOption...)}.
             */
            NEW_INPUT_STREAM("Reading a %s is not possible.", true),
            /**
             * Opening an output stream e.g. via
             * {@link Files#newOutputStream(java.nio.file.Path, java.nio.file.OpenOption...)}.
             */
            NEW_OUTPUT_STREAM("Overwriting a %s is not possible.", true),
            /**
             * Listing the content of a folder e.g. via
             * {@link Files#walk(java.nio.file.Path, java.nio.file.FileVisitOption...)}.
             */
            LIST_FOLDER_CONTENT("It's not possible to list the folder content of a %s.", false),
            /**
             * Creating a new folder e.g. via
             * {@link Files#createDirectory(java.nio.file.Path, java.nio.file.attribute.FileAttribute...)}.
             */
            CREATE_FOLDER("It's not possible to create a folder in a %s.", false);

        private final String m_errorTemplate;

        private final boolean m_appendIntegratedDeploymentNoteForWorkflows;

        private Operation(final String errorTemplate, final boolean appendIntegratedDeploymentNoteForWorkflows) {
            m_errorTemplate = errorTemplate;
            m_appendIntegratedDeploymentNoteForWorkflows = appendIntegratedDeploymentNoteForWorkflows;
        }

        String createErrorMessage(final Entity entity) {
            final String message = String.format(m_errorTemplate, entity);
            if (m_appendIntegratedDeploymentNoteForWorkflows && entity == Entity.WORKFLOW) {
                return message + " See the Integrated Deployment extension for handling workflows.";
            } else {
                return message;
            }
        }

        @Override
        public String toString() {
            return m_errorTemplate;
        }

    }

    /**
     * Creates a standardized exception for {@link WorkflowAware} file systems telling the user that accessing a
     * workflow in the provided manners is not supported.
     *
     * @param workflowPath path to a workflow or component
     * @param modes the {@link AccessMode AccessModes} must not be empty or null
     * @return a {@link FileSystemException} that tells the user that accessing a workflow in the provided manner is not
     *         supported
     */
    public static FileSystemException createAccessKnimeObjectException(final String workflowPath,
        final AccessMode[] modes) {
        final String accessModeString = Arrays.stream(modes)//
            .map(AccessMode::toString)//
            .map(String::toLowerCase)//
            .collect(Collectors.joining("/"));
        final String reason = String.format("The access mode%s %s are not supported on workflows or components.",
            modes.length > 1 ? "s" : "", accessModeString);
        return new FileSystemException(workflowPath, null, reason);
    }

    /**
     * Creates a standardized exception for the case that users try to access paths within a workflow.
     *
     * @param pathIntoWorkflow the path into the workflow
     * @return a {@link FileSystemException} that tells the user that accessing paths within a workflow is not allowed
     */
    public static FileSystemException createAccessInsideWorkflowException(final String pathIntoWorkflow) {
        return new NoSuchFileException(pathIntoWorkflow);
    }

    /**
     * A {@link FileSystemException} thrown by {@link WorkflowAware} file systems e.g. if an unsupported operation is
     * attempted on workflow.
     *
     * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
     */
    public static class WorkflowAwareFSException extends FileSystemException {

        private static final long serialVersionUID = 1L;

        private final Operation m_operation;

        private final Entity m_entity;

        /**
         * Constructor for operations affecting a single file.
         *
         * @param path path to the Knime Object
         * @param reason for this exception
         */
        WorkflowAwareFSException(final String path, final String reason, final Entity entity,
            final Operation operation) {
            super(path, null, reason);
            m_operation = operation;
            m_entity = entity;
        }

        /**
         * @return the operation
         */
        public Operation getOperation() {
            return m_operation;
        }

        /**
         * @return the entity
         */
        public Entity getEntity() {
            return m_entity;
        }

    }
}
