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
 *   20.01.2020 (Mareike Hoeger, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.connections.base;

import static java.lang.String.format;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchEvent.Modifier;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.httpclient.URIException;
import org.apache.commons.httpclient.util.URIUtil;
import org.knime.core.node.util.CheckUtils;
import org.knime.filehandling.core.filechooser.NioFile;

/**
 * Base implementation for unix style paths.
 *
 * @author Mareike Hoeger, KNIME GmbH, Konstanz, Germany
 * @param <T> The file system to the path
 */
public abstract class UnixStylePath<T extends BaseFileSystem> implements Path {

    /** Constant for the to parent string */
    protected static final String TO_PARENT = "..";

    /** Path separator */
    protected final String m_pathSeparator;

    /** List of individual path components */
    protected final ArrayList<String> m_pathParts;

    /** The file system the path belongs to */
    protected final T m_fileSystem;

    /** Whether the path is absolute */
    protected final boolean m_isAbsolute;

    /**
     * Creates an UnixStylePath from the given bucket name and object key
     *
     * @param fileSystem the file system
     * @param pathString path String
     */
    public UnixStylePath(final T fileSystem, final String pathString) {
        CheckUtils.checkNotNull(fileSystem, "FileSystem must not be null.");
        CheckUtils.checkNotNull(pathString, "Path string must not be null.");
        m_fileSystem = fileSystem;
        m_pathSeparator = m_fileSystem.getSeparator();
        m_isAbsolute = pathString.startsWith(m_pathSeparator);
        m_pathParts = getPathSplits(pathString);
    }

    /**
     * Creates an UnixStylePath from the given bucket name and object key
     *
     * @param fileSystem the file system
     * @param first first part of the path
     * @param more subsequent parts of the path
     */
    public UnixStylePath(final T fileSystem, final String first, final String... more) {
        this(fileSystem, concatenatePathSegments(fileSystem.getSeparator(), first, more));
    }

    /**
     * Concatenates the given path segments with the path separator as delimiter, ignoring empty segments.
     *
     * @param separator the file system specific separator
     * @param first first part of the path
     * @param more subsequent parts of the path
     * @return the concatenated string
     */
    protected static String concatenatePathSegments(final String separator, final String first, final String... more) {

        final StringBuilder sb = new StringBuilder(first);

        for (final String segment : more) {
            if (segment.length() > 0) {
                if (sb.length() > 0) {
                    sb.append(separator);
                }
                sb.append(segment);
            }
        }
        return sb.toString();
    }

    private ArrayList<String> getPathSplits(final String pathString) {

        final ArrayList<String> splitList = new ArrayList<>();
        if (pathString.isEmpty()) {
            // special case: the empty path
            splitList.add("");
        } else {

            Arrays.stream(pathString.split(m_pathSeparator)) //
                .filter(c -> !c.isEmpty()) //
                .forEach(splitList::add);
        }
        return splitList;
    }

    /**
     * This method is used to create paths in the other methods.
     *
     * @param pathString the path String to build the Path from
     * @param more subsequent parts of the path
     * @return the path
     */
    public abstract Path createPath(final String pathString, final String... more);

    /**
     * {@inheritDoc}
     */
    @Override
    public T getFileSystem() {
        return m_fileSystem;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isAbsolute() {
        return m_isAbsolute;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Path getRoot() {
        return m_isAbsolute ? createPath(m_pathSeparator) : null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Path getFileName() {
        if (m_pathParts.isEmpty()) {
            return null;
        }
        return createPath(m_pathParts.get(m_pathParts.size() - 1));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Path getParent() {
        if ((m_isAbsolute && m_pathParts.isEmpty()) || (!m_isAbsolute && m_pathParts.size() <= 1)) {
            return null;
        }

        final StringBuilder sb = new StringBuilder();
        if (m_isAbsolute) {
            sb.append(m_pathSeparator);
        }
        for (int i = 0; i < m_pathParts.size() - 1; i++) {
            sb.append(m_pathParts.get(i));
            sb.append(m_pathSeparator);
        }

        return createPath(sb.toString());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getNameCount() {
        return m_pathParts.size();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Path getName(final int index) {
        if (index < 0 || index >= m_pathParts.size()) {
            throw new IllegalArgumentException();
        }

        return createPath(m_pathParts.get(index));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Path subpath(final int beginIndex, final int endIndex) {

        if (beginIndex >= endIndex || beginIndex < 0 || beginIndex >= m_pathParts.size()
            || endIndex > m_pathParts.size()) {
            throw new IllegalArgumentException();
        }
        final String pathString = String.join(m_pathSeparator, m_pathParts.subList(beginIndex, endIndex));

        return createPath(pathString);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean startsWith(final Path other) {
        if (other.getFileSystem() != m_fileSystem) {
            return false;
        }

        if (other.getNameCount() > getNameCount()) {
            return false;
        }

        if (other.isAbsolute() != isAbsolute()) {
            return false;
        }

        @SuppressWarnings("unchecked")
        final UnixStylePath<T> otherUnix = (UnixStylePath<T>)other;
        boolean startsWith = true;
        for (int i = 0; i < other.getNameCount(); i++) {
            if (!m_pathParts.get(i).equals(otherUnix.m_pathParts.get(i))) {
                startsWith = false;
                break;
            }
        }

        return startsWith;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean startsWith(final String other) {
        return startsWith(createPath(other));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean endsWith(final Path other) {

        if (other.getFileSystem() != m_fileSystem) {
            return false;
        }

        if (other.getNameCount() > getNameCount()) {
            return false;
        }

        if (other.getNameCount() == 0 && getNameCount() > 0) {
            return false;
        }

        @SuppressWarnings("unchecked")
        final UnixStylePath<T> unixPath = (UnixStylePath<T>)other;

        int otherIndex = other.getNameCount();
        int index = getNameCount();

        boolean endsWith = true;
        while (otherIndex > 0) {
            index--;
            otherIndex--;
            if (!m_pathParts.get(index).equals(unixPath.m_pathParts.get(otherIndex))) {
                endsWith = false;
                break;
            }

        }

        return endsWith;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean endsWith(final String other) {
        return endsWith(createPath(other));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Path normalize() {
        if (m_pathParts.isEmpty()) {
            return this;
        }

        final List<String> normalized = getNormalizedPathParts();

        //Ensure absolute paths stay absolute
        final String first = m_isAbsolute ? m_pathSeparator : "";

        return createPath(first, normalized.toArray(new String[normalized.size()]));
    }

    /**
     * @return Returns a list of path parts from this path without redundant occurrences of "{@code .}" and
     *         "{@code ..}".
     */
    protected List<String> getNormalizedPathParts() {
        final LinkedList<String> normalized = new LinkedList<>();
        boolean stepUp = true;
        for (final String pathComponent : m_pathParts) {
            if (pathComponent.equals(".")) {
                continue;
            } else if (pathComponent.equals(TO_PARENT)) {
                if (normalized.isEmpty() || !stepUp) {
                    if (!isAbsolute()) {
                        normalized.add(pathComponent);
                        stepUp = false;
                    }
                } else {
                    normalized.removeLast();
                }
            } else {
                normalized.add(pathComponent);
                stepUp = true;
            }
        }
        return normalized;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Path resolve(final Path other) {
        if (other.getFileSystem() != m_fileSystem) {
            throw new IllegalArgumentException("Cannot resolve paths across different file systems");
        }

        if (other.isAbsolute()) {
            return other;
        }

        if (other.getNameCount() == 0) {
            return this;
        }

        return createPath(toString(), other.toString());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Path resolve(final String other) {
        return resolve(createPath(other));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Path resolveSibling(final Path other) {
        if (other.getFileSystem() != m_fileSystem) {
            throw new IllegalArgumentException("Cannot resolve sibling paths across different file systems");
        }

        if (other.isAbsolute()) {
            return other;
        }

        final Path parent = getParent();
        if (parent == null) {
            return other;
        } else {
            return parent.resolve(other);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Path resolveSibling(final String other) {
        return resolveSibling(createPath(other));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Path relativize(final Path other) {
        if (other.getFileSystem() != m_fileSystem) {
            throw new IllegalArgumentException("Cannot relativize paths across different file systems");
        }

        if (this.equals(other)) {
            return createPath("");
        }

        if (this.isAbsolute() != other.isAbsolute()) {
            throw new IllegalArgumentException("Cannot relativize an absolute path with a relative path.");
        }

        if (!(other instanceof UnixStylePath)) {
            throw new IllegalArgumentException("Unknown path implementation, only unix style path can be relativize.");
        }

        @SuppressWarnings("unchecked")
        UnixStylePath<T> unixOther = (UnixStylePath<T>)other;

        if (m_pathParts.isEmpty() || (m_pathParts.size() == 1 && m_pathParts.get(0).isEmpty())) {
            return createPath(String.join(unixOther.m_pathSeparator, unixOther.m_pathParts));
        }

        if (unixOther.startsWith(this)) {
            return unixOther.subpath(getNameCount(), unixOther.getNameCount());
        }
        if (this.startsWith(unixOther)) {
            final String[] toParentArray = new String[getNameCount() - unixOther.getNameCount() - 1];
            Arrays.fill(toParentArray, TO_PARENT);
            return createPath(TO_PARENT, toParentArray);
        }

        int equalPathPartsCount = 0;
        while ((Math.max(this.getNameCount(), unixOther.getNameCount()) > equalPathPartsCount)
            && unixOther.m_pathParts.get(equalPathPartsCount).equals(m_pathParts.get(equalPathPartsCount))) {
            equalPathPartsCount++;
        }

        unixOther = (UnixStylePath<T>)unixOther.subpath(equalPathPartsCount, unixOther.getNameCount());

        int r = 0;
        while (unixOther.m_pathParts.get(r).equals(TO_PARENT)) {
            r++;
        }

        final int parentStepsCount = getNameCount() - equalPathPartsCount;
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parentStepsCount; i++) {
            sb.append(TO_PARENT);
            sb.append(m_pathSeparator);
        }

        sb.append(unixOther.subpath(r, unixOther.getNameCount()));
        return createPath(sb.toString());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public URI toUri() {
        String encodedPath;
        try {
            encodedPath = URIUtil.encodePath(toAbsolutePath().toString());
            return new URI(m_fileSystem.getSchemeString(), m_fileSystem.getHostString(), encodedPath, null);
        } catch (URIException | URISyntaxException ex) {
            throw new IllegalArgumentException(ex);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Path toAbsolutePath() {
        if (m_isAbsolute) {
            return this;
        } else {
            throw new IllegalStateException(format("Relative Path %s cannot be made absolute", toString()));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Path toRealPath(final LinkOption... options) throws IOException {
        return toAbsolutePath().normalize();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public File toFile() {
        return new NioFile(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public WatchKey register(final WatchService watcher, final Kind<?>[] events, final Modifier... modifiers)
        throws IOException {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public WatchKey register(final WatchService watcher, final Kind<?>... events) throws IOException {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Iterator<Path> iterator() {
        final ArrayList<Path> names = new ArrayList<>(getNameCount());
        for (int i = 0; i < getNameCount(); i++) {
            names.add(getName(i));
        }
        return names.iterator();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int compareTo(final Path other) {

        if (other.getFileSystem() != m_fileSystem) {
            throw new IllegalArgumentException("Cannot compare paths across different file systems");
        }

        return toString().compareTo(other.toString());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        int result = m_fileSystem.hashCode();
        result = 31 * result + m_pathParts.hashCode();
        return result;
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }

        if (!(other instanceof UnixStylePath)) {
            return false;
        }

        @SuppressWarnings("unchecked")
        final UnixStylePath<T> path = (UnixStylePath<T>)other;

        if (path.getFileSystem() != getFileSystem()) {
            return false;
        }

        if (path.isAbsolute() != isAbsolute()) {
            return false;
        }

        if (m_pathParts.size() != path.m_pathParts.size()) {
            return false;
        }

        boolean equals = true;
        for (int i = 0; i < path.getNameCount(); i++) {
            if (!m_pathParts.get(i).equals(path.m_pathParts.get(i))) {
                equals = false;
                break;
            }
        }
        return equals;
    }

    @Override
    public String toString() {
        final String root = isAbsolute() ? m_pathSeparator : "";
        return root + String.join(m_pathSeparator, m_pathParts);
    }
}
