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
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.nio.file.WatchService;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.knime.filehandling.core.connections.DefaultFSLocationSpec;
import org.knime.filehandling.core.connections.FSCategory;
import org.knime.filehandling.core.connections.FSFileSystem;
import org.knime.filehandling.core.connections.FSLocationSpec;
import org.knime.filehandling.core.connections.config.LocalFSConnectionConfig;
import org.knime.filehandling.core.connections.meta.FSType;

/**
 *
 * @author bjoern
 */
final class LocalFileSystem extends FSFileSystem<LocalPath> {

    private static final FileSystem PLATFORM_DEFAULT_FS = FileSystems.getDefault();

    /**
     * The file system type of the local file system.
     */
    static final  FSType FS_TYPE = FSType.LOCAL_FS;

    private static final URI BASE_URI = URI.create(FS_TYPE.getTypeId() + ":///");

    /**
     * The {@link FSLocationSpec} for the local convenience file system.
     */
    static final FSLocationSpec CONVENIENCE_FS_LOCATION_SPEC = new DefaultFSLocationSpec(FSCategory.LOCAL);

    /**
     * The {@link FSLocationSpec} for the local convenience file system.
     */
    static final FSLocationSpec CONNECTED_FS_LOCATION_SPEC = new DefaultFSLocationSpec(FSCategory.CONNECTED, FS_TYPE.getTypeId());

    private final LocalFileSystemProvider m_provider;

    LocalFileSystem(final LocalFileSystemProvider provider, final LocalFSConnectionConfig config) {
        super(BASE_URI, createFSLocationSpec(config), config.getWorkingDirectory());
        provider.setFileSystem(this); // NOSONAR this is safe to do here
        m_provider = provider;
    }

    private static FSLocationSpec createFSLocationSpec(final LocalFSConnectionConfig config) {
        if(config.isConnectedFileSystem()) {
            return CONNECTED_FS_LOCATION_SPEC;
        } else {
            return CONVENIENCE_FS_LOCATION_SPEC;
        }
    }

    @Override
    public LocalFileSystemProvider provider() {
        return m_provider;
    }

    @Override
    public LocalPath getPath(final String first, final String... more) {
        return new LocalPath(this, Paths.get(first, more));
    }

    @Override
    public synchronized boolean isClosing() {
        return super.isClosing();
    }

    @Override
    public boolean isReadOnly() {
        return PLATFORM_DEFAULT_FS.isReadOnly();
    }

    @Override
    public String getSeparator() {
        return PLATFORM_DEFAULT_FS.getSeparator();
    }

    @Override
    public Iterable<Path> getRootDirectories() {
        final List<Path> roots = new ArrayList<>();
        for (Path localRoot : PLATFORM_DEFAULT_FS.getRootDirectories()) {
            roots.add(new LocalPath(this, localRoot));
        }
        return roots;
    }

    @Override
    public Iterable<FileStore> getFileStores() {
        return PLATFORM_DEFAULT_FS.getFileStores();
    }

    @Override
    public Set<String> supportedFileAttributeViews() {
        return PLATFORM_DEFAULT_FS.supportedFileAttributeViews();
    }

    @Override
    public PathMatcher getPathMatcher(final String syntaxAndPattern) {
        return PLATFORM_DEFAULT_FS.getPathMatcher(syntaxAndPattern);
    }

    @Override
    public UserPrincipalLookupService getUserPrincipalLookupService() {
        return PLATFORM_DEFAULT_FS.getUserPrincipalLookupService();
    }

    @Override
    public WatchService newWatchService() throws IOException {
        return PLATFORM_DEFAULT_FS.newWatchService();
    }

    @Override
    protected void ensureClosedInternal() throws IOException {
        // do nothing
    }
}
