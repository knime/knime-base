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

import java.io.File;
import java.io.IOException;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchEvent.Modifier;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Iterator;

import org.knime.filehandling.core.connections.FSPath;

/**
 * @author Bjoern Lohrmann, KNIME GmbH
 */
public class LocalPath extends FSPath {

    private final LocalFileSystem m_fileSystem;

    private final Path m_wrappedPath;

    LocalPath(final LocalFileSystem fileSystem, final Path wrappedPath) {
        m_wrappedPath = wrappedPath;
        m_fileSystem = fileSystem;
    }

    @Override
    public boolean isAbsolute() {
        return m_wrappedPath.isAbsolute();
    }

    @Override
    public Path getRoot() {
        final Path wrappedPathRoot = m_wrappedPath.getRoot();
        if (wrappedPathRoot != null) {
            return new LocalPath(m_fileSystem, wrappedPathRoot);
        } else {
            return null;
        }
    }

    @Override
    public Path getFileName() {
        final Path wrappedPathFileName = m_wrappedPath.getFileName();
        if (wrappedPathFileName != null) {
            return new LocalPath(m_fileSystem, wrappedPathFileName);
        } else {
            return null;
        }
    }

    @Override
    public Path getParent() {
        final Path wrappedPathParent = m_wrappedPath.getParent();
        if (wrappedPathParent != null) {
            return new LocalPath(m_fileSystem, wrappedPathParent);
        } else {
            return null;
        }
    }

    @Override
    public int getNameCount() {
        return m_wrappedPath.getNameCount();
    }

    @Override
    public Path getName(final int index) {
        return new LocalPath(m_fileSystem, m_wrappedPath.getName(index));
    }

    @Override
    public Path subpath(final int beginIndex, final int endIndex) {
        return new LocalPath(m_fileSystem, m_wrappedPath.subpath(beginIndex, endIndex));
    }

    @Override
    public boolean startsWith(final Path other) {
        if (!(other instanceof LocalPath)) {
            throw new IllegalArgumentException(LocalFileSystemProvider.PATH_FROM_DIFFERENT_PROVIDER_MESSAGE);
        }

        return m_wrappedPath.startsWith(((LocalPath)other).m_wrappedPath);
    }

    @Override
    public boolean startsWith(final String other) {
        return m_wrappedPath.startsWith(other);
    }

    @Override
    public boolean endsWith(final Path other) {
        if (!(other instanceof LocalPath)) {
            throw new IllegalArgumentException(LocalFileSystemProvider.PATH_FROM_DIFFERENT_PROVIDER_MESSAGE);
        }

        return m_wrappedPath.endsWith(((LocalPath)other).m_wrappedPath);
    }

    @Override
    public boolean endsWith(final String other) {
        return m_wrappedPath.endsWith(other);
    }

    @Override
    public Path normalize() {
        return new LocalPath(m_fileSystem, m_wrappedPath.normalize());
    }

    @Override
    public Path resolve(final Path other) {
        if (!(other instanceof LocalPath)) {
            throw new IllegalArgumentException(LocalFileSystemProvider.PATH_FROM_DIFFERENT_PROVIDER_MESSAGE);
        }

        return new LocalPath(m_fileSystem, m_wrappedPath.resolve(((LocalPath)other).m_wrappedPath));
    }

    @Override
    public Path resolve(final String other) {
        return new LocalPath(m_fileSystem, m_wrappedPath.resolve(other));
    }

    @Override
    public Path resolveSibling(final Path other) {
        if (!(other instanceof LocalPath)) {
            throw new IllegalArgumentException(LocalFileSystemProvider.PATH_FROM_DIFFERENT_PROVIDER_MESSAGE);
        }

        return new LocalPath(m_fileSystem, m_wrappedPath.resolveSibling(((LocalPath)other).m_wrappedPath));
    }

    @Override
    public Path resolveSibling(final String other) {
        return new LocalPath(m_fileSystem, m_wrappedPath.resolveSibling(other));
    }

    @SuppressWarnings("resource")
    @Override
    public Path relativize(final Path obj) {
        if (!(obj instanceof LocalPath)) {
            throw new IllegalArgumentException(LocalFileSystemProvider.PATH_FROM_DIFFERENT_PROVIDER_MESSAGE);
        }

        // the following we are doing to monkey patch a bug in WindowsPath (default file system path on Windows),
        // where emptyPath.relativize("a") will return a wrong result (..\a).
        final LocalPath other = (LocalPath)obj;
        if (other.equals(this)) {
            return new LocalPath(getFileSystem(), Paths.get(""));
        }
        if (isAbsolute() != other.isAbsolute()) {
            throw new IllegalArgumentException("Cannot relativize an absolute path with a relative path.");
        }
        if (isEmptyPath()) {
            return other;
        }

        return new LocalPath(m_fileSystem, m_wrappedPath.relativize(other.m_wrappedPath));
    }

    /**
     * @return whether this path is the empty path
     */
    public boolean isEmptyPath() {
        return !isAbsolute() && m_wrappedPath.getNameCount() == 1 && m_wrappedPath.getName(0).toString().isEmpty();
    }

    @SuppressWarnings("resource")
    @Override
    public Path toAbsolutePath() {
        if (isAbsolute()) {
            return this;
        } else {
            return getFileSystem().getWorkingDirectory().resolve(this);
        }
    }

    @Override
    public Path toRealPath(final LinkOption... options) throws IOException {
        return new LocalPath(m_fileSystem, m_wrappedPath.toRealPath(options));
    }

    @Override
    public File toFile() {
        return m_wrappedPath.toFile();
    }

    @Override
    public WatchKey register(final WatchService watcher, final Kind<?>[] events, final Modifier... modifiers)
        throws IOException {
        return m_wrappedPath.register(watcher, events, modifiers);
    }

    @Override
    public WatchKey register(final WatchService watcher, final Kind<?>... events) throws IOException {
        return m_wrappedPath.register(watcher, events);
    }

    @Override
    public Iterator<Path> iterator() {
        final Iterator<Path> wrappedIter = m_wrappedPath.iterator();
        return new Iterator<Path>() {

            @Override
            public boolean hasNext() {
                return wrappedIter.hasNext();
            }

            @Override
            public Path next() {
                return new LocalPath(m_fileSystem, wrappedIter.next());
            }
        };
    }

    @Override
    public int compareTo(final Path other) {
        if (!(other instanceof LocalPath)) {
            throw new IllegalArgumentException(LocalFileSystemProvider.PATH_FROM_DIFFERENT_PROVIDER_MESSAGE);
        }

        return m_wrappedPath.compareTo(((LocalPath)other).m_wrappedPath);

    }

    @Override
    public LocalFileSystem getFileSystem() {
        return m_fileSystem;
    }

    public Path getWrappedPath() {
        return m_wrappedPath;
    }

    @Override
    public String toString() {
        return m_wrappedPath.toString();
    }

    @Override
    public boolean equals(final Object other) {
        if (other == null) {
            return false;
        }

        if (other == this) {
            return true;
        }

        if (other.getClass() != getClass()) {
            return false;
        }

        final LocalPath otherLocal = (LocalPath)other;
        return m_wrappedPath.equals(otherLocal.m_wrappedPath);
    }

    @Override
    public int hashCode() {
        return m_wrappedPath.hashCode();
    }
}
