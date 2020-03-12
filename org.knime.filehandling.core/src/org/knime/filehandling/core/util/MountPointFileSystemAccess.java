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
 *   Oct 9, 2019 (gabriel): created
 */
package org.knime.filehandling.core.util;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.List;

import org.knime.filehandling.core.connections.base.attributes.FSFileAttributes;

/**
 * Interface for MountPointFileSystemAccess instances.
 *
 * @author Gabriel Einsdorf
 */
public interface MountPointFileSystemAccess {

    /**
     * @return The ids of all mounted mountpoints
     */
    List<String> getMountedIDs();

    /**
     * Resolves a KNIME URL.
     *
     * @param url the KNIME URL to be resolved
     * @return the resolved URL
     * @throws IOException if URL cannot be resolved
     */
    URL resolveKNIMEURL(final URL url) throws IOException;

    /**
     * Lists the children of the Mountpoint object represented by the given URI.
     *
     * @param uri the uri of the object
     * @return list of URI representing the children of the input object
     * @throws IOException if the files could not be listed
     */
    List<URI> listFiles(final URI uri) throws IOException;

    /**
     * Gets the {@link FSFileAttributes} for the Mountpoint object represented by the given URI.
     *
     * @param uri he uri of the object
     * @return the {@link FSFileAttributes}
     * @throws IOException if the attributes could not be build
     */
    FSFileAttributes getFileAttributes(URI uri) throws IOException;

    /**
     * Copies a file from the source to the target.
     *
     * @param source source location
     * @param target target destination
     * @return true if file was copied
     * @throws IOException if the file could not be copied
     */
    boolean copyFile(URI source, URI target) throws IOException;

    /**
     * Moves a file from the source to the target.
     *
     * @param source source location
     * @param target target destination
     * @return true if file was moved
     * @throws IOException if the file could not be moved
     */
    boolean moveFile(URI source, URI target) throws IOException;

    /**
     * Deletes a file at the URI location.
     *
     * @param uri file to be deleted
     * @return true if file was deleted
     * @throws IOException if the file could not be deleted
     */
    boolean deleteFile(URI uri) throws IOException;

    /**
     * Creates a directory at the given URI location/
     *
     * @param uri the location of the directory to be created
     * @throws IOException if the directory could not be created
     */
    void createDirectory(URI uri) throws IOException;

    /**
     * Deploys a workflow from a local file source to a target URI. Also provides the option of attempting to open the
     * workflow at the target location once it has been deployed.
     *
     * @param source the local file representing the to-be-deployed workflow
     * @param target where to deploy the workflow
     * @param overwrite overwrite workflow if it already exists
     * @param attemptOpen if true, attempt to open the workflow after deployment
     *
     * @throws IOException if this method fails for any reason
     */
    void deployWorkflow(File source, URI target, boolean overwrite, boolean attemptOpen) throws IOException;

    /**
     * Checks whether a file at the given URI location is readable.
     *
     * @param uri the location of the file
     * @return true if the file is readable
     * @throws IOException if mountpoint does not exist or the information fetching fails
     */
    boolean isReadable(URI uri) throws IOException;

    /**
     * Checks whether a URI points to a workflow.
     *
     * @param uri the location to be checked
     * @return true if the URI is a workflow
     */
    boolean isWorkflow(URI uri);

    /**
     * Returns the default directory of this mount point.
     *
     * @param uri the location of the mount point
     * @return the default directory of this mount point
     */
    URI getDefaultDirectory(URI uri);
}
