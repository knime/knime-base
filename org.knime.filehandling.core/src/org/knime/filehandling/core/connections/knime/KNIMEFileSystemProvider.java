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
 *   Sep 3, 2019 (Tobias Urhaug, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.filehandling.core.connections.knime;

import static org.knime.core.ui.wrapper.Wrapper.unwrap;
import static org.knime.core.ui.wrapper.Wrapper.wraps;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.AccessMode;
import java.nio.file.CopyOption;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemAlreadyExistsException;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileAttributeView;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.knime.core.node.NodeLogger;
import org.knime.core.node.workflow.NodeContext;
import org.knime.core.node.workflow.WorkflowContext;
import org.knime.core.ui.node.workflow.WorkflowContextUI;
import org.knime.core.ui.node.workflow.WorkflowManagerUI;
import org.knime.core.util.FileUtil;
import org.knime.core.util.IRemoteFileUtilsService;
import org.knime.filehandling.core.connections.base.BaseFileSystem;
import org.knime.filehandling.core.connections.base.BaseFileSystemProvider;
import org.knime.filehandling.core.connections.base.attributes.FSBasicAttributes;
import org.knime.filehandling.core.connections.base.attributes.FSFileAttributeView;
import org.knime.filehandling.core.connections.base.attributes.FSFileAttributes;
import org.knime.filehandling.core.defaultnodesettings.KNIMEConnection;
import org.knime.filehandling.core.defaultnodesettings.KNIMEConnection.Type;
import org.knime.filehandling.core.util.MountPointIDProviderService;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;

/**
 *
 * @author Tobias Urhaug, KNIME GmbH, Berlin, Germany
 */
public class KNIMEFileSystemProvider extends BaseFileSystemProvider {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(KNIMEFileSystemProvider.class);

    private static final KNIMEFileSystemProvider SINGLETON_INSTANCE = new KNIMEFileSystemProvider();

    private static final String SCHEME = "knime";

    private final Map<URI, BaseFileSystem> m_fileSystems = Collections.synchronizedMap(new HashMap<>());

    /**
     * Returns the singleton instance of this provider.
     *
     * @return the singleton instance of this provider
     */
    public static KNIMEFileSystemProvider getInstance() {
        return SINGLETON_INSTANCE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getScheme() {
        return SCHEME;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized FileSystem newFileSystem(final URI uri, final Map<String, ?> env) throws IOException {
        if (m_fileSystems.containsKey(uri)) {
            throw new FileSystemAlreadyExistsException();
        }
        final BaseFileSystem fileSystem = createFileSystem(uri, env);
        m_fileSystems.put(uri, fileSystem);
        return fileSystem;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FileSystem getFileSystem(final URI uri) {
        final FileSystem fileSystem = m_fileSystems.get(uri);
        if (fileSystem == null) {
            throw new FileSystemNotFoundException();
        }
        return fileSystem;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BaseFileSystem createFileSystem(final URI uri, final Map<String, ?> env) {
        if (!uri.getScheme().equalsIgnoreCase(getScheme())) {
            throw new IllegalArgumentException("The URI must have scheme '" + getScheme() + "'");
        }
        URI baseLocationURI;
        try {
            baseLocationURI = createFSKey(uri);
        } catch (final IOException ex) {
            throw new IllegalArgumentException(ex);
        }
        final Type connectionType = KNIMEConnection.connectionTypeForHost(uri.getHost());
        return new KNIMEFileSystem(this, baseLocationURI, connectionType);
    }

    private static URI createFSKey(final URI uri) throws IOException {
        final URL baseLocation = MountPointIDProviderService.instance().resolveKNIMEURL(uri.toURL());
        try {
            return baseLocation.toURI();
        } catch (final URISyntaxException ex) {
            throw new IOException(ex);
        }
    }

    /**
     * Removes the file system for the given URI from the list of file systems.
     *
     * @param uri the URI to the file system
     */
    @Override
    public synchronized void removeFileSystem(final URI uri) {
        m_fileSystems.remove(uri);
    }

    /**
     * Returns whether a file system for the given URI exists in the list of file systems.
     *
     * @param uri the URI to the file system
     * @return whether a file system for the uri exists
     */
    @Override
    public synchronized boolean isOpen(final URI uri) {
        return m_fileSystems.containsKey(uri);
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("resource")
    @Override
    public Path getPath(final URI uri) {
        final FileSystem fileSystem = getFileSystem(uri);
        return fileSystem.getPath(uri.getPath());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SeekableByteChannel newByteChannel(final Path path, final Set<? extends OpenOption> options,
        final FileAttribute<?>... attrs) throws IOException {
        //FIXME This might not work on the server
        final Path localPath = toLocalPath(path);
        return Files.newByteChannel(localPath.normalize(), options);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void createDirectory(final Path dir, final FileAttribute<?>... attrs) throws IOException {
        //FIXME This might not work on the server
        Files.createDirectories(toLocalPath(dir), attrs);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean deleteIfExists(final Path path) throws IOException {
        final Optional<WorkflowContext> serverContext = getServerContext();
        if (serverContext.isPresent()) {
            final WorkflowContext context = serverContext.get();
            final URL knimeURL = ((KNIMEPath)path).getURL();

            boolean exists = false;
            try {
                exists = makeRestCall((s) -> s.exists(knimeURL), context);
            } catch (final Exception ex) {
                throw new IOException(ex);
            }

            boolean deleted = false;
            if (exists) {
                try {
                    makeRestCall((s) -> {
                        s.delete(knimeURL);
                        return null;
                    }, context);
                    deleted = true;
                } catch (final Exception ex) {
                    throw new IOException(ex);
                }
            }

            return deleted;
        } else {
            return Files.deleteIfExists(toLocalPath(path));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void copy(final Path source, final Path target, final CopyOption... options) throws IOException {
        //FIXME This might not work on the server
        Files.copy(toLocalPath(source), toLocalPath(target), options);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void move(final Path source, final Path target, final CopyOption... options) throws IOException {
        //FIXME This might not work on the server
        Files.move(toLocalPath(source), toLocalPath(target), options);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isSameFile(final Path path, final Path path2) throws IOException {
        //FIXME This might not work on the server
        return Files.isSameFile(toLocalPath(path), toLocalPath(path2));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isHidden(final Path path) throws IOException {
        if (onServer()) {
            // FIXME: figure out how we can know if a file is hidden on the server?
            return false;
        }

        return Files.isHidden(toLocalPath(path));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FileStore getFileStore(final Path path) throws IOException {
        return path.getFileSystem().getFileStores().iterator().next();
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("resource")
    @Override
    public void checkAccess(final Path path, final AccessMode... modes) throws IOException {
        final Optional<WorkflowContext> serverContext = getServerContext();

        if (serverContext.isPresent()) {
            final WorkflowContext context = serverContext.get();
            final KNIMEPath knimePath = (KNIMEPath)path;
            final URL knimeURL = knimePath.getURL();

            final KNIMEFileSystem fileSystem = (KNIMEFileSystem)knimePath.getFileSystem();
            if (fileSystem.getConnectionType() == KNIMEConnection.Type.NODE_RELATIVE) {
                throw new IOException("Node relative paths cannot be executed on the server.");
            }

            boolean exists = false;
            try {
                exists = makeRestCall((s) -> s.exists(knimeURL), context);
            } catch (final Exception ex) {
                throw new IOException(ex);
            }

            if (!exists) {
                // Throw IOException to make Files.exist fail!
                throw new IOException("The file does not exist: '" + knimeURL.toString() + "'.");
            }
        } else {
            final Path localPath = toLocalPath(path);
            localPath.getFileSystem().provider().checkAccess(localPath, modes);
        }
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public <V extends FileAttributeView> V getFileAttributeView(final Path path, final Class<V> type,
        final LinkOption... options) {
        if (onServer()) {
            return (V)new FSFileAttributeView(path.getFileName().toString(),
                () -> (FSFileAttributes)readAttributes(path, BasicFileAttributes.class, options));
        }

        return Files.getFileAttributeView(toLocalPath(path), type, options);
    }

    private static Optional<WorkflowContext> getServerContext() {
        final WorkflowContextUI workflowContext = getWorkflowContext();
        if (wraps(workflowContext, WorkflowContext.class)) {
            final WorkflowContext context = unwrap(workflowContext, WorkflowContext.class);
            if (context.getRemoteRepositoryAddress().isPresent() && context.getServerAuthToken().isPresent()) {
                return Optional.of(context);
            }
        }
        return Optional.empty();
    }

    private static boolean onServer() {
        return getServerContext().isPresent();
    }

    private static WorkflowContextUI getWorkflowContext() {
        final NodeContext nodeContext = NodeContext.getContext();
        if (nodeContext != null) {
            return nodeContext.getContextObjectForClass(WorkflowManagerUI.class).map(wfm -> wfm.getContext())
                .orElse(null);
        } else {
            return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings({"unchecked"})
    @Override
    public <A extends BasicFileAttributes> A readAttributes(final Path path, final Class<A> type,
        final LinkOption... options) throws IOException {
        final Optional<WorkflowContext> serverContext = getServerContext();
        if (serverContext.isPresent()) {
            final WorkflowContext context = serverContext.get();
            if (path instanceof KNIMEPath) {
                final KNIMEPath knimePath = (KNIMEPath)path;
                final URL knimeURL = knimePath.getURL();
                boolean isRegularFile = false;
                try {
                    isRegularFile = !makeRestCall((s) -> s.isWorkflowGroup(knimeURL), context);
                } catch (final Exception e) {
                    LOGGER.debug("Error when making 'isDirectory' rest call", e);
                }

                return (A)new FSFileAttributes(isRegularFile, path,
                    p -> new FSBasicAttributes(null, null, null, 0, false, false));
            }
        }

        return Files.readAttributes(toLocalPath(path), type, options);
    }

    // FIXME this is simply copied from KnimeRemoteFile, we should find a solution for better re-use!
    private static <O> O makeRestCall(final IRemoteFileUtilServiceFunction<O> func,
        final WorkflowContext workflowContext) throws Exception {
        if (workflowContext.getRemoteRepositoryAddress().isPresent()
            && workflowContext.getServerAuthToken().isPresent()) {
            final BundleContext ctx = FrameworkUtil.getBundle(IRemoteFileUtilsService.class).getBundleContext();
            final ServiceReference<IRemoteFileUtilsService> ref =
                ctx.getServiceReference(IRemoteFileUtilsService.class);
            if (ref != null) {
                try {
                    return func.run(ctx.getService(ref));
                } finally {
                    ctx.ungetService(ref);
                }
            } else {
                throw new IllegalStateException(
                    "Unable to access KNIME REST service. No service registered. Most likely an Implementation error.");
            }
        } else {
            throw new IllegalStateException(
                "Unable to access KNIME REST service. Invalid context. Most likely an implemenation error.");
        }
    }

    @FunctionalInterface
    private static interface IRemoteFileUtilServiceFunction<O> {
        O run(IRemoteFileUtilsService service) throws Exception;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, Object> readAttributes(final Path path, final String attributes, final LinkOption... options)
        throws IOException {
        //FIXME This might not work on the server
        return Files.readAttributes(toLocalPath(path), attributes, options);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setAttribute(final Path path, final String attribute, final Object value, final LinkOption... options)
        throws IOException {
        //FIXME This might not work on the server
        Files.setAttribute(toLocalPath(path), attribute, value, options);
    }

    private static Path toLocalPath(final Path path) {
        if (path instanceof KNIMEPath) {
            return ((KNIMEPath)path).toLocalPath();
        } else {
            throw new IllegalArgumentException("Input path must be an instance of KNIMEPath");
        }
    }

    private static int getTimeout() {
        return FileUtil.getDefaultURLTimeoutMillis();
    }

    /**
     * Gets or creates a new {@link KNIMEFileSystem} based on the input fsKey.
     *
     * @param fsKey the key that either retrieves or creates a new file system.
     * @return a file system for the key
     * @throws IOException
     */
    public FileSystem getOrCreateFileSystem(final URI fsKey) throws IOException {
        return isOpen(fsKey) ? getFileSystem(fsKey) : newFileSystem(fsKey, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean exists(final Path path) {
        final Optional<WorkflowContext> serverContext = getServerContext();
        if (serverContext.isPresent()) {
            final WorkflowContext context = serverContext.get();
            final URL knimeURL = ((KNIMEPath)path).getURL();

            try {
                return makeRestCall(s -> s.exists(knimeURL), context);
            } catch (final Exception ex) {
                return false;
            }
        } else {
            return Files.exists(path);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public InputStream newInputStreamInternal(final Path path, final OpenOption... options) throws IOException {
        if (path.getFileSystem().provider() != this) {
            throw new IllegalArgumentException("Path is from a different file system provider");
        }

        final KNIMEPath knimePath = (KNIMEPath)path;
        return knimePath.openURLConnection(getTimeout()).getInputStream();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public OutputStream newOutputStreamInternal(final Path path, final OpenOption... options) throws IOException {
        if (path.getFileSystem().provider() != this) {
            throw new IllegalArgumentException("Path is from a different file system provider");
        }

        final KNIMEPath knimePath = (KNIMEPath)path;
        if (onServer()) {
            return FileUtil.openOutputConnection(knimePath.getURL(), "PUT").getOutputStream();
        } else {
            return Files.newOutputStream(knimePath.toLocalPath(), options);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @throws IOException
     */
    @SuppressWarnings("resource")
    @Override
    public Iterator<Path> createPathIterator(final Path dir, final Filter<? super Path> filter) throws IOException {
        final Optional<WorkflowContext> serverContext = getServerContext();
        if (serverContext.isPresent()) {
            final KNIMEPath knimePath = (KNIMEPath)dir;
            final KNIMEFileSystem fileSystem = (KNIMEFileSystem)knimePath.getFileSystem();
            final URL knimeURL = knimePath.getURL();
            try {
                final List<URL> listFiles = FileUtil.listFiles(knimeURL, p -> true, false);
                final List<Path> paths = new ArrayList<>();
                for (final URL fileURL : listFiles) {
                    paths.add(new KNIMEPath(fileSystem, fileURL.getPath()));
                }
                return paths.iterator();
            } catch (IOException | URISyntaxException ex) {
                LOGGER.debug("Error when listing files at '" + knimeURL.toString() + "'.", ex);
                return Collections.emptyIterator();
            }
        } else {
            return Files.newDirectoryStream(toLocalPath(dir), filter).iterator();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected FSFileAttributes fetchAttributesInternal(final Path path, final Class<?> type) throws IOException {
      //provider methods overrides get attributes methods
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void deleteInternal(final Path path) throws IOException {
        //FIXME This might not work on the server
        Files.delete(toLocalPath(path));
    }

}
