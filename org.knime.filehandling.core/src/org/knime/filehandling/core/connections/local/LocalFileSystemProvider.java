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
 *   Apr 6, 2020 (bjoern): created
 */
package org.knime.filehandling.core.connections.local;

import java.io.IOException;
import java.net.URI;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.AccessMode;
import java.nio.file.CopyOption;
import java.nio.file.DirectoryStream;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemAlreadyExistsException;
import java.nio.file.FileSystems;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.spi.FileSystemProvider;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.knime.filehandling.core.connections.FSFileSystemProvider;
import org.knime.filehandling.core.connections.FSSeekableByteChannel;

/**
 *
 * @author bjoern
 */
class LocalFileSystemProvider extends FSFileSystemProvider<LocalPath, LocalFileSystem> {

    static final String PATH_FROM_DIFFERENT_PROVIDER_MESSAGE = "Path is from a different provider";

    static final String KEY_WORKING_DIR = "workingDir";

    private static final FileSystemProvider PLATFORM_DEFAULT_PROVIDER = FileSystems.getDefault().provider();

    private LocalFileSystem m_fileSystem;

    @Override
    public synchronized LocalFileSystem newFileSystem(final URI uri, final Map<String, ?> env) throws IOException {
        if (m_fileSystem != null) {
            throw new FileSystemAlreadyExistsException();
        }

        final String workingDir = (String)env.get(KEY_WORKING_DIR);
        return getOrCreateFileSystem(workingDir);
    }

    /**
     * Gets or creates a new local {@link FileSystem}.
     *
     * @param workingDir
     * @return a file system
     */
    public synchronized LocalFileSystem getOrCreateFileSystem(final String workingDir) {
        if (m_fileSystem == null) {
            m_fileSystem = new LocalFileSystem(this, workingDir);
        }

        return m_fileSystem;
    }

    @Override
    public LocalFileSystem getFileSystem(final URI uri) {
        return m_fileSystem;
    }

    @Override
    public LocalPath getPath(final URI uri) {
        return new LocalPath(m_fileSystem, PLATFORM_DEFAULT_PROVIDER.getPath(uri));
    }

    @Override
    public String getScheme() {
        return "file";
    }

    @SuppressWarnings("resource")
    @Override
    public SeekableByteChannel newByteChannel(final Path path, final Set<? extends OpenOption> options,
        final FileAttribute<?>... attrs) throws IOException {
        final LocalPath localPath = checkCastAndAbsolutizePath(path);

        return new FSSeekableByteChannel(
            PLATFORM_DEFAULT_PROVIDER.newByteChannel(localPath.getWrappedPath(), options, attrs), m_fileSystem);
    }

    @SuppressWarnings("resource")
    @Override
    public DirectoryStream<Path> newDirectoryStream(final Path path, final Filter<? super Path> filter)
        throws IOException {

        final LocalPath localPath = checkCastAndAbsolutizePath(path);
        final DirectoryStream<Path> wrappedStream =
            PLATFORM_DEFAULT_PROVIDER.newDirectoryStream(localPath.getWrappedPath(), filter);
        final boolean relativizeResults = !path.isAbsolute();

        final DirectoryStream<Path> toReturn = new DirectoryStream<Path>() {
            @Override
            public void close() throws IOException {
                try {
                    wrappedStream.close();
                } finally {
                    m_fileSystem.unregisterCloseable(this);
                }
            }

            @Override
            public Iterator<Path> iterator() {
                final Iterator<Path> wrappedIter = wrappedStream.iterator();
                return new Iterator<Path>() {

                    @Override
                    public boolean hasNext() {
                        return wrappedIter.hasNext();
                    }

                    @Override
                    public Path next() {
                        Path defaultFSPath = wrappedIter.next();
                        if (relativizeResults) {
                            defaultFSPath = localPath.getWrappedPath().relativize(defaultFSPath);
                        }
                        return new LocalPath(m_fileSystem, defaultFSPath);
                    }
                };
            }
        };
        m_fileSystem.registerCloseable(toReturn);
        return toReturn;
    }

    @Override
    public void createDirectory(final Path path, final FileAttribute<?>... attrs) throws IOException {
        final LocalPath localPath = checkCastAndAbsolutizePath(path);
        PLATFORM_DEFAULT_PROVIDER.createDirectory(localPath.getWrappedPath(), attrs);

    }

