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
package org.knime.filehandling.core.connections.workflowaware;

import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;

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
            CREATE_FOLDER("It's not possible to create a folder in a %s.", false),
            /**
             * Reading a workflow or workflow-like object, e.g. via
             * {@link WorkflowAware#toLocalWorkflowDir(org.knime.filehandling.core.connections.FSPath)}.
             */
            GET_WORKFLOW("Reading a %s is not possible.", true),
            /**
             * Deploying a workflow or workflow-like object, e.g. via
             * {@link WorkflowAware#deployWorkflow(java.nio.file.Path, org.knime.filehandling.core.connections.FSPath, boolean, boolean)}.
             */
            PUT_WORKFLOW("Deploying a %s is not possible.", true);

        private final String m_errorTemplate;

        private final boolean m_appendIntegratedDeploymentNoteForWorkflows;

        private Operation(final String errorTemplate, final boolean appendIntegratedDeploymentNoteForWorkflows) {
            m_errorTemplate = errorTemplate;
            m_appendIntegratedDeploymentNoteForWorkflows = appendIntegratedDeploymentNoteForWorkflows;
        }

        String createErrorMessage(final Entity entity) {
            final var message = String.format(m_errorTemplate, entity);
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
