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
 *   Aug 28, 2019 (bjoern): created
 */
package org.knime.filehandling.core.connections.url;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.WatchService;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.nio.file.spi.FileSystemProvider;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.knime.filehandling.core.connections.base.UnixStylePathUtil;

/**
 *
 * @author Bjoern Lohrmann, KNIME GmbH
 */
public class URIFileSystem extends FileSystem {

    private final URI m_uri;

    private final List<FileStore> m_fileStores;

    private final URIFileSystemProvider m_provider;

    URIFileSystem(final URI uri, final int timeoutInMillis) {
        m_uri = uri;
        m_fileStores = Collections.unmodifiableList(Collections.singletonList(new URIFileStore(uri)));
        m_provider = new URIFileSystemProvider(timeoutInMillis);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FileSystemProvider provider() {
        return m_provider;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() throws IOException {
        // nothing to do
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isOpen() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isReadOnly() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getSeparator() {
        return UnixStylePathUtil.SEPARATOR;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Iterable<Path> getRootDirectories() {
        try {
            return Collections.singletonList(new URIPath(this,
                new URI(m_uri.getScheme(), m_uri.getAuthority(), UnixStylePathUtil.SEPARATOR, null, null)));
        } catch (URISyntaxException ex) {
            throw new RuntimeException("Failed to create URI for root directory", ex);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Iterable<FileStore> getFileStores() {
        return m_fileStores;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<String> supportedFileAttributeViews() {
        return Collections.singleton("basic");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Path getPath(final String first, final String... more) {
        String path = first;
        if (more.length > 0) {
            path += UnixStylePathUtil.SEPARATOR + String.join(UnixStylePathUtil.SEPARATOR, more);
        }

        try {
            URI uri = URI.create(path);
            if (uri.getScheme() != null && !uri.getScheme().isEmpty()) {
                return new URIPath(this, uri);
            }
            return new URIPath(this, new URI(m_uri.getScheme(), m_uri.getAuthority(), path, null, null));
        } catch (URISyntaxException ex) {
            throw new RuntimeException("Failed to create URI", ex);
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PathMatcher getPathMatcher(final String syntaxAndPattern) {
        throw new UnsupportedOperationException();
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
}
