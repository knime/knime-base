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
import java.util.Optional;
import java.util.Set;

import org.knime.filehandling.core.connections.FSFileSystem;
import org.knime.filehandling.core.connections.FSFileSystemProvider;
import org.knime.filehandling.core.defaultnodesettings.FileSystemChoice.Choice;

/**
 *
 * @author bjoern
 */
class LocalFileSystem extends FSFileSystem<LocalPath> {

    public static final LocalFileSystem INSTANCE = new LocalFileSystem();

    private static final FileSystem DEFAULT_FS = FileSystems.getDefault();

    private LocalFileSystem() {
        super(Choice.LOCAL_FS, Optional.empty());
    }

    @Override
    public FSFileSystemProvider provider() {
        return LocalFileSystemProvider.INSTANCE;
    }

    @Override
    public LocalPath getPath(final String first, final String... more) {
        return new LocalPath(Paths.get(first, more));
    }

    @Override
    public void close() throws IOException {
        // do nothing

    }

    @Override
    public boolean isOpen() {
        return DEFAULT_FS.isOpen();
    }

    @Override
    public boolean isReadOnly() {
        return DEFAULT_FS.isReadOnly();
    }

    @Override
    public String getSeparator() {
        return DEFAULT_FS.getSeparator();
    }

    @Override
    public Iterable<Path> getRootDirectories() {
        final List<Path> roots = new ArrayList<>();
        for (Path localRoot : DEFAULT_FS.getRootDirectories()) {
            roots.add(new LocalPath(localRoot));
        }
        return roots;
    }

    @Override
    public Iterable<FileStore> getFileStores() {
        return DEFAULT_FS.getFileStores();
    }

    @Override
    public Set<String> supportedFileAttributeViews() {
        return DEFAULT_FS.supportedFileAttributeViews();
    }

    @Override
    public PathMatcher getPathMatcher(final String syntaxAndPattern) {
        return DEFAULT_FS.getPathMatcher(syntaxAndPattern);
    }

    @Override
    public UserPrincipalLookupService getUserPrincipalLookupService() {
        return DEFAULT_FS.getUserPrincipalLookupService();
    }

    @Override
    public WatchService newWatchService() throws IOException {
        return DEFAULT_FS.newWatchService();
    }
}
