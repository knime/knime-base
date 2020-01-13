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

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.WatchService;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.attribute.FileStoreAttributeView;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.nio.file.spi.FileSystemProvider;
import java.util.Collections;
import java.util.Set;

import org.knime.core.node.workflow.NodeContext;
import org.knime.filehandling.core.connections.base.UnixStylePathUtil;
import org.knime.filehandling.core.util.MountPointIDProviderService;

/**
 *
 * @author Tobias Urhaug, KNIME GmbH, Berlin, Germany
 */
public class KNIMERemoteFileSystem extends FileSystem {

    private final KNIMERemoteFileSystemProvider m_provider;

    private final URI m_mountpoint;

    private final NodeContext m_nodeContext;

    /**
     *
     *
     * @param provider
     * @param baseLocation
     */
    public KNIMERemoteFileSystem(final KNIMERemoteFileSystemProvider provider, final URI baseLocation) {
        m_provider = provider;
        m_mountpoint = baseLocation;

        m_nodeContext = NodeContext.getContext();
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
        // not needed
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
        return Collections.singletonList(new KNIMERemotePath(this, URI.create(UnixStylePathUtil.SEPARATOR)));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Iterable<FileStore> getFileStores() {
        return Collections.singleton(new FileStore() {

            @Override
            public String type() {
                return "KNIME Remote FileStore";
            }

            @Override
            public boolean supportsFileAttributeView(final String name) {
                return false;
            }

            @Override
            public boolean supportsFileAttributeView(final Class<? extends FileAttributeView> type) {
                return false;
            }

            @Override
            public String name() {
                return null;
            }

            @Override
            public boolean isReadOnly() {
                return false;
            }

            @Override
            public long getUsableSpace() throws IOException {
                return Long.MAX_VALUE;
            }

            @Override
            public long getUnallocatedSpace() throws IOException {
                return Long.MAX_VALUE;
            }

            @Override
            public long getTotalSpace() throws IOException {
                return Long.MAX_VALUE;
            }

            @Override
            public <V extends FileStoreAttributeView> V getFileStoreAttributeView(final Class<V> type) {
                return null;
            }

            @Override
            public Object getAttribute(final String attribute) throws IOException {
                return null;
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<String> supportedFileAttributeViews() {
        return Collections.emptySet();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Path getPath(final String first, final String... more) {
        return new KNIMERemotePath(this, first, more);
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

    /**
     * Returns the mount point of this remote KNIME file system.
     *
     * @return the mount point of this remote KNIME file system
     */
    public String getMountpoint() {
        return m_mountpoint.getHost();
    }

    /**
     * @return the nodeContext
     */
    public NodeContext getNodeContext() {
        return m_nodeContext;
    }

    /**
     * Returns the default directory of this file system.
     *
     * @return the default directory of this file system
     */
    public Path getDefaultDirectory() {
        return new KNIMERemotePath(this, MountPointIDProviderService.instance().getDefaultDirectory(m_mountpoint));
    }
}
