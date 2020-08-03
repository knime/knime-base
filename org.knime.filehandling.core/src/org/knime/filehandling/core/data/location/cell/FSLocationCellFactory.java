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
 *   Feb 24, 2020 (Simon Schmid, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.data.location.cell;

import java.io.IOException;
import java.util.Objects;
import java.util.UUID;

import org.knime.core.data.DataType;
import org.knime.core.data.filestore.FileStore;
import org.knime.core.data.filestore.FileStoreFactory;
import org.knime.core.data.filestore.FileStoreUtil;
import org.knime.core.node.util.CheckUtils;
import org.knime.filehandling.core.connections.FSLocation;
import org.knime.filehandling.core.connections.FSLocationSpec;

/**
 * Factory for {@link FSLocationCell}s. Cells created by such a factory share the same meta data and {@link FileStore}.
 *
 * @author Simon Schmid, KNIME GmbH, Konstanz, Germany
 * @since 4.2
 * @noreference non-public API
 * @noinstantiate non-public API
 */
public final class FSLocationCellFactory {

    /**
     * The {@link DataType} of cells created by this factory.
     */
    public static final DataType TYPE = FSLocationCell.TYPE;

    private final FSLocationCellMetaData m_metaData;

    private final FileStore m_fileStore;

    /**
     * Constructs a {@link FSLocationCellFactory} for the creation of {@link FSLocationCell}s.
     *
     * @param fileStoreFactory used to create the {@link FileStore} shared by the cells created by this factory
     * @param fileSystemType the file system type
     * @param fileSystemSpecifier the file system specifier, can be {@code null}
     */
    public FSLocationCellFactory(final FileStoreFactory fileStoreFactory, final String fileSystemType,
        final String fileSystemSpecifier) {
        // Check null
        CheckUtils.checkNotNull(fileStoreFactory, "The file store factory must not be null.");
        CheckUtils.checkArgumentNotNull(fileSystemType, "The file system must not be null.");
        try {
            m_fileStore = fileStoreFactory.createFileStore(UUID.randomUUID().toString());
        } catch (IOException ex) {
            throw new IllegalStateException("Can't create file store for location cells.", ex);
        }
        m_metaData =
            new FSLocationCellMetaData(FileStoreUtil.getFileStoreKey(m_fileStore), fileSystemType, fileSystemSpecifier);
    }

    /**
     * Constructs a {@link FSLocationCellFactory} for the creation of {@link FSLocationCell}s.
     *
     * @param fileStoreFactory used to create the {@link FileStore} shared by the cells created by this factory
     * @param fsLocationSpec the file system location spec object holding the type and specifier of the file system
     */
    public FSLocationCellFactory(final FileStoreFactory fileStoreFactory, final FSLocationSpec fsLocationSpec) {
        this(fileStoreFactory, fsLocationSpec.getFileSystemCategory(), fsLocationSpec.getFileSystemSpecifier().orElse(null));
    }

    /**
     * Create a new {@link FSLocationCell}.
     *
     * @param fsLocation the file system location
     * @return a {@link FSLocationCell} containing a path and information about the file system
     * @throws NullPointerException if {@code fsLocation} is null
     * @throws IllegalArgumentException if the path is null or the file system type and specifier do not match the ones
     *             in the meta data
     */
    public FSLocationCell createCell(final FSLocation fsLocation) {
        // Check null
        CheckUtils.checkNotNull(fsLocation, "The file system location must not be null.");
        CheckUtils.checkArgumentNotNull(fsLocation.getPath(), "The path must not be null.");
        // Check that fs type and specifier are compatible with meta data
        final String fileSystemType = fsLocation.getFileSystemCategory();
        CheckUtils.checkArgument(fileSystemType.equals(m_metaData.getFileSystemType()),
            "The file system type (%s) must match the file system type in the meta data (%s).", fileSystemType,
            m_metaData.getFileSystemType());
        final String fileSystemSpecifier = fsLocation.getFileSystemSpecifier().orElse(null);
        CheckUtils.checkArgument(Objects.equals(fileSystemSpecifier, m_metaData.getFileSystemSpecifier()),
            "The file system specifier (%s) must match the file system specifier in the meta data (%s).",
            fileSystemSpecifier, m_metaData.getFileSystemSpecifier());
        return new FSLocationCell(m_metaData, m_fileStore, fsLocation);
    }

    /**
     * Create a new {@link FSLocationCell}.
     *
     * @param path the path
     * @return a {@link FSLocationCell} containing a path and information about the file system
     * @throws NullPointerException if {@code path} is null
     */
    public FSLocationCell createCell(final String path) {
        // Check null
        CheckUtils.checkNotNull(path, "The path must not be null.");
        return new FSLocationCell(m_metaData, m_fileStore,
            new FSLocation(m_metaData.getFileSystemType(), m_metaData.getFileSystemSpecifier(), path));
    }
}
