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
 *   5 Mar 2020 ("Marc Bux, KNIME GmbH, Berlin, Germany"): created
 */
package org.knime.filehandling.core.connections.workflowaware;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Optional;

import org.knime.filehandling.core.connections.FSPath;
import org.knime.filehandling.core.util.TempPathCloseable;

/**
 * An interface providing methods for handling workflows. While such workflows are not much different than regular
 * directories on the file system, they often require special handling, e.g., to properly display them in the explorer
 * view.
 *
 * @author "Marc Bux, KNIME GmbH, Berlin, Germany"
 * @noreference non-public API
 * @noimplement non-public API
 */
public interface WorkflowAware {

    /**
     * Returns the type of {@link Entity} that the given path points to.
     *
     * @param path The path for which to determine the {@link Entity}.
     * @return the type of {@link Entity} that the given path points to
     * @throws NoSuchFileException If the given path does not exist.
     * @throws AccessDeniedException If the type of the entity could not be determined due to permission issues.
     * @throws IOException If anything else went wrong.
     */
    Entity getEntityOf(FSPath path) throws IOException;

    /**
     * Deploys a workflow (in local directory shape) to a destination path. Also provides the option of attempting to
     * open the workflow at the destination once it has been deployed.
     *
     * @param source the local file representing the to-be-deployed workflow
     * @param dest where to deploy the workflow
     * @param overwrite overwrite workflow if it already exists
     * @param attemptOpen if true, attempt to open the workflow after deployment
     *
     * @throws IOException if this method fails for any reason
     */
    void deployWorkflow(final Path source, final FSPath dest, final boolean overwrite, final boolean attemptOpen)
        throws IOException;

    /**
     * Turns a workflow path into a local workflow directory.
     *
     * @param workflowToRead The path of the workflow to read.
     * @return a {@link TempPathCloseable} whose path references the workflow in its local directory shape.
     * @throws IOException
     */
    TempPathCloseable toLocalWorkflowDir(FSPath workflowToRead) throws IOException;

    /**
     * @return the (default) mount ID of the underlying workflow repository or mountpoint.
     */
    Optional<String> getMountID();
}
