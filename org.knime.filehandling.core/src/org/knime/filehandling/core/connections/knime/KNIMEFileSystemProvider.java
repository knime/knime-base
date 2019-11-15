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
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.AccessMode;
import java.nio.file.CopyOption;
import java.nio.file.DirectoryStream;
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
import java.nio.file.spi.FileSystemProvider;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.lang3.Validate;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.workflow.NodeContext;
import org.knime.core.node.workflow.WorkflowContext;
import org.knime.core.ui.node.workflow.WorkflowContextUI;
import org.knime.core.ui.node.workflow.WorkflowManagerUI;
import org.knime.core.util.FileUtil;
import org.knime.core.util.IRemoteFileUtilsService;
import org.knime.filehandling.core.connections.FSDirectoryStream;
import org.knime.filehandling.core.connections.attributes.FSBasicAttributes;
import org.knime.filehandling.core.connections.attributes.FSBasicFileAttributeView;
import org.knime.filehandling.core.connections.attributes.FSFileAttributes;
import org.knime.filehandling.core.connections.base.UnixStylePathUtil;
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
public class KNIMEFileSystemProvider extends FileSystemProvider {

    private final static NodeLogger LOGGER = NodeLogger.getLogger(KNIMEFileSystemProvider.class);

    private final static KNIMEFileSystemProvider SINGLETON_INSTANCE = new KNIMEFileSystemProvider();

    private final static String SCHEME = "knime";

    private final Map<URI, FileSystem> m_fileSystems = new HashMap<>();

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
    public FileSystem newFileSystem(final URI uri, final Map<String, ?> env) throws IOException {
        validate(uri);
        URI baseLocationURI = createFSKey(uri);
        Type connectionType = KNIMEConnection.connectionTypeForHost(uri.getHost());
        KNIMEFileSystem knimeFileSystem = new KNIMEFileSystem(this, baseLocationURI, connectionType);
        m_fileSystems.put(baseLocationURI, knimeFileSystem);
        return knimeFileSystem;
    }

    private static URI createFSKey(final URI uri) throws IOException, MalformedURLException {
        URL baseLocation = MountPointIDProviderService.instance().resolveKNIMEURL(uri.toURL());
        try {
            return baseLocation.toURI();
        } catch (URISyntaxException ex) {
            throw new IOException(ex);
        }
    }

