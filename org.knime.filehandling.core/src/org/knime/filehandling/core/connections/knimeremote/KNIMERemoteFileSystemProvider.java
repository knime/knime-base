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
 *   Nov 11, 2019 (Tobias Urhaug, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.filehandling.core.connections.knimeremote;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URL;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.AccessMode;
import java.nio.file.CopyOption;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.FileStore;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.spi.FileSystemProvider;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.eclipse.core.runtime.CoreException;
import org.knime.core.util.FileUtil;
import org.knime.filehandling.core.connections.base.BaseFileSystem;
import org.knime.filehandling.core.connections.base.BaseFileSystemProvider;
import org.knime.filehandling.core.connections.base.attributes.FSFileAttributes;
import org.knime.filehandling.core.util.MountPointIDProviderService;

/**
 * Implementation of {@link FileSystemProvider} for KNIME Mountpoints
 *
 * @author Tobias Urhaug, KNIME GmbH, Berlin, Germany
 */
public class KNIMERemoteFileSystemProvider extends BaseFileSystemProvider {

    private static final String SCHEME = "knime";

    @Override
    public BaseFileSystem createFileSystem(final URI uri, final Map<String, ?> env) {
        return new KNIMERemoteFileSystem(this, uri);
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
    public Path getPath(final URI uri) {
        return getFileSystemInternal().getPath(uri.getPath());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SeekableByteChannel newByteChannel(final Path path, final Set<? extends OpenOption> options,
        final FileAttribute<?>... attrs) throws IOException {
        throw new UnsupportedOperationException();
    }

    private static int getTimeout() {
        return FileUtil.getDefaultURLTimeoutMillis();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void createDirectory(final Path dir, final FileAttribute<?>... attrs) throws IOException {
        MountPointIDProviderService.instance().createDirectory(dir.toUri());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void copy(final Path source, final Path target, final CopyOption... options) throws IOException {
        MountPointIDProviderService.instance().copyFile(source.toUri(), target.toUri());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void move(final Path source, final Path target, final CopyOption... options) throws IOException {
        MountPointIDProviderService.instance().moveFile(source.toUri(), target.toUri());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isSameFile(final Path path, final Path path2) throws IOException {
        return path.equals(path2);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isHidden(final Path path) throws IOException {
        checkPath(path);
        return false;
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
    @Override
    public void checkAccess(final Path path, final AccessMode... modes) throws IOException {
        MountPointIDProviderService.instance().getFileAttributes(path.toUri());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setAttribute(final Path path, final String attribute, final Object value, final LinkOption... options)
        throws IOException {
        throw new UnsupportedOperationException();
    }

    private URL toURL(final Path path) {
        checkPath(path);
        return ((KNIMERemotePath)path).toURL();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean exists(final Path path) {
        try {
            MountPointIDProviderService.instance().getFileAttributes(path.toUri());
            return true;
        } catch (final IOException ex) {
            return false;
        }
    }

    @Override
    protected InputStream newInputStreamInternal(final Path path, final OpenOption... options) throws IOException {

        final KNIMERemotePath uriPath = (KNIMERemotePath)path;
        try {
            return uriPath.openURLConnection(getTimeout()).getInputStream();
        } catch (IOException e) {
            final Throwable rootCause = ExceptionUtils.getRootCause(e);
            if (rootCause instanceof FileNotFoundException) {
                throw new NoSuchFileException(path.toString());
            } else if (isNoSuchFileOnServerMountpoint(rootCause)) {
                throw new NoSuchFileException(path.toString());
            } else {
                throw e;
            }
        }
    }

    private static boolean isNoSuchFileOnServerMountpoint(final Throwable rootCause) {
        return rootCause instanceof CoreException && (
                rootCause.getMessage().endsWith("file does not exist.") // reported by RestServerExplorerFileStore
                || rootCause.getMessage().endsWith("file has already been deleted.") // reported by RestServerExplorerFileStore
                || rootCause.getMessage().endsWith(" It doesn't exist.") // reported by EjbServerExplorerFileStore
                );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected OutputStream newOutputStreamInternal(final Path path, final OpenOption... options) throws IOException {
        final KNIMERemotePath knimePath = (KNIMERemotePath)path;
        final URL knimeURL = toURL(knimePath);
        return FileUtil.openOutputConnection(knimeURL, "PUT").getOutputStream();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Iterator<Path> createPathIterator(final Path dir, final Filter<? super Path> filter)
            throws IOException {
        try {
            return new KNIMERemotePathIterator(dir, filter);
        } catch (IOException e) {
            final Throwable rootCause = ExceptionUtils.getRootCause(e);
            if (rootCause instanceof FileNotFoundException) {
                throw new NoSuchFileException(dir.toString());
            } else if (isNoSuchFileOnServerMountpoint(rootCause)) {
                throw new NoSuchFileException(dir.toString());
            } else {
                throw e;
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected FSFileAttributes fetchAttributesInternal(final Path path, final Class<?> type) throws IOException {
        return MountPointIDProviderService.instance().getFileAttributes(path.toUri());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void deleteInternal(final Path path) throws IOException {
        MountPointIDProviderService.instance().deleteFile(path.toUri());
    }
}
