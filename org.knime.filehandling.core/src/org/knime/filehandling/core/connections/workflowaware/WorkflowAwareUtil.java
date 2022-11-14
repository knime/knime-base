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

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.NoSuchFileException;
import java.util.Optional;

import org.knime.filehandling.core.connections.FSPath;

/**
 * Utility class to work with {@link WorkflowAware} files systems.
 *
 * @author Bjoern Lohrmann, KNIME GmbH
 */
public final class WorkflowAwareUtil {

    private WorkflowAwareUtil() {
    }

    /**
     * Determines whether the file system of the given path is {@link WorkflowAware}.
     *
     * @param path The whose file system to test.
     * @return true, if the file system is {@link WorkflowAware}, false otherwise.
     */
    @SuppressWarnings("resource")
    public static boolean isWorkflowAwarePath(final FSPath path) {
        return path.getFileSystem().getWorkflowAware().isPresent();
    }

    /**
     * Determines the type of {@link Entity} of the given path.
     *
     * @param path The path whose {@link Entity} to determine.
     * @return an {@link Optional} if the underlying file system is {@link WorkflowAware} and the {@link Entity} could
     *         be determined; empty otherwise.
     * @throws NoSuchFileException If the given path does not exist.
     * @throws AccessDeniedException If the type of the entity could not be determined due to permission issues.
     * @throws IOException If anything else went wrong.
     */
    @SuppressWarnings("resource")
    public static Optional<Entity> getWorkflowAwareEntityOf(final FSPath path) throws IOException {
        if (isWorkflowAwarePath(path)) {
            final var wfAware = path.getFileSystem().getWorkflowAware().orElseThrow();
            return Optional.of(wfAware.getEntityOf(path));
        } else {
            return Optional.empty();
        }
    }

    /**
     * Determines if the given path points to a "workflow-like" {@link Entity}. Workflows, components, metanodes and
     * workflow templates are "workflow-like".
     *
     * @param path The path to test.
     * @return true, if the underlying file system is {@link WorkflowAware} and the {@link Entity} of the path is
     *         workflow-like, false otherwise.
     * @throws NoSuchFileException If the given path does not exist.
     * @throws AccessDeniedException If the type of the entity could not be determined due to permission issues.
     * @throws IOException If anything else went wrong.
     */
    public static boolean isWorkflowLikeEntity(final FSPath path) throws IOException {
        return getWorkflowAwareEntityOf(path) //
            .map(WorkflowAwareUtil::isWorkflowLikeEntity) //
            .orElse(false);
    }

    private static boolean isWorkflowLikeEntity(final Entity entity) {
        switch (entity) {
            case COMPONENT:
            case METANODE:
            case WORKFLOW:
            case WORKFLOW_TEMPLATE:
                return true;
            default:
                return false;
        }
    }
}