    @Override
    public void delete(final Path path) throws IOException {
        final LocalPath localPath = checkCastAndAbsolutizePath(path);
        PLATFORM_DEFAULT_PROVIDER.delete(localPath.getWrappedPath());
    }

    @Override
    public void copy(final Path source, final Path target, final CopyOption... options) throws IOException {
        final LocalPath localSourcePath = checkCastAndAbsolutizePath(source);
        final LocalPath localTargetPath = checkCastAndAbsolutizePath(target);
        PLATFORM_DEFAULT_PROVIDER.copy(localSourcePath.getWrappedPath(), localTargetPath.getWrappedPath(), options);
    }

    @Override
    public void move(final Path source, final Path target, final CopyOption... options) throws IOException {
        final LocalPath localSourcePath = checkCastAndAbsolutizePath(source);
        final LocalPath localTargetPath = checkCastAndAbsolutizePath(target);
        PLATFORM_DEFAULT_PROVIDER.move(localSourcePath.getWrappedPath(), localTargetPath.getWrappedPath(), options);
    }

    @Override
    public boolean isSameFile(final Path path, final Path path2) throws IOException {
        final LocalPath localPath = checkCastAndAbsolutizePath(path);
        final LocalPath localPath2 = checkCastAndAbsolutizePath(path2);
        return PLATFORM_DEFAULT_PROVIDER.isSameFile(localPath.getWrappedPath(), localPath2.getWrappedPath());

    }

    @Override
    public boolean isHidden(final Path path) throws IOException {
        final LocalPath localPath = checkCastAndAbsolutizePath(path);
        return PLATFORM_DEFAULT_PROVIDER.isHidden(localPath.getWrappedPath());
    }

    @Override
    public FileStore getFileStore(final Path path) throws IOException {
        final LocalPath localPath = checkCastAndAbsolutizePath(path);
        return PLATFORM_DEFAULT_PROVIDER.getFileStore(localPath.getWrappedPath());
    }

    @Override
    public void checkAccess(final Path path, final AccessMode... modes) throws IOException {
        final LocalPath localPath = checkCastAndAbsolutizePath(path);
        PLATFORM_DEFAULT_PROVIDER.checkAccess(localPath.getWrappedPath(), modes);
    }

    @Override
    public <V extends FileAttributeView> V getFileAttributeView(final Path path, final Class<V> type,
        final LinkOption... options) {
        final LocalPath localPath = checkCastAndAbsolutizePath(path);
        return PLATFORM_DEFAULT_PROVIDER.getFileAttributeView(localPath.getWrappedPath(), type, options);
    }

    @Override
    public <A extends BasicFileAttributes> A readAttributes(final Path path, final Class<A> type,
        final LinkOption... options) throws IOException {
        final LocalPath localPath = checkCastAndAbsolutizePath(path);
        return PLATFORM_DEFAULT_PROVIDER.readAttributes(localPath.getWrappedPath(), type, options);
    }

    @Override
    public Map<String, Object> readAttributes(final Path path, final String attributes, final LinkOption... options)
        throws IOException {
        final LocalPath localPath = checkCastAndAbsolutizePath(path);
        return PLATFORM_DEFAULT_PROVIDER.readAttributes(localPath.getWrappedPath(), attributes, options);
    }

    @Override
    public void setAttribute(final Path path, final String attribute, final Object value, final LinkOption... options)
        throws IOException {
        final LocalPath localPath = checkCastAndAbsolutizePath(path);
        PLATFORM_DEFAULT_PROVIDER.setAttribute(localPath.getWrappedPath(), attribute, value, options);
    }

    @SuppressWarnings("resource")
    protected LocalPath checkCastAndAbsolutizePath(final Path path) {
        if (path.getFileSystem().provider() != this) {
            throw new IllegalArgumentException(PATH_FROM_DIFFERENT_PROVIDER_MESSAGE);
        }

        LocalPath toReturn = (LocalPath)path;
        if (!toReturn.isAbsolute()) {
            toReturn = (LocalPath)m_fileSystem.getWorkingDirectory().resolve(toReturn);
        }

        return toReturn;
    }
}
