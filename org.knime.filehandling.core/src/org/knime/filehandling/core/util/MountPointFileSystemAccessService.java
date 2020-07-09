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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.knime.core.node.NodeLogger;
import org.knime.filehandling.core.connections.base.attributes.BaseFileAttributes;

/**
 * Service that provides access to the file systems of mounted mountpoints.
 *
 * </p>
 * NB This exists to prevent a dependency on org.knime.workbench.explorer.view
 *
 * @author Gabriel Einsdorf
 * @noreference non-public API
 */
public final class MountPointFileSystemAccessService {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(MountPointFileSystemAccessService.class);

    /** The id of the mountpoint id provider extension point. */
    public static final String EXT_POINT_ID = "org.knime.filehandling.core.MountPointFileSystemAccess";

    /** The attribute of the extension point. */
    public static final String EXT_POINT_ATTR_DF = "MountPointFileSystemAccess";

    private static MountPointFileSystemAccessService m_instance;

    private final List<MountPointFileSystemAccess> m_providers = new ArrayList<>();

    private MountPointFileSystemAccessService() {
        try {
            final IExtensionRegistry registry = Platform.getExtensionRegistry();
            final IExtensionPoint point = registry.getExtensionPoint(EXT_POINT_ID);
            if (point == null) {
                LOGGER.error("Invalid extension point: " + EXT_POINT_ID);
                throw new IllegalStateException("ACTIVATION ERROR: --> Invalid extension point: " + EXT_POINT_ID);
            }
            for (final IConfigurationElement elem : point.getConfigurationElements()) {
                final String operator = elem.getAttribute(EXT_POINT_ATTR_DF);
                final String decl = elem.getDeclaringExtension().getUniqueIdentifier();

                if ((operator == null) || operator.isEmpty()) {
                    LOGGER.error("The extension '" + decl + "' doesn't provide the required attribute '"
                        + EXT_POINT_ATTR_DF + "'");
                    LOGGER.error("Extension " + decl + " ignored.");
                    continue;
                }
                try {
                    m_providers.add((MountPointFileSystemAccess)elem.createExecutableExtension(EXT_POINT_ATTR_DF));
                } catch (final Throwable t) {
                    LOGGER.error("Problems during initialization of provider operator (with id '" + operator + "'.)",
                        t);
                    if (decl != null) {
                        LOGGER.error("Extension " + decl + " ignored.", t);
                    }
                }
            }
        } catch (final Exception e) {
            LOGGER.error("Exception while registering aggregation operator extensions", e);
        }
    }

    /**
     * @return get the sole instance of the {@link MountPointFileSystemAccessService}
     */
    public static MountPointFileSystemAccessService instance() {
        if (m_instance == null) {
            m_instance = new MountPointFileSystemAccessService();
        }
        return m_instance;
    }

    /**
     * @return A list of the ids of all mounted mountpoints
     */
    public List<String> getAllMountedIDs() {
        final List<String> mountpointIDs = new ArrayList<>();
        for (final MountPointFileSystemAccess p : m_providers) {
            mountpointIDs.addAll(p.getMountedIDs());
        }
        return mountpointIDs;
    }

    /**
     * Resolves a KNIME URL.
     *
     * @param url the URL to be resolved
     * @return the resolved KNIME URL
     * @throws IOException if the KNIME URL cannot be resolved
     */
    public URL resolveKNIMEURL(final URL url) throws IOException {
        final Optional<MountPointFileSystemAccess> findFirst = m_providers.stream().findFirst();
        if (findFirst.isPresent()) {
            return findFirst.get().resolveKNIMEURL(url);
        }
        throw new RuntimeException("No implementations for the " + EXT_POINT_ID + " available");
    }

    private MountPointFileSystemAccess getProvider() {
        final Optional<MountPointFileSystemAccess> findFirst = m_providers.stream().findFirst();
        if (findFirst.isPresent()) {
            return findFirst.get();
        }
        throw new RuntimeException("No implementations for the " + EXT_POINT_ID + " available");
    }

    /**
     * Lists all the files and folders at a given URI.
     *
     * @param uri the location of the directory
     * @return all files and folders in the given directory
     * @throws IOException if files could not be listed
     */
    public List<URI> listFiles(final URI uri) throws IOException {
        return getProvider().listFiles(uri);
    }

    /**
     * Gets the file attributes to a given URI.
     *
     * @param uri the URI of the file
     * @return the attributes of the URI
     * @throws IOException if attributes could not be created
     *
     */
    public BaseFileAttributes getFileAttributes(final URI uri) throws IOException {
        return getProvider().getFileAttributes(uri);
    }

    /**
     * Copies a file from the source to the target.
     *
     * @param source source location
     * @param target target destination
     * @return true if file was copied
     * @throws IOException if file could not be copied
     */
    public boolean copyFile(final URI source, final URI target) throws IOException {
        return getProvider().copyFile(source, target);
    }

    /**
     * Moves a file from the source to the target.
     *
     * @param source source location
     * @param target target destination
     * @return true if file was moved
     * @throws IOException if file could not be created
     */
    public boolean moveFile(final URI source, final URI target) throws IOException {
        return getProvider().moveFile(source, target);
    }

    /**
     * Deletes a file at the URI location.
     *
     * @param uri file to be deleted
     * @return true if file was deleted
     * @throws IOException if file could not be deleted
     */
    public boolean deleteFile(final URI uri) throws IOException {
        return getProvider().deleteFile(uri);
    }

    /**
     * Creates a directory at the given URI location.
     *
     * @param uri the location of the directory to be created
     * @throws IOException if directory could not be created
     */
    public void createDirectory(final URI uri) throws IOException {
        getProvider().createDirectory(uri);
    }

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
    public void deployWorkflow(final File source, final URI target, final boolean overwrite, final boolean attemptOpen)
        throws IOException {
        getProvider().deployWorkflow(source, target, overwrite, attemptOpen);
    }

    /**
     * Checks whether a file at the given URI location is readable.
     *
     * @param uri the location of the file
     * @return true if the file is readable
     * @throws IOException if mounpoint does not exist or information fetching fails
     */
    public boolean isReadable(final URI uri) throws IOException {
        return getProvider().isReadable(uri);
    }

    /**
     * Checks whether a URI points to a workflow.
     *
     * @param uri the location to be checked
     * @return true if the URI is a workflow
     */
    public boolean isWorkflow(final URI uri) {
        return getProvider().isWorkflow(uri);
    }

    /**
     * Returns the default directory of this mount point.
     *
     * @param uri the location of the mount point
     * @return the default directory of this mount point
     */
    public URI getDefaultDirectory(final URI uri) {
        return getProvider().getDefaultDirectory(uri);
    }
}
