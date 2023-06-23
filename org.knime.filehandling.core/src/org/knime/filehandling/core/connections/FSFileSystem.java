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
 *   Mar 6, 2020 (bjoern): created
 */
package org.knime.filehandling.core.connections;

import java.io.Closeable;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.ClosedFileSystemException;
import java.nio.file.FileSystem;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.knime.core.node.util.CheckUtils;
import org.knime.filehandling.core.connections.meta.FSType;
import org.knime.filehandling.core.connections.workflowaware.WorkflowAware;

/**
 * Abstract super class implemented by all NIO file systems provided in KNIME. This class adds the following features on
 * top of the vanilla NIO {@link FileSystem}:
 *
 * <ul>
 * <li>Usage of generics (makes file systems more convenient to implement)</li>
 * <li>Support for a working directory (see {@link #getWorkingDirectory()})</li>
 * <li>Support for {@link FSLocation} (see {@link #getPath(FSLocation)} and
 * {@link #checkCompatibility(FSLocationSpec)})</li>
 * <li>Support for cleaning up blocked resources when closing the file system.</li>
 * </ul>
 *
 * @author Bjoern Lohrmann, KNIME GmbH
 * @param <T> The type of path that this file system works with.
 * @noreference non-public API
 * @noextend non-public API
 */
public abstract class FSFileSystem<T extends FSPath> extends FileSystem {

    /**
     * The {@link FSLocationSpec} that describes this file system.
     */
    private final FSLocationSpec m_fsLocationSpec;

    /**
     * The working directory, which allows the file system provider methods to resolve paths to absolute ones.
     */
    private final String m_workingDirectory;

    /**
     * A set of {@link Closeable}s that will be closed when the file system is closed.
     */
    private final Set<Closeable> m_closeables = new HashSet<>();

    /**
     * A base URI that is used to construct the URI when invoking {@link FSPath#toUri()}.
     */
    private final URI m_fsBaseUri;

    private boolean m_isOpen = true;

    private boolean m_isClosing = false;

    /**
     * Creates a new instance.
     *
     * @param fsBaseUri A base URI that is used to construct the URI when invoking {@link FSPath#toUri()}. It's path, query or
     *            fragment will be ignored.
     * @param fsLocationSpec An {@link FSLocationSpec} that characterizes this file system.
     * @param workingDir The working directory to use (see {@link #getWorkingDirectory()}).
     */
    public FSFileSystem(final URI fsBaseUri, final FSLocationSpec fsLocationSpec, final String workingDir) {
        CheckUtils.checkArgument(fsBaseUri.getScheme() != null, "scheme of base URI must not be null");
        m_fsBaseUri = fsBaseUri;
        m_fsLocationSpec = fsLocationSpec;
        m_workingDirectory = workingDir;
    }

    /**
     * Creates a new instance.
     *
     * <p>
     * This constructor initializes the base URI (see {@link #getFileSystemBaseURI()}) to be <fs-type>://<uuid>.
     * </p>
     *
     * @param fsLocationSpec An {@link FSLocationSpec} that characterizes this file system.
     * @param workingDir The working directory to use (see {@link #getWorkingDirectory()}).
     */
    public FSFileSystem(final FSLocationSpec fsLocationSpec, final String workingDir) {
        this(createDefaultBaseURI(fsLocationSpec.getFSType()), fsLocationSpec, workingDir);
    }


    private static URI createDefaultBaseURI(final FSType fsType) {
        try {
            return new URI(fsType.getTypeId(), UUID.randomUUID().toString(), null, null);
        } catch (URISyntaxException ex) {
            throw new IllegalArgumentException("FSType is not URI-compatible: " + fsType.getTypeId(), ex);
        }
    }

    /**
     * Returns the {@link FSLocationSpec} for this file system instance.
     *
     * @return the {@link FSLocationSpec} for this file system instance.
     */
    public final FSLocationSpec getFSLocationSpec() {
        return m_fsLocationSpec;
    }

    /**
     * Returns the type of this file system, such as "local", or "amazon-s3".
     *
     * @return the type of this file system as an {@link FSType} instance.
     */
    public final FSType getFSType() {
        return m_fsLocationSpec.getFSType();
    }

    /**
     * Returns the {@link FSCategory file system category} of the {@link FSLocationSpec} for this file system instance.
     *
     * @return the file system category.
     * @see #getFSLocationSpec()
     */
    public final FSCategory getFileSystemCategory() {
        return m_fsLocationSpec.getFSCategory();
    }

    /**
     * Returns the optional file system specifier of the {@link FSLocationSpec} for this file system instance.
     *
     * @return the file system specifier.
     * @see #getFSLocationSpec()
     */
    public final Optional<String> getFileSystemSpecifier() {
        return m_fsLocationSpec.getFileSystemSpecifier();
    }

    @Override
    public synchronized boolean isOpen() {
        return m_isOpen;
    }

    /**
     * @return true when the file system is currently closing and closing all the registered closeables.
     */
    protected synchronized boolean isClosing() {
        return m_isClosing;
    }

    /**
     * Does nothing, since a file system must only be closed by the connection node that instantiated it. Nodes that
     * only *use* a file system should invoke {@link FSConnection#close()} on the respective {@link FSConnection} object
     * to release any blocked resources.
     */
    @Override
    public final void close() throws IOException {
        // do nothing
    }

    /**
     * Actually closes this file system and releases any blocked resources (streams, etc). This method must only be
     * called by the connection node, which has control of the file system lifecycle (hence the reduced visibility).
     *
     * @throws IOException when something went wrong while closing the file system.
     */
    final synchronized void ensureClosed() throws IOException {
        if (m_isOpen) {
            try {
                m_isClosing = true;
                closeAllCloseables();
            } finally {
                ensureClosedInternal();
                m_isOpen = false;
                m_isClosing = false;
            }
        }
    }