    private void validate(final URI uri) {
        Validate.notNull(uri, "URI cannot be null");
        if (m_fileSystems.containsKey(uri)) {
            throw new FileSystemAlreadyExistsException();
        }
        if (!uri.getScheme().equalsIgnoreCase(getScheme())) {
            throw new IllegalArgumentException("The URI must have scheme '" + getScheme() + "'");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FileSystem getFileSystem(final URI uri) {
        try {
            URI fsKey = createFSKey(uri);
            FileSystem fileSystem = m_fileSystems.get(fsKey);
            if (fileSystem == null) {
                throw new FileSystemNotFoundException();
            }
            return fileSystem;
        } catch (IOException ex) {
            throw new FileSystemNotFoundException();
        }
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("resource")
    @Override
    public Path getPath(final URI uri) {
        FileSystem fileSystem = getFileSystem(uri);
        return fileSystem.getPath(uri.getPath());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SeekableByteChannel newByteChannel(final Path path, final Set<? extends OpenOption> options, final FileAttribute<?>... attrs)
        throws IOException {
        return Files.newByteChannel(toLocalPath(path), options);
    }



    /**
     * {@inheritDoc}
     */
    @Override
    public DirectoryStream<Path> newDirectoryStream(final Path dir, final Filter<? super Path> filter) throws IOException {
        Optional<WorkflowContext> serverContext = getServerContext();
        if (serverContext.isPresent()) {
            return new FSDirectoryStream(dir, filter) {

                @SuppressWarnings("resource")
                @Override
                protected Iterator<Path> getIterator(final Path path, final Filter<? super Path> filter1) {

                    KNIMEPath knimePath = (KNIMEPath) dir;
                    KNIMEFileSystem fileSystem = (KNIMEFileSystem) knimePath.getFileSystem();
                    Type connectionType = fileSystem.getConnectionType();
                    URI knimeURL =
                        URI.create(connectionType.getSchemeAndHost() + "/" + UnixStylePathUtil.asUnixStylePath(path.toString()));
                    try {
                        URL url = knimeURL.toURL();
                        List<URL> listFiles = FileUtil.listFiles(url , p -> true, false);
                        List<Path> paths = new ArrayList<>();
                        for (URL fileURL : listFiles) {
                            paths.add(new KNIMEPath(fileSystem, fileURL.getPath()));
                        }
                        return paths.iterator();
                    } catch (IOException | URISyntaxException ex) {
                        LOGGER.debug("Error when fetching files", ex);
                        return Collections.emptyIterator();
                    }
                }
            };
        } else {
            return Files.newDirectoryStream(toLocalPath(dir), filter);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void createDirectory(final Path dir, final FileAttribute<?>... attrs) throws IOException {
        Files.createDirectories(toLocalPath(dir), attrs);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void delete(final Path path) throws IOException {
        Files.delete(toLocalPath(path));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void copy(final Path source, final Path target, final CopyOption... options) throws IOException {
        Files.copy(toLocalPath(source), toLocalPath(target), options);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void move(final Path source, final Path target, final CopyOption... options) throws IOException {
        Files.move(toLocalPath(source), toLocalPath(target), options);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isSameFile(final Path path, final Path path2) throws IOException {
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
        return Files.getFileStore(toLocalPath(path));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void checkAccess(final Path path, final AccessMode... modes) throws IOException {
        Path localPath = toLocalPath(path);
        localPath.getFileSystem().provider().checkAccess(localPath, modes);
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public <V extends FileAttributeView> V getFileAttributeView(final Path path, final Class<V> type, final LinkOption... options) {
        if (onServer()) {
            try {
                return (V)new FSBasicFileAttributeView(path.getFileName().toString(),
                    readAttributes(path, BasicFileAttributes.class, options));
            } catch (final IOException ex) {
                return null;
            }
        }

        return Files.getFileAttributeView(toLocalPath(path), type, options);
    }

    private static Optional<WorkflowContext> getServerContext() {
        WorkflowContextUI workflowContext = getWorkflowContext();
        if (wraps(workflowContext, WorkflowContext.class)) {
            WorkflowContext context = unwrap(workflowContext, WorkflowContext.class);
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
        NodeContext nodeContext = NodeContext.getContext();
        if (nodeContext != null) {
            return
                nodeContext.getContextObjectForClass(WorkflowManagerUI.class).map(wfm -> wfm.getContext()).orElse(null);
        } else {
            return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings({"unchecked", "resource"})
    @Override
    public <A extends BasicFileAttributes> A readAttributes(final Path path, final Class<A> type,
        final LinkOption... options)
        throws IOException {
        Optional<WorkflowContext> serverContext = getServerContext();
        if (serverContext.isPresent()) {
            WorkflowContext context = serverContext.get();
            if (path instanceof KNIMEPath) {
                KNIMEPath knimePath = (KNIMEPath) path;
                KNIMEFileSystem fileSystem = (KNIMEFileSystem) knimePath.getFileSystem();
                Type connectionType = fileSystem.getConnectionType();
                URI create = URI.create(
                    connectionType.getSchemeAndHost() + "/" + UnixStylePathUtil.asUnixStylePath(path.toString()));
                boolean isRegularFile = true;
                try {
                    isRegularFile = !makeRestCall((s) -> s.isWorkflowGroup(create.toURL()), context);
                } catch (Exception e) {
                    LOGGER.debug("Error when making 'isDirectory' rest call", e);
                }

                return (A) new FSFileAttributes(isRegularFile, path,
                    p -> new FSBasicAttributes(null, null, null, 0, false, false));
            }
        }

        return Files.readAttributes(toLocalPath(path), type, options);
    }

    // FIXME this is simply copied from KnimeRemoteFile, we should find a solution for better re-use!
    private static <O> O makeRestCall(final IRemoteFileUtilServiceFunction<O> func, final WorkflowContext workflowContext) throws Exception {
        if (workflowContext.getRemoteRepositoryAddress().isPresent()
            && workflowContext.getServerAuthToken().isPresent()) {
            BundleContext ctx = FrameworkUtil.getBundle(IRemoteFileUtilsService.class).getBundleContext();
            ServiceReference<IRemoteFileUtilsService> ref = ctx.getServiceReference(IRemoteFileUtilsService.class);
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
    public Map<String, Object> readAttributes(final Path path, final String attributes, final LinkOption... options) throws IOException {
        return Files.readAttributes(toLocalPath(path), attributes, options);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setAttribute(final Path path, final String attribute, final Object value, final LinkOption... options) throws IOException {
        Files.setAttribute(toLocalPath(path), attribute, value, options);
    }

    /**
     * Closes the given file system.
     *
     * @param knimeFileSystem file system to be closed
     */
    public void close(final KNIMEFileSystem knimeFileSystem) {
        m_fileSystems.remove(knimeFileSystem.getKey());
    }

    /**
     * Checks whether a given file system is open or not.
     *
     * @param knimeFileSystem the file system
     * @return true if the file system is open
     */
    public boolean isOpen(final KNIMEFileSystem knimeFileSystem) {
        return m_fileSystems.containsKey(knimeFileSystem.getKey());
    }

    private static Path toLocalPath(final Path path) {
        if (path instanceof KNIMEPath) {
            return((KNIMEPath) path).toLocalPath();
        } else {
            throw new IllegalArgumentException("Input path must be an instance of KNIMEPath");
        }
    }

    @Override
    public InputStream newInputStream(final Path path, final OpenOption... options) throws IOException {
        if (path.getFileSystem().provider() != this) {
            throw new IllegalArgumentException("Path is from a different file system provider");
        }

        final KNIMEPath knimePath = (KNIMEPath)path;
        return knimePath.openURLConnection(getTimeout()).getInputStream();
    }

    @Override
    public OutputStream newOutputStream(final Path path, final OpenOption... options) throws IOException {
        if (path.getFileSystem().provider() != this) {
            throw new IllegalArgumentException("Path is from a different file system provider");
        }

        final KNIMEPath knimePath = (KNIMEPath)path;
        return knimePath.openURLConnection(getTimeout()).getOutputStream();
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
        return fileSystemExists(fsKey) ? getFileSystem(fsKey) : newFileSystem(fsKey, null);
    }

    private boolean fileSystemExists(final URI uri) {
        try {
            URI fsKey  = createFSKey(uri);
            return m_fileSystems.containsKey(fsKey);
        } catch (IOException ex) {
            return false;
        }
    }

}
