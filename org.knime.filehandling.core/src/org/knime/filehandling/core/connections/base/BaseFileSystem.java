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
 *   18.12.2019 (Mareike Hoeger, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.connections.base;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileStore;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.WatchService;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.lang3.Validate;
import org.knime.filehandling.core.connections.FSFileSystem;
import org.knime.filehandling.core.connections.FSFileSystemProvider;
import org.knime.filehandling.core.connections.FSLocationSpec;
import org.knime.filehandling.core.connections.FSPath;
import org.knime.filehandling.core.connections.base.attributes.AttributesCache;
import org.knime.filehandling.core.connections.base.attributes.BaseAttributesCache;
import org.knime.filehandling.core.connections.base.attributes.BaseFileAttributes;
import org.knime.filehandling.core.connections.base.attributes.NoOpAttributesCache;

/**
 * Base implementation of {@FileSystem}.
 *
 * @author Mareike Hoeger, KNIME GmbH
 * @since 4.2
 */
public abstract class BaseFileSystem<T extends FSPath> extends FSFileSystem<T> {

    private final BaseFileSystemProvider<?,?> m_fileSystemProvider;

    private final URI m_uri;

    private final AttributesCache m_cache;

    /**
     * Constructs {@FileSystem} with the given file system provider, identifying uri and name an type of the file
     * system.
     *
     * @param fileSystemProvider the provider that the file system belongs to
     * @param uri the uri identifying the file system
     * @param cacheTTL the time to live for cached elements in milliseconds. A value of 0 or smaller indicates no
     *            caching.
     */
    public BaseFileSystem(final BaseFileSystemProvider<?,?> fileSystemProvider,
        final URI uri,
        final long cacheTTL,
        final String workingDirectory,
        final FSLocationSpec fsLocationSpec) {

        super(fsLocationSpec, workingDirectory);

        fileSystemProvider.setFileSystem(this);

        Validate.notNull(fileSystemProvider, "File system provider must not be null.");
        Validate.notNull(uri, "URI must not be null.");

        m_fileSystemProvider = fileSystemProvider;
        m_uri = uri;
        if (cacheTTL > 0) {
            m_cache = new BaseAttributesCache(cacheTTL);
        } else {
            m_cache = new NoOpAttributesCache();
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public FSFileSystemProvider<T, ?> provider() {
        return (FSFileSystemProvider<T, ?>)m_fileSystemProvider;
    }

    @Override
    public final void ensureClosedInternal() throws IOException {
        try {
            prepareClose();
        } finally {
            m_cache.clearCache();
            m_fileSystemProvider.removeFileSystem(m_uri);
        }
    }

    @Override
    public final void closeAllCloseables() {
        super.closeAllCloseables();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isReadOnly() {
        return false;
    }

    @Override
    public synchronized boolean isClosing() {
        return super.isClosing();
    }

    /**
     * This method is called in the {@link #close} method before the file system is removed from the list of file
     * systems in the provider. The method should ensure to close all open channels, directory-streams, and other
     * closeable objects associated with this file system.
     */
    protected abstract void prepareClose();

    /**
     * {@inheritDoc}
     */
    @Override
    public Iterable<FileStore> getFileStores() {
        return Collections.singletonList(new BaseFileStore(getSchemeString(), "default_file_store"));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<String> supportedFileAttributeViews() {
        final Set<String> supportedViews = new HashSet<>();
        supportedViews.add("basic");
        supportedViews.add("posix");
        return supportedViews;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PathMatcher getPathMatcher(final String syntaxAndPattern) {
        final int splitPosition = syntaxAndPattern.indexOf(':');

        if (splitPosition == -1 || splitPosition == syntaxAndPattern.length()) {
            throw new IllegalArgumentException("No valid input. Must be of form syntax:pattern");
        }

        final String syntax = syntaxAndPattern.substring(0, splitPosition);
        final String pattern = syntaxAndPattern.substring(splitPosition + 1);

        Pattern expr;
        if (syntax.equalsIgnoreCase("glob")) {
            expr = GlobToRegexConverter.convert(pattern, getSeparator().charAt(0));
        } else if (syntax.equalsIgnoreCase("regex")) {
            expr = Pattern.compile(pattern);
        } else {
            throw new UnsupportedOperationException(String.format("Syntax %s not supported", syntax));
        }
        return path -> expr.matcher(path.toString()).matches();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public UserPrincipalLookupService getUserPrincipalLookupService() {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public WatchService newWatchService() throws IOException {
        throw new UnsupportedOperationException();
    }

    /**
     * Stores an attribute for the path with the given URI in the attribute cache.
     *
     * @param path the path
     * @param attributes the attributes object to store
     */
    public void addToAttributeCache(final Path path, final BaseFileAttributes attributes) {
        m_cache.storeAttributes(getCachedAttributesKey(path), attributes);
    }

    /**
     * Removes an attribute for the path with the given URI from the attribute cache.
     *
     * @param path the path
     */
    public void removeFromAttributeCache(final Path path) {
        m_cache.removeAttribute(getCachedAttributesKey(path));
    }

    /**
     * Removes an attribute for the path and all of it's children from the attribute cache.
     *
     * @param path the path
     */
    public void removeFromAttributeCacheDeep(final Path path) {
        String key = getCachedAttributesKey(path);
        m_cache.removeAttribute(key);
        m_cache.removeAttributes(key + getSeparator());
    }

    /**
     * Returns an Optional containing the cached file-attributes for a path if present.
     *
     * @param path the path
     * @return optional file attributes from cache
     */
    public Optional<BaseFileAttributes> getCachedAttributes(final Path path) {
        return m_cache.getAttributes(getCachedAttributesKey(path));
    }

    /**
     * If a valid cache entry for this path with the given URI in the provider cache.
     *
     * @param path the path
     * @return whether a valid entry is in the cache
     */
    public boolean hasCachedAttributes(final Path path) {
        return m_cache.getAttributes(getCachedAttributesKey(path)).isPresent();
    }

    /**
     * Clears the attributes cache
     */
    public void clearAttributesCache() {
        m_cache.clearCache();
    }

    /**
     * Generates a attributes cache key for the given path.
     *
     * @param path the path to generate the key for
     * @return attributes cache key
     */
    protected String getCachedAttributesKey(final Path path) {
        return path.toAbsolutePath().normalize().toString();
    }

    /**
     * @return a String for the scheme to build a URI for this path
     */
    public abstract String getSchemeString();

    /**
     * @return a String for the host to build a URI for this path may be null
     */
    public abstract String getHostString();

}