    /**
     * Closes all registered {@link Closeable}s (streams, temp files, ...). This method is openly accessible only for
     * testing purposes and must not be called by any nodes.
     */
    protected synchronized void closeAllCloseables() {
        for (final Closeable closeable : new ArrayList<>(m_closeables)) {
            closeSafely(closeable);
        }
    }

    private static void closeSafely(final Closeable closeable) {
        try {
            closeable.close();
        } catch (final IOException ex) { // NOSONAR nothing we could do here
        }
    }

    /**
     * Concrete file systems can implement this method to release any blocked resources, e.g. network connections. This
     * method is called at the end when the file system is being closed. Registered {@link Closeable}s have already been
     * closed before.
     *
     * @throws IOException when something went wrong while closing the file system.
     */
    protected abstract void ensureClosedInternal() throws IOException;

    /**
     * Informs the file system, that the corresponding {@link Closeable} was closed and does not need to be tracked
     * anymore.
     *
     * @param closeable The closeable.
     */
    public synchronized void unregisterCloseable(final Closeable closeable) {
        m_closeables.remove(closeable);
    }

    /**
     * Adds a {@link Closeable} for tracking, so it can be closed when the file system is closed.
     *
     * @param closeable The closeable.
     * @throws ClosedFileSystemException when the file system has already been closed. The given {@link Closeable} will
     *             be closed prior to throwing the exception in order to not cause any resource leaks.
     */
    public synchronized void registerCloseable(final Closeable closeable) {
        if (!m_isOpen) {
            closeSafely(closeable);
            throw new ClosedFileSystemException();
        }
        m_closeables.add(closeable);
    }

    /**
     * Each file system has a working directory, aka current directory. The working directory allows users of the file
     * system to supply relative paths to many of the provider methods, e.g. to open an input stream. The working
     * directory will be used to resolve such relative paths to absolute ones. The working directory of a file system
     * instance is final and does not change over the lifetime of the file system.
     *
     * @return the working directory, aka current directory.
     */
    public T getWorkingDirectory() {
        return getPath(m_workingDirectory);
    }

    /**
     * The working directory (see {@link #getWorkingDirectory()} to use by default. This method is intended to be used
     * by file system connector node dialogs, to choose a default working directory. The returned value is purely
     * informational, it has no effect on the file system itself.
     *
     * @return the working directory, aka current directory.
     */
    @SuppressWarnings("unchecked")
    public T getDefaultWorkingDirectory() {
        return (T)getRootDirectories().iterator().next();
    }

    /**
     * Checks whether this file system instance is compatible with {@link FSLocation} objects that have the given
     * {@link FSLocationSpec}.
     *
     * @param fsLocationSpec The {@link FSLocationSpec} to check for compatibility.
     * @throws IllegalArgumentException if the given {@link FSLocation} object is not compatible with this file system
     *             instance.
     */
    public void checkCompatibility(final FSLocationSpec fsLocationSpec) {
        CheckUtils.checkArgument(
            fsLocationSpec.getFileSystemCategory() != null && fsLocationSpec.getFSCategory() == getFileSystemCategory(),
            String.format("Only FSLocations of type %s are allowed with this file system.", getFileSystemCategory()));

        CheckUtils.checkArgument(getFileSystemSpecifier().equals(fsLocationSpec.getFileSystemSpecifier()), String
            .format("Only FSLocations with specifier %s are allowed with this file system.", getFileSystemSpecifier()));
    }

    /**
     * Checks whether this file system instance is compatible with {@link FSLocation} objects that have the given
     * {@link FSLocationSpec}.
     *
     * @param fsLocationSpec The {@link FSLocationSpec} to check for compatibility.
     * @return true, if compatible, false otherwise.
     */
    public boolean isCompatible(final FSLocationSpec fsLocationSpec) {
        return fsLocationSpec.getFileSystemCategory() != null
            && fsLocationSpec.getFSCategory() == getFileSystemCategory()
            && getFileSystemSpecifier().equals(fsLocationSpec.getFileSystemSpecifier());
    }

    /**
     * Converts the given {@link FSLocation} to a path object.
     *
     * @param fsLocation The {@link FSLocation} to convert.
     * @return the path object.
     */
    public T getPath(final FSLocation fsLocation) {
        return getPath(fsLocation.getPath());
    }

    /**
     * @return this file system's base URI, which is used to construct the URI when invoking {@link FSPath#toUri()}.
     */
    public final URI getFileSystemBaseURI() {
        return m_fsBaseUri;
    }

    /**
     * @return an {@link Optional} that contains a {@link WorkflowAware} instance, if this file system is
     *         {@link WorkflowAware}; an empty {@link Optional} otherwise.
     */
    public Optional<WorkflowAware> getWorkflowAware() {
        if (provider() instanceof WorkflowAware) {
            return Optional.of((WorkflowAware)provider());
        } else {
            return Optional.empty();
        }
    }

    /**
     * @return an {@link Optional} that contains a {@link ItemVersionAware} instance, if this file system is
     *         {@link ItemVersionAware}; an empty {@link Optional} otherwise.
     */
    public Optional<ItemVersionAware> getItemVersionAware() {
        if (provider() instanceof ItemVersionAware) {
            return Optional.of((ItemVersionAware)provider());
        } else {
            return Optional.empty();
        }
    }

    @Override
    public abstract FSFileSystemProvider<T, ?> provider();

    @Override
    public abstract T getPath(String first, String... more);
}
